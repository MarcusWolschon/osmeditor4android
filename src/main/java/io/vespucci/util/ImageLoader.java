package io.vespucci.util;

import java.io.Serializable;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.content.Context;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public abstract class ImageLoader implements Serializable {

    private static final long serialVersionUID = 2L;

    protected transient Fragment parentFragment = null;

    /**
     * @param caller the caller to set
     */
    public void setParentFragment(Fragment caller) {
        this.parentFragment = caller;
    }

    /**
     * Load the image
     * 
     * @param view the ImageView to load it in to
     * @param uri the Uri or other reference
     */
    public abstract void load(@NonNull SubsamplingScaleImageView view, @NonNull String uri);

    /**
     * Load the image
     * 
     * @param view the ImageView to load it in to
     * @param uri the Uri or other reference
     * @param exifOrientation the EXIT orientation value
     */
    public void load(@NonNull SubsamplingScaleImageView view, @NonNull String uri, int exifOrientation) {
        load(view, uri);
    }

    /**
     * Show the location of the photo on the map
     * 
     * @param context Android Context
     * @param index the index in to the list of photos
     */
    public void showOnMap(@NonNull Context context, int index) {
        // empty
    }

    /**
     * Share the photo with an external program
     * 
     * @param context Android Context
     * @param uri the Uri or other reference to the photo
     */
    public void share(@NonNull Context context, @NonNull String uri) {
        // empty
    }

    /**
     * Indicate if we support an image information function
     * 
     * @return true if supported
     */
    public boolean supportsInfo() {
        return false;
    }

    /**
     * Show some information on the image
     * 
     * @param activity Android activity
     * @param uri the Uri or other reference to the photo
     */
    public void info(@NonNull FragmentActivity activity, @NonNull String uri) {
        // empty
    }

    /**
     * Indicate if we support an image deletion function
     * 
     * @return true if supported
     */
    public boolean supportsDelete() {
        return false;
    }

    /**
     * Delete the photo
     * 
     * @param context Android Context
     * @param uri the Uri or other reference to the photo
     */
    public void delete(@NonNull Context context, @NonNull String uri) {
        // empty
    }

    /**
     * Set a title to display
     * 
     * @param title the TextView to set the title in
     * @param position the position the viewer is at
     */
    public void setTitle(@NonNull TextView title, int position) {
        // empty
    }

    /**
     * Set a description to display
     * 
     * @param description the TextView to set the description in
     * @param position the position the viewer is at
     */
    public void setDescription(@NonNull TextView description, int position) {
        // empty
    }

    /**
     * If an image/Photo is selected call this
     * 
     * @param position the position that was selected
     */
    public void onSelected(int position) {
        // empty
    }

    /**
     * Clear any selection
     */
    public void clearSelection() {
        // empty
    }
}