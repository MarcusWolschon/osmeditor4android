package de.blau.android.propertyeditor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.dialogs.TextLineDialog;
import de.blau.android.dialogs.TextLineDialog.TextLineInterface;
import de.blau.android.osm.Tags;
import de.blau.android.presets.AutoPreset;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetField;
import de.blau.android.presets.PresetFixedField;
import de.blau.android.presets.PresetIconManager;
import de.blau.android.presets.PresetTextField;
import de.blau.android.propertyeditor.TagEditorFragment.TagEditRow;
import de.blau.android.util.ClipboardUtils;
import de.blau.android.util.FileUtil;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class TagSelectedActionModeCallback extends SelectedRowsActionModeCallback {

    private static final String DEBUG_TAG = "TagSelected...";

    // pm: protected static final int MENU_ITEM_DELETE = 1;
    // pm: protected static final int MENU_ITEM_HELP = 15;
    private static final int MENU_ITEM_COPY          = 2;
    private static final int MENU_ITEM_CUT           = 3;
    private static final int MENU_ITEM_CREATE_PRESET = 19;

    public static final String CUSTOM_PRESET_ICON = "custom-preset.png";

    /**
     * Construct a new ActionModeCallback
     * 
     * @param caller the calling Fragment
     * @param rows the Layout holding the rows
     */
    public TagSelectedActionModeCallback(Fragment caller, LinearLayout rows) {
        super(caller, rows);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        mode.setTitle(R.string.tag_action_tag_title);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        super.onPrepareActionMode(mode, menu);
        Context context = caller.getActivity();
        menu.add(Menu.NONE, MENU_ITEM_COPY, Menu.NONE, R.string.menu_copy).setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_copy))
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_copy));
        menu.add(Menu.NONE, MENU_ITEM_CUT, Menu.NONE, R.string.menu_cut).setAlphabeticShortcut(Util.getShortCut(context, R.string.shortcut_cut))
                .setIcon(ThemeUtils.getResIdFromAttribute(context, R.attr.menu_cut));
        menu.add(Menu.NONE, MENU_ITEM_CREATE_PRESET, Menu.NONE, R.string.tag_menu_create_preset);
        return true;
    }

    /**
     * Copy tags to the internal and system clipboard
     * 
     * @param selectedRows List of selected rows
     * @param deleteEachRow if true the selected rows will be delted
     */
    private void copyTags(@NonNull List<TagEditRow> selectedRows, boolean deleteEachRow) {
        if (!selectedRows.isEmpty()) {
            TagEditorFragment fragment = (TagEditorFragment) caller;
            Map<String, String> copiedTags = new LinkedHashMap<>();
            for (TagEditRow row : selectedRows) {
                addKeyValue(copiedTags, row);
                if (deleteEachRow) {
                    row.delete();
                }
            }
            App.getTagClipboard(fragment.getActivity()).copy(copiedTags);
            ClipboardUtils.copyTags(fragment.getActivity(), copiedTags);
        }
    }

    /**
     * Build a map of the keys and values to add to the clipboards
     * 
     * @param tags Map containing the copied tags
     * @param row the current row
     */
    private void addKeyValue(Map<String, String> tags, final TagEditRow row) {
        String key = row.getKey().trim();
        String value = row.getValue().trim();
        boolean bothBlank = "".equals(key) && "".equals(value);
        boolean neitherBlank = !"".equals(key) && !"".equals(value);
        if (!bothBlank) {
            // both blank is never acceptable
            if (neitherBlank) {
                tags.put(key, value);
            }
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return performAction(item.getItemId());
    }

    /**
     * Perform whatever was selected on the menu
     * 
     * @param action the action id
     * @return true
     */
    private boolean performAction(int action) {

        final int size = rows.getChildCount();
        List<TagEditRow> selected = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            View view = rows.getChildAt(i);
            TagEditRow row = (TagEditRow) view;
            if (row.isSelected()) {
                selected.add(row);
            }
        }
        switch (action) {
        case MENU_ITEM_DELETE:
            if (!selected.isEmpty()) {
                for (TagEditRow r : selected) {
                    r.delete();
                }
                ((TagEditorFragment) caller).updateAutocompletePresetItem(null);
            }
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_COPY:
            copyTags(selected, false);
            ((TagEditorFragment) caller).updateAutocompletePresetItem(null);
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_CUT:
            copyTags(selected, true);
            if (currentAction != null) {
                currentAction.finish();
            }
            break;
        case MENU_ITEM_CREATE_PRESET:
            createPreset(selected);
            break;
        case MENU_ITEM_SELECT_ALL:
            ((PropertyRows) caller).selectAllRows();
            return true;
        case MENU_ITEM_DESELECT_ALL:
            ((PropertyRows) caller).deselectAllRows();
            return true;
        case MENU_ITEM_HELP:
            HelpViewer.start(caller.getActivity(), R.string.help_propertyeditor);
            return true;
        default:
            return false;
        }
        return true;
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
     * @param selected the selected rows
     */
    private void createPreset(@NonNull List<TagEditRow> selected) {
        Context ctx = caller.getContext();
        final PresetItem bestPreset = ((TagEditorFragment) caller).getBestPreset();
        TextLineDialog.get(ctx, R.string.create_preset_title, -1,
                caller.getString(R.string.create_preset_default_name, bestPreset != null ? bestPreset.getName() : ""), new TextLineInterface() {

                    @Override
                    public void processLine(EditText input) {
                        Preset preset = Preset.dummyInstance();
                        try {
                            preset.setIconManager(new PresetIconManager(ctx,
                                    FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_AUTOPRESET).getAbsolutePath(), null));
                        } catch (IOException e) {
                            Log.e(DEBUG_TAG, "Setting icon manager failed " + e.getMessage());
                        }
                        PresetGroup group = preset.getRootGroup();
                        Preset.PresetItem customItem = preset.new PresetItem(group, input.getText().toString(), CUSTOM_PRESET_ICON, null);
                        // add linked presets
                        customItem.addAllLinkedPresetNames(new LinkedList<>(bestPreset.getLinkedPresetNames()));
                        // add fields
                        for (TagEditRow row : selected) {
                            String key = row.getKey();
                            String value = row.getValue();
                            boolean notEmpty = value != null && !"".equals(value);
                            PresetItem item = ((TagEditorFragment) caller).getPreset(key);
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
                        ((PropertyRows) caller).deselectAllRows();
                        ((TagEditorFragment) caller).presetSelectedListener.onPresetSelected(customItem);
                    }
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
    boolean isLikeAName(@NonNull String key) {
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
