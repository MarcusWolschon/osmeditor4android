package io.vespucci.net;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;

public class OAuth2Interceptor implements Interceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER               = "Bearer ";

    private final String accessToken;

    /**
     * Create an new interceptor that adds an authorization header
     * 
     * @param accessToken the value to use for the header
     */
    public OAuth2Interceptor(@NonNull String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder().header(AUTHORIZATION_HEADER, BEARER + accessToken).build());
    }
}
