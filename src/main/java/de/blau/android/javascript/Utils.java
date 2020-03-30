package de.blau.android.javascript;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.BuildConfig;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.osm.OsmXml;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SaveFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Various JS related utility methods
 * 
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts">https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts</a>
 * @see <a href=
 *      "https://dxr.mozilla.org/mozilla/source/js/rhino/examples/DynamicScopes.java">https://dxr.mozilla.org/mozilla/source/js/rhino/examples/DynamicScopes.java</a>
 * @author simon
 *
 */
public final class Utils {

    private static final String DEBUG_TAG = "javascript.Utils";

    /**
     * Empty private constructor
     */
    private Utils() {
        // don't allow instantiating of this class
    }

    /**
     * Evaluate JS
     * 
     * @param ctx android context
     * @param scriptName name for error reporting
     * @param script the javascript
     * @return whatever the JS returned as a string
     */
    @Nullable
    public static String evalString(Context ctx, String scriptName, String script) {
        Log.d(DEBUG_TAG, "Eval " + script);
        org.mozilla.javascript.Context rhinoContext = App.getRhinoHelper(ctx).enterContext();
        try {
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object result = rhinoContext.evaluateString(scope, script, scriptName, 1, null);
            return org.mozilla.javascript.Context.toString(result);
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Evaluate JS associated with a key in a preset
     * 
     * @param ctx android context
     * @param scriptName name for error reporting
     * @param script the javascript
     * @param originalTags original tags the property editor was called with
     * @param tags the current tags
     * @param value any value associated with the key
     * @param key2PresetItem map from key to PresetItem
     * @param presets the currently available Presets
     * @return the value that should be assigned to the tag or null if no value should be set
     */
    @Nullable
    public static String evalString(@NonNull Context ctx, @NonNull String scriptName, @NonNull String script, @NonNull Map<String, List<String>> originalTags,
            @NonNull Map<String, List<String>> tags, @NonNull String value, @NonNull Map<String, PresetItem> key2PresetItem, @NonNull Preset[] presets) {
        org.mozilla.javascript.Context rhinoContext = App.getRhinoHelper(ctx).enterContext();
        try {
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object wrappedOut = org.mozilla.javascript.Context.javaToJS(BuildConfig.VERSION_CODE, scope);
            ScriptableObject.putProperty(scope, "versionCode", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(originalTags, scope);
            ScriptableObject.putProperty(scope, "originalTags", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(tags, scope);
            ScriptableObject.putProperty(scope, "tags", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(value, scope);
            ScriptableObject.putProperty(scope, "value", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(key2PresetItem, scope);
            ScriptableObject.putProperty(scope, "key2PresetItem", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(presets, scope);
            ScriptableObject.putProperty(scope, "presets", wrappedOut);
            Log.d(DEBUG_TAG, "Eval (preset): " + script);
            Object result = rhinoContext.evaluateString(scope, script, scriptName, 1, null);
            if (result == null) {
                return null;
            } else {
                return org.mozilla.javascript.Context.toString(result);
            }
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Evaluate a script making the current logic available, this essentially allows access to all data
     * 
     * @param ctx android context
     * @param scriptName name for error reporting
     * @param script the javascript
     * @param logic an instance of Logic
     * @return result of evaluating the JS as a string
     */
    @Nullable
    public static String evalString(@NonNull Context ctx, @NonNull String scriptName, @NonNull String script, @NonNull Logic logic) {
        org.mozilla.javascript.Context rhinoContext = App.getRhinoHelper(ctx).enterContext();
        try {
            Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
            Scriptable scope = rhinoContext.newObject(restrictedScope);
            scope.setPrototype(restrictedScope);
            scope.setParentScope(null);
            Object wrappedOut = org.mozilla.javascript.Context.javaToJS(BuildConfig.VERSION_CODE, scope);
            ScriptableObject.putProperty(scope, "versionCode", wrappedOut);
            wrappedOut = org.mozilla.javascript.Context.javaToJS(logic, scope);
            ScriptableObject.putProperty(scope, "logic", wrappedOut);
            Log.d(DEBUG_TAG, "Eval (logic): " + script);
            Object result = rhinoContext.evaluateString(scope, script, scriptName, 1, null);
            if (result == null) {
                return null;
            } else {
                return org.mozilla.javascript.Context.toString(result);
            }
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    /**
     * Display a simple console with multi-line input and output from the eval method
     * 
     * @param activity android context
     * @param msgResource sub title to display
     * @param callback callback that actually evaluates the input
     */
    @SuppressLint("InflateParams")
    public static void jsConsoleDialog(@NonNull final FragmentActivity activity, int msgResource, @NonNull final EvalCallback callback) {
        // Create some useful objects
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        final Preferences prefs = new Preferences(activity);

        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.tag_menu_js_console);
        Snack.toastTopInfo(activity, msgResource);

        View v = inflater.inflate(R.layout.debug_js, null);
        final EditText input = (EditText) v.findViewById(R.id.js_input);
        final TextView output = (TextView) v.findViewById(R.id.js_output);
        builder.setView(v);

        builder.setPositiveButton(R.string.evaluate, null);
        builder.setNegativeButton(R.string.dismiss, null);
        builder.setNeutralButton(R.string.share, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final AsyncTask<String, Void, String> runner = new AsyncTask<String, Void, String>() {
                            final AlertDialog progress = ProgressDialog.get(activity, Progress.PROGRESS_RUNNING);

                            @Override
                            protected void onPreExecute() {
                                progress.show();
                            }

                            @Override
                            protected String doInBackground(String... js) {
                                try {
                                    return callback.eval(js[0]);
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
                                output.setText(result);
                            }
                        };
                        runner.execute(input.getText().toString());
                    }
                });
                Button neutral = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                Drawable more = ThemeUtils.getTintedDrawable(activity, R.drawable.ic_more_vert_black_36dp, R.attr.colorAccent);
                neutral.setCompoundDrawablesWithIntrinsicBounds(more, null, null, null);
                neutral.setText("");
                neutral.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PopupMenu popupMenu = new PopupMenu(activity, view);
                        popupMenu.inflate(R.menu.js_popup);
                        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                case R.id.js_menu_share:
                                    Intent sendIntent = new Intent();
                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, input.getText().toString());
                                    sendIntent.setType("text/plain");
                                    activity.startActivity(sendIntent);
                                    break;
                                case R.id.js_menu_save:
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
                                case R.id.js_menu_read:
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
                                }
                                return true;
                            }

                        });
                        popupMenu.show();
                    }
                });
            }
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

        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_SAVING);
            }

            @Override
            protected Integer doInBackground(Void... arg) {
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
                        postSaveHandler.onError();
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
        new AsyncTask<Void, Void, String>() {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_LOADING);
            }

            @Override
            protected String doInBackground(Void... arg) {
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
                        postLoad.onError();
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
