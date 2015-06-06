package de.blau.android.actionbar;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.util.Density;

/** Adapter providing icons for the ActionBar edit mode dropdown */
public class ModeDropdownAdapter implements SpinnerAdapter {
	
	private ArrayList<DropdownItem> items = new ArrayList<DropdownItem>();

	private Context context;

	private boolean showOpenStreetBug;
	
	private boolean depreciatedModesEnabled;

	private HashSet<DataSetObserver> observers = new HashSet<DataSetObserver>();
	
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
		private TextView getView(boolean pad) {
			TextView view = new TextView(context);
			int padding = Density.dpToPx(10); //TODO create constant
			if (pad) view.setPadding(padding, padding, padding, padding);
			view.setText(label);
			view.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			view.setCompoundDrawablePadding(padding);
			return view;
		}
	}
	
	public ModeDropdownAdapter(Context context, boolean showOpenStreetBug, boolean depreciatedModesEnabled) {
		this.context = context;
		addItem(Logic.Mode.MODE_MOVE, R.string.menu_move, R.drawable.locked_small);
		addItem(Logic.Mode.MODE_EASYEDIT, R.string.menu_easyedit, R.drawable.menu_edit);
		addItem(Logic.Mode.MODE_ADD, R.string.menu_add, R.drawable.menu_add);
		addItem(Logic.Mode.MODE_TAG_EDIT, R.string.menu_tag, R.drawable.menu_tag);
		addItem(Logic.Mode.MODE_ERASE, R.string.menu_erase, R.drawable.menu_erase);
		addItem(Logic.Mode.MODE_SPLIT, R.string.menu_split, R.drawable.menu_split);
		// OpenStreetBug item must be last so it can be easily hidden
		addItem(Logic.Mode.MODE_OPENSTREETBUG, R.string.menu_openstreetbug, R.drawable.menu_openstreetbug);
		this.showOpenStreetBug = showOpenStreetBug;
		this.depreciatedModesEnabled = depreciatedModesEnabled;
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
	
	/**
	 * Returns the index of the item matching a certain mode
	 * @param mode logic mode
	 * @return the item index (defaults to 0 if no matching item was found)
	 */
	public int getIndexForMode(Logic.Mode mode) {
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).resultingMode == mode) return i;
		}
		return 0;
	}
	
	@Override
	public int getCount() {
		return (showOpenStreetBug ? items.size() : items.size() - 1);
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
		observers.add(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		observers.remove(observer);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return items.get(position).getView(true);
	}
	
	/**
	 * Sets whether the OpenStreetBug menu item should be shown, calling change observers where needed
	 * @param show true if the OSB item should be shown, false if not
	 * @return true if the setting was changed, false if the new value matched the current value
	 */
	public boolean setShowOpenStreetBug(boolean show) {
		if (show == showOpenStreetBug) return false;
		showOpenStreetBug = show;
		for (DataSetObserver observer : observers) {
			observer.onChanged();
		}
		return true;
	}

}
