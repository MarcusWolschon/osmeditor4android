package de.blau.android.util;

import java.io.File;
import java.io.FilenameFilter;

import de.blau.android.contract.Paths;

/**
 * Filter for .xml ending file names
 * 
 * @author Simon Poole
 *
 */
public class XmlFileFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith(Paths.FILE_EXTENSION_XML);
    }
}
