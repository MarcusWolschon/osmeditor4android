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
import java.util.zip.GZIPInputStream;

import de.blau.android.util.DateFormatter;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.acra.ACRA;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osb.Note;
import de.blau.android.osb.NoteComment;
import de.blau.android.services.util.StreamUtils;
import de.blau.android.util.Base64;
import de.blau.android.util.OAuthHelper;
import de.blau.android.util.SavingHelper;

/**
 * @author mb
 */
public class Server {


	/**
	 * Timeout for connections in milliseconds.
	 */
	private static final int TIMEOUT = 45 * 1000;
	
	/**
	 * Location of OSM API
	 */
	private final String serverURL;

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
	public Capabilities capabilities = Capabilities.getDefault();

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
	 * Constructor. Sets {@link #rootOpen} and {@link #createdByTag}.
	 * @param apiurl The OSM API URL to use (e.g. "http://api.openstreetmap.org/api/0.6/").
	 * @param username
	 * @param password
	 * @param oauth 
	 * @param generator the name of the editor.
	 */
	public Server(final String apiurl, final String username, final String password, boolean oauth, String accesstoken, String accesstokensecret, final String generator) {
		Log.d("Server", "constructor");
		if (apiurl != null && !apiurl.equals("")) {
			this.serverURL = apiurl;
		} else {
			this.serverURL = "http://api.openstreetmap.org/api/"+version+"/"; // probably not needed anymore
		}
		this.password = password;
		this.username = username;
		this.oauth = oauth;
		this.generator = generator;
		this.accesstoken = accesstoken;
		this.accesstokensecret = accesstokensecret;
		
		userDetails = null;
		Log.d("Server", "using " + this.username + " with " + this.serverURL);
		Log.d("Server", "oAuth: " + this.oauth + " token " + this.accesstoken + " secret " + this.accesstokensecret);

		XmlPullParserFactory factory = null;
		try {
			factory = XmlPullParserFactory.newInstance();
		} catch (XmlPullParserException e) {
			Log.e("Vespucci", "Problem creating parser factory", e);
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
							Log.d("Server","getUserDetails display name " + result.display_name);
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
								Log.d("Server","getUserDetails received " + result.received);
								result.unread = Integer.parseInt(parser.getAttributeValue(null, "unread"));
								Log.d("Server","getUserDetails unread " + result.unread);
							}
							if (eventType == XmlPullParser.START_TAG && "sent".equals(tagName)) {
								result.sent = Integer.parseInt(parser.getAttributeValue(null, "count"));
								Log.d("Server","getUserDetails sent " + result.sent);
							}
						}
					}
				} finally {
					disconnect(connection);
				}
			} catch (XmlPullParserException e) {
				Log.e("Vespucci", "Problem accessing user details", e);
			} catch (MalformedURLException e) {
				Log.e("Vespucci", "Problem accessing user details", e);
			} catch (ProtocolException e) {
				Log.e("Vespucci", "Problem accessing user details", e);
			} catch (IOException e) {
				Log.e("Vespucci", "Problem accessing user details", e);
			} catch (NumberFormatException e) {
				Log.e("Vespucci", "Problem accessing user details", e);
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
	

	public Capabilities getCachedCapabilities() {
		return capabilities;
	}
	
	
	/**
	 * Get the capabilities for the current API
	 * Side effect set capabilities field and update limits that are used else where
	 * @return The capabilities for this server, or null if it couldn't be determined.
	 */
	public Capabilities getCapabilities() {
		Capabilities result = null;
		HttpURLConnection con = null;
		// 
		try {
			URL capabilitiesURL = getCapabilitiesUrl();
			if (capabilitiesURL == null) {
				return capabilities;
			}
			Log.d("Server","getCapabilities using " + capabilitiesURL.toString());
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
						Log.d("Server","getCapabilities min/max API version " + result.minVersion + "/" + result.maxVersion);
					}
					if (eventType == XmlPullParser.START_TAG && "area".equals(tagName)) {
						String maxArea = parser.getAttributeValue(null, "maximum");
						if (maxArea != null) {
							result.areaMax = Float.parseFloat(maxArea);
						}
						Log.d("Server","getCapabilities maximum area " + maxArea);
					}
					if (eventType == XmlPullParser.START_TAG && "tracepoints".equals(tagName)) {
						String perPage = parser.getAttributeValue(null, "per_page");
						if (perPage != null) {
							result.maxTracepointsPerPage = Integer.parseInt(perPage);
						}
						Log.d("Server","getCapabilities maximum #tracepoints per page " + perPage);
					}
					if (eventType == XmlPullParser.START_TAG && "waynodes".equals(tagName)) {
						String maximumWayNodes = parser.getAttributeValue(null, "maximum");
						if (maximumWayNodes != null) {
							result.maxWayNodes = Integer.parseInt(maximumWayNodes);
						}
						Log.d("Server","getCapabilities maximum #nodes in a way " + maximumWayNodes);
					}
					if (eventType == XmlPullParser.START_TAG && "changesets".equals(tagName)) {
						String maximumElements = parser.getAttributeValue(null, "maximum_elements");
						if (maximumElements != null) {
							result.maxElementsInChangeset = Integer.parseInt(maximumElements);
						}
						Log.d("Server","getCapabilities maximum elements in changesets " + maximumElements);
					}
					if (eventType == XmlPullParser.START_TAG && "timeout".equals(tagName)) {
						String seconds = parser.getAttributeValue(null, "seconds");
						if (seconds != null) {
							result.timeout = Integer.parseInt(seconds);
						}
						Log.d("Server","getCapabilities timeout seconds " + seconds);
					}
					if (eventType == XmlPullParser.START_TAG && "status".equals(tagName)) {
						result.dbStatus = result.stringToStatus(parser.getAttributeValue(null, "database"));
						result.apiStatus = result.stringToStatus(parser.getAttributeValue(null, "api"));
						result.gpxStatus = result.stringToStatus(parser.getAttributeValue(null, "gpx"));
						Log.d("Server","getCapabilities service status FB " + result.dbStatus + " API " + result.apiStatus + " GPX " + result.gpxStatus);
					}	
					if (eventType == XmlPullParser.START_TAG && "blacklist".equals(tagName)) {
						if (result.imageryBlacklist == null) {
							result.imageryBlacklist = new ArrayList<String>();
						}
						String regex = parser.getAttributeValue(null, "regex");
						if (regex != null) {
							result.imageryBlacklist.add(regex);
						}
						Log.d("Server","getCapabilities blacklist regex " + regex);
					}

				} catch (NumberFormatException e) {
					Log.e("Vespucci", "Problem accessing capabilities", e);
				}
			}
			capabilities = result;
			capabilities.updateLimits();
			return result;	
		} catch (XmlPullParserException e) {
			Log.e("Vespucci", "Problem accessing capabilities", e);
		} catch (ProtocolException e) {
			Log.e("Vespucci", "Problem accessing capabilities", e);
		} catch (IOException e) {
			Log.e("Vespucci", "Problem accessing capabilities", e);
		} finally {
			disconnect(con);
		}
		return null;
	}

	public boolean apiAvailable() {
		return capabilities.apiStatus.equals(Capabilities.Status.ONLINE);
	}
	
	public boolean readableDB() {
		return capabilities.dbStatus.equals(Capabilities.Status.ONLINE) || capabilities.dbStatus.equals(Capabilities.Status.READONLY);
	}
	
	public boolean writableDB() {
		return capabilities.dbStatus.equals(Capabilities.Status.ONLINE);
	}
	
	/**
	 * @param area
	 * @return
	 * @throws IOException
	 * @throws OsmServerException
	 */
	public InputStream getStreamForBox(final BoundingBox box) throws OsmServerException, IOException {
		Log.d("Server", "getStreamForBox");
		URL url = new URL(serverURL  + "map?bbox=" + box.toApiString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		boolean isServerGzipEnabled = false;

		Log.d("Server", "getStreamForBox " + url.toString());
		
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
			throw new OsmServerException(con.getResponseCode(), "The API server does not except the request: " + con
					+ ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
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
		Log.d("Server", "getStreamForElement");
		URL url = new URL(serverURL + type + "/" + id + (mode != null ? "/" + mode : ""));
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		boolean isServerGzipEnabled = false;

		Log.d("Server", "getStreamForElement " + url.toString());
		
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
			throw new OsmServerException(con.getResponseCode(), "The API server does not except the request: " + con
					+ ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
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
		Log.d("Server","Deleting " + elem.getName() + " #" + elem.getOsmId());
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
		Log.d("Server","Updating " + elem.getName() + " #" + elem.getOsmId() + " " + getUpdateUrl(elem));
		try {
			connection = openConnectionForWriteAccess(getUpdateUrl(elem), "PUT");
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
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Content-Type", "" + contentType + "; charset=utf-8");
		connection.setRequestProperty("User-Agent", Application.userAgent);
		connection.setConnectTimeout(TIMEOUT);
		connection.setReadTimeout(TIMEOUT);
		connection.setRequestMethod(requestMethod);

		if (oauth) {
			OAuthHelper oa = new OAuthHelper();
			OAuthConsumer consumer = oa.getConsumer(getBaseURL());
			consumer.setTokenWithSecret(accesstoken, accesstokensecret);	
			// sign the request
			try {
				consumer.sign(connection);
				// HttpParameters h = consumer.getRequestParameters();
				
			} catch (OAuthMessageSignerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
				Log.d("Server","Changeset #" + changesetId + " still open, reusing");
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
	
	public Changeset getChangeset(long id) {
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
					Log.d("Server","Changeset #" + id + " is " + (result.open ? "open":"closed"));
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
	

	public void updateChangeset(final long changesetId, final String comment, final String source, final String imagery) throws MalformedURLException, ProtocolException, IOException {
		
		HttpURLConnection connection = null;
		InputStream in = null;

		try {
			XmlSerializable xmlData = new XmlSerializable() {
				@Override
				public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
					startXml(serializer);
					serializer.startTag("", "changeset");
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
		Log.d("Server", "response code " + responsecode);
		if (responsecode == -1) throw new IOException("Invalid response from server");
		if (responsecode != HttpURLConnection.HTTP_OK) {
			if (responsecode == HttpURLConnection.HTTP_GONE && e.getState()==OsmElement.STATE_DELETED) {
				//FIXME we tried to delete an already deleted element: log, but ignore, maybe it would be better to ask user
				Log.d("Server", e.getOsmId() + " already deleted on server");
				return;
			}
			String responseMessage = connection.getResponseMessage();
			if (responseMessage == null) {
				responseMessage = "";
			}
			InputStream in = connection.getErrorStream();
			if (e == null) {
				throw new OsmServerException(responsecode, responsecode + "=\"" + responseMessage + "\" ErrorMessage: " + readStream(in));
			} else {
				throw new OsmServerException(responsecode, e.getName(), e.getOsmId(), responsecode + "=\"" + responseMessage + "\" ErrorMessage: " + readStream(in));
			}
			//TODO: happens the first time on some uploads. responseMessage=ErrorMessage="", works the second time
		}
	}

	private static String readStream(final InputStream in) {
		String res = "";
		if (in != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in), 8000);
			String line = null;
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
			Log.e("Vespucci", "Problem reading", e);
		}

		return res;
	}

	private URL getCreationUrl(final OsmElement elem) throws MalformedURLException {
		return new URL(serverURL  + elem.getName() + "/create");
	}

	private URL getCreateChangesetUrl() throws MalformedURLException {
		return new URL(serverURL  + "changeset/create");
	}

	private URL getCloseChangesetUrl(long changesetId) throws MalformedURLException {
		return new URL(serverURL  + "changeset/" + changesetId + "/close");
	}
	
	private URL getChangesetUrl(long changesetId) throws MalformedURLException {
		return new URL(serverURL  + "changeset/" + changesetId);
	}

	private URL getUpdateUrl(final OsmElement elem) throws MalformedURLException {
		return new URL(serverURL  + elem.getName() + "/" + elem.getOsmId());
	}

	private URL getDeleteUrl(final OsmElement elem) throws MalformedURLException {
		//return getUpdateUrl(elem);
		return new URL(serverURL  + "changeset/" + changesetId + "/upload");
	}
	
	private URL getUserDetailsUrl() throws MalformedURLException {
		return new URL(serverURL  + "user/details");
	}
	
	private URL getCapabilitiesUrl() throws MalformedURLException {
		// need to strip version from serverURL
		int apiPos = serverURL.indexOf("api/");
		if (apiPos > 0) {
			String noVersionURL = serverURL.substring(0, apiPos) + "api/";
			return new URL(noVersionURL  + "capabilities");
		}
		return null;
	}

	public XmlSerializer getXmlSerializer() {
		try {
			XmlSerializer serializer = xmlParserFactory.newSerializer();
			serializer.setPrefix("", "");
			return serializer;
		} catch (IllegalArgumentException e) {
			Log.e("Vespucci", "Problem getting serializer", e);
		} catch (IllegalStateException e) {
			Log.e("Vespucci", "Problem getting serializer", e);
		} catch (IOException e) {
			Log.e("Vespucci", "Problem getting serializer", e);
		} catch (XmlPullParserException e) {
			Log.e("Vespucci", "Problem getting serializer", e);
		}
		return null;
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

	/**
	 * @return the base URL, i.e. the API url with the "/api/version/"-part stripped
	 */
	public String getBaseURL() {
		return serverURL.replaceAll("/api/[0-9]+(?:\\.[0-9]+)+/?$", "/");
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
			Log.d("Server", "getNotesForBox");
			URL url = new URL(serverURL  + "notes?" +
					"limit=" + limit + "&" +
					"bbox=" +
					area.getLeft() / 1E7d +
					"," + area.getBottom() / 1E7d +
					"," + area.getRight() / 1E7d +
					"," + area.getTop() / 1E7d);
			
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			boolean isServerGzipEnabled = false;

			//--Start: header not yet send
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("Accept-Encoding", "gzip");
			con.setRequestProperty("User-Agent", Application.userAgent);

			//--Start: got response header
			isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
			
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return new ArrayList<Note>(); //TODO Return empty list ... this is better than throwing an uncatched exception, but we should provide some user feedback
//				throw new OsmServerException(con.getResponseCode(), "The API server does not except the request: " + con
//						+ ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
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
						Log.e("Vespucci", "Problem parsing bug", e);
					} catch (XmlPullParserException e) {
						// if the bug doesn't parse correctly, there's nothing
						// we can do about it - move on
						Log.e("Vespucci", "Problem parsing bug", e);
					} catch (NumberFormatException e) {
						// if the bug doesn't parse correctly, there's nothing
						// we can do about it - move on
						Log.e("Vespucci", "Problem parsing bug", e);
					}
				}
			}
		} catch (XmlPullParserException e) {
			Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
			return new ArrayList<Note>(); // empty list
		} catch (IOException e) {
			Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
			return new ArrayList<Note>(); // empty list
		} catch (OutOfMemoryError e) {
			Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
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
			Log.d("Server", "getNote");
			URL url = new URL(serverURL  + "notes/" + id);
							
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			boolean isServerGzipEnabled = false;

			//--Start: header not yet send
			con.setReadTimeout(TIMEOUT);
			con.setConnectTimeout(TIMEOUT);
			con.setRequestProperty("Accept-Encoding", "gzip");
			con.setRequestProperty("User-Agent", Application.userAgent);

			//--Start: got response header
			isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));
			
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null; //TODO Return empty list ... this is better than throwing an uncatched exception, but we should provide some user feedback
//				throw new OsmServerException(con.getResponseCode(), "The API server does not except the request: " + con
//						+ ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
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
						Log.e("Vespucci", "Problem parsing bug", e);
					} catch (XmlPullParserException e) {
						// if the bug doesn't parse correctly, there's nothing
						// we can do about it - move on
						Log.e("Vespucci", "Problem parsing bug", e);
					} catch (NumberFormatException e) {
						// if the bug doesn't parse correctly, there's nothing
						// we can do about it - move on
						Log.e("Vespucci", "Problem parsing bug", e);
					}
				}
			}
		} catch (XmlPullParserException e) {
			Log.e("Server", "Server.getNotesForBox:Exception", e);
			return null; // empty list
		} catch (IOException e) {
			Log.e("Server", "Server.getNotesForBox:Exception", e);
			return null; // empty list
		} catch (OutOfMemoryError e) {
			Log.e("Server", "Server.getNotesForBox:Exception", e);
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
			Log.d("Server", "adding note comment" + bug.getId());
			// http://openstreetbugs.schokokeks.org/api/0.1/editPOIexec?id=<Bug ID>&text=<Comment with author and date>
			HttpURLConnection connection = null;
			try {
				try {
					// setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body which will fail
					connection = 
							openConnectionForWriteAccess(new URL(serverURL  + "notes/"+Long.toString(bug.getId())+"/comment?text="  +URLEncoder.encode(comment.getText(), "UTF-8")), "POST", "text/url");
					OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), Charset
							.defaultCharset());
		
					// out.write("text="+URLEncoder.encode(comment.getText(), "UTF-8")+ "\r\n");
					out.flush();
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throw new OsmServerException(connection.getResponseCode(), "The API server does not except the request: " + connection
								+ ", response code: " + connection.getResponseCode() + " \"" + connection.getResponseMessage() + "\"");
					}
					
					InputStream is = connection.getInputStream();	
					XmlPullParser parser = xmlParserFactory.newPullParser();
					parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
					bug.parseBug(parser); // replace contents with result from server 
					Application.getBugStorage().setDirty();
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Server", "addComment:Exception", e);
				} catch (IOException e) {
					Log.e("Server", "addComment:Exception", e);
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
			Log.d("Server", "adding note");
			// http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?lat=<Latitude>&lon=<Longitude>&text=<Bug description with author and date>&format=<Output format>
			HttpURLConnection connection = null;
			try {
				try {
					// setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body which will fail
					connection = 
							openConnectionForWriteAccess(new URL(serverURL  + "notes?lat=" + (bug.getLat() / 1E7d)+"&lon=" + (bug.getLon() / 1E7d) + "&text=" +URLEncoder.encode(comment.getText(), "UTF-8")), "POST", "text/xml");
					OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), Charset
							.defaultCharset());
					// out.write("text="+URLEncoder.encode(comment.getText(), "UTF-8") + "\r\n");
					out.flush();
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throw new OsmServerException(connection.getResponseCode(), "The API server does not except the request: " + connection
								+ ", response code: " + connection.getResponseCode() + " \"" + connection.getResponseMessage() + "\"");
					}
					
					InputStream is = connection.getInputStream();
					
					XmlPullParser parser = xmlParserFactory.newPullParser();
					parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
					bug.parseBug(parser); // replace contents with result from server 
					Application.getBugStorage().setDirty();
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Server", "addNote:Exception", e);
				} catch (IOException e) {
					Log.e("Server", "addNote:Exception", e);
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
			Log.d("Server", "closing note " + bug.getId());
			HttpURLConnection connection = null;
			try {
				try {
					// setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body which will fail
					connection = 
							openConnectionForWriteAccess(new URL(serverURL  + "notes/"+Long.toString(bug.getId())+"/close"  ), "POST", "text/xml");
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throw new OsmServerException(connection.getResponseCode(), "The API server does not except the request: " + connection
								+ ", response code: " + connection.getResponseCode() + " \"" + connection.getResponseMessage() + "\"");
					}
				
					InputStream is = connection.getInputStream();
					
					XmlPullParser parser = xmlParserFactory.newPullParser();
					parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
					bug.parseBug(parser); // replace contents with result from server 
					Application.getBugStorage().setDirty();
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Server", "closeNote:Exception", e);
				}
				catch (IOException e) {
					Log.e("Server", "closeNote:Exception", e);
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
			Log.d("Server", "reopen note " + bug.getId());
			HttpURLConnection connection = null;
			try {
				try {
					connection = 
							openConnectionForWriteAccess(new URL(serverURL  + "notes/"+Long.toString(bug.getId())+"/reopen"  ), "POST", "text/xml");
					if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						throw new OsmServerException(connection.getResponseCode(), "The API server does not except the request: " + connection
								+ ", response code: " + connection.getResponseCode() + " \"" + connection.getResponseMessage() + "\"");
					}
					InputStream is = connection.getInputStream();
					
					XmlPullParser parser = xmlParserFactory.newPullParser();
					parser.setInput(new BufferedInputStream(is, StreamUtils.IO_BUFFER_SIZE), null);
					bug.parseBug(parser); // replace contents with result from server 
					Application.getBugStorage().setDirty();
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Server", "reopenNote:Exception", e);
				}
				catch (IOException e) {
					Log.e("Server", "reopenNote:Exception", e);
				} 
			} finally {
				disconnect(connection);
			}
		}
		return false;
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
					openConnectionForWriteAccess(new URL(serverURL  + "gpx/create"), "POST", "multipart/form-data;boundary="+boundary);
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
				throw new OsmServerException(connection.getResponseCode(), "The API server does not except the request: " + connection
						+ ", response code: " + connection.getResponseCode() + " \"" + connection.getResponseMessage() + "\"");
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
}
