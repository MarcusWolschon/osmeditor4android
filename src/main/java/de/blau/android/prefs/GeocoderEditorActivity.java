package de.blau.android.prefs;

import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.prefs.AdvancedPrefDatabase.GeocoderType;
import de.blau.android.util.ScreenMessage;

/** Provides an activity for editing the API list */
public class GeocoderEditorActivity extends URLListEditActivity {

    private AdvancedPrefDatabase db;

    /**
     * Construct a new instance
     */
    public GeocoderEditorActivity() {
        super();
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, GeocoderEditorActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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
            ScreenMessage.barWarning(this, R.string.toast_min_one_geocoder);
            return;
        }
        item.active = !item.active;
        db.setGeocoderState(item.id, item.active);
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        db.addGeocoder(item.id, item.name, GeocoderType.valueOf(item.value), 0, item.value2, item.active);
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        db.updateGeocoder(item.id, item.name, GeocoderType.valueOf(item.value), 0, item.value2, item.active);
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
            Resources r = getResources();
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
        itemEditDialogWithTypeSpinner(R.layout.listedit_geocoderedit, item, GeocoderType.values(), item != null ? GeocoderType.valueOf(item.value) : null);
    }
}