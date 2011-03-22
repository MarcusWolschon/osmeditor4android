package de.blau.android.thread;

import java.io.IOException;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.R;
import de.blau.android.osm.StorageDelegator;

public class SaveToFileThread extends LogicThread {
	private final StorageDelegator delegator;

	private final boolean showDone;

	final Runnable done = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(caller.getApplicationContext(), R.string.toast_save_done, Toast.LENGTH_SHORT).show();
		}
	};

	public SaveToFileThread(final Activity caller, final Handler handler, final StorageDelegator delegator,
			final boolean showDone) {
		super(caller, handler);
		this.delegator = delegator;
		this.showDone = showDone;
	}

	@Override
	public void run() {
		try {
			handler.post(setProgressBarVisible(true));
			delegator.writeToFile(caller.getApplicationContext());
			if (showDone) {
				handler.post(done);
			}
		} catch (IOException e) {
			Log.e("Vespucci", "Problem saving", e);
		} finally {
			handler.post(setProgressBarVisible(false));
		}
	}

}
