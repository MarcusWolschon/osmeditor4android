package io.vespucci.util;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.Arrays;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.contract.Urls;

/**
 * Class to handle some of the deprecations in Androids WebViewClient
 * 
 * @author Simon
 *
 */
public abstract class UpdatedWebViewClient extends WebViewClient {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, UpdatedWebViewClient.class.getSimpleName().length());
    private static final String DEBUG_TAG = UpdatedWebViewClient.class.getSimpleName().substring(0, TAG_LEN);

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
    @Nullable
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

    /**
     * List of domains for which we expect an untrusted error for on Android pre 7
     */
    private static final List<String> ALLOW_UNTRUSTED = Arrays.asList(Uri.parse(Urls.OSM_LOGIN).getHost(), Uri.parse(Urls.MSF_SERVER).getHost());

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        if (error.getPrimaryError() == SslError.SSL_UNTRUSTED) {
            SslCertificate cert = error.getCertificate();
            Log.w(DEBUG_TAG, "Untrusted certificate " + cert.toString());
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && ALLOW_UNTRUSTED.contains(cert.getIssuedTo().getCName())) {
                // doing a full verification of the cert chain is far too much work for an edge case
                handler.proceed();
                return;
            }
            final Context context = view.getContext();
            ScreenMessage.toastTopError(context, context.getString(R.string.toast_untrusted_certificate, cert.toString()), true);
        }
        super.onReceivedSslError(view, handler, error);
    }
}
