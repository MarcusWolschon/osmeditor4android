package de.blau.android.layer.mapillary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.DownloadInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.ViewBox;
import de.blau.android.photos.MapillaryViewerActivity;
import de.blau.android.photos.PhotoViewerFragment;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.util.DataStorage;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.GeoJson;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.util.rtree.RTree;
import de.blau.android.views.IMapView;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapOverlay extends StyleableLayer
        implements Serializable, ExtentInterface, DiscardInterface, ClickableInterface<MapillaryImage>, DownloadInterface, PruneableInterface, DataStorage {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    /**
     * when reading state lockout writing/reading
     */
    private transient ReentrantLock            readingLock  = new ReentrantLock();
    private transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();
    private transient boolean                  saved        = false;

    public static final String APIKEY_KEY = "MAPILLARY_APIKEY";

    public static final String FILENAME         = "mapillary.res";
    public static final String SET_POSITION_KEY = "set_position";
    private static final int   SHOW_MARKER_ZOOM = 20;

    private RTree<MapillarySequence>     data             = new RTree<>(2, 12);
    private List<BoundingBox>            boxes            = new ArrayList<>();
    private MapillarySequence            selectedSequence = null;
    private int                          selectedImage    = 0;
    private transient Paint              paint;
    private transient Paint              selectedPaint;
    private final transient Path         path             = new Path();
    private transient FloatPrimitiveList points           = new FloatPrimitiveList();
    private String                       apiKey;

    /** Map this is an overlay of. */
    private transient Map map = null;

    /**
     * Styling parameters
     */
    private int   iconRadius;
    private int   color;
    private float strokeWidth;

    /**
     * Download related stuff
     */
    private boolean panAndZoomDownLoad = false;
    private int     panAndZoomLimit    = 16;
    private int     minDownloadSize    = 50;
    private float   maxDownloadSpeed   = 30;
    private long    cacheSize          = 100000000L;

    private transient ThreadPoolExecutor mThreadPool;

    /**
     * Name for this layer (typically the file name)
     */
    private String name;

    /**
     * Directory for caching mapillary images
     */
    private File cacheDir;

    /**
     * Runnable for downloading data
     * 
     * There is some code duplication here, however attempts to merge this didn't work out
     */
    private transient Runnable download = () -> {
        if (mThreadPool == null) {
            mThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        }
        List<BoundingBox> bbList = new ArrayList<>(boxes);
        ViewBox box = new ViewBox(map.getViewBox());
        box.scale(1.2); // make
                        // sides
                        // 20%
                        // larger
        box.ensureMinumumSize(minDownloadSize); // enforce a minimum size
        List<BoundingBox> bboxes = BoundingBox.newBoxes(bbList, box);
        for (BoundingBox b : bboxes) {
            if (b.getWidth() <= 1 || b.getHeight() <= 1) {
                Log.w(DEBUG_TAG, "getNextCenter very small bb " + b.toString());
                continue;
            }
            addBoundingBox(b);
            mThreadPool.execute(() -> downloadBox(map.getContext(), b, new PostAsyncActionHandler() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSuccess() {
                    map.postInvalidate();
                }

                @Override
                public void onError() {
                    deleteBoundingBox(b);
                }
            }));
        }
    };

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    public MapOverlay(final Map map) {
        this.map = map;
        resetStyling();
        FeatureStyle selectedStyle = DataStyle.getInternal(DataStyle.SELECTED_WAY);
        selectedPaint = new Paint(selectedStyle.getPaint());
        selectedPaint.setStrokeWidth(paint.getStrokeWidth() * selectedStyle.getWidthFactor());
        final Context context = map.getContext();
        File[] storageDirs = ContextCompat.getExternalFilesDirs(context, null);
        try {
            cacheDir = FileUtil.getPublicDirectory(storageDirs.length > 1 && storageDirs[1] != null ? storageDirs[1] : storageDirs[0], getName());
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Unable to create cache directory " + e.getMessage());
        }
        try (KeyDatabaseHelper keys = new KeyDatabaseHelper(context); SQLiteDatabase db = keys.getReadableDatabase()) {
            apiKey = KeyDatabaseHelper.getKey(db, APIKEY_KEY);
        }
    }

    @Override
    public boolean isReadyToDraw() {
        return data != null;
    }

    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (!isVisible || data == null) {
            return;
        }
        ViewBox bb = osmv.getViewBox();
        int width = map.getWidth();
        int height = map.getHeight();
        int zoomLevel = map.getZoomLevel();

        Location location = map.getLocation();

        if (zoomLevel >= panAndZoomLimit && panAndZoomDownLoad && (location == null || location.getSpeed() < maxDownloadSpeed)) {
            map.getRootView().removeCallbacks(download);
            map.getRootView().postDelayed(download, 100);
        }

        Collection<MapillarySequence> queryResult = new ArrayList<>();
        data.query(queryResult, bb);
        Log.d(DEBUG_TAG, "features result count " + queryResult.size());
        for (MapillarySequence sequence : queryResult) {
            Feature f = sequence.getFeature();
            drawSequence(canvas, bb, width, height, f, paint, zoomLevel >= SHOW_MARKER_ZOOM);
        }
    }

    /**
     * Draw a line for a sequence with optional markers for the images
     * 
     * @param canvas Canvas object we are drawing on
     * @param bb the current ViewBox
     * @param width screen width in screen coordinates
     * @param height screen height in screen coordinates
     * @param f the GeoJson Feature holding the sequence
     * @param paint Paint object for drawing
     * @param withMarker if true show the markers
     */
    public void drawSequence(@NonNull Canvas canvas, @NonNull ViewBox bb, int width, int height, @NonNull Feature f, @NonNull Paint paint, boolean withMarker) {
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        path.reset();
        @SuppressWarnings("unchecked")
        List<Point> line = ((CoordinateContainer<List<Point>>) f.geometry()).coordinates();
        JsonObject coordinateProperties = (JsonObject) f.getProperty(MapillarySequence.COORDINATE_PROPERTIES_KEY);
        JsonArray cas = coordinateProperties.get(MapillarySequence.CAS_KEY).getAsJsonArray();
        int size = line.size();
        Path mapillaryPath = DataStyle.getCurrent().getMapillaryPath();
        GeoJson.pointListToLinePointsArray(bb, width, height, points, line);
        float[] linePoints = points.getArray();
        int pointsSize = points.size();
        if (pointsSize > 2) {
            path.reset();
            path.moveTo(linePoints[0], linePoints[1]);
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePoints[i + 2], linePoints[i + 3]);
            }
            canvas.drawPath(path, paint);
        }
        if (withMarker) {
            JsonArray imageKeys = coordinateProperties.get(MapillarySequence.IMAGE_KEYS_KEY).getAsJsonArray();
            if (imageKeys != null) {
                try {
                    for (int i = 0; i < imageKeys.size(); i++) {
                        Point p = line.get(i);
                        double longitude = p.longitude();
                        double latitude = p.latitude();
                        if (bb.contains(longitude, latitude)) {
                            float x = GeoMath.lonToX(width, bb, longitude);
                            float y = GeoMath.latToY(height, width, bb, latitude);
                            if (selectedImage == i) {
                                drawMarker(canvas, x, y, cas.get(i).getAsInt(), selectedPaint, mapillaryPath);
                            }
                            drawMarker(canvas, x, y, cas.get(i).getAsInt(), paint, mapillaryPath);
                        }
                    }
                } catch (Exception ex) {
                    Log.e(DEBUG_TAG, "seq key " + f.getProperty("key") + " #coordinates " + size + " #keys " + (imageKeys != null ? imageKeys.size() : "null")
                            + ex.getMessage());
                }
            }
        }
    }

    /**
     * Show a marker for the current GPS position
     * 
     * @param canvas canvas to draw on
     * @param x screen x
     * @param y screen y
     * @param o cardinal orientation in degrees
     * @param paint Paint object to use
     * @param path Path for the marker
     */
    private void drawMarker(@NonNull final Canvas canvas, float x, float y, float o, @NonNull Paint paint, @NonNull Path path) {
        if (o < 0) {
            // no orientation data available
            canvas.drawCircle(x, y, paint.getStrokeWidth(), paint);
        } else {
            // show the orientation using a pointy indicator
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(o);
            canvas.drawPath(path, paint);
            canvas.restore();
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public void onDestroy() {
        data = null;
    }

    /**
     * Stores the current state to the default storage file
     * 
     * @param context Android Context
     * @throws IOException on errors writing the file
     */
    @Override
    public synchronized void onSaveState(@NonNull Context context) throws IOException {
        super.onSaveState(context);
        if (saved) {
            Log.i(DEBUG_TAG, "state not dirty, skipping save");
            return;
        }
        if (readingLock.tryLock()) {
            try {
                // TODO this doesn't really help with error conditions need to throw exception
                if (savingHelper.save(context, FILENAME, this, true)) {
                    saved = true;
                } else {
                    // this is essentially catastrophic and can only happen if something went really wrong
                    // running out of memory or disk, or HW failure
                    if (context instanceof Activity) {
                        Snack.barError((Activity) context, R.string.toast_statesave_failed);
                    }
                }
            } finally {
                readingLock.unlock();
            }
        } else {
            Log.i(DEBUG_TAG, "bug state being read, skipping save");
        }
    }

    /**
     * Loads any saved state from the default storage file
     * 
     * 
     * @param context Android context
     * @return true if the saved state was successfully read
     */
    @Override
    public synchronized boolean onRestoreState(@NonNull Context context) {
        super.onRestoreState(context);
        try {
            readingLock.lock();
            if (data != null && data.count() > 0) {
                // don't restore over existing data
                return true;
            }
            // disable drawing
            setVisible(false);
            MapOverlay restoredOverlay = savingHelper.load(context, FILENAME, true);
            if (restoredOverlay != null) {
                Log.d(DEBUG_TAG, "read saved state");
                data = restoredOverlay.data;
                iconRadius = restoredOverlay.iconRadius;
                color = restoredOverlay.color;
                paint.setColor(color);
                strokeWidth = restoredOverlay.strokeWidth;
                paint.setStrokeWidth(strokeWidth);
                name = restoredOverlay.name;
                selectedSequence = restoredOverlay.selectedSequence;
                selectedImage = restoredOverlay.selectedImage;
                return true;
            } else {
                Log.d(DEBUG_TAG, "saved state null");
                return false;
            }
        } finally {
            // re-enable drawing
            setVisible(true);
            readingLock.unlock();
        }
    }

    /**
     * Given screen coordinates, find all nearby elements.
     *
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
     * @param viewBox Map view box.
     * @return List of photos close to given location.
     */
    @Override
    public List<MapillaryImage> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<MapillaryImage> result = new ArrayList<>();
        Log.d(DEBUG_TAG, "getClicked");
        if (data != null) {
            final float tolerance = DataStyle.getCurrent().getNodeToleranceValue();
            Collection<MapillarySequence> queryResult = new ArrayList<>();
            data.query(queryResult, viewBox);
            Log.d(DEBUG_TAG, "features result count " + queryResult.size());
            for (MapillarySequence sequence : queryResult) {
                Feature f = sequence.getFeature();
                Geometry g = f.geometry();
                if (g == null) {
                    continue;
                }
                if (GeoJSONConstants.LINESTRING.equals(g.type())) {
                    List<Point> line = sequence.getPoints();
                    JsonArray imageKeys = sequence.getImageKeys();
                    for (int i = 0; i < imageKeys.size(); i++) {
                        if (inToleranceArea(viewBox, tolerance, line.get(i), x, y)) {
                            result.add(sequence.getImage(i));
                        }
                    }
                } else {
                    Log.w(DEBUG_TAG, "Unexpected geometry " + g.type());
                }
            }
        }
        Log.d(DEBUG_TAG, "getClicked found " + result.size());
        return result;
    }

    /**
     * Check if the current touch position is in the tolerance area around a Position
     * 
     * @param viewBox the current screen ViewBox
     * @param tolerance the tolerance value
     * @param p the Position
     * @param x screen x coordinate of touch location
     * @param y screen y coordinate of touch location
     * @return true if touch position is in tolerance
     */
    private boolean inToleranceArea(@NonNull ViewBox viewBox, float tolerance, @NonNull Point p, float x, float y) {
        float differenceX = Math.abs(GeoMath.lonToX(map.getWidth(), viewBox, p.longitude()) - x);
        float differenceY = Math.abs(GeoMath.latToY(map.getHeight(), map.getWidth(), viewBox, p.latitude()) - y);
        return differenceX <= tolerance && differenceY <= tolerance && Math.hypot(differenceX, differenceY) <= tolerance;
    }

    /**
     * Return a List of all loaded Features
     * 
     * @return a List of Feature objects
     */
    public List<Feature> getFeatures() {
        Collection<MapillarySequence> queryResult = new ArrayList<>();
        data.query(queryResult);
        List<Feature> result = new ArrayList<>();
        for (MapillarySequence mo : queryResult) {
            result.add(mo.getFeature());
        }
        return result;
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_mapillary);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        if (data != null) {
            Collection<MapillarySequence> queryResult = new ArrayList<>();
            data.query(queryResult);
            BoundingBox extent = null;
            for (MapillarySequence mo : queryResult) {
                if (extent == null) {
                    extent = mo.getBounds();
                } else {
                    extent.union(mo.getBounds());
                }
            }
            return extent;
        }
        return null;
    }

    @Override
    public void discard(Context context) {
        if (readingLock.tryLock()) {
            try {
                data = null;
                File originalFile = context.getFileStreamPath(FILENAME);
                if (!originalFile.delete()) { // NOSONAR
                    Log.e(DEBUG_TAG, "Failed to delete state file " + FILENAME);
                }
            } finally {
                readingLock.unlock();
            }
        }
        map.invalidate();
    }

    @Override
    public void onSelected(FragmentActivity activity, MapillaryImage image) {
        if (image != null) {
            MapillarySequence sequence = get(image);
            selectedSequence = sequence;
            selectedImage = image.index;
            if (sequence != null) {
                ArrayList<String> keys = new ArrayList<>();
                JsonArray imageKeys = sequence.getImageKeys();
                for (int i = 0; i < imageKeys.size(); i++) {
                    keys.add(imageKeys.get(i).getAsString());
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    PhotoViewerFragment.showDialog(activity, keys, image.index, new MapillaryLoader(cacheDir, cacheSize));
                } else {
                    MapillaryViewerActivity.start(activity, keys, image.index, new MapillaryLoader(cacheDir, cacheSize));
                }
                map.invalidate();
            }
        }
    }

    @Override
    public String getDescription(MapillaryImage image) {
        return image.toString();
    }

    @Override
    public MapillaryImage getSelected() {
        return selectedSequence != null ? selectedSequence.getImage(selectedImage) : null;
    }

    @Override
    public void deselectObjects() {
        selectedSequence = null;
        selectedImage = 0;
    }

    @Override
    public void setSelected(MapillaryImage image) {
        selectedSequence = get(image);
        selectedImage = image.index;
    }

    @Override
    public void downloadBox(@NonNull final Context context, @NonNull final BoundingBox box, @Nullable final PostAsyncActionHandler handler) {

        box.makeValidForApi();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    URL url = new URL(Urls.MAPILLARY_API_V3 + "sequences?client_id=" + apiKey + "&bbox=" + box.getLeft() / 1E7d + "," + box.getBottom() / 1E7d
                            + "," + box.getRight() / 1E7d + "," + box.getTop() / 1E7d);
                    Log.d(DEBUG_TAG, "query: " + url.toString());

                    Request request = new Request.Builder().url(url).build();
                    OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(20000, TimeUnit.MILLISECONDS)
                            .readTimeout(20000, TimeUnit.MILLISECONDS).build();
                    Call mapillaryCall = client.newCall(request);
                    Response mapillaryCallResponse = mapillaryCall.execute();
                    if (mapillaryCallResponse.isSuccessful()) {
                        ResponseBody responseBody = mapillaryCallResponse.body();

                        StringBuilder sb = new StringBuilder();
                        try (InputStream inputStream = responseBody.byteStream();
                                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName(OsmXml.UTF_8)))) {
                            int cp;
                            while ((cp = rd.read()) != -1) {
                                sb.append((char) cp);
                            }
                        }
                        FeatureCollection fc = FeatureCollection.fromJson(sb.toString());
                        for (Feature f : fc.features()) {
                            MapillarySequence mo = new MapillarySequence(f);
                            if (!contains(mo)) {
                                data.insert(mo);
                            }
                        }
                    } else {
                        Log.e(DEBUG_TAG, "Sequence download failed " + mapillaryCallResponse.code() + " " + mapillaryCallResponse.message());
                        return null;
                    }
                } catch (Exception ex) {
                    Log.e(DEBUG_TAG, ex.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void param) {
                if (handler != null) {
                    handler.onSuccess();
                }
            }
        }.execute();
    }

    /**
     * Returns true if there is an object with the same key in the "same location"
     * 
     * @param sequence MapillarySequence to check for
     * @return true if sequence was found
     */
    public boolean contains(@NonNull MapillarySequence sequence) {
        Collection<MapillarySequence> queryResult = new ArrayList<>();
        data.query(queryResult, sequence.getBounds());
        Log.d(DEBUG_TAG, "candidates for contain " + queryResult.size());
        for (MapillarySequence mo2 : queryResult) {
            if (sequence.key.equals(mo2.key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given an MapillaryImage return the sequence
     * 
     * @param image the image
     * @return the sequence or null
     */
    @Nullable
    MapillarySequence get(@NonNull MapillaryImage image) {
        Collection<MapillarySequence> queryResult = new ArrayList<>();
        data.query(queryResult, image.box);
        for (MapillarySequence sequence : queryResult) {
            if (image.sequenceKey.equals(sequence.key)) {
                return sequence;
            }
        }
        return null;
    }

    /**
     * Select a specific image in the selected sequence
     * 
     * @param pos the position in the sequence
     */
    public void select(int pos) {
        if (selectedSequence != null) {
            JsonArray imageKeys = selectedSequence.getImageKeys();
            if (pos < imageKeys.size()) {
                selectedImage = pos;
                List<Point> line = selectedSequence.getPoints();
                map.getViewBox().moveTo(map, (int) (line.get(pos).longitude() * 1E7), (int) (line.get(pos).latitude() * 1E7));
                map.invalidate();
            }
        }
    }

    @Override
    public int getColor() {
        return paint.getColor();
    }

    @Override
    public void setColor(int color) {
        paint.setColor(color);
        this.color = color;
    }

    @Override
    public float getStrokeWidth() {
        return paint.getStrokeWidth();
    }

    @Override
    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
        strokeWidth = width;
    }

    @Override
    public Path getPointSymbol() {
        return null;
    }

    @Override
    public void setPointSymbol(Path symbol) {
        // Do nothing
    }

    @Override
    public void resetStyling() {
        paint = new Paint(DataStyle.getInternal(DataStyle.GEOJSON_DEFAULT).getPaint());
        color = paint.getColor();
        strokeWidth = paint.getStrokeWidth();
        iconRadius = map.getIconRadius();
    }

    @Override
    public List<String> getLabelList() {
        return null;
    }

    @Override
    public void setLabel(String key) {
        // Do nothing
    }

    @Override
    public void setPrefs(Preferences prefs) {
        panAndZoomDownLoad = prefs.getPanAndZoomAutoDownload();
        minDownloadSize = prefs.getBugDownloadRadius() * 2;
        maxDownloadSpeed = prefs.getMaxBugDownloadSpeed() / 3.6f;
        panAndZoomLimit = prefs.getPanAndZoomLimit();
        cacheSize = prefs.getMapillaryCacheSize() * 1000000L;
    }

    @Override
    public LayerType getType() {
        return LayerType.MAPILLARY;
    }

    @Override
    public void prune() {
        ViewBox pruneBox = new ViewBox(map.getViewBox());
        pruneBox.scale(1.2);
        prune(pruneBox);
    }

    @Override
    public List<BoundingBox> getBoundingBoxes() {
        return boxes;
    }

    @Override
    public void addBoundingBox(BoundingBox box) {
        synchronized (boxes) {
            boxes.add(box);
        }

    }

    @Override
    public void deleteBoundingBox(BoundingBox box) {
        synchronized (boxes) {
            boxes.remove(box);
        }
    }

    @Override
    public void prune(BoundingBox box) {
        synchronized (data) {
            Collection<MapillarySequence> queryResult = new ArrayList<>();
            data.query(queryResult);
            for (MapillarySequence s : queryResult) {
                if (!box.intersects(s.getBounds())) {
                    data.remove(s);
                }
            }
            BoundingBox.prune(this, box);
            saved = false;
        }
    }
}
