package de.blau.android.osb;

import java.util.Date;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Task to commit changes to an OpenStreetBug.
 * @author Andrew Gregory
 *
 */
public class CommitTask extends AsyncTask<String, Void, Boolean> {
	
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
	protected Boolean doInBackground(String... nickname) {
		boolean result = true;
		if (!bug.isClosed()) {
			Log.d("Vespucci", "CommitTask.doInBackground:Updating OSB");
			if (comment != null && comment.length() > 0) {
				// Fall back to "NoName" if nickname isn't set
				String nn = (nickname == null || nickname.length == 0 ||
								nickname[0] == null || nickname[0].length() == 0)
						? "NoName" : nickname[0];
				// Make the comment
				BugComment bc = new BugComment(comment, nn, new Date());
				// Add or edit the bug as appropriate
				result = (bug.getId() == 0) ? Database.add(bug, bc) : Database.edit(bug, bc);
			}
			// Close the bug if requested, but only if there haven't been any problems
			if (result && close) {
				result = Database.close(bug);
			}
		}
		return result;
	}
	
}
