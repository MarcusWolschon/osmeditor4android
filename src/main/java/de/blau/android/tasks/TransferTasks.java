package de.blau.android.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
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
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.FileUtil;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;

public final class TransferTasks {

    private static final String DEBUG_TAG = TransferTasks.class.getSimpleName();

    public static final String MAPROULETTE_APIKEY_V2 = "maproulette_apikey_v2";

    /** Maximum closed age to display: 7 days. */
    private static final long MAX_CLOSED_AGE = 7L * 24L * 60L * 60L * 1000L;

    /** maximum of tasks per request */
    private static final int MAX_PER_REQUEST = 1000;

    /**
     * Private constructor to stop instantiation
     */
    private TransferTasks() {
        // private
    }

    /**
     * Download tasks for a bounding box, actual requests will depend on what the current filter for tasks is set to
     * 
     * Will not load Notes and Bugs that have been closed for more than a week
     * 
     * @param context Android context
     * @param server current server configuration
     * @param box the bounding box
     * @param add if true merge the download with existing task data
     * @param handler handler to run after the download if not null
     */
    public static void downloadBox(@NonNull final Context context, @NonNull final Server server, @NonNull final BoundingBox box, final boolean add,
            @Nullable final PostAsyncActionHandler handler) {
        downloadBox(context, server, box, add, MAX_CLOSED_AGE, handler);
    }

    /**
     * Download tasks for a bounding box, actual requests will depend on what the current filter for tasks is set to
     * 
     * @param context Android context
     * @param server current server configuration
     * @param box the bounding box
     * @param add if true merge the download with existing task data
     * @param maxClosedAge maximum time in ms since a Note was closed
     * @param handler handler to run after the download if not null
     */
    public static void downloadBox(@NonNull final Context context, @NonNull final Server server, @NonNull final BoundingBox box, final boolean add,
            long maxClosedAge, @Nullable final PostAsyncActionHandler handler) {

        final TaskStorage bugs = App.getTaskStorage();
        final Preferences prefs = new Preferences(context);

        box.makeValidForApi();

        new AsyncTask<Void, Void, Collection<Task>>() {
            @Override
            protected Collection<Task> doInBackground(Void... params) {
                Log.d(DEBUG_TAG, "querying server for " + box);
                Set<String> bugFilter = prefs.taskFilter();
                Collection<Task> result = new ArrayList<>();
                Collection<Note> noteResult = null;
                Resources r = context.getResources();
                if (bugFilter.contains(r.getString(R.string.bugfilter_notes))) {
                    noteResult = server.getNotesForBox(box, MAX_PER_REQUEST);
                }
                if (noteResult != null) {
                    result.addAll(noteResult);
                }
                Collection<OsmoseBug> osmoseResult = null;
                if (bugFilter.contains(r.getString(R.string.bugfilter_osmose_error)) || bugFilter.contains(r.getString(R.string.bugfilter_osmose_warning))
                        || bugFilter.contains(r.getString(R.string.bugfilter_osmose_minor_issue))) {
                    osmoseResult = OsmoseServer.getBugsForBox(context, box, MAX_PER_REQUEST);
                }
                if (osmoseResult != null) {
                    result.addAll(osmoseResult);
                }
                Collection<MapRouletteTask> mapRouletteResult = null;
                if (bugFilter.contains(r.getString(R.string.bugfilter_maproulette))) {
                    mapRouletteResult = MapRouletteServer.getTasksForBox(context, box, MAX_PER_REQUEST);
                }
                if (mapRouletteResult != null) {
                    result.addAll(mapRouletteResult);
                    Map<Long, MapRouletteChallenge> challenges = bugs.getChallenges();
                    for (Entry<Long, MapRouletteChallenge> entry : challenges.entrySet()) {
                        if (entry.getValue() == null) {
                            challenges.put(entry.getKey(), MapRouletteServer.getChallenge(context, entry.getKey()));
                        }
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(Collection<Task> result) {
                if (result == null) {
                    Log.d(DEBUG_TAG, "no bugs found");
                    return;
                }
                if (!add) {
                    Log.d(DEBUG_TAG, "resetting bug storage");
                    bugs.reset();
                }
                bugs.addBoundingBox(box);
                merge(context, bugs, result);
                if (handler != null) {
                    handler.onSuccess();
                }
            }
        }.execute();
    }

    /**
     * Upload Notes or bugs to server, needs to be called from main for now (mainly for OAuth dependency)
     * 
     * @param activity activity calling this
     * @param server current server configuration
     * @param postUploadHandler execute code after an upload
     */
    public static void upload(@NonNull final FragmentActivity activity, @NonNull final Server server,
            @Nullable final PostAsyncActionHandler postUploadHandler) {
        final String PROGRESS_TAG = "tasks";

        Set<String> bugFilter = App.getLogic().getPrefs().taskFilter();
        final List<Task> queryResult = App.getTaskStorage().getTasks();
        // check if we need to oAuth first
        for (Task b : queryResult) {
            if (b.hasBeenChanged() && b instanceof Note && bugFilter.contains(activity.getString(R.string.bugfilter_notes))) {
                PostAsyncActionHandler restartAction = () -> {
                    Preferences prefs = new Preferences(activity);
                    upload(activity, prefs.getServer(), postUploadHandler);
                };
                if (!Server.checkOsmAuthentication(activity, server, restartAction)) {
                    return;
                }
            }
        }
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
                Log.d(DEBUG_TAG, "starting up load of total " + queryResult.size() + " tasks");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                boolean uploadFailed = false;
                for (Task b : queryResult) {
                    if (b.hasBeenChanged()) {
                        Log.d(DEBUG_TAG, b.getDescription());
                        if (b instanceof Note && bugFilter.contains(activity.getString(R.string.bugfilter_notes))) {
                            Note n = (Note) b;
                            NoteComment nc = n.getLastComment();
                            if (nc != null && nc.isNew()) {
                                uploadFailed = !uploadNote(activity, server, n, nc.getText(), n.isClosed(), true, null) || uploadFailed;
                            } else {
                                // just a state change
                                uploadFailed = !uploadNote(activity, server, n, null, n.isClosed(), true, null) || uploadFailed;
                            }
                        } else if (b instanceof OsmoseBug && (bugFilter.contains(activity.getString(R.string.bugfilter_osmose_error))
                                || bugFilter.contains(activity.getString(R.string.bugfilter_osmose_warning))
                                || bugFilter.contains(activity.getString(R.string.bugfilter_osmose_minor_issue)))) {
                            uploadFailed = !OsmoseServer.changeState(activity, (OsmoseBug) b) || uploadFailed;
                        } else if (b instanceof MapRouletteTask && bugFilter.contains(activity.getString(R.string.bugfilter_maproulette))) {
                            uploadFailed = !updateMapRouletteTask(activity, server, (MapRouletteTask) b, true, null) || uploadFailed;
                        }
                    }
                }
                return uploadFailed;
            }

            @Override
            protected void onPostExecute(Boolean uploadFailed) {
                Progress.dismissDialog(activity, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
                if (Boolean.FALSE.equals(uploadFailed)) {
                    if (postUploadHandler != null) {
                        postUploadHandler.onSuccess();
                    }
                    Snack.barInfo(activity, R.string.openstreetbug_commit_ok);
                    if (activity instanceof Main) {
                        ((Main) activity).invalidateMap();
                    }
                } else {
                    if (postUploadHandler != null) {
                        postUploadHandler.onError();
                    }
                    Snack.barError(activity, R.string.openstreetbug_commit_fail);
                }
            }
        }.execute();
    }

    /**
     * Update single bug state
     * 
     * @param context the Android context
     * @param b osmose bug to update
     * @param quiet don't display messages if true
     * @param postUploadHandler if not null run this handler after update
     * @return true if successful
     */
    @SuppressLint("InlinedApi")
    public static boolean updateOsmoseBug(@NonNull final Context context, @NonNull final OsmoseBug b, final boolean quiet,
            @Nullable final PostAsyncActionHandler postUploadHandler) {
        Log.d(DEBUG_TAG, "updateOsmoseBug");
        AsyncTask<Void, Void, Boolean> a = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return OsmoseServer.changeState(context, b);
            }

            @Override
            protected void onPostExecute(Boolean uploadSucceded) {
                finishUpload(context, uploadSucceded, quiet, postUploadHandler);
            }
        };
        a.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
            return a.get();
        } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in question
            Log.e(DEBUG_TAG, "updateOsmoseBug got " + e.getMessage());
            a.cancel(true);
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
     * @param postUploadHandler execute code after an upload
     * @return true if upload was successful
     */
    @TargetApi(11)
    public static boolean uploadNote(@NonNull final FragmentActivity activity, @NonNull final Server server, @NonNull final Note note, final String comment,
            final boolean close, final boolean quiet, @Nullable final PostAsyncActionHandler postUploadHandler) {
        Log.d(DEBUG_TAG, "uploadNote");
        PostAsyncActionHandler restartAction = () -> {
            Preferences prefs = new Preferences(activity); // need to re-get this post authentication
            uploadNote(activity, prefs.getServer(), note, comment, close, quiet, postUploadHandler);
        };
        if (!Server.checkOsmAuthentication(activity, server, restartAction)) {
            return false;
        }

        CommitTask ct = new CommitTask(note, comment, close) {

            /** Flag to track if the bug is new. */
            private boolean newBug;

            @Override
            protected void onPreExecute() {
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
                Log.d(DEBUG_TAG, "uploadNote " + server.getReadWriteUrl());
                return super.doInBackground(server);
            }

            @Override
            protected void onPostExecute(UploadResult result) {
                if (newBug && !App.getTaskStorage().contains(bug)) {
                    App.getTaskStorage().add(bug);
                }
                if (result.getError() == ErrorCodes.OK) {
                    // upload successful
                    bug.setChanged(false);
                    if (activity instanceof Main) {
                        ((Main) activity).invalidateMap();
                    }
                    if (postUploadHandler != null) {
                        postUploadHandler.onSuccess();
                    }
                }
                if (!quiet) {
                    Progress.dismissDialog(activity, Progress.PROGRESS_UPLOADING);
                    if (!activity.isFinishing()) {
                        if (result.getError() == ErrorCodes.INVALID_LOGIN) {
                            InvalidLogin.showDialog(activity);
                        } else if (result.getError() == ErrorCodes.FORBIDDEN) {
                            ForbiddenLogin.showDialog(activity, result.getMessage());
                        } else if (result.getError() != ErrorCodes.OK) {
                            ErrorAlert.showDialog(activity, result.getError());
                        } else { // no error
                            Snack.barInfo(activity, R.string.openstreetbug_commit_ok);
                        }
                    }
                }
            }
        };

        // FIXME seems as if AsyncTask tends to run out of threads here .... not clear if executeOnExecutor actually
        // helps
        ct.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
            return ct.get().getError() == ErrorCodes.OK;
        } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in
                                                                // question
            Log.e(DEBUG_TAG, "uploadNote got " + e.getMessage());
            ct.cancel(true);
            return false;
        }
    }

    /**
     * Update single bug state
     * 
     * @param activity the calling Activity
     * @param server Server configuration
     * @param task MapRouletteTask to update
     * @param quiet don't display messages if true
     * @param postUploadHandler if not null run this handler after update
     * @return true if successful
     */
    @SuppressLint("InlinedApi")
    public static boolean updateMapRouletteTask(@NonNull final FragmentActivity activity, @NonNull Server server, @NonNull final MapRouletteTask task,
            final boolean quiet, @Nullable final PostAsyncActionHandler postUploadHandler) {
        Log.d(DEBUG_TAG, "updateMapRouletteTask");
        PostAsyncActionHandler restartAction = () -> {
            Log.d(DEBUG_TAG, "--- restarting");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Preferences prefs = new Preferences(activity);
                    updateMapRouletteTask(activity, prefs.getServer(), task, quiet, postUploadHandler);
                    return null;
                }
            }.execute();
        };
        if (!Server.checkOsmAuthentication(activity, server, restartAction)) {
            Log.d(DEBUG_TAG, "not authenticated");
            return false;
        }
        String apiKey = server.getUserPreferences().get(MAPROULETTE_APIKEY_V2);
        if (apiKey == null) {
            activity.runOnUiThread(() -> MapRouletteApiKey.set(activity, server, true));
            return false;
        }

        AsyncTask<Void, Void, Boolean> a = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return MapRouletteServer.changeState(activity, apiKey, task);
            }

            @Override
            protected void onPostExecute(Boolean uploadSucceded) {
                finishUpload(activity, uploadSucceded, quiet, postUploadHandler);
            }
        };
        a.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        try {
            return a.get();
        } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in question
            Log.e(DEBUG_TAG, "updateMapRouletteTask got " + e.getMessage());
            a.cancel(true);
        }
        return false;
    }

    /**
     * Write Notes to a file in (J)OSM compatible format
     * 
     * If fileName contains directories these are created, otherwise it is stored in the standard public dir
     * 
     * @param activity activity that called this
     * @param all if true write all notes, if false just those that have been modified
     * @param fileName file to write to
     * @param postWrite handler to execute after the AsyncTask has finished
     */
    public static void writeOsnFile(@NonNull final FragmentActivity activity, final boolean all, @NonNull final String fileName,
            @Nullable final PostAsyncActionHandler postWrite) {
        try {
            File outfile = FileUtil.openFileForWriting(activity, fileName);
            Log.d(DEBUG_TAG, "Saving to " + outfile.getPath());
            writeOsnFile(activity, all, new BufferedOutputStream(new FileOutputStream(outfile)), postWrite);
        } catch (IOException e) {
            handleExceptionOnWrite(activity, postWrite, e);
        }
    }

    /**
     * Common code to execute if a write throws an exception
     * 
     * @param activity the calling FragmentActivity
     * @param postWrite a supplied handler
     * @param e the Exception
     */
    private static void handleExceptionOnWrite(@NonNull final FragmentActivity activity, @Nullable final PostAsyncActionHandler postWrite,
            @NonNull IOException e) {
        Log.e(DEBUG_TAG, "Problem writing", e);
        if (postWrite != null) {
            postWrite.onError();
        }
        if (!activity.isFinishing()) {
            ErrorAlert.showDialog(activity, ErrorCodes.FILE_WRITE_FAILED);
        }
    }

    /**
     * Write Notes to a file in (J)OSM compatible format
     * 
     * If fileName contains directories these are created, otherwise it is stored in the standard public dir
     * 
     * @param activity activity that called this
     * @param all if true write all notes, if false just those that have been modified
     * @param uri Uri to write to
     * @param postWrite handler to execute after the AsyncTask has finished
     */
    public static void writeOsnFile(@NonNull final FragmentActivity activity, final boolean all, @NonNull final Uri uri,
            @Nullable final PostAsyncActionHandler postWrite) {
        try {
            writeOsnFile(activity, all, new BufferedOutputStream(activity.getContentResolver().openOutputStream(uri)), postWrite);
        } catch (IOException e) {
            handleExceptionOnWrite(activity, postWrite, e);
        }
    }

    /**
     * Write Notes to a file in (J)OSM compatible format
     * 
     * If fileName contains directories these are created, otherwise it is stored in the standard public dir
     * 
     * @param activity activity that called this
     * @param all if true write all notes, if false just those that have been modified
     * @param out OutputStream to write to
     * @param postWrite handler to execute after the AsyncTask has finished
     */
    private static void writeOsnFile(@NonNull final FragmentActivity activity, final boolean all, @NonNull final OutputStream out,
            @Nullable final PostAsyncActionHandler postWrite) {
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_SAVING);
            }

            @Override
            protected Integer doInBackground(Void... arg) {
                final List<Task> queryResult = App.getTaskStorage().getTasks();
                int result = 0;
                try {
                    XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
                    serializer.setOutput(out, OsmXml.UTF_8);
                    serializer.startDocument(OsmXml.UTF_8, null);
                    serializer.startTag(null, OsnParser.OSM_NOTES);
                    for (Task t : queryResult) {
                        if (t instanceof Note) {
                            Note n = (Note) t;
                            if (all || n.getId() < 0 || n.hasBeenChanged()) {
                                n.toJosmXml(serializer);
                            }
                        }
                    }
                    serializer.endTag(null, OsnParser.OSM_NOTES);
                    serializer.endDocument();
                } catch (IOException | IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
                    result = ErrorCodes.FILE_WRITE_FAILED;
                    Log.e(DEBUG_TAG, "Problem writing OSN file", e);
                } finally {
                    SavingHelper.close(out);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                finishWriting(activity, result, postWrite);
            }

        }.execute();
    }

    /**
     * Read an Uri in OSN format
     * 
     * @param activity activity that called this
     * @param uri Uri to read
     * @param add if true the elements will be added to the existing ones, otherwise replaced
     * @param postLoad callback to execute once stream has been loaded
     */
    public static void readOsnFile(@NonNull final FragmentActivity activity, @NonNull final Uri uri, final boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {
        try {
            // don't use try with resources as this will close the InputStream while we are still reading it
            InputStream is = activity.getContentResolver().openInputStream(uri); // NOSONAR
            readOsnFile(activity, is, add, postLoad);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem parsing opening inputstream", e);
        }
    }

    /**
     * Read an InputStream in OSN format
     * 
     * Assumes that we have checked for changed Notes in advanced if add is not set to true
     * 
     * @param activity activity that called this
     * @param is InputStream to read
     * @param add if true the elements will be added to the existing ones, otherwise replaced
     * @param postLoad callback to execute once stream has been loaded
     */
    public static void readOsnFile(@NonNull final FragmentActivity activity, @NonNull final InputStream is, final boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {

        new AsyncTask<Boolean, Void, List<Note>>() {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_LOADING);
            }

            @Override
            protected List<Note> doInBackground(Boolean... arg) {
                OsnParser parser = null;
                try (InputStream in = new BufferedInputStream(is)) {
                    parser = new OsnParser();
                    parser.start(in);
                    List<Note> notes = parser.getNotes();
                    List<Exception> exceptions = parser.getExceptions();
                    if (notes.isEmpty() || !exceptions.isEmpty()) {
                        return null; // NOSONAR
                    }
                    return notes;
                } catch (IllegalStateException | NumberFormatException | IOException | SAXException | ParserConfigurationException e) {
                    Log.e(DEBUG_TAG, "Problem parsing OSN file", e);
                }
                return null; // NOSONAR
            }

            @Override
            protected void onPostExecute(List<Note> result) {
                processReadResult(activity, Note.class, add, postLoad, result);
            }
        }.execute(add);
    }

    /**
     * Invalidate map and options menu
     * 
     * @param activity the calling FragmentActivity
     */
    private static void invalidateUi(@NonNull final FragmentActivity activity) {
        if (activity instanceof Main) {
            ((Main) activity).invalidateMap();
        }
        activity.invalidateOptionsMenu();
    }

    /**
     * Read an Uri in custom bug format
     * 
     * @param activity activity that called this
     * @param uri Uri to read
     * @param add if true the elements will be added to the existing ones, otherwise replaced
     * @param postLoad callback to execute once stream has been loaded
     */
    public static void readCustomBugs(@NonNull final FragmentActivity activity, @NonNull final Uri uri, final boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {
        try {
            // don't use try with resources as this will close the InputStream while we are still reading it
            InputStream is = activity.getContentResolver().openInputStream(uri); // NOSONAR
            readCustomBugs(activity, is, add, postLoad);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem parsing", e);
        }
    }

    /**
     * Read an InputStream in custom bug format
     * 
     * @param activity activity that called this
     * @param is InputStream to read
     * @param add if true the elements will be added to the existing ones, otherwise replaced
     * @param postLoad callback to execute once stream has been loaded
     */
    public static void readCustomBugs(@NonNull final FragmentActivity activity, @NonNull final InputStream is, final boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {

        new AsyncTask<Boolean, Void, Collection<CustomBug>>() {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_LOADING);
            }

            @Override
            protected Collection<CustomBug> doInBackground(Boolean... arg) {
                try (InputStream in = new BufferedInputStream(is)) {
                    return CustomBug.parseBugs(is);
                } catch (IllegalStateException | NumberFormatException | IOException e) {
                    Log.e(DEBUG_TAG, "Problem parsing custom tasks", e);
                }
                return null; // NOSONAR
            }

            @Override
            protected void onPostExecute(Collection<CustomBug> result) {
                processReadResult(activity, CustomBug.class, add, postLoad, result);
            }
        }.execute(add);
    }

    /**
     * Process tasks received by reading a file
     * 
     * @param <T> type constraint for c
     * @param activity the calling FragmentActivity
     * @param c the class of Task
     * @param add if true the elements will be added to the existing ones, otherwise replaced
     * @param postLoad callback to execute once tasks have been processed
     * @param tasks the Tasks
     */
    private static <T extends Task> void processReadResult(final FragmentActivity activity, Class<T> c, final boolean add,
            final PostAsyncActionHandler postLoad, Collection<T> tasks) {
        Progress.dismissDialog(activity, Progress.PROGRESS_LOADING);
        if (tasks == null) {
            if (postLoad != null) {
                postLoad.onError();
            }
        } else {
            final TaskStorage bugs = App.getTaskStorage();
            if (!add) {
                delete(bugs, c);
            }
            merge(activity, bugs, tasks);
            addBoundingBoxFromData(bugs, tasks);
            if (postLoad != null) {
                postLoad.onSuccess();
            }
        }
        invalidateUi(activity);
    }

    /**
     * Delete all Tasks of a certain type from storage
     *
     * @param <T> type parameter
     * @param storage Task storage
     * @param c the class we want to delete
     */
    private static <T extends Task> void delete(final TaskStorage storage, Class<T> c) {
        for (Task t : storage.getTasks()) {
            if (c.isInstance(t)) {
                storage.delete(t);
            }
        }
    }

    /**
     * Write CustomBugs to a file
     * 
     * If fileName contains directories these are created, otherwise it is stored in the standard public dir
     * 
     * @param activity activity that called this
     * @param fileName file to write to
     * @param postWrite call this when finished
     */
    public static void writeCustomBugFile(@NonNull final FragmentActivity activity, @NonNull final String fileName,
            @Nullable final PostAsyncActionHandler postWrite) {
        try {
            File outfile = FileUtil.openFileForWriting(activity, fileName);
            Log.d(DEBUG_TAG, "Saving to " + outfile.getPath());
            writeCustomBugFile(activity, new FileOutputStream(outfile), postWrite);
        } catch (IOException e) {
            handleExceptionOnWrite(activity, postWrite, e);
        }
    }

    /**
     * Write CustomBugs to an uri
     * 
     * @param activity activity that called this
     * @param uri uri to write to
     * @param postWrite call this when finished
     */
    public static void writeCustomBugFile(@NonNull final FragmentActivity activity, @NonNull final Uri uri, @Nullable final PostAsyncActionHandler postWrite) {
        try {
            writeCustomBugFile(activity, activity.getContentResolver().openOutputStream(uri), postWrite);
        } catch (IOException e) {
            handleExceptionOnWrite(activity, postWrite, e);
        }
    }

    /**
     * Write CustomBugs to an OutputStream
     * 
     * @param activity activity that called this
     * @param fileOut OutputStream to write to
     * @param postWrite call this when finished
     */
    private static void writeCustomBugFile(@NonNull final FragmentActivity activity, @NonNull final OutputStream fileOut,
            @Nullable final PostAsyncActionHandler postWrite) {
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_SAVING);
            }

            @Override
            protected Integer doInBackground(Void... arg) {
                final List<Task> queryResult = App.getTaskStorage().getTasks();
                int result = 0;
                try (final OutputStream out = new BufferedOutputStream(fileOut)) {
                    out.write("{".getBytes());
                    out.write(CustomBug.headerToJSON().getBytes());
                    out.write("\"errors\": [".getBytes());
                    boolean first = true;
                    for (Task t : queryResult) {
                        if (t instanceof CustomBug && !t.isClosed()) {
                            if (!first) {
                                out.write(",".getBytes());
                            }
                            out.write(((CustomBug) t).toJSON().getBytes());
                            first = false;
                        }
                    }
                    out.write("]}".getBytes());
                } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                    result = ErrorCodes.FILE_WRITE_FAILED;
                    Log.e(DEBUG_TAG, "Problem writing custom task file", e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                finishWriting(activity, result, postWrite);
            }
        }.execute();
    }

    /**
     * Merge tasks in to existing
     * 
     * @param <T> the Task type
     * @param context an Android Context
     * @param storage the target TaskStorage
     * @param tasks a Collection of tasks
     */
    public static <T extends Task> void merge(@NonNull final Context context, @NonNull final TaskStorage storage, @NonNull Collection<T> tasks) {
        final Preferences prefs = App.getLogic().getPrefs();
        boolean generateAlerts = prefs.generateAlerts();
        long now = System.currentTimeMillis();
        for (Task b : tasks) {
            Log.d(DEBUG_TAG, "got bug " + b.getDescription() + " " + storage.toString());
            if (b.getId() < 0 && b instanceof Note) { // need to renumber assuming that there are no duplicates
                ((Note) b).setId(storage.getNextId());
            }
            Task existing = storage.get(b);
            if (existing == null) {
                // add open bugs or closed bugs younger than 7 days
                if (!b.isClosed() || (now - b.getLastUpdate().getTime()) < MAX_CLOSED_AGE) {
                    storage.add(b);
                    Log.d(DEBUG_TAG, "adding bug " + b.getDescription());
                    if (!b.isClosed() && generateAlerts) {
                        IssueAlert.alert(context, prefs, b);
                    }
                }
            } else {
                if (b.getLastUpdate().getTime() > existing.getLastUpdate().getTime()) {
                    // downloaded task is newer
                    if (existing.hasBeenChanged()) { // conflict, show message and abort
                        Snack.toastTopError(context, context.getString(R.string.toast_task_conflict, existing.getDescription()));
                        break;
                    } else {
                        storage.delete(existing);
                        storage.add(b);
                    }
                }
            }
        }
    }

    /**
     * Calculate a bounding box from task data
     * 
     * @param <T> actual type of task
     * @param storage the task storage
     * @param tasks the task data
     */
    public static <T extends Task> void addBoundingBoxFromData(@NonNull final TaskStorage storage, @NonNull Collection<T> tasks) {
        BoundingBox box = null;
        for (T task : tasks) {
            if (box == null) {
                box = task.getBounds();
            } else {
                box.union(task.getBounds());
            }
        }
        if (box != null && !box.isEmpty()) {
            storage.addBoundingBox(box);
        }
    }

    /**
     * Process the result of writing a file
     * 
     * @param activity the calling FragmentActivity
     * @param result the result code
     * @param postWrite callback to use once finished
     */
    private static void finishWriting(@NonNull final FragmentActivity activity, @Nullable Integer result, @Nullable final PostAsyncActionHandler postWrite) {
        Progress.dismissDialog(activity, Progress.PROGRESS_SAVING);
        if (result != null && result != 0) {
            if (result == ErrorCodes.OUT_OF_MEMORY && App.getTaskStorage().hasChanges()) {
                result = ErrorCodes.OUT_OF_MEMORY_DIRTY;
            }
            if (postWrite != null) {
                postWrite.onError();
            }
            if (!activity.isFinishing()) {
                ErrorAlert.showDialog(activity, result);
            }
        } else {
            if (postWrite != null) {
                postWrite.onSuccess();
            }
        }
    }

    /**
     * Process the result of uploading some data
     * 
     * @param context an Android Context
     * @param uploadSucceded if true the upload succeded
     * @param quiet if true don't show toasts
     * @param postUploadHandler callback to use once finished
     */
    private static void finishUpload(@NonNull final Context context, @Nullable Boolean uploadSucceded, final boolean quiet,
            @Nullable final PostAsyncActionHandler postUploadHandler) {
        if (Boolean.TRUE.equals(uploadSucceded)) {
            if (postUploadHandler != null) {
                postUploadHandler.onSuccess();
            }
            if (!quiet) {
                Snack.toastTopInfo(context, R.string.openstreetbug_commit_ok);
            }
        } else {
            if (postUploadHandler != null) {
                postUploadHandler.onError();
            }
            if (!quiet) {
                Snack.toastTopError(context, R.string.openstreetbug_commit_fail);
            }
        }
    }
}
