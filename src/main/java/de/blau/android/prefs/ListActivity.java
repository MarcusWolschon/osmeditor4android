package de.blau.android.prefs;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * This is a quick hack around the issue that there is no Fragment version of SherlockListActivity
 *
 */
@SuppressLint("Registered")
public class ListActivity extends AppCompatActivity {
    private ListView mListView;

    /**
     * Get the ListView
     * 
     * @return the ListView
     */
    protected ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(android.R.id.list);
        }
        return mListView;
    }

    /**
     * Set the adapter holding whatever we want to display in the list
     * 
     * @param adapter a ListAdapter
     */
    void setListAdapter(@NonNull ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    /**
     * Get the adapter
     * 
     * @return a ListAdapter
     */
    protected ListAdapter getListAdapter() {
        ListAdapter adapter = getListView().getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            return ((HeaderViewListAdapter) adapter).getWrappedAdapter();
        } else {
            return adapter;
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View empty = findViewById(android.R.id.empty);
        if (empty != null) {
            getListView().setEmptyView(empty);
        } else {
            Log.e("ListActivitiy", "empty view not found");
        }
    }
}
