package de.blau.android.util.mvt.style;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import de.blau.android.util.mvt.VectorTileDecoder;

public class StringStyleAttribute extends StyleAttribute {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = StringStyleAttribute.class.getSimpleName();

    String literal;

    @Override
    void set(Context ctx, String name, JsonObject paintOrLayout) {
        JsonElement string = paintOrLayout.get(name);
        if (string != null) {
            if (Style.isString(string)) {
                set(string.getAsString());
            } else if (string.isJsonObject()) {// interpolation expression
                function = (JsonObject) string;
            } else { // feature-state or interpolation expression
                Log.w(DEBUG_TAG, "Unsupported " + name + " value " + string);
            }
        }
    }

    @Override
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
        if (function != null) {
            JsonElement temp = Layer.evalCategoryFunction(function, feature, z);
            if (Style.isString(temp)) {
                set(temp.getAsString());
            }
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
