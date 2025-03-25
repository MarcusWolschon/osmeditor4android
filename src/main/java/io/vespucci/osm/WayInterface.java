package io.vespucci.osm;

import java.util.List;

import androidx.annotation.NonNull;

public interface WayInterface {
    
    /**
     * Return list of all nodes in a way
     * 
     * @return a List of Nodes
     */
    @NonNull
    public List<Node> getNodes();
    
    /**
     * Return the number of nodes in the is way
     * 
     * @return the number of nodes in this Way
     */
    public int nodeCount();
    
    /**
     * return true if first == last node, will not work for broken geometries
     * 
     * @return true if closed
     */
    public boolean isClosed();
    
    /**
     * Return the length in m
     * 
     * This uses the Haversine distance between nodes for calculation
     * 
     * @return the length in m
     */
    public double length();
}
