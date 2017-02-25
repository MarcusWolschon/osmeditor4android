package de.blau.android.util.signpost;

import java.io.IOException;
import java.io.InputStream;

/** 
 * Modified to use a version of the Apache httpclient with renamed package and adapted to work with version 4.4 with working SNI support
 */ 
public class HttpResponseAdapter implements oauth.signpost.http.HttpResponse {

    private cz.msebera.android.httpclient.HttpResponse response;

    public HttpResponseAdapter(cz.msebera.android.httpclient.HttpResponse response) {
        this.response = response;
    }

    public InputStream getContent() throws IOException {
        return response.getEntity().getContent();
    }

    public int getStatusCode() throws IOException {
        return response.getStatusLine().getStatusCode();
    }

    public String getReasonPhrase() throws Exception {
        return response.getStatusLine().getReasonPhrase();
    }

    public Object unwrap() {
        return response;
    }
}
