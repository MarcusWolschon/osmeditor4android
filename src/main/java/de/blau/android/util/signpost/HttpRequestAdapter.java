package de.blau.android.util.signpost;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpEntityEnclosingRequest;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;

/** 
 * Modified to use a version of the Apache httpclient with renamed package and adapted to work with version 4.4 with working SNI support
 */ 
public class HttpRequestAdapter implements oauth.signpost.http.HttpRequest {

    private HttpUriRequest request;

    private HttpEntity entity;

    public HttpRequestAdapter(cz.msebera.android.httpclient.client.methods.HttpUriRequest request) {
        this.request = request;
        if (request instanceof HttpEntityEnclosingRequest) {
            entity = ((HttpEntityEnclosingRequest) request).getEntity();
        }
    }

    public String getMethod() {
        return request.getRequestLine().getMethod();
    }

    public String getRequestUrl() {
        return request.getURI().toString();
    }

    public void setRequestUrl(String url) {
        throw new RuntimeException(new UnsupportedOperationException());
    }

    public String getHeader(String name) {
        Header header = request.getFirstHeader(name);
        if (header == null) {
            return null;
        }
        return header.getValue();
    }

    public void setHeader(String name, String value) {
        request.setHeader(name, value);
    }

    public Map<String, String> getAllHeaders() {
        Header[] origHeaders = request.getAllHeaders();
        HashMap<String, String> headers = new HashMap<>();
        for (Header h : origHeaders) {
            headers.put(h.getName(), h.getValue());
        }
        return headers;
    }

    public String getContentType() {
        if (entity == null) {
            return null;
        }
        Header header = entity.getContentType();
        if (header == null) {
            return null;
        }
        return header.getValue();
    }

    public InputStream getMessagePayload() throws IOException {
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }

    public Object unwrap() {
        return request;
    }
}
