package de.blau.android.net;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;
import static de.blau.android.net.HttpHeaders.ACCEPT_HEADER;

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
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.contract.MimeTypes;
import de.blau.android.exception.NoOAuthConfigurationException;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.API.Auth;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.Util;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
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
    private static final String ALLOW_SIGNUP_PARAM          = "allow_signup";
    private static final String CLIENT_SECRET_PARAM         = "client_secret";

    public static final List<String> OSM_SCOPES = Arrays.asList("read_prefs", "write_prefs", "write_api", "read_gpx", "write_gpx", "write_notes");

    private static final char[] PKCE_CHARS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
            'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '_', '~' };

    private static final String ACCESS_TOKEN_FIELD = "access_token";

    private String apiName;

    private final OAuthConfiguration configuration;

    private final SharedPreferences prefs;

    private final String redirectUri;
    private final String authorisationPath;
    private final String tokenPath;

    /**
     * Construct a new helper instance
     * 
     * @param context an Android Context
     * @param apiName the API name
     * @param authorisationPath path to add to base url for authorisation
     * @param tokenPath path to add to base url for obtaining the token
     * @param redirectUri the Uri that should be redirected too
     * @throws OsmException if no configuration could be found for the API instance
     */
    public OAuth2Helper(@NonNull Context context, @NonNull String apiName, @NonNull String authorisationPath, @NonNull String tokenPath,
            @NonNull String redirectUri) throws NoOAuthConfigurationException {
        this.redirectUri = redirectUri;
        this.authorisationPath = authorisationPath;
        this.tokenPath = tokenPath;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(context)) {
            configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), apiName, Auth.OAUTH2);
            if (configuration != null) {
                this.apiName = apiName;
                String clientSecret = configuration.getSecret();
                prefs.edit().putString(CLIENT_SECRET_PARAM, clientSecret != null ? clientSecret : null).apply();
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
    private static String createCodeVerifier(int length) {
        Random r = App.getRandom();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(PKCE_CHARS[r.nextInt(PKCE_CHARS.length)]);
        }
        return result.toString();
    }

    /**
     * Get the a authorisation url, note due to googles policies we never want to allow account creation while
     * displaying the page
     * 
     * @param context an Android Context
     * @param scopes a list of scopes
     * @return the authorisation Url as a String
     * @throws OsmException for configuration errors
     */
    @NonNull
    public String getAuthorisationUrl(@Nullable Context context, @NonNull List<String> scopes) throws OsmException {
        String codeVerifier = createCodeVerifier(128);
        prefs.edit().putString(CODE_VERIFIER_PARAM, codeVerifier).apply();
        try {
            URL base = new URL(configuration.getOauthUrl());
            return builderFromUrl(base).addPathSegment(authorisationPath).addQueryParameter(RESPONSE_TYPE_PARAM, CODE_PARAM)
                    .addQueryParameter(CLIENT_ID_PARAM, configuration.getKey()).addQueryParameter(SCOPE_PARAM, TextUtils.join(" ", scopes))
                    .addQueryParameter(REDIRECT_URI_PARAM, redirectUri).addQueryParameter(STATE_PARAM, apiName)
                    .addQueryParameter(CODE_CHALLENGE_METHOD_PARAM, METHOD_SHA_256_VALUE)
                    .addQueryParameter(CODE_CHALLENGE_PARAM, hashAndEncodeChallenge(codeVerifier)).addQueryParameter(ALLOW_SIGNUP_PARAM, "false").build().url()
                    .toString();
        } catch (MalformedURLException | NoSuchAlgorithmException e) {
            throw new OsmException("Configuration error " + e.getMessage());
        }
    }

    @Override
    ExecutorTask<Void, Void, ?> getAccessTokenTask(Context context, Uri data, Callback handler) {
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
                    String code = data.getQueryParameter(CODE_PARAM);
                    if (code == null) {
                        Log.e(DEBUG_TAG, "Handshake failed no code received");
                        return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, "Handshake failed no code received");
                    }
                    String clientSecret = prefs.getString(CLIENT_SECRET_PARAM, "");
                    FormBody.Builder bodyBuilder = new FormBody.Builder().add(CODE_PARAM, code).add(GRANT_TYPE_PARAM, AUTHORIZATION_CODE_VALUE)
                            .add(REDIRECT_URI_PARAM, redirectUri).add(CLIENT_ID_PARAM, configuration.getKey())
                            .add(CODE_VERIFIER_PARAM, prefs.getString(CODE_VERIFIER_PARAM, ""));
                    if (!Util.isEmpty(clientSecret)) {
                        Log.d(DEBUG_TAG, "adding client secret");
                        bodyBuilder.add(CLIENT_SECRET_PARAM, clientSecret);
                    }
                    RequestBody requestBody = bodyBuilder.build();
                    URL accessTokenUrl = builderFromUrl(new URL(configuration.getOauthUrl())).addPathSegments(tokenPath).build().url();

                    Request request = new Request.Builder().url(accessTokenUrl).post(requestBody).addHeader(ACCEPT_HEADER, MimeTypes.JSON).build();
                    OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.SECONDS).readTimeout(TIMEOUT,
                            TimeUnit.SECONDS);
                    Response result = builder.build().newCall(request).execute();
                    if (result.isSuccessful()) {
                        final MediaType format = result.body().contentType();
                        if (format != null && MimeTypes.APPLICATION_TYPE.equalsIgnoreCase(format.type())
                                && MimeTypes.JSON_SUBTYPE.equalsIgnoreCase(format.subtype())) {
                            return readAccessToken(result);
                        }
                        Log.e(DEBUG_TAG, "Invalid response format " + format != null ? format.toString() : "null");
                    }
                    Log.e(DEBUG_TAG, "Handshake fail " + result.code() + " " + result.message());
                    return new AsyncResult(result.code(), result.message());
                }
            }

            /**
             * Read/parse access token and save it
             * 
             * @param result the Response from the API
             * @return an AsyncResult object indicating if things worked or failed
             */
            private AsyncResult readAccessToken(@NonNull Response result) {
                Log.d(DEBUG_TAG, "readAccessToken");
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(result.body().byteStream(), Charset.forName(OsmXml.UTF_8)))) {
                    JsonElement root = JsonParser.parseReader(rd);
                    if (root.isJsonObject()) {
                        JsonElement accessToken = ((JsonObject) root).get(ACCESS_TOKEN_FIELD);
                        if (accessToken instanceof JsonElement) {
                            return new AsyncResult(ErrorCodes.OK, accessToken.getAsString());
                        }
                    }
                    return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, root.toString());
                } catch (IOException | JsonSyntaxException e) {
                    Log.e(DEBUG_TAG, "Error reading response " + e.getMessage());
                    return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessage());
                }
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
                    handler.onSuccess(result.getMessage());
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
        return HttpUrl.get(base).newBuilder();
    }
}
