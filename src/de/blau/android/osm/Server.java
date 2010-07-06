package de.blau.android.osm;

import java.io.BufferedReader;
import java.io.Closeable;
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

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;

import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIOException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.util.Base64;

/**
 * @author mb
 */
public class Server {

	/**
	 * Location of OSM API
	 */
	private static final String SERVER_URL = "http://api.openstreetmap.org";

	/**
	 * Timeout for connections in milliseconds.
	 */
	private static final int TIMEOUT = 45 * 1000;

	/**
	 * username for write-access on the server.
	 */
	private final String username;

	/**
	 * password for write-access on the server.
	 */
	private final String password;

	/**
	 * <a href="http://wiki.openstreetmap.org/wiki/API">API</a>-Version.
	 */
	private final String version = "0.6";

	private final String osmChangeVersion = "0.3";

	/**
	 * Path to api with trailing slash.
	 */
	private final String path = "/api/" + version + "/";

	/**
	 * Tag with "created_by"-key to identify edits made by this editor.
	 */
	private final String createdByTag;
	private final String createdByKey;

	private long changesetId = -1;

	private String generator;

	private final XmlPullParserFactory xmlParserfactory;

	/**
	 * Constructor. Sets {@link #rootOpen} and {@link #createdByTag}.
	 * 
	 * @param username
	 * @param password
	 * @param generator the name of the editor.
	 */
	public Server(final String username, final String password, final String generator) {
		this.password = password;
		this.username = username;
		this.generator = generator;

		createdByTag = "created_by";
		createdByKey = generator;

		XmlPullParserFactory factory = null;
		try {
			factory = XmlPullParserFactory.newInstance(
						System.getProperty(XmlPullParserFactory.PROPERTY_NAME), 
						null
					);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		xmlParserfactory = factory;
	}

	/**
	 * @param area
	 * @return
	 * @throws IOException
	 * @throws OsmServerException
	 */
	public InputStream getStreamForBox(final BoundingBox box) throws OsmServerException, IOException {
		URL url = new URL(SERVER_URL + path + "map?bbox=" + box.toApiString());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		boolean isServerGzipEnabled = false;

		//--Start: header not yet send
		con.setReadTimeout(TIMEOUT);
		con.setConnectTimeout(TIMEOUT);
		con.setRequestProperty("Accept-Encoding", "gzip");

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
		elem.addOrUpdateTag(createdByTag, createdByKey);

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
			}, this.changesetId);
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
		elem.addOrUpdateTag(createdByTag, createdByKey);

		try {
			connection = openConnectionForWriteAccess(getUpdateUrl(elem), "PUT");
			sendPayload(connection, new XmlSerializable() {
				@Override
				public void toXml(XmlSerializer serializer, long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
					startXml(serializer);
					elem.toXml(serializer, changeSetId);
					endXml(serializer);
				}
			}, this.changesetId);
			checkResponseCode(connection);
			in = connection.getInputStream();
			osmVersion = Integer.parseInt(readLine(in));
		} finally {
			disconnect(connection);
			close(in);
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
			throw new OsmIOException("Could not send data to server");
		} finally {
			close(out);
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
		elem.addOrUpdateTag(createdByTag, createdByKey);

		try {
			connection = openConnectionForWriteAccess(getCreationUrl(elem), "PUT");
			sendPayload(connection, new XmlSerializable() {
				@Override
				public void toXml(XmlSerializer serializer, long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
					startXml(serializer);
					elem.toXml(serializer, changeSetId);
					endXml(serializer);
				}
			}, this.changesetId);
			checkResponseCode(connection);
			in = connection.getInputStream();
			osmId = Integer.parseInt(readLine(in));
		} finally {
			disconnect(connection);
			close(in);
		}
		return osmId;
	}

	public void openChangeset() throws MalformedURLException, ProtocolException, IOException {
		int changesetId = -1;
		HttpURLConnection connection = null;
		InputStream in = null;

		try {
			connection = openConnectionForWriteAccess(getCreateChangesetUrl(), "PUT");
			sendPayload(connection, new XmlSerializable() {
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
					serializer.attribute("", "v", "Vespucci edit");
					serializer.endTag("", "tag");
					serializer.endTag("", "changeset");
					endXml(serializer);
				}
			}, this.changesetId);
			checkResponseCode(connection);
			in = connection.getInputStream();
			changesetId = Integer.parseInt(readLine(in));
		} finally {
			disconnect(connection);
			close(in);
		}
		this.changesetId = changesetId;
	}

	public void closeChangeset() throws MalformedURLException, ProtocolException, IOException {
		HttpURLConnection connection = null;

		try {
			connection = openConnectionForWriteAccess(getCloseChangesetUrl(this.changesetId), "PUT");
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
			throw new OsmServerException(responsecode, "\"" + responseMessage + "\" ErrorMessage: " + readStream(in));
		}
	}

	private static String readStream(final InputStream in) {
		String res = "";
		try {
            if (in != null) {
            	BufferedReader reader = new BufferedReader(new InputStreamReader(in), 8000);
            	String line = null;
            	try {
            		while ((line = reader.readLine()) != null) {
            			res += line;
            		}
            	} catch (IOException e) {
            		Log.w(Server.class.getName() + ":readStream()", "Error in read-operation", e);
            	}
            }
        } catch (Exception e) {
            Log.w(Server.class.getName() + ":readStream()", "Error outside of read-operation", e);
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
			e.printStackTrace();
		}

		return res;
	}

	static public void close(final Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private URL getCreationUrl(final OsmElement elem) throws MalformedURLException {
		return new URL(SERVER_URL + path + elem.getName() + "/create");
	}

	private URL getCreateChangesetUrl() throws MalformedURLException {
		return new URL(SERVER_URL + path + "changeset/create");
	}

	private URL getCloseChangesetUrl(long changesetId) throws MalformedURLException {
		return new URL(SERVER_URL + path + "changeset/" + changesetId + "/close");
	}

	private URL getUpdateUrl(final OsmElement elem) throws MalformedURLException {
		return new URL(SERVER_URL + path + elem.getName() + "/" + elem.getOsmId());
	}

	private URL getDeleteUrl(final OsmElement elem) throws MalformedURLException {
		//return getUpdateUrl(elem);
		return new URL(SERVER_URL + path + "changeset/" + changesetId + "/upload");
	}

	public XmlSerializer getXmlSerializer() {
		try {
			XmlSerializer serializer = xmlParserfactory.newSerializer();
			serializer.setPrefix("", "");
			return serializer;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
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
}
