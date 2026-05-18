package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import android.content.ActivityNotFoundException;
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
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
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
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
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
        private static final String AUTH_PROVIDER_PATH = "/auth/";
        private static final String OAUTH_PROVIDER_HOST = "oauthprovider";

        Object         progressLock  = new Object();
        boolean        progressShown = false;
        Runnable       dismiss       = () -> Progress.dismissDialog(Authorize.this, Progress.PROGRESS_OAUTH);
        private String host;
        private Uri    websiteBaseUri;
        private String lastExternalUrl;
        private long   lastExternalLaunch;

        /**
         * Create a new client
         * 
         * @param host the host we are trying to authorize
         */
        OAuthWebViewClient(@NonNull Uri websiteBaseUri) {
            super();
            this.websiteBaseUri = websiteBaseUri;
            this.host = websiteBaseUri.getHost();
        }

        @Override
        public boolean handleLoading(WebView view, Uri uri) {
            if (Schemes.VESPUCCI.equals(uri.getScheme())) {
                if (OAUTH_PROVIDER_HOST.equalsIgnoreCase(uri.getHost())) {
                    launchExternalProvider(uri.getQueryParameter("provider"), uri.getQueryParameter("referer"));
                    return true;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            }
            if (isAuthProviderRedirect(uri)) {
                launchExternal(uri);
                return true;
            }
            return false;
        }

        private boolean isAuthProviderRedirect(@NonNull Uri uri) {
            String path = uri.getPath();
            String requestHost = uri.getHost();
            return requestHost != null && host != null && requestHost.equalsIgnoreCase(host) && path != null && path.startsWith(AUTH_PROVIDER_PATH);
        }

        private void launchExternal(@NonNull Uri uri) {
            Uri externalUri = toExternalAuthStartUri(uri);
            String url = externalUri.toString();
            long now = System.currentTimeMillis();
            if (url.equals(lastExternalUrl) && now - lastExternalLaunch < 1500L) {
                return;
            }
            lastExternalUrl = url;
            lastExternalLaunch = now;
            launchInCustomTabOrBrowser(externalUri);
        }

        private void launchExternalProvider(@Nullable String provider, @Nullable String referer) {
            if (provider == null || provider.isEmpty()) {
                return;
            }
            Uri.Builder builder = new Uri.Builder().scheme(websiteBaseUri.getScheme()).authority(websiteBaseUri.getAuthority()).path("/login")
                    .appendQueryParameter("preferred_auth_provider", provider);
            if (referer != null && !referer.isEmpty()) {
                builder.appendQueryParameter("referer", referer);
            }
            launchExternal(builder.build());
        }

        @NonNull
        private Uri toExternalAuthStartUri(@NonNull Uri uri) {
            if (!isAuthProviderRedirect(uri)) {
                return uri;
            }
            List<String> segments = uri.getPathSegments();
            if (segments.size() < 2 || !"auth".equals(segments.get(0))) {
                return uri;
            }
            String provider = segments.get(1);
            Uri.Builder builder = new Uri.Builder().scheme(uri.getScheme()).authority(uri.getAuthority()).path("/login")
                    .appendQueryParameter("preferred_auth_provider", provider);
            String referer = uri.getQueryParameter("referer");
            if (referer != null) {
                builder.appendQueryParameter("referer", referer);
            }
            return builder.build();
        }

        @Override
        public WebResourceResponse handleIntercept(WebView view, Uri uri) {
            final String path = uri.getPath();
            final String requestHost = uri.getHost();
            if ((path != null && path.toLowerCase().contains(MATOMO)) || (requestHost != null && requestHost.toLowerCase().contains(MATOMO))) {
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
            lastExternalUrl = null;
            lastExternalLaunch = 0L;
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
                    + "for (var i = navs.length - 1; i >= 0; i--) {"
                    + "  navs[i].remove();"
                    + "}"
                    + "var signups = document.querySelectorAll('a[href*=\"/user/new\"], a[href*=\"signup\"]');"
                    + "for (var j = 0; j < signups.length; j++) {"
                    + "  var tab = signups[j].closest('li,div');"
                    + "  if (tab) {"
                    + "    tab.remove();"
                    + "  } else {"
                    + "    signups[j].style.display = 'none';"
                    + "  }"
                    + "}"
                    + "var authButtons = document.querySelectorAll('a.auth_button[href*=\\\"/auth/\\\"]');"
                    + "for (var k = 0; k < authButtons.length; k++) {"
                    + "  var button = authButtons[k];"
                    + "  if (button.dataset.vespucciHooked === '1') {"
                    + "    continue;"
                    + "  }"
                    + "  button.dataset.vespucciHooked = '1';"
                    + "  button.addEventListener('click', function(e) {"
                    + "    e.preventDefault();"
                    + "    e.stopPropagation();"
                    + "    if (e.stopImmediatePropagation) {"
                    + "      e.stopImmediatePropagation();"
                    + "    }"
                    + "    try {"
                    + "      var href = new URL(this.href, window.location.origin);"
                    + "      var match = href.pathname.match(/\\/auth\\/([^\\/?#]+)/);"
                    + "      if (!match) {"
                    + "        window.location.href = this.href;"
                    + "        return;"
                    + "      }"
                    + "      var provider = match[1];"
                    + "      var refererField = document.getElementById('referer');"
                    + "      var referer = refererField ? refererField.value : (href.searchParams.get('referer') || '');"
                    + "      window.location.href = 'vespucci://oauthprovider?provider=' + encodeURIComponent(provider) + '&referer=' + encodeURIComponent(referer);"
                    + "    } catch (ex) {"
                    + "      window.location.href = this.href;"
                    + "    }"
                    + "  }, true);"
                    + "}"
                    + "})();";
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
                List<String> configNames = new ArrayList<>();
                for (OAuthConfiguration configuration : KeyDatabaseHelper.getOAuthConfigurations(keyDatabase.getReadableDatabase(), auth)) {
                    configNames.add(configuration.getName());
                }
                ThemeUtils.getAlertDialogBuilder(this).setTitle(R.string.choose_oauth_config)
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
        Log.d(DEBUG_TAG, "authUrl " + authUrl);
        synchronized (webViewLock) {
            webView = new WebView(this);
            setContentView(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            Uri uri = Uri.parse(server.getWebsiteBaseUrl());
            webView.setWebViewClient(new OAuthWebViewClient(uri));
            loadUrlOrRestore(savedInstanceState, authUrl);
            ViewGroupCompat.installCompatInsetsDispatch(webView);
            ViewCompat.setOnApplyWindowInsetsListener(webView, onApplyWindowInsetslistener);
        }
    }

    private void launchInCustomTabOrBrowser(@NonNull Uri authUri) {
        String customTabsPackage = CustomTabsClient.getPackageName(this, Collections.emptyList());
        try {
            if (customTabsPackage != null) {
                CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
                customTabsIntent.intent.setPackage(customTabsPackage);
                customTabsIntent.launchUrl(this, authUri);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, authUri));
            }
        } catch (ActivityNotFoundException e) {
            Log.e(DEBUG_TAG, "No browser available for OAuth " + authUri + " " + e.getMessage());
            ScreenMessage.barError(this, getString(R.string.toast_oauth_communication));
            finish();
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
    protected void exit() {
        if (webView == null) {
            setResult(RESULT_OK, new Intent());
            finish();
            return;
        }
        super.exit();
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