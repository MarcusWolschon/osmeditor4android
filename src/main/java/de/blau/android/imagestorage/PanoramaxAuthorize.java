package de.blau.android.imagestorage;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.UpdatedWebViewClient;
import de.blau.android.util.Util;
import de.blau.android.util.WebViewActivity;

public class PanoramaxAuthorize extends WebViewActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PanoramaxAuthorize.class.getSimpleName().length());
    private static final String DEBUG_TAG = PanoramaxAuthorize.class.getSimpleName().substring(0, TAG_LEN);

    static final String URL_KEY = "url";

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
            final String url = Util.getSerializableExtra(getIntent(), URL_KEY, String.class);
            Log.d(DEBUG_TAG, "Claiming key with url " + url);
            loadUrlOrRestore(savedInstanceState, url);
            ViewGroupCompat.installCompatInsetsDispatch(webView);
            ViewCompat.setOnApplyWindowInsetsListener(webView, onApplyWindowInsetslistener);
        }
    }

    private class AuthWebViewClient extends UpdatedWebViewClient {

        private static final String TOKEN_ACCEPTED = "token-accepted";

        @Override
        public boolean handleLoading(WebView view, Uri uri) {
            Log.d(DEBUG_TAG, "handleLoading " + uri.toString());
            if (uri.getPath().endsWith(TOKEN_ACCEPTED)) {
                Log.d(DEBUG_TAG, "Authorization successful");
                ScreenMessage.toastTopInfo(view.getContext(), R.string.toast_authorisation_successful);
                view.postDelayed(() -> exit(), 5000);
            }
            return false;
        }

        @Override
        public void receivedError(WebView view, int errorCode, String description, String failingUrl) {
            exit();
            ScreenMessage.toastTopError(view.getContext(), description);
        }
    }
}
