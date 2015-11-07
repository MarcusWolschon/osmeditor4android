package de.blau.android.util.rtree;

import android.graphics.Rect;

/**
 * An object bounded on an Axis-Aligned Bounding Box.
 * @author Colonel32
 * @author cnvandev
 */
public interface BoundedObject {
	public Rect getBounds();
}
