package de.blau.android.layer;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import de.blau.android.osm.ViewBox;

public interface ClickableInterface<V> {

    /**
     * Get objects near the screen coordinates
     * 
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @param viewBox current ViewBox
     * @return a List of the objects
     */
    List<?> getClicked(float x, float y, @NonNull ViewBox viewBox);

    /**
     * Do whatever should be done on the object if it is selected
     * 
     * @param activity calling Activity
     * @param object the Object from this layer
     */
    void onSelected(@NonNull FragmentActivity activity, @NonNull final V object);

    /**
     * Get the selected object if any
     * 
     * @return the currently selected object or null if their is none
     */
    @Nullable
    V getSelected();

    /**
     * De-select any selected objects
     */
    void deselectObjects();

    /**
     * Get a short description of the object suitable for a menu
     * 
     * @param object the Object from this layer
     * @return the description
     */
    String getDescription(@NonNull final V object);
}
