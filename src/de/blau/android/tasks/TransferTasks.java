package de.blau.android.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.DialogFactory;
import de.blau.android.Logic;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.PostMergeHandler;
import de.blau.android.osm.Server;
import de.blau.android.osm.Track;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Util;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

public class TransferTasks {

	private static final String DEBUG_TAG = TransferTasks.class.getSimpleName();
			
	/** Maximum closed age to display: 7 days. */
	private static final long MAX_CLOSED_AGE = 7 * 24 * 60 * 60 * 1000;
	
	/** viewbox needs to be less wide than this for displaying bugs, just to avoid querying the whole world for bugs */ 
	private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 32;
	

	static public void downloadBox(final Context context, final Server server, final BoundingBox box, final boolean add, final PostAsyncActionHandler handler) {
		
		final TaskStorage bugs = Application.getTaskStorage();
		final Preferences prefs = new Preferences(context);
		
		if (!bugs.loaded()) {
			return;
		}
			
		try {
			box.makeValidForApi();
		} catch (OsmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // TODO remove this? and replace with better error messaging
		
		
		new AsyncTask<Void, Void, Collection<Task>>() {
			@Override
			protected Collection<Task> doInBackground(Void... params) {
				Log.d(DEBUG_TAG,"querying server for " + box);
				Set<String> bugFilter = prefs.taskFilter();
				Collection<Task> result = new ArrayList<Task>();
				Collection<Note> noteResult = null;
				if (bugFilter.contains("NOTES")) {
					noteResult = server.getNotesForBox(box,1000);
				}
				if (noteResult != null) {
					result.addAll(noteResult);
				}
				Collection<OsmoseBug> osmoseResult = null;
				if (bugFilter.contains("OSMOSE_ERROR") || bugFilter.contains("OSMOSE_WARNING") || bugFilter.contains("OSMOSE_MINOR_ISSUE")) {
					osmoseResult = OsmoseServer.getBugsForBox(box, 1000);
				}
				if (osmoseResult != null) {
					result.addAll(osmoseResult);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Collection<Task> result) {
				if (result == null) {
					Log.d(DEBUG_TAG,"no bugs found");
					return;	
				}
				if (!add) {
					Log.d(DEBUG_TAG,"resetting bug storage");
					bugs.reset();
				}
				bugs.add(box);
				long now = System.currentTimeMillis();
				for (Task b : result) {
					Log.d(DEBUG_TAG,"got bug " + b.getDescription() + " " + bugs.toString());
					// add open bugs or closed bugs younger than 7 days
					if (!bugs.contains(b) && (!b.isClosed() || (now - b.getLastUpdate().getTime()) < MAX_CLOSED_AGE)) {
						bugs.add(b);
						Log.d(DEBUG_TAG,"adding bug " + b.getDescription());
						if (!b.isClosed()) {
							IssueAlert.alert(context, b);
						}
					}
				}
				if (handler != null) {
					handler.execute();
				}
			}	
		}.execute();
	}	
	
	/**
	 * Upload Notes or bugs to server, needs to be called from main for now (mainly to OAuth dependency)
	 * @param context
	 * @param server
	 */
	static public void upload(final Context context, final Server server) {
		boolean uploadFailed = false ;
		if (server != null) {
			Util.setSupportProgressBarIndeterminateVisibility(Application.mainActivity,true);
			ArrayList<Task>queryResult = Application.getTaskStorage().getTasks();
			for (Task b:queryResult) {
				if (b.changed) {
					try {
						Thread.sleep(100); // attempt at workaround of Osmose issues
					} catch (InterruptedException e) {
					}
					if (b instanceof Note) {
						if (server.isLoginSet()) {
							if (server.needOAuthHandshake()) {
								Application.mainActivity.oAuthHandshake(server, new PostAsyncActionHandler() {
									@Override
									public void execute() {
										Preferences prefs = new Preferences(context);
										upload(context, prefs.getServer());
									}
								});
								if (server.getOAuth()) // if still set
									Toast.makeText(context, R.string.toast_oauth, Toast.LENGTH_LONG).show();
								return;
							} 
						} else {
							Application.mainActivity.showDialog(DialogFactory.NO_LOGIN_DATA);
							return;
						}
						Note n = (Note)b;
						NoteComment nc = n.getLastComment();
						if (nc != null && nc.isNew()) {
							uploadFailed = !uploadNote(context, server, n, nc.getText(), n.isClosed(), true) || uploadFailed;
						} else {
							uploadFailed = !uploadNote(context, server, n, null, n.isClosed(), true) || uploadFailed; // just a state change
						}
					} else if (b instanceof OsmoseBug) {
						uploadFailed =  uploadOsmoseBug((OsmoseBug)b) || uploadFailed;
					}
				}
			}
			Util.setSupportProgressBarIndeterminateVisibility(Application.mainActivity,false);
			Toast.makeText(context, !uploadFailed ? R.string.openstreetbug_commit_ok : R.string.openstreetbug_commit_fail, Toast.LENGTH_SHORT).show();
		}
	}
	
	static public boolean uploadOsmoseBug(final OsmoseBug b) {
		AsyncTask<Void, Void, Boolean> a = new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {			
				return OsmoseServer.changeState((OsmoseBug)b);
			}
		};
		a.execute();
		try {
			return a.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	
	/**
	 * Commit changes to the currently selected OpenStreetBug.
	 * @param comment Comment to add to the bug.
	 * @param close Flag to indicate if the bug is to be closed.
	 */
	static public boolean uploadNote(final Context context, final Server server, final Note note, final String comment, final boolean close, final boolean quiet) {
		Log.d(DEBUG_TAG, "uploadNote");

		if (server != null) {
			if (server.isLoginSet()) {
				if (server.needOAuthHandshake()) {
					Application.mainActivity.oAuthHandshake(server, new PostAsyncActionHandler() {
						@Override
						public void execute() {
							Preferences prefs = new Preferences(context);
							uploadNote(context, prefs.getServer(), note, comment, close, quiet);
						}
					});
					if (server.getOAuth()) // if still set
						Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_oauth, Toast.LENGTH_LONG).show();
					return false;
				} 
			} else {
				Application.mainActivity.showDialog(DialogFactory.NO_LOGIN_DATA);
				return false;
			}

			CommitTask ct = new CommitTask(note, comment, close) {

				/** Flag to track if the bug is new. */
				private boolean newBug;

				@Override
				protected void onPreExecute() {
					newBug = bug.isNew();
					Util.setSupportProgressBarIndeterminateVisibility(Application.mainActivity, true);
				}

				@Override
				protected Boolean doInBackground(Server... args) {
					// execute() is called below with no arguments (args will be empty)
					// getDisplayName() is deferred to here in case a lengthy OSM query
					// is required to determine the nickname

					return super.doInBackground(server);
				}

				@Override
				protected void onPostExecute(Boolean result) {
					if (newBug && !Application.getTaskStorage().contains(bug)) {
						Application.getTaskStorage().add(bug);
					}
					if (result) {
						// upload sucessful
						bug.changed = false;
					}
					Util.setSupportProgressBarIndeterminateVisibility(Application.mainActivity, false);
					if (!quiet) {
						Toast.makeText(context, result ? R.string.openstreetbug_commit_ok : R.string.openstreetbug_commit_fail, Toast.LENGTH_SHORT).show();
					}
				}
			};

			ct.execute();
			try {
				return ct.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
}
