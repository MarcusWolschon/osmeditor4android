package de.blau.android.layer.streetlevel;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.ImageInfo;
import de.blau.android.layer.LayerType;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ImageLoader;
import de.blau.android.util.ScreenMessage;

public abstract class NetworkImageLoader extends ImageLoader {
    private static final long serialVersionUID = 1L;

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, NetworkImageLoader.class.getSimpleName().length());
    protected static final String DEBUG_TAG = NetworkImageLoader.class.getSimpleName().substring(0, TAG_LEN);

    protected static final String JPG = "." + FileExtensions.JPG;

    public static final String SET_POSITION_KEY = "set_position";
    public static final String COORDINATES_KEY  = "coordinates";
    public static final String LAYER_TYPE_KEY   = "layer_type";

    protected final File                  cacheDir;
    protected final long                  cacheSize;
    protected final String                imageUrl;
    protected final Map<String, double[]> coordinates = new HashMap<>();
    protected final List<String>          ids;

    private static final int               IMAGERY_LOAD_THREADS = 3;
    protected transient ThreadPoolExecutor mThreadPool;

    /**
     * Construct a new loader
     * 
     * @param cacheDir the cacheDir that should be used as a destination for the images
     * @param cacheSize max size of the cache
     * @param imageUrl base url for retrieving the image
     * @param ids list of images ids
     */
    protected NetworkImageLoader(@NonNull File cacheDir, long cacheSize, @NonNull String imageUrl, List<String> ids) {
        this.cacheDir = cacheDir;
        this.cacheSize = cacheSize;
        this.imageUrl = imageUrl;
        this.ids = ids;
    }

    /**
     * Initialize the thread pool
     */
    protected void initThreadPool() {
        if (mThreadPool == null) {
            mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(IMAGERY_LOAD_THREADS);
        }
    }

    /**
     * Prune the image cache
     */
    protected void pruneCache() {
        new ExecutorTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void arg) {
                FileUtil.pruneCache(cacheDir, cacheSize);
                return null;
            }
        }.execute();
    }

    /**
     * Set the image
     * 
     * @param view the ImageView to set it in
     * @param imageFile the file
     */
    protected void setImage(@NonNull SubsamplingScaleImageView view, @NonNull File imageFile) {
        view.post(() -> { // needs to run on the ui thread
            view.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE);
            view.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            view.setImage(ImageSource.uri(Uri.parse(Schemes.FILE + ":" + imageFile.getAbsolutePath())));
        });
    }

    @Override
    public void showOnMap(Context context, int index) {
        if (!App.isPropertyEditorRunning()) {
            Intent intent = new Intent(context, Main.class);
            intent.setAction(Main.ACTION_IMAGE_SELECT);
            intent.putExtra(SET_POSITION_KEY, index);
            String key = ids.get(index);
            if (key != null && coordinates.containsKey(key)) {
                intent.putExtra(COORDINATES_KEY, coordinates.get(key));
            }
            intent.putExtra(LAYER_TYPE_KEY, getLayerType());
            context.startActivity(intent);
        }
    }

    /**
     * Get the LayerType we are associated with
     * 
     * @return a LayerType
     */
    abstract protected LayerType getLayerType();

    @Override
    public void share(Context context, String key) {
        File imageFile = new File(cacheDir, key + JPG);
        if (imageFile.exists()) {
            Uri f = FileProvider.getUriForFile(context, context.getString(R.string.content_provider), imageFile);
            de.blau.android.layer.photos.Util.sharePhoto(context, key, f, MimeTypes.JPEG);
        } else {
            ScreenMessage.toastTopError(context, context.getString(R.string.toast_error_accessing_photo, key));
        }
    }

    @Override
    public boolean supportsInfo() {
        return true;
    }

    @Override
    public void info(@NonNull FragmentActivity activity, @NonNull String uri) {
        Uri f = FileProvider.getUriForFile(activity, activity.getString(R.string.content_provider), new File(cacheDir, uri + JPG));
        ImageInfo.showDialog(activity, f.toString());
    }
}
