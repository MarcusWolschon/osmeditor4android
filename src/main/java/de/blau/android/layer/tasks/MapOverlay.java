package de.blau.android.layer.tasks;

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
import android.location.Location;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.tasks.Bug;
import de.blau.android.tasks.BugFragment;
import de.blau.android.tasks.MapRouletteFragment;
import de.blau.android.tasks.MapRouletteTask;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.NoteFragment;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.views.IMapView;

public class MapOverlay extends MapViewLayer
        implements ExtentInterface, DiscardInterface, ClickableInterface<Task>, LayerInfoInterface, ConfigureInterface, PruneableInterface {

    private static final String DEBUG_TAG = "tasks";

    public static final String FILENAME = "selectedtask.res";

    private static final int THREAD_POOL_SIZE = 2;

    private static final int SHOW_TASKS_LIMIT = 13;

    private static final int AUTOPRUNE_MIN_INTERVAL       = 10000;
    public static final int  DEFAULT_AUTOPRUNE_TASK_LIMIT = 10000;

    /** Map this is an overlay of. */
    private Map map = null;

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
    private int         autoPruneTaskLimit = DEFAULT_AUTOPRUNE_TASK_LIMIT;                            // task count for
                                                                                                      // autoprune

    private ThreadPoolExecutor mThreadPool;

    private List<Task> taskList = new ArrayList<>();

    private Server server;

    private Context context = null;

    /**
     * Construct a new task layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(@NonNull final Map map) {
        this.map = map;
        context = map.getContext();
        setPrefs(map.getPrefs());
    }

    @Override
    public boolean isReadyToDraw() {
        return true;
    }

    /**
     * Runnable for downloading data
     * 
     * There is some code duplication here, however attempts to merge this didn't work out
     */
    Runnable download = new Runnable() {
        private long lastAutoPrune = 0;

        @Override
        public void run() {
            if (mThreadPool == null) {
                mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            }
            List<BoundingBox> bbList = new ArrayList<>(tasks.getBoundingBoxes());
            ViewBox box = new ViewBox(map.getViewBox());
            box.scale(1.2); // make sides 20% larger
            box.ensureMinumumSize(minDownloadSize); // enforce a minimum size
            List<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, box);
            for (BoundingBox b : bboxes) {
                if (b.getWidth() <= 1 || b.getHeight() <= 1) {
                    Log.w(DEBUG_TAG, "getNextCenter very small bb " + b.toString());
                    continue;
                }
                tasks.addBoundingBox(b);
                try {
                    mThreadPool.execute(() -> {
                        TransferTasks.downloadBoxSync(context, server, b, true, App.getTaskStorage(), filter, TransferTasks.MAX_PER_REQUEST);
                        map.postInvalidate();
                    });
                } catch (RejectedExecutionException rjee) {
                    Log.e(DEBUG_TAG, "Execution rejected " + rjee.getMessage());
                    tasks.deleteBoundingBox(b);
                }
            }
            // check interval first as tasks.count traverses the whole R-Tree
            if ((System.currentTimeMillis() - lastAutoPrune) > AUTOPRUNE_MIN_INTERVAL && tasks.count() > autoPruneTaskLimit) {
                try {
                    mThreadPool.execute(MapOverlay.this::prune);
                    lastAutoPrune = System.currentTimeMillis();
                } catch (RejectedExecutionException rjee) {
                    Log.e(DEBUG_TAG, "Prune execution rejected " + rjee.getMessage());
                }
            }
        }
    };

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {

        int zoomLevel = map.getZoomLevel();

        if (isVisible && zoomLevel >= SHOW_TASKS_LIMIT) {
            ViewBox bb = osmv.getViewBox();
            Location location = map.getLocation();

            if (zoomLevel >= panAndZoomLimit && panAndZoomDownLoad && (location == null || location.getSpeed() < maxDownloadSpeed)) {
                map.getRootView().removeCallbacks(download);
                map.getRootView().postDelayed(download, 100);
            }

            //
            int w = map.getWidth();
            int h = map.getHeight();
            taskList = tasks.getTasks(bb, taskList);
            if (taskList != null) {
                Set<String> taskFilter = map.getPrefs().taskFilter();
                for (Task t : taskList) {
                    // filter
                    if (!taskFilter.contains(t.bugFilterKey())) {
                        continue;
                    }
                    float x = GeoMath.lonE7ToX(w, bb, t.getLon());
                    float y = GeoMath.latE7ToY(h, w, bb, t.getLat());
                    boolean isSelected = selected != null && t.equals(selected) && App.getLogic().isInEditZoomRange();
                    if (isSelected && t instanceof Note && ((Note) t).isNew() && map.getPrefs().largeDragArea()) {
                        // if the task can be dragged and large drag area is turned on show the large drag area
                        c.drawCircle(x, y, DataStyle.getCurrent().getLargDragToleranceRadius(), DataStyle.getInternal(DataStyle.NODE_DRAG_RADIUS).getPaint());
                    }
                    if (t.isClosed() && t.hasBeenChanged()) {
                        t.drawBitmapChangedClosed(map.getContext(), c, x, y, isSelected);
                    } else if (t.isClosed()) {
                        t.drawBitmapClosed(map.getContext(), c, x, y, isSelected);
                    } else if (t.isNew() || t.hasBeenChanged()) {
                        t.drawBitmapChanged(map.getContext(), c, x, y, isSelected);
                    } else {
                        t.drawBitmapOpen(map.getContext(), c, x, y, isSelected);
                    }
                }
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
        final float tolerance = DataStyle.getCurrent().getNodeToleranceValue();
        List<Task> tasksInViewBox = tasks.getTasks(viewBox);
        Set<String> taskFilter = map.getPrefs().taskFilter();
        for (Task t : tasksInViewBox) {
            // filter
            if (!taskFilter.contains(t.bugFilterKey())) {
                continue;
            }
            int lat = t.getLat();
            int lon = t.getLon();
            float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
            float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, lat) - y);
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
        if (t instanceof Note && ((Note) t).isNew() && activity instanceof Main) {
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
            } else if (t instanceof Bug) {
                BugFragment.showDialog(activity, t);
            } else if (t instanceof MapRouletteTask) {
                MapRouletteFragment.showDialog(activity, t);
            }
        }
    }

    @Override
    public String getDescription(Task t) {
        return t.getDescription(map.getContext());
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
                // TODO this doesn't really help with error conditions need to throw exception
                if (!savingHelper.save(context, FILENAME, selected, true)) {
                    // this is essentially catastrophic and can only happen if something went really wrong
                    // running out of memory or disk, or HW failure
                    if (context instanceof Activity) {
                        Snack.barError((Activity) context, R.string.toast_statesave_failed);
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
        server = prefs.getServer();
        maxDownloadSpeed = prefs.getMaxBugDownloadSpeed() / 3.6f;
        panAndZoomLimit = prefs.getPanAndZoomLimit();
        filter = prefs.taskFilter();
        autoPruneTaskLimit = prefs.getAutoPruneTaskLimit();
    }

    @Override
    public LayerType getType() {
        return LayerType.TASKS;
    }

    @Override
    public void discard(Context context) {
        onDestroy();
    }
}
