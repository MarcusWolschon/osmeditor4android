package de.blau.android.layer.gpx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.ViewWayPoint;
import de.blau.android.gpx.TrackPoint;
import de.blau.android.gpx.WayPoint;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.resources.symbols.TriangleDown;
import de.blau.android.services.TrackerService;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SerializablePaint;
import de.blau.android.util.Util;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.views.IMapView;

public class MapOverlay extends StyleableLayer implements Serializable, ExtentInterface, ClickableInterface<WayPoint> {

    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = MapOverlay.class.getName();

    private static final String FILENAME = "gpxlayer.res";

    private static final int TRACKPOINT_PARALLELIZATION_THRESHOLD = 10000; // multithreaded if more trackpoints

    /** Map this is an overlay of. */
    private final transient Map map;

    private final transient ExecutorService executorService;
    private final transient ArrayList<FloatPrimitiveList> linePointsList;
    private final transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();

    private transient TrackerService tracker;

    private SerializablePaint wayPointPaint;
    private String            labelKey;

    /**
     * Construct a new GPX layer
     * 
     * @param map the current Map instance
     */
    public MapOverlay(@NonNull final Map map) {
        this.map = map;
        resetStyling();

        int threadPoolSize = Util.usableProcessors();
        Log.d(DEBUG_TAG,"using " + threadPoolSize + " threads");
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        linePointsList = new ArrayList<>(threadPoolSize);
        for(int i = 0; i < threadPoolSize; i++) {
            linePointsList.add(new FloatPrimitiveList());
        }
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
            if (trackPoints.size() < TRACKPOINT_PARALLELIZATION_THRESHOLD || linePointsList.size() == 1) {
                final FloatPrimitiveList linePoints = linePointsList.get(0);
                map.pointListToLinePointsArray(linePoints, trackPoints);
                GeoMath.squashPointsArray(linePoints, getStrokeWidth() * 2);
                canvas.drawLines(linePoints.getArray(), 0, linePoints.size(), paint);
            } else {
                int offset = 0;
                int length = trackPoints.size() / linePointsList.size();
                List<Callable<Void>> callableTasks = new ArrayList<>();
                for (int i = linePointsList.size() - 1; i >= 0; i--) {
                    final FloatPrimitiveList finalLinePoints = linePointsList.get(i);
                    final int finalOffset = offset;
                    final int finalLength;
                    if (i != 0) {
                        finalLength = length + 1; // + 1 to join with next chunk
                    } else {
                        finalLength = trackPoints.size() - offset; // last chunk slightly different due to division remainder
                    }
                    callableTasks.add(() -> {
                        map.pointListToLinePointsArray(finalLinePoints, trackPoints, finalOffset, finalLength);
                        GeoMath.squashPointsArray(finalLinePoints, getStrokeWidth() * 2);
                        canvas.drawLines(finalLinePoints.getArray(), 0, finalLinePoints.size(), paint);
                        return null;
                    });
                    offset += length;
                }
                try {
                    executorService.invokeAll(callableTasks);
                } catch (InterruptedException e) {
                    Log.e(DEBUG_TAG, e.getMessage());
                }
            }
        }
        WayPoint[] wayPoints = tracker.getTrack().getWayPoints();
        if (wayPoints.length != 0 && symbolPath != null) {
            ViewBox viewBox = map.getViewBox();
            int width = map.getWidth();
            int height = map.getHeight();
            int zoomLevel = map.getZoomLevel();
            FeatureStyle fs = DataStyle.getInternal(DataStyle.LABELTEXT_NORMAL);
            Paint paint = fs.getPaint();
            Paint labelBackground = DataStyle.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
            float pointStrokeWidth = paint.getStrokeWidth();
            float yOffset = 2 * pointStrokeWidth + iconRadius;
            for (WayPoint wp : wayPoints) {
                if (viewBox.contains(wp.getLongitude(), wp.getLatitude())) {
                    float x = GeoMath.lonE7ToX(width, viewBox, wp.getLon());
                    float y = GeoMath.latE7ToY(height, width, viewBox, wp.getLat());
                    canvas.save();
                    canvas.translate(x, y);
                    canvas.drawPath(symbolPath, wayPointPaint);
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
                    if ((differenceX <= tolerance) && (differenceY <= tolerance) && Math.hypot(differenceX, differenceY) <= tolerance) {
                        result.add(wpp);
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
    public void onSelected(FragmentActivity activity, WayPoint wp) {
        ViewWayPoint.showDialog(activity, wp);
    }

    @Override
    public String getDescription(WayPoint wp) {
        return wp.getShortDescription(map.getContext());
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        wayPointPaint.setColor(color);
    }

    @Override
    public void setStrokeWidth(float width) {
        super.setStrokeWidth(width);
        wayPointPaint.setStrokeWidth(width);
    }

    @Override
    public void resetStyling() {
        paint = new SerializablePaint(DataStyle.getInternal(DataStyle.GPS_TRACK).getPaint());
        wayPointPaint = new SerializablePaint(DataStyle.getInternal(DataStyle.GPS_POS_FOLLOW).getPaint());
        labelKey = "";
        iconRadius = map.getIconRadius();
        symbolName = TriangleDown.NAME;
        symbolPath = DataStyle.getCurrent().getSymbol(TriangleDown.NAME);
    }

    @Override
    public void setLabel(String key) {
        labelKey = key;
    }

    @Override
    public synchronized boolean save(@NonNull Context context) throws IOException {
        return savingHelper.save(context, FILENAME, this, true);
    }

    @Override
    public synchronized StyleableLayer load(@NonNull Context context) {
        MapOverlay restoredOverlay = savingHelper.load(context, FILENAME, true);
        if (restoredOverlay != null) {
            Log.d(DEBUG_TAG, "read saved state");
            wayPointPaint = restoredOverlay.wayPointPaint;
            labelKey = restoredOverlay.labelKey;
        }
        return restoredOverlay;
    }

    @Override
    public WayPoint getSelected() {
        return null;
    }

    @Override
    public void deselectObjects() {
        // not used
    }

    @Override
    public void setSelected(WayPoint o) {
        // not used
    }

    @Override
    public LayerType getType() {
        return LayerType.GPX;
    }

    @Override
    protected void discardLayer(Context context) {
        if (tracker != null) {
            // FIXME this might not be what the user wants
            tracker.stopTracking(false);
        }
    }
}
