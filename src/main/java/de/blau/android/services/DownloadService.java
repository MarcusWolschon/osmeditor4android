package de.blau.android.services;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.BuildConfig;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.Notifications;
import de.blau.android.util.PendingIntentCompat;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;
import de.blau.android.validation.Validator;

public class DownloadService extends Service {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, DownloadService.class.getSimpleName().length());
    private static final String DEBUG_TAG = DownloadService.class.getSimpleName().substring(0, TAG_LEN);

    private static final String WAKELOCK_TAG       = BuildConfig.APPLICATION_ID + ":download";
    private static final String BOXES_KEY          = "boxes";
    private static final String DATA_DOWNLOAD_KEY  = "data";
    private static final String TASKS_DOWNLOAD_KEY = "tasks";

    private static final String ACTION_START  = "de.blau.android.START";
    private static final String ACTION_ABORT  = "de.blau.android.ABORT";
    private static final String ACTION_RESUME = "de.blau.android.RESUME";
    private static final String ACTION_PAUSE  = "de.blau.android.PAUSE";

    private final DownloaderBinder mBinder = new DownloaderBinder();

    private WakeLock                  wakeLock = null;
    private Preferences               prefs;
    private PendingIntent             pendingAppIntent;
    private PendingIntent             pendingAbortIntent;
    private PendingIntent             pendingPauseIntent;
    private PendingIntent             pendingResumeIntent;
    private boolean                   started;
    private int                       position = 0;
    private DownloaderTask            downloader;
    private final List<BoundingBox>   boxes    = new ArrayList<>();
    private NotificationManagerCompat notificationManager;
    private Handler                   handler;
    private boolean                   downloadData;
    private boolean                   downloadTasks;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(DEBUG_TAG, "onCreate");

        prefs = App.getPreferences(this);

        // create the intents only once
        Intent appStartIntent = new Intent();
        appStartIntent.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(Main.class.getPackage().getName(), Main.class.getName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingAppIntent = PendingIntent.getActivity(this, 0, appStartIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent abortIntent = new Intent(this, DownloadService.class);
        abortIntent.setAction(ACTION_ABORT);
        pendingAbortIntent = PendingIntentCompat.getForegroundService(this, 0, abortIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, DownloadService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        pendingPauseIntent = PendingIntentCompat.getForegroundService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent resumeIntent = new Intent(this, DownloadService.class);
        resumeIntent.setAction(ACTION_RESUME);
        pendingResumeIntent = PendingIntentCompat.getForegroundService(this, 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        notificationManager = NotificationManagerCompat.from(DownloadService.this);

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        super.onDestroy();
        stop();
        notificationManager.cancel(R.id.notification_download);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(DEBUG_TAG, "Received null intent"); //
            return START_NOT_STICKY; // NOTE not clear how or if we should return an error here
        }
        if (!started) {
            Log.d(DEBUG_TAG, intent.toString());
            started = true;
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
            wakeLock.acquire();
            startInternal();
        }
        switch (intent.getAction()) {
        case ACTION_START: // NOSONAR
            downloadData = intent.getBooleanExtra(DATA_DOWNLOAD_KEY, false);
            downloadTasks = intent.getBooleanExtra(TASKS_DOWNLOAD_KEY, false);
            List<BoundingBox> boxesArg = Util.getSerializableExtra(intent, BOXES_KEY, ArrayList.class);
            if (downloader != null && position != 0) { // still running, add boxes to end
                downloader.pause();
            }
            // there is a lot of potential for race conditions here, this tries to avoid them
            synchronized (boxes) {
                if (position == 0) {
                    boxes.clear();
                }
                boxes.addAll(boxesArg);
            } // falling through
        case ACTION_RESUME:
            if (Util.isEmpty(boxes)) {
                Log.e(DEBUG_TAG, "No bounding boxes");
                return START_STICKY;
            }
            if (downloadData) {
                prefs.setAutoPruneData(false); // always turn off
            }
            if (downloadTasks) {
                prefs.setAutoPruneTasks(false);
            }
            downloader = new DownloaderTask();
            downloader.execute(boxes);
            break;
        case ACTION_ABORT:
            position = 0;
            if (downloaderRunning()) {
                downloader.abort();
                return START_STICKY;
            } // we are paused, change the notification
            if (notificationManager.areNotificationsEnabled()) {
                Notification n = buildNotification(false, 0, 0).build();
                notificationManager.notify(R.id.notification_download, n);
            }
            break;
        case ACTION_PAUSE:
            if (downloaderRunning()) {
                downloader.pause();
            }
            break;
        default:
            Log.e(DEBUG_TAG, "Unknown action " + intent.getAction());
        }
        return START_STICKY;
    }

    /**
     * Check if we have a running downloader task
     * 
     * @return true if a downloader task is running
     */
    public boolean downloaderRunning() {
        return downloader != null && downloader.isExecuting();
    }

    private class DownloaderTask extends ExecutorTask<List<BoundingBox>, Void, Void> {

        private static final int MAP_UPDATE_INTERVAL = 10;
        private static final int UPDATE_DELAY        = 1000;

        private final Logic       logic;
        private final Server      server;
        private final Validator   validator;
        private boolean           pause = false;
        private boolean           abort = false;
        private int               boxCount;
        private Runnable          notificationUpdater;
        private final Set<String> taskFilter;
        private final TaskStorage tasks;
        private final Map         map;

        public DownloaderTask() {
            logic = App.getLogic();
            server = prefs.getServer();
            validator = App.getDefaultValidator(DownloadService.this);
            taskFilter = prefs.taskFilter();
            tasks = App.getTaskStorage();
            map = logic.getMap();
        }

        @Override
        protected Void doInBackground(List<BoundingBox> boxes) throws IOException {
            boxCount = boxes.size();
            Log.d(DEBUG_TAG, "Download started " + boxCount + " data " + downloadData + " tasks " + downloadTasks);
            notificationUpdater = () -> {
                if (notificationManager.areNotificationsEnabled() && !pause && !abort) {
                    Notification n = buildNotification(false, boxCount, position).setOnlyAlertOnce(true).build();
                    notificationManager.notify(R.id.notification_download, n);
                    handler.postDelayed(notificationUpdater, UPDATE_DELAY);
                }
            };
            try {
                handler.postDelayed(notificationUpdater, UPDATE_DELAY);
                for (; position < boxCount; position++) {
                    if (pause) {
                        Log.d(DEBUG_TAG, "Stopping due to pause command");
                        return null;
                    }
                    if (abort) {
                        Log.d(DEBUG_TAG, "Stopping due to abort command");
                        boxCount = 0;
                        return null;
                    }
                    final BoundingBox box = getBox(position, boxes);
                    if (downloadData) {
                        AsyncResult result = logic.download(DownloadService.this, server, box, (OsmElement e) -> e.hasProblem(DownloadService.this, validator),
                                null, true, false);
                        if (result.getCode() != ErrorCodes.OK) {
                            pause = true;
                            Log.d(DEBUG_TAG, "Pausing due to result " + result.getCode());
                            ScreenMessage.toastTopError(DownloadService.this,
                                    getString(R.string.toast_download_failed, TrackerService.errorCodesToStringRes(result.getCode()), result.getMessage()));
                            return null;
                        }
                    }
                    if (downloadTasks) {
                        TransferTasks.downloadBoxSync(DownloadService.this, server, box, true, tasks, taskFilter, TransferTasks.MAX_PER_REQUEST);
                        tasks.addBoundingBox(box);
                    }
                }
                if (position % MAP_UPDATE_INTERVAL == 0 && map != null) {
                    handler.post(map::invalidate);
                }

                // done
                position = 0;
                boxCount = 0;
            } finally {
                handler.removeCallbacks(notificationUpdater);
            }
            return null;
        }

        /**
         * Get the BoundingBox to download from the list
         * 
         * @param position the position the BoundingBox is at
         * @param boxes the list of bounding boxes
         * @return a BoundingBox
         */
        @NonNull
        private BoundingBox getBox(int position, @NonNull List<BoundingBox> boxes) {
            synchronized (boxes) { // NOSONAR
                return boxes.get(position);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            boolean paused = position != 0 && boxCount > 0 && !abort;
            if (notificationManager.areNotificationsEnabled()) {
                Notification n = buildNotification(paused, boxCount, position).build();
                handler.postDelayed(() -> notificationManager.notify(R.id.notification_download, n), UPDATE_DELAY);
            }
            if (boxCount == 0 && !abort) {
                ScreenMessage.toastTopInfo(DownloadService.this, R.string.download_completed_successfully);
            }
            if (paused) {
                ScreenMessage.toastTopInfo(DownloadService.this, R.string.download_paused);
            }
            if (map != null) {
                map.invalidate();
            }
        }

        /**
         * Pause the downloads
         */
        public void pause() {
            pause = true;
        }

        /**
         * Abort the downloads
         */
        public void abort() {
            abort = true;
        }
    }

    /**
     * Create the Intent and start the foreground service
     * 
     * @param key the key indicating which service to start
     * 
     */
    public void start(@NonNull List<BoundingBox> boxes, boolean data, boolean tasks) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(BOXES_KEY, new ArrayList<>(boxes));
        intent.putExtra(DATA_DOWNLOAD_KEY, data);
        intent.putExtra(TASKS_DOWNLOAD_KEY, tasks);
        try {
            ContextCompat.startForegroundService(this, intent);
        } catch (IllegalStateException | SecurityException ex) {
            Log.e(DEBUG_TAG, "Can't start service " + ex.getMessage());
            ScreenMessage.toastTopError(this, getString(R.string.download_service_start_failure, ex.getLocalizedMessage()));
        }
    }

    /**
     * Gets called by the first {@link #onStartCommand(Intent, int, int)}
     * 
     * @return true if service could be started
     */
    private boolean startInternal() {
        if (!Notifications.channelEnabled(this, Notifications.DEFAULT_CHANNEL)) {
            ScreenMessage.toastTopError(DownloadService.this, R.string.toast_default_channel_needs_to_be_enabled);
            return false;
        }
        NotificationCompat.Builder notificationBuilder = buildNotification(false, 0, 0);
        ServiceCompat.startForeground(this, R.id.notification_download, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        return true;
    }

    /**
     * Build a notification
     * 
     * @param paused are we paused
     * @param total total number of downloads
     * @param done completed downloads
     * @return a NotificationCompat.Builder
     */
    @NonNull
    private NotificationCompat.Builder buildNotification(boolean paused, int total, int done) {
        NotificationCompat.Builder notificationBuilder = Notifications.builder(this);

        notificationBuilder.setContentTitle(getString(R.string.download_service_title));
        if (total == 0) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.download_service_idle)));
        } else {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.download_service_downloaded, done, total)));
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_notification_vespucci).setOngoing(true).setContentIntent(pendingAppIntent)
                .setColor(ContextCompat.getColor(this, R.color.osm_green));
        if (total != 0) {
            notificationBuilder.addAction(R.drawable.ic_notification_vespucci, getString(R.string.abort), pendingAbortIntent);
            if (!paused) {
                notificationBuilder.addAction(R.drawable.ic_notification_vespucci, getString(R.string.pause), pendingPauseIntent);
            } else {
                notificationBuilder.addAction(R.drawable.ic_notification_vespucci, getString(R.string.resume), pendingResumeIntent);
            }
        }
        return notificationBuilder;
    }

    /**
     * Stops everything
     */
    public synchronized void stop() {
        Log.d(DEBUG_TAG, "Stopping service");
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(Service.STOP_FOREGROUND_LEGACY); // NOSONAR
        } else {
            stopForeground(true); // NOSONAR
        }
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class DownloaderBinder extends Binder {

        /**
         * Get this instance of the TrackerService
         * 
         * @return this instance of the TrackerService
         */
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

}
