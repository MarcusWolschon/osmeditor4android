package de.blau.android.util;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
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

    @Override
    public void onReceive(Context ctxt, Intent intent) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId > 0) {
                DownloadManager mgr = (DownloadManager) ctxt.getSystemService(Context.DOWNLOAD_SERVICE);
                Cursor queryCursor = mgr.query(new DownloadManager.Query().setFilterById(downloadId));
                if (queryCursor != null && queryCursor.getCount() > 0) { // cancelled downloads seem to be removed from
                                                                         // the DB
                    queryCursor.moveToFirst();
                    int status = queryCursor.getInt(queryCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        String localUri = queryCursor.getString(queryCursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        if (localUri != null) {
                            Uri uri = FileUtil.contentUriToFileUri(ctxt, Uri.parse(localUri));
                            String filename = uri.getLastPathSegment();
                            if (localUri.endsWith("." + FileExtensions.MSF)) { // create an API entry
                                try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctxt)) {
                                    if (db.getReadOnlyApiId(filename) == null) {
                                        API current = db.getCurrentAPI();
                                        db.addAPI(java.util.UUID.randomUUID().toString(), filename, current.url, uri.toString(), current.notesurl, "", "",
                                                current.oauth);
                                        Snack.toastTopInfo(ctxt, ctxt.getString(R.string.toast_added_api_entry_for, filename));
                                    } else {
                                        Snack.toastTopInfo(ctxt, ctxt.getString(R.string.toast_updated, filename));
                                    }
                                }
                            } else if (localUri.endsWith("EGM96.dat")) { // gravitation model
                                (new Preferences(ctxt)).setEgmFile(uri);
                                Snack.toastTopInfo(ctxt, R.string.toast_egm_installed);
                                if (ctxt instanceof Main) {
                                    ((Main)ctxt).invalidateOptionsMenu();
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
