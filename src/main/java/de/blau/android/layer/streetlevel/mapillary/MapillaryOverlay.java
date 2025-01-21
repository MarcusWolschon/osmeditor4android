package de.blau.android.layer.streetlevel.mapillary;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.SpannableString;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.DateRangeDialog;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.streetlevel.AbstractImageOverlay;
import de.blau.android.layer.streetlevel.AbstractSequenceFetcher;
import de.blau.android.layer.streetlevel.ImageViewerActivity;
import de.blau.android.photos.PhotoViewerFragment;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.util.mvt.style.Layer;
import de.blau.android.util.mvt.style.Style;
import de.blau.android.util.mvt.style.Symbol;

public class MapillaryOverlay extends AbstractImageOverlay {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapillaryOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapillaryOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final String MAPILLARY_TILES_ID = "MAPILLARYV4";

    // mapbox gl style layer ids
    private static final String IMAGE_LAYER    = "image";
    private static final String OVERVIEW_LAYER = "overview";
    private static final String SEQUENCE_LAYER = "sequence";

    // mapillary API constants
    private static final String CAPTURED_AT_KEY = "captured_at";
    private static final String DATA_KEY        = "data";
    private static final String ID_KEY          = "id";
    private static final String SEQUENCE_ID_KEY = "sequence_id";

    private static final String DEFAULT_MAPILLARY_STYLE_JSON = "mapillary-style.json";

    private String mapillaryImagesUrl    = Urls.DEFAULT_MAPILLARY_IMAGES_V4;
    private String mapillarySequencesUrl = Urls.DEFAULT_MAPILLARY_SEQUENCES_URL_V4;

    public static final String APIKEY_KEY = "MAPILLARY_CLIENT_TOKEN";

    public static final String FILENAME = "mapillary" + "." + FileExtensions.RES;

    static class State implements Serializable {
        private static final long serialVersionUID = 4L;

        private String                                         sequenceId    = null;
        private long                                           imageId       = 0;
        private final java.util.Map<String, ArrayList<String>> sequenceCache = new HashMap<>();
        private long                                           startDate     = 0L;
        private long                                           endDate       = new Date().getTime();
    }

    private State                     mapillaryState = new State();
    private final SavingHelper<State> savingHelper   = new SavingHelper<>();

    private final String apiKey;

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    public MapillaryOverlay(@NonNull final Map map) {
        super(map, MAPILLARY_TILES_ID, IMAGE_LAYER, DEFAULT_MAPILLARY_STYLE_JSON);
        final Context context = map.getContext();
        try (KeyDatabaseHelper keys = new KeyDatabaseHelper(context); SQLiteDatabase db = keys.getReadableDatabase()) {
            apiKey = KeyDatabaseHelper.getKey(db, APIKEY_KEY, EntryType.API_KEY);
            if (apiKey == null) {
                ScreenMessage.toastTopError(context, context.getString(R.string.toast_api_key_missing, APIKEY_KEY));
            }
        }
        setDateRange(mapillaryState.startDate, mapillaryState.endDate);
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        super.onSaveState(ctx);
        savingHelper.save(ctx, FILENAME, mapillaryState, false);
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        boolean result = super.onRestoreState(ctx);
        if (mapillaryState == null) {
            mapillaryState = savingHelper.load(ctx, FILENAME, true);
            if (mapillaryState != null) {
                setSelected(mapillaryState.imageId);
                setDateRange(mapillaryState.startDate, mapillaryState.endDate);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_mapillary);
    }

    @Override
    public void onSelected(FragmentActivity activity, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        if (isPoint(f)) {
            // we ignore anything except the images for now
            java.util.Map<String, Object> attributes = f.getAttributes();
            String sequenceId = (String) attributes.get(SEQUENCE_ID_KEY);
            Long id = (Long) attributes.get(ID_KEY);
            if (id != null && sequenceId != null) {
                ArrayList<String> keys = mapillaryState != null ? mapillaryState.sequenceCache.get(sequenceId) : null;
                if (keys == null) {
                    try {
                        Thread t = new Thread(null, new MapillarySequenceFetcher(activity, mapillarySequencesUrl, sequenceId, id, apiKey),
                                "Mapillary Sequence");
                        t.start();
                    } catch (SecurityException | IllegalThreadStateException e) {
                        Log.e(DEBUG_TAG, "Unable to run SequenceFetcher " + e.getMessage());
                        return;
                    }
                } else {
                    showImages(activity, id, keys);
                }
                setSelected(f);
                mapillaryState.sequenceId = sequenceId;
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
    private void showImages(@NonNull FragmentActivity activity, @NonNull Long id, @NonNull ArrayList<String> ids) {
        int pos = ids.indexOf(id.toString());
        if (pos >= 0 && cacheDir != null) {
            String imagesUrl = String.format(mapillaryImagesUrl, "%s", apiKey);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                PhotoViewerFragment.showDialog(activity, ids, pos, new MapillaryLoader(cacheDir, cacheSize, imagesUrl, ids));
            } else {
                ImageViewerActivity.start(activity, ids, pos, new MapillaryLoader(cacheDir, cacheSize, imagesUrl, ids));
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
    private class MapillarySequenceFetcher extends AbstractSequenceFetcher {

        final Long id;

        /**
         * Construct a new instance
         * 
         * @param activity the calling Activity
         * @param sequenceId the sequence id
         * @param id the image id
         */
        public MapillarySequenceFetcher(@NonNull FragmentActivity activity, @NonNull String urlTemplate, @NonNull String sequenceId, @NonNull Long id,
                @NonNull String apiKey) {
            super(activity, urlTemplate, sequenceId, apiKey);
            this.id = id;
        }

        @Override
        protected void saveIdsAndUpdate(ArrayList<String> ids) {
            if (mapillaryState == null) {
                mapillaryState = new State();
            }
            mapillaryState.sequenceCache.put(sequenceId, ids);
            showImages(activity, id, ids);
        }

        @Override
        protected ArrayList<String> getIds(JsonElement root) throws IOException {
            JsonElement data = ((JsonObject) root).get(DATA_KEY);
            if (!(data instanceof JsonArray)) {
                throw new IOException("data not a JsonArray");
            }
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
            return ids;
        }
    }

    @Override
    public void deselectObjects() {
        mapillaryState = null;
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
            if (mapillaryState == null) {
                mapillaryState = new State();
            }
            mapillaryState.imageId = id;
            selectedFilter.set(2, new JsonPrimitive(id));
            map.invalidate();
            dirty();
        }
    }

    @Override
    public void selectImage(int pos) {
        synchronized (this) {
            if (mapillaryState != null && mapillaryState.sequenceId != null) {
                List<String> ids = mapillaryState.sequenceCache.get(mapillaryState.sequenceId);
                if (ids != null) {
                    String idStr = ids.get(pos);
                    if (idStr != null) {
                        long id = Long.parseLong(ids.get(pos));
                        setSelected(id);
                        return;
                    }
                }
                Log.e(DEBUG_TAG, "position " + pos + " not found in sequence " + mapillaryState.sequenceId);
            }
        }
    }

    @Override
    public void setDateRange(long start, long end) {
        mapillaryState.startDate = start;
        mapillaryState.endDate = end;
        Style style = ((VectorTileRenderer) tileRenderer).getStyle();
        setDateRange(style, IMAGE_LAYER, start, end, null);
        setDateRange(style, SEQUENCE_LAYER, start, end, null);
        setDateRange(style, OVERVIEW_LAYER, start, end, null);
        map.invalidate();
        dirty();
    }

    @Override
    protected void setDateFilterValue(JsonElement filter, long value, @Nullable SimpleDateFormat format) {
        if (filter instanceof JsonArray && ((JsonArray) filter).size() == 3) {
            ((JsonArray) filter).set(2, new JsonPrimitive(value));
        }
    }

    @Override
    protected void flushSequenceCache(@NonNull Context ctx) {
        if (mapillaryState != null) {
            mapillaryState.imageId = 0;
            mapillaryState.sequenceCache.clear();
            mapillaryState.sequenceId = null;
            savingHelper.save(ctx, FILENAME, mapillaryState, false);
        }
    }

    @Override
    public SpannableString getDescription(@NonNull Context context, de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        Long capturedAt = (Long) f.getAttributes().get(CAPTURED_AT_KEY);
        return new SpannableString(context.getString(R.string.mapillary_image, timestampFormat.format(capturedAt)));
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

    /**
     * Show a dialog to set the displayed date range
     * 
     * @param activity the activity we are being shown on
     * @param layerIndex the index of this layer
     */
    public void selectDateRange(@NonNull FragmentActivity activity, int layerIndex) {
        DateRangeDialog.showDialog(activity, layerIndex, mapillaryState.startDate, mapillaryState.endDate);
    }
}