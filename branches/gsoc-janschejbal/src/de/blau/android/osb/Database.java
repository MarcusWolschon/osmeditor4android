package de.blau.android.osb;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.blau.android.Application;
import de.blau.android.services.util.StreamUtils;

import android.graphics.Rect;
import android.util.Log;

/**
 * Utility class to interface to the online OpenStreetBugs database. Solely consists
 * of blocking static methods.
 * @author Andrew Gregory
 */
public class Database {
	
	/** OSB online database host name. */
	private static final String HOST = "openstreetbugs.schokokeks.org";
	
	/** Default OSB API. */
	private static final String API = "/api/0.1/";
	
	/** XML parser factory. */
	private static XmlPullParserFactory factory = null;
	
	private Database() {
	}
	
	/**
	 * Execute an OSB HTTP request.
	 * @param req The specific OSB request.
	 * @param params Parameters to the OSB request.
	 * @return The stream of the OSB server response.
	 * @throws IOException 
	 */
	private static InputStream execute(final String req, final String params) throws IOException {
		URLConnection conn = new URL("http", HOST, 80, API + req + "?" + params).openConnection();
		conn.setRequestProperty("User-Agent", Application.userAgent);
		return conn.getInputStream();
	}
	
	/**
	 * Perform an HTTP request to download up to 100 bugs inside the specified area.
	 * Blocks until the request is complete.
	 * @param area Latitude/longitude *1E7 of area to download.
	 * @return All the bugs in the given area.
	 */
	public static Collection<Bug> get(Rect area) {
		Collection<Bug> result = new ArrayList<Bug>();
		// http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
		try {
			InputStream is = execute("getGPX",
					"limit=100" +
					"&l=" + (double)area.left / 1E7d +
					"&t=" + (double)area.top / 1E7d +
					"&r=" + (double)area.right / 1E7d +
					"&b=" + (double)area.bottom / 1E7d);
			if (factory == null) {
				factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
			}
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
			int eventType;
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				String tagName = parser.getName();
				if (eventType == XmlPullParser.START_TAG && "wpt".equals(tagName)) {
					try {
						result.add(new Bug(parser));
					} catch (IOException e) {
						// if the bug doesn't parse correctly, there's nothing
						// we can do about it - move on
						Log.e("Vespucci", "Problem parsing bug", e);
					} catch (XmlPullParserException e) {
						// if the bug doesn't parse correctly, there's nothing
						// we can do about it - move on
						Log.e("Vespucci", "Problem parsing bug", e);
					} catch (NumberFormatException e) {
						// if the bug doesn't parse correctly, there's nothing
						// we can do about it - move on
						Log.e("Vespucci", "Problem parsing bug", e);
					}
				}
			}
		} catch (XmlPullParserException e) {
			Log.e("Vespucci", "Database.get:Exception", e);
		} catch (IOException e) {
			Log.e("Vespucci", "Database.get:Exception", e);
		}
		return result;
	}
	
	/**
	 * Perform an HTTP request to add the specified bug to the OpenStreetBugs database.
	 * Blocks until the request is complete.
	 * @param bug The bug to add.
	 * @param comment The first comment for the bug.
	 * @return true if the bug was successfully added.
	 */
	public static boolean add(Bug bug, BugComment comment) {
		if (bug.getId() == 0 && bug.comments.size() == 0) {
			// http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?lat=<Latitude>&lon=<Longitude>&text=<Bug description with author and date>&format=<Output format>
			try {
				InputStream is = execute("addPOIexec",
						"lat=" + ((double)bug.getLat() / 1E7d) +
						"&lon=" + ((double)bug.getLon() / 1E7d) +
						"&text=" + URLEncoder.encode(comment.toString(), "UTF-8"));
				if (is != null) {
					BufferedReader r = new BufferedReader(new InputStreamReader(is));
					if ("ok".equals(r.readLine())) {
						String l = r.readLine();
						if (l != null) {
							bug.id = Long.parseLong(l);
							bug.comments.add(comment);
							return true;
						}
					}
				}
			} catch (NumberFormatException e) {
				Log.e("Vespucci", "Database.add:Exception", e);
			} catch (IOException e) {
				Log.e("Vespucci", "Database.add:Exception", e);
			}
		}
		return false;
	}
	
	/**
	 * Perform an HTTP request to add the specified comment to the specified bug.
	 * Blocks until the request is complete.
	 * @param bug The bug to add the comment to.
	 * @param comment The comment to add to the bug.
	 * @return true if the comment was successfully added.
	 */
	public static boolean edit(Bug bug, BugComment comment) {
		if (bug.getId() != 0) {
			// http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?id=<Bug ID>&text=<Comment with author and date>
			try {
				InputStream is = execute("editPOIexec",
						"id=" + Long.toString(bug.getId()) +
						"&text=" + URLEncoder.encode(comment.toString(), "UTF-8"));
				if (is != null) {
					BufferedReader r = new BufferedReader(new InputStreamReader(is));
					if ("comment added".equals(r.readLine())) {
						bug.comments.add(comment);
						return true;
					}
				}
			} catch (IOException e) {
				Log.e("Vespucci", "Database.edit:Exception", e);
			}
		}
		return false;
	}
	
	/**
	 * Perform an HTTP request to close the specified bug.
	 * Blocks until the request is complete.
	 * @param bug The bug to close.
	 * @return true if the bug was successfully closed.
	 */
	public static boolean close(Bug bug) {
		if (bug.getId() != 0) {
			// http://openstreetbugs.schokokeks.org/api/0.1/closePOIexec?id=<Bug ID>
			try {
				InputStream is = execute("closePOIexec",
						"id=" + Long.toString(bug.getId()));
				if (is != null) {
					BufferedReader r = new BufferedReader(new InputStreamReader(is));
					if ("ok".equals(r.readLine())) {
						bug.closed = true;
						return true;
					}
				}
			} catch (IOException e) {
				Log.e("Vespucci", "Database.close:Exception", e);
			}
		}
		return false;
	}

}
