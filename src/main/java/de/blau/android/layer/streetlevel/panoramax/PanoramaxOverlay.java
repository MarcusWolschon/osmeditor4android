package de.blau.android.layer.streetlevel.panoramax;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.DateRangeDialog;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.streetlevel.AbstractImageOverlay;
import de.blau.android.layer.streetlevel.AbstractSequenceFetcher;
import de.blau.android.layer.streetlevel.DateRangeInterface;
import de.blau.android.layer.streetlevel.ImageViewerActivity;
import de.blau.android.layer.streetlevel.SelectImageInterface;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.ViewBox;
import de.blau.android.photos.PhotoViewerFragment;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.symbols.Mapillary;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
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

public class PanoramaxOverlay extends AbstractImageOverlay {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PanoramaxOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = PanoramaxOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final String PANORAMAX_TILES_ID = "PANORAMAX";

    // mapbox gl style layer ids
    private static final String IMAGE_LAYER    = "pictures";
    private static final String SEQUENCE_LAYER = "sequences";

    // panoramax tile API constants
    private static final String TIMESTAMP_KEY   = "ts";
    private static final String ID_KEY          = "id";
    private static final String SEQUENCE_ID_KEY = "first_sequence";

    private static final String DEFAULT_PANORAMAX_STYLE_JSON = "panoramax-style.json";

    /** this is the format used by panoramax */
    private final SimpleDateFormat dateFormat = DateFormatter.getUtcFormat("yyyy-MM-dd");

    private static final String API_PICTURES          = "api/pictures/%s/hd.jpg";
    private static final String API_COLLECTIONS_ITEMS = "api/collections/%s/items";
    private String              panoramaxImagesUrl    = Urls.DEFAULT_PANORAMAX_API_URL + API_PICTURES;
    private String              panoramaxSequencesUrl = Urls.DEFAULT_PANORAMAX_API_URL + API_COLLECTIONS_ITEMS;

    public static final String FILENAME = "panoramax" + "." + FileExtensions.RES;

    static class State implements Serializable {
        private static final long serialVersionUID = 1L;

        private String                                         sequenceId    = null;
        private String                                         imageId       = null;
        private final java.util.Map<String, ArrayList<String>> sequenceCache = new HashMap<>();
        private long                                           startDate     = 0L;
        private long                                           endDate       = new Date().getTime();
    }

    private State                     state        = new State();
    private final SavingHelper<State> savingHelper = new SavingHelper<>();

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    public PanoramaxOverlay(@NonNull final Map map) {
        super(map, PANORAMAX_TILES_ID, IMAGE_LAYER, DEFAULT_PANORAMAX_STYLE_JSON);
        setDateRange(state.startDate, state.endDate);
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        super.onSaveState(ctx);
        savingHelper.save(ctx, FILENAME, state, false);
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        boolean result = super.onRestoreState(ctx);
        if (state == null) {
            state = savingHelper.load(ctx, FILENAME, true);
            if (state != null) {
                setSelected(state.imageId);
                setDateRange(state.startDate, state.endDate);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_panoramax);
    }

    @Override
    public void onSelected(FragmentActivity activity, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        if (isPoint(f)) {
            // we ignore anything except the images for now
            java.util.Map<String, Object> attributes = f.getAttributes();
            String sequenceId = (String) attributes.get(SEQUENCE_ID_KEY);
            String id = (String) attributes.get(ID_KEY);
            if (id != null && sequenceId != null) {
                ArrayList<String> keys = state != null ? state.sequenceCache.get(sequenceId) : null;
                if (keys == null) {
                    try {
                        Thread t = new Thread(null, new PanoramaxSequenceFetcher(activity, panoramaxSequencesUrl, sequenceId, id), "Panoramax Sequence");
                        t.start();
                    } catch (SecurityException | IllegalThreadStateException e) {
                        Log.e(DEBUG_TAG, "Unable to run SequenceFetcher " + e.getMessage());
                        return;
                    }
                } else {
                    showImages(activity, id, keys);
                }
                setSelected(f);
                state.sequenceId = sequenceId;
            }
        }
    }

    /**
     * Start the image viewer
     * 
     * @param activity the calling activity
     * @param id id of the image
     * @param ids list of all ids in the sequence
     */
    private void showImages(@NonNull FragmentActivity activity, @NonNull String id, @NonNull ArrayList<String> ids) {
        int pos = ids.indexOf(id);
        if (pos >= 0 && cacheDir != null) {
            String imagesUrl = String.format(panoramaxImagesUrl, "%s");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                PhotoViewerFragment.showDialog(activity, ids, pos, new PanoramaxLoader(cacheDir, cacheSize, imagesUrl, ids));
            } else {
                ImageViewerActivity.start(activity, ids, pos, new PanoramaxLoader(cacheDir, cacheSize, imagesUrl, ids));
            }
            activity.runOnUiThread(() -> map.invalidate());
        } else {
            Log.e(DEBUG_TAG, "image id " + id + " not found in sequence");
        }
    }

    /**
     * Runnable that will fetch a sequence from the Panoramax API
     * 
     * @author simon
     *
     */
    private class PanoramaxSequenceFetcher extends AbstractSequenceFetcher {

        private static final String FEATURES_KEY = "features";

        private final String id;

        /**
         * Construct a new instance
         * 
         * @param activity the calling Activity
         * @param sequenceId the sequence id
         * @param id the image id
         */
        public PanoramaxSequenceFetcher(@NonNull FragmentActivity activity, @NonNull String urlTemplate, @NonNull String sequenceId, @NonNull String id) {
            super(activity, urlTemplate, sequenceId, null);
            this.id = id;
        }

        @Override
        protected void saveIdsAndUpdate(ArrayList<String> ids) {
            if (state == null) {
                state = new State();
            }
            state.sequenceCache.put(sequenceId, ids);
            showImages(activity, id, ids);

        }

        @Override
        protected ArrayList<String> getIds(JsonElement root) throws IOException {
            JsonElement features = ((JsonObject) root).get(FEATURES_KEY);
            if (!(features instanceof JsonArray)) {
                throw new IOException("features not a JsonArray");
            }
            JsonArray featuresArray = features.getAsJsonArray();
            ArrayList<String> ids = new ArrayList<>();
            for (JsonElement element : featuresArray) {
                if (element instanceof JsonObject) {
                    JsonElement temp = ((JsonObject) element).get(ID_KEY);
                    if (temp != null) {
                        ids.add(temp.getAsString());
                    }
                }
            }
            return ids;
        }
    }

    @Override
    public void deselectObjects() {
        state = null;
        setSelected((String) null);
        dirty();
    }

    @Override
    public void setSelected(de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        setSelected((String) f.getAttributes().get(ID_KEY));
    }

    /**
     * Set a image with a specific id as selected
     * 
     * This manipulates the filter in the style
     * 
     * @param id the image id
     */
    void setSelected(@Nullable String id) {
        if (selectedFilter == null) {
            Style style = ((VectorTileRenderer) tileRenderer).getStyle();
            Layer layer = style.getLayer(SELECTED_IMAGE_LAYER);
            if (layer instanceof Symbol) {
                selectedFilter = layer.getFilter();
            }
        }
        if (selectedFilter != null && selectedFilter.size() == 3) {
            if (state == null) {
                state = new State();
            }
            state.imageId = id;
            selectedFilter.set(2, id != null ? new JsonPrimitive(id) : null);
            map.invalidate();
            dirty();
        }
    }

    @Override
    public void selectImage(int pos) {
        synchronized (this) {
            if (state != null && state.sequenceId != null) {
                List<String> ids = state.sequenceCache.get(state.sequenceId);
                if (ids != null) {
                    String id = ids.get(pos);
                    if (id != null) {
                        setSelected(id);
                        return;
                    }
                }
                Log.e(DEBUG_TAG, "position " + pos + " not found in sequence " + state.sequenceId);
            }
        }
    }

    @Override
    public void setDateRange(long start, long end) {
        state.startDate = start;
        state.endDate = end;
        Style style = ((VectorTileRenderer) tileRenderer).getStyle();
        setDateRange(style, IMAGE_LAYER, start, end, timestampFormat);
        setDateRange(style, SEQUENCE_LAYER, start, end, dateFormat);
        map.invalidate();
        dirty();
    }

    @Override
    protected void setDateFilterValue(@NonNull JsonElement filter, long value, @Nullable SimpleDateFormat format) {
        if (filter instanceof JsonArray && ((JsonArray) filter).size() == 3 && format != null) {
            ((JsonArray) filter).set(2, new JsonPrimitive(format.format(value)));
        }
    }

    @Override
    public SpannableString getDescription(@NonNull Context context, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        return new SpannableString(context.getString(R.string.mapillary_image, f.getAttributes().get(TIMESTAMP_KEY)));
    }

    @Override
    protected void flushSequenceCache(@NonNull Context ctx) {
        if (state != null) {
            state.imageId = null;
            state.sequenceCache.clear();
            state.sequenceId = null;
            savingHelper.save(ctx, FILENAME, state, false);
        }
    }

    @Override
    public void setPrefs(Preferences prefs) {
        cacheSize = prefs.getMapillaryCacheSize() * ONE_MILLION;
        panoramaxSequencesUrl = prefs.getPanoramaxApiUrl() + API_COLLECTIONS_ITEMS;
        panoramaxImagesUrl = prefs.getPanoramaxApiUrl() + API_PICTURES;
    }

    @Override
    @NonNull
    public LayerType getType() {
        return LayerType.PANORAMAX;
    }

    /**
     * Show a dialog to set the displayed date range
     * 
     * @param activity the activity we are being shown on
     * @param layerIndex the index of this layer
     */
    @Override
    public void selectDateRange(@NonNull FragmentActivity activity, int layerIndex) {
        DateRangeDialog.showDialog(activity, layerIndex, state.startDate, state.endDate);
    }
}