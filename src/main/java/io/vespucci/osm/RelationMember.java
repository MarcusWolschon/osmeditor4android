package io.vespucci.osm;

import java.io.Serializable;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * RelationMember stores the necessary information for a relation member, if the element field is null the element
 * itself is not present (not downloaded typically) and only the osm id, type (needed to make the id unique) and role
 * fields are stored.
 * 
 * @author simon
 *
 */
public class RelationMember implements Serializable {
    private static final long serialVersionUID = 6L;

    String             type;
    long               ref;
    String             role    = null;
    private OsmElement element = null;

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
        type = e.getName();
        ref = e.getOsmId();
    }

    /**
     * Constructor for copying, assumes that only role changes
     * 
     * @param rm a RelationMember instance
     */
    public RelationMember(@NonNull final RelationMember rm) {
        type = rm.type;
        ref = rm.ref;
        role = rm.role;
        if (rm.element != null) {
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
        return type;
    }

    /**
     * Get the OSM id of the element
     * 
     * @return the OSM id
     */
    public long getRef() {
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
    public synchronized OsmElement getElement() {
        return element;
    }

    /**
     * Set the element, mainly used for post processing relations
     * 
     * @param e the OsmElement
     */
    public synchronized void setElement(@Nullable final OsmElement e) {
        element = e;
        if (e != null) {
            ref = e.getOsmId();
            type = e.getName();
        }
    }

    /**
     * Check if the OsmElement we are referring to is downloaded or not
     * 
     * @return true if downloaded
     */
    public synchronized boolean downloaded() {
        return element != null;
    }

    @Override
    public String toString() {
        return role + " " + type + " " + ref;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, role, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RelationMember)) {
            return false;
        }
        RelationMember other = (RelationMember) obj;
        return ref == other.ref && Objects.equals(role, other.role) && Objects.equals(type, other.type);
    }
}
