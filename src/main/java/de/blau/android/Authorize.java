package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmException;
import de.blau.android.net.OAuth1aHelper;
import de.blau.android.net.OAuth2Helper;
import de.blau.android.net.OAuthHelper;
import de.blau.android.osm.Server;
import de.blau.android.prefs.API.Auth;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.UpdatedWebViewClient;
import de.blau.android.util.WebViewActivity;
import oauth.signpost.exception.OAuthException;

/**
 * Perform OAuth 1/2 authorisation of this app
 * 
 * @author simon
 *
 */
public class Authorize extends WebViewActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Authorize.class.getSimpleName().length());
    private static final String DEBUG_TAG = Authorize.class.getSimpleName().substring(0, TAG_LEN);

    public static final String ACTION_FINISH_OAUTH = "de.blau.android.FINISH_OAUTH";

    public static final int REQUEST_CODE = Authorize.class.hashCode() & 0x0000FFFF;

    /**
     * Start a Authorize activity
     * 
     * @param activity calling activity
     * @param listener an ActivityResult.Listener to process the result or null
     */
    public static void startForResult(@NonNull FragmentActivity activity, @Nullable ActivityResultHandler.Listener listener) {
        Log.d(DEBUG_TAG, "startForResult");
        if (!hasWebView(activity)) {
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

    private class OAuthWebViewClient extends UpdatedWebViewClient {
        private static final String MATOMO = "matomo";

        Object   progressLock  = new Object();
        boolean  progressShown = false;
        Runnable dismiss       = () -> Progress.dismissDialog(Authorize.this, Progress.PROGRESS_OAUTH);

        @Override
        public boolean handleLoading(WebView view, Uri uri) {
            if (!Schemes.VESPUCCI.equals(uri.getScheme())) {
                return false;
            }
            // vespucci URL
            // or the OSM signup page which we want to open in a normal browser
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }

        @Override
        public WebResourceResponse handleIntercept(WebView view, Uri uri) {
            final String path = uri.getPath();
            if (path != null && path.toLowerCase().contains(MATOMO)) {
                return new WebResourceResponse(MimeTypes.TEXTPLAIN, "utf-8", new ByteArrayInputStream("".getBytes()));
            }
            return super.handleIntercept(view, uri);
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
            super.onPageFinished(view, url);
            synchronized (progressLock) {
                synchronized (webViewLock) {
                    if (progressShown && webView != null) {
                        webView.removeCallbacks(dismiss);
                        webView.postDelayed(dismiss, 500);
                    }
                }
            }
            
            // Remove navigation and sign up tab from osm.org 
            // @formatter:off
            String script = "(function() {" 
                    + "var navs = document.getElementsByTagName('nav');" 
                    + "for (let nav of navs) {" 
                    + "  nav.innerHTML = '';" 
                    + "}"
                    + "var tabs = document.getElementsByClassName('nav-item');" 
                    + "for (let tab of tabs) {" 
                    + "  tab.innerHTML = '';" 
                    + "} })();";
            // @formatter:on
            view.evaluateJavascript(script, null);
        }

        @Override
        public void receivedError(WebView view, int errorCode, String description, String failingUrl) {
            exit();
            ScreenMessage.toastTopError(view.getContext(), description);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final Preferences prefs = App.getPreferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customMain_Light);
        }
        super.onCreate(savedInstanceState);

        Server server = prefs.getServer();
        String apiName = server.getApiName();
        Auth auth = server.getAuthentication();
        Log.d(DEBUG_TAG, "oauth auth for " + apiName + " " + auth);

        String authUrl = null;
        String errorMessage = null;
        try {
            if (auth == Auth.OAUTH1A) {
                OAuth1aHelper oa = new OAuth1aHelper(this, apiName);
                authUrl = oa.getRequestToken();
            } else if (auth == Auth.OAUTH2) {
                OAuth2Helper oa = new OAuth2Helper(this, apiName);
                authUrl = oa.getAuthorisationUrl(this);
            }
        } catch (OsmException oe) {
            server.setOAuth(Auth.BASIC); // ups something went wrong turn oauth off
            errorMessage = getString(R.string.toast_no_oauth, apiName);
        } catch (OAuthException e) {
            errorMessage = OAuthHelper.getErrorMessage(this, e);
        } catch (ExecutionException e) {
            errorMessage = getString(R.string.toast_oauth_communication);
        } catch (TimeoutException e) {
            errorMessage = getString(R.string.toast_oauth_timeout);
        }
        if (authUrl == null) {
            Log.e(DEBUG_TAG, "onCreate error " + errorMessage);
            if (errorMessage != null) {
                ScreenMessage.barError(this, errorMessage);
            }
            return;
        }
        Log.d(DEBUG_TAG, "authURl " + authUrl);
        synchronized (webViewLock) {
            webView = new WebView(this);
            setContentView(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new OAuthWebViewClient());
            loadUrlOrRestore(savedInstanceState, authUrl);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_FINISH_OAUTH.equals(intent.getAction())) {
            Log.d(DEBUG_TAG, "onNewIntent calling finishOAuth");
            exit();
        }
    }
}