
package de.blau.android.util;

import java.util.ArrayList;

import com.nononsenseapps.filepicker.FilePickerActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

public class FilePicker {
	
	static final int FILE_CODE = 7113;

	public static void save(Activity context) {
	    // This always works
	    Intent i = new Intent(context, FilePickerActivity.class);
	    // This works if you defined the intent filter
	    // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

	    // Set these depending on your use case. These are the defaults.
	    i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
	    i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
	    i.putExtra(FilePickerActivity.EXTRA_ALLOW_EXISTING_FILE, true);
	    i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_NEW_FILE);

	    // Configure initial directory by specifying a String.
	    // You could specify a String like "/storage/emulated/0/", but that can
	    // dangerous. Always use Android's API calls to get paths to the SD-card or
	    // internal memory.
	    i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

	    context.startActivityForResult(i, FILE_CODE);
	}
	
	public static void handleResult(Intent data) {
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
            // Do something with the URI
        }
	}
}
