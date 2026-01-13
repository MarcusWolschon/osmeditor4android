package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OperationFailedException;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.presets.Preset;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FragmentUtil;
import de.blau.android.util.ReadFile;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/** Provides an activity to edit the preset list. Downloads preset data when necessary. */
public class StyleConfigurationEditorActivity extends URLListEditActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, StyleConfigurationEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = StyleConfigurationEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

    private AdvancedPrefDatabase db;

    private static final int MENU_RELOAD = 1;

    private static final int MENUITEM_HELP = 1;

    private static final int RESULT_TOTAL_FAILURE     = 0;
    private static final int RESULT_TOTAL_SUCCESS     = 1;
    private static final int RESULT_DOWNLOAD_CANCELED = 4;

    /**
     * Construct a new instance
     */
    public StyleConfigurationEditorActivity() {
        super();
        addAdditionalContextMenuItem(MENU_RELOAD, R.string.preset_update);
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
     * Start the activity and return a result
     * 
     * @param activity the calling Activity
     * @param presetName the name of the preset
     * @param presetUrl the url
     * @param enable if true enable the preset
     * @param requestCode the code to identify the result
     */
    public static void startForResult(@NonNull Activity activity, @NonNull String presetName, @NonNull String presetUrl, boolean enable, int requestCode) {
        Intent intent = new Intent(activity, StyleConfigurationEditorActivity.class);
        intent.setAction(ACTION_NEW);
        intent.putExtra(EXTRA_NAME, presetName);
        intent.putExtra(EXTRA_VALUE, presetUrl);
        intent.putExtra(EXTRA_ENABLE, enable);
        activity.startActivityForResult(intent, requestCode);
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
    public boolean onCreateOptionsMenu(final Menu menu) {
        menu.add(0, MENUITEM_HELP, 0, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(this, R.attr.menu_help))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Log.d(DEBUG_TAG, "onOptionsItemSelected");
        if (item.getItemId() == MENUITEM_HELP) {
            HelpViewer.start(this, R.string.help_presets);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedItem = (ListEditItem) getListView().getItemAtPosition(info.position);
        if (selectedItem != null) {
            Resources r = getResources();
            menu.add(Menu.NONE, MENUITEM_EDIT, Menu.NONE, r.getString(selectedItem.boolean0 ? R.string.edit : R.string.config_category_info))
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
    protected int getAddTextResId() {
        return R.string.urldialog_add_preset;
    }

    @Override
    protected void onLoadList(List<ListEditItem> items) {
        StyleConfiguration[] styles = db.getStyles();
        for (StyleConfiguration style : styles) {
            items.add(new ListEditItem(style.id, style.name, style.url, style.description, style.version, style.custom, style.isActive()));
        }
    }

    @Override
    protected void onItemClicked(ListEditItem item) {
        if (!activeDataStyleEnsured(item)) {
            return;
        }
        db.setStyleState(item.id, true);
        // this is a bit hackish, but only one can be selected
        for (ListEditItem lei : items) {
            lei.active = false;
        }
        item.active = true;
        App.getPreferences(this).setDataStyle(item.name);
        updateAdapter();
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        if (isAddingViaIntent()) {
            item.active = getIntent().getExtras().getBoolean(EXTRA_ENABLE);
        }
        db.addStyle(item.id, item.name, item.value, true, item.active);
        // retrievePresetData(this, db, item);
        if (!isAddingViaIntent() || item.active) { // added a new preset and enabled it: need to rebuild presets
            // App.resetPresets();
        }
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        StyleConfiguration style = db.getStyle(item.id);
        db.updateStyle(item.id, item.name, item.value, item.boolean0);
        if (style.url != null && !style.url.equals(item.value)) {
            // url changed so better recreate everything
            db.removeResourceDirectory(item.id);
            // retrievePresetData(this, db, item);
        }
    }

    @Override
    protected void onItemDeleted(ListEditItem item) {
        if (!activeDataStyleEnsured(item)) {
            return;
        }
        ThemeUtils.getAlertDialogBuilder(this).setTitle(R.string.delete).setMessage(R.string.preset_management_delete)
                .setPositiveButton(R.string.Yes, (dialog, which) -> {
                    db.deleteStyle(item.id);
                    reloadItems();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    /**
     * Check that we have at least one active preset
     * 
     * @param item the current item
     * @return true if there will be at least one active item after item is de-activated or deleted
     */
    private boolean activeDataStyleEnsured(@NonNull ListEditItem item) {
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
                retrieveStyleData(this, db, clickedItem);
            }
            App.resetPresets();
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
     * @param activity a PresetEditorActivity instance
     * @param db an AdvancedPrefDatabase instance
     * @param item the item containing the preset to be downloaded
     */
    private static void retrieveStyleData(@NonNull StyleConfigurationEditorActivity activity, @NonNull AdvancedPrefDatabase db,
            @NonNull final ListEditItem item) {
        final File styleDir = db.getResourceDirectory(item.id);
        // noinspection ResultOfMethodCallIgnored
        styleDir.mkdir();
        if (!styleDir.isDirectory()) {
            throw new OperationFailedException("Could not create style directory " + styleDir.getAbsolutePath());
        }
        new ExecutorTask<Void, Integer, Integer>() {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_PRESET);
            }

            @Override
            protected Integer doInBackground(Void args) {
                Uri uri = Uri.parse(item.value);
                final String scheme = uri.getScheme();
                int loadResult = Schemes.FILE.equals(scheme) || Schemes.CONTENT.equals(scheme) ? PresetLoader.load(activity, uri, styleDir, Preset.PRESETXML)
                        : PresetLoader.download(item.value, styleDir, Preset.PRESETXML);

                if (loadResult == PresetLoader.DOWNLOADED_PRESET_ERROR) {
                    return RESULT_TOTAL_FAILURE;
                }
                return RESULT_TOTAL_SUCCESS;
            }

            @Override
            protected void onPostExecute(Integer result) {
                Progress.dismissDialog(activity, Progress.PROGRESS_PRESET);
                switch (result) {
                case RESULT_TOTAL_SUCCESS:
                    ScreenMessage.barInfo(activity, R.string.preset_download_successful);
                    activity.sendResultIfApplicable(item);
                    break;
                case RESULT_TOTAL_FAILURE:
                    msgbox(R.string.preset_download_failed);
                    break;
                case RESULT_DOWNLOAD_CANCELED:
                    break; // do nothing
                default:
                    break;
                }
            }

            /**
             * Show a simple message box detailing the download result. The activity will end as soon as the box is
             * closed.
             * 
             * @param msgResID string resource id of message
             */
            private void msgbox(int msgResID) {
                AlertDialog.Builder box = ThemeUtils.getAlertDialogBuilder(activity);
                box.setMessage(activity.getResources().getString(msgResID));
                box.setOnCancelListener(dialog -> activity.sendResultIfApplicable(item));
                box.setPositiveButton(R.string.okay, (dialog, which) -> {
                    dialog.dismiss();
                    activity.sendResultIfApplicable(item);
                });
                box.show();
            }

        }.execute();
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
        args.putSerializable(StyleItemEditDialog.ITEM_KEY, item);
        FragmentManager fm = getSupportFragmentManager();
        StyleItemEditDialog f = new StyleItemEditDialog();
        f.setArguments(args);
        f.setShowsDialog(true);
        f.show(fm, StyleItemEditDialog.ITEM_EDIT_DIALOG_TAG);
    }

    public static class StyleItemEditDialog extends CancelableDialogFragment {

        private static final String ITEM_EDIT_DIALOG_TAG = "style_item_edit_dialog";
        static final String         ITEM_KEY             = "item";

        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            ListEditItem item = Util.getSerializeable(getArguments(), ITEM_KEY, ListEditItem.class);
            final AlertDialog.Builder builder = ThemeUtils.getAlertDialogBuilder(getContext());
            final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getContext());
            final View mainView = inflater.inflate(R.layout.listedit_presetedit, null);
            final TextView editName = (TextView) mainView.findViewById(R.id.listedit_editName);
            final TextView editValue = (TextView) mainView.findViewById(R.id.listedit_editValue);
            final TextView versionLabel = (TextView) mainView.findViewById(R.id.listedit_labelVersion);
            final TextView version = (TextView) mainView.findViewById(R.id.listedit_version);
            final ImageButton fileButton = (ImageButton) mainView.findViewById(R.id.listedit_file_button);

            final StyleConfigurationEditorActivity activity = (StyleConfigurationEditorActivity) getActivity();

            final boolean itemExists = item != null;
            if (itemExists) {
                editName.setText(item.name);
                editValue.setText(item.value);
            } else if (activity.isAddingViaIntent()) {
                String tmpName = activity.getIntent().getExtras().getString(EXTRA_NAME);
                String tmpValue = activity.getIntent().getExtras().getString(EXTRA_VALUE);
                editName.setText(tmpName == null ? "" : tmpName);
                editValue.setText(tmpValue == null ? "" : tmpValue);
            }
            if (itemExists && item.value3 != null) {
                version.setText(item.value3);
            } else {
                versionLabel.setVisibility(View.GONE);
                version.setVisibility(View.GONE);
            }
            if (itemExists && !item.boolean0) {
                // name and value are not editable
                editName.setInputType(InputType.TYPE_NULL);
                editName.setBackground(null);
                editValue.setEnabled(false);
                fileButton.setEnabled(false);
            }

            activity.setViewAndButtons(builder, mainView);

            final AlertDialog dialog = builder.create();

            fileButton.setOnClickListener(v -> SelectFile.read(activity, R.string.config_presetsPreferredDir_key, new ReadFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean read(FragmentActivity currentActivity, Uri fileUri) {
                    final Dialog dialog = FragmentUtil.findDialogByTag(currentActivity, ITEM_EDIT_DIALOG_TAG);
                    if (dialog == null) {
                        Log.e(DEBUG_TAG, "Dialog is null");
                        return false;
                    }
                    final TextView editValue = (TextView) dialog.findViewById(R.id.listedit_editValue);

                    editValue.setText(fileUri.toString());
                    SelectFile.savePref(new Preferences(currentActivity), R.string.config_presetsPreferredDir_key, fileUri);
                    return true;
                }
            }));

            // overriding the handlers
            dialog.setOnShowListener((DialogInterface d) -> {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = editName.getText().toString().trim();
                    String styleURL = editValue.getText().toString().trim();
                    changeBackgroundColor(editValue, VALID_COLOR);
                    // validate entries
                    boolean validStyleURL = Patterns.WEB_URL.matcher(styleURL).matches();
                    URL url = null;
                    try {
                        url = new URL(styleURL);
                    } catch (MalformedURLException e) {
                        validStyleURL = false;
                    }

                    // save or display toast, exception for localhost is needed for testing
                    if (validStyleURL || styleURL.startsWith(Schemes.FILE) || styleURL.startsWith(Schemes.CONTENT)
                            || (url != null && "localhost".equals(url.getHost())) || (itemExists && item.id.equals(LISTITEM_ID_DEFAULT))) {
                        if (item == null) {
                            // new item
                            activity.finishCreateItem(new ListEditItem(name, styleURL, null, null, true, null));
                        } else {
                            item.name = name;
                            item.value = styleURL;
                            activity.finishEditItem(item);
                        }
                        dialog.dismiss();
                    } else {
                        // if garbage value entered show toasts
                        ScreenMessage.barError(activity, R.string.toast_invalid_preseturl);
                        changeBackgroundColor(editValue, ERROR_COLOR);
                    }
                });
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> dialog.dismiss());
            });
            return dialog;
        }
    }
}
