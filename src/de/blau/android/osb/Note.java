package de.blau.android.osb;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.Html;
import de.blau.android.Application;
import de.blau.android.util.DateFormatter;


/**
 * A bug in the OpenStreetBugs database, or a prospective new bug.
 * @author Andrew Gregory
 */
public class Note extends Bug implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3L;

	/**
	 * Date pattern used to parse the 'date' attribute of a 'note' from XML.
	 */
	private static final String DATE_PATTERN_NOTE_CREATED_AT = "yyyy-MM-dd HH:mm:ss z";

	/** Bug comments. */
	public List<NoteComment> comments = null;
	private State originalState; // track what we original had
	
	/**
	 * Create a Bug from an OSB GPX XML wpt element.
	 * @param parser Parser up to a wpt element.
	 * @throws IOException If there was a problem parsing the XML.
	 * @throws XmlPullParserException If there was a problem parsing the XML.
	 * @throws NumberFormatException If there was a problem parsing the XML.
	 */
	public Note(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
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
						if (parser.getText().trim().equalsIgnoreCase("closed")) {
							close();
							originalState = State.CLOSED;
						} else { 
							open();
							originalState = State.OPEN;
						}
					}
					if ("comments".equals(tagName)) {
						comments = new ArrayList<NoteComment>();
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
					comments.add(new NoteComment(text, nickname, action, timestamp));
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
						String trimmedDate = parser.getText().trim();
						try {
							timestamp = DateFormatter.getDate(
									DATE_PATTERN_NOTE_CREATED_AT, trimmedDate);
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
	public Note(int lat, int lon) {
		id = Application.getBugStorage().getNextId();
		this.lat = lat;
		this.lon = lon;
		open();
		comments = new ArrayList<NoteComment>();
	}
		
	/**
	 * Get the complete bug comment suitable for use with the OSB database.
	 * @return All the comments concatenated (joined with &lt;hr /&gt;).
	 */
	public String getComment() {
		StringBuilder result = new StringBuilder();
		for (NoteComment comment : comments) {
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
	@Override
	public String getDescription() {
		return "note "+ (comments != null && comments.size() > 0 ? Html.fromHtml(comments.get(0).getText()) : "<new>"); //TODO externalize string
	}
	
	/**
	 * Get the timestamp of the most recent change.
	 * @return The timestamp of the most recent change.
	 */
	@Override
	public Date getLastUpdate() {
		Date result = null;
		for (NoteComment c : comments) {
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

	public void addComment(String comment) {
		if (comment != null && comment.length() > 0) {
			comments.add(new NoteComment(comment));
		}
	}
	
	/**
	 * Return the number of comments
	 * @return
	 */
	public int count() {
		return comments == null ? 0 : comments.size();
	}

	public NoteComment getLastComment() {
		if (comments != null && comments.size() > 0) {
			return comments.get(comments.size()-1);
		}
		return null;
	}
	
	public State getOriginalState() {
		return originalState;
	}
	
	public String bugFilterKey() {
		return "NOTES";
	}
}
