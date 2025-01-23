package de.blau.android.layer.streetlevel.panoramax;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableString;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.DateRangeDialog;
import de.blau.android.dialogs.Progress;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.streetlevel.AbstractImageOverlay;
import de.blau.android.layer.streetlevel.AbstractSequenceFetcher;
import de.blau.android.layer.streetlevel.ImageViewerActivity;
import de.blau.android.photos.PhotoViewerFragment;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.collections.MRUHashMap;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.util.mvt.style.Layer;
import de.blau.android.util.mvt.style.Style;
import de.blau.android.util.mvt.style.Symbol;

public class PanoramaxOverlay extends AbstractImageOverlay {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PanoramaxOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = PanoramaxOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final String PANORAMAX_TILES_ID = "PANORAMAX";

    // mapbox gl style layer ids
    private static final String IMAGE_LAYER    = "pictures";
    private static final String SEQUENCE_LAYER = "sequences";

    // panoramax tile API constants
    private static final String TIMESTAMP_KEY      = "ts";
    private static final String ID_KEY             = "id";
    private static final String FIRST_SEQUENCE_KEY = "first_sequence";
    private static final String SEQUENCES_KEY      = "sequences";

    private static final Pattern SEQUENCES_PATTERN = Pattern.compile("^\\[\\\"([^\\, ]+)\\\".*\\]$");

    private static final String DEFAULT_PANORAMAX_STYLE_JSON = "panoramax-style.json";

    /** this is the format used by panoramax */
    private final SimpleDateFormat dateFormat = DateFormatter.getUtcFormat("yyyy-MM-dd");

    private static final String API_COLLECTIONS_ITEMS = "api/collections/%s/items?limit=1000";
    private String              panoramaxSequencesUrl = Urls.DEFAULT_PANORAMAX_API_URL + API_COLLECTIONS_ITEMS;

    public static final String FILENAME = "panoramax" + "." + FileExtensions.RES;

    private static final int MAX_IMAGE_IDS = 100; // max number of image ids left and right to send to the viewer

    static class State implements Serializable {
        private static final long serialVersionUID = 3L;

        private static final int RETAINED_SEQUENCES = 50;

        private String                                      sequenceId    = null;
        private String                                      imageId       = null;
        private final MRUHashMap<String, ArrayList<String>> sequenceCache = new MRUHashMap<>(RETAINED_SEQUENCES);
        private final java.util.Map<String, String>         urlCache      = new HashMap<>();
        private long                                        startDate     = 0L;
        private long                                        endDate       = new Date().getTime();

        /**
         * Add a sequence to the sequence cache, if the cache capacity is exceeded, entries will be removed from the url
         * cache too
         * 
         * @param id the sequence id
         * @param list the list of image ids in the sequence
         */
        void cacheSequence(@NonNull String id, @NonNull ArrayList<String> list) {
            synchronized (urlCache) {
                final ArrayList<String> removedList = sequenceCache.put(id, list);
                Log.d(DEBUG_TAG, "Sequence cache size " + sequenceCache.size());
                if (removedList != null) {
                    new ExecutorTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void input) throws Exception {
                            // remove entries from URL cache
                            Log.d(DEBUG_TAG, "Removing " + removedList.size() + " entries from url cache");
                            synchronized (urlCache) {
                                for (String url : removedList) {
                                    urlCache.remove(url);
                                }
                            }
                            return null;
                        }
                    }.execute();
                }
            }
        }
    }

    private State                     panoramaxState = new State();
    private final SavingHelper<State> savingHelper   = new SavingHelper<>();

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    public PanoramaxOverlay(@NonNull final Map map) {
        super(map, PANORAMAX_TILES_ID, IMAGE_LAYER, DEFAULT_PANORAMAX_STYLE_JSON);
        // hack so that we can follow the preferences
        try {
            Uri uri = Uri.parse(layerSource.getTileUrl());
            Uri prefsUri = Uri.parse(App.getPreferences(map.getContext()).getPanoramaxApiUrl());
            Log.i(DEBUG_TAG, "Change host from " + uri.getAuthority() + " to " + prefsUri.getAuthority());
            uri.buildUpon().authority(prefsUri.getAuthority());
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "Unparsable tile url " + ex.getMessage());
        }
        setDateRange(0, new Date().getTime());
    }

    @Override
    public void onSaveState(@NonNull Context ctx) throws IOException {
        super.onSaveState(ctx);
        savingHelper.save(ctx, FILENAME, panoramaxState, false);
    }

    @Override
    public boolean onRestoreState(@NonNull Context ctx) {
        boolean result = super.onRestoreState(ctx);
        if (panoramaxState.sequenceCache.isEmpty()) {
            panoramaxState = savingHelper.load(ctx, FILENAME, true);
            if (panoramaxState != null) {
                setSelected(panoramaxState.imageId);
                setDateRange(panoramaxState.startDate, panoramaxState.endDate);
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
        if (!isPoint(f)) {
            Log.e(DEBUG_TAG, "Unexpected feature type " + f);
            return;
        }
        // we ignore anything except the images for now
        java.util.Map<String, Object> attributes = f.getAttributes();
        String sequenceId = getSequenceId(attributes);

        String id = (String) attributes.get(ID_KEY);
        Log.d(DEBUG_TAG, "trying to retrieve sequence " + sequenceId + " for " + id);
        if (id != null && sequenceId != null) {
            ArrayList<String> keys = panoramaxState != null ? panoramaxState.sequenceCache.get(sequenceId) : null;
            if (keys == null) {
                new ExecutorTask<Void, Integer, Void>() {
                    @Override
                    protected void onPreExecute() {
                        Progress.showDialog(activity, Progress.PROGRESS_DOWNLOAD_SEQUENCE);
                    }

                    @Override
                    protected Void doInBackground(Void param) {
                        new PanoramaxSequenceFetcher(activity, panoramaxSequencesUrl, sequenceId, id).run();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD_SEQUENCE);
                    }
                }.execute();
            } else {
                showImages(activity, id, keys);
            }
            setSelected(f);
            panoramaxState.sequenceId = sequenceId;
            return;
        }
        String message = activity.getString(R.string.toast_panoramax_sequence_error, sequenceId, id);
        ScreenMessage.toastTopError(activity, message);
        Log.e(DEBUG_TAG, message);
    }

    /**
     * Try to determine the sequence id from the atributes
     * 
     * @param attributes the attributes
     * @return the id or null if it can't be found
     */
    @Nullable
    private String getSequenceId(@NonNull java.util.Map<String, Object> attributes) {
        String sequenceId = (String) attributes.get(FIRST_SEQUENCE_KEY);
        if (sequenceId == null) {
            Object o = attributes.get(SEQUENCES_KEY);
            if (o instanceof String) {
                Matcher m = SEQUENCES_PATTERN.matcher((String) o);
                if (m.find()) {
                    sequenceId = m.group(1);
                }
            }
        }
        return sequenceId;
    }

    /**
     * Start the image viewer
     * 
     * @param activity the calling activity
     * @param id id of the image
     * @param ids list of all ids in the sequence
     */
    private void showImages(@NonNull FragmentActivity activity, @NonNull String id, @NonNull List<String> ids) {
        int pos = ids.indexOf(id);
        // sequences can be very large, we show an excerpt of ~200 around the current position
        ArrayList<String> tempIds = new ArrayList<>(ids.subList(Math.max(0, pos - MAX_IMAGE_IDS), Math.min(pos + MAX_IMAGE_IDS, ids.size())));
        pos = tempIds.indexOf(id);
        java.util.Map<String, String> tempUrlCache = new HashMap<>();
        for (String tempId : tempIds) {
            tempUrlCache.put(tempId, panoramaxState.urlCache.get(tempId));
        }
        if (pos >= 0 && cacheDir != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                PhotoViewerFragment.showDialog(activity, tempIds, pos, new PanoramaxLoader(cacheDir, cacheSize, tempIds, tempUrlCache));
            } else {
                ImageViewerActivity.start(activity, tempIds, pos, new PanoramaxLoader(cacheDir, cacheSize, tempIds, tempUrlCache));
            }
            activity.runOnUiThread(() -> map.invalidate());
            return;
        } else {
            Log.e(DEBUG_TAG, "Image id " + id + " not found in sequence");
        }
        String message = activity.getString(R.string.toast_panoramax_image_not_in_sequence_error, id);
        ScreenMessage.toastTopError(activity, message);
        Log.e(DEBUG_TAG, message);
    }

    /**
     * Runnable that will fetch a sequence from the Panoramax API
     * 
     * @author simon
     *
     */
    private class PanoramaxSequenceFetcher extends AbstractSequenceFetcher {

        private static final String LINKS_KEY    = "links";
        private static final String REL_KEY      = "rel";
        private static final String NEXT_VALUE   = "next";
        private static final String FEATURES_KEY = "features";
        private static final String ASSETS_KEY   = "assets";
        private static final String HD_KEY       = "hd";
        private static final String HREF_KEY     = "href";

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
            Log.d(DEBUG_TAG, "Saving ids in sequence");
            if (panoramaxState == null) {
                panoramaxState = new State();
            }
            panoramaxState.cacheSequence(sequenceId, ids);
            showImages(activity, id, ids);
        }

        @Override
        protected URL getIds(JsonElement root, ArrayList<String> ids) throws IOException {
            JsonElement features = ((JsonObject) root).get(FEATURES_KEY);
            if (!(features instanceof JsonArray)) {
                throw new IOException("features not a JsonArray");
            }
            JsonArray featuresArray = features.getAsJsonArray();
            for (JsonElement element : featuresArray) {
                if (element instanceof JsonObject) {
                    getIdAndUrl(ids, panoramaxState.urlCache, element);
                }
            }
            // check for paginated response
            JsonElement linksArray = ((JsonObject) root).get(LINKS_KEY);
            if (linksArray instanceof JsonArray) {
                for (JsonElement element : ((JsonArray) linksArray)) {
                    if (element instanceof JsonObject && ((JsonObject) element).has(REL_KEY)
                            && NEXT_VALUE.equals(((JsonObject) element).get(REL_KEY).getAsString())) {
                        Log.d(DEBUG_TAG, "get next page");
                        return new URL(((JsonObject) element).get(HREF_KEY).getAsString());
                    }
                }
            }
            return null;
        }

        /**
         * Get the id and url from the API retrieved JsonElement and add them to the caches
         * 
         * @param ids sequence cache id list
         * @param urls url cache
         * @param element the JsonElement from the API
         */
        private void getIdAndUrl(@NonNull List<String> ids, @NonNull java.util.Map<String, String> urls, @NonNull JsonElement element) {
            JsonElement idElement = ((JsonObject) element).get(ID_KEY);
            if (idElement == null) {
                Log.e(DEBUG_TAG, "id not found in sequence from API");
                return;
            }
            final String idString = idElement.getAsString();
            ids.add(idString);
            JsonElement assets = ((JsonObject) element).get(ASSETS_KEY);
            if (assets == null) {
                Log.e(DEBUG_TAG, "assets not found in sequence from API for id " + idString);
                return;
            }
            JsonElement hd = ((JsonObject) assets).get(HD_KEY);
            if (hd == null) {
                Log.e(DEBUG_TAG, "hd not found in sequence from API for id " + idString);
                return;
            }
            JsonElement href = ((JsonObject) hd).get(HREF_KEY);
            if (href == null) {
                Log.e(DEBUG_TAG, "href not found in sequence from API for id " + idString);
                return;
            }
            urls.put(idString, href.getAsString());

        }
    }

    @Override
    public void deselectObjects() {
        panoramaxState = null;
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
            if (panoramaxState == null) {
                panoramaxState = new State();
            }
            panoramaxState.imageId = id;
            selectedFilter.set(2, id != null ? new JsonPrimitive(id) : null);
            map.invalidate();
            dirty();
        }
    }

    @Override
    public void selectImage(int pos) {
        synchronized (this) {
            if (panoramaxState != null && panoramaxState.sequenceId != null) {
                List<String> ids = panoramaxState.sequenceCache.get(panoramaxState.sequenceId);
                if (ids != null) {
                    String id = ids.get(pos);
                    if (id != null) {
                        setSelected(id);
                        return;
                    }
                }
                Log.e(DEBUG_TAG, "position " + pos + " not found in sequence " + panoramaxState.sequenceId);
            }
        }
    }

    @Override
    public void setDateRange(long start, long end) {
        if (panoramaxState != null) {
            panoramaxState.startDate = start;
            panoramaxState.endDate = end;
        }
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
        if (panoramaxState != null) {
            panoramaxState.imageId = null;
            panoramaxState.sequenceCache.clear();
            panoramaxState.urlCache.clear();
            panoramaxState.sequenceId = null;
            savingHelper.save(ctx, FILENAME, panoramaxState, false);
        }
    }

    @Override
    public void setPrefs(Preferences prefs) {
        cacheSize = prefs.getMapillaryCacheSize() * ONE_MILLION;
        panoramaxSequencesUrl = prefs.getPanoramaxApiUrl() + API_COLLECTIONS_ITEMS;
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
        DateRangeDialog.showDialog(activity, layerIndex, panoramaxState.startDate, panoramaxState.endDate);
    }
}