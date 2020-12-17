package de.blau.android.propertyeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.dialogs.TextLineDialog;
import de.blau.android.osm.Tags;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.PresetIconManager;
import de.blau.android.presets.PresetItemLink;
import de.blau.android.presets.PresetTextField;
import de.blau.android.propertyeditor.TagEditorFragment.TagEditRow;
import de.blau.android.util.FileUtil;
import de.blau.android.util.StringWithDescription;

public final class CustomPreset {

    protected static final String DEBUG_TAG = "CustomPreset";

    public static final String ICON = "custom-preset.png";

    /**
     * Private constructor
     */
    private CustomPreset() {
        // don't instantiate
    }

    /**
     * Create a preset from the selected rows
     * 
     * This will set the current value as the default value of the corresponding PresetField if the tag can be
     * associated with with a PresetItem, otherwise it will create a text field if the value is empty or a fixed field
     * if a value is present.
     * 
     * Values for keys with name-like semantics will be removed.
     * 
     * The PresetItems are stored in the auto-preset.
     * 
     * @param caller the calling TagEditorFragment
     * @param selected the selected rows
     */
    static void create(@NonNull TagEditorFragment caller, @NonNull List<TagEditRow> selected) {
        Context ctx = caller.getContext();
        final PresetItem bestPreset = caller.getBestPreset();
        TextLineDialog.get(ctx, R.string.create_preset_title, -1,
                caller.getString(R.string.create_preset_default_name, bestPreset != null ? bestPreset.getName() : ""), (input, check) -> {
                    Preset preset = Preset.dummyInstance();
                    try {
                        preset.setIconManager(new PresetIconManager(ctx,
                                FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET).getAbsolutePath(), null));
                    } catch (IOException e) {
                        Log.e(DEBUG_TAG, "Setting icon manager failed " + e.getMessage());
                    }
                    PresetGroup group = preset.getRootGroup();
                    Preset.PresetItem customItem = preset.new PresetItem(group, input.getText().toString(), ICON, null);
                    // add linked presets
                    if (bestPreset != null) {
                        List<PresetItemLink> linkedPresets = bestPreset.getLinkedPresetItems();
                        if (linkedPresets != null) {
                            customItem.addAllLinkedPresetItems(new LinkedList<>(bestPreset.getLinkedPresetItems()));
                        }
                    }
                    // add fields
                    for (TagEditRow row : selected) {
                        String key = row.getKey();
                        String value = row.getValue();
                        boolean notEmpty = value != null && !"".equals(value);
                        PresetItem item = caller.getPreset(key);
                        if (item == null) {
                            if (notEmpty) {
                                customItem.addField(new PresetFixedField(key, new StringWithDescription(value)));
                            } else {
                                customItem.addField(new PresetTextField(key));
                            }
                        } else {
                            PresetField field = item.getField(key).copy();
                            if (notEmpty && !isLikeAName(key)) {
                                field.setDefaultValue(value);
                            }
                            field.setOptional(false);
                            customItem.addField(field);
                        }
                    }
                    Preset[] configuredPresets = App.getCurrentPresets(ctx);
                    Preset autoPreset = configuredPresets[configuredPresets.length - 1];
                    if (autoPreset != null) {
                        PresetGroup autoGroup = autoPreset.getGroupByName(ctx.getString(R.string.preset_autopreset));
                        if (group != null) {
                            @SuppressWarnings("unused")
                            PresetItem copy = autoPreset.new PresetItem(autoGroup, customItem);
                        } else {
                            Log.e(DEBUG_TAG, "Couldn't find preset group");
                        }
                    } else {
                        Log.e(DEBUG_TAG, "Preset null");
                        return;
                    }
                    AutoPreset.save(autoPreset);
                    caller.deselectAllRows();
                    caller.presetSelectedListener.onPresetSelected(customItem);
                }).show();
    }

    /**
     * Check if a key in general can be assumed to have a different value for each occurrence
     * 
     * We also assume that key: are variants that follow the same rule
     * 
     * @param key the key to check
     * @return true if the key has name-like semantics
     */
    private static boolean isLikeAName(@NonNull String key) {
        List<String> nameLikeKeys = new ArrayList<>(Tags.I18N_NAME_KEYS);
        nameLikeKeys.add(Tags.KEY_ADDR_HOUSENUMBER);
        nameLikeKeys.add(Tags.KEY_ADDR_HOUSENAME);
        nameLikeKeys.add(Tags.KEY_ADDR_UNIT);
        nameLikeKeys.add(Tags.KEY_REF);
        for (String k : nameLikeKeys) {
            if (k.equals(key) || key.startsWith(k + ":")) {
                return true;
            }
        }
        return false;
    }

}
