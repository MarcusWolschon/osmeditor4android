package de.blau.android.layer.bookmarks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.RectF;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.bookmarks.Bookmark;
import de.blau.android.bookmarks.BookmarkStorage;
import de.blau.android.contract.FileExtensions;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.LabelMinZoomInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.ViewBox;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SerializableTextPaint;
import de.blau.android.views.IMapView;

public class MapOverlay extends StyleableLayer implements DiscardInterface, LabelMinZoomInterface {

    private static final long serialVersionUID = 1L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    public static final String FILENAME = "bookmarks" + "." + FileExtensions.RES;

    private static final int SHOW_LABEL_LIMIT = 14;

    private transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();

    private int             labelMinZoom = SHOW_LABEL_LIMIT;
    private transient Paint labelPaint;
    transient Paint         labelBackground;
    transient float         labelStrokeWidth;
    transient FeatureStyle  labelFs;
    private transient RectF rect;

    private final transient BookmarkStorage bookmarkStorage;

    /**
     * Construct a new grid overlay
     * 
     * @param map the Map instance that this will be drawn on/in
     */
    public MapOverlay(final Map map) {
        this.map = map;
        resetStyling();
        setPrefs(map.getPrefs());
        bookmarkStorage = new BookmarkStorage();
        bookmarkStorage.readList(map.getContext());
        rect = new RectF();
    }

    @Override
    public boolean isReadyToDraw() {
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas, IMapView osmv) {
        if (isVisible) {
            ViewBox vBox = map.getViewBox();
            int width = map.getWidth();
            int height = map.getHeight();
            int zoomLevel = map.getZoomLevel();
            for (Bookmark b : bookmarkStorage.getBookmarks()) {
                ViewBox bb = b.getViewBox();
                if (bb.intersects(vBox)) {
                    float left = GeoMath.lonE7ToX(width, vBox, bb.getLeft());
                    float right = GeoMath.lonE7ToX(width, vBox, bb.getRight());
                    float bottom = GeoMath.latE7ToY(height, width, vBox, bb.getBottom());
                    float top = GeoMath.latE7ToY(height, width, vBox, bb.getTop());
                    rect.set(left, top, right, bottom);
                    canvas.drawRect(rect, paint);
                    String label = b.getComment();
                    if (zoomLevel >= labelMinZoom && !"".equals(label)) {
                        double[] center = bb.getCenter();
                        float x = GeoMath.lonToX(width, vBox, center[0]);
                        float y = GeoMath.latToY(height, width, vBox, center[1]);
                        float halfTextWidth = labelPaint.measureText(label) / 2;
                        FontMetrics fm = labelFs.getFontMetrics();
                        canvas.drawRect(x - halfTextWidth, y + fm.bottom, x + halfTextWidth, y - labelPaint.getTextSize() + fm.bottom, labelBackground);
                        canvas.drawText(label, x - halfTextWidth, y, labelPaint);
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
    public String getName() {
        return map.getContext().getString(R.string.layer_bookmarks);
    }

    @Override
    public void invalidate() {
        bookmarkStorage.readList(map.getContext());
        map.invalidate();
    }

    @Override
    public LayerType getType() {
        return LayerType.BOOKMARKS;
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        labelPaint.setColor(color);
    }

    @Override
    public void resetStyling() {
        Log.d(DEBUG_TAG, "resetStyling");
        DataStyle styles = map.getDataStyle();
        labelFs = styles.getInternal(DataStyle.LABELTEXT_NORMAL);
        labelPaint = new SerializableTextPaint(labelFs.getPaint());
        labelStrokeWidth = labelPaint.getStrokeWidth();
        labelBackground = styles.getInternal(DataStyle.LABELTEXT_BACKGROUND).getPaint();
        paint = new SerializableTextPaint(styles.getInternal(DataStyle.BOOKMARK_DEFAULT).getPaint());
        labelPaint.setColor(paint.getColor());
        labelMinZoom = SHOW_LABEL_LIMIT;
    }

    @Override
    public void setLabelMinZoom(int minZoom) {
        labelMinZoom = minZoom;
    }

    @Override
    public int getLabelMinZoom() {
        return labelMinZoom;
    }

    @Override
    public boolean usesPointSymbol() {
        return false;
    }

    @Override
    protected synchronized boolean save(@NonNull Context context) throws IOException {
        Log.d(DEBUG_TAG, "Saving state to " + FILENAME);
        return savingHelper.save(context, FILENAME, this, true);
    }

    @Override
    protected synchronized StyleableLayer load(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Loading state from " + FILENAME);
        MapOverlay restored = savingHelper.load(context, FILENAME, true, true, false);
        if (restored != null) {
            labelMinZoom = restored.labelMinZoom;
        }
        return restored;
    }

    @Override
    protected void discardLayer(Context context) {
        // Don't do anything except the default
    }
}
