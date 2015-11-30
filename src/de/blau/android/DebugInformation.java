package de.blau.android;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Date;

import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

import de.blau.android.osb.BugStorage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.SavingHelper;
import de.blau.android.views.overlay.OpenStreetMapOverlayTilesOverlay;
import de.blau.android.views.overlay.OpenStreetMapTilesOverlay;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

public class DebugInformation extends SherlockActivity {
	private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_Sherlock_Light);
		}
		
		super.onCreate(savedInstanceState);
		View container = View.inflate(this, R.layout.debug_viewer, null);
		TextView textFull = (TextView)container.findViewById(R.id.debugText);
	
		StringBuilder builder = new StringBuilder();
	
		builder.append(getString(R.string.app_name_version) + "\n");
		builder.append("Maximum avaliable memory " + Runtime.getRuntime().maxMemory() + "\n");
		builder.append("Total memory used " + Runtime.getRuntime().totalMemory() + "\n");
		for (OpenStreetMapViewOverlay ov:Application.mainActivity.getMap().mOverlays) {
			if (ov instanceof OpenStreetMapTilesOverlay|| ov instanceof OpenStreetMapOverlayTilesOverlay) {
				builder.append("Tile Cache " + ((OpenStreetMapTilesOverlay)ov).getRendererInfo().getId() + " usage " + ((OpenStreetMapTilesOverlay)ov).getTileProvider().getCacheUsageInfo() + "\n");
			}
		}
		File stateFile = new File(getFilesDir(), StorageDelegator.FILENAME);
		if (stateFile.exists()) {
			builder.append("State file size " +  stateFile.length() + " last changed " + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(stateFile.lastModified())) + "\n");
		} else {
			builder.append("No state file found\n");
		}
		File bugStateFile = new File(getFilesDir(), BugStorage.FILENAME);
		if (bugStateFile.exists()) {
			builder.append("Bug state file size " +  bugStateFile.length() + " last changed " + DateFormatter.getFormattedString(DATE_TIME_PATTERN, new Date(bugStateFile.lastModified())) + "\n");
		} else {
			builder.append("No bug state file found\n");
		}
		StorageDelegator delegator = Application.getDelegator();
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
		
		textFull.setText(builder.toString());
	
		setContentView(container);
	}
}
