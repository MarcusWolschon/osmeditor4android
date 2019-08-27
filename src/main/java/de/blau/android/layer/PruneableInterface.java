package de.blau.android.layer;

public interface PruneableInterface {

    /**
     * Remove data outside of the current ViewBox from this layer
     */
    void prune();
}
