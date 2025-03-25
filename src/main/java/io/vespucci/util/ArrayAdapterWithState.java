package io.vespucci.util;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import io.vespucci.R;

public class ArrayAdapterWithState<T extends Enabled> extends ArrayAdapter<T> {

    private final int disabledColor;
    private final int normalColor;

    /**
     * Construct an array adapter that supports enabled/disabled state of items
     * 
     * @param context an Android Context
     * @param resource resource id of a layout to use, note this must contain a TextView with id "text1"
     * @param list List of element to display
     */
    public ArrayAdapterWithState(@NonNull Context context, int resource, @NonNull List<T> list) {
        super(context, resource, R.id.text1, list);
        disabledColor = ThemeUtils.getStyleAttribColorValue(context, R.attr.text_disabled, R.color.dark_grey);
        normalColor = ThemeUtils.getStyleAttribColorValue(context, R.attr.text_normal, R.color.black);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).isEnabled();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        TextView tv = (TextView) v.findViewById(R.id.text1);
        tv.setTextColor(isEnabled(position) ? normalColor : disabledColor);
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView v = (TextView) super.getView(position, convertView, parent);
        v.setTextColor(isEnabled(position) ? normalColor : disabledColor);
        return v;
    }
}
