package io.vespucci.util.mvt.style;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.util.Density;
import io.vespucci.util.mvt.VectorTileDecoder;

/**
 * Serializable container for a mapbox-gl style attribute
 * 
 * @author Simon Poole
 */
abstract class StyleAttribute implements Serializable {
    private static final long serialVersionUID = 2L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, StyleAttribute.class.getSimpleName().length());
    private static final String DEBUG_TAG = StyleAttribute.class.getSimpleName().substring(0, TAG_LEN);

    transient JsonElement function;

    /**
     * Set the current value of the attribute
     * 
     * @param ctx an Android Context
     * @param name the name of the attribute
     * @param paintOrLayout the JsonObject holding the attribute
     */
    abstract void set(@Nullable Context ctx, @NonNull String name, @NonNull JsonObject paintOrLayout);

    /**
     * Evaluate the zoom dependent function if any
     * 
     * @param feature the current feature
     * @param z the current zoom level
     */
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
    }

    /**
     * Transform style px values in stops that are assumed to be density independent to screen values
     * 
     * Note that we half the pixel values which seems to be necessary, likely because mapbox uses a different pixel
     * density definition.
     * 
     * @param ctx an Android Context
     * @param stopArray an JsonArray holding the stops
     */
    protected void dipStops(@NonNull Context ctx, @NonNull JsonObject stopArray) {
        JsonArray stops = stopArray.getAsJsonArray(Style.INTERPOLATION_STOPS);
        for (JsonElement stop : stops) {
            if (stop.isJsonArray() && ((JsonArray) stop).size() == 2) {
                JsonElement stopValue = ((JsonArray) stop).get(1);
                if (Style.isNumber(stopValue)) {
                    ((JsonArray) stop).set(1, new JsonPrimitive(Density.dpToPx(ctx, ((JsonArray) stop).get(1).getAsFloat()) / 2));
                } else if (stopValue != null && stopValue.isJsonArray()) {
                    for (int i = 0; i < ((JsonArray) stopValue).size(); i++) {
                        ((JsonArray) stopValue).set(i, new JsonPrimitive(Density.dpToPx(ctx, ((JsonArray) stopValue).get(i).getAsFloat()) / 2));
                    }
                } else {
                    Log.w(DEBUG_TAG, "Unsupported stop value " + stopValue);
                }
            }
        }
    }

    /**
     * Serialize this object
     * 
     * @param out ObjectOutputStream to write to
     * @throws IOException if writing fails
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(function != null ? function.toString() : null);
    }

    /**
     * Read serialized object
     * 
     * @param in the input stream
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the Class to deserialize can't be found
     */
    private void readObject(@NonNull ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Object temp = in.readObject();
        function = temp != null ? (JsonElement) JsonParser.parseString(temp.toString()) : null;
    }
}
