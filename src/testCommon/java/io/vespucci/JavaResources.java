package io.vespucci;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.util.FileUtil;

public final class JavaResources {

    private static final String DEBUG_TAG = JavaResources.class.getSimpleName().substring(0, Math.min(23, JavaResources.class.getSimpleName().length()));

    /**
     * Private constructor to stop instantiation
     */
    private JavaResources() {
        // private
    }

    /**
     * Copy a file from resources to a sub-directory of the standard public Vespucci directory
     * 
     * @param context Android Context
     * @param fileName the name of the file to copy
     * @param source the source sub-directory, can be null
     * @param destination the destination sub-directory
     * @return the destination File
     * @throws IOException if copying goes wrong
     */
    @NonNull
    public static File copyFileFromResources(@NonNull Context context, @NonNull String fileName, @Nullable String source, @NonNull String destination)
            throws IOException {
        File destinationDir = FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), destination);
        File destinationFile = new File(destinationDir, fileName);
        copyFileFromResources(fileName, source, destinationFile);
        return destinationFile;
    }

    /**
     * Copy a file from resources
     * 
     * @param fileName the name of the file to copy
     * @param source the source sub-directory, can be null
     * @param destinationFile destination
     * @throws IOException if copying goes wrong
     * @throws FileNotFoundException if the file is not found
     */
    public static void copyFileFromResources(@NonNull String fileName, @Nullable String source, @NonNull File destinationFile) throws IOException {
        Log.d(DEBUG_TAG, "args " + fileName + " " + source + " " + destinationFile.getAbsolutePath());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (OutputStream os = new FileOutputStream(destinationFile); InputStream is = loader.getResourceAsStream((source != null ? source : "") + fileName)) {
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, ex.getMessage());
        }
    }
}
