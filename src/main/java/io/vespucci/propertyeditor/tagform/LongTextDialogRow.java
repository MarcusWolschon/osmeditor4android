package io.vespucci.propertyeditor.tagform;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.util.AttributeSet;
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
import io.vespucci.presets.PresetItem;
import io.vespucci.presets.PresetTextField;
import io.vespucci.util.StringWithDescription;
import io.vespucci.util.ThemeUtils;

/**
 * Display a single value and allow editing via a dialog
 */
public class LongTextDialogRow extends DialogRow {

    private static final int DEBOUNCE_DELAY = 1000;

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     */
    public LongTextDialogRow(@NonNull Context context) {
        super(context);
    }

    /**
     * Construct a row that will display a Dialog when clicked
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     */
    public LongTextDialogRow(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Add a row that displays a dialog for editing a larger body of text when clicked
     * 
     * @param caller the calling TagFormFragment instance
     * @param inflater the inflater to use
     * @param rowLayout the Layout holding the roes
     * @param preset the relevant PresetItem
     * @param value the current value
     * @param maxLength maximum length of text
     * @param hint a description of the value to display
     * @param key the key
     * @return an instance of TagFormDialogRow
     */
    static DialogRow getRow(@NonNull final TagFormFragment caller, @NonNull final LayoutInflater inflater, @NonNull final LinearLayout rowLayout,
            @Nullable final PresetItem preset, @NonNull final PresetTextField field, @NonNull final String value, int maxLength) {
        final DialogRow row = (DialogRow) inflater.inflate(R.layout.tag_form_text_dialog_row, rowLayout, false);
        String key = field.getKey();
        String hint = field.getHint();
        row.keyView.setText(hint != null ? hint : key);
        row.keyView.setTag(key);
        row.setPreset(preset);

        row.valueView.setHint(R.string.tag_tap_to_edit_hint);
        row.setValue(value);

        row.setOnClickListener(v -> {
            v.setEnabled(false); // debounce
            v.postDelayed(() -> v.setEnabled(true), DEBOUNCE_DELAY);
            buildLongTextDialog(caller, hint != null ? hint : key, key, row, maxLength).show();
        });

        return row;
    }

    /**
     * Build a dialog for editing a larger body of text
     * 
     * @param caller the calling TagFormFragment instance
     * @param hint a description to display
     * @param key the key
     * @param row the row we are started from
     * @param maxLength maximum length of text
     * @return an AlertDialog
     */
    private static AlertDialog buildLongTextDialog(@NonNull final TagFormFragment caller, @NonNull String hint, @NonNull String key,
            @NonNull final DialogRow row, int maxLength) {
        String value = row.getValue();
        Builder builder = new AlertDialog.Builder(caller.getActivity());
        builder.setTitle(hint);
        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(caller.getActivity());
        final View layout = themedInflater.inflate(R.layout.form_text_dialog, null);
        final EditText editText = (EditText) layout.findViewById(R.id.editText);
        editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
        editText.setMinLines(1);
        editText.setText(value);
        builder.setView(layout);
        builder.setNeutralButton(R.string.clear, null);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.save,
                (dialog, which) -> updateTag(((AlertDialog) dialog).getContext(), key, new StringWithDescription(editText.getText().toString())));
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface d) -> {
            editText.requestFocus();
            Button neutral = ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEUTRAL);
            neutral.setOnClickListener(view -> editText.setText(""));
        });
        return dialog;
    }
}
