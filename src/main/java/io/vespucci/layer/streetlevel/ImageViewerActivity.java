package io.vespucci.layer.streetlevel;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.photos.PhotoViewerActivity;
import io.vespucci.photos.PhotoViewerFragment;
import io.vespucci.util.ImageLoader;

/**
 * This allows to distinguish between an activity for photos and one for streetlevel images
 * 
 * @author simon
 *
 */
public class ImageViewerActivity extends PhotoViewerActivity<String> {

    /**
     * Start a new activity with the PhotoViewer as the contents
     * 
     * @param context the Android Context calling this
     * @param photoList a list of photos to show
     * @param startPos the starting position in the list
     * @param loader the PhotoLoader to use
     */
    public static void start(@NonNull Context context, @NonNull ArrayList<String> photoList, int startPos, @Nullable ImageLoader loader) { // NOSONAR
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.putExtra(PhotoViewerFragment.PHOTO_LOADER_KEY, loader);
        intent.putExtra(PhotoViewerFragment.WRAP_KEY, false);
        setExtrasAndStart(context, photoList, startPos, intent);
    }
}
