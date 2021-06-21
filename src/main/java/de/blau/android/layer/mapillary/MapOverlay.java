package de.blau.android.layer.mapillary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

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
import de.blau.android.resources.symbols.Mapillary;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.DataStorage;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.GeoJson;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SerializablePaint;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.util.rtree.RTree;
import de.blau.android.views.IMapView;
import de.blau.android.views.layers.MapTilesOverlayLayer;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapOverlay extends de.blau.android.layer.mvt.MapOverlay {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    /**
     * when reading state lockout writing/reading
     */
    // private transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();

    public static final String APIKEY_KEY = "MAPILLARY_APIKEY";

    public static final String FILENAME         = "mapillary.res";
    public static final String SET_POSITION_KEY = "set_position";
    private static final int   SHOW_MARKER_ZOOM = 20;

    private MapillarySequence            selectedSequence = null;
    private int                          selectedImage    = 0;
    private transient Paint              selectedPaint;
    private final transient Path         path             = new Path();
    private transient FloatPrimitiveList points           = new FloatPrimitiveList();
    private String                       apiKey;

    /** Map this is an overlay of. */
    private transient Map map = null;

    /**
     * Download related stuff
     */
    private long   cacheSize             = 100000000L;
    private String mapillaryApiUrl       = Urls.DEFAULT_MAPILLARY_API_V3;
    private String mapillaryImagesUrl    = "https://graph.mapillary.com/%s?access_token=MLY|4089802601107022|4c604fd248fc3284d97630775058e7d3&fields=thumb_2048_url";
    private String mapillarySequencesUrl = "https://graph.mapillary.com/image_ids?sequence_id=%s&access_token=MLY|4089802601107022|4c604fd248fc3284d97630775058e7d3&fields=id";

    private transient ThreadPoolExecutor mThreadPool;

    /**
     * Directory for caching mapillary images
     */
    private File cacheDir;

    private java.util.Map<String, ArrayList<String>> sequenceCache = new HashMap<>();

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    public MapOverlay(final Map map) {
        super(map, new VectorTileRenderer(), false);
        this.setRendererInfo(TileLayerSource.get(map.getContext(), "NEW-MAPILLARY", false));
        this.map = map;
        // resetStyling();
        // FeatureStyle selectedStyle = DataStyle.getInternal(DataStyle.SELECTED_WAY);
        // selectedPaint = new Paint(selectedStyle.getPaint());
        // selectedPaint.setStrokeWidth(paint.getStrokeWidth() * selectedStyle.getWidthFactor());
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
        setPrefs(map.getPrefs());
    }

    // @Override
    // protected void onDrawFinished(Canvas c, IMapView osmv) {
    // // do nothing
    // }
    //
    // @Override
    // public void onDestroy() {
    // data = null;
    // boxes = null;
    // }
    //
    // @Override
    // public synchronized boolean save(Context context) throws IOException {
    // Log.e(DEBUG_TAG, "saving state");
    // return savingHelper.save(context, FILENAME, this, true);
    // }
    //
    // @Override
    // public synchronized StyleableLayer load(Context context) {
    // Log.e(DEBUG_TAG, "loading state");
    // MapOverlay restoredOverlay = savingHelper.load(context, FILENAME, true);
    // if (restoredOverlay != null) {
    // data = restoredOverlay.data;
    // boxes = restoredOverlay.boxes;
    // selectedSequence = restoredOverlay.selectedSequence;
    // selectedImage = restoredOverlay.selectedImage;
    // }
    // return restoredOverlay;
    // }

    @Override
    public List<VectorTileDecoder.Feature> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<VectorTileDecoder.Feature> result = super.getClicked(x, y, viewBox);
        // remove non image elements for now
        if (result != null) {
            for (VectorTileDecoder.Feature f : new ArrayList<>(result)) {
                if (!GeoJSONConstants.POINT.equals(f.getGeometry().type())) {
                    result.remove(f);
                }
            }
        }
        Log.d(DEBUG_TAG, "getClicked found " + result.size());
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
    //
    // @Override
    // public BoundingBox getExtent() {
    // if (data != null) {
    // Collection<MapillarySequence> queryResult = new ArrayList<>();
    // data.query(queryResult);
    // BoundingBox extent = null;
    // for (MapillarySequence mo : queryResult) {
    // if (extent == null) {
    // extent = mo.getBounds();
    // } else {
    // extent.union(mo.getBounds());
    // }
    // }
    // return extent;
    // }
    // return null;
    // }

    @Override
    public void onSelected(FragmentActivity activity, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        if (f != null) {
            switch (f.getGeometry().type()) {
            case GeoJSONConstants.POINT:

                String sequenceId = (String) f.getAttributes().get("sequence_id");
                Long id = (Long) f.getAttributes().get("id");
                if (id != null && sequenceId != null) {
                    ArrayList<String> keys = sequenceCache.get(sequenceId);
                    if (keys == null) {
                        Thread t = new Thread(null, new SequenceFetcher(activity, sequenceId, id), "Mapillary Sequence");
                        t.start();
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            PhotoViewerFragment.showDialog(activity, keys, keys.indexOf(id.toString()),
                                    new MapillaryLoader(cacheDir, cacheSize, mapillaryImagesUrl));
                        } else {
                            MapillaryViewerActivity.start(activity, keys, keys.indexOf(id.toString()),
                                    new MapillaryLoader(cacheDir, cacheSize, mapillaryImagesUrl));
                        }
                        map.invalidate();
                    }
                }
                // }
            default:
            }
        }
    }

    class SequenceFetcher implements Runnable {
        final FragmentActivity activity;
        final String           sequenceId;
        final Long             id;

        public SequenceFetcher(FragmentActivity activity, String sequenceId, Long id) {
            this.activity = activity;
            this.sequenceId = sequenceId;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(String.format(mapillarySequencesUrl, sequenceId));
                Log.d(DEBUG_TAG, "query sequence: " + url.toString());

                Request request = new Request.Builder().url(url).build();
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(20000, TimeUnit.MILLISECONDS).readTimeout(20000, TimeUnit.MILLISECONDS)
                        .build();
                Call mapillaryCall = client.newCall(request);
                Response mapillaryCallResponse = mapillaryCall.execute();
                if (mapillaryCallResponse.isSuccessful()) {
                    ResponseBody responseBody = mapillaryCallResponse.body();
                    try (InputStream inputStream = responseBody.byteStream()) {
                        if (inputStream != null) {
                            StringBuilder sb = new StringBuilder();
                            int cp;
                            while ((cp = inputStream.read()) != -1) {
                                sb.append((char) cp);
                            }
                            JsonElement root = JsonParser.parseString(sb.toString());
                            if (root.isJsonObject()) {
                                JsonElement data = ((JsonObject) root).get("data");
                                if (data instanceof JsonArray) {
                                    JsonArray idArray = data.getAsJsonArray();
                                    ArrayList<String> ids = new ArrayList<>();
                                    for (JsonElement element : idArray) {
                                        if (element instanceof JsonObject) {
                                            JsonElement temp = ((JsonObject) element).get("id");
                                            if (temp != null) {
                                                ids.add(temp.getAsString());
                                            }
                                        }
                                    }
                                    sequenceCache.put(sequenceId, ids);
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                        PhotoViewerFragment.showDialog(activity, ids, ids.indexOf(id.toString()),
                                                new MapillaryLoader(cacheDir, cacheSize, mapillaryImagesUrl));
                                    } else {
                                        MapillaryViewerActivity.start(activity, ids, ids.indexOf(id.toString()),
                                                new MapillaryLoader(cacheDir, cacheSize, mapillaryImagesUrl));
                                    }
                                    map.invalidate();
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {

            }
        }

    }

    // @Override
    // public String getDescription(MapillaryImage image) {
    // return image.toString();
    // }
    //
    // @Override
    // public MapillaryImage getSelected() {
    // return selectedSequence != null ? selectedSequence.getImage(selectedImage) : null;
    // }
    //
    // @Override
    // public void deselectObjects() {
    // selectedSequence = null;
    // selectedImage = 0;
    // dirty();
    // }
    //
    // @Override
    // public void setSelected(MapillaryImage image) {
    // selectedSequence = get(image);
    // selectedImage = image.index;
    // dirty();
    // }

    /**
     * Select a specific image in the selected sequence
     * 
     * @param pos the position in the sequence
     */
    public synchronized void select(int pos) {
        if (selectedSequence != null) {
            JsonArray imageKeys = selectedSequence.getImageKeys();
            if (pos < imageKeys.size()) {
                selectedImage = pos;
                // dirty(); // need to save the new position
                List<Point> line = selectedSequence.getPoints();
                map.getViewBox().moveTo(map, (int) (line.get(pos).longitude() * 1E7), (int) (line.get(pos).latitude() * 1E7));
                map.invalidate();
            }
        }
    }

    @Override
    public void resetStyling() {
        // paint = new SerializablePaint(DataStyle.getInternal(DataStyle.GEOJSON_DEFAULT).getPaint());
        // iconRadius = map.getIconRadius();
        // symbolName = Mapillary.NAME;
        // symbolPath = DataStyle.getCurrent().getSymbol(Mapillary.NAME);
    }

    @Override
    public void setPrefs(Preferences prefs) {
        cacheSize = prefs.getMapillaryCacheSize() * 1000000L;
        mapillaryApiUrl = prefs.getMapillaryApiUrl();
        // mapillaryImagesUrl = prefs.getMapillaryImagesUrl();
    }

    @Override
    public LayerType getType() {
        return LayerType.MAPILLARY;
    }

    // @Override
    // protected void discardLayer(Context context) {
    // File originalFile = context.getFileStreamPath(FILENAME);
    // if (!originalFile.delete()) { // NOSONAR
    // Log.e(DEBUG_TAG, "Failed to delete state file " + FILENAME);
    // }
    // map.invalidate();
    // }
}