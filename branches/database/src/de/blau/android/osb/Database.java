package de.blau.android.osb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.graphics.Rect;

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
	
	/** User agent string to use (optional). */
	private static String userAgent = null;
	
	/**
	 * Set the user agent string to use when accessing the OSB database.
	 * @param userAgent The new user agent string.
	 */
	public static void setUserAgent(final String userAgent) {
		Database.userAgent = userAgent;
	}
	
	private Database() {
	}
	
	/**
	 * Execute an OSB HTTP request.
	 * @param req The specific OSB request.
	 * @param params Parameters to the OSB request.
	 * @return The stream of the OSB server response.
	 * @throws URISyntaxException
	 * @throws IllegalStateException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private static InputStream execute(final String req, final String params) throws URISyntaxException, IllegalStateException, ClientProtocolException, IOException {
		HttpGet r = new HttpGet(new URI("http", null, HOST, 80, API + req, params, null));
		if (userAgent != null) r.setHeader("User-Agent", userAgent);
		return new DefaultHttpClient().execute(r).getEntity().getContent();
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
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document d = factory.newDocumentBuilder().parse(is);
			NodeList bugs = d.getElementsByTagName("wpt");
			for (int i = 0; i < bugs.getLength(); ++i) {
				result.add(new Bug((Element)bugs.item(i)));
			}
		} catch (Exception e) {
			// ignore
			e.printStackTrace();
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
						"&text=" + comment.toString());
				BufferedReader r = new BufferedReader(new InputStreamReader(is));
				if (r.readLine().equals("ok")) {
					bug.id = Long.parseLong(r.readLine());
					bug.comments.add(comment);
					return true;
				}
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
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
						"&text=" + comment.toString());
				BufferedReader r = new BufferedReader(new InputStreamReader(is));
				if (r.readLine().equals("comment added")) {
					bug.comments.add(comment);
					return true;
				}
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
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
				BufferedReader r = new BufferedReader(new InputStreamReader(is));
				if (r.readLine().equals("ok")) {
					bug.closed = true;
					return true;
				}
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}
		}
		return false;
	}

}
