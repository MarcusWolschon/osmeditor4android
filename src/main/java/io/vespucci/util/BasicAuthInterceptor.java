package io.vespucci.util;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BasicAuthInterceptor implements Interceptor {

    private String credentials;

    /**
     * Construct a new interceptor
     * 
     * @param user the user name
     * @param password the password
     */
    public BasicAuthInterceptor(@NonNull String user, @NonNull String password) {
        this.credentials = Credentials.basic(user, password);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder().header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

}
