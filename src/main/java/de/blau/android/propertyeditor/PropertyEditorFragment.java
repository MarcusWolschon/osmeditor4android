package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.Feedback;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.address.Address;
import de.blau.android.contract.Github;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.exception.DuplicateKeyException;
import de.blau.android.exception.IllegalOperationException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.nsi.Names.TagMap;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.propertyeditor.tagform.TagFormFragment;
import de.blau.android.util.BaseFragment;
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
public class PropertyEditorFragment extends BaseFragment implements PropertyEditorListener, OnPresetSelectedListener, EditorUpdate, FormUpdate, PresetUpdate,
        NameAdapters, OnSaveListener, ch.poole.openinghoursfragment.OnSaveListener {

    private static final String CURRENTITEM            = "current_item";
    static final String         PANELAYOUT             = "pane_layout";
    private static final String PRESET_FRAGMENT        = "preset_fragment";
    public static final String  RECENTPRESETS_FRAGMENT = "recentpresets_fragment";

    public static final String TAGEDIT_DATA              = "dataClass";
    static final String        TAGEDIT_LAST_ADDRESS_TAGS = "applyLastTags";
    static final String        TAGEDIT_SHOW_PRESETS      = "showPresets";
    static final String        TAGEDIT_EXTRA_TAGS        = "extra";
    static final String        TAGEDIT_PRESETSTOAPPLY    = "presetsToApply";

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

    /**
     * The tag we use for Android-logging.
     */
    private static final String DEBUG_TAG = PropertyEditorFragment.class.getSimpleName();

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

    private Preferences        prefs         = null;
    private ExtendedViewPager  mViewPager;
    private boolean            usePaneLayout = false;
    private boolean            isRelation    = false;
    private NetworkStatus      networkStatus;
    private List<String>       isoCodes      = null;
    private ControlListener    controlListener;
    private PageChangeListener pageChangeListener;

    /**
     * Build the intent to start the PropertyEditor
     * 
     * @param dataClass the tags and relation memberships that should be edited
     * @param predictAddressTags try to predict address tags
     * @param showPresets show the preset tab first
     * @param extraTags additional tags that should be added
     * @param presetItems presets that should be applied
     * @param usePaneLayout option control of layout
     * @return a suitable Intent
     */
    @NonNull
    public static PropertyEditorFragment newInstance(@NonNull PropertyEditorData[] dataClass, boolean predictAddressTags, boolean showPresets,
            @Nullable HashMap<String, String> extraTags, @Nullable ArrayList<PresetElementPath> presetItems, @Nullable Boolean usePaneLayout) {
        PropertyEditorFragment f = new PropertyEditorFragment();

        Bundle args = new Bundle();
        args.putSerializable(TAGEDIT_DATA, dataClass);
        args.putBoolean(TAGEDIT_LAST_ADDRESS_TAGS, predictAddressTags);
        args.putBoolean(TAGEDIT_SHOW_PRESETS, showPresets);
        args.putSerializable(TAGEDIT_EXTRA_TAGS, extraTags);
        args.putSerializable(TAGEDIT_PRESETSTOAPPLY, presetItems);
        if (usePaneLayout != null) {
            args.putBoolean(TAGEDIT_SHOW_PRESETS, usePaneLayout);
        }
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        Util.implementsInterface(context, ControlListener.class);
        controlListener = (ControlListener) context;
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
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
            prefs = new Preferences(getContext());
            logic.setPrefs(prefs);
        }

        super.onCreate(savedInstanceState);

        // tags
        if (savedInstanceState == null) {
            // No previous state to restore - get the state from the intent
            Log.d(DEBUG_TAG, "Initializing from intent");
            final Bundle args = getArguments();
            loadData = PropertyEditorData.deserializeArray(args.getSerializable(TAGEDIT_DATA));
            applyLastAddressTags = args.getBoolean(TAGEDIT_LAST_ADDRESS_TAGS);
            showPresets = args.getBoolean(TAGEDIT_SHOW_PRESETS);
            extraTags = (HashMap<String, String>) args.getSerializable(TAGEDIT_EXTRA_TAGS);
            presetsToApply = (ArrayList<PresetElementPath>) args.getSerializable(TAGEDIT_PRESETSTOAPPLY);
            usePaneLayout = args.getBoolean(PANELAYOUT, Screen.isLandscape(getActivity()));

            // if we have a preset to auto apply it doesn't make sense to show the Preset tab except if a group is
            // selected
            if (presetsToApply != null && !presetsToApply.isEmpty()) {
                PresetElement alternativeRootElement = Preset.getElementByPath(App.getCurrentRootPreset(getContext()).getRootGroup(), presetsToApply.get(0));
                showPresets = alternativeRootElement instanceof PresetGroup;
            }
        } else {
            // Restore activity from saved state
            Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
            loadData = PropertyEditorData.deserializeArray(savedInstanceState.getSerializable(TAGEDIT_DATA));
            usePaneLayout = savedInstanceState.getBoolean(PANELAYOUT); // FIXME this disables layout changes on
                                                                       // restarting
            StorageDelegator delegator = App.getDelegator();
            if (!delegator.isDirty() && delegator.isEmpty()) { // this can mean: need to load state
                Log.d(DEBUG_TAG, "Loading saved state");
                logic.syncLoadFromFile(getActivity()); // sync load
                App.getTaskStorage().readFromFile(getActivity());
            }
        }

        Log.d(DEBUG_TAG, "... done.");

        // sanity check
        if (loadData == null) {
            abort("loadData null");
            return;
        }

        osmIds = new long[loadData.length];
        types = new String[loadData.length];

        for (int i = 0; i < loadData.length; i++) {
            osmIds[i] = loadData[i].osmId;
            types[i] = loadData[i].type;
        }

        // we need the first element for stuff that doesn't support multi-select
        element = App.getDelegator().getOsmElement(types[0], osmIds[0]);
        if (element == null) {
            abort("Missing element(s)");
        }

        presets = App.getCurrentPresets(getContext());

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int currentItem = -1;
        if (savedInstanceState != null) {
            currentItem = savedInstanceState.getInt(CURRENTITEM, -1);
        }

        ViewGroup layout = (ViewGroup) inflater.inflate(usePaneLayout ? R.layout.pane_view : R.layout.tab_view, null);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) layout.findViewById(R.id.propertyEditorBar);
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        // FIXME currently we statically change this, it would be nicer to actually make it dependent on if we have
        // actually changed something
        ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionbar.setHomeAsUpIndicator(ThemeUtils.getResIdFromAttribute(getContext(), R.attr.propertyeditor_done));
        actionbar.setDisplayShowTitleEnabled(false);
        actionbar.setDisplayHomeAsUpEnabled(true);

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
        PropertyEditorPagerAdapter pagerAdapter = new PropertyEditorPagerAdapter(getChildFragmentManager(), rtl, tags);
        mViewPager = (ExtendedViewPager) layout.findViewById(R.id.pager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) mViewPager.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.colorAccent, R.color.dark_grey));

        if (usePaneLayout) { // add both preset fragments to panes
            Log.d(DEBUG_TAG, "Adding fragment to pane");
            FragmentManager fm = getChildFragmentManager();
            de.blau.android.propertyeditor.Util.addMRUPresetsFragment(fm, R.id.pane_mru_layout, getElement().getOsmId(), getElement().getName());

            FragmentTransaction ft = fm.beginTransaction();
            presetFragment = (PresetFragment) fm.findFragmentByTag(PRESET_FRAGMENT);
            if (presetFragment != null) {
                ft.remove(presetFragment);
            }
            presetFragment = PresetFragment.newInstance(getElement().getOsmId(), getElement().getName(), presetsToApply, true);
            ft.add(R.id.preset_row, presetFragment, PRESET_FRAGMENT);
            ft.commit();
        }

        mViewPager.setOffscreenPageLimit(4); // FIXME currently this is required or else some of the logic between the
                                             // fragments will not work
        mViewPager.setAdapter(pagerAdapter);
        pageChangeListener = new PageChangeListener();
        mViewPager.addOnPageChangeListener(pageChangeListener);
        // if currentItem is >= 0 then we are restoring and should use it, otherwise the first or 2nd page
        mViewPager.setCurrentItem(currentItem != -1 ? currentItem : pagerAdapter.reversePosition(showPresets || usePaneLayout ? 0 : 1));

        return layout;
    }

    /**
     * Abort this activity
     * 
     * @param cause String showing a cause for this
     */
    private void abort(@NonNull String cause) {
        Snack.toastTopError(getContext(), R.string.toast_inconsistent_state);
        Log.e(DEBUG_TAG, "Inconsistent state because " + cause);
        ACRA.getErrorReporter().putCustomData("CAUSE", cause);
        ACRA.getErrorReporter().handleException(null);
        controlListener.finished(this);
    }

    @Override
    public void onResume() {
        Log.d(DEBUG_TAG, "onResume");
        super.onResume();
        Address.loadLastAddresses(getContext());
        Log.d(DEBUG_TAG, "onResume done");
    }

    @Override
    public void onStop() {
        Log.d(DEBUG_TAG, "onStop");
        // save tag clipboard
        App.getTagClipboard(getContext()).save(getContext());
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        Util.clearCaches(getContext(), newConfig);
        super.onConfigurationChanged(newConfig);
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
            Feedback.start(getContext(), Github.PRESET_REPO_USER, Github.PRESET_REPO_NAME, prefs.useUrlForFeedback());
            return true;
        }
        if (item.getItemId() == R.id.menu_config) {
            PrefEditor.start(getActivity(), PREFERENCES_CODE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(DEBUG_TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == SelectFile.READ_FILE || requestCode == SelectFile.SAVE_FILE) && resultCode == Activity.RESULT_OK) {
            SelectFile.handleResult(requestCode, data);
        } else if (requestCode == PREFERENCES_CODE) {
            // Preferences may have been changed
            prefs = new Preferences(getContext());
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
        private boolean                                  restoring   = false;
        private boolean                                  rtl         = false;
        private boolean                                  firstTime   = true;
        private int                                      primaryItem = -1;

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
        @NonNull
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
        @NonNull
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
        @Nullable
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
        @Nullable
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
        @Nullable
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
                    default:
                        // ERROR
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
                    default:
                        // ERROR
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
                    default:
                        // ERROR
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
                    default:
                        // ERROR
                    }
                }
            }
            Log.e(DEBUG_TAG, "Unknown position " + position);
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
                    default:
                        // ERROR
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
                    default:
                        // ERROR
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
                    default:
                        // ERROR
                    }
                } else {
                    switch (position) {
                    case 0:
                        return getString(R.string.menu_tags);
                    case 1:
                        return isRelation ? getString(R.string.members) : getString(R.string.relations);
                    case 2:
                        return getString(R.string.relations);
                    default:
                        // ERROR
                    }
                }
            }
            Log.e(DEBUG_TAG, "Unknown position " + position);
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
            Log.d(DEBUG_TAG, "Restoring ref to " + fragment.getClass().getSimpleName());
            if (fragment instanceof TagFormFragment) {
                tagFormFragment = (TagFormFragment) fragment;
                tagFormFragmentPosition = position;
            } else if (fragment instanceof TagEditorFragment) {
                tagEditorFragment = (TagEditorFragment) fragment;
                tagEditorFragmentPosition = position;
            } else if (fragment instanceof RelationMembershipFragment) {
                relationMembershipFragment = (RelationMembershipFragment) fragment;
            } else if (fragment instanceof RelationMembersFragment) {
                relationMembersFragment = (RelationMembersFragment) fragment;
            } else if (fragment instanceof PresetFragment) {
                presetFragment = (PresetFragment) fragment;
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

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            super.setPrimaryItem(container, position, object);
            primaryItem = position;
        }

        @Override
        public void finishUpdate(@NonNull ViewGroup container) {
            super.finishUpdate(container);
            // hack to update TagFormFragment on 1st pass if it hasn't happened
            if (firstTime && primaryItem != -1) {
                firstTime = false;
                pageChangeListener.onPageSelected(primaryItem);
            }
        }
    }

    private class PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int page) {
            if (page != tagEditorFragmentPosition && !validateTags()) {
                return;
            }
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
     * Get the current RecentPresetsFragment
     * 
     * @return the current RecentPresetsFragment or null if it can't be found
     */
    @Nullable
    RecentPresetsFragment getRecentPresetsFragment() {
        FragmentManager fm;
        if (usePaneLayout) {
            fm = getChildFragmentManager();
        } else {
            if (tagFormFragment != null) {
                fm = tagFormFragment.getChildFragmentManager();
            } else {
                fm = tagEditorFragment.getChildFragmentManager();
            }
        }
        return (RecentPresetsFragment) fm.findFragmentByTag(RECENTPRESETS_FRAGMENT);
    }

    @Override
    public void updateRecentPresets() {
        RecentPresetsFragment recentPresetsFragment = getRecentPresetsFragment();
        if (recentPresetsFragment != null) {
            recentPresetsFragment.recreateRecentPresetView();
        } else {
            Log.e(DEBUG_TAG, "RecentPresetsFragment not found");
        }
    }

    /**
     * Revert changes in all fragments
     */
    void doRevert() {
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
     * Validate tags, taking appropriate action if required
     * 
     * @return true if the tags are ok
     */
    public boolean validateTags() {
        if (tagEditorFragment != null) {
            try {
                tagEditorFragment.validate();
            } catch (DuplicateKeyException dkex) {
                if (mViewPager.getCurrentItem() != tagEditorFragmentPosition) {
                    mViewPager.setCurrentItem(tagEditorFragmentPosition);
                }
                ErrorAlert.showDialog(getActivity(), ErrorCodes.DUPLICATE_TAG_KEY, dkex.getKey());
                return false;
            }
        } else {
            Log.e(DEBUG_TAG, "validateTags tagEditorFragment is null");
        }
        return true;
    }

    /**
     * Get current values from the fragments and end the activity
     */
    public void updateAndFinish() {
        if (!validateTags()) {
            return;
        }

        List<LinkedHashMap<String, String>> currentTags = getUpdatedTags();

        // save any address tags for "last address tags"
        final int elementCount = currentTags.size();
        if (elementCount == 1) {
            Address.updateLastAddresses(getContext(), (StreetPlaceNamesAdapter) getStreetNameAdapter(null), tagEditorFragment.getType(),
                    tagEditorFragment.getOsmId(), Util.getListMap(currentTags.get(0)), true);
        }

        Logic logic = App.getLogic();
        if (logic != null) {
            // Tags
            for (int i = 0; i < elementCount; i++) {
                final LinkedHashMap<String, String> tags = currentTags.get(i);
                if (!originalTags.get(i).equals(tags)) {
                    try {
                        logic.setTags(getActivity(), types[i], osmIds[i], tags);
                    } catch (OsmIllegalOperationException e) {
                        Snack.barError(getActivity(), e.getMessage());
                    }
                }
            }

            if (elementCount == 1) {
                // Relation members
                if (Relation.NAME.equals(types[0])) {
                    List<RelationMemberDescription> currentMembers = relationMembersFragment.getMembersList();
                    if (!sameMembers(currentMembers, originalMembers)) {
                        Log.d(DEBUG_TAG, "updateAndFinish setting members");
                        logic.updateRelation(getActivity(), osmIds[0], currentMembers);
                        Relation updatedRelation = (Relation) App.getDelegator().getOsmElement(Relation.NAME, osmIds[0]);
                        if (logic.isSelected(updatedRelation)) { // This might be unnecessary
                            logic.removeSelectedRelation(updatedRelation);
                            logic.setSelectedRelation(updatedRelation);
                        }
                    }
                }
                // Parent relations
                MultiHashMap<Long, RelationMemberPosition> currentParents = relationMembershipFragment.getParentRelationMap();
                if (!((originalParents == null && currentParents.isEmpty()) || currentParents.equals(originalParents))) {
                    Log.d(DEBUG_TAG, "updateAndFinish setting parents");
                    logic.updateParentRelations(getActivity(), types[0], osmIds[0], currentParents);
                }
            }
        } else {
            Log.e(DEBUG_TAG, "updateAndFinish logic is null");
        }

        // call through to activity that we are done
        controlListener.finished(this);
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
        final int size = tags1.size();
        if (size != tags2.size()) { /// serious error
            return false;
        }
        for (int i = 0; i < size; i++) {
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
     * Check if we have unsaved changes
     * 
     * @return true if there are changes
     */
    public boolean hasChanges() {
        // Update any non-synced changes
        if (tagFormFragment != null) {
            tagFormFragment.updateEditorFromText();
        }
        List<LinkedHashMap<String, String>> currentTags = getUpdatedTags();
        MultiHashMap<Long, RelationMemberPosition> currentParents = null;
        List<RelationMemberDescription> currentMembers = null;
        if (relationMembershipFragment != null) {
            currentParents = relationMembershipFragment.getParentRelationMap();
        }
        if (relationMembersFragment != null) {
            currentMembers = new ArrayList<>();
            if (Relation.NAME.equals(types[0])) {
                currentMembers = relationMembersFragment.getMembersList();
            }
        }
        return (!same(currentTags, originalTags) // tags different
                // parents changed
                || ((currentParents != null && !currentParents.equals(originalParents)) && !(originalParents == null && currentParents.isEmpty()))
                // members changed
                || (currentMembers != null && !sameMembers(currentMembers, originalMembers)));
    }

    /**
     * Save the state of this activity instance for future restoration.
     * 
     * @param outState The object to receive the saved state.
     */
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Log.d(DEBUG_TAG, "bundle size 1 : " + Util.getBundleSize(outState));
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "bundle size 2 : " + Util.getBundleSize(outState));
        outState.putInt(CURRENTITEM, mViewPager.getCurrentItem());
        outState.putBoolean(PANELAYOUT, usePaneLayout);
        outState.putSerializable(TAGEDIT_DATA, loadData);
        App.getMruTags().save(getContext());
    }

    /** When the Activity is interrupted, save MRUs and address cache */
    @Override
    public void onPause() {
        Preset[] currentPresets = App.getCurrentPresets(getContext());
        for (Preset p : currentPresets) {
            if (p != null) {
                p.saveMRU();
            }
        }
        Address.saveLastAddresses(getContext());
        super.onPause();
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
        onPresetSelected(item, false, false);
    }

    @Override
    public void onPresetSelected(PresetItem item, boolean applyOptional, boolean isAlternative) {
        if (item != null && tagEditorFragment != null) {
            tagEditorFragment.applyPreset(item, applyOptional, isAlternative, true);
            if (mViewPager.getCurrentItem() != tagEditorFragmentPosition) {
                if (tagFormFragment != null) {
                    tagFormFragment.update();
                    mViewPager.setCurrentItem(tagFormFragmentPosition);
                } else {
                    mViewPager.setCurrentItem(tagEditorFragmentPosition);
                }
            }
            // utility presets need to be explicitly added, while this duplicates adding item in other cases
            // it has the nice side effect of moving item to the top
            tagEditorFragment.addToMru(presets, item);
            updateRecentPresets();
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
        enablePresets(true);
    }

    /**
     * Disallow presets to be applied
     */
    public void disablePresets() {
        enablePresets(false);
    }

    /**
     * Allow presets to be applied or not
     * 
     * @param enable if true presets can be applied
     */
    private void enablePresets(boolean enable) {
        RecentPresetsFragment recentPresetsFragment = getRecentPresetsFragment();
        if (recentPresetsFragment != null) {
            if (enable) {
                recentPresetsFragment.enable();
            } else {
                recentPresetsFragment.disable();
            }
        } else {
            Log.e(DEBUG_TAG, "RecentPresetsFragment not found");
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
    public List<LinkedHashMap<String, String>> getUpdatedTags() {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getUpdatedTags();
        } else {
            Log.e(DEBUG_TAG, "getUpdatedTags tagEditorFragment is null");
            return new ArrayList<>();
        }
    }

    @Override
    public LinkedHashMap<String, String> getKeyValueMapSingle(boolean allowBlanks) {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getKeyValueMapSingle(allowBlanks);
        } else {
            Log.e(DEBUG_TAG, "getUpdatedTags tagEditorFragment is null");
            return new LinkedHashMap<>();
        }
    }

    @Override
    public PresetItem getBestPreset() {
        if (tagEditorFragment != null) {
            PresetItem best = tagEditorFragment.getBestPreset();
            if (usePaneLayout) {
                // FIXME it isn't clear where the best place to add/update the display is
                de.blau.android.propertyeditor.Util.addAlternativePresetItemsFragment(getChildFragmentManager(), R.id.pane_alternative_layout, best);
            }
            return best;
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
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, PresetItem> getAllPresets() {
        if (tagEditorFragment != null) {
            return tagEditorFragment.getAllPresets();
        } else {
            Log.e(DEBUG_TAG, "getAllPresets tagEditorFragment is null");
            return new HashMap<>();
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
            streetNameAutocompleteAdapter = new StreetPlaceNamesAdapter(getContext(), R.layout.autocomplete_row, App.getDelegator(), types[0], osmIds[0],
                    values, false); // FIXME
        }
        return streetNameAutocompleteAdapter;
    }

    @Override
    public ArrayAdapter<ValueWithCount> getPlaceNameAdapter(List<String> values) {
        if (placeNameAutocompleteAdapter == null) {
            placeNameAutocompleteAdapter = new StreetPlaceNamesAdapter(getContext(), R.layout.autocomplete_row, App.getDelegator(), types[0], osmIds[0], values,
                    true); // FIXME
        }
        return placeNameAutocompleteAdapter;
    }

    @Override
    public OsmElement getElement() {
        return element; // FIXME validate
    }

    /**
     * Return if we are using the pane/tablet layout
     * 
     * Only used in testing
     * 
     * @return true is in pane mode
     */
    boolean usingPaneLayout() {
        return usePaneLayout;
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
            networkStatus = new NetworkStatus(getContext());
        }
        return networkStatus.isConnected();
    }

    @Override
    public boolean isConnectedOrConnecting() {
        if (networkStatus == null) {
            networkStatus = new NetworkStatus(getContext());
        }
        return networkStatus.isConnectedOrConnecting();
    }

    @Override
    public List<String> getIsoCodes() {
        if (isoCodes == null) {
            try {
                GeoContext geoContext = App.getGeoContext(getContext());
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

    @Override
    public void displayOptional(@NonNull PresetItem presetItem, boolean optional) {
        if (tagFormFragment != null) {
            tagFormFragment.displayOptional(presetItem, optional);
        } else {
            Log.e(DEBUG_TAG, "tagFormFragment is null");
        }
    }
}