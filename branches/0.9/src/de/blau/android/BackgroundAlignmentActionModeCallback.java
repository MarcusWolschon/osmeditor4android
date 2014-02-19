package de.blau.android;



import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.ActionMode.Callback;

import de.blau.android.Logic.Mode;
import de.blau.android.views.util.OpenStreetMapTileServer;

public class BackgroundAlignmentActionModeCallback implements Callback {
	
	private static final int MENUITEM_RESET = 1;
	Mode oldMode;
	double oldOffsetLon;
	double oldOffsetLat;
	OpenStreetMapTileServer osmts;
	Map map;
	
	public BackgroundAlignmentActionModeCallback(Mode oldMode) {
		this.oldMode = oldMode;
		map = Application.mainActivity.getMap();
		osmts = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		oldOffsetLon = osmts.getLonOffset();
		oldOffsetLat = osmts.getLatOffset();
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.setTitle(R.string.menu_tools_background_align);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		menu.add(Menu.NONE, MENUITEM_RESET, Menu.NONE, R.string.menu_tools_background_align_reset);
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case MENUITEM_RESET: 
			osmts.setLatOffset(0.0d);
			osmts.setLonOffset(0.0d);
			map.invalidate();
			break;
		default: return false;
		}
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		Application.mainActivity.logic.setMode(oldMode);

	}

}
