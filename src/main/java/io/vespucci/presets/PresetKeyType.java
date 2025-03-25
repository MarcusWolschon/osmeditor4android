package io.vespucci.presets;

public enum PresetKeyType {
    /**
     * arbitrary single value
     */
    TEXT,
    /**
     * multiple values, single select
     */
    COMBO,
    /**
     * multiple values, multiple select
     */
    MULTISELECT,
    /**
     * single value, set or unset
     */
    CHECK
}
