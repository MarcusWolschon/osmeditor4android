package de.blau.android.layer.photos;

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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Map;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.osm.ViewBox;
import de.blau.android.photos.Photo;
import de.blau.android.photos.PhotoIndex;
import de.blau.android.photos.PhotoViewerActivity;
import de.blau.android.photos.PhotoViewerFragment;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ContentProviderUtil;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Snack;
import de.blau.android.views.IMapView;

/**
 * implement a geo-referenced photo overlay, code stolen from the OSB implementation
 * 
 * @author simon
 *
 */
public class MapOverlay extends MapViewLayer implements DiscardInterface, ClickableInterface<Photo> {

    private static final String DEBUG_TAG = "PhotoOverlay";

    /** viewbox needs to be less wide than this for displaying bugs, just to avoid querying the whole world for bugs */
    private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 32;

    /** Map this is an overlay of. */
    private final Map map;

    /** Photos visible on the overlay. */
    private List<Photo> photos;

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
            }
            return null;
        }

        @Override
        protected void onProgress(Integer progress) {
            if (progress == 0) {
                Snack.barInfoShort(map, R.string.toast_photo_indexing_started);
            } else if (progress == 1) {
                Snack.barInfoShort(map, R.string.toast_photo_indexing_finished);
            }
        }

        @Override
        protected void onPostExecute(Void param) {
            if (handler != null) {
                handler.onSuccess();
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
            if (!indexed && !indexing && !indexer.isExecuting()) {
                indexer.execute((PostAsyncActionHandler) () -> {
                    if (indexed) {
                        map.invalidate();
                        if (map.getPrefs().scanMediaStore()) {
                            observer = new PhotoObserver(new Handler(Looper.getMainLooper()));
                            map.getContext().getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);
                        }
                    }
                });

                return;
            }

            ViewBox bb = osmv.getViewBox();
            if ((bb.getWidth() > TOLERANCE_MIN_VIEWBOX_WIDTH) || (bb.getHeight() > TOLERANCE_MIN_VIEWBOX_WIDTH)) {
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
     * Draw the photo icon
     * 
     * @param c the Canvas
     * @param bb the current ViewBox
     * @param w map width
     * @param h map height
     * @param p the Photo
     * @param i the icon Drawable
     */
    public void drawIcon(Canvas c, ViewBox bb, int w, int h, Photo p, Drawable i) {
        int x = (int) GeoMath.lonE7ToX(w, bb, p.getLon());
        int y = (int) GeoMath.latE7ToY(h, w, bb, p.getLat());
        i.setBounds(new Rect(x - w2, y - h2, x + w2, y + h2));
        if (p.hasDirection()) {
            c.rotate(p.getDirection(), x, y);
            i.draw(c);
            c.rotate(-p.getDirection(), x, y);
        } else {
            i.draw(c);
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public List<Photo> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<Photo> result = new ArrayList<>();
        final float tolerance = DataStyle.getCurrent().getNodeToleranceValue();
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
        Context context = map.getContext();
        Resources resources = context.getResources();
        try {
            Uri photoUri = photo.getRefUri(context);
            if (photoUri != null) {
                Preferences prefs = map.getPrefs();
                if (prefs.useInternalPhotoViewer()) {
                    ArrayList<String> uris = new ArrayList<>();
                    int position = 0;
                    for (int i = 0; i < photos.size(); i++) {
                        Photo p = photos.get(i);
                        Uri uri = p.getRefUri(context);
                        if (uri != null) {
                            uris.add(uri.toString());
                            if (photo.equals(p)) {
                                position = i;
                            }
                        } else {
                            Log.e(DEBUG_TAG, "Null URI at position " + i);
                        }
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        PhotoViewerFragment.showDialog(activity, uris, position, null);
                    } else {
                        PhotoViewerActivity.start(activity, uris, position);
                    }
                } else {
                    Util.startExternalPhotoViewer(context, photoUri);
                }
                selected = photo;
                invalidate();
            } else {
                Log.d(DEBUG_TAG, "onSelected null Uri");
                Snack.toastTopError(context, resources.getString(R.string.toast_error_accessing_photo, photo.getRef()));
            }
        } catch (SecurityException ex) {
            Log.d(DEBUG_TAG, "onSelected security exception starting intent: " + ex);
            Snack.toastTopError(context, resources.getString(R.string.toast_security_error_accessing_photo, photo.getRef()));
        } catch (Exception ex) {
            Log.d(DEBUG_TAG, "onSelected exception starting intent: " + ex);
            ACRAHelper.nocrashReport(ex, "onSelected exception starting intent");
        }
    }

    @Override
    public String getDescription(Photo photo) {
        return photo.getDisplayName();
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
        if (!indexed && !indexing && !indexer.isExecuting()) {
            indexer.execute(handler);
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
                PhotoIndex.addToIndex(pi.addPhoto(context, uri, ContentProviderUtil.getDisplaynameColumn(context, uri)));
            } else {
                pi.deletePhoto(context, uri);
            }
            MapOverlay.this.invalidate();
        }
    }
}
