package de.blau.android.tasks;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * An individual comment associated with an OpenStreetBug.
 * @author Andrew Gregory
 */
public class NoteComment implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The preferred OSB date formats. */
	private static final DateFormat bugDateFormats[] = {
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US), // preferred, used for output (see toString())
		new SimpleDateFormat("yy-MM-dd HH:mm:ss"    , Locale.US), // alternate preferred
		new SimpleDateFormat("dd.MM.yy HH:mm:ss"    , Locale.US), // German
		new SimpleDateFormat("dd/MM/yy HH:mm:ss"    , Locale.US)  // European
	};
	
	/** The comment text. */
	private String text;
	/** The nickname associated with the comment. */
	private String nickname;
	/** The action associated with the comment. */
	private String action;
	/** The timestamp associated with the comment. */
	private Date timestamp;
	
	/**
	 * Create a new comment based on a string in the following format:
	 * "Long text comment here [NickName here, YYYY-MM-DD HH:MM:SS ZZZ]"
	 * Unrecognisable dates will be replaced with the current system date/time.
	 * @param description A description obtained from the OSB database.
	 */
	public NoteComment(String description) {
		text = description;
	}
	
	/**
	 * Create a new comment based on the individual components.
	 * @param text New comment text. Left square brackets are stripped.
	 * @param nickname New nickname. Commas are stripped.
	 * @param timestamp New timestamp.
	 */
	public NoteComment(String text, String nickname, String action,  Date timestamp) {
		this.text = text.replaceAll("\\[", "");
		this.nickname = nickname.replaceAll(",", "");
		this.action = action;
		this.timestamp = timestamp;
	}
	
	/**
	 * Get the comment text.
	 * @return Comment text.
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Get the nickname of the bug submitter.
	 * @return The nickname.
	 */
	public String getNickname() {
		return nickname;
	}
	
	/**
	 * Get the bug timestamp.
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
		// TODO Auto-generated method stub
		return action == null;
	}

}
