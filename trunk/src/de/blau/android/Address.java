package de.blau.android;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import android.util.Log;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.util.GeoMath;

/**
 * Store coordinates and address information for use in address prediction
 * @author simon
 *
 */
public class Address implements Serializable {
	private static final long serialVersionUID = 2L;
	public enum Side {
		LEFT,
		RIGHT,
		UNKNOWN
	}
	Side side = Side.UNKNOWN;
	float lat;
	float lon;
	LinkedHashMap<String, String> tags;
	
	Address(Address a) {
		side = a.side;
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
		this.tags = new LinkedHashMap<String, String>(tags);
	}
	
	/**
	 * Set which side this of the road this address is on
	 * @param wayId
	 * @return
	 */
	void setSide(long wayId) {
		side = Side.UNKNOWN;
		Way w = (Way)Main.logic.delegator.getOsmElement(Way.NAME,wayId);
		if (w == null) {
			return;
		}
		double distance = Double.MAX_VALUE;
		
		// to avoid rounding errors we translate the bb to 0,0
		BoundingBox bb = Application.mainActivity.getMap().getViewBox();
		double latOffset = GeoMath.latE7ToMercatorE7(bb.getBottom());
		double lonOffset = bb.getLeft();
		double ny = GeoMath.latToMercator(lat)-latOffset/1E7D;
		double nx = lon - lonOffset/1E7D;
		
		ArrayList<Node> nodes = new ArrayList<Node>(w.getNodes());
		for (int i = 0;i <= nodes.size()-2;i++) {
			double bx = (nodes.get(i).getLon()-lonOffset)/1E7D;
			double by = (GeoMath.latE7ToMercatorE7(nodes.get(i).getLat())-latOffset )/1E7D;
			double ax = (nodes.get(i+1).getLon()-lonOffset)/1E7D;
			double ay = (GeoMath.latE7ToMercatorE7(nodes.get(i+1).getLat())-latOffset)/1E7D;
			float[] closest = GeoMath.closestPoint((float)nx, (float)ny, (float)bx, (float)by, (float)ax, (float)ay);
			double newDistance = GeoMath.haversineDistance(nx, ny, closest[0], closest[1]);
			if (newDistance < distance) {
				distance = newDistance;
				double determinant = (bx-ax)*(ny-ay) - (by-ay)*(nx-ax);
				if (determinant < 0) {
					side = Side.LEFT;
				} else if (determinant > 0) {
					side = Side.RIGHT;
				}
			}
		}
	}
	
	Side getSide() {
		return side;
	}
}
