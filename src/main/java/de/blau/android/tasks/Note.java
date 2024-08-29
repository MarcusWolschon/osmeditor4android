package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.JosmXmlSerializable;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.Util;

/**
 * A bug in the OpenStreetBugs database, or a prospective new bug. This now works with the OSM Notes system, many
 * references to bugs remain for hysterical reasons :-).
 * 
 * The OSN/JOSM xml format is different than the XML encoding returned from the 0.6 OSM API
 * 
 * @author Andrew Gregory
 * @author Simon Poole
 */
public class Note extends LongIdTask implements Serializable, JosmXmlSerializable {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Note.class.getSimpleName().length());
    private static final String DEBUG_TAG = Note.class.getSimpleName().substring(0, TAG_LEN);

    private static final long serialVersionUID = 8L;

    public static final String  NOTE_ELEMENT     = "note";
    static final String         LON_KEY          = "lon";
    static final String         LAT_KEY          = "lat";
    static final String         ID_KEY           = "id";
    static final String         STATUS           = "status";
    static final String         STATUS_CLOSED    = "closed";
    private static final String DATE_CLOSED      = "date_closed";
    private static final String DATE_CREATED     = "date_created";
    /** JOSM/OSN version of the above */
    static final String         CREATED_AT       = "created_at";
    static final String         CLOSED_AT        = "closed_at";
    /** */
    private static final String COMMENTS_ELEMENT = "comments";
    public static final String  COMMENT_ELEMENT  = "comment";
    private static final String DATE             = "date";
    private static final String HTML             = "html";
    private static final String ACTION           = "action";
    private static final String UID              = "uid";
    private static final String USER             = "user";

    private static final String UNKNOWN_ACTION = "Unknown action";
    private static final String NO_NAME        = "No Name";
    private static final String NO_TEXT        = "No Text";

    protected static BitmapWithOffset cachedIconNoteClosed;
    protected static BitmapWithOffset cachedIconNoteChangedClosed;
    protected static BitmapWithOffset cachedIconNoteOpen;
    protected static BitmapWithOffset cachedIconNoteChanged;

    /**
     * Date pattern used to parse the 'date' attribute of a 'note' from XML.
     */
    private static final String DATE_PATTERN_NOTE_CREATED_AT = "yyyy-MM-dd HH:mm:ss z";

    /** created and closed dates **/
    private long created = -1;
    private long closed  = -1;

    /** Bug comments. */
    private List<NoteComment> comments = null;
    private State             originalState;  // track what we original had

    /**
     * Create a new Note
     * 
     * @param lat Latitude *1E7.
     * @param lon Longitude *1E7.
     */
    public Note(int lat, int lon) {
        id = App.getTaskStorage().getNextId();
        this.created = new Date().getTime();
        this.lat = lat;
        this.lon = lon;
        open();
        comments = new ArrayList<>();
    }

    /**
     * Create a Note from an API 0.6 XML element.
     * 
     * @param parser Parser .
     * @throws IOException If there was a problem parsing the XML.
     * @throws XmlPullParserException If there was a problem parsing the XML.
     * @throws NumberFormatException If there was a problem parsing the XML.
     */
    public Note(XmlPullParser parser) throws XmlPullParserException, IOException, NumberFormatException {
        parseNotes(parser, null);
    }

    /**
     * Setup the icon caches
     * 
     * @param context android Context
     * @param hwAccelerated true if the Canvas is hw accelerated
     */
    public static void setupIconCache(@NonNull Context context, boolean hwAccelerated) {
        cachedIconNoteOpen = getIcon(context, R.drawable.note_open, hwAccelerated);
        cachedIconNoteChanged = getIcon(context, R.drawable.note_changed, hwAccelerated);
        cachedIconNoteChangedClosed = getIcon(context, R.drawable.note_changed_closed, hwAccelerated);
        cachedIconNoteClosed = getIcon(context, R.drawable.note_closed, hwAccelerated);
    }

    /**
     * Set the id
     * 
     * This only makes sense when renumbering newly created Notes
     * 
     * @param id the id
     */
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return id <= 0;
    }

    /**
     * Get the date and time the Note was created
     * 
     * @return the number of milliseconds since the UNIX epoch
     */
    public long getCreatedAt() {
        return created;
    }

    /**
     * Set the date and time the Note was created
     * 
     * @param created the created to set
     */
    public void setCreatedAt(long created) {
        this.created = created;
    }

    /**
     * Get the date and time the Note was closed
     * 
     * @return the number of milliseconds since the UNIX epoch
     */
    public long getClosedAt() {
        return closed;
    }

    /**
     * Set the date and time the Note was closed
     * 
     * @param closed the number of milliseconds since the UNIX epoch
     */
    public void setClosedAt(long closed) {
        this.closed = closed;
    }

    /**
     * Parse a Note from XML
     * 
     * @param parser the parser instance
     * @param existingNote an existing Note if any
     * @return a List of Notes
     * @throws XmlPullParserException if parsing fails
     * @throws IOException for XML reading issues
     * @throws NumberFormatException if a number conversion fails
     */
    @NonNull
    public static List<Note> parseNotes(@NonNull XmlPullParser parser, @Nullable Note existingNote)
            throws XmlPullParserException, IOException, NumberFormatException {

        List<Note> result = new ArrayList<>();
        int eventType;

        final int START = 0;
        final int COMMENTS = 1;
        final int COMMENT = 2;
        int state = START;

        Note note = existingNote;

        String text = NO_TEXT;
        String nickname = NO_NAME;
        int uid = -1;
        String action = UNKNOWN_ACTION;
        Date timestamp = null;

        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (state == START) {
                if (eventType == XmlPullParser.END_TAG && NOTE_ELEMENT.equals(tagName)) {
                    result.add(note);
                    note = null;
                }
                if (eventType == XmlPullParser.START_TAG) {
                    if (NOTE_ELEMENT.equals(tagName)) {
                        int lat = (int) (Double.parseDouble(parser.getAttributeValue(null, LAT_KEY)) * 1E7d);
                        int lon = (int) (Double.parseDouble(parser.getAttributeValue(null, LON_KEY)) * 1E7d);
                        if (note == null) {
                            note = new Note(lat, lon);
                        } else {
                            note.lat = lat;
                            note.lon = lon;
                        }
                    }
                    if (ID_KEY.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        note.id = Long.parseLong(parser.getText().trim());
                    }
                    if (STATUS.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        if (STATUS_CLOSED.equalsIgnoreCase(parser.getText().trim())) {
                            note.close();
                            note.originalState = State.CLOSED;
                        } else {
                            note.open();
                            note.originalState = State.OPEN;
                        }
                    }
                    if (DATE_CREATED.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        String trimmedDate = parser.getText().trim();
                        try {
                            note.created = DateFormatter.getDate(DATE_PATTERN_NOTE_CREATED_AT, trimmedDate).getTime();
                        } catch (java.text.ParseException pex) {
                            note.created = new Date().getTime();
                        }
                    }
                    if (DATE_CLOSED.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        String trimmedDate = parser.getText().trim();
                        try {
                            note.setClosedAt(DateFormatter.getDate(DATE_PATTERN_NOTE_CREATED_AT, trimmedDate).getTime());
                        } catch (java.text.ParseException pex) {
                            note.setClosedAt(new Date().getTime());
                        }
                    }
                    if (COMMENTS_ELEMENT.equals(tagName)) {
                        note.comments = new ArrayList<>();
                        state = COMMENTS;
                    }
                }
            } else if (state == COMMENTS) {
                if ((eventType == XmlPullParser.END_TAG) && COMMENTS_ELEMENT.equals(tagName)) {
                    state = START;
                } else if ((eventType == XmlPullParser.START_TAG) && COMMENT_ELEMENT.equals(tagName)) {
                    state = COMMENT;
                    text = NO_TEXT;
                    nickname = NO_NAME;
                    uid = -1;
                    action = UNKNOWN_ACTION;
                    timestamp = null;
                }
            } else if (state == COMMENT) {
                if ((eventType == XmlPullParser.END_TAG) && COMMENT_ELEMENT.equals(tagName)) {
                    note.comments.add(new NoteComment(note, text, nickname, uid, action, timestamp));
                    state = COMMENTS;
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (USER.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        nickname = parser.getText().trim();
                    }
                    if (UID.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        uid = Integer.parseInt(parser.getText().trim());
                    }
                    if (ACTION.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        action = parser.getText().trim();
                    }
                    if (HTML.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        text = parser.getText().trim();
                    }
                    if (DATE.equals(tagName) && parser.next() == XmlPullParser.TEXT) {
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
        return result;
    }

    /**
     * Get the complete bug comment suitable for use with the OSB database.
     * 
     * @return All existing comments concatenated (joined with &lt;hr /&gt;).
     */
    @NonNull
    public String getComment() {
        StringBuilder result = new StringBuilder();
        for (NoteComment comment : comments) {
            if (!comment.isNew()) {
                if (result.length() > 0) {
                    result.append("<hr />");
                }
                result.append(comment.toString());
            }
        }
        return result.toString();
    }

    @Override
    public String getDescription() {
        return "note " + (comments != null && !comments.isEmpty() ? Util.fromHtml(comments.get(0).getText()) : "<new>");
    }

    @Override
    public String getDescription(@NonNull Context context) {
        String state = stateToString(context, R.array.bug_state, R.array.bug_state_values, getState());
        return context.getString(R.string.note_description, comments != null && !comments.isEmpty() ? Util.fromHtml(comments.get(0).getText()) : "", state);
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
            if (result == null || t.after(result)) {
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
    public void addComment(@Nullable String comment) {
        if (comment != null && comment.length() > 0) {
            comments.add(new NoteComment(this, comment));
        }
    }

    /**
     * Add a comment to the Note
     * 
     * @param comment a NoteComment instance
     */
    public void addComment(@NonNull NoteComment comment) {
        comments.add(comment);
    }

    /**
     * Remove the last comment
     * 
     * This will fail if the last comment isn't new/unsaved
     */
    public void removeLastComment() {
        if (comments != null && !comments.isEmpty()) {
            final int lastPos = comments.size() - 1;
            NoteComment last = comments.get(lastPos);
            if (last.isNew()) {
                comments.remove(lastPos);
                return;
            }
            Log.e(DEBUG_TAG, "Last comment isn't new");
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

    /**
     * Move the position of this Node
     * 
     * @param latE7 new latitude WGS84*1E7
     * @param lonE7 new longitude WGS84*1E7
     */
    protected void move(int latE7, int lonE7) {
        if (isNew()) {
            lat = latE7;
            lon = lonE7;
        }
    }

    /**
     * Get the original state of this Note
     * 
     * @return the original state
     */
    public State getOriginalState() {
        return originalState;
    }

    @Override
    public String bugFilterKey() {
        return "NOTES";
    }

    @Override
    public void toJosmXml(final XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", NOTE_ELEMENT);
        s.attribute("", ID_KEY, Long.toString(id));
        s.attribute("", LAT_KEY, Double.toString((lat / 1E7)));
        s.attribute("", LON_KEY, Double.toString((lon / 1E7)));
        if (created != -1) {
            s.attribute("", CREATED_AT, toJOSMDate(new Date(created)));
        }
        if (getClosedAt() != -1) {
            s.attribute("", CLOSED_AT, toJOSMDate(new Date(getClosedAt())));
        }
        if (count() > 0) {
            for (NoteComment c : comments) {
                c.toJosmXml(s);
            }
        }
        s.endTag("", NOTE_ELEMENT);
    }

    /**
     * Output a date like JOSM wants it
     * 
     * @param date the Date
     * @return a String in JOSM format
     */
    @NonNull
    String toJOSMDate(@NonNull Date date) {
        String josmDate = DateFormatter.JOSM_DATE_OUT.format(date);
        return josmDate.substring(0, josmDate.length() - 2); // strip last two digits
    }

    @Override
    public void drawBitmapOpen(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconNoteOpen, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapChanged(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconNoteChanged, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapChangedClosed(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconNoteChangedClosed, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapClosed(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconNoteClosed, c, x, y, selected, paint);
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Note)) {
            return false;
        }
        Note other = ((Note) obj);
        return id == other.id;
    }
}
