package de.blau.android.propertyeditor;

import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.Tip;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetClickHandler;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetItemLink;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.ImmersiveDialogFragment;

public class AlternativePresetItemsFragment extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = AlternativePresetItemsFragment.class.getSimpleName();

    private static final String ITEM_PATH_KEY = "itemPath";
    public static final String  TAG           = "alternative_preset_item_fragment";

    private OnPresetSelectedListener presetSelectedListener; // NOSONAR false positive
    private PropertyEditorListener   propertyEditorListener;

    /**
     * Create a new instance
     * 
     * @param presetElementPath path to the preset item
     * @return an AlternativePresetItemsFragment
     */
    @NonNull
    public static AlternativePresetItemsFragment newInstance(@NonNull PresetElementPath presetElementPath) {
        AlternativePresetItemsFragment f = new AlternativePresetItemsFragment();

        Bundle args = new Bundle();
        args.putSerializable(ITEM_PATH_KEY, presetElementPath);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        try {
            presetSelectedListener = (OnPresetSelectedListener) context;
            propertyEditorListener = (PropertyEditorListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return createView(inflater, savedInstanceState);
    }

    /**
     * Actually create the view
     * 
     * @param inflater layout inflater
     * @param savedInstanceState saved state or null
     * @return the View
     */
    @Nullable
    private LinearLayout createView(@NonNull LayoutInflater inflater, @Nullable Bundle savedInstanceState) {
        LinearLayout presetsLayout = (LinearLayout) inflater.inflate(R.layout.recentpresets_view, null);

        PresetElementPath presetElementPath = null;

        if (savedInstanceState != null) {
            presetElementPath = (PresetElementPath) savedInstanceState.getSerializable(ITEM_PATH_KEY);
        } else {
            presetElementPath = (PresetElementPath) getArguments().getSerializable(ITEM_PATH_KEY);
        }
        if (presetElementPath == null) {
            Log.e(DEBUG_TAG, "presetElementPath is null");
            return null;
        }
        PresetElement presetItem = Preset.getElementByPath(App.getCurrentRootPreset(getContext()).getRootGroup(), presetElementPath,
                propertyEditorListener.getCountryIsoCode(), false);
        if (!(presetItem instanceof PresetItem)) {
            Log.e(DEBUG_TAG, "no PresetItem found for " + presetElementPath);
            return null;
        }

        View v = getAlternativesView(presetsLayout, (PresetItem) presetItem);
        presetsLayout.addView(v);
        presetsLayout.setVisibility(View.VISIBLE);
        return presetsLayout;
    }

    /**
     * Create the preset View
     * 
     * @param presetLayout the Layout to use
     * @param presetItem the PresetItem
     * @return a View
     */
    @NonNull
    private View getAlternativesView(@NonNull final LinearLayout presetLayout, @NonNull PresetItem presetItem) {

        final PresetClickHandler presetClickHandler = (PresetItem item) -> {
            Log.d(DEBUG_TAG, "normal click");
            presetSelectedListener.onPresetSelected(item, false, true);
        };
        PresetGroup alternatives = Preset.dummyInstance().getRootGroup();
        List<PresetItemLink> links = presetItem.getAlternativePresetItems();
        if (links != null) {
            for (PresetItemLink link : presetItem.getAlternativePresetItems()) {
                for (Preset preset : App.getCurrentPresets(getContext())) {
                    if (preset != null) {
                        PresetItem item = preset.getItemByName(link.getPresetName(), propertyEditorListener.getCountryIsoCode());
                        if (item != null) {
                            alternatives.addElement(item, false);
                            break;
                        }
                    }
                }
            }
        }
        View v = alternatives.getGroupView(getContext(), presetClickHandler, null, null, null);
        v.setId(R.id.recentPresets);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Tip.showDialog(getActivity(), R.string.tip_alternative_tagging_key, R.string.tip_alternative_tagging);
    }
}
