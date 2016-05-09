package de.blau.android.prefs;


import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
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

	protected ListView getListView() {
		if (mListView == null) {
			mListView = (ListView) findViewById(android.R.id.list);
		}
		return mListView;
	}

	protected void setListAdapter(ListAdapter adapter) {
		getListView().setAdapter(adapter);
	}

	protected ListAdapter getListAdapter() {
		ListAdapter adapter = getListView().getAdapter();
		if (adapter instanceof HeaderViewListAdapter) {
			return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
		} else {
			return adapter;
		}
	}
}
