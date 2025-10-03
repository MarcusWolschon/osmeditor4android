package de.blau.android.photos;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ConfigurationChangeAwareActivity;
import de.blau.android.util.ImageLoader;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Util;

/**
 * 
 * Wrapepr around the PhotoViewerFragment so that we can run it in split screen mode
 * 
 * @author simon
 *
 */
public class PhotoViewerActivity<T extends Serializable> extends ConfigurationChangeAwareActivity {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PhotoViewerActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = PhotoViewerActivity.class.getSimpleName().substring(0, TAG_LEN);

    private ArrayList<T>                                photoList   = null;
    private int                                         startPos    = 0;
    private ImageLoader                                 photoLoader = null;
    private boolean                                     wrap        = true;
    private PhotoViewerFragment<T>                      photoViewerFragment;
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;

    /**
     * Start a new activity with the PhotoViewer as the contents
     * 
     * @param context the Android Context calling this
     * @param photoList a list of photos to show
     * @param startPos the starting position in the list
     */
    public static <V extends Serializable> void start(@NonNull Context context, @NonNull ArrayList<V> photoList, int startPos) { // NOSONAR
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
    protected static <V extends Serializable> void setExtrasAndStart(@NonNull Context context, @NonNull ArrayList<V> photoList, int startPos,
            @NonNull Intent intent) {
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

    @SuppressWarnings("unchecked")
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
            photoList = Util.getSerializableExtra(getIntent(), PhotoViewerFragment.PHOTO_LIST_KEY, ArrayList.class);
            startPos = getIntent().getIntExtra(PhotoViewerFragment.START_POS_KEY, 0);
            photoLoader = Util.getSerializableExtra(getIntent(), PhotoViewerFragment.PHOTO_LOADER_KEY, ImageLoader.class);
            wrap = Util.getSerializableExtra(getIntent(), PhotoViewerFragment.WRAP_KEY, Boolean.class);
        } else {
            Log.d(DEBUG_TAG, "Initializing from saved state");
            String photoListFilename = savedInstanceState.getString(PhotoViewerFragment.PHOTO_LIST_KEY);
            photoList = new SavingHelper<ArrayList<T>>().load(this, photoListFilename, true);
            startPos = savedInstanceState.getInt(PhotoViewerFragment.START_POS_KEY);
            photoLoader = Util.getSerializeable(savedInstanceState, PhotoViewerFragment.PHOTO_LOADER_KEY, ImageLoader.class);
            wrap = savedInstanceState.getBoolean(PhotoViewerFragment.WRAP_KEY);
        }
        String tag = PhotoViewerFragment.class.getName() + this.getClass().getName();
        photoViewerFragment = (PhotoViewerFragment<T>) getSupportFragmentManager().findFragmentByTag(tag);
        if (photoViewerFragment == null) {
            photoViewerFragment = PhotoViewerFragment.newInstance(photoList, startPos, photoLoader, wrap);
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, photoViewerFragment, tag).commit();
        }
    }

    @Override
    protected void onStart() {
        Log.d(DEBUG_TAG, "onStart");
        super.onStart();
        deleteRequestLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Log.d("deleteResultLauncher", "deleted " + result.getData());
                photoViewerFragment.removeCurrentImage();
                return;
            }
            Log.e("deleteResultLauncher", "deleting failed " + result.getData());
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ArrayList<T> tempList = Util.getSerializableExtra(getIntent(), PhotoViewerFragment.PHOTO_LIST_KEY, ArrayList.class);
        int tempPos = intent.getIntExtra(PhotoViewerFragment.START_POS_KEY, 0);
        T tempPhoto = tempList.get(tempPos);
        int index = photoList.indexOf(tempPhoto);
        Fragment f = getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (f != null) {
            if (index >= 0) { // existing
                ((PhotoViewerFragment<T>) f).setCurrentPosition(index);
            } else {
                if (photoList.size() == 1) { // add to existing
                    photoList.add(tempPhoto);
                    ((PhotoViewerFragment<T>) f).addPhoto(tempPhoto);
                } else { // replace
                    photoList.clear();
                    photoList.addAll(tempList);
                    startPos = tempPos;
                    ((PhotoViewerFragment<T>) f).replacePhotos(tempList, tempPos);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // NOSONAR don't call super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Fragment f = getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (f == null) {
            Log.e(DEBUG_TAG, "Couldn't locate fragment");
            return;
        }
        String listFilename = ((PhotoViewerFragment<T>) f).photoListFilename();
        new SavingHelper<ArrayList<T>>().save(this, listFilename, photoList, true);
        outState.putString(PhotoViewerFragment.PHOTO_LIST_KEY, listFilename);
        outState.putInt(PhotoViewerFragment.START_POS_KEY, ((PhotoViewerFragment<T>) f).getCurrentPosition());
        outState.putSerializable(PhotoViewerFragment.PHOTO_LOADER_KEY, photoLoader);
        outState.putBoolean(PhotoViewerFragment.WRAP_KEY, wrap);
    }

    /**
     * @return the deleteRequestLauncher
     */
    public ActivityResultLauncher<IntentSenderRequest> getDeleteRequestLauncher() {
        return deleteRequestLauncher;
    }
}
