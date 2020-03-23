package de.blau.android.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

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

    private List<FilterEntry> filter = new ArrayList<>();

    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private static final String DEBUG_TAG        = "TagFilter";

    private transient SQLiteDatabase mDatabase;

    /**
     * Construct a new TagFilter
     * 
     * @param context an Android Context
     */
    public TagFilter(@NonNull Context context) {
        super();
        init(context);
        //
        filter.clear();
        // filter, include INTEGER DEFAULT 0, type TEXT DEFAULT '*', key TEXT DEFAULT '*', value DEFAULT '*', active
        // INTEGER ;

        Cursor dbresult = mDatabase.query("filterentries", new String[] { "include", "type", "key", "value", "active" }, "filter = ?",
                new String[] { DEFAULT_FILTER }, null, null, null);
        dbresult.moveToFirst();
        for (int i = 0; i < dbresult.getCount(); i++) {
            try {
                filter.add(
                        new FilterEntry(dbresult.getInt(0) == 1, dbresult.getString(1), dbresult.getString(2), dbresult.getString(3), dbresult.getInt(4) == 1));
            } catch (PatternSyntaxException psex) {
                Log.e(DEBUG_TAG, "exception getting FilterEntry " + psex.getMessage());
                if (context instanceof Activity) {
                    Snack.barError((Activity) context, context.getString(R.string.toast_invalid_filter_regexp, dbresult.getString(2), dbresult.getString(3)));
                }
            }
            dbresult.moveToNext();
        }
        dbresult.close();
    }

    @Override
    public void init(Context context) {
        mDatabase = new TagFilterDatabaseHelper(context).getReadableDatabase();
    }

    @Override
    protected Include filter(OsmElement e) {
        Include include = Include.DONT;
        String type = e.getName();
        for (FilterEntry f : filter) {
            if (f.active) {
                Include match = Include.DONT;
                SortedMap<String, String> tags = e.getTags();
                if (tags != null && tags.size() > 0) {
                    for (Entry<String, String> t : tags.entrySet()) {
                        if (f.match(type, t.getKey(), t.getValue())) {
                            match = f.withWayNodes ? Include.INCLUDE_WITH_WAYNODES : Include.INCLUDE;
                            break;
                        }
                    }
                } else {
                    match = f.match(type, null, null) ? (f.withWayNodes ? Include.INCLUDE_WITH_WAYNODES : Include.INCLUDE) : Include.DONT;
                }
                if (match != Include.DONT) {
                    // we have a match
                    // Log.d(DEBUG_TAG,e.getDescription(true) + " matched " + f.toString());
                    include = f.include ? match : Include.DONT; // FIXME should relation membership be able to override
                                                                // this?
                    break;
                }
            }
        }

        if (include == Include.DONT) {
            // check if it is a relation member
            List<Relation> parents = e.getParentRelations();
            if (parents != null) {
                for (Relation r : new ArrayList<>(parents)) { // protect against ccm
                    Include relationInclude = testRelation(r, false);
                    if (relationInclude != null && relationInclude != Include.DONT) {
                        return relationInclude; // inherit include status from relation
                    }
                }
            }
        }
        // Log.d(DEBUG_TAG,e.getDescription() + " include: " + include);
        return include;
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
            Preferences prefs = new Preferences(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String buttonPos = layout.getContext().getString(R.string.follow_GPS_left);
            controls = (RelativeLayout) inflater
                    .inflate(prefs.followGPSbuttonPosition().equals(buttonPos) ? R.layout.tagfilter_controls_right : R.layout.tagfilter_controls_left, layout);
            tagFilterButton = (FloatingActionButton) controls.findViewById(R.id.tagFilterButton);
        }

        tagFilterButton.setClickable(true);
        tagFilterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View b) {
                Log.d(DEBUG_TAG, "Button clicked");
                TagFilterActivity.start(context, DEFAULT_FILTER);
            }
        });
        Util.setAlpha(tagFilterButton, Main.FABALPHA);
        setupControls(false);
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
