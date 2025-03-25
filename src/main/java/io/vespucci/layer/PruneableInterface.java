package io.vespucci.layer;

public interface PruneableInterface {

    /**
     * Remove data outside of the current ViewBox from this layer
     */
    void prune();
}
