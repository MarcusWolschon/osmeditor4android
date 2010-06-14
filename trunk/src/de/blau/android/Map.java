package de.blau.android;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.location.Location;
import android.view.View;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track;
import de.blau.android.osm.Way;
import de.blau.android.resources.Paints;
import de.blau.android.util.GeoMath;
import de.blau.android.views.IMapView;
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
    /**
     * The {@link OpenStreetMapTileServer} we use by default.
     */
    final static OpenStreetMapTileServer DEFAULTTILESERVER = OpenStreetMapTileServer.MAPNIK;

	private Preferences pref;

	private Paints paints;

	private Track myTrack;

    /**
     * List of Overlays we are showing.<br/>
     * This list is initialized to contain only one
     * {@link OpenStreetMapTilesOverlay} at construction-time but
     * can be changed to contain additional overlays later.
     * @see #getOverlays()
     */
    protected final List<OpenStreetMapViewOverlay> mOverlays = new ArrayList<OpenStreetMapViewOverlay>();
    /**
     * One of the overlays in {@link #mOverlays} that paints tiles.
     */
    private final OpenStreetMapTilesOverlay myMapTileOverlay;

    /**
     * The visible area in decimal-degree (WGS84) -space.
     */
    private BoundingBox myViewBox;

	private StorageDelegator delegator;

	private byte mode;

	private boolean isInEditZoomRange;

	/**
	 * The currently selected node to edit.
	 */
	private Node mySelectedNode;

	/**
	 * The currently selected way to edit.
	 */
	private Way mySelectedWay;

	public Map(final Context context) {
		super(context);

		setFocusable(true);
		setFocusableInTouchMode(true);

		//Style  me
		setBackgroundColor(getResources().getColor(R.color.ccc_white));
		setDrawingCacheEnabled(false);

        // create an overlay that displays pre-rendered tiles from the internet.
        this.myMapTileOverlay = new OpenStreetMapTilesOverlay(this, DEFAULTTILESERVER, null);
        getOverlays().add(this.myMapTileOverlay);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		long time = System.currentTimeMillis();

        // Draw our Overlays.
        for (OpenStreetMapViewOverlay osmvo : this.getOverlays()) {
            osmvo.onManagedDraw(canvas, this);
        }
        
		paintOsmData(canvas);
		paintGpsTrack(canvas);
		time = System.currentTimeMillis() - time;

		if (pref.isStatsVisible()) {
			paintStats(canvas, (int) (1 / (time / 1000f)));
		}
	}

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		myViewBox.setRatio((float) w / h);
	}

	private void paintGpsTrack(final Canvas canvas) {
		Path path = new Path();
		List<Location> trackPoints = myTrack.getTrackPoints();
		int locationCount = 0;

		for (int i = 0, size = trackPoints.size(); i < size; ++i) {
			Location location = trackPoints.get(i);
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

				if (i == size - 1) {
					paintGpsPos(canvas, location, x, y);
				}
				locationCount++;
			}
		}
		canvas.drawPath(path, paints.get(Paints.TRACK));
	}

	/**
	 * @param canvas
	 * @param location
	 * @param x
	 * @param y
	 */
	private void paintGpsPos(final Canvas canvas, final Location location, final float x, final float y) {
		canvas.drawCircle(x, y, paints.get(Paints.GPS_POS).getStrokeWidth(), paints.get(Paints.GPS_POS));
		if (location.hasAccuracy()) {
            BoundingBox viewBox = getViewBox();
			int radiusGeo = GeoMath.convertMetersToGeoDistanceE7(location.getAccuracy()) + viewBox.getLeft();
			float radiusPx = Math.abs(GeoMath.lonE7ToX(getWidth(), viewBox, radiusGeo));
			canvas.drawCircle(x, y, radiusPx, paints.get(Paints.GPS_ACCURACY));
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
				canvas.save();
				canvas.clipRect(rect, Region.Op.DIFFERENCE);
				canvas.drawRect(screen, paints.get(Paints.VIEWBOX));
				canvas.restore();
			} else if (!RectF.intersects(rect, screen)) {
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
			if (mode != Logic.MODE_APPEND || mySelectedNode != null || delegator.getCurrentStorage().isEndNode(node)) {
				drawNodeTolerance(canvas, node.getState(), lat, lon, x, y);
			}
			if (node == mySelectedNode && isInEditZoomRange) {
				canvas.drawPoint(x, y, paints.get(Paints.SELECTED_NODE));
			} else {
				canvas.drawPoint(x, y, paints.get(Paints.NODE));
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
		if (pref.isToleranceVisible() && mode != Logic.MODE_MOVE && isInEditZoomRange
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
		Paint paint = new Paint(paints.get(Paints.WAY));

		//TODO: order by occurrences
		//setColorByTag(way, paint);

		paintWaySegments(nodes, path);

		//DEBUG-Setting: Set a unique color for each way
		//paint.setColor((int) Math.abs((way.getOsmId()) + 1199991) * 99991);

		//draw way tolerance
		if (pref.isToleranceVisible()
				&& (mode == Logic.MODE_ADD || mode == Logic.MODE_TAG_EDIT || (mode == Logic.MODE_APPEND && mySelectedNode != null))
				&& isInEditZoomRange) {
			canvas.drawPath(path, paints.get(Paints.WAY_TOLERANCE));
		}
		//draw selectedWay highlighting
		if (way == mySelectedWay && isInEditZoomRange) {
			canvas.drawPath(path, paints.get(Paints.SELECTED_WAY));
		}
		//draw the way itself.
		canvas.drawPath(path, paint);
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
				    //TODO: if way has oneway=true/yes/1 or highway=motorway_link, then paint arrows to show the direction od ascending nodes
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
		this.mySelectedNode = aSelectedNode;
	}

	/**
	 * 
	 * @param aSelectedWay the currently selected way to edit.
	 */
	void setSelectedWay(final Way aSelectedWay) {
		this.mySelectedWay = aSelectedWay;
	}

	void setPrefs(final Preferences aPreference) {
		this.pref = aPreference;
	}

	void setTrack(final Track aTrack) {
		this.myTrack = aTrack;
	}

	void setDelegator(final StorageDelegator delegator) {
		this.delegator = delegator;
	}

	void setViewBox(final BoundingBox viewBox) {
		this.myViewBox = viewBox;
	}

	void setMode(final byte mode) {
		this.mode = mode;
	}

	void setIsInEditZoomRange(final boolean isInEditZoomRange) {
		this.isInEditZoomRange = isInEditZoomRange;
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
        return this.mOverlays;
    }


    /**
     * convert decimal degrees to radians.
     * @param deg degrees
     * @return radiants
     */
    private double deg2rad(final double deg) {
      return (deg * Math.PI / 180d);
    }


    /**
     * ${@inheritDoc}.
     */
    @Override
    public int getZoomLevel(final Rect viewPort) {
        double latRightLower = GeoMath.yToLatE7(getHeight(), getViewBox(), viewPort.bottom) / 1E7d;
        double lonRightLower = GeoMath.xToLonE7(getWidth(),  getViewBox(), viewPort.right) / 1E7d;
        double latLeftUpper = GeoMath.yToLatE7(getHeight(),  getViewBox(), viewPort.top) / 1E7d;
        double lonLeftUpper = GeoMath.xToLonE7(getWidth(),   getViewBox(), viewPort.left) / 1E7d;
        // TODO Marcus Wolschon - guess a good zoom-level from this.getViewBox()
        
        long tilecount = Integer.MAX_VALUE;
        int zoomLevel = 17;
        for (; tilecount > 16 && zoomLevel > 0; zoomLevel--) {
            int xTileRightLower = (int) Math.floor(((lonRightLower + 180) / 360d) * Math.pow(2, zoomLevel));
            int xTileLeftUpper  = (int) Math.floor(((lonLeftUpper  + 180) / 360d) * Math.pow(2, zoomLevel));
            int yTileRightLower = (int) Math.floor((1 - Math.log(Math.tan(deg2rad(latRightLower)) + 1 / Math.cos(deg2rad(latRightLower))) / Math.PI) /2 * Math.pow(2, zoomLevel));
            int yTileLeftUpper  = (int) Math.floor((1 - Math.log(Math.tan(deg2rad(latLeftUpper )) + 1 / Math.cos(deg2rad(latLeftUpper ))) / Math.PI) /2 * Math.pow(2, zoomLevel));
            
            tilecount = (1l + Math.abs(xTileLeftUpper - xTileRightLower)) * (1l + Math.abs(yTileLeftUpper - yTileRightLower));
        }
        return zoomLevel;
    }
}
