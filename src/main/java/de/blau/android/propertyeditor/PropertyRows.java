package de.blau.android.propertyeditor;

interface PropertyRows {

    /**
     * Deselect the header check box
     */
    void deselectHeaderCheckBox();

    /**
     * Deselect this row
     */
    void deselectRow();

    /**
     * Select all rows
     */
    void selectAllRows();

    /**
     * Deselect all rows
     */
    void deselectAllRows();

    /**
     * Invert which rows are selected
     */
    void invertSelectedRows();
}
