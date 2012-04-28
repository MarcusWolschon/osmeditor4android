package de.blau.android.actionbar;

import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import de.blau.android.Logic;
import de.blau.android.R;

public class ModeDropdownAdapter implements SpinnerAdapter {
	
	private ArrayList<DropdownItem> items = new ArrayList<DropdownItem>();

	private Context context;
	
	private class DropdownItem {
		private final Logic.Mode resultingMode;
		private final String label;
		private final Drawable icon;
		
		private DropdownItem(Logic.Mode resultingMode, int textResource, int iconResource) {
			this.resultingMode = resultingMode;
			this.label = context.getResources().getString(textResource);
			this.icon = context.getResources().getDrawable(iconResource);
		}
		
		/**
		 * Gets the view for this item
		 * 
		 * (Note that individual views need to be created for getView and getDropDownView,
		 * otherwise, you will get weird crashes)
		 * 
		 * @param pad true if padding should be added
		 * @return the created view
		 */
		private View getView(boolean pad) {
			TextView view = new TextView(context);
			if (pad) view.setPadding(20, 20, 20, 20);
			view.setText(label);
			view.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			view.setCompoundDrawablePadding(15);
			return view;
		}
	}
	
	public ModeDropdownAdapter(Context context, boolean showOpenStreetBug) {
		this.context = context;
		addItem(Logic.Mode.MODE_MOVE, R.string.menu_move, R.drawable.menu_move);
		addItem(Logic.Mode.MODE_ADD, R.string.menu_add, R.drawable.menu_add);
		addItem(Logic.Mode.MODE_EDIT, R.string.menu_edit, R.drawable.menu_edit);
		addItem(Logic.Mode.MODE_TAG_EDIT, R.string.menu_tag, R.drawable.menu_tag);
		addItem(Logic.Mode.MODE_APPEND, R.string.menu_append, R.drawable.menu_append);
		addItem(Logic.Mode.MODE_ERASE, R.string.menu_erase, R.drawable.menu_erase);
		addItem(Logic.Mode.MODE_SPLIT, R.string.menu_split, R.drawable.menu_split);
		if (showOpenStreetBug) {
			addItem(Logic.Mode.MODE_OPENSTREETBUG, R.string.menu_openstreetbug, R.drawable.menu_openstreetbug);
		}
	}
	

	/**
	 * Adds an item to the list
	 * @param resultingMode the Logic.Mode resulting from selecting the item
	 * @param textResource the resource id of the name of the item
	 * @param iconResource the resource id of the icon for the item
	 */
	private void addItem(Logic.Mode resultingMode, int textResource, int iconResource) {
		items.add(new DropdownItem(resultingMode, textResource, iconResource));
	}
	
	/**
	 * Returns the Logic.Mode associated with the item at the given index
	 * @param index the index of the selected item
	 * @return the Logic.Mode resulting from selecting this item
	 */
	public Logic.Mode getModeForItem(int index) {
		return items.get(index).resultingMode;
	}
	
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return items.get(position).getView(false);
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		// content does not change -> no observers
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		// content does not change -> no observers
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return items.get(position).getView(true);
	}

}
