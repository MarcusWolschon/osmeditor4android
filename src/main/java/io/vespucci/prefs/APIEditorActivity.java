package io.vespucci.prefs;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.contract.Paths;
import io.vespucci.dialogs.DataLoss;
import io.vespucci.prefs.API.Auth;
import io.vespucci.prefs.API.AuthParams;
import io.vespucci.util.ContentResolverUtil;
import io.vespucci.util.DatabaseUtil;
import io.vespucci.util.FileUtil;
import io.vespucci.util.FragmentUtil;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.ReadFile;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.SelectFile;
import io.vespucci.util.ThemeUtils;
import io.vespucci.util.Util;

/** Provides an activity for editing the API list */
public class APIEditorActivity extends URLListEditActivity {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, APIEditorActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG = APIEditorActivity.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENU_COPY = 1;

    private AdvancedPrefDatabase db;

    /**
     * Construct a new instance
     */
    public APIEditorActivity() {
        super();
        addAdditionalContextMenuItem(MENU_COPY, R.string.menu_copy);
    }

    /**
     * Start the activity showing a dialog if data has been changed
     * 
     * @param activity the calling FragmentActivity
     */
    public static void start(@NonNull FragmentActivity activity) {
        Intent intent = new Intent(activity, APIEditorActivity.class);
        final Logic logic = App.getLogic();
        if (logic != null && logic.hasChanges()) {
            DataLoss.showDialog(activity, intent, -1);
        } else {
            activity.startActivity(intent);
        }
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
        return R.string.urldialog_add_api;
    }

    @Override
    protected void onLoadList(List<ListEditItem> items) {
        API[] apis = db.getAPIs();
        API current = db.getCurrentAPI();
        for (API api : apis) {
            items.add(new ListEditItem(api.id, api.name, api.url, api.readonlyurl, api.notesurl, api.auth, api.compressedUploads, current.id.equals(api.id)));
        }
    }

    @Override
    protected void onItemClicked(ListEditItem item) {
        API current = db.getCurrentAPI();
        if (!item.id.equals(current.id)) {
            Main.prepareRedownload();
        }
        db.selectAPI(item.id);
        // this is a bit hackish, but only one can be selected
        for (ListEditItem lei : items) {
            lei.active = false;
        }
        item.active = true;
        updateAdapter();
    }

    @Override
    protected void onItemCreated(ListEditItem item) {
        db.addAPI(item.id, item.name, item.value, item.value2, item.value3, new AuthParams((Auth) item.object0, "", "", null, null), item.boolean0);
    }

    @Override
    protected void onItemEdited(ListEditItem item) {
        db.setAPIDescriptors(item.id, item.name, item.value, item.value2, item.value3, (Auth) item.object0);
        db.setAPICompressedUploads(item.id, item.boolean0);
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
            }
            for (Entry<Integer, Integer> entry : additionalMenuItems.entrySet()) {
                menu.add(Menu.NONE, entry.getKey() + MENUITEM_ADDITIONAL_OFFSET, Menu.NONE, r.getString(entry.getValue())).setOnMenuItemClickListener(this);
            }
        }
    }

    @Override
    public void onAdditionalMenuItemClick(int menuItemId, ListEditItem clickedItem) {
        if (menuItemId == MENU_COPY) {
            ListEditItem item = new ListEditItem(getString(R.string.copy_of, clickedItem.name), clickedItem.value, clickedItem.value2, clickedItem.value3,
                    clickedItem.boolean0, Auth.BASIC);
            db.addAPI(item.id, item.name, item.value, item.value2, item.value3, new AuthParams((Auth) item.object0, "", "", null, null), item.boolean0);
            items.clear();
            onLoadList(items);
            updateAdapter();
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
        Bundle args = new Bundle();
        args.putSerializable(ApiItemEditDialog.ITEM_KEY, item);
        FragmentManager fm = getSupportFragmentManager();
        ApiItemEditDialog f = new ApiItemEditDialog();
        f.setArguments(args);
        f.setShowsDialog(true);
        f.show(fm, ApiItemEditDialog.ITEM_EDIT_DIALOG_TAG);
    }

    public static class ApiItemEditDialog extends ImmersiveDialogFragment {

        private static final String ITEM_EDIT_DIALOG_TAG = "api_item_edit_dialog";
        static final String         ITEM_KEY             = "item";

        @Override
        public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
            ListEditItem item = Util.getSerializeable(getArguments(), ITEM_KEY, ListEditItem.class);

            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getContext());
            final View mainView = inflater.inflate(R.layout.listedit_apiedit, null);
            final TextView editName = (TextView) mainView.findViewById(R.id.listedit_editName);
            final TextView editValue = (TextView) mainView.findViewById(R.id.listedit_editValue);
            final TextView editValue2 = (TextView) mainView.findViewById(R.id.listedit_editValue_2);
            final TextView editValue3 = (TextView) mainView.findViewById(R.id.listedit_editValue_3);
            final Spinner auth = (Spinner) mainView.findViewById(R.id.listedit_auth);
            AuthenticationAdapter adapter = new AuthenticationAdapter(getContext(), android.R.layout.simple_spinner_item, Auth.values(),
                    getResources().getStringArray(R.array.authentication_entries));
            auth.setAdapter(adapter);
            final CheckBox checkbox = (CheckBox) mainView.findViewById(R.id.listedit_compressed_uploads);

            final ImageButton fileButton = (ImageButton) mainView.findViewById(R.id.listedit_file_button);

            final URLListEditActivity activity = (URLListEditActivity) getActivity();
            if (item != null) {
                editName.setText(item.name);
                editValue.setText(item.value);
                editValue2.setText(item.value2);
                editValue3.setText(item.value3);
                auth.setSelection(((Auth) item.object0).ordinal());
                checkbox.setChecked(item.boolean0);
            } else if (activity.isAddingViaIntent()) {
                String tmpName = activity.getIntent().getExtras().getString(EXTRA_NAME);
                String tmpValue = activity.getIntent().getExtras().getString(EXTRA_VALUE);
                editName.setText(tmpName == null ? "" : tmpName);
                editValue.setText(tmpValue == null ? "" : tmpValue);
                auth.setSelection(Auth.BASIC.ordinal());
                checkbox.setChecked(false);
            }
            if (item != null && item.id.equals(LISTITEM_ID_DEFAULT)) {
                // name and value are not editable
                editName.setInputType(InputType.TYPE_NULL);
                editName.setBackground(null);
                editValue.setBackground(null);
                editValue.setInputType(InputType.TYPE_NULL);
                editValue2.setEnabled(true);
                editValue3.setEnabled(false);
                checkbox.setEnabled(true);
            }

            activity.setViewAndButtons(builder, mainView);

            fileButton.setOnClickListener(view -> SelectFile.read(activity, R.string.config_msfPreferredDir_key, new ReadFile() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean read(FragmentActivity currentActivity, Uri uri) {
                    final Dialog dialog = FragmentUtil.findDialogByTag(currentActivity, ITEM_EDIT_DIALOG_TAG);
                    if (dialog == null) {
                        Log.e(DEBUG_TAG, "Dialog is null");
                        return false;
                    }
                    final TextView editValue2 = (TextView) dialog.findViewById(R.id.listedit_editValue_2);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // copy file
                        String fileName = ContentResolverUtil.getDisplaynameColumn(currentActivity, uri);
                        if (fileName == null) {
                            ScreenMessage.toastTopError(currentActivity, R.string.not_found_title);
                            return false;
                        }
                        try {
                            final File destination = new File(FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_IMPORTS),
                                    fileName);
                            FileUtil.importFile(currentActivity, uri, destination,
                                    () -> setFileUri(editValue2, Uri.parse(FileUtil.FILE_SCHEME_PREFIX + destination.getAbsolutePath())));
                            return true;
                        } catch (IOException ex) {
                            return false;
                        }
                    } else {
                        Uri fileUri = FileUtil.contentUriToFileUri(currentActivity, uri);
                        if (fileUri == null) {
                            ScreenMessage.toastTopError(currentActivity, R.string.not_found_title);
                            return false;
                        }
                        SelectFile.savePref(new Preferences(currentActivity), R.string.config_msfPreferredDir_key, fileUri);
                        return setFileUri(editValue2, fileUri);
                    }
                }

                /**
                 * Set the TextView displaying the Uri
                 * 
                 * @param textView the TextView
                 * @param fileUri the Uri
                 * @return true if successful
                 */
                private boolean setFileUri(@NonNull final TextView textView, @NonNull Uri fileUri) {
                    Log.d(DEBUG_TAG, "setFileUri " + fileUri);
                    try {
                        if (!DatabaseUtil.isValidSQLite(fileUri.getPath())) {
                            throw new SQLiteException("Not a SQLite database file");
                        }
                        textView.setText(fileUri.toString());
                        return true;
                    } catch (SQLiteException sqex) {
                        ScreenMessage.toastTopError(getActivity(), R.string.toast_not_mbtiles);
                        return false;
                    }
                }
            }));

            final AlertDialog dialog = builder.create();

            // overriding the handlers
            dialog.setOnShowListener((DialogInterface d) -> {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {

                    boolean validAPIURL = true;
                    boolean validReadOnlyAPIURL = true;
                    boolean validNotesAPIURL = true;
                    String name = editName.getText().toString().trim();
                    String apiURL = editValue.getText().toString().trim();
                    String readOnlyAPIURL = editValue2.getText().toString().trim();
                    String notesAPIURL = editValue3.getText().toString().trim();
                    Auth authentication = Auth.values()[auth.getSelectedItemPosition()];
                    boolean compressedUploads = checkbox.isChecked();

                    // (re-)set to black
                    changeBackgroundColor(editValue, VALID_COLOR);
                    changeBackgroundColor(editValue2, VALID_COLOR);
                    changeBackgroundColor(editValue3, VALID_COLOR);

                    // validate entries
                    validAPIURL = Patterns.WEB_URL.matcher(apiURL).matches();
                    if (!"".equals(readOnlyAPIURL)) {
                        validReadOnlyAPIURL = Patterns.WEB_URL.matcher(readOnlyAPIURL).matches() || readOnlyAPIURL.startsWith(FileUtil.FILE_SCHEME_PREFIX);
                    } else {
                        readOnlyAPIURL = null;
                    }
                    if (!"".equals(notesAPIURL)) {
                        validNotesAPIURL = Patterns.WEB_URL.matcher(notesAPIURL).matches();
                    } else {
                        notesAPIURL = null;
                    }

                    // save or display toast
                    if (validAPIURL && validNotesAPIURL && validReadOnlyAPIURL) { // check if fields valid, optional
                                                                                  // ones checked if values entered
                        if (!"".equals(apiURL)) {
                            if (item == null) {
                                // new item
                                activity.finishCreateItem(new ListEditItem(name, apiURL, readOnlyAPIURL, notesAPIURL, compressedUploads, authentication));
                            } else {
                                item.name = name;
                                item.value = apiURL;
                                item.value2 = readOnlyAPIURL;
                                item.value3 = notesAPIURL;
                                item.object0 = authentication;
                                item.boolean0 = compressedUploads;
                                activity.finishEditItem(item);
                            }
                        }
                        dialog.dismiss();
                    } else if (!validAPIURL) { // if garbage value entered show toasts
                        ScreenMessage.barError(activity, R.string.toast_invalid_apiurl);
                        changeBackgroundColor(editValue, ERROR_COLOR);
                    } else if (!validReadOnlyAPIURL) {
                        ScreenMessage.barError(activity, R.string.toast_invalid_readonlyurl);
                        changeBackgroundColor(editValue2, ERROR_COLOR);
                    } else if (!validNotesAPIURL) {
                        ScreenMessage.barError(activity, R.string.toast_invalid_notesurl);
                        changeBackgroundColor(editValue3, ERROR_COLOR);
                    }
                });
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> dialog.dismiss());
            });

            return dialog;
        }
    }

    private static class AuthenticationAdapter extends ArrayAdapter<Auth> {

        private final String[] labels;

        /**
         * Construct a new adapter
         * 
         * @param context The current context.
         * @param resource The resource ID for a layout file containing a layout to use when instantiating views
         * @param objects The objects to represent in the ListView.
         * @param labels The labels to represent in the ListView.
         */
        public AuthenticationAdapter(@NonNull Context context, int resource, @NonNull Auth[] objects, @NonNull String[] labels) {
            super(context, resource, objects);
            if (objects.length != labels.length) {
                throw new IllegalArgumentException("Arrays should have same length");
            }
            this.labels = labels;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ((TextView) view).setText(labels[position]);
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }
}