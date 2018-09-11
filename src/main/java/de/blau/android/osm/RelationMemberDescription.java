package de.blau.android.osm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.App;

/*
 * RelationMemberDescription is an extended version of RelationMember that holds a textual description of the element
 * instead of the element itself
 */
public class RelationMemberDescription extends RelationMember {
    private static final long serialVersionUID = 1104911642016294269L;
    private String            description      = null;
    private boolean           downloaded       = false;
    private int               position         = 0;                   // only used for sorting

    /**
     * Construct a RelationMemberDescription from a RelationMember
     * 
     * @param rm the RelationMember to use
     */
    public RelationMemberDescription(@NonNull final RelationMember rm) {
        super(rm.getElement() != null ? rm.getElement().getName() : rm.getType(), rm.getElement() != null ? rm.getElement().getOsmId() : rm.getRef(),
                rm.getRole());
        OsmElement e = rm.getElement();
        if (e != null) {
            description = e.getDescription(false);
            downloaded = true;
        } else {
            description = "#" + ref;
        }
    }

    /**
     * Construct a new RelationMemberDescriptio
     * 
     * @param type the type of the member OsmElement (NODE, WAY, RELATION) as a String
     * @param id the OSM id of the OsmElement
     * @param role the role in the Relation
     * @param description a description of the element
     */
    public RelationMemberDescription(@NonNull final String type, final long id, @Nullable final String role, @Nullable final String description) {
        super(type, id, role);
        this.description = description;
    }

    /**
     * Get the description for this object if any
     * 
     * @return a String with the description of null if none
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Check if the OsmElement we are referring to is downloaded or not
     * 
     * @return true if downloaded
     */
    public boolean downloaded() {
        return downloaded;
    }

    /**
     * If an downloaded element is present update description and downloaded status
     */
    public void update() {
        OsmElement e = getElement();
        if (e != null) {
            description = e.getDescription(false);
            downloaded = true;
        } else {
            downloaded = false;
        }
    }

    /**
     * This returns (if present), the element directly from storage
     */
    @Override
    public OsmElement getElement() {
        return super.getElement() == null ? App.getDelegator().getOsmElement(getType(), getRef()) : super.getElement();
    }

    /**
     * Get the position of the member in the Relation
     * 
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Set the position of the member in the Relation
     * 
     * @param postiion the position to set
     */
    public void setPosition(int postiion) {
        this.position = postiion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof RelationMemberDescription && ref == ((RelationMemberDescription) o).ref && type.equals(((RelationMemberDescription) o).type)
                && ((role == null && ((RelationMemberDescription) o).role == null) || (role != null && role.equals(((RelationMemberDescription) o).role)))
                && position == ((RelationMemberDescription) o).position;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (int) (ref ^ (ref >>> 32));
        result = 37 * result + (type == null ? 0 : type.hashCode());
        result = 37 * result + (role == null ? 0 : role.hashCode());
        result = 37 * result + position;
        return result;
    }
}
