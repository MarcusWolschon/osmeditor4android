package de.blau.android.grid;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.resources.DataStyle;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.MapViewOverlay;

public class MapOverlay extends MapViewOverlay {

    private static final String SCALE_NONE       = "SCALE_NONE";
    private static final String DEBUG_TAG        = MapOverlay.class.getName();
    private static final float  DISTANCE2SIDE_DP = 4f;
    private static final float  SHORTTICKS_DP    = 12f;
    public static final float   LONGTICKS_DP     = 20f;
    private static final double METERS2FEET      = 3.28084;
    private static final double MILE2FEET        = 5280;
    // private static final double YARD2FEET = 3;

    /** Map this is an overlay of. */
    private final Map map;

    private final Paint fullLine;
    private final Paint labelH;
    private final Paint labelV;

    private final float distance2side;
    private final float shortTicks;
    private final float longTicks;
    private final float oneDP;
    private final float textHeight;
    private final Main  main;
    private final int   actionBarHeight;
    private String      mode    = SCALE_NONE;
    private boolean     enabled = false;

    public MapOverlay(final Map map) {
        this.map = map;
        fullLine = DataStyle.getCurrent(DataStyle.CROSSHAIRS).getPaint();
        labelH = DataStyle.getCurrent(DataStyle.LABELTEXT).getPaint();
        labelV = new Paint(labelH);
        labelV.setTextAlign(Paint.Align.RIGHT);
        textHeight = labelV.getTextSize();
        distance2side = Density.dpToPx(map.getContext(), DISTANCE2SIDE_DP);
        shortTicks = Density.dpToPx(map.getContext(), SHORTTICKS_DP);
        longTicks = Density.dpToPx(map.getContext(), LONGTICKS_DP);
        oneDP = Density.dpToPx(map.getContext(), 1);
        main = map.getContext() instanceof Main ? (Main) map.getContext() : null;
        actionBarHeight = ThemeUtils.getActionBarHeight(map.getContext());
    }

    @Override
    public boolean isReadyToDraw() {
        mode = map.getPrefs().scaleLayer();
        enabled = !SCALE_NONE.equals(mode);
        return enabled && map.getBackgroundLayer().isReadyToDraw();
    }

    @Override
    protected void onDraw(Canvas c, IMapView osmv) {
        if (enabled && map.getViewBox().getWidth() < 200000000L) { // testing for < 20°
            int w = map.getWidth();
            int h = map.getHeight();
            boolean metric = mode.equals("SCALE_METRIC") || mode.equals("SCALE_GRID_METRIC");
            boolean grid = mode.equals("SCALE_GRID_METRIC") || mode.equals("SCALE_GRID_IMPERIAL");
            double centerLat = map.getViewBox().getCenterLat();
            double widthInMeters = GeoMath.haversineDistance(map.getViewBox().getLeft() / 1E7D, centerLat, map.getViewBox().getRight() / 1E7D, centerLat);
            // Log.d(DEBUG_TAG,"distance to side " + distance2side + " tick length long " + longTicks + " short " +
            // shortTicks);
            if (widthInMeters < 1000000 && widthInMeters > 0) { // don't show zoomed out
                float topOffset = 0f;
                // avoid drawing behind the action bar
                if (App.getLogic().getMode() == Mode.MODE_ALIGN_BACKGROUND || (main != null && main.getEasyEditManager().isProcessingAction())) {
                    topOffset = actionBarHeight;
                    Log.d(DEBUG_TAG, "offset " + topOffset);
                }
                c.drawLine(distance2side, distance2side + topOffset, w - distance2side, distance2side + topOffset, fullLine);
                c.drawLine(w - distance2side, distance2side + topOffset, w - distance2side, h - distance2side, fullLine);
                if (grid) {
                    c.drawLine(distance2side, h - distance2side, w - distance2side, h - distance2side, fullLine);
                    c.drawLine(distance2side, distance2side, distance2side, h - distance2side, fullLine);
                }
                if (metric) {
                    double metersPerPixel = widthInMeters / w;
                    double log10 = Math.log10(widthInMeters);
                    double tickDistance = Math.pow(10, Math.floor(log10) - 1);
                    // Log.d(DEBUG_TAG,"log10 " + log10 + " tick distance " + Math.pow(10,Math.floor(log10)-1));
                    if (widthInMeters / tickDistance <= 20) { // heuristic to make the visual effect a bit nicer
                        tickDistance = tickDistance / 10;
                    }
                    float tickDistanceH = Math.round(tickDistance / metersPerPixel);
                    int largeTickSpacing = 10;
                    if (tickDistanceH < 3 * fullLine.getStrokeWidth()) {
                        tickDistanceH = tickDistanceH * 5;
                        largeTickSpacing = 2;
                    }
                    boolean km = tickDistance * 10 >= 1000D;
                    boolean subMeter = tickDistance < 1;
                    c.drawText(km ? "km" : "m", distance2side, longTicks + topOffset + oneDP, labelH);
                    float nextTick = distance2side;
                    int i = 0;
                    Double nextLabel = 0D;
                    while (nextTick < (w - distance2side)) {
                        if (i == largeTickSpacing) {
                            i = 0;
                            c.drawLine(nextTick, distance2side + topOffset, nextTick, (grid ? h - distance2side : longTicks) + topOffset, fullLine);
                            nextLabel = nextLabel + 10 * tickDistance;
                            String labelText = subMeter ? String.format("%.1f", nextLabel) : Integer.toString((int) (km ? nextLabel / 1000 : nextLabel));
                            c.drawText(labelText, nextTick + 2 * oneDP, longTicks + topOffset + 2 * oneDP, labelH);
                        } else {
                            c.drawLine(nextTick, distance2side + topOffset, nextTick, shortTicks + topOffset, fullLine);
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
                            c.drawLine(w - distance2side, nextTick, grid ? distance2side : w - longTicks, nextTick, fullLine);
                            nextLabel = nextLabel + 10 * tickDistance;
                            String labelText = subMeter ? String.format("%.1f", nextLabel) : Integer.toString((int) (km ? nextLabel / 1000 : nextLabel));
                            c.drawText(labelText, w - (shortTicks + distance2side), nextTick + textHeight + oneDP, labelV);
                        } else {
                            c.drawLine(w - distance2side, nextTick, w - shortTicks, nextTick, fullLine);
                        }
                        i++;
                        nextTick = nextTick + tickDistanceH;
                    }
                } else { // imperial FIXME we could probably get rid of some duplicate code here
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
                    if (!mile && tickDistanceH < 3 * fullLine.getStrokeWidth()) {
                        tickDistanceH = tickDistanceH * largeTickSpacing / 2;
                        largeTickSpacing = 2;
                    }

                    c.drawText(mile ? "mile" : "ft", distance2side, longTicks + topOffset + oneDP, labelH);
                    float nextTick = distance2side;
                    int i = 0;
                    int nextLabel = 0;
                    while (nextTick < (w - distance2side)) {
                        if (i == largeTickSpacing) {
                            i = 0;
                            c.drawLine(nextTick, distance2side + topOffset, nextTick, (grid ? h - distance2side : longTicks) + topOffset, fullLine);
                            nextLabel = (int) (nextLabel + largeTickSpacing * tickDistance);
                            if (mile) {
                                c.drawText(Integer.toString((int) (nextLabel / MILE2FEET)), nextTick + 2 * oneDP, longTicks + topOffset + 2 * oneDP, labelH);
                            } else {
                                c.drawText(Integer.toString(nextLabel), nextTick + 2 * oneDP, longTicks + topOffset + 2 * oneDP, labelH);
                            }
                        } else {
                            c.drawLine(nextTick, distance2side + topOffset, nextTick, shortTicks + topOffset, fullLine);
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
                            c.drawLine(w - distance2side, nextTick, grid ? distance2side : w - longTicks, nextTick, fullLine);
                            nextLabel = (int) (nextLabel + largeTickSpacing * tickDistance);
                            if (mile) {
                                c.drawText(Integer.toString((int) (nextLabel / MILE2FEET)), w - (shortTicks + distance2side), nextTick + textHeight + oneDP,
                                        labelV);
                            } else {
                                c.drawText(Integer.toString(nextLabel), w - (shortTicks + distance2side), nextTick + textHeight + oneDP, labelV);
                            }
                        } else {
                            c.drawLine(w - distance2side, nextTick, w - shortTicks, nextTick, fullLine);
                        }
                        i++;
                        nextTick = nextTick + tickDistanceH;
                    }
                }
            }
        }
    }

    @Override
    protected void onDrawFinished(Canvas c, IMapView osmv) {
        // do nothing
    }
}
