package de.blau.android.osm;

import java.io.IOException;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;

/*
 * RelationMemberDescription is an extended version of RelationMember that holds a textual description of the element
 * instead of the element itself
 */
public class RelationMemberDescription extends RelationMember {
    private static final long serialVersionUID = 1104911642016294270L;
    private String            description      = null;
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
        } else {
            description = "#" + ref;
        }
    }

    /**
     * Construct a new RelationMemberDescription
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
     * Construct a new RelationMemberDescription from an existing one
     * 
     * @param rmd the existing RelationMemberDescription
     */
    public RelationMemberDescription(@NonNull final RelationMemberDescription rmd) {
        super(rmd.getType(), rmd.getRef(), rmd.getRole());
        description = rmd.description;
        position = rmd.position;
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

    @Override
    public synchronized boolean downloaded() {
        return getElement() != null;
    }

    /**
     * If an downloaded element is present update description and downloaded status
     */
    public void update() {
        OsmElement e = getElement();
        if (e != null) {
            description = e.getDescription(false);
        }
    }

    /**
     * This returns (if present), the element directly from storage or from the saved reference
     */
    @Override
    public synchronized OsmElement getElement() {
        if (super.getElement() == null) {
            super.setElement(App.getDelegator().getOsmElement(getType(), getRef()));
        }
        return super.getElement();
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
     * @param postion the position to set
     */
    public void setPosition(int postion) {
        this.position = postion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RelationMemberDescription)) {
            return false;
        }
        final RelationMemberDescription other = (RelationMemberDescription) o;
        return ref == other.ref && type.equals(other.type) && Objects.equals(role, other.role)
                && position == other.position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, type, role, position);
    }

    /**
     * Serialize this object
     * 
     * @param out ObjectOutputStream to write to
     * @throws IOException if writing fails
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        super.setElement(null); // don't save the actual object
        out.defaultWriteObject();
    }

    @Override
    public String toString() {
        return super.toString() + " " + position;
    }
}
