package de.blau.android;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmException;
import de.blau.android.net.OAuthHelper;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.FullScreenAppCompatActivity;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;
import oauth.signpost.exception.OAuthException;

/**
 * Perform OAuth authorisation of this app
 * 
 * @author simon
 *
 */
public class Authorize extends FullScreenAppCompatActivity {

    private static final String DEBUG_TAG = "Authorize";

    public static final String ACTION_FINISH_OAUTH = "de.blau.android.FINISH_OAUTH";

    public static final int REQUEST_CODE = Authorize.class.hashCode() & 0x0000FFFF;

    /**
     * webview for logging in and authorizing OAuth
     */
    private WebView oAuthWebView;
    private Object  oAuthWebViewLock = new Object();

    /**
     * Start a Authorize activity
     * 
     * @param activity calling activity
     * @param listener an ActivityResult.Listener to process the result or null
     */
    public static void startForResult(@NonNull FragmentActivity activity, @Nullable ActivityResultHandler.Listener listener) {
        Log.d(DEBUG_TAG, "startForResult");
        if (!Util.supportsWebView(activity)) {
            ErrorAlert.showDialog(activity, ErrorCodes.REQUIRED_FEATURE_MISSING, "WebView");
            return;
        }
        Log.d(DEBUG_TAG, "request code " + REQUEST_CODE);
        if (listener != null) {
            if (activity instanceof ActivityResultHandler) {
                ((ActivityResultHandler) activity).setResultListener(REQUEST_CODE, listener);
            } else {
                throw new ClassCastException("activity must implement ActivityResultHandler");
            }
        }

        Intent intent = new Intent(activity, Authorize.class);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customMain_Light);
        }
        super.onCreate(savedInstanceState);

        Server server = prefs.getServer();

        String url = Server.getBaseUrl(server.getReadWriteUrl());
        OAuthHelper oa;
        try {
            oa = new OAuthHelper(url);
        } catch (OsmException oe) {
            server.setOAuth(false); // ups something went wrong turn oauth off
            Snack.barError(this, R.string.toast_no_oauth);
            return;
        }
        Log.d(DEBUG_TAG, "oauth auth url " + url);

        String authUrl = null;
        String errorMessage = null;
        try {
            authUrl = oa.getRequestToken();
        } catch (OAuthException e) {
            errorMessage = OAuthHelper.getErrorMessage(this, e);
        } catch (ExecutionException e) {
            errorMessage = getString(R.string.toast_oauth_communication);
        } catch (TimeoutException e) {
            errorMessage = getString(R.string.toast_oauth_timeout);
        }
        if (authUrl == null) {
            Snack.barError(this, errorMessage);
            return;
        }
        Log.d(DEBUG_TAG, "authURl " + authUrl);
        synchronized (oAuthWebViewLock) {
            oAuthWebView = new WebView(this);
            // setting our own user agent seems to make google happy
            oAuthWebView.getSettings().setUserAgentString(App.getUserAgent());
            setContentView(oAuthWebView);
            oAuthWebView.getSettings().setJavaScriptEnabled(true);
            oAuthWebView.getSettings().setAllowContentAccess(true);
            oAuthWebView.getLayoutParams().height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            oAuthWebView.getLayoutParams().width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            oAuthWebView.requestFocus(View.FOCUS_DOWN);
            class OAuthWebViewClient extends WebViewClient {
                Object   progressLock  = new Object();
                boolean  progressShown = false;
                Runnable dismiss       = new Runnable() {
                                           @Override
                                           public void run() {
                                               Progress.dismissDialog(Authorize.this, Progress.PROGRESS_OAUTH);
                                           }
                                       };

                /**
                 * @deprecated since API 24
                 */
                @Deprecated
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (!url.contains("vespucci")) {
                        return false;
                    }
                    // vespucci URL
                    // or the OSM signup page which we want to open in a
                    // normal browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    synchronized (progressLock) {
                        if (!progressShown) {
                            progressShown = true;
                            Progress.showDialog(Authorize.this, Progress.PROGRESS_OAUTH);
                        }
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    synchronized (progressLock) {
                        synchronized (oAuthWebViewLock) {
                            if (progressShown && oAuthWebView != null) {
                                oAuthWebView.removeCallbacks(dismiss);
                                oAuthWebView.postDelayed(dismiss, 500);
                            }
                        }
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    finishOAuth();
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
            oAuthWebView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (oAuthWebView != null && oAuthWebView.canGoBack()) {
                            oAuthWebView.goBack();
                        } else {
                            finishOAuth();
                        }
                        return true;
                    }
                    return false;
                }
            });
            oAuthWebView.setWebViewClient(new OAuthWebViewClient());
            oAuthWebView.loadUrl(authUrl);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_FINISH_OAUTH.equals(intent.getAction())) {
            Log.d(DEBUG_TAG, "onNewIntent calling finishOAuth");
            finishOAuth();
        }
    }

    /**
     * Remove the OAuth webview
     */
    public void finishOAuth() {
        Log.d(DEBUG_TAG, "finishOAuth");
        synchronized (oAuthWebViewLock) {
            if (oAuthWebView != null) {
                ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
                contentView.removeView(oAuthWebView);
                try {
                    // the below loadUrl, even though the "official" way to do
                    // it, seems to be prone to crash on some devices.
                    oAuthWebView.loadUrl("about:blank"); // workaround clearView
                                                         // issues
                    oAuthWebView.setVisibility(View.GONE);
                    oAuthWebView.removeAllViews();
                    oAuthWebView.destroy();
                    oAuthWebView = null;
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
        synchronized (oAuthWebViewLock) {
            if (oAuthWebView != null && oAuthWebView.canGoBack()) {
                // we are displaying the oAuthWebView and somebody might want to
                // navigate back
                oAuthWebView.goBack();
                return;
            }
        }
        super.onBackPressed();
    }
}
