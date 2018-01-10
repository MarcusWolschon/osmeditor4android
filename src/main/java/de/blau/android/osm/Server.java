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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.acra.ACRA;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.prefs.API;
import de.blau.android.services.util.StreamUtils;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteComment;
import de.blau.android.util.Base64;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.OAuthHelper;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

/**
 * @author mb
 */
public class Server {

    private static final String DEBUG_TAG = Server.class.getName();

    /**
     * Timeout for connections in milliseconds.
     */
    private static final int TIMEOUT = 45 * 1000;

    /**
     * Location of OSM API
     */
    private final String serverURL;

    /**
     * Location of optional read only OSM API
     */
    private final String readonlyURL;

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
    private static final String version = "0.6";

    private final String osmChangeVersion = "0.3";

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
     * @param apiurl The OSM API URL to use (e.g. "http://api.openstreetmap.org/api/0.6/").
     * @param username
     * @param password
     * @param oauth
     * @param generator the name of the editor.
     */
    public Server(Context context, final API api, final String generator) {
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
        discardedTags = new DiscardedTags(context);
    }

    /**
     * display name and message counts is the only thing that is interesting
     * 
     * @author simon
     *
     */
    public class UserDetails {
        public String display_name = "unknown";
        public int    received     = 0;
        public int    unread       = 0;
        public int    sent         = 0;
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
                HttpURLConnection connection = openConnectionForWriteAccess(getUserDetailsUrl(), "GET");
                try {
                    // connection.getOutputStream().close(); GET doesn't have an outputstream
                    checkResponseCode(connection);
                    XmlPullParser parser = xmlParserFactory.newPullParser();
                    parser.setInput(connection.getInputStream(), null);
                    int eventType;
                    result = new UserDetails();
                    boolean messages = false;
                    while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                        String tagName = parser.getName();
                        if (eventType == XmlPullParser.START_TAG && "user".equals(tagName)) {
                            result.display_name = parser.getAttributeValue(null, "display_name");
                            Log.d(DEBUG_TAG, "getUserDetails display name " + result.display_name);
                        }
                        if (eventType == XmlPullParser.START_TAG && "messages".equals(tagName)) {
                            messages = true;
                        }
                        if (eventType == XmlPullParser.END_TAG && "messages".equals(tagName)) {
                            messages = false;
                        }
                        if (messages) {
                            if (eventType == XmlPullParser.START_TAG && "received".equals(tagName)) {
                                result.received = Integer.parseInt(parser.getAttributeValue(null, "count"));
                                Log.d(DEBUG_TAG, "getUserDetails received " + result.received);
                                result.unread = Integer.parseInt(parser.getAttributeValue(null, "unread"));
                                Log.d(DEBUG_TAG, "getUserDetails unread " + result.unread);
                            }
                            if (eventType == XmlPullParser.START_TAG && "sent".equals(tagName)) {
                                result.sent = Integer.parseInt(parser.getAttributeValue(null, "count"));
                                Log.d(DEBUG_TAG, "getUserDetails sent " + result.sent);
                            }
                        }
                    }
                } finally {
                    disconnect(connection);
                }
            } catch (XmlPullParserException e) {
                Log.e(DEBUG_TAG, "Problem parsing user details", e);
            } catch (MalformedURLException e) {
                Log.e(DEBUG_TAG, "Problem retrieving user details", e);
            } catch (ProtocolException e) {
                Log.e(DEBUG_TAG, "Problem accessing user details", e);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Problem accessing user details", e);
            } catch (NumberFormatException e) {
                Log.e(DEBUG_TAG, "Problem accessing user details", e);
            }
            return result;
        }
        return userDetails; // might not make sense
    }

    /**
     * return the username for this server, may be null
     * 
     * @return
     */
    public String getDisplayName() {
        return username;
    }

    /**
     * @return true if a read only API URL is set
     */
    public boolean hasReadOnly() {
        return readonlyURL != null && !"".equals(readonlyURL);
    }

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
    public Capabilities getCapabilities() {
        try {
            Capabilities result = getCapabilities(getCapabilitiesUrl());
            if (result != null) {
                capabilities = result;
                capabilities.updateLimits();
            }
            return capabilities; // if retrieving failed return the default
        } catch (MalformedURLException e) {
            Log.e(DEBUG_TAG, "Problem with capabilities URL", e);
        }
        return null;
    }

    /**
     * Get the capabilities for the supplied URL
     * 
     * @param capabilitiesURL the URL for the API capabilities call
     * @return The capabilities for this server, or null if it couldn't be determined.
     */
    private Capabilities getCapabilities(URL capabilitiesURL) {
        Capabilities result;
        InputStream is = null;
        //
        try {
            Log.d(DEBUG_TAG, "getCapabilities using " + capabilitiesURL.toString());
            is = openConnection(null, capabilitiesURL);
            
            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(is, null);
            int eventType;
            result = new Capabilities();
            // very hackish just keys on tag names and not in which section of the response we are
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                try {
                    String tagName = parser.getName();
                    if (eventType == XmlPullParser.START_TAG && "version".equals(tagName)) {
                        result.minVersion = parser.getAttributeValue(null, "minimum");
                        result.maxVersion = parser.getAttributeValue(null, "maximum");
                        Log.d(DEBUG_TAG, "getCapabilities min/max API version " + result.minVersion + "/" + result.maxVersion);
                    }
                    if (eventType == XmlPullParser.START_TAG && "area".equals(tagName)) {
                        String maxArea = parser.getAttributeValue(null, "maximum");
                        if (maxArea != null) {
                            result.areaMax = Float.parseFloat(maxArea);
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum area " + maxArea);
                    }
                    if (eventType == XmlPullParser.START_TAG && "tracepoints".equals(tagName)) {
                        String perPage = parser.getAttributeValue(null, "per_page");
                        if (perPage != null) {
                            result.maxTracepointsPerPage = Integer.parseInt(perPage);
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum #tracepoints per page " + perPage);
                    }
                    if (eventType == XmlPullParser.START_TAG && "waynodes".equals(tagName)) {
                        String maximumWayNodes = parser.getAttributeValue(null, "maximum");
                        if (maximumWayNodes != null) {
                            result.maxWayNodes = Integer.parseInt(maximumWayNodes);
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum #nodes in a way " + maximumWayNodes);
                    }
                    if (eventType == XmlPullParser.START_TAG && "changesets".equals(tagName)) {
                        String maximumElements = parser.getAttributeValue(null, "maximum_elements");
                        if (maximumElements != null) {
                            result.maxElementsInChangeset = Integer.parseInt(maximumElements);
                        }
                        Log.d(DEBUG_TAG, "getCapabilities maximum elements in changesets " + maximumElements);
                    }
                    if (eventType == XmlPullParser.START_TAG && "timeout".equals(tagName)) {
                        String seconds = parser.getAttributeValue(null, "seconds");
                        if (seconds != null) {
                            result.timeout = Integer.parseInt(seconds);
                        }
                        Log.d(DEBUG_TAG, "getCapabilities timeout seconds " + seconds);
                    }
                    if (eventType == XmlPullParser.START_TAG && "status".equals(tagName)) {
                        result.dbStatus = Capabilities.stringToStatus(parser.getAttributeValue(null, "database"));
                        result.apiStatus = Capabilities.stringToStatus(parser.getAttributeValue(null, "api"));
                        result.gpxStatus = Capabilities.stringToStatus(parser.getAttributeValue(null, "gpx"));
                        Log.d(DEBUG_TAG, "getCapabilities service status DB " + result.dbStatus + " API " + result.apiStatus + " GPX " + result.gpxStatus);
                    }
                    if (eventType == XmlPullParser.START_TAG && "blacklist".equals(tagName)) {
                        if (result.getImageryBlacklist() == null) {
                            result.setImageryBlacklist(new ArrayList<String>());
                        }
                        String regex = parser.getAttributeValue(null, "regex");
                        if (regex != null) {
                            result.getImageryBlacklist().add(regex);
                        }
                        Log.d(DEBUG_TAG, "getCapabilities blacklist regex " + regex);
                    }
                } catch (NumberFormatException e) {
                    Log.e(DEBUG_TAG, "Problem accessing capabilities", e);
                }
            }
            return result;
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, "Problem parsing capabilities", e);
        } catch (ProtocolException e) {
            Log.e(DEBUG_TAG, "Problem accessing capabilities", e);
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem accessing capabilities", e);
        } finally {
            SavingHelper.close(is);
        }
        return null;
    }

    public boolean apiAvailable() {
        return capabilities.apiStatus.equals(Capabilities.Status.ONLINE) || capabilities.apiStatus.equals(Capabilities.Status.READONLY);
    }

    public boolean readableDB() {
        return capabilities.dbStatus.equals(Capabilities.Status.ONLINE) || capabilities.dbStatus.equals(Capabilities.Status.READONLY);
    }

    public boolean writableDB() {
        return capabilities.dbStatus.equals(Capabilities.Status.ONLINE);
    }

    public boolean readOnlyApiAvailable() {
        return readOnlyCapabilities.apiStatus.equals(Capabilities.Status.ONLINE) || readOnlyCapabilities.apiStatus.equals(Capabilities.Status.READONLY);
    }

    public boolean readOnlyReadableDB() {
        return readOnlyCapabilities.dbStatus.equals(Capabilities.Status.ONLINE) || readOnlyCapabilities.dbStatus.equals(Capabilities.Status.READONLY);
    }

    /**
     * Open a connection to an OSM server and request all data in box
     * 
     * @param context Android context
     * @param box the specified bounding box
     * @return the stream
     * @throws OsmServerException
     * @throws IOException
     */
    public InputStream getStreamForBox(@Nullable final Context context, final BoundingBox box) throws OsmServerException, IOException {
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
     * @throws OsmServerException
     * @throws IOException
     */
    public InputStream getStreamForElement(@Nullable final Context context, @Nullable final String mode, @NonNull final String type, final long id)
            throws OsmServerException, IOException {
        Log.d(DEBUG_TAG, "getStreamForElement");
        URL url = new URL(getReadOnlyUrl() + type + "/" + id + (mode != null ? "/" + mode : ""));
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
     * @throws OsmServerException
     * @throws IOException
     */
    public InputStream getStreamForElements(@Nullable final Context context, @NonNull final String type, final long[] ids)
            throws OsmServerException, IOException {
        Log.d(DEBUG_TAG, "getStreamForElements");

        StringBuilder urlString = new StringBuilder();
        urlString.append(getReadOnlyUrl());
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
     * @param context Android context
     * @param url the URL
     * @return the InputStream
     * @throws IOException
     * @throws OsmServerException
     */
    private InputStream openConnection(@Nullable final Context context, @NonNull URL url) throws IOException, OsmServerException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        boolean isServerGzipEnabled;

        Log.d(DEBUG_TAG, "get input stream for  " + url.toString());

        // --Start: header not yet send
        con.setReadTimeout(TIMEOUT);
        con.setConnectTimeout(TIMEOUT);
        con.setRequestProperty("Accept-Encoding", "gzip");
        con.setRequestProperty("User-Agent", App.getUserAgent());
        con.setInstanceFollowRedirects(true);

        // --Start: got response header
        isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

        // retry if we have no response-code or a redirect
        int responseCode = con.getResponseCode();
        if (responseCode == -1 || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
            Log.w(DEBUG_TAG, "openConnection no valid http response-code or redirect, trying again");
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                // this should only happen once for http->https, normal redirects work as is
                // and we don't want to support down grading
                boolean wasHttps = "https".equals(url.getProtocol());
                String newUrl = con.getHeaderField("Location");
                url = new URL(newUrl);
                if (wasHttps && "http".equals(url.getProtocol())) {
                    Log.w(DEBUG_TAG, "openConnectiion redirect to non-https URL " + newUrl);
                    throw new OsmServerException(responseCode, "Cannot downgrade from https to http");
                }
                Log.w(DEBUG_TAG, "openConnection redirecting to " + newUrl);
            }
            con = (HttpURLConnection) url.openConnection();
            // --Start: header not yet sent
            con.setReadTimeout(TIMEOUT);
            con.setConnectTimeout(TIMEOUT);
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setRequestProperty("User-Agent", App.getUserAgent());
            con.setInstanceFollowRedirects(true);

            // --Start: got response header
            isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
            responseCode = con.getResponseCode();
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            if (context != null && context instanceof Activity) {
                final int finalResponseCode = responseCode;
                final String responseMessage = con.getResponseMessage();
                if (responseCode == 400) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snack.barError((Activity) context, context.getString(R.string.toast_download_failed, finalResponseCode, responseMessage));
                        }
                    });
                } else {
                    ((Activity) context).runOnUiThread(new DownloadErrorToast(context, responseCode, con.getResponseMessage()));
                }
            }
            throwOsmServerException(con);
        }

        if (isServerGzipEnabled) {
            return new GZIPInputStream(con.getInputStream());
        } else {
            return con.getInputStream();
        }
    }

    class DownloadErrorToast implements Runnable {
        final int     code;
        final String  message;
        final Context context;

        DownloadErrorToast(Context context, int code, String message) {
            this.code = code;
            this.message = message;
            this.context = context;
        }

        @Override
        public void run() {
            if (context != null && context instanceof Activity) {
                try {
                    Snack.barError((Activity) context, context.getResources().getString(R.string.toast_download_failed, code, message));
                } catch (Exception ex) {
                    // do nothing ... this is stop bugs in the Android format parsing crashing the app, report the error
                    // because it is likely caused by a translation error
                    ACRA.getErrorReporter().putCustomData("STATUS", "NOCRASH");
                    ACRA.getErrorReporter().handleException(ex);
                }
            }
        }
    }

    /**
     * Sends an delete-request to the server.
     * 
     * @param elem the element which should be deleted.
     * @return true when the server indicates the successful deletion (HTTP 200), otherwise false.
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    public boolean deleteElement(@NonNull final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
        HttpURLConnection connection = null;
        // elem.addOrUpdateTag(createdByTag, createdByKey);
        Log.d(DEBUG_TAG, "Deleting " + elem.getName() + " #" + elem.getOsmId());
        try {
            connection = openConnectionForWriteAccess(getDeleteUrl(elem), "POST");
            sendPayload(connection, new XmlSerializable() {
                @Override
                public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                    final String action = "delete";
                    startChangeXml(serializer, action);
                    elem.toXml(serializer, changeSetId);
                    endChangeXml(serializer, action);
                }
            }, changesetId);
            checkResponseCode(connection, elem);
        } finally {
            disconnect(connection);
        }
        return true;
    }

    /**
     * Return true if either login/pass is set or if oAuth is enabled
     * 
     * @return
     */
    public boolean isLoginSet() {
        return (username != null && (password != null && !username.equals("") && !password.equals(""))) || oauth;
    }

    /**
     * @param connection
     */
    private static void disconnect(@Nullable final HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    /**
     * Update an individual elelemt on the server
     * 
     * @param elem the OsmELement to update
     * @return the new version number
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    public long updateElement(@NonNull final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
        long osmVersion = -1;
        HttpURLConnection connection = null;
        InputStream in = null;
        try {
            URL updateElementUrl = getUpdateUrl(elem);
            Log.d(DEBUG_TAG, "Updating " + elem.getName() + " #" + elem.getOsmId() + " " + updateElementUrl);
            connection = openConnectionForWriteAccess(updateElementUrl, "PUT");
            // remove redundant tags
            discardedTags.remove(elem);
            sendPayload(connection, new XmlSerializable() {
                @Override
                public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                    startXml(serializer);
                    elem.toXml(serializer, changeSetId);
                    endXml(serializer);
                }
            }, changesetId);
            checkResponseCode(connection, elem);
            in = connection.getInputStream();
            try {
                osmVersion = Long.parseLong(readLine(in));
            } catch (NumberFormatException e) {
                throw new OsmServerException(-1, "Server returned illegal element version " + e.getMessage());
            }
        } finally {
            disconnect(connection);
            SavingHelper.close(in);
        }
        return osmVersion;
    }

    /**
     * Send a XmlSerializable Object over a HttpUrlConnection
     * 
     * @param connection the connection
     * @param xmlSerializable the object
     * @param changeSetId changeset id to use
     * @throws OsmIOException thrown if a write or other error occurs
     */
    private void sendPayload(@NonNull final HttpURLConnection connection, @NonNull final XmlSerializable xmlSerializable, long changeSetId) throws OsmIOException {
        OutputStreamWriter out = null;
        try {
            XmlSerializer xmlSerializer = getXmlSerializer();
            out = new OutputStreamWriter(connection.getOutputStream(), Charset.defaultCharset());
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
     * Open a connection for writing to the API authenticating either with OAuth or basic authentication
     * 
     * This assumes a content type of "text/xml"
     * @param url           URL we want to open
     * @param requestMethod the request method
     * @return a HttpURLCOnnection
     * @throws IOException
     * @throws MalformedURLException
     * @throws ProtocolException
     */
    private HttpURLConnection openConnectionForWriteAccess(@NonNull final URL url, @NonNull final String requestMethod)
            throws IOException, MalformedURLException, ProtocolException {
        return openConnectionForWriteAccess(url, requestMethod, "text/xml");
    }

    /**
     * Open a connection for writing to the API authenticating either with OAuth or basic authentication
     * 
     * @param url           URL we want to open
     * @param requestMethod the request method
     * @param contentType   content time (mime string)
     * @return a HttpURLCOnnection
     * @throws IOException
     * @throws MalformedURLException
     * @throws ProtocolException
     */
    private HttpURLConnection openConnectionForWriteAccess(@NonNull final URL url, @NonNull final String requestMethod, @NonNull final String contentType)
            throws IOException, MalformedURLException, ProtocolException {
        Log.d(DEBUG_TAG, "openConnectionForWriteAccess url " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "" + contentType + "; charset=utf-8");
        connection.setRequestProperty("User-Agent", App.getUserAgent());
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.setRequestMethod(requestMethod);

        if (oauth) {
            OAuthHelper oa = new OAuthHelper();
            OAuthConsumer consumer = oa.getConsumer(getBaseUrl(getReadWriteUrl()));
            consumer.setTokenWithSecret(accesstoken, accesstokensecret);
            // sign the request
            try {
                consumer.sign(connection);
                // HttpParameters h = consumer.getRequestParameters();
            } catch (OAuthMessageSignerException | OAuthExpectationFailedException | OAuthCommunicationException e) { // user will get error when we actually try to write
                Log.e(DEBUG_TAG, "OAuth fail", e);
            } 
        } else {
            connection.setRequestProperty("Authorization", "Basic " + Base64.encode(username + ":" + password));
        }

        connection.setDoOutput(!"GET".equals(requestMethod));
        connection.setDoInput(true);
        return connection;
    }

    /**
     * Create a new element on the server
     * 
     * @param elem the OsmELement to create
     * @return the OSM id of the new element
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    public long createElement(@NonNull final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
        long osmId = -1;
        HttpURLConnection connection = null;
        InputStream in = null;
        // elem.addOrUpdateTag(createdByTag, createdByKey);

        try {
            connection = openConnectionForWriteAccess(getCreationUrl(elem), "PUT");
            sendPayload(connection, new XmlSerializable() {
                @Override
                public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                    startXml(serializer);
                    elem.toXml(serializer, changeSetId);
                    endXml(serializer);
                }
            }, changesetId);
            checkResponseCode(connection);
            in = connection.getInputStream();
            try {
                osmId = Long.parseLong(readLine(in));
            } catch (NumberFormatException e) {
                throw new OsmServerException(-1, "Server returned illegal element id " + e.getMessage());
            }
        } finally {
            disconnect(connection);
            SavingHelper.close(in);
        }
        return osmId;
    }

    /**
     * Test if changeset is at least potentially still open.
     * 
     * @return
     */
    public boolean hasOpenChangeset() {
        return changesetId != -1;
    }

    /**
     * Reset changeset id
     */
    public void resetChangeset() {
        changesetId = -1;
    }

    /**
     * Open a new changeset.
     * 
     * @param comment   value for the comment tag
     * @param source    value for the source tag
     * @param imagery   value for the imagery_used tag 
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    public void openChangeset(@Nullable final String comment, @Nullable final String source, @Nullable final String imagery) throws MalformedURLException, ProtocolException, IOException {
        long newChangesetId = -1;
        HttpURLConnection connection = null;
        InputStream in = null;

        if (changesetId != -1) { // potentially still open, check if really the case
            Changeset cs = getChangeset(changesetId);
            if (cs != null && cs.open) {
                Log.d(DEBUG_TAG, "Changeset #" + changesetId + " still open, reusing");
                updateChangeset(changesetId, comment, source, imagery);
                return;
            } else {
                changesetId = -1;
            }
        }
        try {
            XmlSerializable xmlData = changeSetTags(comment, source, imagery);
            connection = openConnectionForWriteAccess(getCreateChangesetUrl(), "PUT");
            sendPayload(connection, xmlData, changesetId);
            if (connection.getResponseCode() == -1) {
                // sometimes we get an invalid response-code the first time.
                disconnect(connection);
                connection = openConnectionForWriteAccess(getCreateChangesetUrl(), "PUT");
                sendPayload(connection, xmlData, changesetId);
            }
            checkResponseCode(connection);
            in = connection.getInputStream();
            try {
                newChangesetId = Long.parseLong(readLine(in));
            } catch (NumberFormatException e) {
                throw new OsmServerException(-1, "Server returned illegal changeset id " + e.getMessage());
            }
        } finally {
            disconnect(connection);
            SavingHelper.close(in);
        }
        changesetId = newChangesetId;
    }

    /**
     * Generate xml for the changeset tags
     * 
     * @param comment   value for the comment tag
     * @param source    value for the source tag
     * @param imagery   value for the imagery_used tag 
     * @return an XmlSerializable for the tags
     */
    private XmlSerializable changeSetTags(@Nullable final String comment, @Nullable final String source, @Nullable final String imagery) {
        return new XmlSerializable() {
            @Override
            public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                startXml(serializer);
                serializer.startTag("", "changeset");
                serializer.startTag("", "tag");
                serializer.attribute("", "k", "created_by");
                serializer.attribute("", "v", generator);
                serializer.endTag("", "tag");
                if (comment != null && comment.length() > 0) {
                    serializer.startTag("", "tag");
                    serializer.attribute("", "k", "comment");
                    serializer.attribute("", "v", comment);
                    serializer.endTag("", "tag");
                }
                if (source != null && source.length() > 0) {
                    serializer.startTag("", "tag");
                    serializer.attribute("", "k", "source");
                    serializer.attribute("", "v", source);
                    serializer.endTag("", "tag");
                }
                if (imagery != null && imagery.length() > 0) {
                    serializer.startTag("", "tag");
                    serializer.attribute("", "k", "imagery_used");
                    serializer.attribute("", "v", imagery);
                    serializer.endTag("", "tag");
                }
                serializer.startTag("", "tag");
                serializer.attribute("", "k", "locale");
                serializer.attribute("", "v", Locale.getDefault().toString());
                serializer.endTag("", "tag");
                serializer.endTag("", "changeset");
                endXml(serializer);
            }
        };
    }

    /**
     * Close the current open changeset, will zap the stored id even if the closing fails, this will force using a new
     * changeset on the next upload
     * 
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    public void closeChangeset() throws MalformedURLException, ProtocolException, IOException {
        HttpURLConnection connection = null;

        try {
            connection = openConnectionForWriteAccess(getCloseChangesetUrl(changesetId), "PUT");
            checkResponseCode(connection);
        } finally {
            disconnect(connection);
            changesetId = -1;
        }
    }

    /**
     * Right now just what we need
     * 
     * @author simon
     *
     */
    public class Changeset {
        public boolean open = false;
    }

    /**
     * Retrieve information for a specific changeset
     * 
     * @param id    id of the changeset
     * @return a Changeset object
     */
    @Nullable
    private Changeset getChangeset(long id) {
        Changeset result = null;
        HttpURLConnection connection = null;
        try {
            connection = openConnectionForWriteAccess(getChangesetUrl(changesetId), "GET");
            checkResponseCode(connection);

            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(connection.getInputStream(), null);
            int eventType;
            result = new Changeset();

            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && "changeset".equals(tagName)) {
                    result.open = parser.getAttributeValue(null, "open").equals("true");
                    Log.d(DEBUG_TAG, "Changeset #" + id + " is " + (result.open ? "open" : "closed"));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Log.d(DEBUG_TAG, "getChangeset got " + e.getMessage());
        }  finally {
            disconnect(connection);
        }
        return result;
    }

    /**
     * Update an existing changeset
     * 
     * @param changesetId   the id of the changeset
     * @param comment       value for the comment tag
     * @param source        value for the source tag
     * @param imagery       value for the imagery_used tag 
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    private void updateChangeset(final long changesetId, @Nullable final String comment, @Nullable final String source, @Nullable final String imagery)
            throws MalformedURLException, ProtocolException, IOException {

        HttpURLConnection connection = null;
        InputStream in = null;

        try {
            XmlSerializable xmlData = changeSetTags(comment, source, imagery);
            connection = openConnectionForWriteAccess(getChangesetUrl(changesetId), "PUT");
            sendPayload(connection, xmlData, changesetId);
            checkResponseCode(connection);
            // ignore response for now
        } finally {
            disconnect(connection);
            SavingHelper.close(in);
        }
    }

   /**
    * Check the response code from a HttpURLConnection and if not OK throw an exception
    * 
    * @param connection the HttpURLConnection
    * @throws IOException
    * @throws OsmException
    */
    private void checkResponseCode(@Nullable final HttpURLConnection connection) throws IOException, OsmException {
        checkResponseCode(connection, null);
    }

    /**
     * Check the response code from a HttpURLConnection and if not OK throw an exception
     * 
     * @param connection the HttpURLConnection
     * @param e          an OsmElement associated with the problem or null
     * @throws IOException
     * @throws OsmException
     */
    private void checkResponseCode(@Nullable final HttpURLConnection connection, @Nullable final OsmElement e) throws IOException, OsmException {
        int responsecode = -1;
        if (connection == null) {
            throw new OsmServerException(responsecode, "Unknown error");
        }
        responsecode = connection.getResponseCode();
        Log.d(DEBUG_TAG, "response code " + responsecode);
        if (responsecode == -1) {
            throw new IOException("Invalid response from server");
        }
        if (responsecode != HttpURLConnection.HTTP_OK) {
            if (responsecode == HttpURLConnection.HTTP_GONE && e.getState() == OsmElement.STATE_DELETED) {
                // FIXME we tried to delete an already deleted element: log, but ignore, maybe it would be better to ask
                // user
                Log.d(DEBUG_TAG, e.getOsmId() + " already deleted on server");
                return;
            }
            throwOsmServerException(connection, e, responsecode);
            // TODO: happens the first time on some uploads. responseMessage=ErrorMessage="", works the second time
        }
    }

    /**
     * Upload edits in OCS format and process the server response
     * 
     * @param delegator reference to the StorageDelegator
     * @throws MalformedURLException
     * @throws ProtocolException
     * @throws IOException
     */
    public void diffUpload(StorageDelegator delegator) throws MalformedURLException, ProtocolException, IOException {
        HttpURLConnection connection = null;
        InputStream in = null;
        try {
            connection = openConnectionForWriteAccess(getDiffUploadUrl(changesetId), "POST");
            for (OsmElement elem : delegator.getApiStorage().getElements()) {
                if (elem.state != OsmElement.STATE_DELETED) {
                    discardedTags.remove(elem);
                }
            }
            delegator.writeOsmChange(connection.getOutputStream(), changesetId, getCachedCapabilities().maxElementsInChangeset);
            processDiffUploadResult(delegator, connection, xmlParserFactory.newPullParser());
        } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException e1) {
            throw new OsmException(e1.getMessage());
        }  finally {
            disconnect(connection);
            SavingHelper.close(in);
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
            .compile("(?i)Precondition failed: (Node|Way) ([0-9]+) is still used by (way|relation)[s]? ([0-9]+).*");
    private static final Pattern ERROR_MESSAGE_PRECONDITION_RELATION_RELATION = Pattern
            .compile("(?i)Precondition failed: The relation ([0-9]+) is used in relation ([0-9]+).");

    /**
     * Process the results of uploading a diff to the API, here because it needs to manipulate the stored data
     * 
     * Note: we try to process as much as possible outside of real parser errors, as the data has already been
     * successfully uploaded to the API, the caller needs to assure that we do not get recalled on the non fatal errors.
     * 
     * @param delegator the StorageDelegator containing to data to update
     * @param connection connection to the API
     * @param parser parser instance
     * @throws IOException on an error processing the data
     */
    private void processDiffUploadResult(StorageDelegator delegator, HttpURLConnection connection, XmlPullParser parser) throws IOException {
        Storage apiStorage = delegator.getApiStorage();
        int code = connection.getResponseCode();
        if (code == HttpURLConnection.HTTP_OK) {
            boolean rehash = false; // if ids are changed we need to rehash
                                    // storage
            try {
                parser.setInput(new BufferedInputStream(connection.getInputStream(), StreamUtils.IO_BUFFER_SIZE), null);
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
                            if ("node".equals(tagName) || "way".equals(tagName) || "relation".equals(tagName)) {
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
                                    Log.e(DEBUG_TAG, "" + e + " not found in api storage!");
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
            } catch (XmlPullParserException e) {
                throw new OsmException(e.toString());
            } catch (NumberFormatException e) {
                throw new OsmException(e.toString());
            } catch (IOException e) {
                throw new OsmException(e.toString());
            }
        } else {
            String message = Server.readStream(connection.getErrorStream());
            String responseMessage = connection.getResponseMessage();
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
                    }
                    Log.e(DEBUG_TAG, "Unknown error message: " + message);
                }
            }
            throw new OsmServerException(code, message);
        }
    }

    private void generateException(Storage apiStorage, String type, String idStr, int code, String responseMessage, String message) throws OsmServerException {
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

    private void startXml(@NonNull XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startDocument("UTF-8", null);
        xmlSerializer.startTag("", "osm");
        xmlSerializer.attribute("", "version", version);
        xmlSerializer.attribute("", "generator", generator);
    }

    private void endXml(@NonNull XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.endTag("", "osm");
        xmlSerializer.endDocument();
    }

    private void startChangeXml(@NonNull XmlSerializer xmlSerializer, @NonNull String action) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.startDocument("UTF-8", null);
        xmlSerializer.startTag("", "osmChange");
        xmlSerializer.attribute("", "version", osmChangeVersion);
        xmlSerializer.attribute("", "generator", generator);
        xmlSerializer.startTag("", action);
        xmlSerializer.attribute("", "version", osmChangeVersion);
        xmlSerializer.attribute("", "generator", generator);
    }

    private void endChangeXml(@NonNull XmlSerializer xmlSerializer, @NonNull String action) throws IllegalArgumentException, IllegalStateException, IOException {
        xmlSerializer.endTag("", action);
        xmlSerializer.endTag("", "osmChange");
        xmlSerializer.endDocument();
    }

    private XmlSerializer getXmlSerializer() throws XmlPullParserException, IllegalArgumentException, IllegalStateException, IOException {
        XmlSerializer serializer = xmlParserFactory.newSerializer();
        serializer.setPrefix("", "");
        return serializer;
    }

    private URL getCreationUrl(@NonNull final OsmElement elem) throws MalformedURLException {
        return new URL(getReadWriteUrl() + elem.getName() + "/create");
    }

    private URL getCreateChangesetUrl() throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + "create");
    }

    private URL getCloseChangesetUrl(long changesetId) throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changesetId + "/close");
    }

    private URL getChangesetUrl(long changesetId) throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changesetId);
    }

    private URL getUpdateUrl(@NonNull final OsmElement elem) throws MalformedURLException {
        return new URL(getReadWriteUrl() + elem.getName() + "/" + elem.getOsmId());
    }

    private URL getDeleteUrl(@NonNull final OsmElement elem) throws MalformedURLException {
        // return getUpdateUrl(elem);
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changesetId + "/upload");
    }

    private URL getDiffUploadUrl(long changeSetId) throws MalformedURLException {
        return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changeSetId + "/upload");
    }

    private URL getUserDetailsUrl() throws MalformedURLException {
        return new URL(getReadWriteUrl() + "user/details");
    }

    private URL getAddCommentUrl(@NonNull String noteId, @NonNull String comment) throws MalformedURLException {
        return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/comment?text=" + comment);
    }

    private URL getNoteUrl(@NonNull String noteId) throws MalformedURLException {
        return new URL(getNotesReadOnlyUrl() + SERVER_NOTES_PATH + noteId);
    }

    /**
     * Return for now general read write API url as a string
     * 
     * @return
     */
    private String getNotesUrl() {
        return serverURL;
    }

    /**
     * Return either the general read write API url as a string or a specific to notes one
     * 
     * @return
     */
    private String getNotesReadOnlyUrl() {
        if (notesURL == null || "".equals(notesURL)) {
            return serverURL;
        } else {
            return notesURL;
        }
    }

    private URL getNotesForBox(long limit, @NonNull BoundingBox area) throws MalformedURLException {
        return new URL(getNotesReadOnlyUrl() + "notes?" + "limit=" + limit + "&" + "bbox=" + area.getLeft() / 1E7d + "," + area.getBottom() / 1E7d + ","
                + area.getRight() / 1E7d + "," + area.getTop() / 1E7d);
    }

    private URL getAddNoteUrl(double latitude, double longitude, @NonNull String comment) throws MalformedURLException {
        return new URL(getNotesUrl() + "notes?lat=" + latitude + "&lon=" + longitude + "&text=" + comment);
    }

    private URL getCloseNoteUrl(@NonNull String noteId) throws MalformedURLException {
        return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/close");
    }

    private URL getReopenNoteUrl(@NonNull String noteId) throws MalformedURLException {
        return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/reopen");
    }

    private URL getUploadTrackUrl() throws MalformedURLException {
        return new URL(getReadWriteUrl() + "gpx/create");
    }

    private URL getCapabilitiesUrl() throws MalformedURLException {
        return getCapabilitiesUrl(getReadOnlyUrl());
    }

    private URL getReadOnlyCapabilitiesUrl() throws MalformedURLException {
        return getCapabilitiesUrl(getReadWriteUrl());
    }

    private URL getCapabilitiesUrl(String url) throws MalformedURLException {
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
     * @return
     */
    private String getReadOnlyUrl() {
        if (readonlyURL == null || "".equals(readonlyURL)) {
            return serverURL;
        } else {
            return readonlyURL;
        }
    }

    /**
     * @return the base URL, i.e. the url with the "/api/version/"-part stripped
     */
    public static String getBaseUrl(String url) {
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
     * @return All the bugs in the given area.
     */
    public Collection<Note> getNotesForBox(@NonNull BoundingBox area, long limit) {
        Collection<Note> result = new ArrayList<>();
        // http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
        try {
            Log.d(DEBUG_TAG, "getNotesForBox");
            URL url = getNotesForBox(limit, area);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            boolean isServerGzipEnabled;

            // --Start: header not yet send
            con.setReadTimeout(TIMEOUT);
            con.setConnectTimeout(TIMEOUT);
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setRequestProperty("User-Agent", App.getUserAgent());

            // --Start: got response header
            isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new ArrayList<>(); // TODO Return empty list ... this is better than throwing an uncatched
                                          // exception, but we should provide some user feedback
                // throw new UnexpectedRequestException(con);
            }

            InputStream is;
            if (isServerGzipEnabled) {
                is = new GZIPInputStream(con.getInputStream());
            } else {
                is = con.getInputStream();
            }

            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && "note".equals(tagName)) {
                    try {
                        result.add(new Note(parser));
                    } catch (IOException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    } catch (XmlPullParserException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    } catch (NumberFormatException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            return new ArrayList<>(); // empty list
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            return new ArrayList<>(); // empty list
        } catch (OutOfMemoryError e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            // TODO ask the user to exit
            return new ArrayList<>(); // empty list
        }
        Log.d(DEBUG_TAG, "Read " + result.size() + " notes from input");
        return result;
    }

    /**
     * Retrieve a single note
     * 
     * @param id
     * @return
     */
    public Note getNote(long id) {
        Note result = null;
        // http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
        try {
            Log.d(DEBUG_TAG, "getNote");
            URL url = getNoteUrl(Long.toString(id));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            boolean isServerGzipEnabled;

            // --Start: header not yet send
            con.setReadTimeout(TIMEOUT);
            con.setConnectTimeout(TIMEOUT);
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.setRequestProperty("User-Agent", App.getUserAgent());

            // --Start: got response header
            isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null; // TODO Return empty list ... this is better than throwing an uncatched exception, but we
                             // should provide some user feedback
                // throw new UnexpectedRequestException(con);
            }

            InputStream is;
            if (isServerGzipEnabled) {
                is = new GZIPInputStream(con.getInputStream());
            } else {
                is = con.getInputStream();
            }

            XmlPullParser parser = xmlParserFactory.newPullParser();
            parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                if (eventType == XmlPullParser.START_TAG && "note".equals(tagName)) {
                    try {
                        result = new Note(parser);
                    } catch (IOException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    } catch (XmlPullParserException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    } catch (NumberFormatException e) {
                        // if the bug doesn't parse correctly, there's nothing
                        // we can do about it - move on
                        Log.e(DEBUG_TAG, "Problem parsing bug", e);
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            return null; // empty list
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            return null; // empty list
        } catch (OutOfMemoryError e) {
            Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
            // TODO ask the user to exit
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
     * @return true if the comment was successfully added.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void addComment(@NonNull Note bug, @NonNull NoteComment comment) throws OsmServerException, IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "adding note comment " + bug.getId());
            // http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?id=<Bug ID>&text=<Comment with author and date>
            HttpURLConnection connection = null;
            try {
                // setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body
                // which will fail
                String encodedComment = URLEncoder.encode(comment.getText(), "UTF-8");
                URL addCommentUrl = getAddCommentUrl(Long.toString(bug.getId()), encodedComment);
                connection = openConnectionForWriteAccess(addCommentUrl, "POST", "text/url");
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), Charset.defaultCharset());

                // out.write("text="+URLEncoder.encode(comment.getText(), "UTF-8")+ "\r\n");
                out.flush();
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                        InputStream errorStream = connection.getErrorStream();
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
                        throwOsmServerException(connection);
                    } else {
                        throwOsmServerException(connection);
                    }
                }
                parseBug(bug, connection.getInputStream());
            } finally {
                disconnect(connection);
            }
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
     * @return true if the bug was successfully added.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void addNote(@NonNull Note bug, @NonNull NoteComment comment) throws XmlPullParserException, OsmServerException, IOException {
        if (bug.isNew()) {
            Log.d(DEBUG_TAG, "adding note");
            // http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?lat=<Latitude>&lon=<Longitude>&text=<Bug
            // description with author and date>&format=<Output format>
            HttpURLConnection connection = null;
            try {
                // setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body
                // which will fail
                String encodedComment = URLEncoder.encode(comment.getText(), "UTF-8");
                URL addNoteUrl = getAddNoteUrl((bug.getLat() / 1E7d), (bug.getLon() / 1E7d), encodedComment);
                connection = openConnectionForWriteAccess(addNoteUrl, "POST", "text/xml");
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), Charset.defaultCharset());
                // out.write("text="+URLEncoder.encode(comment.getText(), "UTF-8") + "\r\n");
                out.flush();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throwOsmServerException(connection);
                }
                parseBug(bug, connection.getInputStream());
            } finally {
                disconnect(connection);
            }
        }
    }

    // TODO rewrite to XML encoding
    /**
     * Perform an HTTP request to close the specified bug.
     * 
     * Blocks until the request is complete. If the note is already closed the error is ignored.
     * 
     * @param bug The bug to close.
     * @return true if the bug was successfully closed.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void closeNote(@NonNull Note bug) throws OsmServerException, IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "closing note " + bug.getId());
            HttpURLConnection connection = null;
            try {
                // setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body
                // which will fail
                URL closeNoteUrl = getCloseNoteUrl(Long.toString(bug.getId()));
                connection = openConnectionForWriteAccess(closeNoteUrl, "POST", "text/xml");
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                        InputStream errorStream = connection.getErrorStream();
                        String message = readStream(errorStream);
                        Matcher m = ERROR_MESSAGE_NOTE_ALREADY_CLOSED.matcher(message);
                        if (m.matches()) {
                            String idStr = m.group(1);
                            Log.d(DEBUG_TAG, "Note " + idStr + " was already closed");
                            return;
                        }
                        throwOsmServerException(connection);
                    } else {
                        throwOsmServerException(connection);
                    }
                }
                parseBug(bug, connection.getInputStream());
            } finally {
                disconnect(connection);
            }
        }
    }

    /**
     * Perform an HTTP request to reopen the specified bug.
     * 
     * Blocks until the request is complete. If the note is already open the error is ignored.
     * 
     * @param bug The bug to close.
     * @return true if the bug was successfully closed.
     * @throws IOException
     * @throws OsmServerException
     * @throws XmlPullParserException
     */
    public void reopenNote(@NonNull Note bug) throws OsmServerException, IOException, XmlPullParserException {
        if (!bug.isNew()) {
            Log.d(DEBUG_TAG, "reopen note " + bug.getId());
            HttpURLConnection connection = null;
            try {
                URL reopenNoteUrl = getReopenNoteUrl(Long.toString(bug.getId()));
                connection = openConnectionForWriteAccess(reopenNoteUrl, "POST", "text/xml");
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                        InputStream errorStream = connection.getErrorStream();
                        String message = readStream(errorStream);
                        Matcher m = ERROR_MESSAGE_NOTE_ALREADY_OPENED.matcher(message);
                        if (m.matches()) {
                            String idStr = m.group(1);
                            Log.d(DEBUG_TAG, "Note " + idStr + " was already open");
                            return;
                        }
                        throwOsmServerException(connection);
                    } else {
                        throwOsmServerException(connection);
                    }
                }
                parseBug(bug, connection.getInputStream());
            } finally {
                disconnect(connection);
            }
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
        bug.parseBug(parser); // replace contents with result from server
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
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws XmlPullParserException
     */
    public void uploadTrack(@NonNull Track track, @NonNull String description, @NonNull String tags, @NonNull Visibility visibility)
            throws MalformedURLException, ProtocolException, IOException, IllegalArgumentException, IllegalStateException, XmlPullParserException {
        HttpURLConnection connection = null;
        try {
            //
            String boundary = "*VESPUCCI*";
            String separator = "--" + boundary + "\r\n";
            connection = openConnectionForWriteAccess(getUploadTrackUrl(), "POST", "multipart/form-data;boundary=" + boundary);
            OutputStream os = connection.getOutputStream();
            OutputStreamWriter out = new OutputStreamWriter(os, Charset.defaultCharset());
            out.write(separator);
            out.write("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
            out.write(description + "\r\n");
            out.write(separator);
            out.write("Content-Disposition: form-data; name=\"tags\"\r\n\r\n");
            out.write(tags + "\r\n");
            out.write(separator);
            out.write("Content-Disposition: form-data; name=\"visibility\"\r\n\r\n");
            out.write(visibility.name().toLowerCase(Locale.US) + "\r\n");
            out.write(separator);
            String fileNamePart = DateFormatter.getFormattedString(DATE_PATTERN_GPX_TRACK_UPLOAD_SUGGESTED_FILE_NAME_PART);
            out.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileNamePart + ".gpx\"\r\n");
            out.write("Content-Type: application/gpx+xml\r\n\r\n");
            out.flush();
            track.exportToGPX(os);
            os.flush();
            out.write("\r\n");
            out.write("--" + boundary + "--\r\n");
            out.flush();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throwOsmServerException(connection);
            }
        } finally {
            disconnect(connection);
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
     * @param t
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
     * Construct and throw an OsmServerException from the connection to the server
     * 
     * @param connection connection to server
     * @throws IOException
     * @throws OsmServerException
     */
    private void throwOsmServerException(@NonNull final HttpURLConnection connection) throws IOException, OsmServerException {
        throwOsmServerException(connection, null, connection.getResponseCode());
    }

    /**
     * Construct and throw an OsmServerException from the connection to the server
     * 
     * @param connection connection connection to server
     * @param e the OSM element that the error was caused by
     * @param responsecode code returen from server
     * @throws IOException
     * @throws OsmServerException
     */
    private void throwOsmServerException(@NonNull final HttpURLConnection connection, @Nullable final OsmElement e, int responsecode) throws IOException, OsmServerException {
        String responseMessage = connection.getResponseMessage();
        if (responseMessage == null) {
            responseMessage = "";
        }
        InputStream in = connection.getErrorStream();
        if (e == null) {
            Log.d(DEBUG_TAG, "respone code " + responsecode + "response message " + responseMessage);
            throw new OsmServerException(responsecode, readStream(in));
        } else {
            throw new OsmServerException(responsecode, e.getName(), e.getOsmId(), readStream(in));
        }
    }

}
