package io.vespucci.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.App;
import io.vespucci.contract.Schemes;
import io.vespucci.osm.Tags;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UrlCheck {

    private static final String DEBUG_TAG = UrlCheck.class.getSimpleName().substring(0, Math.min(23, UrlCheck.class.getSimpleName().length()));

    public static final long TIMEOUT = 2000;

    public enum CheckStatus {
        HTTP, HTTPS, DOESNTEXIST, UNREACHABLE, MALFORMEDURL, INVALID
    }

    /**
     * Check if a remote url or domain can be connected and determine if reachable with https
     * 
     * @param context an Android Context
     * @param urlOrDomain a URL or a naked domain
     * @return a Result object with the result
     */
    public static Result check(@NonNull Context context, @NonNull String urlOrDomain) {
        Result result = connect(urlOrDomain, true);
        CheckStatus status = result.getStatus();
        if (status == CheckStatus.UNREACHABLE) {
            return connect(urlOrDomain, false);
        }
        return result;
    }

    public static class Result {
        private final CheckStatus status;
        private final String      url;
        private final int         code;
        private String            message;

        /**
         * Construct a new Result instance
         * 
         * @param status the status of the connection attempt
         * @param url the resulting url
         */
        public Result(@NonNull final CheckStatus status, @NonNull final String url) {
            this(status, url, 0, null);
        }

        /**
         * Construct a new Result instance
         * 
         * @param status the status of the connection attempt
         * @param url the resulting url
         * @param code the return code
         * @param message the returned message if any
         */
        public Result(@NonNull final CheckStatus status, @NonNull final String url, int code, @Nullable String message) {
            this.status = status;
            this.url = url;
            this.code = code;
            this.message = message;
        }

        /**
         * @return the status
         */
        @NonNull
        public CheckStatus getStatus() {
            return status;
        }

        /**
         * @return the url
         */
        @NonNull
        public String getUrl() {
            return url;
        }

        /**
         * Get the return code
         * 
         * @return the return code
         */
        public int getCode() {
            return code;
        }

        /**
         * Get the message returned from the connection
         * 
         * @return the message or null
         */
        @Nullable
        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return url + " " + status;
        }
    }

    /**
     * Try to connect to a remote site and return if it was possible
     * 
     * @param urlOrDomain a URL or a naked domain
     * @param https use httos if true
     * @return a Result object with the result
     */
    private static Result connect(@NonNull final String urlOrDomain, boolean https) {
        try {
            String tempDomain = urlOrDomain;
            if (urlOrDomain.toLowerCase().startsWith(Schemes.HTTP)) { // strip protocol
                URL temp = new URL(urlOrDomain);
                tempDomain = temp.getHost() + (temp.getPort() != -1 ? ":" + temp.getPort() : "") + temp.getPath();
            }
            URL url = new URL((https ? Tags.HTTPS_PREFIX : Tags.HTTP_PREFIX) + tempDomain);

            Log.d(DEBUG_TAG, "checking url for " + url.toString());

            Request request = new Request.Builder().url(url).head().build();
            OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT,
                    TimeUnit.MILLISECONDS);
            OkHttpClient client = builder.build();
            Call readCall = client.newCall(request);
            try (Response readCallResponse = readCall.execute()) {
                final int code = readCallResponse.code();
                if (code == HttpURLConnection.HTTP_OK) {
                    return new Result(https ? CheckStatus.HTTPS : CheckStatus.HTTP, url.toString());
                }
                return new Result(CheckStatus.INVALID, url.toString(), code, readCallResponse.message());
            }
        } catch (MalformedURLException | IllegalArgumentException e) {
            return new Result(CheckStatus.MALFORMEDURL, urlOrDomain);
        } catch (UnknownHostException uhe) {
            return new Result(CheckStatus.DOESNTEXIST, urlOrDomain);
        } catch (IOException e) {
            return new Result(CheckStatus.UNREACHABLE, urlOrDomain);
        }
    }
}
