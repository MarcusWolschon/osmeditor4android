package de.blau.android.osb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A bug in the OpenStreetBugs database, or a prospective new bug.
 * @author Andrew Gregory
 */
public class Bug {
	
	/** Package accessible members - they are directly updated by the Database class. */
	
	/** OSB Bug ID. */
	long id;
	/** Latitude *1E7. */
	int lat;
	/** Longitude *1E7. */
	int lon;
	/** Bug state. */
	boolean closed;
	/** Bug comments. */
	List<BugComment> comments = new ArrayList<BugComment>();
	
	/**
	 * Create a bug for when the OSB site is down. Debugging only.
	 * @param lat
	 * @param lon
	 * @param closed
	 */
	public Bug(int lat, int lon, boolean closed) {
		// for debugging only
		id = 0;
		this.lat = lat;
		this.lon = lon;
		this.closed = closed;
		comments.add(new BugComment("debugging", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
		comments.add(new BugComment("test", "NoName", new Date()));
	}
	
	/**
	 * Create a Bug from an OSB GPX XML wpt element.
	 * @param parser Parser up to a wpt element.
	 * @throws IOException If there was a problem parsing the XML.
	 * @throws XmlPullParserException If there was a problem parsing the XML.
	 * @throws NumberFormatException If there was a problem parsing the XML.
	 */
	public Bug(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
		lat = (int)(Double.parseDouble(parser.getAttributeValue(null, "lat")) * 1E7d);
		lon = (int)(Double.parseDouble(parser.getAttributeValue(null, "lon")) * 1E7d);
		int eventType;
		while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
			String tagName = parser.getName();
			if (eventType == XmlPullParser.END_TAG) {
				if (tagName.equals("wpt")) {
					break;
				}
			}
			if (eventType == XmlPullParser.START_TAG) {
				if (tagName.equals("id") && parser.next() == XmlPullParser.TEXT) {
					id = Long.parseLong(parser.getText().trim());
				}
				if (tagName.equals("closed") && parser.next() == XmlPullParser.TEXT) {
					closed = Integer.parseInt(parser.getText().trim()) != 0;
				}
				if (tagName.equals("desc") && parser.next() == XmlPullParser.TEXT) {
					for (String c : parser.getText().trim().split("\\<hr \\/\\>")) {
						comments.add(new BugComment(c));
					}
				}
			}
		}
	}
	
	/**
	 * Create a new bug.
	 * @param lat Latitude *1E7.
	 * @param lon Longitude *1E7.
	 */
	public Bug(int lat, int lon) {
		id = 0;
		this.lat = lat;
		this.lon = lon;
		closed = false;
	}
	
	/**
	 * Get the bug ID.
	 * @return The bug ID.
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Get the latitude of the bug.
	 * @return The latitude *1E7.
	 */
	public int getLat() {
		return lat;
	}
	
	/**
	 * Get the longitude of the bug.
	 * @return The longitude *1E7.
	 */
	public int getLon() {
		return lon;
	}
	
	/**
	 * Get the bug open/closed state.
	 * @return true if the bug is closed, false if it's still open.
	 */
	public boolean isClosed() {
		return closed;
	}
	
	/**
	 * Get the complete bug comment suitable for use with the OSB database.
	 * @return All the comments concatenated (joined with &lt;hr /&gt;).
	 */
	public String getComment() {
		StringBuilder result = new StringBuilder();
		for (BugComment comment : comments) {
			if (result.length() > 0) {
				result.append("<hr />");
			}
			result.append(comment.toString());
		}
		return result.toString();
	}
	
	/**
	 * Get a string descriptive of the bug. This is intended to be used as a
	 * short bit of text representative of the bug.
	 * @return The first comment of the bug.
	 */
	public String getDescription() {
		return "bug "+ ((comments.size() > 0) ? comments.get(0).getText() : "<new>");
	}
	
	/**
	 * Get the timestamp of the most recent change.
	 * @return The timestamp of the most recent change.
	 */
	public Date getMostRecentChange() {
		Date result = null;
		for (BugComment c : comments) {
			Date t = c.getTimestamp();
			if (t != null && (result == null || t.after(result))) {
				result = t;
			}
		}
		if (result == null) {
			result = new Date();
		}
		return result;
	}
	
}
