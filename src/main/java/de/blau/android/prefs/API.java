package de.blau.android.prefs;

/**
 * Data structure class for API data
 * @author Jan
 */
public class API {
	public final String id;
	public final String name;
	public final String url;
	public final String readonlyurl;
	public final String notesurl;
	public final String user;
	public final String pass;
	public final String preset;     // no longer used
	public final boolean showicon;	// no longer used
	public final boolean oauth;
	public final String accesstoken;
	public final String accesstokensecret;
	
	public API(String id, String name, String url, String readonlyurl, String notesurl, 
			String user, String pass, String preset, int showicon, int oauth, String accesstoken, String accesstokensecret) {
		this.id = id;
		this.name = name;
		this.url = url;
		this.readonlyurl = readonlyurl;
		this.notesurl = notesurl;
		this.user = user;
		this.pass = pass;
		this.preset = preset;
		this.showicon = (showicon == 1);
		this.oauth = (oauth == 1);
		this.accesstoken = accesstoken;
		this.accesstokensecret = accesstokensecret;
	}
}
