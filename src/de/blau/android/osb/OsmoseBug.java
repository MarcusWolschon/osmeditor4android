package de.blau.android.osb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.names.Names.TagMap;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.util.jsonreader.JsonReader;
import android.content.Context;
import android.content.res.AssetManager;
import android.text.Html;
import android.util.Log;


/**
 * A bug in the OpenStreetBugs database, or a prospective new bug.
 * @author Andrew Gregory
 */
public class OsmoseBug extends Bug implements Serializable {
	
	private static final String DEBUG_TAG = OsmoseBug.class.getSimpleName();
	private static final int LEVEL_ERROR=1;
	private static final int LEVEL_WARNING=2;
	private static final int LEVEL_MINOR_ISSUE=3;
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	private int item;
	private int source;
	private int bugclass; // class
	private String elems;
	private int subclass;
	private String title;
	private String subtitle;
	private int level;
	private Date update;
	private String username;
	
	public static ArrayList<OsmoseBug> parseBugs(InputStream is) throws  IOException, NumberFormatException {
		ArrayList<OsmoseBug> result = new ArrayList<OsmoseBug>();
		try {
			JsonReader reader = new JsonReader(new InputStreamReader(is));
			try {
				try {
					// key object
					String key = null;
					reader.beginObject();
					while (reader.hasNext()) {						
						key = reader.nextName(); // 
						if (key.equals("description")) {
							reader.skipValue();
						} else if (key.equals("errors")) {
							reader.beginArray();
							while (reader.hasNext()) {
								OsmoseBug bug = new OsmoseBug();
								reader.beginArray();
								bug.lat = (int)(reader.nextDouble()*1E7D);
								bug.lon = (int)(reader.nextDouble()*1E7D);
								bug.id = reader.nextLong();
								bug.item = reader.nextInt();
								bug.source = reader.nextInt();
								bug.bugclass = reader.nextInt();
								bug.elems = reader.nextString();
								bug.subclass = reader.nextInt();
								bug.subtitle = reader.nextString();
								bug.title = reader.nextString();
								bug.level = reader.nextInt();
								SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
								try {
									bug.update = df.parse(reader.nextString());
								} catch (java.text.ParseException pex) {
									bug.update = new Date();
								}
								bug.username = reader.nextString();
								reader.endArray();
								result.add(bug);
							}
							reader.endArray();
						}
					}
					reader.endObject();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			finally {
				reader.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Used for when parsing API output
	 */
	public OsmoseBug() {
		open();
	}


	/**
	 * Get a string descriptive of the bug. This is intended to be used as a
	 * short bit of text representative of the bug.
	 * @return The first comment of the bug.
	 */
	@Override
	public String getDescription() {
		return "Osmose: " + (subtitle.length() != 0 ?  subtitle : title ); 
	}
	
	@Override
	/**
	 * Get the timestamp of the most recent change.
	 * @return The timestamp of the most recent change.
	 */
	public Date getLastUpdate() {
		return update;
	}
	
	public ArrayList<OsmElement> getElements() {
		ArrayList<OsmElement> result = new ArrayList<OsmElement>();
		try {
			String[] elements = elems.split("_");
			for (String e:elements) {
				if (elems.startsWith("way")) {
					result.add(Application.getDelegator().getOsmElement(Way.NAME,Long.valueOf(e.substring(3))));
				} else if (elems.startsWith("node")) {
					result.add(Application.getDelegator().getOsmElement(Node.NAME,Long.valueOf(e.substring(4))));
				} else if (elems.startsWith("relation")) {
					result.add(Application.getDelegator().getOsmElement(Relation.NAME,Long.valueOf(e.substring(8))));
				}
			}
		}
		catch(Exception ex) {
			Log.d(DEBUG_TAG,"couldn't retrieve element " + elems + " " + ex);
		}
		return result;		
	}
	
	public String getLongDescription(Context context) {
		String result = "Osmose: " + level2string(context) + "<br><br>" + (subtitle.length() != 0 ?  subtitle : title ) + "<br>";
		String h = context.getString(R.string.element);
		String[] elements = elems.split("_");
		for (String e:elements) {
			OsmElement osm = null;
			String obj = "unknown";
			long id = -1L;
			if (e.startsWith(Way.NAME)) {
				id = Long.valueOf(e.substring(3));
				obj = Way.NAME;
			} else if (e.startsWith(Node.NAME)) {
				id = Long.valueOf(e.substring(4));
				obj = Node.NAME;
			} else if (e.startsWith(Relation.NAME)) {
				id = Long.valueOf(e.substring(8));
				obj = Relation.NAME;
			}
			osm = Application.getDelegator().getOsmElement(obj,id);
			if (osm == null) { 
				result = result + "<br>" + obj + " (" + context.getString(R.string.openstreetbug_not_downloaded) + ") #" + id;
			} else {
				result = result + "<br>" + obj  + " " + osm.getDescription(false);
			}
		}
		result = result + "<br><br>" + context.getString(R.string.openstreetbug_last_updated) + ": " + update;
		return result; 
	}
	

	private String level2string(Context context) {
		switch (level) {
		case LEVEL_ERROR: return context.getString(R.string.error);
		case LEVEL_WARNING: return context.getString(R.string.warning);
		case LEVEL_MINOR_ISSUE: return context.getString(R.string.minor_issue);
		default: return context.getString(R.string.unknown_error_level);
		}
	}
	
	public String bugFilterKey() {
		switch (level) {
		case LEVEL_ERROR: return "OSMOSE_ERROR";
		case LEVEL_WARNING: return "OSMOSE_WARNING";
		case LEVEL_MINOR_ISSUE: return "OSMOSE_MINOR_ISSUE";
		default: return "?";
		}
	}
	
	public int getLevel() {
		return level;
	}
}
