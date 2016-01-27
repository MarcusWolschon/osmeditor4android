package de.blau.android.propertyeditor;

import java.util.ArrayList;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import de.blau.android.R;
import de.blau.android.propertyeditor.RelationMembersFragment.RelationMemberRow;
import de.blau.android.util.ThemeUtils;

public class RelationMemberSelectedActionModeCallback extends SelectedRowsActionModeCallback {
	
	private static final String DEBUG_TAG = RelationMemberSelectedActionModeCallback.class.getSimpleName();
	
	// pm: protected static final int MENU_ITEM_DELETE = 1;
	// pm: private static final int MENU_ITEM_COPY = 2;
	// pm: private static final int MENU_ITEM_CUT = 3;
	// pm: protected static final int MENU_ITEM_HELP = 15;
	private static final int MENU_ITEM_UP = 4;
	private static final int MENU_ITEM_DOWN = 5;
	private static final int MENU_ITEM_SORT = 6;
	private static final int MENU_ITEM_DOWNLOAD = 7;
	private static final int MENU_ITEM_TOP = 8;
	private static final int MENU_ITEM_BOTTOM = 9;
	

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
		
		menu.add(Menu.NONE, MENU_ITEM_UP, Menu.NONE, R.string.menu_up)
		.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_up));
		
		menu.add(Menu.NONE, MENU_ITEM_DOWN, Menu.NONE, R.string.menu_down)
		.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_down));
		
		menu.add(Menu.NONE, MENU_ITEM_SORT, Menu.NONE, R.string.menu_sort)
		.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_sort));
		
		menu.add(Menu.NONE, MENU_ITEM_DOWNLOAD, Menu.NONE, R.string.menu_download)
		.setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_download));
		
		menu.add(Menu.NONE, MENU_ITEM_TOP, Menu.NONE, R.string.menu_top);
		
		menu.add(Menu.NONE, MENU_ITEM_BOTTOM, Menu.NONE, R.string.menu_bottom);
		
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
				selectedPos.add(Integer.valueOf(i));
			}
		} 
		int selectedCount = selectedPos.size();
		int change = 1;
		switch (action) {
		case MENU_ITEM_TOP:
			change = selectedPos.get(0).intValue();
		case MENU_ITEM_UP:
			for (int i = 0;i<selectedCount;i++) {
				int p = selectedPos.get(i).intValue();
				int newPos = p - change;
				rows.removeViewAt(p);
				if (newPos < 0) {
					// one row removed at top. fix up positions
					selectedPos.set(i,size-1);
					for (int j=i+1;j<selectedCount;j++) {
						selectedPos.set(j,Integer.valueOf(selectedPos.get(j).intValue()-1));
					}	
					rows.addView(selected.get(i)); // add at end
				} else {
					selectedPos.set(i,newPos);
					rows.addView(selected.get(i),newPos);
				}
			}
			// this has some heuristics to avoid the selected row vanishing behind the top bars
			scrollToRow(selected.get(0),true, action==MENU_ITEM_TOP || selectedPos.get(0).intValue()<3);
			return true;
		case MENU_ITEM_BOTTOM:
			change = size - selectedPos.get(selectedCount-1).intValue() -1;
		case MENU_ITEM_DOWN:
			for (int i = selectedCount-1;i>=0;i--) {
				int p = selectedPos.get(i).intValue();
				int newPos = p + change;
				rows.removeViewAt(p);
				if (newPos > size -1) {
					// one row removed at bottom. fix up positions
					selectedPos.set(i,0);
					for (int j=i-1;j>=0;j--) {
						selectedPos.set(j,Integer.valueOf(selectedPos.get(j).intValue()+1));
					}	
					rows.addView(selected.get(i),0); // add at end
				} else {
					selectedPos.set(i,newPos);
					rows.addView(selected.get(i),newPos);
				}
			}
			// this has some heuristics to avoid the selected row vanishing behind the bottom actionbar
			scrollToRow(selected.get(selected.size()-1),false, action==MENU_ITEM_BOTTOM || selectedPos.get(selected.size()-1).intValue()>(size-4));
			return true;
		default: return false;
		}
	}

	private void scrollToRow(final RelationMemberRow row,final boolean up, boolean force) {	
		final View sv = caller.getView();
		Rect scrollBounds = new Rect();
		sv.getHitRect(scrollBounds);
		if (row.getLocalVisibleRect(scrollBounds)&& !force) {
			return; // already on screen
		} 
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				if (sv != null && sv instanceof ScrollView) { // should always be the case
					((ScrollView)sv).scrollTo(0, up ? row.getTop(): row.getBottom());
				}
			}
		});
	}
}
