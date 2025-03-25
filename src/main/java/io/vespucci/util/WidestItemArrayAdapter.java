package io.vespucci.util;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;

/**
 * ArrayAdapter that sets the width of all views to that of the widest item.
 * 
 * Pre-computes the widest view in the adapter, use this when you need to wrap_content on a ListView, please be careful
 * and don't use it on an adapter that is extremely numerous in items or it will take a long time.
 * 
 * Yes, this is a hack.
 * 
 * Based on https://stackoverflow.com/a/13959716
 */
public class WidestItemArrayAdapter<T> extends ArrayAdapter<T> {

    int width = -1;

    /**
     * Construct a new adapter
     * 
     * @param context Android Context
     * @param resource resource id of the item layout
     * @param objects the List of objects
     */
    public WidestItemArrayAdapter(@NonNull Context context, int resource, @NonNull List<T> objects) {
        this(context, resource, 0, objects);
    }

    /**
     * Construct a new adapter
     * 
     * @param context Android Context
     * @param resource resource id of the item layout
     * @param textView resource id of the TextView
     * @param objects the List of objects
     */
    public WidestItemArrayAdapter(@NonNull Context context, int resource, int textView, @NonNull List<T> objects) {
        super(context, resource, textView, objects);
        View view = null;
        FrameLayout fakeParent = new FrameLayout(context);
        int maxWidth = 0;
        int count = getCount();
        for (int i = 0; i < count; i++) {
            view = getView(i, view, fakeParent);
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int tempWidth = view.getMeasuredWidth();
            if (tempWidth > maxWidth) {
                maxWidth = tempWidth;
            }
        }
        width = maxWidth;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        if (width > 0) {
            v.setMinimumWidth(width);
        }
        return v;
    }
}
