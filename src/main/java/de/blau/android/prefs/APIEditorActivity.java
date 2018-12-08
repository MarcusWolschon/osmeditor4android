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
import android.text.InputType;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/** Provides an activity for editing the API list */
public class APIEditorActivity extends URLListEditActivity {

    private AdvancedPrefDatabase db;

    /**
     * Construct a new instance
     */
    public APIEditorActivity() {
        super();
    }

    /**
     * Start the activity and return a result
     * 
     * @param activity the calling Activity
     * @param apiName the name of the api
     * @param apiUrl the url
     * @param requestCode the code to identify the result
     */
    public static void startForResult(@NonNull Activity activity, @NonNull String apiName, @NonNull String apiUrl, int requestCode) {
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
        for (ListEditItem lei : items) {
            lei.active = false;
        }
        item.active = true;
        updateAdapter();
        // finish();
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        db.addAPI(item.id, item.name, item.value, item.value_2, item.value_3, "", "", item.boolean_0);
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        db.setAPIDescriptors(item.id, item.name, item.value, item.value_2, item.value_3, item.boolean_0);
    }

    @Override
    protected void onItemDeleted(ListEditItem item) {
        db.deleteAPI(item.id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = (ListEditItem) getListView().getItemAtPosition(info.position);
        if (selectedItem != null) {
            menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(R.string.edit)).setOnMenuItemClickListener(this);
            if (!selectedItem.id.equals(LISTITEM_ID_DEFAULT)) {
                menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, r.getString(R.string.delete)).setOnMenuItemClickListener(this);
                for (Entry<Integer, Integer> entry : additionalMenuItems.entrySet()) {
                    menu.add(Menu.NONE, entry.getKey() + MENUITEM_ADDITIONAL_OFFSET, Menu.NONE, r.getString(entry.getValue())).setOnMenuItemClickListener(this);
                }
            }
        }
    }

    /**
     * Opens the dialog to edit an item
     * 
     * @param item the selected item
     */
    @Override
    protected void itemEditDialog(final ListEditItem item) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);
        final View mainView = inflater.inflate(R.layout.listedit_apiedit, null);
        final TextView editName = (TextView) mainView.findViewById(R.id.listedit_editName);
        final TextView editValue = (TextView) mainView.findViewById(R.id.listedit_editValue);
        final TextView editValue_2 = (TextView) mainView.findViewById(R.id.listedit_editValue_2);
        final TextView editValue_3 = (TextView) mainView.findViewById(R.id.listedit_editValue_3);
        final CheckBox oauth = (CheckBox) mainView.findViewById(R.id.listedit_oauth);

        if (item != null) {
            editName.setText(item.name);
            editValue.setText(item.value);
            editValue_2.setText(item.value_2);
            editValue_3.setText(item.value_3);
            oauth.setChecked(item.boolean_0);
        } else if (isAddingViaIntent()) {
            String tmpName = getIntent().getExtras().getString(EXTRA_NAME);
            String tmpValue = getIntent().getExtras().getString(EXTRA_VALUE);
            editName.setText(tmpName == null ? "" : tmpName);
            editValue.setText(tmpValue == null ? "" : tmpValue);
            oauth.setChecked(false);
        }
        if (item != null && item.id.equals(LISTITEM_ID_DEFAULT)) {
            // name and value are not editable
            editName.setInputType(InputType.TYPE_NULL);
            editName.setBackground(null);
            editValue.setInputType(InputType.TYPE_NULL);
            editValue.setBackground(null);
            editValue_2.setEnabled(false);
            editValue_3.setEnabled(false);
        }

        builder.setView(mainView);
        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing here because we override this button later to change the close behaviour.
                // However, we still need this because on older versions of Android unless we
                // pass a handler the button doesn't get instantiated
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // leave empty
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

        final AlertDialog dialog = builder.create();
        dialog.setView(mainView);
        dialog.show();

        // overriding the handlers
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean validAPIURL = true;
                Boolean validReadOnlyAPIURL = true;
                Boolean validNotesAPIURL = true;
                String name = editName.getText().toString().trim();
                String apiURL = editValue.getText().toString().trim();
                String readOnlyAPIURL = editValue_2.getText().toString().trim();
                String notesAPIURL = editValue_3.getText().toString().trim();
                boolean enabled = oauth.isChecked();

                // (re-)set to black
                changeBackgroundColor(editValue, VALID_COLOR);
                changeBackgroundColor(editValue_2, VALID_COLOR);
                changeBackgroundColor(editValue_3, VALID_COLOR);

                // validate entries
                validAPIURL = Patterns.WEB_URL.matcher(apiURL).matches();
                if (!"".equals(readOnlyAPIURL)) {
                    validReadOnlyAPIURL = Patterns.WEB_URL.matcher(readOnlyAPIURL).matches();
                } else {
                    readOnlyAPIURL = null;
                }
                if (!"".equals(notesAPIURL)) {
                    validNotesAPIURL = Patterns.WEB_URL.matcher(notesAPIURL).matches();
                } else {
                    notesAPIURL = null;
                }

                // save or display toast
                if (validAPIURL && validNotesAPIURL && validReadOnlyAPIURL) { // check if fields valid, optional ones
                                                                              // checked if values entered
                    if (!"".equals(apiURL)) {
                        if (item == null) {
                            // new item
                            finishCreateItem(new ListEditItem(name, apiURL, readOnlyAPIURL, notesAPIURL, enabled));
                        } else {
                            item.name = name;
                            item.value = apiURL;
                            item.value_2 = readOnlyAPIURL;
                            item.value_3 = notesAPIURL;
                            item.boolean_0 = enabled;
                            finishEditItem(item);
                        }
                    }
                    dialog.dismiss();
                } else if (validAPIURL == false) { // if garbage value entered show toasts
                    Snack.barError(APIEditorActivity.this, R.string.toast_invalid_apiurl);
                    changeBackgroundColor(editValue, ERROR_COLOR);
                } else if (!validReadOnlyAPIURL) {
                    Snack.barError(APIEditorActivity.this, R.string.toast_invalid_readonlyurl);
                    changeBackgroundColor(editValue_2, ERROR_COLOR);
                } else if (!validNotesAPIURL) {
                    Snack.barError(APIEditorActivity.this, R.string.toast_invalid_notesurl);
                    changeBackgroundColor(editValue_3, ERROR_COLOR);
                }
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}