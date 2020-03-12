package de.blau.android.contract;

/**
 * Url constants for APIs etc Convention: the constants starting with DEFAULT have a user configurable way of supplying
 * other values
 */
public final class Urls {
    /**
     * Private constructor to avoid instantation
     */
    private Urls() {
        // empty
    }

    public static final String DEFAULT_API               = "https://api.openstreetmap.org/api/0.6/";
    public static final String DEFAULT_API_NAME          = "OpenStreetMap";
    public static final String DEFAULT_API_NO_HTTPS      = "http://api.openstreetmap.org/api/0.6/";
    public static final String DEFAULT_API_NO_HTTPS_NAME = "OpenStreetMap no https";
    public static final String DEFAULT_SANDBOX_API       = "https://master.apis.dev.openstreetmap.org/api/0.6/";
    public static final String DEFAULT_SANDBOX_API_NAME  = "OpenStreetMap sandbox";

    public static final String DEFAULT_NOMINATIM_SERVER = "https://nominatim.openstreetmap.org/";
    public static final String DEFAULT_PHOTON_SERVER    = "https://photon.komoot.de/";

    public static final String DEFAULT_OSMOSE_SERVER      = "https://osmose.openstreetmap.fr/";
    public static final String DEFAULT_MAPROULETTE_SERVER = "https://maproulette.org/";

    public static final String DEFAULT_OFFSET_SERVER = "http://offsets.textual.ru/";

    public static final String DEFAULT_TAGINFO_SERVER = "https://taginfo.openstreetmap.org/";

    // currently not configurable
    public static final String WIKIPEDIA = "https://en.wikipedia.org/wiki/";
    public static final String WIKIDATA  = "https://wikidata.org/wiki/";

    public static final String OSM       = "https://openstreetmap.org";
    public static final String OSM_LOGIN = "https://www.openstreetmap.org/login";
    public static final String OSM_WIKI  = "https://wiki.openstreetmap.org/";

    public static final String ELI = "https://osmlab.github.io/editor-layer-index/imagery.geojson";

    public static final String OAM_SERVER = "https://api.openaerialmap.org/";

    public static final String MSF_SERVER = "https://mapsplit.poole.ch/";
}
