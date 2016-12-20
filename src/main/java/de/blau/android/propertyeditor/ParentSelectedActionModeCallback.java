package de.blau.android.propertyeditor;

import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.widget.LinearLayout;
import de.blau.android.R;

public class ParentSelectedActionModeCallback extends SelectedRowsActionModeCallback {

	public ParentSelectedActionModeCallback(Fragment caller, LinearLayout rows) {
		super(caller, rows);
	}
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		super.onCreateActionMode(mode, menu);
		mode.setTitle(R.string.tag_action_parents_title);
		return true;
	}
}
