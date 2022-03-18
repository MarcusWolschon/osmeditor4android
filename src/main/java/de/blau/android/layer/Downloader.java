package de.blau.android.layer;

import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.osm.ViewBox;

public abstract class Downloader implements Runnable {

    private static final String DEBUG_TAG = Downloader.class.getSimpleName();

    protected long    lastAutoPrune = 0;
    protected ViewBox box           = null;

    @Override
    public void run() {
        if (box == null || !box.isValidForApi()) {
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
