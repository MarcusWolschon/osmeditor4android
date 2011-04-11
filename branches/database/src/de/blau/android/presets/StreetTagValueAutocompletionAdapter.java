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
import java.util.Collection;
import java.util.List;
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
    @SuppressWarnings("unused")
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
    	Collection<Way> ways = streets.getWays();
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
		if (location == null) {
			return Integer.MAX_VALUE;
		}
		List<Node> nodes = way.getNodes();
		double distance = Double.MAX_VALUE;
		Node lastNode = null;
		for (Node node : nodes) {
			if (node != null) {
				//int la = Math.abs(location[0] - node.getLat());
				//int lo = Math.abs(location[1] - node.getLon());
				//int d = la + lo;
				// distance to nodes of way
				//distance = Math.min(d, distance);
				if (lastNode != null) {
					// distance to lines of way
					double d2 = Math.sqrt(ptSegDistSq(lastNode.getLat(), lastNode.getLon(),
							                      node.getLat(), node.getLon(),
							                      location[0], location[1]));
					distance = Math.min(d2, distance);
				}
				lastNode = node;
			}
		}
		return distance;
	}

	public static double ptSegDistSq(double x1, double y1, double x2,
			double y2, double px, double py) {
		/*
		 * A = (x2 - x1, y2 - y1) P = (px - x1, py - y1)
		 */
		x2 -= x1; // A = (x2, y2)
		y2 -= y1;
		px -= x1; // P = (px, py)
		py -= y1;
		double dist;
		if (px * x2 + py * y2 <= 0.0) { // P*A
			dist = px * px + py * py;
		} else {
			px = x2 - px; // P = A - P = (x2 - px, y2 - py)
			py = y2 - py;
			if (px * x2 + py * y2 <= 0.0) { // P*A
				dist = px * px + py * py;
			} else {
				dist = px * y2 - py * x2;
				dist = dist * dist / (x2 * x2 + y2 * y2); // pxA/|A|
			}
		}
		if (dist < 0) {
			dist = 0;
		}
		return dist;
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


