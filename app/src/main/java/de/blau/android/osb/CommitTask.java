package de.blau.android.osb;

import android.os.AsyncTask;
import android.util.Log;
import de.blau.android.osm.Server;

/**
 * Task to commit changes to an OpenStreetBug.
 * @author Andrew Gregory
 *
 */
public class CommitTask extends AsyncTask<Server, Void, Boolean> {
	
	/** Bug associated with the commit. */
	protected final Bug bug;
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
	public CommitTask(final Bug bug, final String comment, final boolean close) {
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
		boolean result = true;
		Server server = servers[0];
		if (bug.isClosed() && !close) { // reopen, do this before trying to add anything
			result = server.reopenNote(bug);
			if (result) {
				bug.reopen();
			}
		}
		if (!bug.isClosed()) {
			Log.d("Vespucci", "CommitTask.doInBackground:Updating OSB");
			if (comment != null && comment.length() > 0) {
				// Make the comment
				BugComment bc = new BugComment(comment);
				// Add or edit the bug as appropriate
				result = (bug.getId() == 0) ? server.addNote(bug, bc) : server.addComment(bug, bc);
			}
			// Close  the bug if requested, but only if there haven't been any problems
			if (result && close) {
					result = server.closeNote(bug);
			}
		}
		return result;
	}
	
}
