package de.blau.android;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.os.Looper;
import androidx.annotation.NonNull;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;

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
    
    /**
     * Load some test data in to storage
     * 
     * @param c Class
     * @param fileName the name of the file with osm data
     * @return the StorageDelegator
     * 
     */
    @NonNull
    public static StorageDelegator loadTestData(@NonNull Class c, @NonNull String fileName) {
        StorageDelegator d = App.getDelegator();
        InputStream input = c.getResourceAsStream("/" + fileName);
        OsmParser parser = new OsmParser();
        try {
            parser.start(input);
            Storage storage = parser.getStorage();
            d.setCurrentStorage(storage);
            d.fixupApiStorage();
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException e) {
            fail(e.getMessage());
        }
        return d;
    }
    
    /**
     * Super ugly hack to get the looper to run
     */
    public static void runLooper() {
        try {
            Thread.sleep(3000); // NOSONAR
        } catch (InterruptedException e) { // NOSONAR
            // Ignore
        }
        shadowOf(Looper.getMainLooper()).idle();
    }
}
