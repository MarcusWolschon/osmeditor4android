package de.blau.android.contract;

/**
 * Url constants for APIs etc
 * Convention: the constants starting with DEFAULT have a user configurable way of supplying other values 
 */
public interface Urls {
	public final static String DEFAULT_API = "https://api.openstreetmap.org/api/0.6/";
	public final static String DEFAULT_API_NO_HTTPS = "http://api.openstreetmap.org/api/0.6/";

	public static final String DEFAULT_NOMINATIM_SERVER = "http://nominatim.openstreetmap.org/"; 
	public static final String DEFAULT_PHOTON_SERVER = "http://photon.komoot.de/"; 
	
	public static final String WIKIPEDIA = "http://wikipedia.org/wiki/";
	public static final String WIKIDATA = "http://wikidata.org/wiki/";
	
	public static final String OSMOSE = "http://osmose.openstreetmap.fr/";
	
	public static final String DEFAULT_OFFSET_SERVER = "http://offsets.textual.ru/";
	
}
