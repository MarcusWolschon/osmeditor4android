package de.blau.android.layer.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DisableInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.views.IMapView;

public class MapOverlay extends MapViewLayer implements ExtentInterface, DisableInterface, ClickableInterface<Task>, ConfigureInterface, PruneableInterface {

    private static final String DEBUG_TAG = "tasks";

    public static final String FILENAME = "selectedtask.res";

    private static final int SHOW_TASKS_LIMIT = 13;

    private boolean enabled = false;

    /** Map this is an overlay of. */
    private final Map map;

    /** Bugs visible on the overlay. */
    private TaskStorage tasks = App.getTaskStorage();

    private transient ReentrantLock readingLock = new ReentrantLock();

    private transient SavingHelper<Task> savingHelper = new SavingHelper<>();

    private Task selected = null;

    private boolean panAndZoomDownLoad = false;
    private int     panAndZoomLimit    = 16;

    private int minDownloadSize = 50;

    private float maxDownloadSpeed = 30;

    private ThreadPoolExecutor mThreadPool;

    private Server server;

    private final Context context;

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
        enabled = map.getPrefs().areBugsEnabled();
        return enabled;
    }

    /**
     * Runnable for downloading data
     * 
     * There is some code duplication here, however attempts to merge this didn't work out
     */
    Runnable download = new Runnable() {

        @Override
        public void run() {
            if (mThreadPool == null) {
                mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
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
                mThreadPool.execute(new Runnable() {

                    @Override
                    public void run() {
                        TransferTasks.downloadBox(context, server, b, true, new PostAsyncActionHandler() {

                            @Override
                            public void onSuccess() {
                                map.postInvalidate();
                            }

                            @Override
                            public void onError() {
                                // do nothing
                            }

                        });
                    }
                });
            }
        }
    };

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {

        int zoomLevel = map.getZoomLevel();

        if (isVisible && enabled && zoomLevel >= SHOW_TASKS_LIMIT) {

            ViewBox bb = osmv.getViewBox();

            Location location = map.getLocation();

            if (zoomLevel >= panAndZoomLimit && panAndZoomDownLoad && (location == null || location.getSpeed() < maxDownloadSpeed)) {
                map.getRootView().removeCallbacks(download);
                map.getRootView().postDelayed(download, 100);
            }

            //
            int w = map.getWidth();
            int h = map.getHeight();
            List<Task> taskList = tasks.getTasks(bb);
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
                    if (isSelected) {
                        // if the task can be dragged and large drag area is turned on show the large drag area
                        if (t instanceof Note && ((Note) t).isNew() && map.getPrefs().largeDragArea()) {
                            c.drawCircle(x, y, DataStyle.getCurrent().getLargDragToleranceRadius(),
                                    DataStyle.getInternal(DataStyle.NODE_DRAG_RADIUS).getPaint());
                        }
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
        if (map.getPrefs().areBugsEnabled()) {
            final float tolerance = DataStyle.getCurrent().getNodeToleranceValue();
            List<Task> taskList = tasks.getTasks(viewBox);
            if (taskList != null) {
                Set<String> taskFilter = map.getPrefs().taskFilter();
                for (Task t : taskList) {
                    // filter
                    if (!taskFilter.contains(t.bugFilterKey())) {
                        continue;
                    }
                    int lat = t.getLat();
                    int lon = t.getLon();
                    float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
                    float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, lat) - y);
                    if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
                        if (Math.hypot(differenceX, differenceY) <= tolerance) {
                            result.add(t);
                        }
                    }
                }
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
        List<BoundingBox> boxes = App.getTaskStorage().getBoundingBoxes();
        if (boxes != null) {
            return BoundingBox.union(new ArrayList<>(boxes));
        }
        return null;
    }

    @Override
    public void disable(Context context) {
        Preferences prefs = new Preferences(context);
        prefs.setBugsEnabled(false);
    }

    @Override
    public void onSelected(FragmentActivity activity, Task t) {
        selected = t;
        if (t instanceof Note && ((Note) t).isNew() && activity instanceof Main) {
            ((Main) activity).getEasyEditManager().editNote((Note) t, this);
        } else {
            if (((Main) activity).getEasyEditManager().inNewNoteSelectedMode()) {
                // Keeping this mode running while showing the dialog for something else is too confusing
                ((Main) activity).getEasyEditManager().finish();
            }
            TaskFragment.showDialog(activity, t);
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
                if (savingHelper.save(context, FILENAME, selected, true)) {

                } else {
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

            Task restoredTask = savingHelper.load(context, FILENAME, true);
            if (restoredTask != null) {
                Log.d(DEBUG_TAG, "read saved state");
                Task taskInStorage = App.getTaskStorage().get(restoredTask);
                selected = taskInStorage;
                return true;
            } else {
                Log.d(DEBUG_TAG, "saved state null");
                return false;
            }
        } finally {
            readingLock.unlock();
        }
    }

    @Override
    public Task getSelected() {
        return selected;
    }

    @Override
    public void deselectObjects() {
        selected = null;
    }

    @Override
    public void setSelected(Task o) {
        // not used
    }

    @Override
    public void setPrefs(Preferences prefs) {
        enabled = prefs.areBugsEnabled();
        panAndZoomDownLoad = prefs.getPanAndZoomAutoDownload();
        minDownloadSize = prefs.getBugDownloadRadius() * 2;
        server = prefs.getServer();
        maxDownloadSpeed = prefs.getMaxBugDownloadSpeed() / 3.6f;
        panAndZoomLimit = prefs.getPanAndZoomLimit();
    }
}
