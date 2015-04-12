package de.blau.android.osb;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import de.blau.android.Main;

/**
 * Handle the user clicking the Commit button when editing an OpenStreetBug.
 * @author Andrew Gregory
 *
 */
public class CommitListener implements OnClickListener {
	
	/** Main Vespucci activity. */
	private final Main caller;
	/** OpenStreetBug comment field. */
	private final EditText comment;
	/** OpenStreetBug close checkbox. */
	private final CheckBox close;
	
	public CommitListener(final Main caller, final EditText comment, final CheckBox close) {
		this.caller = caller;
		this.comment = comment;
		this.close = close;
	}
	
	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		caller.performOpenStreetBugCommit(comment.getText().toString(), close.isChecked());
	}
	
}
