package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.ActionMenuView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.BuildConfig;
import de.blau.android.Feedback;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.contract.Flavors;
import de.blau.android.contract.Github;
import de.blau.android.exception.UiStateException;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

public class PresetFragment extends BaseFragment implements PresetUpdate, PresetClickHandler {

    static final int MAX_SEARCHRESULTS = 10;

    private static final String ALTERNATE_ROOT_PATHS = "alternateRootPaths";

    private static final String PANE_MODE = "paneMode";

    private static final String ELEMENT_NAME_KEY  = "elementName";
    private static final String ELEMENT_ID_KEY    = "elementId";
    private static final String SEARCH_STRING_KEY = "searchString";

    private static final String FRAGMENT_PRESET_SEARCH_RESULTS_TAG = "fragment_preset_search_results";

    private static final String DEBUG_TAG = PresetFragment.class.getSimpleName();

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
         */
        void onPresetSelected(@NonNull PresetItem item, boolean applyOptional);
    }

    private OnPresetSelectedListener mListener;

    private PropertyEditorListener propertyEditorListener;

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
    public static PresetFragment newInstance(long elementId, @NonNull String elementName, @Nullable ArrayList<PresetElementPath> alternateRootPath,
            boolean paneMode) {
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
        try {
            mListener = (OnPresetSelectedListener) context;
            propertyEditorListener = (PropertyEditorListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener");
        }
    }

    Runnable displaySearchResults = new Runnable() {

        @Override
        public void run() {
            getAndShowSearchResults(presetSearch);
        }

    };

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
        List<PresetElementPath> alternateRootPaths = (ArrayList<PresetElementPath>) getArguments().getSerializable(ALTERNATE_ROOT_PATHS);

        LinearLayout presetPaneLayout = (LinearLayout) inflater.inflate(R.layout.preset_pane, null);
        final LinearLayout presetLayout = (LinearLayout) presetPaneLayout.findViewById(R.id.preset_presets);
        if (presets == null || presets.length == 0 || presets[0] == null) {
            TextView warning = new TextView(getActivity());
            warning.setText(R.string.no_valid_preset);
            presetLayout.addView(warning);
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
                    getAndShowSearchResults(presetSearch); // this should recreate the search results
                }
            }

            presetSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    Log.d(DEBUG_TAG, "action id " + actionId + " event " + event);
                    if (actionId == EditorInfo.IME_ACTION_SEARCH
                            || (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        return getAndShowSearchResults(presetSearch);
                    }
                    return false;
                }
            });

            // https://stackoverflow.com/questions/13135447/setting-onclicklistner-for-the-drawable-right-of-an-edittext/26269435#26269435
            // for the following
            presetSearch.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int DRAWABLE_LEFT = 0;
                    // final int DRAWABLE_TOP = 1;
                    final int DRAWABLE_RIGHT = 2;
                    // final int DRAWABLE_BOTTOM = 3;

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        boolean rtlLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                                && presetSearch.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
                        Drawable icon = presetSearch.getCompoundDrawables()[rtlLayout ? DRAWABLE_LEFT : DRAWABLE_RIGHT];
                        if (icon != null) {
                            int[] outLocation = new int[2];
                            presetSearch.getLocationOnScreen(outLocation);
                            float rawX = event.getRawX();
                            int iconWidth = icon.getBounds().width();
                            if ((rtlLayout && rawX <= outLocation[0] + iconWidth) || (rawX >= outLocation[0] + presetSearch.getWidth() - iconWidth)) {
                                presetSearch.setText("");
                                final FragmentManager fm = getChildFragmentManager();
                                de.blau.android.propertyeditor.Util.removeChildFragment(fm, FRAGMENT_PRESET_SEARCH_RESULTS_TAG);
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });

            presetSearch.addTextChangedListener(new TextWatcher() {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // do nothing
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() >= 3) {
                        presetSearch.removeCallbacks(displaySearchResults);
                        presetSearch.postDelayed(displaySearchResults, 200);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // do nothing
                }

            });
        }

        return presetPaneLayout;
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
     * @param presetSearch the EditText used for input
     * @return false if we didn't search
     */
    private boolean getAndShowSearchResults(final View presetSearch) {
        Activity activity = getActivity();
        String term = presetSearch instanceof EditText ? ((EditText) presetSearch).getText().toString() : null;
        if (activity == null || term == null || "".equals(term.trim())) {
            return false;
        }
        final FragmentManager fm = getChildFragmentManager();

        AsyncTask<Void, Void, ArrayList<PresetElement>> list = new AsyncTask<Void, Void, ArrayList<PresetElement>>() {

            @Override
            protected ArrayList<PresetElement> doInBackground(Void... params) {
                return new ArrayList<>(SearchIndexUtils.searchInPresets(activity, term, type, 2, MAX_SEARCHRESULTS, propertyEditorListener.getIsoCodes()));
            }

            @Override
            protected void onPostExecute(ArrayList<PresetElement> result) {
                if (result.isEmpty()) {
                    Snack.toastTopInfo(getContext(), R.string.toast_nothing_found);
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
                        ft.commit();
                    } catch (IllegalStateException isex) {
                        Log.e(DEBUG_TAG, "show of seach results failed with ", isex);
                    }
                } else {
                    searchResultDialog.update(term, result);
                }
            }
        };
        list.execute();
        return true;
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
        rootPreset.addToRootGroup(App.getCurrentPresets(getContext()));
        LinearLayout presetLayout = (LinearLayout) getOurView().getParent();
        if (presetLayout != null) {
            presetLayout.removeAllViews();
            presetLayout.addView(getPresetView());
        }
    }

    /**
     * Get View for the current group
     * 
     * @return a View displaying the elements of the current group
     */
    private View getPresetView() {
        View view = currentGroup.getGroupView(getActivity(), this, type, null, propertyEditorListener.getCountryIsoCode());
        view.setId(R.id.preset_view);
        return view;
    }

    /**
     * If this is not the root group, back goes one group up, otherwise, the default is triggered (canceling the dialog)
     */
    // @Override
    // public boolean onKey(View v, int keyCode, KeyEvent event) {
    // if (keyCode == KeyEvent.KEYCODE_BACK) {
    // PresetGroup group = currentGroup.getParent();
    // if (group != null) {
    // currentGroup = group;
    // currentGroup.getGroupView(getActivity(), (ScrollView) view, this, element.getType());
    // view.invalidate();
    // return true;
    // }
    // }
    // return false;
    // }

    /**
     * Handle clicks on icons representing an item
     */
    @Override
    public void onItemClick(PresetItem item) {
        if (!enabled) {
            return;
        }
        mListener.onPresetSelected(item);
    }

    /**
     * Handle long clicks on icons representing an item
     */
    @Override
    public boolean onItemLongClick(PresetItem item) {
        if (!enabled) {
            return true;
        }
        mListener.onPresetSelected(item);
        return true;
    }

    /**
     * Handle clicks on icons representing a group (changing to that group)
     */
    @Override
    public void onGroupClick(PresetGroup group) {
        ScrollView scrollView = (ScrollView) getOurView();
        currentGroup = group;
        currentGroup.getGroupView(getActivity(), scrollView, this, type, null, propertyEditorListener.getCountryIsoCode());
        scrollView.invalidate();
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onGroupLongClick(PresetGroup group) {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        ActionMenuView menuView = (ActionMenuView) getView().findViewById(R.id.preset_menu);
        // the library providing the Feedback UI is not supported under SDK 15
        boolean enablePresetFeedback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.FLAVOR.equals(Flavors.CURRENT);
        if (paneMode) {
            getActivity().getMenuInflater().inflate(R.menu.preset_nav_menu, menuView.getMenu());
            androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener listener = new androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            };
            menuView.setOnMenuItemClickListener(listener);
            if (enablePresetFeedback) {
                // this adds the item as the last one
                menu.add(Menu.NONE, R.id.menu_preset_feedback, 20, R.string.menu_preset_feedback).setEnabled(propertyEditorListener.isConnected());
            }
        } else {
            inflater.inflate(R.menu.preset_menu, menu);
            menuView.setVisibility(View.GONE);
            if (enablePresetFeedback) {
                menu.findItem(R.id.menu_preset_feedback).setVisible(true).setEnabled(propertyEditorListener.isConnected());
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.preset_menu_top);
        if (item != null) {
            item.setEnabled(currentGroup != rootGroup);
        }
        item = menu.findItem(R.id.preset_menu_up);
        if (item != null) {
            item.setEnabled(currentGroup != rootGroup);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        ScrollView scrollView = (ScrollView) getOurView();
        switch (item.getItemId()) {
        case android.R.id.home:
            ((PropertyEditor) getActivity()).sendResultAndFinish();
            return true;
        case R.id.preset_menu_top:
            if (rootGroup != null) {
                currentGroup = rootGroup;
                currentGroup.getGroupView(getActivity(), scrollView, this, type, null, propertyEditorListener.getCountryIsoCode());
                scrollView.invalidate();
                return true;
            }
            return true;
        case R.id.preset_menu_up:
            if (currentGroup != null && currentGroup != rootGroup) {
                PresetGroup group = currentGroup.getParent();
                if (group != null) {
                    currentGroup = group;
                    currentGroup.getGroupView(getActivity(), scrollView, this, type, null, propertyEditorListener.getCountryIsoCode());
                    scrollView.invalidate();
                    return true;
                }
            }
            return true;
        case R.id.menu_preset_feedback:
            Feedback.start(getContext(), Github.PRESET_REPO_USER, Github.PRESET_REPO_NAME);
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
                    throw new UiStateException("didn't find VIEW_ID");
                } else {
                    Log.d(DEBUG_TAG, "Found VIEW_ID");
                }
                return v;
            }
        } else {
            // given that this is always fatal might as well throw the exception here
            Log.d(DEBUG_TAG, "got null view in getView");
            throw new UiStateException("got null view in getView");
        }
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
