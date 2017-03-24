package de.blau.android;

import java.io.Serializable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;

/**
 * Start vespucci with JOSM style remote control url
 */
public class RemoteControlUrlActivity extends Activity {
	
	private static final String DEBUG_TAG = "RemoteControlUrlAct...";
	public static final String RCDATA = "de.blau.android.RemoteControlActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Uri data = getIntent().getData(); 
		if (data == null) {
			Log.d(DEBUG_TAG,"Called with null data, aborting");
			finish();
			return;
		}
		Log.d(DEBUG_TAG,data.toString());
		Intent intent = new Intent(this, Main.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
		try {  
			// extract command
			String command = data.getPath();
			if ("josm".equals(data.getScheme())) { // extract command from scheme specific part
				command = data.getSchemeSpecificPart();
				if (command != null) {
					int q = command.indexOf('?');
					if (q > 0) {
						command = command.substring(0, q);
					}
				}
			}
			if (command != null && command.startsWith("/")) { // remove any 
				command = command.substring(1);
			}

			Log.d(DEBUG_TAG,"Command: " + command);
			Log.d(DEBUG_TAG,"Query: " + data.getQuery());
			boolean loadAndZoom = "load_and_zoom".equals(command);
			if (loadAndZoom || "zoom".equals(command)) {
				try {
					RemoteControlUrlData rcData = new RemoteControlUrlData();
					rcData.setLoad(loadAndZoom);
					String leftParam = data.getQueryParameter("left");
					String rightParam = data.getQueryParameter("right");
					String bottomParam = data.getQueryParameter("bottom");
					String topParam = data.getQueryParameter("top");

					if (leftParam != null && rightParam != null && bottomParam != null && topParam != null) {
						try {
							Double left = Double.valueOf(leftParam);
							Double right = Double.valueOf(rightParam);
							Double bottom = Double.valueOf(bottomParam);
							Double top = Double.valueOf(topParam);
							rcData.setBox(new BoundingBox(left, bottom, right, top));
							Log.d(DEBUG_TAG,"bbox " + rcData.getBox() + " load " + rcData.load());
						} catch (NumberFormatException e) {
							Log.d(DEBUG_TAG,"Invalid bounding box parameter", e);
							intent = null;
						}
					}
					if (intent != null) { 
						String select = data.getQueryParameter("select");
						if (rcData.load() && select != null) {
							rcData.setSelect(select);
						}
						intent.putExtra(RCDATA, rcData);
					}
				} catch (OsmException e) {
					Log.d(DEBUG_TAG,"OsmException ", e);
					intent = null;
				}
			} else {
				Log.d(DEBUG_TAG,"Unknown RC command: " + command);
				intent = null;
			}
			if (intent != null) {
				startActivity(intent);
			}
		} catch (Exception ex) { // avoid crashing on getting called with stuff that can't be parsed
			Log.d(DEBUG_TAG,"Exception: " + ex);
			intent = null;
		}
		setResult(intent != null ? RESULT_OK : RESULT_CANCELED); // not clear if this actually helps with anything 
		finish();
	}
	
	public static class RemoteControlUrlData implements Serializable {
		private static final long serialVersionUID = 1L;
		private boolean load = false;
		private BoundingBox box;
		private String select = null;
		/**
		 * @return the box
		 */
		public BoundingBox getBox() {
			return box;
		}
		/**
		 * return string with elements to select
		 * @return
		 */
		public String getSelect() {
			return select;
		}
		public void setSelect(String select) {
			this.select = select;
		}
		/**
		 * @param box the box to set
		 */
		public void setBox(BoundingBox box) {
			this.box = box;
		}
		/**
		 * @return the load
		 */
		public boolean load() {
			return load;
		}
		/**
		 * @param load the load to set
		 */
		public void setLoad(boolean load) {
			this.load = load;
		}
	}
}
