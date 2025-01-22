package de.blau.android.util.mvt.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import de.blau.android.util.mvt.VectorTileDecoder;

public class ColorStyleAttribute extends StyleAttribute {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = ColorStyleAttribute.class.getSimpleName().substring(0,
            Math.min(23, ColorStyleAttribute.class.getSimpleName().length()));

    int literal;

    @Override
    void set(Context ctx, String name, JsonObject paintOrLayout) {
        JsonElement color = paintOrLayout.get(name);
        if (color != null) {
            if (Style.isString(color)) {
                set(Color.parseColor(color.getAsString()));
            } else if (color.isJsonObject()) {// interpolation expression
                function = ((JsonObject) color);
            } else if (color.isJsonArray()) {
                function = (JsonArray) color;
            } else { // feature-state
                Log.w(DEBUG_TAG, "Unsupported " + name + " value " + color);
            }
        }
    }

    @Override
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
        if (function instanceof JsonObject) {
            set(Layer.evalColorFunction((JsonObject) function, feature, z));
        } else if (function instanceof JsonArray) {
            Object temp = Layer.evaluateExpression((JsonArray) function, feature);
            if (temp instanceof Number) {
                set(((Number) temp).intValue());
                return;
            }
            if (temp instanceof JsonPrimitive) {
                set(((JsonPrimitive) temp).getAsNumber().intValue());
                return;
            }
            Log.w(DEBUG_TAG, "Value is not an int " + temp);
        }
    }

    /**
     * Set the current value
     * 
     * This is exposed so that conversions and other settings can be made by overriding this method
     * 
     * @param color the new value
     */
    protected void set(int color) {
        literal = color;
    }
}
