package de.blau.android.layer.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.Downloader;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.NonSerializeableLayer;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.tasks.MapRouletteFragment;
import de.blau.android.tasks.MapRouletteTask;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteFragment;
import de.blau.android.tasks.OsmoseBug;
import de.blau.android.tasks.OsmoseBugFragment;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.tasks.Todo;
import de.blau.android.tasks.TodoFragment;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;
import de.blau.android.views.IMapView;

public class MapOverlay extends NonSerializeableLayer
        implements ExtentInterface, DiscardInterface, ClickableInterface<Task>, LayerInfoInterface, ConfigureInterface, PruneableInterface {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final String FILENAME = "selectedtask" + "." + FileExtensions.RES;

    private static final int DOWNLOAD_THREAD_POOL_SIZE = 2;
    private static final int PRUNE_THREAD_POOL_SIZE    = 1;

    private static final int SHOW_TASKS_LIMIT = 13;

    private static final long AUTOPRUNE_MIN_INTERVAL       = 10000L;
    public static final int   DEFAULT_AUTOPRUNE_TASK_LIMIT = 10000;

    /** Bugs visible on the overlay. */
    private TaskStorage tasks = App.getTaskStorage();

    private ReentrantLock readingLock = new ReentrantLock();

    private SavingHelper<Task> savingHelper = new SavingHelper<>();

    private Task selected             = null;
    private Task restoredSelectedTask = null;

    private boolean     panAndZoomDownLoad = false;
    private int         panAndZoomLimit    = de.blau.android.layer.data.MapOverlay.PAN_AND_ZOOM_LIMIT;
    private int         minDownloadSize    = 50;
    private float       maxDownloadSpeed   = 30;
    private Set<String> filter             = new HashSet<>();

    private boolean autoPruneEnabled     = false;
    private int     autoPruneTaskLimit   = DEFAULT_AUTOPRUNE_TASK_LIMIT;
    private int     autoDownloadBoxLimit = de.blau.android.layer.data.MapOverlay.DEFAULT_DOWNLOADBOX_LIMIT;

    private ThreadPoolExecutor downloadThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(DOWNLOAD_THREAD_POOL_SIZE);
    private ThreadPoolExecutor pruneThreadPool    = (ThreadPoolExecutor) Executors.newFixedThreadPool(PRUNE_THREAD_POOL_SIZE);

    private List<Task>        taskList = new ArrayList<>();
    private final ViewBox     bb       = new ViewBox();

    /**
     * Runnable for downloading data
     */
    private final Downloader download;

    // icon styling
    private float   largDragToleranceRadius;
    private Paint   dragAreaPaint;
    private Paint   iconPaint;
    private boolean largeDragArea;

    /**
     * Construct a new task layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(@NonNull final Map map) {
        this.map = map;
        Context context = map.getContext();
        Preferences prefs = map.getPrefs();
        setPrefs(prefs);
        download = new TaskDownloader(prefs.getServer());
        // the following sets up the static icon caches
        boolean hwAccelerated = map.isHardwareLayerType();
        Note.setupIconCache(context, hwAccelerated);
        OsmoseBug.setupIconCache(context, hwAccelerated);
        MapRouletteTask.setupIconCache(context, hwAccelerated);
        Todo.setupIconCache(context, hwAccelerated);
    }

    @Override
    public boolean isReadyToDraw() {
        return true;
    }

    private class TaskDownloader extends Downloader {
        final Server server;

        /**
         * Construct a new instance
         * 
         * @param server the current Server object
         */
        public TaskDownloader(@NonNull Server server) {
            super(server.getCachedCapabilities().getMaxNoteArea());
            this.server = server;
        }

        @Override
        protected void download() {
            downloadThreadPool.execute(() -> {
                ViewBox box = new ViewBox(map.getViewBox());
                box.scale(1.2); // make sides 20% larger
                box.ensureMinumumSize(minDownloadSize); // enforce a minimum size
                List<BoundingBox> bboxes = BoundingBox.newBoxes(tasks.getBoundingBoxes(), box);
                for (BoundingBox b : bboxes) {
                    tasks.addBoundingBox(b);
                    try {
                        downloadThreadPool.execute(() -> {
                            TransferTasks.downloadBoxSync(map.getContext(), server, b, true, App.getTaskStorage(), filter, TransferTasks.MAX_PER_REQUEST);
                            map.postInvalidate();
                        });
                    } catch (RejectedExecutionException rjee) {
                        Log.e(DEBUG_TAG, "Execution rejected " + rjee.getMessage());
                        tasks.deleteBoundingBox(b);
                    }
                }
            });
            // check interval first as tasks.count traverses the whole R-Tree
            if (autoPruneEnabled && (System.currentTimeMillis() - lastAutoPrune) > AUTOPRUNE_MIN_INTERVAL
                    && tasks.reachedPruneLimits(autoPruneTaskLimit, autoDownloadBoxLimit)) {
                try {
                    pruneThreadPool.execute(MapOverlay.this::prune);
                    lastAutoPrune = System.currentTimeMillis();
                } catch (RejectedExecutionException rjee) {
                    Log.e(DEBUG_TAG, "Prune execution rejected " + rjee.getMessage());
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {
        int zoomLevel = map.getZoomLevel();

        if (!isVisible || zoomLevel < SHOW_TASKS_LIMIT) {
            return;
        }
        bb.set(osmv.getViewBox());
        Location location = map.getLocation();

        if (zoomLevel >= panAndZoomLimit && panAndZoomDownLoad && (location == null || location.getSpeed() < maxDownloadSpeed)) {
            map.removeCallbacks(download);
            download.setBox(bb);
            map.postDelayed(download, 100);
        }

        //
        int w = map.getWidth();
        int h = map.getHeight();

        for (Task t : tasks.getTasks(bb, taskList)) {
            // filter
            if (!filter.contains(t.bugFilterKey())) {
                continue;
            }
            float x = GeoMath.lonE7ToX(w, bb, t.getLon());
            float y = GeoMath.latE7ToY(h, w, bb, t.getLat());
            boolean isSelected = t.equals(selected) && App.getLogic().isInEditZoomRange();

            if (isSelected && t.isNew() && largeDragArea) {
                // if the task can be dragged and large drag area is turned on show the large drag area
                c.drawCircle(x, y, largDragToleranceRadius, dragAreaPaint);
            }
            final boolean closed = t.isClosed();
            if (closed && t.hasBeenChanged()) {
                t.drawBitmapChangedClosed(c, x, y, isSelected, iconPaint);
            } else if (closed) {
                t.drawBitmapClosed(c, x, y, isSelected, iconPaint);
            } else if (t.isNew() || t.hasBeenChanged()) {
                t.drawBitmapChanged(c, x, y, isSelected, iconPaint);
            } else {
                t.drawBitmapOpen(c, x, y, isSelected, iconPaint);
            }
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public List<Task> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<Task> result = new ArrayList<>();
        final float tolerance = map.getDataStyle().getCurrent().getNodeToleranceValue();
        List<Task> tasksInViewBox = tasks.getTasks(viewBox);
        final int width = map.getWidth();
        final int height = map.getHeight();
        for (Task t : tasksInViewBox) {
            // filter
            if (!filter.contains(t.bugFilterKey())) {
                continue;
            }
            int lat = t.getLat();
            int lon = t.getLon();
            float differenceX = Math.abs(GeoMath.lonE7ToX(width, viewBox, lon) - x);
            float differenceY = Math.abs(GeoMath.latE7ToY(height, width, viewBox, lat) - y);
            if ((differenceX <= tolerance) && (differenceY <= tolerance) && (Math.hypot(differenceX, differenceY) <= tolerance)) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_tasks);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        return BoundingBox.union(new ArrayList<>(App.getTaskStorage().getBoundingBoxes()));
    }

    @Override
    public void onSelected(FragmentActivity activity, Task t) {
        if (t.isNew() && activity instanceof Main) {
            if (((Main) activity).getEasyEditManager().editNote((Note) t, this)) {
                selected = t;
            }
        } else {
            if (((Main) activity).getEasyEditManager().inNewNoteSelectedMode()) {
                // Keeping this mode running while showing the dialog for something else is too confusing
                ((Main) activity).getEasyEditManager().finish();
            }
            selected = t;
            if (t instanceof Note) {
                NoteFragment.showDialog(activity, t);
            } else if (t instanceof OsmoseBug) {
                OsmoseBugFragment.showDialog(activity, t);
            } else if (t instanceof Todo) {
                TodoFragment.showDialog(activity, t);
            } else if (t instanceof MapRouletteTask) {
                MapRouletteFragment.showDialog(activity, t);
            }
        }
    }

    @Override
    public SpannableString getDescription(Task t) {
        return getDescription(map.getContext(), t);
    }

    @Override
    public SpannableString getDescription(Context context, Task t) {
        SpannableString d = new SpannableString(t.getDescription(context));
        if (t.isClosed()) {
            d.setSpan(new StrikethroughSpan(), 0, d.length(), 0);
        }
        return d;
    }

    @Override
    public void configure(FragmentActivity activity) {
        ConfigurationDialog.showDialog(activity);
    }

    @Override
    public boolean enableConfiguration() {
        return true;
    }

    @Override
    public void prune() {
        ViewBox pruneBox = new ViewBox(map.getViewBox());
        pruneBox.scale(1.6);
        tasks.prune(pruneBox);
    }

    @Override
    public void showInfo(FragmentActivity activity) {
        LayerInfo f = new TaskLayerInfo();
        f.setShowsDialog(true);
        LayerInfo.showDialog(activity, f);
    }

    /**
     * Stores the current state to the default storage file
     * 
     * @param context Android Context
     * @throws IOException on errors writing the file
     */
    @Override
    public synchronized void onSaveState(@NonNull Context context) throws IOException {
        super.onSaveState(context);
        if (readingLock.tryLock()) {
            try {
                if (!savingHelper.save(context, FILENAME, selected, true)) {
                    Log.e(DEBUG_TAG, "onSaveState unable to save");
                    if (context instanceof Activity) {
                        ScreenMessage.barError((Activity) context, R.string.toast_task_layer_statesave_failed);
                    }
                }
            } finally {
                readingLock.unlock();
            }
        } else {
            Log.i(DEBUG_TAG, "bug state being read, skipping save");
        }
    }

    /**
     * Loads any saved state from the default storage file
     * 
     * 
     * @param context Android context
     * @return true if the saved state was successfully read
     */
    @Override
    public synchronized boolean onRestoreState(@NonNull Context context) {
        super.onRestoreState(context);
        try {
            readingLock.lock();
            restoredSelectedTask = savingHelper.load(context, FILENAME, true);
            Log.d(DEBUG_TAG, "reading saved state " + restoredSelectedTask);
            return restoredSelectedTask != null;
        } finally {
            readingLock.unlock();
        }
    }

    @Override
    public Task getSelected() {
        if (selected == null && restoredSelectedTask != null) {
            Task taskInStorage = App.getTaskStorage().get(restoredSelectedTask);
            selected = taskInStorage;
            restoredSelectedTask = null;
        }
        return selected;
    }

    @Override
    public void deselectObjects() {
        selected = null;
        final Context ctx = map.getContext();
        if (ctx instanceof Main && ((Main) ctx).getEasyEditManager().inNewNoteSelectedMode()) {
            ((Main) ctx).getEasyEditManager().finish();
        }
    }

    @Override
    public void setSelected(Task o) {
        // not used
    }

    @Override
    public void setPrefs(Preferences prefs) {
        panAndZoomDownLoad = prefs.getPanAndZoomAutoDownload();
        minDownloadSize = prefs.getBugDownloadRadius() * 2;
        maxDownloadSpeed = prefs.getMaxBugDownloadSpeed() / 3.6f;
        panAndZoomLimit = prefs.getPanAndZoomLimit();
        filter = prefs.taskFilter();
        autoPruneEnabled = prefs.autoPrune();
        autoPruneTaskLimit = prefs.getAutoPruneTaskLimit();
        autoDownloadBoxLimit = prefs.getAutoPruneBoundingBoxLimit();
        largeDragArea = prefs.largeDragArea();
        DataStyle styles = map.getDataStyle();
        largDragToleranceRadius = styles.getCurrent().getLargDragToleranceRadius();
        dragAreaPaint = styles.getInternal(DataStyle.NODE_DRAG_RADIUS).getPaint();
        iconPaint = styles.getInternal(DataStyle.SELECTED_NODE).getPaint();
    }

    @Override
    public LayerType getType() {
        return LayerType.TASKS;
    }

    @Override
    public void discard(Context context) {
        onDestroy();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Util.shutDownThreadPool(downloadThreadPool);
    }
}
