package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.presets.Preset.MatchType;
import de.blau.android.presets.Preset.ValueType;
import de.blau.android.util.StringWithDescription;

public class PresetRole implements Comparable<PresetRole> {
    /**
     * Role this is for
     */
    final String role;

    /**
     * Hint to be displayed in a suitable form
     */
    private String hint;

    /**
     * Constructor
     * 
     * @param role the role for this PresetRole
     * @param hint short description of the role
     */
    public PresetRole(@NonNull String role, @Nullable String hint) {
        this.role = role;
        this.hint = hint;
    }

    /**
     * Copy constructor
     * 
     * @param field PresetField to copy
     */
    public PresetRole(@NonNull PresetRole field) {
        this.role = field.role;
        this.hint = field.hint;
    }

    /**
     * Get the role
     * 
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * @return the hint
     */
    @Nullable
    public String getHint() {
        return hint;
    }

    /**
     * @param hint the hint to set
     */
    void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    @Override
    public String toString() {
        return role + (hint != null ? (role == null || "".equals(role) ? "" : " - ") + hint : "");
    }

    @Override
    public int compareTo(PresetRole another) {
        return role.compareTo(another.role);
    }
}
