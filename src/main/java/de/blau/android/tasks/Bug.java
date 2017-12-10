package de.blau.android.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;


/**
 * Subset of the fields and functionality for handling OSMOSE style bugs
 * 
 * @author Simon Poole
 */
public abstract class Bug extends Task implements Serializable {
	
	static final String DEBUG_TAG = Bug.class.getSimpleName();
	static final int LEVEL_ERROR=1;
	static final int LEVEL_WARNING=2;
	static final int LEVEL_MINOR_ISSUE=3;

	/**
	 * Date pattern used to parse the update date from a Osmose bug.
	 */
	static final String DATE_PATTERN_OSMOSE_BUG_UPDATED_AT = "yyyy-MM-dd HH:mm:ss z";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    String elems;
    String title;
    String subtitle;
    int level;
    Date update;

	/**
	 * Default constructor
	 */
	protected Bug() {
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
	 * Return list of elements from OSMOSE style list
	 * 
	 * This returns fake elements with version -1 for objects not downloaded
	 * @return list of OsmElement
	 */
	public final List<OsmElement> getElements() {
		ArrayList<OsmElement> result = new ArrayList<>();
		String[] elements = elems.split("_");
		StorageDelegator storageDelegator = App.getDelegator();
		for (String e:elements) {
			try {
				if (elems.startsWith("way")) {
					OsmElement osm = storageDelegator.getOsmElement(Way.NAME, Long.valueOf(e.substring(3)));
					if (osm == null) {
						osm = OsmElementFactory.createWay(Long.valueOf(e.substring(3)), -1, -1, (byte) -1);
					}
					result.add(osm);
				} else if (elems.startsWith("node")) {
					OsmElement osm = storageDelegator.getOsmElement(Node.NAME, Long.valueOf(e.substring(4)));
					if (osm == null) {
						osm = OsmElementFactory.createNode(Long.valueOf(e.substring(4)), -1, -1, (byte) -1, 0, 0);
					}
					result.add(osm);
				} else if (elems.startsWith("relation")) {
					OsmElement osm = storageDelegator.getOsmElement(Relation.NAME, Long.valueOf(e.substring(8)));
					if (osm == null) {
						osm = OsmElementFactory.createRelation(Long.valueOf(e.substring(8)), -1, -1, (byte) -1);
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
	
	abstract public String getLongDescription(Context context, boolean withElements);
	
	final String level2string(Context context) {
		switch (level) {
		case LEVEL_ERROR: return context.getString(R.string.error);
		case LEVEL_WARNING: return context.getString(R.string.warning);
		case LEVEL_MINOR_ISSUE: return context.getString(R.string.minor_issue);
		default: return context.getString(R.string.unknown_error_level);
		}
	}
		
	public final int getLevel() {
		return level;
	}
}
