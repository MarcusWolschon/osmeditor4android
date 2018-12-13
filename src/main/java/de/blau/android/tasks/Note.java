package de.blau.android.tasks;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.JosmXmlSerializable;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.Util;

/**
 * A bug in the OpenStreetBugs database, or a prospective new bug. This now works with the OSM Notes system, many
 * references to bugs remain for hysterical reasons :-).
 * 
 * @author Andrew Gregory
 * @author Simon
 */
public class Note extends Task implements Serializable, JosmXmlSerializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4L;

    /**
     * Date pattern used to parse the 'date' attribute of a 'note' from XML.
     */
    private static final String DATE_PATTERN_NOTE_CREATED_AT = "yyyy-MM-dd HH:mm:ss z";

    /**
     * This the standard data/time format used in .osn files and elsewhere in the API, and yes it is different than the
     * above
     */
    private final SimpleDateFormat JOSM_DATE = DateFormatter.getUtcFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /** created and closed dates **/
    private Date created = null;
    private Date closed  = null;

    /** Bug comments. */
    private ArrayList<NoteComment> comments = null;
    private State                  originalState;  // track what we original had

    /**
     * Create a Bug from an OSB GPX XML wpt element.
     * 
     * @param parser Parser up to a wpt element.
     * @throws IOException If there was a problem parsing the XML.
     * @throws XmlPullParserException If there was a problem parsing the XML.
     * @throws NumberFormatException If there was a problem parsing the XML.
     */
    public Note(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
        // note tag has already been read ... very ugly should refactor
        lat = (int) (Double.parseDouble(parser.getAttributeValue(null, "lat")) * 1E7d);
        lon = (int) (Double.parseDouble(parser.getAttributeValue(null, "lon")) * 1E7d);
        parseNote(parser);
    }

    /**
     * Parse a Note from XML
     * 
     * @param parser the parser instance
     * @throws XmlPullParserException if parsing fails
     * @throws IOException for XML reading issues
     * @throws NumberFormatException if a number conversion fails
     */
    public void parseNote(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {

        int eventType;

        final int START = 0;
        final int COMMENTS = 1;
        final int COMMENT = 2;
        int state = START;

        String text = "No Text";
        String nickname = "No Name";
        int uid = -1;
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
                        lat = (int) (Double.parseDouble(parser.getAttributeValue(null, "lat")) * 1E7d);
                        lon = (int) (Double.parseDouble(parser.getAttributeValue(null, "lon")) * 1E7d);
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
                    if ("date_created".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        String trimmedDate = parser.getText().trim();
                        try {
                            created = DateFormatter.getDate(DATE_PATTERN_NOTE_CREATED_AT, trimmedDate);
                        } catch (java.text.ParseException pex) {
                            created = new Date();
                        }
                    }
                    if ("date_closed".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        String trimmedDate = parser.getText().trim();
                        try {
                            closed = DateFormatter.getDate(DATE_PATTERN_NOTE_CREATED_AT, trimmedDate);
                        } catch (java.text.ParseException pex) {
                            closed = new Date();
                        }
                    }
                    if ("comments".equals(tagName)) {
                        comments = new ArrayList<>();
                        state = COMMENTS;
                    }
                }
            } else if (state == COMMENTS) {
                if ((eventType == XmlPullParser.END_TAG) && "comments".equals(tagName)) {
                    state = START;
                } else if ((eventType == XmlPullParser.START_TAG) && "comment".equals(tagName)) {
                    state = COMMENT;
                    text = "No Text";
                    nickname = "No Name";
                    uid = -1;
                    action = "Unknown action";
                    timestamp = null;
                }
            } else if (state == COMMENT) {
                if ((eventType == XmlPullParser.END_TAG) && "comment".equals(tagName)) {
                    comments.add(new NoteComment(this, text, nickname, uid, action, timestamp));
                    state = COMMENTS;
                } else if (eventType == XmlPullParser.START_TAG) {
                    if ("user".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        nickname = parser.getText().trim();
                    }
                    if ("uid".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        uid = Integer.parseInt(parser.getText().trim());
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
                            timestamp = DateFormatter.getDate(DATE_PATTERN_NOTE_CREATED_AT, trimmedDate);
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
     * 
     * @param lat Latitude *1E7.
     * @param lon Longitude *1E7.
     */
    public Note(int lat, int lon) {
        id = App.getTaskStorage().getNextId();
        this.created = new Date();
        this.lat = lat;
        this.lon = lon;
        open();
        comments = new ArrayList<>();
    }

    /**
     * Get the complete bug comment suitable for use with the OSB database.
     * 
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

    @Override
    public String getDescription() {
        return "note " + (comments != null && !comments.isEmpty() ? Util.fromHtml(comments.get(0).getText()) : "<new>");
    }

    @Override
    public String getDescription(@NonNull Context context) {
        String[] states = context.getResources().getStringArray(R.array.bug_state);
        return context.getString(R.string.note_description, comments != null && !comments.isEmpty() ? Util.fromHtml(comments.get(0).getText()) : "",
                states[getState().ordinal()]);
    }

    /**
     * Get the timestamp of the most recent change.
     * 
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

    /**
     * Add a comment to the Note
     * 
     * @param comment comment as a String
     */
    public void addComment(String comment) {
        if (comment != null && comment.length() > 0) {
            comments.add(new NoteComment(this, comment));
        }
    }

    /**
     * Return the number of comments
     * 
     * @return the number of comments attached to this note
     */
    public int count() {
        return comments == null ? 0 : comments.size();
    }

    /**
     * Get the last comment
     * 
     * @return the last NoteComment or null
     */
    @Nullable
    public NoteComment getLastComment() {
        if (comments != null && !comments.isEmpty()) {
            return comments.get(comments.size() - 1);
        }
        return null;
    }

    protected void move(int latE7, int lonE7) {
        if (isNew()) {
            lat = latE7;
            lon = lonE7;
        }
    }

    public State getOriginalState() {
        return originalState;
    }

    public String bugFilterKey() {
        return "NOTES";
    }

    @Override
    public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", "note");
        s.attribute("", "id", Long.toString(id));
        s.attribute("", "lat", Double.toString((lat / 1E7)));
        s.attribute("", "lon", Double.toString((lon / 1E7)));
        if (created != null) {
            s.attribute("", "created_at", toJOSMDate(created));
        }
        if (closed != null) {
            s.attribute("", "closed_at", toJOSMDate(closed));
        }
        if (count() > 0) {
            for (NoteComment c : comments) {
                c.toJosmXml(s);
            }
        }
        s.endTag("", "note");
    }

    String toJOSMDate(Date date) {
        String josmDate = JOSM_DATE.format(date);
        return josmDate.substring(0, josmDate.length() - 2); // strip last two digits
    }
}
