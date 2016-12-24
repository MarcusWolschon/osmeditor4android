package de.blau.android.prefs;

import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;
import de.blau.android.util.ThemeUtils;

/** Provides an activity for editing the API list */
public class APIEditorActivity extends URLListEditActivity {

	private AdvancedPrefDatabase db;
	
	public APIEditorActivity() {
		super();
	}
	
	public static void startForResult(@NonNull Activity activity,
									  @NonNull String apiName,
									  @NonNull String apiUrl,
									  int requestCode) {
		Intent intent = new Intent(activity, APIEditorActivity.class);
		intent.setAction(ACTION_NEW);
		intent.putExtra(EXTRA_NAME, apiName);
		intent.putExtra(EXTRA_VALUE, apiUrl);
		activity.startActivityForResult(intent, requestCode);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customLight);
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
			items.add(new ListEditItem(api.id, api.name, api.url, api.readonlyurl, api.notesurl, api.oauth, current.id.equals(api.id)));
		}
	}

	@Override
	protected void onItemClicked(ListEditItem item) {
		db.selectAPI(item.id);
		// this is a bit hackish, but only one can be selected
		for (ListEditItem lei:items) {
			lei.active = false;
		}
		item.active = true;
		updateAdapter();
		// finish();
	}

	@Override
	protected void onItemCreated(ListEditItem item) {
		db.addAPI(item.id, item.name, item.value, item.value_2, item.value_3, "", "", "", item.enabled);
	}

	@Override
	protected void onItemEdited(ListEditItem item) {
		db.setAPIDescriptors(item.id, item.name, item.value, item.value_2, item.value_3, item.enabled);
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
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);
		final View mainView = inflater.inflate(R.layout.listedit_apiedit, null);
		// final View mainView = View.inflate(ctx, R.layout.listedit_apiedit, null);
		final TextView editName = (TextView)mainView.findViewById(R.id.listedit_editName);
		final TextView editValue = (TextView)mainView.findViewById(R.id.listedit_editValue);
		final TextView editValue_2 = (TextView)mainView.findViewById(R.id.listedit_editValue_2);
		final TextView editValue_3 = (TextView)mainView.findViewById(R.id.listedit_editValue_3);
		final CheckBox oauth = (CheckBox)mainView.findViewById(R.id.listedit_oauth);
		
		if (item != null) {
			editName.setText(item.name);
			editValue.setText(item.value);
			editValue_2.setText(item.value_2);
			editValue_3.setText(item.value_3);
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
			editValue_2.setEnabled(false);
			editValue_3.setEnabled(false);
		}
		
		builder.setView(mainView);

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				Boolean validAPIURL=true;
				Boolean validReadOnlyAPIURL=true;
				Boolean validNotesAPIURL = true;
				String name = editName.getText().toString();
				String value = editValue.getText().toString();
				String value_2 = editValue_2.getText().toString();
				String value_3 = editValue_3.getText().toString();
				boolean enabled = oauth.isChecked();
				if (item == null) {
					// new item
					//validate api here
					validAPIURL= Patterns.WEB_URL.matcher(value).matches();
					if(value_2.trim().matches("")==false){
						validReadOnlyAPIURL=Patterns.WEB_URL.matcher(value_2).matches();

					}
					if(value_3.trim().matches("")==false){
						validNotesAPIURL=Patterns.WEB_URL.matcher(value_3).matches();
					}
					if(validAPIURL==true && validNotesAPIURL==true && validReadOnlyAPIURL==true) {   //check if fields valid, optional ones checked if values entered
						if (!value.equals("")) {
							finishCreateItem(new ListEditItem(name, value, !"".equals(value_2) ? value_2 : null, !"".equals(value_3) ? value_3 : null, oauth.isChecked()));

						}
					}

					else if(validAPIURL==false){

						Toast.makeText(APIEditorActivity.this, "Invalid API URL", Toast.LENGTH_LONG).show();
					}
					else if(validReadOnlyAPIURL==false){

						Toast.makeText(APIEditorActivity.this, "Invalid ReadOnly API URL", Toast.LENGTH_LONG).show();
					}
					else if(validNotesAPIURL==false){

						Toast.makeText(APIEditorActivity.this, "Invalid Notes API URL", Toast.LENGTH_LONG).show();
					}

				} else {
					item.name = name;
					item.value = value;


					if(value_2.trim().matches("")==false){                //check if empty field
						validReadOnlyAPIURL=Patterns.WEB_URL.matcher(value_2).matches();

					}
					if(value_3.trim().matches("")==false){                 //check if empty field
						validNotesAPIURL=Patterns.WEB_URL.matcher(value_3).matches();
					}

					if(validReadOnlyAPIURL==true) {                 //check if valid url entered
						item.value_2 = !"".equals(value_2) ? value_2 : null;
					}
					else{
						Toast.makeText(APIEditorActivity.this, "Invalid ReadOnly API URL", Toast.LENGTH_LONG).show(); //if garbage value entered
					}

					if(validNotesAPIURL==true) {                   //check if valid url entered
						item.value_3 = !"".equals(value_3) ? value_3 : null;
					}
					else{
						Toast.makeText(APIEditorActivity.this, "Invalid Notes API URL", Toast.LENGTH_LONG).show();  //if garbage value entered
					}
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
