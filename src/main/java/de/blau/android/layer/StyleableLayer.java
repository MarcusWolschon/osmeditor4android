package de.blau.android.layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.layer.geojson.MapOverlay;
import de.blau.android.util.Snack;

public abstract class StyleableLayer extends MapViewLayer implements DiscardInterface, Serializable {
    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    /**
     * when reading state lockout writing/reading
     */
    protected transient ReentrantLock readingLock = new ReentrantLock();
    private transient boolean         saved       = false;

    /**
     * Styling parameters
     */
    protected int   iconRadius;
    protected int   color;
    protected float strokeWidth;

    protected transient Paint paint;

    /**
     * Name for this layer (typically the file name)
     */
    protected String name;

    /**
     * Get the current color for this layer
     * 
     * @return the color as an int
     */
    public abstract int getColor();

    /**
     * Set the color for the layer
     * 
     * @param color color to use
     */
    public abstract void setColor(int color);

    /**
     * Get the stroke width for drawing lines
     * 
     * @return the stroke width in pixels
     */
    public abstract float getStrokeWidth();

    /**
     * Set the stroke width for lines
     * 
     * @param width stroke width in pixels
     */
    public abstract void setStrokeWidth(float width);

    /**
     * Symbol for points
     * 
     * @return the Path object used for points
     */
    public Path getPointSymbol() {
        // unused
        return null;
    }

    /**
     * Set the Path for the symbol for points
     * 
     * @param symbol the Path for symbol
     */
    public void setPointSymbol(@NonNull Path symbol) {
        // unimplemented
    }

    /**
     * Set styling parameters back to defaults
     */
    public abstract void resetStyling();

    /**
     * Get a list of keys for labeling
     * 
     * @return a list of keys, null if there are none
     */
    @NonNull
    public List<String> getLabelList() {
        return new ArrayList<>();
    }

    /**
     * Set the key of the label to use
     * 
     * @param key label key to use
     */
    public void setLabel(@NonNull String key) {
        // do nothing as default
    }

    /**
     * Get the current label field/key
     * 
     * @return the current label field/key or null if none
     */
    @Nullable
    public String getLabel() {
        return null;
    }

    @Override
    public synchronized void onSaveState(@NonNull Context context) throws IOException {
        super.onSaveState(context);
        if (saved) {
            Log.i(DEBUG_TAG, "state not dirty, skipping save");
            return;
        }
        if (readingLock.tryLock()) {
            try {
                // TODO this doesn't really help with error conditions need to throw exception
                if (save(context)) {
                    saved = true;
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
     * Save the state of the layer
     * 
     * @param context an Android Context
     * @return true if successfully saved
     */
    protected abstract boolean save(@NonNull Context context) throws IOException;

    @Override
    public synchronized boolean onRestoreState(@NonNull Context context) {
        super.onRestoreState(context);
        try {
            readingLock.lock();
            // disable drawing
            setVisible(false);
            StyleableLayer restoredOverlay = load(context);
            if (restoredOverlay != null) {
                Log.d(DEBUG_TAG, "read saved state");
                iconRadius = restoredOverlay.iconRadius;
                color = restoredOverlay.color;
                paint.setColor(color);
                strokeWidth = restoredOverlay.strokeWidth;
                paint.setStrokeWidth(strokeWidth);
                name = restoredOverlay.name;
                return true;
            } else {
                Log.d(DEBUG_TAG, "saved state null");
                return false;
            }
        } finally {
            // re-enable drawing
            setVisible(true);
            readingLock.unlock();
        }
    }

    /**
     * Load saved state
     * 
     * @param context an Android Context
     * @return a StyleableLayer
     */
    protected abstract StyleableLayer load(@NonNull Context context);

    @Override
    public void discard(Context context) {
        if (readingLock.tryLock()) {
            try {
                discardLayer(context);
            } finally {
                readingLock.unlock();
            }
        }
    }

    /**
     * Actually discard the layer
     * 
     * @param context an Android Context
     */
    protected abstract void discardLayer(@NonNull Context context);

    /**
     * Mark the layer as dirty/unsaved
     */
    protected void dirty() {
        saved = false;
    }
}
