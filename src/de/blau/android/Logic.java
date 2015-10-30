package de.blau.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.acra.ACRA;
import org.apache.http.HttpStatus;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Toast;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.exception.StorageException;
import de.blau.android.osb.Bug;
import de.blau.android.osb.Note;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Capabilities;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.Server;
import de.blau.android.osm.Server.UserDetails;
import de.blau.android.osm.Server.Visibility;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.PostMergeHandler;
import de.blau.android.osm.Track;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.Profile;
import de.blau.android.services.TrackerService;
import de.blau.android.util.EditState;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Offset;
import de.blau.android.util.SavingHelper;
import de.blau.android.views.util.OpenStreetMapTileServer;

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

	private static final String DEBUG_TAG = Logic.class.getSimpleName();

	/**
	 * Enums for modes.
	 */
	public static enum Mode {
		/**
		 * move nodes by tapping the screen
		 */
		MODE_MOVE,
		/**
		 * add nodes by tapping the screen
		 */
		MODE_TAG_EDIT,
		/**
		 * split ways by tapping the screen
		 */
		MODE_EASYEDIT,
		/**
		 * Background alignment mode
		 */
		MODE_ALIGN_BACKGROUND
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
	private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000*2;

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
	 * Filename of file containing the currently edit state
	 */
	private static final String EDITSTATE_FILENAME = "edit.state";
	
	/** Sorter instance for sorting nodes by distance */
	private static final DistanceSorter<OsmElement, Node> nodeSorter = new DistanceSorter<OsmElement, Node>();
	/** Sorter instance for sorting ways by distance */
	private static final DistanceSorter<Way, Way> waySorter = new DistanceSorter<Way, Way>();

	/**
	 * maximum number of nodes in a way for it still to be moveable, arbitrary number for now
	 */
	private static final int MAX_NODES_FOR_MOVE = 100;
	
	/**
	 * Stores the {@link Preferences} as soon as they are available.
	 */
	private Preferences prefs;


	/**
	 * The user-selected node.
	 */
	private List<Node> selectedNodes;

	/**
	 * The user-selected way.
	 */
	private List<Way> selectedWays;
	
	/**
	 * The user-selected relation.
	 */
	private List<Relation> selectedRelations;
	
	/* The following are lists because elements could be add multiple times
	 * adding them once per selected relation and the same for deletion avoids 
	 * having to maintain a count 
	 */
	/**
	 * ways belonging to a selected relation
	 */
	private List<Way> selectedRelationWays = null;
	
	/**
	 * nodes belonging to a selected relation
	 */
	private List<Node> selectedRelationNodes = null;
	
	/**
	 * relations belonging to a selected relation 
	 */
	private List<Relation> selectedRelationRelations = null;
	
	
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
	 * Are we currently dragging a way?
	 * Set by {@link #handleTouchEventDown(float, float)}
	 */
	private boolean draggingWay = false;
	private int startLat;
	private int startLon;
	private float startY;
	private float startX;
	private float centroidY;
	private float centroidX;

	/**
	 * Are we currently dragging a handle?
	 */
	private boolean draggingHandle = false;
	
	/**
	 * 
	 */
	private boolean rotatingWay = false;
	
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

	
	private Set<OsmElement> clickableElements;
	
	/**
	 * add relations to result of clicks/touches
	 */
	private boolean returnRelations = true;
	

	
	/**
	 * The currently selected handle to be dragged to create a new node in a way.
	 */
	private Handle selectedHandle = null;

	/**
	 * Initiate all needed values. Starts Tracker and delegate the first values for the map.
	 * 
	 * @param map Instance of the Map. All new Values will be pushed to it.
	 * @param profile The drawing profile used by the map to paint the objects on screen.
	 */
	Logic(final Map map, final Profile profile) {
		this.map = map;

		viewBox = getDelegator().getLastBox();
		
		mode = Mode.MODE_MOVE;
		setSelectedBug(null);
		setSelectedNode(null);
		setSelectedWay(null);
		setSelectedRelation(null);

		// map.setPaints(paints);
		map.setDelegator(getDelegator());
		map.setViewBox(viewBox);
	}


	/**
	 * Set all {@link Preferences} and delegates them to {@link Tracker} and {@link Map}. The AntiAlias-Flag will be set
	 * to {@link Paints}. Map gets repainted.
	 * 
	 * @param prefs the new Preferences.
	 */
	void setPrefs(final Preferences prefs) {
		this.prefs = prefs;
		Profile.setAntiAliasing(prefs.isAntiAliasingEnabled());
		map.invalidate();
	}

	
	/**
	 * Informs the current drawing profile of the user preferences affecting
	 * drawing, the current screen properties, and clears the way cache.
	 */
	public void updateProfile() {
		Profile.switchTo(prefs.getMapProfile());
		Profile.updateStrokes(strokeWidth(viewBox.getWidth()));
		Profile.setAntiAliasing(prefs.isAntiAliasingEnabled());
		// zap the cached style for all ways
		for (Way w:getDelegator().getCurrentStorage().getWays()) {
			w.setFeatureProfile(null);
		}
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
		switch (mode) {
		case MODE_TAG_EDIT:
			break;
		case MODE_ALIGN_BACKGROUND:		// action mode sanity check
		if (Application.mainActivity.getBackgroundAlignmentActionModeCallback() == null) {
			Log.d("Logic","weird state of edit mode, resetting");
			setMode(Mode.MODE_MOVE);
		}
		case MODE_EASYEDIT:
		case MODE_MOVE:
		default:
			setSelectedNode(null);
			setSelectedWay(null);
			setSelectedRelation(null);
			break;
		}
		map.invalidate();
	}

	/**
	 * Returns the current mode that the program is in.
	 */
	public Mode getMode() {
		return mode;
	}
	
	/**
	 * Checks for changes in the API-Storage.
	 * 
	 * @return {@link StorageDelegator#hasChanges()}
	 */
	public boolean hasChanges() {
		return getDelegator().hasChanges();
	}
	
	/**
	 * Get the current undo instance.
	 * For immediate use only - DO NOT CACHE THIS.
	 * 
	 * @return the UndoStorage, allowing operations like creation of checkpoints and undo/redo.  
	 */
	public UndoStorage getUndo() {
		return getDelegator().getUndo();
	}

	/**
	 * Wrapper to ensure the dirty flag is set 
	 * @return
	 */
	public String undo() {
		String name = getDelegator().getUndo().undo();
		getDelegator().dirty();
		return name;
	}
	
	/**
	 * Wrapper to ensure the dirty flag is set 
	 * @return
	 */
	public String redo() {
		String name = getDelegator().getUndo().redo();
		getDelegator().dirty();
		return name;
	}
	
	/**
	 * Checks if the viewBox is close enough to the viewBox to be in the ability to edit something.
	 * 
	 * @return true, if viewBox' width is smaller than {@link #TOLERANCE_MIN_VIEWBOX_WIDTH}.
	 */
	public boolean isInEditZoomRange() {
		return (viewBox.getWidth() < TOLERANCE_MIN_VIEWBOX_WIDTH) && (viewBox.getHeight() < TOLERANCE_MIN_VIEWBOX_WIDTH) && !map.tooManyNodes();
	}

	/**
	 * Translates the viewBox into the given direction by {@link #TRANSLATION_FACTOR} and sets GPS-Following to false.
	 * Map will be repainted.
	 * 
	 * @param direction the direction of the translation.
	 */
	public void translate(final CursorPaddirection direction) {
		float translation = viewBox.getWidth() * TRANSLATION_FACTOR;
		try {
			switch (direction) {
			case DIRECTION_LEFT:
				viewBox.translate((int) -translation, 0);
				break;
			case DIRECTION_DOWN:
				viewBox.translate(0, -(GeoMath.latE7ToMercatorE7(viewBox.getTop())-(int)(viewBox.getBottomMercator()*1E7D))); 
				break;
			case DIRECTION_RIGHT:
				viewBox.translate((int) translation, 0);
				break;
			case DIRECTION_UP:
				viewBox.translate(0, GeoMath.latE7ToMercatorE7(viewBox.getTop())-(int)(viewBox.getBottomMercator()*1E7D)); 
				break;
			}
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		map.invalidate();
	}
	
	/**
	 * Test if the requested zoom operation can be performed.
	 * 
	 * @param zoomIn The zoom operation: ZOOM_IN or ZOOM_OUT.
	 * @return true if the zoom operation can be performed, false if it can't.
	 */
	public boolean canZoom(final boolean zoomIn) {
		return zoomIn ? viewBox.canZoomIn() : viewBox.canZoomOut();
	}
	
	/**
	 * Zooms in or out. Checks if the new viewBox is close enough for editing
	 * and sends this value to map. Strokes will be updated and map will be repainted.
	 * 
	 * @param zoomIn
	 *            true for zooming in.
	 */
	public void zoom(final boolean zoomIn) {
		if (zoomIn) {
			viewBox.zoomIn();
		} else {
			viewBox.zoomOut();
		}
		Profile.updateStrokes(strokeWidth(viewBox.getWidth()));
		if (rotatingWay) {
			showCrosshairsForCentroid();
		}
		map.postInvalidate();
	}
	
	/**
	 * Zooms the map in or out by the given factor and updates the map view after zooming.
	 * 
	 * @param zoomFactor The factor to zoom by, negative values zoom out, positive zooms in.
	 */
	public void zoom(final float zoomFactor) {
		try {
			viewBox.zoom(zoomFactor);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Profile.updateStrokes(strokeWidth(viewBox.getWidth()));
		if (rotatingWay) {
			showCrosshairsForCentroid();
		}
		map.postInvalidate();
	}
	
	/**
	 * Set the zoom to a specific tile zoom level.
	 * 
	 * @param z The TMS zoom level to zoom to (from 0 for the whole world to about 19 for small areas).
	 */
	public void setZoom(int z) {
		viewBox.setZoom(z);
		if (rotatingWay) {
			showCrosshairsForCentroid();
		}
	}

	/**
	 * Return a stroke width value that increases with zoom and is capped at a configurable value
	 * 
	 * @param width   screenwidth in 10e7 deg.
	 * @return 		  stroke width
	 */
	private float strokeWidth(long width){
		// prefs may not have been initialized
		if (prefs != null ) return Math.min(prefs.getMaxStrokeWidth(), STROKE_FACTOR / width);
		return STROKE_FACTOR / width;
	}
	
	/**
	 * Create an undo checkpoint using a resource string as the name
	 * 
	 * @param stringId the resource id of the string representing the checkpoint name
	 */
	private void createCheckpoint(int stringId) {
		getDelegator().getUndo().createCheckpoint(Application.mainActivity.getResources().getString(stringId));
	}
	
	/**
	 * Remove an undo checkpoint using a resource string as the name
	 * 
	 * @param stringId the resource id of the string representing the checkpoint name
	 */
	private void removeCheckpoint(int stringId) {
		getDelegator().getUndo().removeCheckpoint(Application.mainActivity.getResources().getString(stringId));
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
	public synchronized boolean setTags(final String type, final long osmId, final java.util.Map<String, String> tags) {
		OsmElement osmElement = getDelegator().getOsmElement(type, osmId);

		if (osmElement == null) {
			Log.e(DEBUG_TAG, "Attempted to setTags on a non-existing element " + type + " #" + osmId);
			return false;
		} else {
			createCheckpoint(R.string.undo_action_set_tags);
			getDelegator().setTags(osmElement, tags);
			return true;
		}
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
	public synchronized boolean updateParentRelations(final String type, final long osmId, final HashMap<Long, String> parents) {
		OsmElement osmElement = getDelegator().getOsmElement(type, osmId);
		if (osmElement == null) {
			Log.e(DEBUG_TAG, "Attempted to update relations on a non-existing element");
			return false;
		} else {
			createCheckpoint(R.string.undo_action_update_relations);
			getDelegator().updateParentRelations(osmElement, parents);	
			return true;
		}
	}
	
	/**
	 * Updates the list of members in the selected relation.
	 * Actual work is delegated out to {@link StorageDelegator}.
	 * 
	 * @param osmId The OSM ID of the relation to change.
	 * @param members The new list of members to set for the given relation.
	 */
	public synchronized boolean updateRelation(long osmId, ArrayList<RelationMemberDescription> members) {
		OsmElement osmElement = getDelegator().getOsmElement(Relation.NAME, osmId);
		if (osmElement == null) {
			Log.e(DEBUG_TAG, "Attempted to update non-existing relation #" + osmId);
			return false;
		} else {
			createCheckpoint(R.string.undo_action_update_relations);
			getDelegator().updateRelation((Relation)osmElement, members);	
			return true;
		}
	}

	/**
	 * Prepares the screen for an empty map. Strokes will be updated and map will be repainted.
	 * 
	 * @param box the new empty map-box. Don't mess up with the viewBox!
	 */
	void newEmptyMap(BoundingBox box) {
		Log.d(DEBUG_TAG, "newEmptyMap");
		if (box == null) { // probably should do a more general check if the BB is valid
			try {
				box = new BoundingBox(-180.0d, -GeoMath.MAX_LAT, +180.0d, GeoMath.MAX_LAT); // maximum possible size in mercator projection
			} catch (OsmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// not checking will zap edits, given that this method will only be called when we are not downloading, not a good thing
		if (!getDelegator().isDirty()) {
			getDelegator().reset(false);
			// delegator.setOriginalBox(box); not needed IMHO
		} else {
			//TODO show warning
			Log.e(DEBUG_TAG, "newEmptyMap called on dirty storage");
		}
		// if the map view isn't drawn use an approximation for the aspect ratio of the display ... this is a hack 
		float ratio = (float)Application.mainActivity.getResources().getDisplayMetrics().widthPixels / (float)Application.mainActivity.getResources().getDisplayMetrics().heightPixels;
		if (map.getHeight() != 0) {
			ratio = (float) map.getWidth() / map.getHeight();
		}
		viewBox.setBorders(box, ratio);
		Profile.updateStrokes(strokeWidth(viewBox.getWidth()));
		map.invalidate();
		UndoStorage.updateIcon();
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
		if (returnRelations) {
			// add any relations that the elements are members of
			ArrayList<OsmElement> relations = new ArrayList<OsmElement>();
			for (OsmElement e: result) {
				if (e.getParentRelations() != null) {
					for (Relation r: e.getParentRelations()) {
						if (!relations.contains(r)) { // not very efficient
							relations.add(r);
						}
					}
				}
			}
			result.addAll(relations);
		}
		return result;
	}

	/**
	 * Returns all ways within way tolerance from the given coordinates, and their distances from them.
	 * 
	 * @param x x display coordinate
	 * @param y y display coordinate
	 * @return a hash map mapping Ways to distances
	 */
	public HashMap<Way, Double> getClickedWaysWithDistances(final float x, final float y) {
		HashMap<Way, Double> result = new HashMap<Way, Double>();

		for (Way way : getDelegator().getCurrentStorage().getWays()) {
			List<Node> wayNodes = way.getNodes();

			if (clickableElements != null && !clickableElements.contains(way)) continue;

			//Iterate over all WayNodes, but not the last one.
			for (int k = 0, wayNodesSize = wayNodes.size(); k < wayNodesSize - 1; ++k) {
				Node node1 = wayNodes.get(k);
				Node node2 = wayNodes.get(k + 1);
				// TODO only project once per node
				float node1X = lonE7ToX(node1.getLon());
				float node1Y = latE7ToY(node1.getLat());
				float node2X = lonE7ToX(node2.getLon());
				float node2Y = latE7ToY(node2.getLat());

				if (isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y)) {
					result.put(way, GeoMath.getLineDistance(x, y, node1X, node1Y, node2X, node2Y));
					break;
				}
			}
		}	
		return result;
	}
	
	/**
	 * The small mid segment 'x' handles that allow dragging to easily add a new node to a way.
	 */
	class Handle {
		float x;
		float y;
		
		Handle(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**
	 * Returns all ways with a mid-way segment handle tolerance from the given coordinates, and their distances from them.
	 * 
	 * @param x x display coordinate
	 * @param y y display coordinate
	 * @return a hash map mapping Ways to distances
	 */
	Handle getClickedWayHandleWithDistances(final float x, final float y) {
		
		Handle result = null;
		double bestDistance = Double.MAX_VALUE;
		
		for (Way way : getDelegator().getCurrentStorage().getWays()) {
			List<Node> wayNodes = way.getNodes();

			if (clickableElements != null && !clickableElements.contains(way)) continue;

			//Iterate over all WayNodes, but not the last one.
			for (int k = 0, wayNodesSize = wayNodes.size(); k < wayNodesSize - 1; ++k) {
				Node node1 = wayNodes.get(k);
				Node node2 = wayNodes.get(k + 1);
				// TODO only project once per node
				float node1X = lonE7ToX(node1.getLon());
				float node1Y = latE7ToY(node1.getLat());
				float xDelta = lonE7ToX(node2.getLon()) - node1X;
				float yDelta = latE7ToY(node2.getLat()) - node1Y;
				
				float handleX = node1X + xDelta/2; 
				float handleY = node1Y + yDelta/2;

				float differenceX = Math.abs(handleX - x);
				float differenceY = Math.abs(handleY - y);
				
				if ((differenceX > Profile.getCurrent().wayToleranceValue) && (differenceY > Profile.getCurrent().wayToleranceValue))	continue;
				if (Math.hypot(xDelta,yDelta) <= Profile.getCurrent().minLenForHandle) continue;
				
				double dist = Math.hypot(differenceX, differenceY);
				// TODO better choice for tolerance 
				if ((dist <= Profile.getCurrent().wayToleranceValue) && (dist < bestDistance)) {
					bestDistance = dist;
					result = new Handle(handleX, handleY);
				}
				
			}
		}	
		return result;
	}
	
	/**
	 * Calculates the on-screen distance between a node and the screen coordinate of a click.
	 * Returns null if the node was outside the click tolerance.
	 * 
	 * @param node the node
	 * @param x the x coordinate of the clicked point
	 * @param y the y coordinate of the clicked point
	 * @return The distance between the clicked point and the node in px if the node was within the tolerance value,
	 *         null otherwise
	 */
	private Double clickDistance(Node node, final float x, final float y) {
		return clickDistance(node, x, y, node.isTagged() ? Profile.getCurrent().nodeToleranceValue : Profile.getCurrent().wayToleranceValue/2);
	}

	private Double clickDistance(Node node, final float x, final float y, float tolerance) {

		float differenceX = Math.abs(lonE7ToX(node.getLon()) - x);
		float differenceY = Math.abs(latE7ToY(node.getLat()) - y);
		
		if ((differenceX > tolerance) && (differenceY > tolerance))	return null;
		
		double dist = Math.hypot(differenceX, differenceY);
		return (dist > tolerance) ? null : dist;
	}
	
	/**
	 * Returns all nodes within node tolerance from the given coordinates, and their distances from them.
	 * 
	 * @param x x display coordinate
	 * @param y y display coordinate
	 * @return a hash map mapping Nodes to distances
	 */
	public HashMap<Node, Double> getClickedNodesWithDistances(final float x, final float y) {
		HashMap<Node, Double> result = new HashMap<Node, Double>();
		List<Node> nodes = getDelegator().getCurrentStorage().getNodes();

		for (Node node : nodes) {
			if (clickableElements != null && !clickableElements.contains(node)) continue;

			int lat = node.getLat();
			int lon = node.getLon();

			if (node.getState() != OsmElement.STATE_UNCHANGED || getDelegator().isInDownload(lat, lon)) {
				Double dist = clickDistance(node, x, y);
				if (dist != null) result.put(node, dist);
			}
		}
		return result;
	}
	
	/**
	 * Searches for a Node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox.
	 * 
	 * @param x display-coordinate.
	 * @param y display-coordinate.
	 * @return all nodes within tolerance found in the currentStorage node-list, ordered ascending by distance.
	 */
	public List<OsmElement> getClickedNodes(final float x, final float y) {
		return nodeSorter.sort(getClickedNodesWithDistances(x, y));
	}

	/**
	 * Searches for a way end node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox.
	 * 
	 * @param x display-coordinate.
	 * @param y display-coordinate.
	 * @return all end nodes within tolerance found in the currentStorage node-list, ordered ascending by distance.
	 */
	public List<OsmElement> getClickedEndNodes(final float x, final float y) {
		List<OsmElement> result = new ArrayList<OsmElement>();
		List<OsmElement> allNodes = getClickedNodes(x, y);

		for (OsmElement osmElement : allNodes) {
			if (getDelegator().getCurrentStorage().isEndNode((Node) osmElement))
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
	 * 
	 * @param x x display-coordinate.
	 * @param y y display-coordinate.
	 * @return the ways
	 */
	public List<Way> getClickedWays(final float x, final float y) {
		return waySorter.sort(getClickedWaysWithDistances(x, y));
	}
	
	/**
	 * Returns the closest way (within tolerance) to the given coordinates
	 * 
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
	
	/**
	 * Returns a list of all the clickable OSM elements in storage (does not
	 * restrict to the current screen). Before returning the list is "pruned" to
	 * remove any elements on the exclude list.
	 * 
	 * @param excludes The list of OSM elements to exclude from the results.
	 * @return
	 */
	public Set<OsmElement> findClickableElements(List<OsmElement> excludes) {
		Set<OsmElement> result = new HashSet<OsmElement>();
		result.addAll(getDelegator().getCurrentStorage().getNodes());
		result.addAll(getDelegator().getCurrentStorage().getWays());
		for (OsmElement e:excludes)
			result.remove(e);
		return result;
	}
	
	/**
	 * Get a list of all the Ways connected to the given Node.
	 * 
	 * @param node The Node.
	 * @return A list of all Ways connected to the Node.
	 */
	public List<Way> getWaysForNode(final Node node) {
		return getDelegator().getCurrentStorage().getWays(node);
	}

	/**
	 * Test if the given Node is an end node of a Way. Isolated nodes not part
	 * of a way are not considered an end node.
	 * 
	 * @param node Node to test.
	 * @return true if the Node is an end node of a Way, false otherwise.
	 */
	public boolean isEndNode(final Node node) {
		return getDelegator().getCurrentStorage().isEndNode(node);
	}
	
	/**
	 * Check all nodes in way to see if the bbox of the downloaded data.
	 * 
	 * @param way the way whose nodes should be checked
	 * @return true if the above is the case
	 */
	public boolean isInDownload(Way way) {
		for (Node n:way.getNodes()) {
			if (!getDelegator().isInDownload(n.getLat(), n.getLon())){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check if node is in the bbox for the downloaded data
	 * 
	 * @param node
	 * @return true if the above is the case
	 */
	public boolean isInDownload(Node n) {
		return getDelegator().isInDownload(n.getLat(), n.getLon());
	}
	
	/**
	 * Handles the event when user begins to touch the display. When the viewBox is close enough for editing and the
	 * user is in edit-mode a touched node will bet set to selected. draggingNode will be set if a node is to be moved.
	 * A eventual movement of this node will be done in {@link #handleTouchEventMove(float, float, float, float, boolean)}.
	 * 
	 * @param x display-coord.
	 * @param y display-coord.
	 */
	synchronized void handleTouchEventDown(final float x, final float y) {
		boolean draggingMultiselect = false;
		if (isInEditZoomRange() && mode == Mode.MODE_EASYEDIT) {
			draggingNode = false;
			draggingWay = false;
			draggingHandle = false;
			if (selectedNodes != null && selectedNodes.size() == 1 && selectedWays == null && clickDistance(selectedNodes.get(0), x, y, prefs.largeDragArea() ? Profile.getCurrent().largDragToleranceRadius : Profile.getCurrent().nodeToleranceValue) != null) {
				draggingNode = true;
				if (prefs.largeDragArea()) {
					startX = lonE7ToX(selectedNodes.get(0).getLon());
					startY = latE7ToY(selectedNodes.get(0).getLat());
				}
			}
			else {
				if (selectedWays != null && selectedWays.size() == 1 && selectedNodes == null) {
					if (!rotatingWay) {	
						Way clickedWay = getClickedWay(x, y);
						if (clickedWay != null && (clickedWay.getOsmId() == selectedWays.get(0).getOsmId())) {
							if (selectedWays.get(0).getNodes().size() <= MAX_NODES_FOR_MOVE) {
								startLat = yToLatE7(y);
								startLon = xToLonE7(x);
								draggingWay = true;
							}
							else {
								Toast.makeText(Application.mainActivity, R.string.toast_too_many_nodes_for_move, Toast.LENGTH_LONG).show();
							}
						}
					} else {
						startX = x;
						startY = y;
					}
				} else {
					// check for multi-select
					if ((selectedWays != null && selectedWays.size() > 1) ||  (selectedNodes != null && selectedNodes.size() > 1) 
							|| ((selectedWays != null && selectedWays.size() >= 1) && (selectedNodes != null && selectedNodes.size() >= 1))) {
						Log.d(DEBUG_TAG, "Multi select detected");
						boolean foundSelected = false;
						if (selectedWays != null) {
							List<Way> clickedWays = getClickedWays(x, y);
							for (Way w:clickedWays) {
								if (selectedWays.contains(w)) {
									foundSelected = true;
									break;
								}
							}
						}
						if (!foundSelected && selectedNodes != null) {
							List<OsmElement> clickedNodes = getClickedNodes(x,y);
							for (OsmElement n:clickedNodes) {
								if (selectedNodes.contains(n)) {
									foundSelected = true;
									break;
								}
							}
						}
						if (foundSelected) {
							startLat = yToLatE7(y);
							startLon = xToLonE7(x);
							startX = x;
							startY = y;
							draggingMultiselect = true;
							draggingWay = true;
						}
					} else {
						if (rotatingWay) {
							rotatingWay = false;
							hideCrosshairs();
						} else {
							// way center / handle
							// TODO this may cause issues in action modes were we expect only something from the available selection to be returned
							Handle handle = getClickedWayHandleWithDistances(x, y);
							if (handle != null) {
								Log.d("Logic","start handle drag");
								selectedHandle = handle;
								draggingHandle = true;
							}
						}
					}
				}
			}
		} else {
			draggingNode = false;
			draggingWay = false;
			rotatingWay = false;
			draggingHandle = false;
		}
		Log.d("Logic","handleTouchEventDown creating checkpoints");
		if (draggingNode || draggingWay) {
			if (draggingMultiselect) {
				createCheckpoint(R.string.undo_action_moveobjects);
			} else {
				createCheckpoint(draggingNode ? R.string.undo_action_movenode : R.string.undo_action_moveway);
			}
		} else if (rotatingWay) {
			createCheckpoint(R.string.undo_action_rotateway);
		}
	}

	/**
	 * Calculates the coordinates for the center of the screen and displays a crosshair there. 
	 */
	public void showCrosshairsForCentroid()
	{
		if (selectedWays == null) {
			return;
		}
		synchronized(selectedWays) {
			float centroid[] = centroidXY(map.getWidth(), map.getHeight(), viewBox, selectedWays.get(0));
			if (centroid==null) {
				return;
			}
			centroidX = centroid[0];
			centroidY = centroid[1];
			showCrosshairs(centroidX,centroidY);	
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
	 * @throws OsmIllegalOperationException 
	 */
	synchronized void handleTouchEventMove(final float absoluteX, final float absoluteY, final float relativeX, final float relativeY) {
		if (draggingNode || draggingWay || (draggingHandle && selectedHandle != null)) {
			int lat;
			int lon;
			// checkpoint created where draggingNode is set
			if ((draggingNode && ((selectedNodes != null && selectedNodes.size() == 1) || selectedWays ==  null)) || (draggingHandle && selectedHandle != null)) {
				if (draggingHandle) { // create node only if we are really dragging
					Log.d("Logic","creating node at handle position");
					try {
						if (performAddOnWay(selectedHandle.x, selectedHandle.y)) {
							selectedHandle = null;
							draggingNode = true;
							draggingHandle = false;
							if (prefs.largeDragArea()) {
								startX = lonE7ToX(selectedNodes.get(0).getLon());
								startY = latE7ToY( selectedNodes.get(0).getLat());
							}
							Application.mainActivity.easyEditManager.editElement(selectedNodes.get(0)); // this can only happen in EasyEdit mode
						}
						else return;
					} catch (OsmIllegalOperationException e) {
						Toast.makeText(Application.mainActivity, e.getMessage(), Toast.LENGTH_LONG).show();
						return;
					}
				}
				if (prefs.largeDragArea()) {
					startY = startY + relativeY;
					startX = startX - relativeX;
					lat = yToLatE7(startY);
					lon = xToLonE7(startX);
				}	
				else {
					lat = yToLatE7(absoluteY);
					lon = xToLonE7(absoluteX);
				}
				getDelegator().updateLatLon(selectedNodes.get(0), lat, lon);
				Application.mainActivity.easyEditManager.invalidate(); // if we are in an action mode update menubar
			}
			else { // way dragging and multi-select
				lat = yToLatE7(absoluteY);
				lon = xToLonE7(absoluteX);
				ArrayList<Node> nodes = new ArrayList<Node>();
				if (selectedWays != null && selectedWays.size() > 0) { // shouldn't happen but might be a race condition
					for (Way w:selectedWays) {
						nodes.addAll(w.getNodes());
					}	
				}
				if (selectedNodes != null && selectedNodes.size() > 0) {
					for (Node n:selectedNodes) {
						nodes.add(n);
					}
				}
				getDelegator().moveNodes(nodes, lat - startLat, lon - startLon);
				// update 
				startLat = lat;
				startLon = lon;
			}
			translateOnBorderTouch(absoluteX, absoluteY);
			Application.mainActivity.easyEditManager.invalidate(); // if we are in an action mode update menubar
		} else if (rotatingWay) {
			
			double aSq = (startY-absoluteY)*(startY-absoluteY) + (startX-absoluteX)*(startX-absoluteX);
			double bSq = (absoluteY-centroidY)*(absoluteY-centroidY) + (absoluteX-centroidX)*(absoluteX-centroidX);
			double cSq = (startY-centroidY)*(startY-centroidY) + (startX-centroidX)*(startX-centroidX);

			double cosAngle = (bSq + cSq -aSq)/(2*Math.sqrt(bSq)*Math.sqrt(cSq)); 

			int direction = 1; // 1 clockwise, -1 anti-clockwise
			// not perfect but works good enough
			if ((startY <= centroidY) && (absoluteY <= centroidY)) {
				direction = (startX > absoluteX) ? -1 : 1;
			} else if ((startX >= centroidX) && (absoluteX >= centroidX)) {
				direction = (startY > absoluteY) ?  -1 : 1;
			}
			else if ((startY >= centroidY) && (absoluteY >= centroidY)) {
				direction = (startX < absoluteX) ? -1 : 1;
			}
			else if ((startX < centroidX) && (absoluteX < centroidX)) {
				direction = (startY < absoluteY) ? -1 : 1;
			}
			else if ((startY < startX) && (absoluteY < absoluteX)) {
				direction = (startY > absoluteY) ? -1: 1;
			}
			else if ((startY >= startX) && (absoluteY >= absoluteX)) {
				direction = (startY < absoluteY) ? -1: 1;			
			}
	
			getDelegator().rotateWay(selectedWays.get(0), (float)Math.acos(cosAngle), direction, centroidX, centroidY, map.getWidth(), map.getHeight(), viewBox);
			startY = absoluteY;
			startX = absoluteX;
			Application.mainActivity.easyEditManager.invalidate(); // if we are in an action mode update menubar
		} else {
			if (mode == Mode.MODE_ALIGN_BACKGROUND)
				performBackgroundOffset(relativeX, relativeY);
			else
				performTranslation(relativeX, relativeY);
		}	
		map.invalidate();
	}

	/**
	 * Puts the editor into the mode where the selected way will be rotated by
	 * the handleTouchEventMove function on move events.
	 */
	public void setRotationMode() {
		rotatingWay = true;
	}
	
	/**
	 * Converts screen-coords to gps-coords and delegates translation to {@link BoundingBox#translate(int, int)}.
	 * GPS-Following will be disabled.
	 * 
	 * @param screenTransX Movement on the screen.
	 * @param screenTransY Movement on the screen.
	 */
	private void performTranslation(final float screenTransX, final float screenTransY) {
		int height = map.getHeight();
		int lon = xToLonE7(screenTransX);
		int lat = yToLatE7(height - screenTransY);
		int relativeLon = lon - viewBox.getLeft();
		int relativeLat = lat - viewBox.getBottom();

		try {
			viewBox.translate(relativeLon, relativeLat);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Converts screen-coords to gps-coords and offests background layer.
	 * 
	 * @param screenTransX Movement on the screen.
	 * @param screenTransY Movement on the screen.
	 */
	private void performBackgroundOffset(final float screenTransX, final float screenTransY) {
		int height = map.getHeight();
		int lon = xToLonE7(screenTransX);
		int lat = yToLatE7(height - screenTransY);
		int relativeLon = lon - viewBox.getLeft();
		int relativeLat = lat - viewBox.getBottom();
		OpenStreetMapTileServer osmts = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		double lonOffset = 0d;
		double latOffset = 0d;
		Offset o = osmts.getOffset(map.getZoomLevel());
		if (o != null) {
			lonOffset = o.lon;
			latOffset = o.lat;
		}
		osmts.setOffset(map.getZoomLevel(),lonOffset - relativeLon/1E7d, latOffset - relativeLat/1E7d);
	}

	/**
	 * Executes an add-command for x,y. Adds new nodes and ways to storage. When more than one Node were
	 * created/selected then a new way will be created.
	 * 
	 * @param x screen-coordinate
	 * @param y screen-coordinate
	 * @throws OsmIllegalOperationException 
	 */
	public synchronized void performAdd(final float x, final float y) throws OsmIllegalOperationException {
		Log.d("Logic","performAdd");
		createCheckpoint(R.string.undo_action_add);
		Node nextNode;
		Node lSelectedNode = selectedNodes != null && selectedNodes.size() > 0 ? selectedNodes.get(0) : null;
		Way lSelectedWay = selectedWays != null && selectedWays.size() > 0 ? selectedWays.get(0) : null;

		if (lSelectedNode == null) {
			//This will be the first node.
			lSelectedNode = getClickedNodeOrCreatedWayNode(x, y);
			if (lSelectedNode == null) {
				//A complete new Node...
				int lat = yToLatE7(y);
				int lon = xToLonE7(x);
				lSelectedNode = getDelegator().getFactory().createNodeWithNewId(lat, lon);
				getDelegator().insertElementSafe(lSelectedNode);
				if (!getDelegator().isInDownload(lat, lon)) {
					// warning toast
					Log.d("Logic","Outside of download");
					Toast.makeText(Application.mainActivity, R.string.toast_outside_of_download, Toast.LENGTH_SHORT).show();
				}
			}
		} else {
			//this is not the first node
			nextNode = getClickedNodeOrCreatedWayNode(x, y);
			if (nextNode == null) {
				//clicked on empty space -> create a new Node
				if (lSelectedWay == null) {
					//This is the second Node, so we create a new Way and add the previous selected node to this way
					lSelectedWay = getDelegator().createAndInsertWay(lSelectedNode);
				}
				int lat = yToLatE7(y);
				int lon = xToLonE7(x);
				lSelectedNode = getDelegator().getFactory().createNodeWithNewId(lat, lon);
				getDelegator().addNodeToWay(lSelectedNode, lSelectedWay);
				getDelegator().insertElementSafe(lSelectedNode);
				if (!getDelegator().isInDownload(lat, lon)) {
					// warning toast
					Log.d("Logic","Outside of download");
					Toast.makeText(Application.mainActivity, R.string.toast_outside_of_download, Toast.LENGTH_SHORT).show();
				}
			} else {
				//User clicks an existing Node
				if (nextNode == lSelectedNode) {
					//User clicks the last Node -> end here with adding
					removeCheckpoint(R.string.undo_action_add);
					lSelectedNode = null;
					lSelectedWay = null;
				} else {
					//Create a new way with the existing node, which was clicked.
					if (lSelectedWay == null) {
						lSelectedWay = getDelegator().createAndInsertWay(lSelectedNode);
					}
					//Add the new Node.
					getDelegator().addNodeToWay(nextNode, lSelectedWay);
					lSelectedNode = nextNode;
				}
			}
		}
		setSelectedNode(lSelectedNode);
		setSelectedWay(lSelectedWay);
	}
	
	/**
	 * Simplified version that takes geo coords and doesn't try to merge with existing features
	 * @param lonD
	 * @param latD
	 * @return
	 */
	public synchronized Node performAddNode(Double lonD, Double latD) {
		//A complete new Node...
		Log.d("Logic","performAddNode");
		createCheckpoint(R.string.undo_action_add);
		int lon = (int)(lonD*1E7D);
		int lat = (int)(latD*1E7D);
		Node newNode = getDelegator().getFactory().createNodeWithNewId(lat, lon);
		getDelegator().insertElementSafe(newNode);
		if (!getDelegator().isInDownload(lat, lon)) {
			// warning toast
			Log.d("Logic","Outside of download");
			Toast.makeText(Application.mainActivity, R.string.toast_outside_of_download, Toast.LENGTH_SHORT).show();
		}
		setSelectedNode(newNode);
		return newNode;
	}
	
	/**
	 * Executes an add-command for x,y but only if on way. Adds new node to storage. Will switch selected node,
	 * 
	 * @param x screen-coordinate
	 * @param y screen-coordinate
	 * @throws OsmIllegalOperationException 
	 */
	public synchronized boolean performAddOnWay(final float x, final float y) throws OsmIllegalOperationException {
		createCheckpoint(R.string.undo_action_add);
		Node savedSelectedNode = selectedNodes != null && selectedNodes.size() > 0 ? selectedNodes.get(0) : null;
		
		Node newSelectedNode = getClickedNodeOrCreatedWayNode(x, y);

		if (newSelectedNode == null) {
			newSelectedNode = savedSelectedNode;
			return false;
		}
			
		setSelectedNode(newSelectedNode);
		return true;
	}
	
	/**
	 * Catches the first node at the given position and delegates the deletion to {@link #delegator}.
	 * @param createCheckpoint TODO
	 * @param x screen-coordinate.
	 * @param y screen-coordinate.
	 */
	public synchronized void performEraseNode(final Node node, boolean createCheckpoint) {
		if (node != null) {
			if (createCheckpoint) {
				createCheckpoint(R.string.undo_action_deletenode);
			}
			getDelegator().removeNode(node);
			map.invalidate();
			if (!isInDownload(node)) {
				// warning toast
				Toast.makeText(Application.mainActivity, R.string.toast_outside_of_download, Toast.LENGTH_SHORT).show();
			}
		}
	}
	

	/**
	 * set new coordinates and center BBox on them
	 * 
	 * @param node
	 * @param lon
	 * @param lat
	 */
	public void performSetPosition(Node node, double lon, double lat) {
		if (node != null) {
			createCheckpoint(R.string.undo_action_movenode);
			int lonE7 = (int)(lon*1E7d);
			int latE7 = (int)(lat*1E7d);
			getDelegator().updateLatLon(node, latE7, lonE7);
			viewBox.moveTo(lonE7, latE7);
			map.invalidate();
		}
	}

	/**
	 * Deletes a way.
	 * 
	 * @param way the way to be deleted
	 * @param deleteOrphanNodes if true, way nodes that have no tags and are in no other ways will be deleted too
	 * @param createCheckpoint TODO
	 */
	public synchronized void performEraseWay(final Way way, final boolean deleteOrphanNodes, boolean createCheckpoint) {
		if (createCheckpoint) {
			createCheckpoint(R.string.undo_action_deleteway);
		}
		HashSet<Node> nodes = deleteOrphanNodes ? new HashSet<Node>(way.getNodes()) : null;  //  HashSet guarantees uniqueness
		getDelegator().removeWay(way);
		if (deleteOrphanNodes) {
			for (Node node : nodes) {
				if (getWaysForNode(node).isEmpty() && node.getTags().isEmpty()) getDelegator().removeNode(node);
			}
		}
		map.invalidate();
	}

	/**
	 * Catches the first relation at the given position and delegates the deletion to {@link #delegator}.
	 * @param createCheckpoint TODO
	 * @param x screen-coordinate.
	 * @param y screen-coordinate.
	 */
	public synchronized void performEraseRelation(final Relation relation, boolean createCheckpoint) {
		if (relation != null) {
			if (createCheckpoint) {
				createCheckpoint(R.string.undo_action_delete_relation);
			}
			getDelegator().removeRelation(relation);
			map.invalidate();
		}
	}

	/**
	 * Erase a list of objects
	 * @param selection
	 */
	public synchronized void performEraseMultipleObjects(ArrayList<OsmElement> selection) {
		// need to make three passes this probably should really be in logic
		createCheckpoint(R.string.undo_action_delete_objects);
		for (OsmElement e:selection) {	
			if (e instanceof Relation && e.getState() != OsmElement.STATE_DELETED) {
				performEraseRelation((Relation)e, false);
			}
		}	
		for (OsmElement e:selection) {	
			if (e instanceof Way && e.getState() != OsmElement.STATE_DELETED) {
				if (isInDownload((Way)e)) {
					performEraseWay((Way)e, true, false); // TODO maybe we don't want to delete the nodes
				} else {
					// TODO toast
				}
			}
		}
		for (OsmElement e:selection) {	
			if (e instanceof Node && e.getState() != OsmElement.STATE_DELETED) {
				performEraseNode((Node)e, false);
			}
		}
		
	}
	
	
	/**
	 * Splits all ways at the given node.
	 * 
	 * @param node
	 */
	public synchronized void performSplit(final Node node) {
		if (node != null) {
			// setSelectedNode(node);
			createCheckpoint(R.string.undo_action_split_ways);
			getDelegator().splitAtNode(node);
			map.invalidate();
		}
	}
	
	/**
	 * Splits a way at a given node
	 * @param way the way to split
	 * @param node the node at which the way should be split
	 */
	public synchronized void performSplit(final Way way, final Node node) {
		// setSelectedNode(node);
		createCheckpoint(R.string.undo_action_split_way);
		getDelegator().splitAtNode(way, node);
		map.invalidate();
	}
	
	/**
	 * Split a closed way, needs two nodes
	 * @param way
	 * @param node1
	 * @param node2
	 */
	public synchronized void performClosedWaySplit(Way way, Node node1, Node node2, boolean createPolygons) {
		createCheckpoint(R.string.undo_action_split_way);
		getDelegator().splitAtNodes(way, node1, node2, createPolygons);
		map.invalidate();
	}

	
	/**
	 * Merge two ways.
	 * Ways must be valid (i.e. have at least two nodes) and mergeable
	 * (i.e. have a common start/end node).
	 *  
	 * @param mergeInto Way to merge the other way into. This way will be kept.
	 * @param mergeFrom Way to merge into the other. This way will be deleted.
	 * @throws OsmIllegalOperationException 
	 */
	public synchronized boolean performMerge(Way mergeInto, Way mergeFrom) throws OsmIllegalOperationException {
		createCheckpoint(R.string.undo_action_merge_ways);
		boolean mergeOK = getDelegator().mergeWays(mergeInto, mergeFrom);
		map.invalidate();
		return mergeOK;
	}
	
	/**
	 * Merge a sorted list of ways
	 * @param sortedWays
	 * @throws OsmIllegalOperationException 
	 */
	public synchronized boolean performMerge(List<OsmElement> sortedWays) throws OsmIllegalOperationException {
		createCheckpoint(R.string.undo_action_merge_ways);
		boolean mergeOK = true;
		Way previousWay = (Way) sortedWays.get(0);
		for (int i=1;i<sortedWays.size();i++) {
			Way nextWay = (Way) sortedWays.get(i);
			if (!getDelegator().mergeWays(previousWay, nextWay)) {
				Log.d("Logic","ways " + previousWay.getDescription() + " and " + nextWay + " caused a merge conflict"); 
				mergeOK = false; // signal that we had a problem somewhere
			}
			if (previousWay.getState() == OsmElement.STATE_DELETED) {
				previousWay = nextWay;
			}
		}
		return mergeOK;
	}
	
	/**
	 * Orthogonalize a way (aka make angles 90°)
	 * @param way
	 */
	public synchronized void performOrthogonalize(Way way) {
		if (way.getNodes().size() < 3) return;
		createCheckpoint(R.string.undo_action_orthogonalize);
		getDelegator().orthogonalizeWay(way);
		map.invalidate();
	}

	/**
	 * Replace node in all ways it is a member of with a new node,
	 * leaving node selected, if it already is. Note: relation memberships are not modified
	 * 
	 * @param node
	 */
	public synchronized void performExtract(final Node node) {
		if (node != null) {
			createCheckpoint(R.string.undo_action_extract_node);
			getDelegator().replaceNode(node);
			map.invalidate();
		}
	}
	
	/**
	 * If any ways are close to the node (within the tolerance), return the way.
	 * @param nodeToJoin
	 * @return
	 */
	public OsmElement findJoinableElement(Node nodeToJoin) {
		OsmElement closestElement = null;
		double closestDistance = Double.MAX_VALUE;
		float jx = lonE7ToX(nodeToJoin.getLon());
		float jy = latE7ToY(nodeToJoin.getLat());
		// start by looking for the closest nodes
		for (Node node : getDelegator().getCurrentStorage().getNodes()) {
			if (node != nodeToJoin) {
				Double distance = clickDistance(node, jx, jy);
				if (distance != null && distance < closestDistance) {
					closestDistance = distance;
					closestElement = node;
				}
			}
		}
		if (closestElement == null) {
			// fall back to closest ways
			for (Way way : getDelegator().getCurrentStorage().getWays()) {
				if (!way.hasNode(nodeToJoin)) {
					List<Node> wayNodes = way.getNodes();
					for (int i = 1, wayNodesSize = wayNodes.size(); i < wayNodesSize; ++i) {
						Node node1 = wayNodes.get(i - 1);
						Node node2 = wayNodes.get(i);
						// TODO only project once per node
						float node1X = lonE7ToX(node1.getLon());
						float node1Y = latE7ToY(node1.getLat());
						float node2X = lonE7ToX( node2.getLon());
						float node2Y = latE7ToY(node2.getLat());
						if (isPositionOnLine(jx, jy, node1X, node1Y, node2X, node2Y)) {
							double distance = GeoMath.getLineDistance(jx, jy, node1X, node1Y, node2X, node2Y);
							if (distance < closestDistance) {
								closestDistance = distance;
								closestElement = way;
							}
						}
					}
				}
			}
		}
		return closestElement;
	}
	
	/**
	 * Join a node to a node or way at the point on the way closest to the node.
	 * @param element Node or Way that the node will be joined to.
	 * @param nodeToJoin Node to be joined to the way.
	 * @throws OsmIllegalOperationException 
	 */
	public synchronized boolean performJoin(OsmElement element, Node nodeToJoin) throws OsmIllegalOperationException {
		boolean mergeOK = true;
		if (element instanceof Node) {
			Node node = (Node)element;
			createCheckpoint(R.string.undo_action_join);
			mergeOK = getDelegator().mergeNodes(node, nodeToJoin);
			map.invalidate();
		}
		else if (element instanceof Way) {
			Way way = (Way)element;
			List<Node> wayNodes = way.getNodes();
			for (int i = 1, wayNodesSize = wayNodes.size(); i < wayNodesSize; ++i) {
				Node node1 = wayNodes.get(i - 1);
				Node node2 = wayNodes.get(i);
				float x = lonE7ToX(nodeToJoin.getLon());
				float y = latE7ToY(nodeToJoin.getLat());
				// TODO only project once per node
				float node1X = lonE7ToX(node1.getLon());
				float node1Y = latE7ToY(node1.getLat());
				float node2X = lonE7ToX(node2.getLon());
				float node2Y = latE7ToY(node2.getLat());
				if (isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y)) {
					float[] p = GeoMath.closestPoint(x, y, node1X, node1Y, node2X, node2Y);
					int lat = yToLatE7(p[1]);
					int lon = xToLonE7(p[0]);
					createCheckpoint(R.string.undo_action_join);
					Node node = null;
					if (node == null && lat == node1.getLat() && lon == node1.getLon()) {
						node = node1;
					}
					if (node == null && lat == node2.getLat() && lon == node2.getLon()) {
						node = node2;
					}
					if (node == null) {
						// move the existing node onto the way and insert it into the way
						getDelegator().updateLatLon(nodeToJoin, lat, lon);
						getDelegator().addNodeToWayAfter(node1, nodeToJoin, way);
					} else {
						// merge node into tgtNode
						mergeOK = getDelegator().mergeNodes(node, nodeToJoin);
					}
					map.invalidate();
					break; // need to leave loop !!!
				}
			}
		}
		return mergeOK;
	}
	
	/**
	 * Unjoin ways joined by the given node.
	 * @param node Node that is joining the ways to be unjoined.
	 */
	public synchronized void performUnjoin(Node node) {
		createCheckpoint(R.string.undo_action_unjoin_ways);
		getDelegator().unjoinWays(node);
		map.invalidate();
	}
	
	/**
	 * Reverse a way
	 * @param way the way to reverse
	 * @return true if reverseWay returned true, implying that tags had to be reversed
	 */
	public synchronized boolean performReverse(Way way) {
		createCheckpoint(R.string.undo_action_reverse_way);
		boolean hadToReverse = getDelegator().reverseWay(way);
		map.invalidate();
		return hadToReverse;
	}
	
	public synchronized void performAppendStart(Way way, Node node) {
		setSelectedNode(node);
		setSelectedWay(way);
		map.invalidate();
	}
	
	public synchronized void performAppendStart(OsmElement element) {
		Way lSelectedWay = null;
		Node lSelectedNode = null;
		
		if (element != null) {
			if (element instanceof Node) {
				lSelectedNode = (Node) element;
				List<Way> ways = getDelegator().getCurrentStorage().getWays(lSelectedNode);
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
		performAppendStart(lSelectedWay, lSelectedNode);
	}
	
	public synchronized void performAppendAppend(final float x, final float y) throws OsmIllegalOperationException {
		Log.d("Logic","performAppendAppend");
		createCheckpoint(R.string.undo_action_append);
		Node lSelectedNode = getSelectedNode();
		Way lSelectedWay = getSelectedWay();

		Node node = getClickedNodeOrCreatedWayNode(x, y);
		if (node == lSelectedNode) {
			lSelectedNode = null;
			lSelectedWay = null;
		} else {
			if (node == null) {
				int lat = yToLatE7(y);
				int lon = xToLonE7(x);
				node = getDelegator().getFactory().createNodeWithNewId(lat, lon);
				getDelegator().insertElementSafe(node);
				if (!getDelegator().isInDownload(lat, lon)) {
					// warning toast
					Toast.makeText(Application.mainActivity, R.string.toast_outside_of_download, Toast.LENGTH_SHORT).show();
				}
			}
			try {
				getDelegator().appendNodeToWay(lSelectedNode, node, lSelectedWay);
			} catch (OsmIllegalOperationException e) {
				getDelegator().removeNode(node);
				throw new OsmIllegalOperationException(e);
			}
			lSelectedNode = node;
		}
		setSelectedNode(lSelectedNode);
		setSelectedWay(lSelectedWay);
		map.invalidate();
	}

	/**
	 * Tries to locate the selected node. If x,y lays on a way, a new node at this location will be created, stored in
	 * storage and returned.
	 * 
	 * @param x the x screen coordinate
	 * @param y the y screen coordinate
	 * @return the selected node or the created node, if x,y lays on a way. Null if any node or way was selected.
	 * @throws OsmIllegalOperationException 
	 */
	private synchronized Node getClickedNodeOrCreatedWayNode(final float x, final float y) throws OsmIllegalOperationException {
		Node node = getClickedNode(x, y);
		if (node != null) {
			return node;
		}
		Node savedNode1 = null;
		Node savedNode2 = null;
		Way savedWay = null;
		double savedDistance = Double.MAX_VALUE;
		//create a new node on a way
		for (Way way : getDelegator().getCurrentStorage().getWays()) {
			List<Node> wayNodes = way.getNodes();
			for (int k = 1, wayNodesSize = wayNodes.size(); k < wayNodesSize; ++k) {
				Node node1 = wayNodes.get(k - 1);
				Node node2 = wayNodes.get(k);
				// TODO only project once per node
				float node1X = lonE7ToX(node1.getLon());
				float node1Y = latE7ToY(node1.getLat());
				float node2X = lonE7ToX(node2.getLon());
				float node2Y = latE7ToY(node2.getLat());

				if (isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y)) {
					double distance = GeoMath.getLineDistance(x, y, node1X, node1Y, node2X, node2Y);
					if ((savedNode1 == null && savedNode2 == null) || distance < savedDistance) {
						savedNode1 = node1;
						savedNode2 = node2;
						savedDistance = distance;
						savedWay = way;
					}
				}
			}
		}
		// way found that is in toleance range
		if (savedNode1 != null && savedNode2 != null) {		
			node = createNodeOnWay(savedNode1, savedNode2, x, y);
			if (node != null) {
				getDelegator().insertElementSafe(node);
				try {
					getDelegator().addNodeToWayAfter(savedNode1, node, savedWay);
				} catch (OsmIllegalOperationException e) {
					getDelegator().removeNode(node);
					throw new OsmIllegalOperationException(e);
				}
			}	
		}
		return node;
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
	private synchronized Node createNodeOnWay(final Node node1, final Node node2, final float x, final float y) {
		//Nodes have to be converted to screen-coordinates, due to a better tolerance-check.
		float node1X = lonE7ToX(node1.getLon());
		float node1Y = latE7ToY(node1.getLat());
		float node2X = lonE7ToX(node2.getLon());
		float node2Y = latE7ToY(node2.getLat());

		//At first, we check if the x,y is in the bounding box clamping by node1 and node2.
		if (isPositionOnLine(x, y, node1X, node1Y, node2X, node2Y)) {
			float[] p = GeoMath.closestPoint(x, y, node1X, node1Y, node2X, node2Y);
			int lat = yToLatE7(p[1]);
			int lon = xToLonE7(p[0]);
			Node node = getDelegator().getFactory().createNodeWithNewId(lat, lon);
			return node;
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
		float tolerance = Profile.getCurrent().wayToleranceValue / 2f;
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

		try {
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
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Loads the area defined by mapBox from the OSM-Server.
	 * 
	 * @param mapBox Box defining the area to be loaded.
	 * @param add if true add this data to existing
	 * @param postLoadHandler TODO
	 * @param auto download is being done automatically, try not mess up/move the display
	 */
	public synchronized void downloadBox(final BoundingBox mapBox, final boolean add, final PostAsyncActionHandler postLoadHandler) {
		try {
			mapBox.makeValidForApi();
		} catch (OsmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // TODO remove this? and replace with better error messaging
		
		final PostMergeHandler postMerge =  new PostMergeHandler(){

			@Override
			public void handler(OsmElement e) {
				e.hasProblem(Application.mainActivity);
			}
		};
		
		new AsyncTask<Boolean, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.showDialog(DialogFactory.PROGRESS_LOADING);
			}
			
			@Override
			protected Integer doInBackground(Boolean... arg) {
				int result = 0;
				try {
					Server server = prefs.getServer();
					server.getCapabilities();
					if (!(server.apiAvailable() && server.readableDB())) {
						return DialogFactory.API_OFFLINE;
					}
					final OsmParser osmParser = new OsmParser();
					final InputStream in = prefs.getServer().getStreamForBox(mapBox);
					try {
						osmParser.start(in);
						if (arg[0]) { // incremental load
							if (!getDelegator().mergeData(osmParser.getStorage(),postMerge)) {
								result = DialogFactory.DATA_CONFLICT;
							} else {
								if (mapBox != null) {
									// if we are simply expanding the area no need keep the old bounding boxes
									List<BoundingBox> origBbs = getDelegator().getBoundingBoxes();
									List<BoundingBox> bbs = new ArrayList<BoundingBox>(origBbs);
									for (BoundingBox bb:bbs) {
										if (mapBox.contains(bb)) {
											origBbs.remove(bb);
										}
									}
									getDelegator().addBoundingBox(mapBox);
								}
							}
						} else { // replace data with new download
							getDelegator().reset(false);
							getDelegator().setCurrentStorage(osmParser.getStorage()); // this sets dirty flag
							if (mapBox != null) {
								Log.d("Logic","setting original bbox");
								getDelegator().setOriginalBox(mapBox);
							}
						}
						viewBox.setBorders(mapBox != null ? mapBox : getDelegator().getLastBox()); // set to current or previous
					} finally {
						SavingHelper.close(in);
					}
				} catch (SAXException e) {
					Log.e("Vespucci", "Problem parsing", e);
					Exception ce = e.getException();
					if ((ce instanceof StorageException) && ((StorageException)ce).getCode() == StorageException.OOM) {
						result = DialogFactory.OUT_OF_MEMORY;
					} else {
						result = DialogFactory.INVALID_DATA_RECEIVED;
					}
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				} catch (ParserConfigurationException e) {
					// crash and burn
					// TODO this seems to happen when the API call returns text from a proxy or similar intermediate network device... need to display what we actually got
					Log.e("Vespucci", "Problem parsing", e);
					result = DialogFactory.INVALID_DATA_RECEIVED;
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				} catch (OsmServerException e) {
					result = e.getErrorCode();
					Log.e("Vespucci", "Problem downloading", e);
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				} catch (IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e("Vespucci", "Problem downloading", e);
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {	
				try {
					Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_LOADING);
				} catch (IllegalArgumentException e) {
					// Avoid crash if dialog is already dismissed
					Log.d("Logic", "", e);
				}

				View map = Application.mainActivity.getCurrentFocus();
				try {
					viewBox.setRatio((float)map.getWidth() / (float)map.getHeight());
				} catch (OsmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (result != 0) {
					if (result == DialogFactory.OUT_OF_MEMORY) {
						System.gc();
						if (getDelegator().isDirty()) {
							result = DialogFactory.OUT_OF_MEMORY_DIRTY;
						}
					}	
					try {
						if (!Application.mainActivity.isFinishing()) {
							Application.mainActivity.showDialog(result);
						}
					} catch (Exception ex) { // now and then this seems to throw a WindowManager.BadTokenException, however report, don't crash
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(ex);
					}
				} else {
					if (postLoadHandler != null) {
						postLoadHandler.execute();
					}
				}
				Profile.updateStrokes(strokeWidth(mapBox.getWidth()));
				map.invalidate();

				UndoStorage.updateIcon();
			}	
		}.execute(add);
	}
	
	/**
	 * Loads the area defined by mapBox from the OSM-Server. Static version for auto download
	 * FIXME try to reduce the code duplication here
	 * @param context TODO
	 * @param mapBox Box defining the area to be loaded.
	 * @param add if true add this data to existing
	 * @param auto download is being done automatically, try not mess up/move the display
	 */
	public synchronized static void autoDownloadBox(final Context context, final Server server, final BoundingBox mapBox) {
		try {
			mapBox.makeValidForApi();
		} catch (OsmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // TODO remove this? and replace with better error messaging
		
		final PostMergeHandler postMerge =  new PostMergeHandler(){

			@Override
			public void handler(OsmElement e) {
				e.hasProblem(context);
			}
		};
		
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
			}
			
			@Override
			protected Integer doInBackground(Void... arg) {
				int result = 0;
				try {
					final OsmParser osmParser = new OsmParser();
					final InputStream in = server.getStreamForBox(mapBox);
					try {
						osmParser.start(in);
						if (!getDelegator().mergeData(osmParser.getStorage(),postMerge)) {
							result = DialogFactory.DATA_CONFLICT;
						} else {
							if (mapBox != null) {
								// if we are simply expanding the area no need keep the old bounding boxes
								List<BoundingBox> origBbs = getDelegator().getBoundingBoxes();
								if ( origBbs.size() == 1) { // replace original BB if still present
									if (getDelegator().isEmpty()) {
										origBbs.clear();
									}
								}
								List<BoundingBox> bbs = new ArrayList<BoundingBox>(origBbs);
								for (BoundingBox bb:bbs) {
									if (mapBox.contains(bb)) {
										origBbs.remove(bb);
									}
								}
								getDelegator().addBoundingBox(mapBox);
							}
						}
					} finally {
						SavingHelper.close(in);
					}
				} catch (SAXException e) {
					Log.e("Vespucci", "Problem parsing", e);
					Exception ce = e.getException();
					if ((ce instanceof StorageException) && ((StorageException)ce).getCode() == StorageException.OOM) {
						result = DialogFactory.OUT_OF_MEMORY;
					} else {
						result = DialogFactory.INVALID_DATA_RECEIVED;
					}
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				} catch (ParserConfigurationException e) {
					// crash and burn
					// TODO this seems to happen when the API call returns text from a proxy or similar intermediate network device... need to display what we actually got
					Log.e("Vespucci", "Problem parsing", e);
					result = DialogFactory.INVALID_DATA_RECEIVED;
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				} catch (OsmServerException e) {
					result = e.getErrorCode();
					Log.e("Vespucci", "Problem downloading", e);
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				} catch (IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e("Vespucci", "Problem downloading", e);
					if (getDelegator().getBoundingBoxes().contains(mapBox)) { // remove if download failed
						getDelegator().deleteBoundingBox(mapBox);
					}
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				UndoStorage.updateIcon();
			}
		}.execute();
	}


	/**
	 * Calls the actual downloadBox function using the current map view as the
	 * bounding box for the download.
	 * 
	 * @param add add if true add this data to existing
	 * @see #downloadBox(Main, BoundingBox, boolean)
	 */
	void downloadCurrent(boolean add) {
		Log.d("Logic","viewBox: " + viewBox.getBottom() + " " + viewBox.getLeft() + " " + viewBox.getTop() + " " + viewBox.getRight());
		downloadBox(viewBox.copy(),add, null);
	}
	
	/**
	 * Re-downloads the same area as last time
	 * 
	 * @see #downloadBox(Main, BoundingBox, boolean)
	 */
	void downloadLast() {
		getDelegator().reset(false);
		for (BoundingBox box:getDelegator().getBoundingBoxes()) {
			if (box != null && box.isValidForApi()) downloadBox(box, true, null);
		}
	}

	/**
	 * Return a single element from the API
	 * 
	 * @param type
	 * @param id
	 * @return
	 */
	 synchronized OsmElement downloadElement(final String type, final long id) {
		
		class MyTask extends AsyncTask<Void, Void, OsmElement> {
			int result = 0;
			
			@Override
			protected void onPreExecute() {
	
			}
			
			@Override
			protected OsmElement doInBackground(Void... arg) {
				OsmElement element = null;
				try {
					final OsmParser osmParser = new OsmParser();
					final InputStream in = prefs.getServer().getStreamForElement(null, type, id);
					try {
						osmParser.start(in);
						element = osmParser.getStorage().getOsmElement(type, id);
					} finally {
						SavingHelper.close(in);
					}
				} catch (SAXException e) {
					Log.e("Vespucci", "Problem parsing", e);
					Exception ce = e.getException();
					if ((ce instanceof StorageException) && ((StorageException)ce).getCode() == StorageException.OOM) {
						result = DialogFactory.OUT_OF_MEMORY;
					} else {
						result = DialogFactory.INVALID_DATA_RECEIVED;
					}
				} catch (ParserConfigurationException e) {
					// crash and burn
					// TODO this seems to happen when the API call returns text from a proxy or similar intermediate network device... need to display what we actually got
					Log.e("Vespucci", "Problem parsing", e);
					result = DialogFactory.INVALID_DATA_RECEIVED;
				} catch (OsmServerException e) {
					Log.e("Vespucci", "Problem downloading", e);
				} catch (IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e("Vespucci", "Problem downloading", e);
				}
				return element;
			}
			
			@Override
			protected void onPostExecute(OsmElement result) {
				// potentially do something if there is an error
			}
			
		};
		MyTask loader = new MyTask();
		loader.execute();
		
		try {
			return loader.get(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return null;
		} catch (ExecutionException e) {
			return null;
		} catch (TimeoutException e) {
			return null;
		}
	}
	
	/**
	 * Return multiple elements of the same type from the API and merge them in to our data
	 * Since this doesn't return way nodes this method probably doesn't make sense
	 * 
	 * @param type
	 * @param id
	 * @return
	 */
//	void downloadElements(final String type, long[] ids) {
//		
//		class MyTask extends AsyncTask<Void, Void, Integer> {
//			int result = 0;
//			
//			@Override
//			protected void onPreExecute() {
//				Application.mainActivity.showDialog(DialogFactory.PROGRESS_LOADING);
//			}
//			
//			@Override
//			protected Integer doInBackground(Void... arg) {
//				try {
//					final OsmParser osmParser = new OsmParser();
//					final InputStream in = prefs.getServer().getStreamForElements(type, ids);
//					try {
//						osmParser.start(in);
//						if (!getDelegator().mergeData(osmParser.getStorage())) {
//							result = DialogFactory.DATA_CONFLICT;
//						} 
//					} finally {
//						SavingHelper.close(in);
//					}
//				} catch (SAXException e) {
//					Log.e("Vespucci", "Problem parsing", e);
//					Exception ce = e.getException();
//					if ((ce instanceof StorageException) && ((StorageException)ce).getCode() == StorageException.OOM) {
//						result = DialogFactory.OUT_OF_MEMORY;
//					} else {
//						result = DialogFactory.INVALID_DATA_RECEIVED;
//					}
//				} catch (ParserConfigurationException e) {
//					// crash and burn
//					// TODO this seems to happen when the API call returns text from a proxy or similar intermediate network device... need to display what we actually got
//					Log.e("Vespucci", "Problem parsing", e);
//					result = DialogFactory.INVALID_DATA_RECEIVED;
//				} catch (OsmServerException e) {
//					Log.e("Vespucci", "Problem downloading", e);
//				} catch (IOException e) {
//					result = DialogFactory.NO_CONNECTION;
//					Log.e("Vespucci", "Problem downloading", e);
//				}
//				return result;
//			}
//			
//			@Override
//			protected void onPostExecute(Integer result) {
//
//				try {
//					Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_LOADING);
//				} catch (IllegalArgumentException e) {
//					// Avoid crash if dialog is already dismissed
//					Log.d("Logic", "", e);
//				}
//
//				if (result != 0) {
//					if (result == DialogFactory.OUT_OF_MEMORY) {
//						System.gc();
//						if (getDelegator().isDirty()) {
//							result = DialogFactory.OUT_OF_MEMORY_DIRTY;
//						}
//					}	
//					try {
//						if (!Application.mainActivity.isFinishing()) {
//							Application.mainActivity.showDialog(result);
//						}
//					} catch (Exception ex) { // now and then this seems to throw a WindowManager.BadTokenException, however report, don't crash
//						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
//						ACRA.getErrorReporter().handleException(ex);
//					}
//				}
//				map.invalidate();
//
//				UndoStorage.updateIcon();
//			}
//			
//		};
//		MyTask loader = new MyTask();
//		loader.execute();
//	}
	
	/**
	 * Update a single element from the API
	 * 
	 * @param type
	 * @param id
	 */
	 synchronized int updateElement(final String type, final long id) {
		class MyTask extends AsyncTask<Void, Void, Integer> {
			@Override
			protected void onPreExecute() {
			}
			
			@Override
			protected Integer doInBackground(Void... arg) {
				int result = 0;
				try {
					final OsmParser osmParser = new OsmParser();
					if (!type.equals(Node.NAME)) {
						final InputStream in = prefs.getServer().getStreamForElement("full", type, id);
						try {
							osmParser.start(in);
						} finally {
							SavingHelper.close(in);
						}
					} else {
						// TODO this currently does not retrieve ways the updated node may be a member of
						InputStream in = prefs.getServer().getStreamForElement(null, type, id);
						try {
							osmParser.start(in);
						} finally {
							SavingHelper.close(in);
						}
						in = prefs.getServer().getStreamForElement("relations", type, id);
						try {
							osmParser.start(in);
						} finally {
							SavingHelper.close(in);
						}
					}
					if (!getDelegator().mergeData(osmParser.getStorage(),null)) { // FIXME need to check if providing a handler makes sense here
						result = DialogFactory.DATA_CONFLICT;
					} 
				} catch (SAXException e) {
					Log.e("Vespucci", "Problem parsing", e);
					Exception ce = e.getException();
					if ((ce instanceof StorageException) && ((StorageException)ce).getCode() == StorageException.OOM) {
						result = DialogFactory.OUT_OF_MEMORY;
					} else {
						result = DialogFactory.INVALID_DATA_RECEIVED;
					}
				} catch (ParserConfigurationException e) {
					// crash and burn
					// TODO this seems to happen when the API call returns text from a proxy or similar intermediate network device... need to display what we actually got
					Log.e("Vespucci", "Problem parsing", e);
					result = DialogFactory.INVALID_DATA_RECEIVED;
				} catch (OsmServerException e) {
					Log.e("Vespucci", "Problem downloading", e);
				} catch (IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e("Vespucci", "Problem downloading", e);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				// potentially do something if there is an error
			}
			
		};
		MyTask loader = new MyTask();
		loader.execute();
		
		try {
			return loader.get(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return -1;
		} catch (ExecutionException e) {
			return -1;
		} catch (TimeoutException e) {
			return -1;
		}
	}
	

	/**
	 * Element is deleted on server, delete locally but don't upload
	 * A bit iffy because of memberships in other objects
	 * 
	 * @param e
	 */
	public  synchronized void updateToDeleted(OsmElement e) {
		createCheckpoint(R.string.undo_action_fix_conflict);
		if (e.getName().equals(Node.NAME)) {
			getDelegator().removeNode((Node)e);
		} else if (e.getName().equals(Way.NAME)) {
			getDelegator().removeWay((Way)e);
		} else if (e.getName().equals(Relation.NAME)) {
			getDelegator().removeRelation((Relation)e);
		}
		getDelegator().removeFromUpload(e);
		map.invalidate();		
	}
	
	/**
	 * Read a file in (J)OSM format from device
	 * 
	 * @param fileName
	 * @param add unused currently
	 * @throws FileNotFoundException 
	 */
	 synchronized void readOsmFile(final Uri uri, boolean add) throws FileNotFoundException {
	
		final InputStream is;
		
		if (uri.getScheme().equals("file")) {
			is = new FileInputStream(new File(uri.getPath()));
		} else {
			ContentResolver cr = Application.mainActivity.getContentResolver();
			is = cr.openInputStream(uri);
		}
		
		new AsyncTask<Boolean, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.showDialog(DialogFactory.PROGRESS_LOADING);
			}
			
			@Override
			protected Integer doInBackground(Boolean... arg) {
				int result = 0;
				try {
					final OsmParser osmParser = new OsmParser();
					final InputStream in = new BufferedInputStream(is);
;
					try {
						osmParser.start(in);
						
						getDelegator().reset(false);
						getDelegator().setCurrentStorage(osmParser.getStorage()); // this sets dirty flag
						getDelegator().fixupApiStorage();
						
						viewBox.setBorders(getDelegator().getLastBox()); // set to current or previous
					} finally {
						SavingHelper.close(in);
					}
				} catch (SAXException e) {
					Log.e("Vespucci", "Problem parsing", e);
					Exception ce = e.getException();
					if ((ce instanceof StorageException) && ((StorageException)ce).getCode() == StorageException.OOM) {
						result = DialogFactory.OUT_OF_MEMORY;
					} else {
						result = DialogFactory.INVALID_DATA_RECEIVED;
					}
				} catch (ParserConfigurationException e) {
					// crash and burn
					// TODO this seems to happen when the API call returns text from a proxy or similar intermediate network device... need to display what we actually got
					Log.e("Vespucci", "Problem parsing", e);
					result = DialogFactory.INVALID_DATA_RECEIVED;
				} catch (IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e("Vespucci", "Problem reading", e);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				try {
					Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_LOADING);
				} catch (IllegalArgumentException e) {
					 // Avoid crash if dialog is already dismissed
					Log.d("Logic", "", e);
				}
				View map = Application.mainActivity.getCurrentFocus();
				try {
					viewBox.setRatio((float)map.getWidth() / (float)map.getHeight());
				} catch (OsmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (result != 0) {
					if (result == DialogFactory.OUT_OF_MEMORY) {
						System.gc();
						if (getDelegator().isDirty()) {
							result = DialogFactory.OUT_OF_MEMORY_DIRTY;
						}
					}
					try {
						if (!Application.mainActivity.isFinishing()) {
							Application.mainActivity.showDialog(result);
						}
					} catch (Exception ex) { // now and then this seems to throw a WindowManager.BadTokenException, however report, don't crash
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(ex);
					}
				}
				Profile.updateStrokes(strokeWidth(viewBox.getWidth()));
				map.invalidate();
				UndoStorage.updateIcon();
			}
			
		}.execute(add);
	}

	/**
	 * Write data to a file in (J)OSM compatible format, 
	 * if fileName contains directories these are created, otherwise it is stored in the standard public dir
	 * 
	 * @param fileName
	 */
	public void writeOsmFile(final String fileName) {
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.showDialog(DialogFactory.PROGRESS_SAVING);
			}
			
			@Override
			protected Integer doInBackground(Void... arg) {
				int result = 0;
				try {
					File outfile = new File(fileName);
					String parent = outfile.getParent();
					if (parent == null) { // no directory specified, save to standard location
						outfile = new File(FileUtil.getPublicDirectory(), fileName);
					} else { // ensure directory exists
						File outdir = new File(parent);
						outdir.mkdirs();
						if (!outdir.isDirectory()) {
							throw new IOException("Unable to create directory " + outdir.getPath());
						}
					}
					Log.d("Logic","Saving to " + outfile.getPath());
					final OutputStream out = new BufferedOutputStream(new FileOutputStream(outfile));
					try {
						getDelegator().save(out);
					} catch (IllegalArgumentException e) {
						result = DialogFactory.FILE_WRITE_FAILED;
						Log.e("Logic", "Problem writing", e);
					} catch (IllegalStateException e) {
						result = DialogFactory.FILE_WRITE_FAILED;
						Log.e("Logic", "Problem writing", e);
					} catch (XmlPullParserException e) {
						result = DialogFactory.FILE_WRITE_FAILED;
						Log.e("Logic", "Problem writing", e);
					} finally {
						SavingHelper.close(out);
					}
				} catch (IOException e) {
					result = DialogFactory.FILE_WRITE_FAILED;
					Log.e("Logic", "Problem writing", e);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				try {
					Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_SAVING);
				} catch (IllegalArgumentException e) {
					 // Avoid crash if dialog is already dismissed
					Log.d("Logic", "", e);
				}
				View map = Application.mainActivity.getCurrentFocus();
				try {
					viewBox.setRatio((float)map.getWidth() / (float)map.getHeight());
				} catch (OsmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (result != 0) {
					if (result == DialogFactory.OUT_OF_MEMORY) {
						System.gc();
						if (getDelegator().isDirty()) {
							result = DialogFactory.OUT_OF_MEMORY_DIRTY;
						}
					}
					if (!Application.mainActivity.isFinishing()) {
						Application.mainActivity.showDialog(result);
					}
				}
			}
			
		}.execute();
	}

	
	/**
	 * Saves to a file (synchronously)
	 */
	synchronized void save() {
		try {
			getDelegator().writeToFile(Application.mainActivity);
			Application.getBugStorage().writeToFile(Application.mainActivity);
		} catch (IOException e) {
			Log.e("Vespucci", "Problem saving", e);
		}
	}
	
	
	/**
	 * Saves to a file (asynchronously)
	 */
	void saveAsync() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				save();
				return null;
			}
		}.execute();
	}
	
	/**
	 * Saves the current editing state (selected objects, editing mode, etc) to file.
	 */
	void saveEditingState() {
		OpenStreetMapTileServer osmts = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		EditState editState = new EditState(mode, selectedNodes, selectedWays, selectedRelations, selectedBug, osmts, 
				Application.mainActivity.getShowGPS(), Application.mainActivity.getAutoDownload(),
				Application.mainActivity.getBugAutoDownload(),Application.mainActivity.getImageFileName(),
				viewBox);
		new SavingHelper<EditState>().save(EDITSTATE_FILENAME, editState, false);	
	}
	
	/**
	 * Loads the current editing state (selected objects, editing mode, etc) from file.
	 */
	void loadEditingState() {
		EditState editState = new SavingHelper<EditState>().load(EDITSTATE_FILENAME, false);
		if(editState != null) { // 
			editState.setSelected(this);
			editState.setOffset(map.getOpenStreetMapTilesOverlay().getRendererInfo());
			editState.setMiscState(Application.mainActivity);
			editState.setViewBox(this,map);
		}
	}

	/**
	 * Loads data from a file in the background.
	 * 
	 * @param context 
	 */
	void loadFromFile(Context context, final PostAsyncActionHandler postLoad) {
		
		final int READ_FAILED = 0;
		final int READ_OK = 1;
		final int READ_BACKUP = 2;

		Context[] c = {context};
		AsyncTask<Context, Void, Integer> loader = new AsyncTask<Context, Void, Integer>() {
					
			Context context;
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.showDialog(DialogFactory.PROGRESS_LOADING);
				Log.d("Logic", "loadFromFile onPreExecute");
			}
			
			@Override
			protected Integer doInBackground(Context... c) {
				this.context = c[0];
				if (getDelegator().readFromFile()) {
					viewBox.setBorders(getDelegator().getLastBox());
					return Integer.valueOf(READ_OK);
				} else if (getDelegator().readFromFile(StorageDelegator.FILENAME + ".backup")) {
					getDelegator().dirty(); // we need to overwrite the saved state asap
					viewBox.setBorders(getDelegator().getLastBox());
					return Integer.valueOf(READ_BACKUP);
				}
				return Integer.valueOf(READ_FAILED);
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				Log.d("Logic", "loadFromFile onPostExecute");
				try {
					Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_LOADING);
				} catch (IllegalArgumentException e) {
					 // Avoid crash if dialog is already dismissed
					Log.d("Logic", "", e);
				}
				if (result.intValue() != READ_FAILED) {
					Log.d("Logic", "loadfromFile: File read correctly");
					View map = Application.mainActivity.getCurrentFocus();
					
					try {
						viewBox.setRatio((float)map.getWidth() / (float)map.getHeight());
					} catch (Exception e) {
						// invalid dimensions of similar error
						try {
							viewBox.setBorders(new BoundingBox(-180.0,-GeoMath.MAX_LAT,180.0,GeoMath.MAX_LAT));
						} catch (OsmException e1) {
							// Can't happen?
							e1.printStackTrace();
						}
					}
					Profile.updateStrokes(STROKE_FACTOR / viewBox.getWidth());
					loadEditingState();
					
					if (postLoad != null) {
						postLoad.execute();
					}
					map.invalidate();
					UndoStorage.updateIcon();
					if (result.intValue() == READ_BACKUP) { 
						Toast.makeText(Application.mainActivity, R.string.toast_used_backup, Toast.LENGTH_LONG).show();
					}
				}
				else {
					Log.d("Logic", "loadfromFile: File read failed");
					Intent intent = new Intent(context, BoxPicker.class);
					Application.mainActivity.startActivityForResult(intent, Main.REQUEST_BOUNDINGBOX);
					Toast.makeText(Application.mainActivity, R.string.toast_state_file_failed, Toast.LENGTH_LONG).show();
				}
			}
		};
		loader.execute(c);
	}
	
	/**
	 * Loads data from a file in the background.
	 * 
	 * @param context 
	 */
	void loadBugsFromFile(Context context, final PostAsyncActionHandler postLoad) {
		
		final int READ_FAILED = 0;
		final int READ_OK = 1;
		final int READ_BACKUP = 2;

		Context[] c = {context};
		AsyncTask<Context, Void, Integer> loader = new AsyncTask<Context, Void, Integer>() {
					
			Context context;
			
			@Override
			protected void onPreExecute() {
				Log.d("Logic", "loadBugsFromFile onPreExecute");
			}
			
			@Override
			protected Integer doInBackground(Context... c) {
				this.context = c[0];
				if (Application.getBugStorage().readFromFile()) {
					// viewBox.setBorders(getDelegator().getLastBox());
					return Integer.valueOf(READ_OK);
				} 
				return Integer.valueOf(READ_FAILED);
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				Log.d("Logic", "loadBugsFromFile onPostExecute");
				if (result.intValue() != READ_FAILED) {
					Log.d("Logic", "loadBugsfromFile: File read correctly");
					View map = Application.mainActivity.getCurrentFocus();
					
					// FIXME if no bbox exists from data, ty to use one from bugs
					if (postLoad != null) {
						postLoad.execute();
					}
					// map.invalidate();
					if (result.intValue() == READ_BACKUP) { 
						Toast.makeText(context, R.string.toast_used_bug_backup, Toast.LENGTH_LONG).show();
					}
				}
				else {
					Log.d("Logic", "loadBugsfromFile: File read failed");
				}
			}
		};
		loader.execute(c);
	}
	
	/**
	 * Loads data from a file
	 * 
	 */
	void syncLoadFromFile() {

		final int READ_FAILED = 0;
		final int READ_OK = 1;
		final int READ_BACKUP = 2;

		int result = READ_FAILED;

		Application.mainActivity.showDialog(DialogFactory.PROGRESS_LOADING);

		if (getDelegator().readFromFile()) {
			viewBox.setBorders(getDelegator().getLastBox());
			result = READ_OK;
		} 

		try {
			Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_LOADING);
		} catch (IllegalArgumentException e) {
			// Avoid crash if dialog is already dismissed
			Log.d("Logic", "", e);
		}
		if (result != READ_FAILED) {
			Log.d("Logic", "syncLoadfromFile: File read correctly");
			View map = Application.mainActivity.getCurrentFocus();

			try {
				viewBox.setRatio((float)map.getWidth() / (float)map.getHeight());
			} catch (Exception e) {
				// invalid dimensions of similar error
				try {
					viewBox.setBorders(new BoundingBox(-180.0,-GeoMath.MAX_LAT,180.0,GeoMath.MAX_LAT));
				} catch (OsmException e1) {
					// Can't happen?
					e1.printStackTrace();
				}
			}
			Profile.updateStrokes(STROKE_FACTOR / viewBox.getWidth());
			loadEditingState();
			map.invalidate();
			UndoStorage.updateIcon();
			if (result == READ_BACKUP) { 
				Toast.makeText(Application.mainActivity, R.string.toast_used_backup, Toast.LENGTH_LONG).show();
			}
		}
		else {
			Log.d("Logic", "syncLoadfromFile: File read failed");
			Toast.makeText(Application.mainActivity, R.string.toast_state_file_failed, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * A small class to store the result returned from the OSM server after
	 * trying to upload changes. The response includes things like the HTTP
	 * response code, conflict information, etc.
	 * 
	 * Return not only the error code, but the element involved
	 * 
	 * @author simon
	 */
	public class UploadResult {
		public int error = 0;
		public int httpError = 0;
		public String elementType;
		public long osmId;
		public String message;
	}

	/**
	 * Uploads to the server in the background.
	 * 
	 * @param comment Changeset comment.
	 * @param source The changeset source tag to add.
	 * @param closeChangeset Whether to close the changeset after upload or not.
	 */
	public void upload(final String comment, final String source, final boolean closeChangeset) {
		final Server server = prefs.getServer();
		new AsyncTask<Void, Void, UploadResult>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.setSupportProgressBarIndeterminateVisibility(true);
				getDelegator().clearUndo();
			}
			
			@Override
			protected UploadResult doInBackground(Void... params) {
				UploadResult result = new UploadResult();
				try {
					server.getCapabilities(); // update status
					if (!(server.apiAvailable() && server.writableDB())) {
						result.error =  DialogFactory.API_OFFLINE;
						return result;
					}
					getDelegator().uploadToServer(server, comment, source, closeChangeset);
				} catch (final MalformedURLException e) {
					Log.e(DEBUG_TAG, "", e);
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(e);
				} catch (final ProtocolException e) {
					Log.e(DEBUG_TAG, "", e);
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(e);
				} catch (final OsmServerException e) {
					result.httpError = e.getErrorCode();
					result.message = e.getMessage();
					switch (e.getErrorCode()) {
					case HttpStatus.SC_UNAUTHORIZED:
					case HttpStatus.SC_FORBIDDEN:
						result.error = DialogFactory.WRONG_LOGIN;
						break;
					case HttpStatus.SC_CONFLICT:
					case HttpStatus.SC_PRECONDITION_FAILED:
						result.error = DialogFactory.UPLOAD_CONFLICT;
						result.elementType = e.getElementType();
						result.osmId = e.getElementId();
						break;
					case HttpStatus.SC_BAD_REQUEST:
					case HttpStatus.SC_NOT_FOUND:
					case HttpStatus.SC_GONE:
					case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					case HttpStatus.SC_BAD_GATEWAY:
					case HttpStatus.SC_SERVICE_UNAVAILABLE:
						result.error = DialogFactory.UPLOAD_PROBLEM;
						break;
					//TODO: implement other state handling
					default:
						Log.e(DEBUG_TAG, "", e);
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(e);
						break;
					}
				} catch (final IOException e) {
					result.error = DialogFactory.NO_CONNECTION;
					Log.e(DEBUG_TAG, "", e);
				} catch (final NullPointerException e) {
					Log.e(DEBUG_TAG, "", e);
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(e);
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(UploadResult result) {
				Application.mainActivity.setSupportProgressBarIndeterminateVisibility(false);
				if (result.error == 0) {
					save(); // save now to avoid problems if it doesn't succeed later on, FIXME async or sync
					Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_upload_success, Toast.LENGTH_SHORT).show();
					Application.mainActivity.triggerMenuInvalidation();
				}
				getDelegator().clearUndo();
				Application.mainActivity.getCurrentFocus().invalidate();
				if (!Application.mainActivity.isFinishing()) {
					if (result.error == DialogFactory.UPLOAD_CONFLICT) {
						DialogFactory.createUploadConflictDialog(Application.mainActivity, result).show();
					} else if (result.error != 0) {
						Application.mainActivity.showDialog(result.error);
					}
				}
			}
			
		}.execute();
	}
	
	

	/**
	 * Uploads a GPS track to the server.
	 * 
	 * @param track the track to upload
	 * @param description a description of the track sent to the server
	 * @param tags the tags to apply to the GPS track (comma delimeted)
	 * @param visibility the track visibility, one of the following: private, public, trackable, identifiable
	 */
	public void uploadTrack(final Track track, final String description, final String tags, final Visibility visibility) {
		final Server server = prefs.getServer();
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
				Application.mainActivity.setSupportProgressBarIndeterminateVisibility(true);
			}
			
			@Override
			protected Integer doInBackground(Void... params) {
				int result = 0;
				try {
					server.uploadTrack(track, description, tags, visibility);
				} catch (final MalformedURLException e) {
					Log.e(DEBUG_TAG, "", e);
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(e);
				} catch (final ProtocolException e) {
					Log.e(DEBUG_TAG, "", e);
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(e);
				} catch (final OsmServerException e) {
					switch (e.getErrorCode()) {
					case HttpStatus.SC_UNAUTHORIZED:
						result = DialogFactory.WRONG_LOGIN;
						break;
					case HttpStatus.SC_BAD_REQUEST:
					case HttpStatus.SC_PRECONDITION_FAILED:
					case HttpStatus.SC_CONFLICT:
						result = DialogFactory.UPLOAD_PROBLEM;
						break;
					case HttpStatus.SC_NOT_FOUND:
					case HttpStatus.SC_GONE:
					case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					case HttpStatus.SC_BAD_GATEWAY:
					case HttpStatus.SC_SERVICE_UNAVAILABLE:
						result = DialogFactory.UPLOAD_PROBLEM;
						break;
					//TODO: implement other state handling
					default:
						Log.e(DEBUG_TAG, "", e);
						ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
						ACRA.getErrorReporter().handleException(e);
						break;
					}
				} catch (final IOException e) {
					result = DialogFactory.NO_CONNECTION;
					Log.e(DEBUG_TAG, "", e);
				} catch (final NullPointerException e) {
					Log.e(DEBUG_TAG, "", e);
					ACRA.getErrorReporter().putCustomData("STATUS","NOCRASH");
					ACRA.getErrorReporter().handleException(e);
				} catch (IllegalArgumentException e) {
					result = DialogFactory.UPLOAD_PROBLEM;
				} catch (IllegalStateException e) {
					result = DialogFactory.UPLOAD_PROBLEM;
				} catch (XmlPullParserException e) {
					result = DialogFactory.UPLOAD_PROBLEM;
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				Application.mainActivity.setSupportProgressBarIndeterminateVisibility(false);
				if (result == 0) {
					Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_upload_success, Toast.LENGTH_SHORT).show();
				}
				Application.mainActivity.getCurrentFocus().invalidate();
				if (result != 0) {
					if (!Application.mainActivity.isFinishing()) {
						Application.mainActivity.showDialog(result);
					}
				}
			}
			
		}.execute();
	}
	
	/**
	 * Show a toast indicating how many unread mails are on the server
	 */
	public void checkForMail() {
		final Server server = prefs.getServer();
		new AsyncTask<Void, Void, Integer>() {
			
			@Override
			protected void onPreExecute() {
			}
			
			@Override
			protected Integer doInBackground(Void... params) {
				int result = 0;
				
				UserDetails userDetails = server.getUserDetails();
				if (userDetails != null) {
					result = userDetails.unread;
				}
				return result;
			}
			
			@Override
			protected void onPostExecute(Integer result) {
				if (result > 0) {
					Context ctx = Application.mainActivity.getApplicationContext();
					try {
						Toast.makeText(ctx,ctx.getResources().getString(R.string.toast_unread_mail, result), Toast.LENGTH_LONG).show();
					} catch (java.util.IllegalFormatFlagsException iffex) {
						// do nothing ... this is stop bugs in the Android format parsing crashing the upload, happens at least with the PL string
					}
				}
			}
			
		}.execute();
	}
	
	/**
	 * Make a new bug at the given screen X/Y coordinates.
	 * 
	 * @param x The screen X-coordinate of the bug.
	 * @param y The screen Y-coordinate of the bug.
	 * @return The new bug, which must have a comment added before it can be submitted to OSB.
	 */
	public Note makeNewBug(final float x, final float y) {
		int lat = yToLatE7(y);
		int lon = xToLonE7(x);
		return new Note(lat, lon);
	}
	
	/**
	 *  Setter to a) set the internal value and b) push the value to {@link #map}.
	 */
	public synchronized void setSelectedNode(final Node selectedNode) {
		if (selectedNode != null) { // always restart
			selectedNodes = new LinkedList<Node>();
			selectedNodes.add(selectedNode);
		} else {
			selectedNodes = null;
		}
		map.setSelectedNodes(selectedNodes);
	}
	
	/**
	 * Add nodes to the internal list
	 */
	public synchronized void addSelectedNode(final Node selectedNode) {
		if (selectedNodes == null) {
			setSelectedNode(selectedNode);
		} else {
			if (!selectedNodes.contains(selectedNode)) {
				selectedNodes.add(selectedNode);
			}
		}
	}
	
	public synchronized void removeSelectedNode(Node node) {
		if (selectedNodes != null) {
			selectedNodes.remove(node);
			if (selectedNodes.size() == 0) {
				selectedNodes = null;
			}
		}
	}
	
	/**
	 * Setter to a) set the internal value and b) push the value to {@link #map}.
	 */
	public synchronized void setSelectedWay(final Way selectedWay) {
		if (selectedWay != null) {  // always restart
			selectedWays = new LinkedList<Way>();
			selectedWays.add(selectedWay);
		} else {
			selectedWays = null;
		}
		map.setSelectedWays(selectedWays);
	}
	
	/**
	 * Adds the given way to the list of currently selected ways.
	 */
	public synchronized void addSelectedWay(final Way selectedWay) {
		if (selectedWays == null) {
			setSelectedWay(selectedWay);
		} else {
			if (!selectedWays.contains(selectedWay)) {
				selectedWays.add(selectedWay);
			}
		}
	}
	
	/**
	 * Removes the given way from the list of currently selected ways.
	 */
	public synchronized void removeSelectedWay(Way way) {
		if (selectedWays != null) {
			selectedWays.remove(way);
			if (selectedWays.size() == 0) {
				selectedWays = null;
			}
		}
	}
	
	/**
	 * Setter to a) set the internal value and b) push the value to {@link #map}.
	 */
	public synchronized void setSelectedRelation(final Relation selectedRelation) {
		if (selectedRelations != null) {  // always restart
			selectedRelations = new LinkedList<Relation>();
			selectedRelations.add(selectedRelation);
		} else {
			selectedRelations = null;
		}
		if (selectedRelation != null) {
			selectRelation(selectedRelation);
		}
	}
	
	public synchronized void removeSelectedRelation(Relation relation) {
		if (selectedRelations != null) {
			selectedRelations.remove(relation);
			if (selectedRelations.size() == 0) {
				selectedRelations = null;
			}
		}
	}
	
	/**
	 * Adds the given relation to the list of currently selected relations.
	 */
	public synchronized void addSelectedRelation(final Relation selectedRelation) {
		if (selectedRelations == null) {
			setSelectedRelation(selectedRelation);
		} else {
			if (!selectedRelations.contains(selectedRelation)) {
				selectedRelations.add(selectedRelation);
			}
		}
	}
	
	/**
	 * Set the currently selected bug.
	 * 
	 * @param bug The selected bug.
	 */
	public synchronized void setSelectedBug(final Bug bug) {
		this.selectedBug = bug;
	}

	/**
	 * @return the selectedNode (currently simply the first in the list)
	 */
	public synchronized final Node getSelectedNode() {
		if (selectedNodes != null && selectedNodes.size() > 0) {
			if (!exists(selectedNodes.get(0))) {
				selectedNodes = null; // clear selection if node was deleted
				return null;
			} else {
				return selectedNodes.get(0);
			}
		}
		return null;
	}

	/**
	 * Get list of selected nodes
	 */
	public List<Node> getSelectedNodes() {
		return selectedNodes;
	}

	/**
	 * Return how many nodes are selected
	 */
	public int selectedNodesCount() {
		return selectedNodes == null ? 0 : selectedNodes.size();
	}

	/**
	 * @return the selectedWay (currently simply the first in the list)
	 */
	public synchronized final Way getSelectedWay() {
		if (selectedWays != null && selectedWays.size() > 0) {
			if (!exists(selectedWays.get(0))) {
				selectedWays = null; // clear selection if node was deleted
				return null;
			} else {
				return selectedWays.get(0);
			}
		}
		return null;
	}
	
	/**
	 * Get list of selected ways
	 */
	public List<Way> getSelectedWays() {
		return selectedWays;
	}
	
	/**
	 * Return how many ways are selected
	 */
	public int selectedWaysCount() {
		return selectedWays == null ? 0 : selectedWays.size();
	}
	
	/**
	 * Get list of selected ways
	 */
	public List<Relation> getSelectedRelations() {
		return selectedRelations;
	}
	
	/**
	 * Return how many ways are selected
	 */
	public int selectedRelationsCount() {
		return selectedRelations == null ? 0 : selectedRelations.size();
	}
	
	/**
	 * Check is all selected elements exist, return true if we actually had to remove something
	 */
	boolean resyncSelected() {
		boolean result = false;
		if (selectedNodes != null && selectedNodes.size() > 0) {
			for (Node n:new ArrayList<Node>(selectedNodes)) {
				if (!getDelegator().getCurrentStorage().contains(n)) {
					selectedNodes.remove(n);
					result = true;
				}
			}
		}
		if (selectedWays != null && selectedWays.size() > 0) {
			for (Way w:new ArrayList<Way>(selectedWays)) {
				if (!getDelegator().getCurrentStorage().contains(w)) {
					selectedWays.remove(w);
					result = true;
				}
			}
		}
		if (selectedRelations != null && selectedRelations.size() > 0) {
			for (Relation r:new ArrayList<Relation>(selectedRelations)) {
				if (!getDelegator().getCurrentStorage().contains(r)) {
					selectedRelations.remove(r);
					result = true;
				}
			}
		}
		return result;
	}
	
	/**
	 * Get the selected bug.
	 * 
	 * @return The selected bug.
	 */
	public synchronized final Bug getSelectedBug() {
		return selectedBug;
	}
	
	/**
	 * Will be called when the screen orientation was changed.
	 * 
	 * @param map the new Map-Instance. Be aware: The View-dimensions are not yet set...
	 */
	public void setMap(Map map) {
		this.map = map;
		Profile.updateStrokes(Math.min(prefs.getMaxStrokeWidth(), strokeWidth(viewBox.getWidth())));
		map.setDelegator(getDelegator());
		map.setViewBox(viewBox);
	}
	
	/**
	 * @return a list of all pending changes to upload
	 */
	public List<String> getPendingChanges(final Context aCaller) {
		return getDelegator().listChanges(aCaller.getResources());
	}

	/**
	 * Sets the set of elements that can currently be clicked.
	 * <li>If set to null, the map will use default behaviour.</li>
	 * <li>If set to a non-null value, the map will highlight only elements in the list.</li>
	 * 
	 * @param clickable a set of elements to which highlighting should be limited, or null to remove the limitation
	 */
	public void setClickableElements(Set<OsmElement> clickable) {
		clickableElements = clickable;
	}
	
	/**
	 * @return the list of clickable elements. May be null, meaning no restrictions on clickable elements
	 */
	public Set<OsmElement> getClickableElements() {
		return clickableElements;
	}
	
	/**
	 * Sets if we return relations when touching/clicking.
	 * 
	 * @param on true if we should return relations
	 */
	public void setReturnRelations(boolean on) {
		returnRelations = on;
	}
	
	
	/**
	 * Checks if an element exists, i.e. is in currentStorage
	 * 
	 * @param element the element that is to be checked
	 * @return true if the element exists, false otherwise
	 */
	public boolean exists(OsmElement element) {
		return getDelegator().getCurrentStorage().contains(element);
	}
	
	/**
	 * @return the X coordinate (in pixels) of the given node's position on the
	 *         screen (note that the returned position may be outside of the
	 *         screens bounds).
	 */
	public float getNodeScreenX(Node node) {
		return lonE7ToX(node.getLon());
	}

	/**
	 * @return the Y coordinate (in pixels) of the given node's position on the
	 *         screen (note that the returned position may be outside of the
	 *         screens bounds).
	 */
	public float getNodeScreenY(Node node) {
		return latE7ToY(node.getLat());
	}
	
	/** Helper class for ordering nodes/ways by distance from a click */
	private static class DistanceSorter<OUTTYPE extends OsmElement, T extends OUTTYPE> {
		private Comparator<Entry<T, Double>> comparator =
			new Comparator<Entry<T, Double>>() {
				@Override
				public int compare(Entry<T,Double> lhs, Entry<T,Double> rhs) {
					if (lhs == rhs) return 0;
					if (lhs.getValue() > rhs.getValue()) return 1;
					if (lhs.getValue() < rhs.getValue()) return -1;
					return 0;
				}
			};
		
		/** Takes an element-distance map and returns the elements ordered by distance */
		public ArrayList<OUTTYPE> sort(HashMap<T, Double> input) {
			ArrayList<Entry<T, Double>> entries = new ArrayList<Entry<T,Double>>(input.entrySet());
			Collections.sort(entries, comparator);
			
			ArrayList<OUTTYPE> result = new ArrayList<OUTTYPE>(entries.size());
			for (Entry<T, Double> entry : entries) result.add(entry.getKey());
			return result;			
		}
	}

	/**
	 * Creates a turn restriction relation using the given objects as the members in the relation.
	 * 
	 * @param fromWay the way on which turning off of is restricted in some fashion
	 * @param viaElement the "intersection node" at which the turn is restricted
	 * @param toWay the way that the turn restriction prevents turning onto
	 * @param restriction_type the kind of turn which is restricted
	 * @return a relation element for the turn restriction
	 */
	public Relation createRestriction(Way fromWay, OsmElement viaElement, Way toWay, String restriction_type) {
		
		createCheckpoint(R.string.undo_action_create_relation);
		Relation restriction = getDelegator().createAndInsertReleation();
		SortedMap<String,String> tags = new TreeMap<String,String>();
		tags.put("restriction", restriction_type == null ? "" : restriction_type);
		tags.put("type", "restriction");
		getDelegator().setTags(restriction, tags);
		RelationMember from = new RelationMember("from", fromWay);
		restriction.addMember(from);
		fromWay.addParentRelation(restriction);
		RelationMember via = new RelationMember("via", viaElement);
		restriction.addMember(via);
		viaElement.addParentRelation(restriction);
		RelationMember to = new RelationMember("to", toWay);
		restriction.addMember(to);
		toWay.addParentRelation(restriction);
		
		return restriction;
	}

	/**
	 * Creates a new relation containing the given members.
	 * 
	 * @param type the 'type=*' tag to set on the relation itself
	 * @param members the osm elements to include in the relation
	 * @return the new relation
	 */
	public Relation createRelation(String type, List<OsmElement> members ) {
		
		createCheckpoint(R.string.undo_action_create_relation);
		Relation relation = getDelegator().createAndInsertReleation();
		SortedMap<String,String> tags = new TreeMap<String,String>();
		if (type != null)
			tags.put("type", type);
		else
			tags.put("type", "");
		getDelegator().setTags(relation, tags);
		for (OsmElement e:members) {
			RelationMember rm = new RelationMember("", e);
			relation.addMember(rm);
			e.addParentRelation(relation);
		}
		return relation;
	}
	
	
	/**
	 * Adds the list of elements to the given relation with an empty role set for each new member.
	 */
	public void addMembers(Relation relation, ArrayList<OsmElement> members) {
		createCheckpoint(R.string.undo_action_update_relations);
		getDelegator().addMembersToRelation(relation, members);
	}
	
	/**
	 * Sets the set of ways that belong to a relation and should be highlighted. 
	 * If set to null, the map will use default behaviour.
	 * If set to a non-null value, the map will highlight only elements in the list.
	 * @param set of elements to which highlighting should be limited, or null to remove the limitation
	 */
	public void setSelectedRelationWays(List<Way> ways) {
		selectedRelationWays = ways;
	}
	
	public void addSelectedRelationWay(Way way) {
		if (selectedRelationWays == null) {
			selectedRelationWays = new LinkedList<Way>();
		}
		selectedRelationWays.add(way);
	}
	
	public void removeSelectedRelationWay(Way way) {
		if (selectedRelationWays != null) {
			selectedRelationWays.remove(way);
		}
	}
	
	public List<Way> getSelectedRelationWays() {
		return selectedRelationWays;
	}

	
	/**
	 * Set relation members to be highlighted
	 * @param r
	 */
	public void selectRelation(Relation r) {
		for (RelationMember rm : r.getMembers()) {
			OsmElement e = rm.getElement();
			if (e != null) {
				if (e.getName().equals(Way.NAME)) {
					addSelectedRelationWay((Way) e);
				} else if (e.getName().equals(Node.NAME)) {
					addSelectedRelationNode((Node) e);
				} else if (e.getName().equals(Relation.NAME) && (selectedRelationRelations == null || !selectedRelationRelations.contains((Relation)e))) { // break recursion if already selected
					addSelectedRelationRelation((Relation) e);
				} 
			}
		}
	}
	
	/**
	 * Sets the set of nodes that belong to a relation and should be highlighted. 
	 * If set to null, the map will use default behaviour.
	 * If set to a non-null value, the map will highlight only elements in the list.
	 * @param set of elements to which highlighting should be limited, or null to remove the limitation
	 */
	public void setSelectedRelationNodes(List<Node> nodes) {
		selectedRelationNodes = nodes;
	}
	
	public void addSelectedRelationNode(Node node) {
		if (selectedRelationNodes == null) {
			selectedRelationNodes = new LinkedList<Node>();
		}
		selectedRelationNodes.add(node);
	}
	
	public void removeSelectedRelationNode(Node node) {
		if (selectedRelationNodes != null) {
			selectedRelationNodes.remove(node);
		}
	}
	
	public List<Node> getSelectedRelationNodes() {
		return selectedRelationNodes;
	}
	
	/**
	 * Sets the set of relations that belong to a relation and should be highlighted. 
	 * If set to null, the map will use default behaviour.
	 * If set to a non-null value, the map will highlight only elements in the list.
	 * @param set of elements to which highlighting should be limited, or null to remove the limitation
	 */
	public void setSelectedRelationRelations(List<Relation> relations) {
		selectedRelationRelations = relations;
		if (selectedRelationRelations != null) {
			for (Relation r:selectedRelationRelations) {
				selectRelation(r);
			}
		}
	}
	
	public void addSelectedRelationRelation(Relation relation) {
		if (selectedRelationRelations == null) {
			selectedRelationRelations = new LinkedList<Relation>();
		}
		selectedRelationRelations.add(relation);
	}
	
	public void removeSelectedRelationRelation(Relation relation) {
		if (selectedRelationRelations != null) {
			selectedRelationRelations.remove(relation);
		}
	}
	
	public List<Relation> getSelectedRelationRelations() {
		return selectedRelationRelations;
	}
	
	
	public void fixElementWithConflict(long newVersion, OsmElement elementLocal, OsmElement elementOnServer) {
		createCheckpoint(R.string.undo_action_fix_conflict);

		if (elementOnServer == null) { // deleted
			// given that the element is deleted on the server we likely need to add it back to ways and relations there too
			if (elementLocal.getName().equals(Node.NAME)) {
				for (Way w:getWaysForNode((Node)elementLocal)) {
					getDelegator().setOsmVersion(w,w.getOsmVersion()+1);
				}
			}
			if (elementLocal.hasParentRelations()) {
				for (Relation r:elementLocal.getParentRelations()) {
					getDelegator().setOsmVersion(r,r.getOsmVersion()+1);
				}
			}
		}
		getDelegator().setOsmVersion(elementLocal,newVersion);
	}

	/**
	 * Displays a crosshair marker on the screen at the coordinates given (in pixels).
	 */
	public void showCrosshairs(float x, float y) {
		map.showCrosshairs(x, y);
		map.invalidate();
	}
	
	public void hideCrosshairs() {
		map.hideCrosshairs();
	}


	public void copyToClipboard(OsmElement element) {
		if (element instanceof Node) {
			getDelegator().copyToClipboard(element, ((Node)element).getLat(), ((Node)element).getLon());
		} else if (element instanceof Way) {
			// use current centroid of way
			int result[] = Logic.centroid(map.getWidth(), map.getHeight(), viewBox,(Way)element);
			Log.d("Logic","centroid " + result[0] + " " + result[1]);
			getDelegator().copyToClipboard(element, result[0], result[1]);
		}
	}


	public void cutToClipboard(OsmElement element) {
		createCheckpoint(R.string.undo_action_cut);
		if (element instanceof Node) {
			getDelegator().cutToClipboard(element, ((Node)element).getLat(), ((Node)element).getLon());
		} else if (element instanceof Way) {
			int result[] = Logic.centroid(map.getWidth(), map.getHeight(), viewBox,(Way)element);
			Log.d("Logic","centroid " + result[0] + " " + result[1]);
			getDelegator().cutToClipboard(element, result[0], result[1]);
		}
		map.invalidate();
	}

	public void pasteFromClipboard(float x, float y) {
		createCheckpoint(R.string.undo_action_paste);
		int lat = yToLatE7(y);
		int lon = xToLonE7(x);
		getDelegator().pasteFromClipboard(lat, lon);
	}

	public boolean clipboardIsEmpty() {
		
		return getDelegator().clipboardIsEmpty();
	}


	/**
	 * calculate the centroid of a way
	 * @param viewvBox 
	 * @param h 
	 * @param w 
	 * @param way
	 * @return  WS84 coordinates of centroid
	 */
	public static int[] centroid(int w, int h, BoundingBox v, final Way way) {
		float XY[] = centroidXY(w,h,v,way);
		if (XY == null) {
			return null;
		}
		int lat = GeoMath.yToLatE7(h, w, v, XY[1]);
		int lon = GeoMath.xToLonE7(w, v, XY[0]);
		int result[] = {lat,lon};
		return result;
	}


	/**
	 * calculate the centroid of a way
	 * @param viewvBox 
	 * @param h 
	 * @param w 
	 * @param way
	 * @return screen coordinates of centroid
	 */
	public static float[] centroidXY(int w, int h, BoundingBox v, final Way way) {
		if (way == null) {
			return null;
		}
		// 
		List<Node> vertices = way.getNodes();
		if (way.isClosed()) {
			// see http://paulbourke.net/geometry/polygonmesh/
			double A = 0;
			double Y = 0;
			double X = 0;
			int vs = vertices.size();
			for (int i = 0; i < vs ; i++ ) {
				double x1 = GeoMath.lonE7ToX(w, v, vertices.get(i).getLon());
				double y1 = GeoMath.latE7ToY(h, w, v, vertices.get(i).getLat());
				double x2 = GeoMath.lonE7ToX(w, v, vertices.get((i+1) % vs).getLon());
				double y2 = GeoMath.latE7ToY(h, w, v, vertices.get((i+1) % vs).getLat());
				A = A + (x1*y2 - x2*y1);
				X = X + (x1+x2)*(x1*y2-x2*y1);
				Y = Y + (y1+y2)*(x1*y2-x2*y1);
			}
			Y = Y/(3*A);
			X = X/(3*A);
			float result[] = {(float)X, (float)Y};
			return result;
		} else { //
			double L = 0;
			double Y = 0;
			double X = 0;
			int vs = vertices.size();
			for (int i = 0; i < (vs-1) ; i++ ) {
				double x1 = GeoMath.lonE7ToX(w, v, vertices.get(i).getLon());
				double y1 = GeoMath.latE7ToY(h, w, v, vertices.get(i).getLat());
				double x2 = GeoMath.lonE7ToX(w, v, vertices.get(i+1).getLon());
				double y2 = GeoMath.latE7ToY(h, w, v, vertices.get(i+1).getLat());
				double len = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
				L = L + len;
				X = X + len * (x1+x2)/2;
				Y = Y + len * (y1+y2)/2;
			}
			Y = Y/L;
			X = X/L;
			float result[] = {(float)X, (float)Y};
			return result;
		}	
	}
	
	/**
	 * calculate the centroid of a way
	 * @param way
	 * @return WGS84 coordinates of centroid
	 */
	public static double[] centroidLonLat(final Way way) {
		if (way == null) {
			return null;
		}
		// 
		List<Node> vertices = way.getNodes();
		if (way.isClosed()) {
			// see http://paulbourke.net/geometry/polygonmesh/
			double A = 0;
			double Y = 0;
			double X = 0;
			int vs = vertices.size();
			for (int i = 0; i < vs ; i++ ) {
				double x1 = vertices.get(i).getLon() / 1E7D;
				double y1 = GeoMath.latE7ToMercator(vertices.get(i).getLat());
				double x2 = vertices.get((i+1) % vs).getLon() / 1E7D;
				double y2 = GeoMath.latE7ToMercator(vertices.get((i+1) % vs).getLat());
				A = A + (x1*y2 - x2*y1);
				X = X + (x1+x2)*(x1*y2-x2*y1);
				Y = Y + (y1+y2)*(x1*y2-x2*y1);
			}
			Y = GeoMath.mercatorToLat(Y/(3*A));
			X = X/(3*A);
			double result[] = {X, Y};
			return result;
		} else { //
			double L = 0;
			double Y = 0;
			double X = 0;
			int vs = vertices.size();
			for (int i = 0; i < (vs-1) ; i++ ) {
				double x1 = vertices.get(i).getLon() / 1E7D;
				double y1 = GeoMath.latE7ToMercator(vertices.get(i).getLat());
				double x2 = vertices.get(i+1).getLon() / 1E7D;
				double y2 = GeoMath.latE7ToMercator(vertices.get((i+1)).getLat());
				double len = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
				L = L + len;
				X = X + len * (x1+x2)/2;
				Y = Y + len * (y1+y2)/2;
			}
			Y = GeoMath.mercatorToLat(Y/L);
			X = X/L;
			double result[] = {X, Y};
			return result;
		}	
	}

	
	/**
	 * Arrange way points in a circle
	 * @param way
	 */
	public void performCirculize(Way way) {
		if (way.getNodes().size() < 3) return;
		createCheckpoint(R.string.undo_action_circulize);
		int[] center = centroid(map.getWidth(), map.getHeight(), viewBox, way);
		getDelegator().circulizeWay(center, way);
		map.invalidate();
	}

	
	/**
	 * convenience function
	 * @param x
	 * @return
	 */
	public int xToLonE7(float x) {
		return GeoMath.xToLonE7(map.getWidth(), viewBox, x);
	}

	/**
	 * convenience function
	 * @param y
	 * @return
	 */
	public int yToLatE7(float y) {
		return GeoMath.yToLatE7(map.getHeight(), map.getWidth(), viewBox, y);
	}

	/**
	 * convenience function
	 * @param lon
	 * @return
	 */
	public float lonE7ToX(int lon) {
		return 	GeoMath.lonE7ToX(map.getWidth(), viewBox, lon);
	}

	/**
	 * convenience function
	 * @param lat
	 * @return
	 */
	public float latE7ToY(int lat) {
		return 	GeoMath.latE7ToY(map.getHeight(),map.getWidth(), viewBox, lat);
	}

	/**
	 * @return the delegator
	 */
	public static StorageDelegator getDelegator() {
		return Application.getDelegator();
	}
	
	public void getDataLock() {
		getDelegator().lock();
	}
	
	public void dataUnlock() {
		getDelegator().unlock();
	}

	/**
	 * @return the viewBox
	 */
	public BoundingBox getViewBox() {
		return viewBox;
	}
}
