package de.blau.android.listener;

import java.util.List;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import de.blau.android.Main;
import de.blau.android.dialogs.ConfirmUpload;
import de.blau.android.validation.FormValidation;

/**
 * @author mb
 */
public class UploadListener implements DialogInterface.OnShowListener, View.OnClickListener {

    private final Main                 caller;
    private final EditText             commentField;
    private final EditText             sourceField;
    private final CheckBox             closeChangeset;
    private final List<FormValidation> validations;

    private boolean tagsShown = false;

    /**
     * Upload the current changes
     * 
     * @param caller the instance of Main calling this
     * @param commentField an EditText containing the comment tag value
     * @param sourceField an EditText containing the source tag value
     * @param closeChangeset close the changeset after upload if true
     * @param validations a List of validations to perform on the form fields
     */
    public UploadListener(@NonNull final Main caller, @NonNull final EditText commentField, @NonNull final EditText sourceField,
            @NonNull final CheckBox closeChangeset, @NonNull final List<FormValidation> validations) {
        this.caller = caller;
        this.commentField = commentField;
        this.sourceField = sourceField;
        this.closeChangeset = closeChangeset;
        this.validations = validations;
    }

    @Override
    public void onShow(final DialogInterface dialog) {
        Button button = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        validateFields();
        if (tagsShown || ConfirmUpload.getPage(caller) == ConfirmUpload.TAGS_PAGE) {
            ConfirmUpload.dismissDialog(caller);
            caller.performUpload(commentField.getText().toString(), sourceField.getText().toString(), closeChangeset.isChecked());
        } else {
            ConfirmUpload.showPage(caller, ConfirmUpload.TAGS_PAGE);
            tagsShown = true;
        }
    }

    /**
     * Run all supplied validators
     */
    private void validateFields() {
        for (FormValidation validation : validations) {
            validation.validate();
        }
    }
}
