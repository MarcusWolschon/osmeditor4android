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
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import de.blau.android.TagEditor;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;


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
    private static final String DEBUG_TAG = StreetTagValueAutocompletionAdapter.class.getName();

    /**
     * 
     * @param aContext used to load resources
     * @param aTextViewResourceId given to {@link ArrayAdapter}
     * @param osmId 
     * @param type 
     * @throws ParserConfigurationException if we cannot parse presets.xml
     * @throws SAXException if we cannot parse presets.xml
     * @throws FactoryConfigurationError if we cannot parse presets.xml
     * @throws IOException if we cannot parse presets.xml
     */
    public StreetTagValueAutocompletionAdapter(final Context aContext,
                                       final int aTextViewResourceId,
                                       final StorageDelegator streets,
                                       final String osmElementType,
                                       final long osmId) throws ParserConfigurationException, SAXException, FactoryConfigurationError, IOException {
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
    	SortedMap<Integer, String> retval = new TreeMap<Integer, String>();
    	for (Way way : ways) {
			if (way.getTagWithKey("highway") == null) {
				continue;
			}
			String name = way.getTagWithKey("name");
			if (name == null) {
				continue;
			}

			if (!retval.containsValue(name)) {
				int distance = getDistance(streets, way, location);
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
	private static int getDistance(StorageDelegator streets, Way way, int[] location) {
		if (location == null) {
			return Integer.MAX_VALUE;
		}
		List<Node> nodes = way.getNodes();
		int distance = Integer.MAX_VALUE;
		for (Node node : nodes) {
			if (node != null) {
				int la = Math.abs(location[0] - node.getLat());
				int lo = Math.abs(location[1] - node.getLon());
				int d = la + lo;
				distance = Math.min(d, distance);
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
		return null;
	}

}


