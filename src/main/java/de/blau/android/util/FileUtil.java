package de.blau.android.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Schemes;

public final class FileUtil {
    private static final char PATH_DELIMITER_CHAR = '/';

    private static final String DEBUG_TAG = FileUtil.class.getSimpleName();

    public static final String FILE_SCHEME_PREFIX = Schemes.FILE + ":";

    /**
     * Private constructor to stop instantiation
     */
    private FileUtil() {
        // private
    }

    /**
     * Get our public directory
     * 
     * @return a File object for the public directory
     * @throws IOException if we can't create the directory
     */
    public static @NonNull File getPublicDirectory() throws IOException {
        return getPublicDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Paths.DIRECTORY_PATH_VESPUCCI); // NOSONAR
    }

    /**
     * Check if the public directory has been created
     * 
     * @return true if the directory exists
     */
    public static boolean publicDirectoryExists() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Paths.DIRECTORY_PATH_VESPUCCI).exists(); // NOSONAR
    }

    /**
     * Get our legacy public directory
     * 
     * @return a File object for the public directory
     * @throws IOException if we can't create the directory
     */
    public static @NonNull File getLegacyPublicDirectory() throws IOException {
        return getPublicDirectory(Environment.getExternalStorageDirectory(), Paths.DIRECTORY_PATH_VESPUCCI); // NOSONAR
    }

    /**
     * Get a File for directoryName in baseDirectory, if it doesn't exist create it
     * 
     * @param baseDirectory the base directory
     * @param directoryName the new sub-directory
     * @return a File object for the sub-director of the public directory
     * @throws IOException if we can't create the directory
     */
    public static @NonNull File getPublicDirectory(@NonNull File baseDirectory, @NonNull String directoryName) throws IOException {
        if (directoryName.length() == 0) {
            throw new IllegalArgumentException("Directory path is empty.");
        }
        File outDir = new File(baseDirectory, directoryName);
        // noinspection ResultOfMethodCallIgnored
        outDir.mkdir(); // ensure directory exists
        if (!outDir.isDirectory()) {
            throw new IOException("Unable to create directory: " + outDir.getPath());
        }
        return outDir;
    }

    /**
     * Get a File for directoryName in the Android/data hierarchy, if it doesn't exist create it
     * 
     * @param ctx an Android Context
     * @param directoryName the new sub-directory
     * @return a File object for the sub-directory
     * @throws IOException if we can't create the directory
     */
    public static @NonNull File getApplicationDirectory(@NonNull Context ctx, @NonNull String directoryName) throws IOException {
        if (directoryName.length() == 0) {
            throw new IllegalArgumentException("Directory path is empty.");
        }
        File outDir = new File(ctx.getExternalFilesDir(null), directoryName);
        // noinspection ResultOfMethodCallIgnored
        outDir.mkdir(); // ensure directory exists
        if (!outDir.isDirectory()) {
            throw new IOException("Unable to create directory: " + outDir.getPath());
        }
        return outDir;
    }

    /**
     * Open a local file for writing generating any necessary directories
     * 
     * @param context Android Context
     * @param fileName name of the filw we want to write to
     * @return a File object
     * @throws IOException if we can't create the directories
     */
    @NonNull
    public static File openFileForWriting(@NonNull Context context, @NonNull final String fileName) throws IOException {
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
     * @throws IOException if we can't copy or write the file
     */
    public static void copyFileFromAssets(@NonNull Context context, @NonNull String assetFileName, @NonNull File destinationDir,
            @NonNull String destinationFilename) throws IOException {
        AssetManager assetManager = context.getAssets();
        try (InputStream in = assetManager.open(assetFileName); FileOutputStream out = new FileOutputStream(new File(destinationDir, destinationFilename));) {
            copy(in, out);
        }
    }

    /**
     * Copy a file (without overwriting)
     * 
     * @param inFile input File
     * @param outFile output File
     * @throws IOException if we can't copy or write the file
     */
    public static void copy(@NonNull File inFile, @NonNull File outFile) throws IOException {
        String path = outFile.getAbsolutePath();
        for (int i = 1; i < 100; i++) {
            if (!outFile.exists()) {
                try (FileInputStream in = new FileInputStream(inFile); FileOutputStream out = new FileOutputStream(outFile);) {
                    copy(in, out);
                }
                return;
            }
            outFile = new File(path + " (" + Integer.toString(i) + ")");
        }
        throw new IOException("Unable to copy " + inFile.getAbsolutePath() + " to " + outFile + " without overwriting");
    }

    /**
     * Copy a file
     * 
     * @param in InputStream
     * @param outFile output File
     * @throws IOException if we can't copy or write the file
     */
    public static void copy(@NonNull InputStream in, @NonNull File outFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(outFile);) {
            copy(in, out);
        }
    }

    /**
     * Copy an indeterminate number of bytes from an InputStream to an OutputStream, caller needs to close the streams
     * 
     * @param in InputStream
     * @param out OutputStream
     * @throws IOException if IO fails
     */
    private static void copy(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    /**
     * Copy a directory recursively
     * 
     * @param source source location
     * @param target target location
     * @throws IOException if reading or writing goes wrong
     */
    public static void copyDirectory(@NonNull File source, @NonNull File target) throws IOException {
        if (source.exists()) {
            if (source.isDirectory()) {
                if (!target.exists()) {
                    target.mkdir();
                }
                String[] children = source.list();
                if (children != null) {
                    for (int i = 0; i < source.listFiles().length; i++) {
                        copyDirectory(new File(source, children[i]), new File(target, children[i]));
                    }
                }
            } else {
                copy(source, target);
            }
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
        String path = ContentResolverUtil.getPath(ctx, uri);
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
        return path.substring(path.lastIndexOf('.') + 1);
    }

    /**
     * Check if a directory has more that the maxCacheSize contents and reduce that to 90% by deleting oldest files
     * 
     * @param dir the cache directory
     * @param maxCacheSize the maximum cache size
     */
    public static void pruneCache(@NonNull File dir, long maxCacheSize) {
        long result = 0;

        List<File> fileList = Arrays.asList(dir.listFiles());
        for (File f : fileList) {
            result += f.length();
        }
        if (result > maxCacheSize) {
            maxCacheSize = (long) (0.9 * maxCacheSize); // make 10% free
            Collections.sort(fileList, (f0, f1) -> Long.compare(f0.lastModified(), f1.lastModified()));
            for (File f : fileList) {
                long len = f.length();
                if (f.delete()) { // NOSONAR requires API 26
                    result -= len;
                }
                if (result < maxCacheSize) {
                    break;
                }
            }
        }
    }

    /**
     * Check if a directory contains more than max files and reduce that to 80% by deleting oldest files
     * 
     * @param dir the cache directory
     * @param maxCount the maximum number of files in the directory
     */
    public static void pruneFiles(@NonNull File dir, int maxCount) {
        List<File> fileList = Arrays.asList(dir.listFiles());
        long count = fileList.size();
        if (count > maxCount) {
            maxCount = (int) (0.8 * maxCount); // make 20% free
            Collections.sort(fileList, (f0, f1) -> Long.compare(f0.lastModified(), f1.lastModified()));
            for (File f : fileList) {
                if (f.delete()) { // NOSONAR requires API 26
                    count--;
                } else {
                    Log.e(DEBUG_TAG, "pruneFiles delete failed");
                }
                if (count <= maxCount) {
                    break;
                }
            }
        }
    }

    /**
     * Unpack a zipped file
     * 
     * Code from http://stackoverflow.com/questions/3382996/how-to-unzip-files-programmatically-in-android
     * 
     * @param dir preset directory
     * @param zipname the zip file
     * @return true if successful
     */
    public static boolean unpackZip(@NonNull String dir, @NonNull String zipname) {
        try (InputStream is = new FileInputStream(dir + zipname); ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            String filename;
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;
            while ((ze = zis.getNextEntry()) != null) {
                // zapis do souboru
                filename = ze.getName();
                Log.d(DEBUG_TAG, "Unzip " + filename);
                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (!"".equals(filename)) {
                    if (filename.indexOf(PATH_DELIMITER_CHAR) > 0 && !filename.endsWith(Paths.DELIMITER)) { // NOSONAR
                        int slash = filename.lastIndexOf(PATH_DELIMITER_CHAR);
                        String path = filename.substring(0, slash);
                        File fmd = new File(dir + path);
                        if (!fmd.exists()) {
                            fmd.mkdirs();
                        }
                    } else if (ze.isDirectory()) {
                        File fmd = new File(dir + filename);
                        // noinspection ResultOfMethodCallIgnored
                        if (!fmd.exists()) {
                            fmd.mkdirs();
                        }
                        continue;
                    }
                    try (FileOutputStream fout = new FileOutputStream(dir + filename)) {
                        // cteni zipu a zapis
                        while ((count = zis.read(buffer)) != -1) {
                            fout.write(buffer, 0, count);
                        }
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Unzipping failed with " + e.getMessage());
            return false;
        }

        return true;
    }
}
