package de.blau.android.easyedit;

import java.util.List;

import android.view.View;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Main.MapTouchListener;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;

public class EasyEditManager implements Callback {

	private final Main main;
	private final Logic logic;
	
	private ActionMode pathActionMode = null;
	
	public EasyEditManager(Main main, Logic logic) {
		this.main = main;
		this.logic = logic;
	}

	/**
	 * This is called when we are in edit range, EasyEdit mode is active,
	 * and a click needs to be handled.
	 * @param v the view parameter of the click
	 * @param x the x coordinate (view coordinate?) of the click
	 * @param y the y coordinate (view coordinate?) of the click
	 * @param mapTouchListener 
	 */
	public void handleEasyEditClick(View v, float x, float y, MapTouchListener mapTouchListener) {
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
			handleEasyEditNodeClick((Node)clickedNodes.get(0), mapTouchListener);
			break;
		default:
			v.showContextMenu();
			break;
		}
	
	}

	public void handleEasyEditNodeClick(Node node, MapTouchListener mapTouchListener) {
	
		// If the clicked node is not selected, select it and return
		if (logic.getSelectedNode() == null || node != logic.getSelectedNode()) {
			logic.setSelectedNode(node);
			return;
		}
		
		// Node was selected, we need to edit it.
		// For now, just start the tag editor for the node.
		// TODO JS let user choose tag or way to edit
		// TODO JS menu-based editing here
		mapTouchListener.performTagEdit(node);
		
	}

	public boolean handleEasyEditLongClick(View v, float x, float y) {
		if (pathActionMode != null) {
			// we don't do long clicks while creating paths
			return false;
		}
		// TODO JS
		
		// for now:
		pathStart();
		pathCreateNode(x,y);
		
		return true;
	}
	
	private void pathStart() {
		pathActionMode = main.startActionMode(this);
		pathActionMode.setTitle("Creating path"); // TODO resource
		logic.setSelectedWay(null);
		logic.setSelectedNode(null);
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
	

}
