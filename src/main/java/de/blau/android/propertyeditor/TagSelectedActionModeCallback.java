package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.propertyeditor.TagEditorFragment.TagEditRow;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class TagSelectedActionModeCallback extends SelectedRowsActionModeCallback {
	
	// pm: protected static final int MENU_ITEM_DELETE = 1;
	// pm: protected static final int MENU_ITEM_HELP = 15;
	private static final int MENU_ITEM_COPY = 2;
	private static final int MENU_ITEM_CUT = 3;

	public TagSelectedActionModeCallback(Fragment caller, LinearLayout rows) {
		super(caller, rows);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		super.onCreateActionMode(mode, menu);
		mode.setTitle(R.string.tag_action_tag_title);
		return true;
	}
	
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		super.onPrepareActionMode(mode, menu);
		Context context = caller.getActivity();
		menu.add(Menu.NONE, MENU_ITEM_COPY, Menu.NONE, R.string.menu_copy)
				.setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_copy))
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_copy));
		menu.add(Menu.NONE, MENU_ITEM_CUT, Menu.NONE, R.string.menu_cut)
				.setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_cut))
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_cut));
		return true;
	}

	private void copyTags(@NonNull ArrayList<TagEditRow> selectedRows, boolean deleteEachRow) {
		if (selectedRows.size() > 0) {
			TagEditorFragment fragment = (TagEditorFragment) caller;
			fragment.copiedTags = new LinkedHashMap<String,String>();
			for (TagEditRow row : selectedRows) {
				addKeyValue(fragment.copiedTags, row);
				if (deleteEachRow) {
					row.delete();
				}
			}
			ClipboardUtils.copyTags(fragment.getActivity(), fragment.copiedTags);
		}
	}

	private void addKeyValue(Map<String, String>tags, final TagEditRow row) {
		String key = row.getKey().trim();
		String value = row.getValue().trim();
		boolean bothBlank = "".equals(key) && "".equals(value);
		boolean neitherBlank = !"".equals(key) && !"".equals(value);
		if (!bothBlank) {
			// both blank is never acceptable
			if (neitherBlank) {
				tags.put(key, value);
			}
		}
	}
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		return performAction(item.getItemId());
	}
	
	private boolean performAction(int action) {
		
		final int size = rows.getChildCount();
		ArrayList<TagEditRow> selected = new ArrayList<TagEditRow>();
		for (int i = 0; i < size; ++i) {
			View view = rows.getChildAt(i);
			TagEditRow row = (TagEditRow)view;
			if (row.isSelected()) {
				selected.add(row);
			}
		}
		switch (action) {
		case MENU_ITEM_DELETE:
			if (selected.size() > 0) {
				for (TagEditRow r:selected) {
					r.delete();
				}
				((TagEditorFragment)caller).updateAutocompletePresetItem(null);
			}
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENU_ITEM_COPY:
			copyTags(selected, false);
			((TagEditorFragment)caller).updateAutocompletePresetItem(null);
			if (currentAction != null) {
				currentAction.finish();
			} 
			break;
		case MENU_ITEM_CUT:
			copyTags(selected, true);
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENU_ITEM_SELECT_ALL:
			((PropertyRows) caller).selectAllRows();
			return true;
		case MENU_ITEM_DESELECT_ALL:
			((PropertyRows) caller).deselectAllRows();
			return true;
		case MENU_ITEM_HELP:
			HelpViewer.start(caller.getActivity(), R.string.help_propertyeditor);
			return true;
		default: return false;
		}
		return true;
	}

}
