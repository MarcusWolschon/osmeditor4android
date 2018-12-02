package de.blau.android.presets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;

public class PresetRole implements Comparable<PresetRole> {
    /**
     * Role this is for
     */
    private final String role;

    /**
     * Hint to be displayed in a suitable form
     */
    private String hint;

    /**
     * this role applies to
     */
    private boolean appliesToWay;
    private boolean appliesToNode;
    private boolean appliesToRelation;

    /**
     * Constructor
     * 
     * @param role the role for this PresetRole
     * @param hint short description of the role
     * @param types comma separated list of OSM element types thir role applies to
     */
    public PresetRole(@NonNull String role, @Nullable String hint, @Nullable String types) {
        this.role = role;
        this.hint = hint;
        if (types == null) {
            // Type not specified, assume all types
            appliesToNode = true;
            appliesToWay = true;
            appliesToRelation = true;
        } else {
            String[] typesArray = types.split(",");
            for (String type : typesArray) {
                switch (type.trim()) {
                case Node.NAME:
                    appliesToNode = true;
                    break;
                case Way.NAME:
                    appliesToWay = true;
                    break;
                case Relation.NAME:
                    appliesToRelation = true;
                    break;
                }
            }
        }
    }

    /**
     * Copy constructor
     * 
     * @param field PresetField to copy
     */
    public PresetRole(@NonNull PresetRole field) {
        this.role = field.role;
        this.hint = field.hint;
        this.appliesToWay = field.appliesToWay;
        this.appliesToNode = field.appliesToNode;
        this.appliesToRelation = field.appliesToRelation;
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
     * Set the hint aka description for this role
     * 
     * @param hint the hint
     */
    public void setHint(@Nullable String hint) {
        this.hint = hint;
    }

    /**
     * Test what kind of elements this PresetRole applies to
     * 
     * @param type the ElementType to check for
     * @return true if applicable
     */
    public boolean appliesTo(@Nullable String type) {
        if (type != null) {
            switch (type) {
            case Node.NAME:
                return appliesToNode;
            case Way.NAME:
                return appliesToWay;
            case Relation.NAME:
                return appliesToRelation;
            }
        }
        return true;
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
