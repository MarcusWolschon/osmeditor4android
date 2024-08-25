package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.AutoPresetItem;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetClickHandler;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetSeparator;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.Screen;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class PresetSearchResultsFragment extends DialogFragment implements UpdatePresetSearchResult {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PresetSearchResultsFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = PresetSearchResultsFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String SEARCH_RESULTS_KEY = "searchResults";
    private static final String SEARCH_TERM_KEY    = "searchTerm";

    private OnPresetSelectedListener mOnPresetSelectedListener; // NOSONAR we want to fail in onAttach
    private PresetUpdate             mPresetUpdateListener;     // NOSONAR
    private List<PresetElement>      presets;
    private boolean                  enabled = true;
    private PropertyEditorListener   propertyEditorListener;
    private String                   searchTerm;
    private LinearLayout             presetsLayout;
    private AppCompatButton          searchOnline;

    /**
     * Get an new PresetSearchResultsFragment
     * 
     * @param searchTerm the original term we were searching for
     * @param searchResults a List of PresetEements to display
     * @return an instance of PresetSearchResultsFragment
     */
    public static <L extends List<PresetElement> & Serializable> PresetSearchResultsFragment newInstance(@NonNull String searchTerm, @NonNull L searchResults) {
        PresetSearchResultsFragment f = new PresetSearchResultsFragment();

        Bundle args = new Bundle();
        args.putString(SEARCH_TERM_KEY, searchTerm);
        args.putSerializable(SEARCH_RESULTS_KEY, searchResults);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        Fragment parent = Util.getParentFragmentWithInterface(this, PresetUpdate.class, PropertyEditorListener.class, OnPresetSelectedListener.class);
        mOnPresetSelectedListener = (OnPresetSelectedListener) parent;
        mPresetUpdateListener = (PresetUpdate) parent;
        propertyEditorListener = (PropertyEditorListener) parent;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        presetsLayout = (LinearLayout) inflater.inflate(R.layout.preset_search_results_view, null);

        searchTerm = getArguments().getString(SEARCH_TERM_KEY);
        presets = Util.getSerializeableArrayList(getArguments(), SEARCH_RESULTS_KEY, PresetElement.class);
        /*
         * Saving this argument (done by the FragmentManager) will typically exceed the 1MB transaction size limit and
         * cause a android.os.TransactionTooLargeException Removing it doesn't seem to have direct negative
         * consequences, worst case the view would be recreated empty.
         */
        getArguments().remove(SEARCH_RESULTS_KEY);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreateView");
        if (!getShowsDialog()) {
            searchOnline = (AppCompatButton) presetsLayout.findViewById(R.id.search_online);
            searchOnline.setEnabled(propertyEditorListener.isConnected());
            searchOnline.setOnClickListener(view -> {
                OnlineQuery query = new OnlineQuery(getActivity());
                query.execute();
                view.setEnabled(false);
            });
            return getResultsView(presetsLayout, presets, false);
        }
        return null;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreateDialog");
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.search_results_title);

        final View container = getResultsView(presetsLayout, presets, true);
        builder.setView(container);

        builder.setNeutralButton(R.string.Done, null);
        builder.setPositiveButton(R.string.search_online, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positive = ((AlertDialog) d).getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setEnabled(propertyEditorListener.isConnected() && container != null);
            positive.setOnClickListener(view -> {
                OnlineQuery query = new OnlineQuery(getActivity());
                query.execute();
                view.setEnabled(false);
            });
        });
        return dialog;
    }

    final PresetClickHandler presetClickHandler = (View view, PresetItem item) -> {
        if (!enabled) {
            return;
        }
        Log.d(DEBUG_TAG, "normal click");
        if (item instanceof AutoPresetItem && AutoPreset.addItemToAutoPreset(getContext(), item)) {
            mPresetUpdateListener.update(null);
        }
        mOnPresetSelectedListener.onPresetSelected(item);
        if (getShowsDialog()) {
            dismiss();
        }
    };

    class OnlineQuery extends ExecutorTask<Void, Void, List<PresetElement>> {
        private AlertDialog            progress = null;
        private final FragmentActivity activity;

        /**
         * Construct a new taginfo querier
         * 
         * @param activity the calling activity
         */
        OnlineQuery(@NonNull FragmentActivity activity) {
            super(App.getLogic().getExecutorService(), App.getLogic().getHandler());
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.get(activity, Progress.PROGRESS_SEARCHING);
            progress.show();
        }

        @Override
        protected List<PresetElement> doInBackground(Void param) {
            List<PresetElement> searchResults = new ArrayList<>();
            AutoPreset autoPreset = new AutoPreset(activity);
            Preset fromTaginfo = autoPreset.fromTaginfo(searchTerm.trim(), PresetFragment.MAX_SEARCHRESULTS);
            List<PresetElement> elementsFromTaginfo = fromTaginfo.getRootGroup().getElements();

            for (PresetElement pe : elementsFromTaginfo) {
                searchResults.add(pe);
            }
            if (!searchResults.isEmpty() && presets != null && !presets.isEmpty()) {
                searchResults.add(0, new PresetSeparator(fromTaginfo, fromTaginfo.getRootGroup()));
            }
            return searchResults;
        }

        @Override
        protected void onPostExecute(List<PresetElement> result) {
            try {
                progress.dismiss();
            } catch (Exception ex) {
                Log.e(DEBUG_TAG, "dismiss dialog failed with " + ex);
            }

            if (activity == null || !isAdded()) {
                Log.e(DEBUG_TAG, "onPostExecute missing Activity");
                return;
            }

            if (result.isEmpty()) {
                ScreenMessage.toastTopInfo(getContext(), R.string.toast_nothing_found);
                return;
            }
            if (presets != null) {
                presets.addAll(result);
            } else {
                presets = result;
            }
            getResultsView(presetsLayout, presets, getShowsDialog());
        }
    }

    /**
     * Create the View holding the search results
     * 
     * @param layout our Layout
     * @param displayPresets a List of PresetElements to display
     * @param setPadding apply padding if true
     * @return the View or null
     */
    @NonNull
    private View getResultsView(@NonNull final LinearLayout layout, @Nullable final List<PresetElement> displayPresets, boolean setPadding) {
        View v = null;
        PresetGroup results = new PresetGroup(Preset.dummyInstance(), null, "search results", null);
        results.setItemSort(false);
        if (displayPresets != null) {
            for (PresetElement p : displayPresets) {
                if (p != null) {
                    results.addElement(p, false);
                }
            }
        }
        v = results.getGroupView(getActivity(), presetClickHandler, null, null, null);

        if (setPadding) {
            int padding = ThemeUtils.getDimensionFromAttribute(getActivity(), R.attr.dialogPreferredPadding);
            v.setPadding(padding - Preset.SPACING, Preset.SPACING, padding - Preset.SPACING, padding);
        }

        if (getShowsDialog()) {
            layout.removeAllViews();
            layout.addView(v);
        } else {
            if (layout.getChildCount() > 2) {
                layout.removeViewAt(0);
            }
            layout.addView(v, 0);
            v.setMinimumHeight(v.getHeight());
        }

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout((int) (Screen.getScreenSmallDimension(getActivity()) * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void update(String term, List<PresetElement> updatedPresets) {
        searchTerm = term;
        if (updatedPresets != null) {
            this.presets = updatedPresets;
            getResultsView(presetsLayout, updatedPresets, false);
        }
        if (searchOnline != null) {
            searchOnline.setEnabled(propertyEditorListener.isConnected());
        }
    }
}
