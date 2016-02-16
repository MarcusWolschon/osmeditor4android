package de.blau.android.propertyeditor;

import java.util.ArrayList;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.DialogFactory;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.RelationMembersFragment.Connected;
import de.blau.android.propertyeditor.RelationMembersFragment.RelationMemberRow;
import de.blau.android.util.ThemeUtils;

public class RelationMemberSelectedActionModeCallback extends SelectedRowsActionModeCallback {
	
	private static final String DEBUG_TAG = RelationMemberSelectedActionModeCallback.class.getSimpleName();
	
	// pm: protected static final int MENU_ITEM_DELETE = 1;
	// pm: private static final int MENU_ITEM_COPY = 2;
	// pm: private static final int MENU_ITEM_CUT = 3;
	// pm: protected static final int MENU_ITEM_HELP = 15;
	private static final int MENU_ITEM_MOVE_UP = 4;
	private static final int MENU_ITEM_MOVE_DOWN = 5;
	private static final int MENU_ITEM_SORT = 6;
	private static final int MENU_ITEM_DOWNLOAD = 7;
	private static final int MENU_ITEM_TOP = 8;
	private static final int MENU_ITEM_BOTTOM = 9;
	private static final int MENU_ITEM_MOVE_TOP = 10;
	private static final int MENU_ITEM_MOVE_BOTTOM = 11;
	

	public RelationMemberSelectedActionModeCallback(Fragment caller, LinearLayout rows) {
		super(caller, rows);
	}
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		super.onCreateActionMode(mode, menu);
		mode.setTitle(R.string.tag_action_members_title);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		super.onPrepareActionMode(mode, menu);
		Context context = caller.getActivity();
		
		menu.add(Menu.NONE, MENU_ITEM_MOVE_UP, Menu.NONE, R.string.tag_menu_move_up)
		.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_up));
		
		menu.add(Menu.NONE, MENU_ITEM_MOVE_DOWN, Menu.NONE, R.string.tag_menu_move_down)
		.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_down));
		
//		menu.add(Menu.NONE, MENU_ITEM_SORT, Menu.NONE, R.string.tag_menu_sort)
//		.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_sort));
		
		// we only display the download button if at least one of the selected elements isn't downloaded 
		boolean nonDownloadedSelected = false;
		final int size = rows.getChildCount();
		for (int i = 0; i < size; i++) {
			RelationMemberRow row = (RelationMemberRow) rows.getChildAt(i);
			if (row.isSelected() && !row.getRelationMemberDescription().downloaded()) {
				nonDownloadedSelected = true;
				break;
			}
		} 
		if (nonDownloadedSelected) {
			menu.add(Menu.NONE, MENU_ITEM_DOWNLOAD, Menu.NONE, R.string.tag_menu_download)
			.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_download));
		}
		
		menu.add(Menu.NONE, MENU_ITEM_TOP, Menu.NONE, R.string.tag_menu_top);
		
		menu.add(Menu.NONE, MENU_ITEM_BOTTOM, Menu.NONE, R.string.tag_menu_bottom);
		
		menu.add(Menu.NONE, MENU_ITEM_MOVE_TOP, Menu.NONE, R.string.tag_menu_move_top);
		
		menu.add(Menu.NONE, MENU_ITEM_MOVE_BOTTOM, Menu.NONE, R.string.tag_menu_move_bottom);
		
		return true;
	}
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		super.onActionItemClicked(mode, item);
		return performAction(item.getItemId());
	}
	
	private boolean performAction(int action) {

		final int size = rows.getChildCount();
		ArrayList<RelationMemberRow> selected = new ArrayList<RelationMemberRow>();
		ArrayList<Integer> selectedPos = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			View view = rows.getChildAt(i);
			RelationMemberRow row = (RelationMemberRow)view;
			if (row.isSelected()) {
				selected.add(row);
				selectedPos.add(i);
			}
		} 
		int selectedCount = selectedPos.size();
		int change = 1;
		switch (action) {
		case MENU_ITEM_DELETE: // nOTE real work is don in super
			((RelationMembersFragment) caller).setIcons();
			return true;
		case MENU_ITEM_MOVE_TOP:
			change = selectedPos.get(0).intValue();
		case MENU_ITEM_MOVE_UP:
			for (int i = 0;i<selectedCount;i++) {
				int p = selectedPos.get(i).intValue();
				int newPos = p - change;
				rows.removeViewAt(p);
				if (newPos < 0) {
					// one row removed at top. fix up positions
					selectedPos.set(i,size-1);
					for (int j=i+1;j<selectedCount;j++) {
						selectedPos.set(j,selectedPos.get(j)-1);
					}	
					rows.addView(selected.get(i)); // add at end
					break;
				} else {
					selectedPos.set(i,newPos);
					rows.addView(selected.get(i),newPos);
				}
			}
			// this has some heuristics to avoid the selected row vanishing behind the top bars
			((RelationMembersFragment)caller).scrollToRow(selected.get(0),true, action==MENU_ITEM_MOVE_TOP || forceScroll(selectedPos.get(0),size));
			((RelationMembersFragment)caller).setIcons();
			return true;
		case MENU_ITEM_MOVE_BOTTOM:
			change = size - selectedPos.get(selectedCount-1).intValue() -1;
		case MENU_ITEM_MOVE_DOWN:
			for (int i = selectedCount-1;i>=0;i--) {
				int p = selectedPos.get(i).intValue();
				int newPos = p + change;
				rows.removeViewAt(p);
				if (newPos > size -1) {
					// one row removed at bottom. fix up positions
					selectedPos.set(i,0);
					for (int j=i-1;j>=0;j--) {
						selectedPos.set(j,selectedPos.get(j)+1);
					}	
					rows.addView(selected.get(i),0); // add at end
					break;
				} else {
					selectedPos.set(i,newPos);
					rows.addView(selected.get(i),newPos);
				}
			}
			// this has some heuristics to avoid the selected row vanishing behind the bottom actionbar
			((RelationMembersFragment)caller).scrollToRow(selected.get(selected.size()-1),false, action==MENU_ITEM_MOVE_BOTTOM || forceScroll(selectedPos.get(selected.size()-1),size));
			((RelationMembersFragment)caller).setIcons();
			return true;
		case MENU_ITEM_TOP:
		case MENU_ITEM_BOTTOM:
			((RelationMembersFragment)caller).scrollToRow(null,action==MENU_ITEM_TOP,false);
			return true;
		case MENU_ITEM_DOWNLOAD:
			((SherlockFragmentActivity)caller.getActivity()).setSupportProgressBarIndeterminateVisibility(true);
			Preferences prefs = new Preferences(caller.getActivity());
			if (selectedCount < size) {
				for (int i = 0;i<selectedCount;i++) {
					RelationMemberRow row = selected.get(i);
					if (!row.getRelationMemberDescription().downloaded()) {
						if (Logic.downloadElement(caller.getActivity(), prefs.getServer(), row.getType(), row.getOsmId(), false, false) == 0) {
							updateRow(row, selectedPos.get(i));
							selected.set(i,row);
						}
					}
				}
			} else if (Logic.downloadElement(caller.getActivity(), prefs.getServer(), Relation.NAME, ((RelationMembersFragment)caller).getOsmId(), true, false) == 0) { // everything selected
				for (int i = 0;i<selectedCount;i++) {
					RelationMemberRow row = selected.get(i);
					if (!row.getRelationMemberDescription().downloaded()) {
						updateRow(row, selectedPos.get(i));
						selected.set(i,row);
					}
				}
				if (currentAction != null) { // finish and de-select
					currentAction.finish();
				}
			}
			((RelationMembersFragment)caller).setIcons();
			((SherlockFragmentActivity)caller.getActivity()).setSupportProgressBarIndeterminateVisibility(false);
			invalidate();
			return true;
		default: return false;
		}
	}
	
	private void updateRow(RelationMemberRow row, int pos) {
		RelationMemberDescription rmd = row.getRelationMemberDescription();
		rmd.setElement(rmd.getElement());
		rmd.update();
		row.delete();
		row = ((RelationMembersFragment)caller).insertNewMember(rows, pos +"", rmd, pos, Connected.NOT, true);
	}
	
	private boolean forceScroll(Integer pos, int size) {
		return pos.intValue()<3 || pos.intValue()>(size-4);
	}
}
