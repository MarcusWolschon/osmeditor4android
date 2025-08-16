package de.blau.android.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.util.InsetAwarePopupMenu;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

/**
 * Filter plus UI for filtering on tags NOTE: the relevant ways should be processed before nodes
 * 
 * @author simon
 *
 */
public class TagFilter extends CommonFilter {
    public static final String DEFAULT_FILTER = "Default";

    private class FilterEntry implements Serializable {
        private static final long serialVersionUID = 2L;

        /**
         * Entry is active
         */
        boolean active       = false;
        /**
         * Include matching elements
         */
        boolean include      = false;
        /**
         * Include all element types
         */
        boolean allElements  = false;
        /**
         * Include way nodes
         */
        boolean withWayNodes = false;
        /**
         * OSM element type
         */
        String  type;
        /**
         * Regular expression for keys of tags
         */
        Pattern key;
        /**
         * Regular expression for values of tags
         */
        Pattern value;

        /**
         * Construct a new FilterEntry
         * 
         * @param include Include value for this entry
         * @param type OSM object type
         * @param key key of tag
         * @param value value of tag
         * @param active if true this entry is active, otherwise it will be ignored
         */
        FilterEntry(boolean include, @NonNull String type, @Nullable String key, @Nullable String value, boolean active) {
            this.include = include;
            allElements = "*".equals(type); // just check this once
            withWayNodes = type.endsWith("+");
            this.type = withWayNodes ? type.substring(0, type.length() - 1) : type;
            this.key = key != null && !"".equals(key) ? Pattern.compile(key) : null;
            this.value = value != null && !"".equals(value) ? Pattern.compile(value) : null;
            this.active = active;
        }

        /**
         * Test it filter entry matches
         * 
         * @param type OSM object type
         * @param key key of tag
         * @param value value of tag
         * @return true if a match
         */
        boolean match(@NonNull String type, @Nullable String key, @Nullable String value) {
            if (allElements || this.type.equals(type)) {
                Matcher keyMatcher = null;
                if (this.key != null) {
                    if (key == null) {
                        return false;
                    }
                    keyMatcher = this.key.matcher(key);
                }
                Matcher valueMatcher = null;
                if (this.value != null) {
                    if (value == null) {
                        return false;
                    }
                    valueMatcher = this.value.matcher(value);
                }
                return (keyMatcher == null || keyMatcher.matches()) && (valueMatcher == null || valueMatcher.matches());
            }
            return false;
        }

        @Override
        public String toString() {
            return "Active " + active + " include " + include + " type " + type + " key " + key + " value " + value;
        }
    }

    private List<FilterEntry> filterList = new ArrayList<>();

    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private static final String DEBUG_TAG        = TagFilter.class.getSimpleName().substring(0, Math.min(23, TagFilter.class.getSimpleName().length()));

    /**
     * Construct a new TagFilter
     * 
     * @param context an Android Context
     */
    public TagFilter(@NonNull Context context) {
        super();
        init(context);
    }

    @Override
    public void init(Context context) {
        clear(); // zap caches
        try (TagFilterDatabaseHelper tfDb = new TagFilterDatabaseHelper(context); SQLiteDatabase mDatabase = tfDb.getReadableDatabase()) {
            //
            filterList.clear();
            String filterName = TagFilterDatabaseHelper.getCurrent(mDatabase);
            Cursor dbresult = mDatabase.query(TagFilterDatabaseHelper.FILTERENTRIES_TABLE,
                    new String[] { TagFilterActivity.INCLUDE_COLUMN, TagFilterActivity.TYPE_COLUMN, TagFilterActivity.KEY_COLUMN,
                            TagFilterActivity.VALUE_COLUMN, TagFilterActivity.ACTIVE_COLUMN },
                    TagFilterDatabaseHelper.FILTER_COLUMN + " = ?", new String[] { filterName }, null, null, null);
            dbresult.moveToFirst();
            for (int i = 0; i < dbresult.getCount(); i++) {
                try {
                    filterList.add(new FilterEntry(dbresult.getInt(0) == 1, dbresult.getString(1), dbresult.getString(2), dbresult.getString(3),
                            dbresult.getInt(4) == 1));
                } catch (PatternSyntaxException psex) {
                    Log.e(DEBUG_TAG, "exception getting FilterEntry " + psex.getMessage());
                    if (context instanceof Activity) {
                        ScreenMessage.barError((Activity) context,
                                context.getString(R.string.toast_invalid_filter_regexp, dbresult.getString(2), dbresult.getString(3)));
                    }
                }
                dbresult.moveToNext();
            }
            dbresult.close();
        }
    }

    @Override
    protected Include filter(OsmElement e) {
        Include include = Include.DONT;
        String type = e.getName();
        for (FilterEntry f : getActiveEntries(filterList)) {
            Include match = Include.DONT;
            SortedMap<String, String> tags = e.getTags();
            if (tags.size() > 0) {
                for (Entry<String, String> t : tags.entrySet()) {
                    if (f.match(type, t.getKey(), t.getValue())) {
                        match = withWayNodes(f);
                        break;
                    }
                }
            } else {
                match = f.match(type, null, null) ? withWayNodes(f) : Include.DONT;
            }
            if (match != Include.DONT) {
                // we have a match

                // as we need to potentially invert the way nodes too, we need to add/not add them here
                if (e instanceof Way && match == Include.INCLUDE_WITH_WAYNODES) {
                    includeWayNodes((Way) e, !f.include);
                }

                // if f.include is false invert
                include = f.include ? match : Include.DONT;
                break;
            }
        }

        if (include == Include.DONT) {
            // check if it is a relation member
            List<Relation> parents = e.getParentRelations();
            if (parents == null) {
                return include;
            }
            for (Relation r : new ArrayList<>(parents)) { // protect against ccm
                Include relationInclude = testRelation(r, false);
                if (relationInclude != null && relationInclude != Include.DONT) {
                    return relationInclude; // inherit include status from relation
                }
            }
        }
        return include;
    }

    /**
     * Return the correct Include value if withWayNodes is set on a FilterEntry
     * 
     * @param f the FilterEntry
     * @return the correct Include value
     */
    private Include withWayNodes(FilterEntry f) {
        return f.withWayNodes ? Include.INCLUDE_WITH_WAYNODES : Include.INCLUDE;
    }

    /**
     * Get the active FilterEntry instances
     * 
     * @param entries all FilterEntrys
     * @return only the active ones
     */
    @NonNull
    private List<FilterEntry> getActiveEntries(@NonNull List<FilterEntry> entries) {
        List<FilterEntry> activeEntries = new ArrayList<>();
        for (FilterEntry entry : entries) {
            if (entry.active) {
                activeEntries.add(entry);
            }
        }
        return activeEntries;
    }

    /**
     * Tag filter controls
     */
    private transient FloatingActionButton tagFilterButton;
    private transient ViewGroup            parent;
    private transient RelativeLayout       controls;
    private transient Update               update;

    @Override
    public void addControls(ViewGroup layout, final Update update) {
        Log.d(DEBUG_TAG, "adding filter controls");
        this.parent = layout;
        this.update = update;
        tagFilterButton = (FloatingActionButton) parent.findViewById(R.id.tagFilterButton);
        final Context context = layout.getContext();
        // we weren't already added ...
        if (tagFilterButton == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String buttonPos = layout.getContext().getString(R.string.follow_GPS_left);
            controls = (RelativeLayout) inflater
                    .inflate(App.getPreferences(context).followGPSbuttonPosition().equals(buttonPos) ? R.layout.tagfilter_controls_right
                            : R.layout.tagfilter_controls_left, layout);
            tagFilterButton = (FloatingActionButton) controls.findViewById(R.id.tagFilterButton);
        }

        tagFilterButton.setClickable(true);
        tagFilterButton.setOnClickListener(b -> TagFilterActivity.start(context));
        tagFilterButton.setLongClickable(true);
        tagFilterButton.setOnLongClickListener((View v) -> {
            PopupMenu popup = new InsetAwarePopupMenu(context, tagFilterButton);
            try (TagFilterDatabaseHelper tfDb = new TagFilterDatabaseHelper(context); SQLiteDatabase db = tfDb.getWritableDatabase()) {
                final String[] names = TagFilterDatabaseHelper.getFilterNames(context, db);
                final String current = TagFilterDatabaseHelper.getCurrent(db);
                for (final String filterName : names) {
                    SpannableString s = new SpannableString(TagFilterActivity.getFilterName(context, filterName));
                    final boolean selected = current.equals(filterName);
                    if (selected) {
                        s.setSpan(new ForegroundColorSpan(ThemeUtils.getStyleAttribColorValue(context, R.attr.colorAccent, 0)), 0, s.length(), 0);
                    }
                    MenuItem item = popup.getMenu().add(s);
                    item.setOnMenuItemClickListener((MenuItem menuItem) -> {
                        if (selected) {
                            return true;
                        }
                        switchFilter(context, new TagFilterDatabaseHelper(context), filterName, update);
                        return true;
                    });
                }
                popup.show();
            }
            return true;
        });
        tagFilterButton.setAlpha(Main.FABALPHA);
        setupControls(false);
    }

    /**
     * 
     * 
     * @param context
     * @param tfDb
     * @param filterName
     * @param update
     */
    private void switchFilter(@NonNull final Context context, @NonNull final TagFilterDatabaseHelper tfDb, @NonNull final String filterName,
            @NonNull final Update update) {
        try (SQLiteDatabase db2 = tfDb.getWritableDatabase()) {
            TagFilterDatabaseHelper.setCurrent(db2, filterName);
            init(context);
            update.execute();
        }
    }

    /**
     * Set the current control state
     * 
     * @param toggle if true toggle the if the filter is enabled
     */
    private void setupControls(boolean toggle) {
        enabled = toggle ? !enabled : enabled;
        update.execute();
    }

    @Override
    public void removeControls() {
        if (parent != null && controls != null) {
            parent.removeView(controls);
        }
    }

    @Override
    public void hideControls() {
        if (tagFilterButton != null) {
            tagFilterButton.hide();
        }
    }

    @Override
    public void showControls() {
        if (tagFilterButton != null) {
            tagFilterButton.show();
        }
    }
}
