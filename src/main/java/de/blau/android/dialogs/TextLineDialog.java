package de.blau.android.dialogs;

import java.util.List;

import com.adobe.internal.xmp.impl.Utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
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
        return get(ctx, titleId, hintId, (List<String>) null, null, listener, dismiss);
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
        return get(ctx, titleId, hintId, text, null, listener, true);
    }

    /**
     * Create a Dialog with one EditText
     * 
     * @param ctx the Android Context
     * @param titleId the string resource for the title
     * @param hintId string resource for an hint of -1 if none
     * @param text initial text to display
     * @param buttonText positive button text or null
     * @param listener a TextLineInterface listener provided by the caller
     * @param dismiss dismiss dialog when the positive button is clicked when true
     * @return an AlertDialog instance
     */
    public static AlertDialog get(@NonNull Context ctx, int titleId, int hintId, @Nullable String text, @Nullable String buttonText,
            @NonNull TextLineInterface listener, boolean dismiss) {
        return get(ctx, titleId, hintId, Util.wrapInList(text), buttonText, listener, dismiss);
    }

    /**
     * Create a Dialog with one EditText
     * 
     * @param ctx the Android Context
     * @param titleId the string resource for the title
     * @param hintId string resource for an hint of -1 if none
     * @param prevText a List of previous input to display
     * @param buttonText positive button text or null
     * @param listener a TextLineInterface listener provided by the caller
     * @param dismiss dismiss dialog when the positive button is clicked when true
     * @return an AlertDialog instance
     */
    public static AlertDialog get(@NonNull Context ctx, int titleId, int hintId, @Nullable List<String> prevText, @Nullable String buttonText,
            @NonNull TextLineInterface listener, boolean dismiss) {

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);

        Builder builder = new AlertDialog.Builder(ctx);
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
                OnClickListener autocompleteOnClick = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (v.hasFocus()) {
                            ((AutoCompleteTextView) v).showDropDown();
                        }
                    }
                };
                input.setOnClickListener(autocompleteOnClick);
            }
        }
        builder.setView(layout);
        builder.setNeutralButton(R.string.cancel, null);
        if (buttonText == null) {
            builder.setPositiveButton(R.string.okay, null);
        } else {
            builder.setPositiveButton(buttonText, null);
        }

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (dismiss) {
                            dialog.dismiss();
                        }
                        listener.processLine(input);
                    }
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
         */
        void processLine(@Nullable final EditText input);
    }
}
