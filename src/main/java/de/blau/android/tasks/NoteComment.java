package de.blau.android.tasks;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.xmlpull.v1.XmlSerializer;

import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.osm.JosmXmlSerializable;

/**
 * An individual comment associated with an OpenStreetBug.
 * 
 * @author Andrew Gregory
 */
public class NoteComment implements Serializable, JosmXmlSerializable {

    private static final String DEBUG_TAG = NoteComment.class.getName();

    /**
     * 
     */
    private static final long serialVersionUID = 3L;

    /** The preferred OSB date formats. */
    private static final DateFormat bugDateFormats[] = { new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US), // preferred,
                                                                                                                   // used
                                                                                                                   // for
                                                                                                                   // output
                                                                                                                   // (see
                                                                                                                   // toString())
            new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.US), // alternate preferred
            new SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.US), // German
            new SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.US) // European
    };

    /** The Note we belong to **/
    private Note   note;
    /** The comment text. */
    private String text;
    /** The nickname associated with the comment. */
    private String nickname;
    /** The uid associated with the comment. */
    private int    uid;
    /** The action associated with the comment. */
    private String action;
    /** The timestamp associated with the comment. */
    private Date   timestamp;

    /**
     * Create a new comment based on a string in the following format: "Long text comment here [NickName here,
     * YYYY-MM-DD HH:MM:SS ZZZ]" Unrecognizable dates will be replaced with the current system date/time.
     * 
     * @param note The note the comment should be attached to
     * @param description A description obtained from the OSB database.
     */
    public NoteComment(Note note, String description) {
        this.note = note;
        text = description;
        timestamp = new Date();
    }

    /**
     * Create a new comment based on the individual components.
     * 
     * @param note The note the comment should be attached to
     * @param text New comment text. Left square brackets are stripped.
     * @param nickname New nickname. Commas are stripped.
     * @param uid OSM user id if any
     * @param action action associated with the comment
     * @param timestamp New timestamp.
     */
    public NoteComment(@NonNull Note note, String text, String nickname, int uid, String action, Date timestamp) {
        this.note = note;
        this.text = text.replaceAll("\\[", "");
        this.nickname = nickname.replaceAll(",", "");
        this.uid = uid;
        this.action = action;
        this.timestamp = timestamp;
    }

    /**
     * Get the comment text.
     * 
     * @return Comment text.
     */
    public String getText() {
        return text;
    }

    /**
     * Get the nickname of the bug submitter.
     * 
     * @return The nickname.
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Get the bug timestamp.
     * 
     * @return The bug timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Convert the bug to a string.
     * 
     * @return The bug comment in the preferred OSB format.
     */
    @Override
    public String toString() {
        if (nickname != null || action != null) {
            String date = (timestamp == null) ? "" : ", " + bugDateFormats[0].format(timestamp);
            return text + " [" + action + " " + nickname + date + "]";
        }
        return text;
    }

    public boolean isNew() {
        return action == null;
    }

    @Override
    public void toJosmXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", "comment");
        if (action != null) {
            s.attribute("", "action", action);
        } else {
            if (note.getOriginalState() != note.getState()) {
                switch (note.getState()) {
                case CLOSED:
                    s.attribute("", "action", "closed");
                    break;
                case OPEN:
                    if (note.isNew()) {
                        s.attribute("", "action", "opened");
                    } else {
                        s.attribute("", "action", "reopened");
                    }
                    break;
                default:
                    Log.d(DEBUG_TAG, "Illegal state for Note " + note.getState());
                    break;
                }
            } else {
                s.attribute("", "action", "commented");
            }
        }
        if (timestamp != null) {
            s.attribute("", "timestamp", note.toJOSMDate(timestamp));
        }
        if (nickname != null) {
            s.attribute("", "uid", Integer.toString(uid));
            s.attribute("", "user", nickname);
        }
        s.attribute("", "is_new", Boolean.toString(isNew()));
        s.text(text);
        s.endTag("", "comment");
    }
}
