package de.blau.android.easyedit;

import java.util.HashSet;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

/**
 * This class handles most of the EasyEdit mode actions, to keep it separate from the main class.
 * @author Jan
 *
 */
public class EasyEditManager {

	private final Main main;
	private final Logic logic;
	/** the touch listener from Main */
	
	private ActionMode currentActionMode = null;
	private EasyEditActionModeCallback currentActionModeCallback = null;
	
	public EasyEditManager(Main main, Logic logic) {
		this.main = main;
		this.logic = logic;
	}
	
	/**
	 * This is called when we are in edit range, EasyEdit mode is active,
	 * and a click needs to be handled.
	 * @param v the view parameter of the click
	 * @param x the x coordinate (screen coordinate?) of the click
	 * @param y the y coordinate (screen coordinate?) of the click
	 * @param mapTouchListener 
	 */
	public void handleClick(View v, float x, float y) {
		if (currentActionModeCallback != null && currentActionModeCallback.handleClick(x,y)) {
			return; // action mode handled the click
		}
		
		
		Node clickedNode = logic.getClickedNode(x, y);
		if (clickedNode != null) {
			if (currentActionModeCallback == null || !currentActionModeCallback.handleNodeClick(clickedNode)) {
				// No callback or didn't handle the click, perform default (select node)
				main.startActionMode(new ElementSelectionActionModeCallback(clickedNode));
			}
			return; // a node was clicked, the click was handled, we're done
		}
		
		// No node was clicked, check for ways
		Way clickedWay = logic.getClickedWay(x, y);
		if (clickedWay != null) {
			if (currentActionModeCallback == null || !currentActionModeCallback.handleWayClick(clickedWay)) {
				// No callback or didn't handle the click, perform default (select way)
				main.startActionMode(new ElementSelectionActionModeCallback(clickedWay));
			}
			return; // a way was clicked, the click was handled, we're done
		}

		// User clicked an empty area. If something is selected, deselect it.
		if (currentActionModeCallback instanceof ElementSelectionActionModeCallback) currentActionMode.finish();
	}

	/** This gets called when the map is long-pressed in easy-edit mode */
	public boolean handleLongClick(View v, float x, float y) {
		if (currentActionModeCallback instanceof PathCreationActionModeCallback) {
			// we don't do long clicks while creating paths
			return false;
		}

		v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
		main.startActionMode(new PathCreationActionModeCallback(x, y));
	
		return true;
	}

	/*package*/ void performDeleteNode(Node node) {
		logic.performErase(node);
	}

	/**
	 * Takes a parameter for a node and one for a way.
	 * If the way is not null, opens a tag editor for the way.
	 * Otherwise, opens a tag editor for the node
	 * (unless the node is also null, then nothing happens).
	 * @param possibleNode a node that was edited, or null
	 * @param possibleWay a way that was edited, or null
	 */
	private void tagApplicable(Node possibleNode, Way possibleWay) {
		if (possibleWay == null) {
			// Single node was added
			if (possibleNode != null) { // null-check to be sure
				main.performTagEdit(possibleNode);
			}
		} else { // way was added
			main.performTagEdit(possibleWay);
		}
	}
	
	/**
	 * Finds which ways can be merged with a way.
	 * For this, the ways must not be equal, need to share at least one end node,
	 * and either at least one of them must not have tags, or the tags on both ways must be equal.  
	 * 
	 * @param way the way into which other ways may be merged
	 * @return a list of all ways which can be merged into the given way
	 */
	private HashSet<OsmElement> findMergeableWays(Way way) {
		HashSet<Way> candidates = new HashSet<Way>();
		HashSet<OsmElement> result = new HashSet<OsmElement>();
		candidates.addAll(logic.getWaysForNode(way.getFirstNode()));
		candidates.addAll(logic.getWaysForNode(way.getLastNode()));
		for (Way candidate : candidates) {
			if (	(way != candidate)
					&& (candidate.isEndNode(way.getFirstNode()) || candidate.isEndNode(way.getLastNode()))
					&& (candidate.getTags().isEmpty() || way.getTags().isEmpty() || 
							way.getTags().entrySet().equals(candidate.getTags().entrySet()) )
				) {
				result.add(candidate);
			}
		}
		return result;
	}

	/**
	 * Base class for ActionMode callbacks inside {@link EasyEditManager}.
	 * Derived classes should call {@link #onCreateActionMode(ActionMode, Menu)} and {@link #onDestroyActionMode(ActionMode)}.
	 * It will handle registering and de-registering the action mode callback with the {@link EasyEditManager}.
	 * When the {@link EasyEditManager} receives a click on a node or way, it may pass it to the current action mode callback.
	 * The callback can then swallow it by returning true or allow the default handling to happen by returning false
	 * in the {@link #handleNodeClick(Node)} or {@link #handleWayClick(Way)} methods.
	 * 
	 * @author Jan
	 *
	 */
	public abstract class EasyEditActionModeCallback implements ActionMode.Callback {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			currentActionMode = mode;
			currentActionModeCallback = this;
			Log.d("EasyEditActionModeCallback", "onCreateActionMode");
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			currentActionMode = null;
			currentActionModeCallback = null;
			Log.d("EasyEditActionModeCallback", "onDestroyActionMode");
		}
		
		
		/**
		 * This method gets called when the map is clicked, before checking for clicked nodes/ways.
		 * The ActionModeCallback can then either return true to indicate that the click was handled (or should be ignored),
		 * or return false to indicate default handling should apply
		 * (which includes checking for node/way clicks and calling the corresponding methods).
		 * 
		 * @param x the x screen coordinate of the click
		 * @param y the y screen coordinate of the click
		 * @return true if the click has been handled, false if default handling should apply
		 */
		public boolean handleClick(float x, float y) {
			return false;
		}
		
		/**
		 * This method gets called when a node click has to be handled.
		 * The ActionModeCallback can then either return true to indicate that the click was handled (or should be ignored),
		 * or return false to indicate default handling should apply.
		 * @param node the node that was clicked
		 * @return true if the click has been handled, false if default handling should apply
		 */
		public boolean handleNodeClick(Node node) {
			return false;
		}
		
		/** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		/** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			return false;
		}

		/**
		 * This method gets called when a way click has to be handled.
		 * The ActionModeCallback can then either return true to indicate that the click was handled (or should be ignored),
		 * or return false to indicate default handling should apply.
		 * @param way the way that was clicked
		 * @return true if the click has been handled, false if default handling should apply
		 */
		public boolean handleWayClick(Way way) {
			return false;
		}
	}
	
	/**
	 * This callback handles path creation. It is started after a long-press.
	 * During this action mode, clicks are handled by custom code.
	 * The node and way click handlers are thus never called.
	 */
	private class PathCreationActionModeCallback extends EasyEditActionModeCallback {
		/** x coordinate of first node */
		private float x;
		/** y coordinate of first node */
		private float y;


		public PathCreationActionModeCallback(float x, float y) {
			this.x = x;
			this.y = y;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			super.onCreateActionMode(mode, menu);
			mode.setTitle(R.string.actionmode_createpath);
			logic.setSelectedWay(null);
			logic.setSelectedNode(null);
			pathCreateNode(x, y);
			return true;
		}
		
		@Override
		public boolean handleClick(float x, float y) {
			pathCreateNode(x,y);
			return true;
		}

		/**
		 * Creates/adds a node into a path during path creation
		 * @param x x screen coordinate
		 * @param y y screen coordinate
		 */
		private void pathCreateNode(float x, float y) {
			Node lastSelectedNode = logic.getSelectedNode();
			Way lastSelectedWay = logic.getSelectedWay();
			logic.performAdd(x, y);
			if (logic.getSelectedNode() == null) {
				// user clicked last node again -> finish adding
				currentActionMode.finish();
				tagApplicable(lastSelectedNode, lastSelectedWay);
			}
			main.invalidateMap();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// no menu // TODO undo last step menu?
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// no menu
			return false;
		}


		/**
		 * Path creation action mode is ending
		 */
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			super.onDestroyActionMode(mode);
			Node lastSelectedNode = logic.getSelectedNode();
			Way lastSelectedWay = logic.getSelectedWay();
			logic.setSelectedWay(null);
			logic.setSelectedNode(null);
			tagApplicable(lastSelectedNode, lastSelectedWay);
		}		
	}
	
	/**
	 * This action mode handles element selection. When a node or way should be selected, just start this mode.
	 * The element will be automatically selected, and a second click on the same element will open the tag editor.
	 * @author Jan
	 *
	 */
	private class ElementSelectionActionModeCallback extends EasyEditActionModeCallback {
		private static final int MENUITEM_TAG = 1;
		private static final int MENUITEM_DELETE = 2;
		private static final int MENUITEM_HISTORY = 3;
		private static final int MENUITEM_SPLIT = 4;
		private static final int MENUITEM_MERGE = 5;
		private static final int MENUITEM_REVERSE = 6;
		
		private final boolean isWay;
		private Node node = null;
		private Way way = null;
		private HashSet<OsmElement> cachedMergeableWays = null;
		
		public ElementSelectionActionModeCallback(Node node) {
			isWay = false;
			this.node = node;
		}

		public ElementSelectionActionModeCallback(Way way) {
			isWay = true;
			this.way = way;
			cachedMergeableWays = findMergeableWays(way);
		}
		
		/**
		 * Internal helper to avoid duplicate code in {@link #handleNodeClick(Node)} and {@link #handleWayClick(Way)}.
		 * @param element clicked element
		 * @return true if handled, false if default handling should apply
		 */
		private boolean handleElementClick(OsmElement element) {
			if (element == getElement()) {
				main.performTagEdit(element);
				return true;
			}
			return false;
		}
		
		@Override
		public boolean handleNodeClick(Node node) {
			return handleElementClick(node);
		}
		
		@Override
		public boolean handleWayClick(Way way) {
			return handleElementClick(way);
		}


		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			super.onCreateActionMode(mode, menu);
			logic.setSelectedNode(node);
			logic.setSelectedWay(way);
			main.invalidateMap();
			if (isWay) {
				mode.setTitle(R.string.actionmode_wayselect);
			} else {
				mode.setTitle(R.string.actionmode_nodeselect);
			}
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu.clear();
			menu.add(Menu.NONE, MENUITEM_TAG, 1, R.string.menu_tags);
			menu.add(Menu.NONE, MENUITEM_DELETE, 2, R.string.delete);
			if (getElement().getOsmId() > 0){
				menu.add(Menu.NONE, MENUITEM_HISTORY, 3, R.string.menu_history);
			}
			if (isWay) {
				menu.add(Menu.NONE, MENUITEM_REVERSE, 4, R.string.menu_reverse);
			}
			if (isWay && way.getNodes().size() > 2) {
				menu.add(Menu.NONE, MENUITEM_SPLIT, 5, R.string.menu_split);
			}
			if (isWay && cachedMergeableWays.size() > 0) {
				menu.add(Menu.NONE, MENUITEM_MERGE, 6, R.string.menu_merge);
			}
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case MENUITEM_TAG: main.performTagEdit(getElement()); break;
			case MENUITEM_DELETE: menuDelete(mode); break;
			case MENUITEM_HISTORY: showHistory(); break;
			case MENUITEM_SPLIT: main.startActionMode(new WaySplittingActionModeCallback(way)); break;
			case MENUITEM_MERGE: main.startActionMode(new WayMergingActionModeCallback(way, cachedMergeableWays)); break;
			case MENUITEM_REVERSE: reverseWay();
			}
			return true;
		}
		
		private void reverseWay() {
			logic.performReverse(way);
			if (way.getOneway() != 0) {
				Toast.makeText(main, R.string.toast_oneway_reversed, Toast.LENGTH_LONG).show();
				main.performTagEdit(way);
			}
		}

		private void menuDelete(ActionMode mode) {
			if (!isWay) {
				performDeleteNode(node);
				mode.finish();
			} else {
				// Way handling - TODO js
				Toast.makeText(main, "not implemented", Toast.LENGTH_SHORT).show();
			}		
		}

		/**
		 * Opens the history page of the selected element in a browser
		 */
		private void showHistory() {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(main.getBaseURL()+"browse/"+(isWay?"way":"node")+"/"+getElement().getOsmId()));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			main.startActivity(intent);
		}

		private OsmElement getElement() {
			return isWay? way : node;
		}

		/**
		 * Element selection action mode is ending
		 */
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			super.onDestroyActionMode(mode);
			logic.setSelectedNode(null);
			logic.setSelectedWay(null);
			main.invalidateMap();
		}		
	}
	
	
	private class WaySplittingActionModeCallback extends EasyEditActionModeCallback {
		private Way way;
		private HashSet<OsmElement> nodes = new HashSet<OsmElement>();
		
		public WaySplittingActionModeCallback(Way way) {
			this.way = way;
			nodes.addAll(way.getNodes());
			nodes.remove(way.getFirstNode());
			nodes.remove(way.getLastNode());
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			super.onCreateActionMode(mode, menu);
			mode.setTitle(R.string.menu_split);
			logic.setClickableElements(nodes);
			return true;
		}
		
		@Override
		public boolean handleNodeClick(Node node) { // due to clickableElements, only valid nodes can be clicked
			logic.performSplit(way, node);
			currentActionMode.finish();
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			super.onDestroyActionMode(mode);
		}
		
	}

	private class WayMergingActionModeCallback extends EasyEditActionModeCallback {
		private Way way;
		private HashSet<OsmElement> ways = new HashSet<OsmElement>();
		
		public WayMergingActionModeCallback(Way way, HashSet<OsmElement> mergeableWays) {
			this.way = way;
			ways = mergeableWays;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.setTitle(R.string.menu_merge);
			logic.setClickableElements(ways);
			super.onCreateActionMode(mode, menu);
			return true;
		}

		@Override
		public boolean handleWayClick(Way clickedWay) { // due to clickableElements, only valid ways can be clicked
			logic.performMerge(way, clickedWay);
			main.startActionMode(new ElementSelectionActionModeCallback(way));
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			super.onDestroyActionMode(mode);
		}
		
	}
	
	


}
