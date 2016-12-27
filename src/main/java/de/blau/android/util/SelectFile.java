
package de.blau.android.util;

import java.io.IOException;
import java.util.ArrayList;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import de.blau.android.R;
import de.blau.android.dialogs.GetFileName;

public class SelectFile {
	
	private static final String DEBUG_TAG = SelectFile.class.getName();
	
	public static final int SAVE_FILE = 7113;
	public static final int READ_FILE = 9340;
	public static final int READ_FILE_OLD = 9341;
	
	private static SaveFile saveCallback;
	private final static Object saveCallbackLock = new Object();

	private static ReadFile readCallback;
	private final static Object readCallbackLock = new Object();

	
	public static void save(FragmentActivity activity, de.blau.android.util.SaveFile callback) {
		synchronized (saveCallbackLock) {
			saveCallback = callback;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			Intent i = new Intent(activity, ThemedFilePickerActivity.class);

			i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
			i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
			i.putExtra(FilePickerActivity.EXTRA_ALLOW_EXISTING_FILE, true);
			i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_NEW_FILE);

			try {
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, FileUtil.getPublicDirectory().getPath());
			} catch (IOException e) {
				// if for whatever reason the above doesn't work we use the standard directory
				Log.d(DEBUG_TAG, "falling back to standard dir instead");
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
			}

			activity.startActivityForResult(i, SAVE_FILE);
		} else {
			GetFileName.showDialog(activity, callback);
		}
	}
	
	public static void read(FragmentActivity activity, ReadFile readFile) {
		synchronized (readCallbackLock) {
			readCallback = readFile;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			Intent i = new Intent(activity, ThemedFilePickerActivity.class);

			i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
			i.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, true);
			i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

			try {
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, FileUtil.getPublicDirectory().getPath());
			} catch (IOException e) {
				// if for whatever reason the above doesn't work we use the standard directory
				Log.d(DEBUG_TAG, "falling back to standard dir instead");
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
			}

			activity.startActivityForResult(i, READ_FILE);
		} else {
		    Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
		    intent.setType("*/*"); 
		    intent.addCategory(Intent.CATEGORY_OPENABLE);
		    try {
		    	activity.startActivityForResult(intent,READ_FILE_OLD);
		    } catch (android.content.ActivityNotFoundException ex) {
		        // Potentially direct the user to the Market with a Dialog
		        Toast.makeText(activity, R.string.toast_missing_filemanager, 
		                Toast.LENGTH_SHORT).show();
		    }
		}
	}
	
	
	public static void handleResult(int code, Intent data) {
		// for now this doesn't do anything
		if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
            // For JellyBean and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ClipData clip = data.getClipData();

                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                        // Do something with the URI
                    }
                }
            // For Ice Cream Sandwich
            } else {
                ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);

                if (paths != null) {
                    for (String path: paths) {
                        Uri uri = Uri.parse(path);
                        // Do something with the URI
                    }
                }
            }

        } else {
            Uri uri = data.getData();
            if (code == SAVE_FILE) {
            	synchronized (saveCallbackLock) {
            		if (saveCallback != null) {
            			Log.d(DEBUG_TAG, "saving to " + uri);
            			saveCallback.save(uri);
            		}
            	}
            } else if (code == READ_FILE) {
            	synchronized (readCallbackLock) {
            		if (readCallback != null) {
            			Log.d(DEBUG_TAG, "reading " + uri);
            			readCallback.read(uri);
            		}
            	}
            } else if (code == READ_FILE_OLD) {
            	synchronized (readCallbackLock) {
            		if (readCallback != null) {
            			Log.d(DEBUG_TAG, "reading " + uri);
            			readCallback.read(uri);
            		}
            	}
            }
        }
	}
}
