package de.blau.android.contract;

/**
 * Url constants for APIs etc Convention: the constants starting with DEFAULT have a user configurable way of supplying
 * other values
 */
public interface Urls {
    String DEFAULT_API               = "https://api.openstreetmap.org/api/0.6/";
    String DEFAULT_API_NAME          = "OpenStreetMap";
    String DEFAULT_API_NO_HTTPS      = "http://api.openstreetmap.org/api/0.6/";
    String DEFAULT_API_NO_HTTPS_NAME = "OpenStreetMap no https";
    String DEFAULT_SANDBOX_API       = "https://master.apis.dev.openstreetmap.org/api/0.6/";
    String DEFAULT_SANDBOX_API_NAME  = "OpenStreetMap sandbox";

    String DEFAULT_NOMINATIM_SERVER = "http://nominatim.openstreetmap.org/";
    String DEFAULT_PHOTON_SERVER    = "http://photon.komoot.de/";

    String DEFAULT_OSMOSE_SERVER = "http://osmose.openstreetmap.fr/";

    String DEFAULT_OFFSET_SERVER = "http://offsets.textual.ru/";

    String DEFAULT_TAGINFO_SERVER = "https://taginfo.openstreetmap.org/";

    // currently not configurable
    String WIKIPEDIA = "http://en.wikipedia.org/wiki/";
    String WIKIDATA  = "http://wikidata.org/wiki/";

    String OSM       = "https://openstreetmap.org";
    String OSM_LOGIN = "https://www.openstreetmap.org/login";
    String OSM_WIKI  = "https://wiki.openstreetmap.org/";

    String ELI = "https://raw.githubusercontent.com/osmlab/editor-layer-index/gh-pages/imagery.geojson";

    String OAM_SERVER = "https://api.openaerialmap.org/";
}
