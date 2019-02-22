package de.blau.android.layer.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DisableInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.views.IMapView;

public class MapOverlay extends MapViewLayer implements ExtentInterface, DisableInterface, ClickableInterface<Task>, ConfigureInterface {

    private static final String DEBUG_TAG = "tasks";

    private boolean enabled = false;

    /** Map this is an overlay of. */
    private final Map map;

    /** Bugs visible on the overlay. */
    private TaskStorage tasks = App.getTaskStorage();

    private transient ReentrantLock readingLock = new ReentrantLock();

    public static final String FILENAME = "selectedtask.res";

    private transient SavingHelper<Task> savingHelper = new SavingHelper<>();

    private Task selected = null;

    /**
     * Construct a new task layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(final Map map) {
        this.map = map;
    }

    @Override
    public boolean isReadyToDraw() {
        enabled = map.getPrefs().areBugsEnabled();
        return enabled;
    }

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {
        if (isVisible && enabled) {

            // the idea is to have the circles a bit bigger when zoomed in, not so
            // big when zoomed out
            // currently we don't adjust the icon size for density final float radius = Density.dpToPx(1.0f +
            // osmv.getZoomLevel() / 2.0f);
            ViewBox bb = osmv.getViewBox();

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
        // multi choice preferences are not supported on old SKD versions
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
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
}
