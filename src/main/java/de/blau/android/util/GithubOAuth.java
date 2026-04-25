package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.blau.android.App;
import de.blau.android.contract.Github;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Handles GitHub Device Flow OAuth2
 * https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow
 */
public class GithubOAuth {

    private static final int TAG_LEN = Math.min(LOG_TAG_LEN, GithubOAuth.class.getSimpleName().length());
    private static final String DEBUG_TAG = GithubOAuth.class.getSimpleName().substring(0, TAG_LEN);

    private static final int DEFAULT_POLL_INTERVAL = 5; // seconds
    private static final int MAX_POLL_ATTEMPTS = 120; // 120 * 5s = 10 minutes max wait

    /**
     * Callback interface for Device Flow result
     */
    public interface DeviceFlowCallback {
        /**
         * Called when the user code is received from GitHub and needs to be shown to
         * the user
         */
        void onShowCode(@NonNull String userCode);

        /** Called with the access token once the user approves on GitHub */
        void onSuccess(@NonNull String accessToken);

        /** Called if an error occurs or the user does not authorize in time */
        void onError(@NonNull String reason);
    }

    /**
     * Starts the GitHub/Forgejo/Gitea Device Flow authentication process.
     * This method initiates a background task that:
     * 1. Requests a device code and user code from the host.
     * 2. Notifies the callback to show the user code.
     * 3. Polls the host until the user authorizes the request or the timeout is
     * reached.
     *
     * @param context  Android context for running background tasks.
     * @param host     The repository host (e.g., github.com, codeberg.org).
     * @param clientId The OAuth application client ID for the specified host.
     * @param callback Callback to handle UI updates and the final authentication
     *                 result.
     */
    public static void startDeviceFlow(@NonNull Context context, @NonNull String host, @NonNull String clientId,
            @NonNull DeviceFlowCallback callback) {
        new ExecutorTask<Void, String, String>() {
            private String mUserCode;

            @Override
            protected String doInBackground(Void param) {
                OkHttpClient client = App.getHttpClient().newBuilder()
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                try {
                    // Step 1: Obtain user code and device code
                    Request codeRequest = new Request.Builder()
                            .url(Github.getDeviceCodeUrl(host))
                            .addHeader("Accept", Github.ACCEPT_HEADER_JSON)
                            .post(new FormBody.Builder()
                                    .add(Github.KEY_CLIENT_ID, clientId)
                                    .add(Github.KEY_SCOPE, Github.SCOPE_PUBLIC_REPO)
                                    .build())
                            .build();

                    String deviceCode;
                    int interval = DEFAULT_POLL_INTERVAL;

                    try (Response codeResponse = client.newCall(codeRequest).execute()) {
                        if (!codeResponse.isSuccessful() || codeResponse.body() == null) {
                            return Github.ERROR_PREFIX + Github.ERROR_FAILED_DEVICE_CODE;
                        }
                        JSONObject json = new JSONObject(codeResponse.body().string());
                        mUserCode = json.getString(Github.KEY_USER_CODE);
                        deviceCode = json.getString(Github.KEY_DEVICE_CODE);
                        if (json.has(Github.KEY_INTERVAL)) {
                            interval = json.getInt(Github.KEY_INTERVAL);
                        }
                    }

                    // Step 2: Publish user code to UI
                    publishProgress(mUserCode);

                    // Step 3: Poll the server for the access token
                    for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
                        Thread.sleep(interval * 1000L);

                        Request tokenRequest = new Request.Builder()
                                .url(Github.getAccessTokenUrl(host))
                                .addHeader("Accept", Github.ACCEPT_HEADER_JSON)
                                .post(new FormBody.Builder()
                                        .add(Github.KEY_CLIENT_ID, clientId)
                                        .add(Github.KEY_DEVICE_CODE, deviceCode)
                                        .add(Github.KEY_GRANT_TYPE, Github.GRANT_TYPE_DEVICE_CODE)
                                        .build())
                                .build();

                        try (Response tokenResponse = client.newCall(tokenRequest).execute()) {
                            if (!tokenResponse.isSuccessful() || tokenResponse.body() == null) {
                                continue;
                            }
                            JSONObject tokenJson = new JSONObject(tokenResponse.body().string());
                            if (tokenJson.has(Github.KEY_ACCESS_TOKEN)) {
                                return Github.TOKEN_PREFIX + tokenJson.getString(Github.KEY_ACCESS_TOKEN);
                            }
                            String error = tokenJson.optString(Github.KEY_ERROR, "");
                            if (Github.ERROR_ACCESS_DENIED.equals(error) || Github.ERROR_EXPIRED_TOKEN.equals(error)) {
                                return Github.ERROR_PREFIX + error;
                            }
                            // Keep polling if still pending
                            if (Github.ERROR_SLOW_DOWN.equals(error) && tokenJson.has(Github.KEY_INTERVAL)) {
                                interval = tokenJson.getInt(Github.KEY_INTERVAL);
                            }
                        }
                    }
                    return Github.ERROR_PREFIX + Github.ERROR_TIMEOUT;

                } catch (IOException | JSONException | InterruptedException e) {
                    Log.e(DEBUG_TAG, "Device flow error: " + e.getMessage());
                    return Github.ERROR_PREFIX + e.getMessage();
                }
            }

            @Override
            protected void onProgress(String userCode) {
                callback.onShowCode(userCode);
            }

            @Override
            protected void onPostExecute(String result) {
                if (result != null && result.startsWith(Github.TOKEN_PREFIX)) {
                    callback.onSuccess(result.substring(Github.TOKEN_PREFIX.length()));
                } else {
                    String reason = result != null ? result.replace(Github.ERROR_PREFIX, "") : "unknown";
                    callback.onError(reason);
                }
            }
        }.execute();
    }
}
