/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package de.blau.android.net;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

/** This interceptor compresses the HTTP request body. Many webservers can't handle this! */
public class GzipRequestInterceptor implements Interceptor {

    public static final String GZIP_ENCODING           = "gzip";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        if (originalRequest.body() == null || originalRequest.header(HEADER_CONTENT_ENCODING) != null) {
            return chain.proceed(originalRequest);
        }

        Request compressedRequest = originalRequest.newBuilder().header(HEADER_CONTENT_ENCODING, GZIP_ENCODING)
                .method(originalRequest.method(), gzip(originalRequest.body())).build();
        return chain.proceed(compressedRequest);
    }

    private RequestBody gzip(final RequestBody body) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return body.contentType();
            }

            @Override
            public long contentLength() {
                return -1; // We don't know the compressed length in advance!
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                body.writeTo(gzipSink);
                gzipSink.close();
            }
        };
    }
}
