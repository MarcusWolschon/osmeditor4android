package de.blau.android.util;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;

/**
 * Class to handle some of the deprecations in Androids WebViewClient
 * 
 * @author Simon
 *
 */
public abstract class UpdatedWebViewClient extends WebViewClient {

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) { // nOSONAR
        return handleLoading(view, Uri.parse(url));
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return handleLoading(view, request.getUrl());
    }

    /**
     * Handle the loading of content in to the Webview (or not)
     * 
     * @param view the WEbView
     * @param uri the Uri to load
     * @return true to cancel the current load, otherwise return false
     */
    public boolean handleLoading(@NonNull WebView view, @NonNull Uri uri) { // NOSONAR
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return handleIntercept(view, Uri.parse(url));
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return handleIntercept(view, request.getUrl());
    }

    /**
     * Manipulate the response to a query
     * 
     * @param view the WebView
     * @param uri the request Uri
     * @return a WebResourceResponse or null if normal processing should continue
     */
    protected WebResourceResponse handleIntercept(WebView view, Uri uri) { // NOSONAR
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) { // NOSONAR
        receivedError(view, errorCode, description, failingUrl);
    }

    @TargetApi(android.os.Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
        // Redirect to deprecated method, so you can use it in all
        // SDK versions
        onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
    }

    /**
     * Do something with an error
     * 
     * @param view The WebView that is initiating the callback.
     * @param errorCode The error code corresponding to an ERROR_* value.
     * @param description A String describing the error.
     * @param failingUrl The url that failed to load.
     */
    protected void receivedError(WebView view, int errorCode, String description, String failingUrl) {
        // do nothing
    }
}
