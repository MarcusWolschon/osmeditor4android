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
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.osm.GeoPoint.InterruptibleGeoPoint;
import de.blau.android.util.SavingHelper;

/**
 * GPS track data class. Only one instance allowed.
 * Automatically saves and loads content.
 * Content saving happens continuously to avoid large delays when closing.
 * A BufferedOutputStream is used to prevent large amounts of flash wear.
 */
public class Track {
	private static final String TAG = "Track";

	private final ArrayList<TrackPoint> track;
	
	private final String SAVEFILE = "track.dat";
	
	private final Context ctx;

	/**
	 * if loadingFinished is true, indicates how many records the save file contains
	 */
	private int savedTrackPoints = 0;
	/**
	 * Set to true as soon as loading is finished. If true, it is guaranteed that either:
	 * a) the save file does not exist, savedTrackPoints is 0 and memory does not contain any significant amount of data
	 * b) the save file does exist, is valid and contains exactly savedTrackPoints records
	 */
	private Boolean loadingFinished = false;
	
	/**
	 * Everything except loading happens on the UI thread.
	 * {@link #close()} may be called on the UI thread to close the track.
	 * After this, the track file must not be touched.
	 * This can be easily achieved using {@link #savingDisabled} for the UI thread.
	 * However, an asynchronous load could still be running.
	 * To prevent closing the file while such a load is running,
	 * the loadingLock is used.
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
	private static volatile boolean isOpen = false;
	
	/** set by {@link #markNewSegment()} - indicates that the next track point will have the isNewSegment flag set */
	private boolean nextIsNewSegment = false;
	
	
	/**
	 * Indicates how many of the track points are already in the save file.
	 */

	public Track(Context context) {
		track = new ArrayList<TrackPoint>();
		ctx = context;
		if (isOpen) {
			markSavingBroken("Attempted to open multiple instances of Track - saving disabled", null);
		} else {
			isOpen = true;
			Log.i(TAG, "Opened track");
			asyncLoad();
		}
	}


	public void reset() {
		deleteSaveFile();
		track.clear();
	}

	
	public void addTrackPoint(final Location location) {
		if (location != null) {
			track.add(new TrackPoint(location, nextIsNewSegment));
			nextIsNewSegment = false;
			save();
		}
	}

	public List<TrackPoint> getTrackPoints() {
		return track;
	}

	@Override
	public String toString() {
		String str = "";
		for (TrackPoint loc : track) {
			str += loc.toString() + '\n';
		}
		return str;
	}
	
	public void save() {
		if (savingDisabled)	{
			Log.e(TAG, "Saving disabled but tried to save");
			return;
		}
		if (!loadingFinished) return;
		if (savedTrackPoints == track.size()) return;
		
		// There are records to be saved
		ensureFileOpen();
		while (savedTrackPoints < track.size()) {
			try {
				track.get(savedTrackPoints).toStream(saveFileStream);
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
		if (savingDisabled)	{
			Log.e(TAG, "Saving disabled but tried to ensureFileOpen");
			return;
		}
		if (saveFileStream != null) return;
		File saveFile = new File(Application.mainActivity.getFilesDir(), SAVEFILE);
		try {
			FileOutputStream fileOutput = null;
			DataOutputStream out = null;
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
	
	private void deleteSaveFile() {
		if (savingDisabled)	{
			Log.e(TAG, "Saving disabled but tried to deleteSaveFile");
			return;
		}
		if (saveFileStream != null) {
			SavingHelper.close(saveFileStream);
			saveFileStream = null;
		}
		savedTrackPoints = 0;
		File saveFile = new File(Application.mainActivity.getFilesDir(), SAVEFILE);
		saveFile.delete();
		if (saveFile.exists()) {
			markSavingBroken("Failed to delete undesired track file", null);
		}
	}

	/**
	 * If something terrible happens, use this to log an error and disable saving
	 * @param message
	 * @param exception
	 */
	private void markSavingBroken(String message, Throwable exception) {
		savingDisabled = true;
		Log.e(TAG, "Saving broken - " + message, exception);
	}

	private void asyncLoad() {
		new AsyncTask<Void, Void, Void>() {
			private ArrayList<TrackPoint> loaded = new ArrayList<Track.TrackPoint>();
						
			@Override
			protected Void doInBackground(Void... params) {
				loadingLock.lock();
				if (!isOpen) return null; // if this has been closed by close() in the meantime, STOP
				
				File saveFile = new File(Application.mainActivity.getFilesDir(), SAVEFILE);
				boolean success = load();
				if (!success || loaded.isEmpty()) {
					Log.i(TAG, "Deleting broken or empty save file");
					deleteSaveFile();
				}
				
				// If the save file exists, it contains exactly the elements in loaded
				if (!loaded.isEmpty() && !saveFile.exists()) {
					// A broken save file was partially recovered. Rewrite it now.
					Log.i(TAG, "Rewriting partially recovered save file");
					rewriteSaveFile(loaded);
				}
				
				savedTrackPoints = loaded.size();
				
				// There are only two possible situations now:
				//  - save file does not exist, savedTrackPoints is 0 and memory does not contain any significant amount of data
				//  - save file does exist, is valid and contains exactly savedTrackPoints records
				
				loadingLock.unlock();
				return null;
			}
			
			protected void onPostExecute(Void result) {
				track.addAll(0, loaded);
				loadingFinished = true;
				// See end of doInBackground for possible states
				Log.i(TAG, "Track loading finished, loaded entries: " + loaded.size());
				if (track.size() > savedTrackPoints) save();
			};

			/**
			 * Loads a track from the file to the "loaded" ArrayList.
			 * @return true if the file was loaded without problems, false if some problem occurred and the file needs to be rewritten
			 */
			private boolean load() {
				FileInputStream fileInput = null;
				DataInputStream in = null;
				try {
					fileInput = Application.mainActivity.openFileInput(SAVEFILE);
					in = new DataInputStream(new BufferedInputStream(fileInput));
					long size = fileInput.getChannel().size();
					// if you manage to record over 32 GB of track data (in RAM) on a mobile device,
					// which means non-stop recording over many many years,
					// you deserve the problem you are going to get when the integer overflows in the next line.
					int records = (int)((size - 4) / TrackPoint.RECORD_SIZE);
			
					loaded.ensureCapacity(records);
					if (in.readInt() != TrackPoint.FORMAT_VERSION) {
						Log.e(TAG, "cannot load track, incompatible data format");
						return false;
					}
					
					for (int i = 0; i < records; i++) {
						loaded.add(TrackPoint.fromStream(in));
					}
					
					if ( (size - 4) % TrackPoint.RECORD_SIZE != 0) {
						Log.e(TAG, "track file contains partial record");
						return false;						
					}
					
					return true;
				} catch (FileNotFoundException e) {
					Log.i(TAG, "No saved track");
					return false;
				} catch (Exception e) {
					Log.e(TAG, "failed to (completely) load track" , e);
					return false;
				} finally {
					SavingHelper.close(in);
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
				}
			}
			
		}.execute();
	}
	
	/**
	 * Saves and closes the track. The object should not be used afterwards, as saving will be disabled.
	 * The save file will never be accessed by this object again, and isOpen will be set to false.
	 * This will allow to open the track again.
	 */
	public void close() {
		if (!isOpen) return;
		Log.d(TAG,"Trying to close track");
		loadingLock.lock();
		save();
		if (saveFileStream != null) {
			SavingHelper.close(saveFileStream);
			saveFileStream = null;
		}
		savingDisabled = true;
		isOpen = false;
		Log.i(TAG,"Track closed");
		loadingLock.unlock();
	}

	/**
	 * Call each time a new segment should be created.
	 */
	public void markNewSegment() {
		nextIsNewSegment = true;
	}
	
	/**
	 * Writes GPX data to the output stream.
	 */
	public void exportToGPX(OutputStream outputStream) throws Exception {
		XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
		serializer.setOutput(outputStream, "utf8");
		serializer.startDocument("utf8", null);
		serializer.startTag("", "gpx");
		serializer.attribute("", "version", "1.0");
		serializer.attribute("", "creator", "Vespucci");
		serializer.startTag("", "trk");
		serializer.startTag("", "trkseg");
		boolean hasPoints = false;
		for (TrackPoint pt : getTrackPoints()) {
			if (hasPoints && pt.isNewSegment()) {
				// start new segment
				serializer.endTag("", "trkseg");
				serializer.startTag("", "trkseg");				
			}
			hasPoints = true;
			pt.toXml(serializer);
		}
		serializer.endTag("", "trkseg");
		serializer.endTag("", "trk");
		serializer.endTag("", "gpx");
		serializer.endDocument();
	}

	/**
	 * This is a class to store location points and provide storing/serialization for them.
	 * Everything considered less relevant is commented out to save space.
	 * If you chose that this should be included in the GPX, uncomment it,
	 * increment {@link #FORMAT_VERSION}, set the correct {@link #RECORD_SIZE}
	 * and rewrite {@link #fromStream(DataInputStream)}, {@link #toStream(DataOutputStream)}
	 * and {@link #getGPXString()}.
	 * 
	 * @author Jan
	 */
	public static class TrackPoint implements InterruptibleGeoPoint {
		
		private static final SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		private static final Calendar calendarInstance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		
		public static final int FORMAT_VERSION = 2;
		public static final int RECORD_SIZE = 1+4*8;
		
		public static final byte FLAG_NEWSEGMENT = 1;
		public final byte flags;
		public final double latitude;
		public final double longitude;
		public final double altitude;
		public final long   time;
		// public final Float  accuracy;
		// public final Float  bearing;
		// public final Float  speed;
	
		
		
		public TrackPoint(Location original, boolean isNewSegment) {
			this.flags = encodeFlags(isNewSegment);
			this.latitude  = original.getLatitude();
			this.longitude = original.getLongitude();
			this.altitude  = original.hasAltitude() ? original.getAltitude() : Double.NaN;
			this.time      = original.getTime();
			// this.accuracy  = original.hasAccuracy() ? original.getAccuracy() : null;
			// this.bearing   = original.hasBearing()  ? original.getBearing() : null;
			// this.speed     = original.hasSpeed()    ? original.getSpeed() : null;
		}
		
		private TrackPoint(byte flags, double latitude, double longitude, double altitude, long time) {
			this.flags = flags;
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
			this.time = time;
		}
		
		/**
		 * Loads a track point from a {@link DataInputStream}
		 * @param stream the stream from which to load
		 * @return the loaded data point
		 * @throws IOException if anything goes wrong
		 */
		public static TrackPoint fromStream(DataInputStream stream) throws IOException {
			return new TrackPoint(
					stream.readByte(),   // flags
					stream.readDouble(), //lat
					stream.readDouble(), //lon
					stream.readDouble(), //alt
					stream.readLong()    // time
				);
		}
		
		/**
		 * Writes the current track point to the data output stream
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
		public int getLat() { return (int) (latitude * 1E7); }
		@Override
		public int getLon() { return (int) (longitude * 1E7); }
		
		public double getLatitude()  { return latitude; }
		public double getLongitude() { return longitude; }
		public long   getTime()        { return time; }
	
		public boolean hasAltitude() { return altitude != Double.NaN; }
		// public boolean hasAccuracy() { return accuracy != null; }
		// public boolean hasBearing()  { return bearing != null; }
		// public boolean hasSpeed()    { return speed != null; }
		
		public double getAltitude() { return altitude != Double.NaN ? altitude : 0d; }
		// public float  getAccuracy() { return accuracy != null ? accuracy : 0f; }
		// public float  getBearing()  { return bearing != null  ? bearing  : 0f; }
		// public float  getSpeed()    { return speed != null    ? speed    : 0f; }
		
		private byte encodeFlags(boolean isNewSegment) {
			byte result = 0;
			if (isNewSegment) result += FLAG_NEWSEGMENT;
			return result;
		}
		
		public boolean isNewSegment() {
			return (flags & FLAG_NEWSEGMENT) > 0;
		}
		
		/**
		 * Adds a GPX trkpt (track point) tag to the given serializer (synchronized due to use of calendarInstance)
		 * @param serializer the xml serializer to use for output
		 * @throws IOException
		 */
		public synchronized void toXml(XmlSerializer serializer) throws IOException {
			serializer.startTag("", "trkpt");
			serializer.attribute("", "lat", String.format(Locale.US, "%f", latitude));
			serializer.attribute("", "lon", String.format(Locale.US, "%f", longitude));
			calendarInstance.setTimeInMillis(time);
			String timestamp = ISO8601FORMAT.format(new Date(time));
			serializer.startTag("", "time").text(timestamp).endTag("", "time");
			if (hasAltitude()) {
				serializer.startTag("", "ele").text(String.format(Locale.US, "%f", altitude)).endTag("", "ele");
			}
			serializer.endTag("", "trkpt");
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
}
