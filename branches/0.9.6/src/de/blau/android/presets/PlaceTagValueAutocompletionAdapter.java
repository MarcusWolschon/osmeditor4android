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
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.TagEditor;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.GeoMath;
import de.blau.android.util.MultiHashMap;


/**
 * Project: OSMEditor<br/>
 * TagKeyAutocompletionAdapter.java<br/>
 * created: 12.06.2010 10:43:37 <br/>
 *<br/><br/>
 * <b>Adapter for the {@link AutoCompleteTextView} in the {@link TagEditor}
 * that is for the VALUE  for the key "addr:street" .</a>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class PlaceTagValueAutocompletionAdapter extends ArrayAdapter<String> {

    /**
     * The tag we use for Android-logging.
     */
    @SuppressWarnings("unused")
	private static final String DEBUG_TAG = PlaceTagValueAutocompletionAdapter.class.getName();
    
    private String[] names;
    private Map<String, Long> idsByNames = new HashMap<String, Long>();
    private Map<String, String> typeByNames = new HashMap<String, String>();

    /**
     * 
     * @param aContext used to load resources
     * @param aTextViewResourceId given to {@link ArrayAdapter}
     * @param osmId 
     * @param type 
     */
    public PlaceTagValueAutocompletionAdapter(final Context aContext,
                                       final int aTextViewResourceId,
                                       final StorageDelegator delegator,
                                       final String osmElementType,
                                       final long osmId) {
        super(aContext, aTextViewResourceId);
        Log.d("PlaceTagValuesCompletionAdapter","Constructor ...");
        names = getArray(delegator, getLocation(delegator, osmElementType, osmId));
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
		Map<String, Double> distancesByName = new HashMap<String, Double>();
		String[] nameTags = {Tags.KEY_NAME, Tags.KEY_OFFICIAL_NAME, Tags.KEY_ALT_NAME};
		Log.d("PlaceTagValuesCompletionAdapter","searching for place ways...");
		for (Way way : delegator.getCurrentStorage().getWays()) {
			if (way.getTagWithKey(Tags.KEY_PLACE) != null) {
				double distance = -1D;
				long iD = way.getOsmId();
				
				for (String tag:nameTags) { 
					String name = way.getTagWithKey(tag);
					if (name != null) {
						if (distance == -1D) { // only calc once
							distance = getDistance(way, location);
						}
						if (distancesByName.containsKey(name)) {
							// way already in list - keep shortest distance
							if (distance <  distancesByName.get(name)) {
								distancesByName.put(name, distance);
								idsByNames.put(name,Long.valueOf(iD));
								typeByNames.put(name,Way.NAME);
							}
						} else {
							distancesByName.put(name, distance);
							idsByNames.put(name,Long.valueOf(iD));
							typeByNames.put(name,Way.NAME);
						}
					}
				}
			}
		}
		Log.d("PlaceTagValuesCompletionAdapter","searching for place nodes...");
		for (Node node : delegator.getCurrentStorage().getNodes()) {
			if (node.getTagWithKey(Tags.KEY_PLACE) != null) {
				double distance = -1D;
				long iD = node.getOsmId();

				for (String tag:nameTags) {
					String name = node.getTagWithKey(tag);
					Log.d("PlaceTagValuesCompletionAdapter","adding " + name);
					if (name != null) {
						if (distance == -1D) { // only calc once
							distance = Math.hypot(location[0] - node.getLat(),location[1] - node.getLon());
						}
						if (distancesByName.containsKey(name)) {
							// way already in list - keep shortest distance
							if (distance <  distancesByName.get(name)) {
								distancesByName.put(name, distance);
								idsByNames.put(name,Long.valueOf(iD));
								typeByNames.put(name,Node.NAME);
							}
						} else {
							distancesByName.put(name, distance);
							idsByNames.put(name,Long.valueOf(iD));
							typeByNames.put(name,Node.NAME);
						}
					}
				}
			}
		}
		// sort names by distance
		MultiHashMap<Double, String> retval = new MultiHashMap<Double, String>(true);
		for (String name : distancesByName.keySet()) {
			retval.add(distancesByName.get(name), name);
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

	/**
     * Get the location of the center of the given osm-element
     * @param delegator
     * @param osmElementType
     * @param osmId
     * @return {lat, lon} or null
     */
	static int[] getLocation(final StorageDelegator delegator,
			final String osmElementType, long osmId) {
		OsmElement osmElement = delegator.getOsmElement(osmElementType, osmId);
		if (osmElement instanceof Node) {
			Node n = (Node) osmElement;
			return new int[] {n.getLat(), n.getLon()};
		}
		if (osmElement instanceof Way) {
			de.blau.android.Map map = Application.mainActivity.getMap();
			return Logic.centroid(map.getWidth(), map.getHeight(), map.getViewBox(),(Way)osmElement);
		}
		return null;
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


