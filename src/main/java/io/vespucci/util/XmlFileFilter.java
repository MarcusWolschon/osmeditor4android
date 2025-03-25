package io.vespucci.util;

import java.io.File;
import java.io.FilenameFilter;

import io.vespucci.contract.FileExtensions;

/**
 * Filter for .xml ending file names
 * 
 * @author Simon Poole
 *
 */
public class XmlFileFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith("." + FileExtensions.XML);
    }
}
