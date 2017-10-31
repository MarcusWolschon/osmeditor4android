package de.blau.android.listener;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.List;

import de.blau.android.Main;
import de.blau.android.dialogs.ConfirmUpload;
import de.blau.android.util.mapbox.utils.TextUtils;
import de.blau.android.validation.FormValidation;

/**
 * @author mb
 */
public class UploadListener implements DialogInterface.OnShowListener, View.OnClickListener {

	private final Main caller;
	private final EditText commentField;
	private final EditText sourceField;
	private final CheckBox closeChangeset;
	@NonNull
	private final List<FormValidation> validations;

	/**
	 * @param caller
	 * @param closeChangeset TODO
	 */
	public UploadListener(final Main caller,
						  final EditText commentField,
						  final EditText sourceField,
						  final CheckBox closeChangeset,
						  @NonNull final List<FormValidation> validations) {
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
		if (canBeUploaded()) {
			ConfirmUpload.dismissDialog(caller);
			caller.performUpload(
					commentField.getText().toString(),
					sourceField.getText().toString(),
					closeChangeset.isChecked());
		}
	}

	private void validateFields() {
		for (FormValidation validation : validations) {
			validation.validate();
		}
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean canBeUploaded() {
		if (!TextUtils.isEmpty(commentField.getError())) {
			return false;
		}
		if (!TextUtils.isEmpty(sourceField.getError())) {
			return false;
		}
		return true;
	}
}
