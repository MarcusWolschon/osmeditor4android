package de.blau.android.layer;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Path;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SerializableTextPaint;

public abstract class StyleableLayer extends MapViewLayer implements StyleableInterface, DiscardInterface, Serializable {
    private static final long serialVersionUID = 4L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, StyleableLayer.class.getSimpleName().length());
    private static final String DEBUG_TAG = StyleableLayer.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * when reading state lockout writing/reading
     */
    protected transient ReentrantLock readingLock = new ReentrantLock();
    private transient boolean         saved       = true;

    /**
     * Styling parameters
     */
    protected int                   iconRadius;
    protected String                symbolName;
    protected SerializableTextPaint paint;

    protected transient Path symbolPath;

    protected transient Map map;

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
    }

    @Override
    public float getStrokeWidth() {
        return paint.getStrokeWidth();
    }

    @Override
    public void setStrokeWidth(float width) {
        dirty();
        paint.setStrokeWidth(width);
    }

    @Override
    public String getPointSymbol() {
        return symbolName;
    }

    @Override
    public void setPointSymbol(@Nullable String symbol) {
        dirty();
        symbolName = symbol;
        if (symbol != null) {
            symbolPath = map.getDataStyleManager().getCurrent().getSymbol(symbol);
        }
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
                if (save(context)) {
                    saved = true;
                } else {
                    Log.e(DEBUG_TAG, "onSaveState unable to save");
                    if (context instanceof Activity) {
                        ScreenMessage.barError((Activity) context, context.getString(R.string.toast_layer_statesave_failed, name));
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
                    paint = restoredOverlay.paint;
                    iconRadius = restoredOverlay.iconRadius;
                    name = restoredOverlay.name;
                    symbolName = restoredOverlay.symbolName;
                    symbolPath = App.getDataStyleManager(context).getCurrent().getSymbol(symbolName);
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
    @Nullable
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
    public void dirty() {
        saved = false;
    }

    @Override
    public void setMapInstance(@NonNull Map map) {
        this.map = map;
    }
}
