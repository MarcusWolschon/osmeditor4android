package io.vespucci.osm;

import java.util.List;
import java.util.SortedMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface OsmElementInterface {
    /**
     * @return the id of the object (&lt; 0 are temporary ids)
     */
    public long getOsmId();

    /**
     * @return the version of the object
     */
    public long getOsmVersion();

    /**
     * Get the current tags of the element
     * 
     * @return an unmodifiable map containing the tags
     */
    @NonNull
    public SortedMap<String, String> getTags();

    /**
     * Get the relations this element is a member of
     * 
     * @return a List of the relations, null if none
     */
    @Nullable
    public List<Relation> getParentRelations();

    /**
     * Get the state of this element
     * 
     * @return the state value
     */
    public byte getState();
    
    /**
     * Get the timestamp for this object
     * 
     * @return seconds since the Unix Epoch. negative if no value is set
     */
    public long getTimestamp();
}
