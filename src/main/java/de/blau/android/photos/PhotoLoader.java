package de.blau.android.photos;

import java.io.Serializable;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.content.Context;
import androidx.annotation.NonNull;

public interface PhotoLoader extends Serializable {

    /**
     * Load the image
     * 
     * @param view the ImageView to load it in to
     * @param uri the Uri or other reference
     */
    void load(@NonNull SubsamplingScaleImageView view, @NonNull String uri);
    
    /**
     * Show the location of the photo on the map
     * 
     * @param context Android Context
     * @param index the index in to the list of photos
     */
    void showOnMap(@NonNull Context context, int index);
    
    /**
     * Share the photo with an external program
     * 
     * @param context Android Context
     * @param uri the Uri or other reference to the photo 
     */
    void share(@NonNull Context context, @NonNull String uri);
}
