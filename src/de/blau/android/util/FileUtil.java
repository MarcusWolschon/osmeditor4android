package de.blau.android.util;

import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import de.blau.android.contract.Paths;

public abstract class FileUtil {

    public static
    @NonNull
    File getPublicDirectory() throws IOException {
        return getPublicDirectory(
                Environment.getExternalStorageDirectory(),
                Paths.DIRECTORY_PATH_VESPUCCI);
    }

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
