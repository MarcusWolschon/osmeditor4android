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
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.prefs.URLListEditActivity.ListEditItem;
import de.blau.android.util.GeoMath;
import de.blau.android.util.OAuthHelper;

/**
 * Start vespucci with geo: URLs.
 * see http://www.ietf.org/rfc/rfc5870.txt
 */
public class GeoUrlActivity extends Activity {
	
	public static final String GEODATA = "de.blau.android.GeoUrlActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Uri data = getIntent().getData(); 
		Log.d("GeoURLActivity",data.toString());
	    Intent intent = new Intent(this, Main.class);
		String[] params = data.getSchemeSpecificPart().split(";");
		if (params != null && params.length >= 1) {
			String[] coords = params[0].split(",");
			boolean wgs84 = true; // for now the only supported datum
			if (params.length > 1) {
				for (String p:params) {
					if (p.toLowerCase().matches("crs=.*")) {
						wgs84 = p.toLowerCase().matches("crs=wgs84");
						Log.d("GeoUrlActivity","crs found " + p + ", is wgs84 is " + wgs84);
					}
				}
			}
			if (coords != null && coords.length >= 2 && wgs84) {
				double lat = Double.valueOf(coords[0]);
				double lon = Double.valueOf(coords[1]);
				if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
					GeoUrlData geoData = new GeoUrlData();
					geoData.setLat(lat);
					geoData.setLon(lon);
					intent.putExtra(GEODATA, geoData);
				}
			}
		}
	    startActivity(intent);
	}
	
	public static class GeoUrlData implements Serializable {
		private static final long serialVersionUID = 2L;
		private double lat;
		private double lon;
		/**
		 * @return the lat
		 */
		public double getLat() {
			return lat;
		}
		/**
		 * @param lat the lat to set
		 */
		public void setLat(double lat) {
			this.lat = lat;
		}
		/**
		 * @return the lon
		 */
		public double getLon() {
			return lon;
		}
		/**
		 * @param lon the lon to set
		 */
		public void setLon(double lon) {
			this.lon = lon;
		}
	}
}
