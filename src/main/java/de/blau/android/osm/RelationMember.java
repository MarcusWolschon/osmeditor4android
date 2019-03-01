package de.blau.android.osm;

import java.io.Serializable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * RelationMember stores the necessary information for a relation member, if the element field is null the element
 * itself is not present (not downloaded typically) and only the osm id, type (needed to make the id unique) and role
 * fields are stored.
 * 
 * @author simon
 *
 */
public class RelationMember implements Serializable {

    private static final long serialVersionUID = 4L;
    String                    type             = null;
    long                      ref              = Long.MIN_VALUE;
    String                    role             = null;
    private OsmElement        element          = null;

    /**
     * Constructor for members that have not been downloaded
     * 
     * @param t type of the member OsmElement
     * @param id the OSM id
     * @param r the role of the element
     */
    public RelationMember(@NonNull final String t, final long id, @Nullable final String r) {
        type = t;
        ref = id;
        role = r;
    }

    /**
     * Constructor for members that have been downloaded
     * 
     * @param r the role of the element
     * @param e the OsmElement
     */
    public RelationMember(@Nullable final String r, @NonNull final OsmElement e) {
        role = r;
        element = e;
    }

    /**
     * Constructor for copying, assumes that only role changes
     * 
     * @param rm a RelationMember instance
     */
    public RelationMember(@NonNull final RelationMember rm) {
        if (rm.element == null) {
            type = rm.type;
            ref = rm.ref;
            role = rm.role;
        } else {
            role = rm.role;
            element = rm.element;
        }
    }

    /**
     * Get the OsmElement type
     * 
     * @return the type (NODE, WAY, RELATION) as a String
     */
    @NonNull
    public String getType() {
        if (element != null) {
            return element.getName();
        }
        return type;
    }

    /**
     * Get the OSM id of the element
     * 
     * @return the OSM id
     */
    public long getRef() {
        if (element != null) {
            return element.getOsmId();
        }
        return ref;
    }

    /**
     * Get the role of this relation member
     * 
     * @return the role or null if not set
     */
    @Nullable
    public String getRole() {
        return role;
    }

    /**
     * Set the role for the element
     * 
     * @param role the new role to set
     */
    public void setRole(@Nullable final String role) {
        this.role = role;
    }

    /**
     * @return the element if downloaded, null if it isn't
     */
    @Nullable
    public OsmElement getElement() {
        return element;
    }

    /**
     * set the element, used for post processing relations
     * 
     * @param e the OsmElement
     */
    public void setElement(final OsmElement e) {
        element = e;
    }

    /**
     * Check if the OsmElement we are referring to is downloaded or not
     * 
     * @return true if downloaded
     */
    public boolean downloaded() {
        return element != null;
    }
    
    @Override
    public String toString() {
        return role + " " + type + " " + ref;
    }
}
