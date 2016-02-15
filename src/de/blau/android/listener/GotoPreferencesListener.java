package de.blau.android.listener;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import de.blau.android.prefs.PrefEditor;

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

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		PrefEditor.start(caller);
	}
}
