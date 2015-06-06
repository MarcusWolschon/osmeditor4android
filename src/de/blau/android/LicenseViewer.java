package de.blau.android;

import java.io.InputStreamReader;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

import de.blau.android.prefs.Preferences;
import de.blau.android.util.SavingHelper;
import de.blau.android.views.overlay.OpenStreetMapOverlayTilesOverlay;
import de.blau.android.views.overlay.OpenStreetMapTilesOverlay;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

public class LicenseViewer extends SherlockActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_Sherlock_Light);
		}
		
		super.onCreate(savedInstanceState);
		View container = View.inflate(this, R.layout.license_viewer, null);
		TextView textShort = (TextView)container.findViewById(R.id.licenseShortText);
		TextView textFull = (TextView)container.findViewById(R.id.licenseFullText);
		
		textShort.setText(load("LICENSE.txt"));
		
		StringBuilder builder = new StringBuilder();
		load("LICENSE-GPL3.txt", builder);
		load("LICENSE-Apache.txt", builder);
		load("josm-contributors.txt", builder);
		builder.append("== Debugging Information ==\n");
		builder.append("Maximum avaliable memory " + Runtime.getRuntime().maxMemory() + "\n");
		builder.append("Total memory used " + Runtime.getRuntime().totalMemory() + "\n");
		for (OpenStreetMapViewOverlay ov:Application.mainActivity.getMap().mOverlays) {
			if (ov instanceof OpenStreetMapTilesOverlay|| ov instanceof OpenStreetMapOverlayTilesOverlay) {
				builder.append("Tile Cache " + ((OpenStreetMapTilesOverlay)ov).getRendererInfo().getId() + " usage " + ((OpenStreetMapTilesOverlay)ov).getTileProvider().getCacheUsageInfo() + "\n");
			}
		}
		textFull.setText(builder.toString());

		
		setContentView(container);
	}
	
	private String load(String filename) {
		StringBuilder builder = new StringBuilder();
		load(filename, builder);
		return builder.toString();
	}
	
	private void load(String filename, StringBuilder builder) {
		builder.append("== " + filename + " ==\n");
		
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(getAssets().open(filename), "UTF-8");
			int read = 0;
			do {
				char[] buf = new char[4096];
				read = reader.read(buf);
				if (read > 0) builder.append(buf, 0, read);
			} while (read > 0);
		} catch (Exception e) {
			builder.append("Error while loading file: " + e.getMessage());
		} finally {
			SavingHelper.close(reader);
		}
		builder.append("\n\n\n\n");
	}
}
