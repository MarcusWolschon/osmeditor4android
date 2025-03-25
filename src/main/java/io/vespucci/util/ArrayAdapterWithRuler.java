package io.vespucci.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import io.vespucci.R;

public class ArrayAdapterWithRuler<T> extends ArrayAdapter<T> {
    private final Class<?>       marker;
    private final LayoutInflater inflater;

    /**
     * Construct an array adapter that supports a horizontal ruler
     * 
     * @param context an Android Context
     * @param resource resource id of a layout to use, note this must contain a TextView with id "text1"
     * @param marker class that will be used as a marker where a Ruler should be used
     */
    public ArrayAdapterWithRuler(@NonNull Context context, int resource, @NonNull Class<?> marker) {
        super(context, resource, R.id.text1);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.marker = marker;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return !marker.isInstance(getItem(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (marker.isInstance(getItem(position))) {
            return inflater.inflate(R.layout.ruler_row, null);
        }
        if (convertView instanceof LinearLayout) {
            convertView = null;
        }
        return super.getView(position, convertView, parent);
    }
}
