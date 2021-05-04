package de.blau.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.blau.android.util.FileUtil;

public final class JavaResources {

    /**
     * Private constructor to stop instantiation
     */
    private JavaResources() {
        // private
    }

    /**
     * Copy a file from resources to a sub-directory of the public Vespucci directory
     * 
     * @param context Android Context
     * @param fileName the name of the file to copy
     * @param source the source sub-directory, can be null
     * @param destination the destination sub-directory
     * @param useVespucciDir if true use the Vespucci directory instead of the standard ones
     * @throws IOException if copying goes wrong
     */
    public static void copyFileFromResources(@NonNull Context context, @NonNull String fileName, @Nullable String source, @NonNull String destination,
            boolean useVespucciDir) throws IOException {
        
        File[] storageDirectories = ContextCompat.getExternalFilesDirs(context, null);
        File destinationDir = FileUtil.getPublicDirectory(useVespucciDir ? FileUtil.getPublicDirectory() : storageDirectories[0], destination);
        File destinationFile = new File(destinationDir, fileName);   
        copyFileFromResources(fileName, source, destinationFile);
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
    public static void copyFileFromResources(@NonNull String fileName, @Nullable String source, @NonNull File destinationFile) throws IOException, FileNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (OutputStream os = new FileOutputStream(destinationFile); InputStream is = loader.getResourceAsStream((source != null ? source : "") + fileName)) {
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }
}
