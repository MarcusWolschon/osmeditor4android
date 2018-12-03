package de.blau.android;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.FullScreenAppCompatActivity;
import de.blau.android.util.OAuthHelper;
import de.blau.android.util.Snack;
import oauth.signpost.exception.OAuthException;

/**
 * Perform OAuth authorisation of this app
 * 
 * @author simon
 *
 */
public class Authorize extends FullScreenAppCompatActivity implements ActivityResultHandler {

    private static final String DEBUG_TAG = "Authorize";

    public static final String ACTION_FINISH_OAUTH = "de.blau.android.FINISH_OAUTH";

    java.util.Map<Integer, ActivityResultHandler.Listener> activityResultListeners = new HashMap<>();

    /**
     * webview for logging in and authorizing OAuth
     */
    private WebView oAuthWebView;
    private Object  oAuthWebViewLock = new Object();

    /**
     * Start a PropertyEditor activity
     * 
     * @param activity calling activity
     * @param listener an ActivityResult.Listener to process the result or null
     */
    public static void startForResult(@NonNull Activity activity, @Nullable ActivityResultHandler.Listener listener) {
        Log.d(DEBUG_TAG, "startForResult");
        int requestCode = (int) (Math.random() * Short.MAX_VALUE);

        Log.d(DEBUG_TAG, "request code " + requestCode);
        if (listener != null) {
            if (activity instanceof ActivityResultHandler) {
                ((ActivityResultHandler) activity).setResultListener(requestCode, listener);
            } else {
                throw new ClassCastException("activity must implement ActivityResult");
            }
        }

        Intent intent = new Intent(activity, Authorize.class);
        activity.startActivityForResult(intent, requestCode);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                oAuthWebView.getSettings().setAllowContentAccess(true);
            }
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

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (!url.contains("vespucci")) {
                        // load in in this webview
                        view.loadUrl(url);
                    } else {
                        // vespucci URL
                        // or the OSM signup page which we want to open in a
                        // normal browser
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
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
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
            case ACTION_FINISH_OAUTH:
                Log.d(DEBUG_TAG, "onNewIntent calling finishOAuth");
                finishOAuth();
                return;
            }
        }
    }

    /**
     * Remove the OAuth webview
     */
    public void finishOAuth() {
        Log.d(DEBUG_TAG, "finishOAuth");
        synchronized (oAuthWebViewLock) {
            if (oAuthWebView != null) {
                ViewGroup contentView = findViewById(android.R.id.content);
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
                    // if (restart != null) {
                    // restart.onSuccess();
                    // }
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                } catch (Exception ex) {
                    ACRAHelper.nocrashReport(ex, ex.getMessage());
                }
            } else { // we want to have the controls showing in any case and before restart.onScucess is run
                // showControls();
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(DEBUG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        ActivityResultHandler.Listener listener = activityResultListeners.get(requestCode);
        if (listener != null) {
            listener.processResult(resultCode, data);
        } else {
            Log.w(DEBUG_TAG, "Received activity result without listener, code " + requestCode);
        }
    }

    @Override
    public void setResultListener(int code, Listener listener) {
        activityResultListeners.put(code, listener);
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
