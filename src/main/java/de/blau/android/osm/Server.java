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
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Authorize;
import de.blau.android.ErrorCodes;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.net.OAuthHelper;
import de.blau.android.prefs.API;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.services.util.StreamUtils;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteComment;
import de.blau.android.util.ActivityResultHandler;
import de.blau.android.util.BasicAuthInterceptor;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.FileUtil;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
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
 */
public class Server {
    private static final String DEBUG_TAG = Server.class.getName();

    private static final String OSM_CHANGE_TAG = "osmChange";
    private static final String VERSION_KEY    = "version";
    private static final String GENERATOR_KEY  = "generator";

    private static final String HTTP_PUT    = "PUT";
    private static final String HTTP_POST   = "POST";
    private static final String HTTP_GET    = "GET";
    private static final String HTTP_DELETE = "DELETE";

    private static final MediaType TEXTXML = MediaType.parse("text/xml");

    /**
     * Timeout for connections in milliseconds.
     */
    public static final int TIMEOUT = 45 * 1000;

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
    private boolean oauth;

    /**
     * oauth access token
     */
    private final String accesstoken;

    /**
     * oauth access token secret
     */
    private final String accesstokensecret;

    /**
     * display name of the user and other stuff
     */
    private final UserDetails userDetails;

    /**
     * Current capabilities
     */
    private Capabilities capabilities = Capabilities.getDefault();

    /**
     * Current readonly capabilities
     */
    private Capabilities readOnlyCapabilities = Capabilities.getReadOnlyDefault();

    /**
     * <a href="http://wiki.openstreetmap.org/wiki/API">API</a>-Version.
     */
    private static final String API_VERSION = "0.6";

    private static final String OSMCHANGE_VERSION = "0.3";

    private long changesetId = -1;

    private final String generator;

    private final XmlPullParserFactory xmlParserFactory;

    private final DiscardedTags discardedTags;

    /**
     * Date pattern used for suggesting a file name when uploading GPX tracks.
     */
    private static final String DATE_PATTERN_GPX_TRACK_UPLOAD_SUGGESTED_FILE_NAME_PART = "yyyy-MM-dd'T'HHmmss";

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
        if (api.url != null && !api.url.equals("")) {
            this.serverURL = api.url;
        } else {
            this.serverURL = Urls.DEFAULT_API_NO_HTTPS; // probably not needed anymore
        }
        this.readonlyURL = api.readonlyurl;
        this.notesURL = api.notesurl;
        this.password = api.pass;
        this.username = api.user;
        this.oauth = api.oauth;
        this.generator = generator;
        this.accesstoken = api.accesstoken;
        this.accesstokensecret = api.accesstokensecret;

        userDetails = null;
        Log.d(DEBUG_TAG, "using " + this.username + " with " + this.serverURL);
        Log.d(DEBUG_TAG, "oAuth: " + this.oauth + " token " + this.accesstoken + " secret " + this.accesstokensecret);

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
        if (FileUtil.FILE_SCHEME.equals(readOnlyUri.getScheme())) {
            MBTileProviderDataBase tempDB = null;
            try {
                tempDB = new MBTileProviderDataBase(context, readOnlyUri, 1);
            } catch (SQLiteException sqlex) {
                Log.e(DEBUG_TAG, "Unable to open db " + readOnlyUri);
                Snack.toastTopError(context, context.getString(R.string.toast_unable_to_open_offline_data, getReadOnlyUrl(), sqlex.getLocalizedMessage()));
                // zap readonly api as it is broken
                this.readonlyURL = null;
            }
            mapSplitSource = tempDB;
        } else {
            mapSplitSource = null;
        }
    }

    /**
     * display name and message counts is the only thing that is interesting
     * 
     * @author simon
     *
     */
    public class UserDetails {
        private String display_name = "unknown";
        private int    received     = 0;
        private int    unread       = 0;
        private int    sent         = 0;

        /**
         * @return the display_name
         */
        public String getDisplayName() {
            return display_name;
        }

        /**
         * @param display_name the display_name to set
         */
        public void setDisplayName(String display_name) {
            this.display_name = display_name;
        }

        /**
         * @return the received
         */
        public int getReceivedMessages() {
            return received;
        }

        /**
         * @param received the received to set
         */
        public void setReceivedMessages(int received) {
            this.received = received;
        }

        /**
         * @return the unread
         */
        public int getUnreadMessages() {
            return unread;
        }

        /**
         * @param unread the unread to set
         */
        public void setUnreadMessages(int unread) {
            this.unread = unread;
        }

        /**
         * @return the sent
         */
        public int getSentMessages() {
            return sent;
        }

        /**
         * @param sent the sent to set
         */
        public void setSentMessages(int sent) {
            this.sent = sent;
        }
    }

    /**
     * Get the details for the user.
     * 
     * @return The display name for the user, or null if it couldn't be determined.
     */
    public UserDetails getUserDetails() {
        UserDetails result = null;
        if (userDetails == null) {
            // Haven't retrieved the details from OSM - try to
            try {
                Response response = openConnectionForAuthenticatedAccess(getUserDetailsUrl(), HTTP_GET, (RequestBody) null);
                checkResponseCode(response);
                XmlPullParser parser = xmlParserFactory.newPullParser();
                parser.setInput(response.body().byteStream(), null);
                int eventType;
                result = new UserDetails();
                boolean messages = false;
                while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    String tagName = parser.getName();
                    if (eventType == XmlPullParser.START_TAG && "user".equals(tagName)) {
                        result.setDisplayName(parser.getAttributeValue(null, "display_name"));
                        Log.d(DEBUG_TAG, "getUserDetails display name " + result.getDisplayName());
                    }
                    if (eventType == XmlPullParser.START_TAG && "messages".equals(tagName)) {
                        messages = true;
                    }
                    if (eventType == XmlPullParser.END_TAG && "messages".equals(tagName)) {
                        messages = false;
                    }
                    if (messages) {
                        if (eventType == XmlPullParser.START_TAG && "received".equals(tagName)) {
                            result.setReceivedMessages(Integer.parseInt(parser.getAttributeValue(null, "count")));
                            Log.d(DEBUG_TAG, "getUserDetails received " + result.getReceivedMessages());
                            result.setUnreadMessages(Integer.parseInt(parser.getAttributeValue(null, "unread")));
                            Log.d(DEBUG_TAG, "getUserDetails unread " + result.getUnreadMessages());
                        }
                        if (eventType == XmlPullParser.START_TAG && "sent".equals(tagName)) {
                            result.setSentMessages(Integer.parseInt(parser.getAttributeValue(null, "count")));
                            Log.d(DEBUG_TAG, "getUserDetails sent " + result.getSentMessages());
                        }
                    }
                }
            } catch (XmlPullParserException e) {
                Log.e(DEBUG_TAG, "Problem parsing user details", e);
            } catch (MalformedURLException e) {
                Log.e(DEBUG_TAG, "Problem retrieving user details", e);
            } catch (IOException | NumberFormatException e) {
                Log.e(DEBUG_TAG, "Problem accessing user details", e);
            }
            return result;
        }
        return userDetails; // might not make sense
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
        try {
            Response response = openConnectionForAuthenticatedAccess(getUserPreferencesUrl(), HTTP_GET, (RequestBody) null);
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
        try {
            Response response = openConnectionForAuthenticatedAccess(getSingleUserPreferencesUrl(key), HTTP_PUT,
                    RequestBody.create(null, value != null ? value : ""));
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
        try {
            Response response = openConnectionForAuthenticatedAccess(getSingleUserPreferencesUrl(key), HTTP_DELETE, null);
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
        try (InputStream is = openConnection(null, capabilitiesURL)) {
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
     * @throws OsmServerException thrown for API specific errors
     * @throws IOException thrown general IO problems
     */
    @NonNull
    public InputStream getStreamForBox(@Nullable final Context context, @NonNull final BoundingBox box) throws OsmServerException, IOException {
        Log.d(DEBUG_TAG, "getStreamForBox");
        URL url = new URL(getReadOnlyUrl() + "map?bbox=" + box.toApiString());
        return openConnection(context, url);
    }

    /**
     * Get a single element from the API
     * 
     * @param context Android context
     * @param mode "full" or null
     * @param type type (node, way, relation) of the object
     * @param id the OSM id of the object
     * @return the stream
     * @throws OsmServerException thrown for API specific errors
     * @throws IOException thrown general IO problems
     */
    @NonNull
    public InputStream getStreamForElement(@Nullable final Context context, @Nullable final String mode, @NonNull final String type, final long id)
            throws OsmServerException, IOException {
        Log.d(DEBUG_TAG, "getStreamForElement");
        URL url = new URL((hasMapSplitSource() ? getReadWriteUrl() : getReadOnlyUrl()) + type + "/" + id + (mode != null ? "/" + mode : ""));
        return openConnection(context, url);
    }

    /**
     * Get a multiple elements of the same type from the API
     * 
     * @param context Android context
     * @param type type (node, way, relation) of the object
     * @param ids array containing the OSM ids of the objects
     * @return the stream
     * @throws OsmServerException thrown for API specific errors
     * @throws IOException thrown general IO problems
     */
    @NonNull
    public InputStream getStreamForElements(@Nullable final Context context, @NonNull final String type, final long[] ids)
            throws OsmServerException, IOException {
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
        return openConnection(context, url);
    }

    /**
     * Given an URL, open the connection and return the InputStream
     * 
     * Uses default timeout values
     * 
     * @param context Android context
     * @param url the URL
     * @return the InputStream
     * @throws IOException
     * @throws OsmServerException
     */
    @NonNull
    public static InputStream openConnection(@Nullable final Context context, @NonNull URL url) throws IOException, OsmServerException {
        return openConnection(context, url, TIMEOUT, TIMEOUT);
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
     * @throws OsmServerException if we got an error from the remote server
     * 
     */
    @NonNull
    public static InputStream openConnection(@Nullable final Context context, @NonNull URL url, int connectTimeout, int readTimeout)
            throws IOException, OsmServerException {
        Log.d(DEBUG_TAG, "get input stream for  " + url.toString());
        try {
            Request request = new Request.Builder().url(url).build();
            OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).readTimeout(readTimeout,
                    TimeUnit.MILLISECONDS);
            // if (oauth) {
            // OAuthHelper oa = new OAuthHelper();
            // OkHttpOAuthConsumer consumer = oa.getOkHttpConsumer(getBaseUrl(getReadOnlyUrl()));
            // if (consumer != null) {
            // consumer.setTokenWithSecret(accesstoken, accesstokensecret);
            // builder.addInterceptor(new SigningInterceptor(consumer));
            // }
            // }
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
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snack.barError((Activity) context, context.getString(R.string.toast_download_failed, responseCode, responseMessage));
                            }
                        });
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

    /**
     * Sends an delete-request to the server.
     * 
     * Note this uses the diff upload mechanism
     * 
     * @param elem the element which should be deleted.
     * @return true when the server indicates the successful deletion (HTTP 200), otherwise false.
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     * @throws IOException
     */
    public boolean deleteElement(@NonNull final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
        Log.d(DEBUG_TAG, "Deleting " + elem.getName() + " #" + elem.getOsmId());
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return TEXTXML;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try {
                    sendPayload(sink.outputStream(), new XmlSerializable() {
                        @Override
                        public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                            final String action = "delete";
                            startChangeXml(serializer, action);
                            elem.toXml(serializer, changeSetId);
                            endChangeXml(serializer, action);
                        }
                    }, changesetId);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    throw new IOException(e);
                }
            }
        };
        Response response = openConnectionForAuthenticatedAccess(getDiffUploadUrl(changesetId), HTTP_POST, body);
        checkResponseCode(response, elem);
        return true;
    }

    /**
     * Return true if either login/pass is set or if oAuth is enabled
     * 
     * @return true if either oauth is set or we have login information
     */
    public boolean isLoginSet() {
        return (username != null && (password != null && !username.equals("") && !password.equals(""))) || oauth;
    }

    /**
     * Update an individual element on the server
     * 
     * @param elem the OsmElement to update
     * @return the new version number
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     * @throws IOException
     */
    public long updateElement(@NonNull final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
        long osmVersion = -1;
        InputStream in = null;
        try {
            URL updateElementUrl = getUpdateElementUrl(elem);
            Log.d(DEBUG_TAG, "Updating " + elem.getName() + " #" + elem.getOsmId() + " " + updateElementUrl);

            // remove redundant tags
            discardedTags.remove(elem);

            RequestBody body = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return TEXTXML;
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try {
                        sendPayload(sink.outputStream(), new XmlSerializable() {
                            @Override
                            public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                                startXml(serializer, generator);
                                elem.toXml(serializer, changeSetId);
                                endXml(serializer);
                            }
                        }, changesetId);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        throw new IOException(e);
                    }
                }
            };
            Response response = openConnectionForAuthenticatedAccess(updateElementUrl, HTTP_PUT, body);
            checkResponseCode(response, elem);
            in = response.body().byteStream();
            try {
                osmVersion = Long.parseLong(readLine(in));
            } catch (NumberFormatException e) {
                throw new OsmServerException(-1, "Server returned illegal element version " + e.getMessage());
            }
        } finally {
            SavingHelper.close(in);
        }
        return osmVersion;
    }

    /**
     * Send a XmlSerializable Object over a HttpUrlConnection
     * 
     * @param outputStream OutputStream to write to
     * @param xmlSerializable the object
     * @param changeSetId changeset id to use
     * @throws OsmIOException thrown if a write or other error occurs
     */
    private void sendPayload(@NonNull final OutputStream outputStream, @NonNull final XmlSerializable xmlSerializable, long changeSetId) throws OsmIOException {
        OutputStreamWriter out = null;
        try {
            XmlSerializer xmlSerializer = getXmlSerializer();
            out = new OutputStreamWriter(outputStream, Charset.defaultCharset());
            xmlSerializer.setOutput(out);
            xmlSerializable.toXml(xmlSerializer, changeSetId);
        } catch (IOException e) {
            throw new OsmIOException("Could not send data to server", e);
        } catch (IllegalArgumentException e) {
            throw new OsmIOException("Sending illegal format object failed", e);
        } catch (IllegalStateException | XmlPullParserException e) {
            throw new OsmIOException("Sending failed due to serialization error", e);
        } finally {
            SavingHelper.close(out);
        }
    }

    /**
     * Open a connection to the API authenticating either with OAuth or basic authentication
     * 
     * @param url URL we want to open
     * @param requestMethod the request method
     * @param body the RequestBody or null for a get
     * @return a Response object
     * @throws IOException
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     */
    private Response openConnectionForAuthenticatedAccess(@NonNull final URL url, @NonNull final String requestMethod, @Nullable final RequestBody body)
            throws IOException, MalformedURLException, ProtocolException {
        Log.d(DEBUG_TAG, "openConnectionForWriteAccess url " + url);

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
            }
        } else {
            if (HTTP_DELETE.equals(requestMethod)) {
                requestBuilder.delete();
            }
        }
        Request request = requestBuilder.build();

        OkHttpClient.Builder builder = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT,
                TimeUnit.MILLISECONDS);
        if (oauth) {
            OAuthHelper oa = new OAuthHelper();
            OkHttpOAuthConsumer consumer = oa.getOkHttpConsumer(getBaseUrl(getReadWriteUrl()));
            if (consumer != null) {
                consumer.setTokenWithSecret(accesstoken, accesstokensecret);
                builder.addInterceptor(new SigningInterceptor(consumer));
            }
        } else {
            builder.addInterceptor(new BasicAuthInterceptor(username, password));
        }

        OkHttpClient client = builder.build();

        Call call = client.newCall(request);

        return call.execute();
    }

    /**
     * Create a new element on the server
     * 
     * @param elem the OsmELement to create
     * @return the OSM id of the new element
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     * @throws IOException
     */
    public long createElement(@NonNull final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
        long osmId = -1;
        InputStream in = null;

        try {
            RequestBody body = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return TEXTXML;
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try {
                        sendPayload(sink.outputStream(), new XmlSerializable() {
                            @Override
                            public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                                startXml(serializer, generator);
                                elem.toXml(serializer, changeSetId);
                                endXml(serializer);
                            }
                        }, changesetId);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        throw new IOException(e);
                    }
                }
            };
            Response response = openConnectionForAuthenticatedAccess(getCreateElementUrl(elem), HTTP_PUT, body);
            checkResponseCode(response);
            in = response.body().byteStream();
            try {
                osmId = Long.parseLong(readLine(in));
            } catch (NumberFormatException e) {
                throw new OsmServerException(-1, "Server returned illegal element id " + e.getMessage());
            }
        } finally {
            SavingHelper.close(in);
        }
        return osmId;
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
     * @param imagery value for the imagery_used tag
     * @param extraTags Additional tags to add
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     * @throws IOException
     */
    public void openChangeset(boolean closeOpenChangeset, @Nullable final String comment, @Nullable final String source, @Nullable final String imagery,
            @Nullable Map<String, String> extraTags) throws MalformedURLException, ProtocolException, IOException {
        long newChangesetId = -1;

        if (changesetId != -1) { // potentially still open, check if really the case
            Changeset cs = getChangeset(changesetId);
            if (cs != null && cs.open) {
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
            } else {
                changesetId = -1;
            }
        }

        final XmlSerializable xmlData = new Changeset(generator, comment, source, imagery, extraTags).tagsToXml();
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return TEXTXML;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sendPayload(sink.outputStream(), xmlData, changesetId);
            }
        };
        Response response = openConnectionForAuthenticatedAccess(getCreateChangesetUrl(), HTTP_PUT, body);

        checkResponseCode(response);

        try (InputStream in = response.body().byteStream()) {
            newChangesetId = Long.parseLong(readLine(in));
        } catch (NumberFormatException e) {
            throw new OsmServerException(-1, "Server returned illegal changeset id " + e.getMessage());
        }

        changesetId = newChangesetId;
    }

    /**
     * Close the current open changeset, will zap the stored id even if the closing fails, this will force using a new
     * changeset on the next upload
     * 
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     * @throws IOException
     */
    public void closeChangeset() throws MalformedURLException, ProtocolException, IOException {
        try {
            Response response = openConnectionForAuthenticatedAccess(getCloseChangesetUrl(changesetId), HTTP_PUT, RequestBody.create(null, ""));
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
    private Changeset getChangeset(long id) {
        Changeset result = null;
        try {
            Response response = openConnectionForAuthenticatedAccess(getChangesetUrl(changesetId), HTTP_GET, (RequestBody) null);
            checkResponseCode(response);
            result = Changeset.parse(xmlParserFactory.newPullParser(), response.body().byteStream());
        } catch (IOException | XmlPullParserException e) {
            Log.d(DEBUG_TAG, "getChangeset got " + e.getMessage());
        }
        return result;
    }

    /**
     * Update an existing changeset
     * 
     * @param changesetId the id of the changeset
     * @param comment value for the comment tag
     * @param source value for the source tag
     * @param imagery value for the imagery_used tag
     * @param extraTags Additional tags to add
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     * @throws IOException
     */
    private void updateChangeset(final long changesetId, @Nullable final String comment, @Nullable final String source, @Nullable final String imagery,
            @Nullable Map<String, String> extraTags) throws MalformedURLException, ProtocolException, IOException {
        final XmlSerializable xmlData = new Changeset(generator, comment, source, imagery, extraTags).tagsToXml();
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return TEXTXML;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sendPayload(sink.outputStream(), xmlData, changesetId);
            }
        };
        Response response = openConnectionForAuthenticatedAccess(getChangesetUrl(changesetId), HTTP_PUT, body);
        checkResponseCode(response);
        // ignore response for now
    }

    /**
     * Check the response code from a HttpURLConnection and if not OK throw an exception
     * 
     * @param response response from the server connection
     * @throws IOException
     * @throws OsmException
     */
    private void checkResponseCode(@Nullable final Response response) throws IOException, OsmException {
        checkResponseCode(response, null);
    }

    /**
     * Check the response code from a HttpURLConnection and if not OK throw an exception
     * 
     * @param response response from the server connection
     * @param e an OsmElement associated with the problem or null
     * @throws IOException
     * @throws OsmException
     */
    private void checkResponseCode(@Nullable final Response response, @Nullable final OsmElement e) throws IOException, OsmException {
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
                // FIXME we tried to delete an already deleted element: log, but ignore, maybe it would be better to ask
                // user
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
     * @throws MalformedURLException if the URL can't be constructed properly
     * @throws ProtocolException
     * @throws IOException
     */
    public void diffUpload(final StorageDelegator delegator) throws MalformedURLException, ProtocolException, IOException {
        try {
            for (OsmElement elem : delegator.getApiStorage().getElements()) {
                if (elem.state != OsmElement.STATE_DELETED) {
                    discardedTags.remove(elem);
                }
            }
            RequestBody body = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return TEXTXML;
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    try {
                        OsmXml.writeOsmChange(delegator.getApiStorage(), sink.outputStream(), changesetId, getCachedCapabilities().getMaxElementsInChangeset(),
                                App.getUserAgent());
                    } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
                        throw new IOException(e);
                    }
                }
            };
            Response response = openConnectionForAuthenticatedAccess(getDiffUploadUrl(changesetId), HTTP_POST, body);
            processDiffUploadResult(delegator, response, xmlParserFactory.newPullParser());
        } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e1) {
            throw new OsmException(e1.getMessage());
        }
    }

    /**
     * These patterns are fairly, to very, unforgiving, hopefully API 0.7 will give the error codes back in a more
     * structured way
     */
    private static final Pattern ERROR_MESSAGE_CLOSED_CHANGESET               = Pattern.compile("(?i)The changeset ([0-9]+) was closed at.*");
    private static final Pattern ERROR_MESSAGE_VERSION_CONFLICT               = Pattern
            .compile("(?i)Version mismatch: Provided ([0-9]+), server had: ([0-9]+) of (Node|Way|Relation) ([0-9]+)");
    private static final Pattern ERROR_MESSAGE_DELETED                        = Pattern
            .compile("(?i)The (node|way|relation) with the id ([0-9]+) has already been deleted");
    private static final Pattern ERROR_MESSAGE_PRECONDITION_STILL_USED        = Pattern
            .compile("(?i)(?:Precondition failed: )?(Node|Way) ([0-9]+) is still used by (way|relation)[s]? ([0-9]+).*");
    private static final Pattern ERROR_MESSAGE_PRECONDITION_RELATION_RELATION = Pattern
            .compile("(?i)(?:Precondition failed: )?The relation ([0-9]+) is used in relation ([0-9]+).");
    public static final Pattern  ERROR_MESSAGE_BAD_OAUTH_REQUEST              = Pattern.compile("(?i)Bad OAuth request.*");

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
    private void processDiffUploadResult(StorageDelegator delegator, Response response, XmlPullParser parser) throws IOException {
        Storage apiStorage = delegator.getApiStorage();
        int code = response.code();
        if (code == HttpURLConnection.HTTP_OK) {
            boolean rehash = false; // if ids are changed we need to rehash
                                    // storage
            try {
                parser.setInput(new BufferedInputStream(response.body().byteStream(), StreamUtils.IO_BUFFER_SIZE), null);
                int eventType;
                boolean inResponse = false;
                while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String tagName = parser.getName();
                        if (inResponse) {
                            String oldIdStr = parser.getAttributeValue(null, "old_id");
                            if (oldIdStr == null) { // must always be present
                                Log.e(DEBUG_TAG, "oldId missing! tag " + tagName);
                                continue;
                            }
                            long oldId = Long.parseLong(oldIdStr);
                            String newIdStr = parser.getAttributeValue(null, "new_id");
                            String newVersionStr = parser.getAttributeValue(null, "new_version");
                            if (Node.NAME.equals(tagName) || Way.NAME.equals(tagName) || Relation.NAME.equals(tagName)) {
                                OsmElement e = apiStorage.getOsmElement(tagName, oldId);
                                if (e != null) {
                                    if (e.getState() == OsmElement.STATE_DELETED && newIdStr == null && newVersionStr == null) {
                                        if (!apiStorage.removeElement(e)) {
                                            Log.e(DEBUG_TAG, "Deleted " + e + " was already removed from local storage!");
                                        }
                                        Log.w(DEBUG_TAG, e + " deleted in API");
                                        delegator.dirty();
                                    } else if (e.getState() == OsmElement.STATE_CREATED && oldId < 0 && newIdStr != null && newVersionStr != null) {
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
                                } else {
                                    // log crash or what
                                    Log.e(DEBUG_TAG, "" + oldIdStr + " not found in api storage! New id " + newIdStr + " new version " + newVersionStr);
                                }
                            }
                        } else if (eventType == XmlPullParser.START_TAG && "diffResult".equals(tagName)) {
                            inResponse = true;
                        } else {
                            Log.e(DEBUG_TAG, "Unknown start tag: " + tagName);
                        }
                    }
                }
                if (rehash) {
                    delegator.getCurrentStorage().rehash();
                    if (!apiStorage.isEmpty()) { // shouldn't happen
                        apiStorage.rehash();
                    }
                }
            } catch (XmlPullParserException | NumberFormatException | IOException e) {
                throw new OsmException(e.toString());
            }
        } else {
            String message = Server.readStream(response.body().byteStream());
            String responseMessage = response.message();
            Log.d(DEBUG_TAG, "Error code: " + code + " response: " + responseMessage + " message: " + message);
            if (code == HttpURLConnection.HTTP_CONFLICT) {
                // got conflict , possible messages see
                // http://wiki.openstreetmap.org/wiki/API_v0.6#Diff_upload:_POST_.2Fapi.2F0.6.2Fchangeset.2F.23id.2Fupload
                Matcher m = ERROR_MESSAGE_VERSION_CONFLICT.matcher(message);
                if (m.matches()) {
                    String type = m.group(3);
                    String idStr = m.group(4);
                    generateException(apiStorage, type, idStr, code, responseMessage, message);
                } else {
                    m = ERROR_MESSAGE_CLOSED_CHANGESET.matcher(message);
                    if (m.matches()) {
                        // note this should never happen, since we check
                        // if the changeset is still open before upload
                        throw new OsmServerException(HttpURLConnection.HTTP_BAD_REQUEST, code + "=\"" + responseMessage + "\" ErrorMessage: " + message);
                    }
                }
                Log.e(DEBUG_TAG, "Code: " + code + " unknown error message: " + message);
                throw new OsmServerException(HttpURLConnection.HTTP_BAD_REQUEST,
                        "Original error " + code + "=\"" + responseMessage + "\" ErrorMessage: " + message);
            } else if (code == HttpURLConnection.HTTP_GONE) {
                Matcher m = ERROR_MESSAGE_DELETED.matcher(message);
                if (m.matches()) {
                    String type = m.group(1);
                    String idStr = m.group(2);
                    generateException(apiStorage, type, idStr, code, responseMessage, message);
                }
            } else if (code == HttpURLConnection.HTTP_PRECON_FAILED) {
                // Besides the messages parsed here, theoretically the following two messages could be returned:
                // Way #{id} requires the nodes with id in (#{missing_ids}), which either do not exist, or are not
                // visible.
                // and
                // Relation with id #{id} cannot be saved due to #{element} with id #{element.id}
                // however it shouldn't be possible to create such situations with vespucci
                Matcher m = ERROR_MESSAGE_PRECONDITION_STILL_USED.matcher(message);
                if (m.matches()) {
                    String type = m.group(1);
                    String idStr = m.group(2);
                    generateException(apiStorage, type, idStr, code, responseMessage, message);
                } else {
                    m = ERROR_MESSAGE_PRECONDITION_RELATION_RELATION.matcher(message);
                    if (m.matches()) {
                        String idStr = m.group(1);
                        generateException(apiStorage, "relation", idStr, code, responseMessage, message);
                    } else {
                        Log.e(DEBUG_TAG, "Unknown error message: " + message);
                    }
                }
            }
            throw new OsmServerException(code, message);
        }
    }

    /**
     * Build and throw and exception containign some details on the affected OsmElement
     * 
     * @param apiStorage the current api storage
     * @param type the type of the OsmElement
     * @param idStr a String containing the if of the element
     * @param code the returned HTTP code
     * @param responseMessage the HTTP error message
     * @param message the API error message
     * @throws OsmServerException nearly always
     */
    private void generateException(@NonNull Storage apiStorage, @Nullable String type, @Nullable String idStr, int code, @Nullable String responseMessage,
            @Nullable String message) throws OsmServerException {
        if (type != null && idStr != null) {
            long osmId = Long.parseLong(idStr);
            OsmElement e = apiStorage.getOsmElement(type.toLowerCase(Locale.US), osmId);
            if (e != null) {
                throw new OsmServerException(code, e.getName(), e.getOsmId(), code + "=\"" + responseMessage + "\" ErrorMessage: " + message);
            }
        }
        Log.e(DEBUG_TAG, "Error message matched, but parsing failed: " + message);
    }

    /**
     * Read a stream to its "end" and return the results as a String
     * 
     * @param in an InputStream to read
     * @return a String containing the read contents
     */
    @NonNull
    private static String readStream(@Nullable final InputStream in) {
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
     * Read a single line from an InputStream
     * 
     * @param in the InputStream
     * @return a String containing the line without the EOL or null
     */
    @Nullable
    private static String readLine(@NonNull final InputStream in) {
        // TODO: Optimize? -> no Reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(in), 9);
        String res = null;
        try {
            res = reader.readLine();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem reading", e);
        }

        return res;
    }

    /**
     * Start an XML document for an OSM API operation
     * 
     * @param xmlSerializer an XmlSerializer instance
     * @param generator an identifier for the current application
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    static void startXml(@NonNull XmlSerializer xmlSerializer, @NonNull String generator) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startDocument(OsmXml.UTF_8, null);
        xmlSerializer.startTag("", "osm");
        xmlSerializer.attribute("", VERSION_KEY, API_VERSION);
        xmlSerializer.attribute("", GENERATOR_KEY, generator);
    }

    /**
     * End an XML document for an OSM API operation
     * 
     * @param xmlSerializer an XmlSerializer instance
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    static void endXml(@NonNull XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.endTag("", "osm");
        xmlSerializer.endDocument();
    }

    /**
     * Start an XML document for an OSM diff upload for a single action element
     * 
     * @param xmlSerializer an XmlSerializer instance
     * @param action the action (create, modify, delete)
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void startChangeXml(@NonNull XmlSerializer xmlSerializer, @NonNull String action)
            throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startDocument(OsmXml.UTF_8, null);
        xmlSerializer.startTag("", OSM_CHANGE_TAG);
        xmlSerializer.attribute("", VERSION_KEY, OSMCHANGE_VERSION);
        xmlSerializer.attribute("", GENERATOR_KEY, generator);
        xmlSerializer.startTag("", action);
        xmlSerializer.attribute("", VERSION_KEY, OSMCHANGE_VERSION);
        xmlSerializer.attribute("", GENERATOR_KEY, generator);
    }

    /**
     * End an XML document for an OSM diff upload for a single action element
     * 
     * @param xmlSerializer an XmlSerializer instance
     * @param action the action (create, modify, delete)
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    private void endChangeXml(@NonNull XmlSerializer xmlSerializer, @NonNull String action)
            throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.endTag("", action);
        xmlSerializer.endTag("", OSM_CHANGE_TAG);
        xmlSerializer.endDocument();
    }

    /**
     * Get an new XmlSerializer
     * 
     * @return an new XmlSerializer instance
     * @throws XmlPullParserException
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
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
     * Get the URL for creating an osm element
     * 
     * @param elem the OSM element
     * @return the URL
     * @throws MalformedURLException if the URL we tried to create was malformed
     */
    private URL getCreateElementUrl(@NonNull final OsmElement elem) throws MalformedURLException {
        return new URL(getReadWriteUrl() + elem.getName() + "/create");
    }

    /**
     * Get the URL for updating an osm element
     * 
     * @param elem the OSM element
     * @return the URL
     * @throws MalformedURLException if the URL we tried to create was malformed
     */
    @NonNull
    private URL getUpdateElementUrl(@NonNull final OsmElement elem) throws MalformedURLException {
        return new URL(getReadWriteUrl() + elem.getName() + "/" + elem.getOsmId());
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
     * Get the url for uploading a GPS track
     * 
     * @return the url
     * @throws MalformedURLException if the url couldn't be constructed properly
     */
    @NonNull
    private URL getUploadTrackUrl() throws MalformedURLException {
        return new URL(getReadWriteUrl() + "gpx/create");
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
     * @return the URL the OSM website, FIXME for now hardwired and a bit broken
     */
    public String getWebsiteBaseUrl() {
        return getBaseUrl(getReadWriteUrl()).replace("api.", "");
    }

    /*
     * New Notes API code mostly from old OSB implementation the relevant API documentation is still in flux so this
     * implementation may have issues
     */

    /**
     * Perform an HTTP request to download up to limit bugs inside the specified area. Blocks until the request is
     * complete.
     * 
     * @param area Latitude/longitude *1E7 of area to download.
     * @param limit maximum number of Notes to return, value of between 1 and 10000 is valid
     * @return All the Notes in the given area.
     */
    public Collection<Note> getNotesForBox(@NonNull BoundingBox area, long limit) {
        Collection<Note> result = new ArrayList<>();
        // http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
        try {
            Log.d(DEBUG_TAG, "getNotesForBox");
            URL url = getNotesForBox(limit, area);
            InputStream is = openConnection(null, url);
            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && "note".equals(tagName)) {
                    try {
                        result.add(new Note(parser));
                    } catch (IOException | XmlPullParserException | NumberFormatException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    }
                }
            }
        } catch (XmlPullParserException | IOException | OutOfMemoryError e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            return new ArrayList<>(); // empty list
        }
        Log.d(DEBUG_TAG, "Read " + result.size() + " notes from input");
        return result;
    }

    /**
     * Retrieve a single note
     * 
     * @param id the id of the Note to retrieve
     * @return the Note, null if not found or other error
     */
    public Note getNote(long id) {
        Note result = null;
        // http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
        try {
            Log.d(DEBUG_TAG, "getNote");
            URL url = getNoteUrl(Long.toString(id));
            InputStream is = openConnection(null, url);
            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && "note".equals(tagName)) {
                    try {
                        result = new Note(parser);
                    } catch (IOException | XmlPullParserException | NumberFormatException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    }
                }
            }
        } catch (XmlPullParserException | IOException | OutOfMemoryError e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            return null; // empty list
        }
        return result;
    }

    // The note 10597 was closed at 2017-09-24 17:59:18 UTC
    private static final Pattern ERROR_MESSAGE_NOTE_ALREADY_CLOSED = Pattern.compile("(?i)The note ([0-9]+) was closed at.*");
    //
    private static final Pattern ERROR_MESSAGE_NOTE_ALREADY_OPENED = Pattern.compile("(?i)The note ([0-9]+) is already open.*");

    // TODO rewrite to XML encoding (if supported)
    /**
     * Perform an HTTP request to add the specified comment to the specified bug.
     * 
     * Blocks until the request is complete. Will reopen the Note if it is already closed.
     * 
     * @param bug The bug to add the comment to.
     * @param comment The comment to add to the bug.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void addComment(@NonNull Note bug, @NonNull NoteComment comment) throws OsmServerException, IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "adding note comment " + bug.getId());
            // http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?id=<Bug ID>&text=<Comment with author and date>

            // setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body
            // which will fail
            String encodedComment = URLEncoder.encode(comment.getText(), OsmXml.UTF_8);
            URL addCommentUrl = getAddNoteCommentUrl(Long.toString(bug.getId()), encodedComment);

            Response response = openConnectionForAuthenticatedAccess(addCommentUrl, HTTP_POST, RequestBody.create(null, ""));

            int responseCode = response.code();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                    InputStream errorStream = response.body().byteStream();
                    String message = readStream(errorStream);
                    Log.d(DEBUG_TAG, "409: " + message);
                    Matcher m = ERROR_MESSAGE_NOTE_ALREADY_CLOSED.matcher(message);
                    if (m.matches()) {
                        String idStr = m.group(1);
                        Log.d(DEBUG_TAG, "Note " + idStr + " was already closed");
                        reopenNote(bug);
                        addComment(bug, comment);
                        return;
                    }
                    throwOsmServerException(response);
                } else {
                    throwOsmServerException(response);
                }
            }
            parseBug(bug, response.body().byteStream());
        }
    }

    // TODO rewrite to XML encoding
    /**
     * Perform an HTTP request to add the specified bug to the OpenStreetBugs database.
     * 
     * Blocks until the request is complete.
     * 
     * @param bug The bug to add.
     * @param comment The first comment for the bug.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void addNote(@NonNull Note bug, @NonNull NoteComment comment) throws XmlPullParserException, OsmServerException, IOException {
        if (bug.isNew()) {
            Log.d(DEBUG_TAG, "adding note");
            // http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?lat=<Latitude>&lon=<Longitude>&text=<Bug
            // description with author and date>&format=<Output format>

            String encodedComment = URLEncoder.encode(comment.getText(), OsmXml.UTF_8);
            URL addNoteUrl = getAddNoteUrl((bug.getLat() / 1E7d), (bug.getLon() / 1E7d), encodedComment);

            Response response = openConnectionForAuthenticatedAccess(addNoteUrl, HTTP_POST, RequestBody.create(null, ""));
            if (!response.isSuccessful()) {
                throwOsmServerException(response);
            }
            parseBug(bug, response.body().byteStream());
        }
    }

    // TODO rewrite to XML encoding
    /**
     * Perform an HTTP request to close the specified bug.
     * 
     * Blocks until the request is complete. If the note is already closed the error is ignored.
     * 
     * @param bug The bug to close.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void closeNote(@NonNull Note bug) throws OsmServerException, IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "closing note " + bug.getId());
            URL closeNoteUrl = getCloseNoteUrl(Long.toString(bug.getId()));
            Response response = openConnectionForAuthenticatedAccess(closeNoteUrl, HTTP_POST, RequestBody.create(null, ""));

            int responseCode = response.code();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                    InputStream errorStream = response.body().byteStream();
                    String message = readStream(errorStream);
                    Matcher m = ERROR_MESSAGE_NOTE_ALREADY_CLOSED.matcher(message);
                    if (m.matches()) {
                        String idStr = m.group(1);
                        Log.d(DEBUG_TAG, "Note " + idStr + " was already closed");
                        return;
                    }
                    throwOsmServerException(response);
                } else {
                    throwOsmServerException(response);
                }
            }
            parseBug(bug, response.body().byteStream());
        }
    }

    /**
     * Perform an HTTP request to reopen the specified bug.
     * 
     * Blocks until the request is complete. If the note is already open the error is ignored.
     * 
     * @param bug The bug to close.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void reopenNote(@NonNull Note bug) throws OsmServerException, IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "reopen note " + bug.getId());
            URL reopenNoteUrl = getReopenNoteUrl(Long.toString(bug.getId()));
            Response response = openConnectionForAuthenticatedAccess(reopenNoteUrl, HTTP_POST, RequestBody.create(null, ""));
            int responseCode = response.code();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                    InputStream errorStream = response.body().byteStream();
                    String message = readStream(errorStream);
                    Matcher m = ERROR_MESSAGE_NOTE_ALREADY_OPENED.matcher(message);
                    if (m.matches()) {
                        String idStr = m.group(1);
                        Log.d(DEBUG_TAG, "Note " + idStr + " was already open");
                        return;
                    }
                    throwOsmServerException(response);
                } else {
                    throwOsmServerException(response);
                }
            }
            parseBug(bug, response.body().byteStream());
        }
    }

    /**
     * Parse a single OSm note (bug) from an InputStream
     * 
     * @param bug bug to parse in to
     * @param inputStream the input
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void parseBug(@NonNull Note bug, @NonNull InputStream inputStream) throws IOException, XmlPullParserException {
        XmlPullParser parser = xmlParserFactory.newPullParser();
        parser.setInput(new BufferedInputStream(inputStream, StreamUtils.IO_BUFFER_SIZE), null);
        bug.parseNote(parser); // replace contents with result from server
        App.getTaskStorage().setDirty();
    }

    /**
     * GPS track API visibility/
     */
    public enum Visibility {
        PRIVATE, PUBLIC, TRACKABLE, IDENTIFIABLE
    }

    /**
     * Upload a GPS track in GPX format
     * 
     * @param track the track
     * @param description optional description
     * @param tags optional tags
     * @param visibility privacy/visibility setting
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    public void uploadTrack(@NonNull final Track track, @NonNull String description, @NonNull String tags, @NonNull Visibility visibility)
            throws MalformedURLException, ProtocolException, IOException {
        RequestBody gpxBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/gpx+xm");
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try {
                    track.exportToGPX(sink.outputStream());
                } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
                    throw new IOException(e);
                }
            }
        };
        String fileNamePart = DateFormatter.getFormattedString(DATE_PATTERN_GPX_TRACK_UPLOAD_SUGGESTED_FILE_NAME_PART);
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("description", description)
                .addFormDataPart("tags", tags).addFormDataPart("visibility", visibility.name().toLowerCase(Locale.US))
                .addFormDataPart("file", fileNamePart + ".gpx", gpxBody).build();
        Response response = openConnectionForAuthenticatedAccess(getUploadTrackUrl(), HTTP_POST, requestBody);
        if (!response.isSuccessful()) {
            throwOsmServerException(response);
        }
    }

    /**
     * 
     * @return true if we are using OAuth but have not retrieved the accesstoken yet
     */
    public boolean needOAuthHandshake() {
        return oauth && ((accesstoken == null) || (accesstokensecret == null));
    }

    /**
     * Override the oauth flag from the API configuration, only needed if inconsistent config
     * 
     * @param t the value to set the flag to
     */
    public void setOAuth(boolean t) {
        oauth = t;
    }

    /**
     * 
     * @return true if oauth is enabled
     */
    public boolean getOAuth() {
        return oauth;
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
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Authorize.startForResult(activity, new ActivityResultHandler.Listener() {
                            @Override
                            public void processResult(int resultCode, Intent result) {
                                if (Activity.RESULT_OK == resultCode) {
                                    restartAction.onSuccess();
                                } else {
                                    Log.w(DEBUG_TAG, "Authorized returned with " + resultCode);
                                    restartAction.onError();
                                }
                            }
                        });
                    }
                });
                if (server.getOAuth()) { // if still set
                    Snack.barError(activity, R.string.toast_oauth);
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
     * @throws XmlPullParserException
     */
    @NonNull
    XmlPullParser getXmlParser() throws XmlPullParserException {
        return xmlParserFactory.newPullParser();
    }

    /**
     * Construct and throw an OsmServerException from the connection to the server
     * 
     * @param response response from server
     * @throws IOException
     * @throws OsmServerException
     */
    public static void throwOsmServerException(Response response) throws OsmServerException, IOException {
        throwOsmServerException(response, null, response.code());
    }

    /**
     * Construct and throw an OsmServerException from the connection to the server
     * 
     * @param response response from server
     * @param e the OSM element that the error was caused by
     * @param responsecode code returned from server
     * @throws IOException
     * @throws OsmServerException
     */
    public static void throwOsmServerException(@NonNull final Response response, @Nullable final OsmElement e, int responsecode)
            throws IOException, OsmServerException {
        String responseMessage = response.message();
        if (responseMessage == null) {
            responseMessage = "";
        }
        InputStream in = response.body().byteStream();
        if (e == null) {
            Log.d(DEBUG_TAG, "response code " + responsecode + "response message " + responseMessage);
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
}
