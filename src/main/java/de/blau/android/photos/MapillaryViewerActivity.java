package de.blau.android.photos;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This allows to distinguish between an activity for photos and one for mapillary images
 * 
 * @author simon
 *
 */
public class MapillaryViewerActivity extends PhotoViewerActivity {
    
    /**
     * Start a new activity with the PhotoViewer as the contents
     * 
     * @param context the Android Context calling this
     * @param photoList a list of photos to show
     * @param startPos the starting position in the list
     */
    public static void start(@NonNull Context context, @NonNull ArrayList<String> photoList, int startPos, @Nullable PhotoLoader loader) {
        Intent intent = new Intent(context, MapillaryViewerActivity.class);
        intent.putExtra(PhotoViewerFragment.PHOTO_LOADER_KEY, loader);
        intent.putExtra(PhotoViewerFragment.WRAP_KEY, false);
        setExtrasAndStart(context, photoList, startPos, intent);
    }
}
