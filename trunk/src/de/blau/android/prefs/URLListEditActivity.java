package de.blau.android.prefs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

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
public abstract class URLListEditActivity extends SherlockListActivity implements OnMenuItemClickListener, android.view.MenuItem.OnMenuItemClickListener, OnItemClickListener {

	public static final String ACTION_NEW = "new";
	public static final String EXTRA_NAME = "name";
	public static final String EXTRA_VALUE = "value";
	public static final String EXTRA_ITEM = "item";
	
	protected Resources r;
	protected final Context ctx;

	protected static final int MENUITEM_EDIT = 0;
	protected static final int MENUITEM_DELETE = 1;
	protected static final int MENUITEM_ADDITIONAL_OFFSET = 1000;
	
	protected static final String LISTITEM_ID_DEFAULT = AdvancedPrefDatabase.ID_DEFAULT;
	private ListAdapter adapter;
	protected final List<ListEditItem> items;

	protected ListEditItem selectedItem = null;
	
	private boolean addingViaIntent = false;
	protected final LinkedHashMap<Integer, Integer> additionalMenuItems = new LinkedHashMap<Integer, Integer>();
	
	public URLListEditActivity() {
		ctx = this; // Change when changing Activity to Fragment
		items = new ArrayList<URLListEditActivity.ListEditItem>();
	}
	
	public URLListEditActivity(List<ListEditItem> items) {
		ctx = this; // Change when changing Activity to Fragment
		this.items = items;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		r = getResources();
		onLoadList(items);
		TextView v = (TextView)View.inflate(ctx, android.R.layout.simple_list_item_1, null);
		v.setText(r.getString(getAddTextResId()));
		v.setTextColor(ctx.getResources().getColor(android.R.color.darker_gray));
		v.setTypeface(null,Typeface.ITALIC);
		getListView().addFooterView(v);
		
		updateAdapter();

		getListView().setOnItemClickListener(this);
		getListView().setLongClickable(true);
		getListView().setOnCreateContextMenuListener(this);
		
		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		addingViaIntent = ((getIntent() != null && ACTION_NEW.equals(getIntent().getAction())));
		if (isAddingViaIntent()) {
			itemEditDialog(null);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (!super.onOptionsItemSelected(item)) {
			switch (item.getItemId()) {
			case android.R.id.home: finish(); break;
			default: return false;
			}
		}
		return true;
	}

	/** refreshes the data adapter (list content) */
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
			Log.d("URLListEditActivity","Item clicked");
			ListItem listItem = (ListItem)view;
			listItem.setChecked(!listItem.isChecked());
			onItemClicked((ListEditItem)item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		selectedItem = (ListEditItem)getListView().getItemAtPosition(info.position);
		if (selectedItem != null && !selectedItem.id.equals(LISTITEM_ID_DEFAULT) ) {
			menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(R.string.edit)).setOnMenuItemClickListener(this);
			menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, r.getString(R.string.delete)).setOnMenuItemClickListener(this);
			for (Entry<Integer, Integer> entry : additionalMenuItems.entrySet() ) {
				menu.add(Menu.NONE, entry.getKey() + MENUITEM_ADDITIONAL_OFFSET, Menu.NONE,	r.getString(entry.getValue()))
					.setOnMenuItemClickListener(this);
			}
		}
	}
	
	private boolean onMenuItemClick(int itemId) {
		if (itemId >= MENUITEM_ADDITIONAL_OFFSET) {
			onAdditionalMenuItemClick(itemId - MENUITEM_ADDITIONAL_OFFSET, selectedItem);
		}
		switch (itemId) {
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
	
	@Override
	public boolean onMenuItemClick(MenuItem menuitem) {
		return onMenuItemClick(menuitem.getItemId());
	}
	
	@Override
	public boolean onMenuItemClick(android.view.MenuItem menuitem) {
		return onMenuItemClick(menuitem.getItemId());
	}
	
	/**
	 * Add an additional menu item. Override {@link #onAdditionalMenuItemClick(int, ListEditItem)} to handle it.
	 * @param menuId a non-negative integer by which you will recognize the menu item
	 * @param stringId the resource id of the string that will be the name of the menu item
	 */
	protected void addAdditionalContextMenuItem(int menuId, int stringId) {
		additionalMenuItems.put(menuId, stringId);
	}
 	
 	/**
 	 * Override this to handle additional menu item clicks.
 	 * Use {@link #addAdditionalContextMenuItem(int, int)} to add menu items.
 	 * @param menuItemId the menu item ID supplied when creating the additional menu
 	 * @param clickedItem the item for which the context menu was opened
 	 */
 	public void onAdditionalMenuItemClick(int menuItemId, ListEditItem clickedItem) {
		// default: nothing, override if needed
	}

 	/**
 	 * Opens the dialog to edit an item
 	 * @param item the selected item
 	 */
	protected void itemEditDialog(final ListEditItem item) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		final View mainView = View.inflate(ctx, R.layout.listedit_edit, null);
		final TextView editName = (TextView)mainView.findViewById(R.id.listedit_editName);
		final TextView editValue = (TextView)mainView.findViewById(R.id.listedit_editValue);
		if (item != null) {
			editName.setText(item.name);
			editValue.setText(item.value);
		} else if (isAddingViaIntent()) {
			String tmpName = getIntent().getExtras().getString(EXTRA_NAME);
			String tmpValue = getIntent().getExtras().getString(EXTRA_VALUE);
			editName.setText(tmpName == null ? "" : tmpName);
			editValue.setText(tmpValue == null ? "" : tmpValue);
		}
		
		builder.setView(mainView);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
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
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		
		builder.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				if (isAddingViaIntent()) {
					setResult(RESULT_CANCELED);
					finish();
				}
			}
		});
		
		builder.show();
	}
	

	/**
	 * Called by {@link #itemEditDialog(ListEditItem)} when an item is successfully created
	 * @param item the new item
	 */
	protected void finishCreateItem(ListEditItem item) {
		items.add(item);
		updateAdapter();
		onItemCreated(item);
		
		if (canAutoClose()) sendResultIfApplicable(item);
	}
	
	/**
	 * If this editor {@link #isAddingViaIntent()},
	 * finishes the activity (sending RESULT_OK with the given item) 
	 * @param item created/edited item to send as result
	 */
	protected void sendResultIfApplicable(ListEditItem item) {
		if (isAddingViaIntent()) {
			Intent intent = new Intent();
			intent.putExtra(EXTRA_ITEM, item);
			setResult(RESULT_OK, intent);
			finish();
		}		
	}
	
	/**
	 * Override this if you need to keep the dialog open after an intent-initiated edit event
	 * (e.g. to finish downloading preset data).
	 * You are responsible for finishing the activity and sending the result if you return false.
	 * You will probably want to use {@link #sendResultIfApplicable(ListEditItem)}
	 * @return false to stop the dialog from closing automatically after an intent-initiated edit event
	 */
	protected boolean canAutoClose() {
		return true;
	}

	/**
	 * Called by {@link #itemEditDialog(ListEditItem)} when an item is successfully edited
	 * @param item the new item
	 */
	protected void finishEditItem(ListEditItem item) {
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
	
	protected abstract int getAddTextResId();

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
	public static class ListEditItem implements Serializable {
		private static final long serialVersionUID = 7574708515164503466L;
		public final String id;
		public String name;
		public String value;
		public boolean enabled;
		public boolean active;
		
		/**
		 * Create a new item with a new, random UUID and the given name and value
		 * @param name
		 * @param value
		 */
		public ListEditItem(String name, String value) {
			this(name, value, false);
		}
		public ListEditItem(String name, String value, boolean enabled) {
			id = java.util.UUID.randomUUID().toString();
			this.value = value;
			this.name = name;
			this.enabled = enabled;
			this.active = false;
		}
		
		/**
		 * Create an item with the given id, name and value.
		 * You are responsible for keeping the IDs unique!
		 * @param id
		 * @param name
		 * @param value
		 */
		public ListEditItem(String id, String name, String value) {
			this(id, name, value, false);
		}
		
		public ListEditItem(String id, String name, String value, boolean enabled) {
			this.id = id;
			this.value = value;
			this.name = name;
			this.enabled = enabled;
			this.active = false;
		}
		
		public ListEditItem(String id, String name, String value, boolean enabled, boolean active) {
			this.id = id;
			this.value = value;
			this.name = name;
			this.enabled = enabled;
			this.active = active;
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
			super(context, R.layout.list_item, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ListItem v;
			if (convertView instanceof ListItem) {
				v = (ListItem)convertView;
			} else {
				v = (ListItem)View.inflate(ctx, R.layout.list_item, null);
			}
			v.setText1(getItem(position).name);
			v.setText2(getItem(position).value);
			v.setChecked(getItem(position).active);
			return v;
		}	
	}

	public List<ListEditItem> getItems() {
		return items;
	}

	/**
	 * @return true if this editor has been called via an intent to add an entry
	 */
	public boolean isAddingViaIntent() {
		return addingViaIntent;
	}
	
	public static class ListItem extends LinearLayout {
		
		private TextView text1;
		private TextView text2;
		private CheckBox checkBox;
		
		public ListItem(Context context) {
			super(context);
		}
		
		public ListItem(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		public ListItem(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			
			text1 = (TextView)findViewById(R.id.listItemText1);
			text2 = (TextView)findViewById(R.id.listItemText2);
			checkBox = (CheckBox)findViewById(R.id.listItemCheckBox);
		}
		
		public void setChecked(boolean checked) {
			checkBox.setChecked(checked);
		}
		
		public boolean isChecked() {
			return checkBox.isChecked();
		}
		
		public void setText1(String txt) {
			text1.setText(txt);
		}
		
		public void setText2(String txt) {
			text2.setText(txt);
		}
	}
}
