package de.blau.android.propertyeditor;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.propertyeditor.TagEditorFragment.KeyValueHandler;
import de.blau.android.propertyeditor.TagEditorFragment.TagEditRow;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.ThemeUtils;

public class TagSelectedActionModeCallback implements Callback {
	
	private static final int MENUITEM_DELETE = 1;
	private static final int MENUITEM_COPY = 2;
	private static final int MENUITEM_CUT = 3;
	private static final int MENUITEM_HELP = 8;
	
	ActionMode currentAction;
	
	LinearLayout rows = null;
	TagEditorFragment caller = null;
	
	public TagSelectedActionModeCallback(TagEditorFragment caller, LinearLayout rows) {
		this.rows = rows;
		this.caller = caller;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.setTitle(R.string.tag_action_title);
		currentAction = mode;
		((PropertyEditor)caller.getActivity()).disablePaging();
		((PropertyEditor)caller.getActivity()).disablePresets();
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, R.string.delete).setIcon(ThemeUtils.getResIdFromAttribute(caller.getActivity(),R.attr.menu_delete));
		menu.add(Menu.NONE, MENUITEM_COPY, Menu.NONE, R.string.menu_copy).setIcon(ThemeUtils.getResIdFromAttribute(caller.getActivity(),R.attr.menu_copy));
		menu.add(Menu.NONE, MENUITEM_CUT, Menu.NONE, R.string.menu_cut).setIcon(ThemeUtils.getResIdFromAttribute(caller.getActivity(),R.attr.menu_cut));
		menu.add(Menu.NONE, MENUITEM_HELP, Menu.NONE, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(caller.getActivity(),R.attr.menu_help));
		return true;
	}

	public void addKeyValue(Map<String,String>tags, final TagEditRow row) {
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
		
		final int size = rows.getChildCount();
		ArrayList<TagEditRow> selected = new ArrayList<TagEditRow>();
		for (int i = 0; i < size; ++i) {
			View view = rows.getChildAt(i);
			TagEditRow row = (TagEditRow)view;
			if (row.isSelected()) {
				selected.add(row);
			}
		}
		switch (item.getItemId()) {
		case MENUITEM_DELETE: 
			if (selected.size() > 0) {
				for (TagEditRow r:selected) {
					r.deleteRow();
				}
			}
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENUITEM_COPY:
			if (selected.size() > 0) {
				caller.copiedTags = new LinkedHashMap<String,String>();
				for (TagEditRow r:selected) {
					addKeyValue(caller.copiedTags, r);
				}
				ClipboardUtils.copyTags(caller.getActivity(), caller.copiedTags);
			}
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENUITEM_CUT:
			if (selected.size() > 0) {
				caller.copiedTags = new LinkedHashMap<String,String>();
				for (TagEditRow r:selected) {
					addKeyValue(caller.copiedTags, r);
					r.deleteRow();
				}
				ClipboardUtils.copyTags(caller.getActivity(), caller.copiedTags);
			}
			if (currentAction != null) {
				currentAction.finish();
			}
			break;
		case MENUITEM_HELP:
			Intent startHelpViewer = new Intent(Application.mainActivity, HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, R.string.help_propertyeditor);
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
			TagEditRow row = (TagEditRow)view;
			row.deSelect();
		}
		currentAction = null;
		caller.deselectHeaderCheckBox();
		((PropertyEditor)caller.getActivity()).enablePaging();
		((PropertyEditor)caller.getActivity()).enablePresets();
		caller.tagSelectedActionModeCallback = null;
	}

	public boolean tagDeselected() {
		final int size = rows.getChildCount();
		for (int i = 0; i < size; ++i) {
			View view = rows.getChildAt(i);
			TagEditRow row = (TagEditRow)view;
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
