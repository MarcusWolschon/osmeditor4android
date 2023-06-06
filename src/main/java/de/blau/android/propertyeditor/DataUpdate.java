package de.blau.android.propertyeditor;

public interface DataUpdate {

    /**
     * Called when the OSM data may have changed and we might need to update
     */
    abstract void onDataUpdate();
}
