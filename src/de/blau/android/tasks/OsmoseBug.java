package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.util.DateFormatter;


/**
 * A bug in the OpenStreetBugs database, or a prospective new bug.
 * @author Andrew Gregory
 */
public class OsmoseBug extends Task implements Serializable {
	
	private static final String DEBUG_TAG = OsmoseBug.class.getSimpleName();
	private static final int LEVEL_ERROR=1;
	private static final int LEVEL_WARNING=2;
	private static final int LEVEL_MINOR_ISSUE=3;

	/**
	 * Date pattern used to parse the update date from a Osmose bug.
	 */
	private static final String DATE_PATTERN_OSMOSE_BUG_UPDATED_AT = "yyyy-MM-dd HH:mm:ss z";

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
								try {
									bug.update = DateFormatter.getDate(
											DATE_PATTERN_OSMOSE_BUG_UPDATED_AT,
											reader.nextString());
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
	
	/**
	 * THis returns fake elements with version -1 for objects not downloaded
	 * @return
	 */
	public ArrayList<OsmElement> getElements() {
		ArrayList<OsmElement> result = new ArrayList<OsmElement>();
		String[] elements = elems.split("_");
		StorageDelegator storageDelegator = Application.getDelegator();
		for (String e:elements) {
			try {
				if (elems.startsWith("way")) {
					OsmElement osm = storageDelegator.getOsmElement(Way.NAME, Long.valueOf(e.substring(3)));
					if (osm == null) {
						osm = OsmElementFactory.createWay(Long.valueOf(e.substring(3)), -1, (byte) -1);
					}
					result.add(osm);
				} else if (elems.startsWith("node")) {
					OsmElement osm = storageDelegator.getOsmElement(Node.NAME, Long.valueOf(e.substring(4)));
					if (osm == null) {
						osm = OsmElementFactory.createNode(Long.valueOf(e.substring(4)), -1, (byte) -1, 0, 0);
					}
					result.add(osm);
				} else if (elems.startsWith("relation")) {
					OsmElement osm = storageDelegator.getOsmElement(Relation.NAME, Long.valueOf(e.substring(8)));
					if (osm == null) {
						osm = OsmElementFactory.createRelation(Long.valueOf(e.substring(8)), -1, (byte) -1);
					}
					result.add(osm);
				}
			}
			catch(Exception ex) {
				Log.d(DEBUG_TAG,"couldn't retrieve element " + elems + " " + ex);
			}
		}
		return result;		
	}
	
	public String getLongDescription(Context context, boolean withElements) {
		String result = "Osmose: " + level2string(context) + "<br><br>" + (subtitle.length() != 0 ?  subtitle : title ) + "<br>";
		if (withElements) {
			for (OsmElement osm:getElements()) {
				if (osm.getOsmVersion() >= 0) { 
					result = result + "<br>" + osm.getName() + " (" + context.getString(R.string.openstreetbug_not_downloaded) + ") #" + osm.getOsmId();
				} else {
					result = result + "<br>" + osm.getName() + " " + osm.getDescription(false);
				}
				result = result + "<br><br>";
			}
		}
		result = result + context.getString(R.string.openstreetbug_last_updated) + ": " + update 
				+ " " + context.getString(R.string.id) + ": " + id;
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
