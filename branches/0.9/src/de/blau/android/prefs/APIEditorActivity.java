package de.blau.android.prefs;

import java.util.List;
import java.util.Map.Entry;

import com.actionbarsherlock.view.Menu;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.prefs.URLListEditActivity.ListEditItem;
import de.blau.android.util.OAuthHelper;
import de.blau.android.Application;
import de.blau.android.R;

/** Provides an activity for editing the API list */
public class APIEditorActivity extends URLListEditActivity {

	private AdvancedPrefDatabase db;
	
	public APIEditorActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
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
		for (API api : apis) {
			items.add(new ListEditItem(api.id, api.name, api.url, api.oauth));
		}
	}

	@Override
	protected void onItemClicked(ListEditItem item) {
		db.selectAPI(item.id);
		finish();
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
		if (item.id.equals(LISTITEM_ID_DEFAULT)) {
			// name and value are not editable
			editName.setEnabled(false);
			editValue.setEnabled(false);
		}
		
		builder.setView(mainView);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String name = editName.getText().toString();
				String value = editValue.getText().toString();
				boolean enabled = oauth.isChecked();
				if (item == null) {
					// new item
					if (!value.equals("")) {
						finishCreateItem(new ListEditItem(name, value));
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
