package de.blau.android.util;

import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.Filter;

/**
 * 
 * Adapted from http://stackoverflow.com/questions/8512762/autocompletetextview-disable-filtering
 *
 * @param <T>
 */
public class FilterlessArrayAdapter<T> extends ArrayAdapter<T> {

    private Filter  filter = new NoFilter();
    private List<T> items;

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }

    public FilterlessArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
        super(context, textViewResourceId, objects);
        items = objects;
    }

    private class NoFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence arg0) {
            FilterResults result = new FilterResults();
            result.values = items;
            result.count = items.size();
            return result;
        }

        @Override
        protected void publishResults(CharSequence arg0, FilterResults arg1) {
            notifyDataSetChanged();
        }
    }
}
