package de.blau.android.tasks;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.acra.ACRA;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.Log;
import de.blau.android.ErrorCodes;
import de.blau.android.UploadResult;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.Server;
import de.blau.android.tasks.Task.State;

/**
 * Task to commit changes to an OpenStreetMap "Note".
 * Originally this code was intended for "Bugs" uploaded to the OpenStreetBugs database 
 * and the expression continues to linger on in various location.
 * @author Andrew Gregory
 *
 */
class CommitTask extends AsyncTask<Server, Void, UploadResult> {
	private static final String DEBUG_TAG = CommitTask.class.getSimpleName();
	/** Bug associated with the commit. */
	final Note bug;
	/** Comment associated with the commit. */
	private final String comment;
	/** Flag indicating if the bug should be closed. */
	private final boolean close;
	
	/**
	 * Create the background task to upload changes to OSM.
	 * @param bug The Note to commit changes to.
	 * @param comment An optional comment to add to the Note.
	 * @param close A close to indicate if the Note should be closed.
	 */
	public CommitTask(final Note bug, final String comment, final boolean close) {
		Log.d(DEBUG_TAG,bug.getDescription() + " >" + comment + "< " + close);
		this.bug = bug;
		this.comment = comment;
		this.close = close;
	}
	
	@Override
	protected UploadResult doInBackground(Server... servers) {
		Log.d(DEBUG_TAG,"doInBackGround");
		UploadResult result = new UploadResult();
		try {
			Server server = servers[0];
			if (!bug.isNew()) {
				if (bug.getOriginalState() == State.CLOSED && !close) { // reopen, do this before trying to add anything
					server.reopenNote(bug);
				}
			}

			if (bug.getOriginalState() != State.CLOSED) {
				Log.d(DEBUG_TAG, "CommitTask.doInBackground:Updating OSB");
				if (comment != null && comment.length() > 0) {
					// Make the comment
					NoteComment bc = new NoteComment(bug,comment);
					// Add or edit the bug as appropriate
					if (bug.isNew()) {
						server.addNote(bug, bc);
					} else { 
						server.addComment(bug, bc);
					}
				}
				// Close  the bug if requested, but only if there haven't been any problems
				if (close) {
					server.closeNote(bug);
				}
			}
		} catch (final OsmServerException e) {
			result.httpError = e.getErrorCode();
			result.message = e.getMessage();
			switch (e.getErrorCode()) {
			case HttpURLConnection.HTTP_FORBIDDEN:
				result.error = ErrorCodes.FORBIDDEN;
				break;
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				result.error = ErrorCodes.INVALID_LOGIN;
				break;
			case HttpURLConnection.HTTP_BAD_REQUEST:
			case HttpURLConnection.HTTP_NOT_FOUND:
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
			case HttpURLConnection.HTTP_BAD_GATEWAY:
			case HttpURLConnection.HTTP_UNAVAILABLE:
				result.error = ErrorCodes.UPLOAD_PROBLEM;
				break;
				//TODO: implement other state handling
			default:
				Log.e(DEBUG_TAG, "", e);
				ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
				ACRA.getErrorReporter().handleException(e);
				result.error = ErrorCodes.UPLOAD_PROBLEM; // use this as generic error
				break;
			}
		} catch (XmlPullParserException e) {
			result.error = ErrorCodes.INVALID_DATA_RECEIVED;
		} catch (IOException e) {
			result.error = ErrorCodes.NO_CONNECTION;
		}
		return result;
	}
}
