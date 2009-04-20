package de.blau.android;

import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.location.Location;
import android.view.View;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track;
import de.blau.android.osm.Way;
import de.blau.android.osm.OsmElement.State;
import de.blau.android.resources.Paints;
import de.blau.android.util.GeoMath;

/**
 * Paints all data provided previously by {@link Logic}.
 * 
 * @author mb
 */
public class Map extends View {

	@SuppressWarnings("unused")
	private static final String DEBUG_TAG = Map.class.getSimpleName();

	private Preferences pref;

	private Paints paints;

	private Track track;

	private BoundingBox viewBox;

	private StorageDelegator delegator;

	private byte mode;

	private boolean isInEditZoomRange;

	private Node selectedNode;

	private Way selectedWay;

	public Map(final Context context) {
		super(context);

		setFocusable(true);
		setFocusableInTouchMode(true);

		//Style me
		setBackgroundColor(getResources().getColor(R.color.ccc_white));
		setDrawingCacheEnabled(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		long time = System.currentTimeMillis();
		paintOsmData(canvas);
		paintGpsTrack(canvas);
		time = System.currentTimeMillis() - time;

		if (pref.isStatsVisible()) {
			paintStats(canvas, (int) (1 / (time / 1000f)));
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		viewBox.setRatio((float) w / h);
	}

	private void paintGpsTrack(final Canvas canvas) {
		Path path = new Path();
		List<Location> trackPoints = track.getTrackPoints();
		int locationCount = 0;

		for (int i = 0, size = trackPoints.size(); i < size; ++i) {
			Location location = trackPoints.get(i);
			if (location != null) {
				int lon = (int) (location.getLongitude() * 1E7);
				int lat = (int) (location.getLatitude() * 1E7);
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

		text = "viewBox: " + viewBox.toString();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Ways (current/modified) :" + delegator.getWayCount() + "/"
				+ delegator.getModifiedWayCount();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Nodes (current/modified) :" + delegator.getNodeCount() + "/"
				+ delegator.getModifiedNodeCount();
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
		Collection<Way> ways = delegator.getWays();
		for (Way way : ways) {
			paintWay(canvas, way);
		}
			
		//Paint all nodes
		Collection<Node> nodes = delegator.getNodes();
		for (Node node : nodes) {
			paintNode(canvas, node);
		}

		paintStorageBox(canvas);
	}

	private void paintStorageBox(final Canvas canvas) {
		BoundingBox originalBox = delegator.getOriginalBox();
		int screenWidth = getWidth();
		int screenHeight = getHeight();
		float left = GeoMath.lonE7ToX(screenWidth, viewBox, originalBox.getLeft());
		float right = GeoMath.lonE7ToX(screenWidth, viewBox, originalBox.getRight());
		float bottom = GeoMath.latE7ToY(screenHeight, viewBox, originalBox.getBottom());
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
		if (viewBox.isIn(lat, lon)) {
			float x = GeoMath.lonE7ToX(getWidth(), viewBox, lon);
			float y = GeoMath.latE7ToY(getHeight(), viewBox, lat);

			//draw tolerance box
			if (mode != Logic.MODE_APPEND || selectedNode != null || delegator.isEndNode(node)) {
				drawNodeTolerance(canvas, node.getState(), lat, lon, x, y);
			}
			if (node == selectedNode && isInEditZoomRange) {
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
	private void drawNodeTolerance(final Canvas canvas, final State state, final int lat, final int lon,
			final float x, final float y) {
		if (pref.isToleranceVisible() && mode != Logic.MODE_MOVE && isInEditZoomRange
				&& (state != State.UNCHANGED || delegator.getOriginalBox().isIn(lat, lon))) {
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
				&& (mode == Logic.MODE_ADD || mode == Logic.MODE_TAG_EDIT || (mode == Logic.MODE_APPEND && selectedNode != null))
				&& isInEditZoomRange) {
			canvas.drawPath(path, paints.get(Paints.WAY_TOLERANCE));
		}
		//draw selectedWay highlighting
		if (way == selectedWay && isInEditZoomRange) {
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
		BoundingBox box = viewBox;
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
				float x = GeoMath.lonE7ToX(getWidth(), viewBox, nodeLon);
				float y = GeoMath.latE7ToY(getHeight(), viewBox, nodeLat);
				if (visibleSections == 0) {
					//first node is the beginning. Start line here.
					path.moveTo(x, y);
				} else {
					path.lineTo(x, y);
				}
				++visibleSections;
			} else if (nextNode != null && box.intersects(nodeLat, nodeLon, nextNode.getLat(), nextNode.getLon())) {
				//Just move the path to this node, no way has to be rendered outside the view.
				float x = GeoMath.lonE7ToX(getWidth(), viewBox, nodeLon);
				float y = GeoMath.latE7ToY(getHeight(), viewBox, nodeLat);

				path.moveTo(x, y);
			}
		}
		return visibleSections;
	}

	BoundingBox getViewBox() {
		return viewBox;
	}

	void setSelectedNode(final Node selectedNode) {
		this.selectedNode = selectedNode;
	}

	void setSelectedWay(final Way selectedWay) {
		this.selectedWay = selectedWay;
	}

	void setPrefs(final Preferences pref) {
		this.pref = pref;
	}

	void setTrack(final Track track) {
		this.track = track;
	}

	void setDelegator(final StorageDelegator delegator) {
		this.delegator = delegator;
	}

	void setViewBox(final BoundingBox viewBox) {
		this.viewBox = viewBox;
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
}
