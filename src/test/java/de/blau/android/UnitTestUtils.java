package de.blau.android;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;

public final class UnitTestUtils {
    
    /**
     * Private constructor to avoid instantiation
     */
    private UnitTestUtils() {
        // don't instantiate
    }
    
    /**
     * Copy a file from resources to the current directory
     * 
     * @param c a Class
     * @param fileName the name of the file to copy
     * @throws IOException if copying goes wrong
     */
    public static void copyFileFromResources(@NonNull Class<?> c, @NonNull String fileName)
            throws IOException {
      
        File destinationDir = new File(".");
        File destinationFile = new File(destinationDir, fileName);
        try (OutputStream os = new FileOutputStream(destinationFile); InputStream is = c.getResourceAsStream("/" + fileName);) {

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        }
    }
}
