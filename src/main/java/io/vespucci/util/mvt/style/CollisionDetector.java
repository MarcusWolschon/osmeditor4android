package io.vespucci.util.mvt.style;

import android.graphics.Rect;
import androidx.annotation.NonNull;

/**
 * Collision detection interface
 * 
 * @author Simon
 *
 */
public interface CollisionDetector {

    /**
     * Resets the current detector
     */
    public void reset();

    /**
     * Check if rect collides with an existing one
     * 
     * If there is no collision adds rect to the exiting ones
     * 
     * @param rect the new Rect
     * @return true if there is a collision
     */
    public boolean collides(@NonNull Rect rect);

    /**
     * Check if the line collides with an existing one
     * 
     * @param start start coordinates
     * @param end end coordinates
     * @param height line height
     * @return true if there is a collision
     */
    public boolean collides(@NonNull float[] start, @NonNull float[] end, float height);
}
