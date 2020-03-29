package de.blau.android.presets;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;

public class PresetRole implements Comparable<PresetRole> {

    private static final String DEBUG_TAG = PresetRole.class.getSimpleName();

    enum Requisite {
        OPTIONAL, REQUIRED
    }

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

    private String memberExpression;

    private Requisite requisite;
    private int       count;
    private boolean   regexp;

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

    /**
     * Set an member expression that indicates which elements can match in JOSM filter syntax
     * 
     * @param memberExpression the member expression
     */
    public void setMemberExpression(@Nullable String memberExpression) {
        this.memberExpression = memberExpression;
    }

    /**
     * Get any member expression
     * 
     * @return the member expression or null if none
     */
    @Nullable
    public String getMemberExpression() {
        return memberExpression;
    }

    /**
     * Set if this role is required or not
     * 
     * @param requisiteString "optional" or "required"
     */
    public void setRequisite(@Nullable String requisiteString) {
        if (requisiteString != null) {
            Requisite.valueOf(requisiteString.toUpperCase(Locale.US));
        } else {
            requisite = null;
        }
    }

    /**
     * Get the requisite value
     * 
     * @return OPTIONAL, REQUIRED or null if not set
     */
    @Nullable
    public Requisite getRequisite() {
        return requisite;
    }

    /**
     * Set the number of times this role can be present in a Relation
     * 
     * @param countString a String containg an int
     */
    public void setCount(@Nullable String countString) {
        if (countString != null) {
            try {
                count = Integer.parseInt(countString);
            } catch (NumberFormatException e) {
                Log.e(DEBUG_TAG, "Invalid count value " + countString);
                count = 0;
            }
        } else {
            count = 0;
        }
    }

    /**
     * Get the maximum number of times this role can occur
     * 
     * @return maximum number of times this role can occur, 0 = unlimited
     */
    public int getCount() {
        return count;
    }

    /**
     * Set if the role String is an regexp or not
     * 
     * We only use this to ignore the role for now
     * 
     * @param regexpString "true" if the role is an regexp
     */
    public void setRegexp(@Nullable String regexpString) {
        regexp = regexpString != null && Preset.TRUE.equals(regexpString);
    }

    /**
     * Check if the role is actually a regexp
     * 
     * @return true if the role is a regexp
     */
    public boolean isRegexp() {
        return regexp;
    }

    @Override
    public String toString() {
        return role + (hint != null ? (role == null || "".equals(role) ? "" : " - ") + hint : "");
    }

    @Override
    public int compareTo(PresetRole another) {
        return role.compareTo(another.role);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PresetRole other = (PresetRole) obj;
        if (role == null) {
            if (other.role != null) {
                return false;
            }
        } else if (!role.equals(other.role)) {
            return false;
        }
        return true;
    }
}
