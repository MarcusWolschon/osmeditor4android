package de.blau.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import de.blau.android.listener.ConfirmUploadListener;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.DownloadCurrentListener;
import de.blau.android.listener.GotoPreferencesListener;
import de.blau.android.listener.UploadListener;
import de.blau.android.osb.CommitListener;

/**
 * Encapsulates Dialog-Creation from {@link Main} and delegates the creation-command to {@link android.app.Dialog.Builder}.
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
	
	public static final int UPLOAD_PROBLEM = 7;
	
	public static final int CONFIRM_UPLOAD = 8;
	
	public static final int OPENSTREETBUG_EDIT = 9;
	
	public static final int DATA_CONFLICT = 10;
	
	private final Main caller;
	
	private final Builder noLoginDataSet;
	
	private final Builder wrongLogin;
	
	private final Builder noConnection;
	
	private final Builder downloadCurrentWithChanges;
	
	private final Builder uploadProblem;
	
	private final Builder confirmUpload;
	
	private final Builder openStreetBugEdit;
	
	private final Builder dataConflict;
	
	/**
	 * @param caller
	 */
	public DialogFactory(final Main caller) {
		this.caller = caller;
		
		// Create some useful objects
		final Context context = caller.getApplicationContext();
		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
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
		
		uploadProblem = createBasicDialog(R.string.upload_problem_title, R.string.upload_problem_message);
		uploadProblem.setPositiveButton(R.string.okay, doNothingListener);
		
		confirmUpload = createBasicDialog(R.string.confirm_upload_title, 0); // body gets replaced later
		View layout = inflater.inflate(R.layout.upload_comment, null);
		confirmUpload.setView(layout);
		confirmUpload.setPositiveButton(R.string.transfer_download_current_upload, new UploadListener(caller, (EditText)layout.findViewById(R.id.upload_comment), (EditText)layout.findViewById(R.id.upload_source)));
		confirmUpload.setNegativeButton(R.string.no, doNothingListener);
		
		openStreetBugEdit = createBasicDialog(R.string.openstreetbug_edit_title, 0); // body gets replaced later
		layout = inflater.inflate(R.layout.openstreetbug_edit, null);
		openStreetBugEdit.setView(layout);
		openStreetBugEdit.setPositiveButton(R.string.openstreetbug_commitbutton, new CommitListener(caller, (EditText)layout.findViewById(R.id.openstreetbug_comment), (CheckBox)layout.findViewById(R.id.openstreetbug_close)));
	
		dataConflict = createBasicDialog(R.string.data_conflict_title, R.string.data_conflict_message);
		dataConflict.setPositiveButton(R.string.okay, doNothingListener);
	}
	
	/**
	 * @param id
	 * @return
	 */
	public Dialog create(final int id) {
		switch (id) {
		
		case NO_LOGIN_DATA:
			return noLoginDataSet.create();
			
		case WRONG_LOGIN:
			return wrongLogin.create();
			
		case NO_CONNECTION:
			return noConnection.create();
			
		case DOWNLOAD_CURRENT_WITH_CHANGES:
			return downloadCurrentWithChanges.create();
			
		case PROGRESS_LOADING:
			return createBasicProgressDialog(R.string.progress_message);
			
		case PROGRESS_DOWNLOAD:
			return createBasicProgressDialog(R.string.progress_download_message);
			
		case UPLOAD_PROBLEM:
			return uploadProblem.create();
			
		case CONFIRM_UPLOAD:
			return confirmUpload.create();
			
		case OPENSTREETBUG_EDIT:
			return openStreetBugEdit.create();
		
		case DATA_CONFLICT:
			return dataConflict.create();
		}
		
		return null;
	}
	
	/**
	 * Creates a dialog warning the user that he has unsaved changes that will be discarded.
	 * @param context Activity creating the dialog and starting the intent Activity if confirmed
	 * @param intent Intent representing the Activity to start on confirmation
	 * @param requestCode If the activity should return a result, a non-negative request code.
	 *                    If no result is expected, set to -1.
	 * @return the created dialog
	 */
	public static Dialog createDataLossActivityDialog(final Activity context, final Intent intent,
			final int requestCode) {
		Builder dialog = new AlertDialog.Builder(context);
		dialog.setIcon(R.drawable.alert_dialog_icon);
		dialog.setTitle(R.string.unsaved_data_title);
		dialog.setMessage(R.string.unsaved_data_message);
		dialog.setPositiveButton(R.string.unsaved_data_proceed,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						context.startActivityForResult(intent, requestCode);
					}
				}
			);
		dialog.setNegativeButton(R.string.cancel, null);
		return dialog.create();
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
		if (messageId != 0) {
			dialog.setMessage(messageId);
		}
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
