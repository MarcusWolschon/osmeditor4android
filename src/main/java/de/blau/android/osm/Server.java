package de.blau.android.osm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Authorize;
import de.blau.android.ErrorCodes;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Schemes;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.net.GzipRequestInterceptor;
import de.blau.android.net.OAuth1aHelper;
import de.blau.android.net.OAuth2Interceptor;
import de.blau.android.prefs.API;
import de.blau.android.prefs.API.Auth;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.services.util.StreamUtils;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteComment;
import de.blau.android.util.BasicAuthInterceptor;
import de.blau.android.util.ScreenMessage;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;
import se.akerfeldt.okhttp.signpost.SigningInterceptor;

/**
 * @author mb
 * @author Simon
 */
public class Server {

    private static final String DEBUG_TAG = Server.class.getSimpleName().substring(0, Math.min(23, Server.class.getSimpleName().length()));

    /**
     * <a href="http://wiki.openstreetmap.org/wiki/API">API</a>-Version.
     */
    private static final String API_VERSION = "0.6";

    // maximum number of elements retrievable in one multifetch call, essentially nothing is guaranteed, making this
    // very very iffy, JOSM uses 200 which is likely derived from the maximum length supported in MS IE/Edge, if the
    // powers that be decide to lower this, at least it won't be just us complaining
    //
    // see https://wiki.openstreetmap.org/wiki/API_v0.6#Multi_fetch:_GET_/api/0.6/[nodes|ways|relations]?#parameters
    public static final int MULTI_FETCH_MAX_ELEMENTS = 200;

    private static final String VERSION_KEY         = "version";
    private static final String GENERATOR_KEY       = "generator";
    private static final String OLD_ID_ATTR         = "old_id";
    private static final String NEW_VERSION_ATTR    = "new_version";
    private static final String NEW_ID_ATTR         = "new_id";
    private static final String DIFF_RESULT_ELEMENT = "diffResult";

    private static final String HTTP_PUT    = "PUT";
    static final String         HTTP_POST   = "POST";
    static final String         HTTP_GET    = "GET";
    private static final String HTTP_DELETE = "DELETE";

    // single element get modes
    public static final String MODE_FULL      = "full";
    public static final String MODE_RELATIONS = "relations";
    public static final String MODE_WAYS      = "ways";

    private static final MediaType TEXTXML = MediaType.parse(MimeTypes.TEXTXML);

    /**
     * Default timeout for connections in milliseconds.
     */
    public static final int DEFAULT_TIMEOUT = 45 * 1000;

    /**
     * Name of the API entry used for this instance
     */
    private final String name;
    /**
     * Location of OSM API
     */
    private final String serverURL;

    /**
     * Location of optional read only OSM API
     */
    private String readonlyURL;

    /**
     * MapSplit tiled OSM data source
     */
    private final MBTileProviderDataBase mapSplitSource;

    /**
     * Location of optional notes OSM API
     */
    private final String notesURL;

    /**
     * username for write-access on the server.
     */
    private final String username;

    /**
     * password for write-access on the server.
     */
    private final String password;

    /**
     * use oauth
     */
    private Auth authentication;

    /**
     * oauth 1 and 2 access token
     */
    private final String accesstoken;

    /**
     * oauth access token secret
     */
    private final String accesstokensecret;

    /**
     * Timeout to use for server interaction in ms
     */
    private final int timeout;

    /**
     * If compressed uploads are supported
     */
    private final boolean compressedUploads;

    /**
     * display name of the user and other stuff
     */
    private UserDetails cachedUserDetails;

    /**
     * Current capabilities
     */
    private Capabilities capabilities = Capabilities.getDefault();

    /**
     * Current readonly capabilities
     */
    private Capabilities readOnlyCapabilities = Capabilities.getReadOnlyDefault();

    private long changesetId = -1;

    private final String generator;

    private final XmlPullParserFactory xmlParserFactory;

    private final DiscardedTags discardedTags;

    private final OkHttpOAuthConsumer oAuthConsumer;

    /**
     * Server path component for "api/" as in "http://api.openstreetmap.org/api/".
     */
    private static final String SERVER_API_PATH = "api/";

    /**
     * Server path component for "changeset/" as in "http://api.openstreetmap.org/api/0.6/changeset/".
     */
    private static final String SERVER_CHANGESET_PATH = "changeset/";

    /**
     * Server path component for "notes/" as in "http://api.openstreetmap.org/api/0.6/notes/".
     */
    private static final String SERVER_NOTES_PATH = "notes/";

    /**
     * Constructor. Sets {@link #rootOpen} and {@link #createdByTag}.
     * 
     * @param context Android Context
     * @param api an API object containing the current settings
     * @param generator how we identify ourself for APIs
     */
    public Server(@NonNull Context context, @NonNull final API api, @NonNull final String generator) {
        Log.d(DEBUG_TAG, "constructor");
        if (api.url != null && !"".equals(api.url)) {
            this.serverURL = api.url;
        } else {
            this.serverURL = Urls.DEFAULT_API_NO_HTTPS; // probably not needed anymore
        }
        this.name = api.name;
        this.readonlyURL = api.readonlyurl;
        this.notesURL = api.notesurl;
        this.password = api.pass;
        this.username = api.user;
        this.authentication = api.auth;
        this.generator = generator;
        this.accesstoken = api.accesstoken;
        this.accesstokensecret = api.accesstokensecret;
        this.timeout = api.timeout;
        this.compressedUploads = api.compressedUploads;

        if (authentication == Auth.OAUTH1A) {
            oAuthConsumer = new OAuth1aHelper().getOkHttpConsumer(context, name);
            if (oAuthConsumer != null) {
                oAuthConsumer.setTokenWithSecret(accesstoken, accesstokensecret);
            }
        } else {
            oAuthConsumer = null;
        }

        Log.d(DEBUG_TAG, "API entry " + name + " with " + this.serverURL);

        XmlPullParserFactory factory = null;
        try {
            factory = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, "Problem creating parser factory", e);
        }
        xmlParserFactory = factory;

        // initialize list of redundant tags
        discardedTags = App.getDiscardedTags(context);

        // if we have a tiled OSM data source, open
        Uri readOnlyUri = Uri.parse(getReadOnlyUrl());
        if (Schemes.FILE.equals(readOnlyUri.getScheme())) {
            MBTileProviderDataBase tempDB = null;
            try {
                tempDB = new MBTileProviderDataBase(context, readOnlyUri, 1);
            } catch (SQLiteException sqlex) {
                Log.e(DEBUG_TAG, "Unable to open db " + readOnlyUri);
                ScreenMessage.toastTopError(context,
                        context.getString(R.string.toast_unable_to_open_offline_data, getReadOnlyUrl(), sqlex.getLocalizedMessage()));
                // zap readonly api as it is broken
                this.readonlyURL = null;
            }
            mapSplitSource = tempDB;
        } else {
            mapSplitSource = null;
        }
    }

    /**
     * Get the cached details for the user.
     * 
     * @return An UserDetails object, or null if we haven't cached it yet.
     */
    @Nullable
    public UserDetails getCachedUserDetails() {
        return cachedUserDetails;
    }

    /**
     * Get the details for the user.
     * 
     * Caches the result for applications where a current version isn't needed
     * 
     * @return A current UserDetails object, or null if it couldn't be determined.
     */
    @Nullable
    public UserDetails getUserDetails() {
        try (Response response = openConnectionForAuthenticatedAccess(getUserDetailsUrl(), HTTP_GET, (RequestBody) null)) {
            checkResponseCode(response);
            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(response.body().byteStream(), null);
            cachedUserDetails = UserDetails.fromXml(parser);
            return cachedUserDetails;
        } catch (XmlPullParserException | IOException | NumberFormatException e) {
            Log.e(DEBUG_TAG, "Problem accessing user details", e);
        }
        return null;
    }

    /**
     * Return the username we use for this server, may be null
     * 
     * @return the display/user name
     */
    @Nullable
    public String getDisplayName() {
        return username;
    }

    /**
     * Get the preferences stored on the OSM API
     * 
     * @return a Map of key - value Strings
     */
    @NonNull
    public Map<String, String> getUserPreferences() {
        Map<String, String> result = new HashMap<>();
        try (Response response = openConnectionForAuthenticatedAccess(getUserPreferencesUrl(), HTTP_GET, (RequestBody) null)) {
            checkResponseCode(response);
            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(response.body().byteStream(), null);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && "preference".equals(tagName)) {
                    result.put(parser.getAttributeValue(null, "k"), parser.getAttributeValue(null, "v"));
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, "Problem parsing user preferences", e);
        } catch (MalformedURLException e) {
            Log.e(DEBUG_TAG, "Problem retrieving user preferences", e);
        } catch (IOException | NumberFormatException e) {
            Log.e(DEBUG_TAG, "Problem accessing user preferences", e);
        }
        return result;
    }

    /**
     * Set a user preference on the API server
     * 
     * @param key preference key
     * @param value preference value
     * @throws OsmException if something goes wrong
     */
    public void setUserPreference(@NonNull String key, @Nullable String value) throws OsmException {
        try (Response response = openConnectionForAuthenticatedAccess(getSingleUserPreferencesUrl(key), HTTP_PUT,
                RequestBody.create(null, value != null ? value : ""))) {
            int responseCode = response.code();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String message = Server.readStream(response.body().byteStream());
                Log.e(DEBUG_TAG, "Problem setting user preferences " + key + "=" + value + " code " + responseCode + " message " + message);
                throw new OsmServerException(responseCode, message);
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem setting user preferences " + key, e);
            throw new OsmException(e.getMessage());
        }
    }

    /**
     * Delete a user preference on the API server
     * 
     * @param key preference key
     * @throws OsmException if something goes wrong
     */
    public void deleteUserPreference(@NonNull String key) throws OsmException {
        try (Response response = openConnectionForAuthenticatedAccess(getSingleUserPreferencesUrl(key), HTTP_DELETE, null)) {
            int responseCode = response.code();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String message = Server.readStream(response.body().byteStream());
                Log.e(DEBUG_TAG, "Problem deleting user preferences " + key + " code " + responseCode + " message " + message);
                throw new OsmServerException(responseCode, message);
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem deleting user preferences " + key, e);
            throw new OsmException(e.getMessage());
        }
    }

    /**
     * @return true if a read only API URL is set
     */
    public boolean hasReadOnly() {
        return readonlyURL != null && !"".equals(readonlyURL);
    }

    /**
     * Get Capabilities from a read only API instance if configures
     * 
     * @return a Capabilities object, if none could be retrieved this will be the default
     */
    @Nullable
    public Capabilities getReadOnlyCapabilities() {
        try {
            Capabilities result = getCapabilities(getReadOnlyCapabilitiesUrl());
            if (result != null) {
                readOnlyCapabilities = result;
            }
            return readOnlyCapabilities; // if retrieving failed return the default
        } catch (MalformedURLException e) {
            Log.e(DEBUG_TAG, "Problem with read-only capabilities URL", e);
        }
        return null;
    }

    /**
     * Return either the default capabilities or such that have already been retrieved from the server
     * 
     * This avoids having a time consuming and network requiring call to get a fresh copy
     * 
     * @return a Capabilities object
     */
    @NonNull
    public Capabilities getCachedCapabilities() {
        if (capabilities == null) {
            return Capabilities.getDefault();
        }
        return capabilities;
    }

    /**
     * Get the capabilities for the current API
     * 
     * Side effect set capabilities field and update limits that are used elsewhere
     * 
     * @return The capabilities for this server, or null if it couldn't be determined.
     */
    @NonNull
    public Capabilities getCapabilities() {
        try {
            Capabilities result = getCapabilities(getCapabilitiesUrl());
            if (result != null) {
                capabilities = result;
            }
            return capabilities; // if retrieving failed return the default
        } catch (MalformedURLException e) {
            Log.e(DEBUG_TAG, "Problem with capabilities URL", e);
        }
        return capabilities; // if retrieving failed return the default
    }

    /**
     * Get the capabilities for the supplied URL
     * 
     * @param capabilitiesURL the URL for the API capabilities call
     * @return The capabilities for this server, or null if it couldn't be determined.
     */
    private Capabilities getCapabilities(@NonNull URL capabilitiesURL) {
        //
        try (InputStream is = openConnection(null, capabilitiesURL, timeout, timeout)) {
            Log.d(DEBUG_TAG, "getCapabilities using " + capabilitiesURL.toString());
            return Capabilities.parse(xmlParserFactory.newPullParser(), is);
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, "Problem parsing capabilities", e);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem accessing capabilities", e);
        }
        return null;
    }

    /**
     * @return true if the api is at least available for reading
     */
    public boolean apiAvailable() {
        return capabilities.getApiStatus().equals(Capabilities.Status.ONLINE) || capabilities.getApiStatus().equals(Capabilities.Status.READONLY);
    }

    /**
     * @return true if the database is at least available for reading
     */
    public boolean readableDB() {
        return capabilities.getDbStatus().equals(Capabilities.Status.ONLINE) || capabilities.getDbStatus().equals(Capabilities.Status.READONLY);
    }

    /**
     * @return true if the database is available for writing
     */
    public boolean writableDB() {
        return capabilities.getDbStatus().equals(Capabilities.Status.ONLINE);
    }

    /**
     * @return true if the read only api entry isavailable for reading
     */
    public boolean readOnlyApiAvailable() {
        return readOnlyCapabilities.getApiStatus().equals(Capabilities.Status.ONLINE)
                || readOnlyCapabilities.getApiStatus().equals(Capabilities.Status.READONLY);
    }

    /**
     * @return true if the database of read only api entry is available for reading
     */
    public boolean readOnlyReadableDB() {
        return readOnlyCapabilities.getDbStatus().equals(Capabilities.Status.ONLINE) || readOnlyCapabilities.getDbStatus().equals(Capabilities.Status.READONLY);
    }

    /**
     * Open a connection to an OSM server and request all data in box
     * 
     * @param context Android context
     * @param box the specified bounding box
     * @return the stream
     * @throws IOException thrown general IO problems
     */
    @NonNull
    public InputStream getStreamForBox(@Nullable final Context context, @NonNull final BoundingBox box) throws IOException {
        Log.d(DEBUG_TAG, "getStreamForBox");
        URL url = new URL(getReadOnlyUrl() + "map?bbox=" + box.toApiString());
        return openConnection(context, url, timeout, timeout);
    }

    /**
     * Get a single element from the API
     * 
     * @param context Android context
     * @param mode "full", "relations", "ways" or null
     * @param type type (node, way, relation) of the object
     * @param id the OSM id of the object
     * @return the stream
     * @throws IOException thrown general IO problems
     */
    @NonNull
    public InputStream getStreamForElement(@Nullable final Context context, @Nullable final String mode, @NonNull final String type, final long id)
            throws IOException {
        Log.d(DEBUG_TAG, "getStreamForElement");
        URL url = new URL((hasMapSplitSource() ? getReadWriteUrl() : getReadOnlyUrl()) + type + "/" + id + (mode != null ? "/" + mode : ""));
        return openConnection(context, url, timeout, timeout);
    }

    /**
     * Get a multiple elements of the same type from the API
     * 
     * @param context Android context
     * @param type type (node, way, relation) of the object
     * @param ids array containing the OSM ids of the objects
     * @return the stream
     * @throws IOException thrown general IO problems
     */
    @NonNull
    public InputStream getStreamForElements(@Nullable final Context context, @NonNull final String type, final long[] ids) throws IOException {
        Log.d(DEBUG_TAG, "getStreamForElements");

        StringBuilder urlString = new StringBuilder();
        urlString.append(hasMapSplitSource() ? getReadWriteUrl() : getReadOnlyUrl());
        urlString.append(type);
        urlString.append("s?"); // that's a plural s
        urlString.append(type);
        urlString.append("s="); // and another one
        int size = ids.length;
        for (int i = 0; i < size; i++) {
            urlString.append(Long.toString(ids[i]));
            if (i < size - 1) {
                urlString.append(',');
            }
        }
        URL url = new URL(urlString.toString());
        return openConnection(context, url, timeout, timeout);
    }

    /**
     * Given an URL, open the connection and return the InputStream
     * 
     * Uses default timeout values
     * 
     * @param context Android context
     * @param url the URL
     * @return the InputStream
     * @throws IOException on an IO issue
     */
    @NonNull
    public static InputStream openConnection(@Nullable final Context context, @NonNull URL url) throws IOException {
        return openConnection(context, url, Server.DEFAULT_TIMEOUT, Server.DEFAULT_TIMEOUT);
    }

    /**
     * Given an URL, open the connection and return the InputStream
     * 
     * @param context Android context
     * @param url the URL
     * @param connectTimeout connection timeout in ms
     * @param readTimeout read timeout in ms
     * @return the InputStream
     * @throws IOException on any IO and other error
     * 
     */
    @NonNull
    public static InputStream openConnection(@Nullable final Context context, @NonNull URL url, int connectTimeout, int readTimeout) throws IOException {
        Log.d(DEBUG_TAG, "get input stream for  " + url.toString());
        try {
            Request request = new Request.Builder().url(url).build();
            OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).readTimeout(readTimeout,
                    TimeUnit.MILLISECONDS);
            OkHttpClient client = builder.build();
            Call readCall = client.newCall(request);
            Response readCallResponse = readCall.execute();
            if (readCallResponse.isSuccessful()) {
                ResponseBody responseBody = readCallResponse.body();
                return responseBody.byteStream();
            } else {
                if (context instanceof Activity) {
                    final int responseCode = readCallResponse.code();
                    final String responseMessage = readCallResponse.message();
                    if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                        ((Activity) context).runOnUiThread(() -> ScreenMessage.barError((Activity) context,
                                context.getString(R.string.toast_download_failed, responseCode, responseMessage)));
                    } else {
                        ((Activity) context).runOnUiThread(new DownloadErrorToast(context, responseCode, responseMessage));
                    }
                }
                throwOsmServerException(readCallResponse);
            }
        } catch (IllegalArgumentException iaex) {
            throw new IOException("Illegal argument", iaex);
        }
        throw new IOException("openCOnnection this can't happen"); // this is actually unreachable
    }

    abstract class XmlRequestBody extends RequestBody {
        @Override
        public MediaType contentType() {
            return TEXTXML;
        }
    }

    /**
     * Return true if either login/pass is set or if oAuth is enabled
     * 
     * @return true if either oauth is set or we have login information
     */
    public boolean isLoginSet() {
        return (username != null && (password != null && !"".equals(username) && !"".equals(password))) || authentication != Auth.BASIC;
    }

    /**
     * Send a XmlSerializable Object over a HttpUrlConnection
     * 
     * @param outputStream OutputStream to write to
     * @param xmlSerializable the object
     * @param changeSetId changeset id to use
     * @throws OsmIOException thrown if a write or other error occurs
     */
    @SuppressLint("NewApi") // StandardCharsets is desugared for APIs < 19.
    private void sendPayload(@NonNull final OutputStream outputStream, @NonNull final XmlSerializable xmlSerializable, long changeSetId) throws OsmIOException {
        try (OutputStreamWriter out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            XmlSerializer xmlSerializer = getXmlSerializer();
            xmlSerializer.setOutput(out);
            xmlSerializable.toXml(xmlSerializer, changeSetId);
        } catch (IOException e) {
            throw new OsmIOException("Could not send data to server", e);
        } catch (IllegalArgumentException e) {
            throw new OsmIOException("Sending illegal format object failed", e);
        } catch (IllegalStateException | XmlPullParserException e) {
            throw new OsmIOException("Sending failed due to serialization error", e);
        }
    }

    /**
     * Open a connection to the API authenticating either with OAuth or basic authentication
     * 
     * @param url URL we want to open
     * @param requestMethod the request method
     * @param body the RequestBody or null for a get
     * @return a Response object
     * @throws IOException on an IO issue
     */
    @NonNull
    Response openConnectionForAuthenticatedAccess(@NonNull final URL url, @NonNull final String requestMethod, @Nullable final RequestBody body)
            throws IOException {
        Log.d(DEBUG_TAG, "openConnectionForWriteAccess url " + url + " authentication " + authentication);

        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (body != null) {
            switch (requestMethod) {
            case HTTP_POST:
                requestBuilder.post(body);
                break;
            case HTTP_PUT:
                requestBuilder.put(body);
                break;
            case HTTP_DELETE:
                requestBuilder.delete(body);
                break;
            default:
                Log.e(DEBUG_TAG, "Unknown request method " + requestMethod);
            }
        } else {
            if (HTTP_DELETE.equals(requestMethod)) {
                requestBuilder.delete();
            }
        }
        Request request = requestBuilder.build();

        OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout,
                TimeUnit.MILLISECONDS);
        switch (authentication) {
        case OAUTH1A:
            builder.addInterceptor(new SigningInterceptor(oAuthConsumer));
            break;
        case OAUTH2:
            builder.addInterceptor(new OAuth2Interceptor(accesstoken));
            break;
        case BASIC:
            builder.addInterceptor(new BasicAuthInterceptor(username, password));
        }
        // if compressed uploads are supported and compression interceptor
        if ((HTTP_POST.equals(requestMethod) || HTTP_PUT.equals(requestMethod)) && compressedUploads) {
            builder.addInterceptor(new GzipRequestInterceptor());
        }

        return builder.build().newCall(request).execute();
    }

    /**
     * Test if changeset is at least potentially still open.
     * 
     * @return true if there is a potentially open changeset
     */
    public boolean hasOpenChangeset() {
        return changesetId != -1;
    }

    /**
     * Get the current open changeset id (if -1 there is none)
     * 
     * @return the current id
     */
    public long getOpenChangeset() {
        return changesetId;
    }

    /**
     * Set the current changeset id
     * 
     * This is only useful for restoring state
     * 
     * @param id the changeset id
     */
    public void setOpenChangeset(long id) {
        changesetId = id;
    }

    /**
     * Reset changeset id
     */
    public void resetChangeset() {
        setOpenChangeset(-1);
    }

    /**
     * Open a new changeset.
     * 
     * @param closeOpenChangeset if true attempt to close a potentially open changeset first
     * @param comment value for the comment tag
     * @param source value for the source tag
     * @param imagery list of values for the imagery_used tag
     * @param extraTags Additional tags to add
     * @throws IOException on an IO issue
     */
    public void openChangeset(boolean closeOpenChangeset, @Nullable final String comment, @Nullable final String source, @Nullable final List<String> imagery,
            @Nullable Map<String, String> extraTags) throws IOException {

        if (changesetId != -1) { // potentially still open, check if really the case
            Changeset cs = getChangeset(changesetId);
            if (cs != null && cs.isOpen()) {
                if (closeOpenChangeset) {
                    try {
                        closeChangeset();
                    } catch (IOException e) {
                        // Never fail
                    }
                } else {
                    Log.d(DEBUG_TAG, "Changeset #" + changesetId + " still open, reusing");
                    updateChangeset(changesetId, comment, source, imagery, extraTags);
                    return;
                }
            }
            changesetId = -1;
        }

        final XmlSerializable xmlData = new Changeset(generator, comment, source, imagery, extraTags, getCachedCapabilities()).tagsToXml();
        RequestBody body = new XmlRequestBody() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sendPayload(sink.outputStream(), xmlData, changesetId);
            }
        };
        Response response = openConnectionForAuthenticatedAccess(getCreateChangesetUrl(), HTTP_PUT, body);

        checkResponseCode(response);

        try (InputStream in = response.body().byteStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in), 9);
            String line = reader.readLine();
            if (line != null) {
                changesetId = Long.parseLong(line);
            } else {
                throw new OsmServerException(-1, "Server returned no changeset id");
            }
        } catch (NumberFormatException e) {
            throw new OsmServerException(-1, "Server returned illegal changeset id " + e.getMessage());
        }
    }

    /**
     * Close the current open changeset, will zap the stored id even if the closing fails, this will force using a new
     * changeset on the next upload
     * 
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws IOException on an IO issue
     */
    public void closeChangeset() throws IOException {
        try (Response response = openConnectionForAuthenticatedAccess(getCloseChangesetUrl(changesetId), HTTP_PUT, RequestBody.create(null, ""))) {
            checkResponseCode(response);
        } finally {
            changesetId = -1;
        }
    }

    /**
     * Retrieve information for a specific changeset
     * 
     * @param id id of the changeset
     * @return a Changeset object
     */
    @Nullable
    public Changeset getChangeset(long id) {
        try (Response response = openConnectionForAuthenticatedAccess(getChangesetUrl(changesetId), HTTP_GET, (RequestBody) null)) {
            checkResponseCode(response);
            return Changeset.parse(xmlParserFactory.newPullParser(), response.body().byteStream());
        } catch (IOException | XmlPullParserException e) {
            Log.d(DEBUG_TAG, "getChangeset got " + e.getMessage());
        }
        return null;
    }

    /**
     * Update an existing changeset
     * 
     * @param changesetId the id of the changeset
     * @param comment value for the comment tag
     * @param source value for the source tag
     * @param imagery list of values for the imagery_used tag
     * @param extraTags Additional tags to add
     * @return a Changeset object
     * @throws IOException on an IO issue
     */
    @Nullable
    public Changeset updateChangeset(final long changesetId, @Nullable final String comment, @Nullable final String source,
            @Nullable final List<String> imagery, @Nullable Map<String, String> extraTags) throws IOException {
        final XmlSerializable xmlData = new Changeset(generator, comment, source, imagery, extraTags, getCachedCapabilities()).tagsToXml();
        RequestBody body = new XmlRequestBody() {
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sendPayload(sink.outputStream(), xmlData, changesetId);
            }
        };
        try (Response response = openConnectionForAuthenticatedAccess(getChangesetUrl(changesetId), HTTP_PUT, body)) {
            checkResponseCode(response);
            return Changeset.parse(xmlParserFactory.newPullParser(), response.body().byteStream());
        } catch (IOException | XmlPullParserException e) {
            Log.d(DEBUG_TAG, "getChangeset got " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieve the actual changes for a specific changeset
     * 
     * @param id id of the changeset
     * @return a Storage object
     */
    @Nullable
    public Storage getChanges(long id) {
        try (Response response = openConnectionForAuthenticatedAccess(getChangesetDownloadUrl(changesetId), HTTP_GET, (RequestBody) null)) {
            checkResponseCode(response);
            OsmChangeParser oscParser = new OsmChangeParser();
            oscParser.clearBoundingBoxes(); // this removes the default bounding box
            oscParser.start(response.body().byteStream());
            return oscParser.getStorage();
        } catch (IOException | SAXException | ParserConfigurationException e) {
            Log.d(DEBUG_TAG, "getChanges got " + e.getMessage());
        }
        return null;
    }

    /**
     * Check the response code from a HttpURLConnection and if not OK throw an exception
     * 
     * @param response response from the server connection
     * @throws IOException on an IO issue
     */
    static void checkResponseCode(@Nullable final Response response) throws IOException {
        checkResponseCode(response, null);
    }

    /**
     * Check the response code from a HttpURLConnection and if not OK throw an exception
     * 
     * @param response response from the server connection
     * @param e an OsmElement associated with the problem or null
     * @throws IOException on an IO issue
     */
    private static void checkResponseCode(@Nullable final Response response, @Nullable final OsmElement e) throws IOException {
        int responsecode = -1;
        if (response == null) {
            throw new OsmServerException(responsecode, "Unknown error");
        }
        responsecode = response.code();
        Log.d(DEBUG_TAG, "response code " + responsecode);
        if (responsecode == -1) {
            throw new IOException("Invalid response from server");
        }
        if (responsecode != HttpURLConnection.HTTP_OK) {
            if (responsecode == HttpURLConnection.HTTP_GONE && e != null && e.getState() == OsmElement.STATE_DELETED) {
                // we tried to delete an already deleted element: log, but ignore
                Log.d(DEBUG_TAG, e.getOsmId() + " already deleted on server");
                return;
            }
            throwOsmServerException(response, e, responsecode);
        }
    }

    /**
     * Upload edits in OCS format and process the server response
     * 
     * @param delegator reference to the StorageDelegator
     * @param storage a Storage element hold the elements to upload
     * @throws IOException if writing the output doesn't work
     */
    public void diffUpload(@NonNull final StorageDelegator delegator, @NonNull final Storage storage) throws IOException {
        try {
            for (OsmElement elem : storage.getElements()) {
                if (elem.state != OsmElement.STATE_DELETED) {
                    discardedTags.remove(elem);
                }
            }
            RequestBody body = new XmlRequestBody() {
                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try {
                        OsmXml.writeOsmChange(storage, sink.outputStream(), changesetId, getCachedCapabilities().getMaxElementsInChangeset(),
                                App.getUserAgent());
                    } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
                        throw new IOException(e);
                    }
                }
            };
            try (Response response = openConnectionForAuthenticatedAccess(getDiffUploadUrl(changesetId), HTTP_POST, body)) {
                processDiffUploadResult(delegator, response, xmlParserFactory.newPullParser());
            }
        } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
            throw new OsmException(e.getMessage());
        }
    }

    public static final Pattern ERROR_MESSAGE_BAD_OAUTH_REQUEST = Pattern.compile("(?i)Bad OAuth request.*");

    /**
     * Process the results of uploading a diff to the API, here because it needs to manipulate the stored data
     * 
     * Note: we try to process as much as possible outside of real parser errors, as the data has already been
     * successfully uploaded to the API, the caller needs to assure that we do not get recalled on the non fatal errors.
     * 
     * @param delegator the StorageDelegator containing to data to update
     * @param response Response from the API
     * @param parser parser instance
     * @throws IOException on an error processing the data
     */
    private void processDiffUploadResult(@NonNull StorageDelegator delegator, @NonNull Response response, @NonNull XmlPullParser parser) throws IOException {
        int code = response.code();
        if (code != HttpURLConnection.HTTP_OK) {
            String message = Server.readStream(response.body().byteStream());
            String responseMessage = response.message();
            Log.d(DEBUG_TAG, "Error code: " + code + " response: " + responseMessage + " message: " + message);
            throw new OsmServerException(code, message);
        }
        // success so update ids and versions
        Storage apiStorage = delegator.getApiStorage();
        boolean rehash = false; // if ids are changed we need to rehash storage
        try {
            parser.setInput(new BufferedInputStream(response.body().byteStream(), StreamUtils.IO_BUFFER_SIZE), null);
            int eventType;
            boolean inResponse = false;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (eventType != XmlPullParser.START_TAG) {
                    continue;
                }
                String tagName = parser.getName();
                if (inResponse) {
                    String oldIdStr = parser.getAttributeValue(null, OLD_ID_ATTR);
                    if (oldIdStr == null) { // must always be present
                        Log.e(DEBUG_TAG, "oldId missing! tag " + tagName);
                        continue;
                    }
                    long oldId = Long.parseLong(oldIdStr);
                    String newIdStr = parser.getAttributeValue(null, NEW_ID_ATTR);
                    String newVersionStr = parser.getAttributeValue(null, NEW_VERSION_ATTR);
                    if (Node.NAME.equals(tagName) || Way.NAME.equals(tagName) || Relation.NAME.equals(tagName)) {
                        OsmElement e = apiStorage.getOsmElement(tagName, oldId);
                        if (e == null) {
                            // log crash or what
                            Log.e(DEBUG_TAG, "" + oldIdStr + " not found in api storage! New id " + newIdStr + " new version " + newVersionStr);
                            continue;
                        }
                        if (e.getState() == OsmElement.STATE_DELETED && newIdStr == null && newVersionStr == null) {
                            if (!apiStorage.removeElement(e)) {
                                Log.e(DEBUG_TAG, "Deleted " + e + " was already removed from local storage!");
                            }
                            Log.w(DEBUG_TAG, e + " deleted in API");
                            delegator.dirty();
                        } else if (e.isNew() && oldId < 0 && newIdStr != null && newVersionStr != null) {
                            long newId = Long.parseLong(newIdStr);
                            long newVersion = Long.parseLong(newVersionStr);
                            if (newId > 0) {
                                if (!apiStorage.removeElement(e)) {
                                    Log.e(DEBUG_TAG, "New " + e + " was already removed from api storage!");
                                }
                                Log.w(DEBUG_TAG, "New " + e + " added to API");
                                e.setOsmId(newId); // id change requires rehash, so that removing works,
                                                   // remove first then set id
                                e.setOsmVersion(newVersion);
                                e.setState(OsmElement.STATE_UNCHANGED);
                                delegator.dirty();
                                rehash = true;
                            } else {
                                Log.d(DEBUG_TAG, "Didn't get new ID: " + newId + " version " + newVersionStr);
                            }
                        } else if (e.getState() == OsmElement.STATE_MODIFIED && oldId > 0 && newIdStr != null && newVersionStr != null) {
                            long newId = Long.parseLong(newIdStr);
                            long newVersion = Long.parseLong(newVersionStr);
                            if (newId == oldId && newVersion > e.getOsmVersion()) {
                                if (!apiStorage.removeElement(e)) {
                                    Log.e(DEBUG_TAG, "Updated " + e + " was already removed from api storage!");
                                }
                                e.setOsmVersion(newVersion);
                                Log.w(DEBUG_TAG, e + " updated in API");
                                e.setState(OsmElement.STATE_UNCHANGED);
                            } else {
                                Log.d(DEBUG_TAG, "Didn't get new version: " + newVersion + " for " + newId);
                            }
                            delegator.dirty();
                        } else {
                            Log.e(DEBUG_TAG, "Unknown state for " + e.getOsmId() + " " + e.getState());
                        }
                    }
                } else if (eventType == XmlPullParser.START_TAG && DIFF_RESULT_ELEMENT.equals(tagName)) {
                    inResponse = true;
                } else {
                    Log.e(DEBUG_TAG, "Unknown start tag: " + tagName);
                }
            }
            if (rehash) {
                delegator.getCurrentStorage().rehash();
                if (!apiStorage.isEmpty()) { // shouldn't happen
                    apiStorage.rehash();
                }
            }
        } catch (XmlPullParserException | NumberFormatException e) {
            if (e.getMessage().contains(SocketTimeoutException.class.getSimpleName())) {
                // getCause is null, so hack around the issue
                throw new SocketTimeoutException(e.getLocalizedMessage());
            }
            throw new OsmException(e.toString());
        }
    }

    /**
     * Read a stream to its "end" and return the results as a String
     * 
     * @param in an InputStream to read
     * @return a String containing the read contents
     */
    @NonNull
    public static String readStream(@Nullable final InputStream in) {
        StringBuilder res = new StringBuilder();
        if (in != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in), 8000);
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    res.append(line);
                }
            } catch (IOException e) {
                Log.e(Server.class.getName() + ":readStream()", "Error in read-operation", e);
            }
        }
        return res.toString();
    }

    /**
     * Start an XML document for an OSM API operation
     * 
     * @param xmlSerializer an XmlSerializer instance
     * @param generator an identifier for the current application
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException on an IO error
     */
    static void startXml(@NonNull XmlSerializer xmlSerializer, @NonNull String generator) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startDocument(OsmXml.UTF_8, null);
        xmlSerializer.startTag("", OsmXml.OSM);
        xmlSerializer.attribute("", VERSION_KEY, API_VERSION);
        xmlSerializer.attribute("", GENERATOR_KEY, generator);
    }

    /**
     * End an XML document for an OSM API operation
     * 
     * @param xmlSerializer an XmlSerializer instance
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException on an IO error
     */
    static void endXml(@NonNull XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.endTag("", OsmXml.OSM);
        xmlSerializer.endDocument();
    }

    /**
     * Get an new XmlSerializer
     * 
     * @return an new XmlSerializer instance
     * @throws XmlPullParserException
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException on an IO error
     */
    private XmlSerializer getXmlSerializer() throws XmlPullParserException, IllegalArgumentException, IllegalStateException, IOException {
        XmlSerializer serializer = xmlParserFactory.newSerializer();
        serializer.setPrefix("", "");
        return serializer;
    }

    /**
     * Get the URL for creating a changeset
     * 
     * @return the URL
     * @throws MalformedURLException if the URL we tried to create was malformed
     */
    private URL getCreateChangesetUrl() throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + "create");
    }

    /**
     * Get the URL for closing a changeset
     * 
     * @param changesetId the id of the changeset
     * @return the URL
     * @throws MalformedURLException if the URL we tried to create was malformed
     */
    private URL getCloseChangesetUrl(long changesetId) throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changesetId + "/close");
    }

    /**
     * Get the URL for retrieving or updating a changeset
     * 
     * @param changesetId the id of the changeset
     * @return the URL
     * @throws MalformedURLException if the URL we tried to create was malformed
     */
    private URL getChangesetUrl(long changesetId) throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changesetId);
    }

    /**
     * Get the URL for a changesets osmChange xml
     * 
     * @param changesetId the id of the changeset
     * @return the URL
     * @throws MalformedURLException if the URL we tried to create was malformed
     */
    private URL getChangesetDownloadUrl(long changesetId) throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changesetId + "/download");
    }

    /**
     * Get the URL for diff uploads
     * 
     * @param changeSetId the current open changeset id
     * @return the URL
     * @throws MalformedURLException if the URL we tried to create was malformed
     */
    @NonNull
    private URL getDiffUploadUrl(long changeSetId) throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changeSetId + "/upload");
    }

    /**
     * Get the user details url
     * 
     * @return the users detail url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getUserDetailsUrl() throws MalformedURLException {
        return new URL(getReadWriteUrl() + "user/details");
    }

    /**
     * Get the user preferences url for all preferences
     * 
     * @return the users preferences url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getUserPreferencesUrl() throws MalformedURLException {
        return new URL(getReadWriteUrl() + "user/preferences");
    }

    /**
     * Get the user preferences url for a specific preference
     * 
     * @param key the key for the preference
     * @return the users preferences url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getSingleUserPreferencesUrl(@NonNull String key) throws MalformedURLException {
        return new URL(getReadWriteUrl() + "user/preferences/" + key);
    }

    /**
     * Get the url for adding a comment to a note
     * 
     * @param noteId the note id
     * @param comment the comment to add
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getAddNoteCommentUrl(@NonNull String noteId, @NonNull String comment) throws MalformedURLException {
        return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/comment?text=" + comment);
    }

    /**
     * Get the url to retrieve a specific note
     * 
     * @param noteId the note id
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getNoteUrl(@NonNull String noteId) throws MalformedURLException {
        return new URL(getNotesReadOnlyUrl() + SERVER_NOTES_PATH + noteId);
    }

    /**
     * Return for Notes read write API url as a string (this eill be the same as the general read write api currently)
     * 
     * @return a String with the url
     */
    private String getNotesUrl() {
        return serverURL;
    }

    /**
     * Return either the general read write API url as a string or a specific to notes one
     * 
     * @return a String with the url
     */
    private String getNotesReadOnlyUrl() {
        if (notesURL == null || "".equals(notesURL)) {
            return serverURL;
        } else {
            return notesURL;
        }
    }

    /**
     * Get the url to retrieve notes in a specific area
     * 
     * @param limit the maximum number of notes to return
     * @param area the BoundingBox
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getNotesForBox(long limit, @NonNull BoundingBox area) throws MalformedURLException {
        return new URL(getNotesReadOnlyUrl() + "notes?" + "limit=" + limit + "&" + "bbox=" + area.getLeft() / 1E7d + "," + area.getBottom() / 1E7d + ","
                + area.getRight() / 1E7d + "," + area.getTop() / 1E7d);
    }

    /**
     * Get the url to add a note
     * 
     * @param latitude the WGS84 latitude
     * @param longitude the WGS84 longitude
     * @param comment the initial comment
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getAddNoteUrl(double latitude, double longitude, @NonNull String comment) throws MalformedURLException {
        return new URL(getNotesUrl() + "notes?lat=" + latitude + "&lon=" + longitude + "&text=" + comment);
    }

    /**
     * Get the url for closing a note
     * 
     * @param noteId the note id
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getCloseNoteUrl(@NonNull String noteId) throws MalformedURLException {
        return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/close");
    }

    /**
     * Get the url for re-opening a note
     * 
     * @param noteId the note id
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getReopenNoteUrl(@NonNull String noteId) throws MalformedURLException {
        return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/reopen");
    }

    /**
     * Get the url for retrieving the API capabilities
     * 
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getCapabilitiesUrl() throws MalformedURLException {
        return getCapabilitiesUrl(getReadOnlyUrl());
    }

    /**
     * Get the Capabilities URL for the read-only API
     * 
     * @return a String with the url
     * @throws MalformedURLException if the URL can't be constructed properly
     */
    @NonNull
    private URL getReadOnlyCapabilitiesUrl() throws MalformedURLException {
        return getCapabilitiesUrl(getReadWriteUrl());
    }

    /**
     * Get the Capabilities URL for an API
     * 
     * @param url base API url
     * @return a String with the url
     * @throws MalformedURLException if the URL can't be constructed properly
     */
    @NonNull
    private URL getCapabilitiesUrl(@NonNull String url) throws MalformedURLException {
        // need to strip version from serverURL
        int apiPos = url.indexOf(SERVER_API_PATH);
        if (apiPos > 0) {
            String noVersionURL = getReadWriteUrl().substring(0, apiPos) + SERVER_API_PATH;
            return new URL(noVersionURL + "capabilities");
        }
        throw new MalformedURLException("Invalid API URL: " + getReadWriteUrl());
    }

    /**
     * @return the read/write URL
     */
    public String getReadWriteUrl() {
        return serverURL;
    }

    /**
     * Return the url as a string for a read only API if it exists otherwise the result is the same as for read/write
     * 
     * @return a String with the url
     */
    @NonNull
    public String getReadOnlyUrl() {
        if (readonlyURL == null || "".equals(readonlyURL)) {
            return serverURL;
        } else {
            return readonlyURL;
        }
    }

    /**
     * Get an url with the api and version indicator stripped if present
     * 
     * @param url the original url
     * @return the base URL, i.e. the url with the "/api/version/"-part stripped
     */
    public static String getBaseUrl(@NonNull String url) {
        return url.replaceAll("/api/[0-9]+(?:\\.[0-9]+)+/?$", "/");
    }

    /**
     * Get the base url for the website associated with the API
     * 
     * FIXME for now hardwired, this should be part of the API configuration
     * 
     * @return the URL for the OSM website
     */
    public String getWebsiteBaseUrl() {
        return getBaseUrl(getReadWriteUrl()).replace("api.", "");
    }

    /*
     * New Notes API code mostly from old OSB implementation the relevant API documentation is still in flux so this
     * implementation may have issues
     */

    /**
     * Retrieve a single note
     * 
     * @param id the id of the Note to retrieve
     * @return the Note, null if not found or other error
     * @throws IOException
     * @throws XmlPullParserException
     * @throws NumberFormatException
     */
    @Nullable
    public Note getNote(long id) throws NumberFormatException, XmlPullParserException, IOException {
        // http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
        Log.d(DEBUG_TAG, "getNote");
        try (InputStream is = openConnection(null, getNoteUrl(Long.toString(id)), timeout, timeout)) {
            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
            List<Note> result = Note.parseNotes(parser, null);
            return !result.isEmpty() ? result.get(0) : null;
        }
    }

    /**
     * Perform an HTTP request to download up to limit bugs inside the specified area. Blocks until the request is
     * complete.
     * 
     * @param area Latitude/longitude *1E7 of area to download.
     * @param limit maximum number of Notes to return, value of between 1 and 10000 is valid
     * @return All the Notes in the given area.
     */
    @NonNull
    public Collection<Note> getNotesForBox(@NonNull BoundingBox area, long limit) {
        // http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
        Log.d(DEBUG_TAG, "getNotesForBox");
        try (InputStream is = openConnection(null, getNotesForBox(limit, area), timeout, timeout)) {
            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
            return Note.parseNotes(parser, null);
        } catch (XmlPullParserException | IOException | OutOfMemoryError e) {
            Log.e(DEBUG_TAG, "getNotesForBox Exception", e);
            return new ArrayList<>(); // empty list
        }
    }

    /**
     * Perform an HTTP request to add the specified bug to the OpenStreetBugs database.
     * 
     * Blocks until the request is complete.
     * 
     * @param bug The bug to add.
     * @param comment The first comment for the bug.
     * @throws IOException on an IO error
     * @throws XmlPullParserException
     */
    public void addNote(@NonNull Note bug, @NonNull NoteComment comment) throws XmlPullParserException, IOException {
        if (bug.isNew()) {
            Log.d(DEBUG_TAG, "adding note");
            // http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?lat=<Latitude>&lon=<Longitude>&text=<Bug
            // description with author and date>&format=<Output format>
            String encodedComment = URLEncoder.encode(comment.getText(), OsmXml.UTF_8);
            URL addNoteUrl = getAddNoteUrl((bug.getLat() / 1E7d), (bug.getLon() / 1E7d), encodedComment);
            try (Response response = openConnectionForAuthenticatedAccess(addNoteUrl, HTTP_POST, RequestBody.create(null, ""))) {
                if (!response.isSuccessful()) {
                    throwOsmServerException(response);
                }
                updateNote(bug, response.body().byteStream());
            }
        }
    }

    // The note 10597 was closed at 2017-09-24 17:59:18 UTC
    private static final Pattern ERROR_MESSAGE_NOTE_ALREADY_CLOSED = Pattern.compile("(?i)The note ([0-9]+) was closed at.*");
    //
    private static final Pattern ERROR_MESSAGE_NOTE_ALREADY_OPENED = Pattern.compile("(?i)The note ([0-9]+) is already open.*");

    /**
     * Perform an HTTP request to add the specified comment to the specified bug.
     * 
     * Blocks until the request is complete. Will reopen the Note if it is already closed.
     * 
     * @param bug The bug to add the comment to.
     * @param comment The comment to add to the bug.
     * @throws IOException on an IO error
     * @throws XmlPullParserException
     */
    public void addComment(@NonNull Note bug, @NonNull NoteComment comment) throws IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "adding note comment " + bug.getId());
            // http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?id=<Bug ID>&text=<Comment with author and date>
            //
            // setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body
            // which will fail
            String encodedComment = URLEncoder.encode(comment.getText(), OsmXml.UTF_8);
            URL addCommentUrl = getAddNoteCommentUrl(Long.toString(bug.getId()), encodedComment);

            try (Response response = openConnectionForAuthenticatedAccess(addCommentUrl, HTTP_POST, RequestBody.create(null, ""))) {
                if (response.isSuccessful()) {
                    updateNote(bug, response.body().byteStream());
                    return;
                }
                handleNoteError(bug, response, ERROR_MESSAGE_NOTE_ALREADY_CLOSED, (Matcher m) -> {
                    String idStr = m.group(1);
                    Log.d(DEBUG_TAG, "addComment note " + idStr + " was already closed");
                    reopenNote(bug);
                    addComment(bug, comment);
                });
            }
        }
    }

    private interface NoteConflict {
        /**
         * Resolve a conflict returned by the Notes API
         * 
         * @param m matcher for the error message that matched
         */
        void resolve(@NonNull Matcher m) throws IOException, XmlPullParserException;
    }

    /**
     * Handles errors returned by the notes API
     * 
     * @param bug the Node
     * @param response Response from the API
     * @param pattern a Pattern to match in case of a conflict
     * @param resolver code to resolve the conflict
     * @throws IOException if IO goes wrong
     * @throws XmlPullParserException if we can't update the note
     */
    private void handleNoteError(@NonNull Note bug, @NonNull Response response, @NonNull Pattern pattern, @NonNull NoteConflict resolver)
            throws IOException, XmlPullParserException {
        int responseCode = response.code();
        if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
            InputStream errorStream = response.body().byteStream();
            String message = readStream(errorStream);
            Log.d(DEBUG_TAG, "409: " + message);
            Matcher m = pattern.matcher(message);
            if (m.matches()) {
                resolver.resolve(m);
                return;
            }
            throwOsmServerException(response);
        } else if (responseCode == HttpURLConnection.HTTP_GONE) {
            hiddenNote(bug);
        } else {
            throwOsmServerException(response);
        }
    }

    /**
     * If the note was hidden on the server delete it locally
     * 
     * @param bug the hidden Node
     */
    private void hiddenNote(@NonNull Note bug) {
        Log.d(DEBUG_TAG, "note was hidden on server");
        App.getTaskStorage().delete(bug);
    }

    /**
     * Perform an HTTP request to close the specified bug.
     * 
     * Blocks until the request is complete. If the note is already closed the error is ignored.
     * 
     * @param bug The bug to close.
     * @throws IOException on an IO error
     * @throws XmlPullParserException
     */
    public void closeNote(@NonNull Note bug) throws IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "closing note " + bug.getId());
            URL closeNoteUrl = getCloseNoteUrl(Long.toString(bug.getId()));
            try (Response response = openConnectionForAuthenticatedAccess(closeNoteUrl, HTTP_POST, RequestBody.create(null, ""))) {
                if (response.isSuccessful()) {
                    updateNote(bug, response.body().byteStream());
                    return;
                }
                handleNoteError(bug, response, ERROR_MESSAGE_NOTE_ALREADY_CLOSED, (Matcher m) -> {
                    String idStr = m.group(1);
                    Log.d(DEBUG_TAG, "closeNote note " + idStr + " was already closed");
                });
            }
        }
    }

    /**
     * Perform an HTTP request to reopen the specified bug.
     * 
     * Blocks until the request is complete. If the note is already open the error is ignored.
     * 
     * @param bug The bug to close.
     * @throws IOException on an IO error
     * @throws XmlPullParserException
     */
    public void reopenNote(@NonNull Note bug) throws IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "reopen note " + bug.getId());
            URL reopenNoteUrl = getReopenNoteUrl(Long.toString(bug.getId()));
            try (Response response = openConnectionForAuthenticatedAccess(reopenNoteUrl, HTTP_POST, RequestBody.create(null, ""))) {
                if (response.isSuccessful()) {
                    updateNote(bug, response.body().byteStream());
                    return;
                }
                handleNoteError(bug, response, ERROR_MESSAGE_NOTE_ALREADY_OPENED, (Matcher m) -> {
                    String idStr = m.group(1);
                    Log.d(DEBUG_TAG, "reopenNode note " + idStr + " was already open");
                });
            }
        }
    }

    /**
     * Parse a single OSM note (bug) from an InputStream
     * 
     * @param bug bug to parse in to
     * @param inputStream the input
     * @throws IOException on an IO error
     * @throws XmlPullParserException
     */
    private void updateNote(@NonNull Note bug, @NonNull InputStream inputStream) throws IOException, XmlPullParserException {
        XmlPullParser parser = xmlParserFactory.newPullParser();
        parser.setInput(new BufferedInputStream(inputStream, StreamUtils.IO_BUFFER_SIZE), null);
        Note.parseNotes(parser, bug); // replace contents with result from server
        App.getTaskStorage().setDirty();
    }

    /**
     * Test if we need to authorize
     * 
     * @return true if we are using OAuth but have not retrieved the accesstoken yet
     */
    public boolean needOAuthHandshake() {
        return (authentication == Auth.OAUTH1A && (accesstoken == null || accesstokensecret == null)) || (authentication == Auth.OAUTH2 && accesstoken == null);
    }

    /**
     * Override the auth value from the API configuration, only needed if inconsistent config
     * 
     * @param auth the authentication menu to use
     */
    public void setOAuth(@NonNull Auth auth) {
        authentication = auth;
    }

    /**
     * 
     * @return true if oauth is enabled
     */
    public boolean getOAuth() {
        return authentication == Auth.OAUTH1A || authentication == Auth.OAUTH2;
    }

    /**
     * Check if we are correctly authenticated wrt the OSM server API we are using
     * 
     * @param activity calling FragmentActivity
     * @param server the current in use Server API
     * @param restartAction the action to do when we've been successfully authenticated
     * @return true if login was already ok else false
     */
    public static boolean checkOsmAuthentication(@NonNull final FragmentActivity activity, @NonNull final Server server,
            @NonNull PostAsyncActionHandler restartAction) {
        if (server.isLoginSet()) {
            if (server.needOAuthHandshake()) {
                activity.runOnUiThread(() -> Authorize.startForResult(activity, (resultCode, result) -> {
                    if (Activity.RESULT_OK == resultCode) {
                        restartAction.onSuccess();
                    } else {
                        Log.w(DEBUG_TAG, "Authorized returned with " + resultCode);
                        restartAction.onError(null);
                    }
                }));
                if (server.getOAuth()) { // if still set
                    ScreenMessage.barError(activity, R.string.toast_oauth);
                }
                return false;
            }
            return true;
        } else {
            ErrorAlert.showDialog(activity, ErrorCodes.NO_LOGIN_DATA);
            return false;
        }
    }

    /**
     * Get a new XmlParser
     * 
     * @return an instance of XmlPullParserFactory
     * @throws XmlPullParserException {@see XmlPullParserException}
     */
    @NonNull
    XmlPullParser getXmlParser() throws XmlPullParserException {
        return xmlParserFactory.newPullParser();
    }

    /**
     * Construct and throw an OsmServerException from the connection to the server
     * 
     * @param response response from server
     * @throws IOException on an IO error
     */
    public static void throwOsmServerException(Response response) throws IOException {
        throwOsmServerException(response, null, response.code());
    }

    /**
     * Construct and throw an OsmServerException from the connection to the server
     * 
     * @param response response from server
     * @param e the OSM element that the error was caused by
     * @param responsecode code returned from server
     * @throws IOException on an IO error
     */
    public static void throwOsmServerException(@NonNull final Response response, @Nullable final OsmElement e, int responsecode) throws IOException {
        String responseMessage = response.message();
        if (responseMessage == null) {
            responseMessage = "";
        }
        InputStream in = response.body().byteStream();
        if (e == null) {
            Log.d(DEBUG_TAG, "response code " + responsecode + " response message " + responseMessage);
            throw new OsmServerException(responsecode, readStream(in));
        } else {
            throw new OsmServerException(responsecode, e.getName(), e.getOsmId(), readStream(in));
        }
    }

    @Override
    public String toString() {
        return "server: " + serverURL + " readonly: " + readonlyURL + " notes " + notesURL;
    }

    /**
     * Check if we have a read-only tiled data source
     * 
     * @return true if available
     */
    public boolean hasMapSplitSource() {
        return mapSplitSource != null;
    }

    /**
     * Get the current read-only tiled data source
     * 
     * @return an instance of MBTileProviderDataBase or null
     */
    @Nullable
    public MBTileProviderDataBase getMapSplitSource() {
        return mapSplitSource;
    }

    /**
     * Close the MapSplitSource if present
     */
    public void closeMapSplitSource() {
        if (mapSplitSource != null) {
            mapSplitSource.close();
        }
    }

    /**
     * Get the name of the API configuration used for this instance
     * 
     * @return the API name
     */
    public String getApiName() {
        return name;
    }

    /**
     * @return the auth
     */
    public Auth getAuthentication() {
        return authentication;
    }
}
