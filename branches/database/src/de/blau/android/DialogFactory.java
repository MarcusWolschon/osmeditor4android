package de.blau.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.DownloadCurrentListener;
import de.blau.android.listener.GotoErrorMailerListener;
import de.blau.android.listener.GotoPreferencesListener;
import de.blau.android.listener.UploadListener;
import de.blau.android.listener.ConfirmUploadListener;

/**
 * Capsulates Dialog-Creation from {@link Main} and delegates the creation-command to {@link android.app.Dialog.Builder}.
 * 
 * @author mb
 */
public class DialogFactory {

	public static final int NO_LOGIN_DATA = 1;

	public static final int WRONG_LOGIN = 2;

	public static final int NO_CONNECTION = 3;

	public static final int DOWNLOAD_CURRENT_WITH_CHANGES = 4;

	public static final int PROGRESS_LOADING = 5;

	public static final int PROGRESS_DOWNLOAD = 6;

	public static final int UNDEFINED_ERROR = 7;

    public static final int CONFIRM_UPLOAD = 8;

	private final Main caller;

	private final Builder noLoginDataSet;

	private final Builder wrongLogin;

	private final Builder noConnection;

	private final Builder downloadCurrentWithChanges;

	private final Builder undefinedError;

    private final Builder confirmUpload;

	/**
	 * @param caller
	 */
	public DialogFactory(final Main caller) {
		this.caller = caller;

		GotoPreferencesListener gotoPreferencesListener = new GotoPreferencesListener(caller);
		DoNothingListener doNothingListener = new DoNothingListener();

		noLoginDataSet = createBasicDialog(R.string.no_login_data_title, R.string.no_login_data_message);
		noLoginDataSet.setPositiveButton(R.string.okay, gotoPreferencesListener);

		wrongLogin = createBasicDialog(R.string.wrong_login_data_title, R.string.wrong_login_data_message);
		wrongLogin.setPositiveButton(R.string.okay, gotoPreferencesListener);

		noConnection = createBasicDialog(R.string.no_connection_title, R.string.no_connection_message);
		noConnection.setPositiveButton(R.string.okay, doNothingListener);

		downloadCurrentWithChanges = createBasicDialog(R.string.transfer_download_current_dialog_title,
			R.string.transfer_download_current_dialog_message);
		downloadCurrentWithChanges.setPositiveButton(R.string.transfer_download_current_upload,
		        new ConfirmUploadListener(caller));
		downloadCurrentWithChanges.setNeutralButton(R.string.transfer_download_current_back, doNothingListener);
		downloadCurrentWithChanges.setNegativeButton(R.string.transfer_download_current_download,
			new DownloadCurrentListener(caller));

		undefinedError = createBasicDialog(R.string.undefined_error_title, R.string.undefined_error_message);
		undefinedError.setPositiveButton(R.string.undefined_error_sendbutton, new GotoErrorMailerListener(caller));
		undefinedError.setNegativeButton(R.string.no, doNothingListener);


        confirmUpload = createBasicDialog(R.string.confirm_upload_title, R.string.confirm_upload_title); // body gets replaced later
        confirmUpload.setPositiveButton(R.string.transfer_download_current_upload, new UploadListener(caller));
        confirmUpload.setNegativeButton(R.string.no, doNothingListener);
	}

	/**
	 * @param aCaller 
	 * @param id
	 * @return
	 */
	public Dialog create(final Main aCaller, final int id) {
		switch (id) {

		case NO_LOGIN_DATA:
			return noLoginDataSet.create();

		case WRONG_LOGIN:
			return wrongLogin.create();

		case NO_CONNECTION:
			return noConnection.create();

		case DOWNLOAD_CURRENT_WITH_CHANGES:
			return downloadCurrentWithChanges.create();

		case UNDEFINED_ERROR:
			return undefinedError.create();

		case PROGRESS_LOADING:
			return createBasicProgressDialog(R.string.progress_message);

		case PROGRESS_DOWNLOAD:
			return createBasicProgressDialog(R.string.progress_download_message);

		case CONFIRM_UPLOAD:
		    confirmUpload.setMessage(aCaller.getString(R.string.confirm_upload_text, aCaller.getPendingChanges()));
            return confirmUpload.create();
		}

		return null;
	}

	/**
	 * @param titleId the resource-id of the title 
	 * @param messageId the resource-id of the message
	 * @return a dialog-builder
	 */
	private Builder createBasicDialog(final int titleId, final int messageId) {
		Builder dialog = new AlertDialog.Builder(caller);
		dialog.setIcon(R.drawable.alert_dialog_icon);
		dialog.setTitle(titleId);
		dialog.setMessage(messageId);
		return dialog;
	}

	private ProgressDialog createBasicProgressDialog(final int messageId) {
		ProgressDialog progress = new ProgressDialog(caller);
		progress.setTitle(R.string.progress_title);
		progress.setIndeterminate(true);
		progress.setCancelable(true);
		progress.setMessage(caller.getResources().getString(messageId));
		return progress;
	}


}
