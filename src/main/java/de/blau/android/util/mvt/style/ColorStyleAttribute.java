package de.blau.android.util.mvt.style;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import de.blau.android.util.mvt.VectorTileDecoder;

public class ColorStyleAttribute extends StyleAttribute {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = ColorStyleAttribute.class.getSimpleName().substring(0, Math.min(23, ColorStyleAttribute.class.getSimpleName().length()));

    int literal;

    @Override
    void set(Context ctx, String name, JsonObject paintOrLayout) {
        JsonElement color = paintOrLayout.get(name);
        if (color != null) {
            if (Style.isString(color)) {
                set(Color.parseColor(color.getAsString()));
            } else if (color.isJsonObject()) {// interpolation expression
                function = ((JsonObject) color);
            } else { // feature-state or interpolation expression
                Log.w(DEBUG_TAG, "Unsupported " + name + " value " + color);
            }
        }
    }

    @Override
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
        if (function != null) {
            set(Layer.evalColorFunction(function, feature, z));
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
