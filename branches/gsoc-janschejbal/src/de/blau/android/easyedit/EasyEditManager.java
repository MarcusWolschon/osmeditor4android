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
import de.blau.android.Main.MapTouchListener;
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
	private MapTouchListener touchlistener;
	
	private ActionMode currentActionMode = null;
	private EasyEditActionModeCallback currentActionModeCallback = null;
	
	public EasyEditManager(Main main, Logic logic, MapTouchListener mapTouchListener) {
		this.main = main;
		this.logic = logic;
		this.touchlistener = mapTouchListener;
	}
	
	/**
	 * This is called when we are in edit range, EasyEdit mode is active,
	 * and a click needs to be handled.
	 * @param v the view parameter of the click
	 * @param x the x coordinate (view coordinate?) of the click
	 * @param y the y coordinate (view coordinate?) of the click
	 * @param mapTouchListener 
	 */
	public void handleClick(View v, float x, float y) {
		if (currentActionModeCallback instanceof PathCreationActionModeCallback) {
			// currently creating a path, handle accordingly
			pathCreateNode(x,y);
			return;
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
		pathStart(x,y);
	
		return true;
	}
	

	/**
	 * This gets called when a way is clicked in easy-edit mode
	 * The first click on a way selects it, then the second one shows the tag editor.
	 * @param way the clicked way
	 */
	/*package*/ void handleWayClick(Way way) {
		Way oldway = logic.getSelectedWay();
		
		// If the clicked way is not selected, select it and return
		if (oldway == null || way != oldway) {
			if (currentActionModeCallback instanceof ElementSelectionActionModeCallback) currentActionMode.finish();
			logic.setSelectedNode(null);
			logic.setSelectedWay(way);
			main.startActionMode(new ElementSelectionActionModeCallback(way));
			main.invalidateMap();
			return;
		}
		
		// Node was selected, let user choose to edit ways if they exist
		performTagEdit(way);
	}

	/*package*/ void performTagEdit(OsmElement element) {
		touchlistener.performTagEdit(element);
	}


	/*package*/ void performDeleteNode(Node node) {
		logic.performErase(node);
	}

	/**
	 * Starts the creation of a new path at the given coordinates
	 * @param x x screen coordinate
	 * @param y y screen coordinate
	 */
	private void pathStart(float x, float y) {
		main.startActionMode(new PathCreationActionModeCallback());
		logic.setSelectedWay(null);
		logic.setSelectedNode(null);
		pathCreateNode(x, y);
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
				performTagEdit(possibleNode);
			}
		} else { // way was added
			performTagEdit(possibleWay);
		}
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
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			super.onCreateActionMode(mode, menu);
			mode.setTitle("Creating Path"); // TODO res
			return true;
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
		private final boolean isWay;
		private Node node = null;
		private Way way = null;
		
		
		public ElementSelectionActionModeCallback(Node node) {
			isWay = false;
			this.node = node;
		}

		public ElementSelectionActionModeCallback(Way way) {
			isWay = true;
			this.way = way;
		}
		
		/**
		 * Internal helper to avoid duplicate code in {@link #handleNodeClick(Node)} and {@link #handleWayClick(Way)}.
		 * @param element clicked element
		 * @return true if handled, false if default handling should apply
		 */
		private boolean handleElementClick(OsmElement element) {
			if (element == getElement()) {
				performTagEdit(element);
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
				mode.setTitle("Way selected"); // TODO res
			} else {
				mode.setTitle("Node selected"); // TODO res
			}
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// Universal: Tag, delete, history
			// Ways: Split/Merge?
			menu.clear();
			menu.add("tag"); // TODO replace with proper items
			menu.add("delete"); // TODO replace with proper items
			menu.add("history"); // TODO replace with proper items
			menu.add("split"); // TODO replace with proper items
			menu.add("join"); // TODO replace with proper items
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// TODO
			if (item.getTitle().equals("delete")) { // TODO replace with proper item handling
				if (isWay) {
					Toast.makeText(main, "not implemented", Toast.LENGTH_SHORT).show();
				} else {
					performDeleteNode(node);
				}
				mode.finish();
			} else if(item.getTitle().equals("tag")) {
				performTagEdit(getElement());
			} else if (item.getTitle().equals("history")) {
				showHistory();
			} else if (item.getTitle().equals("split")) {
				main.startActionMode(new WaySplittingActionModeCallback(way));
			}
			return false;
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
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			super.onCreateActionMode(mode, menu);
			logic.setClickableElements(nodes);
			return true;
		}
		
		@Override
		public boolean handleNodeClick(Node node) {
			if (nodes.contains(node)) {
				Toast.makeText(main, "split", Toast.LENGTH_SHORT).show(); // TODO implement
			} else {
				Toast.makeText(main, "invalid node, pick a valid one", Toast.LENGTH_SHORT).show(); // TODO implement
			}
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			super.onDestroyActionMode(mode);
		}
		
	}


}
