package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.Feedback;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.Github;
import de.blau.android.exception.IllegalOperationException;
import de.blau.android.names.Names.TagMap;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.propertyeditor.tagform.TagFormFragment;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.BugFixedAppCompatActivity;
import de.blau.android.util.GeoContext;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.Screen;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;
import de.blau.android.util.StreetPlaceNamesAdapter;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.views.ExtendedViewPager;

/**
 * An Activity to edit OSM-Tags. Sends the edited Tags as Result to its caller-Activity (normally {@link Main}).
 * 
 * The Editor does not directly edit the original tags or relation memberships which makes the code fairly and perhaps
 * unnecessarily complex
 * 
 * @author mb
 * @author simon
 */
public class PropertyEditor extends BugFixedAppCompatActivity implements PropertyEditorListener, OnPresetSelectedListener, EditorUpdate, FormUpdate,
        PresetUpdate, NameAdapters, OnSaveListener, ch.poole.openinghoursfragment.OnSaveListener {

    private static final String CURRENTITEM            = "current_item";
    private static final String PANELAYOUT             = "pane_layout";
    private static final String PRESET_FRAGMENT        = "preset_fragment";
    static final String         RECENTPRESETS_FRAGMENT = "recentpresets_fragment";

    public static final String  TAGEDIT_DATA              = "dataClass";
    private static final String TAGEDIT_LAST_ADDRESS_TAGS = "applyLastTags";
    private static final String TAGEDIT_SHOW_PRESETS      = "showPresets";
    private static final String TAGEDIT_EXTRA_TAGS        = "extra";
    private static final String TAGEDIT_PRESETSTOAPPLY    = "presetsToApply";

    private static final int PREFERENCES_CODE = 5634;

    /** The layout containing the edit rows */
    LinearLayout rowLayout = null;

    PresetFragment presetFragment;

    TagFormFragment tagFormFragment;
    private int     tagFormFragmentPosition = -1;

    TagEditorFragment tagEditorFragment;
    private int       tagEditorFragmentPosition = -1;

    RelationMembershipFragment relationMembershipFragment;
    RelationMembersFragment    relationMembersFragment;

    private PropertyEditorPagerAdapter pagerAdapter;

    /**
     * The tag we use for Android-logging.
     */
    private static final String DEBUG_TAG = PropertyEditor.class.getSimpleName();

    private long[] osmIds;

    private String[] types;

    Preset[]           presets = null;
    /**
     * The OSM element for reference. DO NOT ATTEMPT TO MODIFY IT.
     */
    private OsmElement element;

    private PropertyEditorData[] loadData;

    private boolean                      applyLastAddressTags = false;
    private boolean                      showPresets          = false;
    private HashMap<String, String>      extraTags            = null;
    private ArrayList<PresetElementPath> presetsToApply       = null;

    /**
     * Handles "enter" key presses.
     */
    static final OnKeyListener myKeyListener = new MyKeyListener();

    /**
     * True while the activity is between onResume and onPause. Used to suppress autocomplete dropdowns while the
     * activity is not running (showing them can lead to crashes). Needs to be static to be accessible in TagEditRow.
     */
    static boolean running = false;

    /**
     * Display form based editing
     */
    private boolean formEnabled = false;

    /**
     * Used both in the form and conventional tag editor fragments
     */
    private StreetPlaceNamesAdapter streetNameAutocompleteAdapter = null;
    private StreetPlaceNamesAdapter placeNameAutocompleteAdapter  = null;

    /**
     * 
     */
    private ArrayList<LinkedHashMap<String, String>> originalTags;

    /**
     * the same for relations
     */
    private MultiHashMap<Long, RelationMemberPosition> originalParents;
    private ArrayList<RelationMemberDescription>       originalMembers;

    private Preferences             prefs         = null;
    private ExtendedViewPager       mViewPager;
    private boolean                 usePaneLayout = false;
    private boolean                 isRelation    = false;
    private transient NetworkStatus networkStatus;
    private List<String>            isoCodes      = null;

    /**
     * Start a PropertyEditor activity
     * 
     * @param activity calling activity
     * @param dataClass the tags and relation memberships that should be edited
     * @param predictAddressTags try to predict address tags
     * @param showPresets show the preset tab first
     * @param extraTags additional tags that should be added
     * @param presetItems presets that should be applied
     * @param requestCode request code for the response
     */
    public static void startForResult(@NonNull Activity activity, @NonNull PropertyEditorData[] dataClass, boolean predictAddressTags, boolean showPresets,
            HashMap<String, String> extraTags, ArrayList<PresetElementPath> presetItems, int requestCode) {
        Log.d(DEBUG_TAG, "startForResult");
        Intent intent = new Intent(activity, PropertyEditor.class);
        intent.putExtra(TAGEDIT_DATA, dataClass);
        intent.putExtra(TAGEDIT_LAST_ADDRESS_TAGS, Boolean.valueOf(predictAddressTags));
        intent.putExtra(TAGEDIT_SHOW_PRESETS, Boolean.valueOf(showPresets));
        intent.putExtra(TAGEDIT_EXTRA_TAGS, extraTags);
        intent.putExtra(TAGEDIT_PRESETSTOAPPLY, presetItems);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        int currentItem = -1; // used when restoring
        Logic logic = App.getLogic();
        if (logic == null) {
            super.onCreate(savedInstanceState); // have to call through first
            // cause for this is currently unknown, but it isn't recoverable
            abort("Logic is null");
            return;
        }
        prefs = logic.getPrefs();
        if (prefs == null) {
            Log.e(DEBUG_TAG, "prefs was null creating new");
            prefs = new Preferences(this);
            logic.setPrefs(prefs);
        }
        if (prefs.lightThemeEnabled()) {
            setTheme(R.style.Theme_customTagEditor_Light);
        }

        super.onCreate(savedInstanceState);

        if (prefs.splitActionBarEnabled()) {
            // TODO determine if we want to reinstate the bottom bar
        }

        // tags
        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from intent");
            loadData = PropertyEditorData.deserializeArray(getIntent().getSerializableExtra(TAGEDIT_DATA));
            applyLastAddressTags = (Boolean) getIntent().getSerializableExtra(TAGEDIT_LAST_ADDRESS_TAGS);
            showPresets = (Boolean) getIntent().getSerializableExtra(TAGEDIT_SHOW_PRESETS);
            extraTags = (HashMap<String, String>) getIntent().getSerializableExtra(TAGEDIT_EXTRA_TAGS);
            presetsToApply = (ArrayList<PresetElementPath>) getIntent().getSerializableExtra(TAGEDIT_PRESETSTOAPPLY);
            usePaneLayout = Screen.isLandscape(this);

            // if we have a preset to auto apply it doesn't make sense to show the Preset tab
            if (presetsToApply != null && !presetsToApply.isEmpty()) {
                PresetElement alternativeRootElement = Preset.getElementByPath(App.getCurrentRootPreset(this).getRootGroup(), presetsToApply.get(0));
                if (!(alternativeRootElement instanceof PresetGroup)) {
                    showPresets = false;
                }
            }
        } else {
            // Restore activity from saved state
            Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
            loadData = PropertyEditorData.deserializeArray(savedInstanceState.getSerializable(TAGEDIT_DATA));
            currentItem = savedInstanceState.getInt(CURRENTITEM, -1);
            usePaneLayout = savedInstanceState.getBoolean(PANELAYOUT); // FIXME this disables layout changes on
                                                                       // restarting
            StorageDelegator delegator = App.getDelegator();
            if (!delegator.isDirty() && delegator.isEmpty()) { // this can mean: need to load state
                Log.d(DEBUG_TAG, "Loading saved state");
                logic.syncLoadFromFile(this); // sync load
                App.getTaskStorage().readFromFile(this);
            }
        }

        Log.d(DEBUG_TAG, "... done.");

        // sanity check
        StorageDelegator delegator = App.getDelegator();
        if (delegator == null || loadData == null) {
            abort(delegator == null ? "Delegator null" : "loadData null");
            return;
        }

        osmIds = new long[loadData.length];
        types = new String[loadData.length];

        for (int i = 0; i < loadData.length; i++) {
            osmIds[i] = loadData[i].osmId;
            types[i] = loadData[i].type;
        }

        // we need the first element for stuff that doesn't support multi-select
        element = delegator.getOsmElement(types[0], osmIds[0]);
        if (element == null) {
            abort("Missing element(s)");
        }

        presets = App.getCurrentPresets(this);

        if (usePaneLayout) {
            setContentView(R.layout.pane_view);
            Log.d(DEBUG_TAG, "Using layout for large devices");
        } else {
            setContentView(R.layout.tab_view);
        }

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.propertyEditorBar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        setSupportActionBar(toolbar);

        // FIXME currently we statically change this, it would be nicer to actually make it dependent on if we have
        // actually changed something
        getSupportActionBar().setHomeAsUpIndicator(ThemeUtils.getResIdFromAttribute(this, R.attr.propertyeditor_done));

        // tags
        ArrayList<LinkedHashMap<String, String>> tags = new ArrayList<>();
        originalTags = new ArrayList<>();
        for (PropertyEditorData aLoadData : loadData) {
            originalTags.add(aLoadData.originalTags != null ? aLoadData.originalTags : aLoadData.tags);
            tags.add(aLoadData.tags);
        }

        if (loadData.length == 1) { // for now no support of relations and form for multi-select
            // parent relations
            originalParents = loadData[0].originalParents != null ? loadData[0].originalParents : loadData[0].parents;

            if (types[0].endsWith(Relation.NAME)) {
                // members of this relation
                originalMembers = loadData[0].originalMembers != null ? loadData[0].originalMembers : loadData[0].members;
                isRelation = true;
            }

            formEnabled = prefs.tagFormEnabled();
        }

        boolean rtl = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration config = getResources().getConfiguration();
            rtl = config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        }
        pagerAdapter = new PropertyEditorPagerAdapter(getSupportFragmentManager(), rtl, tags);
        mViewPager = (ExtendedViewPager) findViewById(R.id.pager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) mViewPager.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(this, R.attr.colorAccent, R.color.dark_grey));

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(true);

        if (usePaneLayout) { // add both preset fragments to panes
            Log.d(DEBUG_TAG, "Adding MRU prests");
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
            if (recentPresetsFragment != null) {
                ft.remove(recentPresetsFragment);
            }
            recentPresetsFragment = RecentPresetsFragment.newInstance(getElement().getOsmId(), getElement().getName()); // FIXME
                                                                                                                        // collect
                                                                                                                        // tags
            ft.add(R.id.recent_preset_row, recentPresetsFragment, RECENTPRESETS_FRAGMENT);

            presetFragment = (PresetFragment) fm.findFragmentByTag(PRESET_FRAGMENT);
            if (presetFragment != null) {
                ft.remove(presetFragment);
            }
            presetFragment = PresetFragment.newInstance(getElement().getOsmId(), getElement().getName(), presetsToApply, true); // FIXME
                                                                                                                                // collect
                                                                                                                                // tags
            ft.add(R.id.preset_row, presetFragment, PRESET_FRAGMENT);

            ft.commit();
        }

        mViewPager.setOffscreenPageLimit(4); // FIXME currently this is required or else some of the logic between the
                                             // fragments will not work
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.addOnPageChangeListener(new PageChangeListener());
        // if currentItem is >= 0 then we are restoring and should use it, otherwise the first or 2nd page
        mViewPager.setCurrentItem(currentItem != -1 ? currentItem : pagerAdapter.reversePosition(showPresets || usePaneLayout ? 0 : 1));
    }

    /**
     * Abort this activity
     * 
     * @param cause String showing a cause for this
     */
    private void abort(String cause) {
        Snack.toastTopError(this, R.string.toast_inconsistent_state);
        Log.e(DEBUG_TAG, "Inconsistent state because " + cause);
        ACRA.getErrorReporter().putCustomData("CAUSE", cause);
        ACRA.getErrorReporter().handleException(null);
        finish();
    }

    @Override
    protected void onStart() {
        Log.d(DEBUG_TAG, "onStart");
        super.onStart();
        Log.d(DEBUG_TAG, "onStart done");
    }

    @Override
    protected void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        running = true;
        Address.loadLastAddresses(this);
        Log.d(DEBUG_TAG, "onResume done");
    }

    @Override
    protected void onStop() {
        Log.d(DEBUG_TAG, "onStop");
        // save tag clipboard
        App.getTagClipboard(this).save(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        Util.clearCaches(this, newConfig);
        super.onConfigurationChanged(newConfig);
        this.recreate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Due to a problem of not being able to intercept android.R.id.home in fragments on older android versions
        // we start passing the event to the currently displayed fragment.
        // REF: http://stackoverflow.com/questions/21938419/intercepting-actionbar-home-button-in-fragment
        Fragment fragment = ((PropertyEditorPagerAdapter) mViewPager.getAdapter()).getItem(false, mViewPager.getCurrentItem());
        if (item.getItemId() == android.R.id.home && fragment != null && fragment.getView() != null && fragment.onOptionsItemSelected(item)) {
            Log.d(DEBUG_TAG, "called fragment onOptionsItemSelected");
            return true;
        }
        if (item.getItemId() == R.id.menu_preset_feedback) { // only used in pane mode
            Feedback.start(this, Github.PRESET_REPO_USER, Github.PRESET_REPO_NAME);
            return true;
        }
        if (item.getItemId() == R.id.menu_config) {
            PrefEditor.start(this, PREFERENCES_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(DEBUG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == SelectFile.READ_FILE || requestCode == SelectFile.READ_FILE_OLD || requestCode == SelectFile.SAVE_FILE)
                && resultCode == RESULT_OK) {
            SelectFile.handleResult(requestCode, data);
        } else if (requestCode == PREFERENCES_CODE) {
            // Preferences may have been changed
            prefs = new Preferences(this);
            App.getLogic().setPrefs(prefs);
        }
    }

    @Override
    public boolean onTop(Fragment me) {
        int item = mViewPager.getCurrentItem();
        PropertyEditorPagerAdapter adapter = (PropertyEditorPagerAdapter) mViewPager.getAdapter();
        Fragment top = adapter.getItem(false, item);
        Log.d(DEBUG_TAG, "onTop " + item + " " + (top != null ? top.getClass().getCanonicalName() : "null"));
        return me == top;
    }

    public class PropertyEditorPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<LinkedHashMap<String, String>> tags;
        private boolean                                  restoring = false;
        private boolean                                  rtl       = false;

        /**
         * Construct a new PagerAdapter
         * 
         * 
         * @param fm out FragementManager
         * @param rtl true if we should use RTL order for the fragments
         * @param tags the tags
         */
        public PropertyEditorPagerAdapter(FragmentManager fm, boolean rtl, ArrayList<LinkedHashMap<String, String>> tags) {
            super(fm);
            this.tags = tags;
            this.rtl = rtl;
        }

        @Override
        public int getCount() {
            int pages = 0;
            if (loadData.length == 1) {
                if (types[0].endsWith(Relation.NAME)) {
                    pages = 4;
                } else {
                    pages = 3;
                }
                if (formEnabled) {
                    pages++;
                }
            } else {
                pages = 2;
            }
            return usePaneLayout ? pages - 1 : pages; // preset page not in pager
        }

        /**
         * Get a new TagFormFragment instance
         * 
         * @param position position in the Pager
         * @param displayRecentPresets if true display the MRU Fragment
         * @return a TagFormFragment instance
         */
        Fragment tagFormFragment(int position, boolean displayRecentPresets) {
            tagFormFragmentPosition = position;
            tagFormFragment = TagFormFragment.newInstance(displayRecentPresets, applyLastAddressTags, loadData[0].focusOnKey);
            return tagFormFragment;
        }

        /**
         * Get a new TagEditorFragment instance
         * 
         * @param position position in the Pager
         * @param displayRecentPresets if true display the MRU Fragment
         * @return a TagEditorFragment instance
         */
        Fragment tagEditorFragment(int position, boolean displayRecentPresets) {
            tagEditorFragmentPosition = position;
            tagEditorFragment = TagEditorFragment.newInstance(osmIds, types, tags, applyLastAddressTags, loadData[0].focusOnKey, displayRecentPresets,
                    extraTags, presetsToApply);
            return tagEditorFragment;
        }

        /**
         * Get a new RelationMembershipFragment instance
         * 
         * @return a RelationMembershipFragment instance
         */
        Fragment relationMembershipFragment() {
            if (loadData.length == 1) {
                relationMembershipFragment = RelationMembershipFragment.newInstance(loadData[0].parents, types[0]);
                return relationMembershipFragment;
            }
            return null;
        }

        /**
         * Get a new RelationMembersFragment instance
         * 
         * @return a new RelationMembersFragment instance
         */
        Fragment relationMembersFragment() {
            if (loadData.length == 1 && types[0].endsWith(Relation.NAME)) {
                relationMembersFragment = RelationMembersFragment.newInstance(osmIds[0], loadData[0].members);
                return relationMembersFragment;
            }
            return null;
        }

        @Override
        public Fragment getItem(int position) {
            return getItem(true, position);
        }

        /**
         * Get the Fragment associated with a specific position in the Pager
         * 
         * @param instantiate if true instantiate the Fragment
         * @param position the position
         * @return a Fragment
         */
        public Fragment getItem(boolean instantiate, int position) {
            Log.d(DEBUG_TAG, "getItem " + instantiate + " " + position);
            position = reversePosition(position);
            if (formEnabled) {
                if (!usePaneLayout) {
                    switch (position) {
                    case 0:
                        if (instantiate) {
                            presetFragment = PresetFragment.newInstance(getElement().getOsmId(), getElement().getName(), presetsToApply, false); //
                        }
                        return presetFragment;
                    case 1:
                        return instantiate ? tagFormFragment(position, true) : tagFormFragment;
                    case 2:
                        return instantiate ? tagEditorFragment(position, false) : tagEditorFragment;
                    case 3:
                        return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment)
                                : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
                    case 4:
                        return instantiate ? relationMembershipFragment() : relationMembershipFragment;
                    }
                } else {
                    switch (position) {
                    case 0:
                        return instantiate ? tagFormFragment(position, false) : tagFormFragment;
                    case 1:
                        return instantiate ? tagEditorFragment(position, false) : tagEditorFragment;
                    case 2:
                        return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment)
                                : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
                    case 3:
                        return instantiate ? relationMembershipFragment() : relationMembershipFragment;
                    }
                }
            } else {
                if (!usePaneLayout) {
                    switch (position) {
                    case 0:
                        if (instantiate) {
                            presetFragment = PresetFragment.newInstance(getElement().getOsmId(), getElement().getName(), presetsToApply, false); //
                        }
                        return presetFragment;
                    case 1:
                        return instantiate ? tagEditorFragment(position, true) : tagEditorFragment;
                    case 2:
                        return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment)
                                : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
                    case 3:
                        return instantiate ? relationMembershipFragment() : relationMembershipFragment;
                    }
                } else {
                    switch (position) {
                    case 0:
                        return instantiate ? tagEditorFragment(position, false) : tagEditorFragment;
                    case 1:
                        return isRelation ? (instantiate ? relationMembersFragment() : relationMembersFragment)
                                : (instantiate ? relationMembershipFragment() : relationMembershipFragment);
                    case 2:
                        return instantiate ? relationMembershipFragment() : relationMembershipFragment;
                    }
                }
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (formEnabled) {
                position = reversePosition(position);
                if (!usePaneLayout) {
                    switch (position) {
                    case 0:
                        return getString(R.string.tag_menu_preset);
                    case 1:
                        return getString(R.string.menu_tags);
                    case 2:
                        return getString(R.string.tag_details);
                    case 3:
                        return isRelation ? getString(R.string.members) : getString(R.string.relations);
                    case 4:
                        return getString(R.string.relations);
                    }
                } else {
                    switch (position) {
                    case 0:
                        return getString(R.string.menu_tags);
                    case 1:
                        return getString(R.string.tag_details);
                    case 2:
                        return isRelation ? getString(R.string.members) : getString(R.string.relations);
                    case 3:
                        return getString(R.string.relations);
                    }
                }
            } else {
                if (!usePaneLayout) {
                    switch (position) {
                    case 0:
                        return getString(R.string.tag_menu_preset);
                    case 1:
                        return getString(R.string.menu_tags);
                    case 2:
                        return isRelation ? getString(R.string.members) : getString(R.string.relations);
                    case 3:
                        return getString(R.string.relations);
                    }
                } else {
                    switch (position) {
                    case 0:
                        return getString(R.string.menu_tags);
                    case 1:
                        return isRelation ? getString(R.string.members) : getString(R.string.relations);
                    case 2:
                        return getString(R.string.relations);
                    }
                }
            }
            return "error";
        }

        /**
         * If RTL layout reverse the position
         * 
         * @param position the position
         * @return the reversed position if an RTL layout
         */
        int reversePosition(int position) {
            if (rtl) {
                position = getCount() - position - 1;
            }
            return position;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            BaseFragment fragment = (BaseFragment) super.instantiateItem(container, position);
            // update fragment refs here
            if (fragment instanceof TagFormFragment) {
                tagFormFragment = (TagFormFragment) fragment;
                Log.d(DEBUG_TAG, "Restored ref to TagFormFragment");
            } else if (fragment instanceof TagEditorFragment) {
                tagEditorFragment = (TagEditorFragment) fragment;
                Log.d(DEBUG_TAG, "Restored ref to TagEditorFragment");
            } else if (fragment instanceof RelationMembershipFragment) {
                relationMembershipFragment = (RelationMembershipFragment) fragment;
                Log.d(DEBUG_TAG, "Restored ref to RelationMembershipFragment");
            } else if (fragment instanceof RelationMembersFragment) {
                relationMembersFragment = (RelationMembersFragment) fragment;
                Log.d(DEBUG_TAG, "Restored ref to RelationMembersFragment");
            } else if (fragment instanceof PresetFragment) {
                presetFragment = (PresetFragment) fragment;
                Log.d(DEBUG_TAG, "Restored ref to PresetFragment");
            } else {
                Log.d(DEBUG_TAG, "Unknown fragment ...");
            }
            // hack to recreate the form ui when restoring as there is no callback that
            // runs after the references here have been recreated
            if (restoring && tagFormFragment != null && tagEditorFragment != null) {
                tagsUpdated();
            }
            return fragment;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            Log.d(DEBUG_TAG, "restoreState");
            super.restoreState(state, loader);
            restoring = true;
            Log.d(DEBUG_TAG, "restoreState done");
        }

        @Override
        public Parcelable saveState() {
            Log.d(DEBUG_TAG, "saveState");
            Bundle bundle = (Bundle) super.saveState();
            Log.d(DEBUG_TAG, "saveState done");
            return bundle;
        }
    }

    private class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int page) {
            Log.d(DEBUG_TAG, "page " + page + " selected");
            if (formEnabled && page == tagFormFragmentPosition && tagFormFragment != null) {
                tagFormFragment.update();
            }
            if (page == tagEditorFragmentPosition && tagEditorFragment != null) {
                tagEditorFragment.focusOnEmptyValue();
            }
        }
    }

    /**
     * Removes an old RecentPresetView and replaces it by a new one (to update it)
     */
    public void recreateRecentPresetView() {
        if (usePaneLayout) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
            if (recentPresetsFragment != null) {
                ((RecentPresetsFragment) recentPresetsFragment).recreateRecentPresetView();
            }
        } else {
            FragmentManager fm;
            if (tagFormFragment != null) {
                fm = tagFormFragment.getChildFragmentManager();
            } else {
                fm = tagEditorFragment.getChildFragmentManager();
            }
            recreateRecentPresetView(fm);
        }
    }

    /**
     * Recreate the MRU list of Presets
     * 
     * @param fm the child fragment manager for the fragment
     */
    void recreateRecentPresetView(@NonNull FragmentManager fm) {
        Log.d(DEBUG_TAG, "Updating MRU prests");
        Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
        if (recentPresetsFragment != null) {
            ((RecentPresetsFragment) recentPresetsFragment).recreateRecentPresetView();
        }
    }

    @Override
    public void onBackPressed() {
        if (tagFormFragment != null) {
            tagFormFragment.updateEditorFromText(); // update any non-synced changes to the editor fragment
        }
        List<LinkedHashMap<String, String>> currentTags = getUpdatedTags();
        MultiHashMap<Long, RelationMemberPosition> currentParents = null;
        List<RelationMemberDescription> currentMembers = null;
        if (relationMembershipFragment != null) {
            currentParents = relationMembershipFragment.getParentRelationMap();
        }
        if (relationMembersFragment != null) {
            currentMembers = new ArrayList<>(); // FIXME
            if (types[0].equals(Relation.NAME)) { // FIXME
                currentMembers = relationMembersFragment.getMembersList();
            }
        }
        // if we haven't edited just exit
        if (!same(currentTags, originalTags) // tags different
                || ((currentParents != null && !currentParents.equals(originalParents))
                        && !(originalParents == null && (currentParents == null || currentParents.isEmpty()))) // parents
                                                                                                               // changed
                || (getElement() != null && getElement().getName().equals(Relation.NAME)
                        && (currentMembers != null && !sameMembers(currentMembers, originalMembers)))) {
            new AlertDialog.Builder(this).setNeutralButton(R.string.cancel, null)
                    .setNegativeButton(R.string.tag_menu_revert, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            doRevert();
                        }
                    }).setPositiveButton(R.string.tag_menu_exit_no_save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            PropertyEditor.super.onBackPressed();
                        }
                    }).create().show();
        } else {
            PropertyEditor.super.onBackPressed();
        }
    }

    /**
     * Revert changes in all fragments
     */
    private void doRevert() {
        if (tagEditorFragment != null) {
            tagEditorFragment.doRevert();
        }
        if (relationMembershipFragment != null) {
            relationMembershipFragment.doRevert();
        }
        if (relationMembersFragment != null) {
            relationMembersFragment.doRevert();
        }
    }

    /**
     * Get current values from the fragments and end the activity
     */
    public void sendResultAndFinish() {

        List<LinkedHashMap<String, String>> currentTags = getUpdatedTags();
        if (currentTags != null) {
            // save any address tags for "last address tags"
            if (currentTags.size() == 1) {
                Address.updateLastAddresses(tagEditorFragment, Util.getListMap(currentTags.get(0)));// FIXME
            }
            Intent intent = new Intent();

            MultiHashMap<Long, RelationMemberPosition> currentParents = null;
            ArrayList<RelationMemberDescription> currentMembers = null;
            PropertyEditorData[] newData = new PropertyEditorData[currentTags.size()];

            if (currentTags.size() == 1) { // normal single mode, relations might have changed
                currentParents = relationMembershipFragment.getParentRelationMap();
                currentMembers = new ArrayList<>(); // FIXME
                if (types[0].endsWith(Relation.NAME)) {
                    currentMembers = relationMembersFragment.getMembersList();
                }

                if (!same(currentTags, originalTags) || !(originalParents == null && currentParents.isEmpty()) && !currentParents.equals(originalParents)
                        || (getElement() != null && getElement().getName().equals(Relation.NAME) && !currentMembers.equals(originalMembers))) {
                    // changes were made
                    Log.d(DEBUG_TAG, "saving tags");
                    for (int i = 0; i < currentTags.size(); i++) {
                        newData[i] = new PropertyEditorData(osmIds[i], types[i], currentTags.get(i).equals(originalTags.get(i)) ? null : currentTags.get(i),
                                null, (originalParents == null && currentParents.isEmpty()) || currentParents.equals(originalParents) ? null : currentParents,
                                null, currentMembers.equals(originalMembers) ? null : currentMembers, null);
                    }
                }
            } else { // multi select just tags could have been changed
                if (!same(currentTags, originalTags)) {
                    // changes were made
                    for (int i = 0; i < currentTags.size(); i++) {
                        newData[i] = new PropertyEditorData(osmIds[i], types[i], currentTags.get(i).equals(originalTags.get(i)) ? null : currentTags.get(i),
                                null, null, null, null, null);
                    }
                }
            }

            intent.putExtra(TAGEDIT_DATA, newData);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    /**
     * Check if two lists of tags are the same
     * 
     * Note: this considers order relevant
     * 
     * @param tags1 first list of tags
     * @param tags2 second list of tags
     * @return true if the lists are the same
     */
    private boolean same(@Nullable List<LinkedHashMap<String, String>> tags1, @Nullable List<LinkedHashMap<String, String>> tags2) {
        if (tags1 == null) {
            return tags2 == null;
        }
        if (tags2 == null) {
            return tags1 == null;
        }
        // check for tag changes
        if (tags1.size() != tags2.size()) { /// serious error
            return false;
        }
        for (int i = 0; i < tags1.size(); i++) {
            if (!tags1.get(i).equals(tags2.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if two lists of RelationMembetDescription are the same
     * 
     * Note: this considers order relevant
     * 
     * @param rmds1 first list of members
     * @param rmds2 second list of members
     * @return true if the lists contain the same members
     */
    private boolean sameMembers(@Nullable List<RelationMemberDescription> rmds1, @Nullable List<RelationMemberDescription> rmds2) {
        if (rmds1 == null) {
            return rmds2 == null;
        }
        if (rmds2 == null) {
            return rmds1 == null;
        }
        if (rmds1.size() != rmds2.size()) { /// serious error
            return false;
        }
        for (int i = 0; i < rmds1.size(); i++) {
            RelationMemberDescription rmd1 = rmds1.get(i);
            RelationMemberDescription rmd2 = rmds2.get(i);
            if (rmd1 == rmd2) {
                continue;
            }
            if (rmd1 != null && !rmd1.equals(rmd2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Save the state of this activity instance for future restoration.
     * 
     * @param outState The object to receive the saved state.
     */
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Log.d(DEBUG_TAG, "bundle size 1 : " + Util.getBundleSize(outState));
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "bundle size 2 : " + Util.getBundleSize(outState));
        outState.putInt(CURRENTITEM, mViewPager.getCurrentItem());
        outState.putBoolean(PANELAYOUT, usePaneLayout);
        outState.putSerializable(TAGEDIT_DATA, loadData);
        App.getMruTags().save(this);
    }

    /** When the Activity is interrupted, save MRUs and address cache */
    @Override
    protected void onPause() {
        running = false;
        Preset[] presets = App.getCurrentPresets(this);
        if (presets != null) {
            for (Preset p : presets) {
                if (p != null) {
                    p.saveMRU();
                }
            }
        }
        Address.saveLastAddresses(this);
        super.onPause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(DEBUG_TAG, "onRestoreInstanceState done");
    }

    /**
     * Insert a new row of key+value -edit-widgets if some text is entered into the current one.
     * 
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class MyKeyListener implements OnKeyListener {
        @Override
        public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
                if (view instanceof EditText) {
                    // on Enter -> goto next EditText
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        View nextView = view.focusSearch(View.FOCUS_RIGHT);
                        if (!(nextView instanceof EditText)) {
                            nextView = view.focusSearch(View.FOCUS_LEFT);
                            if (nextView != null) {
                                nextView = nextView.focusSearch(View.FOCUS_DOWN);
                            }
                        }
                        if (nextView instanceof EditText) {
                            nextView.requestFocus();
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    @Override
    public void onPresetSelected(PresetItem item) {
        onPresetSelected(item, false);
    }

    @Override
    public void onPresetSelected(PresetItem item, boolean applyOptional) {
        if (item != null && tagEditorFragment != null) {
            tagEditorFragment.applyPreset(item, applyOptional, true);
            if (tagFormFragment != null) {
                tagFormFragment.update();
                mViewPager.setCurrentItem(tagFormFragmentPosition);
            } else {
                mViewPager.setCurrentItem(tagEditorFragmentPosition);
            }
            // utility presets need to be explicitly added, while this duplicates adding item in other cases
            // it has the nice side effect of moving item to the top
            tagEditorFragment.addToMru(presets, item);
            recreateRecentPresetView();
        }
    }

    /**
     * Allow ViewPager to work
     */
    public void enablePaging() {
        mViewPager.setPagingEnabled(true);
    }

    /**
     * Disallow ViewPAger to work
     */
    public void disablePaging() {
        mViewPager.setPagingEnabled(false);
    }

    /**
     * Allow presets to be applied
     */
    public void enablePresets() {
        if (usePaneLayout) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
            if (recentPresetsFragment != null) {
                ((RecentPresetsFragment) recentPresetsFragment).enable();
            }
        } else {
            tagEditorFragment.enableRecentPresets();
        }
    }

    /**
     * Disallow presets to be applied
     */
    public void disablePresets() {
        if (usePaneLayout) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment recentPresetsFragment = fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
            if (recentPresetsFragment != null) {
                ((RecentPresetsFragment) recentPresetsFragment).disable();
            }
            presetFragment.disable();
        } else {
            tagEditorFragment.disableRecentPresets();
        }
    }

    @Override
    public void updateSingleValue(@NonNull String key, @NonNull String value) {
        if (tagEditorFragment != null) {
            tagEditorFragment.updateSingleValue(key, value);
        } else {
            Log.e(DEBUG_TAG, "updateSingleValue tagEditorFragment is null");
        }
    }

    @Override
    public void updateTags(@NonNull Map<String, String> tags, boolean flush) {
        if (tagEditorFragment != null) {
            tagEditorFragment.updateTags(tags, flush);
        } else {
            Log.e(DEBUG_TAG, "updateSingleValue tagEditorFragment is null");
        }
    }

    @Override
    public void revertTags() {
        if (tagEditorFragment != null) {
            tagEditorFragment.revertTags();
        } else {
            Log.e(DEBUG_TAG, "revertTags tagEditorFragment is null");
        }
    }

    @Override
    public void deleteTag(final String key) {
        if (tagEditorFragment != null) {
            tagEditorFragment.deleteTag(key);
        } else {
            Log.e(DEBUG_TAG, "deleteTag tagEditorFragment is null");
        }
    }

    @Override
    @Nullable
    public List<LinkedHashMap<String, String>> getUpdatedTags() {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getUpdatedTags();
        } else {
            Log.e(DEBUG_TAG, "getUpdatedTags tagEditorFragment is null");
            return null;
        }
    }

    @Override
    @Nullable
    public LinkedHashMap<String, String> getKeyValueMapSingle(boolean allowBlanks) {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getKeyValueMapSingle(allowBlanks);
        } else {
            Log.e(DEBUG_TAG, "getUpdatedTags tagEditorFragment is null");
            return null;
        }
    }

    @Override
    public PresetItem getBestPreset() {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getBestPreset();
        } else {
            Log.e(DEBUG_TAG, "getBestPreset tagEditorFragment is null");
            return null;
        }
    }

    @Override
    public List<PresetItem> getSecondaryPresets() {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getSecondaryPresets();
        } else {
            Log.e(DEBUG_TAG, "getSecondaryPresets tagEditorFragment is null");
            return null;
        }
    }

    @Override
    @Nullable
    public Map<String, PresetItem> getAllPresets() {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getAllPresets();
        } else {
            Log.e(DEBUG_TAG, "getAllPresets tagEditorFragment is null");
            return null;
        }
    }

    @Override
    public void updatePresets() {
        if (tagEditorFragment != null) {
            tagEditorFragment.updatePresets();
        } else {
            Log.e(DEBUG_TAG, "updatePresets tagEditorFragment is null");
        }
    }

    @Override
    public void predictAddressTags(boolean allowBlanks) {
        if (tagEditorFragment != null) {
            tagEditorFragment.predictAddressTags(allowBlanks);
        } else {
            Log.e(DEBUG_TAG, "predictAddressTags tagEditorFragment is null");
        }
    }

    @Override
    public void applyTagSuggestions(TagMap tags, Runnable afterApply) {
        if (tagEditorFragment != null) {
            tagEditorFragment.applyTagSuggestions(tags, afterApply);
        } else {
            Log.e(DEBUG_TAG, "applyTagSuggestions tagEditorFragment is null");
        }
    }

    @Override
    public boolean paste(boolean replace) {
        if (tagEditorFragment != null) {
            return tagEditorFragment.paste(replace);
        } else {
            Log.e(DEBUG_TAG, "paste tagEditorFragment is null");
        }
        return false;
    }

    @Override
    public boolean pasteFromClipboardIsPossible() {
        if (tagEditorFragment != null) {
            return tagEditorFragment.pasteFromClipboardIsPossible();
        } else {
            Log.e(DEBUG_TAG, "pasteFromClipboardIsPossible tagEditorFragment is null");
        }
        return false;
    }

    @Override
    public boolean pasteFromClipboard(boolean replace) {
        if (tagEditorFragment != null) {
            return tagEditorFragment.pasteFromClipboard(replace);
        } else {
            Log.e(DEBUG_TAG, "pasteFromClipboard tagEditorFragment is null");
        }
        return false;
    }

    @Override
    public void tagsUpdated() {
        if (tagFormFragment != null) {
            tagFormFragment.tagsUpdated();
        } else {
            Log.e(DEBUG_TAG, "tagFormFragment is null");
        }
    }

    @Override
    public void update(ElementType type) {
        if (presetFragment != null) {
            presetFragment.update(type);
        } else {
            Log.e(DEBUG_TAG, "presetFragment is null");
        }

    }

    @Override
    public ArrayAdapter<ValueWithCount> getStreetNameAdapter(List<String> values) {
        if (streetNameAutocompleteAdapter == null) {
            streetNameAutocompleteAdapter = new StreetPlaceNamesAdapter(this, R.layout.autocomplete_row, App.getDelegator(), types[0], osmIds[0], values,
                    false); // FIXME
        }
        return streetNameAutocompleteAdapter;
    }

    @Override
    public ArrayAdapter<ValueWithCount> getPlaceNameAdapter(List<String> values) {
        if (placeNameAutocompleteAdapter == null) {
            placeNameAutocompleteAdapter = new StreetPlaceNamesAdapter(this, R.layout.autocomplete_row, App.getDelegator(), types[0], osmIds[0], values, true); // FIXME
        }
        return placeNameAutocompleteAdapter;
    }

    @Override
    public OsmElement getElement() {
        return element; // FIXME validate
    }

    /**
     * Return if we are using the pave/tablet layout
     * 
     * @return true is in pane mode
     */
    boolean paneLayout() {
        return usePaneLayout;
    }

    @Override
    /**
     * Workaround for bug mentioned below
     */
    public ActionMode startSupportActionMode(@NonNull final ActionMode.Callback callback) {
        // Fix for bug https://code.google.com/p/android/issues/detail?id=159527
        final ActionMode mode = super.startSupportActionMode(callback);
        if (mode != null) {
            mode.invalidate();
        }
        return mode;
    }

    @Override
    /**
     * A tag has been updated, reflect this in both editors
     */
    public void save(String key, String value) {
        updateSingleValue(key, value);
        tagsUpdated();
    }

    @Override
    public void applyPreset(@NonNull PresetItem preset, boolean addOptional) {
        tagEditorFragment.applyPreset(preset, addOptional);
    }

    @Override
    public boolean isConnected() {
        if (networkStatus == null) {
            networkStatus = new NetworkStatus(this);
        }
        return networkStatus.isConnected();
    }

    @Override
    public boolean isConnectedOrConnecting() {
        if (networkStatus == null) {
            networkStatus = new NetworkStatus(this);
        }
        return networkStatus.isConnectedOrConnecting();
    }

    @Override
    public List<String> getIsoCodes() {
        if (isoCodes == null) {
            try {
                GeoContext geoContext = App.getGeoContext(this);
                if (geoContext != null) {
                    isoCodes = geoContext.getIsoCodes(getElement());
                }
            } catch (IllegalArgumentException iaex) {
                Log.e(DEBUG_TAG, "getIsoCodes " + iaex + " for " + getElement().getType() + " " + getElement().getOsmId());
            }
        }
        return isoCodes;
    }

    @Override
    public String getCountryIsoCode() {
        return GeoContext.getCountryIsoCode(getIsoCodes());
    }

    @Override
    public Preset[] getPresets() {
        return presets;
    }

    @Override
    public boolean updateEditorFromText() {
        // This is only used internally by the TagFormFragment
        throw new IllegalOperationException("updateEditorFromText can only be called internally");
    }
}
