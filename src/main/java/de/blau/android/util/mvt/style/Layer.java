package de.blau.android.util.mvt.style;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.util.Log;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.android.sprites.Sprites;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.IntegerUtil;
import de.blau.android.util.SerializableTextPaint;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileDecoder.Feature;

public abstract class Layer implements Serializable {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Layer.class.getSimpleName().length());
    private static final String DEBUG_TAG = Layer.class.getSimpleName().substring(0, TAG_LEN);

    private static final long serialVersionUID = 14L;

    private static final String INTERPOLATION_DEFAULT                 = "default";
    private static final String INTERPOLATION_PROPERTY                = "property";
    private static final String INTERPOLATION_TYPE_EXPONENTIONAL_BASE = "base";
    private static final String INTERPOLATION_TYPE_EXPONENTIAL        = "exponential";
    private static final String INTERPOLATION_TYPE_IDENTITY           = "identity";
    private static final String INTERPOLATION_TYPE_CATEGORY           = "categorical";
    private static final String INTERPOLATION_TYPE                    = "type";
    private static final String LAYER_KEY_ID                          = "$id";
    private static final String LAYER_KEY_TYPE                        = "$type";
    private static final String LAYER_JOIN_MITER                      = "miter";
    private static final String LAYER_JOIN_ROUND                      = "round";
    private static final String LAYER_JOIN_BEVEL                      = "bevel";
    private static final String LAYER_CAP_SQUARE                      = "square";
    private static final String LAYER_CAP_ROUND                       = "round";
    private static final String LAYER_CAP_BUTT                        = "butt";

    private static final String LAYER_FILTER_ANY    = "any";
    private static final String LAYER_FILTER_ALL    = "all";
    private static final String LAYER_FILTER_NOT_IN = "!in";
    private static final String LAYER_FILTER_IN     = "in";
    private static final String LAYER_FILTER_GT_EQ  = ">=";
    private static final String LAYER_FILTER_GT     = ">";
    private static final String LAYER_FILTER_LT_EQ  = "<=";
    private static final String LAYER_FILTER_LT     = "<";
    private static final String LAYER_FILTER_NOT_EQ = "!=";
    private static final String LAYER_FILTER_EQ     = "==";

    private static final String LAYER_EXPRESSION_NOT_HAS    = "!has";
    private static final String LAYER_EXPRESSION_HAS        = "has";
    private static final String LAYER_EXPRESSION_GET        = "get";
    private static final String LAYER_EXPRESSION_TO_BOOLEAN = "to-boolean";
    private static final String LAYER_EXPRESSION_TO_NUMBER  = "to-number";
    private static final String LAYER_EXPRESSION_MATCH      = "match";

    private static final String FALSE = "false";

    private static final long RGB_ONLY   = 0x00FFFFFFL;
    private static final long ALPHA_ONLY = 0xFF000000L;

    public enum Type {
        FILL, LINE, SYMBOL, CIRCLE, HEATMAP, FILL_EXTRUSION, RASTER, HILLSHADE, BACKGROUD, SKY
    }

    private String       iD;
    private final String sourceLayer;
    private int          minZoom     = 0;
    private int          maxZoom     = -1;
    private boolean      visible     = true;
    private boolean      interactive = true;
    StringStyleAttribute pattern     = new StringStyleAttribute() {
                                         private static final long serialVersionUID = 1L;

                                         @Override
                                         protected void set(String pattern) {
                                             if (pattern == null || !pattern.equals(literal)) {
                                                 paint.setShader(null);
                                             }
                                             literal = pattern;
                                         }
                                     };
    ColorStyleAttribute  color       = new ColorStyleAttribute() {
                                         private static final long serialVersionUID = 1L;

                                         @Override
                                         protected void set(int color) {
                                             setColor(color);
                                         }
                                     };
    FloatStyleAttribute  opacity     = new FloatStyleAttribute(false) {
                                         private static final long serialVersionUID = 1L;

                                         @Override
                                         protected void set(float opacity) {
                                             paint.setAlpha(Math.round(opacity * 255));
                                         }
                                     };
    StringStyleAttribute lineCap     = new StringStyleAttribute() {
                                         private static final long serialVersionUID = 1L;

                                         @Override
                                         protected void set(String cap) {
                                             if (cap != null) {
                                                 switch (cap) {
                                                 case LAYER_CAP_BUTT:
                                                     paint.setStrokeCap(Cap.BUTT);
                                                     break;
                                                 case LAYER_CAP_ROUND:
                                                     paint.setStrokeCap(Cap.ROUND);
                                                     break;
                                                 case LAYER_CAP_SQUARE:
                                                     paint.setStrokeCap(Cap.SQUARE);
                                                     break;
                                                 default:
                                                     // log?
                                                 }
                                             }
                                         }
                                     };
    StringStyleAttribute lineJoin    = new StringStyleAttribute() {
                                         private static final long serialVersionUID = 1L;

                                         @Override
                                         protected void set(String join) {
                                             if (join != null) {
                                                 switch (join) {
                                                 case LAYER_JOIN_BEVEL:
                                                     paint.setStrokeJoin(Join.BEVEL);
                                                     break;
                                                 case LAYER_JOIN_ROUND:
                                                     paint.setStrokeJoin(Join.ROUND);
                                                     break;
                                                 case LAYER_JOIN_MITER:
                                                     paint.setStrokeJoin(Join.MITER);
                                                     break;
                                                 default:
                                                     // log?
                                                 }
                                             }
                                         }
                                     };

    protected SerializableTextPaint paint = new SerializableTextPaint();

    private transient JsonArray filter = null;

    protected transient Path  path           = new Path();
    protected transient Rect  destinationRect;
    protected transient float scaleX;
    protected transient float scaleY;
    private transient boolean patternChecked = false;

    /**
     * Default constructor
     * 
     * @param sourceLayer the source (data) layer
     */
    protected Layer(@Nullable String sourceLayer) {
        this.sourceLayer = sourceLayer;
        paint.setAlpha(255);
    }

    /**
     * Copy constructor
     * 
     * @param other another Style
     */
    protected Layer(@NonNull Layer other) {
        this.iD = other.iD; // copy
        this.sourceLayer = other.sourceLayer;
        this.minZoom = other.minZoom;
        this.maxZoom = other.maxZoom;
        this.paint = new SerializableTextPaint(other.paint);
        this.visible = other.visible;
        this.interactive = other.interactive;
        this.pattern = other.pattern;
    }

    /**
     * Get the source layer for this style
     * 
     * @return the sourceLayer
     */
    @Nullable
    public String getSourceLayer() {
        return sourceLayer;
    }

    /**
     * @return the iD
     */
    public String getId() {
        return iD;
    }

    /**
     * @param iD the iD to set
     */
    public void setId(String iD) {
        this.iD = iD;
    }

    /**
     * @return the minZoom
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * @param minZoom the minZoom to set
     */
    public void setMinZoom(int minZoom) {
        this.minZoom = minZoom;
    }

    /**
     * @return the maxZoom
     */
    public int getMaxZoom() {
        return maxZoom;
    }

    /**
     * @param maxZoom the maxZoom to set
     */
    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    /**
     * @return if true the layer should be rendered
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible if true render the layer
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return if true can be used interactively
     */
    public boolean isInteractive() {
        return interactive;
    }

    /**
     * @param interactive if true enable interactive use
     */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    /**
     * Get color for this layer
     * 
     * @return the color
     */
    public int getColor() {
        return paint.getColor();
    }

    /**
     * Set the color for this layer
     * 
     * If alpha is not set in the color value the prev. value will be used
     * 
     * @param color the color value
     */
    public void setColor(int color) {
        int tempAlpha = paint.getAlpha();
        paint.setColor(color);
        if (color >>> 24 == 0) {
            paint.setAlpha(tempAlpha);
        }
    }

    /**
     * Set the alpha/opacity value
     * 
     * @param alpha 0-255
     */
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    /**
     * Set the stroke width
     * 
     * @param width the width in px
     */
    public void setStrokeWidth(float width) {
        paint.setStrokeWidth(width);
    }

    /**
     * Get the strock width
     * 
     * @return the width in px
     */
    public float getStrokeWidth() {
        return paint.getStrokeWidth();
    }

    /**
     * Get the current filter
     * 
     * @return a JsonArray representing the filter
     */
    @Nullable
    public JsonArray getFilter() {
        return filter;
    }

    /**
     * Set a filter for this style
     * 
     * @param filter the filter
     */
    public void setFilter(@Nullable JsonArray filter) {
        this.filter = filter;
    }

    /**
     * Evaluate a filter expression
     * 
     * @param expression the expression
     * @param feature the feature we need to filter gains
     * @return true if the filter excepts the feature
     */
    public boolean evaluateFilter(@NonNull JsonArray expression, @NonNull VectorTileDecoder.Feature feature) {
        String function = expression.get(0).getAsString();
        final int size = expression.size();
        switch (function) {
        case LAYER_FILTER_EQ:
        case LAYER_FILTER_NOT_EQ:
        case LAYER_FILTER_LT:
        case LAYER_FILTER_LT_EQ:
        case LAYER_FILTER_GT:
        case LAYER_FILTER_GT_EQ:
            String key = expression.get(1).getAsString();
            Object left = getKeyValue(feature, key);
            if (left == null) {
                return LAYER_FILTER_NOT_EQ.equals(function); // val doesn't exist is true
            }
            return compare(function, left, expression.get(2));
        case LAYER_FILTER_IN:
            key = expression.get(1).getAsString();
            left = getKeyValue(feature, key);
            if (left == null) {
                return false;
            }
            for (int i = 2; i < size; i++) {
                if (compare(LAYER_FILTER_EQ, left, expression.get(i))) {
                    return true;
                }
            }
            return false;
        case LAYER_FILTER_NOT_IN:
            key = expression.get(1).getAsString();
            left = getKeyValue(feature, key);
            if (left == null) {
                return true;
            }
            for (int i = 2; i < size; i++) {
                if (compare(LAYER_FILTER_EQ, left, expression.get(i))) {
                    return false;
                }
            }
            return true;
        case LAYER_FILTER_ALL:
            for (int i = 1; i < size; i++) {
                if (!evaluateFilter((JsonArray) expression.get(i), feature)) {
                    return false;
                }
            }
            return true;
        case LAYER_FILTER_ANY:
            for (int i = 1; i < size; i++) {
                if (evaluateFilter((JsonArray) expression.get(i), feature)) {
                    return true;
                }
            }
            return false;
        default:
            Object result = evaluateExpression(expression, feature);
            return result instanceof Boolean ? (Boolean) result : result != null;
        }
    }

    /**
     * Compare am Object to a JsonElement
     * 
     * @param function how to compare them
     * @param left the Object
     * @param jsonElement the JsonElement
     * @return true if the condition we use to compare is met
     */
    private boolean compare(@NonNull String function, @Nullable Object left, @NonNull JsonElement jsonElement) {
        int result;
        if (left instanceof String) {
            result = ((String) left).compareTo(jsonElement.getAsString());
        } else if (left instanceof Integer) {
            result = Integer.compare((int) left, jsonElement.getAsInt());
        } else if (left instanceof Long) {
            result = Long.compare((long) left, jsonElement.getAsLong());
        } else if (left instanceof Float) {
            result = Float.compare((float) left, jsonElement.getAsFloat());
        } else if (left instanceof Double) {
            result = Double.compare((double) left, jsonElement.getAsDouble());
        } else if (left instanceof Boolean) {
            result = Boolean.compare((boolean) left, jsonElement.getAsBoolean());
        } else {
            Log.e(DEBUG_TAG, "compare unsupported object " + (left != null ? left.getClass().getCanonicalName() : "null") + " "
                    + jsonElement.getClass().getSimpleName());
            return false;
        }
        switch (function) {
        case LAYER_FILTER_EQ:
            return result == 0;
        case LAYER_FILTER_NOT_EQ:
            return result != 0;
        case LAYER_FILTER_LT:
            return result < 0;
        case LAYER_FILTER_LT_EQ:
            return result <= 0;
        case LAYER_FILTER_GT:
            return result > 0;
        case LAYER_FILTER_GT_EQ:
            return result >= 0;
        default:
            Log.e(DEBUG_TAG, "compare unsupported function " + function);
        }
        return false;
    }

    /**
     * Get the value for a specific key from the features attributes
     * 
     * @param feature the Feature
     * @param key the key
     * @return the value for the key
     */
    private static Object getKeyValue(VectorTileDecoder.Feature feature, String key) {
        switch (key) {
        case LAYER_KEY_TYPE:
            String type = feature.getGeometry().type();
            return GeoJSONConstants.MULTILINESTRING.equals(type) ? GeoJSONConstants.LINESTRING : type;
        case LAYER_KEY_ID:
            return feature.getId();
        default:
            return feature.getAttributes().get(key);
        }
    }

    /**
     * Evaluate a new style expression
     * 
     * @param expression the expression
     * @param feature the feature
     * @return the result
     */
    @Nullable
    public static Object evaluateExpression(@NonNull JsonArray expression, @NonNull VectorTileDecoder.Feature feature) {
        final int expressionSize = expression.size();
        if (expressionSize < 2) {
            Log.w(DEBUG_TAG, "Invalid expression " + expression);
            return null;
        }
        String function = expression.get(0).getAsString();
        switch (function) {
        case LAYER_EXPRESSION_HAS:
        case LAYER_EXPRESSION_NOT_HAS:
            if (expressionSize == 3) {
                Log.w(DEBUG_TAG, "Two argument versions of has and !has are not implemented");
                return null;
            }
            JsonElement arg1 = expression.get(1);
            String key = arg1.isJsonArray() ? evaluateExpression((JsonArray) arg1, feature).toString() : arg1.getAsString();
            Object left = getKeyValue(feature, key);
            return LAYER_EXPRESSION_HAS.equals(function) ? left != null : left == null;
        case LAYER_EXPRESSION_GET:
            if (expressionSize == 3) {
                Log.w(DEBUG_TAG, "Two argument version of get is not implemented");
                return null;
            }
            arg1 = expression.get(1);
            key = arg1.isJsonArray() ? evaluateExpression((JsonArray) arg1, feature).toString() : arg1.getAsString();
            return getKeyValue(feature, key);
        case LAYER_EXPRESSION_TO_BOOLEAN:
            arg1 = expression.get(1);
            Object o = arg1.isJsonArray() ? evaluateExpression((JsonArray) arg1, feature) : arg1;
            return isTrue(o);
        case LAYER_EXPRESSION_TO_NUMBER:
            if (expressionSize > 2) {
                Log.w(DEBUG_TAG, "Multiple argument version of to-number is not implemented");
            }
            arg1 = expression.get(1);
            o = arg1.isJsonArray() ? evaluateExpression((JsonArray) arg1, feature) : arg1;
            if (o instanceof String) {
                try {
                    return Double.parseDouble((String) o);
                } catch (NumberFormatException nfex) {
                    //
                }
            }
            if (o instanceof Boolean) {
                return ((Boolean) o).booleanValue() ? Integer.valueOf(1) : Integer.valueOf(0);
            }
            if (o instanceof Number) {
                return o;
            }
            Log.e(DEBUG_TAG, "Error parsing as a number  " + o + " " + expression);
            return Integer.valueOf(0);
        case LAYER_EXPRESSION_MATCH:
            arg1 = expression.get(1);
            o = arg1.isJsonArray() ? evaluateExpression((JsonArray) arg1, feature) : arg1;
            for (int i = 2; i < expressionSize - 1; i = i + 2) {
                JsonElement label = expression.get(i);
                if (label instanceof JsonArray) {
                    for (JsonElement e : (JsonArray) label) {
                        if (literalEquals(o, e)) {
                            return expression.get(i + 1);
                        }
                    }
                } else if (literalEquals(o, label)) {
                    return expression.get(i + 1);
                }
            }
            return expression.get(expressionSize - 1);
        default:
            Log.e(DEBUG_TAG, "Unknown/unsupported expression " + function);
        }
        return null;
    }

    /**
     * Test if the literals of an Object and a JsonElement are equal
     * 
     * @param o the Object
     * @param e the JsonElement
     * @return true if equal
     */
    private static boolean literalEquals(@Nullable Object o, @NonNull JsonElement e) {
        return (o instanceof JsonElement && o.equals(e)) || (o instanceof String && o.equals(e.getAsString()))
                || (o instanceof Number && o.equals(e.getAsNumber()));
    }

    /**
     * Check if Object o has a "true" value
     * 
     * @param o the Object
     * @return true if it corresponds to a "true" value
     */
    private static boolean isTrue(@Nullable Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        if (o instanceof Integer) {
            return ((Integer) o != 0);
        }
        if (o instanceof String) {
            return !("".equals(o) || FALSE.equals(o));
        }
        if (o instanceof JsonPrimitive) {
            JsonPrimitive p = (JsonPrimitive) o;
            if (p.isNumber()) {
                return p.getAsInt() != 0;
            }
            if (p.isString()) {
                String str = p.getAsString();
                return !(FALSE.equals(str) || "".equals(str));
            }
            Log.w(DEBUG_TAG, "isTrue unexpected value " + p.toString());
        }
        return true;
    }

    /**
     * Render the Feature or other content to the specified Canvas, using parameters from this Style
     * 
     * @param c the target Canvas
     * @param style the current style
     * @param feature the Feature or null
     * @param z current zoom level
     * @param screenRect total screen rect
     * @param destinationRect destination rect
     * @param scaleX scaling factor for tile in x direction
     * @param scaleY scaling factor for tile in x direction
     */
    @CallSuper
    public void render(@NonNull Canvas c, @NonNull Style style, @Nullable VectorTileDecoder.Feature feature, int z, @Nullable Rect screenRect,
            @NonNull Rect destinationRect, float scaleX, float scaleY) {
        if (!patternChecked) {
            patternChecked = true;
            Sprites sprites = style.getSprites();
            if (pattern.literal != null && sprites != null) {
                if (paint.getShader() == null) {
                    Bitmap bitmap = sprites.get(pattern.literal);
                    if (bitmap != null) {
                        BitmapShader bitmapShader = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
                        paint.setShader(bitmapShader);
                        return;
                    }
                }
                Log.w(DEBUG_TAG, "sprite or bitmap don't exist to set pattern from " + pattern);
            }
        }
    }

    /**
     * Called pre-render for things that need to be done on zoom change
     * 
     * @param style the current style
     * @param feature the Feature or null
     * @param z current zoom level
     */
    @CallSuper
    public void onZoomChange(@NonNull Style style, @Nullable VectorTileDecoder.Feature feature, int z) {
        color.eval(feature, z);
        opacity.eval(feature, z);
        pattern.eval(feature, z);
        lineCap.eval(feature, z);
        lineJoin.eval(feature, z);
    }

    /**
     * Evaluate a interpolation functions for floats
     * 
     * @param function the function
     * @param feature a feature
     * @param x x
     * @return the y for x
     */
    protected static double evalNumberFunction(@NonNull JsonObject function, @Nullable Feature feature, double x) {
        JsonElement type = function.get(INTERPOLATION_TYPE);
        JsonElement property = function.get(INTERPOLATION_PROPERTY);
        JsonElement defaultValue = function.get(INTERPOLATION_DEFAULT);
        if (Style.isString(property)) {
            if (feature != null) {
                Object o = feature.getAttributes().get(property.getAsString());
                if (o instanceof Number) {
                    x = ((Number) o).doubleValue();
                } else if (o instanceof String) {
                    x = Double.parseDouble((String) o);
                } else if (Style.isNumber(defaultValue)) {
                    return defaultValue.getAsDouble();
                }
            } else {
                Log.e(DEBUG_TAG, "Null feature but property provided " + property);
            }
        } else if (Style.isArray(property) && feature != null) { // expression
            Object o = evaluateExpression((JsonArray) property, feature);
            if (o instanceof Number) {
                x = ((Number) o).doubleValue();
            }
        }
        if (type == null || INTERPOLATION_TYPE_EXPONENTIAL.equals(type.getAsString())) {
            float base = getExponentialBase(function);
            JsonArray stops = getInterpolationStops(function);
            if (stops != null) {
                JsonArray start = (JsonArray) stops.get(0);
                float startX = start.get(0).getAsFloat();
                float startY = start.get(1).getAsFloat();
                if (x <= startX) {
                    return startY;
                }
                final int stopsSize = stops.size();
                for (int i = 1; i < stopsSize; i++) {
                    JsonArray next = (JsonArray) stops.get(i);
                    float nextX = next.get(0).getAsFloat();
                    float nextY = next.get(1).getAsFloat();
                    if (x <= nextX) {
                        return interpolation(base, startX, startY, nextX, nextY, x);
                    }
                    startX = nextX;
                    startY = nextY;
                }
                return startY;
            }
        } else if (INTERPOLATION_TYPE_IDENTITY.equals(type.getAsString())) {
            return x;
        } else {
            Log.e(DEBUG_TAG, "Unsupported interpolation function " + type);
        }
        return 0;
    }

    /**
     * Evaluate a interpolation functions for color
     * 
     * @param function the function
     * @param feature a feature
     * @param x x
     * @return the color for x
     */
    protected static int evalColorFunction(@NonNull JsonObject function, @Nullable Feature feature, int x) {
        JsonElement type = function.get(INTERPOLATION_TYPE);
        if (type == null || INTERPOLATION_TYPE_EXPONENTIAL.equals(type.getAsString())) {
            float base = getExponentialBase(function);
            JsonArray stops = getInterpolationStops(function);
            if (stops != null) {
                JsonArray start = (JsonArray) stops.get(0);
                double startX = start.get(0).getAsFloat();
                long startY = IntegerUtil.toUnsignedLong(Color.parseColor(start.get(1).getAsString()));
                if (x <= startX) {
                    return (int) startY;
                }
                long alpha = startY & ALPHA_ONLY;
                startY = startY & RGB_ONLY;
                final int stopsSize = stops.size();
                for (int i = 1; i < stopsSize; i++) {
                    JsonArray next = (JsonArray) stops.get(i);
                    float nextX = next.get(0).getAsFloat();
                    long nextY = IntegerUtil.toUnsignedLong(Color.parseColor(next.get(1).getAsString()));
                    alpha = nextY & ALPHA_ONLY;
                    nextY = nextY & RGB_ONLY;
                    if (x <= nextX) {
                        return (int) (Math.round(interpolation(base, startX, startY, nextX, nextY, x)) | alpha);
                    }
                    startX = nextX;
                    startY = nextY;
                }
                return (int) (startY | alpha);
            }
        }
        return 0;
    }

    /**
     * Evaluate a interpolation functions for float values in an array
     * 
     * @param function the function
     * @param feature a feature
     * @param x x
     * @return the array for x
     */
    protected static JsonArray evalArrayFunction(@NonNull JsonObject function, @Nullable Feature feature, int x) {
        JsonElement type = function.get(INTERPOLATION_TYPE);
        JsonArray result = new JsonArray();
        if (type == null || INTERPOLATION_TYPE_EXPONENTIAL.equals(type.getAsString())) {
            float base = getExponentialBase(function);
            JsonArray stops = getInterpolationStops(function);
            if (stops != null) {
                JsonArray start = (JsonArray) stops.get(0);
                double startX = start.get(0).getAsFloat();
                JsonArray startY = start.get(1).getAsJsonArray();
                if (x <= startX) {
                    return startY;
                }
                final int stopsSize = stops.size();
                for (int i = 1; i < stopsSize; i++) {
                    JsonArray next = (JsonArray) stops.get(i);
                    float nextX = next.get(0).getAsFloat();
                    JsonArray nextY = next.get(1).getAsJsonArray();
                    if (x <= nextX) {
                        // check that array elements are actually numbers?
                        for (int j = 0; j < nextY.size(); j++) {
                            result.add(new JsonPrimitive(interpolation(base, startX, startY.get(j).getAsDouble(), nextX, nextY.get(j).getAsDouble(), x)));
                        }
                        return result;
                    }
                    startX = nextX;
                    startY = nextY;
                }
                return startY;
            }
        }
        return new JsonArray();
    }

    /**
     * Get interpolation stops
     * 
     * @param function the JsonObject holding the values
     * @return an array of the stop values or null
     */
    @Nullable
    private static JsonArray getInterpolationStops(JsonObject function) {
        JsonElement stops = function.get(Style.INTERPOLATION_STOPS);
        return stops != null && stops.isJsonArray() ? (JsonArray) stops : null;
    }

    /**
     * Get the base for an exponential interpolation
     * 
     * @param function the JsonObject holding the values
     * @return a float value for the base, 1 if no value can be found
     */
    private static float getExponentialBase(JsonObject function) {
        JsonElement temp = function.get(INTERPOLATION_TYPE_EXPONENTIONAL_BASE);
        float base = 1;
        if (temp != null && temp.isJsonPrimitive()) {
            base = temp.getAsFloat();
        }
        return base;
    }

    /**
     * Evaluate a interpolation functions for "categories"
     * 
     * @param function the function
     * @param feature a feature
     * @param x x
     * @return the "category"" for x
     */
    @Nullable
    protected static JsonElement evalCategoryFunction(@NonNull JsonObject function, @Nullable Feature feature, int x) {
        JsonElement type = function.get(INTERPOLATION_TYPE);
        if (type == null || INTERPOLATION_TYPE_CATEGORY.equals(type.getAsString())) {
            JsonArray stops = getInterpolationStops(function);
            if (stops != null) {
                JsonArray start = (JsonArray) stops.get(0);
                double startX = start.get(0).getAsFloat();
                JsonElement startY = start.get(1);
                if (x <= startX) {
                    return startY;
                }
                final int stopsSize = stops.size();
                for (int i = 1; i < stopsSize; i++) {
                    JsonArray next = (JsonArray) stops.get(i);
                    float nextX = next.get(0).getAsFloat();
                    JsonElement nextY = next.get(1);
                    if (x <= nextX) {
                        return nextY;
                    }
                    startY = nextY;
                }
                return startY;
            }
        }
        return null;
    }

    /**
     * Return a interpolated value from a exponential or linear function
     * 
     * @param base the base if 1 we will use linear interpolation
     * @param x1 x of first point
     * @param y1 y of first point
     * @param x2 x of 2nd point
     * @param y2 y of 2nd point
     * @param x the x value we want to interpolate for
     * @return f(x)
     */
    static double interpolation(double base, double x1, double y1, double x2, double y2, double x) {
        if (base == 1D) { // actually linear, problematic construct as we shouldn't be comparing FP numbers
            final double a = (x2 * y1 - x1 * y2) / (x2 - x1);
            final double s = (y2 - a) / x2;
            return x * s + a;
        }
        final double dY = y1 - y2;
        final double dX = Math.pow(base, x1) - Math.pow(base, x2);
        double h = logBase(base, dX / dY);
        double k = y1 - Math.pow(base, x1) * dY / dX;

        return Math.pow(base, x - h) + k;
    }

    /**
     * Calculate the log of x in the specified base
     * 
     * @param base the base
     * @param x x
     * @return log x
     */
    private static double logBase(double base, double x) {
        return Math.log(x) / Math.log(base);
    }

    /**
     * Serialize this object
     * 
     * @param out ObjectOutputStream to write to
     * @throws IOException if writing fails
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(filter != null ? filter.toString() : null);
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
        filter = temp != null ? (JsonArray) JsonParser.parseString(temp.toString()) : null;
        this.path = new Path();
        patternChecked = false;
    }

    @Override
    @NonNull
    public String toString() {
        return iD + " " + sourceLayer + " " + minZoom + " " + maxZoom;
    }
}
