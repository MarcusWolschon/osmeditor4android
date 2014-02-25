package de.blau.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.GeoPoint.InterruptibleGeoPoint;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.resources.Profile;
import de.blau.android.resources.Profile.FeatureProfile;
import de.blau.android.services.TrackerService;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.OpenStreetMapOverlayTilesOverlay;
import de.blau.android.views.overlay.OpenStreetMapTilesOverlay;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;
import de.blau.android.views.util.OpenStreetMapTileServer;

/**
 * Paints all data provided previously by {@link Logic}.<br/>
 * As well as a number of overlays.
 * There is a default overlay that fetches rendered tiles
 * from an OpenStreetMap-server.
 * 
 * @author mb 
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 */

public class Map extends View implements IMapView {
	
	@SuppressWarnings("unused")
	private static final String DEBUG_TAG = Map.class.getSimpleName();

	public static final int ICON_SIZE_DP = 20;
	
	/** Use reflection to access Canvas method only available in API11. */
	private static final Method mIsHardwareAccelerated;
	
	/** half the width/height of a node icon in px */
	private final int iconRadius;
	
	private Preferences prefs;
	
	/** Direction we're pointing. 0-359 is valid, anything else is invalid.*/
	private float orientation = -1f;
	
	/**
	 * List of Overlays we are showing.<br/>
	 * This list is initialized to contain only one
	 * {@link OpenStreetMapTilesOverlay} at construction-time but
	 * can be changed to contain additional overlays later.
	 * @see #getOverlays()
	 */
	protected final List<OpenStreetMapViewOverlay> mOverlays = new ArrayList<OpenStreetMapViewOverlay>();
	
	/**
	 * The visible area in decimal-degree (WGS84) -space.
	 */
	private BoundingBox myViewBox;
	
	private StorageDelegator delegator;
	
	private boolean showIcons = false;
	/**
	 * Stores icons that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags.
	 */
	private final HashMap<Object, Bitmap> iconcache = new HashMap<Object, Bitmap>();
	
	/** Caches if the map is zoomed into edit range during one onDraw pass */
	private boolean tmpDrawingInEditRange;

	/** Caches the edit mode during one onDraw pass */
	private Logic.Mode tmpDrawingEditMode;
	
	/** Caches the currently selected node during one onDraw pass */
	private Node tmpDrawingSelectedNode;

	/** Caches the currently selected way during one onDraw pass */
	private Way tmpDrawingSelectedWay;
	
	/** Caches the current "clickable elements" set during one onDraw pass */
	private Set<OsmElement> tmpClickableElements;

	/** used for highlighting relation members */
	private Set<Way> tmpDrawingSelectedRelationWays;
	private Set<Node> tmpDrawingSelectedRelationNodes;
	
	/** Caches the preset during one onDraw pass */
	private Preset[] tmpPresets;
	
	/** Temp Rect during onDraw */
	private Rect tmpRect = new Rect();
	
	/** Caches the Paint used for node tolerance */
	Paint nodeTolerancePaint;
	
	/** Caches the Paint used for way tolerance */
	Paint wayTolerancePaint;
	
	/** cached zoom level, calculated once per onDraw pass **/
	int zoomLevel = 0;
	
	private ArrayList<Float> handles;
	
	private Location displayLocation = null;
	private boolean isFollowingGPS = false;

	private TrackerService tracker;
	
	/**
	 * support for display a crosshairs at a position
	 */
	private boolean showCrosshairs = false;
	private int crosshairsLat = 0;
	private int crosshairsLon = 0;
	
	static {
		Method m;
		try {
			m = Canvas.class.getMethod("isHardwareAccelerated", (Class[])null);
		} catch (NoSuchMethodException e) {
			m = null;
		}
		mIsHardwareAccelerated = m;
	}
	
	public Map(final Context context) {
		super(context);
		
		setFocusable(true);
		setFocusableInTouchMode(true);
		
		//Style me
		setBackgroundColor(getResources().getColor(R.color.ccc_white));
		setDrawingCacheEnabled(false);
		
		iconRadius = Math.round(ICON_SIZE_DP * context.getResources().getDisplayMetrics().density / 2.0f);
	}
	
	public void createOverlays()
	{
		// create an overlay that displays pre-rendered tiles from the internet.
		if (mOverlays.size() == 0) // only set once
		{
			if (prefs == null) // just to be safe
				mOverlays.add(new OpenStreetMapTilesOverlay(this, OpenStreetMapTileServer.getDefault(getResources(), true), null));
			else {
				// mOverlays.add(new OpenStreetMapTilesOverlay(this, OpenStreetMapTileServer.get(getResources(), prefs.backgroundLayer(), true), null));
				OpenStreetMapTilesOverlay osmto = new OpenStreetMapTilesOverlay(this, OpenStreetMapTileServer.get(Application.mainActivity, prefs.backgroundLayer(), true), null);
				// Log.d("Map","background tile renderer " + osmto.getRendererInfo().toString());
				mOverlays.add(osmto);
				mOverlays.add(new OpenStreetMapOverlayTilesOverlay(this));
			}
			mOverlays.add(new de.blau.android.osb.MapOverlay(this, prefs.getServer()));
			mOverlays.add(new de.blau.android.photos.MapOverlay(this, prefs.getServer()));
		}
	}
	
	public OpenStreetMapTilesOverlay getOpenStreetMapTilesOverlay() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if ((osmvo instanceof OpenStreetMapTilesOverlay) && !(osmvo instanceof OpenStreetMapOverlayTilesOverlay)) {
				return (OpenStreetMapTilesOverlay)osmvo;
			}
		}
		return null;
	}
	
	public de.blau.android.osb.MapOverlay getOpenStreetBugsOverlay() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo instanceof de.blau.android.osb.MapOverlay) {
				return (de.blau.android.osb.MapOverlay)osmvo;
			}
		}
		return null;
	}
	
	public de.blau.android.photos.MapOverlay getPhotosOverlay() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo instanceof de.blau.android.photos.MapOverlay) {
				return (de.blau.android.photos.MapOverlay)osmvo;
			}
		}
		return null;
	}
	
	public void onDestroy() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			osmvo.onDestroy();
		}
		tracker = null;
		iconcache.clear();
		tmpPresets = null;
	}
	
	public void onLowMemory() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			osmvo.onLowMemory();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		long time = System.currentTimeMillis();
		
		canvas.getClipBounds(tmpRect);
		zoomLevel = calcZoomLevel(tmpRect);

		tmpDrawingInEditRange = Main.logic.isInEditZoomRange();
		tmpDrawingEditMode = Main.logic.getMode();
		tmpDrawingSelectedNode = Main.logic.getSelectedNode();
		tmpDrawingSelectedWay = Main.logic.getSelectedWay();
		tmpClickableElements = Main.logic.getClickableElements();
		tmpDrawingSelectedRelationWays = Main.logic.getSelectedRelationWays();
		tmpDrawingSelectedRelationNodes = Main.logic.getSelectedRelationNodes();
		tmpPresets = Main.getCurrentPresets();
		nodeTolerancePaint = Profile.getCurrent(Profile.NODE_TOLERANCE).getPaint();
		wayTolerancePaint = Profile.getCurrent(Profile.WAY_TOLERANCE).getPaint();
		handles = null;
		
		// Draw our Overlays.
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			osmvo.onManagedDraw(canvas, this);
		}
		
		if (zoomLevel >12)
			paintOsmData(canvas);
		if (zoomLevel > 10) {
			paintStorageBox(canvas, delegator.getBoundingBoxes());
			paintGpsTrack(canvas);
		}
		paintGpsPos(canvas);
		if (tmpDrawingInEditRange)
			paintCrosshairs(canvas);
		
		time = System.currentTimeMillis() - time;
		
		if (prefs.isStatsVisible()) {
			paintStats(canvas, (int) (1 / (time / 1000f)));
		}
	}
	

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		try {
			myViewBox.setRatio((float) w / h, true);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* Overlay Event Forwarders */
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo.onTouchEvent(event, this)) {
				return true;
			}
		}
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo.onKeyDown(keyCode, event, this)) {
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo.onKeyUp(keyCode, event, this)) {
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	
	/**
	 * As of Android 4.0.4, clipping with Op.DIFFERENCE is not supported if hardware acceleration is used.
	 * (see http://android-developers.blogspot.de/2011/03/android-30-hardware-acceleration.html)
	 * Op.DIFFERENCE and clipPatch supported as of 18
	 * 
	 * @param c Canvas to check
	 * @return true if the canvas supports proper clipping with Op.DIFFERENCE
	 */
	private boolean hasFullClippingSupport(Canvas c) {
		if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 18 && mIsHardwareAccelerated != null) {
			try {
				return !(Boolean)mIsHardwareAccelerated.invoke(c, (Object[])null);
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		// Older versions do not use hardware acceleration
		return true;
	}
	
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo.onTrackballEvent(event, this)) {
				return true;
			}
		}
		return super.onTrackballEvent(event);
	}
	
	private void paintCrosshairs(Canvas canvas) {
		// 
		if (showCrosshairs) {
			Paint paint = Profile.getCurrent(Profile.CROSSHAIRS).getPaint();
			canvas.save();
			canvas.translate(GeoMath.lonE7ToX(getWidth(), getViewBox(), crosshairsLon), GeoMath.latE7ToY(getHeight(), getWidth(), getViewBox(),crosshairsLat));
			canvas.drawPath(Profile.CROSSHAIRS_PATH, paint);
			canvas.restore();
		}
	}
	
	private void paintGpsTrack(final Canvas canvas) {
		if (tracker == null) return;
		float[] linePoints = pointListToLinePointsArray(tracker.getTrackPoints());
		canvas.drawLines(linePoints, Profile.getCurrent(Profile.GPS_TRACK).getPaint());
	}
	
	/**
	 * @param canvas
	 */
	private void paintGpsPos(final Canvas canvas) {
		if (displayLocation == null) return;
		BoundingBox viewBox = getViewBox();
		float x = GeoMath.lonE7ToX(getWidth(), viewBox, (int)(displayLocation.getLongitude() * 1E7));
		float y = zoomLevel > 12 ? GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, (int)(displayLocation.getLatitude() * 1E7)) : GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, (int)(displayLocation.getLatitude() * 1E7));

		float o = -1f;
		if (displayLocation.hasBearing() && displayLocation.hasSpeed() && displayLocation.getSpeed() > 1.4f) {
			// 1.4m/s ~= 5km/h ~= walking pace
			// faster than walking pace - use the GPS bearing
			o = displayLocation.getBearing();
		} else {
			// slower than walking pace - use the compass orientation (if available)
			if (orientation >= 0) {
				o = orientation;
			}
		}
		Paint paint = null;
		if (isFollowingGPS) {
			paint = Profile.getCurrent(Profile.GPS_POS_FOLLOW).getPaint();
		} else {
			paint = Profile.getCurrent(Profile.GPS_POS).getPaint();
		}
	
		if (o < 0) {
			// no orientation data available
			canvas.drawCircle(x, y, paint.getStrokeWidth(), paint);
		} else {
			// show the orientation using a pointy indicator
			canvas.save();
			canvas.translate(x, y);
			canvas.rotate(o);
			canvas.drawPath(Profile.ORIENTATION_PATH, paint);
			canvas.restore();
		}
		if (displayLocation.hasAccuracy()) {
			try {
				BoundingBox accuracyBox = GeoMath.createBoundingBoxForCoordinates(
						displayLocation.getLatitude(), displayLocation.getLongitude(),
						displayLocation .getAccuracy());
				RectF accuracyRect = new RectF(
						GeoMath.lonE7ToX(getWidth() , viewBox, accuracyBox.getLeft()),
						GeoMath.latE7ToY(getHeight(), getWidth() , viewBox, accuracyBox.getTop()),
						GeoMath.lonE7ToX(getWidth() , viewBox, accuracyBox.getRight()),
						GeoMath.latE7ToY(getHeight(), getWidth() , viewBox, accuracyBox.getBottom()));
				canvas.drawOval(accuracyRect, Profile.getCurrent(Profile.GPS_ACCURACY).getPaint());
			} catch (OsmException e) {
				// it doesn't matter if the location accuracy doesn't get drawn
			}
		}
	}
	
	private void paintStats(final Canvas canvas, final int fps) {
		int pos = 1;
		String text = "";
		Paint infotextPaint = Profile.getCurrent(Profile.INFOTEXT).getPaint();
		float textSize = infotextPaint.getTextSize();
		
		BoundingBox viewBox = getViewBox();
		
		text = "viewBox: " + viewBox.toString();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Relations (current/API) :" + delegator.getCurrentStorage().getRelations().size() + "/"
				+ delegator.getApiRelationCount();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Ways (current/API) :" + delegator.getCurrentStorage().getWays().size() + "/"
				+ delegator.getApiWayCount();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Nodes (current/Waynodes/API) :" + delegator.getCurrentStorage().getNodes().size() + "/"
				+ delegator.getCurrentStorage().getWaynodes().size() + "/" + delegator.getApiNodeCount();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "fps: " + fps;
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
	}
	
	/**
	 * Paints all OSM data on the given canvas.
	 * 
	 * @param canvas Canvas, where the data shall be painted on.
	 */
	private void paintOsmData(final Canvas canvas) {
		//Paint all ways
		List<Way> ways = delegator.getCurrentStorage().getWays();
		for (int i = 0, size = ways.size(); i < size; ++i) {
			paintWay(canvas, ways.get(i));
		}
		
		//Paint all nodes
		List<Node> nodes = delegator.getCurrentStorage().getNodes();
		for (int i = 0, size = nodes.size(); i < size; ++i) {
			paintNode(canvas, nodes.get(i));
		}
		paintHandles(canvas);
	}
	
	private void paintStorageBox(final Canvas canvas, List<BoundingBox> list) {
		
		Canvas c = canvas;
		Bitmap b = null;
		// Clipping with Op.DIFFERENCE is not supported when a device uses hardware acceleration
		// drawing to a bitmap however will currently not be accelerated
		if (!hasFullClippingSupport(canvas)) {
			b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
			c = new Canvas(b);
		} else {
			c.save();
		}
		int screenWidth = getWidth();
		int screenHeight = getHeight();
		BoundingBox viewBox = getViewBox();
		Path path = new Path();
		RectF screen = new RectF(0, 0, getWidth(), getHeight());
		for (BoundingBox bb:list) {
			float left = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getLeft());
			float right = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getRight());
			float bottom = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox , bb.getBottom());
			float top = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, bb.getTop());
			RectF rect = new RectF(left, top, right, bottom);
			rect.intersect(screen);
			path.addRect(rect, Path.Direction.CW);
		}
	
		Paint boxpaint = Profile.getCurrent(Profile.VIEWBOX).getPaint();
		c.clipPath(path, Region.Op.DIFFERENCE);
		c.drawRect(screen, boxpaint);
			
		if (!hasFullClippingSupport(canvas)) {
			canvas.drawBitmap(b, 0, 0, null);
		} else {
			c.restore();
		}
	}
	
	/**
	 * Paints the given node on the canvas.
	 * 
	 * @param canvas Canvas, where the node shall be painted on.
	 * @param node Node which shall be painted.
	 */
	private void paintNode(final Canvas canvas, final Node node) {
		int lat = node.getLat();
		int lon = node.getLon();
		
		//Paint only nodes inside the viewBox.
		BoundingBox viewBox = getViewBox();
		if (viewBox.isIn(lat, lon)) {
			float x = GeoMath.lonE7ToX(getWidth(), viewBox, lon);
			float y = GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, lat);
			
			//draw tolerance box
			if (	tmpDrawingInEditRange
					&& (prefs.isToleranceVisible() || (tmpClickableElements != null && tmpClickableElements.contains(node)))
					&& (tmpClickableElements == null || tmpClickableElements.contains(node))
					&&	(tmpDrawingEditMode != Logic.Mode.MODE_APPEND
						|| tmpDrawingSelectedNode != null
						|| delegator.getCurrentStorage().isEndNode(node)
						)
				)
			{
				drawNodeTolerance(canvas, node.getState(), lat, lon, x, y);
			}
			
			String featureKey;
			String featureKeyThin;
			String featureKeyTagged;
			if (node == tmpDrawingSelectedNode 
					|| (tmpDrawingSelectedRelationNodes != null && tmpDrawingSelectedRelationNodes.contains(node)) 
					&& tmpDrawingInEditRange) {
				// general node style
				featureKey = Profile.SELECTED_NODE;
				// style for house numbers
				featureKeyThin = Profile.SELECTED_NODE_THIN;
				// style for tagged nodes or otherwise important
				featureKeyTagged = Profile.SELECTED_NODE_TAGGED;
				if (node == tmpDrawingSelectedNode && prefs.largeDragArea()) {
					canvas.drawCircle(x, y, Profile.getCurrent().largDragToleranceRadius, Profile.getCurrent(Profile.NODE_DRAG_RADIUS).getPaint());
				}
			} else if (node.hasProblem()) {
				// general node style
				featureKey = Profile.PROBLEM_NODE;
				// style for house numbers
				featureKeyThin = Profile.PROBLEM_NODE_THIN;
				// style for tagged nodes or otherwise important
				featureKeyTagged = Profile.PROBLEM_NODE_TAGGED;
			} else {
				// general node style
				featureKey = Profile.NODE;
				// style for house numbers
				featureKeyThin = Profile.NODE_THIN;
				// style for tagged nodes or otherwise important
				featureKeyTagged = Profile.NODE_TAGGED;
			}

			// draw house-numbers
			if (node.getTagWithKey("addr:housenumber") != null && node.getTagWithKey("addr:housenumber").trim().length() > 0) {
				Paint paint2 = Profile.getCurrent(featureKeyThin).getPaint();
				canvas.drawCircle(x, y, 10, paint2);
				String text = node.getTagWithKey("addr:housenumber");
				canvas.drawText(text, x - (paint2.measureText(text) / 2), y + 3, paint2);
				return; // don't want to be covered by icon
			} else if (node.isTagged()) {
				canvas.drawPoint(x, y, Profile.getCurrent(featureKeyTagged).getPaint());
			} else { //TODO: draw other known elements different too
				// draw regular nodes
				canvas.drawPoint(x, y, Profile.getCurrent(featureKey).getPaint());
				return; // no tags -> no icon, so we can exit here
			}
			
			if (showIcons && tmpPresets != null && !featureKey.equals(Profile.SELECTED_NODE)) paintNodeIcon(node, canvas, x, y);
		}
	}
	
	/**
	 * Paints an icon for an element. tmpPreset needs to be available (i.e. not null).
	 * @param element the element whose icon should be painted
	 * @param canvas the canvas on which to draw
	 * @param x the x position where the center of the icon goes
	 * @param y the y position where the center of the icon goes
	 */
	private void paintNodeIcon(OsmElement element, Canvas canvas, float x, float y) {
		Bitmap icon = null;
		SortedMap<String, String> tags = element.getTags();
		if (iconcache.containsKey(tags)) {
			icon = iconcache.get(tags); // may be null!
		} else if (tmpPresets != null) {
			// icon not cached, ask the preset, render to a bitmap and cache result
			PresetItem match = Preset.findBestMatch(tmpPresets,tags);
			if (match != null && match.getMapIcon() != null) {
				icon = Bitmap.createBitmap(iconRadius*2, iconRadius*2, Config.ARGB_8888);
				match.getMapIcon().draw(new Canvas(icon));
			}
			iconcache.put(tags, icon);
		}
		if (icon != null) {
			// we have an icon! draw it.
			canvas.drawBitmap(icon, x - iconRadius, y - iconRadius, null);
		}
	}

	/**
	 * @param canvas
	 * @param node
	 * @param lat
	 * @param lon
	 * @param x
	 * @param y
	 */
	private void drawNodeTolerance(final Canvas canvas, final Byte nodeState, final int lat, final int lon,
			final float x, final float y) {
		if ( (tmpDrawingEditMode != Logic.Mode.MODE_MOVE && tmpDrawingEditMode != Logic.Mode.MODE_ALIGN_BACKGROUND)
				&& (nodeState != OsmElement.STATE_UNCHANGED || delegator.isInDownload(lat, lon))) {
			canvas.drawCircle(x, y, nodeTolerancePaint.getStrokeWidth(), nodeTolerancePaint);
		}
	}

	/**
	 * Paints the given way on the canvas.
	 * 
	 * @param canvas Canvas, where the node shall be painted on.
	 * @param way way which shall be painted.
	 */
	private void paintWay(final Canvas canvas, final Way way) {
		float[] linePoints = pointListToLinePointsArray(way.getNodes());
		Paint paint;
		
		//draw way tolerance
		if (tmpDrawingInEditRange // if we are not in editing rage none of the further checks are necessary
				&& (prefs.isToleranceVisible() || (tmpClickableElements != null && tmpClickableElements.contains(way))) // if prefs are turned off but we are doing an EasyEdit operation show anyway
				&& (tmpClickableElements == null || tmpClickableElements.contains(way))
				&& (tmpDrawingEditMode == Logic.Mode.MODE_ADD 
					|| tmpDrawingEditMode == Logic.Mode.MODE_TAG_EDIT
					|| tmpDrawingEditMode == Logic.Mode.MODE_EASYEDIT
					|| (tmpDrawingEditMode == Logic.Mode.MODE_APPEND && tmpDrawingSelectedNode != null))) {
			canvas.drawLines(linePoints, wayTolerancePaint);
		}
		//draw selectedWay highlighting
		boolean isSelected = tmpDrawingInEditRange // if we are not in editing range don't show selected way ... may be a better idea to do so
				&& (way == tmpDrawingSelectedWay 
				|| (tmpDrawingSelectedRelationWays != null && tmpDrawingSelectedRelationWays.contains(way)));
		if  (isSelected) {
			paint = Profile.getCurrent(Profile.SELECTED_WAY).getPaint();
			canvas.drawLines(linePoints, paint);
			paint = Profile.getCurrent(Profile.WAY_DIRECTION).getPaint();
			drawOnewayArrows(canvas, linePoints, false, paint);
		} 

		int onewayCode = way.getOneway();
		if (onewayCode != 0) {
			FeatureProfile fp = Profile.getCurrent(Profile.ONEWAY_DIRECTION);
			drawOnewayArrows(canvas, linePoints, (onewayCode == -1), fp.getPaint());
		} else if (way.getTagWithKey("waterway") != null) { // waterways flow in the way direction
			FeatureProfile fp = Profile.getCurrent(Profile.ONEWAY_DIRECTION);
			drawOnewayArrows(canvas, linePoints, false, fp.getPaint());
		}
		
		// 
		FeatureProfile fp; // no need to get the default here
		
		// this logic needs to be separated out
		if (way.hasProblem()) {
			fp = Profile.getCurrent(Profile.PROBLEM_WAY);
		} else {
			FeatureProfile wayFp = way.getFeatureProfile();
			if (wayFp == null) {
				fp = Profile.getCurrent(Profile.WAY); // default for ways
				// three levels of hierarchy for roads and special casing of tracks, two levels for everything else
				String highwayType = way.getTagWithKey("highway");
				if (highwayType != null) {
					FeatureProfile tempFp = Profile.getCurrent("way-highway");
					if (tempFp != null) {
						fp = tempFp;
					}
					tempFp = Profile.getCurrent("way-highway-" + highwayType);
					if (tempFp != null) {
						fp = tempFp;
					}
					String highwaySubType;
					if (highwayType.equals("track")) { // special case
						highwaySubType = way.getTagWithKey("tracktype");
					} else {
						highwaySubType = way.getTagWithKey(highwayType);
					}
					if (highwaySubType != null) {
						tempFp = Profile.getCurrent("way-highway-" + highwayType + "-" + highwaySubType);
						if (tempFp != null) {
							fp = tempFp;
						}
					} 
				} else {
					// order in the array defines precedence
					String[] tags = {"building","railway","landuse","waterway","natural","addr:interpolation","boundary","amenity","shop","power",
							"aerialway","military","historic"};
					FeatureProfile tempFp = null;
					for (String tag:tags) {
						tempFp = getProfile(tag, way);
						if (tempFp != null) {
							fp = tempFp;
							break;
						}
					}
					if (tempFp == null) {
						ArrayList<Relation> relations = way.getParentRelations();
						// check for any relation memberships with low prio, take first one
						String[] relationTags = {"boundary","landuse","natural","waterway","building"};
						if (relations != null) { 
							for (Relation r : relations) {
								for (String tag:relationTags) {
									tempFp = getProfile(tag, r);
									if (tempFp != null) {
										fp = tempFp;
										break;
									} 
								}
								if (tempFp != null) { // break out of loop over relations
									break;
								}
							}
						}
					}
				}
				way.setFeatureProfile(fp);
			} else {
				fp = wayFp;
			}
		}
			
		// draw the way itself
		canvas.drawLines(linePoints, fp.getPaint());
		
		if (!isSelected) {
			// add "geometry improvement" handles
			for (int i = 2; i < linePoints.length; i=i+4) {
				float x0 = linePoints[i-2];
				float y0 = linePoints[i-1];
				float xDelta = linePoints[i] - x0;
				float yDelta = linePoints[i+1] - y0;
				
				double len = Math.hypot(xDelta,yDelta);
				if (len > Profile.getCurrent().minLenForHandle) {
					if (handles == null) handles =new ArrayList<Float>();
					handles.add(x0 + xDelta/2);
					handles.add(y0 + yDelta/2);
				}
			}
		}
	}
	

	void paintHandles(Canvas canvas) {
		if (handles != null) {
			canvas.save();
			float lastX = 0;
			float lastY = 0;
			for (int i = 0; i < handles.size(); i=i+2) {
				// draw handle
				// canvas.drawCircle(x0 + xDelta/2, y0 + yDelta/2, 5, Profile.getCurrent(Profile.HANDLE).getPaint());
				// canvas.drawPoint(x0 + xDelta/2, y0 + yDelta/2, Profile.getCurrent(Profile.HANDLE).getPaint());
				float X = handles.get(i);
				float Y = handles.get(i+1);
				canvas.translate(X-lastX, Y-lastY);
				lastX = X;
				lastY = Y;
				canvas.drawPath(Profile.X_PATH, Profile.getCurrent(Profile.HANDLE).getPaint());
			}
			canvas.restore();	
		}
	}
	
	
	FeatureProfile getProfile(String tag, OsmElement e) {
		String mainType = e.getTagWithKey(tag);
		FeatureProfile fp = null;
		if (mainType != null) {
			FeatureProfile tempFp = Profile.getCurrent("way-" + tag);
			if (tempFp != null) {
				fp = tempFp;
			}
			tempFp = Profile.getCurrent("way-" + tag + "-" + mainType);
			if (tempFp != null) {
				fp = tempFp;
			}
		}
		return fp;
	}
	/**
	 * Draws directional arrows for a way
	 * @param canvas the canvas on which to draw
	 * @param linePoints line segment array in the format returned by {@link #pointListToLinePointsArray(Iterable)}.
	 * @param reverse if true, the arrows will be painted in the reverse direction
	 * @param paint the paint to use for drawing the arrows
	 */
	private void drawOnewayArrows(Canvas canvas, float[] linePoints, boolean reverse, Paint paint) {
		int ptr = 0;
		while (ptr < linePoints.length) {
			canvas.save();
			float x1 = linePoints[ptr++];
			float y1 = linePoints[ptr++];
			float x2 = linePoints[ptr++];
			float y2 = linePoints[ptr++];

			float x = (x1+x2)/2;
			float y = (y1+y2)/2;
			canvas.translate(x,y);
			float angle = (float)(Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI);
			canvas.rotate(reverse ? angle-180 : angle);
			canvas.drawPath(Profile.WAY_DIRECTION_PATH, paint);
			canvas.restore();
		}
	}

	/**
	 * Converts a geographical way/path/track to a list of screen-coordinate points for drawing.
	 * Only segments that are inside the ViewBox are included.
	 * @param nodes An iterable (e.g. List or array) with GeoPoints of the line that should be drawn
	 *              (e.g. a Way or a GPS track)
	 * @return an array of floats in the format expected by {@link Canvas#drawLines(float[], Paint)}.
	 */
	private float[] pointListToLinePointsArray(final Iterable<? extends GeoPoint> nodes) {
		ArrayList<Float> points = new ArrayList<Float>();
		BoundingBox box = getViewBox();
		
		//loop over all nodes
		GeoPoint prevNode = null;
		float prevX=0f;
		float prevY=0f;
		for (GeoPoint node : nodes) {
			int nodeLon = node.getLon();
			int nodeLat = node.getLat();
			boolean interrupted = false;
			if (node instanceof InterruptibleGeoPoint) {
				interrupted = ((InterruptibleGeoPoint)node).isInterrupted();
			}
			float X = Float.MIN_VALUE;
			float Y = Float.MIN_VALUE;
			if (!interrupted && prevNode != null && box.intersects(nodeLat, nodeLon, prevNode.getLat(), prevNode.getLon())) {
				X = GeoMath.lonE7ToX(getWidth(), box, nodeLon);
				Y = GeoMath.latE7ToY(getHeight(), getWidth(), box, nodeLat);
				if (prevX == Float.MIN_VALUE) { // last segment didn't intersect
					prevX = GeoMath.lonE7ToX(getWidth(), box, prevNode.getLon());
					prevY = GeoMath.latE7ToY(getHeight(), getWidth(), box, prevNode.getLat());
				}
				// Line segment needs to be drawn
				points.add(prevX);
				points.add(prevY);
				points.add(X);
				points.add(Y);
			}
			prevNode = node;
			prevX = X;
			prevY = Y;
		}
		
		// convert from ArrayList<Float> to float[]
		float[] result = new float[points.size()];
		int i = 0;
		for (Float f : points) result[i++] = f;
		return result;
	}
	

	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public BoundingBox getViewBox() {
		return myViewBox;
	}
	
	/**
	 * @param aSelectedNode the currently selected node to edit.
	 */
	void setSelectedNode(final Node aSelectedNode) {
		tmpDrawingSelectedNode = aSelectedNode;
	}
	
	/**
	 * 
	 * @param aSelectedWay the currently selected way to edit.
	 */
	void setSelectedWay(final Way aSelectedWay) {
		tmpDrawingSelectedWay = aSelectedWay;
	}
	
	public Preferences getPrefs() {
		return prefs;
	}
	
	void setPrefs(final Preferences aPreference) {
		prefs = aPreference;
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo instanceof OpenStreetMapTilesOverlay && !(osmvo instanceof OpenStreetMapOverlayTilesOverlay)) {
				final OpenStreetMapTileServer backgroundTS = OpenStreetMapTileServer.get(Application.mainActivity, prefs.backgroundLayer(), true);
				((OpenStreetMapTilesOverlay)osmvo).setRendererInfo(backgroundTS);
			} else if (osmvo instanceof OpenStreetMapOverlayTilesOverlay) {
				final OpenStreetMapTileServer overlayTS = OpenStreetMapTileServer.get(Application.mainActivity, prefs.overlayLayer(), true);
				((OpenStreetMapOverlayTilesOverlay)osmvo).setRendererInfo(overlayTS);
			}
		}
		showIcons = prefs.getShowIcons();
		iconcache.clear();
	}
	
	void setOrientation(final float orientation) {
		this.orientation = orientation;
	}
	
	void setLocation(Location location) {
		displayLocation = location;
	}

	void setTracker(TrackerService tracker) {
		this.tracker = tracker;
	}

	void setDelegator(final StorageDelegator delegator) {
		this.delegator = delegator;
	}
	
	void setViewBox(final BoundingBox viewBox) {
		myViewBox = viewBox;
		try {
			myViewBox.setRatio((float) getWidth()/ getHeight(), false);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void showCrosshairs(float x, float y) {
		showCrosshairs = true;
		// store as lat lon for redraws on translation and zooming
		crosshairsLat = GeoMath.yToLatE7(getHeight(), getWidth(), getViewBox(), y);
		crosshairsLon = GeoMath.xToLonE7(getWidth() , getViewBox(), x);
	}
	
	public void hideCrosshairs() {
		showCrosshairs = false;
	}
	
	/**
	 * You can add/remove/reorder your Overlays using the List of
	 * {@link OpenStreetMapViewOverlay}. The first (index 0) Overlay gets drawn
	 * first, the one with the highest as the last one.
	 */
	public List<OpenStreetMapViewOverlay> getOverlays() {
		return mOverlays;
	}
	
	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public int getZoomLevel(final Rect viewPort) {
		return zoomLevel;
	}
	
	/**
	 * This calculates the best tile zoom level to use (not the actual zoom level of the map!)
	 * @param viewPort
	 * @return the tile zoom level
	 */
	public int calcZoomLevel(final Rect viewPort) {
		final OpenStreetMapTileServer s = getOpenStreetMapTilesOverlay().getRendererInfo();
		if (!s.isMetadataLoaded()) // protection on startup
			return 0;
		
		// Calculate lat/lon of view extents
		final double latBottom = getViewBox().getBottom() / 1E7; // GeoMath.yToLatE7(viewPort.height(), getViewBox(), viewPort.bottom) / 1E7d;
		final double lonRight  = getViewBox().getRight() / 1E7; // GeoMath.xToLonE7(viewPort.width() , getViewBox(), viewPort.right ) / 1E7d;
		final double latTop    = getViewBox().getTop() / 1E7; // GeoMath.yToLatE7(viewPort.height(), getViewBox(), viewPort.top   ) / 1E7d;
		final double lonLeft   = getViewBox().getLeft() / 1E7; // GeoMath.xToLonE7(viewPort.width() , getViewBox(), viewPort.left  ) / 1E7d;
		
		// Calculate tile x/y scaled 0.0 to 1.0
		final double xTileRight  = (lonRight + 180d) / 360d;
		final double xTileLeft   = (lonLeft  + 180d) / 360d;
		final double yTileBottom = (1d - Math.log(Math.tan(Math.toRadians(latBottom)) + 1d / Math.cos(Math.toRadians(latBottom))) / Math.PI) / 2d;
		final double yTileTop    = (1d - Math.log(Math.tan(Math.toRadians(latTop   )) + 1d / Math.cos(Math.toRadians(latTop   ))) / Math.PI) / 2d;
		
		// Calculate the ideal zoom to fit into the view
		final double xTiles = (viewPort.width()  / (xTileRight  - xTileLeft)) / s.getTileWidth();
		final double yTiles = (viewPort.height() / (yTileBottom - yTileTop )) / s.getTileHeight();
		final double xZoom = Math.log(xTiles) / Math.log(2d);
		final double yZoom = Math.log(yTiles) / Math.log(2d);
		
		// Log.d("Map","xzoom " + xZoom + " yzoom " + yZoom);
		// Zoom out to the next integer step
		int zoom = (int)Math.floor(Math.max(0, Math.min(xZoom, yZoom)));
		zoom = Math.min(zoom, s.getMaxZoomLevel());	
		
		return zoom;
	}

	public Location getLocation() {
		return displayLocation;
	}
	
	public void setFollowGPS(boolean follow) {
		isFollowingGPS = follow;
	}
}
