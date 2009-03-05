package de.blau.android.thread;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;

import org.apache.http.HttpStatus;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.DialogFactory;
import de.blau.android.R;
import de.blau.android.exception.OsmServerException;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;

/**
 * @author mb
 */
public class UploadThread extends LogicThread {

	private static final String DEBUG_TAG = UploadThread.class.getSimpleName();

	private final Server server;

	private final Runnable uploadSuccess = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(caller.getApplicationContext(), R.string.toast_upload_success, Toast.LENGTH_SHORT).show();
			caller.getCurrentFocus().invalidate();
		}
	};

	private final StorageDelegator delegator;

	/**
	 * @param caller
	 * @param handler Handler that have to be created in the Activity's instance.
	 * @param server
	 * @param delegator holds the data for upload.
	 */
	public UploadThread(final Activity caller, final Handler handler, final Server server,
			final StorageDelegator delegator) {
		super(caller, handler);
		this.server = server;
		this.delegator = delegator;
	}

	@Override
	synchronized public void run() {
		try {
			handler.post(setProgressBarVisible(true));
			delegator.uploadToServer(server);
			handler.post(uploadSuccess);
		} catch (final MalformedURLException e) {
			handler.post(getDialog(DialogFactory.UNDEFINED_ERROR));
			Log.e(DEBUG_TAG, e.getStackTrace().toString());
			//exceptions.add(e);
		} catch (final ProtocolException e) {
			handler.post(getDialog(DialogFactory.UNDEFINED_ERROR));
			Log.e(DEBUG_TAG, e.getStackTrace().toString());
			//exceptions.add(e);
		} catch (final OsmServerException e) {
			handleOsmServerException(e);
			Log.e(DEBUG_TAG, e.getStackTrace().toString());
			//exceptions.add(e);
		} catch (final IOException e) {
			handler.post(getDialog(DialogFactory.NO_CONNECTION));
			Log.e(DEBUG_TAG, e.getStackTrace().toString());
			//exceptions.add(e);
		} catch (final NullPointerException e) {
			handler.post(getDialog(DialogFactory.UNDEFINED_ERROR));
			Log.e(DEBUG_TAG, e.getStackTrace().toString());
			//exceptions.add(e);
		} finally {
			handler.post(setProgressBarVisible(false));
		}
	}

	/**
	 * @param e
	 */
	private void handleOsmServerException(final OsmServerException e) {
		switch (e.getErrorCode()) {
		case HttpStatus.SC_UNAUTHORIZED:
			handler.post(getDialog(DialogFactory.WRONG_LOGIN));
			break;

		//TODO: implement other state handling
		default:
			handler.post(getDialog(DialogFactory.UNDEFINED_ERROR));
		}
	}

}
