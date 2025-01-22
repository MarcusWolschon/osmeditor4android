package de.blau.android.util.mvt.style;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import de.blau.android.util.mvt.VectorTileDecoder;

public class FloatArrayStyleAttribute extends StyleAttribute {

    private static final long serialVersionUID = 1L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, FloatArrayStyleAttribute.class.getSimpleName().length());
    private static final String DEBUG_TAG = FloatArrayStyleAttribute.class.getSimpleName().substring(0, TAG_LEN);

    float[] literal = new float[] { 0f, 0f };
    boolean convert;

    /**
     * Constructor
     * 
     * @param convert if true convert from DIP to screen pixels
     */
    FloatArrayStyleAttribute(boolean convert) {
        this.convert = convert;
    }

    @Override
    void set(Context ctx, String name, JsonObject paintOrLayout) {
        JsonElement array = paintOrLayout.get(name);
        if (array != null) {
            if (array.isJsonArray() && ((JsonArray) array).size() == 2) {
                float a1 = ((JsonArray) array).get(0).getAsFloat();
                float a2 = ((JsonArray) array).get(1).getAsFloat();
                final float conversionFactor = ctx.getResources().getDisplayMetrics().density / 2;
                set(new float[] { a1 * (convert ? conversionFactor : 1), a2 * (convert ? conversionFactor : 1) });
            } else if (array.isJsonObject()) {// interpolation expression
                if (convert) {
                    dipStops(ctx, (JsonObject) array);
                }
                function = (JsonObject) array;
            } else { // feature-state or expression
                Log.w(DEBUG_TAG, "Unsupported " + name + " value " + array);
            }
        }
    }

    @Override
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
        if (function instanceof JsonObject) {
            JsonArray r = Layer.evalArrayFunction((JsonObject) function, feature, z);
            set(new float[] { r.get(0).getAsFloat(), r.get(1).getAsFloat() });
        }
    }

    /**
     * Set the current value
     * 
     * This is exposed so that conversions and other settings can be made by overriding this method
     * 
     * @param array the new value
     */
    protected void set(float[] array) {
        literal = array;
    }
}
