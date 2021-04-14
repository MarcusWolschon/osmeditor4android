package de.blau.android.layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.util.Snack;

public abstract class StyleableLayer extends MapViewLayer implements StyleableInterface, DiscardInterface, Serializable {
    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = StyleableLayer.class.getSimpleName();

    /**
     * when reading state lockout writing/reading
     */
    protected transient ReentrantLock readingLock = new ReentrantLock();
    private transient boolean         saved       = true;

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

    @Override
    public int getColor() {
        return paint.getColor();
    }

    @Override
    public void setColor(int color) {
        dirty();
        paint.setColor(color);
        this.color = color;
    }

    @Override
    public float getStrokeWidth() {
        return paint.getStrokeWidth();
    }

    @Override
    public void setStrokeWidth(float width) {
        dirty();
        paint.setStrokeWidth(width);
        strokeWidth = width;
    }

    /**
     * Set styling parameters back to defaults
     */
    @Override
    public abstract void resetStyling();

    @Override
    public synchronized void onSaveState(@NonNull Context context) throws IOException {
        super.onSaveState(context);
        if (saved) {
            Log.i(DEBUG_TAG, "state not dirty, skipping save");
            return;
        }
        if (readingLock.tryLock()) {
            try {
                Log.i(DEBUG_TAG, "saving state");
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
        boolean tempVisible = isVisible();
        try {
            readingLock.lock();
            if (saved) { // don't overwrite new state
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
            } else {
                Log.d(DEBUG_TAG, "dirty state not loading saved");
                return true;
            }
        } finally {
            // re-enable drawing
            setVisible(tempVisible);
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
