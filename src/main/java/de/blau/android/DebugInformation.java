package de.blau.android;

import java.io.File;
import java.util.Date;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.DateFormatter;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

public class DebugInformation extends AppCompatActivity {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
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
        send.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ACRA.getErrorReporter().putCustomData("DEBUGINFO", getDebugText("<BR>"));
                ACRA.getErrorReporter().handleException(null);
            }
        });

        textFull.setAutoLinkMask(0);
        textFull.setText(getDebugText("\n"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            textFull.setTextIsSelectable(true);
        }

        setContentView(container);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    String getDebugText(String eol) {
        StringBuilder builder = new StringBuilder();

        builder.append(getString(R.string.app_name_version) + eol);
        builder.append("Maximum avaliable memory " + Runtime.getRuntime().maxMemory() + eol);
        builder.append("Total memory used " + Runtime.getRuntime().totalMemory() + eol);
        Logic logic = App.getLogic();
        if (logic != null) {
            Map map = logic.getMap();
            if (map != null) {
                for (MapViewLayer ov : map.getLayers()) {
                    if (ov instanceof MapTilesLayer || ov instanceof MapTilesOverlayLayer) {
                        builder.append("Tile Cache " + ((MapTilesLayer) ov).getTileLayerConfiguration().getId() + " usage "
                                + ((MapTilesLayer) ov).getTileProvider().getCacheUsageInfo() + eol);
                    }
                }
            } else {
                builder.append("Map not available, this is a seriously curious state, please report a bug!\n");
            }
        } else {
            builder.append("Logic not available, this is a seriously curious state, please report a bug!\n");
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
        StorageDelegator delegator = App.getDelegator();
        if (delegator != null) {
            builder.append("Relations (current/API): " + delegator.getCurrentStorage().getRelations().size() + "/" + delegator.getApiRelationCount() + eol);
            builder.append("Ways (current/API): " + delegator.getCurrentStorage().getWays().size() + "/" + delegator.getApiWayCount() + eol);
            builder.append("Nodes (current/Waynodes/API): " + delegator.getCurrentStorage().getNodes().size() + "/"
                    + delegator.getCurrentStorage().getWaynodes().size() + "/" + delegator.getApiNodeCount() + eol);
        } else {
            builder.append("Delegator not available, this is a seriously curious state, please report a bug!\n");
        }

        builder.append("Available location providers\n");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        for (String providerName : locationManager.getAllProviders()) {
            builder.append(providerName + " enabled " + locationManager.isProviderEnabled(providerName) + eol);
        }
        return builder.toString();
    }
}
