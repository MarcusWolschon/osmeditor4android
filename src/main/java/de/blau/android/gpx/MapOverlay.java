package de.blau.android.gpx;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.util.Log;
import de.blau.android.Map;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.osm.Track.WayPoint;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.services.TrackerService;
import de.blau.android.util.GeoMath;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.views.IMapView;
import de.blau.android.views.layers.MapViewLayer;

public class MapOverlay extends MapViewLayer {

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    private TrackerService tracker;

    /** Map this is an overlay of. */
    private final Map map;

    private int                      iconRadius;
    private final FloatPrimitiveList linePoints;

    public MapOverlay(final Map map) {
        this.map = map;
        iconRadius = map.getIconRadius();
        linePoints = new FloatPrimitiveList();
    }

    @Override
    public boolean isReadyToDraw() {
        tracker = map.getTracker();
        return tracker != null && map.getBackgroundLayer().isReadyToDraw();
    }

    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (tracker == null) {
            return;
        }
        List<TrackPoint> trackPoints = tracker.getTrackPoints();
        if (trackPoints != null && !trackPoints.isEmpty()) {
            map.pointListToLinePointsArray(linePoints, trackPoints);
            Paint trackPaint = DataStyle.getCurrent(DataStyle.GPS_TRACK).getPaint();
            canvas.drawLines(linePoints.getArray(), 0, linePoints.size(), trackPaint);
        }
        WayPoint[] wayPoints = tracker.getTrack().getWayPoints();
        if (wayPoints != null && wayPoints.length != 0) {
            ViewBox viewBox = map.getViewBox();
            int width = map.getWidth();
            int height = map.getHeight();
            int zoomLevel = map.getZoomLevel();
            Paint wayPointPaint = DataStyle.getCurrent(DataStyle.GPS_POS_FOLLOW).getPaint();
            FeatureStyle fs = DataStyle.getCurrent(DataStyle.LABELTEXT_NORMAL);
            Paint paint = fs.getPaint();
            Paint labelBackground = DataStyle.getCurrent(DataStyle.LABELTEXT_BACKGROUND).getPaint();
            float strokeWidth = paint.getStrokeWidth();
            float yOffset = 2 * strokeWidth + iconRadius;
            for (WayPoint wp : wayPoints) {
                if (viewBox.contains(wp.getLongitude(), wp.getLatitude())) {
                    float x = GeoMath.lonE7ToX(width, viewBox, wp.getLon());
                    float y = GeoMath.latE7ToY(height, width, viewBox, wp.getLat());
                    canvas.save();
                    canvas.translate(x, y);
                    canvas.drawPath(DataStyle.getCurrent().getWaypointPath(), wayPointPaint);
                    canvas.restore();
                    if (zoomLevel > Map.SHOW_LABEL_LIMIT) {
                        String label = wp.getName();
                        if (label == null) {
                            label = wp.getDescription();
                            if (label == null) {
                                continue;
                            }
                        }
                        float halfTextWidth = paint.measureText(label) / 2;
                        FontMetrics fm = fs.getFontMetrics();
                        canvas.drawRect(x - halfTextWidth, y + yOffset + fm.bottom, x + halfTextWidth, y + yOffset - paint.getTextSize() + fm.bottom,
                                labelBackground);
                        canvas.drawText(label, x - halfTextWidth, y + yOffset, paint);
                    }
                }
            }
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public void onDestroy() {
        tracker = null;
    }

    /**
     * Given screen coordinates, find all nearby way points.
     * 
     * @param x Screen X-coordinate.
     * @param y Screen Y-coordinate.
     * @param viewBox Map view box.
     * @return List of photos close to given location.
     */
    public List<WayPoint> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<WayPoint> result = new ArrayList<>();
        Log.d(DEBUG_TAG, "getClicked");

        WayPoint[] wayPoints = tracker.getTrack().getWayPoints();
        if (wayPoints != null && wayPoints.length != 0) {
            final float tolerance = DataStyle.getCurrent().getNodeToleranceValue();
            for (WayPoint wpp : wayPoints) {
                int lat = wpp.getLat();
                int lon = wpp.getLon();
                float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
                float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, lat) - y);
                if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
                    if (Math.hypot(differenceX, differenceY) <= tolerance) {
                        result.add(wpp);
                    }
                }
            }
        }
        Log.d(DEBUG_TAG, "getClicked found " + result.size());
        return result;
    }
}
