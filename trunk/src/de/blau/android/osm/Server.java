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
	private static final int TIMEOUT = 30 * 1000;

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

	/**
	 * The opening root element for the XML file transferring to the server.
	 */
	private final String rootOpen;

	/**
	 * The closing root element for the XML file transferring to the server.
	 */
	private static final String rootClose = "</osm>";

	private long changesetId = -1;

	private String generator;

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
		rootOpen = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<osm version=\"" + version + "\" generator=\""
				+ generator + "\">\n";

		createdByTag = "created_by";
		createdByKey = generator;
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

		if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new OsmServerException(con.getResponseCode(), "The API server does not except the request: " + con
					+ ", response code: " + con.getResponseCode());
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
		String xml = encloseRootOsmChange(elem, "delete");

		try {
			connection = openConnectionForWriteAccess(getDeleteUrl(elem), "POST");
			sendPayload(connection, xml);
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
		String xml = encloseRoot(elem);

		try {
			connection = openConnectionForWriteAccess(getUpdateUrl(elem), "PUT");
			sendPayload(connection, xml);
			checkResponseCode(connection);
			in = connection.getInputStream();
			osmVersion = Integer.parseInt(readLine(in));
		} finally {
			disconnect(connection);
			close(in);
		}
		return osmVersion;
	}

	/**
	 * @param connection
	 * @param xml
	 * @throws OsmIOException
	 */
	private void sendPayload(final HttpURLConnection connection, final String xml) throws OsmIOException {
		connection.setFixedLengthStreamingMode(xml.getBytes().length);
		OutputStreamWriter out = null;
		try {
			out = new OutputStreamWriter(connection.getOutputStream(), Charset.defaultCharset());
			out.write(xml);
			out.flush();
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
		elem.addOrUpdateTag(createdByTag, createdByTag);
		String xml = encloseRoot(elem);

		try {
			connection = openConnectionForWriteAccess(getCreationUrl(elem), "PUT");
			sendPayload(connection, xml);
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
		String xml = "<osm><changeset><tag k=\"created_by\" v=\"" + this.generator
				+ "\"/><tag k=\"comment\" v=\"Vespucci edit\"/></changeset></osm>";

		try {
			connection = openConnectionForWriteAccess(getCreateChangesetUrl(), "PUT");
			sendPayload(connection, xml);
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
		int responsecode;
		responsecode = connection.getResponseCode();
		if (responsecode != HttpURLConnection.HTTP_OK) {
			InputStream in = connection.getErrorStream();
			throw new OsmServerException(responsecode, "ErrorMessage: " + readStream(in));
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
				e.printStackTrace();
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

	private String encloseRoot(final OsmElement elem) {
		return rootOpen + elem.toXml(this.changesetId) + rootClose;
	}

	private String encloseRootOsmChange(final OsmElement elem, String action) {
		return getOsmChangeOpen(action) + elem.toXml(this.changesetId) + getOsmChangeClose(action);
	}

	private String getOsmChangeOpen(String action) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<osmChange version=\"" + osmChangeVersion
				+ "\" generator=\"" + generator + "\">\n" + "<" + action + " version=\"" + osmChangeVersion
				+ "\" generator=\"" + generator + "\">\n";
	}

	private String getOsmChangeClose(String action) {
		return "</" + action + ">\n</osmChange>";
	}

}
