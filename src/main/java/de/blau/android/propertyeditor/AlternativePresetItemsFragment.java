package de.blau.android.propertyeditor;

import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetClickHandler;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetItemLink;
import de.blau.android.propertyeditor.PresetFragment.OnPresetSelectedListener;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;

public class AlternativePresetItemsFragment extends ImmersiveDialogFragment {

    private static final String ITEM_PATH_KEY = "itemPath";

    private static final String DEBUG_TAG = AlternativePresetItemsFragment.class.getSimpleName();

    private static final String TAG = "alternative_preset_item_fragment";

    private OnPresetSelectedListener presetSelectedListener;

    private boolean enabled = true;

    /**
     * Show a dialog containing a view of alternative preset items
     * 
     * @param activity the calling Activity
     * @param presetElementPath path to the preset with alternatives
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull PresetElementPath presetElementPath) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            AlternativePresetItemsFragment photoViewerFragment = newInstance(presetElementPath);
            photoViewerFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new RecentPresetsFragement instance
     * 
     * @param elementId the current OsmElement id
     * @param elementName the name of the OsmElement (Node, Way, Relation)
     * @return a RecentPresetsFragement instance
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
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.dismiss, doNothingListener);
        builder.setView(createView(ThemeUtils.getLayoutInflater(getContext()), savedInstanceState));
        return builder.create();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(inflater, savedInstanceState);
        }
        return null;
    }

    /**
     * Actually create the view
     * 
     * @param inflater layout inflater
     * @param savedInstanceState saved state or null
     * @return
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

        PresetElement presetItem = Preset.getElementByPath(App.getCurrentRootPreset(getContext()).getRootGroup(), presetElementPath);
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
     * @param element the current OsmElement
     * @param presets the current active Presets
     * @return a View
     */
    @NonNull
    private View getAlternativesView(@NonNull final LinearLayout presetLayout, @NonNull PresetItem presetItem) {

        final PresetClickHandler presetClickHandler = new PresetClickHandler() {
            @Override
            public void onItemClick(PresetItem item) {
                if (!enabled) {
                    return;
                }
                Log.d(DEBUG_TAG, "normal click");
                presetSelectedListener.onPresetSelected(item);
                Dialog dialog = getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

            @Override
            public void onGroupClick(PresetGroup group) {
                // should not have groups
            }

            @Override
            public boolean onGroupLongClick(PresetGroup group) {
                return false;
            }

            @Override
            public boolean onItemLongClick(PresetItem item) {
                // not used
                return false;
            }
        };
        PresetGroup alternatives = Preset.dummyInstance().getRootGroup();
        List<PresetItemLink> links = presetItem.getAlternativePresetItems();
        if (links != null) {
            for (PresetItemLink link : presetItem.getAlternativePresetItems()) {
                for (Preset preset : App.getCurrentPresets(getContext())) {
                    if (preset != null) {
                        PresetItem item = preset.getItemByName(link.getPresetName());
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
