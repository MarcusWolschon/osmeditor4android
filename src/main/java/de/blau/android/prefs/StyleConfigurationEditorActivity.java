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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.resources.DataStyleManager;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

/** Provides an activity to edit the style list. Downloads style data when necessary. */
public class StyleConfigurationEditorActivity extends AbstractConfigurationEditorActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, StyleConfigurationEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = StyleConfigurationEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENU_RELOAD = 1;

    private static final String STYLE_XML = "style.xml";
    
    private final DataStyleManager manager;

    /**
     * Construct a new instance
     */
    public StyleConfigurationEditorActivity() {
        super();
        addAdditionalContextMenuItem(MENU_RELOAD, R.string.style_update);
        manager = App.getDataStyleManager(this);
    }

    /**
     * Start the activity
     * 
     * @param context an Android Context
     */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, StyleConfigurationEditorActivity.class);
        context.startActivity(intent);
    }

    /**
     * Build an intent suitable for starting this activity
     * 
     * @param activity the calling Activity
     * @param styleName the name of the style
     * @param styleUrl the url
     * @param enable if true enable the style
     * @return the Intent
     */
    @NonNull
    public static Intent getIntent(@NonNull Activity activity, @NonNull String styleName, @NonNull String styleUrl, boolean enable) {
        Intent intent = new Intent(activity, StyleConfigurationEditorActivity.class);
        intent.setAction(ACTION_NEW);
        intent.putExtra(EXTRA_NAME, styleName);
        intent.putExtra(EXTRA_VALUE, styleUrl);
        intent.putExtra(EXTRA_ENABLE, enable);
        return intent;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = (ListEditItem) getListView().getItemAtPosition(info.position);
        if (selectedItem != null) {
            Resources r = getResources();
            menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(selectedItem.boolean0 ? R.string.menu_edit : R.string.menu_view))
                    .setOnMenuItemClickListener(this);
            if (selectedItem.boolean0) {
                menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, r.getString(R.string.delete)).setOnMenuItemClickListener(this);
                for (Entry<Integer, Integer> entry : additionalMenuItems.entrySet()) {
                    final int key = entry.getKey();
                    menu.add(Menu.NONE, key + MENUITEM_ADDITIONAL_OFFSET, Menu.NONE, r.getString(entry.getValue())).setOnMenuItemClickListener(this);
                }
            }
        }
    }

    @Override
    protected int getHelpResourceId() {
        return R.string.help_styles;
    }

    @Override
    protected int getAddTextResId() {
        return R.string.urldialog_add_style;
    }

    @Override
    protected void onLoadList(List<ListEditItem> items) {
        StyleConfiguration[] styles = db.getStyles();
        for (StyleConfiguration style : styles) {
            items.add(new ListEditItem(style.id, style.name, style.url, style.description, style.version, style.custom, style.isActive()));
        }
    }

    @Override
    protected void setListItemViews(ListItem v, ListEditItem listEditItem) {
        v.setText1(manager.translate(listEditItem.name));
        v.setText2(manager.translate(listEditItem.value2));
        v.setChecked(listEditItem.active);
    }

    @Override
    protected void onItemClicked(ListEditItem item) {
        if (!activeDataStyleEnsured(item)) {
            return;
        }
        db.setStyleState(item.id, true);
        deactivateAll();
        item.active = true;
        updateAdapter();
    }

    /**
     * Deactivate all items
     */
    private void deactivateAll() {
        // this is a bit hackish, but only one can be selected
        for (ListEditItem lei : items) {
            lei.active = false;
        }
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        if (isAddingViaIntent() && getIntent().getExtras().getBoolean(EXTRA_ENABLE)) {
            Log.w(DEBUG_TAG, "Adding from intent");
            deactivateAll();
            item.active = true;
        }
        db.addStyle(item.id, item.name, item.value, true, item.active);
        StyleConfiguration conf = db.getStyle(item.id);
        if (conf == null) {
            Log.e(DEBUG_TAG, "Style configuration not found for " + item.id);
            return;
        }
        retrieveData(this, db, item, STYLE_XML, false);
        if (!isAddingViaIntent()) { // added a new style and enabled it: need to rebuild styles
            manager.reset(this, true);
        }
        reloadItems();
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        StyleConfiguration style = db.getStyle(item.id);
        db.updateStyle(item.id, item.name, item.value, item.boolean0);
        if (style.url != null && !style.url.equals(item.value)) {
            // url changed so better recreate everything
            db.removeResourceDirectory(item.id);
            manager.reset(this, true);
        }
    }

    @Override
    protected void onItemDeleted(ListEditItem item) {
        if (!activeDataStyleEnsured(item)) {
            return;
        }
        ThemeUtils.getAlertDialogBuilder(this).setTitle(R.string.delete).setMessage(R.string.style_management_delete)
                .setPositiveButton(R.string.Yes, (dialog, which) -> {
                    db.deleteStyle(item.id);
                    reloadItems();
                    manager.reset(this, true);
                }).setNegativeButton(R.string.cancel, null).show();
    }

    /**
     * Check that we have at least one active style
     * 
     * @param item the current item
     * @return true if there will be at least one active item after item is de-activated or deleted
     */
    private boolean activeDataStyleEnsured(@NonNull ListEditItem item) {
        if (item.active) { // at least one item needs to be selected
            updateAdapter();
            ScreenMessage.barWarning(this, R.string.toast_min_one_style);
            return false;
        }
        return true;
    }

    @Override
    public void onAdditionalMenuItemClick(int menuItemId, ListEditItem clickedItem) {
        if (MENU_RELOAD == menuItemId) {
            StyleConfiguration style = db.getStyle(clickedItem.id);
            if (style.url != null) {
                retrieveData(this, db, clickedItem, STYLE_XML, true);
            }
            manager.reset(this, true);
            return;
        }
        Log.e(DEBUG_TAG, "Unknown menu item " + menuItemId);
    }

    /**
     * Reload the ListView and invalidate
     */
    private void reloadItems() {
        items.clear();
        onLoadList(items);
        updateAdapter();
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
        StyleItemEditDialog f = new StyleItemEditDialog();
        f.setArguments(args);
        f.setShowsDialog(true);
        f.show(fm, LoadableResourceItemEditDialog.ITEM_EDIT_DIALOG_TAG);
    }

    public static class StyleItemEditDialog extends LoadableResourceItemEditDialog {

        StyleItemEditDialog() {
            super(R.layout.listedit_styleedit);
        }

        @Override
        void finishItem(@NonNull URLListEditActivity activity, @Nullable ListEditItem item, @NonNull String name, @NonNull String url) {
            if (item == null) {
                // new item
                activity.finishCreateItem(new ListEditItem(name, url, null, null, true, null));
            } else {
                item.name = name;
                item.value = url;
                activity.finishEditItem(item);
            }
        }

        @Override
        boolean isDefault(@NonNull ListEditItem item) {
            return !item.boolean0;
        }
    }
}
