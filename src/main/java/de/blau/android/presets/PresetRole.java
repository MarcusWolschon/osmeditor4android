package de.blau.android.presets;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;

public class PresetRole extends Regionalizable implements Comparable<PresetRole>, Serializable {

    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = PresetRole.class.getSimpleName().substring(0, Math.min(23, PresetRole.class.getSimpleName().length()));

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
     * This role is deprecated
     */
    private boolean deprecated = false;

    /**
     * this role applies to
     */
    private boolean appliesToWay;
    private boolean appliesToNode;
    private boolean appliesToRelation;
    private boolean appliesToClosedWay;
    private boolean appliesToArea;

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
                case PresetParser.CLOSEDWAY:
                    appliesToClosedWay = true;
                    break;
                case PresetParser.MULTIPOLYGON:
                case PresetParser.AREA:
                    appliesToArea = true;
                    break;
                default:
                    unknownType(type);
                }
            }
        }
    }

    /**
     * Copy constructor
     * 
     * @param source PresetField to copy
     */
    public PresetRole(@NonNull PresetRole source) {
        super(source);
        this.role = source.role;
        this.hint = source.hint;
        this.appliesToWay = source.appliesToWay;
        this.appliesToNode = source.appliesToNode;
        this.appliesToRelation = source.appliesToRelation;
        this.appliesToClosedWay = source.appliesToClosedWay;
        this.appliesToArea = source.appliesToArea;
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
     * @return true if deprecated
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * @param deprecated make this field deprecated
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * Test what kind of elements this PresetRole applies to
     * 
     * This version is used if the element hasn't been downloaded
     * 
     * @param type the type of element to check for
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
            default:
                unknownType(type);
            }
        }
        return true;
    }

    /**
     * Log a message when we account an unknown element type
     * 
     * @param type the unknown type
     */
    void unknownType(@NonNull String type) {
        Log.e(DEBUG_TAG, "Unknown element type " + type);
    }

    /**
     * Test what kind of elements this PresetRole applies to
     * 
     * @param type the ElementType to check for
     * @return true if applicable
     */
    public boolean appliesTo(@Nullable ElementType type) {
        if (type != null) {
            switch (type) {
            case NODE:
                return appliesToNode;
            case WAY:
                return appliesToWay;
            case RELATION:
                return appliesToRelation;
            case CLOSEDWAY:
                return appliesToClosedWay;
            case AREA:
                return appliesToArea;
            default:
                unknownType(type.toString());
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
     * @param countString a String containing an int
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
        regexp = regexpString != null && PresetParser.TRUE.equals(regexpString);
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
        if (de.blau.android.util.Util.isEmpty(role)) {
            return hint != null ? hint : "";
        }
        if (hint != null) {
            return role + " - " + hint;
        }
        return role;
    }

    @Override
    public int compareTo(PresetRole another) {
        return role.compareTo(another.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appliesToArea, appliesToClosedWay, appliesToNode, appliesToRelation, appliesToWay, count, deprecated, hint, memberExpression,
                regexp, requisite, role);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PresetRole)) {
            return false;
        }
        PresetRole other = (PresetRole) obj;
        return appliesToArea == other.appliesToArea && appliesToClosedWay == other.appliesToClosedWay && appliesToNode == other.appliesToNode
                && appliesToRelation == other.appliesToRelation && appliesToWay == other.appliesToWay && count == other.count && deprecated == other.deprecated
                && Objects.equals(hint, other.hint) && Objects.equals(memberExpression, other.memberExpression) && regexp == other.regexp
                && requisite == other.requisite && Objects.equals(role, other.role);
    }
}
