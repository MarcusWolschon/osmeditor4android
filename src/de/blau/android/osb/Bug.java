package de.blau.android.osb;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.Html;


/**
 * A bug in the OpenStreetBugs database, or a prospective new bug.
 * @author Andrew Gregory
 */
public class Bug implements Serializable {
	
	/** Package accessible members - they are directly updated by the Database class. */
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** OSB Bug ID. */
	long id;
	/** Latitude *1E7. */
	int lat;
	/** Longitude *1E7. */
	int lon;
	/** Bug state. */
	boolean closed;
	/** Bug comments. */
	public List<BugComment> comments = null;
	
	
	/**
	 * Create a Bug from an OSB GPX XML wpt element.
	 * @param parser Parser up to a wpt element.
	 * @throws IOException If there was a problem parsing the XML.
	 * @throws XmlPullParserException If there was a problem parsing the XML.
	 * @throws NumberFormatException If there was a problem parsing the XML.
	 */
	public Bug(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
		// note tag has already been read ... very ugly should refactor
		lat = (int)(Double.parseDouble(parser.getAttributeValue(null, "lat")) * 1E7d);
		lon = (int)(Double.parseDouble(parser.getAttributeValue(null, "lon")) * 1E7d);
		parseBug(parser);
	}
	
	public void parseBug(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {

		int eventType;
		
		final int START = 0;
		final int COMMENTS = 1;
		final int COMMENT = 2;
		int state = START;
		
		String text = "No Text";
		String nickname = "No Name"; 
		String action = "Unknown action";
		Date timestamp = null;
		
		while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
			String tagName = parser.getName();
			if (state == START) {
				if (eventType == XmlPullParser.END_TAG) {
					if ("note".equals(tagName)) {
						break;
					}
				}
				if (eventType == XmlPullParser.START_TAG) {
					if ("note".equals(tagName)) {
						lat = (int)(Double.parseDouble(parser.getAttributeValue(null, "lat")) * 1E7d);
						lon = (int)(Double.parseDouble(parser.getAttributeValue(null, "lon")) * 1E7d);
					}
					if ("id".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						id = Long.parseLong(parser.getText().trim());
					}
					if ("status".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						closed = parser.getText().trim().equalsIgnoreCase("closed");
					}
					if ("comments".equals(tagName)) {
						comments = new ArrayList<BugComment>();
						state = COMMENTS;
					}
				}
			}
			else if (state == COMMENTS) {
				if ((eventType == XmlPullParser.END_TAG) && "comments".equals(tagName)) {
					state = START;
				}
				else if ((eventType == XmlPullParser.START_TAG) && "comment".equals(tagName)) {
					state = COMMENT;
					text = "No Text";
					nickname = "No Name"; 
					action = "Unknown action";
					timestamp = null;
				}
			}
			else if (state == COMMENT) {
				if ((eventType == XmlPullParser.END_TAG) && "comment".equals(tagName)) {
					comments.add(new BugComment(text, nickname, action, timestamp));
					state = COMMENTS;
				}
				else if (eventType == XmlPullParser.START_TAG) {
					if ("user".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						nickname = parser.getText().trim();
					}
					if ("action".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						action = parser.getText().trim();
					}
					if ("html".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						text = parser.getText().trim();
					}
					if ("date".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
						try {
							timestamp = df.parse(parser.getText().trim());
						} catch (java.text.ParseException pex) {
							timestamp = new Date();
						}
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
		comments = new ArrayList<BugComment>();
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
	 * Close the bug
	 */
	public void close() {
		closed = true;
	}
	
	/**
	 * Close the bug
	 */
	public void reopen() {
		closed = false;
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
		return "note "+ (comments != null && comments.size() > 0 ? Html.fromHtml(comments.get(0).getText()) : "<new>"); //TODO externalize string
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
