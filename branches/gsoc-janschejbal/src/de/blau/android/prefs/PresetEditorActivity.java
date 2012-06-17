package de.blau.android.prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.PresetInfo;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetIconManager;
import de.blau.android.services.util.StreamUtils;

public class PresetEditorActivity extends URLListEditActivity {

	private AdvancedPrefDatabase db;
	// TODO context menu option to re-download preset
	
	public PresetEditorActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		db = new AdvancedPrefDatabase(this);
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onLoadList(List<ListEditItem> items) {
		PresetInfo[] presets = db.getPresets();
		for (PresetInfo preset : presets) {
			items.add(new ListEditItem(preset.id, preset.name, preset.url));
		}
	}

	@Override
	protected void onItemClicked(ListEditItem item) {
		db.setCurrentAPIPreset(item.id);
		finish();
	}

	@Override
	protected void onItemCreated(ListEditItem item) {
		db.addPreset(item.id, item.name, item.value);
		downloadPresetData(item.id, item.value);
		db.setCurrentAPIPreset(item.id);
		Main.resetPreset();
	}

	@Override
	protected void onItemEdited(ListEditItem item) {
		db.setPresetInfo(item.id, item.name, item.value);
		db.removePresetDirectory(item.id);
		downloadPresetData(item.id, item.value);
		Main.resetPreset();
	}

	@Override
	protected void onItemDeleted(ListEditItem item) {
		db.deletePreset(item.id);
		Main.resetPreset();
	}
	
	private void downloadPresetData(final String id, final String url) {
		final File presetDir = db.getPresetDirectory(id);
		presetDir.mkdir();
		if (!presetDir.isDirectory()) throw new RuntimeException("Could not create preset directory " + presetDir.getAbsolutePath());

		new AsyncTask<Void, Integer, Integer>() {
			private ProgressDialog progress;
			private boolean canceled = false;
			
			private final int RESULT_TOTAL_FAILURE = 0; 
			private final int RESULT_TOTAL_SUCCESS = 1; 
			private final int RESULT_IMAGE_FAILURE = 2; 
			private final int RESULT_PRESET_NOT_PARSABLE = 3; 
			private final int RESULT_DOWNLOAD_CANCELED = 4; 
			
			@Override
			protected void onPreExecute() {
				progress = new ProgressDialog(PresetEditorActivity.this);
				progress.setTitle(R.string.progress_title);
				progress.setIndeterminate(true);
				progress.setCancelable(true);
				progress.setMessage(PresetEditorActivity.this.getResources().getString(R.string.progress_download_message));
				progress.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						canceled = true;
					}
				});
				progress.show();
			}
			
			@Override
			protected Integer doInBackground(Void... arg0) {
				if (!download(url, Preset.PRESETXML)) {
					return RESULT_TOTAL_FAILURE;
				}
				
				ArrayList<String> urls = Preset.parseForURLs(presetDir);
				if (urls == null) {
					return RESULT_PRESET_NOT_PARSABLE;
				}
				
				boolean allImagesSuccessful = true;
				int count = 0;
				for (String url : urls) {
					if (canceled) return RESULT_DOWNLOAD_CANCELED;
					count++;
					allImagesSuccessful &= download(url, null);
					this.publishProgress(count, url.length());
				}
				return allImagesSuccessful? RESULT_TOTAL_SUCCESS : RESULT_IMAGE_FAILURE;
			}
			
			/**
			 * Updates the progress bar
			 * @param values two integers, first is the current progress, second is the max progress
			 */
			@Override
			protected void onProgressUpdate(Integer... values) {
				progress.setIndeterminate(false);
				progress.setMax(values[1]);
				progress.setProgress(values[0]);
			};
			
			/**
			 * 
			 * @param url
			 * @param filename A filename where to save the file.
			 *    If null, the URL will be hashed using the PresetIconManager hash function
			 *    and the file will be saved to hashvalue.png
			 *    (where "hashvalue" will be replaced with the URL hash).
			 * @return true if file was successfully downloaded, false otherwise
			 */
			private boolean download(String url, String filename) {
				if (filename == null) {
					filename = PresetIconManager.hash(url)+".png";
				}
			
				try {
					HttpURLConnection conn = (HttpURLConnection)((new URL(url)).openConnection());
					conn.setInstanceFollowRedirects(true);
					if (conn.getResponseCode() != 200) return false;
					InputStream downloadStream = conn.getInputStream();
					OutputStream fileStream = new FileOutputStream(new File(presetDir, filename));
					StreamUtils.copy(downloadStream, fileStream);
					downloadStream.close();
					fileStream.close();
					return true;
				} catch (Exception e) {
					Log.w("PresetDownloader", "Could not download file " + url, e);
					return false;
				}				
			}

			@Override
			protected void onPostExecute(Integer result) {
				progress.dismiss();
				switch (result) {
				case RESULT_TOTAL_SUCCESS:
					Toast.makeText(PresetEditorActivity.this, R.string.preset_download_successful, Toast.LENGTH_LONG).show();
					break;
				case RESULT_TOTAL_FAILURE:
					msgbox(R.string.preset_download_failed);
					break;
				case RESULT_PRESET_NOT_PARSABLE:
					msgbox(R.string.preset_download_parse_failed);
					db.removePresetDirectory(id);
					break;
				case RESULT_IMAGE_FAILURE:
					msgbox(R.string.preset_download_missing_images);
					break;
				case RESULT_DOWNLOAD_CANCELED:
					break; // do nothing
				default:
					break;
				}
			}
			
		}.execute();
	}
	
	private void msgbox(int msgResID) {
		AlertDialog.Builder box = new AlertDialog.Builder(this);
		box.setMessage(getResources().getString(msgResID));
		box.show();
	}
	
	

}
