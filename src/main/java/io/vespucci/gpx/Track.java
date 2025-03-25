package io.vespucci.gpx;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.contract.FileExtensions;
import io.vespucci.gpx.WayPoint.Link;
import io.vespucci.osm.OsmXml;
import io.vespucci.util.ExecutorTask;
import io.vespucci.util.SavingHelper;
import io.vespucci.util.SavingHelper.Exportable;

/**
 * GPS track data class. Only one instance allowed. Automatically saves and loads content. Content saving happens
 * continuously to avoid large delays when closing. A BufferedOutputStream is used to prevent large amounts of flash
 * wear.
 */
public class Track extends DefaultHandler implements GpxTimeFormater, Exportable {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Track.class.getSimpleName().length());
    private static final String DEBUG_TAG = Track.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TRKSEG_ELEMENT = "trkseg";
    private static final String TRK_ELEMENT    = "trk";
    private static final String GPX_ELEMENT    = "gpx";

    private final List<TrackPoint> currentTrack;

    private final List<WayPoint> currentWayPoints;

    private static final String SAVEFILE = "track.dat";

    private static final String WAYPOINT_SAVEFILE = "waypoints.dat";

    private final Context ctx;

    private SavingHelper<ArrayList<WayPoint>> wayPointsSaver = new SavingHelper<>();

    /**
     * For conversion from UNIX epoch time and back
     */
    public static final String     DATE_PATTERN_ISO8601_UTC = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private final SimpleDateFormat iso8601Format;
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
    private boolean loadingFinished = false;

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
     * Ensure only one instance may be saving at a time
     */
    private static volatile boolean isSaving   = false;
    private static Object           savingLock = new Object();

    /** set by {@link #markNewSegment()} - indicates that the next track point will have the isNewSegment flag set */
    private boolean nextIsNewSegment = false;

    /**
     * Basic constructor
     * 
     * @param context Android Context (required for recording)
     * @param recording if true the instance will be used for recording
     */
    public Track(@Nullable Context context, boolean recording) {
        // Hardcode 'Z' timezone marker as otherwise '+0000' will be used, which is invalid in GPX
        iso8601Format = new SimpleDateFormat(DATE_PATTERN_ISO8601_UTC, Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));

        currentTrack = recording ? Collections.synchronizedList(new ArrayList<>()) : new ArrayList<>();
        currentWayPoints = recording ? Collections.synchronizedList(new ArrayList<>()) : new ArrayList<>();
        ctx = context;
        synchronized (savingLock) {
            if (isSaving && recording) {
                markSavingBroken("Attempted to open multiple instances of Track - saving disabled for this instance", null);
            } else if (recording) {
                isSaving = true;
                Log.i(DEBUG_TAG, "Opened track");
                asyncLoad();
            } else {
                savingDisabled = true;
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
     * @return an array of TrackPoint
     */
    @NonNull
    public List<TrackPoint> getTrackPoints() {
        return currentTrack;
    }

    /**
     * Get the WayPoints for this track
     * 
     * @return a List of WayPoint
     */
    @NonNull
    public List<WayPoint> getWayPoints() {
        return currentWayPoints;
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
            wayPointsSaver.save(ctx, WAYPOINT_SAVEFILE, new ArrayList<>(currentWayPoints), true);
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
        Log.i(DEBUG_TAG, "track saved " + savedTrackPoints + " points");
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
                fileOutput = ctx.openFileOutput(SAVEFILE, Context.MODE_APPEND); // NOSONAR closed in close
                out = new DataOutputStream(new BufferedOutputStream(fileOutput)); // NOSONAR closed in close
            } else {
                // no save file, create one
                fileOutput = ctx.openFileOutput(SAVEFILE, Context.MODE_PRIVATE); // NOSONAR closed in close
                out = new DataOutputStream(new BufferedOutputStream(fileOutput)); // NOSONAR closed in close
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
        if (!saveFile.delete() && saveFile.exists()) { // NOSONAR nio delete requires API 26
            markSavingBroken("Failed to delete undesired track file", null);
        }

        saveFile = new File(ctx.getFilesDir(), WAYPOINT_SAVEFILE);
        if (!saveFile.delete() && saveFile.exists()) { // NOSONAR nio delete requires API 26
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
        // Logic instance might not be available here
        new ExecutorTask<Void, Void, Void>() {
            private ArrayList<TrackPoint> loaded = new ArrayList<>();

            @Override
            protected Void doInBackground(Void param) {
                loadingLock.lock();
                try {
                    if (!isSaving) {
                        return null; // if this has been closed by close() in the meantime, STOP
                    }

                    List<WayPoint> loadedWayPoints = wayPointsSaver.load(ctx, WAYPOINT_SAVEFILE, true);
                    if (loadedWayPoints != null) {
                        currentWayPoints.clear();
                        currentWayPoints.addAll(loadedWayPoints);
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
                Log.i(DEBUG_TAG, "asyncLoad track loading finished, loaded entries: " + loaded.size());
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
                try (FileInputStream fileInput = ctx.openFileInput(SAVEFILE); DataInputStream in = new DataInputStream(new BufferedInputStream(fileInput));) {
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
                }
            }

            /**
             * Saves the given data to disk, overwriting anything already saved
             */
            private void rewriteSaveFile(Iterable<TrackPoint> data) {
                try (FileOutputStream fileOutput = ctx.openFileOutput(SAVEFILE, Context.MODE_PRIVATE);
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fileOutput));) {
                    out.writeInt(TrackPoint.FORMAT_VERSION);
                    for (TrackPoint point : data) {
                        point.toStream(out);
                    }
                } catch (Exception e) {
                    markSavingBroken("Failed to rewrite broken save file", e);
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
        if (!isSaving) {
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
            synchronized (savingLock) {
                isSaving = false;
            }
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
     * @throws IOException if writing to the stream fails
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public void exportToGPX(@NonNull OutputStream outputStream) throws XmlPullParserException, IOException {
        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        serializer.setOutput(outputStream, OsmXml.UTF_8);
        serializer.startDocument(OsmXml.UTF_8, null);
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null, GPX_ELEMENT);
        serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/0");
        serializer.attribute(null, "xsi:schemaLocation", "http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd");
        serializer.attribute(null, "version", "1.0");
        serializer.attribute(null, "creator", "Vespucci");
        for (WayPoint wt : getWayPoints()) {
            wt.toXml(serializer, this);
        }
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

    /**
     * Format a time as an ISO8601 string
     * 
     * @param time the time as ms since the epoch
     * @return the ISO8601 formated value
     */
    public String format(long time) {
        calendarInstance.setTimeInMillis(time);
        return iso8601Format.format(new Date(time));
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
            Log.e(DEBUG_TAG, "importFromGPX failed " + e);
        }
    }

    /**
     * start parsing a GPX file
     * 
     * @param in InputStream that we are reading from
     * @throws SAXException on parsing exceptions
     * @throws IOException if reading the InputStream caused errors
     * @throws ParserConfigurationException
     */
    private void start(final InputStream in) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        factory.setNamespaceAware(true);
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(in, this);
    }

    /**
     * minimalistic GPX file parser
     */
    private boolean    newSegment        = false;
    private double     parsedLat;
    private double     parsedLon;
    private double     parsedEle         = Double.NaN;
    private long       parsedTime        = 0L;
    private String     parsedName        = null;
    private String     parsedDescription = null;
    private String     parsedType        = null;
    private String     parsedSymbol      = null;
    private Link       parsedLink        = null;
    private List<Link> parsedLinks       = new ArrayList<>();

    private enum State {
        NONE, TIME, ELE, NAME, DESC, TYPE, SYM, LINK, TRACK, WAYPOINT, WAYPOINT_LINK, LINK_TEXT
    }

    private State state = State.NONE;

    @Override
    public void startElement(final String uri, final String element, final String qName, final Attributes atts) {
        try {
            switch (element) {
            case GPX_ELEMENT:
                state = State.NONE;
                Log.d(DEBUG_TAG, "parsing gpx");
                break;
            case TRK_ELEMENT:
                Log.d(DEBUG_TAG, "parsing trk");
                state = State.TRACK;
                break;
            case TRKSEG_ELEMENT:
                Log.d(DEBUG_TAG, "parsing trkseg");
                newSegment = true;
                break;
            case TrackPoint.TRKPT_ELEMENT:
            case WayPoint.WPT_ELEMENT:
                parsedLat = Double.parseDouble(atts.getValue(TrackPoint.LAT_ATTR));
                parsedLon = Double.parseDouble(atts.getValue(TrackPoint.LON_ATTR));
                if (WayPoint.WPT_ELEMENT.equals(element)) {
                    state = State.WAYPOINT;
                    parsedLinks.clear();
                }
                break;
            case TrackPoint.TIME_ELEMENT:
                state = State.TIME;
                break;
            case TrackPoint.ELE_ELEMENT:
                state = State.ELE;
                break;
            case WayPoint.NAME_ELEMENT:
                state = State.NAME;
                break;
            case WayPoint.DESC_ELEMENT:
                state = State.DESC;
                break;
            case WayPoint.TYPE_ELEMENT:
                state = State.TYPE;
                break;
            case WayPoint.SYM_ELEMENT:
                state = State.SYM;
                break;
            case WayPoint.Link.TEXT_ELEMENT:
                if (state != State.WAYPOINT_LINK) {
                    break;
                }
                state = State.LINK_TEXT;
                break;
            case WayPoint.LINK_ELEMENT:
                if (state != State.WAYPOINT) {
                    break;
                }
                state = State.WAYPOINT_LINK;
                parsedLink = new WayPoint.Link();
                parsedLink.setUrl(atts.getValue(WayPoint.Link.HREF_ATTR));
                break;
            default:
            }
        } catch (Exception e) {
            Log.e("Profil", "Parse Exception", e);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        final String string = new String(ch, start, length);
        switch (state) {
        case NONE:
            return;
        case ELE:
            parsedEle = Double.parseDouble(string);
            return;
        case TIME:
            try {
                parsedTime = parseTime(string);
            } catch (ParseException e) {
                parsedTime = 0L;
            }
            return;
        case NAME:
            parsedName = string;
            return;
        case DESC:
            parsedDescription = string;
            return;
        case TYPE:
            parsedType = string;
            return;
        case LINK_TEXT:
            if (parsedLink != null) {
                parsedLink.setDescription(string);
            }
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
        return iso8601Format.parse(t).getTime();
    }

    @Override
    public void endElement(final String uri, final String element, final String qName) {
        switch (element) {
        case GPX_ELEMENT:
            break;
        case TRK_ELEMENT:
            state = State.NONE;
            break;
        case TRKSEG_ELEMENT:
            break;
        case TrackPoint.TRKPT_ELEMENT:
            currentTrack.add(new TrackPoint(newSegment ? TrackPoint.FLAG_NEWSEGMENT : 0, parsedLat, parsedLon, parsedEle, parsedTime));
            newSegment = false;
            parsedEle = Double.NaN;
            parsedTime = 0L;
            state = State.TRACK;
            break;
        case WayPoint.WPT_ELEMENT:
            WayPoint wpt = new WayPoint(parsedLat, parsedLon, parsedEle, parsedTime);
            wpt.setName(parsedName);
            wpt.setDescription(parsedDescription);
            wpt.setType(parsedType);
            wpt.setSymbol(parsedSymbol);
            if (!parsedLinks.isEmpty()) {
                wpt.setLinks(new ArrayList<>(parsedLinks));
            }
            currentWayPoints.add(wpt);

            parsedEle = Double.NaN;
            parsedTime = 0L;
            parsedName = null;
            parsedDescription = null;
            parsedType = null;
            parsedSymbol = null;
            parsedLinks.clear();
            state = State.NONE;
            break;
        case WayPoint.Link.TEXT_ELEMENT:
            state = State.WAYPOINT_LINK;
            break;
        case WayPoint.LINK_ELEMENT:
            parsedLinks.add(parsedLink);
            state = State.WAYPOINT;
            break;
        case TrackPoint.TIME_ELEMENT:
        case TrackPoint.ELE_ELEMENT:
        case WayPoint.NAME_ELEMENT:
        case WayPoint.DESC_ELEMENT:
        case WayPoint.TYPE_ELEMENT:
        case WayPoint.SYM_ELEMENT:
            state = State.WAYPOINT;
            break;
        default:
            state = State.NONE;
        }
    }

    /**
     * Get the starting TrackPoint
     * 
     * @return the first TrackPoint or null
     */
    @Nullable
    public TrackPoint getFirstTrackPoint() {
        if (!currentTrack.isEmpty()) {
            return currentTrack.get(0);
        }
        return null;
    }

    /**
     * Get the first WayPoint
     * 
     * @return the first WayPoint or null
     */
    @Nullable
    public WayPoint getFirstWayPoint() {
        if (!currentWayPoints.isEmpty()) {
            return currentWayPoints.get(0);
        }
        return null;
    }

    /**
     * Exports the GPX data
     */
    @Override
    public void export(OutputStream outputStream) throws Exception {
        exportToGPX(outputStream);
    }

    @Override
    public String exportExtension() {
        return FileExtensions.GPX;
    }
}
