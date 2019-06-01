package de.blau.android.util;

import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
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
public class DownloadActivity extends FullScreenAppCompatActivity {

    private static final String DEBUG_TAG = "MsfDownload";

    private static final String DOWNLOAD_SITE_KEY = "downloadSite";

    private WebView downloadWebView;
    private Object  downloadWebViewLock = new Object();

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
    public static void start(@NonNull Activity activity, @NonNull String downloadSite) {
        Log.d(DEBUG_TAG, "start");
        Intent intent = new Intent(activity, DownloadActivity.class);
        intent.putExtra(DOWNLOAD_SITE_KEY, downloadSite);
        activity.startActivity(intent);
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
        } else {
            url = savedInstanceState.getString(DOWNLOAD_SITE_KEY);
        }
        if (url == null) {
            Log.e(DEBUG_TAG, "No download site found");
            finish();
            return;
        }

        setContentView(R.layout.download);
        downloadWebView = (WebView) findViewById(R.id.downloadSiteWebView);

        CheckBox networks = (CheckBox) findViewById(R.id.allowAllNetworks);
        networks.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                allNetworks = isChecked;
                prefs.setAllowAllNetworks(isChecked);
            }
        });
        networks.setChecked(prefs.allowAllNetworks());

        mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerReceiver(onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        synchronized (downloadWebViewLock) {
            downloadWebView.getSettings().setUserAgentString(App.getUserAgent());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                downloadWebView.getSettings().setAllowContentAccess(true);
            }
            downloadWebView.getLayoutParams().height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            downloadWebView.getLayoutParams().width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            downloadWebView.requestFocus(View.FOCUS_DOWN);
            class DownloadWebViewClient extends WebViewClient {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Log.i(DEBUG_TAG, "Url clicked: " + url);
                    if (url.endsWith("." + FileExtensions.MSF)) {
                        Uri uri = Uri.parse(url);
                        final String filename = uri.getLastPathSegment();
                        AdvancedPrefDatabase db = new AdvancedPrefDatabase(DownloadActivity.this);
                        String apiId = db.getReadOnlyApiId(filename);
                        if (apiId != null) {
                            API[] apis = db.getAPIs(apiId);
                            if (apis.length == 1) {
                                File file = new File(Uri.parse(apis[0].readonlyurl).getPath());
                                if (file.delete()) {
                                    Log.i(DEBUG_TAG, "Deleted " + filename);
                                }
                            }
                        }
                        // Start download
                        DownloadManager.Request request = new DownloadManager.Request(uri).setAllowedOverRoaming(false).setTitle(filename)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalFilesDir(DownloadActivity.this, Environment.DIRECTORY_DOWNLOADS, filename);
                        if (!allNetworks) {
                            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                        }
                        lastDownload = mgr.enqueue(request);
                        downloadWebView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                checkStatus(mgr, lastDownload, filename);
                            }
                        }, 5000);

                        Log.i(DEBUG_TAG, "Download id: " + lastDownload);

                    } else {
                        // load in in this webview
                        view.loadUrl(url);
                    }
                    return true;
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    finishSelection();
                    Snack.toastTopError(view.getContext(), description);
                }

                @TargetApi(android.os.Build.VERSION_CODES.M)
                @Override
                public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                    // Redirect to deprecated method, so you can use it in all
                    // SDK versions
                    onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
                }
            }
            downloadWebView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (downloadWebView != null && downloadWebView.canGoBack()) {
                            downloadWebView.goBack();
                        } else {
                            finishSelection();
                        }
                        return true;
                    }
                    return false;
                }
            });
            downloadWebView.setWebViewClient(new DownloadWebViewClient());
            downloadWebView.loadUrl(url);
        }
    }

    /**
     * Remove the webview
     */
    public void finishSelection() {
        Log.d(DEBUG_TAG, "finish download selection");
        synchronized (downloadWebViewLock) {
            if (downloadWebView != null) {
                ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
                contentView.removeView(downloadWebView);
                try {
                    // the below loadUrl, even though the "official" way to do
                    // it, seems to be prone to crash on some devices.
                    downloadWebView.loadUrl("about:blank"); // workaround clearView
                    // issues
                    downloadWebView.setVisibility(View.GONE);
                    downloadWebView.removeAllViews();
                    downloadWebView.destroy();
                    downloadWebView = null;
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                } catch (Exception ex) {
                    ACRAHelper.nocrashReport(ex, ex.getMessage());
                }
            }
        }
    }

    /**
     * potentially do some special stuff for exiting
     */
    @Override
    public void onBackPressed() {
        Log.d(DEBUG_TAG, "onBackPressed()");
        synchronized (downloadWebViewLock) {
            if (downloadWebView != null && downloadWebView.canGoBack()) {
                // we are displaying a WebView and somebody might want to
                // navigate back
                downloadWebView.goBack();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putString(DOWNLOAD_SITE_KEY, url);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onNotificationClick);
    }

    BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            // nothing for now
        }
    };

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
            Log.e(DEBUG_TAG, "Download not found id: " + lastDownload);
        } else {
            queryCursor.moveToFirst();
            int status = queryCursor.getInt(queryCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_FAILED) {
                int reason = queryCursor.getInt(queryCursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                Snack.toastTopError(DownloadActivity.this, errorMessage(this, reason, filename));
            } else if (status == DownloadManager.STATUS_RUNNING) {
                Snack.toastTopInfo(this, getString(R.string.toast_download_started, filename));
            }
        }
    }

    /**
     * Get a human readable error message from the error code
     * 
     * @param ctx Android COntext
     * @param error the error code
     * @param filename the name of the file that the error applies to
     * @return a String with the message
     */
    String errorMessage(@NonNull Context ctx, int error, @NonNull String filename) {
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
