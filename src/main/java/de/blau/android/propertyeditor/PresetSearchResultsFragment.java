package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.AutoPresetItem;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.ThemeUtils;

public class PresetSearchResultsFragment extends DialogFragment {

    private static final String SEARCH_RESULTS_KEY = "searchResults";

    private static final String DEBUG_TAG = PresetSearchResultsFragment.class.getSimpleName();

    private OnPresetSelectedListener mOnPresetSelectedListener;
    private PresetUpdate             mPresetUpdateListener;
    private OsmElement               element;
    private ArrayList<PresetElement> presets;
    private boolean                  enabled = true;

    /**
     */
    public static PresetSearchResultsFragment newInstance(ArrayList<PresetElement> searchResults) {
        PresetSearchResultsFragment f = new PresetSearchResultsFragment();

        Bundle args = new Bundle();
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

        presets = (ArrayList<PresetElement>) getArguments().getSerializable(SEARCH_RESULTS_KEY);
        /*
         * Saving this argument (done by the FragmentManager) will typically exceed the 1MB transaction size limit and
         * cause a android.os.TransactionTooLargeException Removing it doesn't seem to have direct negative
         * consequences, worst case the dialog would be recreated empty.
         */
        getArguments().remove(SEARCH_RESULTS_KEY);

        View v = getResultsView(presetsLayout, presets);

        if (v != null) {
            builder.setView(v);
        }
        return builder.create();
    }

    /**
     * Create the View holding the search results
     * 
     * @param presetLayout our Layout
     * @param presets a List of PresetElements to display
     * @return the View or null
     */
    @Nullable
    private View getResultsView(@NonNull final LinearLayout presetLayout, @Nullable final List<PresetElement> presets) {
        View v = null;
        if (presets != null && !presets.isEmpty()) {

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

            Preset[] currentPresets = App.getCurrentPresets(getActivity());

            if (!(currentPresets != null && currentPresets.length > 0)) {
                return null;
            }

            PresetGroup results = new Preset().new PresetGroup(null, "search results", null);
            for (PresetElement p : presets) {
                if (p != null) {
                    results.addElement(p);
                }
            }
            v = results.getGroupView(getActivity(), presetClickHandler, null, null);

            int padding = ThemeUtils.getDimensionFromAttribute(getActivity(), R.attr.dialogPreferredPadding);
            v.setPadding(padding - Preset.SPACING, Preset.SPACING, padding - Preset.SPACING, padding);
        } else {
            Log.d(DEBUG_TAG, "getResultsView problem");
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
