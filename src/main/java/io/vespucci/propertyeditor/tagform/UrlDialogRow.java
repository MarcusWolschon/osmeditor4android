package io.vespucci.propertyeditor.tagform;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.dialogs.Progress;
import io.vespucci.dialogs.ProgressDialog;
import io.vespucci.net.UrlCheck;
import io.vespucci.net.UrlCheck.CheckStatus;
import io.vespucci.presets.PresetItem;
import io.vespucci.propertyeditor.PropertyEditorListener;
import io.vespucci.util.ExecutorTask;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.ThemeUtils;

/**
 * Display a single value and allow editing via a dialog
 */
public class UrlDialogRow extends DialogRow {

    private static final String DEBUG_TAG = UrlDialogRow.class.getSimpleName().substring(0, Math.min(23, UrlDialogRow.class.getSimpleName().length()));

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     */
    public UrlDialogRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public UrlDialogRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Add a row that displays a dialog for selecting a single when clicked
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the roes
     * @param preset the relevant PresetItem
     * @param hint a description of the value to display
     * @param key the key
     * @param value the current value
     * @return an instance of DialogRow
     */
    static DialogRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @NonNull final PresetItem preset, @Nullable final String hint, @NonNull final String key, @NonNull final String value) {
        final DialogRow row = (DialogRow) inflater.inflate(R.layout.tag_form_combo_dialog_row, rowLayout, false);
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);
        row.setValue(value);
        row.valueView.setHint(R.string.tag_tap_to_edit_hint);
        row.setOnClickListener(v -> {
            final View finalView = v;
            finalView.setEnabled(false); // debounce
            final AlertDialog dialog = buildUrlDialog(caller, hint != null ? hint : key, key, row, preset);
            dialog.setOnDismissListener(d -> finalView.setEnabled(true));
            dialog.show();
        });
        return row;
    }

    /**
     * Build a dialog for adding/Editing an url and checking it online
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key
     * @param row the row we are started from
     * @param preset the relevant PresetItem
     * @return an AlertDialog
     */
    private static AlertDialog buildUrlDialog(@NonNull final TagFormFragment caller, @NonNull String hint, @NonNull String key, @NonNull final DialogRow row,
            @NonNull final PresetItem preset) {
        final AlertDialog progress = ProgressDialog.get(caller.getActivity(), Progress.PROGRESS_SEARCHING);

        String value = row.getValue();
        Builder builder = new AlertDialog.Builder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());

        final View layout = themedInflater.inflate(R.layout.text_line, null);
        final EditText input = layout.findViewById(R.id.text_line_edit);
        input.setText(value);
        builder.setView(layout);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String ourValue = input.getText().toString();
            caller.updateSingleValue((String) layout.getTag(), ourValue);
            row.setValue(ourValue);
            row.setChanged(true);
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.setNeutralButton(R.string.check, null);

        final AlertDialog dialog = builder.create();
        layout.setTag(key);

        dialog.setOnShowListener(d -> {
            Button neutral = ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEUTRAL);
            if (caller.getActivity() instanceof PropertyEditorListener) {
                neutral.setEnabled(((PropertyEditorListener) caller.getActivity()).isConnected());
            }
            neutral.setOnClickListener(view -> {
                Logic logic = App.getLogic();
                ExecutorTask<String, Void, UrlCheck.Result> loader = new ExecutorTask<String, Void, UrlCheck.Result>(logic.getExecutorService(),
                        logic.getHandler()) {

                    @Override
                    protected void onPreExecute() {
                        progress.show();
                    }

                    @Override
                    protected UrlCheck.Result doInBackground(String url) {
                        return UrlCheck.check(caller.getContext(), url);
                    }

                    @Override
                    protected void onPostExecute(UrlCheck.Result result) {
                        Log.d(DEBUG_TAG, "onPostExecute");
                        try {
                            progress.dismiss();
                        } catch (Exception ex) {
                            Log.e(DEBUG_TAG, "dismiss dialog failed with " + ex);
                        }
                        input.setText(result.getUrl());
                        CheckStatus status = result.getStatus();
                        if (status != CheckStatus.HTTP && status != CheckStatus.HTTPS) {
                            Activity activity = caller.getActivity();
                            String[] statusStrings = activity.getResources().getStringArray(R.array.checkstatus_entries);
                            if (status == CheckStatus.INVALID) {
                                String msg = result.getMessage() != null
                                        ? activity.getString(R.string.toast_url_check_with_msg, result.getUrl(), result.getCode(), result.getMessage())
                                        : activity.getString(R.string.toast_url_check, result.getUrl(), result.getCode());
                                ScreenMessage.toastTopError(activity, msg);
                            } else {
                                ScreenMessage.toastTopError(activity, statusStrings[status.ordinal()]);
                            }
                        }
                    }
                };
                loader.execute(input.getText().toString());
            });
        });
        return dialog;
    }
}
