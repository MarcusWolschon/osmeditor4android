package de.blau.android.net;

import java.io.IOException;

import android.support.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class UserAgentInterceptor implements Interceptor {

    private final String userAgent;

    /**
     * Create an new interceptor that adds an user-agent header
     * 
     * @param userAgent the value to use for the header
     */
    public UserAgentInterceptor(@NonNull String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder().header("User-Agent", userAgent).build();
        return chain.proceed(requestWithUserAgent);
    }

}
