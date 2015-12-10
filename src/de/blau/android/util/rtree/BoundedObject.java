package de.blau.android.util.rtree;

import de.blau.android.osm.BoundingBox;

/**
 * An object bounded on an Axis-Aligned Bounding Box.
 * @author Colonel32
 * @author cnvandev
 */
public interface BoundedObject {
	public BoundingBox getBounds();
}
