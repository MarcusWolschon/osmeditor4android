package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import de.blau.android.App;
import de.blau.android.BuildConfig;
import de.blau.android.Feedback;
import de.blau.android.HelpViewer;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.contract.Flavors;
import de.blau.android.contract.Github;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetClickHandler;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.OnTextChangedWatcher;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.Sound;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class PresetFragment extends BaseFragment implements PresetUpdate, PresetClickHandler {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PresetFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = PresetFragment.class.getSimpleName().substring(0, TAG_LEN);

    static final int         MAX_SEARCHRESULTS      = 10;
    private static final int MAX_DISTANCE           = 2;
    private static final int MIN_SEARCH_TERM_LENGTH = 3;

    private static final String ALTERNATE_ROOT_PATHS = "alternateRootPaths";

    private static final String PANE_MODE = "paneMode";

    private static final String ELEMENT_NAME_KEY  = "elementName";
    private static final String ELEMENT_ID_KEY    = "elementId";
    private static final String SEARCH_STRING_KEY = "searchString";

    private static final String FRAGMENT_PRESET_SEARCH_RESULTS_TAG = "fragment_preset_search_results";

    public interface OnPresetSelectedListener {
        /**
         * Call back when a PresetItem is selected
         * 
         * @param item the PresetItem
         */
        void onPresetSelected(@NonNull PresetItem item);

        /**
         * Call back when a PresetItem is selected
         * 
         * @param item the PresetItem
         * @param applyOptional if true apply optional fields
         * @param isAlternative if true if the item is an alternative to the existing tagging
         * @param prefill how to prefill empty values
         */
        void onPresetSelected(@NonNull PresetItem item, boolean applyOptional, boolean isAlternative, @NonNull Prefill prefill);
    }

    private OnPresetSelectedListener mListener;
    private PropertyEditorListener   propertyEditorListener;
    private EditorUpdate             editorUpdate;

    /** The type of OSM element to which the preset will be applied (used for filtering) */
    private ElementType type;

    private Preset      rootPreset;
    private PresetGroup currentGroup;
    private PresetGroup rootGroup;

    private EditText presetSearch = null;

    private boolean enabled = true;

    private boolean paneMode = false;

    /**
     * Create a new PresetFragement instance
     * 
     * @param elementId the current OsmElement id
     * @param elementName the name of the OsmElement (Node, Way, Relation)
     * @param alternateRootPath an alternative location for the top of the preset tree
     * @param paneMode we are displayed in Pane mode
     * @return a new PResetFragment
     */
    public static <L extends List<PresetElementPath> & Serializable> PresetFragment newInstance(long elementId, @NonNull String elementName,
            @Nullable L alternateRootPath, boolean paneMode) {
        PresetFragment f = new PresetFragment();

        Bundle args = new Bundle();
        args.putLong(ELEMENT_ID_KEY, elementId);
        args.putString(ELEMENT_NAME_KEY, elementName);
        args.putBoolean(PANE_MODE, paneMode);
        args.putSerializable(ALTERNATE_ROOT_PATHS, alternateRootPath);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        Fragment parent = getParentFragment();
        Util.implementsInterface(parent, EditorUpdate.class, PropertyEditorListener.class, OnPresetSelectedListener.class);
        mListener = (OnPresetSelectedListener) parent;
        propertyEditorListener = (PropertyEditorListener) parent;
        editorUpdate = (EditorUpdate) parent;
    }

    private final class SearchResultsDisplay implements Runnable {

        private ExecutorTask<Void, Void, ArrayList<PresetElement>> searchTask;

        @Override
        public void run() {
            searchTask = executeSearchTask(presetSearch);
            if (searchTask != null) {
                searchTask.execute();
            }
        }

        /**
         * Cancel execution of the current task
         */
        public void cancel() {
            if (searchTask != null) {
                searchTask.cancel();
            }
        }
    }

    private SearchResultsDisplay displaySearchResults = new SearchResultsDisplay();

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        long elementId = getArguments().getLong(ELEMENT_ID_KEY);
        String elementName = getArguments().getString(ELEMENT_NAME_KEY);

        OsmElement element = App.getDelegator().getOsmElement(elementName, elementId);
        if (element != null) {
            type = element.getType();
        } else {
            Log.e(DEBUG_TAG, elementName + " " + elementId + " vanished");
        }
        Preset[] presets = App.getCurrentPresets(getActivity());
        Log.d(DEBUG_TAG, "presets size " + presets.length);
        paneMode = getArguments().getBoolean(PANE_MODE);
        @SuppressWarnings("unchecked")
        List<PresetElementPath> alternateRootPaths = Util.getSerializeableArrayList(getArguments(), ALTERNATE_ROOT_PATHS, PresetElementPath.class);

        LinearLayout presetPaneLayout = (LinearLayout) inflater.inflate(R.layout.preset_pane, null);
        final LinearLayout presetLayout = (LinearLayout) presetPaneLayout.findViewById(R.id.preset_presets);
        if (!hasValidPreset(presets)) {
            TextView warning = new TextView(getActivity());
            warning.setText(R.string.no_valid_preset);
            presetLayout.addView(warning);
            ScreenMessage.toastTopError(getContext(), R.string.no_valid_preset);
            return presetPaneLayout;
        }

        rootPreset = App.getCurrentRootPreset(getActivity());
        if (alternateRootPaths != null && !alternateRootPaths.isEmpty()) {
            PresetElement alternativeRootElement = Preset.getElementByPath(rootPreset.getRootGroup(), alternateRootPaths.get(0));
            if (alternativeRootElement instanceof PresetGroup) {
                rootGroup = (PresetGroup) alternativeRootElement;
            }
        }

        if (rootGroup == null) {
            rootGroup = rootPreset.getRootGroup();
        }
        currentGroup = rootGroup;

        presetLayout.addView(getPresetView());

        presetSearch = (EditText) presetPaneLayout.findViewById(R.id.preset_search_edit);
        if (presetSearch != null) {
            if (savedInstanceState != null) { // restore search string
                String searchString = savedInstanceState.getString(SEARCH_STRING_KEY);
                if (searchString != null) {
                    presetSearch.setText(searchString);
                    executeSearchTask(presetSearch);
                }
            }

            presetSearch.setOnEditorActionListener((v, actionId, event) -> {
                Log.d(DEBUG_TAG, "action id " + actionId + " event " + event);
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    executeSearchTask(presetSearch);
                    return true;
                }
                return false;
            });

            // https://stackoverflow.com/questions/13135447/setting-onclicklistner-for-the-drawable-right-of-an-edittext/26269435#26269435
            // for the following
            presetSearch.setOnTouchListener((v, event) -> {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_RIGHT = 2;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    boolean rtlLayout = presetSearch.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
                    Drawable icon = presetSearch.getCompoundDrawables()[rtlLayout ? DRAWABLE_LEFT : DRAWABLE_RIGHT];
                    if (icon != null) {
                        int[] outLocation = new int[2];
                        presetSearch.getLocationOnScreen(outLocation);
                        float rawX = event.getRawX();
                        int iconWidth = icon.getBounds().width();
                        if ((rtlLayout && rawX <= outLocation[0] + iconWidth) || (rawX >= outLocation[0] + presetSearch.getWidth() - iconWidth)) {
                            presetSearch.setText("");
                            de.blau.android.propertyeditor.Util.removeChildFragment(getChildFragmentManager(), FRAGMENT_PRESET_SEARCH_RESULTS_TAG);
                            return true;
                        }
                    }
                }
                return false;
            });

            presetSearch.addTextChangedListener((OnTextChangedWatcher) (CharSequence cs, int start, int count, int after) -> {
                displaySearchResults.cancel();
                presetSearch.removeCallbacks(displaySearchResults);
                if (cs.length() >= MIN_SEARCH_TERM_LENGTH) {
                    presetSearch.postDelayed(displaySearchResults, 200);
                } else {
                    de.blau.android.propertyeditor.Util.removeChildFragment(getChildFragmentManager(), FRAGMENT_PRESET_SEARCH_RESULTS_TAG);
                }
            });
        }

        return presetPaneLayout;
    }

    /**
     * Execute a preset search
     * 
     * @param editText the EditText holding the text to use for the search
     * @return a reference to the task
     */
    private ExecutorTask<Void, Void, ArrayList<PresetElement>> executeSearchTask(@NonNull final EditText editText) {
        ExecutorTask<Void, Void, ArrayList<PresetElement>> searchTask = getAndShowSearchResults(editText);
        if (searchTask != null) {
            searchTask.execute(); // this should recreate the search results
        }
        return searchTask;
    }

    /**
     * Check if we have at least one valid preset
     * 
     * @param presets the currently configured presets
     * @return true if we have at least one valid preset
     */
    private boolean hasValidPreset(@NonNull Preset[] presets) {
        if (presets.length == 0) {
            return false;
        }
        for (Preset p : presets) {
            if (p != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(DEBUG_TAG, "onStart");
        update(this.type); // preset configuration might have changed
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        if (presetSearch != null) {
            outState.putString(SEARCH_STRING_KEY, presetSearch.getText().toString());
        }
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    /**
     * Query the preset search index and display results in a dialog
     * 
     * @param editText the EditText used for input
     * @return an ExecutorTask or null
     */
    @Nullable
    private ExecutorTask<Void, Void, ArrayList<PresetElement>> getAndShowSearchResults(@Nullable final View editText) {
        Activity activity = getActivity();
        String term = editText instanceof EditText ? ((EditText) editText).getText().toString() : null;
        if (activity == null || term == null || "".equals(term.trim())) {
            return null;
        }
        final FragmentManager fm = getChildFragmentManager();
        Logic logic = App.getLogic();
        return new ExecutorTask<Void, Void, ArrayList<PresetElement>>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected ArrayList<PresetElement> doInBackground(Void param) {
                return new ArrayList<>(
                        SearchIndexUtils.searchInPresets(activity, term, type, MAX_DISTANCE, MAX_SEARCHRESULTS, propertyEditorListener.getIsoCodes()));
            }

            @Override
            protected void onPostExecute(ArrayList<PresetElement> result) {
                if (!isCancelled() && !fm.isDestroyed()) {
                    if (result.isEmpty()) {
                        ScreenMessage.toastTopInfo(getContext(), R.string.toast_nothing_found);
                        if (!propertyEditorListener.isConnected()) { // if not online nothing we can do
                            return;
                        }
                    }
                    PresetSearchResultsFragment searchResultDialog = (PresetSearchResultsFragment) fm.findFragmentByTag(FRAGMENT_PRESET_SEARCH_RESULTS_TAG);
                    if (searchResultDialog == null) {
                        searchResultDialog = PresetSearchResultsFragment.newInstance(term, result);
                        try {
                            Log.d(DEBUG_TAG, "Creating new result fragment");
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.add(R.id.preset_results, searchResultDialog, FRAGMENT_PRESET_SEARCH_RESULTS_TAG);
                            ft.commitNow();
                        } catch (IllegalStateException isex) {
                            Log.e(DEBUG_TAG, "show of seach results failed with ", isex);
                        }
                    } else {
                        searchResultDialog.update(term, result);
                    }
                }
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // calling setHasOptionsMenu here instead of in on Create supposedly
        // fixes issues with onCreateOptionsMenu being called
        // before the view is inflated
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void update(ElementType type) {
        if (type != null) {
            this.type = type;
        }
        View view = getOurView();
        if (view != null) { // all of this requires onCreateView to have run
            rootPreset.addToRootGroup(App.getCurrentPresets(getContext()));
            currentGroup = rootGroup;
            LinearLayout presetLayout = (LinearLayout) view.getParent();
            if (presetLayout != null) {
                presetLayout.removeAllViews();
                presetLayout.addView(getPresetView());
            }
        }
    }

    /**
     * Get View for the current group
     * 
     * @return a View displaying the elements of the current group
     */
    private View getPresetView() {
        View view = currentGroup.getGroupView(getActivity(), this, type, propertyEditorListener.getElement(), null, propertyEditorListener.getIsoCodes());
        view.setId(R.id.preset_view);
        return view;
    }

    /**
     * Handle clicks on icons representing an item
     */
    @Override
    public void onItemClick(View view, PresetItem item) {
        if (enabled) {
            mListener.onPresetSelected(item);
        }
    }

    /**
     * Handle long clicks on icons representing an item
     */
    @Override
    public boolean onItemLongClick(View view, PresetItem item) {
        if (enabled) {
            Preset preset = item.getPreset();
            Preset[] presets = propertyEditorListener.getPresets();
            if (preset.equals(presets[presets.length - 1])) {
                ThemeUtils.getAlertDialogBuilder(getContext()).setTitle(R.string.delete_custom_preset_title)
                        .setPositiveButton(R.string.Delete, (dialog, which) -> {
                            preset.deleteItem(item);
                            AutoPreset.save(getActivity(), preset);
                            editorUpdate.updatePresets();
                            propertyEditorListener.updateRecentPresets();
                            ScrollView scrollView = (ScrollView) getOurView();
                            if (scrollView != null) {
                                currentGroup.getGroupView(getActivity(), scrollView, this, type, propertyEditorListener.getElement(), null,
                                        propertyEditorListener.getIsoCodes());
                                scrollView.invalidate();
                            }
                        }).setNeutralButton(R.string.cancel, null).show();
            } else {
                Sound.beep();
            }
        }
        return true;
    }

    /**
     * Handle clicks on icons representing a group (changing to that group)
     */
    @Override
    public void onGroupClick(View view, PresetGroup group) {
        ScrollView scrollView = (ScrollView) getOurView();
        if (scrollView != null) {
            currentGroup = group;
            currentGroup.getGroupView(getActivity(), scrollView, this, type, propertyEditorListener.getElement(), null, propertyEditorListener.getIsoCodes());
            scrollView.invalidate();
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        ActionMenuView menuView = (ActionMenuView) getView().findViewById(R.id.preset_menu);
        // the library providing the Feedback UI is not supported under SDK 15
        boolean enablePresetFeedback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.FLAVOR.equals(Flavors.CURRENT);
        if (paneMode) {
            menuView.setVisibility(View.VISIBLE);
            getActivity().getMenuInflater().inflate(R.menu.preset_nav_menu, menuView.getMenu());
            menuView.setOnMenuItemClickListener(this::onOptionsItemSelected);
            if (enablePresetFeedback) {
                // this adds the item as the last one
                menu.add(Menu.NONE, R.id.menu_preset_feedback, 20, R.string.menu_preset_feedback).setEnabled(propertyEditorListener.isConnected());
            }
        } else {
            inflater.inflate(R.menu.preset_menu, menu);
            if (enablePresetFeedback) {
                menu.findItem(R.id.menu_preset_feedback).setVisible(true).setEnabled(propertyEditorListener.isConnected());
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean enableNavigation = getOurView() != null && currentGroup != rootGroup;
        MenuItem item = menu.findItem(R.id.preset_menu_top);
        if (item != null) {
            item.setEnabled(enableNavigation);
        }
        item = menu.findItem(R.id.preset_menu_up);
        if (item != null) {
            item.setEnabled(enableNavigation);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        ScrollView scrollView = (ScrollView) getOurView();
        switch (item.getItemId()) {
        case android.R.id.home:
            propertyEditorListener.updateAndFinish();
            return true;
        case R.id.preset_menu_top:
            if (rootGroup != null && scrollView != null) {
                currentGroup = rootGroup;
                currentGroup.getGroupView(getActivity(), scrollView, this, type, propertyEditorListener.getElement(), null,
                        propertyEditorListener.getIsoCodes());
                scrollView.invalidate();
                return true;
            }
            return true;
        case R.id.preset_menu_up:
            if (currentGroup != null && currentGroup != rootGroup) {
                PresetGroup group = currentGroup.getParent();
                if (group != null && scrollView != null) {
                    currentGroup = group;
                    currentGroup.getGroupView(getActivity(), scrollView, this, type, propertyEditorListener.getElement(), null,
                            propertyEditorListener.getIsoCodes());
                    scrollView.invalidate();
                    return true;
                }
            }
            return true;
        case R.id.menu_preset_feedback:
            Feedback.start(getContext(), Github.PRESET_REPO_USER, Github.PRESET_REPO_NAME, App.getLogic().getPrefs().useUrlForFeedback());
            return true;
        case R.id.preset_menu_help:
            HelpViewer.start(getActivity(), R.string.help_presets);
            return true;
        default:
            // ignore
        }
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
        return false;
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the view containing what we added
     */
    @Nullable
    private View getOurView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.preset_view) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.preset_view);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find VIEW_ID");
                } else {
                    Log.d(DEBUG_TAG, "Found VIEW_ID");
                }
                return v;
            }
        }
        return v;
    }

    /**
     * Enable preset selection
     */
    protected void enable() {
        enabled = true;
    }

    /**
     * Disable preset selection
     */
    void disable() {
        enabled = false;
    }
}
