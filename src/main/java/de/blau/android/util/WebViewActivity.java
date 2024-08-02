package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.dialogs.ErrorAlert;

/**
 * Common code for activities that display a navigable WebView
 * 
 * @author simon
 *
 */
public abstract class WebViewActivity extends FullScreenAppCompatActivity implements OnKeyListener {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WebViewActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = WebViewActivity.class.getSimpleName().substring(0, TAG_LEN);

    protected WebView webView;
    protected Object  webViewLock = new Object();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    /**
     * Check if we have a working WebView, if not toast
     * 
     * @param activity the current activity
     * @return true if we have a WebView
     */
    protected static boolean hasWebView(@NonNull FragmentActivity activity) {
        if (!Util.supportsWebView(activity)) {
            ErrorAlert.showDialog(activity, ErrorCodes.REQUIRED_FEATURE_MISSING, "WebView");
            return false;
        }
        return true;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP && webView != null && !webView.canGoBack()) {
            exit();
            return true;
        }
        return false;
    }

    /**
     * Handle the back button/key being pressed
     */
    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {

        @Override
        public void handleOnBackPressed() {
            Log.d(DEBUG_TAG, "onBackPressed()");
            synchronized (webViewLock) {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                    return;
                }
            }
            exit();
        }
    };

    /**
     * Do what ever clean up is necessary and finish the activity
     * 
     * This version assumes that the caller wants a result
     */
    protected void exit() {
        Log.d(DEBUG_TAG, "exit");
        synchronized (webViewLock) {
            if (webView != null) {
                ViewGroup contentView = (ViewGroup) findViewById(android.R.id.content);
                contentView.removeView(webView);
                try {
                    // the below loadUrl, even though the "official" way to do
                    // it, seems to be prone to crash on some devices.
                    webView.loadUrl("about:blank"); // workaround clearView
                                                    // issues
                    webView.setVisibility(View.GONE);
                    webView.removeAllViews();
                    webView.destroy();
                    webView = null;
                } catch (Exception ex) {
                    ACRAHelper.nocrashReport(ex, ex.getMessage());
                }
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    /**
     * Load a url in the WebView or restore its previous state Does some further common configuration
     * 
     * @param savedInstanceState the state, possibly null
     * @param url the URL
     */
    protected void loadUrlOrRestore(@Nullable Bundle savedInstanceState, @NonNull String url) {
        webView.setOnKeyListener(this);
        webView.getSettings().setUserAgentString(App.getUserAgent());
        webView.getSettings().setAllowContentAccess(true);
        webView.getLayoutParams().height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        webView.getLayoutParams().width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        webView.requestFocus(View.FOCUS_DOWN);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(url);
        }
    }
}