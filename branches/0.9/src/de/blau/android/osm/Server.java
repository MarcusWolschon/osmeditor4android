package de.blau.android.osm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.HttpURLConnectionRequestAdapter;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import oauth.signpost.http.HttpRequest;
import oauth.signpost.http.HttpResponse;
import oauth.signpost.signature.SignatureBaseString;

import org.acra.ACRA;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.DialogFactory;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osb.Bug;
import de.blau.android.osb.BugComment;
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
	private String accesstoken;
	
	/**
	 * oauth access token secret
	 */
	private String accesstokensecret;
	
	/**
	 * display name of the user.
	 */
	private UserDetails userDetails;

	/**
	 * <a href="http://wiki.openstreetmap.org/wiki/API">API</a>-Version.
	 */
	private static final String version = "0.6";

	private final String osmChangeVersion = "0.3";

//	/**
//	 * Tag with "created_by"-key to identify edits made by this editor.
//	 */
//	private final String createdByTag;
//	private final String createdByKey;

	private long changesetId = -1;

	private String generator;

	private final XmlPullParserFactory xmlParserFactory;

	
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

//		createdByTag = "created_by";
//		createdByKey = generator;

		XmlPullParserFactory factory = null;
		try {
			factory = XmlPullParserFactory.newInstance();
		} catch (XmlPullParserException e) {
			Log.e("Vespucci", "Problem creating parser factory", e);
		}
		xmlParserFactory = factory;
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
			// Haven't retrieved the detailsfrom OSM - try to
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
		int code;
		String message;
		
		DownloadErrorToast(int code, String message) {
			this.code = code;
			this.message = message;
		}
		
		public void run() {
			try {
				Context mainCtx = Application.mainActivity.getApplicationContext();
				Toast.makeText(mainCtx,
					  mainCtx.getResources().getString(R.string.toast_download_failed, code, message), Toast.LENGTH_LONG).show();
			} catch (Exception ex) {
			  	// do nothing ... this is stop bugs in the Android format parsing crashing the app, report the error because it is likely casued by a translation error 
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
			checkResponseCode(connection);
		} finally {
			disconnect(connection);
		}
		return true;
	}

	/**
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
//		elem.addOrUpdateTag(createdByTag, createdByKey); 
		Log.d("Server","Updating " + elem.getName() + " #" + elem.getOsmId() + " " + getUpdateUrl(elem));
		try {
			connection = openConnectionForWriteAccess(getUpdateUrl(elem), "PUT");
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
			osmVersion = Long.parseLong(readLine(in));
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
			osmId = Long.parseLong(readLine(in));
		} finally {
			disconnect(connection);
			SavingHelper.close(in);
		}
		return osmId;
	}

	/**
	 * Open a new changeset.
	 * @param comment Changeset comment.
	 * @param source 
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws IOException
	 */
	public void openChangeset(final String comment, final String source) throws MalformedURLException, ProtocolException, IOException {
		long newChangesetId = -1;
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
					serializer.startTag("", "tag");
					serializer.attribute("", "k", "comment");
					serializer.attribute("", "v", comment);
					serializer.endTag("", "tag");
					serializer.startTag("", "tag");
					serializer.attribute("", "k", "source");
					serializer.attribute("", "v", source);
					serializer.endTag("", "tag");
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
			newChangesetId = Long.parseLong(readLine(in));
		} finally {
			disconnect(connection);
			SavingHelper.close(in);
		}
		changesetId = newChangesetId;
	}

	public void closeChangeset() throws MalformedURLException, ProtocolException, IOException {
		HttpURLConnection connection = null;

		try {
			connection = openConnectionForWriteAccess(getCloseChangesetUrl(changesetId), "PUT");
			checkResponseCode(connection);
		} finally {
			disconnect(connection);
		}
	}

	/**
	 * @param connection
	 * @throws IOException
	 * @throws OsmException
	 */
	private void checkResponseCode(final HttpURLConnection connection) throws IOException, OsmException {
		int responsecode = connection.getResponseCode();
		Log.d("Server", "response code " + responsecode);
		if (responsecode == -1) throw new IOException("Invalid response from server");
		if (responsecode != HttpURLConnection.HTTP_OK) {
			String responseMessage = connection.getResponseMessage();
			if (responseMessage == null) {
				responseMessage = "";
			}
			InputStream in = connection.getErrorStream();
			throw new OsmServerException(responsecode, responsecode + "=\"" + responseMessage + "\" ErrorMessage: " + readStream(in));
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
	 * Perform an HTTP request to download up to 100 bugs inside the specified area.
	 * Blocks until the request is complete.
	 * @param area Latitude/longitude *1E7 of area to download.
	 * @return All the bugs in the given area.
	 */
	public Collection<Bug> getNotesForBox(Rect area, long limit) {
		Collection<Bug> result = new ArrayList<Bug>();
		// http://openstreetbugs.schokokeks.org/api/0.1/getGPX?b=48&t=49&l=11&r=12&limit=100
		try {
			Log.d("Server", "getNotesForBox");
			URL url = new URL(serverURL  + "notes?" +
					"limit=" + limit + "&" +
					"bbox=" +
					(double)area.left / 1E7d +
					"," + (double)area.bottom / 1E7d +
					"," + (double)area.right / 1E7d +
					"," + (double)area.top / 1E7d);
			
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
				throw new OsmServerException(con.getResponseCode(), "The API server does not except the request: " + con
						+ ", response code: " + con.getResponseCode() + " \"" + con.getResponseMessage() + "\"");
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
						result.add(new Bug(parser));
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
		} catch (IOException e) {
			Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
		} catch (OutOfMemoryError e) {
			Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
			// TODO ask the user to exit
			return new ArrayList<Bug>(); // empty list
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
	public boolean addComment(Bug bug, BugComment comment) {
		if (bug.getId() != 0) {
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
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
				} catch (IOException e) {
					Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
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
	public boolean addNote(Bug bug, BugComment comment) {
		if (bug.getId() == 0 && bug.comments.size() == 0) {
			// http://openstreetbugs.schokokeks.org/api/0.1/addPOIexec?lat=<Latitude>&lon=<Longitude>&text=<Bug description with author and date>&format=<Output format>
			HttpURLConnection connection = null;
			try {
				try {
					// setting text/xml here is a hack to stop signpost (the oAuth library) from trying to sign the body which will fail
					connection = 
							openConnectionForWriteAccess(new URL(serverURL  + "notes?lat=" + ((double)bug.getLat() / 1E7d)+"&lon=" + ((double)bug.getLon() / 1E7d) + "&text=" +URLEncoder.encode(comment.getText(), "UTF-8")), "POST", "text/xml");
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
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
				} catch (IOException e) {
					Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
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
	public boolean closeNote(Bug bug) {
		
		if (bug.getId() != 0) {
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
					bug.close();
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
				}
				catch (IOException e) {
					Log.e("Vespucci", "Server.closeNote:Exception", e);
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
	public boolean reopenNote(Bug bug) {
		
		if (bug.getId() != 0) {
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
					bug.reopen();
					return true;
				} catch (XmlPullParserException e) {
					Log.e("Vespucci", "Server.getNotesForBox:Exception", e);
				}
				catch (IOException e) {
					Log.e("Vespucci", "Server.closeNote:Exception", e);
				} 
			} finally {
				disconnect(connection);
			}
		}
		return false;
	}

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
