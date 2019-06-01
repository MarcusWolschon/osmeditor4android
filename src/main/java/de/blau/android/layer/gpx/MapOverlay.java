package de.blau.android.layer.gpx;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.ViewWayPoint;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.osm.Track.WayPoint;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.services.TrackerService;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.views.IMapView;

public class MapOverlay extends StyleableLayer implements Serializable, ExtentInterface, ClickableInterface<WayPoint> {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    private transient TrackerService tracker;

    /** Map this is an overlay of. */
    private final transient Map map;

    private final transient FloatPrimitiveList linePoints;

    public static final String FILENAME = "gpxlayer.res";

    private transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();
    // private transient boolean saved = false;

    private int             iconRadius;
    private transient Paint trackPaint;
    private transient Paint wayPointPaint;
    private int             color;
    private float           strokeWidth;
    private String          labelKey;

    /**
     * Construct a new GPX layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(@NonNull final Map map) {
        this.map = map;
        linePoints = new FloatPrimitiveList();
        resetStyling();
    }

    @Override
    public boolean isReadyToDraw() {
        tracker = map.getTracker();
        return tracker != null;
    }

    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (!isVisible || tracker == null) {
            return;
        }
        List<TrackPoint> trackPoints = tracker.getTrackPoints();
        if (!trackPoints.isEmpty()) {
            map.pointListToLinePointsArray(linePoints, trackPoints);
            canvas.drawLines(linePoints.getArray(), 0, linePoints.size(), trackPaint);
        }
        WayPoint[] wayPoints = tracker.getTrack().getWayPoints();
        if (wayPoints.length != 0) {
            ViewBox viewBox = map.getViewBox();
            int width = map.getWidth();
            int height = map.getHeight();
            int zoomLevel = map.getZoomLevel();
            FeatureStyle fs = DataStyle.getInternal(DataStyle.LABELTEXT_NORMAL);
            Paint paint = fs.getPaint();
            Paint labelBackground = DataStyle.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
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

    @Override
    public List<WayPoint> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<WayPoint> result = new ArrayList<>();
        Log.d(DEBUG_TAG, "getClicked");
        if (tracker != null && tracker.getTrack() != null) {
            WayPoint[] wayPoints = tracker.getTrack().getWayPoints();
            if (wayPoints.length != 0) {
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
        }
        Log.d(DEBUG_TAG, "getClicked found " + result.size());
        return result;
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_gpx);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        if (tracker != null) {
            List<TrackPoint> trackPoints = tracker.getTrackPoints();
            trackPoints.addAll(tracker.getWayPoints());
            BoundingBox result = null;
            for (TrackPoint tp : trackPoints) {
                if (result == null) {
                    result = new BoundingBox(tp.getLongitude(), tp.getLatitude());
                } else {
                    result.union(tp.getLongitude(), tp.getLatitude());
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        if (tracker != null) {
            List<TrackPoint> trackPoints = tracker.getTrack().getTrackPoints();
            WayPoint[] wayPoints = tracker.getTrack().getWayPoints();
            return tracker.isTracking() || !trackPoints.isEmpty() || wayPoints.length != 0;
        }
        return false;
    }

    @Override
    public void onSelected(FragmentActivity activity, WayPoint wp) {
        ViewWayPoint.showDialog(activity, wp);
    }

    @Override
    public String getDescription(WayPoint wp) {
        return wp.getShortDescription(map.getContext());
    }

    @Override
    public int getColor() {
        return trackPaint.getColor();
    }

    @Override
    public void setColor(int color) {
        trackPaint.setColor(color);
        wayPointPaint.setColor(color);
        this.color = color;
    }

    @Override
    public float getStrokeWidth() {
        return trackPaint.getStrokeWidth();
    }

    @Override
    public void setStrokeWidth(float width) {
        trackPaint.setStrokeWidth(width);
        wayPointPaint.setStrokeWidth(width);
        strokeWidth = width;
    }

    @Override
    public Path getPointSymbol() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPointSymbol(Path symbol) {
        // TODO Auto-generated method stub
    }

    @Override
    public void resetStyling() {
        trackPaint = new Paint(DataStyle.getInternal(DataStyle.GPS_TRACK).getPaint());
        wayPointPaint = new Paint(DataStyle.getInternal(DataStyle.GPS_POS_FOLLOW).getPaint());
        color = trackPaint.getColor();
        strokeWidth = trackPaint.getStrokeWidth();
        labelKey = "";
        iconRadius = map.getIconRadius();
    }

    @Override
    public List<String> getLabelList() {
        return null;
    }

    @Override
    public void setLabel(String key) {
        labelKey = key;
    }

    /**
     * Stores the current state to the default storage file
     * 
     * @param context Android Context
     * @throws IOException on errors writing the file
     */
    public synchronized void onSaveState(@NonNull Context context) throws IOException {
        super.onSaveState(context);
        // TODO this doesn't really help with error conditions need to throw exception
        if (!savingHelper.save(context, FILENAME, this, true)) {
            // this is essentially catastrophic and can only happen if something went really wrong
            // running out of memory or disk, or HW failure
            if (context instanceof Activity) {
                Snack.barError((Activity) context, R.string.toast_statesave_failed);
            }
        }
    }

    /**
     * Loads any saved state from the default storage file
     * 
     * 
     * @param context Android context
     * @return true if the saved state was successfully read
     */
    public synchronized boolean onRestoreState(@NonNull Context context) {
        super.onRestoreState(context);
        MapOverlay restoredOverlay = savingHelper.load(context, FILENAME, true);
        if (restoredOverlay != null) {
            Log.d(DEBUG_TAG, "read saved state");
            iconRadius = restoredOverlay.iconRadius;
            color = restoredOverlay.color;
            trackPaint.setColor(color);
            wayPointPaint.setColor(color);
            strokeWidth = restoredOverlay.strokeWidth;
            trackPaint.setStrokeWidth(strokeWidth);
            wayPointPaint.setStrokeWidth(strokeWidth);
            labelKey = restoredOverlay.labelKey;
            return true;
        } else {
            Log.d(DEBUG_TAG, "saved state null");
            return false;
        }
    }

    @Override
    public WayPoint getSelected() {
        return null;
    }

    @Override
    public void deselectObjects() {
        // not used
    }
}
