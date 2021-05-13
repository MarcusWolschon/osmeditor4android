package de.blau.android.util.mvt.style;

import java.util.List;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SerializablePaint;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileDecoder.Feature;

public class Line extends Layer {

    private static final long serialVersionUID = 5L;

    FloatStyleAttribute lineWidth = new FloatStyleAttribute(true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected void set(float value) {
            paint.setStrokeWidth(value);
        }
    };

    private float[] dashArray;

    /**
     * Default constructor
     */
    public Line(@NonNull String sourceLayer) {
        super(sourceLayer);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Copy constructor
     * 
     * @param other another Style
     */
    public Line(@NonNull Line other) {
        super(other);
    }

    /**
     * Create a rudimentary style from Paint objects for the geometries and labels
     * 
     * @param layer source layer
     * @param paint the Paint to use for the geometries
     * @return a Style
     */
    @NonNull
    public static Line fromPaint(@NonNull String layer, @NonNull Paint paint) {
        Line style = new Line(layer);
        style.paint = new SerializablePaint(paint);
        style.paint.setAntiAlias(true);
        style.paint.setStyle(Paint.Style.STROKE);
        return style;
    }

    @Override
    public void onZoomChange(Style style, VectorTileDecoder.Feature feature, int z) {
        super.onZoomChange(style, feature, z);
        lineWidth.eval(feature, z);
        setDashArrayOnPaint();
    }

    @Override
    public void render(Canvas c, Style style, Feature feature, int z, Rect screenRect, Rect destinationRect, float scaleX, float scaleY) {
        super.render(c, style, feature, z, screenRect, destinationRect, scaleX, scaleY);
        this.destinationRect = destinationRect;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        Geometry g = feature.getGeometry();
        switch (g.type()) {
        case GeoJSONConstants.LINESTRING:
            @SuppressWarnings("unchecked")
            List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
            drawLine(c, line);
            break;
        case GeoJSONConstants.MULTILINESTRING:
            @SuppressWarnings("unchecked")
            List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            for (List<Point> l : lines) {
                drawLine(c, l);
            }
            break;
        default:
            // log?
        }
    }

    /**
     * Draw a line
     * 
     * @param canvas Canvas object we are drawing on
     * @param line a List of Points making up the line
     */
    public void drawLine(@NonNull Canvas canvas, @NonNull List<Point> line) {
        int lineSize = line.size();
        if (lineSize > 1) {
            path.rewind();
            path.moveTo((float) (destinationRect.left + line.get(0).longitude() * scaleX), (float) (destinationRect.top + line.get(0).latitude() * scaleY));
            for (int i = 1; i < lineSize; i++) {
                path.lineTo((float) (destinationRect.left + line.get(i).longitude() * scaleX), (float) (destinationRect.top + line.get(i).latitude() * scaleY));
            }
            canvas.drawPath(path, paint);
        }
    }

    /**
     * Set the line width
     * 
     * @param width the width in pixels
     */
    public void setLineWidth(float width) {
        paint.setStrokeWidth(width);
        setDashArrayOnPaint();
    }

    /**
     * Set a dash pattern
     * 
     * @param dashArray an array of floats indicating relative lengths of on/off segments
     */
    public void setDashArray(@Nullable List<Float> dashArray) {
        if (dashArray != null) {
            this.dashArray = new float[dashArray.size()];
            for (int i = 0; i < dashArray.size(); i++) {
                this.dashArray[i] = dashArray.get(i);
            }
            setDashArrayOnPaint();
        } else {
            this.dashArray = null;
            paint.setPathEffect(null);
        }
    }

    /**
     * Set up the dash array in the paint object
     * 
     * This multiplies the dash lengths with the stroke width
     */
    private void setDashArrayOnPaint() {
        if (dashArray != null && dashArray.length > 0) {
            float[] temp = new float[dashArray.length];
            float width = Math.max(1, paint.getStrokeWidth());
            for (int i = 0; i < dashArray.length; i++) {
                temp[i] = dashArray[i] * width;
            }
            DashPathEffect dp = new DashPathEffect(temp, 0);
            paint.setPathEffect(dp);
        }
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + " " + getClass().getSimpleName() + " " + Integer.toHexString(paint.getColor()) + " " + paint.getStrokeWidth();
    }
}
