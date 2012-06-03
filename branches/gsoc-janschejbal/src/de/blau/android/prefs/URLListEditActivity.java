package de.blau.android.prefs;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.actionbarsherlock.view.Menu;

import de.blau.android.R;

/**
 * This activity allows the user to edit a list of URLs.
 * Each entry consists of a unique ID, a name and a URL.
 * The user can add new entries via a button and edit/delete existing entries by long-pressing them.
 * Entries with {@link #LISTITEM_ID_DEFAULT} as their ID cannot be edited/deleted by the user.
 * 
 * 
 * You will probably want to override {@link #onItemClicked(AdapterView, View, int, long)},
 * {@link #onItemCreated(ListEditItem)}, {@link #onItemEdited(ListEditItem)} and 
 * {@link #onItemDeleted(ListEditItem)}.
 * @author Jan
 *
 */
public abstract class URLListEditActivity extends ListActivity implements OnMenuItemClickListener, OnItemClickListener {

	
	protected Resources r;
	protected final Context ctx;

	protected static final int MENUITEM_EDIT = 0;
	protected static final int MENUITEM_DELETE = 1;
	protected static final String LISTITEM_ID_DEFAULT = AdvancedPrefDatabase.ID_DEFAULT;

	private ListAdapter adapter;
	protected final List<ListEditItem> items;

	private ListEditItem selectedItem = null;
	
	public URLListEditActivity() {
		this.ctx = this; // Change when changing Activity to Fragment
		this.items = new ArrayList<URLListEditActivity.ListEditItem>();
	}
	
	public URLListEditActivity(List<ListEditItem> items) {
		this.ctx = this; // Change when changing Activity to Fragment
		this.items = items;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		r = getResources();

		this.onLoadList(items);
		
		TextView v = (TextView)View.inflate(ctx, android.R.layout.simple_list_item_1, null);
		v.setText(r.getString(R.string.add));
		v.setTextColor(ctx.getResources().getColor(android.R.color.darker_gray));
		v.setTypeface(null,Typeface.ITALIC);
		this.getListView().addFooterView(v);
		
		this.updateAdapter();

		this.getListView().setOnItemClickListener(this);
		this.getListView().setLongClickable(true);
		this.getListView().setOnCreateContextMenuListener(this);
	}



	protected void updateAdapter() {
		adapter = new ListEditAdapter(ctx, items);
		setListAdapter(adapter);		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		Object item = parent.getItemAtPosition(pos);
		if (item == null) {
			// clicked on "new" button
			itemEditDialog(null);
		} else {
			onItemClicked((ListEditItem)item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		selectedItem = (ListEditItem)this.getListView().getItemAtPosition(info.position);
		if (selectedItem != null && !selectedItem.id.equals(LISTITEM_ID_DEFAULT)) {
			menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(R.string.edit)).setOnMenuItemClickListener(this);
			menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, r.getString(R.string.delete)).setOnMenuItemClickListener(this);
		}
	}


 	@Override
	public boolean onMenuItemClick(MenuItem menuitem) {
		switch (menuitem.getItemId()) {
		case MENUITEM_EDIT:
			itemEditDialog(selectedItem);
			updateAdapter();
			break;
		case MENUITEM_DELETE:
			deleteItem(selectedItem);
			break;
		}

		return true;
	}

 	/**
 	 * Opens the dialog to edit an item
 	 * @param item the selected item
 	 */
	private void itemEditDialog(final ListEditItem item) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		final View mainView = View.inflate(ctx, R.layout.listedit_edit, null);
		final TextView editName = (TextView)mainView.findViewById(R.id.listedit_editName);
		final TextView editValue = (TextView)mainView.findViewById(R.id.listedit_editValue);
		if (item != null) {
			editName.setText(item.name);
			editValue.setText(item.value);
		}
		
		builder.setView(mainView);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String name = editName.getText().toString();
				String value = editValue.getText().toString();
				if (item == null) {
					// new item
					if (!value.equals("")) {
						finishCreateItem(new ListEditItem(name, value));
					}
				} else {
					item.name = name;
					item.value = value;
					finishEditItem(item);
				}
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		
		builder.show();
	}
	

	/**
	 * Called by {@link #itemEditDialog(ListEditItem)} when an item is successfully created
	 * @param item the new item
	 */
	private void finishCreateItem(ListEditItem item) {
		items.add(item);
		updateAdapter();
		onItemCreated(item);
	}

	/**
	 * Called by {@link #itemEditDialog(ListEditItem)} when an item is successfully edited
	 * @param item the new item
	 */
	private void finishEditItem(ListEditItem item) {
		updateAdapter();
		onItemEdited(item);
	}

	/**
	 * Deletes an item
	 * @param item
	 */
	private void deleteItem(ListEditItem item) {
		if (items.remove(item)) {
			updateAdapter();
			onItemDeleted(item);
		}
	}

	/**
	 * Called when the list should be loaded.
	 * Override this and fill the list given to you
	 * @param items 
	 * @param item the created item
	 */
	protected abstract void onLoadList(List<ListEditItem> items);
	
	/**
	 * Called when an item is clicked. Override to handle this event.
	 * @param item the created item
	 */
	protected abstract void onItemClicked(ListEditItem item);
	
	/**
	 * Called when an item is created. Override to handle this event.
	 * @param item the created item
	 */
	protected abstract void onItemCreated(ListEditItem item);

	/**
	 * Called when an item is edited. Override to handle this event.
	 * @param item the new state of the item
	 */
	protected abstract void onItemEdited(ListEditItem item);

	/**
	 * Called when an item is deleted. Override to handle this event.
	 * @param item the item that was deleted
	 */
	protected abstract void onItemDeleted(ListEditItem item);


	/**
	 * 
	 * @author Jan
	 */
	public class ListEditItem {
		public final String id;
		public String name;
		public String value;
		
		/**
		 * Create a new item with a new, random UUID and the given name and value
		 * @param name
		 * @param value
		 */
		public ListEditItem(String name, String value) {
			this.id = java.util.UUID.randomUUID().toString();
			this.value = value;
			this.name = name;
		}
		
		/**
		 * Create an item with the given id, name and value.
		 * You are responsible for keeping the IDs unique!
		 * @param id
		 * @param name
		 * @param value
		 */
		public ListEditItem(String id, String name, String value) {
			this.id = id;
			this.value = value;
			this.name = name;
		}

		@Override
		public String toString() {
			return value;
		}
	}
	
	/**
	 * This adapter provides two-line item views for the list view
	 * @author Jan
	 */
	private class ListEditAdapter extends ArrayAdapter<ListEditItem> {

		public ListEditAdapter(Context context, List<ListEditItem> items) {
			super(context, android.R.layout.simple_list_item_2, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TwoLineListItem v;
			if (convertView != null && TwoLineListItem.class.isInstance(convertView.getClass())) {
				v = (TwoLineListItem)convertView;
			} else {
				v = (TwoLineListItem)View.inflate(ctx, android.R.layout.simple_list_item_2, null);
			}
			v.getText1().setText(getItem(position).name);
			v.getText2().setText(getItem(position).value);
			return v;
		}
		
	}

	public List<ListEditItem> getItems() {
		return items;
	}

}
