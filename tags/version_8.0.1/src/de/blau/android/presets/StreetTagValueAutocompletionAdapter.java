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
import java.util.List;
import java.util.TreeMap;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import de.blau.android.TagEditor;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.util.GeoMath;


/**
 * Project: OSMEditor<br/>
 * TagKeyAutocompletionAdapter.java<br/>
 * created: 12.06.2010 10:43:37 <br/>
 *<br/><br/>
 * <b>Adapter for the {@link AutoCompleteTextView} in the {@link TagEditor}
 * that is for the VALUE  for the key "addr:street" .</a>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class StreetTagValueAutocompletionAdapter extends ArrayAdapter<String> {

    /**
     * The tag we use for Android-logging.
     */
    @SuppressWarnings("unused")
	private static final String DEBUG_TAG = StreetTagValueAutocompletionAdapter.class.getName();

    /**
     * 
     * @param aContext used to load resources
     * @param aTextViewResourceId given to {@link ArrayAdapter}
     * @param osmId 
     * @param type 
     */
    public StreetTagValueAutocompletionAdapter(final Context aContext,
                                       final int aTextViewResourceId,
                                       final StorageDelegator streets,
                                       final String osmElementType,
                                       final long osmId) {
        super(aContext, aTextViewResourceId, getArray(streets, aContext, getLocation(streets, osmElementType, osmId)));
    }

    
    /**
     * Get all street-names in the area
     * @param streets
     * @param aContext
     * @param location
     * @return all street-names
     */
    private static String[] getArray(final StorageDelegator streets,
    		final Context aContext, final int[] location) {
    	List<Way> ways = streets.getCurrentStorage().getWays();
    	TreeMap<Double, String> retval = new TreeMap<Double, String>();
    	for (Way way : ways) {
			if (way.getTagWithKey("highway") == null) {
				continue;
			}
			String name = way.getTagWithKey("name");
			if (name == null) {
				continue;
			}

			if (!retval.containsValue(name)) {
				double distance = getDistance(streets, way, location);

				// other way with the same name but different distance
				for (Way way2 : ways) {
					if (way2.getTagWithKey("highway") == null) {
						continue;
					}
					String name2 = way2.getTagWithKey("name");
					if (name2 == null || !name2.equalsIgnoreCase(name)) {
						continue;
					}
					distance = Math.min(distance, getDistance(streets, way2, location));
				}
				retval.put(distance, name);
			}
		}
		return retval.values().toArray(new String[retval.size()]);
	}

    /**
     * @param streets
     * @param way
     * @param location
     * @return the minimum distance of the given way to the given location
     */
	private static double getDistance(final StorageDelegator streets, final Way way, final int[] location) {
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
     * @param streets
     * @param osmElementType
     * @param osmId
     * @return {lat, lon} or null
     */
	private static int[] getLocation(final StorageDelegator streets,
			final String osmElementType, long osmId) {
		OsmElement osmElement = streets.getOsmElement(osmElementType, osmId);
		if (osmElement instanceof Node) {
			Node n = (Node) osmElement;
			return new int[] {n.getLat(), n.getLon()};
		}
		if (osmElement instanceof Way) {
			Way w = (Way) osmElement;
			int max = w.getNodes().size();
			Node n = w.getNodes().get(max / 2); // take a node from the middle
			return new int[] {n.getLat(), n.getLon()};
		}
		return null;
	}

}


