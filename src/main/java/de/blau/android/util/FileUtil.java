package de.blau.android.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.contract.Paths;

public abstract class FileUtil {
    private static final String DEBUG_TAG          = FileUtil.class.getSimpleName();
    public static final String  FILE_SCHEME        = "file";
    public static final String  FILE_SCHEME_PREFIX = "file:";

    /**
     * Get our public directory, creating it if it doesn't exist
     * 
     * @return a File object for the public directory
     * @throws IOException
     */
    public static @NonNull File getPublicDirectory() throws IOException {
        return getPublicDirectory(Environment.getExternalStorageDirectory(), Paths.DIRECTORY_PATH_VESPUCCI);
    }

    /**
     * Get a File for directoryName in baseDirectory, if it doesn't exist create it
     * 
     * @param baseDirectory the base directory
     * @param directoryName the new sub-directory
     * @return a File object for the sub-director of the public directory
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

    /**
     * Copy a file from the assets to a (public) destination
     * 
     * @param context Android Context
     * @param assetFileName the filename in the assets
     * @param destinationDir destination directory
     * @param destinationFilename destination filename
     * @throws IOException
     */
    public static void copyFileFromAssets(@NonNull Context context, @NonNull String assetFileName, @NonNull File destinationDir,
            @NonNull String destinationFilename) throws IOException {
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

    /**
     * Try to convert a content Uri to a file Uri (will handle file uris gracefully)
     * 
     * @param ctx Android Context
     * @param uri the content Uri
     * @return the converted Uri or null
     */
    @Nullable
    public static Uri contentUriToFileUri(@NonNull Context ctx, @NonNull Uri uri) {
        String path = SelectFile.getPath(ctx, uri);
        if (path != null) {
            return Uri.parse(FILE_SCHEME_PREFIX + path);
        }
        return null;
    }

    /**
     * Get the extension of the file, aka the last . separated bit
     * 
     * @param path the path including the filename and extension
     * @return the extension or the full path
     */
    @NonNull
    public static String getExtension(@Nullable String path) {
        if (path == null) {
            return "";
        }
        return path.substring(path.lastIndexOf(".") + 1);
    }
}
