package io.vespucci.osm;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.gpx.Track;

public class GpxFile {
    private static final String DEBUG_TAG = GpxFile.class.getSimpleName().substring(0, Math.min(23, GpxFile.class.getSimpleName().length()));

    private long         id;
    private String       name;
    private long         timestamp;
    private double       lon;
    private double       lat;
    private String       description;
    private List<String> tags;

    /**
     * Get the OSM id for the track
     * 
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * Get the name of the track
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the timestamp
     * 
     * @return the timestamp as seconds since the epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the longitude
     * 
     * @return the WGS83 longitude
     */
    public double getLon() {
        return lon;
    }

    /**
     * Get the latitude
     * 
     * @return the WGS84 latitude
     */
    public double getLat() {
        return lat;
    }

    /**
     * Get the description of the track
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get any optional tags
     * 
     * @return the tags
     */
    @NonNull
    public List<String> getTags() {
        return tags != null ? tags : new ArrayList<>();
    }

    @Override
    public String toString() {
        return name + (description != null ? " " + description : "");
    }

    /*
     * <gpx_file id="3865232" name="2021_10_10T133854.gpx" user="SimonPoole" visibility="public" pending="false"
     * timestamp="2021-10-10T13:38:55Z" lat="47.51912633" lon="8.12352579"> <description>Perimukweg</description>
     * </gpx_file>
     */

    /**
     * Parse the output of the GPX API /user/gpx_files call
     * 
     * @param in InputStream that we are reading from
     * @return a List of GpxFile
     * @throws SAXException on parsing exceptions
     * @throws IOException if reading the InputStream caused errors
     * @throws ParserConfigurationException
     */
    @NonNull
    public static List<GpxFile> parse(final InputStream in) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        Parser parser = new Parser();
        saxParser.parse(in, parser);
        return parser.get();
    }

    private static class Parser extends DefaultHandler {
        private static final String OSM_ELEMENT         = "osm";
        private static final String GPX_FILE_ELEMENT    = "gpx_file";
        private static final String DESCRIPTION_ELEMENT = "description";
        private static final String TAG_ELEMENT         = "tag";

        private enum State {
            NONE, OSM, GPX_FILE, DESC, TAG
        }

        private String                 parsedDescription = null;
        private String                 parsedTag         = null;
        private final List<GpxFile>    result;
        private GpxFile                temp;
        private final SimpleDateFormat iso8601Format;

        private State state = State.NONE;

        /**
         * Construct a new parser
         */
        Parser() {
            super();
            result = new ArrayList<>();
            iso8601Format = new SimpleDateFormat(Track.DATE_PATTERN_ISO8601_UTC, Locale.US);
            iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        /**
         * Get the result of parsing
         * 
         * @return a list of parsed GpxFile
         */
        @NonNull
        public List<GpxFile> get() {
            return result;
        }

        @Override
        public void startElement(final String uri, final String element, final String qName, final Attributes atts) {
            try {
                switch (element) {
                case OSM_ELEMENT:
                    state = State.OSM;
                    break;
                case GPX_FILE_ELEMENT:
                    state = State.GPX_FILE;
                    temp = new GpxFile();
                    temp.id = Long.parseLong(atts.getValue("id"));
                    temp.name = atts.getValue("name");
                    temp.timestamp = iso8601Format.parse(atts.getValue("timestamp")).getTime();
                    temp.lon = Double.parseDouble(atts.getValue("lon"));
                    temp.lat = Double.parseDouble(atts.getValue("lat"));
                    break;
                case DESCRIPTION_ELEMENT:
                    state = State.DESC;
                    break;
                case TAG_ELEMENT:
                    state = State.TAG;
                    if (temp.tags == null) {
                        temp.tags = new ArrayList<>();
                    }
                    break;
                default:
                    Log.w(DEBUG_TAG, "got unexpected element " + element);
                }
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Parse Exception", e);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            String tempStr = new String(ch, start, length);
            if (State.DESC == state) {
                parsedDescription = tempStr;
            } else if (State.TAG == state) {
                parsedTag = tempStr;
            }
        }

        @Override
        public void endElement(final String uri, final String element, final String qName) {
            switch (element) {
            case GPX_FILE_ELEMENT:
                state = State.OSM;
                result.add(temp);
                break;
            case DESCRIPTION_ELEMENT:
                state = State.GPX_FILE;
                temp.description = parsedDescription;
                break;
            case TAG_ELEMENT:
                state = State.GPX_FILE;
                temp.tags.add(parsedTag);
                break;
            case OSM_ELEMENT:
            default:
                state = State.NONE;
            }
        }
    }
}