package de.blau.android.layer.grid;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.Locale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SerializableTextPaint;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.IMapView;

public class MapOverlay extends StyleableLayer implements DiscardInterface, ConfigureInterface {

    private static final long serialVersionUID = 4L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapOverlay.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapOverlay.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MAX_WIDTH_IN_METERS = 1000000;

    private static final String METER_LABEL     = "m";
    private static final String KILOMETER_LABEL = "km";
    private static final String FEET_LABEL      = "ft";
    private static final String MILE_LABEL      = "mile";

    public static final String FILENAME = "grid" + "." + FileExtensions.RES;

    private transient SavingHelper<MapOverlay> savingHelper = new SavingHelper<>();

    public static final float   DISTANCE2SIDE_DP = 4f;
    private static final float  SHORTTICKS_DP    = 12f;
    public static final float   LONGTICKS_DP     = 20f;
    private static final double METERS2FEET      = 3.28084;
    private static final double MILE2FEET        = 5280;

    /** Map this is an overlay of. */
    private final transient Map map;

    private transient Main main;

    private SerializableTextPaint labelH;
    private SerializableTextPaint labelV;

    private float   distance2side;
    private float   shortTicks;
    private float   longTicks;
    private float   oneDP;
    private float   textHeight;
    private int     actionBarHeight;
    private boolean splitActionBar = false;
    private boolean metric;
    private boolean grid;

    /**
     * Construct a new grid overlay
     * 
     * @param map the Map instance that this will be drawn on/in
     */
    public MapOverlay(final Map map) {
        this.map = map;
        resetStyling();
        setPrefs(map.getPrefs());
    }

    @Override
    public boolean isReadyToDraw() {
        return true;
    }

    /**
     * Get any offset needed to avod drawing behing an actionbar
     * 
     * @return the offset
     */
    private float getTopOffset() {
        return (App.getLogic().getMode() == Mode.MODE_ALIGN_BACKGROUND || (main != null && main.getEasyEditManager().isProcessingAction() && splitActionBar))
                ? actionBarHeight
                : 0f;
    }

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {
        if (isVisible && map.getViewBox().getWidth() < 200000000L) { // testing for < 20°
            int w = map.getWidth();
            int h = map.getHeight();
            double centerLat = map.getViewBox().getCenterLat();
            double widthInMeters = GeoMath.haversineDistance(map.getViewBox().getLeft() / 1E7D, centerLat, map.getViewBox().getRight() / 1E7D, centerLat);
            if (widthInMeters < MAX_WIDTH_IN_METERS && widthInMeters > 0) { // don't show zoomed out
                float topOffset = getTopOffset();
                c.drawLine(distance2side, distance2side + topOffset, w - distance2side, distance2side + topOffset, paint);
                c.drawLine(w - distance2side, distance2side + topOffset, w - distance2side, h - distance2side, paint);
                if (grid) {
                    c.drawLine(distance2side, h - distance2side, w - distance2side, h - distance2side, paint);
                    c.drawLine(distance2side, distance2side, distance2side, h - distance2side, paint);
                }
                if (metric) {
                    drawMetric(c, w, h, widthInMeters, topOffset);
                } else {
                    drawImperial(c, w, h, widthInMeters, topOffset);
                }
            }
        }
    }

    /**
     * Draw ticks/grid in imperial units
     * 
     * @param c the Canvas to draw on
     * @param w the width of the Canvas in pixels
     * @param h the height of the Canvas in pixels
     * @param widthInMeters the width of the Canvas in meters
     * @param topOffset any offset to avoid actionbars
     */
    private void drawImperial(@NonNull Canvas c, int w, int h, double widthInMeters, float topOffset) {
        double widthInFeet = widthInMeters * METERS2FEET;
        double feetPerPixel = widthInFeet / w;
        boolean mile = widthInFeet > MILE2FEET;
        boolean subFoot = widthInFeet <= 10;

        double tickDistance = 0;
        int largeTickSpacing = 10;

        if (mile) { // between 1 and 12 miles use fractions
            if (widthInFeet <= 2 * MILE2FEET) {
                largeTickSpacing = 16;
                tickDistance = MILE2FEET / largeTickSpacing;
            } else if (widthInFeet <= 6 * MILE2FEET) {
                largeTickSpacing = 8;
                tickDistance = MILE2FEET / largeTickSpacing;
            } else if (widthInFeet <= 10 * MILE2FEET) {
                largeTickSpacing = 4;
                tickDistance = MILE2FEET / largeTickSpacing;
            } else {
                double log10 = Math.log10(widthInFeet / MILE2FEET);
                tickDistance = MILE2FEET * Math.pow(10, Math.floor(log10) - 1);
            }
        } else if (subFoot) {
            largeTickSpacing = 12;
            tickDistance = 1D / largeTickSpacing;
        } else {
            double log10 = Math.log10(widthInFeet);
            tickDistance = Math.pow(10, Math.floor(log10) - 1);
        }

        float tickDistanceH = Math.round(tickDistance / feetPerPixel);
        if (!mile && tickDistanceH < 3 * paint.getStrokeWidth()) {
            tickDistanceH = tickDistanceH * largeTickSpacing / 2;
            largeTickSpacing = 2;
        }

        c.drawText(mile ? MILE_LABEL : FEET_LABEL, distance2side, longTicks + topOffset + oneDP, labelH);
        float nextTick = distance2side;
        int i = 0;
        int nextLabel = 0;
        while (nextTick < (w - distance2side)) {
            if (i == largeTickSpacing) {
                i = 0;
                c.drawLine(nextTick, distance2side + topOffset, nextTick, (grid ? h - distance2side : longTicks) + topOffset, paint);
                nextLabel = (int) (nextLabel + largeTickSpacing * tickDistance);
                c.drawText(Integer.toString(getNextImperialLabel(mile, nextLabel)), nextTick + 2 * oneDP, longTicks + topOffset + 2 * oneDP, labelH);
            } else {
                c.drawLine(nextTick, distance2side + topOffset, nextTick, shortTicks + topOffset, paint);
            }
            i++;
            nextTick = nextTick + tickDistanceH;
        }

        nextTick = distance2side + tickDistanceH + topOffset; // dont't draw first tick
        i = 1;
        nextLabel = 0;
        while (nextTick < (h - distance2side)) {
            if (i == largeTickSpacing) {
                i = 0;
                c.drawLine(w - distance2side, nextTick, grid ? distance2side : w - longTicks, nextTick, paint);
                nextLabel = (int) (nextLabel + largeTickSpacing * tickDistance);
                c.drawText(Integer.toString(getNextImperialLabel(mile, nextLabel)), w - (shortTicks + distance2side), nextTick + textHeight + oneDP, labelV);
            } else {
                c.drawLine(w - distance2side, nextTick, w - shortTicks, nextTick, paint);
            }
            i++;
            nextTick = nextTick + tickDistanceH;
        }
    }

    /**
     * Get the next label value as an int
     * 
     * @param mile true if we are showing miless
     * @param nextLabel the raw next value for the label
     * @return the next value, potentially scaled
     */
    private int getNextImperialLabel(boolean mile, double nextLabel) {
        return (int) (mile ? nextLabel / MILE2FEET : nextLabel);
    }

    /**
     * Draw ticks/grid in metrix units
     * 
     * @param c the Canvas to draw on
     * @param w the width of the Canvas in pixels
     * @param h the height of the Canvas in pixels
     * @param widthInMeters the width of the Canvas in meters
     * @param topOffset any offset to avoid actionbars
     */
    private void drawMetric(@NonNull Canvas c, int w, int h, double widthInMeters, float topOffset) {
        double metersPerPixel = widthInMeters / w;
        double log10 = Math.log10(widthInMeters);
        double tickDistance = Math.pow(10, Math.floor(log10) - 1);
        if (widthInMeters / tickDistance <= 20) { // heuristic to make the visual effect a bit nicer
            tickDistance = tickDistance / 2;
        }
        float tickDistanceH = Math.round(tickDistance / metersPerPixel);
        int largeTickSpacing = 10;
        if (tickDistanceH < 3 * paint.getStrokeWidth()) {
            tickDistanceH = tickDistanceH * 5;
            largeTickSpacing = 2;
        }
        boolean km = tickDistance * 10 >= 1000D;
        boolean subMeter = tickDistance < 1;
        c.drawText(km ? KILOMETER_LABEL : METER_LABEL, distance2side, longTicks + topOffset + oneDP, labelH);
        float nextTick = distance2side;
        int i = 0;
        double nextLabel = 0D;
        while (nextTick < (w - distance2side)) {
            if (i == largeTickSpacing) {
                i = 0;
                c.drawLine(nextTick, distance2side + topOffset, nextTick, (grid ? h - distance2side : longTicks) + topOffset, paint);
                nextLabel = nextLabel + 10 * tickDistance;
                String labelText = subMeter ? String.format(Locale.US, "%.1f", nextLabel) : Integer.toString(getNextMetricLabel(km, nextLabel));
                c.drawText(labelText, nextTick + 2 * oneDP, longTicks + topOffset + 2 * oneDP, labelH);
            } else {
                c.drawLine(nextTick, distance2side + topOffset, nextTick, shortTicks + topOffset, paint);
            }
            i++;
            nextTick = nextTick + tickDistanceH;
        }

        nextTick = distance2side + tickDistanceH + topOffset; // dont't draw first tick
        i = 1;
        nextLabel = 0D;
        while (nextTick < (h - distance2side)) {
            if (i == largeTickSpacing) {
                i = 0;
                c.drawLine(w - distance2side, nextTick, grid ? distance2side : w - longTicks, nextTick, paint);
                nextLabel = nextLabel + 10 * tickDistance;
                String labelText = subMeter ? String.format(Locale.US, "%.1f", nextLabel) : Integer.toString(getNextMetricLabel(km, nextLabel));
                c.drawText(labelText, w - (shortTicks + distance2side), nextTick + textHeight + oneDP, labelV);
            } else {
                c.drawLine(w - distance2side, nextTick, w - shortTicks, nextTick, paint);
            }
            i++;
            nextTick = nextTick + tickDistanceH;
        }
    }

    /**
     * Get the next label value as an int
     * 
     * @param km true if we are showing kilometers
     * @param nextLabel the raw next value for the label
     * @return the next value, potentially scaled
     */
    private int getNextMetricLabel(boolean km, double nextLabel) {
        return (int) (km ? nextLabel / 1000 : nextLabel);
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }

    @Override
    public String getName() {
        return map.getContext().getString(R.string.layer_grid);
    }

    @Override
    public void invalidate() {
        map.invalidate();
    }

    @Override
    public boolean enableConfiguration() {
        return true;
    }

    @Override
    public void configure(FragmentActivity activity) {
        ConfigurationDialog.showDialog(activity);
    }

    @Override
    public LayerType getType() {
        return LayerType.SCALE;
    }

    @Override
    public void setPrefs(Preferences prefs) {
        String mode = prefs.scaleLayer();
        splitActionBar = prefs.splitActionBarEnabled();
        Context ctx = map.getContext();
        metric = ctx.getString(R.string.scale_metric).equals(mode) || ctx.getString(R.string.scale_grid_metric).equals(mode);
        grid = ctx.getString(R.string.scale_grid_metric).equals(mode) || ctx.getString(R.string.scale_grid_imperial).equals(mode);
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        labelH.setColor(color);
        labelV.setColor(color);
    }

    @Override
    public boolean usesPointSymbol() {
        return false;
    }

    @Override
    public void resetStyling() {
        Log.d(DEBUG_TAG, "resetStyling");
        main = map.getContext() instanceof Main ? (Main) map.getContext() : null;
        DataStyle styles = map.getDataStyle();
        paint = new SerializableTextPaint(styles.getInternal(DataStyle.CROSSHAIRS).getPaint());
        labelH = new SerializableTextPaint(styles.getInternal(DataStyle.LABELTEXT).getPaint());
        labelV = new SerializableTextPaint(labelH);
        labelV.setTextAlign(Paint.Align.RIGHT);
        textHeight = labelV.getTextSize();
        distance2side = Density.dpToPx(map.getContext(), DISTANCE2SIDE_DP);
        shortTicks = Density.dpToPx(map.getContext(), SHORTTICKS_DP);
        longTicks = Density.dpToPx(map.getContext(), LONGTICKS_DP);
        oneDP = Density.dpToPx(map.getContext(), 1);

        actionBarHeight = ThemeUtils.getActionBarHeight(map.getContext());
    }

    @Override
    protected synchronized boolean save(@NonNull Context context) throws IOException {
        Log.d(DEBUG_TAG, "Saving state to " + FILENAME);
        return savingHelper.save(context, FILENAME, this, true);
    }

    @Override
    protected synchronized StyleableLayer load(@NonNull Context context) {
        Log.d(DEBUG_TAG, "Loading state from " + FILENAME);
        MapOverlay restored = savingHelper.load(context, FILENAME, true);
        if (restored != null) {
            labelH = restored.labelH;
            labelV = restored.labelV;
        }
        return restored;
    }

    @Override
    protected void discardLayer(Context context) {
        // Don't do anything except the default
    }
}
