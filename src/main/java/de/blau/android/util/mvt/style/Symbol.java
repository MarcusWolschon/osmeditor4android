package de.blau.android.util.mvt.style;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.android.sprites.Sprites;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SerializablePaint;
import de.blau.android.util.SerializableTextPaint;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileDecoder.Feature;

public class Symbol extends Layer {

    private static final String DEBUG_TAG = Symbol.class.getSimpleName();

    private static final long serialVersionUID = 16L;

    static final String         SYMBOL_PLACEMENT_POINT       = "point";
    private static final String SYMBOL_PLACEMENT_LINE_CENTER = "line-center";
    public static final String  SYMBOL_PLACEMENT_LINE        = "line";
    private static final String SYMBOL_ANCHOR_BOTTOM_RIGHT   = "bottom-right";
    private static final String SYMBOL_ANCHOR_BOTTOM_LEFT    = "bottom-left";
    private static final String SYMBOL_ANCHOR_TOP_RIGHT      = "top-right";
    private static final String SYMBOL_ANCHOR_TOP_LEFT       = "top-left";
    private static final String SYMBOL_ANCHOR_BOTTOM         = "bottom";
    private static final String SYMBOL_ANCHOR_TOP            = "top";
    private static final String SYMBOL_ANCHOR_RIGHT          = "right";
    private static final String SYMBOL_ANCHOR_LEFT           = "left";
    private static final String SYMBOL_ANCHOR_CENTER         = "center";

    public static final float DEFAULT_TEXT_SIZE      = 16f; // this needs to be converted to screen px
    public static final float DEFAULT_TEXT_MAX_WIDTH = 10f;

    private static final int FUDGE = 6;

    StringStyleAttribute          iconImage = new StringStyleAttribute();
    private String                symbolName;
    private transient Path        symbolPath;
    StringStyleAttribute          label     = new StringStyleAttribute();
    private String                labelKey;
    private SerializableTextPaint labelPaint;
    private transient FontMetrics labelFontMetrics;

    ColorStyleAttribute      textColor         = new ColorStyleAttribute() {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(int color) {
                                                       int tempAlpha = labelPaint.getAlpha();
                                                       labelPaint.setColor(color);
                                                       if (color >>> 24 == 0) {
                                                           labelPaint.setAlpha(tempAlpha);
                                                       }
                                                   }
                                               };
    FloatStyleAttribute      textOpacity       = new FloatStyleAttribute(true) {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(float opacity) {
                                                       labelPaint.setAlpha(Math.round(opacity * 255));
                                                   }
                                               };
    StringStyleAttribute     textTransform     = new StringStyleAttribute() {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(String transform) {
                                                       literal = !Style.TEXT_TRANSFORM_NONE.equals(transform) ? transform : null;
                                                   }
                                               };
    StringStyleAttribute     symbolPlacement   = new StringStyleAttribute() {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(String placement) {
                                                       if (placement == null) {
                                                           literal = null;
                                                           return;
                                                       }
                                                       switch (placement) {
                                                       case SYMBOL_PLACEMENT_POINT:
                                                           literal = null;
                                                           break;
                                                       case SYMBOL_PLACEMENT_LINE:
                                                       case SYMBOL_PLACEMENT_LINE_CENTER:
                                                           literal = placement;
                                                           break;
                                                       default:
                                                           Log.w(DEBUG_TAG, "Unknown smybol-placement value " + placement);
                                                       }
                                                   }
                                               };
    FloatStyleAttribute      iconRotate        = new FloatStyleAttribute(false);
    StringStyleAttribute     textAnchor        = new StringStyleAttribute() {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(String anchor) {
                                                       literal = getAnchor(anchor);
                                                   }
                                               };
    StringStyleAttribute     iconAnchor        = new StringStyleAttribute() {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(String anchor) {
                                                       literal = getAnchor(anchor);
                                                   }
                                               };
    FloatArrayStyleAttribute textOffset        = new FloatArrayStyleAttribute(false);
    FloatArrayStyleAttribute iconOffset        = new FloatArrayStyleAttribute(false);
    FloatStyleAttribute      iconSize          = new FloatStyleAttribute(true);
    FloatStyleAttribute      textMaxWidth      = new FloatStyleAttribute(false);
    FloatStyleAttribute      textSize          = new FloatStyleAttribute(true) {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(float value) {
                                                       setTextSize(value);
                                                   }
                                               };
    FloatStyleAttribute      textHaloWidth     = new FloatStyleAttribute(true) {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(float width) {
                                                       labelPaint.setShadowLayer(width, 0f, 0f, labelPaint.getShadowLayerColor());
                                                   }
                                               };
    ColorStyleAttribute      textHaloColor     = new ColorStyleAttribute() {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(int color) {
                                                       int alpha = (int) (((long) color) >> 24);
                                                       if (alpha == 0) {
                                                           color = (int) ((long) color | 0xFF000000L);
                                                       }
                                                       labelPaint.setShadowLayer(labelPaint.getShadowLayerRadius(), 0, 0, color);
                                                   }
                                               };
    FloatStyleAttribute      textLetterSpacing = new FloatStyleAttribute(true) {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(float letterSpacing) {
                                                       labelPaint.setLetterSpacing(letterSpacing);
                                                   }
                                               };
    StringStyleAttribute     textJustify       = new StringStyleAttribute() {
                                                   private static final long serialVersionUID = 1L;

                                                   @Override
                                                   protected void set(String justify) {
                                                       if (justify != null) {
                                                           switch (justify) {
                                                           case Style.TEXT_JUSTIFY_LEFT:
                                                               labelPaint.setTextAlign(Align.LEFT);
                                                               break;
                                                           case Style.TEXT_JUSTIFY_RIGHT:
                                                               labelPaint.setTextAlign(Align.RIGHT);
                                                               break;
                                                           case Style.TEXT_JUSTIFY_CENTER:
                                                               labelPaint.setTextAlign(Align.CENTER);
                                                               break;
                                                           default:
                                                               // log?
                                                           }
                                                       }
                                                   }
                                               };

    private transient StringBuilder builder   = new StringBuilder();
    private transient StringBuilder moustache = new StringBuilder();

    private transient PathMeasure pathMeasure = new PathMeasure();

    private transient float vOffset;
    private transient float ems;

    private transient Rect iconRect  = new Rect();
    private transient Rect labelRect = new Rect();

    private transient CollisionDetector detector = new SimpleCollisionDetector();

    /**
     * Default constructor
     * 
     * @param sourceLayer the source (data) layer
     */
    public Symbol(@NonNull String sourceLayer) {
        super(sourceLayer);
        setLabelPaint(new SerializableTextPaint());
        setTextSize(DEFAULT_TEXT_SIZE); // needs to be changed to DIP
        textMaxWidth.literal = DEFAULT_TEXT_MAX_WIDTH;
        labelPaint.setTextAlign(Align.CENTER);
    }

    /**
     * Set the collision detector
     * 
     * @param detector the CollisionDetector instance to use
     */
    public void setCollisionDetector(@NonNull CollisionDetector detector) {
        this.detector = detector;
    }

    /**
     * Copy constructor
     * 
     * @param other another Layer
     */
    public Symbol(@NonNull Symbol other) {
        super(other);

        this.iconImage = other.iconImage;
        this.symbolPath = other.symbolPath;
        this.label = other.label;
        setLabelPaint(new SerializableTextPaint(other.labelPaint));
        this.textSize = other.textSize;
        this.textColor = other.textColor;
        this.textOpacity = other.textOpacity;
        this.textLetterSpacing = other.textLetterSpacing;
        this.textHaloWidth = other.textHaloWidth;
        this.textHaloColor = other.textHaloColor;
        this.textTransform = other.textTransform;
        this.symbolPlacement = other.symbolPlacement;
        this.iconRotate = other.iconRotate;
        this.textAnchor = other.textAnchor;
        this.iconAnchor = other.iconAnchor;
        this.textOffset = other.textOffset;
        this.iconOffset = other.iconOffset;
    }

    /**
     * Create a rudimentary style from Paint objects for the geometries and labels
     * 
     * @param layerName the layer name
     * @param paint the Paint to use for the geometries
     * @param labelPaint the Paint to use for labels
     * @param symbolName a name to use for point symbols
     * @return a Style
     */
    @NonNull
    public static Symbol fromPaint(@NonNull String layerName, @NonNull Paint paint, @NonNull Paint labelPaint, @Nullable String symbolName) {
        Symbol style = new Symbol(layerName);
        style.paint = new SerializablePaint(paint);
        style.paint.setStyle(Paint.Style.STROKE);
        style.setSymbol(symbolName);
        style.setLabelPaint(new SerializableTextPaint(labelPaint));
        labelPaint.setTextAlign(Align.CENTER);
        return style;
    }

    /**
     * Set the symbol
     * 
     * @param symbolName name of the symbol
     */
    public void setSymbol(@Nullable String symbolName) {
        this.symbolName = symbolName;
        if (symbolName != null) {
            symbolPath = DataStyle.getCurrent().getSymbol(symbolName);
        } else {
            symbolPath = null;
        }
    }

    /**
     * Get the name of the current symbol
     * 
     * @return the symbol name
     */
    @Nullable
    public String getSymbol() {
        return symbolName;
    }

    /**
     * Get the labelKey this is used by the styling dialog
     * 
     * @return the labelKey
     */
    public String getLabelKey() {
        return labelKey;
    }

    /**
     * Set the labelKey this is used by the styling dialog
     * 
     * @param labelKey the labelKey to set
     */
    public void setLabelKey(@Nullable String labelKey) {
        this.labelKey = labelKey;
    }

    /**
     * @return the labelPaint
     */
    public SerializableTextPaint getLabelPaint() {
        return labelPaint;
    }

    /**
     * Set text justification
     * 
     * @param justify one of left, right or center
     */
    public void setTextJustify(@NonNull String justify) {
        textJustify.set(justify);
    }

    /**
     * @param labelPaint the labelPaint to set
     */
    private void setLabelPaint(SerializableTextPaint labelPaint) {
        this.labelPaint = labelPaint;
        labelPaint.setTypeface(DataStyle.getInternal(DataStyle.LABELTEXT_NORMAL).getPaint().getTypeface());
        labelFontMetrics = labelPaint.getFontMetrics();
        vOffset = (-labelFontMetrics.top - labelFontMetrics.bottom) / 2;
        ems = labelPaint.measureText("M");
    }

    @Override
    public void onZoomChange(@NonNull Style style, @Nullable VectorTileDecoder.Feature feature, int z) {
        super.onZoomChange(style, feature, z);
        textSize.eval(feature, z);
        label.eval(feature, z);
        textTransform.eval(feature, z);
        textMaxWidth.eval(feature, z);
        textJustify.eval(feature, z);
        textColor.eval(feature, z);
        textOpacity.eval(feature, z);
        textLetterSpacing.eval(feature, z);
        textHaloWidth.eval(feature, z);
        textHaloColor.eval(feature, z);
        textOffset.eval(feature, z);
        textAnchor.eval(feature, z);
        iconOffset.eval(feature, z);
        symbolPlacement.eval(feature, z);
        iconImage.eval(feature, z);
        iconAnchor.eval(feature, z);
    }

    @Override
    public void render(Canvas c, Style style, Feature feature, int z, Rect screenRect, Rect destinationRect, float scaleX, float scaleY) {
        super.render(c, style, feature, z, screenRect, destinationRect, scaleX, scaleY);
        this.destinationRect = destinationRect;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        Sprites sprites = style.getSprites();
        Geometry g = feature.getGeometry();
        final boolean hasLabel = (label.literal != null && !"".equals(label.literal)) || labelKey != null;
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            if (symbolPlacement.literal == null) {
                float x = (float) (destinationRect.left + ((Point) g).longitude() * scaleX);
                float y = (float) (destinationRect.top + ((Point) g).latitude() * scaleY);
                if (!destinationRect.contains((int) x, (int) y)) {
                    return; // don't render stuff in the buffer around the tile
                }
                if (iconImage.literal != null || symbolPath != null) {
                    drawIcon(c, sprites, screenRect, x, y, feature);
                }
                if (hasLabel) {
                    drawLabel(c, screenRect, x, y, feature);
                }
            }
            break;
        case GeoJSONConstants.MULTIPOINT:
            if (symbolPlacement.literal == null) {
                @SuppressWarnings("unchecked")
                List<Point> pointList = ((CoordinateContainer<List<Point>>) g).coordinates();
                for (Point p : pointList) {
                    float x = (float) (destinationRect.left + p.longitude() * scaleX);
                    float y = (float) (destinationRect.top + p.latitude() * scaleY);
                    if (!destinationRect.contains((int) x, (int) y)) {
                        return; // don't render stuff in the buffer around the tile
                    }
                    if (iconImage.literal != null || symbolPath != null) {
                        drawIcon(c, sprites, screenRect, x, y, feature);
                    }
                    if (hasLabel) {
                        drawLabel(c, screenRect, x, y, feature);
                    }
                }
            }
            break;
        case GeoJSONConstants.LINESTRING:
            if (hasLabel && (SYMBOL_PLACEMENT_LINE.equals(symbolPlacement.literal) || SYMBOL_PLACEMENT_LINE_CENTER.equals(symbolPlacement.literal))) {
                @SuppressWarnings("unchecked")
                List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
                drawLineLabel(c, destinationRect, line, feature);
            }
            break;
        case GeoJSONConstants.MULTILINESTRING:
            if (hasLabel && (SYMBOL_PLACEMENT_LINE.equals(symbolPlacement.literal) || SYMBOL_PLACEMENT_LINE_CENTER.equals(symbolPlacement.literal))) {
                @SuppressWarnings("unchecked")
                List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
                for (List<Point> l : lines) {
                    drawLineLabel(c, destinationRect, l, feature);
                }
            }
            break;
        case GeoJSONConstants.POLYGON:
        case GeoJSONConstants.MULTIPOLYGON:
            // FIXME support
            break;
        default:
            // do nothing for now
        }
    }

    /**
     * Create the label to display either from the label or the labelKey fields
     * 
     * @param attributes the features attributed
     * @return a String or ""
     */
    @NonNull
    private String evaluateLabel(@NonNull Map<String, Object> attributes) {
        if (label.literal != null) {
            String output = deMoustache(label.literal, attributes);
            if (textTransform.literal != null) {
                switch (textTransform.literal) {
                case Style.TEXT_TRANSFORM_UPPERCASE:
                    output = output.toUpperCase();
                    break;
                case Style.TEXT_TRANSFORM_LOWERCASE:
                    output = output.toLowerCase();
                    break;
                default:
                    // do nothing
                }
            }
            return output;
        } else if (labelKey != null) {
            Object output = attributes.get(labelKey);
            return output != null ? output.toString() : "";
        }
        return "";
    }

    /**
     * Replace occurrences of {property} with the value of property in the features attributes
     * 
     * @param input the input string
     * @param attributes the feature attributes
     * @return a String with any moustache templates replaced
     */
    @NonNull
    private String deMoustache(@NonNull String input, @NonNull Map<String, Object> attributes) {
        // primitive moustache handling
        builder.setLength(0);
        moustache.setLength(0);
        boolean inMoustache = false;
        for (char c : input.toCharArray()) {
            switch (c) {
            case '{':
                inMoustache = true;
                break;
            case '}':
                String key = moustache.toString();
                if (attributes.containsKey(key)) {
                    builder.append(attributes.get(key).toString());
                }
                inMoustache = false;
                break;
            default:
                if (inMoustache) {
                    moustache.append(c);
                } else {
                    builder.append(c);
                }
            }
        }
        return builder.toString();
    }

    /**
     * Draw an icon
     * 
     * @param canvas Canvas object we are drawing on
     * @param sprites Object holding the sprite bitmap and sheet
     * @param screenRect current screen Rect
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @param feature the Feature we are rendering
     */
    public void drawIcon(@NonNull Canvas canvas, Sprites sprites, @NonNull Rect screenRect, float x, float y, @NonNull Feature feature) {
        if (!screenRect.contains((int) x, (int) y)) {
            return;
        }
        canvas.save();

        Bitmap icon = feature.getCachedBitmap();
        if (icon == null && iconImage.literal != null) {
            icon = retrieveIcon(sprites, deMoustache(iconImage.literal, feature.getAttributes()));
            feature.setCachedBitmap(icon);
        }
        if (icon != null) {
            int retina = sprites != null && sprites.retina() ? 2 : 1;
            final int width = (int) (icon.getWidth() * iconSize.literal / retina);
            final int height = (int) (icon.getHeight() * iconSize.literal / retina);
            if (iconAnchor.literal != null) {
                switch (iconAnchor.literal) {
                case SYMBOL_ANCHOR_CENTER:
                    x = x - width / 2F;
                    y = y - height / 2F;
                    break;
                case SYMBOL_ANCHOR_BOTTOM: // NOSONAR
                    y = y - height;
                case SYMBOL_ANCHOR_TOP:
                    x = x - width / 2F;
                    break;
                case SYMBOL_ANCHOR_RIGHT: // NOSONAR
                    x = x - width;
                case SYMBOL_ANCHOR_LEFT:
                    y = y - height / 2F;
                    break;
                case SYMBOL_ANCHOR_BOTTOM_RIGHT: // NOSONAR
                    x = x - width;
                case SYMBOL_ANCHOR_BOTTOM_LEFT:
                    y = y - height;
                    break;
                case SYMBOL_ANCHOR_TOP_RIGHT: // NOSONAR
                    x = x - width;
                case SYMBOL_ANCHOR_TOP_LEFT:
                default:
                    // just ignore
                }
            } else { // center
                x = x - width / 2F;
                y = y - height / 2F;
            }
            x = x + iconOffset.literal[0] * width;
            y = y + iconOffset.literal[1] * height;
            iconRect.set((int) x, (int) y, (int) (x + 1 * width), (int) (y + 1 * height));
            if (detector.collides(iconRect)) {
                return;
            }
            rotate(canvas, feature);
            canvas.drawBitmap(icon, null, iconRect, null);
        } else if (symbolPath != null) {
            canvas.translate(x, y);
            rotate(canvas, feature);
            canvas.drawPath(symbolPath, paint);
        }
        canvas.restore();
    }

    /**
     * Rotate the canvas if necessary
     * 
     * @param canvas the Canvas to rotate
     * @param feature the relevant Feature
     */
    private void rotate(@NonNull Canvas canvas, @NonNull Feature feature) {
        iconRotate.eval(feature, 0);
        float rotation = iconRotate.literal;
        if (rotation != 0) {
            canvas.rotate(rotation);
        }
    }

    /**
     * Get an icon caching it if necessary
     * 
     * @param sprites the Sprites object
     * @param name the name of the icon
     * @return a Bitmap or null
     */
    @Nullable
    private Bitmap retrieveIcon(@NonNull Sprites sprites, @NonNull String name) {
        if (sprites != null) {
            return sprites.get(name);
        }
        return null;
    }

    /**
     * Store the actual width in the layout
     * 
     * @author simon
     *
     */
    class StaticLayoutWithWidth extends StaticLayout {
        final int width;

        /**
         * Construct a new instance
         * 
         * @param source CharSequence
         * @param paint TextPaint
         * @param outerwidth int
         * @param align Layout.Alignment
         * @param spacingmult float
         * @param spacingadd float
         * @param includepad boolean
         */
        @SuppressWarnings("deprecation")
        public StaticLayoutWithWidth(CharSequence source, TextPaint paint, int outerwidth, Alignment align, float spacingmult, float spacingadd,
                boolean includepad) {
            super(source, paint, outerwidth, align, spacingmult, spacingadd, includepad);
            int w = 0;
            for (int i = 0; i < getLineCount(); i++) {
                float tempWidth = getLineWidth(i);
                if (tempWidth > w) {
                    w = Math.round(tempWidth);
                }
            }
            width = w;
        }

        /**
         * Get the maximum width of a line
         * 
         * @return the maximum width
         */
        public int getActualWidth() {
            return width;
        }
    }

    /**
     * 
     * Draw a point label
     * 
     * @param canvas Canvas object we are drawing on
     * @param screenRect rect for the current screen
     * @param x target screen x coordinate
     * @param y target screen y coordinate
     * @param feature the Feature we are rendering
     */
    public void drawLabel(@NonNull Canvas canvas, @NonNull Rect screenRect, float x, float y, @NonNull Feature feature) {
        if (!screenRect.contains((int) x, (int) y)) {
            return;
        }
        Object layout = feature.getCachedLabel();
        if (!(layout instanceof StaticLayoutWithWidth)) {
            layout = new StaticLayoutWithWidth(evaluateLabel(feature.getAttributes()), labelPaint, (int) (textMaxWidth.literal * ems), Alignment.ALIGN_NORMAL,
                    1.0f, 0, false);
            feature.setCachedLabel(layout);
        }
        int w = ((StaticLayoutWithWidth) layout).getActualWidth();
        int h = ((StaticLayoutWithWidth) layout).getHeight();
        if (textAnchor.literal != null) {
            switch (textAnchor.literal) {
            case SYMBOL_ANCHOR_TOP:
                // default with StaticLayout
                break;
            case SYMBOL_ANCHOR_BOTTOM:
                y = y - h;
                break;
            default:
                // just ignore for now
            }
        } else { // center
            y = y + h / 2f;
        }
        x = x + textOffset.literal[0] * ems;
        y = y + textOffset.literal[1] * ems;
        labelRect.set((int) x - w / 2, (int) y + FUDGE, (int) x + w / 2, (int) y + h - FUDGE);
        if (detector.collides(labelRect)) {
            return;
        }
        canvas.save();
        canvas.translate(x, y);
        ((StaticLayout) layout).draw(canvas);
        canvas.restore();
    }

    /**
     * Draw a line label
     * 
     * @param canvas Canvas object we are drawing on
     * @param destinationRect where we are drawing to
     * @param line a List of Points making up the line
     * @param feature the Feature we are displaying
     */
    public void drawLineLabel(@NonNull Canvas canvas, @NonNull Rect destinationRect, @NonNull List<Point> line, @NonNull Feature feature) {
        int lineSize = line.size();
        if (lineSize > 1) {
            Object evaluatedLabel = feature.getCachedLabel();
            if (!(evaluatedLabel instanceof String)) {
                evaluatedLabel = evaluateLabel(feature.getAttributes());
            }
            if (!"".equals(evaluatedLabel)) {
                path.rewind();
                int last = lineSize - 1;
                if (line.get(0).longitude() > line.get(last).longitude()) {
                    path.moveTo((float) (destinationRect.left + line.get(last).longitude() * scaleX),
                            (float) (destinationRect.top + line.get(last).latitude() * scaleY));
                    for (int i = (last - 1); i >= 0; i--) {
                        path.lineTo((float) (destinationRect.left + line.get(i).longitude() * scaleX),
                                (float) (destinationRect.top + line.get(i).latitude() * scaleY));
                    }
                } else {
                    path.moveTo((float) (destinationRect.left + line.get(0).longitude() * scaleX),
                            (float) (destinationRect.top + line.get(0).latitude() * scaleY));
                    for (int i = 1; i < lineSize; i++) {
                        path.lineTo((float) (destinationRect.left + line.get(i).longitude() * scaleX),
                                (float) (destinationRect.top + line.get(i).latitude() * scaleY));
                    }
                }
                pathMeasure.setPath(path, false);
                float halfPathLength = pathMeasure.getLength() / 2;
                float halfTextLength = labelPaint.measureText((String) evaluatedLabel) / 2;
                if (halfPathLength > halfTextLength) {
                    float[] start = new float[2];
                    pathMeasure.getPosTan(halfPathLength - halfTextLength, start, null);
                    float[] end = new float[2];
                    pathMeasure.getPosTan(halfPathLength + halfTextLength, end, null);
                    if (detector.collides(start, end, labelPaint.getTextSize())) {
                        return;
                    }
                    canvas.drawTextOnPath((String) evaluatedLabel, path, 0, vOffset, labelPaint);
                }
            }
        }
    }

    /**
     * Set the text size
     * 
     * @param size the size
     */
    public void setTextSize(float size) {
        labelPaint.setTextSize(size);
        labelFontMetrics = labelPaint.getFontMetrics();
        vOffset = (-labelFontMetrics.top - labelFontMetrics.bottom) / 2;
        ems = labelPaint.measureText("M");
    }

    /**
     * Manually set the icon rotation attribute
     * 
     * @param iconRotate the attribute
     */
    public void setIconRotate(@Nullable FloatStyleAttribute iconRotate) {
        this.iconRotate = iconRotate;
    }

    /**
     * Get the current symbol-placement value
     * 
     * @return the symbol-placement value
     */
    @NonNull
    public String getSymbolPlacement() {
        return symbolPlacement.literal == null ? SYMBOL_PLACEMENT_POINT : symbolPlacement.literal;
    }

    /**
     * Actually get the anchor variable
     * 
     * @param anchor see above
     * @return the anchor value
     */
    @Nullable
    private String getAnchor(@Nullable String anchor) {
        if (anchor != null) {
            switch (anchor) {
            case SYMBOL_ANCHOR_CENTER:
                return null;
            case SYMBOL_ANCHOR_LEFT:
            case SYMBOL_ANCHOR_RIGHT:
            case SYMBOL_ANCHOR_TOP:
            case SYMBOL_ANCHOR_BOTTOM:
            case SYMBOL_ANCHOR_TOP_LEFT:
            case SYMBOL_ANCHOR_TOP_RIGHT:
            case SYMBOL_ANCHOR_BOTTOM_LEFT:
            case SYMBOL_ANCHOR_BOTTOM_RIGHT:
                return anchor;
            default:
                Log.w(DEBUG_TAG, "Unknown anchor value " + anchor);
            }
        }
        return null;
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

        if (labelPaint != null) {
            setLabelPaint(labelPaint);
        }
        builder = new StringBuilder();
        moustache = new StringBuilder();
        pathMeasure = new PathMeasure();
        iconRect = new Rect();
        labelRect = new Rect();

        setSymbol(symbolName);
    }
}
