package io.vespucci.util;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import androidx.annotation.NonNull;

/**
 * 
 * Adapted from http://stackoverflow.com/questions/8512762/autocompletetextview-disable-filtering
 *
 * @param <T> the type the adapter contains
 * 
 * @author Simon Poole
 */
public class FilterlessArrayAdapter<T> extends ArrayAdapter<T> {

    private Filter  filter = new NoFilter();
    private List<T> items;

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }

    /**
     * Construct a new adapter
     * 
     * @param context an Android Context
     * @param textViewResourceId The resource ID for a layout file containing a TextView to use when instantiating views
     * @param objects The objects to represent in the ListView.
     */
    public FilterlessArrayAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<T> objects) {
        super(context, textViewResourceId, objects);
        items = objects;
    }

    private final class NoFilter extends Filter {

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
