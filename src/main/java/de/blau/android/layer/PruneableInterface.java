package de.blau.android.layer;

public interface PruneableInterface {

    /**
     * Remove data outside of the current ViewBox from this layer
     */
    void prune();
    
    /**
     * Check if auto prune is turned on for this layer
     * 
     * @return true is enabled
     */
    boolean autoPrune();
    
    /**
     * Set the auto prune state
     * 
     * @param enable if true enable auto prune for this layer
     */
    void setAutoPrune(boolean enable);
}
