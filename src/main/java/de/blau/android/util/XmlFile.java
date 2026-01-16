package de.blau.android.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Paths;

/**
 * Utility for preset and style files in xml format
 * 
 * @author Simon Poole
 *
 */
public final class XmlFile {

    private static final FilenameFilter xmlFileFilter = (File dir, String name) -> name.endsWith("." + FileExtensions.XML);
    private static final FileFilter     directoryFilter  = File::isDirectory;

    /**
     * Default private constructor
     */
    private XmlFile() {
        // nothing
    }
    
    /**
     * Get a candidate xml file name in dir
     * 
     * Will descend recursively in to sub-directories if file is not found at top
     * 
     * @param dir the directory
     * @return the file name or null
     */
    @Nullable
    public static String getFileName(@NonNull File dir) {
        File[] list = dir.listFiles(xmlFileFilter);
        if (list != null && list.length > 0) { // simply use the first XML file found
            return list[0].getName();
        } else {
            list = dir.listFiles(directoryFilter);
            if (list != null) {
                for (File f : list) {
                    String fileName = getFileName(f);
                    if (fileName != null) {
                        return f.getName() + Paths.DELIMITER + fileName;
                    }
                }
            }
        }
        return null;
    }
}
