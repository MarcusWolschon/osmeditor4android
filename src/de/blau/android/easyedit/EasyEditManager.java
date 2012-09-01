package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
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
	 * Call to let the action mode (if any) have a first go at the click.
	 * @param x the x coordinate (screen coordinate?) of the click
	 * @param y the y coordinate (screen coordinate?) of the click
	 * @return true if the click was handled
	 */
	public boolean actionModeHandledClick(float x, float y) {
		return (currentActionModeCallback != null && currentActionModeCallback.handleClick(x,y));
	}
	
	/**
	 * Handle case where nothing is touched.
	 */
	public void nothingTouched() {
		// User clicked an empty area. If something is selected, deselect it.
		if (currentActionModeCallback instanceof ElementSelectionActionModeCallback) currentActionMode.finish();
	}
	
	/**
	 * Handle editing the given element.
	 * @param element The OSM element to edit.
	 */
	public void editElement(OsmElement element) {
		if (currentActionModeCallback == null || !currentActionModeCallback.handleElementClick(element)) {
			// No callback or didn't handle the click, perform default (select element)
			ActionMode.Callback cb = null;
			if (element instanceof Node) cb = new NodeSelectionActionModeCallback((Node)element);
			if (element instanceof Way ) cb = new  WaySelectionActionModeCallback((Way )element);
			if (cb != null) main.startActionMode(cb);
		}
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
	private Set<OsmElement> findMergeableWays(Way way) {
		Set<Way> candidates = new HashSet<Way>();
		Set<OsmElement> result = new HashSet<OsmElement>();
		candidates.addAll(logic.getWaysForNode(way.getFirstNode()));
		candidates.addAll(logic.getWaysForNode(way.getLastNode()));
		for (Way candidate : candidates) {
			if ((way != candidate)
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
		 * This method gets called when an OsmElement click has to be handled.
		 * The ActionModeCallback can then either return true to indicate that the click was handled (or should be ignored),
		 * or return false to indicate default handling should apply.
		 * @param element the OsmElement that was clicked
		 * @return true if the click has been handled, false if default handling should apply
		 */
		public boolean handleElementClick(OsmElement element) {
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
	}
	
	/**
	 * This callback handles path creation. It is started after a long-press.
	 * During this action mode, clicks are handled by custom code.
	 * The node and way click handlers are thus never called.
	 */
	private class PathCreationActionModeCallback extends EasyEditActionModeCallback {
		private static final int MENUITEM_UNDO = 1;
		
		/** x coordinate of first node */
		private float x;
		/** y coordinate of first node */
		private float y;
		
		/** contains a pointer to the created way if one was created. used to fix selection after undo. */
		private Way createdWay = null;
		/** contains a list of created nodes. used to fix selection after undo. */
		private ArrayList<Node> createdNodes = new ArrayList<Node>();
		
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
			} else { // update cache for undo
				createdWay = logic.getSelectedWay();
				if (createdWay != null) {
					createdNodes = new ArrayList<Node>(createdWay.getNodes());
				} else {
					createdNodes = new ArrayList<Node>();
					createdNodes.add(logic.getSelectedNode());
				}
			}
			main.invalidateMap();
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu.clear();
			menu.add(0, MENUITEM_UNDO, 1, R.string.undo).setIcon(R.drawable.undo);
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case MENUITEM_UNDO:
				handleUndo();
				break;
			default:
				Log.e("PathCreationActionModeCallback", "Unknown menu item");
				break;
			}
			return false;
		}
		
		private void handleUndo() {
			logic.getUndo().undo();
			if (logic.getSelectedNode() == null) { // should always happen, node removed
				 Iterator<Node> nodeIterator = createdNodes.iterator();
				 while (nodeIterator.hasNext()) { // remove nodes that do not exist anymore
					 if (!logic.exists(nodeIterator.next())) nodeIterator.remove();
				 }
				 if (createdNodes.isEmpty()) {
					 // all nodes have been deleted, cancel action mode
					 currentActionMode.finish();
				 } else {
					 // select last node
					 logic.setSelectedNode(createdNodes.get(createdNodes.size()-1));
				 }
			}
			createdWay = logic.getSelectedWay(); // will be null if way was deleted by undo
			main.invalidateMap();
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
	private abstract class ElementSelectionActionModeCallback extends EasyEditActionModeCallback {
		private static final int MENUITEM_TAG = 1;
		private static final int MENUITEM_DELETE = 2;
		private static final int MENUITEM_HISTORY = 3;
		
		protected OsmElement element = null;
		
		public ElementSelectionActionModeCallback(OsmElement element) {
			this.element = element;
		}
		
		/**
		 * Internal helper to avoid duplicate code in {@link #handleNodeClick(Node)} and {@link #handleWayClick(Way)}.
		 * @param element clicked element
		 * @return true if handled, false if default handling should apply
		 */
		public boolean handleElementClick(OsmElement element) {
			if (element == this.element) {
				main.performTagEdit(element);
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu.clear();
			menu.add(Menu.NONE, MENUITEM_TAG, 1, R.string.menu_tags);
			menu.add(Menu.NONE, MENUITEM_DELETE, 2, R.string.delete);
			if (element.getOsmId() > 0){
				menu.add(Menu.NONE, MENUITEM_HISTORY, 3, R.string.menu_history);
			}
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case MENUITEM_TAG: main.performTagEdit(element); break;
			case MENUITEM_DELETE: menuDelete(mode); break;
			case MENUITEM_HISTORY: showHistory(); break;
			default: return false;
			}
			return true;
		}
		
		protected abstract void menuDelete(ActionMode mode);
		
		/**
		 * Opens the history page of the selected element in a browser
		 */
		private void showHistory() {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(main.getBaseURL()+"browse/"+element.getName()+"/"+element.getOsmId()+"/history"));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			main.startActivity(intent);
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
	
	private class NodeSelectionActionModeCallback extends ElementSelectionActionModeCallback {
		private NodeSelectionActionModeCallback(Node node) {
			super(node);
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			super.onCreateActionMode(mode, menu);
			logic.setSelectedNode((Node)element);
			logic.setSelectedWay(null);
			main.invalidateMap();
			mode.setTitle(R.string.actionmode_nodeselect);
			return true;
		}
		
		protected void menuDelete(ActionMode mode) {
			logic.performEraseNode((Node)element);
			mode.finish();
		}
		
	}
	
	private class WaySelectionActionModeCallback extends ElementSelectionActionModeCallback {
		private static final int MENUITEM_SPLIT = 4;
		private static final int MENUITEM_MERGE = 5;
		private static final int MENUITEM_REVERSE = 6;
		
		private Set<OsmElement> cachedMergeableWays;
		
		private WaySelectionActionModeCallback(Way way) {
			super(way);
			cachedMergeableWays = findMergeableWays(way);
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			super.onCreateActionMode(mode, menu);
			logic.setSelectedNode(null);
			logic.setSelectedWay((Way)element);
			main.invalidateMap();
			mode.setTitle(R.string.actionmode_wayselect);
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			super.onPrepareActionMode(mode, menu);
			menu.add(Menu.NONE, MENUITEM_REVERSE, 4, R.string.menu_reverse);
			if (((Way)element).getNodes().size() > 2) {
				menu.add(Menu.NONE, MENUITEM_SPLIT, 5, R.string.menu_split);
			}
			if (cachedMergeableWays.size() > 0) {
				menu.add(Menu.NONE, MENUITEM_MERGE, 6, R.string.menu_merge);
			}
			return true;
		}
		
		private void reverseWay() {
			Way way = (Way) element;
			logic.performReverse(way);
			if (way.getOneway() != 0) {
				Toast.makeText(main, R.string.toast_oneway_reversed, Toast.LENGTH_LONG).show();
				main.performTagEdit(way);
			}
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			if (!super.onActionItemClicked(mode, item)) {
				switch (item.getItemId()) {
				case MENUITEM_SPLIT: main.startActionMode(new WaySplittingActionModeCallback((Way)element)); break;
				case MENUITEM_MERGE: main.startActionMode(new WayMergingActionModeCallback((Way)element, cachedMergeableWays)); break;
				case MENUITEM_REVERSE: reverseWay(); break;
				default: return false;
				}
			}
			return true;
		}
		
		protected void menuDelete(ActionMode mode) {
			TextView textView = new TextView(main);
			textView.setText(R.string.deleteway_description);
			int pad = Math.round(10f * main.getResources().getDisplayMetrics().density);
			textView.setPadding(pad, pad, pad, pad);
			new AlertDialog.Builder(main)
				.setTitle(R.string.delete)
				.setView(textView) 
				.setPositiveButton(R.string.deleteway_wayonly,
					new OnClickListener() {	
						@Override
						public void onClick(DialogInterface dialog, int which) {
							logic.performEraseWay((Way)element, false);
							currentActionMode.finish();
						}
					})
				.setNeutralButton(R.string.deleteway_wayandnodes,
					new OnClickListener() {	
						@Override
						public void onClick(DialogInterface dialog, int which) {
							logic.performEraseWay((Way)element, true);
							currentActionMode.finish();
						}
					})
				.show();
		}
		
	}
	
	private class WaySplittingActionModeCallback extends EasyEditActionModeCallback {
		private Way way;
		private Set<OsmElement> nodes = new HashSet<OsmElement>();
		
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
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid nodes can be clicked
			logic.performSplit(way, (Node)element);
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
		private Set<OsmElement> ways;
		
		public WayMergingActionModeCallback(Way way, Set<OsmElement> mergeableWays) {
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
		public boolean handleElementClick(OsmElement element) { // due to clickableElements, only valid ways can be clicked
			logic.performMerge(way, (Way)element);
			main.startActionMode(new WaySelectionActionModeCallback(way));
			return true;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			logic.setClickableElements(null);
			super.onDestroyActionMode(mode);
		}
		
	}
	
}
