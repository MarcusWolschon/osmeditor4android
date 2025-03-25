package io.vespucci.propertyeditor;

/**
 * Determine the how empty value fields should be treated when applying a preset
 */
public enum Prefill {
    /**
     * Don't prefill tag value
     */
    NEVER, 
    /**
     * Use preset default or useLastAsDefault value to determine if the tag value should be prefilled
     */
    PRESET, 
    /**
     * If available  use the last used value 
     */
    FORCE_LAST 
}
