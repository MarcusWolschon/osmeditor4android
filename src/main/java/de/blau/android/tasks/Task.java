package de.blau.android.tasks;

import java.io.Serializable;
import java.util.Date;

import android.content.Context;
import android.support.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.rtree.BoundedObject;

/**
 * Base class for OSM Notes, Osmose Errors and in the future perhaps more
 *
 */
public abstract class Task implements Serializable, BoundedObject {

    /**
     * 
     */
    private static final long serialVersionUID = 7L;

    class BitmapWithOffset {
        Bitmap icon = null;
        float  w2   = 0f;
        float  h2   = 0f;
    }

    public static BitmapWithOffset cachedIconClosed;
    public static BitmapWithOffset cachedIconChangedClosed;
    public static BitmapWithOffset cachedIconOpen;
    public static BitmapWithOffset cachedIconChanged;

    /** OSB Bug ID. */
    long id;
    /** Latitude *1E7. */
    int  lat;
    /** Longitude *1E7. */
    int  lon;

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
     * Get the bug ID.
     * 
     * @return The bug ID.
     */
    public long getId() {
        return id;
    }

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
        return state == State.CLOSED || state == State.FALSE_POSITIVE;
    }

    /**
     * Get the bug open/closed state.
     * 
     * @return true if the bug is closed, false if it's still open.
     */
    public boolean hasBeenChanged() {
        return changed;
    }

    /**
     * Close the bug
     */
    void close() {
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

    /**
     * Get a (degenerated) BoundingBox for this task
     * 
     * @return a BoundingBox for the location of this Task
     */
    @NonNull
    public BoundingBox getBounds() {
        return new BoundingBox(lon, lat);
    }

    /**
     * Return true if a newly created bug, only makes sense for Notes
     * 
     * @return true if new
     */
    public boolean isNew() {
        return id <= 0;
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
     * Draw an icon at the specified location on hte canvas
     * 
     * @param context Android Context
     * @param cache the cache for the icon
     * @param c the Canvas we are drawing on
     * @param icon the resource id for the icon
     * @param x x position on the canvas 
     * @param y y position on the canvas 
     */
    void drawIcon(Context context, BitmapWithOffset cache, Canvas c, int icon, float x, float y) {
        if (cache == null) {
            cache = new BitmapWithOffset();
            cache.icon = BitmapFactory.decodeResource(context.getResources(), icon);
            cache.w2 = cache.icon.getWidth() / 2f;
            cache.h2 = cache.icon.getHeight() / 2f;
        }
        c.drawBitmap(cache.icon, x - cache.w2, y - cache.h2, null);
    }

    /**
     * Draw an icon for the open state
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas 
     * @param y y position on the canvas 
     */
    public void drawBitmapOpen(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconOpen,  c, R.drawable.bug_open, x, y);
    }
    
    /**
     * Draw an icon for when the Task has been changed and not uploaded
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas 
     * @param y y position on the canvas 
     */
    public void drawBitmapChanged(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconChanged,  c, R.drawable.bug_changed, x, y);
    }
    
    /**
     * Draw an icon for when the Task has been closed but not uploaded
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas 
     * @param y y position on the canvas 
     */
    public void drawBitmapChangedClosed(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconChangedClosed,  c, R.drawable.bug_changed_closed, x, y);
    }
    
    /**
     * Draw an icon for when the Task has been closed
     * 
     * @param context Android Context
     * @param c the Canvas we are drawing on
     * @param x x position on the canvas 
     * @param y y position on the canvas 
     */
    public void drawBitmapClosed(Context context, Canvas c, float x, float y) {
        drawIcon(context, cachedIconClosed,  c, R.drawable.bug_closed, x, y);
    }
}
