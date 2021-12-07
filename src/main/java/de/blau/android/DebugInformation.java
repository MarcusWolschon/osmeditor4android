package de.blau.android;

import java.io.File;
import java.util.Date;

import org.acra.ACRA;

import com.zeugmasolutions.localehelper.LocaleAwareCompatActivity;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.Server.UserDetails;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.DateFormatter;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

public class DebugInformation extends LocaleAwareCompatActivity {
    private static final String DEBUG_TAG = "DebugInformation";

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private Preferences prefs;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = new Preferences(this);
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
    String getDebugText(String eol) {
        StringBuilder builder = new StringBuilder();

        builder.append(getString(R.string.app_name_version) + eol);
        builder.append("Flavor: " + BuildConfig.FLAVOR + eol);
        ApplicationInfo appInfo = getApplicationContext().getApplicationInfo();
        builder.append("Target SDK: " + appInfo.targetSdkVersion + eol);

        builder.append("Maximum avaliable memory " + Runtime.getRuntime().maxMemory() + eol);
        builder.append("Total memory used " + Runtime.getRuntime().totalMemory() + eol);
        Logic logic = App.getLogic();
        if (logic != null) {
            Map map = logic.getMap();
            if (map != null) {
                for (MapViewLayer ov : map.getLayers()) {
                    if (ov instanceof MapTilesLayer || ov instanceof MapTilesOverlayLayer) {
                        TileLayerSource tileLayerConfiguration = ((MapTilesLayer<?>) ov).getTileLayerConfiguration();
                        if (tileLayerConfiguration != null) {
                            builder.append("Tile Cache " + tileLayerConfiguration.getId() + " usage "
                                    + ((MapTilesLayer<?>) ov).getTileProvider().getCacheUsageInfo() + eol);
                        }
                    }
                }
            } else {
                builder.append("Map not available, this is a seriously curious state, please report a bug!" + eol);
            }
        } else {
            builder.append("Logic not available, this is a seriously curious state, please report a bug!" + eol);
        }
        File stateFile = new File(getFilesDir(), StorageDelegator.FILENAME);
        if (stateFile.exists()) {
            builder.append("State file size " + stateFile.length() + " last changed "
                    + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(stateFile.lastModified())) + eol);
        } else {
            builder.append("No state file found\n");
        }
        File bugStateFile = new File(getFilesDir(), TaskStorage.FILENAME);
        if (bugStateFile.exists()) {
            builder.append("Bug state file size " + bugStateFile.length() + " last changed "
                    + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(bugStateFile.lastModified())) + eol);
        } else {
            builder.append("No bug state file found\n");
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

        return builder.toString();
    }
}
