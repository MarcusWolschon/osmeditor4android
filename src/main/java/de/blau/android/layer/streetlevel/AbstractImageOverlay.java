package de.blau.android.layer.streetlevel;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import android.content.Context;
import android.graphics.Canvas;
import android.text.SpannableString;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Map;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.symbols.Mapillary;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileRenderer;
import de.blau.android.util.mvt.style.Layer;
import de.blau.android.util.mvt.style.Style;
import de.blau.android.util.mvt.style.Symbol;
import de.blau.android.views.IMapView;

public abstract class AbstractImageOverlay extends de.blau.android.layer.mvt.MapOverlay implements DateRangeInterface, SelectImageInterface {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AbstractImageOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = AbstractImageOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final int     DEFAULT_MIN_ZOOM   = 16;
    public static final int     DEFAULT_CACHE_SIZE = 100;
    protected static final long ONE_MILLION        = 1000000L;

    // mapbox gl style layer ids
    protected static final String SELECTED_IMAGE_LAYER = "selected_image";

    protected final SimpleDateFormat timestampFormat = DateFormatter.getUtcFormat("yyyy-MM-dd HH:mm:ssZ");

    protected int minZoom = DEFAULT_MIN_ZOOM;

    /**
     * Download related stuff
     */
    protected long cacheSize = DEFAULT_CACHE_SIZE * ONE_MILLION;

    protected JsonArray selectedFilter = null;

    /**
     * Directory for caching images
     */
    protected File cacheDir;

    private final String imageLayer;
    private final String styleJson;

    /**
     * Construct this layer
     * 
     * @param map the Map object we are displayed on
     */
    protected AbstractImageOverlay(@NonNull final Map map, @NonNull String tileId, @NonNull String imageLayer, @NonNull String styleJson) {
        super(map, new VectorTileRenderer(), false);
        this.setRendererInfo(TileLayerSource.get(map.getContext(), tileId, false));
        this.map = map;
        final Context context = map.getContext();
        File[] storageDirs = context.getExternalCacheDirs();
        try {
            cacheDir = FileUtil.getPublicDirectory(storageDirs.length > 1 && storageDirs[1] != null ? storageDirs[1] : storageDirs[0], getName());
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Unable to create cache directory " + e.getMessage());
        }
        this.imageLayer = imageLayer;
        this.styleJson = styleJson;
        setPrefs(map.getPrefs());

        resetStyling();
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull IMapView osmv) {
        if (map.getZoomLevel() >= minZoom) {
            super.onDraw(c, osmv);
        }
    }

    @Override
    public List<VectorTileDecoder.Feature> getClicked(final float x, final float y, final ViewBox viewBox) {
        Style style = ((VectorTileRenderer) tileRenderer).getStyle();
        Layer layer = style.getLayer(imageLayer);
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
    public void invalidate() {
        map.invalidate();
    }

    /**
     * Check if this Feature geometry is a Point
     * 
     * @param f the Feature
     * @return true if the geometry is a Point
     */
    protected boolean isPoint(@NonNull de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        return GeoJSONConstants.POINT.equals(f.getGeometry().type());
    }

    /**
     * Flush both the sequence and image cache
     * 
     * @param ctx an Android Context
     */
    public synchronized void flushCaches(@NonNull Context ctx) {
        flushSequenceCache(ctx);
        try {
            Thread t = new Thread(null, () -> FileUtil.pruneCache(cacheDir, 0), "Image Cache Zapper");
            t.start();
        } catch (SecurityException | IllegalThreadStateException e) {
            Log.e(DEBUG_TAG, "Unable to flush image cache " + e.getMessage());
        }
    }

    @Override
    public SpannableString getDescription(de.blau.android.util.mvt.VectorTileDecoder.Feature f) {
        return getDescription(map.getContext(), f);
    }

    @Override
    public void resetStyling() {
        try (InputStream is = map.getContext().getAssets().open(styleJson)) {
            Style style = new Style();
            style.loadStyle(map.getContext(), is);
            ((VectorTileRenderer) tileRenderer).setStyle(style);
            Layer layer = style.getLayer(imageLayer);
            if (layer instanceof Symbol) {
                ((Symbol) layer).setSymbol(Mapillary.NAME, map.getDataStyle());
                layer.setColor(layer.getColor());
            }
            layer = style.getLayer(SELECTED_IMAGE_LAYER);
            if (layer instanceof Symbol) {
                ((Symbol) layer).setSymbol(Mapillary.NAME, map.getDataStyle());
                layer.setColor(layer.getColor());
                selectedFilter = layer.getFilter();
            }
            dirty();
            Log.d(DEBUG_TAG, "Loaded style successfully");
        } catch (IOException ioex) {
            Log.d(DEBUG_TAG, "Reading default mapillary style failed");
        }
    }

    /**
     * Set a range for the capture date
     * 
     * This manipulates the filter in the layer
     * 
     * @param style style to change
     * @param layerName the name of the layer to change
     * @param start the lower bound for the capture date in ms since the epoch
     * @param end the upper bound for the capture date in ms since the epoch
     * @param optional format for string dates
     */
    protected void setDateRange(@NonNull Style style, @NonNull String layerName, long start, long end, @Nullable SimpleDateFormat format) {
        Layer layer = style.getLayer(layerName);
        if (layer == null) {
            Log.e(DEBUG_TAG, layerName + " not found");
            return;
        }
        JsonArray filter = layer.getFilter();
        if (filter != null && filter.size() == 3) {
            setDateFilterValue(filter.get(1), start, format);
            setDateFilterValue(filter.get(2), end, format);
            return;
        }
        Log.e(DEBUG_TAG, "filter not found");
    }

    /**
     * Set the value of a filter
     * 
     * @param filter a JsonArray representing a filter
     * @param value the value to set
     * @param optional format for string dates
     */
    protected abstract void setDateFilterValue(JsonElement filter, long value, @Nullable SimpleDateFormat format);

    @Override
    public void flushTileCache(@Nullable final FragmentActivity activity, boolean all) {
        super.flushTileCache(activity, all);
        if (activity != null) {
            flushSequenceCache(activity);
        }
    }

    /**
     * Flush the sequence cache
     * 
     * @param ctx an Android Context
     */
    protected abstract void flushSequenceCache(@NonNull Context ctx);

    @Override
    public int onDrawAttribution(@NonNull Canvas c, @NonNull IMapView osmv, int offset) {
        return offset;
    }
}