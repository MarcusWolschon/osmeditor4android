package io.vespucci.contract;

/**
 * Constants for file names
 */
public final class Files {
    public static final String FILE_NAME_IMAGERY             = "imagery.geojson";
    public static final String FILE_NAME_VESPUCCI_IMAGERY    = "imagery_vespucci.geojson";
    public static final String FILE_NAME_AUTOPRESET_TEMPLATE = "autopreset-template.xml";
    public static final String FILE_NAME_AUTOPRESET          = "autopreset.xml";
    public static final String FILE_NAME_MRUTAGS             = "mrutags.xml";
    public static final String FILE_NAME_KEYS_V2             = "keys2.txt";
    public static final String FILE_NAME_KEYS_V2_DEFAULT     = "keys2-default.txt";
    public static final String FILE_NAME_GEOCONTEXT          = "geocontext.json";
    public static final String FILE_NAME_BOUNDARIES          = "boundaries.ser";
    public static final String FILE_NAME_MRUFILE             = "mru.dat";

    /**
     * Where we install the current version of vespucci
     */
    public static final String VERSION = "version.dat";

    /**
     * Private constructor
     */
    private Files() {
        // don't instantiate
    }
}
