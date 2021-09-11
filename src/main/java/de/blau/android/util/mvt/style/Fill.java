package de.blau.android.util.mvt.style;

import java.util.List;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SerializablePaint;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileDecoder.Feature;

public class Fill extends Layer {

    private static final long serialVersionUID = 4L;

    SerializablePaint        outline       = null;
    ColorStyleAttribute      outlineColor  = new ColorStyleAttribute() {
                                               private static final long serialVersionUID = 1L;

                                               @Override
                                               protected void set(int color) {
                                                   if (outline == null) {
                                                       outline = new SerializablePaint();
                                                       outline.setStyle(Paint.Style.STROKE);
                                                   }
                                                   int tempAlpha = outline.getAlpha();
                                                   outline.setColor(color);
                                                   if (color >>> 24 == 0) {
                                                       outline.setAlpha(tempAlpha);
                                                   }
                                               }
                                           };
    FloatArrayStyleAttribute fillTranslate = new FloatArrayStyleAttribute(true);

    /**
     * Default constructor
     * 
     * @param sourceLayer the source (data) layer
     */
    public Fill(@NonNull String sourceLayer) {
        super(sourceLayer);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    /**
     * Copy constructor
     * 
     * @param other another Style
     */
    public Fill(@NonNull Fill other) {
        super(other);
    }

    /**
     * Create a rudimentary Layer from Paint objects for the geometries
     * 
     * @param layer source layer
     * @param paint the Paint to use for the geometries
     * @return a Fill Layer
     */
    @NonNull
    public static Fill fromPaint(@NonNull String layer, @NonNull Paint paint) {
        Fill style = new Fill(layer);
        style.paint = new SerializablePaint(paint);
        style.paint.setAntiAlias(true);
        style.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        return style;
    }

    @Override
    public void onZoomChange(@NonNull Style style, @Nullable VectorTileDecoder.Feature feature, int z) {
        super.onZoomChange(style, feature, z);
        outlineColor.eval(feature, z);
        fillTranslate.eval(feature, z);
    }

    @Override
    public void render(Canvas c, Style style, Feature feature, int z, Rect screenRect, Rect destinationRect, float scaleX, float scaleY) {
        super.render(c, style, feature, z, screenRect, destinationRect, scaleX, scaleY);
        this.destinationRect = destinationRect;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        Geometry g = feature.getGeometry();
        switch (g.type()) {
        case GeoJSONConstants.POLYGON:
            @SuppressWarnings("unchecked")
            List<List<Point>> rings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            drawPolygon(c, rings);
            break;
        case GeoJSONConstants.MULTIPOLYGON:
            @SuppressWarnings("unchecked")
            List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
            for (List<List<Point>> polygon : polygons) {
                drawPolygon(c, polygon);
            }
            break;
        default:
            // log?
        }
    }

    /**
     * Draw a polygon
     * 
     * @param canvas Canvas object we are drawing on
     * @param polygon List of List of Point objects defining the polygon rings
     */
    private void drawPolygon(@NonNull Canvas canvas, @NonNull List<List<Point>> polygon) {
        path.reset();
        for (List<Point> ring : polygon) {
            int ringSize = ring.size();
            if (ringSize > 2) {
                float left = destinationRect.left + fillTranslate.literal[0];
                float top = destinationRect.top + fillTranslate.literal[1];
                path.moveTo((float) (left + ring.get(0).longitude() * scaleX), (float) (top + ring.get(0).latitude() * scaleY));
                for (int i = 1; i < ringSize; i++) {
                    path.lineTo((float) (left + ring.get(i).longitude() * scaleX), (float) (top + ring.get(i).latitude() * scaleY));
                }
                path.close();
            }
        }
        path.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(path, paint);
        if (outline != null) {
            canvas.drawPath(path, outline);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        if (outline != null) {
            outline.setAlpha(alpha);
        }
    }

    /**
     * Enable / disable antialias
     * 
     * @param enable is true enable antialias
     */
    public void setAntiAlias(boolean enable) {
        paint.setAntiAlias(enable);
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + " " + getClass().getSimpleName() + " " + Integer.toHexString(paint.getColor());
    }
}
