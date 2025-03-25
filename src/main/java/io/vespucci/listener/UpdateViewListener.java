package io.vespucci.listener;

/**
 * Callback to update a view held by something else
 * 
 * @author Simon Poole
 *
 */
public interface UpdateViewListener {

    /**
     * Call this to update a relevant view after an operation
     */
    void update();
}
