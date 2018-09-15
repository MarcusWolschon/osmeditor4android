package de.blau.android.prefs;

import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.prefs.AdvancedPrefDatabase.GeocoderType;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/** Provides an activity for editing the API list */
public class GeocoderEditorActivity extends URLListEditActivity {

    private AdvancedPrefDatabase db;

    public GeocoderEditorActivity() {
        super();
    }

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, GeocoderEditorActivity.class);
        context.startActivity(intent);
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
        return R.string.urldialog_add_geocoder;
    }

    @Override
    protected void onLoadList(List<ListEditItem> items) {
        Geocoder[] geocoders = db.getGeocoders();
        for (Geocoder geocoder : geocoders) {
            items.add(new ListEditItem(geocoder.id, geocoder.name, geocoder.type.toString(), geocoder.url, null, false, geocoder.active));
        }
    }

    @Override
    protected void onItemClicked(ListEditItem item) {
        if (item.active && db.getActiveGeocoders().length == 1) { // at least one item needs to be selected
            updateAdapter();
            Snack.barWarning(this, R.string.toast_min_one_geocoder);
            return;
        }
        item.active = !item.active;
        db.setGeocoderState(item.id, item.active);
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        db.addGeocoder(item.id, item.name, GeocoderType.valueOf(item.value), 0, item.value_2, item.active);
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        db.updateGeocoder(item.id, item.name, GeocoderType.valueOf(item.value), 0, item.value_2, item.enabled);
    }

    @Override
    protected void onItemDeleted(ListEditItem item) {
        db.deleteGeocoder(item.id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = (ListEditItem) getListView().getItemAtPosition(info.position);
        if (selectedItem != null) {
            menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(R.string.edit)).setOnMenuItemClickListener(this);
            if (db.getActiveGeocoders().length > 1) {
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
        final View mainView = inflater.inflate(R.layout.listedit_geocoderedit, null);
        final TextView editName = (TextView) mainView.findViewById(R.id.listedit_editName);
        final Spinner geocoderType = (Spinner) mainView.findViewById(R.id.geocoder_type);
        final TextView url = (TextView) mainView.findViewById(R.id.listedit_editValue_2);

        GeocoderType[] geocoderTypes = GeocoderType.values();
        ArrayAdapter<GeocoderType> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, geocoderTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        geocoderType.setAdapter(adapter);

        if (item != null) {
            editName.setText(item.name);
            geocoderType.setSelection((GeocoderType.valueOf(item.value)).ordinal());
            url.setText(item.value_2);

            if (item.id.equals(LISTITEM_ID_DEFAULT)) {
                // name and value are not editable
                editName.setEnabled(false);
                geocoderType.setEnabled(false);
                url.setEnabled(false);
            }
        }

        builder.setView(mainView);

        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = editName.getText().toString();
                String value = ((GeocoderType) geocoderType.getSelectedItem()).name();
                String value_2 = url.getText().toString();

                if (item == null) {
                    // new item
                    if (!"".equals(value)) {
                        finishCreateItem(new ListEditItem(name, value, !"".equals(value_2) ? value_2 : null, null, false));
                    }
                } else {
                    item.name = name;
                    item.value = value;
                    item.value_2 = !"".equals(value_2) ? value_2 : null;
                    finishEditItem(item);
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
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
