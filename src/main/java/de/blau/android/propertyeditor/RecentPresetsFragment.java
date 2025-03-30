package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.exception.UiStateException;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetClickHandler;
import de.blau.android.presets.PresetItem;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.Util;

public class RecentPresetsFragment extends BaseFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RecentPresetsFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = RecentPresetsFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String ELEMENT_NAME_KEY = "elementType";
    private static final String ELEMENT_ID_KEY   = "elementId";

    private OnPresetSelectedListener presetSelectedListener;
    private OsmElement               element;
    private Preset[]                 presets;
    private boolean                  enabled = true;
    private PropertyEditorListener   propertyEditorListener;

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
        Fragment parent = Util.getParentFragmentWithInterface(this, PropertyEditorListener.class, OnPresetSelectedListener.class);
        presetSelectedListener = (OnPresetSelectedListener) parent;
        propertyEditorListener = (PropertyEditorListener) parent;
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
        if (presets == null || presets.length == 0 || element == null) {
            Log.d(DEBUG_TAG, "getRecentPresetsView problem with presets or element " + element);
            return null;
        }
        // check if any of the presets has a MRU
        boolean mruFound = false;
        for (Preset p : presets) {
            if (p != null && p.hasMRU()) {
                mruFound = true;
                break;
            }
        }
        if (!mruFound) {
            Log.d(DEBUG_TAG, "getRecentPresetsView no MRU found!");
            return null;
        }
        final ElementType filterType = element.getType();
        final PresetClickHandler presetClickHandler = new PresetClickHandler() {
            @Override
            public void onItemClick(View view, PresetItem item) {
                Log.d(DEBUG_TAG, "normal click");
                if (enabled) {
                    presetSelectedListener.onPresetSelected(item);
                    final Preferences preferences = App.getPreferences(getContext());
                    presetSelectedListener.onPresetSelected(item, preferences.applyWithOptionalTags(getContext(), item), false,
                            preferences.applyWithLastValues(getContext(), item) ? Prefill.FORCE_LAST : Prefill.PRESET);
                    recreateRecentPresetView(presetLayout);
                }
            }

            @Override
            public boolean onItemLongClick(View view, PresetItem item) {
                Log.d(DEBUG_TAG, "long click");
                if (enabled) {
                    showPopupMenu(presetLayout, view, item);
                }
                return true;
            }

        };
        // all MRUs get added to this view
        View v = Preset.getRecentPresetView(getActivity(), presets, presetClickHandler, filterType, propertyEditorListener.getIsoCodes());
        v.setId(R.id.recentPresets);
        return v;
    }

    /**
     * Show a small popup menu for setting some preferences for specific presets
     * 
     * @param presetLayout the layout holding the preset buttons
     * @param view the button that was clicked on
     * @param item the associated PresetItem
     */
    private void showPopupMenu(@NonNull final LinearLayout presetLayout, @NonNull View view, @NonNull PresetItem item) {
        final PopupMenu popup = new PopupMenu(getActivity(), view);
        final Menu menu = popup.getMenu();
        final Preferences prefs = App.getPreferences(getContext());
        menu.add(R.string.apply_with_last_values).setCheckable(true).setChecked(prefs.applyWithLastValues(getContext(), item))
                .setOnMenuItemClickListener((MenuItem menuItem) -> {
                    prefs.setApplyWithLastValues(getContext(), item, !menuItem.isChecked());
                    return true;
                });
        menu.add(R.string.apply_with_optional_tags).setCheckable(true).setChecked(prefs.applyWithOptionalTags(getContext(), item))
                .setOnMenuItemClickListener((MenuItem menuItem) -> {
                    prefs.setApplyWithOptionalTags(getContext(), item, !menuItem.isChecked());
                    return true;
                });
        menu.add(R.string.remove).setOnMenuItemClickListener(unused -> {
            removePresetFromMRU(presetLayout, item);
            return true;
        });
        popup.show();
    }

    /**
     * Removes a preset from the MRU
     * 
     * @param presetLayout the layout holding the MRU View
     * @param item the preset to apply
     */
    private void removePresetFromMRU(@NonNull LinearLayout presetLayout, @NonNull PresetItem item) {
        for (Preset p : propertyEditorListener.getPresets()) {
            if (p != null && p.contains(item)) {
                p.removeRecentlyUsed(item);
                break;
            }
        }
        recreateRecentPresetView(presetLayout);
    }

    /**
     * Recreate the MRU view
     */
    public void recreateRecentPresetView() {
        View view = getOurView();
        if (view != null) {
            recreateRecentPresetView((LinearLayout) view);
        }
    }

    /**
     * Recreate the MRU view
     * 
     * @param presetLayout the Layout holding the preset Views
     */
    private void recreateRecentPresetView(@NonNull LinearLayout presetLayout) {
        Log.d(DEBUG_TAG, "recreateRecentPresetView");
        presets = App.getCurrentPresets(getActivity());
        presetLayout.removeAllViews();
        View v = getRecentPresetsView(presetLayout, element, presets);
        if (v != null) {
            presetLayout.addView(v);
            presetLayout.setVisibility(View.VISIBLE);
        }
        presetLayout.invalidate();
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the row container view
     */
    @Nullable
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
        }
        return null;
    }

    /**
     * Enable selection of presets
     */
    void enable() {
        enabled = true;
    }

    /**
     * Disable selection of presets
     */
    void disable() {
        enabled = false;
    }
}
