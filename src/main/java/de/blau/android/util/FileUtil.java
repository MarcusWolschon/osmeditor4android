package de.blau.android.util;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
import android.support.annotation.NonNull;
import de.blau.android.contract.Paths;

public abstract class FileUtil {
	
	/**
	 * Get our public directory, creating it if it doesn't exist
	 * @return
	 * @throws IOException
	 */
    public static
    @NonNull
    File getPublicDirectory() throws IOException {
        return getPublicDirectory(
                Environment.getExternalStorageDirectory(),
                Paths.DIRECTORY_PATH_VESPUCCI);
    }

    /**
     * Get a File for directoryName in baseDirectory, if it doesn't exist create it
     * @param baseDirectory
     * @param directoryName
     * @return
     * @throws IOException
     */
    public static
    @NonNull
    File getPublicDirectory(@NonNull File baseDirectory, @NonNull String directoryName)
            throws IOException {
        if (directoryName.length() == 0) {
            throw new IllegalArgumentException("Directory path is empty.");
        }
        File outDir = new File(baseDirectory, directoryName);
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdir(); // ensure directory exists;
        if (!outDir.isDirectory()) {
            throw new IOException("Unable to create directory: " + outDir.getPath());
        }
        return outDir;
    }
}
