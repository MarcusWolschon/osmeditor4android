package de.blau.android.propertyeditor;

import de.blau.android.osm.OsmElement.ElementType;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
interface PresetFilterUpdate {
	/**
	 * Fetch new tags etc
	 */
	void typeUpdated(ElementType type);
}

