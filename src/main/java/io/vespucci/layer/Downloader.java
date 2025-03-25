package io.vespucci.layer;

import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.osm.ViewBox;

public abstract class Downloader implements Runnable {

    private static final String DEBUG_TAG = Downloader.class.getSimpleName().substring(0, Math.min(23, Downloader.class.getSimpleName().length()));

    protected long      lastAutoPrune = 0;
    protected ViewBox   box           = null;
    private final float maxArea;

    /**
     * Construct a new instance
     * 
     * @param maxArea max area that can be downloaded
     */
    protected Downloader(float maxArea) {
        this.maxArea = maxArea;
    }

    @Override
    public void run() {
        if (box == null || !box.isValidForApi(maxArea)) {
            Log.e(DEBUG_TAG, "Downloader run with null or too large ViewBox " + box);
            return;
        }
        download();
    }

    /**
     * Actually download
     */
    protected abstract void download();

    /**
     * Set the view to download
     * 
     * @param box the ViewBox
     */
    public void setBox(@NonNull ViewBox box) {
        this.box = new ViewBox(box);
    }
}
