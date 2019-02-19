package de.blau.android;

import java.io.InputStream;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.ACRAHelper;

/**
 * Simple extension around AsyncTask for loading OSM data files or similar which typically a lot of template code
 * 
 * @author simon
 *
 */
public abstract class ReadAsyncClass extends AsyncTask<Boolean, Void, ReadAsyncResult> {

    private static final String  DEBUG_TAG = "ReadAsyncClass";
    final FragmentActivity       activity;
    final Map                    map;
    final InputStream            is;
    final boolean                add;
    final PostAsyncActionHandler postLoad;
    final ViewBox                viewBox;

    /**
     * Construct a new instance
     * 
     * @param activity the calling FragmentActivity
     * @param is the InputStream
     * @param add if true add to exiting data (not always used)
     * @param postLoad a handler to call afte the load has completed
     */
    public ReadAsyncClass(@NonNull final FragmentActivity activity, @NonNull final InputStream is, boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {
        this.activity = activity;
        this.is = is;
        this.add = add;
        this.postLoad = postLoad;
        map = activity instanceof Main ? ((Main) activity).getMap() : null;
        viewBox = App.getLogic().getViewBox();
    }

    @Override
    protected void onPreExecute() {
        Progress.showDialog(activity, Progress.PROGRESS_LOADING);
    }

    @Override
    protected void onPostExecute(ReadAsyncResult result) {
        Progress.dismissDialog(activity, Progress.PROGRESS_LOADING);
        if (map != null) {
            try {
                viewBox.setRatio(map, (float) map.getWidth() / (float) map.getHeight());
            } catch (OsmException e) {
                Log.d(DEBUG_TAG, "onPostExecute got " + e.getMessage());
            }
            DataStyle.updateStrokes(App.getLogic().strokeWidth(viewBox.getWidth()));
        }
        int code = result.getCode();
        if (code != 0) {
            if (code == ErrorCodes.OUT_OF_MEMORY && App.getDelegator().isDirty()) {
                code = ErrorCodes.OUT_OF_MEMORY_DIRTY;
            }
            try {
                if (!activity.isFinishing()) {
                    ErrorAlert.showDialog(activity, code, result.getMessage());
                }
            } catch (Exception ex) { // now and then this seems to throw a WindowManager.BadTokenException,
                ACRAHelper.nocrashReport(ex, ex.getMessage());
            }
            if (postLoad != null) {
                postLoad.onError();
            }
        } else {
            if (postLoad != null) {
                postLoad.onSuccess();
            }
        }
        if (map != null) {
            map.invalidate();
        }
        activity.supportInvalidateOptionsMenu();
    }
}
