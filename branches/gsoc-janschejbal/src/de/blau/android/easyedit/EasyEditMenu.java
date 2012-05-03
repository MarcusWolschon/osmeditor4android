package de.blau.android.easyedit;

import java.util.ArrayList;
import java.util.List;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

import com.actionbarsherlock.view.Menu;

import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;

public class EasyEditMenu implements OnMenuItemClickListener {
	private List<ContextMenuEntry> contextMenuEntries = new ArrayList<EasyEditMenu.ContextMenuEntry>();
	
	private final EasyEditManager manager;
	
	/*package*/ EasyEditMenu(EasyEditManager manager) {
		this.manager = manager;
	}

	public void add(List<? extends OsmElement> elements, ContextMenuAction action) {
		for (OsmElement element : elements) add(element, action, null);
	}
	
	public void add(OsmElement element, ContextMenuAction action, String description) {
		contextMenuEntries.add(new ContextMenuEntry(element, action, description));
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		for (int i = 0; i < contextMenuEntries.size(); i++) {
			ContextMenuEntry entry = contextMenuEntries.get(i);
			menu.add(Menu.NONE, i, Menu.NONE, entry.getDescription()).setOnMenuItemClickListener(this);
		}
	}


	public boolean onMenuItemClick(android.view.MenuItem item) {
		ContextMenuEntry entry = contextMenuEntries.get(item.getItemId());
		switch (entry.action) {
			case HANDLE_NODE_CLICK:
				manager.handleNodeClick((Node)entry.element);
				break;
			case TAG_EDIT:
				manager.performTagEdit(entry.element);
				break;
			case DELETE_NODE:
				manager.performDeleteNode((Node)entry.element);
				break;
		}
		return true;
	}

	public void trigger() {
		manager.triggerMapContextMenu();
	}
	
	private class ContextMenuEntry {
		final OsmElement element;
		final ContextMenuAction action;
		final String description;
		
		private ContextMenuEntry(OsmElement element, ContextMenuAction action, String description) {
			this.element = element;
			this.action = action;
			this.description = description;
		}
	
		public CharSequence getDescription() {
			if (description != null) return description;
			switch (action) {
			case HANDLE_NODE_CLICK: return element.getDescription();
			case TAG_EDIT: return "Edit " + element.getDescription();
			case DELETE_NODE: return "Delete " + element.getDescription();
			default: return "MISSING MENU CASE";
			}
		}
	}

	public enum ContextMenuAction {HANDLE_NODE_CLICK, TAG_EDIT, DELETE_NODE}
}
