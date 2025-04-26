package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.NoOAuthConfigurationException;
import de.blau.android.exception.OsmException;
import de.blau.android.net.OAuth1aHelper;
import de.blau.android.net.OAuth2Helper;
import de.blau.android.net.OAuthHelper;
import de.blau.android.net.OAuthHelper.OAuthConfiguration;
import de.blau.android.osm.Server;
import de.blau.android.prefs.API.Auth;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
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

        Object         progressLock  = new Object();
        boolean        progressShown = false;
        Runnable       dismiss       = () -> Progress.dismissDialog(Authorize.this, Progress.PROGRESS_OAUTH);
        private String host;

        /**
         * Create a new client
         * 
         * @param host the host we are trying to authorize
         */
        OAuthWebViewClient(@NonNull String host) {
            super();
            this.host = host;
        }

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
                    Progress.showDialog(Authorize.this, Progress.PROGRESS_OAUTH, host, null);
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
        String errorMessage = null;
        try {
            openWebView(savedInstanceState, server, apiName, auth);
        } catch (NoOAuthConfigurationException nex) {
            try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(this)) {
                // get list of possible configs
                List<String> configNames = new ArrayList<>();
                for (OAuthConfiguration configuration : KeyDatabaseHelper.getOAuthConfigurations(keyDatabase.getReadableDatabase(), auth)) {
                    configNames.add(configuration.getName());
                }
                new AlertDialog.Builder(this).setTitle(R.string.choose_oauth_config)
                        .setItems(configNames.toArray(new String[0]), (DialogInterface dialog, int which) -> {
                            try (KeyDatabaseHelper keyDatabase2 = new KeyDatabaseHelper(this)) {
                                KeyDatabaseHelper.copyKey(keyDatabase2.getWritableDatabase(), configNames.get(which),
                                        auth == Auth.OAUTH1A ? EntryType.API_OAUTH1_KEY : EntryType.API_OAUTH2_KEY, apiName);
                            }
                            try {
                                openWebView(savedInstanceState, server, apiName, auth);
                            } catch (OsmException | NoOAuthConfigurationException | OAuthException | TimeoutException | ExecutionException e) {
                                String message = getString(R.string.toast_no_oauth, apiName);
                                Log.e(DEBUG_TAG, "still no config found " + message);
                                new Handler(Looper.getMainLooper()).post(() -> ScreenMessage.barError(Authorize.this, message));
                                finish();
                            }
                        }).setNegativeButton(R.string.abort, (DialogInterface dialog, int which) -> finish()).create().show();
                return;
            }

        } catch (OsmException oe) {
            errorMessage = getString(R.string.toast_no_oauth, apiName);
        } catch (OAuthException e) {
            errorMessage = OAuthHelper.getErrorMessage(this, e);
        } catch (ExecutionException e) {
            errorMessage = getString(R.string.toast_oauth_communication);
        } catch (TimeoutException e) {
            errorMessage = getString(R.string.toast_oauth_timeout);
        }
        Log.e(DEBUG_TAG, "onCreate error " + errorMessage);
        if (errorMessage != null) {
            ScreenMessage.barError(this, errorMessage);
            finish();
        }
    }

    /**
     * Open the webview
     * 
     * @param savedInstanceState sany saved state
     * @param server the Server instance
     * @param auth Auth type
     * @throws OsmException
     * @throws OAuthException
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws NoOAuthConfigurationException
     */
    private void openWebView(@Nullable final Bundle savedInstanceState, @NonNull Server server, @NonNull String apiName, @NonNull Auth auth)
            throws OsmException, OAuthException, TimeoutException, ExecutionException, NoOAuthConfigurationException {
        String authUrl = null;
        if (auth == Auth.OAUTH1A) {
            OAuth1aHelper oa = new OAuth1aHelper(this, apiName);
            authUrl = oa.getRequestToken();
        } else if (auth == Auth.OAUTH2) {
            OAuth2Helper oa = new OAuth2Helper(this, apiName);
            authUrl = oa.getAuthorisationUrl(this);
        }
        if (authUrl == null) {
            throw new OsmException("authUrl is null");
        }
        Log.d(DEBUG_TAG, "authURl " + authUrl);
        synchronized (webViewLock) {
            webView = new WebView(this);
            setContentView(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            Uri uri = Uri.parse(server.getWebsiteBaseUrl());
            webView.setWebViewClient(new OAuthWebViewClient(uri.getHost()));
            loadUrlOrRestore(savedInstanceState, authUrl);
            ViewGroupCompat.installCompatInsetsDispatch(webView);
            ViewCompat.setOnApplyWindowInsetsListener(webView, onApplyWindowInsetslistener);
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

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        // remove any cookies, in particular session cookies, this might seem to be overkill, but there is no per cookie
        // method
        final CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies((Boolean b) -> cookieManager.flush());
        super.onDestroy();
    }
}