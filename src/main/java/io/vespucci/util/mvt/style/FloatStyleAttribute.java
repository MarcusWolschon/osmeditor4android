package io.vespucci.util.mvt.style;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.util.mvt.VectorTileDecoder;

public class FloatStyleAttribute extends StyleAttribute {

    private static final long serialVersionUID = 2L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, FloatStyleAttribute.class.getSimpleName().length());
    private static final String DEBUG_TAG = FloatStyleAttribute.class.getSimpleName().substring(0, TAG_LEN);

    float                 literal;
    private final boolean convert;
    private float         conversionFactor = 1;

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
    void set(@NonNull Context ctx, @NonNull String name, @NonNull JsonObject paintOrLayout, float d) {
        conversionFactor = ctx.getResources().getDisplayMetrics().density / 2;
        literal = convert ? d * conversionFactor : d;
        set(ctx, name, paintOrLayout);
    }

    @Override
    void set(Context ctx, String name, JsonObject paintOrLayout) {
        conversionFactor = ctx.getResources().getDisplayMetrics().density / 2;
        JsonElement number = paintOrLayout.get(name);
        if (number != null) {
            if (Style.isNumber(number)) {
                set(number.getAsFloat() * (convert ? conversionFactor : 1));
            } else if (number.isJsonObject()) {// interpolation expression
                if (convert) {
                    dipStops(ctx, (JsonObject) number);
                }
                function = number;
            } else if (number.isJsonArray()) {
                function = number;
            } else { // feature-state
                Log.w(DEBUG_TAG, "Unsupported " + name + " value " + number);
            }
        }
    }

    @Override
    public void eval(@Nullable VectorTileDecoder.Feature feature, int z) {
        if (function instanceof JsonObject) {
            set((float) Layer.evalNumberFunction((JsonObject) function, feature, z));
        } else if (function instanceof JsonArray && feature != null) {
            Object temp = Layer.evaluateExpression((JsonArray) function, feature);
            if (temp instanceof Number) {
                set(((Number) temp).floatValue() * (convert ? conversionFactor : 1));
                return;
            }
            if (temp instanceof JsonPrimitive && ((JsonPrimitive) temp).isNumber()) {
                set(((JsonPrimitive) temp).getAsNumber().floatValue());
                return;
            }
            Log.w(DEBUG_TAG, "Value is not a number " + temp);
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
