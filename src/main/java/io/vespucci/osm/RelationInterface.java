package io.vespucci.osm;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface RelationInterface {

    /**
     * Return complete list of relation members
     * 
     * @return list of members, or null if there are none
     */
    @Nullable
    public List<RelationMember> getMembers();
    
    /**
     * Return all relation member elements for this OSM element
     * 
     * @param e OsmElement to search for
     * @return the list of corresponding RelationMembers, empty if non found
     */
    @NonNull
    public List<RelationMember> getAllMembers(@NonNull OsmElement e);
}
