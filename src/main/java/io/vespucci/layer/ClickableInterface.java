package io.vespucci.layer;

import java.util.List;

import android.content.Context;
import android.text.SpannableString;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.osm.ViewBox;

public interface ClickableInterface<V> {

    /**
     * Get objects near the screen coordinates
     * 
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @param viewBox current ViewBox
     * @return a List of the objects
     */
    List<V> getClicked(float x, float y, @NonNull ViewBox viewBox);

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
     * @return the currently selected object or null if there is none
     */
    @Nullable
    V getSelected();

    /**
     * Set the selected object
     * 
     * This is not expected to cause any external action to happen
     * 
     * @param o the object to select
     */
    void setSelected(V o);

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
    @NonNull
    SpannableString getDescription(@NonNull final V object);

    /**
     * Get a short description of the object suitable for a menu
     * 
     * @param context an Android Context
     * @param object the Object from this layer
     * @return the description
     */
    @NonNull
    SpannableString getDescription(@NonNull Context context, @NonNull final V object);
}
