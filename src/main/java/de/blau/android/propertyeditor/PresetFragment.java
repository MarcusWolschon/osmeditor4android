package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.ActionMenuView;
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
import de.blau.android.HelpViewer;
import de.blau.android.R;
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

public class PresetFragment extends BaseFragment implements PresetUpdate, PresetClickHandler {

    static final int MAX_SEARCHRESULTS = 10;

    private static final String ALTERNATE_ROOT_PATHS = "alternateRootPaths";

    private static final String PANE_MODE = "paneMode";

    private static final String ELEMENT = "element";

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

    private PresetGroup currentGroup;
    private PresetGroup rootGroup;

    private boolean enabled = true;

    private boolean paneMode = false;

    /**
     * Create a new PresetFragement instance
     * 
     * @param e the OsmElement this applies to
     * @param alternateRootPath an alternative location for the top of the preset tree
     * @param paneMode we are displayed in Pane mode
     * @return a new PResetFragment
     */
    public static PresetFragment newInstance(@NonNull OsmElement e, @Nullable ArrayList<PresetElementPath> alternateRootPath, boolean paneMode) {
        PresetFragment f = new PresetFragment();

        Bundle args = new Bundle();
        args.putSerializable(ELEMENT, e);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        OsmElement element = (OsmElement) getArguments().getSerializable(ELEMENT);
        type = element.getType();
        Preset[] presets = App.getCurrentPresets(getActivity());
        Log.d(DEBUG_TAG, "presets size " + presets.length);
        paneMode = getArguments().getBoolean(PANE_MODE);
        @SuppressWarnings("unchecked")
        List<PresetElementPath> alternateRootPaths = (ArrayList<PresetElementPath>) getArguments().getSerializable(ALTERNATE_ROOT_PATHS);

        LinearLayout presetPaneLayout = (LinearLayout) inflater.inflate(R.layout.preset_pane, null);
        LinearLayout presetLayout = (LinearLayout) presetPaneLayout.findViewById(R.id.preset_presets);
        if (presets == null || presets.length == 0 || presets[0] == null) {
            TextView warning = new TextView(getActivity());
            warning.setText(R.string.no_valid_preset);
            presetLayout.addView(warning);
            return presetPaneLayout;
        }

        if (alternateRootPaths != null && !alternateRootPaths.isEmpty()) {
            PresetElement alternativeRootElement = Preset.getElementByPath(presets[0].getRootGroup(), alternateRootPaths.get(0));
            if (alternativeRootElement instanceof PresetGroup) {
                rootGroup = (PresetGroup) alternativeRootElement;
            }
        }

        if (rootGroup == null) {
            Preset preset = new Preset(); // dummy preset to hold the elements of all
            rootGroup = preset.new PresetGroup(null, "", null);
            preset.setRootGroup(rootGroup);
            addToRootGroup(presets);
        }
        currentGroup = rootGroup;

        presetLayout.addView(getPresetView());

        final EditText presetSearch = (EditText) presetPaneLayout.findViewById(R.id.preset_search_edit);
        if (presetSearch != null) {
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
                    // final int DRAWABLE_LEFT = 0;
                    // final int DRAWABLE_TOP = 1;
                    final int DRAWABLE_RIGHT = 2;
                    // final int DRAWABLE_BOTTOM = 3;

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        Drawable icon = presetSearch.getCompoundDrawables()[DRAWABLE_RIGHT];
                        if (icon != null && event.getRawX() >= (presetSearch.getRight() - icon.getBounds().width())) { // FIXME
                            return getAndShowSearchResults(presetSearch);
                        }
                    }
                    return false;
                }
            });
        }

        return presetPaneLayout;
    }

    /**
     * Add the top level PresetElements to rootGroup
     * 
     * @param presets array of current presets
     */
    private void addToRootGroup(Preset[] presets) {
        if (presets.length > 0) {
            // a bit of a hack ... this adds the elements from other presets to the dummy root group
            List<PresetElement> rootElements = rootGroup.getElements();
            rootElements.clear();
            for (Preset p : presets) {
                if (p != null) {
                    for (PresetElement e : p.getRootGroup().getElements()) {
                        if (!rootElements.contains(e)) { // only do this if not already present
                            rootGroup.addElement(e, true);
                        }
                    }
                }
            }
        }
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
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(FRAGMENT_PRESET_SEARCH_RESULTS_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        AsyncTask<Void, Void, ArrayList<PresetElement>> list = new AsyncTask<Void, Void, ArrayList<PresetElement>>() {

            @Override
            protected ArrayList<PresetElement> doInBackground(Void... params) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        presetSearch.setEnabled(false);
                    }
                });
                try {
                    return new ArrayList<>(SearchIndexUtils.searchInPresets(activity, term, type, 2, MAX_SEARCHRESULTS));
                } finally {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            presetSearch.setEnabled(true);
                        }
                    });
                }
            }

            @Override
            protected void onPostExecute(ArrayList<PresetElement> result) {
                if (result.isEmpty()) {
                    Snack.barInfo(getActivity(), R.string.toast_nothing_found);
                    if (propertyEditorListener.isConnected()) { // if not online nothing we can do
                        return;
                    }
                }
                PresetSearchResultsFragment searchResultDialog = PresetSearchResultsFragment.newInstance(term, result);
                try {
                    searchResultDialog.show(fm, FRAGMENT_PRESET_SEARCH_RESULTS_TAG);
                } catch (IllegalStateException isex) {
                    Log.e(DEBUG_TAG, "show of seach results failed with ", isex);
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
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void update(ElementType type) {
        if (type != null) {
            this.type = type;
        }
        addToRootGroup(App.getCurrentPresets(getContext()));
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
        View view = currentGroup.getGroupView(getActivity(), this, type, null);
        view.setId(R.id.preset_view);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(DEBUG_TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(DEBUG_TAG, "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(DEBUG_TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(DEBUG_TAG, "onDestroy");
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
     * Handle clicks on icons representing an item (closing the dialog with the item as a result)
     */
    @Override
    public void onItemClick(PresetItem item) {
        if (!enabled) {
            return;
        }
        mListener.onPresetSelected(item);
        // dismiss();
    }

    /**
     * for now do the same
     */
    @Override
    public boolean onItemLongClick(PresetItem item) {
        if (!enabled) {
            return true;
        }
        mListener.onPresetSelected(item);
        // dismiss();
        return true;
    }

    /**
     * Handle clicks on icons representing a group (changing to that group)
     */
    @Override
    public void onGroupClick(PresetGroup group) {
        ScrollView scrollView = (ScrollView) getOurView();
        currentGroup = group;
        currentGroup.getGroupView(getActivity(), scrollView, this, type, null);
        scrollView.invalidate();
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.supportInvalidateOptionsMenu();
        }
    }

    @Override
    public boolean onGroupLongClick(PresetGroup group) {
        return false;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        // final MenuInflater inflater = getSupportMenuInflater();
        super.onCreateOptionsMenu(menu, inflater);
        ActionMenuView menuView = (ActionMenuView) getView().findViewById(R.id.preset_menu);
        if (paneMode) {
            getActivity().getMenuInflater().inflate(R.menu.preset_nav_menu, menuView.getMenu());
            android.support.v7.widget.ActionMenuView.OnMenuItemClickListener listener = new android.support.v7.widget.ActionMenuView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            };
            menuView.setOnMenuItemClickListener(listener);
        } else {
            inflater.inflate(R.menu.preset_menu, menu);
            menuView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // FIXME this currently causes problems likely in Pane mode
        // menu.findItem(R.id.preset_menu_top).setEnabled(currentGroup != rootGroup);
        // menu.findItem(R.id.preset_menu_up).setEnabled(currentGroup != rootGroup);
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
                currentGroup.getGroupView(getActivity(), scrollView, this, type, null);
                scrollView.invalidate();
                return true;
            }
            return true;
        case R.id.preset_menu_up:
            if (currentGroup != null && currentGroup != rootGroup) {
                PresetGroup group = currentGroup.getParent();
                if (group != null) {
                    currentGroup = group;
                    currentGroup.getGroupView(getActivity(), scrollView, this, type, null);
                    scrollView.invalidate();
                    return true;
                }
            }
            return true;
        case R.id.preset_menu_help:
            HelpViewer.start(getActivity(), R.string.help_presets);
            return true;
        }
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.supportInvalidateOptionsMenu();
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
