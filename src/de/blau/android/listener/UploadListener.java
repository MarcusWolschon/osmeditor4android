package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import de.blau.android.Main;
import de.blau.android.dialogs.ConfirmUpload;

/**
 * @author mb
 */
public class UploadListener implements OnClickListener {
	
	private final Main caller;
	private final EditText commentField;
	private final EditText sourceField;
	private final CheckBox closeChangeset;
	
	/**
	 * @param caller
	 * @param closeChangeset TODO
	 */
	public UploadListener(final Main caller, final EditText commentField, final EditText sourceField, final CheckBox closeChangeset) {
		this.caller = caller;
		this.commentField = commentField;
		this.sourceField = sourceField;
		this.closeChangeset = closeChangeset;
	}
	
	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		ConfirmUpload.dismissDialog(caller);
		caller.performUpload(commentField.getText().toString(), sourceField.getText().toString(), closeChangeset.isChecked());
	}
}
