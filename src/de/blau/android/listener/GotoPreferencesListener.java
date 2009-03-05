package de.blau.android.listener;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import de.blau.android.PrefEditor;

/**
 * @author mb
 */
public class GotoPreferencesListener implements OnClickListener {

	private final Activity caller;

	/**
	 * @param caller
	 */
	public GotoPreferencesListener(final Activity caller) {
		this.caller = caller;
	}

	public void onClick(final DialogInterface dialog, final int which) {
		caller.startActivity(new Intent(caller, PrefEditor.class));
	}
}
