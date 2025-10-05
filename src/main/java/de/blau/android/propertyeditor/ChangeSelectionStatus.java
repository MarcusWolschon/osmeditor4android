package de.blau.android.propertyeditor;

interface ChangeSelectionStatus {
    /**
     * Set the selected status
     * 
     * @param current the current status
     * @return the new status
     */
    boolean set(boolean current);
}
