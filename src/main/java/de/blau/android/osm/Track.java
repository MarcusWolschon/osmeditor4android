package de.blau.android.osm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.osm.GeoPoint.InterruptibleGeoPoint;
import de.blau.android.util.SavingHelper;

/**
 * GPS track data class. Only one instance allowed. Automatically saves and loads content. Content saving happens
 * continuously to avoid large delays when closing. A BufferedOutputStream is used to prevent large amounts of flash
 * wear.
 */
public class Track extends DefaultHandler implements GpxTimeFormater {
    private static final String SYM_ELEMENT = "sym";

    private static final String TYPE_ELEMENT = "type";

    private static final String DESCRIPTION_ELEMENT = "description";

    private static final String NAME_ELEMENT = "name";

    private static final String WPT_ELEMENT = "wpt";

    private static final String ELE_ELEMENT = "ele";

    private static final String TIME_ELEMENT = "time";

    private static final String TRKPT_ELEMENT = "trkpt";

    private static final String TRKSEG_ELEMENT = "trkseg";

    private static final String TRK_ELEMENT = "trk";

    private static final String GPX_ELEMENT = "gpx";

    private static final String DEBUG_TAG = "Track";

    private final List<TrackPoint> currentTrack;

    private final ArrayList<WayPoint> currentWayPoints;

    private static final String SAVEFILE = "track.dat";

    private static final String WAYPOINT_SAVEFILE = "waypoints.dat";

    private final Context ctx;

    private transient SavingHelper<ArrayList<WayPoint>> wayPointsSaver = new SavingHelper<>();

    /**
     * For conversion from UNIX epoch time and back
     */
    private static final String    DATE_PATTERN_ISO8601_UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private final SimpleDateFormat ISO8601FORMAT;
    private final Calendar         calendarInstance         = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * if loadingFinished is true, indicates how many records the save file contains
     */
    private int savedTrackPoints = 0;

    /**
     * Set to true as soon as loading is finished. If true, it is guaranteed that either: a) the save file does not
     * exist, savedTrackPoints is 0 and memory does not contain any significant amount of data b) the save file does
     * exist, is valid and contains exactly savedTrackPoints records
     */
    private Boolean loadingFinished = false;

    /**
     * Everything except loading happens on the UI thread. {@link #close()} may be called on the UI thread to close the
     * track. After this, the track file must not be touched. This can be easily achieved using {@link #savingDisabled}
     * for the UI thread. However, an asynchronous load could still be running. To prevent closing the file while such a
     * load is running, the loadingLock is used.
     */
    private ReentrantLock loadingLock = new ReentrantLock();

    /**
     * Set to true when an unrecoverable error prevents track saving, to avoid crashing the app.
     */
    private boolean savingDisabled = false;

    private DataOutputStream saveFileStream = null;

    /**
     * Ensure only one instance may be open at a time
     */
    private static volatile boolean isOpen   = false;
    private static Object           openLock = new Object();

    /** set by {@link #markNewSegment()} - indicates that the next track point will have the isNewSegment flag set */
    private boolean nextIsNewSegment = false;

    /**
     * Basic constructor
     * 
     * @param context Android Context
     */
    public Track(Context context) {
        // Hardcode 'Z' timezone marker as otherwise '+0000' will be used, which is invalid in GPX
        ISO8601FORMAT = new SimpleDateFormat(DATE_PATTERN_ISO8601_UTC, Locale.US);
        ISO8601FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        currentTrack = new ArrayList<>();
        currentWayPoints = new ArrayList<>();
        ctx = context;
        synchronized (openLock) {
            if (isOpen) {
                markSavingBroken("Attempted to open multiple instances of Track - saving disabled for this instance", null);
            } else {
                isOpen = true;
                Log.i(DEBUG_TAG, "Opened track");
                asyncLoad();
            }
        }
    }

    /**
     * Reset the recorded track and delete the saved state files
     */
    public void reset() {
        deleteSaveFile();
        currentTrack.clear();
        currentWayPoints.clear();
    }

    /**
     * Check if we have any stored elements
     * 
     * @return true if there are neither track or way points stored
     */
    public boolean isEmpty() {
        return (currentTrack == null || currentTrack.isEmpty()) && (currentWayPoints == null || currentWayPoints.isEmpty());
    }

    /**
     * Create and add a TrackPoint from an Android Location
     * 
     * @param location the Location object
     */
    public void addTrackPoint(final Location location) {
        if (location != null) {
            currentTrack.add(new TrackPoint(location, nextIsNewSegment));
            nextIsNewSegment = false;
        }
    }

    /**
     * Get the TrackPoints for this track
     * 
     * FIXME allocating a new array should be avoided but may be necessary to avoid concurrent mod. issues
     * 
     * @return an array of TrackPoint
     */
    public List<TrackPoint> getTrackPoints() {
        return new ArrayList<>(currentTrack);
    }

    /**
     * Get the WayPoints for this track
     * 
     * FIXME allocating a new array should be avoided but may be necessary to avoid concurrent mod. issues
     * 
     * @return a List of WayPoint
     */
    public WayPoint[] getWayPoints() {
        return currentWayPoints.toArray(new WayPoint[0]); // need a shallow copy here
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("");
        for (TrackPoint loc : currentTrack) {
            str.append(loc.toString());
            str.append('\n');
        }
        return str.toString();
    }

    /**
     * Save current state to files
     * 
     * Note: currently the track saving is still using the original implementation and not simply serializing to disk
     */
    public void save() {
        if (savingDisabled) {
            Log.e(DEBUG_TAG, "Saving disabled but tried to save");
            return;
        }
        if (!loadingFinished) {
            return;
        }

        if (currentWayPoints != null) {
            wayPointsSaver.save(ctx, WAYPOINT_SAVEFILE, currentWayPoints, true);
        }

        if (savedTrackPoints == currentTrack.size()) {
            return;
        }

        // There are records to be saved
        ensureFileOpen();
        while (savedTrackPoints < currentTrack.size()) {
            try {
                currentTrack.get(savedTrackPoints).toStream(saveFileStream);
            } catch (IOException e) {
                markSavingBroken("Failed to save track point", e);
                return;
            }
            savedTrackPoints++;
        }
    }

    /**
     * Opens the saveFileStream if necessary
     */
    private void ensureFileOpen() {
        if (savingDisabled) {
            Log.e(DEBUG_TAG, "Saving disabled but tried to ensureFileOpen");
            return;
        }
        if (saveFileStream != null) {
            return;
        }
        File saveFile = new File(ctx.getFilesDir(), SAVEFILE);
        FileOutputStream fileOutput = null;
        DataOutputStream out = null;
        try {
            if (saveFile.exists()) {
                // append to existing save file
                fileOutput = ctx.openFileOutput(SAVEFILE, Context.MODE_APPEND);
                out = new DataOutputStream(new BufferedOutputStream(fileOutput));
            } else {
                // no save file, create one
                fileOutput = ctx.openFileOutput(SAVEFILE, Context.MODE_PRIVATE);
                out = new DataOutputStream(new BufferedOutputStream(fileOutput));
                out.writeInt(TrackPoint.FORMAT_VERSION);
                savedTrackPoints = 0;
            }
            saveFileStream = out;
        } catch (Exception e) {
            markSavingBroken("Failed to open track save file", e);
        }
    }

    /**
     * Delete files containing saved state
     */
    private void deleteSaveFile() {
        if (savingDisabled) {
            Log.e(DEBUG_TAG, "Saving disabled but tried to deleteSaveFile");
            return;
        }
        if (saveFileStream != null) {
            SavingHelper.close(saveFileStream);
            saveFileStream = null;
        }
        savedTrackPoints = 0;
        File saveFile = new File(ctx.getFilesDir(), SAVEFILE);
        if (!saveFile.delete() && saveFile.exists()) {
            markSavingBroken("Failed to delete undesired track file", null);
        }

        saveFile = new File(ctx.getFilesDir(), WAYPOINT_SAVEFILE);
        if (!saveFile.delete() && saveFile.exists()) {
            Log.w(DEBUG_TAG, "Failed to delete waypoint save file");
        }
    }

    /**
     * If something terrible happens, use this to log an error and disable saving
     * 
     * @param message message to log
     * @param exception exception to throw
     */
    private void markSavingBroken(String message, Throwable exception) {
        savingDisabled = true;
        Log.e(DEBUG_TAG, "Saving broken - " + message, exception);
    }

    /**
     * Load saved track state asynchronously, locking against changes in the time
     */
    private void asyncLoad() {
        new AsyncTask<Void, Void, Void>() {
            private ArrayList<TrackPoint> loaded = new ArrayList<>();

            @Override
            protected Void doInBackground(Void... params) {
                loadingLock.lock();
                try {
                    List<WayPoint> loadedWayPoints = wayPointsSaver.load(ctx, WAYPOINT_SAVEFILE, true);
                    if (loadedWayPoints != null) {
                        currentWayPoints.clear();
                        currentWayPoints.addAll(loadedWayPoints);
                    }
                    if (!isOpen) {
                        return null; // if this has been closed by close() in the meantime, STOP
                    }

                    File saveFile = new File(ctx.getFilesDir(), SAVEFILE);
                    boolean success = load();
                    if (!success || loaded.isEmpty()) {
                        Log.i(DEBUG_TAG, "Deleting broken or empty save file");
                        deleteSaveFile();
                    }

                    // If the save file exists, it contains exactly the elements in loaded
                    if (!loaded.isEmpty() && !saveFile.exists()) {
                        // A broken save file was partially recovered. Rewrite it now.
                        Log.i(DEBUG_TAG, "Rewriting partially recovered save file");
                        rewriteSaveFile(loaded);
                    }

                    savedTrackPoints = loaded.size();

                    // There are only two possible situations now:
                    // - save file does not exist, savedTrackPoints is 0 and memory does not contain any significant
                    // amount of data
                    // - save file does exist, is valid and contains exactly savedTrackPoints records

                    return null;
                } finally {
                    loadingLock.unlock();
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                currentTrack.addAll(0, loaded);
                loadingFinished = true;
                // See end of doInBackground for possible states
                Log.i(DEBUG_TAG, "Track loading finished, loaded entries: " + loaded.size());
                if (currentTrack.size() > savedTrackPoints) {
                    save();
                }
            }

            /**
             * Loads a track from the file to the "loaded" ArrayList.
             * 
             * @return true if the file was loaded without problems, false if some problem occurred and the file needs
             *         to be rewritten
             */
            private boolean load() {
                FileInputStream fileInput = null;
                DataInputStream in = null;
                try {
                    fileInput = ctx.openFileInput(SAVEFILE);
                    in = new DataInputStream(new BufferedInputStream(fileInput));
                    long size = fileInput.getChannel().size();
                    // if you manage to record over 32 GB of track data (in RAM) on a mobile device,
                    // which means non-stop recording over many many years,
                    // you deserve the problem you are going to get when the integer overflows in the next line.
                    int records = (int) ((size - 4) / TrackPoint.RECORD_SIZE);

                    loaded.ensureCapacity(records);
                    if (in.readInt() != TrackPoint.FORMAT_VERSION) {
                        Log.e(DEBUG_TAG, "cannot load track, incompatible data format");
                        return false;
                    }

                    for (int i = 0; i < records; i++) {
                        loaded.add(TrackPoint.fromStream(in));
                    }

                    if ((size - 4) % TrackPoint.RECORD_SIZE != 0) {
                        Log.e(DEBUG_TAG, "track file contains partial record");
                        return false;
                    }

                    return true;
                } catch (FileNotFoundException e) {
                    Log.i(DEBUG_TAG, "No saved track");
                    return false;
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "failed to (completely) load track", e);
                    return false;
                } finally {
                    SavingHelper.close(in);
                    SavingHelper.close(fileInput);
                }
            }

            /**
             * Saves the given data to disk, overwriting anything already saved
             */
            private void rewriteSaveFile(Iterable<TrackPoint> data) {
                FileOutputStream fileOutput = null;
                DataOutputStream out = null;
                try {
                    fileOutput = ctx.openFileOutput(SAVEFILE, Context.MODE_PRIVATE);
                    out = new DataOutputStream(new BufferedOutputStream(fileOutput));
                    out.writeInt(TrackPoint.FORMAT_VERSION);
                    for (TrackPoint point : data) {
                        point.toStream(out);
                    }
                } catch (Exception e) {
                    markSavingBroken("Failed to rewrite broken save file", e);
                } finally {
                    SavingHelper.close(out);
                    SavingHelper.close(fileOutput);
                }
            }

        }.execute();
    }

    /**
     * Saves and closes the track. The object should not be used afterwards, as saving will be disabled. The save file
     * will never be accessed by this object again, and isOpen will be set to false. This will allow to open the track
     * again.
     */
    public void close() {
        if (!isOpen) {
            return;
        }
        Log.d(DEBUG_TAG, "Trying to close track");
        loadingLock.lock();
        try {
            save();
            if (saveFileStream != null) {
                SavingHelper.close(saveFileStream);
                saveFileStream = null;
            }
            savingDisabled = true;
            isOpen = false;
            Log.i(DEBUG_TAG, "Track closed");
        } finally {
            loadingLock.unlock();
        }
    }

    /**
     * Call each time a new segment should be created.
     */
    public void markNewSegment() {
        nextIsNewSegment = true;
    }

    /**
     * Writes GPX data to the output stream.
     * 
     * @param outputStream the stream we are writing to
     * @throws XmlPullParserException
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public void exportToGPX(@NonNull OutputStream outputStream) throws XmlPullParserException, IllegalArgumentException, IllegalStateException, IOException {
        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        serializer.setOutput(outputStream, "UTF-8");
        serializer.startDocument("UTF-8", null);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null, GPX_ELEMENT);
        serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/0");
        serializer.attribute(null, "xsi:schemaLocation", "http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd");
        serializer.attribute(null, "version", "1.0");
        serializer.attribute(null, "creator", "Vespucci");
        serializer.startTag(null, TRK_ELEMENT);
        serializer.startTag(null, TRKSEG_ELEMENT);
        boolean hasPoints = false;
        for (TrackPoint pt : getTrackPoints()) {
            if (hasPoints && pt.isNewSegment()) {
                // start new segment
                serializer.endTag(null, TRKSEG_ELEMENT);
                serializer.startTag(null, TRKSEG_ELEMENT);
            }
            hasPoints = true;
            pt.toXml(serializer, this);
        }
        serializer.endTag(null, TRKSEG_ELEMENT);
        serializer.endTag(null, TRK_ELEMENT);
        serializer.endTag(null, GPX_ELEMENT);
        serializer.endDocument();
    }

    public String format(long time) {
        calendarInstance.setTimeInMillis(time);
        return ISO8601FORMAT.format(new Date(time));
    }

    /**
     * Reads GPX data from the output stream.
     * 
     * Doesn't reset the storage and will simply add elements
     * 
     * @param is InputStream that we are reading from
     */
    public void importFromGPX(InputStream is) {
        try {
            start(is);
        } catch (Exception e) {
            Log.e("Track", "importFromGPX failed " + e);
        }
    }

    /**
     * start parsing a GPX file
     * 
     * @param in InputStream that we are reading from
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private void start(final InputStream in) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(in, this);
    }

    /**
     * minimalistic GPX file parser
     */
    private boolean newSegment        = false;
    private double  parsedLat;
    private double  parsedLon;
    private double  parsedEle         = Double.NaN;
    private long    parsedTime        = 0L;
    private String  parsedName        = null;
    private String  parsedDescription = null;
    private String  parsedType        = null;
    private String  parsedSymbol      = null;

    private enum State {
        NONE, TIME, ELE, NAME, DESC, TYPE, SYM
    }

    private State state = State.NONE;

    @Override
    public void startElement(final String uri, final String element, final String qName, final Attributes atts) {
        try {
            switch (element) {
            case GPX_ELEMENT:
                state = State.NONE;
                Log.d("Track", "parsing gpx");
                break;
            case TRK_ELEMENT:
                Log.d("Track", "parsing trk");
                break;
            case TRKSEG_ELEMENT:
                Log.d("Track", "parsing trkseg");
                newSegment = true;
                break;
            case TRKPT_ELEMENT:
            case WPT_ELEMENT:
                parsedLat = Double.parseDouble(atts.getValue("lat"));
                parsedLon = Double.parseDouble(atts.getValue("lon"));
                break;
            case TIME_ELEMENT:
                state = State.TIME;
                break;
            case ELE_ELEMENT:
                state = State.ELE;
                break;
            case NAME_ELEMENT:
                state = State.NAME;
                break;
            case DESCRIPTION_ELEMENT:
                state = State.DESC;
                break;
            case TYPE_ELEMENT:
                state = State.TYPE;
                break;
            case SYM_ELEMENT:
                state = State.SYM;
                break;
            default:
            }
        } catch (Exception e) {
            Log.e("Profil", "Parse Exception", e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        switch (state) {
        case NONE:
            return;
        case ELE:
            parsedEle = Double.parseDouble(new String(ch, start, length));
            return;
        case TIME:
            try {
                parsedTime = parseTime(new String(ch, start, length));
            } catch (ParseException e) {
                parsedTime = 0L;
            }
            return;
        case NAME:
            parsedName = new String(ch, start, length);
            return;
        case DESC:
            parsedDescription = new String(ch, start, length);
            return;
        case TYPE:
            parsedType = new String(ch, start, length);
            return;
        default:
            break;
        }
    }

    /**
     * Parse a string to a milliseconds since the epoch value.
     * 
     * Synchronized method to avoid potential problem with static DateFormat
     * 
     * @param t input time string
     * @return milliseconds since the epoch
     * @throws ParseException if the time string cannot be parsed
     */
    private synchronized long parseTime(String t) throws ParseException {
        return ISO8601FORMAT.parse(t).getTime();
    }

    @Override
    public void endElement(final String uri, final String element, final String qName) {
        switch (element) {
        case GPX_ELEMENT:
            break;
        case TRK_ELEMENT:
            break;
        case TRKSEG_ELEMENT:
            break;
        case TRKPT_ELEMENT:
            currentTrack.add(new TrackPoint(newSegment ? TrackPoint.FLAG_NEWSEGMENT : 0, parsedLat, parsedLon, parsedEle, parsedTime));
            newSegment = false;
            parsedEle = Double.NaN;
            parsedTime = 0L;
            state = State.NONE;
            break;
        case WPT_ELEMENT:
            currentWayPoints.add(new WayPoint(parsedLat, parsedLon, parsedEle, parsedTime, parsedName, parsedDescription, parsedType, parsedSymbol));
            parsedEle = Double.NaN;
            parsedTime = 0L;
            parsedName = null;
            parsedDescription = null;
            parsedType = null;
            parsedSymbol = null;
            state = State.NONE;
            break;
        case TIME_ELEMENT:
        case ELE_ELEMENT:
        case NAME_ELEMENT:
        case DESCRIPTION_ELEMENT:
        case TYPE_ELEMENT:
        case SYM_ELEMENT:
            state = State.NONE;
            break;
        default:
            state = State.NONE;
        }
    }

    /**
     * Get the list of TrackPoints
     * 
     * @return a List of TrackPoint
     */
    public List<TrackPoint> getTrack() {
        return currentTrack;
    }

    /**
     * This is a class to store location points and provide storing/serialization for them. Everything considered less
     * relevant is commented out to save space. If you chose that this should be included in the GPX, uncomment it,
     * increment {@link #FORMAT_VERSION}, set the correct {@link #RECORD_SIZE} and rewrite
     * {@link #fromStream(DataInputStream)}, {@link #toStream(DataOutputStream)} and {@link #getGPXString()}.
     * 
     * @author Jan
     */
    public static class TrackPoint implements InterruptibleGeoPoint, Serializable {

        // private static final SimpleDateFormat ISO8601FORMAT;
        // private static final Calendar calendarInstance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // static {
        // // Hardcode 'Z' timezone marker as otherwise '+0000' will be used, which is invalid in GPX
        // ISO8601FORMAT = new SimpleDateFormat(DATE_PATTERN_ISO8601_UTC);
        // ISO8601FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        // }

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public static final int FORMAT_VERSION = 2;
        public static final int RECORD_SIZE    = 1 + 4 * 8;

        public static final byte FLAG_NEWSEGMENT = 1;
        public final byte        flags;
        public final double      latitude;
        public final double      longitude;
        public final double      altitude;
        public final long        time;
        // public final Float accuracy;
        // public final Float bearing;
        // public final Float speed;

        /**
         * Construct a TrackPoint from a location
         * 
         * @param loc the Location we want to use
         * @param isNewSegment true id we are starting a new segment
         */
        public TrackPoint(Location loc, boolean isNewSegment) {
            flags = encodeFlags(isNewSegment);
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();
            altitude = loc.hasAltitude() ? loc.getAltitude() : Double.NaN;
            time = loc.getTime();
            // accuracy = original.hasAccuracy() ? original.getAccuracy() : null;
            // bearing = original.hasBearing() ? original.getBearing() : null;
            // speed = original.hasSpeed() ? original.getSpeed() : null;
        }

        public TrackPoint(byte flags, double latitude, double longitude, double altitude, long time) {
            // Log.d("Track","new trkpt " + flags + " " + latitude+ " " + longitude+ " " + altitude+ " " + time);
            this.flags = flags;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.time = time;
        }

        public TrackPoint(byte flags, double latitude, double longitude, long time) {
            this(flags, latitude, longitude, Double.NaN, time);
        }

        /**
         * Loads a track point from a {@link DataInputStream}
         * 
         * @param stream the stream from which to load
         * @return the loaded data point
         * @throws IOException if anything goes wrong
         */
        public static TrackPoint fromStream(DataInputStream stream) throws IOException {
            return new TrackPoint(stream.readByte(), // flags
                    stream.readDouble(), // lat
                    stream.readDouble(), // lon
                    stream.readDouble(), // alt
                    stream.readLong() // time
            );
        }

        /**
         * Writes the current track point to the data output stream
         * 
         * @param stream target stream
         * @throws IOException
         */
        public void toStream(DataOutputStream stream) throws IOException {
            stream.writeByte(flags);
            stream.writeDouble(latitude);
            stream.writeDouble(longitude);
            stream.writeDouble(altitude);
            stream.writeLong(time);
        }

        @Override
        public int getLat() {
            return (int) (latitude * 1E7);
        }

        @Override
        public int getLon() {
            return (int) (longitude * 1E7);
        }

        /**
         * Get the latitude
         * 
         * @return the latitude in WGS84 coordinates
         */
        public double getLatitude() {
            return latitude;
        }

        /**
         * Get the longitude
         * 
         * @return the longitude in WGS84 coordinates
         */
        public double getLongitude() {
            return longitude;
        }

        /**
         * Return the time as milliseconds since the epoch
         * 
         * @return time in milliseconds
         */
        public long getTime() {
            return time;
        }

        /**
         * Check if this object has an altitude value
         * 
         * @return true if we have a valid altitude
         */
        public boolean hasAltitude() {
            return !Double.isNaN(altitude);
        }
        // public boolean hasAccuracy() { return accuracy != null; }
        // public boolean hasBearing() { return bearing != null; }
        // public boolean hasSpeed() { return speed != null; }

        /**
         * Get the alitude
         * 
         * @return the altitude
         */
        public double getAltitude() {
            return !Double.isNaN(altitude) ? altitude : 0d;
        }
        // public float getAccuracy() { return accuracy != null ? accuracy : 0f; }
        // public float getBearing() { return bearing != null ? bearing : 0f; }
        // public float getSpeed() { return speed != null ? speed : 0f; }

        private byte encodeFlags(boolean isNewSegment) {
            byte result = 0;
            if (isNewSegment) {
                result += FLAG_NEWSEGMENT;
            }
            return result;
        }

        /**
         * Check if this ibject marks the beginning of a new segment
         * 
         * @return true if a new segment starts here
         */
        public boolean isNewSegment() {
            return (flags & FLAG_NEWSEGMENT) > 0;
        }

        /**
         * Adds a GPX trkpt (track point) tag to the given serializer (synchronized due to use of calendarInstance)
         * 
         * @param serializer the xml serializer to use for output
         * @param gtf class providing a formatter to GPX time data
         * @throws IOException
         */
        public synchronized void toXml(XmlSerializer serializer, GpxTimeFormater gtf) throws IOException {
            serializer.startTag(null, TRKPT_ELEMENT);
            serializer.attribute(null, "lat", String.format(Locale.US, "%f", latitude));
            serializer.attribute(null, "lon", String.format(Locale.US, "%f", longitude));
            if (hasAltitude()) {
                serializer.startTag(null, ELE_ELEMENT).text(String.format(Locale.US, "%f", altitude)).endTag(null, ELE_ELEMENT);
            }
            serializer.startTag(null, TIME_ELEMENT).text(gtf.format(time)).endTag(null, TIME_ELEMENT);
            serializer.endTag(null, TRKPT_ELEMENT);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%f, %f", latitude, longitude);
        }

        @Override
        public boolean isInterrupted() {
            return isNewSegment();
        }
    }

    public static class WayPoint extends TrackPoint {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        private String name;
        private String description;
        private String type;
        private String symbol;

        public WayPoint(double latitude, double longitude, double altitude, long time, String name, String description, String type, String symbol) {
            super((byte) 0, latitude, longitude, altitude, time);
            this.name = name;
            this.description = description;
            this.type = type;
            this.symbol = symbol;
        }

        /**
         * Adds a GPX trkpt (track point) tag to the given serializer (synchronized due to use of calendarInstance)
         * 
         * @param serializer the xml serializer to use for output
         * @param gtf class providing a formatter to GPX time data
         * @throws IOException
         */
        public synchronized void toXml(XmlSerializer serializer, GpxTimeFormater gtf) throws IOException {
            serializer.startTag(null, WPT_ELEMENT);
            serializer.attribute(null, "lat", String.format(Locale.US, "%f", latitude));
            serializer.attribute(null, "lon", String.format(Locale.US, "%f", longitude));
            if (hasAltitude()) {
                serializer.startTag(null, ELE_ELEMENT).text(String.format(Locale.US, "%f", altitude)).endTag(null, ELE_ELEMENT);
            }
            serializer.startTag(null, TIME_ELEMENT).text(gtf.format(time)).endTag(null, TIME_ELEMENT);
            if (name != null) {
                serializer.startTag(null, NAME_ELEMENT).text(name).endTag(null, NAME_ELEMENT);
            }
            if (description != null) {
                serializer.startTag(null, DESCRIPTION_ELEMENT).text(description).endTag(null, DESCRIPTION_ELEMENT);
            }
            if (type != null) {
                serializer.startTag(null, TYPE_ELEMENT).text(type).endTag(null, TYPE_ELEMENT);
            }
            if (symbol != null) {
                serializer.startTag(null, SYM_ELEMENT).text(symbol).endTag(null, SYM_ELEMENT);
            }
            serializer.endTag(null, WPT_ELEMENT);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%f, %f", latitude, longitude);
        }

        @Override
        public boolean isInterrupted() {
            return isNewSegment();
        }

        public String getSymbol() {
            return symbol;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Generate a short description suitable for a menu
         * 
         * @param ctx Android Context
         * @return the description
         */
        public String getShortDescription(@NonNull Context ctx) {
            if (getName() != null) {
                return ctx.getString(R.string.waypoint_description, getName());
            }
            if (getType() != null) {
                return ctx.getString(R.string.waypoint_description, getType());
            }
            return ctx.getString(R.string.waypoint_description, toString());
        }
    }
}
