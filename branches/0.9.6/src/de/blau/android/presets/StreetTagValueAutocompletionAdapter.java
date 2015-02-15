/**
 * TagKeyAutocompletionAdapter.java
 * created: 12.06.2010 10:43:37
 * (c) 2010 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of OSMEditor by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMEditor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMEditor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMEditor.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************
 * Editing this file:
 *  -For consistent code-quality this file should be checked with the
 *   checkstyle-ruleset enclosed in this project.
 *  -After the design of this file has settled it should get it's own
 *   JUnit-Test that shall be executed regularly. It is best to write
 *   the test-case BEFORE writing this class and to run it on every build
 *   as a regression-test.
 */
package de.blau.android.presets;

//other imports
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.propertyeditor.PropertyEditor;
import de.blau.android.util.GeoMath;
import de.blau.android.util.MultiHashMap;


/**
 * Project: OSMEditor<br/>
 * TagKeyAutocompletionAdapter.java<br/>
 * created: 12.06.2010 10:43:37 <br/>
 *<br/><br/>
 * <b>Adapter for the {@link AutoCompleteTextView} in the {@link PropertyEditor}
 * that is for the VALUE  for the key "addr:street" .</a>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class StreetTagValueAutocompletionAdapter extends ArrayAdapter<String> {

    /**
     * The tag we use for Android-logging.
     */
    @SuppressWarnings("unused")
	private static final String DEBUG_TAG = StreetTagValueAutocompletionAdapter.class.getName();
    
    private String[] names;
    private Map<String, Long> idsByNames = new HashMap<String, Long>();

    /**
     * 
     * @param aContext used to load resources
     * @param aTextViewResourceId given to {@link ArrayAdapter}
     * @param osmId 
     * @param type 
     */
    public StreetTagValueAutocompletionAdapter(final Context aContext, final int aTextViewResourceId,
                                       final StorageDelegator delegator,
                                       final String osmElementType,
                                       final long osmId) {
        super(aContext, aTextViewResourceId);
        names = getArray(delegator, PlaceTagValueAutocompletionAdapter.getLocation(delegator, osmElementType, osmId));
        for (String s:names) {
        	super.add(s);
        }
    }

    
    /**
     * Get all street-names in the area
     * @param delegator
     * @param location
     * @return all street-names
     */
    private String[] getArray(final StorageDelegator delegator, final int[] location) {
		// build list of names with their closest distance to location
		Map<String, Double> distancesByNames = new HashMap<String, Double>();
		String[] nameTags = {Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME, Tags.KEY_NAME_LEFT, Tags.KEY_NAME_RIGHT};
		
		for (Way way : delegator.getCurrentStorage().getWays()) {
			if (way.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
				double distance = -1D;
				long iD = way.getOsmId();
				for (String tag:nameTags) { 
Log.d("StreetTagValueAutocompletionAdapter","Search for " + tag);
					String name = way.getTagWithKey(tag);
					if (name != null) {
						Log.d("StreetTagValueAutocompletionAdapter","Name " + name);
						if (distance == -1D) { // only calc once
							distance = getDistance(way, location);
						}
						if (distancesByNames.containsKey(name)) {
							// way already in list - keep shortest distance
							if (distance <  distancesByNames.get(name)) {
								distancesByNames.put(name, distance);
								idsByNames.put(name,Long.valueOf(iD));
							}
						} else {
							distancesByNames.put(name, distance);
							idsByNames.put(name,Long.valueOf(iD));
						}
					}
				}
			}
		}
		// sort names by distance
		MultiHashMap<Double, String> retval = new MultiHashMap<Double, String>(true);
		for (String name : distancesByNames.keySet()) {
			retval.add(distancesByNames.get(name), name);
		}
		 
		return retval.getValues().toArray(new String[retval.getValues().size()]);
	}
    
    /**
     * @param way
     * @param location
     * @return the minimum distance of the given way to the given location
     */
	private static double getDistance(final Way way, final int[] location) {
		double distance = Double.MAX_VALUE;
		if (location != null) {
			Node n1 = null;
			for (Node n2 : way.getNodes()) {
				// distance to nodes of way
				if (n1 != null) {
					// distance to lines of way
					distance = Math.min(distance,
							GeoMath.getLineDistance(
									location[0], location[1],
									n1.getLat(), n1.getLon(),
									n2.getLat(), n2.getLon()));
				}
				n1 = n2;
			}
		}
		return distance;
	}


	public String[] getNames() {
		return names;
	}
	
	public long getId(String name) throws OsmException {
		Log.d("StreetTagValueAutocompletionAdapter","looking for " + name);
		Long iD = idsByNames.get(name);
		if (iD != null) {
			return iD.longValue();
		}
		else {
			throw new OsmException("way not found in adapter");
		}
	}
}


