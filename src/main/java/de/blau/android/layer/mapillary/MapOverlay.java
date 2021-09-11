package de.blau.android.layer.mapillary;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.ViewBox;
import de.blau.android.photos.MapillaryViewerActivity;
import de.blau.android.photos.PhotoViewerFragment;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.symbols.Mapillary;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.util.mvt.style.Layer;
import de.blau.android.util.mvt.style.Style;
import de.blau.android.util.mvt.style.Symbol;
import de.blau.android.views.IMapView;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapOverlay extends de.blau.android.layer.mvt.MapOverlay {

    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName();

    public static final String MAPILLARY_TILES_ID           = "MAPILLARYV4";
    public static final int    MAPILLARY_DEFAULT_MIN_ZOOM   = 16;
    public static final int    MAPILLARY_DEFAULT_CACHE_SIZE = 100;
    private static final long  ONE_MILLION                  = 1000000L;

    // mapbox gl style layer ids
    private static final String SELECTED_IMAGE_LAYER = "selected_image";
    private static final String IMAGE_LAYER          = "image";

    // mapillary API constants
    private static final String CAPTURED_AT_KEY = "captured_at";
    private static final String DATA_KEY        = "data";
    private static final String ID_KEY          = "id";
    private static final String SEQUENCE_ID_KEY = "sequence_id";

    private static final String DEFAULT_MAPILLARY_STYLE_JSON = "mapillary-style.json";

    private String mapillaryImagesUrl    = Urls.DEFAULT_MAPILLARY_IMAGES_V4;
    private String mapillarySequencesUrl = Urls.DEFAULT_MAPILLARY_SEQUENCES_URL_V4;

    public static final String APIKEY_KEY = "MAPILLARY_CLIENT_TOKEN";

    public static final String FILENAME         = "mapillary.res";
    public static final String SET_POSITION_KEY = "set_position";
    public static final String COORDINATES_KEY  = "coordinates";

    static class Selected implements Serializable {
        private static final long serialVersionUID = 2L;

        private String                                         sequenceId    = null;
        private long                                           imageId       = 0;
        private final java.util.Map<String, ArrayList<String>> sequenceCache = new HashMap<>();
    }

    private Selected                     selected     = new Selected();
    private final SavingHelper<Selected> savingHelper = new SavingHelper<>();

    private final String apiKey;

    /** Map this is an overlay of. */
    private Map map = null;

    /**
     * Download related stuff
     */
    private long cacheSize = MAPILLARY_DEFAULT_CACHE_SIZE * ONE_MILLION;

    private JsonArray selectedFilter = null;

    /**
     * Directory for caching mapillary images
     */
    private File cacheDir;

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    public MapOverlay(@NonNull final Map map) {
        super(map, new VectorTileRenderer(), false);
        this.setRendererInfo(TileLayerSource.get(map.getContext(), MAPILLARY_TILES_ID, false));
        this.map = map;
        final Context context = map.getContext();
        File[] storageDirs = ContextCompat.getExternalCacheDirs(context);
        try {
            cacheDir = FileUtil.getPublicDirectory(storageDirs.length > 1 && storageDirs[1] != null ? storageDirs[1] : storageDirs[0], getName());
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Unable to create cache directory " + e.getMessage());
        }
        try (KeyDatabaseHelper keys = new KeyDatabaseHelper(context); SQLiteDatabase db = keys.getReadableDatabase()) {
            apiKey = KeyDatabaseHelper.getKey(db, APIKEY_KEY);
        }
        setPrefs(map.getPrefs());

        resetStyling();
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull IMapView osmv) {
        if (map.getZoomLevel() >= MAPILLARY_DEFAULT_MIN_ZOOM) {
            super.onDraw(c, osmv);
        }
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        super.onSaveState(ctx);
        savingHelper.save(ctx, FILENAME, selected, false);
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        boolean result = super.onRestoreState(ctx);
        if (selected == null) {
            selected = savingHelper.load(ctx, FILENAME, true);
            if (selected != null) {
                setSelected(selected.imageId);
            }
        }
        return result;
    }

    @Override
    public List<VectorTileDecoder.Feature> getClicked(final float x, final float y, final ViewBox viewBox) {
        Style style = ((VectorTileRenderer) tileRenderer).getStyle();
        Layer layer = style.getLayer(IMAGE_LAYER);
        if (layer instanceof Symbol && map.getZoomLevel() < layer.getMinZoom()) {
            return new ArrayList<>();
        }
        List<VectorTileDecoder.Feature> result = super.getClicked(x, y, viewBox);
        // remove non image elements for now
        if (result != null) {
            for (VectorTileDecoder.Feature f : new ArrayList<>(result)) {
                if (!isPoint(f)) {
                    result.remove(f);
                }
            }
        }
        Log.d(DEBUG_TAG, "getClicked found " + (result != null ? result.size() : "nothing"));
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
    public void onSelected(FragmentActivity activity, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        if (isPoint(f)) {
            // we ignore anything except the images for now
            java.util.Map<String, Object> attributes = f.getAttributes();
            String sequenceId = (String) attributes.get(SEQUENCE_ID_KEY);
            Long id = (Long) attributes.get(ID_KEY);
            if (id != null && sequenceId != null) {
                ArrayList<String> keys = selected != null ? selected.sequenceCache.get(sequenceId) : null;
                if (keys == null) {
                    try {
                        Thread t = new Thread(null, new SequenceFetcher(activity, sequenceId, id), "Mapillary Sequence");
                        t.start();
                    } catch (SecurityException | IllegalThreadStateException e) {
                        Log.e(DEBUG_TAG, "Unable to run SequenceFetcher " + e.getMessage());
                        return;
                    }
                } else {
                    showImages(activity, id, keys);
                }
                setSelected(f);
                selected.sequenceId = sequenceId;
            }
        }
    }

    /**
     * Check if this Feature geometry is a Point
     * 
     * @param f the Feature
     * @return true if the geometry is a Point
     */
    private boolean isPoint(@NonNull de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        return GeoJSONConstants.POINT.equals(f.getGeometry().type());
    }

    /**
     * Start the image viewer
     * 
     * @param activity the calling activity
     * @param id id of the image
     * @param ids list of all ids in the sequence
     */
    private void showImages(@NonNull FragmentActivity activity, @NonNull Long id, @NonNull ArrayList<String> ids) {
        int pos = ids.indexOf(id.toString());
        if (pos >= 0 && cacheDir != null) {
            String imagesUrl = String.format(mapillaryImagesUrl, "%s", apiKey);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                PhotoViewerFragment.showDialog(activity, ids, pos, new MapillaryLoader(cacheDir, cacheSize, imagesUrl, ids));
            } else {
                MapillaryViewerActivity.start(activity, ids, pos, new MapillaryLoader(cacheDir, cacheSize, imagesUrl, ids));
            }
            activity.runOnUiThread(() -> map.invalidate());
        } else {
            Log.e(DEBUG_TAG, "image id " + id + " not found in sequence");
        }
    }

    /**
     * Runnable that will fetch a sequence from the Mapillary API
     * 
     * @author simon
     *
     */
    class SequenceFetcher implements Runnable {

        final FragmentActivity activity;
        final String           sequenceId;
        final Long             id;

        /**
         * Construct a new instance
         * 
         * @param activity the calling Activity
         * @param sequenceId the sequence id
         * @param id the image id
         */
        public SequenceFetcher(@NonNull FragmentActivity activity, @NonNull String sequenceId, @NonNull Long id) {
            this.activity = activity;
            this.sequenceId = sequenceId;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(String.format(mapillarySequencesUrl, sequenceId, apiKey));
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
                                JsonElement data = ((JsonObject) root).get(DATA_KEY);
                                if (data instanceof JsonArray) {
                                    JsonArray idArray = data.getAsJsonArray();
                                    ArrayList<String> ids = new ArrayList<>();
                                    for (JsonElement element : idArray) {
                                        if (element instanceof JsonObject) {
                                            JsonElement temp = ((JsonObject) element).get(ID_KEY);
                                            if (temp != null) {
                                                ids.add(temp.getAsString());
                                            }
                                        }
                                    }
                                    if (selected == null) {
                                        selected = new Selected();
                                    }
                                    selected.sequenceCache.put(sequenceId, ids);
                                    showImages(activity, id, ids);
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                Log.d(DEBUG_TAG, "query sequence failed with " + ex.getMessage());
            }
        }
    }

    @Override
    public void deselectObjects() {
        selected = null;
        setSelected(0);
        dirty();
    }

    @Override
    public void setSelected(de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        setSelected(((Long) f.getAttributes().get(ID_KEY)));
    }

    /**
     * Set a image with a specific id as selected
     * 
     * This manipulates the filter in the style
     * 
     * @param id the image id
     */
    void setSelected(long id) {
        if (selectedFilter == null) {
            Style style = ((VectorTileRenderer) tileRenderer).getStyle();
            Layer layer = style.getLayer(SELECTED_IMAGE_LAYER);
            if (layer instanceof Symbol) {
                selectedFilter = layer.getFilter();
            }
        }
        if (selectedFilter != null && selectedFilter.size() == 3) {
            if (selected == null) {
                selected = new Selected();
            }
            selected.imageId = id;
            selectedFilter.set(2, new JsonPrimitive(id));
            map.invalidate();
            dirty();
        }
    }

    /**
     * Select a specific image in the selected sequence
     * 
     * @param pos the position in the sequence
     */
    public synchronized void select(int pos) {
        if (selected != null && selected.sequenceId != null) {
            List<String> ids = selected.sequenceCache.get(selected.sequenceId);
            long id = Long.parseLong(ids.get(pos));
            setSelected(id);
        }
    }

    /**
     * Flush both the sequence and image cache
     * 
     * @param ctx an Android Context
     */
    public synchronized void flushCaches(@NonNull Context ctx) {
        flushSequenceCache(ctx);
        try {
            Thread t = new Thread(null, () -> FileUtil.pruneCache(cacheDir, 0), "Mapillary Image Cache Zapper");
            t.start();
        } catch (SecurityException | IllegalThreadStateException e) {
            Log.e(DEBUG_TAG, "Unable to flush image cache " + e.getMessage());
        }
    }

    /**
     * Flush the sequence cache
     * 
     * @param ctx an Android Context
     */
    private void flushSequenceCache(@NonNull Context ctx) {
        if (selected != null) {
            selected.imageId = 0;
            selected.sequenceCache.clear();
            selected.sequenceId = null;
            savingHelper.save(ctx, FILENAME, selected, false);
        }
    }

    @Override
    public String getDescription(de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        Long capturedAt = (Long) f.getAttributes().get(CAPTURED_AT_KEY);
        return map.getContext().getString(R.string.mapillary_image, DateFormatter.getUtcFormat(OsmParser.TIMESTAMP_FORMAT).format(capturedAt));
    }

    @Override
    public void resetStyling() {
        try (InputStream is = map.getContext().getAssets().open(DEFAULT_MAPILLARY_STYLE_JSON)) {
            Style style = new Style();
            style.loadStyle(map.getContext(), is);
            ((VectorTileRenderer) tileRenderer).setStyle(style);
            Layer layer = style.getLayer(IMAGE_LAYER);
            if (layer instanceof Symbol) {
                ((Symbol) layer).setSymbol(Mapillary.NAME);
                layer.setColor(layer.getColor());
            }
            layer = style.getLayer(SELECTED_IMAGE_LAYER);
            if (layer instanceof Symbol) {
                ((Symbol) layer).setSymbol(Mapillary.NAME);
                layer.setColor(layer.getColor());
                selectedFilter = layer.getFilter();
            }
            dirty();
            Log.d(DEBUG_TAG, "Loaded  successfully");
        } catch (IOException ioex) {
            Log.d(DEBUG_TAG, "Reading default mapillary style failed");
        }
    }

    @Override
    public void flushTileCache(@Nullable final FragmentActivity activity, boolean all) {
        super.flushTileCache(activity, all);
        if (activity != null) {
            flushSequenceCache(activity);
        }
    }

    @Override
    public void setPrefs(Preferences prefs) {
        cacheSize = prefs.getMapillaryCacheSize() * ONE_MILLION;
        mapillarySequencesUrl = prefs.getMapillarySequencesUrlV4();
        mapillaryImagesUrl = prefs.getMapillaryImagesUrlV4();
    }

    @Override
    @NonNull
    public LayerType getType() {
        return LayerType.MAPILLARY;
    }

    @Override
    public int onDrawAttribution(@NonNull Canvas c, @NonNull IMapView osmv, int offset) {
        return offset;
    }
}