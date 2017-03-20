package de.blau.android.contract;

/**
 * Url constants for APIs etc
 * Convention: the constants starting with DEFAULT have a user configurable way of supplying other values 
 */
public interface Urls {
	String DEFAULT_API = "https://api.openstreetmap.org/api/0.6/";
	String DEFAULT_API_NO_HTTPS = "http://api.openstreetmap.org/api/0.6/";

	String DEFAULT_NOMINATIM_SERVER = "http://nominatim.openstreetmap.org/";
	String DEFAULT_PHOTON_SERVER = "http://photon.komoot.de/";
	
	String WIKIPEDIA = "http://en.wikipedia.org/wiki/";
	String WIKIDATA = "http://wikidata.org/wiki/";
	
	String OSMOSE = "http://osmose.openstreetmap.fr/";
	
	String DEFAULT_OFFSET_SERVER = "http://offsets.textual.ru/";
	
	String OSM = "https://openstreetmap.org";
	String OSM_LOGIN = "https://www.openstreetmap.org/login";
}
