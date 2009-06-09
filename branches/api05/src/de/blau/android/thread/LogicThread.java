package de.blau.android.thread;

import android.app.Activity;
import android.os.Handler;

public abstract class LogicThread extends Thread implements Runnable {

	protected final Activity caller;

	protected final Handler handler;

	/**
	 * @param caller
	 * @param handler
	 */
	public LogicThread(final Activity caller, final Handler handler) {
		this.caller = caller;
		this.handler = handler;
	}

	@Override
	public abstract void run();

	protected Runnable setProgressBarVisible(final boolean visible) {
		return new Runnable() {
			public void run() {
				caller.setProgressBarIndeterminateVisibility(visible);
			}
		};
	}

	protected Runnable getDialog(final int dialogId) {
		return new Runnable() {
			public void run() {
				caller.showDialog(dialogId);
			}
		};
	}
}
