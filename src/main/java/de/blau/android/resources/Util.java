package de.blau.android.resources;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.dialogs.Layers;
import de.blau.android.osm.BoundingBox;

public final class Util {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Layers.class.getSimpleName().length());
    private static final String DEBUG_TAG = Layers.class.getSimpleName().substring(0, TAG_LEN);

    private static final int BASE_STATE  = 0;
    private static final int PARAM_STATE = 1;

    /**
     * Private constructor
     */
    private Util() {
        // don't allow instantiating of this class
    }

    /**
     * Replace any placeholders in a WFS url
     * 
     * @param url the url with placeholders
     * @param proj projection
     * @param box boundingbox
     * @return the url with placeholders replaced
     */
    /**
     * 
     * 
     * @return
     */
    @NonNull
    public static String replaceWfsPlaceholders(@NonNull final String url, @Nullable String proj, @NonNull BoundingBox box) {
        StringBuilder builder = new StringBuilder();
        StringBuilder param = new StringBuilder();
        int state = BASE_STATE;
        for (char c : url.toCharArray()) {
            if (state == BASE_STATE) {
                if (c == TileLayerSource.PLACEHOLDER_START) {
                    state = PARAM_STATE;
                    param.setLength(0); // reset
                } else {
                    builder.append(c);
                }
                continue;
            }
            if (c == TileLayerSource.PLACEHOLDER_END) {
                state = BASE_STATE;
                String p = param.toString();
                switch (p) {
                case TileLayerSource.PROJ_PLACEHOLDER:
                    builder.append(proj);
                    break;
                case TileLayerSource.BBOX_PLACEHOLDER:
                    TileLayerSource.buildBox(builder, box.getLeft() / 1E7D, box.getBottom() / 1E7D, box.getRight() / 1E7D, box.getTop() / 1E7D);
                    builder.append(",");
                    builder.append(TileLayerSource.EPSG_4326);
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown place holder " + p);
                }
            } else {
                param.append(c);
            }
        }
        return builder.toString();
    }
}
