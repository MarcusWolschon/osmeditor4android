package de.blau.android.filter;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.cursoradapter.widget.CursorAdapter;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.prefs.ListActivity;

/**
 * Activity for editing filter entries. Due to the difficulties in using a ListView for editable items, this is a rather
 * hackish and inefficient, but given that we are only going to have a small number of items likely OK.
 * 
 * @author simon
 *
 */
public class TagFilterActivity extends ListActivity {

    private static final int    TAG_LEN    = Math.min(LOG_TAG_LEN, TagFilterActivity.class.getSimpleName().length());
    private static final String DEBUG_TAG  = TagFilterActivity.class.getSimpleName().substring(0, TAG_LEN);
    private static final String FILTER_KEY = "FILTER";

    static final String FILTERENTRIES_TABLE = "filterentries";
    static final String VALUE_COLUMN        = "value";
    static final String KEY_COLUMN          = "key";
    static final String TYPE_COLUMN         = "type";
    static final String INCLUDE_COLUMN      = "include";
    static final String ACTIVE_COLUMN       = "active";
    static final String FILTER_COLUMN       = "filter";

    private static final String QUERY = "SELECT rowid as _id, active, include, type, key, value FROM filterentries WHERE filter = '";

    private String                  filter          = null;
    private TagFilterDatabaseHelper tfDb;
    private SQLiteDatabase          db;
    private Cursor                  tagFilterCursor = null;
    private TagFilterAdapter        filterAdapter;

    private class ViewHolder {
        int      id;
        boolean  modified;
        CheckBox active;
        Spinner  mode;
        Spinner  type;
        TextView keyView;
        TextView valueView;
    }

    /**
     * Start this activity
     * 
     * @param context an Android Context
     * @param filter the name of the TagFilter
     */
    public static void start(@NonNull Context context, String filter) {
        Intent intent = new Intent(context, TagFilterActivity.class);
        intent.putExtra(FILTER_KEY, filter);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (App.getPreferences(this).lightThemeEnabled()) {
            setTheme(R.style.Theme_customActionBar_Light);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_activity);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(true);
        final String filterParam;
        if (savedInstanceState == null) {
            filterParam = getIntent().getStringExtra(FILTER_KEY);
        } else {
            filterParam = savedInstanceState.getString(FILTER_KEY);
        }
        filter = filterParam;
        tfDb = new TagFilterDatabaseHelper(this);
        db = tfDb.getWritableDatabase();
        tagFilterCursor = db.rawQuery(QUERY + filter + "'", null); // NOSONAR filter isn't actually user generated
        filterAdapter = new TagFilterAdapter(this, tagFilterCursor);

        FloatingActionButton add = (FloatingActionButton) findViewById(R.id.add);
        if (add != null) {
            add.setOnClickListener(v -> {
                updateDatabaseFromList();
                insertRow(filter, true, true, 0, "", "");
                tagFilterCursor = db.rawQuery(QUERY + filter + "'", null); // NOSONAR filter isn't actually user
                                                                           // generated
                Cursor oldCursor = filterAdapter.swapCursor(tagFilterCursor);
                oldCursor.close();
                filterAdapter.notifyDataSetChanged();
                Log.d(DEBUG_TAG, "button clicked");
            });
            add.show();
        }
        // this makes fields in the items focusable
        getListView().setFocusable(false);
        getListView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        getListView().setItemsCanFocus(true);
        // Attach cursor adapter to the ListView
        getListView().setAdapter(filterAdapter);

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
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
        if (db != null) {
            db.close();
        }
        if (tfDb != null) {
            tfDb.close();
        }
    }

    /**
     * Insert a new filter entry
     * 
     * @param filter name of the filter
     * @param active if true this entry is active, otherwise it will be ignored
     * @param include Include value for this entry
     * @param type OSM object type
     * @param key key of tag
     * @param value value of tag
     */
    private void insertRow(String filter, boolean active, boolean include, int type, @Nullable String key, @Nullable String value) {
        ContentValues values = new ContentValues();
        values.put(FILTER_COLUMN, filter);
        values.put(ACTIVE_COLUMN, active ? 1 : 0);
        values.put(INCLUDE_COLUMN, include ? 1 : 0);
        values.put(TYPE_COLUMN, getTypeValue(type));
        values.put(KEY_COLUMN, key);
        values.put(VALUE_COLUMN, value);
        db.insert(FILTERENTRIES_TABLE, null, values);
    }

    /**
     * Update an existing medicine
     * 
     * @param id of the entry
     * @param filter name of the filter
     * @param active if true this entry is active, otherwise it will be ignored
     * @param include Include value for this entry
     * @param type OSM object type
     * @param key key of tag
     * @param value value of tag
     */
    private void updateRow(int id, String filter, boolean active, boolean include, int type, @Nullable String key, @Nullable String value) {
        ContentValues values = new ContentValues();
        values.put(FILTER_COLUMN, filter);
        values.put(ACTIVE_COLUMN, active ? 1 : 0);
        values.put(INCLUDE_COLUMN, include ? 1 : 0);
        values.put(TYPE_COLUMN, getTypeValue(type));
        values.put(KEY_COLUMN, key);
        values.put(VALUE_COLUMN, value);
        Log.d(DEBUG_TAG, "updating " + id + " " + values);
        db.update(FILTERENTRIES_TABLE, values, "rowid=" + id, null);
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
        default:
            Log.w(DEBUG_TAG, "Unknown menu item " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update database if exiting via back button
     */
    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {

        @Override
        public void handleOnBackPressed() {
            Log.d(DEBUG_TAG, "onBackPressed()");
            updateDatabaseFromList();
            onBackPressedCallback.setEnabled(false);
            getOnBackPressedDispatcher().onBackPressed();
        }
    };

    /**
     * Update the database from the whole view
     */
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

    /**
     * Update a an entry from a ViewHolder
     * 
     * @param vh the ViewHolder
     */
    private void update(@NonNull ViewHolder vh) {
        Log.d(DEBUG_TAG, "saving contents for id " + vh.id);
        updateRow(vh.id, filter, vh.active.isChecked(), "+".equals(vh.mode.getSelectedItem()), vh.type.getSelectedItemPosition(),
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

        /**
         * Construct a new adapter
         * 
         * @param context an Android Context
         * @param cursor a database cursor
         */
        public TagFilterAdapter(@NonNull Context context, @NonNull Cursor cursor) {
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
            vh.active.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow(ACTIVE_COLUMN)) == 1);
            vh.active.setTag(id);
            vh.active.setOnCheckedChangeListener((button, isChecked) -> vh.modified = true);
            vh.mode.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow(INCLUDE_COLUMN)) == 1 ? 1 : 0);
            OnItemSelectedListener listener = new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id2) {
                    vh.modified = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // Empty
                }
            };
            vh.mode.setOnItemSelectedListener(listener);
            vh.type.setSelection(getTypeEntryIndex(cursor.getString(cursor.getColumnIndexOrThrow(TYPE_COLUMN))));
            vh.type.setOnItemSelectedListener(listener);

            String key = cursor.getString(cursor.getColumnIndexOrThrow(KEY_COLUMN));
            vh.keyView.setText(key);
            TextWatcher watcher = new TextWatcher() {
                @Override
                public void afterTextChanged(Editable arg0) {
                    vh.modified = true;
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    // Empty
                }

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    // Empty
                }
            };
            vh.keyView.addTextChangedListener(watcher);

            String value = cursor.getString(cursor.getColumnIndexOrThrow(VALUE_COLUMN));
            vh.valueView.setText(value);
            vh.valueView.addTextChangedListener(watcher);
            vh.valueView.setOnClickListener(View::requestFocus);

            ImageButton delete = (ImageButton) view.findViewById(R.id.delete);
            delete.setOnClickListener(v -> {
                updateDatabaseFromList();
                db.delete(FILTERENTRIES_TABLE, "rowid=" + id, null);
                newCursor();
                Log.d(DEBUG_TAG, "delete clicked");
            });
        }

        /**
         * Get index in the string array resource
         * 
         * @param value the value we need the index for
         * @return the index or 0
         */
        private int getTypeEntryIndex(@NonNull String value) {
            Resources r = getResources();
            String[] values = r.getStringArray(R.array.tagfilter_type_values);
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    return i;
                }
            }
            return 0;
        }

        /**
         * Swao the cursor for a new one
         */
        private void newCursor() {
            Cursor newCursor = db.rawQuery(QUERY + filter + "'", null);
            Cursor oldCursor = filterAdapter.swapCursor(newCursor);
            oldCursor.close();
        }
    }

    /**
     * Get the type value from a string array resource
     * 
     * @param index the index
     * @return the type string
     */
    private String getTypeValue(int index) {
        Resources r = getResources();
        String[] values = r.getStringArray(R.array.tagfilter_type_values);
        return values[index];
    }
}
