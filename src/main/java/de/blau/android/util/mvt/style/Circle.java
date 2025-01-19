package de.blau.android.util.mvt.style;

import java.util.List;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.SerializableTextPaint;
import de.blau.android.util.mvt.VectorTileDecoder;
import de.blau.android.util.mvt.VectorTileDecoder.Feature;

public class Circle extends Layer {

    private static final long serialVersionUID = 1L;

    public static final float DEFAULT_RADIUS = 5;

    SerializableTextPaint stroke        = null;
    ColorStyleAttribute   strokeColor   = new ColorStyleAttribute() {
                                            private static final long serialVersionUID = 1L;

                                            @Override
                                            protected void set(int color) {
                                                createStrokePaint();
                                                int tempAlpha = stroke.getAlpha();
                                                stroke.setColor(color);
                                                if (color >>> 24 == 0) {
                                                    stroke.setAlpha(tempAlpha);
                                                }
                                            }
                                        };
    FloatStyleAttribute   strokeOpacity = new FloatStyleAttribute(false) {
                                            private static final long serialVersionUID = 1L;

                                            @Override
                                            protected void set(float opacity) {
                                                createStrokePaint();
                                                stroke.setAlpha(Math.round(opacity * 255));
                                            }
                                        };
    FloatStyleAttribute   strokeWidth   = new FloatStyleAttribute(true) {
                                            private static final long serialVersionUID = 1L;

                                            @Override
                                            protected void set(float value) {
                                                createStrokePaint();
                                                stroke.setStrokeWidth(value);
                                            }
                                        };

    FloatArrayStyleAttribute circleTranslate = new FloatArrayStyleAttribute(true);
    FloatStyleAttribute      circleRadius    = new FloatStyleAttribute(true);

    /**
     * Default constructor
     * 
     * @param sourceLayer the source (data) layer
     */
    public Circle(@NonNull String sourceLayer) {
        super(sourceLayer);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    /**
     * Copy constructor
     * 
     * @param other another Layer
     */
    public Circle(@NonNull Circle other) {
        super(other);
    }

    /**
     * Create a rudimentary Layer from Paint objects for the geometries
     * 
     * @param layer source layer
     * @param paint the Paint to use for the geometries
     * @return a Circle Layer
     */
    @NonNull
    public static Circle fromPaint(@NonNull String layer, @NonNull Paint paint) {
        Circle style = new Circle(layer);
        style.paint = new SerializableTextPaint(paint);
        style.paint.setAntiAlias(true);
        style.paint.setStyle(Paint.Style.FILL_AND_STROKE);
        return style;
    }

    @Override
    public void onZoomChange(@NonNull Style style, @Nullable VectorTileDecoder.Feature feature, int z) {
        super.onZoomChange(style, feature, z);
        strokeColor.eval(feature, z);
        strokeOpacity.eval(feature, z);
        strokeWidth.eval(feature, z);
        circleTranslate.eval(feature, z);
        circleRadius.eval(feature, z);
    }

    @Override
    public void render(@NonNull Canvas c, @NonNull Style style, @Nullable Feature feature, int z, @Nullable Rect screenRect, @NonNull Rect destinationRect,
            float scaleX, float scaleY) {
        super.render(c, style, feature, z, screenRect, destinationRect, scaleX, scaleY);
        if (feature == null) {
            return;
        }
        this.destinationRect = destinationRect;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        Geometry g = feature.getGeometry();
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            float x = (float) (destinationRect.left + ((Point) g).longitude() * scaleX);
            float y = (float) (destinationRect.top + ((Point) g).latitude() * scaleY);
            if (!destinationRect.contains((int) x, (int) y)) {
                return; // don't render stuff in the buffer around the tile
            }
            drawCircle(c, x, y);
            break;
        case GeoJSONConstants.MULTIPOINT:
            @SuppressWarnings("unchecked")
            List<Point> pointList = ((CoordinateContainer<List<Point>>) g).coordinates();
            for (Point p : pointList) {
                x = (float) (destinationRect.left + p.longitude() * scaleX);
                y = (float) (destinationRect.top + p.latitude() * scaleY);
                if (!destinationRect.contains((int) x, (int) y)) {
                    return; // don't render stuff in the buffer around the tile
                }
                drawCircle(c, x, y);
            }
            break;
        default:
            // log?
        }
    }

    /**
     * Draw a circle
     * 
     * @param canvas Canvas object we are drawing on
     * @param x screen x of center
     * @param y screen y of center
     */
    private void drawCircle(@NonNull Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, circleRadius.literal, paint);
        if (stroke != null) {
            canvas.drawCircle(x, y, circleRadius.literal, stroke);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        if (stroke != null) {
            stroke.setAlpha(alpha);
        }
    }

    /**
     * Create a new Paint object for the stroke
     */
    private void createStrokePaint() {
        if (stroke == null) {
            stroke = new SerializableTextPaint();
            stroke.setStyle(Paint.Style.STROKE);
        }
    }

    @Override
    @NonNull
    public String toString() {
        return super.toString() + " " + getClass().getSimpleName() + " " + Integer.toHexString(paint.getColor());
    }
}
