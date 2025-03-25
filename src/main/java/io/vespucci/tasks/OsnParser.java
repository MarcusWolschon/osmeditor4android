package io.vespucci.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.exception.OsmParseException;
import io.vespucci.exception.StorageException;
import io.vespucci.util.DateFormatter;

/**
 * Parse Notes in JOSM format (not the API 0.6 format)
 * 
 * @author Simon Poole
 *
 */
public class OsnParser extends DefaultHandler {
    private static final String DEBUG_TAG = OsnParser.class.getSimpleName().substring(0, Math.min(23, OsnParser.class.getSimpleName().length()));

    static final String         OSM_NOTES   = "osm-notes";
    private static final String OSMAND_TEXT = "text";

    private final List<Exception> exceptions = new ArrayList<>();

    private final List<Note> notes       = new ArrayList<>();
    private Note             note        = null;
    private StringBuilder    commentText = new StringBuilder();
    private String           nickname;
    private int              uid;
    private String           action;
    private Date             timestamp;

    /**
     * Triggers the beginning of parsing.
     * 
     * @param in the InputStream
     * @throws SAXException {@see SAXException}
     * @throws IOException when the xmlRetriever could not provide any data.
     * @throws ParserConfigurationException if a parser feature is used that is not supported
     */
    public void start(@NonNull final InputStream in) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(in, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
        try {
            switch (name) {
            case OSM_NOTES:
                // ignored
                break;
            case Note.NOTE_ELEMENT:
                String lat = atts.getValue(Note.LAT_KEY);
                String lon = atts.getValue(Note.LON_KEY);
                if (lat == null || lon == null) {
                    throw new OsmParseException("lat " + lat + " lon " + lon);
                }
                note = new Note((int) (Double.parseDouble(lat) * 1E7d), (int) (Double.parseDouble(lon) * 1E7d));
                String id = atts.getValue(Note.ID_KEY);
                if (id == null) {
                    throw new OsmParseException("id attribute missing");
                }
                note.id = Long.parseLong(id);
                String createdAt = atts.getValue(Note.CREATED_AT);
                if (createdAt != null) {
                    note.setCreatedAt(DateFormatter.JOSM_DATE_IN.parse(createdAt).getTime());
                }
                String closedAt = atts.getValue(Note.CLOSED_AT);
                if (closedAt != null) {
                    note.setClosedAt(DateFormatter.JOSM_DATE_IN.parse(closedAt).getTime());
                }
                break;
            case NoteComment.COMMENT_TAG:
                if (note == null) {
                    throw new OsmParseException("Note comment without note");
                }
                commentText = new StringBuilder();
                String text = atts.getValue(OSMAND_TEXT);
                if (text != null) {
                    commentText.append(text);
                }
                nickname = atts.getValue(NoteComment.USER_ATTR);
                String uidString = atts.getValue(NoteComment.UID_ATTR);
                if (uidString != null) {
                    uid = Integer.parseInt(uidString);
                }
                action = atts.getValue(NoteComment.ACTION_ATTR);
                String timestampString = atts.getValue(NoteComment.TIMESTAMP_ATTR);
                timestamp = new Date(0L);
                if (timestampString != null) {
                    timestamp = DateFormatter.JOSM_DATE_IN.parse(timestampString);
                }
                break;
            default:
                throw new OsmParseException("Unknown element " + name);
            }
        } catch (ParseException | OsmParseException | NumberFormatException e) {
            logAndSaveException(e);
        }
    }

    /**
     * Log and save the exception
     * 
     * @param e the Exception
     */
    private void logAndSaveException(@NonNull Exception e) {
        Log.e(DEBUG_TAG, "Exception during parsing: ", e);
        exceptions.add(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            if (note == null) {
                throw new OsmParseException("Note comment text without note");
            }
            commentText.append(ch, start, length);
        } catch (OsmParseException e) {
            logAndSaveException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String name, final String qName) throws SAXException {
        try {
            switch (name) {
            case Note.NOTE_ELEMENT:
                if (note == null) {
                    throw new OsmParseException("End of note without note");
                }
                notes.add(note);
                break;
            case NoteComment.COMMENT_TAG:
                if (note == null) {
                    throw new OsmParseException("End of note comment without note");
                }
                NoteComment comment = new NoteComment(note, commentText.toString().trim(), nickname, uid, action, timestamp);
                note.addComment(comment);
                break;
            default:
                // ignore everything else
            }
        } catch (StorageException sex) {
            throw new SAXException(sex);
        } catch (OsmParseException e) {
            logAndSaveException(e);
        }
    }

    /**
     * Get any notes that were parsed
     * 
     * @return a List of Notes
     */
    @NonNull
    public List<Note> getNotes() {
        return notes;
    }

    /**
     * Get a list of exceptions
     * 
     * @return the exceptions
     */
    @NonNull
    public List<Exception> getExceptions() {
        return exceptions;
    }
}
