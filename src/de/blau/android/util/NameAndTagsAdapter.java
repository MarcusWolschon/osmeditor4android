package de.blau.android.util;

import java.util.ArrayList;
import java.util.Locale;

import de.blau.android.names.Names.NameAndTags;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

public class NameAndTagsAdapter extends ArrayAdapter<NameAndTags> {
	
//	 private final String MY_DEBUG_TAG = "NameAndTagsAdapter";
//	    private ArrayList<NameAndTags> items;
	    private ArrayList<NameAndTags> itemsAll;
	    private ArrayList<NameAndTags> suggestions;
//	    private int viewResourceId;
	    
    public NameAndTagsAdapter(Context context, int viewResourceId, ArrayList<NameAndTags> items) {
        super(context, viewResourceId, items);
//        this.items = items;
        this.itemsAll = (ArrayList<NameAndTags>) items.clone();
        this.suggestions = new ArrayList<NameAndTags>();
//        this.viewResourceId = viewResourceId;
    }

//    public View getView(int position, View convertView, ViewGroup parent) {
//        View v = convertView;
//        if (v == null) {
//            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            v = vi.inflate(viewResourceId, null);
//        }
//        NameAndTags customer = items.get(position);
//        if (customer != null) {
//            TextView customerNameLabel = (TextView) v.findViewById(R.id.customerNameLabel);
//            if (customerNameLabel != null) {
////              Log.i(MY_DEBUG_TAG, "getView NameAndTags Name:"+customer.getName());
//                customerNameLabel.setText(customer.getName());
//            }
//        }
//        return v;
//    }

    @Override
    public Filter getFilter() {
        return nameFilter;
    }

    Filter nameFilter = new Filter() {
        @Override
        public String convertResultToString(Object resultValue) {
            String str = ((NameAndTags)(resultValue)).getName(); 
            return str;
        }
        
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if(constraint != null) {
                suggestions.clear();
                for (NameAndTags name : itemsAll) {
                    if(name.getName().toLowerCase(Locale.US).startsWith(constraint.toString().toLowerCase(Locale.US))){
                        suggestions.add(name);
                    }
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }
        
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<NameAndTags> filteredList = (ArrayList<NameAndTags>) results.values;
            if(results != null && results.count > 0) {
                clear();
                for (NameAndTags c : filteredList) {
                    add(c);
                }
                notifyDataSetChanged();
            }
        }
    };
}
