package de.blau.android.prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.MimeTypes;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.Progress;
import de.blau.android.exception.OperationFailedException;
import de.blau.android.osm.Server;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetIconManager;
import de.blau.android.services.util.StreamUtils;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Provides an activity to edit the preset list. Downloads preset data when necessary. */
public class PresetEditorActivity extends URLListEditActivity {

    private static final String DEBUG_TAG = "PresetEditorActivity";

    private AdvancedPrefDatabase db;

    private static final int MENU_RELOAD = 1;
    private static final int MENU_UP     = 2;
    private static final int MENU_DOWN   = 3;

    private static final int MENUITEM_HELP = 1;

    private static final String FILE_NAME_TEMPORARY_ARCHIVE = "temp.zip";

    /**
     * Construct a new instance
     */
    public PresetEditorActivity() {
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
        Intent intent = new Intent(context, PresetEditorActivity.class);
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
        Intent intent = new Intent(activity, PresetEditorActivity.class);
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
        Log.d("AdvancedPrefEditor", "onOptionsItemSelected");
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
    protected int getAddTextResId() {
        return R.string.urldialog_add_preset;
    }

    @Override
    protected void onLoadList(List<ListEditItem> items) {
        PresetInfo[] presets = db.getPresets();
        for (PresetInfo preset : presets) {
            items.add(new ListEditItem(preset.id, preset.name, preset.url, preset.useTranslations, preset.active));
        }
    }

    @Override
    protected void onItemClicked(ListEditItem item) {
        if (item.active && db.getActivePresets().length == 1) { // at least one item needs to be selected
            updateAdapter();
            Snack.barWarning(this, R.string.toast_min_one_preset);
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
        downloadPresetData(item);
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
            db.removePresetDirectory(item.id);
            downloadPresetData(item);
        }
        App.resetPresets();
    }

    @Override
    protected void onItemDeleted(ListEditItem item) {
        db.deletePreset(item.id);
        App.resetPresets();
    }

    @Override
    public void onAdditionalMenuItemClick(int menuItemId, ListEditItem clickedItem) {
        switch (menuItemId) {
        case MENU_RELOAD:
            PresetInfo preset = db.getPreset(clickedItem.id);
            if (preset.url != null) {
                downloadPresetData(clickedItem);
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
     * Download data (XML, icons) for a certain preset
     * 
     * @param item the item containing the preset to be downloaded
     */
    private void downloadPresetData(@NonNull final ListEditItem item) {
        final File presetDir = db.getPresetDirectory(item.id);
        // noinspection ResultOfMethodCallIgnored
        presetDir.mkdir();
        if (!presetDir.isDirectory()) {
            throw new OperationFailedException("Could not create preset directory " + presetDir.getAbsolutePath());
        }
        if (item.value.startsWith(Preset.APKPRESET_URLPREFIX)) {
            PresetEditorActivity.super.sendResultIfApplicable(item);
            return;
        }
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Integer, Integer>(logic.getExecutorService(), logic.getHandler()) {
            private boolean canceled = false;

            private static final int RESULT_TOTAL_FAILURE       = 0;
            private static final int RESULT_TOTAL_SUCCESS       = 1;
            private static final int RESULT_IMAGE_FAILURE       = 2;
            private static final int RESULT_PRESET_NOT_PARSABLE = 3;
            private static final int RESULT_DOWNLOAD_CANCELED   = 4;

            private static final int DOWNLOADED_PRESET_ERROR = -1;
            private static final int DOWNLOADED_PRESET_XML   = 0;
            private static final int DOWNLOADED_PRESET_ZIP   = 1;

            @Override
            protected void onPreExecute() {
                Progress.showDialog(PresetEditorActivity.this, Progress.PROGRESS_PRESET);
            }

            @Override
            protected Integer doInBackground(Void args) {
                int loadResult = RESULT_TOTAL_SUCCESS;
                Uri uri = Uri.parse(item.value);
                final String scheme = uri.getScheme();
                if (Schemes.FILE.equals(scheme) || Schemes.CONTENT.equals(scheme)) {
                    loadResult = load(uri, Preset.PRESETXML);
                } else {
                    loadResult = download(item.value, Preset.PRESETXML);
                }

                if (loadResult == DOWNLOADED_PRESET_ERROR) {
                    return RESULT_TOTAL_FAILURE;
                } else if (loadResult == DOWNLOADED_PRESET_ZIP) {
                    return RESULT_TOTAL_SUCCESS;
                } // fall through to further processing

                List<String> urls = Preset.parseForURLs(presetDir);
                if (urls == null) {
                    Log.e(DEBUG_TAG, "Could not parse preset for URLs");
                    return RESULT_PRESET_NOT_PARSABLE;
                }

                boolean allImagesSuccessful = true;
                for (String url : urls) {
                    if (canceled) {
                        return RESULT_DOWNLOAD_CANCELED;
                    }
                    allImagesSuccessful &= (download(url, null) == DOWNLOADED_PRESET_XML);
                }
                return allImagesSuccessful ? RESULT_TOTAL_SUCCESS : RESULT_IMAGE_FAILURE;
            }

            /**
             * Download a Preset
             * 
             * @param url the url to download from
             * @param filename A filename where to save the file. If null, the URL will be hashed using the
             *            PresetIconManager hash function and the file will be saved to hashvalue.png (where "hashvalue"
             *            will be replaced with the URL hash).
             * @return code indicating result
             */
            private int download(@NonNull String url, @Nullable String filename) {
                if (filename == null) {
                    filename = PresetIconManager.hash(url) + ".png";
                }
                InputStream downloadStream = null;
                OutputStream fileStream = null;
                try {
                    Log.d(DEBUG_TAG, "Downloading " + url + " to " + presetDir + "/" + filename);
                    boolean zip = false;

                    Request request = new Request.Builder().url(url).build();
                    OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                            .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
                    Call presetCall = client.newCall(request);
                    Response presetCallResponse = presetCall.execute();
                    if (presetCallResponse.isSuccessful()) {
                        ResponseBody responseBody = presetCallResponse.body();
                        downloadStream = responseBody.byteStream();
                        String contentType = responseBody.contentType().toString();
                        zip = (contentType != null && MimeTypes.ZIP.equalsIgnoreCase(contentType))
                                || url.toLowerCase(Locale.US).endsWith("." + FileExtensions.ZIP);
                        if (zip) {
                            Log.d(DEBUG_TAG, "detected zip file");
                            filename = FILE_NAME_TEMPORARY_ARCHIVE;
                        }
                    } else {
                        Log.w(DEBUG_TAG, "Could not download file " + url + " respose code " + presetCallResponse.code());
                        return DOWNLOADED_PRESET_ERROR;
                    }
                    fileStream = new FileOutputStream(new File(presetDir, filename));
                    StreamUtils.copy(downloadStream, fileStream);

                    if (zip && FileUtil.unpackZip(presetDir.getPath() + "/", filename)) {
                        if (!(new File(presetDir, FILE_NAME_TEMPORARY_ARCHIVE)).delete()) { // NOSONAR requires API 26
                            Log.e(DEBUG_TAG, "Could not delete " + FILE_NAME_TEMPORARY_ARCHIVE);
                        }
                        return DOWNLOADED_PRESET_ZIP;
                    }
                    return DOWNLOADED_PRESET_XML;
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Could not download file " + url + " " + e.getMessage());
                    return DOWNLOADED_PRESET_ERROR;
                } finally {
                    SavingHelper.close(downloadStream);
                    SavingHelper.close(fileStream);
                }
            }

            /**
             * Load a Preset from a local file
             * 
             * @param uri the uri to load from
             * @param filename A filename where to save the file.
             * @return code indicating result
             */
            private int load(@NonNull Uri uri, @NonNull String filename) {
                boolean zip = uri.getPath().toLowerCase(Locale.US).endsWith("." + FileExtensions.ZIP)
                        || MimeTypes.ZIP.equals(getContentResolver().getType(uri));
                if (zip) {
                    Log.d(DEBUG_TAG, "detected zip file");
                    filename = FILE_NAME_TEMPORARY_ARCHIVE;
                }
                try (InputStream loadStream = getContentResolver().openInputStream(uri);
                        OutputStream fileStream = new FileOutputStream(new File(presetDir, filename));) {
                    Log.d(DEBUG_TAG, "Loading " + uri + " to " + presetDir + "/" + filename);
                    StreamUtils.copy(loadStream, fileStream);
                    if (zip && FileUtil.unpackZip(presetDir.getPath() + "/", filename)) {
                        if (!(new File(presetDir, FILE_NAME_TEMPORARY_ARCHIVE)).delete()) { // NOSONAR requires API 26
                            Log.e(DEBUG_TAG, "Could not delete " + FILE_NAME_TEMPORARY_ARCHIVE);
                        }
                        return DOWNLOADED_PRESET_ZIP;
                    }
                    return DOWNLOADED_PRESET_XML;
                } catch (Exception e) {
                    Log.e(DEBUG_TAG, "Could not load file " + uri + " " + e.getMessage());
                    return DOWNLOADED_PRESET_ERROR;
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
                Progress.dismissDialog(PresetEditorActivity.this, Progress.PROGRESS_PRESET);
                switch (result) {
                case RESULT_TOTAL_SUCCESS:
                    Snack.barInfo(PresetEditorActivity.this, R.string.preset_download_successful);
                    PresetEditorActivity.super.sendResultIfApplicable(item);
                    break;
                case RESULT_TOTAL_FAILURE:
                    msgbox(R.string.preset_download_failed);
                    break;
                case RESULT_PRESET_NOT_PARSABLE:
                    db.removePresetDirectory(item.id);
                    msgbox(R.string.preset_download_parse_failed);
                    break;
                case RESULT_IMAGE_FAILURE:
                    msgbox(R.string.preset_download_missing_images);
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
                AlertDialog.Builder box = new AlertDialog.Builder(PresetEditorActivity.this);
                box.setMessage(getResources().getString(msgResID));
                box.setOnCancelListener(dialog -> PresetEditorActivity.super.sendResultIfApplicable(item));
                box.setPositiveButton(R.string.okay, (dialog, which) -> {
                    dialog.dismiss();
                    PresetEditorActivity.super.sendResultIfApplicable(item);
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
    @SuppressLint("InflateParams")
    @Override
    protected void itemEditDialog(final ListEditItem item) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);
        final View mainView = inflater.inflate(R.layout.listedit_presetedit, null);
        final TextView editName = (TextView) mainView.findViewById(R.id.listedit_editName);
        final TextView editValue = (TextView) mainView.findViewById(R.id.listedit_editValue);
        final CheckBox useTranslations = (CheckBox) mainView.findViewById(R.id.listedit_translations);
        final ImageButton fileButton = (ImageButton) mainView.findViewById(R.id.listedit_file_button);

        if (item != null) {
            editName.setText(item.name);
            editValue.setText(item.value);
            useTranslations.setChecked(item.boolean0);
        } else if (isAddingViaIntent()) {
            String tmpName = getIntent().getExtras().getString(EXTRA_NAME);
            String tmpValue = getIntent().getExtras().getString(EXTRA_VALUE);
            editName.setText(tmpName == null ? "" : tmpName);
            editValue.setText(tmpValue == null ? "" : tmpValue);
            useTranslations.setChecked(true);
        }
        if (item != null && item.id.equals(LISTITEM_ID_DEFAULT)) {
            // name and value are not editable
            editName.setInputType(InputType.TYPE_NULL);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                editName.setBackground(null);
            }
            editValue.setEnabled(false);
            fileButton.setEnabled(false);
        }

        builder.setView(mainView);
        builder.setPositiveButton(R.string.okay, (dialog, which) -> {
            // Do nothing here because we override this button later to change the close behaviour.
            // However, we still need this because on older versions of Android unless we
            // pass a handler the button doesn't get instantiated
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            // leave empty
        });
        builder.setOnCancelListener(dialog -> {
            if (isAddingViaIntent()) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        fileButton.setOnClickListener(v -> SelectFile.read(PresetEditorActivity.this, R.string.config_presetsPreferredDir_key, new ReadFile() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean read(Uri fileUri) {
                editValue.setText(fileUri.toString());
                SelectFile.savePref(new Preferences(PresetEditorActivity.this), R.string.config_presetsPreferredDir_key, fileUri);
                return true;
            }
        }));

        final AlertDialog dialog = builder.create();
        dialog.setView(mainView);
        dialog.show();

        // overriding the handlers
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String presetURL = editValue.getText().toString().trim();
            boolean useTranslationsEnabled = useTranslations.isChecked();
            changeBackgroundColor(editValue, VALID_COLOR);
            // validate entries
            boolean validPresetURL = Patterns.WEB_URL.matcher(presetURL).matches();
            URL url = null;
            try {
                url = new URL(presetURL);
            } catch (MalformedURLException e) {
                validPresetURL = false;
            }

            // save or display toast, exception for localhost is needed for testing
            if (validPresetURL || presetURL.startsWith(Schemes.FILE) || presetURL.startsWith(Schemes.CONTENT)
                    || (url != null && "localhost".equals(url.getHost())) || (item != null && item.id.equals(LISTITEM_ID_DEFAULT))) {
                if (item == null) {
                    // new item
                    finishCreateItem(new ListEditItem(name, presetURL, null, null, useTranslationsEnabled));
                } else {
                    item.name = name;
                    item.value = presetURL;
                    item.boolean0 = useTranslationsEnabled;
                    finishEditItem(item);
                }
                dialog.dismiss();
            } else {
                // if garbage value entered show toasts
                Snack.barError(PresetEditorActivity.this, R.string.toast_invalid_preseturl);
                changeBackgroundColor(editValue, ERROR_COLOR);
            }
        });

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> dialog.dismiss());
    }
}
