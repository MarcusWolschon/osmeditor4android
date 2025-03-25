package io.vespucci.imageryoffset;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;
import io.vespucci.osm.ViewBox;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.util.DateFormatter;
import io.vespucci.util.GeoMath;
import io.vespucci.util.ScreenMessage;

public final class ImageryOffsetUtils {
    private static final String DEBUG_TAG = ImageryOffsetUtils.class.getSimpleName().substring(0, Math.min(23, ImageryOffsetUtils.class.getSimpleName().length()));

    /**
     * Private default constructor
     */
    private ImageryOffsetUtils() {
        // hide default constructor
    }

    /**
     * Retrieve offsets from the DB for a specific layer and apply them (displaying a toast)
     * 
     * @param ctx Android Context
     * @param prefs the current preferences
     * @param tileServerConfig a TileLayerServer object
     * @param bbox the current ViewBox if null the offsets will be applied unconditionally
     */
    public static void applyImageryOffsets(@NonNull Context ctx, @NonNull Preferences prefs, @NonNull final TileLayerSource tileServerConfig,
            @Nullable ViewBox bbox) {
        Log.d(DEBUG_TAG, "applyImageryOffsets");
        if (tileServerConfig == null) {
            Log.d(DEBUG_TAG, "applyImageryOffsets tileServerConfig is null");
            return;
        }
        try (ImageryOffsetDatabase offsetDb = new ImageryOffsetDatabase(ctx); SQLiteDatabase db = offsetDb.getReadableDatabase()) {
            List<ImageryOffset> offsets = ImageryOffsetDatabase.getOffsets(db, tileServerConfig.getImageryOffsetId());
            double centerLat = 0D;
            double centerLon = 0D;
            if (bbox != null) {
                centerLat = bbox.getCenterLat();
                centerLon = (bbox.getLeft() + bbox.getWidth() / 2d) / 1E7d;
            }
            if (!tileServerConfig.isMetadataLoaded()) {
                Log.e(DEBUG_TAG, "Meta-data for " + tileServerConfig + " not loaded");
                return;
            }
            boolean appliedOffset = false;
            for (ImageryOffset offset : offsets) {
                if (bbox != null) {
                    double distance = GeoMath.haversineDistance(centerLon, centerLat, offset.getLon(), offset.getLat());
                    Log.d(DEBUG_TAG,
                            "applyImageryOffsets distance is " + distance + " " + centerLon + " " + centerLat + " " + offset.getLon() + " " + offset.getLat());
                    if (distance > prefs.getMaxOffsetDistance()) {
                        Log.d(DEBUG_TAG, "not applying");
                        continue;
                    }
                }
                double deltaLon = offset.getLon() - offset.getImageryLon();
                double deltaLat = offset.getLat() - offset.getImageryLat();
                for (int z = offset.getMinZoom(); z <= offset.getMaxZoom(); z++) {
                    Offset oldOffset = tileServerConfig.getOffset(z);
                    if (oldOffset == null || oldOffset.getDeltaLon() != deltaLon || oldOffset.getDeltaLat() != deltaLat) {
                        tileServerConfig.setOffset(z, deltaLon, deltaLat);
                        appliedOffset = true;
                    }
                }
            }
            if (bbox != null && appliedOffset && Looper.myLooper() != null) {
                ScreenMessage.toastTopInfo(ctx, R.string.toast_applied_offset);
            }
        }
    }

    /**
     * Create a list of ImageryOffsets from a layer configuration
     * 
     * @param osmts layer configuration
     * @param bbox current ViewBox
     * @param author an author string to add
     * @return a List of ImageryOffset
     */
    @NonNull
    public static List<ImageryOffset> offsets2ImageryOffset(@NonNull final TileLayerSource osmts, @NonNull final ViewBox bbox, @Nullable final String author) {
        Offset[] offsets = osmts.getOffsets(); // current offset
        String imageryId = osmts.getImageryOffsetId();
        List<ImageryOffset> offsetList = new ArrayList<>();
        if (!osmts.isMetadataLoaded()) {
            return offsetList;
        }
        Offset lastOffset = null;
        ImageryOffset im = null;
        for (int z = 0; z < offsets.length; z++) { // iterate through the list and generate a new offset when
                                                   // necessary
            Offset o = offsets[z];
            if (o != null && (o.getDeltaLon() != 0 || o.getDeltaLat() != 0)) { // non-null zoom
                if (lastOffset != null && im != null && lastOffset.getDeltaLon() == o.getDeltaLon() && lastOffset.getDeltaLat() == o.getDeltaLat()) {
                    im.setMaxZoom(im.getMaxZoom() + 1);
                    lastOffset = o;
                    continue;
                }
                im = new ImageryOffset();
                im.imageryId = imageryId;
                im.setLon((bbox.getLeft() + bbox.getWidth() / 2d) / 1E7d);
                im.setLat(bbox.getCenterLat());
                im.setImageryLon(im.getLon() - o.getDeltaLon());
                im.setImageryLat(im.getLat() - o.getDeltaLat());
                im.setMinZoom(z + osmts.getMinZoomLevel());
                im.setMaxZoom(im.getMinZoom());
                Calendar c = Calendar.getInstance();
                im.date = DateFormatter.getFormattedString(ImageryOffset.DATE_PATTERN_IMAGERY_OFFSET_CREATED_AT, c.getTime());
                im.author = author;
                offsetList.add(im);
            }
            lastOffset = o;
        }

        return offsetList;
    }
}
