package de.blau.android.tasks;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.GeoPoint;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.rtree.BoundedObject;

/**
 * Base class for OSM Notes, Osmose Errors and in the future perhaps more
 *
 */
public abstract class Task implements Serializable, BoundedObject, GeoPoint {
    /**
     * 
     */
    private static final long serialVersionUID = 7L;

    private static final int ICON_SELECTED_BORDER = 2;
    private int              iconSelectedBorder;

    class BitmapWithOffset {
        Bitmap icon = null;
        float  w2   = 0f;
        float  h2   = 0f;
    }

    protected static BitmapWithOffset cachedIconClosed;
    protected static BitmapWithOffset cachedIconChangedClosed;
    protected static BitmapWithOffset cachedIconOpen;
    protected static BitmapWithOffset cachedIconChanged;

    /** Latitude *1E7. */
    int lat;
    /** Longitude *1E7. */
    int lon;

    /** Bug state. */
    /**
     * Enums for modes.
     */
    public enum State {
        OPEN, CLOSED, FALSE_POSITIVE, SKIPPED, DELETED, ALREADY_FIXED, TOO_HARD
    }

    private State state;

    /** Has been edited */
    private boolean changed = false;

    /**
     * Get the latitude of the bug.
     * 
     * @return The latitude *1E7.
     */
    public int getLat() {
        return lat;
    }

    /**
     * Get the longitude of the bug.
     * 
     * @return The longitude *1E7.
     */
    public int getLon() {
        return lon;
    }

    /**
     * @return the state
     */
    @NonNull
    public State getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(@NonNull State state) {
        this.state = state;
    }

    /**
     * Get the bug open/closed state.
     * 
     * @return true if the bug is closed, false if it's still open.
     */
    public boolean isClosed() {
        return state == State.CLOSED || state == State.FALSE_POSITIVE || state == State.ALREADY_FIXED || state == State.DELETED;
    }

    /**
     * Check if the task has been changed
     * 
     * @return true if the task has been changed.
     */
    public boolean hasBeenChanged() {
        return changed;
    }

    /**
     * Check if this task is new
     * 
     * @return true if new
     */
    public boolean isNew() {
        return false;
    }

    /**
     * Close the bug
     */
    public void close() {
        state = State.CLOSED;
    }

    /**
     * Close the bug
     */
    public void setFalse() {
        state = State.FALSE_POSITIVE;
    }

    /**
     * Open the bug
     */
    public void open() {
        state = State.OPEN;
    }

    /**
     * Check if the bug is open
     * 
     * @return true if open
     */
    public boolean isOpen() {
        return state == State.OPEN;
    }

    @NonNull
    @Override
    public BoundingBox getBounds() {
        return new BoundingBox(lon, lat);
    }

    @NonNull
    @Override
    public BoundingBox getBounds(@NonNull BoundingBox result) {
        result.resetTo(lon, lat);
        return result;
    }

    /**
     * Check if we can upload the task.
     * 
     * Override to return false if necessary
     * 
     * @return true if this bug could be uploaded
     */
    public boolean canBeUploaded() {
        return true;
    }

    /**
     * Indicate that the bug has been modified or not
     * 
     * @param changed if true the Task has been changed
     */
    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    /**
     * Get a description for this Task
     * 
     * @return a String containing a short description
     */
    @NonNull
    public abstract String getDescription();

    /**
     * Get a description for this Task, i18n version
     * 
     * @param context Android Context
     * @return a String containing a short description
     */
    @NonNull
    public abstract String getDescription(@NonNull Context context);

    /**
     * Get the time this Task was last updated
     * 
     * @return a Date object
     */
    @NonNull
    public abstract Date getLastUpdate();

    /**
     * Get the string that is used for filtering
     * 
     * Has to match a value in the bugfilter.xml resource file (unluckily there is no elegant way to reference the file
     * here)
     * 
     * @return the string we will filter on
     */
    @NonNull
    public abstract String bugFilterKey();

    /**
     * Icon drawing related stuff
     */
    /**
     * Draw an icon at the specified location on the canvas
     * 
     * @param context Android Context
     * @param cache the cache for the icon
     * @param c the Canvas we are drawing on
     * @param icon the resource id for the icon
     * @param x x position on the canvas
     * @param y y position on the canvas
     * @param selected true if selected and highlighting should be applied
     */
    @NonNull
    BitmapWithOffset drawIcon(@NonNull Context context, @Nullable BitmapWithOffset cache, @NonNull Canvas c, int icon, float x, float y, boolean selected) {
        if (cache == null) {
            cache = new BitmapWithOffset();
            cache.icon = BitmapFactory.decodeResource(context.getResources(), icon);
            cache.w2 = cache.icon.getWidth() / 2f;
            cache.h2 = cache.icon.getHeight() / 2f;
            iconSelectedBorder = Density.dpToPx(context, ICON_SELECTED_BORDER);
        }
        if (selected) {
            RectF r = new RectF(x - cache.w2 - iconSelectedBorder, y - cache.h2 - iconSelectedBorder, x + cache.w2 + iconSelectedBorder,
                    y + cache.h2 + iconSelectedBorder);
            c.drawRoundRect(r, iconSelectedBorder, iconSelectedBorder, DataStyle.getInternal(DataStyle.SELECTED_NODE).getPaint());
        }
        c.drawBitmap(cache.icon, x - cache.w2, y - cache.h2, null);
        return cache;
    }

    /**
     * Draw an icon for the open state
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas
     * @param y y position on the canvas
     * @param selected true if selected
     */
    public void drawBitmapOpen(Context context, Canvas c, float x, float y, boolean selected) {
        cachedIconOpen = drawIcon(context, cachedIconOpen, c, R.drawable.bug_open, x, y, selected);
    }

    /**
     * Draw an icon for when the Task has been changed and not uploaded
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas
     * @param y y position on the canvas
     * @param selected true if selected
     */
    public void drawBitmapChanged(Context context, Canvas c, float x, float y, boolean selected) {
        cachedIconChanged = drawIcon(context, cachedIconChanged, c, R.drawable.bug_changed, x, y, selected);
    }

    /**
     * Draw an icon for when the Task has been closed but not uploaded
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas
     * @param y y position on the canvas
     * @param selected true if selected
     */
    public void drawBitmapChangedClosed(Context context, Canvas c, float x, float y, boolean selected) {
        cachedIconChangedClosed = drawIcon(context, cachedIconChangedClosed, c, R.drawable.bug_changed_closed, x, y, selected);
    }

    /**
     * Draw an icon for when the Task has been closed
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas
     * @param y y position on the canvas
     * @param selected true if selected
     */
    public void drawBitmapClosed(Context context, Canvas c, float x, float y, boolean selected) {
        cachedIconClosed = drawIcon(context, cachedIconClosed, c, R.drawable.bug_closed, x, y, selected);
    }

    /**
     * Sort a list of Tasks by their distance to the supplied coordinates, nearest first
     *
     * @param <T> sub class of Task
     * @param tasks the list of tasks
     * @param lon WGS84 longitude
     * @param lat WGS84 latitude
     */
    public static <T extends Task> void sortByDistance(@NonNull List<T> tasks, final double lon, final double lat) {
        Collections.sort(tasks, (T t1, T t2) -> Double.compare(GeoMath.haversineDistance(lon, lat, t1.getLon() / 1E7D, t1.getLat() / 1E7D),
                GeoMath.haversineDistance(lon, lat, t2.getLon() / 1E7D, t2.getLat() / 1E7D)));
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
