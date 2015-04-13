package de.blau.android;

import java.io.Serializable;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import de.blau.android.util.GeoMath;

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
	    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    String[] query = data.getSchemeSpecificPart().split("\\?"); // used by osmand likely not standard conform
	    if (query != null && query.length >= 1) {
			String[] params = query[0].split(";");
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
					try {
						double lat = Double.valueOf(coords[0]);
						double lon = Double.valueOf(coords[1]);
						if (lon >= -180 && lon <= 180 && lat >= -GeoMath.MAX_LAT && lat <= GeoMath.MAX_LAT) {
							GeoUrlData geoData = new GeoUrlData();
							geoData.setLat(lat);
							geoData.setLon(lon);
							intent.putExtra(GEODATA, geoData);
						}
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
	    }
	    startActivity(intent);
	    finish();
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
