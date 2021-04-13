package de.blau.android.util.mvt;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.mapbox.geojson.CoordinateContainer;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.mvt.VectorTileDecoder.FeatureIterable;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.util.MapTileProvider.TileDecoder;

public class VectorTileRenderer implements MapTilesLayer.TileRenderer<VectorTileDecoder.FeatureIterable> {

    private static final String DEBUG_TAG = VectorTileRenderer.class.getSimpleName();

    private final Path path = new Path();

    int   iconRadius = 10;
    float scaleX     = 1f;
    float scaleY     = 1f;

    Rect screenRect;

    Paint        vectorPaint      = new Paint(DataStyle.getInternal(DataStyle.GPS_TRACK).getPaint());
    FeatureStyle labelFs          = DataStyle.getInternal(DataStyle.LABELTEXT_NORMAL);
    Paint        labelPaint       = labelFs.getPaint();
    Paint        labelBackground  = DataStyle.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
    float        labelStrokeWidth = labelPaint.getStrokeWidth();

    @Override
    public void render(Canvas c, FeatureIterable features, Rect fromRect, Rect screenRect, Paint paint) {
        scaleX = screenRect.width() / 256f;
        scaleY = screenRect.height() / 256f;
        this.screenRect = screenRect;
        for (VectorTileDecoder.Feature feature : features) {
            draw(c, feature, vectorPaint, "name");
        }
    }

    @Override
    public TileDecoder<VectorTileDecoder.FeatureIterable> decoder() {
        return (byte[] data, boolean small) -> {
            try {
                return new VectorTileDecoder().decode(data);
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
    private void draw(@NonNull Canvas canvas, @NonNull VectorTileDecoder.Feature f, @NonNull Paint paint, @Nullable String labelKey) {
        Geometry g = f.getGeometry();
        Map<String, Object> attributes = f.getAttributes();
        final String label = attributes != null ? (String) attributes.get(labelKey) : null;
        switch (g.type()) {
        case GeoJSONConstants.POINT:
            paint.setStyle(Paint.Style.STROKE);
            drawPoint(canvas, (Point) g, paint, label);
            break;
        case GeoJSONConstants.MULTIPOINT:
            @SuppressWarnings("unchecked")
            List<Point> pointList = ((CoordinateContainer<List<Point>>) g).coordinates();
            paint.setStyle(Paint.Style.STROKE);
            for (Point q : pointList) {
                drawPoint(canvas, q, paint, label);
            }
            break;
        case GeoJSONConstants.LINESTRING:
            @SuppressWarnings("unchecked")
            List<Point> line = ((CoordinateContainer<List<Point>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            drawLine(canvas, line, paint, label);
            break;
        case GeoJSONConstants.MULTILINESTRING:
            @SuppressWarnings("unchecked")
            List<List<Point>> lines = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            for (List<Point> l : lines) {
                drawLine(canvas, l, paint, label);
            }
            break;
        case GeoJSONConstants.POLYGON:
            @SuppressWarnings("unchecked")
            List<List<Point>> rings = ((CoordinateContainer<List<List<Point>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            drawPolygon(canvas, rings, paint);
            break;
        case GeoJSONConstants.MULTIPOLYGON:
            @SuppressWarnings("unchecked")
            List<List<List<Point>>> polygons = ((CoordinateContainer<List<List<List<Point>>>>) g).coordinates();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            for (List<List<Point>> polygon : polygons) {
                drawPolygon(canvas, polygon, paint);
            }
            break;
        default:
            Log.e(DEBUG_TAG, "drawGeometry unknown GeoJSON geometry " + g.type());
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
    public void drawLine(@NonNull Canvas canvas, @NonNull List<Point> line, @NonNull Paint paint, @Nullable String label) {
        int lineSize = line.size();
        if (lineSize > 1) {
            path.reset();
            path.moveTo((float) (screenRect.left + line.get(0).longitude() * scaleX), (float) (screenRect.top + line.get(0).latitude() * scaleY));
            for (int i = 1; i < lineSize; i++) {
                path.lineTo((float) (screenRect.left + line.get(i).longitude() * scaleX), (float) (screenRect.top + line.get(i).latitude() * scaleY));
            }
            canvas.drawPath(path, paint);
            if (label != null) {
                canvas.drawTextOnPath(label, path, 0, 0, labelPaint);
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
    public void drawPolygon(@NonNull Canvas canvas, @NonNull List<List<Point>> polygon, @NonNull Paint paint) {
        path.reset();
        for (List<Point> ring : polygon) {
            int ringSize = ring.size();
            if (ringSize > 2) {
                path.moveTo((float) (screenRect.left + ring.get(0).longitude() * scaleX), (float) (screenRect.top + ring.get(0).latitude() * scaleY));
                for (int i = 1; i < ringSize; i++) {
                    path.lineTo((float) (screenRect.left + ring.get(i).longitude() * scaleX), (float) (screenRect.top + ring.get(i).latitude() * scaleY));
                }
                path.close();
            }
        }
        path.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(path, paint);
    }

    /**
     * Draw a marker
     * 
     * @param canvas Canvas object we are drawing on
     *
     * @param p the Position of the marker
     * @param paint Paint object for drawing
     * @param label label to display, null if none
     */
    public void drawPoint(@NonNull Canvas canvas, @NonNull Point p, @NonNull Paint paint, @Nullable String label) {
        float x = (float) (screenRect.left + p.longitude() * scaleX);
        float y = (float) (screenRect.top + p.latitude() * scaleY);
        canvas.save();
        canvas.translate(x, y);
        canvas.drawPath(DataStyle.getCurrent().getWaypointPath(), paint);
        canvas.restore();
        if (label != null) {
            float yOffset = 2 * labelStrokeWidth + iconRadius;
            float halfTextWidth = labelPaint.measureText(label) / 2;
            FontMetrics fm = labelFs.getFontMetrics();
            canvas.drawRect(x - halfTextWidth, y + yOffset + fm.bottom, x + halfTextWidth, y + yOffset - labelPaint.getTextSize() + fm.bottom, labelBackground);
            canvas.drawText(label, x - halfTextWidth, y + yOffset, labelPaint);
        }
    }
}
