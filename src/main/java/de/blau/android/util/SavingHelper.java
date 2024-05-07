package de.blau.android.util;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;

/**
 * Helper class for loading and saving individual serializable objects to files. Does all error handling, stream opening
 * etc.
 *
 * @param <T> The type of the saved objects
 */
public class SavingHelper<T extends Serializable> {

    private static final String DEBUG_TAG = SavingHelper.class.getSimpleName().substring(0, Math.min(23, SavingHelper.class.getSimpleName().length()));

    private static final String BACKUP_EXTENSION = ".backup";

    private static final long DEFAULT_STACK_SIZE = 200000L;
    private static final int  ADD_STACK          = 2000000;
    private static final int  LOTS_OF_MEMORY     = 10000000;
    private static final int  TIMEOUT            = 60000;   // wait max 60 s for thread to finish, NOTE this limits the
                                                            // size of the file that can be saved

    /**
     * Date pattern used for the export file name.
     */
    private static final String DATE_PATTERN_EXPORT_FILE_NAME_PART = "yyyy-MM-dd'T'HHmmss";

    private final long stackSize;

    /**
     * Create a new instance
     */
    public SavingHelper() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        stackSize = DEFAULT_STACK_SIZE + (freeMemory > LOTS_OF_MEMORY ? ADD_STACK : 0);
    }

    /**
     * Serializes the given object and writes it to a private file with the given name
     * 
     * Original version was running out of stack, fixed by moving to a thread
     * 
     * @param context Android Context
     * @param filename filename of the save file
     * @param object object to save
     * @param compress true if the output should be gzip-compressed, false if it should be written without compression,
     *            ignored
     * @return true if successful, false if saving failed for some reason
     */
    public synchronized boolean save(@NonNull Context context, @NonNull String filename, @NonNull T object, boolean compress) {
        return save(context, filename, object, compress, false);
    }

    /**
     * Serializes the given object and writes it to a private file with the given name
     * 
     * Original version was running out of stack, fixed by moving to a thread
     * 
     * @param context Android Context
     * @param filename filename of the save file
     * @param object object to save
     * @param compress true if the output should be gzip-compressed, false if it should be written without compression,
     *            ignored
     * @return true if successful, false if saving failed for some reason
     * @param jdk use the built-in serialisation if true
     */
    public synchronized boolean save(@NonNull Context context, @NonNull String filename, @NonNull T object, boolean compress, boolean jdk) {
        try {
            Log.d(DEBUG_TAG, "preparing to save " + filename);
            SaveThread r = new SaveThread(context, filename, object, compress, jdk);
            Thread t = new Thread(null, r, SaveThread.DEBUG_TAG, stackSize);
            t.start();
            t.join(TIMEOUT);
            Log.d(DEBUG_TAG, "save thread finished");
            return r.getResult();
        } catch (Exception e) { // NOSONAR
            ACRAHelper.nocrashReport(e, e.getMessage());
            return false;
        }
    }

    private class SaveThread implements Runnable {

        private static final String DEBUG_TAG = "SaveThread";

        final String  filename;
        T             object;
        final boolean compress;
        final Context context;
        boolean       result = false;
        final boolean jdkSerialisation;

        /**
         * Construct a new SaveThread
         * 
         * @param context Android Context
         * @param fn the name of the file to save to
         * @param obj the object to save
         * @param c if true compress
         * @param jdk use the built-in serialisation if true
         */
        SaveThread(@NonNull Context context, @NonNull String fn, @NonNull T obj, boolean c, boolean jdk) {
            filename = fn;
            object = obj;
            compress = c;
            this.context = context;
            jdkSerialisation = jdk;
        }

        /**
         * Check if the save was successful
         * 
         * @return true if the save was successful
         */
        public boolean getResult() {
            return result;
        }

        @Override
        public void run() {
            Log.i(DEBUG_TAG, "saving  " + filename);
            String tempFilename = filename + "." + System.currentTimeMillis();
            try (OutputStream out = context.openFileOutput(tempFilename, Context.MODE_PRIVATE);
                    ObjectOutput objectOut = jdkSerialisation ? new ObjectOutputStream(out) : App.getFSTInstance().getObjectOutput(out)) {
                objectOut.writeObject(object);
                objectOut.flush();
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Exception, failed to save " + filename, e);
                ACRAHelper.nocrashReport(e, e.getMessage());
                return;
            } catch (Error e) { // NOSONAR crashing is not an option
                final String message = "Error, failed to save " + filename + " " + e.getMessage() + " with stack size " + stackSize;
                Log.e(DEBUG_TAG, message, e);
                ACRAHelper.nocrashReport(e, message);
                return;
            }
            try {
                rename(context, filename, filename + BACKUP_EXTENSION); // don't overwrite last saved state
                rename(context, tempFilename, filename); // rename to expected name
                Log.i(DEBUG_TAG, "saved " + filename + " successfully");
                result = true;
            } catch (Exception ex) {
                Log.e(DEBUG_TAG, "Exception, renaming " + filename, ex);
                ACRAHelper.nocrashReport(ex, ex.getMessage());
            }
        }
    }

    /**
     * Loads and de-serializes a single object from the given file Original version was running out of stack, fixed by
     * moving to a thread
     * 
     * @param context Android Context
     * @param filename filename of the save file
     * @param compressed true if the output is gzip-compressed, false if it is uncompressed
     * @return the de-serialized object if successful, null if loading/deserialization/casting failed
     */
    public synchronized T load(@NonNull Context context, @NonNull String filename, boolean compressed) {
        return load(context, filename, compressed, false, false);
    }

    /**
     * Loads and de-serializes a single object from the given file Original version was running out of stack, fixed by
     * moving to a thread
     * 
     * @param context Android Context
     * @param filename filename of the save file
     * @param compressed true if the output is gzip-compressed, false if it is uncompressed
     * @param deleteOnFail if true delete the file we tried to load (because it is likely corrupted)
     * @param jdk use the built-in serialisation if true
     * @return the de-serialized object if successful, null if loading/deserialization/casting failed
     */
    public synchronized T load(@NonNull Context context, @NonNull String filename, boolean compressed, boolean deleteOnFail, boolean jdk) {
        try {
            Log.d(DEBUG_TAG, "preparing to load " + filename);
            LoadThread r = new LoadThread(context, filename, compressed, deleteOnFail, jdk);
            Thread t = new Thread(null, r, LoadThread.DEBUG_TAG, stackSize);
            t.start();
            t.join(TIMEOUT);
            Log.d(DEBUG_TAG, "load thread finished");
            return r.getResult();
        } catch (Exception e) { // NOSONAR
            ACRAHelper.nocrashReport(e, e.getMessage());
            return null;
        }
    }

    private class LoadThread implements Runnable {

        private static final String DEBUG_TAG = "LoadThread";

        final String  filename;
        final boolean compressed;
        final boolean deleteOnFail;
        final Context context;
        T             result = null;
        final boolean jdkSerialisation;

        /**
         * Create a new LoadThread
         * 
         * @param context Android Context
         * @param fn the filename of the file to load
         * @param c if true compress
         * @param deleteOnFail if true delete if the file can't be read
         * @param jdk use the built-in serialisation if true
         */
        LoadThread(@NonNull Context context, @NonNull String fn, boolean c, boolean deleteOnFail, boolean jdk) {
            filename = fn;
            compressed = c;
            this.deleteOnFail = deleteOnFail;
            this.context = context;
            jdkSerialisation = jdk;
        }

        /**
         * Get the loaded class
         * 
         * @return the loaded class
         */
        public T getResult() {
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try (InputStream in = context.openFileInput(filename);
                    ObjectInput objectIn = jdkSerialisation ? new ObjectInputStream(in) : App.getFSTInstance().getObjectInput(in)) {
                Log.d(DEBUG_TAG, "loading  " + filename);
                result = (T) objectIn.readObject();
                Log.d(DEBUG_TAG, "loaded " + filename + " successfully");
            } catch (FileNotFoundException fnfe) {
                // this happens a lot and shouldn't generate an error report
                Log.e(DEBUG_TAG, "file not found " + filename);
            } catch (IOException ioex) {
                logFailedToLoad(ioex);
                try {
                    if (deleteOnFail) {
                        context.deleteFile(filename);
                    }
                } catch (Exception ex) {
                    // ignore
                }
            } catch (Exception e) {
                logFailedToLoad(e);
                if (e instanceof InvalidClassException) { // serial id mismatch, will typically happen on upgrades
                    // do nothing
                } else {
                    ACRAHelper.nocrashReport(e, "failed to load " + filename + " " + e.getMessage() + " withh stack size " + stackSize);
                }
            } catch (Error e) { // NOSONAR crashing is not an option
                logFailedToLoad(e);
                ACRAHelper.nocrashReport(e, e.getMessage());
            }
        }

        /**
         * Log that we got an exception or error
         * 
         * @param e the Throwable we received
         */
        private void logFailedToLoad(Throwable e) {
            Log.e(DEBUG_TAG, "failed to load " + filename, e);
        }
    }

    /**
     * Convenience function - closes the given stream (can be any Closable), catching and logging exceptions
     * 
     * @param stream a Closeable to close
     */
    public static void close(@Nullable final Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Problem closing", e);
            }
        }
    }

    /**
     * Rename existing file in same directory if target file exists, delete Code nicked from
     * http://stackoverflow.com/users/325442/mr-bungle
     * 
     * @param context Android Context
     * @param originalFileName the original filename
     * @param newFileName the new filename
     */
    private static void rename(@NonNull Context context, @NonNull String originalFileName, @NonNull String newFileName) {
        File originalFile = context.getFileStreamPath(originalFileName);
        if (originalFile.exists()) {
            Log.d(DEBUG_TAG, "renaming " + originalFileName + " size " + originalFile.length() + " to " + newFileName);
            File newFile = new File(originalFile.getParent(), newFileName);
            if (newFile.exists()) {
                context.deleteFile(newFileName);
            }
            if (!originalFile.renameTo(newFile)) {
                Log.e(DEBUG_TAG, "renaming failed!");
            }
        }
    }

    /**
     * Exports an Exportable asynchronously, displaying a toast on success or failure
     * 
     * @param ctx context for the toast
     * @param exportable the exportable
     * @param uri Uri to write to
     */
    public static void asyncExport(@NonNull final Context ctx, @NonNull final Exportable exportable, @NonNull Uri uri) {
        new ExecutorTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void param) {
                try (OutputStream outputStream = ctx.getContentResolver().openOutputStream(uri, FileUtil.TRUNCATE_WRITE_MODE)) {
                    exportable.export(outputStream);
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Export failed - " + uri.toString());
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                try {
                    if (!result) { // NOSONAR result can't be null
                        ScreenMessage.toastTopError(ctx, R.string.toast_export_failed);
                    } else {
                        Log.i(DEBUG_TAG, "Successful export to " + uri);
                        ScreenMessage.toastTopInfo(ctx, ctx.getResources().getString(R.string.toast_export_success, uri.getPath()));
                    }
                } catch (Exception | Error ignored) { // NOSONAR crashing is not an option
                    Log.e(DEBUG_TAG, "Toast in asyncExport.onPostExecute failed with " + ignored.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Sync export an Exportable to a file named with the current date and type
     * 
     * @param ctx an optional Android Context
     * @param exportable the Exportable
     * @return if successful the path the export was to
     */
    @Nullable
    public static String export(@Nullable Context ctx, @NonNull Exportable exportable) {
        String filename = getExportFilename(exportable);
        try {
            File outfile = new File(FileUtil.getPublicDirectory(), filename);
            try (FileOutputStream fout = new FileOutputStream(outfile); OutputStream outputStream = new BufferedOutputStream(fout)) {
                exportable.export(outputStream);
                Log.i(DEBUG_TAG, "Successful export to " + filename);
                if (ctx != null) {
                    new Handler(ctx.getMainLooper())
                            .post(() -> ScreenMessage.toastTopInfo(ctx, ctx.getResources().getString(R.string.toast_export_success, filename)));
                }
                return outfile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Export failed - " + filename);
            if (ctx != null) {
                new Handler(ctx.getMainLooper()).post(() -> ScreenMessage.toastTopError(ctx, R.string.toast_export_failed));
            }
        }
        return null;
    }

    /**
     * Get an automatically generated file name
     * 
     * @param exportable the Exportable
     * @return the filename
     */
    @NonNull
    public static String getExportFilename(@NonNull Exportable exportable) {
        return DateFormatter.getFormattedString(DATE_PATTERN_EXPORT_FILE_NAME_PART) + "." + exportable.exportExtension();
    }

    public interface Exportable {
        /**
         * Exports some data to an OutputStream
         * 
         * @param outputStream the stream to write to
         * @throws Exception thrown on a write error
         */
        void export(OutputStream outputStream) throws Exception; // NOSONAR this needs to generic

        /** @return the extension to be used for exports */
        String exportExtension();
    }
}
