package de.blau.android;

import java.io.Serializable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import de.blau.android.RemoteControlUrlActivity.RemoteControlUrlData;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;

/**
 * Start vespucci with OSM remote control url
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
	    String command = data.getPath();
	    if (command.startsWith("/")) {
	    	command = command.substring(1);
	    }
	    Log.d(DEBUG_TAG,command);
	    if (command.equals("load_and_zoom") || command.equals("zoom")) {
	    	try {
	    		RemoteControlUrlData rcData = new RemoteControlUrlData();

	    		rcData.setLoad(command.equals("load_and_zoom"));
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
	    				Log.d(DEBUG_TAG,"NumberFormatException ", e);
	    				e.printStackTrace();
	    			}
	    		}

	    		String select = data.getQueryParameter("select");
	    		if (rcData.load() && select != null) {
	    			rcData.setSelect(select);
	    		}
	    		intent.putExtra(RCDATA, rcData);

	    	} catch (OsmException e) {
	    		Log.d(DEBUG_TAG,"OsmException ", e);
	    		e.printStackTrace();
	    	}
	    }
	    startActivity(intent);
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
