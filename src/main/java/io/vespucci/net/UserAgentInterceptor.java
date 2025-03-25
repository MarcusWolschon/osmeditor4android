package io.vespucci.net;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Version;

public class UserAgentInterceptor implements Interceptor {

    public static final String USER_AGENT_HEADER = "User-Agent";

    private final String userAgent;
    private final String defaultUserAgent;

    /**
     * Create an new interceptor that adds an user-agent header
     * 
     * @param userAgent the value to use for the header
     */
    public UserAgentInterceptor(@NonNull String userAgent) {
        this.userAgent = userAgent;
        this.defaultUserAgent = Version.userAgent();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (defaultUserAgent.equals(request.header(USER_AGENT_HEADER))) {
            request = request.newBuilder().header(USER_AGENT_HEADER, userAgent).build();
        }
        return chain.proceed(request);
    }
}
