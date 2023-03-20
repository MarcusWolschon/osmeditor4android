package de.blau.android.javascript;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.contract.MimeTypes;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SaveFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class ConsoleDialog {
    private static final String DEBUG_TAG = ConsoleDialog.class.getSimpleName();

    /**
     * Display a simple console with multi-line input and output from the eval method
     * 
     * @param activity android context
     * @param titleResource TODO
     * @param callback callback that actually evaluates the input
     */
    @SuppressLint("InflateParams")
    public static void show(@NonNull final FragmentActivity activity, int titleResource, @NonNull final EvalCallback callback) {
        // Create some useful objects
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        final Preferences prefs = App.getPreferences(activity);

        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(titleResource);
        ;

        View v = inflater.inflate(R.layout.console, null);
        final EditText input = (EditText) v.findViewById(R.id.input);
        final TextView output = (TextView) v.findViewById(R.id.output);
        builder.setView(v);

        builder.setPositiveButton(R.string.share, null);
        builder.setNegativeButton(R.string.evaluate, null);
        builder.setNeutralButton(R.string.dismiss, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button negative = ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEGATIVE);
            negative.setOnClickListener(view -> {
                Logic logic = App.getLogic();
                final ExecutorTask<String, Void, String> runner = new ExecutorTask<String, Void, String>(logic.getExecutorService(), logic.getHandler()) {
                    final AlertDialog progress = ProgressDialog.get(activity, Progress.PROGRESS_RUNNING);

                    @Override
                    protected void onPreExecute() {
                        progress.show();
                    }

                    @Override
                    protected String doInBackground(String text) {
                        try {
                            return callback.eval(text);
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
                popupMenu.setOnMenuItemClickListener(item -> {
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
                });
                popupMenu.show();
            });
        });
        dialog.show();
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
                try (BufferedOutputStream out = new BufferedOutputStream(activity.getContentResolver().openOutputStream(uri))) {
                    try {
                        out.write(script.getBytes());
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        result = ErrorCodes.FILE_WRITE_FAILED;
                        Log.e(DEBUG_TAG, "Problem writing", e);
                    }
                } catch (IOException e) {
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
