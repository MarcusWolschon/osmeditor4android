package de.blau.android.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import com.google.gson.stream.JsonWriter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.UploadResult;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.ForbiddenLogin;
import de.blau.android.dialogs.InvalidLogin;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.Task.State;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.IssueAlert;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;

public final class TransferTasks {

    private static final String DEBUG_TAG = TransferTasks.class.getSimpleName().substring(0, Math.min(23, TransferTasks.class.getSimpleName().length()));

    public static final String MAPROULETTE_APIKEY_V2 = "maproulette_apikey_v2";

    /** Maximum closed age to display: 7 days. */
    private static final long MAX_CLOSED_AGE = 7L * 24L * 60L * 60L * 1000L;

    /** maximum of tasks per request */
    public static final int MAX_PER_REQUEST = 1000;

    /**
     * Private constructor to stop instantiation
     */
    private TransferTasks() {
        // private
    }

    /**
     * Download tasks for a bounding box, actual requests will depend on what the current filter for tasks is set to
     * 
     * Executed in background thread
     * 
     * @param context Android context
     * @param server current server configuration
     * @param box the bounding box
     * @param add if true merge the download with existing task data
     * @param maxNotes maximum number of notes
     * @param handler handler to run after the download if not null
     */
    public static void downloadBox(@NonNull final Context context, @NonNull final Server server, @NonNull final BoundingBox box, final boolean add,
            int maxNotes, @Nullable final PostAsyncActionHandler handler) {

        final TaskStorage bugs = App.getTaskStorage();
        final Preferences prefs = App.getPreferences(context);

        box.makeValidForApi(server.getCachedCapabilities().getMaxNoteArea());
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Collection<Task>>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected Collection<Task> doInBackground(Void param) {
                Log.d(DEBUG_TAG, "querying server for " + box);
                return downloadBoxSync(context, server, box, add, bugs, prefs.taskFilter(), maxNotes);
            }

            @Override
            protected void onPostExecute(Collection<Task> result) {
                bugs.addBoundingBox(box);
                if (handler != null) {
                    handler.onSuccess();
                }
            }
        }.execute();
    }

    /**
     * Download tasks for a bounding box, actual requests will depend on what the current filter for tasks is set to
     * 
     * @param context Android context
     * @param server current server configuration
     * @param box the bounding box
     * @param add if true merge the download with existing task data
     * @param bugs the TaskStorage
     * @param bugFilter Strings indicating which tasks to download
     * @param maxNotes maximum number of notes
     * @return any tasks found in the BoundingBox
     */
    @NonNull
    public static Collection<Task> downloadBoxSync(@NonNull final Context context, @NonNull final Server server, @NonNull final BoundingBox box,
            final boolean add, @NonNull final TaskStorage bugs, final Set<String> bugFilter, int maxNotes) {
        if (!add) {
            Log.d(DEBUG_TAG, "resetting bug storage");
            bugs.reset();
        }
        Collection<Task> result = new ArrayList<>();
        Resources r = context.getResources();

        if (bugFilterContains(r, bugFilter, R.string.bugfilter_notes)) {
            result.addAll(server.getNotesForBox(box, maxNotes));
        }

        Preferences prefs = App.getPreferences(context);
        if (bugFilterContains(r, bugFilter, R.string.bugfilter_osmose_error) || bugFilterContains(r, bugFilter, R.string.bugfilter_osmose_warning)
                || bugFilterContains(r, bugFilter, R.string.bugfilter_osmose_minor_issue)) {
            result.addAll(OsmoseServer.getBugsForBox(prefs.getOsmoseServer(), box, MAX_PER_REQUEST));
        }

        final String mapRouletteServer = prefs.getMapRouletteServer();
        if (bugFilterContains(r, bugFilter, R.string.bugfilter_maproulette)) {
            Collection<MapRouletteTask> mapRouletteResult = MapRouletteServer.getTasksForBox(mapRouletteServer, box, MAX_PER_REQUEST);
            if (mapRouletteResult != null) {
                result.addAll(mapRouletteResult);
                Map<Long, MapRouletteChallenge> challenges = bugs.getChallenges();
                for (Long key : new ArrayList<>(challenges.keySet())) {
                    MapRouletteChallenge challenge = challenges.get(key);
                    if (challenge == null) {
                        challenges.put(key, MapRouletteServer.getChallenge(mapRouletteServer, key));
                    }
                }
            }
        }

        merge(context, bugs, result);
        return result;
    }

    /**
     * Check if using the tasks of a certain type is enabled or not
     * 
     * @param bugFilter the filter
     * @param r resources
     * @param stringRes the resouce id of the specific task type
     * @return true if the task type is enabled
     */
    private static boolean bugFilterContains(Resources r, final Set<String> bugFilter, int stringRes) {
        return bugFilter.contains(r.getString(stringRes));
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

        final List<Task> queryResult = App.getTaskStorage().getTasks();
        // check if we need to oAuth first
        for (Task b : queryResult) {
            if (b.hasBeenChanged() && b instanceof Note) {
                PostAsyncActionHandler restartAction = () -> upload(activity, App.getPreferences(activity).getServer(), postUploadHandler);
                if (!Server.checkOsmAuthentication(activity, server, restartAction)) {
                    return;
                }
            }
        }
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Boolean>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
                Log.d(DEBUG_TAG, "starting up load of total " + queryResult.size() + " tasks");
            }

            @Override
            protected Boolean doInBackground(Void param) {
                boolean uploadFailed = false;
                Preferences prefs = App.getPreferences(activity);
                for (Task b : queryResult) {
                    if (b.hasBeenChanged()) {
                        Log.d(DEBUG_TAG, b.getDescription());
                        if (b instanceof Note) {
                            Note n = (Note) b;
                            NoteComment nc = n.getLastComment();
                            uploadFailed = (uploadNote(server, n, nc != null && nc.isNew() ? nc : null, n.isClosed()).getError() != ErrorCodes.OK)
                                    || uploadFailed;
                        } else if (b instanceof OsmoseBug) {
                            uploadFailed = (OsmoseServer.changeState(prefs.getOsmoseServer(), (OsmoseBug) b).getError() != ErrorCodes.OK) || uploadFailed;
                        } else if (b instanceof MapRouletteTask) {
                            uploadFailed = !updateMapRouletteTask(activity, server, prefs.getMapRouletteServer(), (MapRouletteTask) b, true, null)
                                    || uploadFailed;
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
                    ScreenMessage.barInfo(activity, R.string.openstreetbug_commit_ok);
                    if (activity instanceof Main) {
                        ((Main) activity).invalidateMap();
                    }
                } else {
                    if (postUploadHandler != null) {
                        postUploadHandler.onError(null);
                    }
                    ScreenMessage.barError(activity, R.string.openstreetbug_commit_fail);
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
        Logic logic = App.getLogic();
        ExecutorTask<Void, Void, UploadResult> a = new ExecutorTask<Void, Void, UploadResult>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected UploadResult doInBackground(Void param) {
                Preferences prefs = App.getPreferences(context);
                return OsmoseServer.changeState(prefs.getOsmoseServer(), b);
            }

            @Override
            protected void onPostExecute(UploadResult result) {
                finishUpload(context, result, quiet, postUploadHandler);
            }
        };
        a.execute();
        try {
            return a.get().getError() == ErrorCodes.OK;
        } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in question
            Log.e(DEBUG_TAG, "updateOsmoseBug got " + e.getMessage());
            a.cancel();
        }
        return false;
    }

    /**
     * Commit changes to a Note
     * 
     * @param server Server configuration
     * @param note the Note
     * @param comment optional comment
     * @param close if true close the Note
     * @return an UploadResult object indicating the result of the operation
     */
    private static UploadResult uploadNote(@NonNull final Server server, @NonNull final Note note, @Nullable final NoteComment comment, final boolean close) {
        Log.d(DEBUG_TAG, "uploadNote " + server.getReadWriteUrl());
        UploadResult result = new UploadResult();
        try {
            boolean newNote = note.isNew();
            boolean wasClosed = note.getOriginalState() == State.CLOSED;
            if (!newNote && wasClosed && !close) {
                // reopen, do this before trying to add anything
                server.reopenNote(note);
            }
            if (!(wasClosed && close)) { // this doesn't make sense
                if (comment != null && comment.getText().length() > 0) {
                    // Add or edit the bug as appropriate
                    if (newNote) {
                        server.addNote(note, comment);
                    } else {
                        server.addComment(note, comment);
                    }
                }
                // Close the bug if requested, but only if there haven't been any problems
                if (close) {
                    server.closeNote(note);
                }
            }
            note.setChanged(false);
        } catch (final OsmServerException e) {
            int errorCode = e.getHttpErrorCode();
            result.setHttpError(errorCode);
            String message = e.getMessage();
            result.setMessage(message);
            switch (errorCode) {
            case HttpURLConnection.HTTP_FORBIDDEN:
                result.setError(ErrorCodes.FORBIDDEN);
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                result.setError(ErrorCodes.INVALID_LOGIN);
                break;
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_NOT_FOUND:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
            case HttpURLConnection.HTTP_BAD_GATEWAY:
            case HttpURLConnection.HTTP_UNAVAILABLE:
                result.setError(ErrorCodes.UPLOAD_PROBLEM);
                break;
            default:
                Log.e(DEBUG_TAG, "", e);
                ACRAHelper.nocrashReport(e, message);
                result.setError(ErrorCodes.UPLOAD_PROBLEM); // use this as generic error
                break;
            }
        } catch (XmlPullParserException e) {
            result.setError(ErrorCodes.INVALID_DATA_RECEIVED);
        } catch (IOException e) {
            result.setError(ErrorCodes.NO_CONNECTION);
        }
        return result;
    }

    /**
     * Commit changes to a Note
     * 
     * Interactive version that uploads asynchronously
     * 
     * @param activity activity that called this
     * @param server Server configuration
     * @param note the Note to upload
     * @param comment Comment to add to the Note.
     * @param close if true the Note is to be closed.
     * @param postUploadHandler execute code after an upload
     * @return true if upload was successful
     */
    public static boolean uploadNote(@NonNull final FragmentActivity activity, @NonNull final Server server, @NonNull final Note note,
            @Nullable final NoteComment comment, final boolean close, @Nullable final PostAsyncActionHandler postUploadHandler) {
        Log.d(DEBUG_TAG, "uploadNote");
        PostAsyncActionHandler restartAction = () -> {
            Preferences prefs = new Preferences(activity); // need to re-get this post authentication
            uploadNote(activity, prefs.getServer(), note, comment, close, postUploadHandler);
        };
        if (!Server.checkOsmAuthentication(activity, server, restartAction)) {
            return false;
        }
        Logic logic = App.getLogic();
        ExecutorTask<Server, Void, UploadResult> ct = new ExecutorTask<Server, Void, UploadResult>(logic.getExecutorService(), logic.getHandler()) {

            boolean newNote; // needs to be determined before upload

            @Override
            protected void onPreExecute() {
                newNote = note.isNew();
                Progress.showDialog(activity, Progress.PROGRESS_UPLOADING);
            }

            @Override
            protected UploadResult doInBackground(Server args) {
                return uploadNote(server, note, comment, close);
            }

            @Override
            protected void onPostExecute(UploadResult result) {
                if (newNote && !App.getTaskStorage().contains(note)) {
                    App.getTaskStorage().add(note);
                }
                if (result.getError() == ErrorCodes.OK) {
                    // upload successful
                    if (activity instanceof Main) {
                        ((Main) activity).invalidateMap();
                    }
                    if (postUploadHandler != null) {
                        postUploadHandler.onSuccess();
                    }
                }
                Progress.dismissDialog(activity, Progress.PROGRESS_UPLOADING);
                if (!activity.isFinishing()) {
                    if (result.getError() == ErrorCodes.INVALID_LOGIN) {
                        InvalidLogin.showDialog(activity);
                    } else if (result.getError() == ErrorCodes.FORBIDDEN) {
                        ForbiddenLogin.showDialog(activity, result.getMessage());
                    } else if (result.getError() != ErrorCodes.OK) {
                        ErrorAlert.showDialog(activity, result.getError());
                    } else { // no error
                        ScreenMessage.barInfo(activity, R.string.openstreetbug_commit_ok);
                    }
                }
            }
        };
        ct.execute();
        try {
            return ct.get().getError() == ErrorCodes.OK;
        } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in
                                                                // question
            Log.e(DEBUG_TAG, "uploadNote got " + e.getMessage());
            ct.cancel();
            return false;
        }
    }

    /**
     * Retrieve a single Note from the OSM API
     * 
     * @param server the API server instance
     * @param id the Note id
     * @return the Note or null
     * @throws NumberFormatException
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Nullable
    public static Note downloadNote(@NonNull Server server, long id) throws NumberFormatException, XmlPullParserException, IOException {
        Note note = server.getNote(id);
        if (note != null) {
            App.getTaskStorage().add(note);
        }
        return note;
    }

    /**
     * Update single bug state
     * 
     * @param activity the calling Activity
     * @param server Server configuration
     * @param maprouletteServer the maproulette server
     * @param task MapRouletteTask to update
     * @param quiet don't display messages if true
     * @param postUploadHandler if not null run this handler after update
     * @return true if successful
     */
    @SuppressLint("InlinedApi")
    public static boolean updateMapRouletteTask(@NonNull final FragmentActivity activity, @NonNull Server server, @NonNull String maprouletteServer,
            @NonNull final MapRouletteTask task, final boolean quiet, @Nullable final PostAsyncActionHandler postUploadHandler) {
        Log.d(DEBUG_TAG, "updateMapRouletteTask");
        final PostAsyncActionHandler restartAction = () -> {
            Log.d(DEBUG_TAG, "--- restarting");
            Logic logic = App.getLogic();
            new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
                @Override
                protected Void doInBackground(Void param) {
                    updateMapRouletteTask(activity, logic.getPrefs().getServer(), maprouletteServer, task, quiet, postUploadHandler);
                    return null;
                }
            }.execute();
        };
        if (!Server.checkOsmAuthentication(activity, server, restartAction)) {
            Log.d(DEBUG_TAG, "not authenticated");
            return false;
        }
        Logic logic = App.getLogic();
        ExecutorTask<Void, Void, UploadResult> a = new ExecutorTask<Void, Void, UploadResult>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected UploadResult doInBackground(Void param) {
                String apiKey = server.getUserPreferences().get(MAPROULETTE_APIKEY_V2);
                if (apiKey == null) {
                    return new UploadResult(ErrorCodes.MISSING_API_KEY);
                }
                return MapRouletteServer.changeState(maprouletteServer, apiKey, task);
            }

            @Override
            protected void onPostExecute(UploadResult result) {
                if (result.getError() == ErrorCodes.MISSING_API_KEY) {
                    MapRouletteApiKey.set(activity, server, true);
                    return;
                }
                finishUpload(activity, result, quiet, postUploadHandler);
            }
        };
        a.execute();
        try {
            return a.get().getError() == ErrorCodes.OK;
        } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in question
            Log.e(DEBUG_TAG, "updateMapRouletteTask got " + e.getMessage());
            a.cancel();
        }
        return false;
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
            postWrite.onError(null);
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
     * @param postWrite handler to execute after the task has finished
     */
    public static void writeOsnFile(@NonNull final FragmentActivity activity, final boolean all, @NonNull final Uri uri,
            @Nullable final PostAsyncActionHandler postWrite) {
        try {
            writeOsnFile(activity, all, new BufferedOutputStream(activity.getContentResolver().openOutputStream(uri, FileUtil.TRUNCATE_WRITE_MODE)), postWrite);
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
     * @param postWrite handler to execute after the task has finished
     */
    private static void writeOsnFile(@NonNull final FragmentActivity activity, final boolean all, @NonNull final OutputStream out,
            @Nullable final PostAsyncActionHandler postWrite) {
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Integer>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_SAVING);
            }

            @Override
            protected Integer doInBackground(Void arg) {
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
        Logic logic = App.getLogic();
        new ExecutorTask<Boolean, Void, List<Note>>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_LOADING);
            }

            @Override
            protected List<Note> doInBackground(Boolean arg) {
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
     * Read an Uri in todo format // NOSONAR
     * 
     * @param activity activity that called this
     * @param uri Uri to read
     * @param add if true the elements will be added to the existing ones, otherwise replaced
     * @param postLoad callback to execute once stream has been loaded
     */
    public static void readTodos(@NonNull final FragmentActivity activity, @NonNull final Uri uri, final boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {
        try {
            // don't use try with resources as this will close the InputStream while we are still reading it
            InputStream is = activity.getContentResolver().openInputStream(uri); // NOSONAR
            readTodos(activity, is, add, postLoad);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem parsing", e);
        }
    }

    /**
     * Read an InputStream in todo format // NOSONAR
     * 
     * @param activity activity that called this
     * @param is InputStream to read
     * @param add if true the elements will be added to the existing ones, otherwise replaced
     * @param postLoad callback to execute once stream has been loaded
     */
    public static void readTodos(@NonNull final FragmentActivity activity, @NonNull final InputStream is, final boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {
        Logic logic = App.getLogic();
        new ExecutorTask<Boolean, Void, Collection<Todo>>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_LOADING);
            }

            @Override
            protected Collection<Todo> doInBackground(Boolean arg) {
                try (InputStream in = new BufferedInputStream(is)) {
                    return Todo.parseTodos(is);
                } catch (IllegalStateException | NumberFormatException | IOException e) {
                    Log.e(DEBUG_TAG, "Problem parsing custom tasks", e);
                }
                return null; // NOSONAR
            }

            @Override
            protected void onPostExecute(Collection<Todo> result) {
                processReadResult(activity, Todo.class, add, postLoad, result);
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
    private static <T extends Task> void processReadResult(@NonNull final FragmentActivity activity, @NonNull Class<T> c, final boolean add,
            @Nullable final PostAsyncActionHandler postLoad, Collection<T> tasks) {
        Progress.dismissDialog(activity, Progress.PROGRESS_LOADING);
        if (tasks == null) {
            if (postLoad != null) {
                postLoad.onError(null);
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
     * Write Todos to an uri
     * 
     * @param activity activity that called this
     * @param uri uri to write to * @param list name of the todo list // NOSONAR
     * @param list name of the todo list // NOSONAR
     * @param all if true write all todos, otherwise just open ones
     * @param postWrite call this when finished
     */
    public static void writeTodoFile(@NonNull final FragmentActivity activity, @NonNull final Uri uri, @Nullable String list, boolean all,
            @Nullable final PostAsyncActionHandler postWrite) {
        try {
            writeTodoFile(activity, activity.getContentResolver().openOutputStream(uri, FileUtil.TRUNCATE_WRITE_MODE), list, all, postWrite);
        } catch (IOException e) {
            handleExceptionOnWrite(activity, postWrite, e);
        }
    }

    /**
     * Write Todos to an OutputStream
     * 
     * @param activity activity that called this
     * @param fileOut OutputStream to write to
     * @param list name of the todo list // NOSONAR
     * @param all if true write all todos, otherwise just open ones
     * @param postWrite call this when finished
     */
    private static void writeTodoFile(@NonNull final FragmentActivity activity, @NonNull final OutputStream fileOut, @Nullable String list, boolean all,
            @Nullable final PostAsyncActionHandler postWrite) {
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Integer>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_SAVING);
            }

            @Override
            protected Integer doInBackground(Void arg) {
                final List<Todo> queryResult = App.getTaskStorage().getTodos(list, true);
                int result = 0;
                try (final OutputStream out = new BufferedOutputStream(fileOut); JsonWriter writer = new JsonWriter(new PrintWriter(out))) {
                    writer.beginObject();
                    if (list != null && !"".equals(list)) {
                        Todo.headerToJSON(writer, list);
                    }
                    writer.name(Todo.TODOS);
                    writer.beginArray();
                    for (Todo t : queryResult) {
                        if (all || !t.isClosed()) {
                            t.toJSON(writer);
                        }
                    }
                    writer.endArray();
                    writer.endObject();
                } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                    result = ErrorCodes.FILE_WRITE_FAILED;
                    Log.e(DEBUG_TAG, "Problem writing todo file", e);
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
        synchronized (storage) { // NOSONAR this will be the same object
            for (Task b : tasks) {
                if (b.isNew()) { // need to renumber assuming that there are no duplicates
                    ((Note) b).setId(storage.getNextId());
                }
                Task existing = storage.get(b);
                if (existing == null) {
                    // add open bugs or closed bugs younger than 7 days
                    if (!b.isClosed() || (now - b.getLastUpdate().getTime()) < MAX_CLOSED_AGE) {
                        storage.add(b);
                        if (!b.isClosed() && generateAlerts) {
                            IssueAlert.alert(context, prefs, b);
                        }
                    }
                } else {
                    if (b.getLastUpdate().getTime() > existing.getLastUpdate().getTime()) {
                        // downloaded task is newer
                        if (existing.hasBeenChanged()) { // conflict, show message and abort
                            ScreenMessage.toastTopError(context, context.getString(R.string.toast_task_conflict, existing.getDescription()));
                            break;
                        } else {
                            storage.delete(existing);
                            storage.add(b);
                        }
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
                postWrite.onError(null);
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
     * @param result if true the upload succeeded
     * @param quiet if true don't show toasts
     * @param postUploadHandler callback to use once finished
     */
    private static void finishUpload(@NonNull final Context context, @NonNull UploadResult result, final boolean quiet,
            @Nullable final PostAsyncActionHandler postUploadHandler) {
        if (result.getError() == ErrorCodes.OK) {
            if (postUploadHandler != null) {
                postUploadHandler.onSuccess();
            }
            if (!quiet) {
                ScreenMessage.toastTopInfo(context, R.string.openstreetbug_commit_ok);
            }
        } else {
            if (postUploadHandler != null) {
                postUploadHandler.onError(new AsyncResult(result.getError(), result.getMessage()));
            }
            if (!quiet) {
                String message = result.getMessage();
                if (message != null && !"".equals(message)) {
                    ScreenMessage.toastTopError(context, context.getString(R.string.openstreetbug_commit_fail_with_message, message));
                } else {
                    ScreenMessage.toastTopError(context, R.string.openstreetbug_commit_fail);
                }
            }
        }
    }
}
