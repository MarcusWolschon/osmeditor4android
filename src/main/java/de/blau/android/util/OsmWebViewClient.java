package de.blau.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import androidx.annotation.Nullable;

/**
 * WebViewClient for some OSM website specifics
 * 
 * @author Simon
 *
 */
public abstract class OsmWebViewClient extends UpdatedWebViewClient {

    private static final String DEBUG_TAG = OsmWebViewClient.class.getSimpleName();

    private static final String MATOMO = "matomo";

    private Runnable dismiss = () -> dismissProgressDialog();

    private Object  progressLock  = new Object();
    private boolean progressShown = false;

    /**
     * Manipulate the response to a query
     * 
     * @param view the WebView
     * @param uri the request Uri
     * @return a WebResourceResponse or null if normal processing should continue
     */
    @Nullable
    protected WebResourceResponse handleIntercept(WebView view, Uri uri) { // NOSONAR
        // remove known trackers
        final String path = uri.getPath();
        if (path != null && path.toLowerCase().contains(MATOMO)) {
            return emptyResponse();
        }
        return null;
    }

    @Override
    public void receivedError(WebView view, int errorCode, String description, String failingUrl) {
        exit();
        ScreenMessage.toastTopError(view.getContext(), description);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        synchronized (progressLock) {
            if (!progressShown) {
                progressShown = true;
                showProgressDialog();
            }
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        synchronized (progressLock) {
            if (view != null) { // shouldn't happen but historically has
                Context context = view.getContext();
                if (context instanceof WebViewActivity) {
                    synchronized (((WebViewActivity) context).webViewLock) {
                        if (progressShown) {
                            view.removeCallbacks(dismiss);
                            view.postDelayed(dismiss, 500);
                        }
                    }
                    return;
                }
            }
            Log.e(DEBUG_TAG, "onPageFinish context not a WebViewActivity");
        }
    }

    /**
     * Exit the activity holding the WebView
     */
    protected abstract void exit();

    /**
     * Show the progress dialog
     */
    protected abstract void showProgressDialog();

    /**
     * Dismiss the progress dialog
     */
    protected abstract void dismissProgressDialog();
}
