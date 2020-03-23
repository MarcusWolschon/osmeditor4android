package de.blau.android.osm;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/*
 * RelationMemberDescription is an extended version of RelationMember that includes the position in the member list
 */
public class RelationMemberPosition implements Serializable {
    private static final long    serialVersionUID = 1104911642016294269L;
    private final RelationMember member;
    private int                  position         = 0;

    /**
     * Construct a RelationMemberDescription from a RelationMember
     * 
     * @param rm the RelationMember to use
     * @param position the position in the list of members
     */
    public RelationMemberPosition(@NonNull final RelationMember rm, int position) {
        member = rm;
        this.position = position;
    }

    /**
     * Get the RelationMember
     * 
     * @return the RelationMember
     */
    @NonNull
    public RelationMember getRelationMember() {
        return member;
    }

    /**
     * Get the role of this relation member
     * 
     * @return the role or null if not set
     */
    @Nullable
    public String getRole() {
        return member.getRole();
    }

    /**
     * Set the role for the element
     * 
     * @param role the new role to set
     */
    public void setRole(@Nullable final String role) {
        member.setRole(role);
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
        if (!(o instanceof RelationMemberPosition)) {
            return false;
        }
        RelationMember other = ((RelationMemberPosition) o).member;
        if (!member.getType().equals(other.getType()) || member.getRef() != other.getRef()) {
            return false;
        }
        String memberRole = member.getRole();
        String otherRole = other.getRole();
        if (memberRole != null && !memberRole.equals(otherRole)) {
            return false;
        }
        if (memberRole == null && otherRole != null) {
            return false;
        }
        return position == ((RelationMemberPosition) o).position;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + member.hashCode();
        result = 37 * result + position;
        return result;
    }
}
