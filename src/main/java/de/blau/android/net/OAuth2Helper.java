package de.blau.android.net;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.exception.NoOAuthConfigurationException;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.API;
import de.blau.android.prefs.API.Auth;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.util.ExecutorTask;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Helper class for OAuth2
 * 
 * This implements the authentication as of section 4.1 of RFC 6749 and a PKCE challenge as per RFC 7636 as currently
 * implemented by the OSM API and nothing more.
 * 
 */
public class OAuth2Helper extends OAuthHelper {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OAuth2Helper.class.getSimpleName().length());
    private static final String DEBUG_TAG = OAuth2Helper.class.getSimpleName().substring(0, TAG_LEN);

    public static final String REDIRECT_URI = "vespucci:/oauth2/";
    // instead of hardwiring these we could extract them from
    // https://www.openstreetmap.org/.well-known/oauth-authorization-server
    private static final String AUTHORIZE_PATH    = "oauth2/authorize";
    public static final String  ACCESS_TOKEN_PATH = "oauth2/token";

    public static final String  STATE_PARAM                 = "state";
    private static final String REDIRECT_URI_PARAM          = "redirect_uri";
    private static final String SCOPE_PARAM                 = "scope";
    static final String         CLIENT_ID_PARAM             = "client_id";
    private static final String RESPONSE_TYPE_PARAM         = "response_type";
    private static final String GRANT_TYPE_PARAM            = "grant_type";
    private static final String AUTHORIZATION_CODE_VALUE    = "authorization_code";
    static final String         CODE_PARAM                  = "code";
    private static final String CODE_CHALLENGE_PARAM        = "code_challenge";
    private static final String CODE_CHALLENGE_METHOD_PARAM = "code_challenge_method";
    private static final String METHOD_SHA_256_VALUE        = "S256";
    private static final String CODE_VERIFIER_PARAM         = "code_verifier";
    private static final String ERROR_DESCRIPTION_PARAM     = "error_description";
    private static final String ERROR_PARAM                 = "error";

    private static final List<String> SCOPES = Arrays.asList("read_prefs", "write_prefs", "write_api", "read_gpx", "write_gpx", "write_notes");

    private static final char[] PKCE_CHARS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
            'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '_', '~' };

    private static final String ACCESS_TOKEN_FIELD = "access_token";

    private String apiName;

    private final OAuthConfiguration configuration;

    /**
     * Construct a new helper instance
     * 
     * @param context an Android Context
     * @param apiName the API name
     * 
     * @throws OsmException if no configuration could be found for the API instance
     */
    public OAuth2Helper(@NonNull Context context, @NonNull String apiName) throws NoOAuthConfigurationException {
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(context)) {
            configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), apiName, Auth.OAUTH2);
            if (configuration != null) {
                this.apiName = apiName;
                return;
            }
            logMissingApi(apiName);
        } catch (IllegalArgumentException e) {
            Log.e(DEBUG_TAG, e.getMessage());
        }
        throw new NoOAuthConfigurationException("No matching OAuth 2 configuration found for API " + apiName);
    }

    /**
     * Generate a SHA-256 hash of the challenge String and Base64 encode it
     * 
     * See https://datatracker.ietf.org/doc/html/rfc8414
     * 
     * @param challenge the challenge string
     * @return a Base64 encoded String of the hash
     * @throws NoSuchAlgorithmException
     */
    @NonNull
    private String hashAndEncodeChallenge(@NonNull String challenge) throws NoSuchAlgorithmException {
        return Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(challenge.getBytes(Charset.forName("US-ASCII"))),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    /**
     * Get a String of random chars of length length
     * 
     * @param length the number of chars in the String
     * @return a String of random chars of length length
     */
    @NonNull
    private String createCodeVerifier(int length) {
        Random r = App.getRandom();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(PKCE_CHARS[r.nextInt(PKCE_CHARS.length)]);
        }
        return result.toString();
    }

    /**
     * Get the a authorisation url
     * 
     * @param context an Android Context
     * @return the authorisation Url as a String
     * @throws OsmException for configuration errors
     */
    @NonNull
    public String getAuthorisationUrl(Context context) throws OsmException {
        String codeVerifier = createCodeVerifier(128);
        setAccessToken(context, null, codeVerifier); // the PKCE challenge requires state, so we store it here
        try {
            URL base = new URL(configuration.getOauthUrl());
            return builderFromUrl(base).addPathSegments(AUTHORIZE_PATH).addQueryParameter(RESPONSE_TYPE_PARAM, CODE_PARAM)
                    .addQueryParameter(CLIENT_ID_PARAM, configuration.getKey()).addQueryParameter(SCOPE_PARAM, TextUtils.join(" ", SCOPES))
                    .addQueryParameter(REDIRECT_URI_PARAM, REDIRECT_URI).addQueryParameter(STATE_PARAM, apiName)
                    .addQueryParameter(CODE_CHALLENGE_METHOD_PARAM, METHOD_SHA_256_VALUE)
                    .addQueryParameter(CODE_CHALLENGE_PARAM, hashAndEncodeChallenge(codeVerifier)).build().url().toString();
        } catch (MalformedURLException | NoSuchAlgorithmException e) {
            throw new OsmException("Configuration error " + e.getMessage());
        }
    }

    @Override
    ExecutorTask<Void, Void, ?> getAccessTokenTask(Context context, Uri data, PostAsyncActionHandler handler) {
        String error = data.getQueryParameter(ERROR_PARAM);
        if (error != null) {
            String description = data.getQueryParameter(ERROR_DESCRIPTION_PARAM);
            throw new IllegalArgumentException(description == null ? error : description);
        }
        return new ExecutorTask<Void, Void, AsyncResult>() {
            @Override
            protected AsyncResult doInBackground(Void param) throws IOException {
                Log.d(DEBUG_TAG, "oAuthHandshake doInBackground");
                try (AdvancedPrefDatabase prefDb = new AdvancedPrefDatabase(context)) {
                    API api = prefDb.getCurrentAPI();
                    String code = data.getQueryParameter(CODE_PARAM);
                    RequestBody requestBody = new FormBody.Builder().add(CODE_PARAM, code).add(GRANT_TYPE_PARAM, AUTHORIZATION_CODE_VALUE)
                            .add(REDIRECT_URI_PARAM, OAuth2Helper.REDIRECT_URI).add(CLIENT_ID_PARAM, configuration.getKey())
                            .add(CODE_VERIFIER_PARAM, api.accesstokensecret).build();
                    URL base = new URL(configuration.getOauthUrl());
                    URL accessTokenUrl = builderFromUrl(base).addPathSegments(OAuth2Helper.ACCESS_TOKEN_PATH).build().url();
                    Request request = new Request.Builder().url(accessTokenUrl).post(requestBody).build();
                    OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.SECONDS).readTimeout(TIMEOUT,
                            TimeUnit.SECONDS);
                    Response result = builder.build().newCall(request).execute();
                    if (result.isSuccessful()) {
                        return readAccessToken(context, result);
                    }
                    Log.e(DEBUG_TAG, "Handshake fail " + result.code() + " " + result.message());
                    return new AsyncResult(result.code(), result.message());
                }
            }

            /**
             * Read/parse access token and save it
             * 
             * @param context an Android Context
             * @param result the Response from the API
             * @return an AsyncResult object indicating if things worked or failed
             */
            private AsyncResult readAccessToken(@NonNull Context context, @NonNull Response result) {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(result.body().byteStream(), Charset.forName(OsmXml.UTF_8)))) {
                    JsonElement root = JsonParser.parseReader(rd);
                    if (root.isJsonObject()) {
                        JsonElement accessToken = ((JsonObject) root).get(ACCESS_TOKEN_FIELD);
                        if (accessToken instanceof JsonElement) {
                            setAccessToken(context, accessToken.getAsString(), null);
                        }
                    }
                } catch (IOException | JsonSyntaxException e) {
                    Log.e(DEBUG_TAG, "Error reading response " + e.getMessage());
                    return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessage());
                }
                return new AsyncResult(ErrorCodes.OK);
            }

            @Override
            protected void onBackgroundError(Exception e) {
                Log.d(DEBUG_TAG, "oAuthHandshake onBackgroundError " + e.getMessage());
                handler.onError(new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessage()));
            }

            @Override
            protected void onPostExecute(AsyncResult result) {
                Log.d(DEBUG_TAG, "oAuthHandshake onPostExecute");
                if (ErrorCodes.OK == result.getCode()) {
                    handler.onSuccess();
                    return;
                }
                handler.onError(result);
            }
        };
    }

    /**
     * Start a new HttpUrl.Builder from an existing URL
     * 
     * @param base the existing URL
     * @return a HttpUrl.Builder instance
     */
    private HttpUrl.Builder builderFromUrl(@NonNull URL base) {
        HttpUrl.Builder builder = new HttpUrl.Builder().scheme(base.getProtocol()).host(base.getHost());
        if (base.getPort() != -1) {
            builder.port(base.getPort());
        }
        return builder;
    }
}
