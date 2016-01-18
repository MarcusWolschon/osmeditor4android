package de.blau.android.propertyeditor;

/**
 * Interface for updating key:value pairs in the TagEditor from other fragments via the activity
 */
abstract interface FormUpdate {
	/**
	 * Fetch new tags etc
	 */
	abstract void tagsUpdated();
}

