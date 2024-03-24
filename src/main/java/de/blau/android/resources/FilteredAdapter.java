package de.blau.android.resources;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Filter;
import de.blau.android.util.WidestItemArrayAdapter;

public class FilteredAdapter extends WidestItemArrayAdapter<String> {
    private final List<String> originalObjects;

    public FilteredAdapter(Context context, int resource, int textView, List<String> objects) {
        super(context, resource, textView, objects);
        originalObjects = new ArrayList<>(objects);
    }

    @Override
    public Filter getFilter() {

        return new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                for (String v : (List<String>) results.values) {
                    add(v);
                }
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                List<String> filteredNames = new ArrayList<>();

                constraint = constraint.toString().toLowerCase();
                for (String object : originalObjects) {
                    if (object.toLowerCase().contains(constraint.toString())) {
                        filteredNames.add(object);
                    }
                }

                results.count = filteredNames.size();
                results.values = filteredNames;
                return results;
            }
        };
    }
}
