package de.blau.android.tasks;

import android.os.AsyncTask;
import android.util.Log;
import de.blau.android.osm.Server;
import de.blau.android.tasks.Task.State;

/**
 * Task to commit changes to an OpenStreetBug.
 * @author Andrew Gregory
 *
 */
public class CommitTask extends AsyncTask<Server, Void, Boolean> {
	private static final String DEBUG_TAG = CommitTask.class.getSimpleName();
	/** Bug associated with the commit. */
	protected final Note bug;
	/** Comment associated with the commit. */
	protected final String comment;
	/** Flag indicating if the bug should be closed. */
	protected final boolean close;
	
	/**
	 * Create the background task to upload changes to OSB.
	 * @param bug The bug to commit changes to.
	 * @param comment An optional comment to add to the bug.
	 * @param close A close to indicate if the bug should be closed.
	 */
	public CommitTask(final Note bug, final String comment, final boolean close) {
		Log.d(DEBUG_TAG,bug.getDescription() + " >" + comment + "< " + close);
		this.bug = bug;
		this.comment = comment;
		this.close = close;
	}
	
	/**
	 * Commit bug changes to the OSB database.
	 * @param nickname An array of Strings, but only the first item is used. That item
	 * is the user nickname to associate with bug comments. If not specified, "NoName"
	 * will be used.
	 */
	@Override
	protected Boolean doInBackground(Server... servers) {
		Log.d(DEBUG_TAG,"doInBackGround");
		boolean result = true;
		Server server = servers[0];
		if (!bug.isNew()) {
			if (bug.getOriginalState() == State.CLOSED && !close) { // reopen, do this before trying to add anything
				result = server.reopenNote(bug);
			}
		}

		if (bug.getOriginalState() != State.CLOSED) {
			Log.d(DEBUG_TAG, "CommitTask.doInBackground:Updating OSB");
			if (comment != null && comment.length() > 0) {
				// Make the comment
				NoteComment bc = new NoteComment(comment);
				// Add or edit the bug as appropriate
				result = bug.isNew() ? server.addNote(bug, bc) : server.addComment(bug, bc);
				Log.d(DEBUG_TAG, result ? "sucessful":"failed");
			}
			// Close  the bug if requested, but only if there haven't been any problems
			if (result && close) {
				result = server.closeNote(bug);
			}
		}
		return result;
	}
	
}
