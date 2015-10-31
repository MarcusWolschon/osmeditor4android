package de.blau.android;

import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import de.blau.android.Logic.UploadResult;
import de.blau.android.contract.Paths;
import de.blau.android.listener.ConfirmUploadListener;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.listener.DownloadCurrentListener;
import de.blau.android.listener.GpxUploadListener;
import de.blau.android.listener.UploadListener;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.Search;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Search.SearchResult;

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
	
	public static final int DATA_CONFLICT = 10;
	
	public static final int OUT_OF_MEMORY = 11;

	public static final int OUT_OF_MEMORY_DIRTY = 12;
	
	public static final int PROGRESS_DELETING = 13;
	
	public static final int BACKGROUND_PROPERTIES = 14;
	
	public static final int INVALID_DATA_RECEIVED = 15;
	
	public static final int PROGRESS_SEARCHING = 16;
	
	public static final int PROGRESS_SAVING = 17;
	
	public static final int SEARCH = 18;

	public static final int SAVE_FILE = 19;

	public static final int FILE_WRITE_FAILED = 20;

	public static final int NEWBIE = 21;
	
	public static final int GPX_UPLOAD = 22;
	
	public static final int UPLOAD_CONFLICT = 23;

	public static final int API_OFFLINE = 24;
	
	public static final int NEW_VERSION = 25;
	
	public static final int PROGRESS_OAUTH = 26;
		
	private final Main caller;
	
	private final Builder noLoginDataSet;
	
	private final Builder wrongLogin;
	
	private final Builder noConnection;
	
	private final Builder downloadCurrentWithChanges;
	
	private final Builder uploadProblem;
	
	private final Builder confirmUpload;
	
	private final Builder dataConflict;
	
	private final Builder apiOffline;
	
	private final Builder outOfMemory;
	
	private final Builder outOfMemoryDirty;
	
	private final Builder backgroundProperties;
	
	private final Builder invalidDataReceived;
	
	private final Builder fileWriteFailed;
	
	private final Builder newbie;
	
	private final Builder gpxUpload;
	
	private final Builder newVersion;
			
	/**
	 * @param caller
	 */
	public DialogFactory(final Main caller) {
		this.caller = caller;
		
		// inflater needs to be got from a themed view or else all our custom stuff will not style correctly
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(caller);
		
		DoNothingListener doNothingListener = new DoNothingListener();
		
		noLoginDataSet = createBasicDialog(caller, R.string.no_login_data_title, R.string.no_login_data_message);
		noLoginDataSet.setPositiveButton(R.string.okay, doNothingListener); // logins in the preferences should no longer be used
		
		wrongLogin = createBasicDialog(caller, R.string.wrong_login_data_title, R.string.wrong_login_data_message);
		wrongLogin.setNegativeButton(R.string.cancel, doNothingListener); // logins in the preferences should no longer be used
		final Server server = new Preferences(caller).getServer();
		if (server.getOAuth()) {
			wrongLogin.setPositiveButton(R.string.wrong_login_data_re_authenticate, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					caller.oAuthHandshake(server, null);
				}
			});
		}
		
		noConnection = createBasicDialog(caller, R.string.no_connection_title, R.string.no_connection_message);
		noConnection.setPositiveButton(R.string.okay, doNothingListener);
		
		downloadCurrentWithChanges = createBasicDialog(caller,
			R.string.transfer_download_current_dialog_title, R.string.transfer_download_current_dialog_message);
		downloadCurrentWithChanges.setPositiveButton(R.string.transfer_download_current_upload,
			new ConfirmUploadListener(caller));
		downloadCurrentWithChanges.setNeutralButton(R.string.transfer_download_current_back, doNothingListener);
		downloadCurrentWithChanges.setNegativeButton(R.string.transfer_download_current_download,
			new DownloadCurrentListener(caller));
		
		uploadProblem = createBasicDialog(caller, R.string.upload_problem_title, R.string.upload_problem_message);
		uploadProblem.setPositiveButton(R.string.okay, doNothingListener);
		
		confirmUpload = createBasicDialog(caller, R.string.confirm_upload_title, 0); // body gets replaced later
		View layout = inflater.inflate(R.layout.upload_comment, null);
		confirmUpload.setView(layout);
		CheckBox closeChangeset = (CheckBox)layout.findViewById(R.id.upload_close_changeset);
		closeChangeset.setChecked(new Preferences(caller).closeChangesetOnSave());
		confirmUpload.setPositiveButton(R.string.transfer_download_current_upload, new UploadListener(caller, (EditText)layout.findViewById(R.id.upload_comment), 
					(EditText)layout.findViewById(R.id.upload_source), closeChangeset));
		confirmUpload.setNegativeButton(R.string.no, doNothingListener);
		
		dataConflict = createBasicDialog(caller, R.string.data_conflict_title, R.string.data_conflict_message);
		dataConflict.setPositiveButton(R.string.okay, doNothingListener);
		
		apiOffline = createBasicDialog(caller, R.string.api_offline_title, R.string.api_offline_message);
		apiOffline.setPositiveButton(R.string.okay, doNothingListener);
		
		// displaying these dialogs might make things worse
		outOfMemory = createBasicDialog(caller, R.string.out_of_memory_title, R.string.out_of_memory_message);
		outOfMemory.setPositiveButton(R.string.okay, doNothingListener);
		
		outOfMemoryDirty = createBasicDialog(caller, R.string.out_of_memory_title, R.string.out_of_memory_dirty_message);
		outOfMemoryDirty.setPositiveButton(R.string.okay, doNothingListener);
		
		backgroundProperties = createBackgroundPropertiesDialog();
		layout = inflater.inflate(R.layout.background_properties, null);
		backgroundProperties.setView(layout);
		backgroundProperties.setPositiveButton(R.string.okay, doNothingListener);
		SeekBar seeker = (SeekBar) layout.findViewById(R.id.background_contrast_seeker);
		seeker.setOnSeekBarChangeListener(createSeekBarListener());
				
		invalidDataReceived = createBasicDialog(caller, R.string.invalid_data_received_title, R.string.invalid_data_received_message);
		invalidDataReceived.setPositiveButton(R.string.okay, doNothingListener);
		
		fileWriteFailed = createBasicDialog(caller, R.string.file_write_failed_title, R.string.file_write_failed_message);
		fileWriteFailed.setPositiveButton(R.string.okay, doNothingListener);
		
		newbie = createBasicDialog(caller, R.string.welcome_title, R.string.welcome_message);
		newbie.setPositiveButton(R.string.okay, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						caller.gotoBoxPicker();
					}
				});
		newbie.setNeutralButton(R.string.read_introduction, 	new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						HelpViewer.start(caller.getApplicationContext(), R.string.help_introduction);
					}
				});
		
		gpxUpload = createBasicDialog(caller, R.string.confirm_upload_title, 0); // body gets replaced later
		layout = inflater.inflate(R.layout.upload_gpx, null);
		gpxUpload.setView(layout);
		gpxUpload.setPositiveButton(R.string.transfer_download_current_upload, new GpxUploadListener(caller, (EditText)layout.findViewById(R.id.upload_gpx_description), 
				(EditText)layout.findViewById(R.id.upload_gpx_tags), (Spinner)layout.findViewById(R.id.upload_gpx_visibility)));
		gpxUpload.setNegativeButton(R.string.cancel, doNothingListener);
		
		newVersion = createBasicDialog(caller, R.string.upgrade_title, R.string.upgrade_message);
		newVersion.setNegativeButton(R.string.cancel, doNothingListener);
		newVersion.setNeutralButton(R.string.read_upgrade, 	new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						HelpViewer.start(caller.getApplicationContext(), R.string.help_upgrade);
					}
				});
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
		
		case DATA_CONFLICT:
			return dataConflict.create();
			
		case API_OFFLINE:
			return apiOffline.create();
		
		case OUT_OF_MEMORY:
			return outOfMemory.create();
			
		case OUT_OF_MEMORY_DIRTY:
			return outOfMemoryDirty.create();
			
		case PROGRESS_DELETING:
			return createBasicProgressDialog(R.string.progress_general_title, R.string.progress_deleting_message);
			
		case PROGRESS_SEARCHING:
			return createBasicProgressDialog(R.string.progress_general_title, R.string.progress_searching_message);
		
		case PROGRESS_SAVING:
			return createBasicProgressDialog(R.string.progress_general_title, R.string.progress_saving_message);
			
		case PROGRESS_OAUTH:
			return createBasicProgressDialog(R.string.progress_general_title, R.string.progress_oauth);
			
		case BACKGROUND_PROPERTIES:
			return backgroundProperties.create();
			
		case INVALID_DATA_RECEIVED:
			return invalidDataReceived.create();
			
		case SEARCH:
			return createSearchDialog(caller);
			
		case SAVE_FILE:
			return createSaveFileDialog(caller);
			
		case FILE_WRITE_FAILED:
			return fileWriteFailed.create();
			
		case NEWBIE:
			return newbie.create();
			
		case GPX_UPLOAD:
			return gpxUpload.create();
			
		case NEW_VERSION:
			return newVersion.create();
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
		dialog.setIcon(ThemeUtils.getResIdFromAttribute(context,R.attr.alert_dialog));
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
	 * @param ctx TODO
	 * @param titleId the resource-id of the title
	 * @param messageId the resource-id of the message
	 * @return a dialog-builder
	 */
	private Builder createBasicDialog(Context ctx, final int titleId, final int messageId) {
		Builder dialog = new AlertDialog.Builder(caller);
		dialog.setIcon(ThemeUtils.getResIdFromAttribute(caller,R.attr.alert_dialog));
		dialog.setTitle(titleId);
		if (messageId != 0) {
			dialog.setMessage(messageId);
		}
		return dialog;
	}
	
	/**
	 * @param titleId the resource-id of the title
	 * @param messageId the resource-id of the message
	 * @return a dialog-builder
	 */
	private Builder createBackgroundPropertiesDialog() {
		Builder dialog = new AlertDialog.Builder(caller);
		dialog.setTitle(R.string.menu_tools_background_properties);
		return dialog;
	}
	
	private OnSeekBarChangeListener createSeekBarListener() {
		return new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, int progress, final boolean fromTouch) {
				Map map = Application.mainActivity.getMap();
				map.getOpenStreetMapTilesOverlay().setContrast(progress/127.5f - 1f); // range from -1 to +1
				map.invalidate();
			}
			
			@Override
			public void onStartTrackingTouch(final SeekBar seekBar) {
			}
			
			@Override
			public void onStopTrackingTouch(final SeekBar arg0) {
			}
		};
	}
	
	private ProgressDialog createBasicProgressDialog(final int messageId) {
		return createBasicProgressDialog(R.string.progress_title, messageId);
	}
	
	private ProgressDialog createBasicProgressDialog(final int titleId, final int messageId) {
		ProgressDialog progress = new ProgressDialog(caller);
		progress.setTitle(titleId);
		progress.setIndeterminate(true);
		progress.setCancelable(true);
		progress.setMessage(caller.getResources().getString(messageId));
		return progress;
	}
	
	private Dialog createSearchDialog(final Main caller) {
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(caller);
		Builder searchBuilder = createBasicDialog(caller, R.string.menu_find, R.string.find_message);
		LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.query_entry, null);
		searchBuilder.setView(searchLayout);
		final EditText searchEdit = (EditText) searchLayout.findViewById(R.id.location_search_edit);
		searchEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		searchBuilder.setNegativeButton(R.string.cancel, null);
		
		
		final Dialog searchDialog = searchBuilder.create();
		
		final de.blau.android.util.SearchItemFoundCallback searchItemFoundCallback = new de.blau.android.util.SearchItemFoundCallback() {
			@Override
			public void onItemFound(SearchResult sr) {
				// turn this off or else we get bounced back to our current GPS position
				caller.setFollowGPS(false);
				caller.getMap().setFollowGPS(false);
				//
				Main.getLogic().setZoom(19);
				caller.getMap().getViewBox().moveTo((int) (sr.getLon() * 1E7d), (int)(sr.getLat()* 1E7d));
				searchDialog.dismiss();
				caller.getMap().invalidate();
			}
		};
		
		searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_SEARCH
		        		|| (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
		            Search search = new Search(caller, searchItemFoundCallback);
		            search.find(v.getText().toString());
		            return true;
		        }
		        return false;
		    }
		});
		
		return searchDialog;
	}
	
	private Dialog createSaveFileDialog(final Main caller) {
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(caller);
		Builder saveFileBuilder = createBasicDialog(caller, R.string.save_file, 0);
		LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.save_file, null);
		saveFileBuilder.setView(searchLayout);
		final EditText saveFileEdit = (EditText) searchLayout.findViewById(R.id.save_file_edit);
		saveFileBuilder.setNegativeButton(R.string.cancel, null);
		saveFileBuilder.setPositiveButton(R.string.save, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// FIXME instead of hardcoding the directory, this should be the default and alternatives seclectable by the user
				Main.getLogic().writeOsmFile(Environment.getExternalStorageDirectory().getPath() + "/" + Paths.DIRECTORY_PATH_VESPUCCI + "/" + saveFileEdit.getText().toString());
			}
		});
		
		final Dialog saveFileDialog = saveFileBuilder.create();
		
		return saveFileDialog;
	}
	
	/**
	 * @param titleId the resource-id of the title
	 * @param messageId the resource-id of the message
	 * @return a dialog-builder
	 */
	public static Builder createExistingTrackDialog(final Main caller, final Uri uri) {
		Builder existingTrack = new AlertDialog.Builder(Application.mainActivity);
		existingTrack.setIcon(ThemeUtils.getResIdFromAttribute(caller,R.attr.alert_dialog));
		existingTrack.setTitle(R.string.existing_track_title);
		existingTrack.setMessage(R.string.existing_track_message);
		
		existingTrack.setPositiveButton(R.string.replace, 	new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				caller.getTracker().stopTracking(true);
				try {
					caller.getTracker().importGPXFile(uri);
				} catch (FileNotFoundException e) {
					try {
						Toast.makeText(caller,caller.getResources().getString(R.string.toast_file_not_found, uri.toString()), Toast.LENGTH_LONG).show();
					} catch (Exception ex) {
						// protect against translation errors
					}
				}
			}
		});
		existingTrack.setNeutralButton(R.string.keep, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				caller.getTracker().stopTracking(false);
				try {
					caller.getTracker().importGPXFile(uri);
				} catch (FileNotFoundException e) {
					try {
						Toast.makeText(caller,caller.getResources().getString(R.string.toast_file_not_found, uri.toString()), Toast.LENGTH_LONG).show();
					} catch (Exception ex) {
						// protect against translation errors
					}
				}
			}
		});
		existingTrack.setNegativeButton(R.string.cancel, null);
		return existingTrack;
	}
	
	/**
	 * @param titleId the resource-id of the title
	 * @param messageId the resource-id of the message
	 * @return a dialog-builder
	 */
	public static Builder createUploadConflictDialog(final Main caller, UploadResult result) {
		Builder uploadConflict = new AlertDialog.Builder(Application.mainActivity);
		uploadConflict.setIcon(ThemeUtils.getResIdFromAttribute(caller,R.attr.alert_dialog));
		uploadConflict.setTitle(R.string.upload_conflict_title);
		final OsmElement elementOnServer = Main.getLogic().downloadElement(result.elementType, result.osmId);
		final OsmElement elementLocal = Application.getDelegator().getOsmElement(result.elementType, result.osmId);
		final long newVersion;
		try {
			boolean useServerOnly = false;
			if (elementOnServer != null) {
				if (elementLocal.getState()==OsmElement.STATE_DELETED) {
					uploadConflict.setMessage(caller.getResources().getString(R.string.upload_conflict_message_referential, elementLocal.getDescription()));
					useServerOnly = true;
				} else {
					uploadConflict.setMessage(caller.getResources().getString(R.string.upload_conflict_message_version, elementLocal.getDescription(), elementLocal.getOsmVersion(), elementOnServer.getOsmVersion()));
				}
				newVersion = elementOnServer.getOsmVersion();
			} else {
				uploadConflict.setMessage(caller.getResources().getString(R.string.upload_conflict_message_deleted, elementLocal.getDescription(), elementLocal.getOsmVersion()));
				newVersion = elementLocal.getOsmVersion() + 1;
			}
			if (!useServerOnly) {
				uploadConflict.setPositiveButton(R.string.use_local_version, 	new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Main.getLogic().fixElementWithConflict(newVersion, elementLocal, elementOnServer);
						caller.confirmUpload();
					}
				});
			}
			uploadConflict.setNeutralButton(R.string.use_server_version,new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					StorageDelegator storageDelegator = Application.getDelegator();
					storageDelegator.removeFromUpload(elementLocal);
					if (elementOnServer != null) {
						Main.getLogic().updateElement(elementLocal.getName(), elementLocal.getOsmId());
					} else { // delete local element
						Main.getLogic().updateToDeleted(elementLocal);
					}
					if (!storageDelegator.getApiStorage().isEmpty()) {
						caller.confirmUpload();
					}
				}
			});
		} catch (Exception e) {
			// protect against translation problems
			e.printStackTrace();
		}
		uploadConflict.setNegativeButton(R.string.cancel, null);
		return uploadConflict;
	}
}
