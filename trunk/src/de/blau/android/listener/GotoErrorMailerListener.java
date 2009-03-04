package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import de.blau.android.Main;
import de.blau.android.util.ErrorMailer;

/**
 * @author mb
 */
public class GotoErrorMailerListener implements OnClickListener {

	private final Main caller;

	/**
	 * @param caller
	 */
	public GotoErrorMailerListener(final Main caller) {
		this.caller = caller;
	}

	public void onClick(final DialogInterface dialog, final int which) {
		caller.startActivity(ErrorMailer.send(caller.getExceptions(), caller.getResources()));
	}
}
