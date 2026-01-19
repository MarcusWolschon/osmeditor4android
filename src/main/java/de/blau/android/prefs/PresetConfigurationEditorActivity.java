package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.presets.Preset;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

/** Provides an activity to edit the preset list. Downloads preset data when necessary. */
public class PresetConfigurationEditorActivity extends AbstractConfigurationEditorActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PresetConfigurationEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = PresetConfigurationEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENU_RELOAD = 1;
    private static final int MENU_UP     = 2;
    private static final int MENU_DOWN   = 3;

    /**
     * Construct a new instance
     */
    public PresetConfigurationEditorActivity() {
        super();
        addAdditionalContextMenuItem(MENU_RELOAD, R.string.preset_update);
        addAdditionalContextMenuItem(MENU_UP, R.string.move_up);
        addAdditionalContextMenuItem(MENU_DOWN, R.string.move_down);
    }

    /**
     * Start the activity
     * 
     * @param context an Android Context
     */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, PresetConfigurationEditorActivity.class);
        context.startActivity(intent);
    }

    /**
     * Build an intent suitable for starting this activity
     * 
     * @param activity the calling Activity
     * @param presetName the name of the preset
     * @param presetUrl the url
     * @param enable if true enable the preset
     * @return the Intent
     */
    @NonNull
    public static Intent getIntent(@NonNull Activity activity, @NonNull String presetName, @NonNull String presetUrl, boolean enable) {
        Intent intent = new Intent(activity, PresetConfigurationEditorActivity.class);
        intent.setAction(ACTION_NEW);
        intent.putExtra(EXTRA_NAME, presetName);
        intent.putExtra(EXTRA_VALUE, presetUrl);
        intent.putExtra(EXTRA_ENABLE, enable);
        return intent;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = (ListEditItem) getListView().getItemAtPosition(info.position);
        if (selectedItem != null) {
            Resources r = getResources();
            menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(R.string.edit)).setOnMenuItemClickListener(this);
            final boolean isDefault = LISTITEM_ID_DEFAULT.equals(selectedItem.id);
            if (!isDefault) {
                menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, r.getString(R.string.delete)).setOnMenuItemClickListener(this);
            }
            for (Entry<Integer, Integer> entry : additionalMenuItems.entrySet()) {
                final int key = entry.getKey();
                if (MENU_RELOAD == key && isDefault) { // can't reload builtin preset
                    continue;
                }
                menu.add(Menu.NONE, key + MENUITEM_ADDITIONAL_OFFSET, Menu.NONE, r.getString(entry.getValue())).setOnMenuItemClickListener(this);
            }
        }
    }

    @Override
    protected int getHelpResourceId() {
        return R.string.help_presets;
    }

    @Override
    protected int getAddTextResId() {
        return R.string.urldialog_add_preset;
    }

    @Override
    protected void onLoadList(List<ListEditItem> items) {
        PresetInfo[] presets = db.getPresets();
        for (PresetInfo preset : presets) {
            items.add(new ListEditItem(preset.id, preset.name, preset.url, preset.shortDescription, preset.version, preset.useTranslations, preset.active));
        }
    }

    @Override
    protected void onItemClicked(ListEditItem item) {
        if (!activePresetEnsured(item)) {
            return;
        }
        item.active = !item.active;
        db.setPresetState(item.id, item.active);
        App.resetPresets();
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        if (isAddingViaIntent()) {
            item.active = getIntent().getExtras().getBoolean(EXTRA_ENABLE);
        }
        db.addPreset(item.id, item.name, item.value, item.active);
        retrieveData(this, db, item, Preset.PRESETXML, true);
        if (!isAddingViaIntent() || item.active) { // added a new preset and enabled it: need to rebuild presets
            App.resetPresets();
        }
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        PresetInfo preset = db.getPreset(item.id);
        db.setPresetInfo(item.id, item.name, item.value, item.boolean0);
        if (preset.url != null && !preset.url.equals(item.value)) {
            // url changed so better recreate everything
            db.removeResourceDirectory(item.id);
            retrieveData(this, db, item, Preset.PRESETXML, true);
        }
        App.resetPresets();
    }

    @Override
    protected void onItemDeleted(ListEditItem item) {
        if (!activePresetEnsured(item)) {
            return;
        }
        ThemeUtils.getAlertDialogBuilder(this).setTitle(R.string.delete).setMessage(R.string.preset_management_delete)
                .setPositiveButton(R.string.Yes, (dialog, which) -> {
                    db.deletePreset(item.id);
                    reloadItems();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    /**
     * Check that we have at least one active preset
     * 
     * @param item the current item
     * @return true if there will be at least one active item after item is de-activated or deleted
     */
    private boolean activePresetEnsured(@NonNull ListEditItem item) {
        if (item.active && db.getActivePresets().length == 1) { // at least one item needs to be selected
            updateAdapter();
            ScreenMessage.barWarning(this, R.string.toast_min_one_preset);
            return false;
        }
        return true;
    }

    @Override
    public void onAdditionalMenuItemClick(int menuItemId, ListEditItem clickedItem) {
        switch (menuItemId) {
        case MENU_RELOAD:
            PresetInfo preset = db.getPreset(clickedItem.id);
            if (preset.url != null) {
                retrieveData(this, db, clickedItem, Preset.PRESETXML, true);
            }
            App.resetPresets();
            break;
        case MENU_UP:
            int oldPos = items.indexOf(clickedItem);
            if (oldPos > 0) {
                db.movePreset(oldPos, oldPos - 1);
                reloadItems();
            }
            break;
        case MENU_DOWN:
            oldPos = items.indexOf(clickedItem);
            if (oldPos < items.size() - 1) {
                db.movePreset(oldPos, oldPos + 1);
                reloadItems();
            }
            break;
        default:
            Log.e(DEBUG_TAG, "Unknown menu item " + menuItemId);
            break;
        }
    }

    /**
     * Reload the ListView and invalidate the global preset var
     */
    private void reloadItems() {
        items.clear();
        onLoadList(items);
        updateAdapter();
        App.resetPresets();
    }

    /**
     * Download data (XML, icons) for a certain preset or load it from a file
     * 
     * @param activity a URLListEditActivity instance
     * @param db an AdvancedPrefDatabase instance
     * @param item the item containing the preset to be downloaded
     */
    static void retrieveData(@NonNull URLListEditActivity activity, @NonNull AdvancedPrefDatabase db, @NonNull final ListEditItem item) {
        retrieveData(activity, db, item, Preset.PRESETXML, true);
    }

    @Override
    protected boolean canAutoClose() { // download needs to get done
        return false;
    }

    /**
     * Opens the dialog to edit an item
     * 
     * @param item the selected item
     */
    @Override
    protected void itemEditDialog(final ListEditItem item) {
        Bundle args = new Bundle();
        args.putSerializable(LoadableResourceItemEditDialog.ITEM_KEY, item);
        FragmentManager fm = getSupportFragmentManager();
        PresetItemEditDialog f = new PresetItemEditDialog();
        f.setArguments(args);
        f.setShowsDialog(true);
        f.show(fm, LoadableResourceItemEditDialog.ITEM_EDIT_DIALOG_TAG);
    }

    public static class PresetItemEditDialog extends LoadableResourceItemEditDialog {

        private CheckBox useTranslations;

        PresetItemEditDialog() {
            super(R.layout.listedit_presetedit);
        }

        @Override
        protected void aditionalFieldsSetup(@NonNull FragmentActivity activity, @NonNull View layout, boolean itemExists) {
            useTranslations = (CheckBox) layout.findViewById(R.id.listedit_translations);
        }

        @Override
        void finishItem(@NonNull URLListEditActivity activity, @Nullable ListEditItem item, @NonNull String name, @NonNull String url) {
            boolean useTranslationsEnabled = useTranslations.isChecked();
            if (item == null) {
                // new item
                activity.finishCreateItem(new ListEditItem(name, url, null, null, useTranslationsEnabled, null));
            } else {
                item.name = name;
                item.value = url;
                item.boolean0 = useTranslationsEnabled;
                activity.finishEditItem(item);
            }
        }

        @Override
        boolean isDefault(ListEditItem item) {
            return URLListEditActivity.LISTITEM_ID_DEFAULT.equals(item.id);
        }
    }
}
