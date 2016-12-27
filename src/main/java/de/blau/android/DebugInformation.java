package de.blau.android;

import java.io.File;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.DateFormatter;
import de.blau.android.views.overlay.MapOverlayTilesOverlay;
import de.blau.android.views.overlay.MapTilesOverlay;
import de.blau.android.views.overlay.MapViewOverlay;

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
		View container = View.inflate(this, R.layout.debug_viewer, null);
		TextView textFull = (TextView)container.findViewById(R.id.debugText);
	
		StringBuilder builder = new StringBuilder();
	
		builder.append(getString(R.string.app_name_version) + "\n");
		builder.append("Maximum avaliable memory " + Runtime.getRuntime().maxMemory() + "\n");
		builder.append("Total memory used " + Runtime.getRuntime().totalMemory() + "\n");
		Main main = App.mainActivity;
		if (main != null) {
			List<MapViewOverlay> overlays = main.getMap().mOverlays;
			synchronized(overlays) {
				for (MapViewOverlay ov:overlays) {
					if (ov instanceof MapTilesOverlay || ov instanceof MapOverlayTilesOverlay) {
						builder.append("Tile Cache " + ((MapTilesOverlay)ov).getRendererInfo().getId() + " usage " + ((MapTilesOverlay)ov).getTileProvider().getCacheUsageInfo() + "\n");
					}
				}
			}
		} else {
			builder.append("Main not available\n");
		}
		File stateFile = new File(getFilesDir(), StorageDelegator.FILENAME);
		if (stateFile.exists()) {
			builder.append("State file size " +  stateFile.length() + " last changed " + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(stateFile.lastModified())) + "\n");
		} else {
			builder.append("No state file found\n");
		}
		File bugStateFile = new File(getFilesDir(), TaskStorage.FILENAME);
		if (bugStateFile.exists()) {
			builder.append("Bug state file size " +  bugStateFile.length() + " last changed " + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(bugStateFile.lastModified())) + "\n");
		} else {
			builder.append("No bug state file found\n");
		}
		StorageDelegator delegator = App.getDelegator();
		builder.append("Relations (current/API): " + delegator.getCurrentStorage().getRelations().size() + "/"
				+ delegator.getApiRelationCount()+"\n");
		builder.append("Ways (current/API): " + delegator.getCurrentStorage().getWays().size() + "/"
				+ delegator.getApiWayCount()+"\n");
		builder.append("Nodes (current/Waynodes/API): " + delegator.getCurrentStorage().getNodes().size() + "/"
				+ delegator.getCurrentStorage().getWaynodes().size() + "/" + delegator.getApiNodeCount()+"\n");
		
		builder.append("Available location providers\n");
		LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		for (String providerName:locationManager.getAllProviders()) {
			builder.append(providerName + " enabled " + locationManager.isProviderEnabled(providerName) + "\n");
		}
		textFull.setAutoLinkMask(0);
		textFull.setText(builder.toString());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			textFull.setTextIsSelectable(true);
		}
	
		setContentView(container);
	}
}
