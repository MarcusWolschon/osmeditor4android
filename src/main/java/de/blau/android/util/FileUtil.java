package de.blau.android.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.contract.Paths;

public abstract class FileUtil {

    private static final String DEBUG_TAG = FileUtil.class.getSimpleName();

    /**
     * Get our public directory, creating it if it doesn't exist
     * 
     * @return
     * @throws IOException
     */
    public static @NonNull File getPublicDirectory() throws IOException {
        return getPublicDirectory(Environment.getExternalStorageDirectory(), Paths.DIRECTORY_PATH_VESPUCCI);
    }

    /**
     * Get a File for directoryName in baseDirectory, if it doesn't exist create it
     * 
     * @param baseDirectory
     * @param directoryName
     * @return
     * @throws IOException
     */
    public static @NonNull File getPublicDirectory(@NonNull File baseDirectory, @NonNull String directoryName) throws IOException {
        if (directoryName.length() == 0) {
            throw new IllegalArgumentException("Directory path is empty.");
        }
        File outDir = new File(baseDirectory, directoryName);
        // noinspection ResultOfMethodCallIgnored
        outDir.mkdir(); // ensure directory exists;
        if (!outDir.isDirectory()) {
            throw new IOException("Unable to create directory: " + outDir.getPath());
        }
        return outDir;
    }

    /**
     * Open a local file for writing generating any necessary directories
     * 
     * @param fileName name of the filw we want to write to
     * @return a File object
     * @throws IOException
     */
    @NonNull
    public static File openFileForWriting(@NonNull final String fileName) throws IOException {
        File outfile = new File(fileName);
        String parent = outfile.getParent();
        if (parent == null) { // no directory specified, save to standard location
            outfile = new File(FileUtil.getPublicDirectory(), fileName);
        } else { // ensure directory exists
            File outdir = new File(parent);
            // noinspection ResultOfMethodCallIgnored
            outdir.mkdirs();
            if (!outdir.isDirectory()) {
                throw new IOException("Unable to create directory " + outdir.getPath());
            }
        }
        return outfile;
    }

    public static void copyFileFromAssets(Context context, String assetFileName, File destinationDir, String destinationFilename) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            AssetManager assetManager = context.getAssets();
            in = assetManager.open(assetFileName);
            File destinationFile = new File(destinationDir, destinationFilename);
            out = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            SavingHelper.close(in);
            SavingHelper.close(out);
        }
    }
}
