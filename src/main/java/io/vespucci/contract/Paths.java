package io.vespucci.contract;

/**
 * Path constants for directories, files, extensions and similar.
 */
public final class Paths {
    /**
     * Private constructor to avoid instantation
     */
    private Paths() {
        // empty
    }

    public static final String DIRECTORY_PATH_EXTERNAL_SD_CARD   = "/external_sd";   // NOSONAR
    public static final String DIRECTORY_PATH_PICTURES           = "Pictures";
    public static final String DIRECTORY_PATH_SCRIPTS            = "Scripts";
    public static final String DIRECTORY_PATH_IMPORTS            = "imports";
    public static final String DIRECTORY_PATH_STYLES             = "styles";
    public static final String DIRECTORY_PATH_IMAGERY            = "imagery";
    public static final String DIRECTORY_PATH_OTHER              = "other";
    public static final String DIRECTORY_PATH_STORAGE            = "/storage";       // NOSONAR
    public static final String DIRECTORY_PATH_VESPUCCI           = "Vespucci";
    public static final String DIRECTORY_PATH_AUTOPRESET         = "autopreset";
    public static final String DIRECTORY_PATH_TILE_CACHE         = "/tiles/";        // NOSONAR
    public static final String DIRECTORY_PATH_TILE_CACHE_CLASSIC = "/andnav2/tiles/";// NOSONAR
    public static final String DIRECTORY_PATH_GPX                = "GPX";
    public static final String DIRECTORY_PATH_AUTOSAVE           = "Autosave";
    
    public static final String DELIMITER                         = "/";
}
