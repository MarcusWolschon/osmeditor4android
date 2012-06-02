package de.blau.android.easyedit;

import java.util.List;

import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Main.MapTouchListener;
import de.blau.android.easyedit.EasyEditMenu.ContextMenuAction;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

public class EasyEditManager implements Callback {

	private final Main main;
	private final Logic logic;
	private MapTouchListener touchlistener;
	
	private ActionMode pathActionMode = null;
	
	private EasyEditMenu contextMenu;
	
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
		if (pathActionMode != null) {
			// currently creating a path, handle accordingly
			pathCreateNode(x,y);
			return;
		}
		
		List<OsmElement> clickedNodes = logic.getClickedNodes(x, y);
		switch (clickedNodes.size()) {
		case 0:
			logic.setSelectedNode(null);
			break;
		case 1:
			handleNodeClick((Node)clickedNodes.get(0));
			break;
		default:
			contextMenu = new EasyEditMenu(this);
			contextMenu.add(clickedNodes, ContextMenuAction.HANDLE_NODE_CLICK);
			contextMenu.trigger();
			break;
		}
	
	}

	public boolean handleLongClick(View v, float x, float y) {
		if (pathActionMode != null) {
			// we don't do long clicks while creating paths
			return false;
		}

		pathStart(x,y);
	
		return true;
	}
	

	
	/*package*/ void handleNodeClick(Node node) {
	
		// If the clicked node is not selected, select it and return
		if (logic.getSelectedNode() == null || node != logic.getSelectedNode()) {
			logic.setSelectedNode(node);
			main.invalidateMap();
			return;
		}
		
		// Node was selected, let user choose to edit ways if they exist
		
		List<Way> ways = logic.getWaysForNode(node);

		contextMenu = new EasyEditMenu(this);
		contextMenu.add(node, ContextMenuAction.TAG_EDIT, "Edit node");
		contextMenu.add(ways, ContextMenuAction.TAG_EDIT);
		contextMenu.add(node, ContextMenuAction.DELETE_NODE, "Delete node");
		contextMenu.trigger();
	}

	/*package*/ void performTagEdit(OsmElement element) {
		// For now, just start the tag editor for the node.
		// TODO JS menu-based editing here
		touchlistener.performTagEdit(element);
	}



	/*package*/ void performDeleteNode(Node node) {
		logic.performErase(node);
	}

	private void pathStart(float x, float y) {
		pathActionMode = main.startActionMode(this);
		pathActionMode.setTitle("Creating path"); // TODO resource
		logic.setSelectedWay(null);
		logic.setSelectedNode(null);
		pathCreateNode(x, y);
	}

	private void pathCreateNode(float x, float y) {
		logic.performAdd(x, y);
		if (logic.getSelectedNode() == null) {
			// user clicked last node again -> finish adding
			pathActionMode.finish();
		}
		main.invalidateMap();
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// TODO Auto-generated method stub
		// TODO js undo-last
		// TODO js cancel-path
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		pathActionMode = null;
		logic.setSelectedWay(null);
		logic.setSelectedNode(null);
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (contextMenu != null) {
			contextMenu.onCreateContextMenu(menu, v, menuInfo);
			contextMenu = null;
		}
	}
	
	public void triggerMapContextMenu() {
		main.triggerMapContextMenu();
	}
	

}
