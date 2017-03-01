/*
 * Copyright (c) 2009 Matthias Kaeppler Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package de.blau.android.util.signpost;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import oauth.signpost.AbstractOAuthProvider;
import oauth.signpost.http.HttpRequest;

/**
 * This implementation uses the Apache Commons {@link HttpClient} 4.x HTTP
 * implementation to fetch OAuth tokens from a service provider. Android users
 * should use this provider implementation in favor of the default one, since
 * the latter is known to cause problems with Android's Apache Harmony
 * underpinnings.
 * 
 * @author Matthias Kaeppler
 *  
 * Modified to use a version of the Apache httpclient with renamed package and adapted to work with version 4.4 with working SNI support
 * 
 */
public class CommonsHttpOAuthProvider extends AbstractOAuthProvider {

    private static final long serialVersionUID = 1L;

    private transient HttpClient httpClient;

    public CommonsHttpOAuthProvider(String requestTokenEndpointUrl, String accessTokenEndpointUrl,
            String authorizationWebsiteUrl) {
        super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
        // this.httpClient = new DefaultHttpClient();
        this.httpClient = HttpClients.createDefault();
    }

    public CommonsHttpOAuthProvider(String requestTokenEndpointUrl, String accessTokenEndpointUrl,
            String authorizationWebsiteUrl, HttpClient httpClient) {
        super(requestTokenEndpointUrl, accessTokenEndpointUrl, authorizationWebsiteUrl);
        this.httpClient = httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    protected HttpRequest createRequest(String endpointUrl) throws Exception {
        HttpPost request = new HttpPost(endpointUrl);
        return new HttpRequestAdapter(request);
    }

    @Override
    protected oauth.signpost.http.HttpResponse sendRequest(HttpRequest request) throws Exception {
        HttpResponse response = httpClient.execute((HttpUriRequest) request.unwrap());
        return new HttpResponseAdapter(response);
    }

    @Override
    protected void closeConnection(HttpRequest request, oauth.signpost.http.HttpResponse response)
            throws Exception {
        if (response != null) {
            HttpEntity entity = ((HttpResponse) response.unwrap()).getEntity();
            if (entity != null) {
                try {
                    // free the connection
                    entity.consumeContent();
                } catch (IOException e) {
                    // this means HTTP keep-alive is not possible
                    e.printStackTrace();
                }
            }
        }
    }
}
