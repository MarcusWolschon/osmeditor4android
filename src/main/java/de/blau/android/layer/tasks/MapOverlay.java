package de.blau.android.layer.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import de.blau.android.App;
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
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;

public class MapOverlay extends MapViewLayer implements ExtentInterface, DisableInterface, ClickableInterface, ConfigureInterface {

    private static final String DEBUG_TAG = "tasks";

    private boolean enabled = false;

    /** Map this is an overlay of. */
    private final Map map;

    /** Bugs visible on the overlay. */
    private TaskStorage tasks = App.getTaskStorage();

    /**
     * Construct a new layer
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

                    if (t.isClosed() && t.hasBeenChanged()) {
                        t.drawBitmapChangedClosed(map.getContext(), c, x, y);
                    } else if (t.isClosed()) {
                        t.drawBitmapClosed(map.getContext(), c, x, y);
                    } else if (t.isNew() || t.hasBeenChanged()) {
                        t.drawBitmapChanged(map.getContext(), c, x, y);
                    } else {
                        t.drawBitmapOpen(map.getContext(), c, x, y);
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
            // For debugging the OSB editor when the OSB site is down:
            // result.add(new Bug(GeoMath.yToLatE7(map.getHeight(), viewBox, y), GeoMath.xToLonE7(map.getWidth(),
            // viewBox, x), true));
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
    public void onSelected(FragmentActivity activity, Object object) {
        if (!(object instanceof Task)) {
            Log.e(DEBUG_TAG, "Wrong object for " + getName() + " " + object.getClass().getName());
            return;
        }
        Task bug = (Task) object;
        App.getLogic().setSelectedBug(bug);
        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("fragment_bug");
        try {
            if (prev != null) {
                ft.remove(prev);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "performBugEdit removing dialog ", isex);
        }
        TaskFragment bugDialog = TaskFragment.newInstance(bug);
        try {
            bugDialog.show(fm, "fragment_bug");
        } catch (IllegalStateException isex) {
            // FIXME properly
            Log.e(DEBUG_TAG, "performBugEdit showing dialog ", isex);
        }
    }

    @Override
    public String getDescription(Object object) {
        if (!(object instanceof Task)) {
            Log.e(DEBUG_TAG, "Wrong object for " + getName() + " " + object.getClass().getName());
            return "?";
        }
        Task bug = (Task) object;
        return bug.getDescription(map.getContext());
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
}
