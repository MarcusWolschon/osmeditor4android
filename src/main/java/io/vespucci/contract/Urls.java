package io.vespucci.contract;

/**
 * Url constants for APIs etc Convention: the constants starting with DEFAULT have a user configurable way of supplying
 * other values
 */
public final class Urls {
    /**
     * Private constructor to avoid instantiation
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

    public static final String DEFAULT_NOMINATIM_SERVER   = "https://nominatim.openstreetmap.org/";
    public static final String DEFAULT_PHOTON_SERVER      = "https://photon.komoot.io/";
    public static final String DEFAULT_OSMOSE_SERVER      = "https://osmose.openstreetmap.fr/";
    public static final String DEFAULT_MAPROULETTE_SERVER = "https://maproulette.org/";
    public static final String DEFAULT_OFFSET_SERVER      = "http://offsets.textual.ru/";
    public static final String DEFAULT_TAGINFO_SERVER     = "https://taginfo.openstreetmap.org/";
    public static final String DEFAULT_OVERPASS_SERVER    = "https://overpass-api.de/api/interpreter";
    public static final String DEFAULT_OAM_SERVER         = "https://api.openaerialmap.org/";
    public static final String DEFAULT_PANORAMAX_API_URL  = "https://panoramax.xyz/";

    // these are only configurable for testing
    public static final String DEFAULT_MAPILLARY_IMAGES_V4        = "https://graph.mapillary.com/%s?access_token=%s&fields=thumb_2048_url,computed_geometry,computed_compass_angle,captured_at";
    public static final String DEFAULT_MAPILLARY_SEQUENCES_URL_V4 = "https://graph.mapillary.com/image_ids?sequence_id=%s&access_token=%s&fields=id";

    public static final String DEFAULT_OSM_WIKI = "https://wiki.openstreetmap.org/";

    // currently not configurable
    public static final String WIKIPEDIA = "https://wikipedia.org/wiki/";
    public static final String WIKIDATA  = "https://wikidata.org/wiki/";

    public static final String OSM       = "https://openstreetmap.org";
    public static final String OSM_LOGIN = "https://www.openstreetmap.org/login";

    public static final String ELI          = "https://osmlab.github.io/editor-layer-index/imagery.geojson";
    public static final String JOSM_IMAGERY = "https://josm.openstreetmap.de/maps?format=geojson";

    public static final String MSF_SERVER = "https://mapsplit.poole.ch/";

    public static final String EGM96 = "https://github.com/simonpoole/egm96/raw/master/src/main/resources/EGM96.dat";

    public static final String GITHUB = "https://github.com/";

    public static final String REMOTE_ICON_LOCATION = "https://simonpoole.github.io/beautified-JOSM-preset/icons/png/";
}
