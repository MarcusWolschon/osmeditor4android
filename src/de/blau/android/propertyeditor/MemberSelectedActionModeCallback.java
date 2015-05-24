package de.blau.android.propertyeditor;



import java.util.ArrayList;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.propertyeditor.RelationMembersFragment.RelationMemberRow;

public class MemberSelectedActionModeCallback implements Callback {
	
	private static final int MENUITEM_DELETE = 1;
	private static final int MENUITEM_HELP = 8;
	
	ActionMode currentAction;
	
	LinearLayout rows = null;
	RelationMembersFragment caller = null;
	
	public MemberSelectedActionModeCallback(RelationMembersFragment caller, LinearLayout rows) {
		this.rows = rows;
		this.caller = caller;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.setTitle(R.string.tag_action_title);
		currentAction = mode;
		((PropertyEditor)caller.getActivity()).disablePaging();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, R.string.delete).setIcon(R.drawable.tag_menu_delete);
		menu.add(Menu.NONE, MENUITEM_HELP, Menu.NONE, R.string.menu_help);
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case MENUITEM_DELETE: 
			final int size = rows.getChildCount();
			ArrayList<RelationMemberRow> toDelete = new ArrayList<RelationMemberRow>();
			for (int i = 0; i < size; ++i) {
				View view = rows.getChildAt(i);
				RelationMemberRow row = (RelationMemberRow)view;
				if (row.isSelected()) {
					toDelete.add(row);
				}
			}
			if (toDelete.size() > 0) {
				for (RelationMemberRow r:toDelete) {
					r.deleteRow();
				}
			}
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENUITEM_HELP:
			Intent startHelpViewer = new Intent(Application.mainActivity, HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, mode.getTitle().toString());
			Application.mainActivity.startActivity(startHelpViewer);
			return true;
		default: return false;
		}
		return true;
	}
	
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		final int size = rows.getChildCount();
		for (int i = 0; i < size; ++i) { 
			View view = rows.getChildAt(i);
			RelationMemberRow row = (RelationMemberRow)view;
			row.deSelect();
		}
		((PropertyEditor)caller.getActivity()).enablePaging();
		caller.deselectHeaderCheckBox();
		currentAction = null;
		caller.memberSelectedActionModeCallback = null;
	}

	public boolean memberDeselected() {
		final int size = rows.getChildCount();
		for (int i = 1; i < size; ++i) { // > 1 skip header
			View view = rows.getChildAt(i);
			RelationMemberRow row = (RelationMemberRow)view;
			if (row.isSelected()) {
				// something is still selected
				return false;
			}
		}
		// nothing selected -> finish
		if (currentAction != null) {
			currentAction.finish();
		}
		return true;
	}
}
