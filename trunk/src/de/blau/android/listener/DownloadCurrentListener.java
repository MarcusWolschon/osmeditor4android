package de.blau.android.listener;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import de.blau.android.Main;

/**
 * @author mb
 */
public class DownloadCurrentListener implements OnClickListener {

	private final Main caller;

	/**
	 * @param caller
	 */
	public DownloadCurrentListener(final Main caller) {
		this.caller = caller;
	}

	public void onClick(final DialogInterface dialog, final int which) {
		caller.performCurrentViewHttpLoad();
	}
}
