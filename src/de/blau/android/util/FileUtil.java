package de.blau.android.util;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
import de.blau.android.contract.Paths;

public class FileUtil {
	
	public static File getPublicDirectory() throws IOException {
		File sdcard = Environment.getExternalStorageDirectory();
		File outdir = new File(sdcard, Paths.DIRECTORY_PATH_VESPUCCI);
		outdir.mkdir(); // ensure directory exists;
		if (!outdir.isDirectory() ) {
			throw new IOException("Unable to create directory " + outdir.getPath());
		}
		return outdir;
	}
	
}
