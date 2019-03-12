package de.blau.android.propertyeditor;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.exception.UiStateException;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetClickHandler;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.Util;

public class RecentPresetsFragment extends BaseFragment {

    private static final String ELEMENT_NAME_KEY = "elementType";
    private static final String ELEMENT_ID_KEY   = "elementId";

    private static final String DEBUG_TAG = RecentPresetsFragment.class.getSimpleName();

    private OnPresetSelectedListener mListener;
    private OsmElement               element;
    private Preset[]                 presets;
    private boolean                  enabled = true;

    /**
     * Create a new RecentPresetsFragement instance
     * 
     * @param elementId the current OsmElement id
     * @param elementName the name of the OsmElement (Node, Way, Relation)
     * @return a RecentPresetsFragement instance
     */
    @NonNull
    public static RecentPresetsFragment newInstance(long elementId, @NonNull String elementName) {
        RecentPresetsFragment f = new RecentPresetsFragment();

        Bundle args = new Bundle();
        args.putLong(ELEMENT_ID_KEY, elementId);
        args.putString(ELEMENT_NAME_KEY, elementName);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        try {
            mListener = (OnPresetSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout presetsLayout = (LinearLayout) inflater.inflate(R.layout.recentpresets_view, null);

        long elementId = getArguments().getLong(ELEMENT_ID_KEY);
        String elementName = getArguments().getString(ELEMENT_NAME_KEY);

        element = App.getDelegator().getOsmElement(elementName, elementId);

        presets = App.getCurrentPresets(getActivity());

        View v = getRecentPresetsView(presetsLayout, element, presets);
        if (v != null) {
            presetsLayout.addView(v);
            presetsLayout.setVisibility(View.VISIBLE);
        }
        return presetsLayout;
    }

    /**
     * Create the MRU preset View
     * 
     * @param presetLayout the Layout to use
     * @param element the current OsmElement
     * @param presets the current active Presets
     * @return a View
     */
    @Nullable
    private View getRecentPresetsView(@NonNull final LinearLayout presetLayout, @Nullable final OsmElement element, @Nullable final Preset[] presets) {
        View v = null;
        if (presets != null && presets.length >= 1 && element != null) {
            // check if any of the presets has a MRU
            boolean mruFound = false;
            for (Preset p : presets) {
                if (p != null) {
                    if (p.hasMRU()) {
                        mruFound = true;
                        break;
                    }
                }
            }
            if (mruFound) {
                final ElementType filterType = element.getType();
                final PresetClickHandler presetClickHandler = new PresetClickHandler() {
                    @Override
                    public void onItemClick(PresetItem item) {
                        if (!enabled) {
                            return;
                        }
                        Log.d(DEBUG_TAG, "normal click");
                        mListener.onPresetSelected(item);
                        recreateRecentPresetView(presetLayout);
                    }

                    @Override
                    public boolean onItemLongClick(PresetItem item) {
                        if (!enabled) {
                            return true;
                        }
                        Log.d(DEBUG_TAG, "long click");
                        removePresetFromMRU(presetLayout, item);
                        return true;
                    }

                    @Override
                    public void onGroupClick(PresetGroup group) {
                        // should not have groups
                    }

                    @Override
                    public boolean onGroupLongClick(PresetGroup group) {
                        return false;
                    }
                };
                // all MRUs get added to this view
                v = Preset.getRecentPresetView(getActivity(), presets, presetClickHandler, filterType);

                v.setId(R.id.recentPresets);
            } else {
                Log.d(DEBUG_TAG, "getRecentPresetsView no MRU found!");
            }
        } else {
            Log.d(DEBUG_TAG, "getRecentPresetsView problem with presets or element " + element);
        }
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
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
     * Removes a preset from the MRU
     * 
     * @param presetLayout the layout holding the MRU View
     * @param item the preset to apply
     */
    private void removePresetFromMRU(@NonNull LinearLayout presetLayout, @NonNull PresetItem item) {
        //
        Preset[] presets = App.getCurrentPresets(getActivity());
        if (presets != null) {
            for (Preset p : presets) {
                if (p.contains(item)) {
                    p.removeRecentlyUsed(item);
                    break;
                }
            }
        }
        recreateRecentPresetView(presetLayout);
    }

    /**
     * Recreate the MRU view
     */
    public void recreateRecentPresetView() {
        recreateRecentPresetView((LinearLayout) getOurView());
    }

    /**
     * Recreate the MRU view
     * 
     * @param presetLayout the Layout holding the preset Views
     */
    private void recreateRecentPresetView(@NonNull LinearLayout presetLayout) {
        Log.d(DEBUG_TAG, "recreateRecentPresetView");
        presetLayout.removeAllViews();
        View v = getRecentPresetsView(presetLayout, element, presets);
        if (v != null) {
            presetLayout.addView(v);
            presetLayout.setVisibility(View.VISIBLE);
        } else {

        }
        presetLayout.invalidate();
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the row container view
     */
    @NonNull
    private View getOurView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.recentpresets_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.recentpresets_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.recentpresets_layout");
                    throw new UiStateException("didn't find R.id.recentpresets_layoutt");
                } else {
                    Log.d(DEBUG_TAG, "Found R.id.recentpresets_layout");
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
     * Enable selection of presets
     */
    void enable() {
        enabled = true;
    }

    /**
     * Diable selection of presets
     */
    void disable() {
        enabled = false;
    }
}
