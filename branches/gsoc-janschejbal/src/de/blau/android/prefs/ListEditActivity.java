package de.blau.android.prefs;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import de.blau.android.R;

public class ListEditActivity extends ListActivity implements OnMenuItemClickListener, OnItemClickListener {


	private static final int MENUITEM_EDIT = 0;
	private static final int MENUITEM_DELETE = 1;
	private static final String LISTITEM_ID_DEFAULT = "DEFAULT";

	private ListAdapter adapter;
	private final List<ListEditItem> items;

	private ListEditItem selectedItem = null;
	private Context ctx;
	
	
	public ListEditActivity() {
		this.ctx = this; // Change when changing Activity to Fragment
		this.items = new ArrayList<ListEditActivity.ListEditItem>();
		items.add(new ListEditItem("Eins"));
		items.add(new ListEditItem("Zweiundzwanzig"));
	}

	public ListEditActivity(List<ListEditItem> items) {
		this.ctx = this; // Change when changing Activity to Fragment
		this.items = items;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		TextView v = (TextView)View.inflate(ctx, android.R.layout.simple_list_item_1, null);
		v.setText("TODO LOC Add new");
		v.setTextColor(ctx.getResources().getColor(android.R.color.darker_gray));
		v.setTypeface(null,Typeface.ITALIC);
		this.getListView().addFooterView(v);
		
		this.updateAdapter();

		this.getListView().setOnItemClickListener(this);
		this.getListView().setLongClickable(true);
		this.getListView().setOnCreateContextMenuListener(this);
		//this.getListView().setOnItemLongClickListener(this);
	}



	private void updateAdapter() {
		adapter = new ArrayAdapter<ListEditItem>(ctx, android.R.layout.simple_list_item_1, items);
		setListAdapter(adapter);		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		Object item = parent.getItemAtPosition(pos);
		if (item == null) {
			// "new" button
			itemEditDialog(null);
		} else {
			handleClick((ListEditItem)item);
		}
	}

	private void handleClick(ListEditItem item) {
		//TODO
		Toast.makeText(ctx, item.toString(), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		selectedItem = (ListEditItem)this.getListView().getItemAtPosition(info.position);
		if (selectedItem != null && selectedItem.id != LISTITEM_ID_DEFAULT) {
			menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, "TODOLOC EDIT").setOnMenuItemClickListener(this);
			menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, "TODOLOC DELETE").setOnMenuItemClickListener(this);
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

	private void itemEditDialog(final ListEditItem item) {
		AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
		alert.setTitle("Title");
		alert.setMessage("Message");
		final EditText input = new EditText(ctx);
		if (item != null) input.setText(item.value);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (item == null) {
					// new item
					if (!value.equals("")) {
						finishCreateItem(new ListEditItem(value));
					}
				} else {
					if (!item.value.equals(value)) {
						item.value = value;
						finishEditItem(item);
					}
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
			}
		});
		
		alert.show();
	}
	

	
	private void finishCreateItem(ListEditItem item) {
		items.add(item);
		updateAdapter();
		// TODO notify
	}

	private void finishEditItem(ListEditItem item) {
		updateAdapter();
		// TODO notify
	}


	private void deleteItem(ListEditItem item) {
		if (items.remove(item)) {
			updateAdapter();
			// TODO notify
		}
	}


	public class ListEditItem {
		public final String id;
		public String value;

		public ListEditItem(String value) {
			this.id = java.util.UUID.randomUUID().toString();
			this.value = value;
		}

		public ListEditItem(String id, String value ) {
			this.id = id;
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

}
