package de.blau.android.util;

import java.io.ByteArrayInputStream;
import java.io.File;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Paths;
import de.blau.android.prefs.API;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;

/**
 * Open a webview on a site with files to download and download them on click with the DownloadManager
 * 
 * Currently only supports MSF files that it post download automatically configures as a read only source
 * 
 * Note: the broadcast received for completed downloads is configured in the manifest
 * 
 * @author simon
 *
 */
public class DownloadActivity extends WebViewActivity {

    private static final String DEBUG_TAG = DownloadActivity.class.getSimpleName().substring(0, Math.min(23, DownloadActivity.class.getSimpleName().length()));

    static final String DOWNLOAD_SITE_KEY = "downloadSite";

    private DownloadManager mgr          = null;
    private long            lastDownload = -1L;
    private String          url          = null;
    private boolean         allNetworks  = false;

    /**
     * Start a Download activity
     * 
     * @param activity calling activity
     * @param downloadSite the site with the files
     */
    public static void start(@NonNull FragmentActivity activity, @NonNull String downloadSite) {
        Log.d(DEBUG_TAG, "start");
        if (!hasWebView(activity)) {
            return;
        }
        Intent intent = new Intent(activity, DownloadActivity.class);
        intent.putExtra(DOWNLOAD_SITE_KEY, downloadSite);
        activity.startActivity(intent);
    }

    private final class DownloadWebViewClient extends UpdatedWebViewClient {

        private static final String FAVICON = "favicon.ico";

        @Override
        public boolean handleLoading(@NonNull WebView view, @NonNull Uri uri) {
            Log.i(DEBUG_TAG, "Url clicked: " + uri.toString());
            final String filename = uri.getLastPathSegment();
            if (FileExtensions.MSF.equals(FileUtil.getExtension(filename))) {
                try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(DownloadActivity.this)) {
                    String apiId = db.getReadOnlyApiId(filename);
                    if (apiId != null) {
                        API[] apis = db.getAPIs(apiId);
                        if (apis.length == 1) {
                            File file = new File(Uri.parse(apis[0].readonlyurl).getPath());
                            if (file.delete()) { // NOSONAR requires API 26
                                Log.i(DEBUG_TAG, "Deleted " + filename);
                            }
                        }
                    }
                }
                // Start download
                @SuppressWarnings("deprecation")
                DownloadManager.Request request = new DownloadManager.Request(uri).setAllowedOverRoaming(false).setTitle(filename)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Paths.DIRECTORY_PATH_VESPUCCI + Paths.DELIMITER + filename)
                        .setVisibleInDownloadsUi(true);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                if (!allNetworks) {
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                }
                lastDownload = mgr.enqueue(request);
                webView.postDelayed(() -> checkStatus(mgr, lastDownload, filename), 5000);

                Log.i(DEBUG_TAG, "Download id: " + lastDownload);
                return true;
            }
            return false;
        }

        @Override
        protected WebResourceResponse handleIntercept(WebView view, Uri uri) {
            if (FAVICON.equals(uri.getLastPathSegment())) {
                return new WebResourceResponse(MimeTypes.PNG, "utf-8", new ByteArrayInputStream("".getBytes()));
            }
            return super.handleIntercept(view, uri);
        }

        @Override
        public void receivedError(WebView view, int errorCode, String description, String failingUrl) {
            exit();
            ScreenMessage.toastTopError(view.getContext(), description);
        }

        /**
         * Check the status of a download and if failed toast a message
         * 
         * @param mgr a DownloadManager instance
         * @param id the download id
         * @param filename the name of the file we are downloading
         */
        private void checkStatus(@NonNull final DownloadManager mgr, final long id, @NonNull final String filename) {
            Cursor queryCursor = mgr.query(new DownloadManager.Query().setFilterById(id));
            if (queryCursor == null) {
                Log.e(DEBUG_TAG, "Download not found id: " + id);
            } else {
                queryCursor.moveToFirst();
                try {
                    int status = queryCursor.getInt(queryCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_FAILED) {
                        int reason = queryCursor.getInt(queryCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                        ScreenMessage.toastTopError(DownloadActivity.this, errorMessage(DownloadActivity.this, reason, filename));
                    } else if (status == DownloadManager.STATUS_RUNNING) {
                        ScreenMessage.toastTopInfo(DownloadActivity.this, getString(R.string.toast_download_started, filename));
                    }
                } catch (IllegalArgumentException iaex) {
                    Log.e(DEBUG_TAG, iaex.getMessage());
                    ScreenMessage.toastTopError(DownloadActivity.this, errorMessage(DownloadActivity.this, DownloadManager.ERROR_UNKNOWN, filename));
                }
            }
        }

        /**
         * Get a human readable error message from the error code
         * 
         * @param ctx Android Context
         * @param error the error code
         * @param filename the name of the file that the error applies to
         * @return a String with the message
         */
        private String errorMessage(@NonNull Context ctx, int error, @NonNull String filename) {
            int res;
            switch (error) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                res = R.string.toast_cannot_resume_download_of;
                break;
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                res = R.string.toast_device_not_found_for;
                break;
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                res = R.string.toast_file_already_exists;
                break;
            case DownloadManager.ERROR_FILE_ERROR:
                res = R.string.toast_file_error_for;
                break;
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                res = R.string.toast_http_data_error_for;
                break;
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                res = R.string.toast_error_insufficient_space_for;
                break;
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                res = R.string.toast_error_too_many_redirects_for;
                break;
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                res = R.string.toast_error_too_unhandled_http_code_for;
                break;
            case DownloadManager.ERROR_UNKNOWN:
            default:
                res = R.string.toast_unknown_error_for;
            }
            return ctx.getString(res, filename);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customLight);
        }

        super.onCreate(savedInstanceState);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setTitle(R.string.download_files_title);

        if (savedInstanceState == null) {
            url = getIntent().getStringExtra(DOWNLOAD_SITE_KEY);
        }

        setContentView(R.layout.download);
        webView = (WebView) findViewById(R.id.downloadSiteWebView);

        CheckBox networks = (CheckBox) findViewById(R.id.allowAllNetworks);
        networks.setOnCheckedChangeListener((buttonView, isChecked) -> {
            allNetworks = isChecked;
            prefs.setAllowAllNetworks(isChecked);
        });
        networks.setChecked(prefs.allowAllNetworks());

        mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED), RECEIVER_EXPORTED);
        } else {
            registerReceiver(onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        }
        synchronized (webViewLock) {
            webView.setWebViewClient(new DownloadWebViewClient());
            loadUrlOrRestore(savedInstanceState, url);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onNotificationClick);
    }

    private BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            // nothing for now
        }
    };
}
