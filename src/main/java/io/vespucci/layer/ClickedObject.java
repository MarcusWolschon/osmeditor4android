package io.vespucci.layer;

import android.content.Context;
import android.text.SpannableString;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public class ClickedObject<V> {
    private final ClickableInterface<V> layer;
    private final V                     object;

    /**
     * Construct a new container for objects that were clicked on a layer
     * 
     * @param layer the layer the object is on
     * @param object the object
     */
    public ClickedObject(@NonNull ClickableInterface<V> layer, @NonNull V object) {
        this.layer = layer;
        this.object = object;
    }

    /**
     * Do something when this is selected
     */
    public void onSelected(@NonNull FragmentActivity activity) {
        layer.onSelected(activity, object);
    }

    /**
     * Get a description of the object
     *
     * @return the description
     */
    public SpannableString getDescription() {
        return layer.getDescription(object);
    }

    /**
     * Get a description of the object
     *
     * @return the description
     */
    public SpannableString getDescription(@NonNull Context context) {
        return layer.getDescription(context, object);
    }

    /**
     * @return the object
     */
    @NonNull
    public V getObject() {
        return object;
    }

    /**
     * @return the layer
     */
    public ClickableInterface<V> getLayer() {
        return layer;
    }

}
