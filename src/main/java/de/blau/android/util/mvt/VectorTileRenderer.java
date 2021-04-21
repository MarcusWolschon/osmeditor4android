package de.blau.android.util.mvt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.symbols.TriangleDown;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SerializablePaint;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.util.MapTileProvider.TileDecoder;

public class VectorTileRenderer implements MapTilesLayer.TileRenderer<List<VectorTileDecoder.Feature>> {

    private static final String DEBUG_TAG = VectorTileRenderer.class.getSimpleName();

    final HashMap<String, Style> styles = new HashMap<>();
    final Style                  defaultStyle;

    private final Path path = new Path();

    int   iconRadius = 10;
    float scaleX     = 1f;
    float scaleY     = 1f;

    private Set<String>              layerNames    = new HashSet<>();
    private Map<String, Set<String>> attributeKeys = new HashMap<>();

    Rect destinationRect;
    Rect screenRect;
    Rect tempRect = new Rect();

    /**
     * Create a new instance
     */
    public VectorTileRenderer() {
        defaultStyle = Style.FromPaint(DataStyle.getInternal(DataStyle.GPS_TRACK).getPaint(), DataStyle.getInternal(DataStyle.LABELTEXT_NORMAL).getPaint(),
                TriangleDown.NAME);
    }

    /**
     * Set the style for a specific layer
     * 
     * @param layerName the layer name
     * @param layerStyle the style to use
     */
    public void setLayerStyle(@NonNull String layerName, @NonNull Style layerStyle) {
        Log.d(DEBUG_TAG, "setting style for " + layerName);
        styles.put(layerName, layerStyle);
    }

    /**
     * Get the style for a specific layer
     * 
     * @param layerName the layer name
     * @return the style or null
     */
    @Nullable
    public Style getLayerStyle(@NonNull String layerName) {
        return styles.get(layerName);
    }

    /**
     * Replace the styles with the argument
     * 
     * @param newStyles a Map of the new styles
     */
    public void setLayerStyles(@NonNull Map<String, Style> newStyles) {
        styles.clear();
        styles.putAll(newStyles);
    }

    /**
     * Get the styles
     * 
     * @return a map containing the styles
     */
    @NonNull
    public HashMap<String, Style> getLayerStyles() { // NOSONAR
        return styles;
    }

    @Override
    public void render(Canvas c, List<VectorTileDecoder.Feature> features, int z, Rect fromRect, Rect destinationRect, Paint paint) {
        scaleX = destinationRect.width() / 256f;
        scaleY = destinationRect.height() / 256f;
        this.destinationRect = destinationRect;
        if (screenRect == null) {
            screenRect = new Rect();
            c.getClipBounds(screenRect);
        }
        c.save();
        c.clipRect(destinationRect);
        for (VectorTileDecoder.Feature feature : features) {
            final String layerName = feature.getLayerName();
            Style style = styles.get(layerName);
            if (style == null) {
                style = new Style(defaultStyle);
                styles.put(layerName, style);
            }
            if (z >= style.getMinZoom() && (style.getMaxZoom() == -1 || z <= style.getMaxZoom())) {
                draw(c, feature, style);
            }
        }
        c.restore();
    }

    @Override
    public TileDecoder<List<VectorTileDecoder.Feature>> decoder() {
        return (byte[] data, boolean small) -> {
            try {
                VectorTileDecoder decoder = new VectorTileDecoder();
                List<VectorTileDecoder.Feature> features = decoder.decode(data).asList();
                for (VectorTileDecoder.Feature feature : features) {
                    final String layerName = feature.getLayerName();
                    layerNames.add(layerName);
                    Set<String> keys = attributeKeys.get(layerName);
                    if (keys == null) {
                        keys = new HashSet<>();
                        attributeKeys.put(layerName, keys);
                    }
                    keys.addAll(feature.getAttributes().keySet());
                }
                return features;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        };
    }

    /**
     * Draw the feature
     * 
     * @param canvas the Canvas to render on to
     * @param f the Feature
     * @param paint the Paint to use
     * @param labelKey the key for a label
     */
    private void draw(@NonNull Canvas canvas, @NonNull VectorTileDecoder.Feature f, @NonNull Style style) {
        if (intersectsScreen(f)) {
            Geometry g = f.getGeometry();
            Map<String, Object> attributes = f.getAttributes();
            final String label = style.getLabelKey() != null && attributes.containsKey(style.getLabelKey()) ? attributes.get(style.getLabelKey()).toString()
                    : null;
            switch (g.type()) {
            case GeoJSONConstants.POINT:
                drawPoint(canvas, (Point) g, style, label);
                break;
            case GeoJSONConstants.MULTIPOINT:
                @SuppressWarnings("unchecked")
                List<Point> pointList = ((CoordinateContainer<List<Point>>) g).coordinates();
                for (Point q : pointList) {
                    drawPoint(canvas, q, style, label);
                }
                break;
            case GeoJSONConstants.LINESTRING:
                @SuppressWarnings("unchecked")
                List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
                drawLine(canvas, line, style, label);
                break;
            case GeoJSONConstants.MULTILINESTRING:
                @SuppressWarnings("unchecked")
                List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
                for (List<Point> l : lines) {
                    drawLine(canvas, l, style, label);
                }
                break;
            case GeoJSONConstants.POLYGON:
                @SuppressWarnings("unchecked")
                List<List<Point>> rings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
                drawPolygon(canvas, rings, style);
                break;
            case GeoJSONConstants.MULTIPOLYGON:
                @SuppressWarnings("unchecked")
                List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
                for (List<List<Point>> polygon : polygons) {
                    drawPolygon(canvas, polygon, style);
                }
                break;
            default:
                Log.e(DEBUG_TAG, "drawGeometry unknown GeoJSON geometry " + g.type());
            }
        }
    }

    /**
     * Draw a line
     * 
     * @param canvas Canvas object we are drawing on
     * @param line a List of Points making up the line
     * @param paint Paint object for drawing
     * @param label a label to display or null
     */
    public void drawLine(@NonNull Canvas canvas, @NonNull List<Point> line, @NonNull Style style, @Nullable String label) {
        int lineSize = line.size();
        if (lineSize > 1) {
            path.reset();
            path.moveTo((float) (destinationRect.left + line.get(0).longitude() * scaleX), (float) (destinationRect.top + line.get(0).latitude() * scaleY));
            for (int i = 1; i < lineSize; i++) {
                path.lineTo((float) (destinationRect.left + line.get(i).longitude() * scaleX), (float) (destinationRect.top + line.get(i).latitude() * scaleY));
            }
            canvas.drawPath(path, style.getLinePaint());
            if (label != null) {
                canvas.drawTextOnPath(label, path, 0, 0, style.getLabelPaint());
            }
        }
    }

    /**
     * Draw a polygon
     * 
     * @param canvas Canvas object we are drawing on
     * @param polygon List of List of Point objects defining the polygon rings
     * @param paint Paint object for drawing
     */
    public void drawPolygon(@NonNull Canvas canvas, @NonNull List<List<Point>> polygon, @NonNull Style style) {
        path.reset();
        for (List<Point> ring : polygon) {
            int ringSize = ring.size();
            if (ringSize > 2) {
                path.moveTo((float) (destinationRect.left + ring.get(0).longitude() * scaleX), (float) (destinationRect.top + ring.get(0).latitude() * scaleY));
                for (int i = 1; i < ringSize; i++) {
                    path.lineTo((float) (destinationRect.left + ring.get(i).longitude() * scaleX),
                            (float) (destinationRect.top + ring.get(i).latitude() * scaleY));
                }
                path.close();
            }
        }
        path.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(path, style.getPolygonPaint());
    }

    /**
     * Draw a marker
     * 
     * @param canvas Canvas object we are drawing on
     *
     * @param p the Position of the marker
     * @param style Style for this point
     * @param label label to display, null if none
     */
    public void drawPoint(@NonNull Canvas canvas, @NonNull Point p, @NonNull Style style, @Nullable String label) {
        if (style.getSymbolPath() != null) {
            float x = (float) (destinationRect.left + p.longitude() * scaleX);
            float y = (float) (destinationRect.top + p.latitude() * scaleY);
            if (!screenRect.contains((int) x, (int) y)) {
                return;
            }
            canvas.save();
            canvas.translate(x, y);
            canvas.drawPath(style.getSymbolPath(), style.getPointPaint());
            canvas.restore();
            if (label != null) {
                final SerializablePaint labelPaint = style.getLabelPaint();
                if (labelPaint != null && labelPaint.getTypeface() != null) {
                    float yOffset = 2 * style.labelStrokeWidth + iconRadius;
                    float halfTextWidth = labelPaint.measureText(label) / 2;
                    final float bottom = style.getLabelFontMetrics().bottom;
                    canvas.drawRect(x - halfTextWidth, y + yOffset + bottom, x + halfTextWidth, y + yOffset - labelPaint.getTextSize() + bottom,
                            style.getLabelBackground());
                    canvas.drawText(label, x - halfTextWidth, y + yOffset, labelPaint);
                }
            }
        }
    }

    /**
     * Get a list of all layer names we've seen up to now
     * 
     * @return a List of names
     */
    @NonNull
    public List<String> getLayerNames() {
        return new ArrayList<>(layerNames);
    }

    /**
     * Get a list of all attribute keys we've seen up to now
     * 
     * @param layerName the keys for the layer
     * @return a List of keys
     */
    @NonNull
    public List<String> getAttributeKeys(@NonNull String layerName) {
        Set<String> keys = attributeKeys.get(layerName);
        return keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    }

    /**
     * Calculate the bounding box of a List of Points and sets an existing rect to the values
     * 
     * @param rect the Rect for the result
     * @param points the List of Points
     */
    private void rectFromPoints(Rect rect, List<Point> points) {
        Point first = points.get(0);
        int start = 0;
        if (rect.isEmpty()) {
            rect.set((int) first.longitude(), (int) first.latitude(), (int) first.longitude(), (int) first.latitude());
            start = 1;
        }
        for (int i = start; i < points.size(); i++) {
            Point p = points.get(i);
            rect.union((int) p.longitude(), (int) p.latitude());
        }
    }

    /**
     * Check if the feature intersects the screen
     * 
     * Caches the bounding box of the feature in the feature
     * 
     * @param f the Feature
     * @return true if intersects the screen
     */
    private boolean intersectsScreen(@NonNull VectorTileDecoder.Feature f) {
        Rect rect = f.getBox();
        if (rect == null) {
            Geometry g = f.getGeometry();
            switch (g.type()) {
            case GeoJSONConstants.POINT:
                return true;
            case GeoJSONConstants.MULTIPOINT:
                @SuppressWarnings("unchecked")
                List<Point> pointList = ((CoordinateContainer<List<Point>>) g).coordinates();
                rect = new Rect();
                rectFromPoints(rect, pointList);
                f.setBox(rect);
                return true;
            case GeoJSONConstants.LINESTRING:
                @SuppressWarnings("unchecked")
                List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
                rect = new Rect();
                rectFromPoints(rect, line);
                f.setBox(rect);
                break;
            case GeoJSONConstants.MULTILINESTRING:
                @SuppressWarnings("unchecked")
                List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
                rect = new Rect();
                for (List<Point> l : lines) {
                    rectFromPoints(rect, l);
                }
                f.setBox(rect);
                break;
            case GeoJSONConstants.POLYGON:
                @SuppressWarnings("unchecked")
                List<List<Point>> rings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
                rect = new Rect();
                for (List<Point> ring : rings) {
                    rectFromPoints(rect, ring);
                }
                f.setBox(rect);
                break;
            case GeoJSONConstants.MULTIPOLYGON:
                @SuppressWarnings("unchecked")
                List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
                rect = new Rect();
                for (List<List<Point>> polygon : polygons) {
                    for (List<Point> ring : polygon) {
                        rectFromPoints(rect, ring);
                    }
                }
                f.setBox(rect);
                break;
            default:
                Log.e(DEBUG_TAG, "drawGeometry unknown GeoJSON geometry " + g.type());
            }
        }

        tempRect.set(rect);
        tempRect.right = destinationRect.left + (int) (tempRect.right * scaleX);
        tempRect.left = destinationRect.left + (int) (tempRect.left * scaleX);
        tempRect.bottom = destinationRect.top + (int) (tempRect.bottom * scaleY);
        tempRect.top = destinationRect.top + (int) (tempRect.top * scaleY);
        return tempRect.intersect(screenRect);
    }
}
