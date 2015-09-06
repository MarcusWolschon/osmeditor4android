package de.blau.android.prefs;

import java.util.List;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;

import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

/** Provides an activity for editing the API list */
public class APIEditorActivity extends URLListEditActivity {

	private AdvancedPrefDatabase db;
	
	public APIEditorActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_Sherlock_Light);
		}
		db = new AdvancedPrefDatabase(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected int getAddTextResId() {
		return R.string.urldialog_add_api;
	}
	
	@Override
	protected void onLoadList(List<ListEditItem> items) {
		API[] apis = db.getAPIs();
		API current = db.getCurrentAPI();
		for (API api : apis) {
			items.add(new ListEditItem(api.id, api.name, api.url, api.oauth, current.id.equals(api.id)));
		}
	}

	@Override
	protected void onItemClicked(ListEditItem item) {
		db.selectAPI(item.id);
		// this is a bit hackish
		for (ListEditItem lei:items) {
			lei.active = false;
		}
		item.active = true;
		updateAdapter();
		// finish();
	}

	@Override
	protected void onItemCreated(ListEditItem item) {
		db.addAPI(item.id, item.name, item.value, "", "", "", false, item.enabled);
	}

	@Override
	protected void onItemEdited(ListEditItem item) {
		db.setAPIDescriptors(item.id, item.name, item.value, item.enabled);
	}

	@Override
	protected void onItemDeleted(ListEditItem item) {
		db.deleteAPI(item.id);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		selectedItem = (ListEditItem)getListView().getItemAtPosition(info.position);
		if (selectedItem != null ) {
			menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(R.string.edit)).setOnMenuItemClickListener(this);
			if (!selectedItem.id.equals(LISTITEM_ID_DEFAULT)) {
				menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, r.getString(R.string.delete)).setOnMenuItemClickListener(this);
				for (Entry<Integer, Integer> entry : additionalMenuItems.entrySet() ) {
					menu.add(Menu.NONE, entry.getKey() + MENUITEM_ADDITIONAL_OFFSET, Menu.NONE,	r.getString(entry.getValue()))
						.setOnMenuItemClickListener(this);
				}
			}
		}
	}
	
 	/**
 	 * Opens the dialog to edit an item
 	 * @param item the selected item
 	 */
	@Override
	protected void itemEditDialog(final ListEditItem item) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		final View mainView = View.inflate(ctx, R.layout.listedit_apiedit, null);
		final TextView editName = (TextView)mainView.findViewById(R.id.listedit_editName);
		final TextView editValue = (TextView)mainView.findViewById(R.id.listedit_editValue);
		final CheckBox oauth = (CheckBox)mainView.findViewById(R.id.listedit_oauth);
		
		if (item != null) {
			editName.setText(item.name);
			editValue.setText(item.value);
			oauth.setChecked(item.enabled);
		} else if (isAddingViaIntent()) {
			String tmpName = getIntent().getExtras().getString(EXTRA_NAME);
			String tmpValue = getIntent().getExtras().getString(EXTRA_VALUE);
			editName.setText(tmpName == null ? "" : tmpName);
			editValue.setText(tmpValue == null ? "" : tmpValue);
			oauth.setChecked(false);
		}
		if (item != null && item.id.equals(LISTITEM_ID_DEFAULT)) {
			// name and value are not editable
			editName.setEnabled(false);
			editValue.setEnabled(false);
		}
		
		builder.setView(mainView);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String name = editName.getText().toString();
				String value = editValue.getText().toString();
				boolean enabled = oauth.isChecked();
				if (item == null) {
					// new item
					if (!value.equals("")) {
						finishCreateItem(new ListEditItem(name, value, oauth.isChecked()));
					}
				} else {
					item.name = name;
					item.value = value;
					item.enabled = enabled;
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
}
