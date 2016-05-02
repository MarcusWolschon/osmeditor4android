package de.blau.android.services;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SavingHelper.Exportable;

public class TrackerService extends Service implements LocationListener, NmeaListener, Exportable {

	private static final float TRACK_LOCATION_MIN_ACCURACY = 200f;

	private static final String TAG = "TrackerService";

	protected static final int LOCATION_UPDATE = 0;
	protected static final int CONNECTION_FAILED = 1;

	private static final String AUTODOWNLOAD = "autodownload";
	
	private static final String BUGAUTODOWNLOAD = "bugautodownload";

	private static final String TRACK = "track";

	private final TrackerBinder mBinder = new TrackerBinder();
	
	private LocationManager locationManager = null;
	
	private boolean tracking = false;
	
	private boolean downloading = false;
	
	private boolean downloadingBugs = false;

	private Track track;

	private TrackerLocationListener externalListener = null;

	private boolean listenerNeedsGPS = false;

	private Location lastLocation = null;
	private Location previousLocation = null;
	private Location previousBugLocation = null;
	
	private boolean gpsEnabled = false;
	
	private Preferences prefs = null; 
	
	private Location nmeaLocation = new Location("nmea");
	
	private Handler mHandler = null;
	
	private NmeaTcpClient tcpClient = null;
	
	private NmeaTcpClientServer tcpServer = null;
	
	private enum GpsSource {
		INTERNAL,
		NMEA,
		TCP
	}
	private GpsSource source = GpsSource.INTERNAL;

	private ConnectivityManager connectivityManager;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		track = new Track(this);
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		prefs = new Preferences(this); 
	}

	@Override
	public void onDestroy() {
		stopTracking(false);
		track.close();
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			Log.d(TAG,"Received null intent"); // FIXME do something more drastic
			return -1; // FIXME not clear how we should return an error here
		}
		if (intent.getBooleanExtra(TRACK, false)) {
			startTrackingInternal();
		} else if (intent.getBooleanExtra(AUTODOWNLOAD, false)) {
			startAutoDownloadInternal();
		} else if (intent.getBooleanExtra(BUGAUTODOWNLOAD, false)) {
			startBugAutoDownloadInternal();
		} else {
			Log.d(TAG,"Received intent with unknown meaning");
		}
		
		return START_STICKY;
	}
	
	/**
	 * Starts the tracker service (which invokes {@link #onStartCommand(Intent, int, int)},
	 * which invokes {@link #startTrackingInternal()}, which does the actual work.
	 * To start tracking, bind the service, then call this.
	 */
	public void startTracking() {
		Intent intent = new Intent(this, TrackerService.class);
		intent.putExtra(TRACK,true);
		startService(intent);
	}
	
	/**
	 * Starts the tracker service (which invokes {@link #onStartCommand(Intent, int, int)},
	 * which invokes {@link #startTrackingInternal()}, which does the actual work.
	 * To start tracking, bind the service, then call this.
	 */
	public void startAutoDownload() {
		Intent intent = new Intent(this, TrackerService.class);
		intent.putExtra(AUTODOWNLOAD,true);
		startService(intent);
	}
	
	/**
	 * Starts the tracker service (which invokes {@link #onStartCommand(Intent, int, int)},
	 * which invokes {@link #startTrackingInternal()}, which does the actual work.
	 * To start tracking, bind the service, then call this.
	 */
	public void startBugAutoDownload() {
		Intent intent = new Intent(this, TrackerService.class);
		intent.putExtra(BUGAUTODOWNLOAD,true);
		startService(intent);
	}
	
	/**
	 * Actually starts tracking.
	 * Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
	 * See {@link #startTracking()} for the public method to call when tracking should be started.
	 */
	private void startTrackingInternal() {
		if (tracking) {
			track.markNewSegment();
			return;
		}
		startInternal();
		tracking = true;
		track.markNewSegment();
		try {
			Application.mainActivity.triggerMenuInvalidation();
		} catch (Exception e) {
			Log.e(TAG,"startTrackingInternal triggerMenuInvalidation failed " + e);
		} 
		updateGPSState();
	}
	
	/**
	 * Actually starts tracking.
	 * Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
	 * See {@link #startTracking()} for the public method to call when tracking should be started.
	 */
	private void startAutoDownloadInternal() {
		if (downloading) return;
		startInternal();
		downloading = true;
		try {
			Application.mainActivity.triggerMenuInvalidation();
		} catch (Exception e) {
			Log.e(TAG,"startAutoDownloadInternal triggerMenuInvalidation failed " + e);
		} 	
		updateGPSState();
	}
	
	/**
	 * Actually starts tracking.
	 * Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
	 * See {@link #startTracking()} for the public method to call when tracking should be started.
	 */
	private void startBugAutoDownloadInternal() {
		if (downloadingBugs) return;
		startInternal();
		downloadingBugs = true;
		try {
			Application.mainActivity.triggerMenuInvalidation();
		} catch (Exception e) {
			Log.e(TAG,"startBugAutoDownloadInternal triggerMenuInvalidation failed " + e);
		} 
		updateGPSState();
	}
	
	/**
	 * Actually starts tracking.
	 * Gets called by {@link #onStartCommand(Intent, int, int)} when the service is started.
	 * See {@link #startTracking()} for the public method to call when tracking should be started.
	 */
	private void startInternal() {
		if (tracking || downloading || downloadingBugs) return; // all ready running
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
		Resources res = getResources();
		Intent appStartIntent = new Intent();
		appStartIntent
			.setAction(Intent.ACTION_MAIN)
			.addCategory(Intent.CATEGORY_LAUNCHER)
			.setComponent(new ComponentName(Main.class.getPackage().getName(), Main.class.getName()))
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingAppIntent = PendingIntent.getActivity(this, 0, appStartIntent, 0);
		notificationBuilder
			.setContentTitle(res.getString(R.string.tracking_active_title))
			.setContentText(res.getString(R.string.tracking_active_text))
			.setSmallIcon(R.drawable.osm_logo)
			.setOngoing(true)
			.setUsesChronometer(true)
			.setContentIntent(pendingAppIntent);
		startForeground(R.id.notification_tracker, notificationBuilder.build());
	}
	
	/**
	 * Stops tracking
	 * @param deleteTrack true if the track should be deleted, false if it should be kept
	 */
	public void stopTracking(boolean deleteTrack) {
		Log.d(TAG,"Stop tracking");
		if (!tracking) {
			if (deleteTrack) track.reset();
			return;
		}
		if (deleteTrack) {
			track.reset();
		} else {
			track.save();
		}
		tracking = false;
		stop();
	}
	
	/**
	 * Stops autodownloading OSM data
	 */
	public void stopAutoDownload() {
		Log.d(TAG,"Stop auto-download");
		downloading= false;
		stop();
	}
	
	/**
	 * Stops autodownloading bugs
	 */
	public void stopBugAutoDownload() {
		Log.d(TAG,"Stop auto-download");
		downloadingBugs= false;
		stop();
	}
	
	public void stop()
	{
		if (!tracking && !downloading && !downloadingBugs) {
			Log.d(TAG,"Stopping auto-service");
			updateGPSState();
			stopForeground(true);
			stopSelf();
		}
	}
	
	public boolean isTracking() {
		return tracking;
	}

	public List<TrackPoint> getTrackPoints() {
		return track.getTrackPoints();
	}
	
	/**
	 * Exports the GPX data
	 */
	@Override
	public void export(OutputStream outputStream) throws Exception {
		track.exportToGPX(outputStream);
	}
	
	@Override
	public String exportExtension() {
		return "gpx";
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
		
    public class TrackerBinder extends Binder {
        public TrackerService getService() {
            return TrackerService.this;
        }
    }

	@Override
	public void onLocationChanged(Location location) {
		if (source != GpsSource.INTERNAL && location.getProvider().equals("gps")) {
			return; // ignore updates from internal gps
		}
		// Log.d(TAG,"onLocationChanged " + location.getProvider());
		if (!location.hasAccuracy() || location.getAccuracy() <= TRACK_LOCATION_MIN_ACCURACY) {
			if (tracking) {
				track.addTrackPoint(location);
			}
			NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
			if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) { // only attempt to download if we have a network
				if (downloading) {
					autoDownload(location);
				}
				if (downloadingBugs) {
					bugAutoDownload(location);
				}
			}
		}
		if (externalListener != null) externalListener.onLocationChanged(location);
		lastLocation = location;
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "Provider disabled: " + provider);
		gpsEnabled = false;
		try {
			locationManager.removeUpdates(this);
		} catch (SecurityException sex) {
			// can be safely ignored
		}
		locationManager.removeNmeaListener(this);
		if (tcpClient != null) {
			tcpClient.cancel();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	public interface TrackerLocationListener {
		public void onLocationChanged(Location location);
	}

	private void updateGPSState() {
		boolean needed = listenerNeedsGPS || tracking || downloading || downloadingBugs;
		if (needed && !gpsEnabled) {
			Log.d(TAG, "Enabling GPS updates");
			Preferences prefs = new Preferences(this);
			try {
				Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (last != null) {
					onLocationChanged(last);
				}
			} catch (Exception e) {} // Ignore
			try {
				// used to pass updates to UI thread
				mHandler = new Handler(Looper.getMainLooper()) {
	                @Override
	                public void handleMessage(Message inputMessage) {
	                    switch (inputMessage.what) {
	                        case LOCATION_UPDATE:
	                        	Location l = (Location) inputMessage.obj;
	                        	onLocationChanged(l);
	                        	break;
	                        case CONNECTION_FAILED:
	                        	Toast.makeText(Application.mainActivity, (String)inputMessage.obj, Toast.LENGTH_LONG).show();
	                        	break;
	                    }
	                }
				};
				if (prefs.getGpsSource().equals("tcpclient") || prefs.getGpsSource().equals("tcpserver")) {
					source = GpsSource.TCP;
					if (prefs.getGpsSource().equals("tcpclient") && tcpClient == null) {
						tcpClient = new NmeaTcpClient(prefs.getGpsTcpSource());
						Thread t = new Thread(null, tcpClient, "TcpClient");
						t.start();
					} else if (tcpServer == null){
						tcpServer = new NmeaTcpClientServer(prefs.getGpsTcpSource());
						Thread t = new Thread(null, tcpServer, "TcpClientServer");
						t.start();
					}
				}
	
				if (prefs.getGpsSource().equals("nmea") || prefs.getGpsSource().equals("internal")) {
					source = GpsSource.INTERNAL;
					// internal NMEA resource only works if normal updates are turned on
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, prefs.getGpsInterval(),
							prefs.getGpsDistance(), this);
					if (prefs.getGpsSource().equals("nmea")) {
						source = GpsSource.NMEA;
						locationManager.addNmeaListener(this);
					} 
				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to enable GPS", e);
				Toast.makeText(this, R.string.gps_failure, Toast.LENGTH_SHORT).show();				
			}
			gpsEnabled = true;
		} else if (!needed && gpsEnabled) {
			Log.d(TAG, "Disabling GPS updates");
			try {
				locationManager.removeUpdates(this);
			} catch (SecurityException sex) {
				// can be safely ignored
			}
			gpsEnabled = false;
		}
	}
	
	public void setListenerNeedsGPS(boolean listenerNeedsGPS) {
		this.listenerNeedsGPS = listenerNeedsGPS;
		updateGPSState();
		if (listenerNeedsGPS && externalListener != null && lastLocation != null) externalListener.onLocationChanged(lastLocation);
	}

	public void setListener(TrackerLocationListener listener) {
		if (listener == null) setListenerNeedsGPS(false);
		externalListener = listener;
	}

	/**
	 * Read a file in GPX format from device
	 * @param fileName
	 * @param add unused currently
	 * @throws FileNotFoundException 
	 */
	public void importGPXFile(final Uri uri) throws FileNotFoundException {
		
		final InputStream is;
		
		if (uri.getScheme().equals("file")) {
			is = new FileInputStream(new File(uri.getPath()));
		} else {
			ContentResolver cr = getContentResolver();
			is = cr.openInputStream(uri);
		}
		
		final int existingPoints = track.getTrackPoints().size();
	
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Progress.showDialog(Application.mainActivity, Progress.PROGRESS_LOADING);
			}
			
			@Override
			protected Integer doInBackground(Void... arg) {
				int result = 0;
				InputStream in;
				in = new BufferedInputStream(is);
				try {
					track.importFromGPX(in);
				} finally {
					SavingHelper.close(in);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				try {
					Progress.dismissDialog(Application.mainActivity, Progress.PROGRESS_LOADING);
					Toast.makeText(Application.mainActivity.getApplicationContext(), 
							Application.mainActivity.getApplicationContext().getResources().getString(R.string.toast_imported_track_points,track.getTrackPoints().size()-existingPoints), Toast.LENGTH_LONG).show();
					// the following is extremely ugly
					Main.triggerMenuInvalidationStatic();
				} catch (IllegalStateException e) {
					 // Avoid crash if activity is paused
					Log.e(TAG, "onPostExecute", e);
				}
			}
			
		}.execute();
	}

	public Track getTrack() {
		return track;
	}

	@Override
	public void onNmeaReceived(long timestamp, String nmea) {
		processNmeaSentance(nmea);
	}
	
	/**
	 * Try to update our position from NMEA input using some heuristics
	 * @param sentance
	 */
	enum GnssSystem {
		NONE, // pre-fix
		BEIDOU,
		GLONASS,
		GPS,
		MULTIPLE
	}
	GnssSystem system = GnssSystem.NONE;

	/**
	 * Minimal parsing of two NMEA 0183 sentences
	 * TODO add fix type filtering
	 * TODO do something with the hdop value
	 * @param sentence
	 */
	private void processNmeaSentance(String sentence) {
		boolean posUpdate = false;
		if (sentence.length() > 9) { // everything shorter is invalid
			int star = sentence.indexOf('*');
			if (star > 5) {
				String withoutChecksum = sentence.substring(1, star);
				int receivedChecksum = Integer.parseInt(sentence.substring(star+1,star+3),16);
				int checksum = 0;
				for (byte b:withoutChecksum.getBytes()) {
					checksum = checksum ^ b;
				}
				if (receivedChecksum == checksum) {
					String talker = withoutChecksum.substring(0,2);
					String s = withoutChecksum.substring(2,5);
					
					double lat = Double.NaN;
					double lon = Double.NaN;
					double hdop = Double.NaN;
					double height = Double.NaN;
					double course = Double.NaN;
					double speed = Double.NaN;
						
					if (s.equals("GNS")) {
						String[] values = withoutChecksum.split(",",-12); // java magic
						if (values.length==13) {
							try {
								if ((!values[6].toUpperCase(Locale.US).startsWith("NN") || !values[6].toUpperCase(Locale.US).equals("N")) && Integer.parseInt(values[7]) >= 4) { // at least one "good" system needs a fix
									lat = nmeaLatToDecimal(values[2])*(values[3].toUpperCase(Locale.US).equals("N")?1:-1);
									lon = nmeaLonToDecimal(values[4])*(values[5].toUpperCase(Locale.US).equals("E")?1:-1);
									hdop = Double.parseDouble(values[8]);
									height = Double.parseDouble(values[9]);
									posUpdate = true;
								}
							} catch (NumberFormatException e) {
								Log.d("TrackerService","Invalid number format in " + sentence);
								return;
							}
						} else {
							Log.d("TrackerService","Invalid number " + values.length + " of values " + sentence);
							return;
						}
					} else if (s.equals("GGA")) {
						String[] values = withoutChecksum.split(",",-14); // java magic
						if (values.length==15) {
							try {
								if (!values[6].equals("0") && Integer.parseInt(values[7]) >= 4) { // we need a fix
									lat = nmeaLatToDecimal(values[2])*(values[3].toUpperCase(Locale.US).equals("N")?1:-1);
									lon = nmeaLonToDecimal(values[4])*(values[5].toUpperCase(Locale.US).equals("E")?1:-1);
									hdop = Double.parseDouble(values[8]);
									height = Double.parseDouble(values[9]);
									posUpdate = true;
								}
							} catch (NumberFormatException e) {
								Log.d("TrackerService","Invalid number format in " + sentence);
								return;
							}
						} else {
							Log.d("TrackerService","Invalid number " + values.length + " of values " + sentence);
							return;
						}
					} else if (s.equals("VTG")) {
						String[] values = withoutChecksum.split(",",-11); // java magic
						if (values.length==12) {
							try {
								if (!values[9].toUpperCase(Locale.US).startsWith("N")) {
									course = Double.parseDouble(values[1]);
									nmeaLocation.setBearing((float)course);
									speed = Double.parseDouble(values[7]);
									nmeaLocation.setSpeed((float)(speed/3.6D));
								}
							} catch (NumberFormatException e) {
								Log.d("TrackerService","Invalid number format in " + sentence);
								return;
							}
						} else {
							Log.d("TrackerService","Invalid number " + values.length + " of values " + sentence);
							return;
						}
					} else { 
						// unsupported sentence
						return;
					}
					// Log.d("TrackerService","Got from NMEA " + lat + " " + lon + " " + hdop + " " + height);
					// the following assumes that the behaviour of the GPS receiver will not change
					// and assume multiple systems are better than GPS which in turn is better the GLONASS ... this naturally may not be really true
					if (system==GnssSystem.NONE) { // take whatever we get
						if (talker.equals("GP")) {
							system=GnssSystem.GPS;
						} else if (talker.equals("GL")) {
							system=GnssSystem.GLONASS;
						} else if (talker.equals("GN")) {
							system=GnssSystem.MULTIPLE;
						} else {
							// new system we don't know about? BEIDOU probably best ignored for now
							return;
						}
					} else if (system==GnssSystem.GLONASS) {
						if (talker.equals("GP")) {
							system=GnssSystem.GPS;
						} else if (talker.equals("GN")) {
							system=GnssSystem.MULTIPLE;
						}
					}else if (system==GnssSystem.GPS) {
						if (talker.equals("GL")) {
							// ignore
							return;
						} else if (talker.equals("GN")) {
							system=GnssSystem.MULTIPLE;
						}
					} else if (system==GnssSystem.MULTIPLE) {
						if (!talker.equals("GN")) {
							return;
						}
					}

					if (posUpdate) { // we could do filtering etc here
						nmeaLocation.setAltitude(height);
						nmeaLocation.setLatitude(lat);
						nmeaLocation.setLongitude(lon);
						// can't call something on the UI thread directly need to send a message
						Message newLocation = mHandler.obtainMessage(LOCATION_UPDATE, nmeaLocation);
						// Log.d("TrackerService","Update " + l);
						newLocation.sendToTarget();
						if (tracking) {
							track.addTrackPoint(nmeaLocation);
						}
						NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
						if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) { // only attempt to download if we have a network
							if (downloading) {
								autoDownload(new Location(nmeaLocation));
							}
							if (downloadingBugs) {
								bugAutoDownload(new Location(nmeaLocation));
							}
						}
						lastLocation = nmeaLocation;
					}
					return;
				}
			}
		}
		Log.d("TrackerService","Checksum failed on " + sentence);
	}

	/**
	 * Convert from NMEA format to decimal (there is already a method in Location so this is not really necessary)
	 * @param nmea
	 * @return
	 * @throws NumberFormatException
	 */
	Double nmeaLonToDecimal(String nmea) throws NumberFormatException {
		int deg = Integer.parseInt(nmea.substring(0,3));
		Double min = Double.parseDouble(nmea.substring(3));
		return deg+min/60d;
	}
	
	/**
	 * Convert from NMEA format to decimal (there is already a method in Location so this is not really necessary)
	 * @param nmea
	 * @return
	 * @throws NumberFormatException
	 */
	Double nmeaLatToDecimal(String nmea) throws NumberFormatException {
		int deg = Integer.parseInt(nmea.substring(0,2));
		Double min = Double.parseDouble(nmea.substring(2));
		return deg+min/60d;
	}
	
	public class NmeaTcpClient implements Runnable {

		String host;
		int port;
		boolean canceled = false;
		
		NmeaTcpClient(String hostAndPort) {
			int doubleColon = hostAndPort.indexOf(':');
			if (doubleColon > 0) {
				host = hostAndPort.substring(0,doubleColon);
				port = Integer.parseInt(hostAndPort.substring(doubleColon+1));
			} // otherwise crash and burn?
		}

		public void cancel() {
			canceled = true;
		}

		@Override
		public void run() {
			Socket socket = null;
			DataOutputStream dos = null;
			BufferedReader input = null;
			try {
				Log.d("TrackerService", "Connecting to " + host+":"+port + " ...");

				socket = new Socket(host, port);
				dos = new DataOutputStream(socket.getOutputStream());

				InputStreamReader isr = new InputStreamReader(socket.getInputStream());
				input = new BufferedReader(isr);

				while (!canceled) {
					processNmeaSentance(input.readLine());
				}

			} catch (Exception e) {
				Log.e("TrackerService", "failed to open/read " + host+":"+port, e);
				Message failed = mHandler.obtainMessage(CONNECTION_FAILED, e.getMessage());
				failed.sendToTarget();
			} catch (Error e) {
				Message failed = mHandler.obtainMessage(CONNECTION_FAILED, e.getMessage());
				failed.sendToTarget();
			} finally {
				SavingHelper.close(dos);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					SavingHelper.close(socket);
				}
				SavingHelper.close(input);
			}
		}
	}
	
	/** 
	 * The current android RTKLIB port doesn't support TCP servers. just clients ... so we run one
	 * @author simon
	 *
	 */
	public class NmeaTcpClientServer implements Runnable {
		boolean canceled = false;
		int port = 1959;
		
		NmeaTcpClientServer(String hostAndPort) {
			// only use the port string
			int doubleColon = hostAndPort.indexOf(':');
			if (doubleColon > 0) {
				port = Integer.parseInt(hostAndPort.substring(doubleColon+1));
			} 
		}
		
		public void cancel() {
			canceled = true;
		}

		@Override
		public void run() {
			DataOutputStream dos = null;
			BufferedReader input = null;
			ServerSocket serverSocket = null;
			try {
				serverSocket = new ServerSocket(port);
				while (!canceled) {
					Socket socket = serverSocket.accept();
					Log.d("TrackerService", "Incoming connection from " + socket.getRemoteSocketAddress().toString() + " ...");
					dos = new DataOutputStream(socket.getOutputStream());

					InputStreamReader isr = new InputStreamReader(socket.getInputStream());
					input = new BufferedReader(isr);
					try {
						while (!canceled) {
							processNmeaSentance(input.readLine());
						}
					} catch (IOException ioex ) {
						// happens if client closes the socket
						SavingHelper.close(dos);
						SavingHelper.close(input);
					}
				}
			} catch (Exception e) {
				Message failed = mHandler.obtainMessage(CONNECTION_FAILED, e.getMessage());
				failed.sendToTarget();
			} catch (Error e) {
				Message failed = mHandler.obtainMessage(CONNECTION_FAILED, e.getMessage());
				failed.sendToTarget();
			} finally {
				SavingHelper.close(dos);
				SavingHelper.close(input);
				if (serverSocket != null) {
					try {
						serverSocket.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}
	}

	private void autoDownload(Location location) {
		// some heuristics for now to keep downloading to a minimum
		// speed needs to be <= 6km/h (aka brisk walking speed) 
		int radius = prefs.getDownloadRadius();
		if ((location.getSpeed() < prefs.getMaxDownloadSpeed()/3.6f) && (previousLocation==null || location.distanceTo(previousLocation) > radius/8)) {
			StorageDelegator storageDelegator = Application.getDelegator();
			ArrayList<BoundingBox> bbList = new ArrayList<BoundingBox>(storageDelegator.getBoundingBoxes());
			BoundingBox newBox = getNextBox(bbList,previousLocation, location,radius);
			if (newBox != null) {
				if (radius != 0) { // download
					ArrayList<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, newBox); 
					for (BoundingBox b:bboxes) {
						if (b.getWidth() <= 1 || b.getHeight() <= 1) {
							// ignore super small bb likely due to rounding errors
							Log.d(TAG,"getNextCenter very small bb " + b.toString());
							continue;
						}
						storageDelegator.addBoundingBox(b);  // will be filled once download is complete
						Log.d(TAG,"getNextCenter loading " + b.toString());
						Application.getLogic().autoDownloadBox(this,prefs.getServer(), b); 
					}
				}
				previousLocation  = location;
			}
		}
	}
	
	boolean bbLoaded(ArrayList<BoundingBox> bbs, int lonE7, int latE7) {		
		for (BoundingBox b:bbs) {
			if (b.isIn(latE7, lonE7)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return a suitable next BB, simply creates a raster of the download radius size
	 * @param location
	 * @return
	 */
	private BoundingBox getNextBox(ArrayList<BoundingBox> bbs,Location prevLocation, Location location, int radius) {
	
		
		double lon = location.getLongitude();
		double lat = location.getLatitude();
		double mlat = GeoMath.latToMercator(lat);
		double width = 2*GeoMath.convertMetersToGeoDistance(radius);
		
		int currentLeftE7 = (int) (Math.floor(lon / width)*width * 1E7);
		double currentMBottom = Math.floor(mlat/ width)*width;
		int currentBottomE7 = (int) (GeoMath.mercatorToLat(currentMBottom) * 1E7);
		int widthE7 = (int) (width*1E7);
		
		try {
			BoundingBox b = new BoundingBox(currentLeftE7, currentBottomE7, currentLeftE7 + widthE7, currentBottomE7 + widthE7);

			if (!bbLoaded(bbs, (int)(lon*1E7D), (int)(lat*1E7D))) {
				return b;
			}

			double bRight = b.getRight()/1E7d;
			double bLeft = b.getLeft()/1E7d;
			double mBottom = GeoMath.latE7ToMercator(b.getBottom());
			double mHeight = GeoMath.latE7ToMercator(b.getTop()) - mBottom;
			double dLeft = lon - bLeft;
			double dRight = bRight - lon;
			
			double dTop = mBottom + mHeight - mlat;
			double dBottom = mlat - mBottom;
			
			Log.d(TAG,"getNextCenter dLeft " + dLeft + " dRight " + dRight + " dTop " + dTop + " dBottom " + dBottom);
			Log.d(TAG,"getNextCenter " + b.toString());


			// top or bottom is closest
			if (dTop < dBottom) { // top closest
				if (dLeft < dRight) {
					return new BoundingBox(b.getLeft()-widthE7, b.getBottom(), b.getRight(), b.getTop() + widthE7);
				} else {
					return new BoundingBox(b.getLeft(), b.getBottom(), b.getRight() + widthE7, b.getTop() + widthE7);
				}	
			} else {
				if (dLeft < dRight) {
					return new BoundingBox(b.getLeft()-widthE7, b.getBottom()-widthE7, b.getRight(), b.getTop() );
				} else {
					return new BoundingBox(b.getLeft(), b.getBottom()-widthE7, b.getRight() + widthE7, b.getTop());
				}	
			}
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			return null;
		}	
	}
	
	private void bugAutoDownload(Location location) {
		// some heuristics for now to keep downloading to a minimum
		// speed needs to be <= 6km/h (aka brisk walking speed) 
		int radius = prefs.getBugDownloadRadius();
		if ((location.getSpeed() < prefs.getMaxBugDownloadSpeed()/3.6f) && (previousBugLocation==null || location.distanceTo(previousBugLocation) > radius/8)) {
			ArrayList<BoundingBox> bbList = new ArrayList<BoundingBox>(Application.getTaskStorage().getBoundingBoxes());
			BoundingBox newBox = getNextBox(bbList,previousBugLocation, location, radius);
			if (newBox != null) {
				if (radius != 0) { // download
					ArrayList<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, newBox); 
					for (BoundingBox b:bboxes) {
						if (b.getWidth() <= 1 || b.getHeight() <= 1) {
							// ignore super small bb likely due to rounding errors
							Log.d(TAG,"bugAutoDownload very small bb " + b.toString());
							continue;
						}
						Application.getTaskStorage().add(b);  // will be filled once download is complete
						Log.d(TAG,"bugAutoDownloads loading " + b.toString());
						TransferTasks.downloadBox(this,prefs.getServer(), b, true, null);
					}
				}
				previousBugLocation  = location;
			} else {
				Log.d(TAG,"bugAutoDownload no bb");
			}
			
		}
	}
	
	public Location getLastLocation() {
		return lastLocation;
	}
}
