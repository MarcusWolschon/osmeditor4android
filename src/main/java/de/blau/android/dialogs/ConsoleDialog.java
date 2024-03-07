package de.blau.android.dialogs;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.contract.MimeTypes;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SaveFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class ConsoleDialog extends DialogFragment {
    private static final String DEBUG_TAG = ConsoleDialog.class.getSimpleName();

    private static final String TAG              = "consoledialog";
    private static final String TITLE_KEY        = "title";
    private static final String INITIAL_TEXT_KEY = "initial_text";
    private static final String CALLBACK_KEY     = "callback";
    private static final String CHECKBOX1_KEY    = "checkbox1";
    private static final String CHECKBOX2_KEY    = "checkbox2";

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param titleResource text resource for the title
     * @param checkbox1Resource checkbox text resource for flag 1
     * @param checkbox2Resource checkbox text resource for flag 2
     * @param initialText initial code to display
     * @param callback code to execute on when Run/Evaluate is clicked
     */
    public static void showDialog(@NonNull FragmentActivity activity, int titleResource, int checkbox1Resource, int checkbox2Resource,
            @Nullable String initialText, @NonNull final EvalCallback callback) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ConsoleDialog consoleFragment = newInstance(titleResource, checkbox1Resource, checkbox2Resource, initialText, callback);
            consoleFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new instance
     * 
     * @param titleResource text resource for the title
     * @param checkbox1Resource checkbox text resource for flag 1
     * @param checkbox2Resource checkbox text resource for flag 2
     * @param initialText initial code to display
     * @param callback code to execute on when Run/Evaluate is clicked
     * 
     * @return a ConsoleDialog instance
     */
    private static ConsoleDialog newInstance(int titleResource, int checkbox1Resource, int checkbox2Resource, @Nullable String initialText,
            @NonNull final EvalCallback callback) {

        ConsoleDialog f = new ConsoleDialog();

        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, titleResource);
        args.putInt(CHECKBOX1_KEY, checkbox1Resource);
        args.putInt(CHECKBOX2_KEY, checkbox2Resource);
        args.putString(INITIAL_TEXT_KEY, initialText);
        args.putSerializable(CALLBACK_KEY, callback);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Builder builder = new AlertDialog.Builder(getActivity());
        int titleResource = getArguments().getInt(TITLE_KEY);
        EvalCallback callback = Util.getSerializeable(getArguments(), CALLBACK_KEY, EvalCallback.class);
        int checkbox1Resource = getArguments().getInt(CHECKBOX1_KEY);
        int checkbox2Resource = getArguments().getInt(CHECKBOX2_KEY);
        String initialText = getArguments().getString(INITIAL_TEXT_KEY);

        // Create some useful objects
        final FragmentActivity activity = getActivity();

        final Preferences prefs = App.getPreferences(getActivity());
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        View v = inflater.inflate(R.layout.console, null);
        final EditText input = (EditText) v.findViewById(R.id.input);
        final TextView output = (TextView) v.findViewById(R.id.output);
        final CheckBox checkbox1 = (CheckBox) v.findViewById(R.id.checkbox1);
        final CheckBox checkbox2 = (CheckBox) v.findViewById(R.id.checkbox2);

        setUpCheckBox(checkbox1Resource, checkbox1);
        setUpCheckBox(checkbox2Resource, checkbox2);

        if (initialText != null) {
            input.setText(initialText);
        }

        builder.setTitle(titleResource);
        builder.setView(v);
        builder.setPositiveButton(R.string.share, null);
        builder.setNegativeButton(R.string.run, null);
        builder.setNeutralButton(R.string.Done, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button negative = ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEGATIVE);
            negative.setOnClickListener(view -> {
                Logic logic = App.getLogic();
                final ExecutorTask<String, Void, String> runner = new ExecutorTask<String, Void, String>(logic.getExecutorService(), logic.getHandler()) {
                    final AlertDialog progress = ProgressDialog.get(getActivity(), Progress.PROGRESS_RUNNING);

                    @Override
                    protected void onPreExecute() {
                        progress.show();
                    }

                    @Override
                    protected String doInBackground(String text) {
                        try {
                            return callback.eval(getActivity(), text, checkbox1.isChecked(), checkbox2.isChecked());
                        } catch (Exception ex) {
                            Log.e(DEBUG_TAG, "dialog failed with " + ex);
                            return ex.getMessage();
                        }
                    }

                    @Override
                    protected void onPostExecute(final String result) {
                        try {
                            progress.dismiss();
                        } catch (Exception ex) {
                            Log.e(DEBUG_TAG, "dismiss dialog failed with " + ex);
                        }
                        // small hack to display HTML correctly
                        output.setText(result.startsWith("<") ? Util.fromHtml(result) : result);
                    }
                };
                runner.execute(input.getText().toString());
            });
            Button positive = ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE);
            Drawable more = ThemeUtils.getTintedDrawable(activity, R.drawable.ic_more_vert_black_24dp, R.attr.colorAccent);
            positive.setCompoundDrawablesWithIntrinsicBounds(null, null, more, null);
            positive.setText("");
            positive.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(activity, view);
                popupMenu.inflate(R.menu.console_popup);
                popupMenu.setOnMenuItemClickListener(getOnItemClickListener(prefs, input));
                popupMenu.show();
            });
        });
        return dialog;
    }

    /**
     * Get an OnMenuItemClickListener for the overflow menu lookalike
     * 
     * @param prefs the current Preferences instance
     * @param input the EditText with the code
     * @return
     */
    private OnMenuItemClickListener getOnItemClickListener(@NonNull final Preferences prefs, @NonNull final EditText input) {
        final FragmentActivity activity = getActivity();
        return item -> {
            switch (item.getItemId()) {
            case R.id.console_menu_share:
                String text = input.getText().toString();
                Intent shareIntent = new ShareCompat.IntentBuilder(activity).setText(text).setType(MimeTypes.TEXTPLAIN).getIntent();
                activity.startActivity(shareIntent);
                break;
            case R.id.console_menu_save:
                SelectFile.save(activity, R.string.config_scriptsPreferredDir_key, new SaveFile() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean save(Uri fileUri) {
                        fileUri = FileUtil.contentUriToFileUri(activity, fileUri);
                        if (fileUri == null) {
                            Log.e(DEBUG_TAG, "Couldn't convert " + fileUri);
                            return false;
                        }
                        writeScriptFile(activity, fileUri, input.getText().toString(), null);
                        SelectFile.savePref(prefs, R.string.config_scriptsPreferredDir_key, fileUri);
                        return true;
                    }
                });
                break;
            case R.id.console_menu_load:
                SelectFile.read(activity, R.string.config_scriptsPreferredDir_key, new ReadFile() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean read(Uri fileUri) {
                        readScriptFile(activity, fileUri, input, null);
                        SelectFile.savePref(prefs, R.string.config_scriptsPreferredDir_key, fileUri);
                        return true;
                    }
                });
                break;
            default:
                Log.w(DEBUG_TAG, "Unknown menu item " + item.getItemId());
            }
            return true;
        };
    }

    /**
     * Set up a checkbox if checkboxResource is valid
     * 
     * @param checkboxResource the text resource for the checkbox
     * @param checkbox the CheckBox
     */
    private void setUpCheckBox(int checkboxResource, @NonNull final CheckBox checkbox) {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (checkboxResource > 0) {
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setText(checkboxResource);
            String prefKey = getClass().getSimpleName() + "-" + getString(checkboxResource);
            checkbox.setChecked(sharedPrefs.getBoolean(prefKey, false));
            checkbox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> sharedPrefs.edit().putBoolean(prefKey, isChecked).commit());
        }
    }

    /**
     * Write the contents of the console to a Uri
     * 
     * @param activity the calling Activity
     * @param uri an Uri pointing to the output destination
     * @param script the script to save
     * @param postSaveHandler called after saving
     */
    private static void writeScriptFile(@NonNull final FragmentActivity activity, @NonNull final Uri uri, @NonNull final String script,
            @Nullable final PostAsyncActionHandler postSaveHandler) {
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Integer>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_SAVING);
            }

            @Override
            protected Integer doInBackground(Void arg) {
                int result = 0;
                try (BufferedOutputStream out = new BufferedOutputStream(activity.getContentResolver().openOutputStream(uri, FileUtil.TRUNCATE_WRITE_MODE))) {
                    out.write(script.getBytes());
                } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                    result = ErrorCodes.FILE_WRITE_FAILED;
                    Log.e(DEBUG_TAG, "Problem writing", e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                Progress.dismissDialog(activity, Progress.PROGRESS_SAVING);
                if (result != 0) {
                    if (postSaveHandler != null) {
                        postSaveHandler.onError(null);
                    }
                } else {
                    if (postSaveHandler != null) {
                        postSaveHandler.onSuccess();
                    }
                }
            }
        }.execute();
    }

    /**
     * Read a script in to a EditText widget
     * 
     * @param activity the calling Activity
     * @param uri the URI of the file to load
     * @param input the EditTExt
     * @param postLoad called after loading
     */
    public static void readScriptFile(@NonNull final FragmentActivity activity, final Uri uri, final EditText input, final PostAsyncActionHandler postLoad) {
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, String>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_LOADING);
            }

            @Override
            protected String doInBackground(Void arg) {
                ByteArrayOutputStream result = null;
                String r = null;
                try (InputStream is = activity.getContentResolver().openInputStream(uri)) {
                    result = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                    r = result.toString(OsmXml.UTF_8);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Problem reading", e);
                } finally {
                    SavingHelper.close(result);
                }
                return r;
            }

            @Override
            protected void onPostExecute(String result) {
                Progress.dismissDialog(activity, Progress.PROGRESS_LOADING);
                if (result == null) {
                    if (postLoad != null) {
                        postLoad.onError(null);
                    }
                } else {
                    if (postLoad != null) {
                        postLoad.onSuccess();
                    }
                    input.setText(result);
                }
            }
        }.execute();
    }
}
