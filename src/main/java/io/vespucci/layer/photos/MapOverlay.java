package io.vespucci.layer.photos;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.Map;
import io.vespucci.PostAsyncActionHandler;
import io.vespucci.layer.ClickableInterface;
import io.vespucci.layer.DiscardInterface;
import io.vespucci.layer.LayerType;
import io.vespucci.layer.NonSerializeableLayer;
import io.vespucci.osm.ViewBox;
import io.vespucci.photos.Photo;
import io.vespucci.photos.PhotoIndex;
import io.vespucci.photos.PhotoViewerActivity;
import io.vespucci.photos.PhotoViewerFragment;
import io.vespucci.prefs.Preferences;
import io.vespucci.util.ACRAHelper;
import io.vespucci.util.ContentResolverUtil;
import io.vespucci.util.ExecutorTask;
import io.vespucci.util.GeoMath;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.Util;
import io.vespucci.views.IMapView;

/**
 * implement a geo-referenced photo overlay, code stolen from the OSB implementation
 * 
 * @author simon
 *
 */
public class MapOverlay extends NonSerializeableLayer implements DiscardInterface, ClickableInterface<Photo> {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    private static final int VIEWER_MAX = 100;

    /** viewbox needs to be less wide than this for displaying photos, just to avoid querying the whole world */
    private static final int TOLERANCE_MAX_VIEWBOX_WIDTH = 40000 * 32;

    /** Photos visible on the overlay. */
    private List<Photo> photos;

    private final ViewBox bb = new ViewBox();

    /** have we already run a scan? */
    private boolean indexed = false;

    /** set while we are actually creating index */
    private boolean indexing = false;

    /** default icon */
    private final Drawable icon;

    /** selected icon */
    private final Drawable selectedIcon;

    /** icon dimensions */
    private int h2;
    private int w2;

    /**
     * Index disk/in-memory of photos
     */
    private PhotoIndex pi = null;

    /** last selected photo, may not be still displayed */
    private Photo selected = null;

    private PhotoIndexer  indexer;
    private PhotoObserver observer;

    /**
     * Update the photo index
     */
    private class PhotoIndexer extends ExecutorTask<PostAsyncActionHandler, Integer, Void> {

        private PostAsyncActionHandler handler;

        /**
         * Create a new indexer
         * 
         * @param executorService the ExecutorService to use
         * @param uiHandler the Hander for the mail looper
         */
        PhotoIndexer(@NonNull ExecutorService executorService, @NonNull Handler uiHandler) {
            super(executorService, uiHandler);
        }

        @Override
        protected Void doInBackground(PostAsyncActionHandler handler) {
            this.handler = handler;
            if (!indexing) {
                indexing = true;
                publishProgress(0);
                pi.createOrUpdateIndex();
                pi.fill(null);
                publishProgress(1);
                indexing = false;
                indexed = true;
                if (map.getPrefs().scanMediaStore()) {
                    observer = new PhotoObserver(new Handler(Looper.getMainLooper()));
                    map.getContext().getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);
                }
            }
            return null;
        }

        @Override
        protected void onProgress(Integer progress) {
            if (progress == 0) {
                ScreenMessage.toastTopInfo(map.getContext(), R.string.toast_photo_indexing_started);
            } else if (progress == 1) {
                ScreenMessage.toastTopInfo(map.getContext(), R.string.toast_photo_indexing_finished);
            }
        }

        @Override
        protected void onPostExecute(Void param) {
            if (handler != null) {
                if (indexed) {
                    handler.onSuccess();
                } else {
                    handler.onError(null);
                }
            }
        }
    }

    /**
     * Construct a new photo layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(@NonNull final Map map) {
        Context context = map.getContext();
        this.map = map;
        photos = new ArrayList<>();
        icon = ContextCompat.getDrawable(context, R.drawable.camera_red);
        selectedIcon = ContextCompat.getDrawable(context, R.drawable.camera_green);
        // note this assumes the icons are the same size
        w2 = icon.getIntrinsicWidth() / 2;
        h2 = icon.getIntrinsicHeight() / 2;

        pi = new PhotoIndex(context);
        Logic logic = App.getLogic();
        indexer = new PhotoIndexer(logic.getExecutorService(), logic.getHandler());
    }

    @Override
    public boolean isReadyToDraw() {
        return true;
    }

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {
        if (isVisible) {
            if (needsIndexing()) {
                indexWithPermission();
                return;
            }

            bb.set(osmv.getViewBox());
            if ((bb.getWidth() > TOLERANCE_MAX_VIEWBOX_WIDTH) || (bb.getHeight() > TOLERANCE_MAX_VIEWBOX_WIDTH)) {
                return;
            }

            // draw all the photos
            int w = map.getWidth();
            int h = map.getHeight();
            photos = pi.getPhotos(bb);
            for (Photo p : photos) {
                if (!p.equals(selected)) {
                    drawIcon(c, bb, w, h, p, icon);
                }
            }
            if (selected != null) {
                drawIcon(c, bb, w, h, selected, selectedIcon);
            }
        }
    }

    /**
     * Check for permissions and run the indexer
     */
    private void indexWithPermission() {
        if (Util.permissionGranted(map.getContext(), Main.STORAGE_PERMISSION)) {
            indexer.execute(map::invalidate);
        }
    }

    /**
     * Draw the photo icon
     * 
     * @param c the Canvas
     * @param bb the current ViewBox
     * @param w map width
     * @param h map height
     * @param p the Photo
     * @param icon the icon Drawable
     */
    public void drawIcon(@NonNull Canvas c, @NonNull ViewBox bb, int w, int h, @NonNull Photo p, @NonNull Drawable icon) {
        int x = (int) GeoMath.lonE7ToX(w, bb, p.getLon());
        int y = (int) GeoMath.latE7ToY(h, w, bb, p.getLat());
        icon.setBounds(new Rect(x - w2, y - h2, x + w2, y + h2));
        if (p.hasDirection()) {
            c.rotate(p.getDirection(), x, y);
            icon.draw(c);
            c.rotate(-p.getDirection(), x, y);
        } else {
            icon.draw(c);
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public List<Photo> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<Photo> result = new ArrayList<>();
        final float tolerance = map.getDataStyle().getCurrent().getNodeToleranceValue();
        for (Photo p : photos) {
            float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, p.getLon()) - x);
            float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, p.getLat()) - y);
            if ((differenceX <= tolerance) && (differenceY <= tolerance) && Math.hypot(differenceX, differenceY) <= tolerance) {
                Uri photoUri = p.getRefUri(map.getContext());
                if (photoUri != null) { // only return valid entries
                    result.add(p);
                }
            }
        }
        Log.d("photos.MapOverlay", "getClickedPhotos found " + result.size());
        return result;
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_photos);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public void onSelected(FragmentActivity activity, Photo photo) {
        Resources resources = activity.getResources();
        try {
            Uri photoUri = photo.getRefUri(activity);
            if (photoUri != null) {
                Preferences prefs = map.getPrefs();
                if (prefs.useInternalPhotoViewer()) {
                    startInternalViewer(activity, photo);
                } else {
                    io.vespucci.layer.photos.Util.startExternalPhotoViewer(activity, photoUri);
                }
                selected = photo;
                invalidate();
            } else {
                Log.d(DEBUG_TAG, "onSelected null Uri");
                ScreenMessage.toastTopError(activity, resources.getString(R.string.toast_error_accessing_photo, photo.getRef()));
            }
        } catch (SecurityException ex) {
            Log.d(DEBUG_TAG, "onSelected security exception starting intent: " + ex);
            ScreenMessage.toastTopError(activity, resources.getString(R.string.toast_security_error_accessing_photo, photo.getRef()));
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, "onSelected exception starting intent: " + ex);
            ACRAHelper.nocrashReport(ex, "onSelected exception starting intent");
        }
    }

    /**
     * Start the internal photo viewer for a photo
     * 
     * @param activity the calling activity
     * @param photo the Photo
     */
    private void startInternalViewer(@NonNull FragmentActivity activity, @NonNull Photo photo) {
        List<Photo> temp = new ArrayList<>(photos);
        GeoMath.sortGeoPoint(photo, temp, new ViewBox(bb), map.getWidth(), map.getHeight());
        ArrayList<Photo> shortList = new ArrayList<>(temp.subList(0, Math.min(temp.size(), VIEWER_MAX)));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            PhotoViewerFragment.showDialog(activity, shortList, 0, null);
        } else {
            PhotoViewerActivity.start(activity, shortList, 0);
        }
    }

    @Override
    public SpannableString getDescription(Photo photo) {
        return getDescription(map.getContext(), photo);
    }

    @Override
    public SpannableString getDescription(Context context, Photo photo) {
        return new SpannableString(photo.getDisplayName());
    }

    @Override
    public Photo getSelected() {
        return selected;
    }

    @Override
    public void deselectObjects() {
        selected = null;
    }

    @Override
    public void setSelected(Photo o) {
        selected = o;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (observer != null) {
            map.getContext().getContentResolver().unregisterContentObserver(observer);
        }
    }

    /**
     * Create the index
     * 
     * @param handler handler to run after the index has been created
     */
    public void createIndex(@Nullable PostAsyncActionHandler handler) {
        if (needsIndexing()) {
            indexer.execute(handler);
        }
    }

    /**
     * Check if we need to run the indexer
     * 
     * @return true if we need to index
     */
    private boolean needsIndexing() {
        return !indexed && !indexing && !indexer.isExecuting();
    }

    /**
     * Recreate the index
     */
    public void reIndex() {
        if (pi != null && !indexing) {
            pi.resetIndex();
            indexWithPermission();
        }
    }

    @Override
    public LayerType getType() {
        return LayerType.PHOTO;
    }

    @Override
    public void discard(Context context) {
        onDestroy();
    }

    class PhotoObserver extends ContentObserver {

        /**
         * Construct a new Observer
         * 
         * @param handler the Handler to use
         */
        public PhotoObserver(@NonNull Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(DEBUG_TAG, "onChange " + uri);
            final Context context = map.getContext();
            if (!pi.isIndexed(uri)) {
                PhotoIndex.addToIndex(pi.addPhoto(context, uri, ContentResolverUtil.getDisplaynameColumn(context, uri)));
            } else {
                pi.deletePhoto(context, uri);
            }
            MapOverlay.this.invalidate();
        }
    }
}
