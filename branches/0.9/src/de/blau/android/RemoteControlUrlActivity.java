package de.blau.android;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.prefs.URLListEditActivity.ListEditItem;
import de.blau.android.util.GeoMath;
import de.blau.android.util.OAuthHelper;

/**
 * Start vespucci with OSM remote control url
 */
public class RemoteControlUrlActivity extends Activity {
	
	public static final String RCDATA = "de.blau.android.RemoteControlActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Uri data = getIntent().getData(); 
		Log.d("RemoteControlUrlActivity",data.toString());
	    Intent intent = new Intent(this, Main.class);
	    String command = data.getPath();
	    if (command.startsWith("/")) {
	    	command = command.substring(1);
	    }
	    Log.d("RemoteControlUrlActivity",command);
	    if (command.equals("load_and_zoom") || command.equals("zoom")) {
	    	try {
				Double left = Double.valueOf(data.getQueryParameter("left"));
				Double right = Double.valueOf(data.getQueryParameter("right"));
				Double bottom = Double.valueOf(data.getQueryParameter("bottom"));
				Double top = Double.valueOf(data.getQueryParameter("top"));
				
				RemoteControlUrlData rcData = new RemoteControlUrlData();
				rcData.setBox(new BoundingBox(left, bottom, right, top));
				rcData.setLoad(command.equals("load_and_zoom"));
				intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
				Log.d("RemoteControlUrlActivity","bbox " + rcData.getBox() + " load " + rcData.load());
				intent.putExtra(RCDATA, rcData);

			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				Log.d("RemoteControlUrlActivity","NumberFormatException ", e);
				e.printStackTrace();
			} catch (OsmException e) {
				Log.d("RemoteControlUrlActivity","OsmException ", e);
				// TODO Auto-generated catch block
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
		/**
		 * @return the box
		 */
		public BoundingBox getBox() {
			return box;
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
