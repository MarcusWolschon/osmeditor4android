package de.blau.android.util.mvt.style;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.android.sprites.Sprites;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Schemes;
import de.blau.android.osm.Server;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.symbols.TriangleDown;
import de.blau.android.util.ColorUtil;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.util.mvt.style.Source.SourceType;

public class Style implements Serializable {

    private static final String DEBUG_TAG = Style.class.getSimpleName().substring(0, Math.min(23, Style.class.getSimpleName().length()));

    /**
     * 
     */
    private static final long serialVersionUID = 7L;

    private static final String STYLE_SOURCES             = "sources";
    private static final String STYLE_SPRITE              = "sprite";
    private static final String STYLE_LAYERS              = "layers";
    private static final String STYLE_VERSION             = "version";
    private static final String SOURCE_TYPE               = "type";
    private static final String SOURCE_TYPE_VECTOR        = "vector";
    private static final String SOURCE_TILES              = "tiles";
    private static final String SOURCE_MINZOOM            = "minzoom";
    private static final String SOURCE_MAXZOOM            = "maxzoom";
    private static final String SOURCE_ATTRIBUTION        = "attribution";
    private static final String SOURCE_BOUNDS             = "bounds";
    private static final String ICON_SIZE                 = "icon-size";
    private static final String ICON_OFFSET               = "icon-offset";
    private static final String TEXT_OFFSET               = "text-offset";
    private static final String ICON_ANCHOR               = "icon-anchor";
    private static final String ICON_IMAGE                = "icon-image";
    private static final String ICON_ROTATE               = "icon-rotate";
    private static final String SYMBOL_PLACEMENT          = "symbol-placement";
    static final String         INTERPOLATION_STOPS       = "stops";
    private static final String TEXT_HALO_COLOR           = "text-halo-color";
    private static final String TEXT_HALO_WIDTH           = "text-halo-width";
    private static final String TEXT_ANCHOR               = "text-anchor";
    public static final String  TEXT_JUSTIFY_CENTER       = "center";
    public static final String  TEXT_JUSTIFY_RIGHT        = "right";
    public static final String  TEXT_JUSTIFY_LEFT         = "left";
    private static final String TEXT_JUSTIFY              = "text-justify";
    static final String         TEXT_TRANSFORM_NONE       = "none";
    static final String         TEXT_TRANSFORM_LOWERCASE  = "lowercase";
    static final String         TEXT_TRANSFORM_UPPERCASE  = "uppercase";
    private static final String TEXT_TRANSFORM            = "text-transform";
    private static final String TEXT_LETTER_SPACING       = "text-letter-spacing";
    private static final String TEXT_SIZE                 = "text-size";
    private static final String TEXT_FIELD                = "text-field";
    private static final String TEXT_OPACITY              = "text-opacity";
    private static final String TEXT_COLOR                = "text-color";
    private static final String TEXT_MAX_WIDTH            = "text-max-width";
    private static final String ICON_COLOR                = "icon-color";
    private static final String LAYER_TYPE_SYMBOL         = "symbol";
    private static final String FILL_EXTRUSION_COLOR      = "fill-extrusion-color";
    private static final String FILL_EXTRUSION_OPACITY    = "fill-extrusion-opacity";
    private static final String FILL_EXTRUSION_PATTERN    = "fill-extrusion-pattern";
    private static final String FILL_EXTRUSION_TRANSLATE  = "fill-extrusion-translate";
    private static final String LAYER_TYPE_FILL_EXTRUSION = "fill-extrusion";
    private static final String FILL_PATTERN              = "fill-pattern";
    private static final String FILL_OUTLINE_COLOR        = "fill-outline-color";
    private static final String FILL_TRANSLATE            = "fill-translate";
    private static final String FILL_OPACITY              = "fill-opacity";
    private static final String FILL_COLOR                = "fill-color";
    private static final String FILL_ANTIALIAS            = "fill-antialias";
    private static final String LAYER_TYPE_FILL           = "fill";
    private static final String LINE_JOIN                 = "line-join";
    private static final String LINE_CAP                  = "line-cap";
    private static final String LINE_DASHARRAY            = "line-dasharray";
    private static final String LINE_WIDTH                = "line-width";
    private static final String LINE_OPACITY              = "line-opacity";
    private static final String LINE_COLOR                = "line-color";
    private static final String LAYER_TYPE_LINE           = "line";
    private static final String BACKGROUND_PATTERN        = "background-pattern";
    private static final String BACKGROUND_OPACITY        = "background-opacity";
    private static final String BACKGROUND_COLOR          = "background-color";
    private static final String LAYER_TYPE_BACKGROUND     = "background";
    private static final String CIRCLE_RADIUS             = "circle-radius";
    private static final String CIRCLE_STROKE_COLOR       = "circle-stroke-color";
    private static final String CIRCLE_STROKE_OPACITY     = "circle-stroke-opacity";
    private static final String CIRCLE_STROKE_WIDTH       = "circle-stroke-width";
    private static final String CIRCLE_TRANSLATE          = "circle-translate";
    private static final String CIRCLE_OPACITY            = "circle-opacity";
    private static final String CIRCLE_COLOR              = "circle-color";
    private static final String LAYER_TYPE_CIRCLE         = "circle";
    private static final String LAYER_MAXZOOM             = "maxzoom";
    private static final String LAYER_MINZOOM             = "minzoom";
    private static final String LAYER_ID                  = "id";
    private static final String LAYER_REF                 = "ref";
    private static final String LAYER_INTERACTIVE         = "interactive";
    private static final Object LAYER_VISIBILITY_VISIBLE  = "visible";
    private static final String LAYER_VISIBLITY           = "visibility";
    private static final String LAYER_SOURCE_LAYER        = "source-layer";
    private static final String LAYER_PAINT               = "paint";
    private static final String LAYER_LAYOUT              = "layout";
    private static final String LAYER_FILTER              = "filter";
    private static final String LAYER_TYPE                = "type";

    private static final String RETINA = "@2x";

    private static final List<String> SOURCE_LAYER_REQUIRED = Arrays.asList(LAYER_TYPE_FILL, LAYER_TYPE_FILL_EXTRUSION, LAYER_TYPE_LINE, LAYER_TYPE_SYMBOL,
            LAYER_TYPE_CIRCLE);

    private int                               version;
    private final MultiHashMap<String, Layer> layerMap   = new MultiHashMap<>();
    private final List<Layer>                 layers     = new ArrayList<>();
    private boolean                           autoStyles = true;
    private Sprites                           sprites;
    private Map<String, Source>               sources    = new HashMap<>();

    private transient CollisionDetector detector = new SimpleCollisionDetector();

    /**
     * Add a layer for a specific source layer
     * 
     * @param sourceLayer the source layer name
     * @param layerStyle the style to use
     */
    public void addLayer(@NonNull String sourceLayer, @NonNull Layer layerStyle) {
        Log.d(DEBUG_TAG, "setting style for " + sourceLayer);
        synchronized (layers) {
            layerMap.add(sourceLayer, layerStyle);
            layers.add(layerStyle);
        }
    }

    /**
     * Replace the layers with the argument
     * 
     * @param newLayers a Map of the new styles
     */
    public void setLayers(@NonNull List<Layer> newLayers) {
        synchronized (layers) {
            layerMap.clear();
            layers.clear();
            layers.addAll(newLayers);
            for (Layer layer : layers) {
                String sourceLayer = layer.getSourceLayer();
                if (sourceLayer != null) {
                    layerMap.add(sourceLayer, layer);
                }
            }
        }
    }

    /**
     * Get all Layers for this Style
     * 
     * @return a List of the Styles
     */
    @NonNull
    public List<Layer> getLayers() {
        return layers;
    }

    /**
     * Get all Layers for a specific source layer
     * 
     * @param sourceLayer the name of the source layer
     * @return the Layers
     */
    @NonNull
    public Collection<Layer> getLayers(@NonNull String sourceLayer) {
        return layerMap.get(sourceLayer);
    }

    /**
     * Get all Layer with a specific id
     * 
     * @param id the id
     * @return the layer or null if not found
     */
    @Nullable
    public Layer getLayer(@NonNull String id) {
        for (Layer layer : layers) {
            if (id.equals(layer.getId())) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Load and parse mapbox gl styles from an InputStream
     * 
     * @param ctx an Android Context
     * @param is the InputStream
     */
    @SuppressLint("NewApi") // StandardCharsets is desugared for APIs < 19.
    public void loadStyle(@NonNull Context ctx, @NonNull InputStream is) {
        List<Layer> tempList = new ArrayList<>();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            JsonElement root = JsonParser.parseReader(rd);
            if (root.isJsonObject()) {
                JsonObject rootObject = (JsonObject) root;
                JsonElement temp = rootObject.get(STYLE_VERSION);
                if (temp != null) {
                    version = temp.getAsInt();
                }
                temp = rootObject.get(STYLE_SOURCES);
                if (isObject(temp)) {
                    getSources((JsonObject) temp);
                }
                temp = rootObject.getAsJsonPrimitive(STYLE_SPRITE);
                if (temp != null) {
                    getSprites(ctx, temp.getAsString());
                }
                JsonArray layerArray = rootObject.getAsJsonArray(STYLE_LAYERS);
                if (layerArray != null) {
                    for (JsonElement layer : layerArray) {
                        if (layer.isJsonObject()) {
                            Layer style = parseLayer(ctx, layerArray, (JsonObject) layer);
                            if (style != null) {
                                tempList.add(style);
                            }
                        }
                    }
                } else {
                    Log.e(DEBUG_TAG, "No layers found");
                }
            }
            if (!tempList.isEmpty()) {
                autoStyles = false;
                setLayers(tempList);
            }
        } catch (IOException | JsonSyntaxException e) {
            Log.e(DEBUG_TAG, "Opening " + e.getMessage());
        }
    }

    /**
     * Add sources from the sources object
     * 
     * Only adds vector sources and does not support TileJson
     * 
     * @param sourcesObject the input JsonObject
     */
    private void getSources(@NonNull JsonObject sourcesObject) {
        for (Entry<String, JsonElement> entry : sourcesObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                JsonObject valueObject = (JsonObject) value;
                JsonElement type = valueObject.get(SOURCE_TYPE);
                JsonElement tiles = valueObject.get(SOURCE_TILES);
                if (isString(type) && SOURCE_TYPE_VECTOR.equals(type.getAsString()) && isArray(tiles) && ((JsonArray) tiles).size() > 0) {
                    Source source = new Source(SourceType.VECTOR);
                    sources.put(entry.getKey(), source);
                    int size = ((JsonArray) tiles).size();
                    source.tileUrls = new String[size];
                    for (int i = 0; i < size; i++) {
                        source.getTileUrls()[i] = ((JsonArray) tiles).get(i).getAsString();
                    }
                    JsonElement minZoom = valueObject.get(SOURCE_MINZOOM);
                    if (isNumber(minZoom)) {
                        source.minZoom = minZoom.getAsInt();
                    }
                    JsonElement maxZoom = valueObject.get(SOURCE_MAXZOOM);
                    if (isNumber(maxZoom)) {
                        source.maxZoom = maxZoom.getAsInt();
                    }
                    JsonElement attribution = valueObject.get(SOURCE_ATTRIBUTION);
                    if (isString(attribution)) {
                        source.attribution = attribution.getAsString();
                    }
                    JsonElement bounds = valueObject.get(SOURCE_BOUNDS);
                    if (isArray(bounds) && ((JsonArray) bounds).size() == 4) {
                        JsonArray boundsArray = ((JsonArray) bounds);
                        try {
                            source.bounds.set((int) (boundsArray.get(0).getAsDouble() * 1E7), (int) (boundsArray.get(1).getAsDouble() * 1E7),
                                    (int) (boundsArray.get(2).getAsDouble() * 1E7), (int) (boundsArray.get(3).getAsDouble() * 1E7));
                        } catch (IllegalStateException isex) {
                            Log.e(DEBUG_TAG, "Not a legal bounding box " + bounds);
                        }
                    }
                }
            }
        }
    }

    /**
     * Load sprites async
     * 
     * @param ctx an Android Context
     * @param url the Url to load the sprites from
     */
    private void getSprites(@NonNull Context ctx, @NonNull String url) {
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Sprites>(logic.getExecutorService(), logic.getHandler()) {

            /**
             * Get the sheet and image for a sprite url
             * 
             * @param fullUrl the url without file extensions
             * @return a Sprites object or null
             */
            @Nullable
            Sprites get(@NonNull String fullUrl) {
                try {
                    final Uri jsonUri = Uri.parse(fullUrl + "." + FileExtensions.JSON);
                    final Uri imageUri = Uri.parse(fullUrl + "." + FileExtensions.PNG);
                    if (Schemes.FILE.equals(jsonUri.getScheme())) {
                        final ContentResolver contentResolver = ctx.getContentResolver();
                        try (BufferedInputStream sheet = new BufferedInputStream(contentResolver.openInputStream(jsonUri));
                                BufferedInputStream image = new BufferedInputStream(contentResolver.openInputStream(imageUri))) {
                            return new Sprites(ctx, sheet, image);
                        }
                    } else {
                        try (InputStream sheet = Server.openConnection(null, new URL(jsonUri.toString()));
                                InputStream image = Server.openConnection(null, new URL(imageUri.toString()))) {
                            return new Sprites(ctx, sheet, image);
                        }
                    }
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "getSprites " + e.getMessage());
                }
                return null;
            }

            @Override
            protected Sprites doInBackground(Void param) {
                Sprites result = get(url + RETINA); // try "retina" resolution 1st
                if (result == null) {
                    return get(url);
                } else {
                    result.setRetina(true);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Sprites result) {
                Log.i(DEBUG_TAG, "getSprites " + url + (result != null ? " loaded" : " not loaded"));
                sprites = result;
            }
        }.execute();
    }

    /**
     * Parse a Mapbox-GL style layer object
     * 
     * @param ctx an Android Context
     * @param layers the existing Layers
     * @param layer the layer JsonObject
     * @return a Layer
     */
    @Nullable
    private Layer parseLayer(@NonNull Context ctx, @NonNull JsonArray layers, @NonNull JsonObject layer) {
        JsonElement tempElement = layer.get(LAYER_REF);
        JsonObject refLayer = null;
        if (isString(tempElement)) {
            String ref = tempElement.getAsString();
            for (JsonElement l : layers) {
                if (l.isJsonObject()) {
                    tempElement = ((JsonObject) l).get(LAYER_ID);
                    if (tempElement != null && tempElement.isJsonPrimitive() && ref.equals(tempElement.getAsString())) {
                        refLayer = (JsonObject) l;
                        break;
                    }
                }
            }
            if (refLayer != null) {
                copyObject(LAYER_TYPE, refLayer, layer);
                copyObject(LAYER_FILTER, refLayer, layer);
                copyObject(LAYER_LAYOUT, refLayer, layer);
                copyObject(LAYER_PAINT, refLayer, layer);
                copyObject(LAYER_SOURCE_LAYER, refLayer, layer);
                copyObject(LAYER_VISIBLITY, refLayer, layer);
            }
        }
        tempElement = layer.get(LAYER_TYPE);
        String type = null;
        if (isString(tempElement)) {
            type = tempElement.getAsString();
        } else {
            Log.e(DEBUG_TAG, "layer without type " + layer);
            return null;
        }
        tempElement = layer.get(LAYER_SOURCE_LAYER);
        String sourceLayer = null;
        if (isString(tempElement)) {
            sourceLayer = tempElement.getAsString();
        } else if (SOURCE_LAYER_REQUIRED.contains(type)) {
            Log.e(DEBUG_TAG, "layer without required source layer " + layer);
            return null;
        }
        String iD = layer.get(LAYER_ID).getAsString();
        tempElement = layer.get(LAYER_FILTER);
        JsonArray filter = null;
        if (isArray(tempElement)) {
            filter = tempElement.getAsJsonArray();
        }
        tempElement = layer.get(LAYER_LAYOUT);
        JsonObject layout = null;
        if (isObject(tempElement)) {
            layout = tempElement.getAsJsonObject();
        }
        tempElement = layer.get(LAYER_PAINT);
        JsonObject paint = null;
        if (isObject(tempElement)) {
            paint = tempElement.getAsJsonObject();
        }
        tempElement = layer.get(LAYER_MINZOOM);
        int minZoom = 0;
        if (isNumber(tempElement)) {
            minZoom = tempElement.getAsInt();
        }
        tempElement = layer.get(LAYER_MAXZOOM);
        int maxZoom = -1;
        if (isNumber(tempElement)) {
            maxZoom = tempElement.getAsInt();
        }
        tempElement = layer.get(LAYER_INTERACTIVE);
        boolean interactive = true;
        if (isBoolean(tempElement)) {
            interactive = tempElement.getAsBoolean();
        }

        switch (type) {
        case LAYER_TYPE_BACKGROUND:
            Background background = new Background();
            background.setId(iD);
            if (paint != null) {
                background.color.set(ctx, BACKGROUND_COLOR, paint);
                background.opacity.set(ctx, BACKGROUND_OPACITY, paint);
                background.pattern.set(ctx, BACKGROUND_PATTERN, paint);
            }
            if (layout != null) {
                setVisibility(layout, background);
            }
            return background;
        case LAYER_TYPE_LINE:
            Line line = new Line(sourceLayer); // NOSONAR
            setBasicAttributes(line, iD, filter, minZoom, maxZoom, interactive);
            if (paint != null) {
                line.color.set(ctx, LINE_COLOR, paint);
                line.opacity.set(ctx, LINE_OPACITY, paint);
                line.lineWidth.set(ctx, LINE_WIDTH, paint, Line.DEFAULT_LINE_WIDTH);
                JsonElement lineDashArray = paint.get(LINE_DASHARRAY);
                if (lineDashArray != null) {
                    if (lineDashArray.isJsonArray()) {
                        List<Float> dashArray = new ArrayList<>();
                        for (JsonElement e : (JsonArray) lineDashArray) {
                            if (e.isJsonPrimitive()) {
                                dashArray.add(e.getAsFloat());
                            }
                        }
                        if (!dashArray.isEmpty()) {
                            line.setDashArray(dashArray);
                        }
                    } else {
                        Log.w(DEBUG_TAG, "Unsupported line-dasharray value " + lineDashArray);
                    }
                }
            }
            if (layout != null) {
                setVisibility(layout, line);
                line.lineCap.set(ctx, LINE_CAP, layout);
                line.lineJoin.set(ctx, LINE_JOIN, layout);
            }
            return line;
        case LAYER_TYPE_FILL:
            Fill fill = new Fill(sourceLayer); // NOSONAR
            setBasicAttributes(fill, iD, filter, minZoom, maxZoom, interactive);
            if (paint != null) {
                fill.color.set(ctx, FILL_COLOR, paint);
                fill.opacity.set(ctx, FILL_OPACITY, paint);
                fill.pattern.set(ctx, FILL_PATTERN, paint);
                fill.outlineColor.set(ctx, FILL_OUTLINE_COLOR, paint);
                JsonElement fillAntiAlias = paint.get(FILL_ANTIALIAS);
                if (fillAntiAlias != null) {
                    if (isBoolean(fillAntiAlias)) {
                        fill.setAntiAlias(fillAntiAlias.getAsBoolean());
                    } else {
                        Log.w(DEBUG_TAG, "Unsupported fill-antialias value " + fillAntiAlias);
                    }
                }
                fill.fillTranslate.set(ctx, FILL_TRANSLATE, paint);
            }
            if (layout != null) {
                setVisibility(layout, fill);
            }
            return fill;
        case LAYER_TYPE_FILL_EXTRUSION:
            FillExtrusion fillExtrusion = new FillExtrusion(sourceLayer); // NOSONAR
            setBasicAttributes(fillExtrusion, iD, filter, minZoom, maxZoom, interactive);
            if (paint != null) {
                fillExtrusion.color.set(ctx, FILL_EXTRUSION_COLOR, paint);
                fillExtrusion.opacity.set(ctx, FILL_EXTRUSION_OPACITY, paint);
                fillExtrusion.pattern.set(ctx, FILL_EXTRUSION_PATTERN, paint);
                fillExtrusion.fillTranslate.set(ctx, FILL_EXTRUSION_TRANSLATE, paint);
            }
            if (layout != null) {
                setVisibility(layout, fillExtrusion);
            }
            return fillExtrusion;
        case LAYER_TYPE_SYMBOL:
            Symbol symbol = new Symbol(sourceLayer); // NOSONAR
            setBasicAttributes(symbol, iD, filter, minZoom, maxZoom, interactive);
            symbol.setCollisionDetector(detector);
            if (paint != null) {
                symbol.color.set(ctx, ICON_COLOR, paint);
                symbol.textColor.set(ctx, TEXT_COLOR, paint);
                symbol.textOpacity.set(ctx, TEXT_OPACITY, paint);
                symbol.textHaloWidth.set(ctx, TEXT_HALO_WIDTH, paint);
                symbol.textHaloColor.set(ctx, TEXT_HALO_COLOR, paint);
            }
            if (layout != null) {
                setVisibility(layout, symbol);
                symbol.label.set(ctx, TEXT_FIELD, layout);
                symbol.textSize.set(ctx, TEXT_SIZE, layout, 16f);
                symbol.textMaxWidth.set(ctx, TEXT_MAX_WIDTH, layout, Symbol.DEFAULT_TEXT_MAX_WIDTH);
                symbol.textLetterSpacing.set(ctx, TEXT_LETTER_SPACING, layout);
                symbol.textTransform.set(ctx, TEXT_TRANSFORM, layout);
                symbol.textJustify.set(ctx, TEXT_JUSTIFY, layout);
                symbol.textAnchor.set(ctx, TEXT_ANCHOR, layout);
                symbol.textOffset.set(ctx, TEXT_OFFSET, layout);
                symbol.symbolPlacement.set(ctx, SYMBOL_PLACEMENT, layout);
                symbol.iconImage.set(ctx, ICON_IMAGE, layout);
                symbol.iconRotate.set(ctx, ICON_ROTATE, layout);
                symbol.iconAnchor.set(ctx, ICON_ANCHOR, layout);
                symbol.iconOffset.set(ctx, ICON_OFFSET, layout);
                symbol.iconSize.set(ctx, ICON_SIZE, layout, 1f);
            }
            return symbol;
        case LAYER_TYPE_CIRCLE:
            Circle circle = new Circle(sourceLayer); // NOSONAR
            setBasicAttributes(circle, iD, filter, minZoom, maxZoom, interactive);
            if (paint != null) {
                circle.color.set(ctx, CIRCLE_COLOR, paint);
                circle.opacity.set(ctx, CIRCLE_OPACITY, paint);
                circle.strokeColor.set(ctx, CIRCLE_STROKE_COLOR, paint);
                circle.strokeOpacity.set(ctx, CIRCLE_STROKE_OPACITY, paint);
                circle.strokeWidth.set(ctx, CIRCLE_STROKE_WIDTH, paint);
                circle.circleTranslate.set(ctx, CIRCLE_TRANSLATE, paint);
                circle.circleRadius.set(ctx, CIRCLE_RADIUS, paint, Circle.DEFAULT_RADIUS);
            }
            if (layout != null) {
                setVisibility(layout, circle);
            }
            return circle;
        default:
            // ignore
        }
        return null;
    }

    /**
     * Set some basic attributes for a vector layer
     * 
     * @param layer the Layer
     * @param iD the id
     * @param filter a filter if any
     * @param minZoom minimum zoom
     * @param maxZoom maximum zoom
     * @param interactive true if the layer is interactive
     */
    private void setBasicAttributes(@NonNull Layer layer, @NonNull String iD, @Nullable JsonArray filter, int minZoom, int maxZoom, boolean interactive) {
        layer.setId(iD);
        layer.setFilter(filter);
        layer.setMinZoom(minZoom);
        layer.setMaxZoom(maxZoom);
        layer.setInteractive(interactive);
    }

    /**
     * Set a layers visibility
     * 
     * @param layout the JSON layout object
     * @param layer the Layer
     */
    private void setVisibility(JsonObject layout, Layer layer) {
        JsonElement visibility = layout.get(LAYER_VISIBLITY);
        if (isString(visibility)) {
            layer.setVisible(LAYER_VISIBILITY_VISIBLE.equals(visibility.getAsString()));
        }
    }

    /**
     * Test if a JsonElement is a String
     * 
     * @param element the JsonElement
     * @return true if a String
     */
    static boolean isString(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isString();
    }

    /**
     * Test if a JsonElement is a Number
     * 
     * @param element the JsonElement
     * @return true if a Number
     */
    static boolean isNumber(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber();
    }

    /**
     * Test if a JsonElement is a Boolean
     * 
     * @param element the JsonElement
     * @return true if a Boolean
     */
    static boolean isBoolean(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean();
    }

    /**
     * Test if a JsonElement is an JsonObject
     * 
     * @param element the JsonElement
     * @return true if a Boolean
     */
    private boolean isObject(@Nullable JsonElement element) {
        return element != null && element.isJsonObject();
    }

    /**
     * Test if a JsonElement is an JsonObject
     * 
     * @param element the JsonElement
     * @return true if a Boolean
     */
    static boolean isArray(@Nullable JsonElement element) {
        return element != null && element.isJsonArray();
    }

    /**
     * Copy an object from one JsonObject to the destination if it is missing
     * 
     * @param name of the JsonObject
     * @param from source JsonObject
     * @param to destination JsonObject
     */
    private void copyObject(@NonNull String name, @NonNull JsonObject from, @NonNull JsonObject to) {
        if (!to.has(name) && from.has(name)) {
            to.add(name, from.get(name).deepCopy());
        }
    }

    /**
     * Enable/disable automatic style generation
     * 
     * @param enabled if true enable
     */
    public void setAutoStyle(boolean enabled) {
        this.autoStyles = enabled;
    }

    /**
     * Check if automatically generating a style is enabled
     * 
     * @return true if enabled
     */
    public boolean isAutoStyle() {
        return autoStyles;
    }

    /**
     * Add three automatically generate Layers for the supplied source layer
     * 
     * @param map the current Map instance
     * 
     * @param sourceLayer the name of the source layer
     */
    public void addAutoLayers(@NonNull de.blau.android.Map map, @NonNull String sourceLayer) {
        if (!layerMap.containsKey(sourceLayer)) {
            DataStyle styles = map.getDataStyle();
            synchronized (layers) {
                // add default styles
                Paint paint = new Paint(styles.getInternal(DataStyle.MVT_DEFAULT).getPaint());
                paint.setColor(ColorUtil.generateColor(layers.size() / 3, 11, paint.getColor()));
                Line defaultLine = Line.fromPaint(sourceLayer, paint);
                layerMap.add(sourceLayer, defaultLine);
                layers.add(defaultLine);
                Fill defaultFill = Fill.fromPaint(sourceLayer, paint);
                layerMap.add(sourceLayer, defaultFill);
                layers.add(defaultFill);
                Symbol defaultSymbol = Symbol.fromPaint(sourceLayer, paint, styles.getInternal(DataStyle.LABELTEXT_NORMAL).getPaint(), TriangleDown.NAME,
                        styles);
                layerMap.add(sourceLayer, defaultSymbol);
                layers.add(defaultSymbol);
                Collections.sort(layers, LAYER_TYPE_COMPARATOR);
            }
        }
    }

    /**
     * Set the sprites
     * 
     * @param sprite the Sprite object or null
     */
    void setSprites(@Nullable Sprites sprite) {
        sprites = sprite;
    }

    /**
     * Get the Sprites object for this style
     * 
     * @return the sprites
     */
    @Nullable
    public Sprites getSprites() {
        return sprites;
    }

    /**
     * Get any configured sources for this style
     * 
     * @return a Map containing the sources
     */
    @NonNull
    public Map<String, Source> getSources() {
        return sources;
    }

    /**
     * 
     * Comparator for sorting Fill - Line - Symbol order
     *
     */
    private static final Comparator<Layer> LAYER_TYPE_COMPARATOR = (Layer s1, Layer s2) -> {
        if (s1 instanceof Fill) {
            if (!(s2 instanceof Fill)) {
                return -1;
            }
            return 0;
        }
        if (s1 instanceof Line) {
            if (s2 instanceof Fill) {
                return 1;
            } else if (s2 instanceof Symbol) {
                return -1;
            }
            return 0;
        }
        if (s1 instanceof Symbol) {
            if (!(s2 instanceof Symbol)) {
                return 1;
            }
            return 0;
        }
        return 0; // throw an exception?
    };

    /**
     * Reset the container for label bounds
     */
    public void resetCollisionDetection() {
        detector.reset();
    }

    /**
     * Read serialized object
     * 
     * @param stream the input stream
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the Class to deserialize can't be found
     */
    private void readObject(@NonNull ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        detector = new SimpleCollisionDetector();
        for (Layer layer : layers) {
            if (layer instanceof Symbol) {
                ((Symbol) layer).setCollisionDetector(detector);
            }
        }
    }
}
