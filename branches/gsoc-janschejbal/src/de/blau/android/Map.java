package de.blau.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.location.Location;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track.TrackPoint;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.Paints;
import de.blau.android.services.TrackerService;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.OpenStreetBugsOverlay;
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
	
	private Preferences pref;
	
	private Paints paints;
	
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

	private Location displayLocation = null;

	private TrackerService tracker;
	
		
	public Map(final Context context) {
		super(context);
		
		setFocusable(true);
		setFocusableInTouchMode(true);
		
		//Style me
		setBackgroundColor(getResources().getColor(R.color.ccc_white));
		setDrawingCacheEnabled(false);
		
		// create an overlay that displays pre-rendered tiles from the internet.
		mOverlays.add(new OpenStreetMapTilesOverlay(this, OpenStreetMapTileServer.getDefault(getResources()), null));
		mOverlays.add(new OpenStreetBugsOverlay(this));
	}
	
	public OpenStreetMapTilesOverlay getOpenStreetMapTilesOverlay() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo instanceof OpenStreetMapTilesOverlay) {
				return (OpenStreetMapTilesOverlay)osmvo;
			}
		}
		return null;
	}
	
	public OpenStreetBugsOverlay getOpenStreetBugsOverlay() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo instanceof OpenStreetBugsOverlay) {
				return (OpenStreetBugsOverlay)osmvo;
			}
		}
		return null;
	}
	
	public void onDestroy() {
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			osmvo.onDestroy();
		}
		this.tracker = null;
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
		
		tmpDrawingInEditRange = Main.logic.isInEditZoomRange();
		tmpDrawingEditMode = Main.logic.getMode();
		tmpDrawingSelectedNode = Main.logic.getSelectedNode();
		tmpDrawingSelectedWay = Main.logic.getSelectedWay();
		tmpClickableElements = Main.logic.getClickableElements();
				
		// Draw our Overlays.
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			osmvo.onManagedDraw(canvas, this);
		}
		
		paintOsmData(canvas);
		paintGpsTrack(canvas);
		paintGpsPos(canvas);
		time = System.currentTimeMillis() - time;
		
		if (pref.isStatsVisible()) {
			paintStats(canvas, (int) (1 / (time / 1000f)));
		}
	}
	
	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		myViewBox.setRatio((float) w / h, true);
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
	 * 
	 * @param c Canvas to check
	 * @return true if the canvas supports proper clipping with Op.DIFFERENCE
	 */
	@SuppressLint("NewApi")
	private boolean hasFullClippingSupport(Canvas c) {
		if (Build.VERSION.SDK_INT < 11) return true; // Older versions do not use hardware acceleration
		return !c.isHardwareAccelerated();
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
	
	private void paintGpsTrack(final Canvas canvas) {
		if (tracker == null) return;
		Path path = new Path();
		List<TrackPoint> trackPoints = tracker.getTrackPoints();
		int locationCount = 0;
		
		for (int i = 0, size = trackPoints.size(); i < size; ++i) {
			TrackPoint location = trackPoints.get(i);
			if (location != null) {
				int lon = (int) (location.getLongitude() * 1E7);
				int lat = (int) (location.getLatitude() * 1E7);
				
				BoundingBox viewBox = getViewBox();
				float x = GeoMath.lonE7ToX(getWidth(), viewBox, lon);
				float y = GeoMath.latE7ToY(getHeight(), viewBox, lat);
				if (locationCount == 0) {
					path.moveTo(x, y);
				} else {
					path.lineTo(x, y);
				}

				locationCount++;
			}
		}
		canvas.drawPath(path, paints.get(Paints.TRACK));
	}
	
	/**
	 * @param canvas
	 */
	private void paintGpsPos(final Canvas canvas) {
		if (displayLocation == null) return;
		BoundingBox viewBox = getViewBox();
		float x = GeoMath.lonE7ToX(getWidth(), viewBox, (int)(displayLocation.getLongitude() * 1E7));
		float y = GeoMath.latE7ToY(getHeight(), viewBox, (int)(displayLocation.getLatitude() * 1E7));

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
		if (o < 0) {
			// no orientation data available
			canvas.drawCircle(x, y, paints.get(Paints.GPS_POS).getStrokeWidth(), paints.get(Paints.GPS_POS));
		} else {
			// show the orientation using a pointy indicator
			canvas.save();
			canvas.translate(x, y);
			canvas.rotate(o);
			canvas.drawPath(Paints.ORIENTATION_PATH, paints.get(Paints.GPS_POS));
			canvas.restore();
		}
		if (displayLocation.hasAccuracy()) {
			try {
				BoundingBox accuracyBox = GeoMath.createBoundingBoxForCoordinates(
						displayLocation.getLatitude(), displayLocation.getLongitude(),
						displayLocation .getAccuracy());
				RectF accuracyRect = new RectF(
						GeoMath.lonE7ToX(getWidth() , viewBox, accuracyBox.getLeft()),
						GeoMath.latE7ToY(getHeight(), viewBox, accuracyBox.getTop()),
						GeoMath.lonE7ToX(getWidth() , viewBox, accuracyBox.getRight()),
						GeoMath.latE7ToY(getHeight(), viewBox, accuracyBox.getBottom()));
				canvas.drawOval(accuracyRect, paints.get(Paints.GPS_ACCURACY));
			} catch (OsmException e) {
				// it doesn't matter if the location accuracy doesn't get drawn
			}
		}
	}
	
	private void paintStats(final Canvas canvas, final int fps) {
		int pos = 1;
		String text = "";
		Paint infotextPaint = paints.get(Paints.INFOTEXT);
		float textSize = infotextPaint.getTextSize();
		
		BoundingBox viewBox = getViewBox();
		
		text = "viewBox: " + viewBox.toString();
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
		
		paintStorageBox(canvas);
	}
	
	private void paintStorageBox(final Canvas canvas) {
		BoundingBox originalBox = delegator.getOriginalBox();
		int screenWidth = getWidth();
		int screenHeight = getHeight();
		BoundingBox viewBox = getViewBox();
		float left = GeoMath.lonE7ToX(screenWidth, viewBox, originalBox.getLeft());
		float right = GeoMath.lonE7ToX(screenWidth, viewBox, originalBox.getRight());
		float bottom = GeoMath.latE7ToY(screenHeight, viewBox , originalBox.getBottom());
		float top = GeoMath.latE7ToY(screenHeight, viewBox, originalBox.getTop());
		RectF rect = new RectF(left, top, right, bottom);
		RectF screen = new RectF(0, 0, getWidth(), getHeight());
		if (!rect.contains(screen)) {
			if (RectF.intersects(rect, screen)) {
				// Clipping with Op.DIFFERENCE is not supported when a device uses hardware acceleration
				if (hasFullClippingSupport(canvas)) {
					canvas.save();
					canvas.clipRect(rect, Region.Op.DIFFERENCE);
					canvas.drawRect(screen, paints.get(Paints.VIEWBOX));
					canvas.restore();
				} else {
					Paint boxpaint = paints.get(Paints.VIEWBOX);
					canvas.drawRect(0, 0, screen.right, top, boxpaint); // Cover top
					canvas.drawRect(0, bottom, screen.right, screen.bottom, boxpaint); // Cover bottom
					canvas.drawRect(right, top, screen.right, bottom, boxpaint); // Cover right
					canvas.drawRect(0, top, left, bottom, boxpaint); // Cover left
				}
			} else {
				canvas.drawRect(screen, paints.get(Paints.VIEWBOX));
			}
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
			float y = GeoMath.latE7ToY(getHeight(), viewBox, lat);
			
			//draw tolerance box
			if (	(tmpClickableElements == null || tmpClickableElements.contains(node))
					&&	(tmpDrawingEditMode != Logic.Mode.MODE_APPEND
						|| tmpDrawingSelectedNode != null
						|| delegator.getCurrentStorage().isEndNode(node)
						)
				)
			{
				drawNodeTolerance(canvas, node.getState(), lat, lon, x, y);
			}
			
			int paintKey;
			int paintKey2;
			if (node == tmpDrawingSelectedNode && tmpDrawingInEditRange) {
				// general node style
				paintKey = Paints.SELECTED_NODE;
				// style for house numbers
				paintKey2 = Paints.SELECTED_NODE_THIN;
			} else if (node.hasProblem()) {
				// general node style
				paintKey = Paints.PROBLEM_NODE;
				// style for house numbers
				paintKey2 = Paints.PROBLEM_NODE_THIN;
			} else {
				// general node style
				paintKey = Paints.NODE;
				// style for house numbers
				paintKey2 = Paints.NODE_THIN;
			}

			// draw house-numbers
			if (node.getTagWithKey("addr:housenumber") != null && node.getTagWithKey("addr:housenumber").trim().length() > 0) {
				Paint paint2 = paints.get(paintKey2);
				canvas.drawCircle(x, y, 10, paint2);
				String text = node.getTagWithKey("addr:housenumber");
				canvas.drawText(text, x - (paint2.measureText(text) / 2), y + 3, paint2);
			} else { //TODO: draw other known elements different too
				// TODO js use preset icons (possibly optional?)
				// draw regular nodes
				canvas.drawPoint(x, y, paints.get(paintKey));
			}
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
		if (pref.isToleranceVisible() && tmpDrawingEditMode != Logic.Mode.MODE_MOVE && tmpDrawingInEditRange
				&& (nodeState != OsmElement.STATE_UNCHANGED || delegator.getOriginalBox().isIn(lat, lon))) {
			canvas.drawCircle(x, y, paints.get(Paints.NODE_TOLERANCE).getStrokeWidth(), paints
					.get(Paints.NODE_TOLERANCE));
		}
	}

	/**
	 * Paints the given way on the canvas.
	 * 
	 * @param canvas Canvas, where the node shall be painted on.
	 * @param way way which shall be painted.
	 */
	private void paintWay(final Canvas canvas, final Way way) {
		List<Node> nodes = way.getNodes();
		Path path = new Path();
		int paintKey = Paints.WAY;
		
		//TODO: order by occurrences
		//setColorByTag(way, paint);
		
		paintWaySegments(nodes, path);
		
		//DEBUG-Setting: Set a unique color for each way
		//paint.setColor((int) Math.abs((way.getOsmId()) + 1199991) * 99991);
		
		//draw way tolerance
		if (pref.isToleranceVisible()
				&& (tmpClickableElements == null || tmpClickableElements.contains(way))
				&& (tmpDrawingEditMode == Logic.Mode.MODE_ADD 
					|| tmpDrawingEditMode == Logic.Mode.MODE_TAG_EDIT
					|| tmpDrawingEditMode == Logic.Mode.MODE_EASYEDIT
					|| (tmpDrawingEditMode == Logic.Mode.MODE_APPEND && tmpDrawingSelectedNode != null))
				&& tmpDrawingInEditRange) {
			canvas.drawPath(path, paints.get(Paints.WAY_TOLERANCE));
		}
		//draw selectedWay highlighting
		if (way == tmpDrawingSelectedWay && tmpDrawingInEditRange) {
			canvas.drawPath(path, paints.get(Paints.SELECTED_WAY));
		}
		
		if (way.hasProblem()) {
			paintKey = Paints.PROBLEM_WAY;
		} else {
			String highway = way.getTagWithKey("highway"); // cache frequently accessed key
			if (way.getTagWithKey("railway") != null) {
				paintKey = Paints.RAILWAY;
			} else if (way.getTagWithKey("waterway") != null) {
				paintKey = Paints.WATERWAY;
			} else if (way.getTagWithKey("addr:interpolation") != null) {
				paintKey = Paints.INTERPOLATION;
			} else if (way.getTagWithKey("boundary") != null) {
				paintKey = Paints.BOUNDARY;
			} else if (highway != null) {
				if (highway.equalsIgnoreCase("footway") || highway.equalsIgnoreCase("cycleway")) {
					paintKey = Paints.FOOTWAY;
				}
			}
		}
		// draw the way itself
		canvas.drawPath(path, paints.get(paintKey));
	}
	
	/**
	 * @param nodes
	 * @param path
	 * @param box
	 * @param visibleSections
	 * @return
	 */
	private int paintWaySegments(final List<Node> nodes, final Path path) {
		BoundingBox box = getViewBox();
		int visibleSections = 0;
		//loop over all nodes
		for (int i = 0, size = nodes.size(); i < size; ++i) {
			Node node = nodes.get(i);
			int nodeLon = node.getLon();
			int nodeLat = node.getLat();
			Node prevNode = null;
			Node nextNode = null;
			
			if (i - 1 >= 0) {
				prevNode = nodes.get(i - 1);
			}
			if (i + 1 < size) {
				nextNode = nodes.get(i + 1);
			}
			
			if (prevNode == null || box.intersects(nodeLat, nodeLon, prevNode.getLat(), prevNode.getLon())) {
				float x = GeoMath.lonE7ToX(getWidth(), box, nodeLon);
				float y = GeoMath.latE7ToY(getHeight(), box, nodeLat);
				if (visibleSections == 0) {
					//first node is the beginning. Start line here.
					path.moveTo(x, y);
				} else {
					//TODO: if way has oneway=true/yes/1 or highway=motorway_link, then paint arrows to show the direction of ascending nodes
					path.lineTo(x, y);
				}
				++visibleSections;
			} else if (nextNode != null && box.intersects(nodeLat, nodeLon, nextNode.getLat(), nextNode.getLon())) {
				//Just move the path to this node, no way has to be rendered outside the view.
				float x = GeoMath.lonE7ToX(getWidth(), box, nodeLon);
				float y = GeoMath.latE7ToY(getHeight(), box, nodeLat);
				path.moveTo(x, y);
			}
		}
		return visibleSections;
	}
	
	/**
	 * ${@inheritDoc}.
	 */
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
		return pref;
	}
	
	void setPrefs(final Preferences aPreference) {
		pref = aPreference;
		for (OpenStreetMapViewOverlay osmvo : mOverlays) {
			if (osmvo instanceof OpenStreetMapTilesOverlay) {
				OpenStreetMapTilesOverlay o = (OpenStreetMapTilesOverlay)osmvo;
				o.setRendererInfo(OpenStreetMapTileServer.get(getResources(), pref.backgroundLayer()));
			}
		}
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
	}
	
	void setPaints(final Paints paints) {
		this.paints = paints;
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
		final OpenStreetMapTileServer s = getOpenStreetMapTilesOverlay().getRendererInfo();
		
		// Calculate lat/lon of view extents
		final double latBottom = GeoMath.yToLatE7(viewPort.height(), getViewBox(), viewPort.bottom) / 1E7d;
		final double lonRight  = GeoMath.xToLonE7(viewPort.width() , getViewBox(), viewPort.right ) / 1E7d;
		final double latTop    = GeoMath.yToLatE7(viewPort.height(), getViewBox(), viewPort.top   ) / 1E7d;
		final double lonLeft   = GeoMath.xToLonE7(viewPort.width() , getViewBox(), viewPort.left  ) / 1E7d;
		
		// Calculate tile x/y scaled 0.0 to 1.0
		final double xTileRight  = (lonRight + 180d) / 360d;
		final double xTileLeft   = (lonLeft  + 180d) / 360d;
		final double yTileBottom = (1d - Math.log(Math.tan(Math.toRadians(latBottom)) + 1d / Math.cos(Math.toRadians(latBottom))) / Math.PI) / 2d;
		final double yTileTop    = (1d - Math.log(Math.tan(Math.toRadians(latTop   )) + 1d / Math.cos(Math.toRadians(latTop   ))) / Math.PI) / 2d;
		
		// Calculate the ideal zoom to fit into the view
		final double xTiles = ((double)viewPort.width()  / (xTileRight  - xTileLeft)) / s.getTileWidth();
		final double yTiles = ((double)viewPort.height() / (yTileBottom - yTileTop )) / s.getTileHeight();
		final double xZoom = Math.log(xTiles) / Math.log(2d);
		final double yZoom = Math.log(yTiles) / Math.log(2d);
		
		// Zoom out to the next integer step
		int zoom = (int)Math.floor(Math.min(xZoom, yZoom));
		
		// Sanity check result
		zoom = Math.max(zoom, s.getMinZoomLevel());
		zoom = Math.min(zoom, s.getMaxZoomLevel());
		
		return zoom;
	}

	public Location getLocation() {
		return displayLocation;
	}
	
}
