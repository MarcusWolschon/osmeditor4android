package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.imagestorage.PanoramaxStorage;
import de.blau.android.imagestorage.WikimediaCommonsStorage;
import de.blau.android.prefs.AdvancedPrefDatabase.ImageStorageType;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.InsetAwarePopupMenu;
import de.blau.android.util.ThemeUtils;

/** Provides an activity for editing the API list */
public class ImageStorageEditorActivity extends URLListEditActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ImageStorageEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = ImageStorageEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENU_AUTHORIZE = 1;

    private AdvancedPrefDatabase db;

    /**
     * Construct a new instance
     */
    public ImageStorageEditorActivity() {
        super();
        addAdditionalContextMenuItem(MENU_AUTHORIZE, R.string.menu_authorize);
    }

    /**
     * Construct a new instance
     * 
     * @param context an Android Context
     */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, ImageStorageEditorActivity.class);
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
        FloatingActionButton add = (FloatingActionButton) findViewById(R.id.add);
        if (add != null) {
            add.setOnClickListener(v -> {
                Log.d(DEBUG_TAG, "button clicked");
                PopupMenu popup = new InsetAwarePopupMenu(this, add);
                MenuItem item = popup.getMenu().add(R.string.imagestores_add_from_panoramax_list);
                item.setOnMenuItemClickListener(unused -> {
                    selectPanoramaxStore();
                    return false;
                });
                item = popup.getMenu().add(R.string.imagestores_add_manually);
                item.setOnMenuItemClickListener(unused -> {
                    itemEditDialog(null);
                    return false;
                });
                popup.show();
            });

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    /**
     * Display a modal with a list of Panoramax instances
     */
    private void selectPanoramaxStore() {
        new ExecutorTask<Void, Void, List<ImageStorageConfiguration>>() {
            private AlertDialog progress = null;

            @Override
            protected void onPreExecute() {
                progress = ProgressDialog.get(ImageStorageEditorActivity.this, Progress.PROGRESS_SEARCHING);
                progress.show();
            }

            @Override
            protected List<ImageStorageConfiguration> doInBackground(Void nothing) throws IOException {
                return PanoramaxStorage.getInstances(ImageStorageEditorActivity.this);
            }

            @Override
            protected void onPostExecute(List<ImageStorageConfiguration> instances) {
                try {
                    if (progress != null) {
                        progress.dismiss();
                    }
                } catch (Exception ex) {
                    Log.e(DEBUG_TAG, "dismiss dialog failed with " + ex);
                }
                String[] names = new String[instances.size()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = instances.get(i).name;
                }
                ThemeUtils.getAlertDialogBuilder(ImageStorageEditorActivity.this).setItems(names, (DialogInterface d, int which) -> {
                    ImageStorageConfiguration imagestore = instances.get(which);
                    // hack we test for null id in itemEditDialog
                    ListEditItem item = new ListEditItem("", imagestore.name, imagestore.type.toString(), imagestore.url, null, null, false, false); // NOSONAR
                    itemEditDialog(item);
                }).show();
            }
        }.execute();
    }

    @Override
    protected int getAddTextResId() {
        return R.string.urldialog_add_image_store;
    }

    @Override
    protected void onLoadList(List<ListEditItem> items) {
        ImageStorageConfiguration[] imagestores = db.getImageStores();
        for (ImageStorageConfiguration imagestore : imagestores) {
            items.add(new ListEditItem(imagestore.id, imagestore.name, imagestore.type.toString(), imagestore.url, null, false, imagestore.active));
        }
    }

    @Override
    protected void onItemClicked(ListEditItem item) {
        for (ListEditItem lei : items) {
            lei.active = false;
        }
        item.active = true;
        db.setImageStoreState(item.id, item.active);
        updateAdapter();
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        db.addImageStore(item.id, item.name, ImageStorageType.valueOf(item.value), item.value2, item.active);
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        db.updateImageStore(item.id, item.name, ImageStorageType.valueOf(item.value), item.value2, item.active);
    }

    @Override
    protected void onItemDeleted(ListEditItem item) {
        db.deleteImageStore(item.id);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = (ListEditItem) getListView().getItemAtPosition(info.position);
        if (selectedItem != null) {
            Resources r = getResources();
            menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(R.string.edit)).setOnMenuItemClickListener(this);
            if (db.getImageStores().length > 0) {
                menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, r.getString(R.string.delete)).setOnMenuItemClickListener(this);
                for (Entry<Integer, Integer> entry : additionalMenuItems.entrySet()) {
                    menu.add(Menu.NONE, entry.getKey() + MENUITEM_ADDITIONAL_OFFSET, Menu.NONE, r.getString(entry.getValue())).setOnMenuItemClickListener(this);
                }
            }
        }
    }

    @Override
    public void onAdditionalMenuItemClick(int menuItemId, ListEditItem clickedItem) {
        if (menuItemId == MENU_AUTHORIZE) {
            ImageStorageConfiguration[] stores = db.getImageStores(clickedItem.id);
            if (stores.length == 1) {
                switch (stores[0].type) {
                case PANORAMAX:
                    final PanoramaxStorage panoramaxStore = new PanoramaxStorage(stores[0]);
                    panoramaxStore.authorize(ImageStorageEditorActivity.this);
                    return;
                case WIKIMEDIA_COMMONS:
                    final WikimediaCommonsStorage wikimediaCommonsStore = new WikimediaCommonsStorage(stores[0]);
                    wikimediaCommonsStore.authorize(ImageStorageEditorActivity.this);
                    return;
                default: // fall through
                }
                Log.e(DEBUG_TAG, "Unknown ImageStore type " + stores[0].type);
                return;
            }
            Log.e(DEBUG_TAG, "More than one image store for id " + clickedItem.id);
        } else {
            Log.e(DEBUG_TAG, "Unknown menu item " + menuItemId);
        }
    }

    /**
     * Opens the dialog to edit an item
     * 
     * @param item the selected item
     */
    @Override
    protected void itemEditDialog(final ListEditItem item) {
        itemEditDialogWithTypeSpinner(R.layout.listedit_imagestoreedit, item, ImageStorageType.values(),
                item != null ? ImageStorageType.valueOf(item.value) : null);
    }
}