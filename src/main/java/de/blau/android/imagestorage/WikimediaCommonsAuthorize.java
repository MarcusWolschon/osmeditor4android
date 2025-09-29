package de.blau.android.imagestorage;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.prefs.ImageStorageConfiguration;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.UpdatedWebViewClient;
import de.blau.android.util.Util;
import de.blau.android.util.WebViewActivity;

public class WikimediaCommonsAuthorize extends WebViewActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WikimediaCommonsAuthorize.class.getSimpleName().length());
    private static final String DEBUG_TAG = WikimediaCommonsAuthorize.class.getSimpleName().substring(0, TAG_LEN);

    static final String CONFIGURATION_KEY    = "configuration";
    static final String REGISTRATION_URL_KEY = "registration";

    private ImageStorageConfiguration configuration;
    private String                    registrationUrl = Urls.WIKIMEDIA_REGISTRATION_URL;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final Preferences prefs = App.getPreferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customMain_Light);
        }
        super.onCreate(savedInstanceState);

        synchronized (webViewLock) {
            webView = new WebView(this);
            setContentView(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new AuthWebViewClient());
            final Intent intent = getIntent();
            configuration = Util.getSerializableExtra(intent, CONFIGURATION_KEY, ImageStorageConfiguration.class);
            String temp = intent.getStringExtra(REGISTRATION_URL_KEY);
            if (temp != null) {
                // this is mainly used for testing
                registrationUrl = temp;
            }
            Log.d(DEBUG_TAG, "Registration URL " + registrationUrl);
            loadUrlOrRestore(savedInstanceState, registrationUrl);
            ViewGroupCompat.installCompatInsetsDispatch(webView);
            ViewCompat.setOnApplyWindowInsetsListener(webView, onApplyWindowInsetslistener);
        }
    }

    /**
     * @param registrationUrl the registrationUrl to set
     */
    void setRegistrationUrl(String registrationUrl) {
        this.registrationUrl = registrationUrl;
    }

    private class AuthWebViewClient extends UpdatedWebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(DEBUG_TAG, "onPageFinished URL " + url);
            if (!registrationUrl.equals(url)) {
                return;
            }

            // @formatter:off
            String script = 
                    "(function() {" 
                            + "const match = document.documentElement.innerHTML.match(/Access token<\\/dt>.<dd><span[^>]*.([A-Za-z0-9-_\\.]*)/si);" 
                            + "return match ? match[1] : null;" 
                        + "} "
                  + ")();";
            // @formatter:on
            view.evaluateJavascript(script, (String token) -> {
                if (token != null && !"null".equals(token)) { // JS grrr
                    token = token.replace("\"", "");
                    Log.d(DEBUG_TAG, "Extracted \"" + token + "\"");
                    // this should only be set if auth was successful
                    try (KeyDatabaseHelper kdb = new KeyDatabaseHelper(WikimediaCommonsAuthorize.this); SQLiteDatabase db = kdb.getWritableDatabase()) {
                        KeyDatabaseHelper.replaceOrDeleteKey(db, configuration.id, KeyDatabaseHelper.EntryType.WIKIMEDIA_COMMONS_KEY, token, false, true, null,
                                null);
                        Log.d(DEBUG_TAG, "Authorization successful");
                        ScreenMessage.toastTopInfo(WikimediaCommonsAuthorize.this, R.string.toast_authorisation_successful);
                        view.postDelayed(() -> exit(), 5000);
                    }
                    return;
                }
                Log.e(DEBUG_TAG, "Access token not found");
            });
        }

        @Override
        public void receivedError(WebView view, int errorCode, String description, String failingUrl) {
            exit();
            ScreenMessage.toastTopError(view.getContext(), description);
        }
    }
}
