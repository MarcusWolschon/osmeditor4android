package de.blau.android.dialogs;

import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.AppCompatCheckBox;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class TextLineDialog {

    /**
     * Create a Dialog with one EditText
     * 
     * @param ctx the Android Context
     * @param titleId the string resource for the title
     * @param hintId string resource for an hint of -1 if none
     * @param listener a TextLineInterface listener provided by the caller
     * @param dismiss dismiss dialog when the positive button is clicked when true
     * @return an AlertDialog instance
     */
    public static AlertDialog get(@NonNull Context ctx, int titleId, int hintId, @NonNull TextLineInterface listener, boolean dismiss) {
        return get(ctx, titleId, hintId, -1, (List<String>) null, null, listener, null, null, dismiss);
    }

    /**
     * Create a Dialog with one EditText
     * 
     * @param ctx the Android Context
     * @param titleId the string resource for the title
     * @param hintId string resource for an hint of -1 if none
     * @param text initial text to display
     * @param listener a TextLineInterface listener provided by the caller
     * @return an AlertDialog instance
     */
    public static AlertDialog get(@NonNull Context ctx, int titleId, int hintId, @Nullable String text, @NonNull TextLineInterface listener) {
        return get(ctx, titleId, hintId, -1, text != null ? Util.wrapInList(text) : null, null, listener, null, null, true);
    }

    /**
     * Create a Dialog with one EditText
     * 
     * @param ctx the Android Context
     * @param titleId the string resource for the title
     * @param hintId string resource for an hint of -1 if none
     * @param text initial text to display
     * @param listener a TextLineInterface listener provided by the caller
     * @param dismiss dismiss dialog when the positive button is clicked when true
     * @return an AlertDialog instance
     */
    public static AlertDialog get(@NonNull Context ctx, int titleId, int hintId, @Nullable String text, @NonNull TextLineInterface listener, boolean dismiss) {
        return get(ctx, titleId, hintId, -1, text != null ? Util.wrapInList(text) : null, null, listener, null, null, dismiss);
    }

    /**
     * Create a Dialog with one EditText
     * 
     * @param ctx the Android Context
     * @param titleId the string resource for the title
     * @param hintId string resource for an hint of -1 if none
     * @param checkTextId string resource for an optional checkbox, if -1 no checkbox is shown
     * @param prevText a List of previous input to display
     * @param posButtonText positive button text or null
     * @param posListener a TextLineInterface listener provided by the caller used by the pos. button
     * @param negButtonText negative button text or null
     * @param negListener a TextLineInterface listener provided by the caller used by the neg. button
     * @param dismiss dismiss dialog when the positive button is clicked when true
     * @return an AlertDialog instance
     */
    public static AlertDialog get(@NonNull Context ctx, int titleId, int hintId, int checkTextId, @Nullable List<String> prevText,
            @Nullable String posButtonText, @NonNull TextLineInterface posListener, @Nullable String negButtonText, @Nullable TextLineInterface negListener,
            boolean dismiss) {

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);

        Builder builder = ThemeUtils.getAlertDialogBuilder(ctx);
        builder.setTitle(titleId);

        View layout = inflater.inflate(R.layout.text_line, null);
        final AutoCompleteTextView input = layout.findViewById(R.id.text_line_edit);
        if (hintId > 0) {
            input.setHint(hintId);
        }
        if (prevText != null) {
            if (prevText.size() == 1) {
                input.setText(prevText.get(0));
            } else {
                input.setAdapter(new ArrayAdapter<String>(ctx, R.layout.autocomplete_row, prevText));
                input.setThreshold(0);
                input.setOnClickListener(v -> {
                    if (v.hasFocus()) {
                        ((AutoCompleteTextView) v).showDropDown();
                    }
                });
            }
        }
        final AppCompatCheckBox checkbox = layout.findViewById(R.id.checkbox);
        if (checkTextId > 0) {
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setText(checkTextId);
        } else {
            checkbox.setVisibility(View.GONE);
        }
        builder.setView(layout);
        builder.setNeutralButton(R.string.cancel, null);
        if (posButtonText == null) {
            builder.setPositiveButton(R.string.okay, null);
        } else {
            builder.setPositiveButton(posButtonText, null);
        }

        if (negButtonText != null) {
            builder.setNegativeButton(negButtonText, null);
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positive = ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener(view -> {
                if (dismiss) {
                    d.dismiss();
                }
                posListener.processLine(input, checkbox.isChecked());
            });
            if (negButtonText != null) {
                Button negative = ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEGATIVE);
                negative.setOnClickListener(view -> {
                    if (dismiss) {
                        d.dismiss();
                    }
                    negListener.processLine(input, checkbox.isChecked());
                });
            }
        });
        return dialog;
    }

    public interface TextLineInterface {
        /**
         * Do something with the text entered by the user
         * 
         * @param input the EditTExt
         * @param check true if the optional checkbox is checked
         */
        void processLine(@Nullable final EditText input, boolean check);
    }
}
