package de.blau.android.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import com.drew.lang.annotations.NotNull;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.UploadResult;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.ForbiddenLogin;
import de.blau.android.dialogs.InvalidLogin;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.FileUtil;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;

public class TransferTasks {

	private static final String DEBUG_TAG = TransferTasks.class.getSimpleName();
			
	/** Maximum closed age to display: 7 days. */
	private static final long MAX_CLOSED_AGE = 7 * 24 * 60 * 60 * 1000;
	
	/** viewbox needs to be less wide than this for displaying bugs, just to avoid querying the whole world for bugs */ 
	private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 32;
	
	/**
	 * Download tasks for a bounding box, actual requests will depend on what the current filter for tasks is set to
	 * 
	 * @param context Android context
	 * @param server current server configuration
	 * @param box the bounding box
	 * @param add if true merge teh download with existing task data
	 * @param handler handler to run after the download if not null
	 */
	static public void downloadBox(@NotNull final Context context, @NotNull final Server server, @NotNull final BoundingBox box, final boolean add, @Nullable final PostAsyncActionHandler handler) {
		
		final TaskStorage bugs = App.getTaskStorage();
		final Preferences prefs = new Preferences(context);
		
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
				Resources r = context.getResources();
				if (bugFilter.contains(r.getString(R.string.bugfilter_notes))) {
					noteResult = server.getNotesForBox(box,1000);
				}
				if (noteResult != null) {
					result.addAll(noteResult);
				}
				Collection<OsmoseBug> osmoseResult = null;
				if (bugFilter.contains(r.getString(R.string.bugfilter_osmose_error)) 
						|| bugFilter.contains(r.getString(R.string.bugfilter_osmose_warning)) 
						|| bugFilter.contains(r.getString(R.string.bugfilter_osmose_minor_issue))) {
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
					handler.onSuccess();
				}
			}	
		}.execute();
	}	
	
	/**
	 * Upload Notes or bugs to server, needs to be called from main for now (mainly for OAuth dependency)
	 * @param main instance of main calling this
	 * @param server current server config
	 */
	static public void upload(@NotNull final Main main, final Server server) {
		final String PROGRESS_TAG = "tasks";

		if (server != null) {
			final ArrayList<Task>queryResult = App.getTaskStorage().getTasks();
			// check if we need to oAuth first
			for (Task b:queryResult) {
				if (b.changed && b instanceof Note) {
					if (server.isLoginSet()) {
						if (server.needOAuthHandshake()) {
							main.oAuthHandshake(server, new PostAsyncActionHandler() {
								@Override
								public void onSuccess() {
									Preferences prefs = new Preferences(main);
									upload(main, prefs.getServer());
								}
								@Override
								public void onError() {
								}
							});
							if (server.getOAuth()) { // if still set
								Snack.barError(main, R.string.toast_oauth);
							}
							return;
						} 
					} else {
						ErrorAlert.showDialog(main,ErrorCodes.NO_LOGIN_DATA);
						return;
					}
				}
			}
			new AsyncTask<Void, Void, Boolean>() {
				@Override
				protected void onPreExecute() {
					Progress.showDialog(main, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
					Log.d(DEBUG_TAG,"starting up load of total " + queryResult.size() + " tasks");
				}

				@Override
				protected Boolean doInBackground(Void... params) {
					boolean uploadFailed = false;
					for (Task b:queryResult) {
						if (b.changed) {
							try {
								Thread.sleep(100); // attempt at workaround of Osmose issues
							} catch (InterruptedException e) {}
							Log.d(DEBUG_TAG, b.getDescription());
							if (b instanceof Note) {
								Note n = (Note)b;
								NoteComment nc = n.getLastComment();
								if (nc != null && nc.isNew()) {
									uploadFailed = !uploadNote(main, server, n, nc.getText(), n.isClosed(), true, null) || uploadFailed;
								} else {
									uploadFailed = !uploadNote(main, server, n, null, n.isClosed(), true, null) || uploadFailed; // just a state change
								}
							} else if (b instanceof OsmoseBug) {
								uploadFailed =  uploadOsmoseBug((OsmoseBug)b) || uploadFailed;
							}
						}
					}
					return uploadFailed;
				}
				
				@Override
				protected void onPostExecute(Boolean uploadFailed) {
					Progress.dismissDialog(main, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
					if (!uploadFailed) {
						Snack.barInfo(main, R.string.openstreetbug_commit_ok);
					} else {
						Snack.barError(main, R.string.openstreetbug_commit_fail);
					}
				}
			}.execute();
		}
	}
	
	/**
	 * Upload single bug state
	 * @param b osmose bug to upload
	 * @return true if successful
	 */
	@SuppressLint("InlinedApi")
	static public boolean uploadOsmoseBug(final OsmoseBug b) {
		Log.d(DEBUG_TAG, "uploadOsmoseBug");
		AsyncTask<Void, Void, Boolean> a = new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {			
				return OsmoseServer.changeState((OsmoseBug)b);
			}
		};
	    if(Build.VERSION.SDK_INT >= 11) {
	        a.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	    } else {
	        a.execute();
	    }
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
	 * Commit changes to a Note
	 * 
	 * @param activity activity that called this
	 * @param server Server configuration
	 * @param note the Note to upload
	 * @param comment Comment to add to the Note.
	 * @param close if true the Note is to be closed.
	 * @param quiet don't display an error message on errors
	 * @param postUploadHandler execute code after a successful upload
	 * @return true if upload was successful
	 */
	@TargetApi(11)
	static public boolean uploadNote(@NonNull final FragmentActivity activity, @NonNull final Server server, @NonNull final Note note, final String comment, 
			final boolean close, final boolean quiet,  @Nullable final PostAsyncActionHandler postUploadHandler) {
		Log.d(DEBUG_TAG, "uploadNote");

		if (server != null) {
			if (server.isLoginSet()) {
				if (server.needOAuthHandshake()) {
					if (activity instanceof Main) {
						((Main)activity).oAuthHandshake(server, new PostAsyncActionHandler() {
							@Override
							public void onSuccess() {
								Preferences prefs = new Preferences(activity);
								uploadNote(activity, prefs.getServer(), note, comment, close, quiet, postUploadHandler);
							}
							@Override
							public void onError() {
							}
						});
					}
					if (server.getOAuth()) { // if still set
						Snack.barError(activity, R.string.toast_oauth);
					}
					return false;
				} 
			} else {
				ErrorAlert.showDialog(activity,ErrorCodes.NO_LOGIN_DATA);
				return false;
			}

			CommitTask ct = new CommitTask(note, comment, close) {

				/** Flag to track if the bug is new. */
				private boolean newBug;

				@Override
				protected void onPreExecute() {
					Log.d(DEBUG_TAG,"onPreExecute");
					newBug = bug.isNew();
					if (!quiet) {
						Progress.showDialog(activity, Progress.PROGRESS_UPLOADING);
					}
				}

				@Override
				protected UploadResult doInBackground(Server... args) {
					// execute() is called below with no arguments (args will be empty)
					// getDisplayName() is deferred to here in case a lengthy OSM query
					// is required to determine the nickname
					Log.d(DEBUG_TAG,"doInBackground " + server.getReadWriteUrl());
					return super.doInBackground(server);
				}

				@Override
				protected void onPostExecute(UploadResult result) {
					Log.d(DEBUG_TAG,"onPostExecute");
					if (newBug && !App.getTaskStorage().contains(bug)) {
						App.getTaskStorage().add(bug);
					}
					if (result.error == ErrorCodes.OK) {
						// upload successful
						bug.changed = false;
						if (postUploadHandler != null) {
							postUploadHandler.onSuccess();
						}
					}	
					if (!quiet) {
						Progress.dismissDialog(activity, Progress.PROGRESS_UPLOADING);
						// Toast.makeText(context, result ? R.string.openstreetbug_commit_ok : R.string.openstreetbug_commit_fail, Toast.LENGTH_SHORT).show();
						if (!activity.isFinishing()) {
							if (result.error == ErrorCodes.INVALID_LOGIN) {
								InvalidLogin.showDialog(activity);
							} else if (result.error == ErrorCodes.FORBIDDEN) {
								ForbiddenLogin.showDialog(activity,result.message);
							} else if (result.error != 0) {
								ErrorAlert.showDialog(activity,result.error);
							}
						}
					}
				}
			};

			// FIXME seems as if AsyncTask tends to run out of threads here .... not clear if executeOnExecutor actually helps
		    if(Build.VERSION.SDK_INT >= 11) {
		        ct.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		    } else {
		        ct.execute();
		    }
			try {
				return ct.get().error == ErrorCodes.OK;
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
	
	/**
	 * Write Notes to a file in (J)OSM compatible format
	 * 
	 * If fileName contains directories these are created, otherwise it is stored in the standard public dir
	 * @param activity activity that called this
	 * @param all if true write all notes, if false just those that have been modified
	 * @param fileName file to write to
	 */
	static public void writeOsnFile(@NotNull final FragmentActivity activity, final boolean all, final String fileName) {
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Progress.showDialog(activity, Progress.PROGRESS_SAVING);
			}
			
			@Override
			protected Integer doInBackground(Void... arg) {
				final ArrayList<Task>queryResult = App.getTaskStorage().getTasks();
				int result = 0;
				try {
					File outfile = new File(fileName);
					String parent = outfile.getParent();
					if (parent == null) { // no directory specified, save to standard location
						outfile = new File(FileUtil.getPublicDirectory(), fileName);
					} else { // ensure directory exists
						File outdir = new File(parent);
						//noinspection ResultOfMethodCallIgnored
						outdir.mkdirs();
						if (!outdir.isDirectory()) {
							throw new IOException("Unable to create directory " + outdir.getPath());
						}
					}
					Log.d(DEBUG_TAG,"Saving to " + outfile.getPath());
					final OutputStream out = new BufferedOutputStream(new FileOutputStream(outfile));
					try {
						XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
						serializer.setOutput(out, "UTF-8");
						serializer.startDocument("UTF-8", null);
						serializer.startTag(null, "osm-notes");
						for (Task t:queryResult) {
							if (t instanceof Note) {
								Note n = (Note) t;
								if (all || n.getId() < 0 || n.hasBeenChanged()) {
									n.toJosmXml(serializer);
								}
							}
						}
						serializer.endTag(null, "osm-notes");
						serializer.endDocument();
						
					} catch (IllegalArgumentException e) {
						result = ErrorCodes.FILE_WRITE_FAILED;
						Log.e(DEBUG_TAG, "Problem writing", e);
					} catch (IllegalStateException e) {
						result = ErrorCodes.FILE_WRITE_FAILED;
						Log.e(DEBUG_TAG, "Problem writing", e);
					} catch (XmlPullParserException e) {
						result = ErrorCodes.FILE_WRITE_FAILED;
						Log.e(DEBUG_TAG, "Problem writing", e);
					} finally {
						SavingHelper.close(out);
					}
				} catch (IOException e) {
					result = ErrorCodes.FILE_WRITE_FAILED;
					Log.e(DEBUG_TAG, "Problem writing", e);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				Progress.dismissDialog(activity, Progress.PROGRESS_SAVING);
				if (result != 0) {
					if (result == ErrorCodes.OUT_OF_MEMORY) {
						System.gc();
						if (App.getTaskStorage().hasChanges()) {
							result = ErrorCodes.OUT_OF_MEMORY_DIRTY;
						}
					}
					if (!activity.isFinishing()) {
						ErrorAlert.showDialog(activity,result);
					}
				}
			}
			
		}.execute();
	}

}
