package io.vespucci.tasks;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.exception.IllegalOperationException;
import io.vespucci.osm.JosmXmlSerializable;
import io.vespucci.util.DateFormatter;

/**
 * An individual comment associated with an OpenStreetBug.
 * 
 * @author Andrew Gregory
 */
public class NoteComment implements Serializable, JosmXmlSerializable {

    private static final String DEBUG_TAG = NoteComment.class.getSimpleName().substring(0, Math.min(23, NoteComment.class.getSimpleName().length()));

    /**
     * 
     */
    private static final long serialVersionUID = 4L;

    static final String COMMENT_TAG      = "comment";
    static final String ACTION_ATTR      = "action";
    static final String ACTION_COMMENTED = "commented";
    static final String ACTION_REOPENED  = "reopened";
    static final String ACTION_OPENED    = "opened";
    static final String ACTION_CLOSED    = "closed";
    static final String IS_NEW_ATTR      = "is_new";
    static final String USER_ATTR        = "user";
    static final String UID_ATTR         = "uid";
    static final String TIMESTAMP_ATTR   = "timestamp";

    /** The Note we belong to **/
    private final Note note;
    /** The comment text. */
    private String     text;
    /** The nickname associated with the comment. */
    private String     nickname;
    /** The uid associated with the comment. */
    private int        uid;
    /** The action associated with the comment. */
    private String     action;
    /** The timestamp associated with the comment. */
    private long       timestamp = -1;

    /**
     * Create a new comment based on a string in the following format: "Long text comment here [NickName here,
     * YYYY-MM-DD HH:MM:SS ZZZ]" Unrecognizable dates will be replaced with the current system date/time.
     * 
     * @param note The note the comment should be attached to
     * @param description A description obtained from the OSB database.
     */
    public NoteComment(@NonNull Note note, String description) {
        this.note = note;
        text = description;
        timestamp = new Date().getTime();
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
    public NoteComment(@NonNull Note note, @Nullable String text, @Nullable String nickname, int uid, @Nullable String action, @Nullable Date timestamp) {
        this.note = note;
        this.text = text != null ? text.replace("[", "") : null;
        this.nickname = nickname != null ? nickname.replace(",", "") : null;
        this.uid = uid;
        this.action = action;
        this.timestamp = timestamp != null ? timestamp.getTime() : null;
    }

    /**
     * Get the comment text.
     * 
     * @return Comment text.
     */
    @Nullable
    public String getText() {
        return text;
    }

    /**
     * Set the comment text, this is only permissible for new comments
     * 
     * @param text the comment text
     */
    public void setText(@Nullable String text) {
        if (!isNew()) {
            throw new IllegalOperationException("Attempt to set text for an existing note");
        }
        this.text = text;
    }

    /**
     * Get the nickname of the bug submitter.
     * 
     * @return The nickname.
     */
    @Nullable
    public String getNickname() {
        return nickname;
    }

    /**
     * Get the bug timestamp.
     * 
     * @return The bug timestamp.
     */
    @NonNull
    public Date getTimestamp() {
        return new Date(timestamp);
    }

    /**
     * @return the action
     */
    @Nullable
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
            String date = (timestamp == -1) ? "" : ", " + DateFormatter.getUtcFormat("yyyy-MM-dd HH:mm:ss z").format(new Date(timestamp));
            return text + " [" + action + " " + nickname + date + "]";
        }
        return text;
    }

    /**
     * Check if this is a new comment
     * 
     * @return true if new
     */
    public boolean isNew() {
        return action == null;
    }

    @Override
    public void toJosmXml(XmlSerializer s) throws IllegalArgumentException, IllegalStateException, IOException {
        s.startTag("", COMMENT_TAG);
        if (action != null) {
            s.attribute("", ACTION_ATTR, action);
        } else {
            if (note.getOriginalState() != note.getState()) {
                switch (note.getState()) {
                case CLOSED:
                    s.attribute("", ACTION_ATTR, ACTION_CLOSED);
                    break;
                case OPEN:
                    if (note.isNew()) {
                        s.attribute("", ACTION_ATTR, ACTION_OPENED);
                    } else {
                        s.attribute("", ACTION_ATTR, ACTION_REOPENED);
                    }
                    break;
                default:
                    Log.d(DEBUG_TAG, "Illegal state for Note " + note.getState());
                    break;
                }
            } else {
                s.attribute("", ACTION_ATTR, ACTION_COMMENTED);
            }
        }
        if (timestamp != -1) {
            s.attribute("", TIMESTAMP_ATTR, note.toJOSMDate(new Date(timestamp)));
        }
        if (nickname != null) {
            s.attribute("", UID_ATTR, Integer.toString(uid));
            s.attribute("", USER_ATTR, nickname);
        }
        s.attribute("", IS_NEW_ATTR, Boolean.toString(isNew()));
        s.text(text);
        s.endTag("", COMMENT_TAG);
    }
}
