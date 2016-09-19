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

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.services.util.StreamUtils;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteComment;
import de.blau.android.util.Base64;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.OAuthHelper;
import de.blau.android.util.SavingHelper;
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
	 * Base server URL of the OpenStreetMap API
	 */
	private static final String SERVER_BASE_URL = "http://api.openstreetmap.org";

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
	 * @param apiurl The OSM API URL to use (e.g. "http://api.openstreetmap.org/api/0.6/").
	 * @param username
	 * @param password
	 * @param oauth 
	 * @param generator the name of the editor.
	 */
	public Server(final API api,final String generator) {
		Log.d(DEBUG_TAG, "constructor");
		if (api.url != null && !api.url.equals("")) {
			this.serverURL = api.url;
		} else {
			this.serverURL = SERVER_BASE_URL + "/" + SERVER_API_PATH + version + "/"; // probably not needed anymore
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
		discardedTags = new DiscardedTags();
	}
	
	/**
	 * display name and message counts is the only thing that is interesting
	 * @author simon
	 *
	 */
	public class UserDetails {
		public String	display_name = "unknown";
		public int	received = 0;
		public int unread = 0;
		public int sent = 0;
	}
	
	/**
	 * Get the details for the user.
	 * @return The display name for the user, or null if it couldn't be determined.
	 */
	public UserDetails getUserDetails() {
		UserDetails result = null;
		if (userDetails == null) {
			// Haven't retrieved the details from OSM - try to
			try {
				HttpURLConnection connection = openConnectionForWriteAccess(getUserDetailsUrl(), "GET");
				try {
					//connection.getOutputStream().close(); GET doesn't have an outputstream
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
							Log.d(DEBUG_TAG,"getUserDetails display name " + result.display_name);
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
								Log.d(DEBUG_TAG,"getUserDetails received " + result.received);
								result.unread = Integer.parseInt(parser.getAttributeValue(null, "unread"));
								Log.d(DEBUG_TAG,"getUserDetails unread " + result.unread);
							}
							if (eventType == XmlPullParser.START_TAG && "sent".equals(tagName)) {
								result.sent = Integer.parseInt(parser.getAttributeValue(null, "count"));
								Log.d(DEBUG_TAG,"getUserDetails sent " + result.sent);
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
	
	public Capabilities  getReadOnlyCapabilities() {
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

	
	public Capabilities getCachedCapabilities() {
		return capabilities;
	}
	
	
	/**
	 * Get the capabilities for the current API
	 * Side effect set capabilities field and update limits that are used elsewhere
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
	 * @param capabilitiesURL
	 * @return The capabilities for this server, or null if it couldn't be determined.
	 */
	public Capabilities getCapabilities(URL capabilitiesURL) {
		Capabilities result;
		HttpURLConnection con = null;
		// 
		try {		
			Log.d(DEBUG_TAG,"getCapabilities using " + capabilitiesURL.toString());
			con = (HttpURLConnection) capabilitiesURL.openConnection();
			//--Start: header not yet send
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("User-Agent", Application.userAgent);

			//connection.getOutputStream().close(); GET doesn't have an outputstream
			checkResponseCode(con);
			XmlPullParser parser = xmlParserFactory.newPullParser();
			parser.setInput(con.getInputStream(), null);
			int eventType;
			result = new Capabilities();
			// very hackish just keys on tag names and not in which section of the response we are
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				try {
					String tagName = parser.getName();
					if (eventType == XmlPullParser.START_TAG && "version".equals(tagName)) {
						result.minVersion = parser.getAttributeValue(null, "minimum");
						result.maxVersion = parser.getAttributeValue(null, "maximum");
						Log.d(DEBUG_TAG,"getCapabilities min/max API version " + result.minVersion + "/" + result.maxVersion);
					}
					if (eventType == XmlPullParser.START_TAG && "area".equals(tagName)) {
						String maxArea = parser.getAttributeValue(null, "maximum");
						if (maxArea != null) {
							result.areaMax = Float.parseFloat(maxArea);
						}
						Log.d(DEBUG_TAG,"getCapabilities maximum area " + maxArea);
					}
					if (eventType == XmlPullParser.START_TAG && "tracepoints".equals(tagName)) {
						String perPage = parser.getAttributeValue(null, "per_page");
						if (perPage != null) {
							result.maxTracepointsPerPage = Integer.parseInt(perPage);
						}
						Log.d(DEBUG_TAG,"getCapabilities maximum #tracepoints per page " + perPage);
					}
					if (eventType == XmlPullParser.START_TAG && "waynodes".equals(tagName)) {
						String maximumWayNodes = parser.getAttributeValue(null, "maximum");
						if (maximumWayNodes != null) {
							result.maxWayNodes = Integer.parseInt(maximumWayNodes);
						}
						Log.d(DEBUG_TAG,"getCapabilities maximum #nodes in a way " + maximumWayNodes);
					}
					if (eventType == XmlPullParser.START_TAG && "changesets".equals(tagName)) {
						String maximumElements = parser.getAttributeValue(null, "maximum_elements");
						if (maximumElements != null) {
							result.maxElementsInChangeset = Integer.parseInt(maximumElements);
						}
						Log.d(DEBUG_TAG,"getCapabilities maximum elements in changesets " + maximumElements);
					}
					if (eventType == XmlPullParser.START_TAG && "timeout".equals(tagName)) {
						String seconds = parser.getAttributeValue(null, "seconds");
						if (seconds != null) {
							result.timeout = Integer.parseInt(seconds);
						}
						Log.d(DEBUG_TAG,"getCapabilities timeout seconds " + seconds);
					}
					if (eventType == XmlPullParser.START_TAG && "status".equals(tagName)) {
						result.dbStatus = Capabilities.stringToStatus(parser.getAttributeValue(null, "database"));
						result.apiStatus = Capabilities.stringToStatus(parser.getAttributeValue(null, "api"));
						result.gpxStatus = Capabilities.stringToStatus(parser.getAttributeValue(null, "gpx"));
						Log.d(DEBUG_TAG,"getCapabilities service status DB " + result.dbStatus + " API " + result.apiStatus + " GPX " + result.gpxStatus);
					}	
					if (eventType == XmlPullParser.START_TAG && "blacklist".equals(tagName)) {
						if (result.imageryBlacklist == null) {
							result.imageryBlacklist = new ArrayList<String>();
						}
						String regex = parser.getAttributeValue(null, "regex");
						if (regex != null) {
							result.imageryBlacklist.add(regex);
						}
						Log.d(DEBUG_TAG,"getCapabilities blacklist regex " + regex);
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
			disconnect(con);
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
	 * @param area
	 * @return
	 * @throws IOException
	 * @throws OsmServerException
	 */
	public InputStream getStreamForBox(final BoundingBox box) throws OsmServerException, IOException {
		Log.d(DEBUG_TAG, "getStreamForBox");
		URL url = new URL(getReadOnlyUrl()  + "map?bbox=" + box.toApiString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		boolean isServerGzipEnabled;

		Log.d(DEBUG_TAG, "getStreamForBox " + url.toString());
		
		//--Start: header not yet send
		con.setReadTimeout(TIMEOUT);
		con.setConnectTimeout(TIMEOUT);
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setRequestProperty("User-Agent", Application.userAgent);

		//--Start: got response header
		isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

		// retry if we have no response-code
		if (con.getResponseCode() == -1) {
			Log.w(getClass().getName()+ ":getStreamForBox", "no valid http response-code, trying again");
			con = (HttpURLConnection) url.openConnection();
			//--Start: header not yet send
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("Accept-Encoding", "gzip");
			con.setRequestProperty("User-Agent", Application.userAgent);

			//--Start: got response header
			isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
		}

		if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
			if (con.getResponseCode() == 400) {
				Application.mainActivity.runOnUiThread(new Runnable() {
					  @Override
					public void run() {
						  Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_download_bbox_failed, Toast.LENGTH_LONG).show();
					  }
				});
			}
			else {
				Application.mainActivity.runOnUiThread(new DownloadErrorToast(con.getResponseCode(), con.getResponseMessage()));
			}
			throwUnexpectedRequestException(con);
		}

		if (isServerGzipEnabled) {
			return new GZIPInputStream(con.getInputStream());
		} else {
			return con.getInputStream();
		}
	}
	
	/**
	 * Get a single element from the API
	 * @param full TODO
	 * @param type
	 * @param id
	 * @return
	 * @throws OsmServerException
	 * @throws IOException
	 */
	public InputStream getStreamForElement(String mode, final String type, final long id) throws OsmServerException, IOException {
		Log.d(DEBUG_TAG, "getStreamForElement");
		URL url = new URL(getReadOnlyUrl() + type + "/" + id + (mode != null ? "/" + mode : ""));
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		boolean isServerGzipEnabled;

		Log.d(DEBUG_TAG, "getStreamForElement " + url.toString());
		
		//--Start: header not yet send
		con.setReadTimeout(TIMEOUT);
		con.setConnectTimeout(TIMEOUT);
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setRequestProperty("User-Agent", Application.userAgent);

		//--Start: got response header
		isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

		// retry if we have no response-code
		if (con.getResponseCode() == -1) {
			Log.w(getClass().getName()+ ":getStreamForElement", "no valid http response-code, trying again");
			con = (HttpURLConnection) url.openConnection();
			//--Start: header not yet send
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("Accept-Encoding", "gzip");
			con.setRequestProperty("User-Agent", Application.userAgent);

			//--Start: got response header
			isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
		}

		if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
			if (con.getResponseCode() == 400) {
				Application.mainActivity.runOnUiThread(new Runnable() {
					  @Override
					public void run() {
						  Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_download_bbox_failed, Toast.LENGTH_LONG).show();
					  }
				});
			}
			else {
				Application.mainActivity.runOnUiThread(new DownloadErrorToast(con.getResponseCode(), con.getResponseMessage()));
			}
			throwUnexpectedRequestException(con);
		}

		if (isServerGzipEnabled) {
			return new GZIPInputStream(con.getInputStream());
		} else {
			return con.getInputStream();
		}
	}

	
	class DownloadErrorToast implements Runnable {
		final int code;
		final String message;
		
		DownloadErrorToast(int code, String message) {
			this.code = code;
			this.message = message;
		}
		
		@Override
		public void run() {
			try {
				Context mainCtx = Application.mainActivity.getApplicationContext();
				Toast.makeText(mainCtx,
					  mainCtx.getResources().getString(R.string.toast_download_failed, code, message), Toast.LENGTH_LONG).show();
			} catch (Exception ex) {
			  	// do nothing ... this is stop bugs in the Android format parsing crashing the app, report the error because it is likely caused by a translation error
				ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
				ACRA.getErrorReporter().handleException(ex);
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
	public boolean deleteElement(final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
		HttpURLConnection connection = null;
//		elem.addOrUpdateTag(createdByTag, createdByKey);
		Log.d(DEBUG_TAG,"Deleting " + elem.getName() + " #" + elem.getOsmId());
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
	 * @return
	 */
	public boolean isLoginSet() {
		return (username != null && (password != null && !username.equals("") && !password.equals(""))) || oauth;
	}

	/**
	 * @param connection
	 */
	private static void disconnect(final HttpURLConnection connection) {
		if (connection != null) {
			connection.disconnect();
		}
	}

	public long updateElement(final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
		long osmVersion = -1;
		HttpURLConnection connection = null;
		InputStream in = null;
		try {
			URL updateElementUrl = getUpdateUrl(elem);
			Log.d(DEBUG_TAG,"Updating " + elem.getName() + " #" + elem.getOsmId() + " " + updateElementUrl);
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
				throw new OsmServerException(-1,"Server returned illegal element version " + e.getMessage());
			}
		} finally {
			disconnect(connection);
			SavingHelper.close(in);
		}
		return osmVersion;
	}

	private void sendPayload(final HttpURLConnection connection,
			final XmlSerializable xmlSerializable, long changeSetId)
			throws OsmIOException {
		OutputStreamWriter out = null;
		try {
			XmlSerializer xmlSerializer = getXmlSerializer();
			out = new OutputStreamWriter(connection.getOutputStream(), Charset
					.defaultCharset());
			xmlSerializer.setOutput(out);
			xmlSerializable.toXml(xmlSerializer, changeSetId);
		} catch (IOException e) {
			throw new OsmIOException("Could not send data to server", e);
		} catch (IllegalArgumentException e) {
			throw new OsmIOException("Sending illegal format object failed", e);
		} finally {
			SavingHelper.close(out);
		}
	}

	/**
	 * @param elem
	 * @param xml
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 */
	private HttpURLConnection openConnectionForWriteAccess(final URL url, final String requestMethod)
			throws IOException, MalformedURLException, ProtocolException {
		return openConnectionForWriteAccess(url, requestMethod, "text/xml");
	}
	
	private HttpURLConnection openConnectionForWriteAccess(final URL url, final String requestMethod, final String contentType)
			throws IOException, MalformedURLException, ProtocolException {
		Log.d(DEBUG_TAG, "openConnectionForWriteAccess url " + url);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Content-Type", "" + contentType + "; charset=utf-8");
		connection.setRequestProperty("User-Agent", Application.userAgent);
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
			} catch (OAuthMessageSignerException e) { // user will get error when we actually try to write
				Log.e(DEBUG_TAG, "OAuth fail",e);
			} catch (OAuthExpectationFailedException e) {
				Log.e(DEBUG_TAG, "OAuth fail",e);
			} catch (OAuthCommunicationException e) {
				Log.e(DEBUG_TAG, "OAuth fail",e);
			}	
		} else {
			connection.setRequestProperty("Authorization", "Basic " + Base64.encode(username + ":" + password));
		}
		
		connection.setDoOutput(!"GET".equals(requestMethod));
		connection.setDoInput(true);
		return connection;
	}

	public long createElement(final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
		long osmId = -1;
		HttpURLConnection connection = null;
		InputStream in = null;
//		elem.addOrUpdateTag(createdByTag, createdByKey);

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
				throw new OsmServerException(-1,"Server returned illegal element id " + e.getMessage());
			}
		} finally {
			disconnect(connection);
			SavingHelper.close(in);
		}
		return osmId;
	}
	
	/**
	 * Test if changeset is at least potentially still open.
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
	 * @param comment Changeset comment.
	 * @param source 
	 * @param imagery TODO
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws IOException
	 */
	public void openChangeset(final String comment, final String source, final String imagery) throws MalformedURLException, ProtocolException, IOException {
		long newChangesetId = -1;
		HttpURLConnection connection = null;
		InputStream in = null;

		if (changesetId != -1) { // potentially still open, check if really the case
			Changeset cs = getChangeset(changesetId);
			if (cs != null && cs.open) {
				Log.d(DEBUG_TAG,"Changeset #" + changesetId + " still open, reusing");
				updateChangeset(changesetId, comment, source, imagery);
				return;
			} else {
				changesetId = -1;
			}
		}
		try {
			XmlSerializable xmlData = new XmlSerializable() {
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
					serializer.endTag("", "changeset");
					endXml(serializer);
				}
			};
			connection = openConnectionForWriteAccess(getCreateChangesetUrl(), "PUT");
			sendPayload(connection, xmlData, changesetId);
			if (connection.getResponseCode() == -1) {
				//sometimes we get an invalid response-code the first time.
				disconnect(connection);
				connection = openConnectionForWriteAccess(getCreateChangesetUrl(), "PUT");
				sendPayload(connection, xmlData, changesetId);
			}
			checkResponseCode(connection);
			in = connection.getInputStream();
			try {
				newChangesetId = Long.parseLong(readLine(in));
			} catch (NumberFormatException e) {
				throw new OsmServerException(-1,"Server returned illegal changeset id " + e.getMessage());
			}
		} finally {
			disconnect(connection);
			SavingHelper.close(in);
		}
		changesetId = newChangesetId;
	}

	/**
	 * Close the current open changeset, will zap the stored id even if the closing fails, 
	 * this will force using a new changeset on the next upload
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
	 * @author simon
	 *
	 */
	public class Changeset {
		public boolean open = false;
	}
	
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
					Log.d(DEBUG_TAG,"Changeset #" + id + " is " + (result.open ? "open":"closed"));
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			disconnect(connection);
		}
		return result;
	}
	

	private void updateChangeset(final long changesetId, final String comment, final String source, final String imagery) throws MalformedURLException, ProtocolException, IOException {
		
		HttpURLConnection connection = null;
		InputStream in = null;

		try {
			XmlSerializable xmlData = new XmlSerializable() {
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
					serializer.endTag("", "changeset");
					endXml(serializer);
				}
			};
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
	 * @param connection
	 * @throws IOException
	 * @throws OsmException
	 */
	private void checkResponseCode(final HttpURLConnection connection) throws IOException, OsmException {
		checkResponseCode(connection, null);
	}
	
	/**
	 * @param connection
	 * @throws IOException
	 * @throws OsmException
	 */
	private void checkResponseCode(final HttpURLConnection connection, final OsmElement e) throws IOException, OsmException {
		int responsecode = -1;
		if (connection == null ) {
			throw new OsmServerException(responsecode, "Unknown error");
		}
		responsecode = connection.getResponseCode();
		Log.d(DEBUG_TAG, "response code " + responsecode);
		if (responsecode == -1) throw new IOException("Invalid response from server");
		if (responsecode != HttpURLConnection.HTTP_OK) {
			if (responsecode == HttpURLConnection.HTTP_GONE && e.getState()==OsmElement.STATE_DELETED) {
				//FIXME we tried to delete an already deleted element: log, but ignore, maybe it would be better to ask user
				Log.d(DEBUG_TAG, e.getOsmId() + " already deleted on server");
				return;
			}
			String responseMessage = connection.getResponseMessage();
			if (responseMessage == null) {
				responseMessage = "";
			}
			InputStream in = connection.getErrorStream();
			if (e == null) {
				Log.d(DEBUG_TAG, "response message " + responseMessage);
				throw new OsmServerException(responsecode, responsecode + "=\"" + responseMessage + "\" ErrorMessage: " + readStream(in));
			} else {
				throw new OsmServerException(responsecode, e.getName(), e.getOsmId(), responsecode + "=\"" + responseMessage + "\" ErrorMessage: " + readStream(in));
			}
			//TODO: happens the first time on some uploads. responseMessage=ErrorMessage="", works the second time
		}
	}
	
	public void diffUpload(StorageDelegator delegator) throws MalformedURLException, ProtocolException, IOException {
		
		HttpURLConnection connection = null;
		InputStream in = null;

		try {
			connection = openConnectionForWriteAccess(getDiffUploadUrl(changesetId), "POST");
			delegator.writeOsmChange(connection.getOutputStream(), changesetId);
			processDiffUploadResult(delegator, connection, xmlParserFactory.newPullParser());
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (XmlPullParserException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			disconnect(connection);
			SavingHelper.close(in);
		}
	}
	
	/**
	 * These patterns are fairly, to very, unforgiving, hopefully API 0.7 will give the error codes back in a more structured way
	 */
	static final Pattern ERROR_MESSAGE_CLOSED_CHANGESET = Pattern.compile("(?i)The changeset ([0-9]+) was closed at");
	static final Pattern ERROR_MESSAGE_VERSION_CONFLICT = Pattern.compile("(?i)Version mismatch: Provided ([0-9]+), server had: ([0-9]+) of (Node|Way|Relation) ([0-9]+)");
	static final Pattern ERROR_MESSAGE_DELETED = Pattern.compile("(?i)The (node|way|relation) with the id ([0-9]+) has already been deleted");
	static final Pattern ERROR_MESSAGE_PRECONDITION_STILL_USED = Pattern.compile("(?i)Precondition failed: (Node|Way) ([0-9]+) is still used by (way|relation)[s]? ([0-9]+).*");
	static final Pattern ERROR_MESSAGE_PRECONDITION_RELATION_RELATION = Pattern.compile("(?i)Precondition failed: The relation ([0-9]+) is used in relation ([0-9]+).");
	
	/**
	 * Process the results of uploading a diff to the API, here because it needs to manipulate the stored data
	 * @param parser
	 * @param in
	 * @throws IOException 
	 */
	public void processDiffUploadResult(StorageDelegator delegator, HttpURLConnection connection, XmlPullParser parser) throws IOException {
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
									if (e.getState() == OsmElement.STATE_DELETED && newIdStr == null
											&& newVersionStr == null) {
										if (!apiStorage.removeElement(e)) {
											Log.e(DEBUG_TAG,
													"Deleted " + e + " was already removed from local storage!");
										}
										Log.w(DEBUG_TAG, e + " deleted in API");
										delegator.dirty();
									} else if (e.getState() == OsmElement.STATE_CREATED && oldId < 0 && newIdStr != null
											&& newVersionStr != null) {
										long newId = Long.parseLong(newIdStr);
										int newVersion = Integer.parseInt(newVersionStr);
										if (newId > 0) {
											if (!apiStorage.removeElement(e)) {
												Log.e(DEBUG_TAG, "New " + e + " was already removed from api storage!");
											}
											Log.w(DEBUG_TAG, "New " + e + " added to API");
											e.setOsmId(newId); // id change requires rehash, so that removing works, remove first then set id
											e.setOsmVersion(newVersion);
											e.setState(OsmElement.STATE_UNCHANGED);
											delegator.dirty();
											rehash = true;
										} else {
											Log.d(DEBUG_TAG, "Didn't get new ID: " + newId);
										}
									} else if (e.getState() == OsmElement.STATE_MODIFIED && oldId > 0
											&& newIdStr != null && newVersionStr != null) {
										long newId = Long.parseLong(newIdStr);
										int newVersion = Integer.parseInt(newVersionStr);
										if (newId == oldId && newVersion > 0) {
											if (!apiStorage.removeElement(e)) {
												Log.e(DEBUG_TAG,
														"Updated " + e + " was already removed from api storage!");
											}
											e.setOsmVersion(newVersion);
											Log.w(DEBUG_TAG, e + " updated in API");
											e.setState(OsmElement.STATE_UNCHANGED);
										} else {
											Log.d(DEBUG_TAG, "Didn't get new version: " + newVersion + " for " + newId);
										}
										delegator.dirty();
									} else {
										Log.e(DEBUG_TAG, "Unkown start tag in result: " + tagName);
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
						throw new OsmServerException(HttpURLConnection.HTTP_BAD_REQUEST,
								code + "=\"" + responseMessage + "\" ErrorMessage: " + message);
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
				// Way #{id} requires the nodes with id in (#{missing_ids}), which either do not exist, or are not visible.
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
			throw new OsmServerException(code, code + "=\"" + responseMessage + "\" ErrorMessage: " + message);
		}
	}

	private void generateException(Storage apiStorage, String type, String idStr, int code, String responseMessage, String message) throws OsmServerException {
		if (type != null && idStr != null) {
			long osmId = Long.parseLong(idStr);
			OsmElement e = apiStorage.getOsmElement(type.toLowerCase(Locale.US), osmId);
			if (e!=null) {
				throw new OsmServerException(code, e.getName(), e.getOsmId(), code + "=\"" + responseMessage + "\" ErrorMessage: " + message);
			}
		}
		Log.e(DEBUG_TAG, "Error message matched, but parsing failed: " + message);
	}

	static String readStream(final InputStream in) {
		String res = "";
		if (in != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in), 8000);
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					res += line;
				}
			} catch (IOException e) {
				Log.e(Server.class.getName() + ":readStream()", "Error in read-operation", e);
			}
		}
		return res;
	}

	private static String readLine(final InputStream in) {
		//TODO: Optimize? -> no Reader
		BufferedReader reader = new BufferedReader(new InputStreamReader(in), 9);
		String res = null;
		try {
			res = reader.readLine();
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "Problem reading", e);
		}

		return res;
	}

	private void startXml(XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
		xmlSerializer.startDocument("UTF-8", null);
		xmlSerializer.startTag("", "osm");
		xmlSerializer.attribute("", "version", version);
		xmlSerializer.attribute("", "generator", generator);
	}

	private void endXml(XmlSerializer xmlSerializer) throws IllegalArgumentException, IllegalStateException, IOException {
		xmlSerializer.endTag("", "osm");
		xmlSerializer.endDocument();
	}

	private void startChangeXml(XmlSerializer xmlSerializer, String action) throws IllegalArgumentException, IllegalStateException, IOException {
		xmlSerializer.startDocument("UTF-8", null);
		xmlSerializer.startTag("", "osmChange");
		xmlSerializer.attribute("", "version", osmChangeVersion);
		xmlSerializer.attribute("", "generator", generator);
		xmlSerializer.startTag("", action);
		xmlSerializer.attribute("", "version", osmChangeVersion);
		xmlSerializer.attribute("", "generator", generator);
	}

	private void endChangeXml(XmlSerializer xmlSerializer, String action) throws IllegalArgumentException, IllegalStateException, IOException {
		xmlSerializer.endTag("", action);
		xmlSerializer.endTag("", "osmChange");
		xmlSerializer.endDocument();
	}

	private XmlSerializer getXmlSerializer() {
		try {
			XmlSerializer serializer = xmlParserFactory.newSerializer();
			serializer.setPrefix("", "");
			return serializer;
		} catch (IllegalArgumentException e) {
			Log.e(DEBUG_TAG, "Problem getting serializer", e);
		} catch (IllegalStateException e) {
			Log.e(DEBUG_TAG, "Problem getting serializer", e);
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "Problem getting serializer", e);
		} catch (XmlPullParserException e) {
			Log.e(DEBUG_TAG, "Problem getting serializer", e);
		}
		return null;
	}

	private URL getCreationUrl(final OsmElement elem) throws MalformedURLException {
		return new URL(getReadWriteUrl()  + elem.getName() + "/create");
	}

	private URL getCreateChangesetUrl() throws MalformedURLException {
		return new URL(getReadWriteUrl()  + SERVER_CHANGESET_PATH + "create");
	}

	private URL getCloseChangesetUrl(long changesetId) throws MalformedURLException {
		return new URL(getReadWriteUrl()  + SERVER_CHANGESET_PATH + changesetId + "/close");
	}
	
	private URL getChangesetUrl(long changesetId) throws MalformedURLException {
		return new URL(getReadWriteUrl()  + SERVER_CHANGESET_PATH + changesetId);
	}

	private URL getUpdateUrl(final OsmElement elem) throws MalformedURLException {
		return new URL(getReadWriteUrl()  + elem.getName() + "/" + elem.getOsmId());
	}

	private URL getDeleteUrl(final OsmElement elem) throws MalformedURLException {
		//return getUpdateUrl(elem);
		return new URL(getReadWriteUrl()  + SERVER_CHANGESET_PATH + changesetId + "/upload");
	}
	
	private URL getDiffUploadUrl(long changeSetId) throws MalformedURLException {
		return new URL(getReadWriteUrl() + SERVER_CHANGESET_PATH + changeSetId + "/upload");
	}
	
	private URL getUserDetailsUrl() throws MalformedURLException {
		return new URL(getReadWriteUrl()  + "user/details");
	}
	
	private URL getAddCommentUrl(@NonNull String noteId, @NonNull String comment)
			throws MalformedURLException {
		return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/comment?text=" + comment);
	}

	private URL getNoteUrl(@NonNull String noteId) throws MalformedURLException {
		return new URL(getNotesReadOnlyUrl() + SERVER_NOTES_PATH + noteId);
	}
	
	/**
	 * Return for now general read write API url as a string 
	 * @return
	 */
	private String getNotesUrl() {
		return serverURL;
	}
	
	/**
	 * Return either the general read write API url as a string or a specific to notes one
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
		return new URL(getNotesReadOnlyUrl()  + "notes?" +
				"limit=" + limit + "&" +
				"bbox=" +
				area.getLeft() / 1E7d +
				"," + area.getBottom() / 1E7d +
				"," + area.getRight() / 1E7d +
				"," + area.getTop() / 1E7d);
	}

	private URL getAddNoteUrl(double latitude, double longitude, @NonNull String comment)
			throws MalformedURLException {
		return new URL(getNotesUrl() + "notes?lat=" + latitude + "&lon=" + longitude + "&text=" + comment);
	}

	private URL getCloseNoteUrl(@NonNull String noteId) throws MalformedURLException {
		return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/close");
	}

	private URL getReopenNoteUrl(@NonNull String noteId) throws MalformedURLException {
		return new URL(getNotesUrl() + SERVER_NOTES_PATH + noteId + "/reopen");
	}

	private URL getUploadTrackUrl() throws MalformedURLException {
		return new URL(getReadWriteUrl()  + "gpx/create");
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
			return new URL(noVersionURL  + "capabilities");
		}
		return null;
	}

	/**
	 * @return the read/write URL
	 */
	public String getReadWriteUrl() {
		return serverURL;
	}
	
	/**
	 * Return the url as a string for a read only API if it exists otherwise the result is the same as for read/write
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
	public String getBaseUrl(String url) {
		return url.replaceAll("/api/[0-9]+(?:\\.[0-9]+)+/?$", "/");
	}
	
	/**
	 * @return the URL the OSM website, FIXME for now hardwired and a bit broken
	 */
	public String getWebsiteBaseUrl() {
		return getBaseUrl(getReadWriteUrl()).replace("api.", "");
	}
	
	/* New Notes API 
	 * code mostly from old OSB implementation
	 * the relevant API documentation is still in flux so this implementation may have issues
	 */
	
	/**
	 * Perform an HTTP request to download up to limit bugs inside the specified area.
	 * Blocks until the request is complete.
	 * @param area Latitude/longitude *1E7 of area to download.
	 * @return All the bugs in the given area.
	 */
	public Collection<Note> getNotesForBox(BoundingBox area, long limit) {
		Collection<Note> result = new ArrayList<Note>();
		// http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
		try {
			Log.d(DEBUG_TAG, "getNotesForBox");
			URL url = getNotesForBox(limit, area);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			boolean isServerGzipEnabled;

			//--Start: header not yet send
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("Accept-Encoding", "gzip");
			con.setRequestProperty("User-Agent", Application.userAgent);

			//--Start: got response header
			isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
			
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return new ArrayList<Note>(); //TODO Return empty list ... this is better than throwing an uncatched exception, but we should provide some user feedback
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
			return new ArrayList<Note>(); // empty list
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
			return new ArrayList<Note>(); // empty list
		} catch (OutOfMemoryError e) {
			Log.e(DEBUG_TAG, "Server.getNotesForBox:Exception", e);
			// TODO ask the user to exit
			return new ArrayList<Note>(); // empty list
		}
		return result;
	}
	
	/**
	 * Retrieve a single note
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

			//--Start: header not yet send
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("Accept-Encoding", "gzip");
			con.setRequestProperty("User-Agent", Application.userAgent);

			//--Start: got response header
			isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
			
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null; //TODO Return empty list ... this is better than throwing an uncatched exception, but we should provide some user feedback
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
	
	//TODO rewrite to XML encoding (if supported)
	/**
	 * Perform an HTTP request to add the specified comment to the specified bug.
	 * Blocks until the request is complete.
	 * @param bug The bug to add the comment to.
	 * @param comment The comment to add to the bug.
	 * @return true if the comment was successfully added.
	 */
	public boolean addComment(Note bug, NoteComment comment) {
		if (!bug.isNew()) {
			Log.d(DEBUG_TAG, "adding note comment" + bug.getId());
			// http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?id=<Bug ID>&text=<Comment with author and date>
			HttpURLConnection connection = null;
			try {
				try {
					// setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body which will fail
					String encodedComment = URLEncoder.encode(comment.getText(), "UTF-8");
					URL addCommentUrl = getAddCommentUrl(Long.toString(bug.getId()), encodedComment);
					connection = openConnectionForWriteAccess(addCommentUrl, "POST", "text/url");
					OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), Charset
							.defaultCharset());
		
					// out.write("text="+URLEncoder.encode(comment.getText(), "UTF-8")+ "\r\n");
					out.flush();
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throwUnexpectedRequestException(connection);
					}
					parseBug(bug, connection.getInputStream());
					return true;
				} catch (XmlPullParserException e) {
					Log.e(DEBUG_TAG, "addComment:Exception", e);
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "addComment:Exception", e);
				}
			} finally {
				disconnect(connection);
			}
		}
		return false;
	}
	
	//TODO rewrite to XML encoding
	/**
	 * Perform an HTTP request to add the specified bug to the OpenStreetBugs database.
	 * Blocks until the request is complete.
	 * @param bug The bug to add.
	 * @param comment The first comment for the bug.
	 * @return true if the bug was successfully added.
	 */
	public boolean addNote(Note bug, NoteComment comment) {
		if (bug.isNew()) {
			Log.d(DEBUG_TAG, "adding note");
			// http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?lat=<Latitude>&lon=<Longitude>&text=<Bug description with author and date>&format=<Output format>
			HttpURLConnection connection = null;
			try {
				try {
					// setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body which will fail
					String encodedComment = URLEncoder.encode(comment.getText(), "UTF-8");
					URL addNoteUrl = getAddNoteUrl((bug.getLat() / 1E7d), (bug.getLon() / 1E7d), encodedComment);
					connection = openConnectionForWriteAccess(addNoteUrl, "POST", "text/xml");
					OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), Charset
							.defaultCharset());
					// out.write("text="+URLEncoder.encode(comment.getText(), "UTF-8") + "\r\n");
					out.flush();
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throwUnexpectedRequestException(connection);
					}
					parseBug(bug, connection.getInputStream());
					return true;
				} catch (XmlPullParserException e) {
					Log.e(DEBUG_TAG, "addNote:Exception", e);
				} catch (IOException e) {
					Log.e(DEBUG_TAG, "addNote:Exception", e);
				}		
			} finally {
				disconnect(connection);
			}
		}
		return false;
	}
	
	//TODO rewrite to XML encoding
	/**
	 * Perform an HTTP request to close the specified bug.
	 * Blocks until the request is complete.
	 * @param bug The bug to close.
	 * @return true if the bug was successfully closed.
	 */
	public boolean closeNote(Note bug) {
		
		if (!bug.isNew()) {
			Log.d(DEBUG_TAG, "closing note " + bug.getId());
			HttpURLConnection connection = null;
			try {
				try {
					// setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body which will fail
					URL closeNoteUrl = getCloseNoteUrl(Long.toString(bug.getId()));
					connection = openConnectionForWriteAccess(closeNoteUrl, "POST", "text/xml");
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throwUnexpectedRequestException(connection);
					}
					parseBug(bug, connection.getInputStream());
					return true;
				} catch (XmlPullParserException e) {
					Log.e(DEBUG_TAG, "closeNote:Exception", e);
				}
				catch (IOException e) {
					Log.e(DEBUG_TAG, "closeNote:Exception", e);
				} 
			} finally {
				disconnect(connection);
			}
		}
		return false;
	}
	
	/**
	 * Perform an HTTP request to reopen the specified bug.
	 * Blocks until the request is complete.
	 * @param bug The bug to close.
	 * @return true if the bug was successfully closed.
	 */
	public boolean reopenNote(Note bug) {
		
		if (!bug.isNew()) {
			Log.d(DEBUG_TAG, "reopen note " + bug.getId());
			HttpURLConnection connection = null;
			try {
				try {
					URL reopenNoteUrl = getReopenNoteUrl(Long.toString(bug.getId()));
					connection = openConnectionForWriteAccess(reopenNoteUrl, "POST", "text/xml");
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throwUnexpectedRequestException(connection);
					}
					parseBug(bug, connection.getInputStream());
					return true;
				} catch (XmlPullParserException e) {
					Log.e(DEBUG_TAG, "reopenNote:Exception", e);
				}
				catch (IOException e) {
					Log.e(DEBUG_TAG, "reopenNote:Exception", e);
				} 
			} finally {
				disconnect(connection);
			}
		}
		return false;
	}
	
	private void parseBug(@NonNull Note bug, @NonNull InputStream inputStream)
			throws IOException, XmlPullParserException {
		XmlPullParser parser = xmlParserFactory.newPullParser();
		parser.setInput(new BufferedInputStream(inputStream, StreamUtils.IO_BUFFER_SIZE), null);
		bug.parseBug(parser); // replace contents with result from server
		Application.getTaskStorage().setDirty();
	}

	/**
	 * GPS track API
	 */
	public enum Visibility {
		PRIVATE,
		PUBLIC,
		TRACKABLE,
		IDENTIFIABLE
	}
	
	/**
	 * @throws IOException 
	 * @throws ProtocolException 
	 * @throws MalformedURLException 
	 * @throws XmlPullParserException 
	 * @throws IllegalStateException 
	 * @throws IllegalArgumentException 
	 
	 */
	public void uploadTrack(Track track, String description, String tags, Visibility visibility) throws MalformedURLException, ProtocolException, IOException, IllegalArgumentException, IllegalStateException, XmlPullParserException {
		HttpURLConnection connection = null;
		try {
			// 
			String boundary="*VESPUCCI*";
			String separator="--"+boundary+"\r\n";
			connection = 
					openConnectionForWriteAccess(getUploadTrackUrl(), "POST", "multipart/form-data;boundary="+boundary);
			OutputStream os = connection.getOutputStream();
			OutputStreamWriter out = new OutputStreamWriter(os, Charset .defaultCharset());
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
			out.write("--"+boundary+"--\r\n");
			out.flush();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throwUnexpectedRequestException(connection);
			}
		} finally {
			disconnect(connection);
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean needOAuthHandshake() {
		return oauth && ((accesstoken == null) || (accesstokensecret == null)) ;
	}
	
	/**
	 * Override the oauth flag from the API configuration, only needed if inconsistent config
	 * @param t
	 */
	public void setOAuth(boolean t) {
		oauth = t;
	}
	
	public boolean getOAuth() {
		return oauth;
	}

	private void throwUnexpectedRequestException(@NonNull HttpURLConnection connection)
			throws IOException {
		String detailMessage = "The API server does not except the request: " + connection +
				", response code: " + connection.getResponseCode() +
				" \"" + connection.getResponseMessage() + "\"";
		throw new OsmServerException(connection.getResponseCode(), detailMessage);
	}
}
