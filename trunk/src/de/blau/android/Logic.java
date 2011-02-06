package de.blau.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import de.blau.android.exception.FollowGpsException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.resources.Paints;
import de.blau.android.thread.LoadFromFileThread;
import de.blau.android.thread.LoadFromStreamThread;
import de.blau.android.thread.SaveToFileThread;
import de.blau.android.thread.UploadThread;
import de.blau.android.util.GeoMath;

/**
 * Contains several responsibilities of Logic-Work:
 * <ul>
 * <li>user mode-Administration</li>
 * <li>viewBox-Administration</li>
 * <li>pushing relevant updated values to {@link Map}-Object</li>
 * <li>Handle interaction-Events</li>
 * <li>holding the {@link Tracker}-Object</li>
 * <li>holding the {@link StorageDelegator}-Object</li>
 * <li>Starting threads from thread.*</li>
 * </ul>
 * In future releases every responsibility will get outsourced.
 * 
 * @author mb
 */
public class Logic {

	@SuppressWarnings("unused")
	private static final String DEBUG_TAG = Main.class.getSimpleName();

	/**
	 * Enums for modes.
	 */
	public static final byte MODE_MOVE = 0;

	public static final byte MODE_EDIT = 1;

	public static final byte MODE_ADD = 2;

	public static final byte MODE_ERASE = 3;

	public static final byte MODE_APPEND = 4;

	public static final byte MODE_TAG_EDIT = 5;

	public static final byte MODE_SPLIT = 6;
	/**
	 * Enums for directions. Used for translation via cursor-pad.
	 */
	public static final byte DIRECTION_LEFT = 0;

	public static final byte DIRECTION_DOWN = 1;

	public static final byte DIRECTION_RIGHT = 2;

	public static final byte DIRECTION_UP = 3;

	/**
	 * Enums for zooming.
	 */
	public static final boolean ZOOM_IN = true;

	public static final boolean ZOOM_OUT = false;

	/**
	 * Minimum width of the viewBox for showing the tolerance. When the viewBox is wider, no element selection is
	 * possible.
	 */
	private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000;

	/**
	 * In MODE_EDIT this value is used for the padding of the display border.
	 */
	private static final int PADDING_ON_BORDER_TOUCH = 5;

	/**
	 * In MODE_EDIT, when the user moves a node {@link PADDING_ON_BORDER_TOUCH} pixel to a border, the map will be
	 * translated by this factor.
	 */
	private static final double BORDER_TOCH_TRANSLATION_FACTOR = 0.02;

	/**
	 * Translation factor used by cursor-pad.
	 */
	private static final float TRANSLATION_FACTOR = 0.15f;

	/**
	 * Global factor for all nodes and lines.
	 */
	public static final float STROKE_FACTOR = 100000;

	/**
	 * See {@link StorageDelegator}.
	 */
	protected final StorageDelegator delegator = new StorageDelegator();

	/**
	 * Stores the {@link Preferences} as soon as they are available.
	 */
	private Preferences prefs;

	/**
	 * The user-selected node.
	 */
	private Node selectedNode;

	/**
	 * The user-selected way.
	 */
	private Way selectedWay;

	/**
	 * Current mode.
	 */
	private byte mode;

	/**
	 * The viewBox for the map. All changes on this Object are made in here or in {@link Tracker}.
	 */
	private final BoundingBox viewBox;

	/**
	 * An instance of the map. Value set by Main via constructor.
	 */
	private Map map;

	/**
	 * Needed for updating the strokes.
	 */
	private final Paints paints;

	/**
	 * Responsible for GPS-Tracking logic.
	 */
	private final Tracker tracker;

	/**
	 * Initiate all needed values. Starts Tracker and delegate the first values for the map.
	 * 
	 * @param locationManager Needed for the Tracker. Should be instanced in Main.
	 * @param map Instance of the Map. All new Values will be pushed to it.
	 * @param paints Needed for updating the strokes on zooming.
	 */
	Logic(final LocationManager locationManager, final Map map, final Paints paints) {
		this.map = map;
		this.paints = paints;

		viewBox = delegator.getOriginalBox();
		tracker = new Tracker(locationManager, map);

		mode = MODE_MOVE;
		setSelectedNode(null);
		setSelectedWay(null);

		map.setPaints(paints);
		map.setTrack(tracker.getTrack());
		map.setDelegator(delegator);
		map.setMode(mode);
		map.setViewBox(viewBox);
	}

	/**
	 * Delegates newTrackingState to {@link tracker}.
	 * 
	 * @param newTrackingState the new Trackingstate. Enums are stored in {@link Tracker}.
	 */
	void setTrackingState(final int newTrackingState) {
		tracker.setTrackingState(newTrackingState);
	}

	void disableGpsUpdates() {
		tracker.removeUpdates();
	}

	/**
	 * Set all {@link Preferences} and delegates them to {@link Tracker} and {@link Map}. The AntiAlias-Flag will be set
	 * to {@link Paints}. Map gets repainted.
	 * 
	 * @param prefs the new Preferences.
	 */
	void setPrefs(final Preferences prefs) {
		this.prefs = prefs;
		paints.setAntiAliasing(prefs.isAntiAliasingEnabled());
		tracker.setPrefs(prefs);
		//setTrackingState(trackingState);
		map.invalidate();
	}

	/**
	 * Sets new mode. All selected Elements will be nulled. Map gets repainted.
	 * 
	 * @param mode mode.
	 */
	public void setMode(final byte mode) {
		this.mode = mode;
		map.setMode(mode);
		setSelectedNode(null);
		setSelectedWay(null);
		map.invalidate();
	}

	public byte getMode() {
		return mode;
	}

	/**
	 * Delegates follow-flag to {@link Tracker}.
	 * 
	 * @param follow true if map should follow the gps-position.
	 * @return true if map will follow the gps-position.
	 * @throws FollowGpsException When no actual GPS-Position is available.
	 */
	boolean setFollowGps(final boolean follow) throws FollowGpsException {
		return tracker.setFollowGps(follow);
	}

	/**
	 * Checks for changes in the API-Storage.
	 * 
	 * @return {@link StorageDelegator#hasChanges()}
	 */
	public boolean hasChanges() {
		return delegator.hasChanges();
	}

	/**
	 * Checks if the viewBox is close enough to the viewBox to be in the ability to edit something. Value will be set to
	 * map, too.
	 * 
	 * @return true, if viewBox' width is smaller than {@link #TOLERANCE_MIN_VIEWBOX_WIDTH}.
	 */
	public boolean isInEditZoomRange() {
		boolean isInEditZoomRange = viewBox.getWidth() < TOLERANCE_MIN_VIEWBOX_WIDTH;
		map.setIsInEditZoomRange(isInEditZoomRange);
		return isInEditZoomRange;
	}

	/**
	 * Translates the viewBox into the given direction by {@link #TRANSLATION_FACTOR} and sets GPS-Following to false.
	 * Map will be repainted.
	 * 
	 * @param direction the direction of the translation.
	 */
	public void translate(final byte direction) {
		float translation = viewBox.getWidth() * TRANSLATION_FACTOR;
		if (direction == DIRECTION_LEFT) {
			viewBox.translate((int) -translation, 0);
		} else if (direction == DIRECTION_DOWN) {
			viewBox.translate(0, (int) (-translation / viewBox.getMercatorFactorPow3()));
		} else if (direction == DIRECTION_RIGHT) {
			viewBox.translate((int) translation, 0);
		} else if (direction == DIRECTION_UP) {
			viewBox.translate(0, (int) (translation / viewBox.getMercatorFactorPow3()));
		}

		try {
			setFollowGps(false);
		} catch (FollowGpsException e) {}

		map.invalidate();
	}
	
	/**
	 * Test if the requested zoom operation can be performed.
	 * @param zoomIn The zoom operation: ZOOM_IN or ZOOM_OUT.
	 * @return true if the zoom operation can be performed, false if it can't.
	 */
	public boolean canZoom(final boolean zoomIn) {
		return zoomIn ? viewBox.canZoomIn() : viewBox.canZoomOut();
	}
	
	/**
	 * Zooms in or out. Checks if the new viewBox is close enough for editing and sends this value to map. Strokes will
	 * be updated and map will be repainted.
	 * 
	 * @param zoomIn true for zooming in.
	 */
	public void zoom(final boolean zoomIn) {
		if (zoomIn) {
			viewBox.zoomIn();
		} else {
			viewBox.zoomOut();
		}
		isInEditZoomRange();
		paints.updateStrokes((STROKE_FACTOR / viewBox.getWidth()));
		map.invalidate();
	}

	/**
	 * Delegates the inserting of the Tag-list to {@link StorageDelegator}.
	 * 
	 * @param type type of the element for the Tag-list.
	 * @param osmId OSM-ID of the element.
	 * @param tags Tag-List to be set.
	 * @return false if no element exists for the given osmId/type.
	 */
	boolean insertTags(final String type, final long osmId, final java.util.Map<String, String> tags) {
		OsmElement osmElement = delegator.getOsmElement(type, osmId);

		if (osmElement == null) {
			return false;
		} else {
			delegator.insertTags(osmElement, tags);
			return true;
		}
	}

	/**
	 * Prepares the screen for an empty map. Strokes will be updated and map will be repainted.
	 * 
	 * @param box the new empty map-box. Don't mess up with the viewBox!
	 */
	void newEmptyMap(final BoundingBox box) {
		delegator.reset();
		delegator.setOriginalBox(box);
		viewBox.setRatio((float) map.getWidth() / map.getHeight());
		paints.updateStrokes((STROKE_FACTOR / viewBox.getWidth()));
		map.invalidate();
	}

	/**
	 * Searches for all Ways and Nodes at x,y plus the shown node-tolerance. Nodes have to lie in the mapBox. For
	 * optimization reasons the tolerance will be handled as square, not circle.
	 * 
	 * @param x display-coordinate.
	 * @param y display-coordinate.
	 * @return a List of all OsmElements (Nodes and Ways) within the tolerance
	 */
	public List<OsmElement> getClickedNodesAndWays(final float x, final float y) {
		ArrayList<OsmElement> result = new ArrayList<OsmElement>();

		for (Way way : delegator.getCurrentStorage().getWays()) {
			List<Node> wayNodes = way.getNodes();

			//Iterate over all WayNodes, but not the last one.
			for (int k = 0, wayNodesSize = wayNodes.size(); k < wayNodesSize - 1; ++k) {
				Node node1 = wayNodes.get(k);
				Node node2 = wayNodes.get(k + 1);
				float node1X = GeoMath.lonE7ToX(map.getWidth(), viewBox, node1.getLon());
				float node1Y = GeoMath.latE7ToY(map.getHeight(), viewBox, node1.getLat());
				float node2X = GeoMath.lonE7ToX(map.getWidth(), viewBox, node2.getLon());
				float node2Y = GeoMath.latE7ToY(map.getHeight(), viewBox, node2.getLat());

				if (isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y)) {
					result.add(way);
					break;
				}
			}
		}

		result.addAll(getClickedNodes(x, y));

		return result;
	}

	public List<OsmElement> getClickedNodes(final float x, final float y) {
		List<OsmElement> result = new ArrayList<OsmElement>();
		
		final float tolerance = Paints.NODE_TOLERANCE_VALUE;

		List<Node> nodes = delegator.getCurrentStorage().getNodes();
		for (Node node : nodes) {
			int lat = node.getLat();
			int lon = node.getLon();
			if (node.getState() != OsmElement.STATE_UNCHANGED || delegator.getOriginalBox().isIn(lat, lon)) {
				float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
				float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), viewBox, lat) - y);
				if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
					if (Math.sqrt(Math.pow(differenceX, 2) + Math.pow(differenceY, 2)) <= tolerance) {
						result.add(node);
					}
				}
			}
		}

		return result;
	}

	public List<OsmElement> getClickedEndNodes(final float x, final float y) {
		List<OsmElement> result = new ArrayList<OsmElement>();
		List<OsmElement> allNodes = getClickedNodes(x, y);

		for (OsmElement osmElement : allNodes) {
			if (delegator.getCurrentStorage().isEndNode((Node) osmElement))
				result.add(osmElement);
		}

		return result;
	}

	/**
	 * Searches for a Node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox. For optimization
	 * reasons the tolerance will be handled as square, not circle.
	 * 
	 * @param x display-coordinate.
	 * @param y display-coordinate.
	 * @return the first node found in the current-Storage node-list. null, when no node was found.
	 */
	private Node getClickedNode(final float x, final float y) {
		List<Node> nodes = delegator.getCurrentStorage().getNodes();
		float tolerance = Paints.NODE_TOLERANCE_VALUE;

		//An existing node was selected
		for (int i = 0, nodesSize = nodes.size(); i < nodesSize; ++i) {
			Node node = nodes.get(i);
			int lat = node.getLat();
			int lon = node.getLon();
			if (node.getState() != OsmElement.STATE_UNCHANGED || delegator.getOriginalBox().isIn(lat, lon)) {
				float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
				float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), viewBox, lat) - y);
				if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
					if (Math.sqrt(Math.pow(differenceX, 2) + Math.pow(differenceY, 2)) <= tolerance) {
						return node;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Handles the event when user begins to touch the display. When the viewBox is close enough for editing and the
	 * user is in edit-mode a touched node will bet set to selected. A eventual movement of this node will be done in
	 * {@link #handleTouchEventMove(float, float, float, float, boolean)}.
	 * 
	 * @param x display-coord.
	 * @param y display-coord.
	 */
	void handleTouchEventDown(final float x, final float y) {
		if (isInEditZoomRange() && mode == MODE_EDIT) {
			// TODO Need to handle multiple possible targets here too (Issue #6)
			setSelectedNode(getClickedNode(x, y));
			map.invalidate();
		}
	}

	/**
	 * Handles a finger-movement on the touchscreen. Moves a node when it's in Edit-Zoom-Range, a node was previously
	 * selected and the user is in edit-mode. Otherwise the movement will be interpreted as map-translation. Map will be
	 * repainted.
	 * 
	 * @param absoluteX The absolute display-coordinate.
	 * @param absoluteY The absolute display-coordinate.
	 * @param relativeX The difference to the last absolute display-coordinate.
	 * @param relativeY The difference to the last absolute display-coordinate.
	 * @param hasMoved indicates if the user has made only a click or a real movement. A Node should not be translated
	 *            only because of a single click.
	 */
	void handleTouchEventMove(final float absoluteX, final float absoluteY, final float relativeX,
			final float relativeY, final boolean hasMoved) {
		if (mode == MODE_EDIT && selectedNode != null && isInEditZoomRange()) {
			if (hasMoved) {
				int lat = GeoMath.yToLatE7(map.getHeight(), viewBox, absoluteY);
				int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, absoluteX);
				delegator.updateLatLon(selectedNode, lat, lon);
				translateOnBorderTouch(absoluteX, absoluteY);
				map.invalidate();
			}
		} else {
			performTranslation(relativeX, relativeY);
			map.invalidate();
		}
	}

	/**
	 * Converts screen-coords to gps-coords and delegates translation to {@link BoundingBox#translate(int, int)}.
	 * GPS-Following will be disabled.
	 * 
	 * @param screenTransX Movement on the screen.
	 * @param screenTransY Movement on the screen.
	 */
	private void performTranslation(final float screenTransX, final float screenTransY) {
		try {
			tracker.setFollowGps(false);
		} catch (FollowGpsException e) {}
		int height = map.getHeight();
		int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, screenTransX);
		int lat = GeoMath.yToLatE7(height, viewBox, height - screenTransY);
		int relativeLon = lon - viewBox.getLeft();
		int relativeLat = lat - viewBox.getBottom();

		viewBox.translate(relativeLon, relativeLat);
	}

	/**
	 * Executes an add-command for x,y. Adds new nodes and ways to storage. When more than one Node were
	 * created/selected then a new way will be created.
	 * 
	 * @param x screen-coordinate
	 * @param y screen-coordinate
	 */
	public void performAdd(final float x, final float y) {
		Node nextNode;
		Node lSelectedNode = selectedNode;
		Way lSelectedWay = selectedWay;

		if (lSelectedNode == null) {
			//This will be the first node.
			lSelectedNode = getClickedNodeOrCreatedWayNode(x, y);
			if (lSelectedNode == null) {
				//A complete new Node...
				int lat = GeoMath.yToLatE7(map.getHeight(), viewBox, y);
				int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, x);
				lSelectedNode = OsmElementFactory.createNodeWithNewId(lat, lon);
				delegator.insertElementSafe(lSelectedNode);
			}
		} else {
			//this is not the first node
			nextNode = getClickedNodeOrCreatedWayNode(x, y);
			if (nextNode == null) {
				//clicked on empty space -> create a new Node
				if (lSelectedWay == null) {
					//This is the second Node, so we create a new Way and add the previous selected node to this way
					lSelectedWay = delegator.createAndInsertWay(lSelectedNode);
				}
				int lat = GeoMath.yToLatE7(map.getHeight(), viewBox, y);
				int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, x);
				lSelectedNode = OsmElementFactory.createNodeWithNewId(lat, lon);
				delegator.addNodeToWay(lSelectedNode, lSelectedWay);
				delegator.insertElementSafe(lSelectedNode);
			} else {
				//User clicks an existing Node
				if (nextNode == lSelectedNode) {
					//User clicks the last Node -> end here with adding
					lSelectedNode = null;
					lSelectedWay = null;
				} else {
					//Create a new way with the existing node, which was clicked.
					if (lSelectedWay == null) {
						lSelectedWay = delegator.createAndInsertWay(lSelectedNode);
					}
					//Add the new Node.
					delegator.addNodeToWay(nextNode, lSelectedWay);
					lSelectedNode = nextNode;
				}
			}
		}
		setSelectedNode(lSelectedNode);
		setSelectedWay(lSelectedWay);
	}

	/**
	 * Catches the first node at the given position and delegates the deletion to {@link #delegator}.
	 * 
	 * @param x screen-coordinate.
	 * @param y screen-coordinate.
	 */
	public void performErase(final Node node) {
		if (node != null) {
			delegator.removeNode(node);
			map.invalidate();
		}
	}

	/**
	 * Catches the first node at the given position and delegates the deletion to {@link #delegator}.
	 * 
	 * @param x screen-coordinate.
	 * @param y screen-coordinate.
	 */
	public void performSplit(final Node node) {
		if (node != null) {
			delegator.splitAtNode(node);
			map.invalidate();
		}
	}
	

	public void performAppendStart(OsmElement element) {
		Way lSelectedWay = null;
		Node lSelectedNode = null;

		if (element != null) {
			if (element instanceof Node) {
				lSelectedNode = (Node) element;
				List<Way> ways = delegator.getCurrentStorage().getWays(lSelectedNode);
				// TODO Resolve possible multiple ways that end at the node
				for (Way way : ways) {
					if (way.isEndNode(lSelectedNode)) {
						lSelectedWay = way;
						break;
					}
				}
				if (lSelectedWay == null) {
					lSelectedNode = null;
				}
			}
		}
		setSelectedNode(lSelectedNode);
		setSelectedWay(lSelectedWay);

		map.invalidate();
	}

	public void performAppendAppend(final float x, final float y) {
		Node lSelectedNode = getSelectedNode();
		Way lSelectedWay = getSelectedWay();

		Node node = getClickedNodeOrCreatedWayNode(x, y);
		if (node == lSelectedNode) {
			lSelectedNode = null;
			lSelectedWay = null;
		} else {
			if (node == null) {
				int lat = GeoMath.yToLatE7(map.getHeight(), viewBox, y);
				int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, x);
				node = OsmElementFactory.createNodeWithNewId(lat, lon);
				delegator.insertElementSafe(node);
			}
			delegator.appendNodeToWay(lSelectedNode, node, lSelectedWay);
			lSelectedNode = node;
		}
		setSelectedNode(lSelectedNode);
		setSelectedWay(lSelectedWay);
	}

	/**
	 * Tries to locate the selected node. If x,y lays on a way, a new node at this location will be created, stored in
	 * storage and returned.
	 * 
	 * @param x the x screen coordinate
	 * @param y the y screen coordinate
	 * @return the selected node or the created node, if x,y lays on a way. Null if any node or way was selected.
	 */
	private Node getClickedNodeOrCreatedWayNode(final float x, final float y) {
		List<Way> ways = delegator.getCurrentStorage().getWays();

		Node node = getClickedNode(x, y);
		if (node != null) {
			return node;
		}

		//create a new node on a way
		for (int i = 0, waysSize = ways.size(); i < waysSize; ++i) {
			Way way = ways.get(i);
			List<Node> wayNodes = way.getNodes();

			for (int k = 0, wayNodesSize = wayNodes.size(); k < wayNodesSize - 1; ++k) {
				Node nodeBefore = wayNodes.get(k);
				node = createNodeOnWay(nodeBefore, wayNodes.get(k + 1), x, y);
				if (node != null) {
					delegator.insertElementSafe(node);
					delegator.addNodeToWayAfter(nodeBefore, node, way);
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * Creates a new node at x,y between node1 and node2. When x,y does not lay on the line between node1 and node2 it
	 * will return null.
	 * 
	 * @param node1 the first node
	 * @param node2 the second node
	 * @param x screen coordinate where the new node shall be created.
	 * @param y screen coordinate where the new node shall be created.
	 * @return a new created node at lon/lat corresponding to x,y. When x,y does not lay on the line between node1 and
	 *         node2 it will return null.
	 */
	private Node createNodeOnWay(final Node node1, final Node node2, final float x, final float y) {
		//Nodes have to be converted to screen-coordinates, due to a better tolerance-check.
		float node1X = GeoMath.lonE7ToX(map.getWidth(), viewBox, node1.getLon());
		float node1Y = GeoMath.latE7ToY(map.getHeight(), viewBox, node1.getLat());
		float node2X = GeoMath.lonE7ToX(map.getWidth(), viewBox, node2.getLon());
		float node2Y = GeoMath.latE7ToY(map.getHeight(), viewBox, node2.getLat());

		//At first, we check if the x,y is in the bounding box clamping by node1 and node2.
		if (isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y)) {
			int lat = GeoMath.yToLatE7(map.getHeight(), viewBox, y);
			int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, x);
			return OsmElementFactory.createNodeWithNewId(lat, lon);
		}
		return null;
	}

	/**
	 * Checks if the x,y-position plus the tolerance is on a line between node1(x,y) and node2(x,y).
	 * 
	 * @return true, when x,y plus way-tolerance lays on the line between node1 and node2.
	 */
	private boolean isPositionOnLine(final float x, final float y,
			final float node1X, final float node1Y,
			final float node2X, final float node2Y) {
		float tolerance = Paints.WAY_TOLERANCE_VALUE / 2f;
		if (GeoMath.isBetween(x, node1X, node2X, tolerance) && GeoMath.isBetween(y, node1Y, node2Y, tolerance)) {
			// equation (14) on http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
			float distance = (float) (Math.abs((node2X - node1X) * (node1Y - y) - (node1X - x) * (node2Y - node1Y)) /
			                 Math.sqrt(Math.pow(node2X - node1X, 2.0) + Math.pow(node2Y - node1Y, 2.0)));
			return (distance < tolerance);
		}
		return false;
	}

	/**
	 * Translates the {@link #viewBox} in the direction of x/y's next border.
	 * 
	 * @param x screen-coordinate
	 * @param y screen-coordinate
	 */
	public void translateOnBorderTouch(final float x, final float y) {
		int translationOnBorderTouch = (int) (viewBox.getWidth() * BORDER_TOCH_TRANSLATION_FACTOR);

		if (x > map.getWidth() - PADDING_ON_BORDER_TOUCH) {
			viewBox.translate(translationOnBorderTouch, 0);
		} else if (x < PADDING_ON_BORDER_TOUCH) {
			viewBox.translate(-translationOnBorderTouch, 0);
		}

		if (y > map.getHeight() - PADDING_ON_BORDER_TOUCH) {
			viewBox.translate(0, -translationOnBorderTouch);
		} else if (y < PADDING_ON_BORDER_TOUCH) {
			viewBox.translate(0, translationOnBorderTouch);
		}
	}

	/**
	 * Loads the area defined by mapBox from the OSM-Server. Afterwards the {@link LoadFromStreamThread} will be
	 * instanced and started.
	 * 
	 * @param caller Reference to the caller-activity.
	 * @param handler Handler generated in the UI-Thread.
	 * @param mapBox Box defining the area to be loaded.
	 * @throws OsmServerException When a problem with the osm-server occurs.
	 * @throws IOException general IOException
	 */
	void downloadBox(final Activity caller, final Handler handler, final BoundingBox mapBox) throws OsmServerException,
			IOException {
		InputStream in = prefs.getServer().getStreamForBox(mapBox);
		paints.updateStrokes((STROKE_FACTOR / mapBox.getWidth()));
		new LoadFromStreamThread(caller, handler, delegator, in, mapBox, viewBox).start();
	}

	/**
	 * @see #downloadBox(Activity, Handler, BoundingBox)
	 */
	void downloadCurrent(final Activity caller, final Handler handler) throws OsmServerException, IOException {
		downloadBox(caller, handler, viewBox);
	}

	/**
	 * Starts a new {@link SaveToFileThread}.
	 * 
	 * @param caller Reference to the caller-activity.
	 * @param handler Handler generated in the UI-Thread.
	 * @param showDone when true, a Toast will be shown when the file was saved.
	 */
	void save(final Activity caller, final Handler handler, final boolean showDone) {
		new SaveToFileThread(caller, handler, delegator, showDone).start();
	}

	/**
	 * Starts a new {@link LoadFromFileThread}.
	 * 
	 * @param caller Reference to the caller-activity.
	 * @param handler Handler generated in the UI-Thread.
	 */
	void loadFromFile(final Activity caller, final Handler handler) {
		new LoadFromFileThread(caller, handler, delegator, viewBox, paints).start();
	}

	/**
	 * Starts a new {@link UploadThread}.
	 * 
	 * @param caller Reference to the caller-activity.
	 * @param handler Handler generated in the UI-Thread.
	 * @param comment Changeset comment.
	 */
	public void upload(final Activity caller, final Handler handler, final String comment) {
		Server server = prefs.getServer();
		new UploadThread(caller, handler, server, delegator, comment).start();
	}

	/**
	 * Internal setter to a) set the internal value and b) push the value to {@link #map}.
	 */
	public void setSelectedNode(final Node selectedNode) {
		this.selectedNode = selectedNode;
		map.setSelectedNode(selectedNode);
	}

	/**
	 * Internal setter to a) set the internal value and b) push the value to {@link #map}.
	 */
	public void setSelectedWay(final Way selectedWay) {
		this.selectedWay = selectedWay;
		map.setSelectedWay(selectedWay);
	}

	/**
	 * @return the selectedNode
	 */
	public final Node getSelectedNode() {
		return selectedNode;
	}

	/**
	 * @return the selectedWay
	 */
	public final Way getSelectedWay() {
		return selectedWay;
	}
	
	/**
	 * Will be called when the screen orientation was changed.
	 * 
	 * @param map the new Map-Instance. Be aware: The View-dimensions are not yet set...
	 */
	public void setMap(Map map) {
		this.map = map;
		paints.updateStrokes(Math.min(prefs.getMaxStrokeWidth(), STROKE_FACTOR / viewBox.getWidth()));
		map.setPaints(paints);
		map.setTrack(tracker.getTrack());
		map.setDelegator(delegator);
		map.setMode(mode);
		map.setViewBox(viewBox);
	}
	
	/**
	 * @return a list of all pending changes to upload
	 */
	public Set<String> getPendingChanges(final Context aCaller) {
		return delegator.listChanges(aCaller.getResources());
	}
	
}
