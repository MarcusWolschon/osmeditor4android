package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
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
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.AutoPresetItem;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetClickHandler;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.Screen;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

public class PresetSearchResultsFragment extends DialogFragment implements UpdatePresetSearchResult {

    private static final String SEARCH_RESULTS_KEY = "searchResults";
    private static final String SEARCH_TERM_KEY    = "searchTerm";

    private static final String DEBUG_TAG = PresetSearchResultsFragment.class.getSimpleName();

    private OnPresetSelectedListener mOnPresetSelectedListener;
    private PresetUpdate             mPresetUpdateListener;
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
    public static PresetSearchResultsFragment newInstance(@NonNull String searchTerm, @NonNull ArrayList<PresetElement> searchResults) {
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
        try {
            mOnPresetSelectedListener = (OnPresetSelectedListener) context;
            mPresetUpdateListener = (PresetUpdate) context;
            propertyEditorListener = (PropertyEditorListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener and PresetUpdate");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        presetsLayout = (LinearLayout) inflater.inflate(R.layout.preset_search_results_view, null);

        searchTerm = getArguments().getString(SEARCH_TERM_KEY);
        presets = (ArrayList<PresetElement>) getArguments().getSerializable(SEARCH_RESULTS_KEY);
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

    final PresetClickHandler presetClickHandler = new PresetClickHandler() {
        @Override
        public void onItemClick(PresetItem item) {
            if (!enabled) {
                return;
            }
            Log.d(DEBUG_TAG, "normal click");
            if (item instanceof AutoPresetItem) {
                Preset[] configuredPresets = App.getCurrentPresets(getContext());
                int autopresetPosition = configuredPresets.length - 1;
                Preset preset = configuredPresets[autopresetPosition];
                if (preset == null) {
                    // may happen during testing
                    AdvancedPrefDatabase.createEmptyAutoPreset(getContext(), configuredPresets, autopresetPosition);
                    preset = configuredPresets[autopresetPosition];
                    if (preset == null) {
                        Log.e(DEBUG_TAG, "Couldn't create auto preset");
                        return;
                    }
                }
                PresetGroup group = preset.getGroupByName(getContext().getString(R.string.preset_autopreset));
                if (group != null) {
                    item = preset.new PresetItem(group, item);
                } else {
                    Log.e(DEBUG_TAG, "Couldn't find preset group");
                }
                AutoPreset.save(preset);
                mPresetUpdateListener.update(null);
            }
            mOnPresetSelectedListener.onPresetSelected(item);
            if (getShowsDialog()) {
                dismiss();
            }
        }

        @Override
        public void onGroupClick(PresetGroup group) {
            // should not have groups
        }

        @Override
        public boolean onItemLongClick(PresetItem item) {
            return false;
        }

        @Override
        public boolean onGroupLongClick(PresetGroup group) {
            return false;
        }
    };

    class OnlineQuery extends AsyncTask<Void, Void, List<PresetElement>> {
        private AlertDialog            progress = null;
        private final FragmentActivity activity;

        /**
         * Construct a new taginfo querier
         * 
         * @param activity the calling activity
         */
        OnlineQuery(@NonNull FragmentActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.get(activity, Progress.PROGRESS_SEARCHING);
            progress.show();
        }

        @Override
        protected List<PresetElement> doInBackground(Void... params) {
            List<PresetElement> searchResults = new ArrayList<>();
            AutoPreset autoPreset = new AutoPreset(activity);
            Preset fromTaginfo = autoPreset.fromTaginfo(searchTerm.trim(), PresetFragment.MAX_SEARCHRESULTS);
            List<PresetElement> elementsFromTaginfo = fromTaginfo.getRootGroup().getElements();

            for (PresetElement pe : elementsFromTaginfo) {
                searchResults.add(pe);
            }
            if (searchResults.isEmpty()) {
                return null;
            }
            if (presets != null && !presets.isEmpty()) {
                searchResults.add(0, fromTaginfo.new PresetSeparator(fromTaginfo.getRootGroup()));
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

            if (result == null || result.isEmpty()) {
                Snack.toastTopInfo(getContext(), R.string.toast_nothing_found);
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
        PresetGroup results = Preset.dummyInstance().new PresetGroup(null, "search results", null);
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
            dialog.getWindow().setLayout((int) (Screen.getScreenSmallDimemsion(getActivity()) * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
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
