package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.AutoPresetItem;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

public class PresetSearchResultsFragment extends DialogFragment {

    private static final String SEARCH_RESULTS_KEY = "searchResults";
    private static final String SEARCH_TERM_KEY    = "searchTerm";

    private static final String DEBUG_TAG = PresetSearchResultsFragment.class.getSimpleName();

    private OnPresetSelectedListener mOnPresetSelectedListener;
    private PresetUpdate             mPresetUpdateListener;
    private OsmElement               element;
    private List<PresetElement>      presets;
    private boolean                  enabled = true;
    private PropertyEditorListener   propertyEditorListener;

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
        // f.setShowsDialog(true);

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
    }

    @NonNull
    @SuppressWarnings("unchecked")
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.search_results_title);
        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        LinearLayout presetsLayout = (LinearLayout) inflater.inflate(R.layout.preset_search_results_view, null);

        String searchTerm = getArguments().getString(SEARCH_TERM_KEY);
        presets = (ArrayList<PresetElement>) getArguments().getSerializable(SEARCH_RESULTS_KEY);
        /*
         * Saving this argument (done by the FragmentManager) will typically exceed the 1MB transaction size limit and
         * cause a android.os.TransactionTooLargeException Removing it doesn't seem to have direct negative
         * consequences, worst case the dialog would be recreated empty.
         */
        getArguments().remove(SEARCH_RESULTS_KEY);

        final View container = getResultsView(presetsLayout, presets, true);

        if (container != null) {
            builder.setView(container);
        }
        AsyncTask<Void, Void, List<PresetElement>> query = new AsyncTask<Void, Void, List<PresetElement>>() {
            private AlertDialog      progress = null;
            private FragmentActivity activity = getActivity();

            @Override
            protected void onPreExecute() {
                progress = ProgressDialog.get(activity, Progress.PROGRESS_SEARCHING);
                progress.show();
            }

            @Override
            protected List<PresetElement> doInBackground(Void... params) {
                List<PresetElement> searchResults = new ArrayList<>();
                AutoPreset autoPreset = new AutoPreset(activity);
                Preset fromTaginfo = autoPreset.fromTaginfo(searchTerm.trim(), PresetFragment.MAX_SEARCHRESULTS - searchResults.size());
                List<PresetElement> elementsFromTaginfo = fromTaginfo.getRootGroup().getElements();

                for (PresetElement pe : elementsFromTaginfo) {
                    searchResults.add(pe);
                }
                if (searchResults.isEmpty()) {
                    return null;
                }
                if (!presets.isEmpty()) {
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
                if (result == null || result.isEmpty()) {
                    Snack.toastTopInfo(getContext(), R.string.toast_nothing_found);
                    return;
                }
                presets.addAll(result);
                int height = ((ViewGroup) container).getHeight();
                ((ViewGroup) container).setMinimumHeight(height);
                int width = ((ViewGroup) container).getWidth();
                ((ViewGroup) container).setMinimumWidth(width);
                ((ViewGroup) container).removeAllViews();
                ((ViewGroup) container).addView(getResultsView(presetsLayout, presets, false));
            }
        };

        builder.setNeutralButton(R.string.Done, null);
        builder.setPositiveButton(R.string.search_online, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                if (propertyEditorListener.isConnected() && container != null) {
                    positive.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            query.execute();
                            view.setEnabled(false);
                        }
                    });
                } else {
                    positive.setEnabled(false);
                }
            }
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
                Preset[] presets = App.getCurrentPresets(getContext());
                Preset preset = presets[presets.length - 1];
                if (preset != null) {
                    PresetGroup group = preset.getGroupByName(getContext().getString(R.string.preset_autopreset));
                    if (group != null) {
                        PresetItem copy = preset.new PresetItem(group, item);
                    } else {
                        Log.e(DEBUG_TAG, "Couldn't find preset group");
                    }
                } else {
                    Log.e(DEBUG_TAG, "Preset null");
                    return;
                }
                // fixme update preset fragment
                AutoPreset.save(preset);
                mPresetUpdateListener.update(null);
            }
            mOnPresetSelectedListener.onPresetSelected(item);
            dismiss();
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

    /**
     * Create the View holding the search results
     * 
     * @param presetLayout our Layout
     * @param presets a List of PresetElements to display
     * @param setPadding apply padding if true
     * @return the View or null
     */
    @Nullable
    private View getResultsView(@NonNull final LinearLayout presetLayout, @Nullable final List<PresetElement> presets, boolean setPadding) {
        View v = null;
        PresetGroup results = new Preset().new PresetGroup(null, "search results", null);
        if (presets != null) {
            for (PresetElement p : presets) {
                if (p != null) {
                    results.addElement(p);
                }
            }
        }
        v = results.getGroupView(getActivity(), presetClickHandler, null, null);

        if (setPadding) {
            int padding = ThemeUtils.getDimensionFromAttribute(getActivity(), R.attr.dialogPreferredPadding);
            v.setPadding(padding - Preset.SPACING, Preset.SPACING, padding - Preset.SPACING, padding);
        }
        return v;
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
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the View holding our content or null
     */
    @Nullable
    public View getOurView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.recentpresets_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.recentpresets_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.recentpresets_layoutt");
                } else {
                    Log.d(DEBUG_TAG, "Found R.id.recentpresets_layout");
                }
                return v;
            }
        } else {
            Log.d(DEBUG_TAG, "got null view in getView");
        }
        return null;
    }
}
