package de.blau.android.photos;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ConfigurationChangeAwareActivity;
import de.blau.android.util.ImageLoader;
import de.blau.android.util.Util;

/**
 * 
 * Wrapepr around the PhotoViewerFragment so that we can run it in split screen mode
 * 
 * @author simon
 *
 */
public class PhotoViewerActivity extends ConfigurationChangeAwareActivity {

    private static final String DEBUG_TAG = PhotoViewerActivity.class.getSimpleName().substring(0, Math.min(23, PhotoViewerActivity.class.getSimpleName().length()));

    ArrayList<String> photoList   = null;
    int               startPos    = 0;
    ImageLoader       photoLoader = null;
    boolean           wrap        = true;

    /**
     * Start a new activity with the PhotoViewer as the contents
     * 
     * @param context the Android Context calling this
     * @param photoList a list of photos to show
     * @param startPos the starting position in the list
     */
    public static void start(@NonNull Context context, @NonNull ArrayList<String> photoList, int startPos) { // NOSONAR
        Intent intent = new Intent(context, PhotoViewerActivity.class);
        intent.putExtra(PhotoViewerFragment.WRAP_KEY, true);
        setExtrasAndStart(context, photoList, startPos, intent);
    }

    /**
     * Set standard extras on the Intent and then send it
     * 
     * @param context Android Context
     * @param photoList list of photos
     * @param startPos the starting position in the list
     * @param intent the Intent to use
     */
    protected static void setExtrasAndStart(@NonNull Context context, @NonNull ArrayList<String> photoList, int startPos, @NonNull Intent intent) {
        intent.putExtra(PhotoViewerFragment.PHOTO_LIST_KEY, photoList);
        intent.putExtra(PhotoViewerFragment.START_POS_KEY, startPos);
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;
        } else {
            flags = flags | Intent.FLAG_ACTIVITY_CLEAR_TASK;
        }
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreate");
        Preferences prefs = new Preferences(this);
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        }
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from intent");
            photoList = getIntent().getStringArrayListExtra(PhotoViewerFragment.PHOTO_LIST_KEY);
            startPos = getIntent().getIntExtra(PhotoViewerFragment.START_POS_KEY, 0);
            photoLoader = Util.getSerializableExtra(getIntent(), PhotoViewerFragment.PHOTO_LOADER_KEY, ImageLoader.class);
            wrap = Util.getSerializableExtra(getIntent(), PhotoViewerFragment.WRAP_KEY, Boolean.class);
        } else {
            Log.d(DEBUG_TAG, "Initializing from saved state");
            photoList = savedInstanceState.getStringArrayList(PhotoViewerFragment.PHOTO_LIST_KEY);
            startPos = savedInstanceState.getInt(PhotoViewerFragment.START_POS_KEY);
            photoLoader = Util.getSerializeable(savedInstanceState, PhotoViewerFragment.PHOTO_LOADER_KEY, ImageLoader.class);
            wrap = savedInstanceState.getBoolean(PhotoViewerFragment.WRAP_KEY);
        }
        String tag = PhotoViewerFragment.class.getName() + this.getClass().getName();
        Fragment f = getSupportFragmentManager().findFragmentByTag(tag);
        if (f == null) {
            f = PhotoViewerFragment.newInstance(photoList, startPos, photoLoader, wrap);
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, f, tag).commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ArrayList<String> tempList = getIntent().getStringArrayListExtra(PhotoViewerFragment.PHOTO_LIST_KEY);
        int tempPos = intent.getIntExtra(PhotoViewerFragment.START_POS_KEY, 0);
        String tempPhoto = tempList.get(tempPos);
        int index = photoList.indexOf(tempPhoto);
        Fragment f = getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (f != null) {
            if (index >= 0) { // existing
                ((PhotoViewerFragment) f).setCurrentPosition(index);
            } else {
                if (photoList.size() == 1) { // add to existing
                    photoList.add(tempPhoto);
                    ((PhotoViewerFragment) f).addPhoto(tempPhoto);
                } else { // replace
                    photoList.clear();
                    photoList.addAll(tempList);
                    startPos = tempPos;
                    ((PhotoViewerFragment) f).replacePhotos(tempList, tempPos);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        outState.putStringArrayList(PhotoViewerFragment.PHOTO_LIST_KEY, photoList);
        Fragment f = getSupportFragmentManager().findFragmentById(android.R.id.content);
        outState.putInt(PhotoViewerFragment.START_POS_KEY, f != null ? ((PhotoViewerFragment) f).getCurrentPosition() : startPos);
        outState.putSerializable(PhotoViewerFragment.PHOTO_LOADER_KEY, photoLoader);
        outState.putBoolean(PhotoViewerFragment.WRAP_KEY, wrap);
    }
}
