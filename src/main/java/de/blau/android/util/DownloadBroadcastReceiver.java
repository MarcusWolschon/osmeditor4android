package de.blau.android.util;

import java.io.File;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Urls;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

/**
 * Receiver for completed downloads
 * 
 * @author simon
 *
 */
public class DownloadBroadcastReceiver extends BroadcastReceiver {

    private static final String DEBUG_TAG = DownloadBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context ctxt, Intent intent) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId > 0) {
                DownloadManager mgr = (DownloadManager) ctxt.getSystemService(Context.DOWNLOAD_SERVICE);
                Cursor queryCursor = mgr.query(new DownloadManager.Query().setFilterById(downloadId));
                // cancelled downloads seem to be removed from the DB
                if (queryCursor == null || queryCursor.getCount() == 0) {
                    return;
                }
                queryCursor.moveToFirst();
                try {
                    int status = queryCursor.getInt(queryCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        processDownload(ctxt, queryCursor.getString(queryCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)));
                    }
                } catch (IllegalArgumentException iaex) {
                    Log.e(DEBUG_TAG, iaex.getMessage());
                    Snack.toastTopError(ctxt, iaex.getMessage());
                }
            }
        }

    }

    /**
     * Process a successful download
     * 
     * @param ctxt Android Context
     * @param localUri uri for the downloaded file
     */
    private void processDownload(@NonNull Context ctxt, @Nullable String localUri) {
        if (localUri != null) {
            Uri uri = FileUtil.contentUriToFileUri(ctxt, Uri.parse(localUri));
            String filename = uri.getLastPathSegment();
            if (localUri.endsWith("." + FileExtensions.MSF)) {
                processMSF(ctxt, uri, filename);
            } else {
                final String egmFilename = Uri.parse(Urls.EGM96).getLastPathSegment();
                if (localUri.endsWith(egmFilename + "." + FileExtensions.TEMP)) {
                    processEGM(ctxt, uri, egmFilename);
                }
            }
        }
    }

    /**
     * Process the temp EGM file
     * 
     * @param ctxt Android Context
     * @param uri file uri of the downloaded file
     * @param egmFilename target filename
     */
    private void processEGM(@NonNull Context ctxt, @NonNull Uri uri, @NonNull final String egmFilename) {
        // rename the file to avoid the file manager from deleting it
        File tempFile = new File(ContentResolverUtil.getPath(ctxt, uri));
        File targetFile = new File(tempFile.getParent(), egmFilename);
        if (targetFile.exists()) {
            targetFile.delete(); // NOSONAR
        }
        if (!tempFile.renameTo(targetFile)) {
            Log.e(DEBUG_TAG, "renaming failed!");
            return;
        }
        (new Preferences(ctxt)).setEgmFile(Uri.parse(targetFile.toURI().toString()));
        Snack.toastTopInfo(ctxt, R.string.toast_egm_installed);
        if (ctxt instanceof Main) {
            ((Main) ctxt).invalidateOptionsMenu();
        }
    }

    /**
     * Create an API entry
     * 
     * @param ctxt Android Context
     * @param uri file uri of the downloaded file
     * @param filename target filename
     */
    private void processMSF(@NonNull Context ctxt, @NonNull Uri uri, @NonNull String filename) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctxt)) {
            if (db.getReadOnlyApiId(filename) == null) {
                API current = db.getCurrentAPI();
                db.addAPI(java.util.UUID.randomUUID().toString(), filename, current.url, uri.toString(), current.notesurl, "", "", current.oauth);
                Snack.toastTopInfo(ctxt, ctxt.getString(R.string.toast_added_api_entry_for, filename));
            } else {
                Snack.toastTopInfo(ctxt, ctxt.getString(R.string.toast_updated, filename));
            }
        }
    }
}
