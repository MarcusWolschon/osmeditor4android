package de.blau.android.propertyeditor;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.propertyeditor.TagEditorFragment.TagEditRow;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class TagSelectedActionModeCallback extends SelectedRowsActionModeCallback {
	
	private static final int MENU_ITEM_COPY = 2;
	private static final int MENU_ITEM_CUT = 3;

	public TagSelectedActionModeCallback(Fragment caller, LinearLayout rows) {
		super(caller, rows);
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		Context context = caller.getActivity();
		menu.add(Menu.NONE, MENU_ITEM_DELETE, Menu.NONE, R.string.delete)
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_delete));
		menu.add(Menu.NONE, MENU_ITEM_COPY, Menu.NONE, R.string.menu_copy)
				.setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_copy))
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_copy));
		menu.add(Menu.NONE, MENU_ITEM_CUT, Menu.NONE, R.string.menu_cut)
				.setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_cut))
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_cut));
		menu.add(Menu.NONE, MENU_ITEM_HELP, Menu.NONE, R.string.menu_help)
				.setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_help))
				.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_help));
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
			}
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENU_ITEM_COPY:
			copyTags(selected, false);
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
		case MENU_ITEM_HELP:
			HelpViewer.start(Application.mainActivity, R.string.help_propertyeditor);
			return true;
		default: return false;
		}
		return true;
	}

}
