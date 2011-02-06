package de.blau.android.osb;

import java.util.Date;

import de.blau.android.R;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * Task to commit changes to an OpenStreetBug.
 * @author Andrew Gregory
 *
 */
public class CommitTask extends AsyncTask<String, Void, Boolean> {
	
	private final Activity caller;
	private final Bug bug;
	private final String comment;
	private final boolean close;
	
	/**
	 * Create the background task to upload changes to OSB.
	 * @param caller The main Vespucci activity.
	 * @param bug The bug to commit changes to.
	 * @param comment An optional comment to add to the bug.
	 * @param nickname An optional nickname to associate with the bug. If not specified, "NoName" will be used.
	 * @param close A close to indicate if the bug should be closed.
	 */
	public CommitTask(final Activity caller, final Bug bug, final String comment, final boolean close) {
		this.caller = caller;
		this.bug = bug;
		this.comment = comment;
		this.close = close;
	}
	
	@Override
	protected void onPreExecute() {
		caller.setProgressBarIndeterminateVisibility(true);
	}
	
	/**
	 * Commit bug changes to the OSB database.
	 * @param nickname An array of Strings, but only the first item is used. That item is the user nickname to associate with bug comments.
	 */
	@Override
	protected Boolean doInBackground(String... nickname) {
		boolean result = true;
		if (!bug.isClosed()) {
			Log.d("Vespucci", "Updating OSB");
			if (comment != null && comment.length() > 0) {
				// Fall back to "NoName" if nickname isn't set
				String nn = (nickname == null || nickname.length == 0 ||
								nickname[0] == null || nickname[0].length() == 0)
						? "NoName" : nickname[0];
				BugComment bc = new BugComment(comment, nn, new Date());
				result = (bug.getId() == 0) ? Database.add(bug, bc) : Database.edit(bug, bc);
			}
			if (result && close) {
				result = Database.close(bug);
			}
		}
		return result;
	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		caller.setProgressBarIndeterminateVisibility(false);
		Toast.makeText(caller.getApplicationContext(), result ? R.string.openstreetbug_commit_ok : R.string.openstreetbug_commit_fail, Toast.LENGTH_SHORT).show();
	}
	
}
