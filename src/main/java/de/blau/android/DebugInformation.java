package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.util.Date;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import de.blau.android.layer.LayerConfig;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.tiles.MapTilesLayer;
import de.blau.android.layer.tiles.MapTilesOverlayLayer;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.UserDetails;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.services.util.MapTileFilesystemProvider;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ConfigurationChangeAwareActivity;
import de.blau.android.util.DateFormatter;

public class DebugInformation extends ConfigurationChangeAwareActivity {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DebugInformation.class.getSimpleName().length());
    private static final String DEBUG_TAG = DebugInformation.class.getSimpleName().substring(0, TAG_LEN);

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private Preferences prefs;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = App.getPreferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customLight);
        }

        super.onCreate(savedInstanceState);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setTitle(R.string.config_debugbutton_title);

        View container = View.inflate(this, R.layout.debug_viewer, null);
        TextView textFull = (TextView) container.findViewById(R.id.debugText);

        Button send = (Button) container.findViewById(R.id.sendDebug);
        send.setOnClickListener(v -> {
            ACRA.getErrorReporter().putCustomData("DEBUGINFO", getDebugText("<BR>"));
            ACRA.getErrorReporter().handleException(null);
        });

        textFull.setAutoLinkMask(0);
        textFull.setText(getDebugText("\n"));
        textFull.setTextIsSelectable(true);

        setContentView(container);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            Log.w(DEBUG_TAG, "Unknown menu item " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Generate the debug text we want to display
     * 
     * @param eol what to use as end of line
     * @return a String containing the text
     */
    String getDebugText(@NonNull String eol) {
        StringBuilder builder = new StringBuilder();

        builder.append(getString(R.string.app_name_version) + eol);
        builder.append("Flavor: " + BuildConfig.FLAVOR + eol);
        ApplicationInfo appInfo = getApplicationContext().getApplicationInfo();
        builder.append("Target SDK: " + appInfo.targetSdkVersion + eol);

        builder.append("Maximum avaliable memory " + Runtime.getRuntime().maxMemory() + eol);
        builder.append("Total memory used (non-native) " + Runtime.getRuntime().totalMemory() + eol);
        builder.append("Native memory usage " + Debug.getNativeHeapAllocatedSize() + eol);
        Logic logic = App.getLogic();
        if (logic != null) {
            appendLayers(eol, builder, logic);
            appendSelection(eol, builder, logic);
        } else {
            builder.append("Logic not available, this is a seriously curious state, please report a bug!" + eol);
        }
        MapTileFilesystemProvider fsProvider = App.getMapTileFilesystemProvider(this);
        if (fsProvider != null) {
            builder.append("Current used file system net tile cache size: " + fsProvider.getCurrentCacheByteSize() + "B" + eol);
        } else {
            builder.append("No file system tile cache!" + eol);
        }
        File stateFile = new File(getFilesDir(), StorageDelegator.FILENAME);
        if (stateFile.exists()) {
            builder.append("State file size " + stateFile.length() + " last changed "
                    + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(stateFile.lastModified())) + eol);
        } else {
            builder.append("No state file found" + eol);
        }
        File bugStateFile = new File(getFilesDir(), TaskStorage.FILENAME);
        if (bugStateFile.exists()) {
            builder.append("Bug state file size " + bugStateFile.length() + " last changed "
                    + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(bugStateFile.lastModified())) + eol);
        } else {
            builder.append("No bug state file found" + eol);
        }

        ACRAHelper.addElementCounts(builder, eol);

        builder.append("Available location providers\n");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        for (String providerName : locationManager.getAllProviders()) {
            builder.append(providerName + " enabled " + locationManager.isProviderEnabled(providerName) + eol);
        }

        UserDetails userDetails = prefs.getServer().getCachedUserDetails();
        if (userDetails != null) {
            builder.append("Display name " + userDetails.getDisplayName() + eol);
        }

        builder.append("Configured layers" + eol);
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(this)) {
            for (LayerConfig layer : db.getLayers()) {
                builder.append(layer.toString() + eol);
            }
        }

        return builder.toString();
    }

    /**
     * Append selection information
     * 
     * @param eol ELO string
     * @param builder the StringBuilder
     * @param logic the Logic instance
     */
    private void appendSelection(String eol, StringBuilder builder, Logic logic) {
        builder.append("Selection stack" + eol);
        int pos = 0;
        for (Selection s : logic.getSelectionStack()) {
            builder.append("Selection " + pos + " " + s.nodeCount() + " nodes " + s.wayCount() + " ways " + s.relationCount() + " relations" + eol);
            pos++;
        }
    }

    /**
     * Append layer information
     * 
     * @param eol ELO string
     * @param builder the StringBuilder
     * @param logic the Logic instance
     */
    private void appendLayers(@NonNull String eol, @NonNull StringBuilder builder, @NonNull Logic logic) {
        Map map = logic.getMap();
        if (map != null) {
            for (MapViewLayer ov : map.getLayers()) {
                if (ov instanceof MapTilesLayer || ov instanceof MapTilesOverlayLayer) {
                    TileLayerSource tileLayerConfiguration = ((MapTilesLayer<?>) ov).getTileLayerConfiguration();
                    if (tileLayerConfiguration != null) {
                        builder.append("In memory Tile Cache " + tileLayerConfiguration.getId() + " type " + tileLayerConfiguration.getType() + " tiles "
                                + tileLayerConfiguration.getTileType() + " usage " + ((MapTilesLayer<?>) ov).getTileProvider().getCacheUsageInfo() + eol);
                    }
                }
            }
        } else {
            builder.append("Map not available, this is a seriously curious state, please report a bug!" + eol);
        }
    }
}
