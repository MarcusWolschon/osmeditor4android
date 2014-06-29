package de.blau.android;

import java.io.Serializable;
import java.util.LinkedHashMap;

import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;

/**
 * Store coordinates and address information for reuse
 * @author simon
 *
 */
public class Address implements Serializable {
	private static final long serialVersionUID = 1L;
	float lat;
	float lon;
	LinkedHashMap<String, String> tags;
	
	Address(Address a) {
		lat = a.lat;
		lon = a.lon;
		tags = new LinkedHashMap<String, String>(a.tags);
	}
	
	Address() {
		tags = new LinkedHashMap<String, String>();
	}
	
	Address(String type, long id, LinkedHashMap<String, String> tags) {
		OsmElement e = Main.logic.delegator.getOsmElement(type, id);
		switch (e.getType()) {
		case NODE: lat = ((Node)e).getLat()/1E7F; lon = ((Node)e).getLon()/1E7F; break;
		case WAY:
		case CLOSEDWAY:
			de.blau.android.Map map = Application.mainActivity.getMap();
			int[] center = Logic.centroid(map.getWidth(), map.getHeight(), map.getViewBox(), (Way)e);
			lat = center[0]/1E7F;
			lon = center[1]/1E7F;
			break;
		case RELATION:
			// doing nothing is probably best for now
		default:
			break;
		}
		this.tags = tags;
	}
}
