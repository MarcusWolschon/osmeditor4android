package de.blau.android.util.mvt.style;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import de.blau.android.util.mvt.VectorTileDecoder;

public class FloatStyleAttribute extends StyleAttribute {

    private static final long serialVersionUID = 1L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, FloatStyleAttribute.class.getSimpleName().length());
    private static final String DEBUG_TAG = FloatStyleAttribute.class.getSimpleName().substring(0, TAG_LEN);

    float   literal;
    boolean convert;

    /**
     * Constructor
     * 
     * @param convert if true convert from DIP to screen pixels
     */
    public FloatStyleAttribute(boolean convert) {
        this.convert = convert;
    }

    /**
     * Set the current value of the attribute
     * 
     * @param ctx an Android Context
     * @param name the name of the attribute
     * @param paintOrLayout the JsonObject holding the attribute
     * @param d default value, this will be converted from dip to screen pixels if necessary
     */
    void set(Context ctx, String name, JsonObject paintOrLayout, float d) {
        literal = convert ? d * ctx.getResources().getDisplayMetrics().density / 2 : d;
        set(ctx, name, paintOrLayout);
    }

    @Override
    void set(Context ctx, String name, JsonObject paintOrLayout) {
        JsonElement number = paintOrLayout.get(name);
        if (number != null) {
            if (Style.isNumber(number)) {
                set(number.getAsFloat() * (convert ? ctx.getResources().getDisplayMetrics().density / 2 : 1));
            } else if (number.isJsonObject()) {// interpolation expression
                if (convert) {
                    dipStops(ctx, (JsonObject) number);
                }
                function = (JsonObject) number;
            } else { // feature-state or interpolation expression
                Log.w(DEBUG_TAG, "Unsupported " + name + " value " + number);
            }
        }
    }

    @Override
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
        if (function != null) {
            set((float) Layer.evalNumberFunction(function, feature, z));
        }
    }

    /**
     * Set the current value
     * 
     * This is exposed so that conversions and other settings can be made by overriding this method
     * 
     * @param value the new value
     */
    protected void set(float value) {
        literal = value;
    }
}
