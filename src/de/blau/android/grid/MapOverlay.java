package de.blau.android.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.graphics.AvoidXfermode;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.resources.Profile;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.IssueAlert;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

public class MapOverlay extends OpenStreetMapViewOverlay {
	
	private static final String DEBUG_TAG = MapOverlay.class.getName();
	private static final float DISTANCE2SIDE = 4f;
	private static final float SHORTTICKS = 12f;
	private static final float LONGTICKS = 20f;
	private static final double METERS2FEET = 3.28084;
	private static final double MILE2FEET = 5280;
	private static final double YARD2FEET = 3;
	
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
	
	public MapOverlay(final Map map, Server s) {
		this.map = map;
		fullLine = Profile.getCurrent(Profile.CROSSHAIRS).getPaint();
		labelH = Profile.getCurrent(Profile.LABELTEXT).getPaint();
		labelV = new Paint(labelH);
		labelV.setTextAlign(Paint.Align.RIGHT);
		textHeight = labelV.getTextSize();
		distance2side = Density.dpToPx(map.getContext(),DISTANCE2SIDE);
		shortTicks = Density.dpToPx(map.getContext(),SHORTTICKS);
		longTicks = Density.dpToPx(map.getContext(),LONGTICKS);
		oneDP = Density.dpToPx(map.getContext(),1);
	}
	
	@Override
	public boolean isReadyToDraw() {
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			return map.getOpenStreetMapTilesOverlay().isReadyToDraw();
		}
		return true;
	}
	
	@Override
	protected void onDraw(Canvas c, IMapView osmv) {
		String mode = map.getPrefs().scaleLayer();
		if (!mode.equals("SCALE_NONE") && map.getViewBox().getWidth() < 200000000L) { // testing for < 20°
			int w = map.getWidth();
			int h = map.getHeight();
			boolean metric = mode.equals("SCALE_METRIC") || mode.equals("SCALE_GRID_METRIC");
			boolean grid = mode.equals("SCALE_GRID_METRIC") || mode.equals("SCALE_GRID_IMPERIAL");
			double centerLat = map.getViewBox().getCenterLat();
			double widthInMeters = GeoMath.haversineDistance(map.getViewBox().getLeft()/1E7D, centerLat, map.getViewBox().getRight()/1E7D, centerLat);
			// Log.d(DEBUG_TAG,"distance to side " + distance2side + " tick length long " + longTicks + " short " + shortTicks);
			if (widthInMeters < 1000000 && widthInMeters > 0) { // don't show zoomed out
				c.drawLine(distance2side, distance2side, w-distance2side, distance2side, fullLine);
				c.drawLine(w-distance2side, distance2side, w-distance2side, h-distance2side, fullLine);
				if (grid) {
					c.drawLine(distance2side, h-distance2side, w-distance2side, h-distance2side, fullLine);
					c.drawLine(distance2side, distance2side, distance2side, h-distance2side, fullLine);
				}
				if (metric) {
					double metersPerPixel = widthInMeters/w;
					double log10 = Math.log10(widthInMeters);
					double tickDistance = Math.pow(10,Math.floor(log10)-1);
					// Log.d(DEBUG_TAG,"log10 " + log10 + " tick distance " + Math.pow(10,Math.floor(log10)-1));
					if (widthInMeters/tickDistance <= 20) { // heuristic to make the visual effect a bit nicer
						tickDistance = tickDistance /10;
					}
					float tickDistanceH = Math.round(tickDistance/metersPerPixel);

					boolean km = tickDistance*10 >= 1000;
					c.drawText(km ? "km" : "m", distance2side, longTicks + oneDP, labelH);
					float nextTick = distance2side;
					int i = 0;
					int nextLabel = 0;
					while (nextTick < (w-distance2side)) {
						if (i == 10) {
							i = 0;
							c.drawLine(nextTick, distance2side, nextTick, grid ? h-distance2side : longTicks, fullLine);
							nextLabel = (int) (nextLabel + 10*tickDistance);
							c.drawText(Integer.toString(km ? nextLabel/1000: nextLabel), nextTick + 2*oneDP, longTicks + 2*oneDP, labelH);
						} else {
							c.drawLine(nextTick, distance2side, nextTick, shortTicks, fullLine);
						}
						i++;
						nextTick = nextTick + tickDistanceH;
					}

					nextTick = distance2side + tickDistanceH; // dont't draw first tick
					i = 1;
					nextLabel = 0;
					while (nextTick < (h-distance2side)) {
						if (i == 10) {
							i = 0;
							c.drawLine(w-distance2side, nextTick, grid ? distance2side : w-longTicks, nextTick, fullLine);
							nextLabel = (int) (nextLabel + 10*tickDistance);
							c.drawText(Integer.toString(km ? nextLabel/1000: nextLabel), w-(shortTicks+distance2side), nextTick + textHeight + oneDP, labelV);
						} else {
							c.drawLine(w-distance2side, nextTick, w-shortTicks, nextTick, fullLine);
						}
						i++;
						nextTick = nextTick + tickDistanceH;
					}
				} else { // imperial FIXME we could probably get rid of some duplicate code here
					double widthInFeet = widthInMeters * METERS2FEET;
					double feetPerPixel = widthInFeet/w;
					boolean mile = widthInFeet > MILE2FEET;
					
					double tickDistance = 0;
					int smallTickMax = 10;
					
					if (mile) { // between 1 and 12 miles use fractions
						if (widthInFeet <= 2*MILE2FEET) {
							smallTickMax = 16;
							tickDistance = MILE2FEET / smallTickMax;
						} else if (widthInFeet <= 6*MILE2FEET) {
							smallTickMax = 8;
							tickDistance = MILE2FEET / smallTickMax;
						} else if (widthInFeet <= 10*MILE2FEET) {
							smallTickMax = 4;
							tickDistance = MILE2FEET / smallTickMax;
						} else if (widthInFeet <= 50*MILE2FEET) {
							smallTickMax = 2;
							tickDistance = MILE2FEET / smallTickMax;
						} else {
							double log10 = Math.log10(widthInFeet/MILE2FEET);
							tickDistance = MILE2FEET*Math.pow(10,Math.floor(log10)-1);
						}
					} else {
						double log10 = Math.log10(widthInFeet);
						tickDistance = Math.pow(10,Math.floor(log10)-1);
						// Log.d(DEBUG_TAG,"log10 " + log10 + " tick distance " + Math.pow(10,Math.floor(log10)-1));
						if (widthInFeet/tickDistance <= 30) { // heuristic to make the visual effect a bit nicer
							tickDistance = tickDistance /10;
						}
					}
					
					float tickDistanceH = Math.round(tickDistance/feetPerPixel);
					
					c.drawText(mile ? "mile" : "ft", distance2side, longTicks + oneDP, labelH);
					float nextTick = distance2side;
					int i = 0;
					int nextLabel = 0;
					while (nextTick < (w-distance2side)) {
						if (i == smallTickMax) {
							i = 0;
							c.drawLine(nextTick, distance2side, nextTick, grid ? h-distance2side : longTicks, fullLine);
							if (mile) {
								// Log.d(DEBUG_TAG,"mile tick " + nextTick + " label " + nextLabel);
								nextLabel = (int) (nextLabel + smallTickMax*tickDistance);
								c.drawText(Integer.toString((int)(nextLabel/MILE2FEET)), nextTick + 2*oneDP, longTicks + 2*oneDP, labelH);
							} else {
								nextLabel = (int) (nextLabel + 10*tickDistance);
								c.drawText(Integer.toString((int)(nextLabel)), nextTick + 2*oneDP, longTicks + 2*oneDP, labelH);
							}
						} else {
							c.drawLine(nextTick, distance2side, nextTick, shortTicks, fullLine);
							
						}
						i++;
						nextTick = nextTick + tickDistanceH;
					}

					nextTick = distance2side + tickDistanceH; // dont't draw first tick
					i = 1;
					nextLabel = 0;
					while (nextTick < (h-distance2side)) {
						if (i == smallTickMax) {
							i = 0;
							c.drawLine(w-distance2side, nextTick, grid ? distance2side : w-longTicks, nextTick, fullLine);
							if (mile) {
								nextLabel = (int) (nextLabel + smallTickMax*tickDistance);
								c.drawText(Integer.toString((int)(nextLabel/MILE2FEET)), w-(shortTicks+distance2side), nextTick + textHeight + oneDP, labelV);
							} else {
								nextLabel = (int) (nextLabel + 10*tickDistance);
								c.drawText(Integer.toString((int)nextLabel), w-(shortTicks+distance2side), nextTick + textHeight + oneDP, labelV);
							}
						} else {
							c.drawLine(w-distance2side, nextTick, w-shortTicks, nextTick, fullLine);
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
