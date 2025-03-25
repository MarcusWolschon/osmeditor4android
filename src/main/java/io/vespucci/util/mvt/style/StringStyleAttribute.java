package io.vespucci.util.mvt.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import io.vespucci.util.mvt.VectorTileDecoder;

public class StringStyleAttribute extends StyleAttribute {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = StringStyleAttribute.class.getSimpleName().substring(0,
            Math.min(23, StringStyleAttribute.class.getSimpleName().length()));

    String literal;

    @Override
    void set(Context ctx, String name, JsonObject paintOrLayout) {
        JsonElement string = paintOrLayout.get(name);
        if (string != null) {
            if (Style.isString(string)) {
                set(string.getAsString());
            } else if (string.isJsonObject() || string.isJsonArray()) {// interpolation expression
                function = string;
            } else { // feature-state
                Log.w(DEBUG_TAG, "Unsupported " + name + " value " + string);
            }
        }
    }

    @Override
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
        if (function instanceof JsonObject) {
            JsonElement temp = Layer.evalCategoryFunction((JsonObject) function, feature, z);
            if (Style.isString(temp)) {
                set(temp.getAsString());
            }
        } else if (function instanceof JsonArray && feature != null) {
            Object temp = Layer.evaluateExpression((JsonArray) function, feature);
            if (temp instanceof String) {
                set((String) temp);
                return;
            }
            if (temp instanceof JsonElement) {
                set(((JsonElement) temp).getAsString());
                return;
            }
            Log.w(DEBUG_TAG, "Value is not a String " + temp);
        }

    }

    /**
     * Set the current value
     * 
     * This is exposed so that conversions and other settings can be made by overriding this method
     * 
     * @param value the new value
     */
    protected void set(@Nullable String value) {
        literal = value;
    }
}
