package de.blau.android.filter;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.prefs.ListActivity;
import de.blau.android.prefs.Preferences;

public class TagFilterActivity extends ListActivity  {
	private static final String DEBUG_TAG = "TagFilterActivity";
	private static final String FILTER = "FILTER";
	private static final String QUERY = "SELECT rowid as _id, active, include, type, key, value FROM filterentries WHERE filter = '";
	String filter = null;
	SQLiteDatabase db;
	Cursor tagFilterCursor = null;
	
	public static void start(@NonNull Context context, String filter) {
		Intent intent = new Intent(context, TagFilterActivity.class);
		intent.putExtra(FILTER, filter);
		context.startActivity(intent);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Preferences prefs = new Preferences(this);
		if (prefs.lightThemeEnabled()) {
			setTheme(R.style.Theme_customActionBar_Light);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_activity);

		ActionBar actionbar = getSupportActionBar();
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setDisplayHomeAsUpEnabled(true);
		final String filter;
		if (savedInstanceState == null) {
			filter = (String)getIntent().getSerializableExtra(FILTER);
		} else {
			filter = savedInstanceState.getString(FILTER);
		}
		this.filter = filter;
		final SQLiteDatabase db = new TagFilterDatabaseHelper(this).getWritableDatabase();
		this.db = db;
		tagFilterCursor = db.rawQuery(QUERY + filter + "'", null);
		final TagFilterAdapter filterAdapter = new TagFilterAdapter(this, tagFilterCursor);
		
		FloatingActionButton add = (FloatingActionButton) findViewById(R.id.add);
		if (add != null) {
			add.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					updateDatabaseFromList();
					insertRow(filter,true,true,0,"","");
					tagFilterCursor = db.rawQuery(QUERY + filter + "'", null);
					Cursor oldCursor = filterAdapter.swapCursor(tagFilterCursor);
					oldCursor.close();
					Log.d(DEBUG_TAG,"button clicked");
				}});
			add.setVisibility(View.VISIBLE);
		}
		// this makes fields in the items focusable
		getListView().setFocusable(false);
		getListView().setDescendantFocusability(ListView.FOCUS_AFTER_DESCENDANTS);
		getListView().setItemsCanFocus(true);
		// Attach cursor adapter to the ListView 
		getListView().setAdapter(filterAdapter);	
	}
	
	@Override
	public void onPause() {
		super.onPause();
		tagFilterCursor.close();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (tagFilterCursor == null || tagFilterCursor.isClosed()) {
			tagFilterCursor = db.rawQuery(QUERY + filter + "'", null);
			((TagFilterAdapter)getListView().getAdapter()).swapCursor(tagFilterCursor);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		db.close();
	}
	
	void insertRow(String filter,boolean active, boolean include, int type, String key, String value) {
		ContentValues values = new ContentValues();
		values.put("filter", filter);
		values.put("active", active ? 1 : 0);
		values.put("include", include ? 1 : 0);
		values.put("type", getTypeValue(type));
		values.put("key", key);
		values.put("value", value);
		db.insert("filterentries", null, values);
	}
	
	void updateRow(int id, String filter,boolean active, boolean include, int type, String key, String value) {
		ContentValues values = new ContentValues();
		values.put("filter", filter);
		values.put("active", active ? 1 : 0);
		values.put("include", include ? 1 : 0);
		values.put("type", getTypeValue(type));
		values.put("key", key);
		values.put("value", value);
		Log.d(DEBUG_TAG,"updating " + id + " " + values);
		db.update("filterentries", values, "rowid="+id, null);
	}
	
	
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		Log.d(DEBUG_TAG,"onSaveInstanceState");
		super.onSaveInstanceState(outState);
		updateDatabaseFromList();
		outState.putString(FILTER, filter);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
//			new AlertDialog.Builder(this)
//			.setNeutralButton(R.string.cancel, null)
//			.setNegativeButton(R.string.tag_menu_exit_no_save,        	
//					new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface arg0, int arg1) {
//					finish();
//				}
//			})
//	        .setPositiveButton(R.string.save, 
//	        	new DialogInterface.OnClickListener() {
//		            @Override
//					public void onClick(DialogInterface arg0, int arg1) {
//		            	updateDatabaseFromList();
//		                finish();
//		            }
//	        }).create().show();
			updateDatabaseFromList();
            finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
		updateDatabaseFromList();
		super.onBackPressed();
	}
	
	void updateDatabaseFromList() {
        ListView lv = getListView();
        for (int i = 0; i < lv.getCount(); i++) {
            View view = lv.getChildAt(i);
            CheckBox active = (CheckBox) view.findViewById(R.id.active);
            Spinner mode = (Spinner) view.findViewById(R.id.mode);
            Spinner type = (Spinner) view.findViewById(R.id.type);
            TextView keyView = (TextView) view.findViewById(R.id.key);
            TextView valueView = (TextView) view.findViewById(R.id.value);
            updateRow((Integer)view.getTag(),filter,active.isChecked(), "+".equals((String)mode.getSelectedItem()), type.getSelectedItemPosition(), keyView.getText().toString(), valueView.getText().toString());
        }
	}

	private class TagFilterAdapter extends CursorAdapter {
		public TagFilterAdapter(Context context, Cursor cursor) {
			super(context, cursor, 0);
		}

		// The newView method is used to inflate a new view and return it,
		// you don't bind any data to the view at this point.
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.tagfilter_item, parent, false);
		}

		// The bindView method is used to bind all data to a given view
		// such as setting the text on a TextView.
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// Find fields to populate in inflated template
			CheckBox active = (CheckBox) view.findViewById(R.id.active);
			Spinner mode = (Spinner) view.findViewById(R.id.mode);
			Spinner type = (Spinner) view.findViewById(R.id.type);
			TextView keyView = (TextView) view.findViewById(R.id.key);
			TextView valueView = (TextView) view.findViewById(R.id.value);
			//
			active.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow("active"))==1);
			mode.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow("include"))==1?1:0);
			type.setSelection(getTypeEntryIndex(cursor.getString(cursor.getColumnIndexOrThrow("type"))));
			String key = cursor.getString(cursor.getColumnIndexOrThrow("key"));
			keyView.setText(key);
			String value = cursor.getString(cursor.getColumnIndexOrThrow("value"));
			valueView.setText(value);
			ImageButton delete = (ImageButton) view.findViewById(R.id.delete);
			int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
			view.setTag(id);
			delete.setTag(id);
			delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					updateDatabaseFromList();
					db.delete("filterentries", "rowid=" + v.getTag(), null);
					Cursor newCursor = db.rawQuery(QUERY + filter + "'", null);
					Cursor oldCursor = TagFilterAdapter.this.swapCursor(newCursor);
					oldCursor.close();
					Log.d("TagFilterActivity","delete clicked");
				}});
		}

	}

	int getTypeEntryIndex(String value) {
		Resources r = getResources();
		String[] values = r.getStringArray(R.array.tagfilter_type_values);
		for (int i=0;i<values.length;i++) {
			if (values[i].equals(value)) {
				return i;
			}
		}
		return 0;
	}

	String getTypeValue(int index) {
		Resources r = getResources();
		String[] values = r.getStringArray(R.array.tagfilter_type_values);
		return values[index];
	}
}
