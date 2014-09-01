package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.EditText;
import de.blau.android.Main;

/**
 * @author mb
 */
public class UploadListener implements OnClickListener {
	
	private final Main caller;
	private final EditText commentField;
	private final EditText sourceField;
	
	/**
	 * @param caller
	 */
	public UploadListener(final Main caller, final EditText commentField, final EditText sourceField) {
		this.caller = caller;
		this.commentField = commentField;
		this.sourceField = sourceField;
	}
	
	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		caller.performUpload(commentField.getText().toString(), sourceField.getText().toString());
	}
	
}
