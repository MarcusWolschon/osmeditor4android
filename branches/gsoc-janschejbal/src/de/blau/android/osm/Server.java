package de.blau.android.osm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import de.blau.android.Application;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.util.Base64;
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
	 * display name of the user.
	 */
	private String display_name;

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

	
	
	@Deprecated
	public Server(final String username, final String password, final String generator) {
		this("", username, password, generator);
	}
	
	/**
	 * Constructor. Sets {@link #rootOpen} and {@link #createdByTag}.
	 * @param apiurl The OSM API URL to use (e.g. "http://api.openstreetmap.org/api/0.6/").
	 * @param username
	 * @param password
	 * @param generator the name of the editor.
	 */
	public Server(final String apiurl, final String username, final String password, final String generator) {
		if (apiurl != null && !apiurl.equals("")) {
			this.serverURL = apiurl;
		} else {
			this.serverURL = "http://api.openstreetmap.org/api/"+version+"/";
		}
		this.password = password;
		this.username = username;
		this.generator = generator;
		display_name = null;

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
	 * Get the display name for the user.
	 * @return The display name for the user, or null if it couldn't be determined.
	 */
	public String getDisplayName() {
		if (display_name == null) {
			// Haven't retrieved the display name from OSM - try to
			try {
				HttpURLConnection connection = openConnectionForWriteAccess(getUserDetailsUrl(), "GET");
				try {
					connection.getOutputStream().close();
					checkResponseCode(connection);
					XmlPullParser parser = xmlParserFactory.newPullParser();
					parser.setInput(connection.getInputStream(), null);
					int eventType;
					while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
						String tagName = parser.getName();
						if (eventType == XmlPullParser.START_TAG && tagName.equals("user")) {
							display_name = parser.getAttributeValue(null, "display_name");
						}
					}
				} finally {
					disconnect(connection);
				}
			} catch (XmlPullParserException e) {
				Log.e("Vespucci", "Problem accessing display name", e);
			} catch (MalformedURLException e) {
				Log.e("Vespucci", "Problem accessing display name", e);
			} catch (ProtocolException e) {
				Log.e("Vespucci", "Problem accessing display name", e);
			} catch (IOException e) {
				Log.e("Vespucci", "Problem accessing display name", e);
			}
		}
		return display_name;
	}

	/**
	 * @param area
	 * @return
	 * @throws IOException
	 * @throws OsmServerException
	 */
	public InputStream getStreamForBox(final BoundingBox box) throws OsmServerException, IOException {
		URL url = new URL(serverURL  + "map?bbox=" + box.toApiString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		boolean isServerGzipEnabled = false;

		//--Start: header not yet send
		con.setReadTimeout(TIMEOUT);
		con.setConnectTimeout(TIMEOUT);
		con.setRequestProperty("Accept-Encoding", "gzip");
		con.setRequestProperty("User-Agent", Application.userAgent);

		//--Start: got response header
		isServerGzipEnabled = "gzip".equals(con.getHeaderField("Content-encoding"));

		// retry if we have no resopnse-code
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

		try {
			connection = openConnectionForWriteAccess(getDeleteUrl(elem), "POST");
			sendPayload(connection, new XmlSerializable() {
				@Override
				public void toXml(XmlSerializer serializer, long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
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
		return username != null && password != null && !username.equals("") && !username.equals("");
	}

	/**
	 * @param connection
	 */
	private static void disconnect(final HttpURLConnection connection) {
		if (connection != null) {
			connection.disconnect();
		}
	}

	public int updateElement(final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
		int osmVersion = -1;
		HttpURLConnection connection = null;
		InputStream in = null;
//		elem.addOrUpdateTag(createdByTag, createdByKey);

		try {
			connection = openConnectionForWriteAccess(getUpdateUrl(elem), "PUT");
			sendPayload(connection, new XmlSerializable() {
				@Override
				public void toXml(XmlSerializer serializer, long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
					startXml(serializer);
					elem.toXml(serializer, changeSetId);
					endXml(serializer);
				}
			}, changesetId);
			checkResponseCode(connection);
			in = connection.getInputStream();
			osmVersion = Integer.parseInt(readLine(in));
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
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent", Application.userAgent);
		connection.setConnectTimeout(TIMEOUT);
		connection.setReadTimeout(TIMEOUT);
		connection.setRequestProperty("Authorization", "Basic " + Base64.encode(username + ":" + password));
		connection.setRequestMethod(requestMethod);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		return connection;
	}

	public int createElement(final OsmElement elem) throws MalformedURLException, ProtocolException, IOException {
		int osmId = -1;
		HttpURLConnection connection = null;
		InputStream in = null;
//		elem.addOrUpdateTag(createdByTag, createdByKey);

		try {
			connection = openConnectionForWriteAccess(getCreationUrl(elem), "PUT");
			sendPayload(connection, new XmlSerializable() {
				@Override
				public void toXml(XmlSerializer serializer, long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
					startXml(serializer);
					elem.toXml(serializer, changeSetId);
					endXml(serializer);
				}
			}, changesetId);
			checkResponseCode(connection);
			in = connection.getInputStream();
			osmId = Integer.parseInt(readLine(in));
		} finally {
			disconnect(connection);
			SavingHelper.close(in);
		}
		return osmId;
	}

	/**
	 * Open a new changeset.
	 * @param comment Changeset comment.
	 * @throws MalformedURLException
	 * @throws ProtocolException
	 * @throws IOException
	 */
	public void openChangeset(final String comment) throws MalformedURLException, ProtocolException, IOException {
		int newChangesetId = -1;
		HttpURLConnection connection = null;
		InputStream in = null;

		try {
			XmlSerializable xmlData = new XmlSerializable() {
				@Override
				public void toXml(XmlSerializer serializer, long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
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
			newChangesetId = Integer.parseInt(readLine(in));
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
}
