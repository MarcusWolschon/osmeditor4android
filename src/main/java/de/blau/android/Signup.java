package de.blau.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.dialogs.Progress;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.OsmWebViewClient;
import de.blau.android.util.WebViewActivity;

/**
 * Create a new OSM account
 * 
 * Depends on the path of the page with the sign up form
 * 
 * https://master.apis.dev.openstreetmap.org/user/new
 * 
 * the format of confirmation url received in the received e-mail (see the app manifest)
 * 
 * https://master.apis.dev.openstreetmap.org/user/SimonDev1234/confirm?confirm_string=7x4FHpz7zvd1O8Z8hd70NIxVQDvIXpFH
 * 
 * and the query parameter added when the "Start mapping" button in clicked
 * 
 * @author Simon
 *
 */
public class Signup extends WebViewActivity {

    private static final String DEBUG_TAG = Signup.class.getSimpleName();

    public static final int REQUEST_CODE = Signup.class.hashCode() & 0x0000FFFF;

    private static final String EDIT_HELP_VALUE = "1";
    private static final String EDIT_HELP_PARAM = "edit_help";
    private static final String USER_NEW_PATH   = "/user/new";

    /**
     * Start a Signup activity
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

        Intent intent = new Intent(activity, Signup.class);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    private class SignupViewClient extends OsmWebViewClient {

        @Override
        public boolean handleLoading(WebView view, Uri uri) {
            final String editHelp = uri.getQueryParameter(EDIT_HELP_PARAM);
            if (editHelp != null && editHelp.equals(EDIT_HELP_VALUE)) {
                exit();
                return true;
            }
            return false;
        }

        @Override
        public void exit() {
            Signup.this.exit();
        }

        @Override
        protected void showProgressDialog() {
            Progress.showDialog(Signup.this, Progress.PROGRESS_WEBSITE);
        }

        @Override
        protected void dismissProgressDialog() {
            Progress.dismissDialog(Signup.this, Progress.PROGRESS_WEBSITE);
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        final Preferences prefs = App.getPreferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customMain_Light);
        }
        super.onCreate(savedInstanceState);

        String dataUrl = getIntent().getDataString();
        String signupUrl = dataUrl == null ? prefs.getServer().getWebsiteBaseUrl() + USER_NEW_PATH : dataUrl;

        Log.d(DEBUG_TAG, "signup for " + signupUrl);
        synchronized (webViewLock) {
            webView = new WebView(this);
            setContentView(webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new SignupViewClient());
            loadUrlOrRestore(savedInstanceState, signupUrl);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(DEBUG_TAG, "onNewIntent");
        super.onNewIntent(intent);
        loadUrlOrRestore(null, intent.getDataString());
    }
}