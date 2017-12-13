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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.prefs.ListActivity;
import de.blau.android.prefs.Preferences;

/**
 * Activity for editing filter entries. Due to the diffiulties in using a ListView for editable items, this is a rather
 * hackish and inefficient, but given that we are only going to have a small number of items likely OK.
 * 
 * @author simon
 *
 */
public class TagFilterActivity extends ListActivity {
    private static final String DEBUG_TAG       = "TagFilterActivity";
    private static final String FILTER_KEY      = "FILTER";
    private static final String QUERY           = "SELECT rowid as _id, active, include, type, key, value FROM filterentries WHERE filter = '";
    private String              filter          = null;
    private SQLiteDatabase      db;
    private Cursor              tagFilterCursor = null;
    private TagFilterAdapter    filterAdapter;

    private class ViewHolder {
        int      id;
        boolean  modified;
        CheckBox active;
        Spinner  mode;
        Spinner  type;
        TextView keyView;
        TextView valueView;
    }

    public static void start(@NonNull Context context, String filter) {
        Intent intent = new Intent(context, TagFilterActivity.class);
        intent.putExtra(FILTER_KEY, filter);
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
            filter = (String) getIntent().getSerializableExtra(FILTER_KEY);
        } else {
            filter = savedInstanceState.getString(FILTER_KEY);
        }
        this.filter = filter;
        final SQLiteDatabase db = new TagFilterDatabaseHelper(this).getWritableDatabase();
        this.db = db;
        tagFilterCursor = db.rawQuery(QUERY + filter + "'", null);
        filterAdapter = new TagFilterAdapter(this, tagFilterCursor);

        FloatingActionButton add = (FloatingActionButton) findViewById(R.id.add);
        if (add != null) {
            add.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    updateDatabaseFromList();
                    insertRow(filter, true, true, 0, "", "");
                    tagFilterCursor = db.rawQuery(QUERY + filter + "'", null);
                    Cursor oldCursor = filterAdapter.swapCursor(tagFilterCursor);
                    oldCursor.close();
                    filterAdapter.notifyDataSetChanged();
                    Log.d(DEBUG_TAG, "button clicked");
                }
            });
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
            ((TagFilterAdapter) getListView().getAdapter()).swapCursor(tagFilterCursor);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        db.close();
    }

    private void insertRow(String filter, boolean active, boolean include, int type, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("filter", filter);
        values.put("active", active ? 1 : 0);
        values.put("include", include ? 1 : 0);
        values.put("type", getTypeValue(type));
        values.put("key", key);
        values.put("value", value);
        db.insert("filterentries", null, values);
    }

    private void updateRow(int id, String filter, boolean active, boolean include, int type, String key, String value) {
        ContentValues values = new ContentValues();
        values.put("filter", filter);
        values.put("active", active ? 1 : 0);
        values.put("include", include ? 1 : 0);
        values.put("type", getTypeValue(type));
        values.put("key", key);
        values.put("value", value);
        Log.d(DEBUG_TAG, "updating " + id + " " + values);
        db.update("filterentries", values, "rowid=" + id, null);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        updateDatabaseFromList();
        outState.putString(FILTER_KEY, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tagfilter_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            updateDatabaseFromList();
            finish();
            break;
        case R.id.menu_help:
            HelpViewer.start(this, R.string.help_tagfilter);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        updateDatabaseFromList();
        super.onBackPressed();
    }

    private void updateDatabaseFromList() {
        Log.d(DEBUG_TAG, "update DB");
        ListView lv = getListView();
        for (int i = 0; i < lv.getCount(); i++) {
            View view = lv.getChildAt(i);
            if (view != null) {
                ViewHolder vh = (ViewHolder) view.getTag();
                if (vh != null && vh.modified) {
                    update(vh);
                }
            } else {
                Log.e(DEBUG_TAG, "view for index " + i + " is null");
            }
        }
    }

    private void update(ViewHolder vh) {
        Log.d(DEBUG_TAG, "saving contents for id " + vh.id);
        updateRow(vh.id, filter, vh.active.isChecked(), "+".equals((String) vh.mode.getSelectedItem()), vh.type.getSelectedItemPosition(),
                vh.keyView.getText().toString(), vh.valueView.getText().toString());
    }

    /**
     * Android doesn't really support editable items in ListViews and recycles the views as necessary. Our strategy is
     * to check if the view has been modified before recycling and if yes save to the database this means however that
     * changes cannot be undone without storing the original content of the rows in question, which we however currently
     * don't do.
     * 
     * @author simon
     *
     */
    private class TagFilterAdapter extends CursorAdapter {
        public TagFilterAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            View view = LayoutInflater.from(context).inflate(R.layout.tagfilter_item, parent, false);
            ViewHolder vh = new ViewHolder();
            // Find fields to populate in inflated template
            vh.active = (CheckBox) view.findViewById(R.id.active);
            vh.mode = (Spinner) view.findViewById(R.id.mode);
            vh.type = (Spinner) view.findViewById(R.id.type);
            vh.keyView = (TextView) view.findViewById(R.id.key);
            vh.valueView = (TextView) view.findViewById(R.id.value);
            view.setTag(vh);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Log.d(DEBUG_TAG, "bindView");
            final ViewHolder vh = (ViewHolder) view.getTag();
            if (vh.modified) { // very hackish
                update(vh);
                Cursor newCursor = db.rawQuery(QUERY + filter + "'", null);
                Cursor oldCursor = this.swapCursor(newCursor);
                oldCursor.close();
                this.notifyDataSetChanged();
                vh.modified = false;
                return;
            }
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            vh.id = id;
            Log.d(DEBUG_TAG, "bindView id " + id);

            //
            vh.active.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow("active")) == 1);
            vh.active.setTag(id);
            vh.active.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                    vh.modified = true;
                    Log.d("TagFilterActivity", "marked view as modified");
                }
            });
            vh.mode.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow("include")) == 1 ? 1 : 0);
            OnItemSelectedListener listener = new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id2) {
                    vh.modified = true;
                    Log.d("TagFilterActivity", "marked view as modified");
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            };
            vh.mode.setOnItemSelectedListener(listener);
            vh.type.setSelection(getTypeEntryIndex(cursor.getString(cursor.getColumnIndexOrThrow("type"))));
            vh.type.setOnItemSelectedListener(listener);

            String key = cursor.getString(cursor.getColumnIndexOrThrow("key"));
            vh.keyView.setText(key);
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void afterTextChanged(Editable arg0) {
                    vh.modified = true;
                    Log.d("TagFilterActivity", "marked view as modified");
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }
            };
            vh.keyView.addTextChangedListener(watcher);

            String value = cursor.getString(cursor.getColumnIndexOrThrow("value"));
            vh.valueView.setText(value);
            vh.valueView.addTextChangedListener(watcher);
            vh.valueView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.requestFocus();
                }
            });

            ImageButton delete = (ImageButton) view.findViewById(R.id.delete);
            delete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateDatabaseFromList();
                    db.delete("filterentries", "rowid=" + id, null);
                    newCursor();
                    Log.d("TagFilterActivity", "delete clicked");
                }
            });
        }
    }

    private int getTypeEntryIndex(String value) {
        Resources r = getResources();
        String[] values = r.getStringArray(R.array.tagfilter_type_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private String getTypeValue(int index) {
        Resources r = getResources();
        String[] values = r.getStringArray(R.array.tagfilter_type_values);
        return values[index];
    }

    private void newCursor() {
        Cursor newCursor = db.rawQuery(QUERY + filter + "'", null);
        Cursor oldCursor = filterAdapter.swapCursor(newCursor);
        oldCursor.close();
    }
}
