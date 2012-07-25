package de.blau.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpStatus;
import org.xml.sax.SAXException;

import android.content.Context;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import de.blau.android.exception.FollowGpsException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osb.Bug;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Track;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.Paints;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;

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

	private static final String DEBUG_TAG = Main.class.getSimpleName();

	/**
	 * Enums for modes.
	 */
	public static enum Mode {
		/**
		 * move nodes by tapping the screen
		 */
		MODE_MOVE,
		/**
		 * edit ways and nodes by tapping the screen
		 */
		MODE_EDIT,
		/**
		 * add nodes by tapping the screen
		 */
		MODE_ADD,
		/**
		 * erase ways and nodes by tapping the screen
		 */
		MODE_ERASE,
		/**
		 * append nodes to the end of a way by tapping the screen
		 */
		MODE_APPEND,
		/**
		 * edit tags of ways and nodes by tapping the screen
		 */
		MODE_TAG_EDIT,
		/**
		 * split ways by tapping the screen
		 */
		MODE_SPLIT,
		/**
		 * file bug in OpenStreetBugs by tapping the screen
		 */
		MODE_OPENSTREETBUG,
		/**
		 * easy editing mode supporting multiple operations and menu-based tagging
		 */
		MODE_EASYEDIT
	}

	/**
	 * Enums for directions. Used for translation via cursor-pad.
	 */
	public static enum CursorPaddirection {
		DIRECTION_LEFT,
		DIRECTION_DOWN,
		DIRECTION_RIGHT,
		DIRECTION_UP
	}

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
	 * Filename of file containing the currently selected edit mode
	 */
	private static final String MODE_FILENAME = "editmode.state";
	
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
	 * The user-selected bug.
	 */
	private Bug selectedBug;

	/**
	 * Are we currently dragging a node?
	 * Set by {@link #handleTouchEventDown(float, float)}
	 */
	private boolean draggingNode = false;

	/**
	 * Current mode.
	 */
	private Mode mode;

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

	private Set<OsmElement> clickableElements;

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

		mode = Mode.MODE_MOVE;
		setSelectedBug(null);
		setSelectedNode(null);
		setSelectedWay(null);

		map.setPaints(paints);
		map.setDelegator(delegator);
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
	 * Sets new mode.
	 * If the new mode is different from the current one,
	 * all selected Elements will be nulled, the Map gets repainted,
	 * and the action bar will be reset.
	 * 
	 * @param mode mode.
	 */
	public void setMode(final Mode mode) {
		if (this.mode == mode) return;
		this.mode = mode;
		Main.onEditModeChanged();
		setSelectedBug(null);
		setSelectedNode(null);
		setSelectedWay(null);
		map.invalidate();
	}

	public Mode getMode() {
		return mode;
	}
	
	public Track getTrack() {
		return tracker.getTrack();
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
	 * Checks if the viewBox is close enough to the viewBox to be in the ability to edit something.
	 * 
	 * @return true, if viewBox' width is smaller than {@link #TOLERANCE_MIN_VIEWBOX_WIDTH}.
	 */
	public boolean isInEditZoomRange() {
		return viewBox.getWidth() < TOLERANCE_MIN_VIEWBOX_WIDTH;
	}

	/**
	 * Translates the viewBox into the given direction by {@link #TRANSLATION_FACTOR} and sets GPS-Following to false.
	 * Map will be repainted.
	 * 
	 * @param direction the direction of the translation.
	 */
	public void translate(final CursorPaddirection direction) {
		float translation = viewBox.getWidth() * TRANSLATION_FACTOR;
		switch (direction) {
		case DIRECTION_LEFT:
			viewBox.translate((int) -translation, 0);
			break;
		case DIRECTION_DOWN:
			viewBox.translate(0, (int) (-translation / viewBox.getMercatorFactorPow3()));
			break;
		case DIRECTION_RIGHT:
			viewBox.translate((int) translation, 0);
			break;
		case DIRECTION_UP:
			viewBox.translate(0, (int) (translation / viewBox.getMercatorFactorPow3()));
			break;
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
	
	public void zoom(final float zoomFactor) {
		viewBox.zoom(zoomFactor);
		isInEditZoomRange();
		paints.updateStrokes((STROKE_FACTOR / viewBox.getWidth()));
		map.postInvalidate();
	}

	/**
	 * Delegates the setting of the Tag-list to {@link StorageDelegator}.
	 * All existing tags will be replaced.
	 * 
	 * @param type type of the element for the Tag-list.
	 * @param osmId OSM-ID of the element.
	 * @param tags Tag-List to be set.
	 * @return false if no element exists for the given osmId/type.
	 */
	public boolean setTags(final String type, final long osmId, final java.util.Map<String, String> tags) {
		OsmElement osmElement = delegator.getOsmElement(type, osmId);

		if (osmElement == null) {
			return false;
		} else {
			delegator.setTags(osmElement, tags);
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
	 * Searches for all Ways and Nodes at x,y plus the shown node-tolerance. Nodes have to lie in the mapBox.
	 * 
	 * @param x display-coordinate.
	 * @param y display-coordinate.
	 * @return a List of all OsmElements (Nodes and Ways) within the tolerance
	 */
	public List<OsmElement> getClickedNodesAndWays(final float x, final float y) {
		ArrayList<OsmElement> result = new ArrayList<OsmElement>();
		result.addAll(getClickedNodes(x, y));
		result.addAll(getClickedWays(x, y));
		return result;
	}

	/**
	 * Returns all ways within way tolerance from the given coordinates, and their distances from them.
	 * @param x x display coordinate
	 * @param y y display coordinate
	 * @return a hash map mapping Ways to distances
	 */
	public HashMap<Way, Double> getClickedWaysWithDistances(final float x, final float y) {
		HashMap<Way, Double> result = new HashMap<Way, Double>();

		for (Way way : delegator.getCurrentStorage().getWays()) {
			List<Node> wayNodes = way.getNodes();

			if (clickableElements != null && !clickableElements.contains(way)) continue;

			//Iterate over all WayNodes, but not the last one.
			for (int k = 0, wayNodesSize = wayNodes.size(); k < wayNodesSize - 1; ++k) {
				Node node1 = wayNodes.get(k);
				Node node2 = wayNodes.get(k + 1);
				float node1X = GeoMath.lonE7ToX(map.getWidth(), viewBox, node1.getLon());
				float node1Y = GeoMath.latE7ToY(map.getHeight(), viewBox, node1.getLat());
				float node2X = GeoMath.lonE7ToX(map.getWidth(), viewBox, node2.getLon());
				float node2Y = GeoMath.latE7ToY(map.getHeight(), viewBox, node2.getLat());

				if (isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y)) {
					result.put(way, GeoMath.getLineDistance(x, y, node1X, node1Y, node2X, node2Y));
					break;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Returns all nodes within node tolerance from the given coordinates, and their distances from them.
	 * @param x x display coordinate
	 * @param y y display coordinate
	 * @return a hash map mapping Nodes to distances
	 */
	public HashMap<Node, Double> getClickedNodesWithDistances(final float x, final float y) {
		HashMap<Node, Double> result = new HashMap<Node, Double>();
		List<Node> nodes = delegator.getCurrentStorage().getNodes();

		final float tolerance = Paints.NODE_TOLERANCE_VALUE;
		
		for (Node node : nodes) {
			int lat = node.getLat();
			int lon = node.getLon();
			if (clickableElements != null && !clickableElements.contains(node)) continue;
			if (node.getState() != OsmElement.STATE_UNCHANGED || delegator.getOriginalBox().isIn(lat, lon)) {
				float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
				float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), viewBox, lat) - y);
				if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
					double dist = Math.sqrt(Math.pow(differenceX, 2) + Math.pow(differenceY, 2));
					if (dist <= tolerance) {
						result.put(node, dist);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Searches for a Node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox.
	 * 
	 * @param x display-coordinate.
	 * @param y display-coordinate.
	 * @return all nodes within tolerance found in the currentStorage node-list. null, when no node was found.
	 */
	public List<OsmElement> getClickedNodes(final float x, final float y) {
		return new ArrayList<OsmElement>(getClickedNodesWithDistances(x, y).keySet());
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
	 * Searches for a Node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox.
	 * 
	 * @param x display-coordinate.
	 * @param y display-coordinate.
	 * @return the nearest node found in the current-Storage node-list. null, when no node was found.
	 */
	public Node getClickedNode(final float x, final float y) {
		Node bestNode = null;
		Double bestDistance = Double.MAX_VALUE;
		HashMap<Node, Double> candidates = getClickedNodesWithDistances(x, y);
		for (Entry<Node, Double> candidate : candidates.entrySet()) {
			if (candidate.getValue() < bestDistance) {
				bestNode = candidate.getKey();
				bestDistance = candidate.getValue();
			}
		}
		return bestNode;
	}
	

	/**
	 * Returns all ways within click tolerance from the given coordinate 
	 * @param x x display-coordinate.
	 * @param y y display-coordinate.
	 * @return the ways
	 */
	public List<Way> getClickedWays(final float x, final float y) {
		return new ArrayList<Way>(getClickedWaysWithDistances(x, y).keySet());
	}
	
	/**
	 * Returns the closest way (within tolerance) to the given coordinates
	 * @param x the x display-coordinate.
	 * @param y the y display-coordinate.
	 * @return the closest way, or null if no way is found within the tolerance
	 */
	public Way getClickedWay(final float x, final float y) {
		Way bestWay = null;
		Double bestDistance = Double.MAX_VALUE;
		HashMap<Way, Double> candidates = getClickedWaysWithDistances(x, y);
		for (Entry<Way, Double> candidate : candidates.entrySet()) {
			if (candidate.getValue() < bestDistance) {
				bestWay = candidate.getKey();
				bestDistance = candidate.getValue();
			}
		}
		return bestWay;
	}
	
	public List<Way> getWaysForNode(Node node) {
		return delegator.getCurrentStorage().getWays(node);
	}

	/**
	 * Handles the event when user begins to touch the display. When the viewBox is close enough for editing and the
	 * user is in edit-mode a touched node will bet set to selected. draggingNode will be set if a node is to be moved.
	 * A eventual movement of this node will be done in {@link #handleTouchEventMove(float, float, float, float, boolean)}.
	 * 
	 * @param x display-coord.
	 * @param y display-coord.
	 */
	void handleTouchEventDown(final float x, final float y) {
		if (isInEditZoomRange() && mode == Mode.MODE_EDIT) {
			// TODO Need to handle multiple possible targets here too (Issue #6)
			setSelectedNode(getClickedNode(x, y));
			map.invalidate();
			draggingNode = (selectedNode != null);
		} else if (isInEditZoomRange() && mode == Mode.MODE_EASYEDIT) {
			draggingNode = (selectedNode != null && getClickedNodes(x, y).contains(selectedNode));
		} else {
			draggingNode = false;
		}
	}

	/**
	 * Handles a finger-movement on the touchscreen.
	 * Moves a node when draggingNode was set by {@link #handleTouchEventDown(float, float)}.
	 * Otherwise the movement will be interpreted as map-translation.
	 * Map will be repainted.
	 * 
	 * @param absoluteX The absolute display-coordinate.
	 * @param absoluteY The absolute display-coordinate.
	 * @param relativeX The difference to the last absolute display-coordinate.
	 * @param relativeY The difference to the last absolute display-coordinate.
	 */
	void handleTouchEventMove(final float absoluteX, final float absoluteY, final float relativeX, final float relativeY) {
		if (draggingNode) {
			int lat = GeoMath.yToLatE7(map.getHeight(), viewBox, absoluteY);
			int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, absoluteX);
			delegator.updateLatLon(selectedNode, lat, lon);
			translateOnBorderTouch(absoluteX, absoluteY);
			map.invalidate();
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
		} catch (FollowGpsException e) {
		}
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
	 * Splits all ways at the given node.
	 * 
	 * @param node
	 */
	public void performSplit(final Node node) {
		if (node != null) {
			delegator.splitAtNode(node);
			map.invalidate();
		}
	}
	
	/**
	 * Splits a way at a given node
	 * @param way the way to split
	 * @param node the node at which the way should be split
	 */
	public void performSplit(final Way way, final Node node) {
		delegator.splitAtNode(way, node);
	}
	
	/**
	 * Merge two ways.
	 * Ways must be valid (i.e. have at least two nodes) and mergeable
	 * (i.e. have a common start/end node).
	 * 
	 * If the first way does not have tags, but the second one does, the tags will be copied.
	 * Otherwise, only the tags of the first way will be kept.
	 *  
	 * @param mergeInto Way to merge the other way into. This way will be kept.
	 * @param mergeFrom Way to merge into the other. This way will be deleted.
	 */
	public void performMerge(Way mergeInto, Way mergeFrom) {
		if (mergeInto.getTags().isEmpty() && !mergeFrom.getTags().isEmpty()) {
			delegator.setTags(mergeInto, mergeFrom.getTags());
		}
		delegator.mergeWays(mergeInto, mergeFrom);
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
		Node node = getClickedNode(x, y);
		if (node != null) {
			return node;
		}
		//create a new node on a way
		for (Way way : delegator.getCurrentStorage().getWays()) {
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
			return (GeoMath.getLineDistance(x, y, node1X, node1Y, node2X, node2Y) < tolerance);
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
	 * Loads the area defined by mapBox from the OSM-Server.
	 * 
	 * @param mapBox Box defining the area to be loaded.
	 */
	void downloadBox(final BoundingBox mapBox) {
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.showDialog(DialogFactory.PROGRESS_LOADING);
			}
			
			@Override
			protected Integer doInBackground(Void... arg0) {
				int result = 0;
				try {
					final OsmParser osmParser = new OsmParser();
					final InputStream in = prefs.getServer().getStreamForBox(mapBox);
					try {
						osmParser.start(in);
						delegator.reset();
						delegator.setCurrentStorage(osmParser.getStorage());
						if (mapBox != null && delegator.isEmpty()) {
							delegator.setOriginalBox(mapBox);
						}
						viewBox.setBorders(delegator.getOriginalBox());
					} finally {
						SavingHelper.close(in);
					}
				} catch (SAXException e) {
					Log.e("Vespucci", "Problem parsing", e);
				} catch (ParserConfigurationException e) {
					Log.e("Vespucci", "Problem parsing", e);
				} catch (OsmServerException e) {
					result = DialogFactory.UNDEFINED_ERROR;
					Log.e("Vespucci", "Problem downloading", e);
					Application.mainActivity.getExceptions().add(e);
				} catch (IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e("Vespucci", "Problem downloading", e);
					Application.mainActivity.getExceptions().add(e);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				try {
					Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_LOADING);
				} catch (Exception e) {} // Avoid crash if dialog is already dismissed
				View map = Application.mainActivity.getCurrentFocus();
				viewBox.setRatio((float)map.getWidth() / (float)map.getHeight());
				paints.updateStrokes((STROKE_FACTOR / mapBox.getWidth()));
				map.invalidate();
				if (result != 0) {
					Application.mainActivity.showDialog(result);
				}
			}
			
		}.execute();
	}

	/**
	 * @see #downloadBox(Main, BoundingBox)
	 */
	void downloadCurrent() {
		downloadBox(viewBox);
	}
	
	/**
	 * Re-downloads the same area as last time
	 * @see #downloadBox(Main, BoundingBox)
	 */
	void downloadLast() {
		BoundingBox box = delegator.getOriginalBox();
		if (box != null && box.isValidForApi()) downloadBox(box);
	}

	/**
	 * Saves to a file in the background.
	 * 
	 * @param showDone when true, a Toast will be shown when the file was saved.
	 */
	void saveAsync(final boolean showDone) {
		new AsyncTask<Void, Void, Void>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.setProgressBarIndeterminateVisibility(true);
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				save();
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				Application.mainActivity.setProgressBarIndeterminateVisibility(false);
				if (showDone) {
					Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_save_done, Toast.LENGTH_SHORT).show();
				}
			}
			
		}.execute();
	}
	
	/**
	 * Saves to a file (synchronously)
	 */
	void save() {
		try {
			Context ctx = Application.mainActivity.getApplicationContext();
			delegator.writeToFile();
			if (new SavingHelper<Mode>().load(MODE_FILENAME) != mode) { // save only if changed
				new SavingHelper<Mode>().save(MODE_FILENAME, mode);
			}
			// TODO js save GPS track
		} catch (IOException e) {
			Log.e("Vespucci", "Problem saving", e);
		}
	}
	
	/**
	 * Loads data from a file in the background.
	 */
	void loadFromFile() {
		new AsyncTask<Void, Void, Void>() {
			
			Mode loadedMode = null;
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.showDialog(DialogFactory.PROGRESS_LOADING);
				Log.d("Logic", "loadFromFile onPreExecute");
			}
			
			@Override
			protected Void doInBackground(Void... params) {
				delegator.readFromFile();
				viewBox.setBorders(delegator.getOriginalBox());
				loadedMode = new SavingHelper<Mode>().load(MODE_FILENAME);
				// TODO js load GPS track
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				Log.d("Logic", "loadFromFile onPostExecute");
				Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_LOADING);
				View map = Application.mainActivity.getCurrentFocus();
				setMode(loadedMode == null ? Mode.MODE_MOVE : loadedMode);
				viewBox.setRatio((float)map.getWidth() / (float)map.getHeight());
				paints.updateStrokes(STROKE_FACTOR / viewBox.getWidth());
				map.invalidate();
			}
			
		}.execute();
	}

	/**
	 * Uploads to the server in the background.
	 * 
	 * @param comment Changeset comment.
	 */
	public void upload(final String comment) {
		final Server server = prefs.getServer();
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.setProgressBarIndeterminateVisibility(true);
			}
			
			@Override
			protected Integer doInBackground(Void... params) {
				int result = 0;
				try {
					delegator.uploadToServer(server, comment);
				} catch (final MalformedURLException e) {
					result = DialogFactory.UNDEFINED_ERROR;
					Log.e(DEBUG_TAG, "", e);
					//caller.getExceptions().add(e);
				} catch (final ProtocolException e) {
					result = DialogFactory.UNDEFINED_ERROR;
					Log.e(DEBUG_TAG, "", e);
					//caller.getExceptions().add(e);
				} catch (final OsmServerException e) {
					switch (e.getErrorCode()) {
					case HttpStatus.SC_UNAUTHORIZED:
						result = DialogFactory.WRONG_LOGIN;
						break;
					//TODO: implement other state handling
					default:
						result = DialogFactory.UNDEFINED_ERROR;
						break;
					}
					Log.e(DEBUG_TAG, "", e);
					//caller.getExceptions().add(e);
				} catch (final IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e(DEBUG_TAG, "", e);
					//caller.getExceptions().add(e);
				} catch (final NullPointerException e) {
					result = DialogFactory.UNDEFINED_ERROR;
					Log.e(DEBUG_TAG, "", e);
					//caller.getExceptions().add(e);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				Application.mainActivity.setProgressBarIndeterminateVisibility(false);
				Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_upload_success, Toast.LENGTH_SHORT).show();
				Application.mainActivity.getCurrentFocus().invalidate();
				if (result != 0) {
					Application.mainActivity.showDialog(result);
				}
			}
			
		}.execute();
	}
	
	/**
	 * Make a new bug at the given screen X/Y coordinates.
	 * @param x The screen X-coordinate of the bug.
	 * @param y The screen Y-coordinate of the bug.
	 * @return The new bug, which must have a comment added before it can be submitted to OSB.
	 */
	public Bug makeNewBug(final float x, final float y) {
		int lat = GeoMath.yToLatE7(map.getHeight(), viewBox, y);
		int lon = GeoMath.xToLonE7(map.getWidth(), viewBox, x);
		return new Bug(lat, lon);
	}
	
	/**
	 * Internal setter to set the internal value
	 */
	public void setSelectedNode(final Node selectedNode) {
		this.selectedNode = selectedNode;
	}

	/**
	 * Internal setter to a) set the internal value and b) push the value to {@link #map}.
	 */
	public void setSelectedWay(final Way selectedWay) {
		this.selectedWay = selectedWay;
		map.setSelectedWay(selectedWay);
	}
	
	/**
	 * Set the currently selected bug.
	 * @param selectedBug The selected bug.
	 */
	public void setSelectedBug(final Bug selectedBug) {
		this.selectedBug = selectedBug;
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
	 * Get the selected bug.
	 * @return The selected bug.
	 */
	public final Bug getSelectedBug() {
		return selectedBug;
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
		map.setDelegator(delegator);
		map.setViewBox(viewBox);
	}
	
	/**
	 * @return a list of all pending changes to upload
	 */
	public List<String> getPendingChanges(final Context aCaller) {
		return delegator.listChanges(aCaller.getResources());
	}

	/**
	 * Sets the set of elements that can currently be clicked.
	 * If set to null, the map will use default behaviour.
	 * If set to a non-null value, the map will highlight only elements in the list.
	 * @param clickable a set of elements to which highlighting should be limited, or null to remove the limitation
	 */
	public void setClickableElements(Set<OsmElement> clickable) {
		this.clickableElements = clickable;
	}
	
	/**
	 * @return 
	 * @return the list of clickable elements. May be null, meaning no restrictions on clickable elements
	 */
	public Set<OsmElement> getClickableElements() {
		return clickableElements;
	}
	
}
