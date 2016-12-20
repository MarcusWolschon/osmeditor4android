package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import android.util.Log;
import de.blau.android.Application;
import de.blau.android.contract.Urls;
import de.blau.android.osm.BoundingBox;
import de.blau.android.tasks.Task.State;

public class OsmoseServer {
	
	private static final String DEBUG_TAG = OsmoseServer.class.getSimpleName();
	
	final static String serverHost = Urls.OSMOSE;
	final static String apiPath = "/api/0.2/";
	/** 
	 * the list of supported languages was simply generated from the list of .po in the osmose repo and tested against the API
	 */
	final static List<String> supportedLanguages = Arrays.asList("ca", "cs", "en", "da", "de", "el", "es", "fr", "hu", "it", "ja", "lt", "nl", "pl", "pt", "ro", "ru", "sw", "uk");
	
	/**
	 * Timeout for connections in milliseconds.
	 */
	private static final int TIMEOUT = 45 * 1000;
	
	/**
	 * Perform an HTTP request to download up to limit bugs inside the specified area.
	 * Blocks until the request is complete.
	 * @param area Latitude/longitude *1E7 of area to download.
	 * @return All the bugs in the given area.
	 */
	public static Collection<OsmoseBug> getBugsForBox(BoundingBox area, long limit) {
		Collection<OsmoseBug> result = null;
		// http://osmose.openstreetmap.fr/de/api/0.2/errors?bbox=8.32,47.33,8.42,47.28&full=true
		try {
			Log.d(DEBUG_TAG, "getBugssForBox");
			URL url;

			url = new URL(getServerURL()  + "errors?" +
					"bbox=" +
					area.getLeft() / 1E7d +
					"," + area.getBottom() / 1E7d +
					"," + area.getRight() / 1E7d +
					"," + area.getTop() / 1E7d +
					"&full=true");
			
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			boolean isServerGzipEnabled = false;

			//--Start: header not yet sent
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("Accept-Encoding", "gzip");
			con.setRequestProperty("User-Agent", Application.userAgent);

			//--Start: got response header
			isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
			
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return new ArrayList<OsmoseBug>(); 
			}

			InputStream is;
			if (isServerGzipEnabled) {
				is = new GZIPInputStream(con.getInputStream());
			} else {
				is = con.getInputStream();
			}
			result = OsmoseBug.parseBugs(is);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public static boolean changeState(OsmoseBug bug) {
		// http://osmose.openstreetmap.fr/de/api/0.2/error/3313305479/done
		// http://osmose.openstreetmap.fr/de/api/0.2/error/3313313045/false
		if (bug.state ==  State.OPEN) {
			return false; // open is the default state and we shouldn't actually get here
		}
		try {		
			URL url;
			url = new URL(getServerURL()  + "error/" + bug.getId() + "/" + (bug.state == State.CLOSED ? "done" : "false"));	
			Log.d(DEBUG_TAG, "changeState " + url.toString());
			
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			//--Start: header not yet sent
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("User-Agent", Application.userAgent);
			int responseCode = con.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				Log.d(DEBUG_TAG, "changeState respnse code " + responseCode);
				if (responseCode ==  HttpURLConnection.HTTP_GONE) {
					bug.changed = false; // don't retry
					Application.getTaskStorage().setDirty();
				}
				return false; 
			}
			bug.changed = false;
			Application.getTaskStorage().setDirty();			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false; 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false; 
		}
		Log.d(DEBUG_TAG, "changeState sucess");
		return true;	
	}
	
	private static String getServerURL() {
		String lang = Locale.getDefault().getLanguage();
		if (!supportedLanguages.contains(lang)) {
			lang = "en";
		}
		return serverHost + lang + apiPath;
	}
}
