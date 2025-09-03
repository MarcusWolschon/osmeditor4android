package de.blau.android.layer.gpx;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.ViewWayPoint;
import de.blau.android.gpx.Track;
import de.blau.android.gpx.TrackPoint;
import de.blau.android.gpx.WayPoint;
import de.blau.android.layer.ClickableInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LabelMinZoomInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.StyleableFileLayer;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.resources.symbols.TriangleDown;
import de.blau.android.services.TrackerService;
import de.blau.android.util.ColorUtil;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.GeoMath;
import de.blau.android.util.PlaybackTask;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SerializableTextPaint;
import de.blau.android.util.collections.FloatPrimitiveList;
import de.blau.android.views.IMapView;

public class MapOverlay extends StyleableFileLayer
        implements Serializable, ExtentInterface, ClickableInterface<WayPoint>, LayerInfoInterface, LabelMinZoomInterface {

    private static final long serialVersionUID = 5L; // note that this can't actually be serialized as the transient
                                                     // wields need to be set in readObject

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    private static final String FILENAME = "gpxlayer" + "." + FileExtensions.RES;

    private final transient FloatPrimitiveList       linePoints   = new FloatPrimitiveList(FloatPrimitiveList.MEDIUM_DEFAULT);
    private final transient Path                     path         = new Path();
    private final transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();

    private transient Track       track;
    private transient GpxPlayback playbackTask = null;

    private SerializableTextPaint wayPointPaint;
    private String                labelKey;
    private int                   labelMinZoom;
    private TrackPoint            pausedPoint;

    // way point label styling
    private final transient FontMetrics  fm;
    private final transient Paint        labelBackground;
    private final transient float        yOffset;
    private final transient Paint        fontPaint;
    private final transient List<String> labelList;

    /**
     * Construct a new GPX layer
     * 
     * @param map the current Map instance
     * @param contentId the id for the current contents
     */
    public MapOverlay(@NonNull final Map map, @NonNull String contentId) {
        super(contentId, FILENAME);
        this.map = map;
        Context context = map.getContext();
        final Preferences prefs = map.getPrefs();
        // this is slightly annoying as we need to protect against overwriting already saved state
        initStyling(!hasStateFile(context), prefs.getGpxStrokeWidth(), prefs.getGpxLabelSource(), prefs.getGpxLabelMinZoom(), prefs.getGpxSynbol());
        DataStyle styles = map.getDataStyle();
        setColor(ColorUtil.generateColor(map.getLayerTypeCount(LayerType.GPX), 9, styles.getInternal(DataStyle.GPS_TRACK).getPaint().getColor()));

        // the following can only be changed in the DataStyle
        FeatureStyle fs = styles.getInternal(DataStyle.LABELTEXT_NORMAL);
        fontPaint = fs.getPaint();
        fm = fs.getFontMetrics();
        labelBackground = styles.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
        yOffset = 2 * fontPaint.getStrokeWidth() + iconRadius;

        labelList = Arrays.asList(context.getString(R.string.gpx_automatic), context.getString(R.string.gpx_name), context.getString(R.string.gpx_description),
                context.getString(R.string.gpx_type));
    }

    /**
     * Set the Track to display
     * 
     * @param track the track to set
     */
    public void setTrack(@Nullable Track track) {
        this.track = track;
    }

    /**
     * Retrieve the Track we are displaying
     * 
     * @return a Track or null
     */
    @Nullable
    public Track getTrack() {
        return track;
    }

    @Override
    public boolean isReadyToDraw() {
        return track != null;
    }

    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (!isVisible || track == null) {
            return;
        }
        drawTrackPoints(canvas);
        drawWayPoints(canvas);
    }

    /**
     * Draw the trackpoints
     * 
     * @param canvas the Canvas to draw on
     */
    private void drawTrackPoints(@NonNull Canvas canvas) {
        List<TrackPoint> trackPoints = track.getTrackPoints();
        int size = trackPoints.size();
        if (size > 0) {
            map.pointListToLinePointsArray(linePoints, trackPoints);
            GeoMath.squashPointsArray(linePoints, getStrokeWidth() * 2);
            float[] linePointsArray = linePoints.getArray();
            path.rewind();
            path.moveTo(linePointsArray[0], linePointsArray[1]);
            int pointsSize = linePoints.size();
            for (int i = 0; i < pointsSize; i = i + 4) {
                path.lineTo(linePointsArray[i + 2], linePointsArray[i + 3]);
            }
            canvas.drawPath(path, paint);
        }
    }

    /**
     * Draw way points
     * 
     * @param canvas the Canvas to draw on to
     */
    private void drawWayPoints(@NonNull Canvas canvas) {
        if (symbolPath == null) {
            return;
        }
        final ViewBox viewBox = map.getViewBox();
        final int width = map.getWidth();
        final int height = map.getHeight();
        final int zoomLevel = map.getZoomLevel();
        final int labelIndex = labelList.indexOf(labelKey);
        final boolean drawLabel = zoomLevel >= labelMinZoom && labelIndex >= 0;
        final float topOffset = yOffset + fm.bottom;
        final float textSize = fontPaint.getTextSize();
        for (WayPoint wp : track.getWayPoints()) {
            int lon = wp.getLon();
            int lat = wp.getLat();
            if (viewBox.contains(lon, lat)) {
                float x = GeoMath.lonE7ToX(width, viewBox, lon);
                float y = GeoMath.latE7ToY(height, width, viewBox, lat);
                canvas.save();
                canvas.translate(x, y);
                canvas.drawPath(symbolPath, wayPointPaint);
                canvas.restore();
                if (drawLabel) {
                    String label = indexToLabel(labelIndex, wp);
                    if (label != null) {
                        float halfTextWidth = fontPaint.measureText(label) / 2;
                        float top = y + topOffset;
                        canvas.drawRect(x - halfTextWidth, top, x + halfTextWidth, top - textSize, labelBackground);
                        canvas.drawText(label, x - halfTextWidth, y + yOffset, fontPaint);
                    }
                }
            }
        }
    }

    /**
     * Get a label from a waypoint
     * 
     * @param labelIndex the index
     * @param wp the waypoint
     * @return the label or null
     */
    @Nullable
    private String indexToLabel(final int labelIndex, WayPoint wp) {
        switch (labelIndex) {
        case 0:
            return wp.getLabel();
        case 1:
            return wp.getName();
        case 2:
            return wp.getDescription();
        case 3:
            return wp.getType();
        default:
            return null;
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public void onDestroy() {
        setTrack(null);
    }

    @Override
    public List<WayPoint> getClicked(final float x, final float y, final ViewBox viewBox) {
        List<WayPoint> result = new ArrayList<>();
        Log.d(DEBUG_TAG, "getClicked");
        if (track != null) {
            List<WayPoint> wayPoints = track.getWayPoints();
            if (!wayPoints.isEmpty()) {
                final float tolerance = map.getDataStyle().getCurrent().getNodeToleranceValue();
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

    /**
     * Set the name of this layer
     * 
     * @param name the name
     */
    public void setName(@Nullable String name) {
        this.name = name;
        if (name != null && FILENAME.equals(stateFileName)) {
            setStateFileName(name);
        }
    }

    @Override
    public String getName() {
        return name != null ? name : map.getContext().getString(R.string.layer_gpx);
    }

    @Override
    @Nullable
    public String getContentId() {
        return contentId;
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public BoundingBox getExtent() {
        if (track != null) {
            List<TrackPoint> trackPoints = track.getTrackPoints();
            trackPoints.addAll(track.getWayPoints());
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
        ViewWayPoint.showDialog(activity, contentId, wp);
    }

    @Override
    public SpannableString getDescription(WayPoint wp) {
        return getDescription(map.getContext(), wp);
    }

    @Override
    public SpannableString getDescription(Context context, WayPoint wp) {
        return new SpannableString(wp.getShortDescription(context));
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
        map.getPrefs().setGpxStrokeWidth(width);
    }

    @Override
    public void resetStyling() {
        initStyling(true, DataStyle.DEFAULT_GPX_STROKE_WIDTH, labelList.get(0), Map.SHOW_LABEL_LIMIT, TriangleDown.NAME);
    }

    /**
     * Set the styling to the provided values
     * 
     * @param style if true set styling
     * @param strokeWidth the stroke width
     * @param labelKey the source of the label
     * @param labelMinZoom min. zoom from on we show the label
     * @param symbolName the name of the point symbol
     */
    private void initStyling(boolean style, float strokeWidth, @NonNull String labelKey, int labelMinZoom, @NonNull String symbolName) {
        DataStyle styles = map.getDataStyle();
        paint = new SerializableTextPaint(styles.getInternal(DataStyle.GPS_TRACK).getPaint());
        wayPointPaint = new SerializableTextPaint(styles.getInternal(DataStyle.GPS_POS_FOLLOW).getPaint());
        iconRadius = map.getIconRadius();
        if (style) {
            paint.setStrokeWidth(strokeWidth);
            // currently styling always sets the waypoint stroke width to the same as the track
            wayPointPaint.setStrokeWidth(strokeWidth);
            setLabel(labelKey);
            setLabelMinZoom(labelMinZoom);
            setPointSymbol(symbolName);
        }
    }

    @Override
    public void setLabel(String key) {
        dirty();
        labelKey = key;
        map.getPrefs().setGpxLabelSource(key);
    }

    @Override
    public List<String> getLabelList() {
        return new ArrayList<>(labelList); // list will be modified in caller
    }

    @Override
    public String getLabel() {
        return labelKey;
    }

    @Override
    public void setLabelMinZoom(int minZoom) {
        dirty();
        labelMinZoom = minZoom;
        map.getPrefs().setGpxLabelMinZoom(minZoom);
    }

    @Override
    public int getLabelMinZoom() {
        return labelMinZoom;
    }

    @Override
    public void setPointSymbol(@Nullable String symbol) {
        super.setPointSymbol(symbol);
        if (symbol != null) {
            map.getPrefs().setGpxSymbol(symbol);
        }
    }

    /**
     * Read a file in GPX format from device
     * 
     * @param ctx current context this was called from
     * @param uri Uri for the file to read
     * @return true if the file was loaded successfully
     */
    public boolean fromFile(@NonNull final Context ctx, @NonNull final Uri uri) {
        Log.d(DEBUG_TAG, "Loading track from " + uri);
        if (track == null) {
            track = new Track(ctx, false);
        }
        name = ContentResolverUtil.getDisplaynameColumn(ctx, uri);
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        setStateFileName(uri.getEncodedPath());
        Logic logic = App.getLogic();

        final int ERROR = -1;
        final int OK = 0;

        try {
            return new ExecutorTask<Void, Void, Integer>(logic.getExecutorService(), logic.getHandler()) {

                @Override
                protected Integer doInBackground(Void arg) {
                    final String uriString = uri.toString();
                    try (InputStream is = ctx.getContentResolver().openInputStream(uri); BufferedInputStream in = new BufferedInputStream(is)) {
                        track.importFromGPX(in);
                        return OK;
                    } catch (SecurityException sex) {
                        Log.e(DEBUG_TAG, sex.getMessage());
                        // note need a context here that is on the ui thread
                        ScreenMessage.toastTopError(map.getContext(), ctx.getString(R.string.toast_permission_denied, uriString));
                        return ERROR;
                    } catch (IOException iex) {
                        ScreenMessage.toastTopError(map.getContext(), ctx.getString(R.string.toast_error_reading, uriString));
                        return ERROR;
                    }
                }
            }.execute().get(Server.DEFAULT_TIMEOUT, TimeUnit.SECONDS) == OK; // result is not going to be null
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            Log.e(DEBUG_TAG, e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized boolean save(@NonNull Context context) throws IOException {
        Log.d(DEBUG_TAG, "Saving state to " + stateFileName);
        if (playbackTask != null) {
            playbackTask.pause();
            pausedPoint = playbackTask.getPausedPoint();
            playbackTask.cancel();
        }
        return savingHelper.save(context, stateFileName, this, true);
    }

    @Override
    public synchronized StyleableLayer load(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Loading state from " + stateFileName);
        MapOverlay restoredOverlay = savingHelper.load(context, stateFileName, true, true, false);
        if (restoredOverlay == null) {
            return null;
        }
        Log.d(DEBUG_TAG, "read saved state");
        wayPointPaint = restoredOverlay.wayPointPaint;
        labelKey = restoredOverlay.labelKey;
        labelMinZoom = restoredOverlay.labelMinZoom;
        if (playbackTask == null && restoredOverlay.pausedPoint != null) {
            // restart playback
            playbackTask = new GpxPlayback();
            playbackTask.execute(restoredOverlay.pausedPoint);
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
        track = null;
        File originalFile = context.getFileStreamPath(stateFileName);
        if (!originalFile.delete()) { // NOSONAR requires API 26
            Log.e(DEBUG_TAG, "Failed to delete state file " + stateFileName);
        }
        map.invalidate();
    }

    /**
     * Start/resume playback of this track
     */
    public void startPlayback() {
        if (playbackTask != null) {
            playbackTask.resume();
            return;
        }
        playbackTask = new GpxPlayback();
        playbackTask.execute(null);
    }

    private class GpxPlayback extends PlaybackTask<TrackPoint, Void, Void> {
        private boolean       paused      = false;
        private TrackPoint    pausedPoint = null;
        private final Context context;

        /**
         * Create a new instance
         */
        public GpxPlayback() {
            super(App.getLogic().getExecutorService(), App.getLogic().getHandler());
            context = MapOverlay.this.map.getContext();
            if (!(context instanceof Main)) {
                throw new IllegalStateException("Needs to be run from Main");
            }
        }

        @Override
        protected Void doInBackground(TrackPoint start) throws Exception {
            TrackerService tracker = ((Main) context).getTracker();
            final Track t = getTrack();
            if (t != null) {
                Location loc = new Location(LocationManager.GPS_PROVIDER);
                final List<TrackPoint> points = t.getTrackPoints();
                List<TrackPoint> pointsToPlay = start == null ? points : points.subList(points.indexOf(start) + 1, points.size());
                for (TrackPoint tp : pointsToPlay) {
                    while (paused && !isCancelled()) {
                        pausedPoint = tp;
                        sleep();
                    }

                    if (isCancelled()) {
                        break;
                    }

                    tp.toLocation(loc);
                    tracker.setGpsLocation(loc);
                    sleep();
                }
            } else {
                Log.e(DEBUG_TAG, "Null track");
            }
            return null;
        }

        /**
         * Sleep 1s, this could be adjustable
         */
        private void sleep() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { // NOSONAR
                //
            }
        }

        @Override
        protected void onPostExecute(Void output) {
            playbackTask = null;
            ScreenMessage.toastTopInfo(context, R.string.layer_toast_playback_finished);
        }

        @Override
        public void pause() {
            paused = true;
            ScreenMessage.toastTopInfo(context, R.string.layer_toast_playback_paused);
        }

        @Override
        public void resume() {
            paused = false;
            ScreenMessage.toastTopInfo(context, R.string.layer_toast_playback_resumed);
        }

        @Override
        public boolean isPaused() {
            return paused;
        }

        /**
         * Get the point at which we were paused
         * 
         * @return the TrackPoint or null
         */
        @Nullable
        public TrackPoint getPausedPoint() {
            return pausedPoint;
        }

    }

    /**
     * Pause playback
     */
    public void pausePlayback() {
        if (playbackTask != null) {
            playbackTask.pause();
        }
    }

    /**
     * Check if we are playing the track
     * 
     * @return true if the track is being played
     */
    public boolean isPlaying() {
        return playbackTask != null && !playbackTask.isPaused();
    }

    /**
     * Check if we are not playing the track
     * 
     * @return true if we are not playing the track
     */
    public boolean isStopped() {
        return playbackTask == null;
    }

    /**
     * Stop playing the track
     */
    public void stopPlayback() {
        if (playbackTask != null) {
            playbackTask.cancel();
        }
    }

    @Override
    public void showInfo(FragmentActivity activity) {
        LayerInfo f = new GpxLayerInfo();
        f.setShowsDialog(true);
        Bundle args = new Bundle();
        args.putSerializable(GpxLayerInfo.LAYER_ID_KEY, contentId);
        f.setArguments(args);
        LayerInfo.showDialog(activity, f);
    }
}
