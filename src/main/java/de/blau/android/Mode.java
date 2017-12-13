package de.blau.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.filter.CorrectFilter;
import de.blau.android.filter.Filter;
import de.blau.android.filter.IndoorFilter;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;

/**
 * Enums for modes.
 */
public enum Mode {

    /**
     * edit geometries in "easyedit" mode
     */
    MODE_EASYEDIT(R.string.mode_easy, "EASY", true, true, true, true, null, R.drawable.unlocked_white, new FilterModeConfig()),
    /**
     * tag edit only mode
     */
    MODE_TAG_EDIT(R.string.mode_tag_only, "TAG", true, true, false, true, null, R.drawable.unlocked_tag_white, new FilterModeConfig()),
    /**
     * Background alignment mode
     */
    MODE_ALIGN_BACKGROUND(R.string.mode_easy, "EASY", false, false, false, false, MODE_EASYEDIT, R.drawable.unlocked_white, new ModeConfig() {
        @Override
        public void setup(Main main, Logic logic) {
            if (main.getBackgroundAlignmentActionModeCallback() == null) {
                Log.d("Logic", "weird state of edit mode, resetting");
                logic.setMode(main, Mode.MODE_EASYEDIT);
            }
        }

        @Override
        public void teardown(Main main, Logic logic) {
        }

        @Override
        public HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
            return null;
        }

        @Override
        public ArrayList<PresetElementPath> getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
            return null;
        }
    }),
    /**
     * Indoor mode
     */
    MODE_INDOOR(R.string.mode_indoor, "INDOOR", true, true, true, false, null, R.drawable.unlocked_indoor_white, new ModeConfig() {

        @Override
        public void setup(final Main main, final Logic logic) {
            Filter.Update updater = new Filter.Update() {
                @Override
                public void execute() {
                    logic.invalidateMap();
                    main.scheduleAutoLock();
                }
            };
            Filter filter = logic.getFilter();
            if (filter != null) {
                if (!(filter instanceof IndoorFilter)) {
                    filter.saveState();
                    filter.hideControls();
                    filter.removeControls();
                    IndoorFilter indoor = new IndoorFilter();
                    indoor.saveFilter(filter);
                    logic.setFilter(indoor);
                    indoor.addControls(main.getMapLayout(), updater);
                }
            } else { // no filter yet
                logic.setFilter(new IndoorFilter());
                logic.getFilter().addControls(main.getMapLayout(), updater);
            }
            logic.getFilter().showControls();
            logic.deselectAll();

        }

        @Override
        public void teardown(final Main main, final Logic logic) {
            Filter.Update updater = new Filter.Update() {
                @Override
                public void execute() {
                    logic.invalidateMap();
                    main.scheduleAutoLock();
                }
            };

            // indoor mode is a special case of a filter
            // needs to be removed here and previous filter, if any, restored
            Filter filter = logic.getFilter();
            if (filter != null) {
                if (filter instanceof IndoorFilter) {
                    filter.saveState();
                    filter.hideControls();
                    filter.removeControls();
                    filter = filter.getSavedFilter();
                    logic.setFilter(filter);
                    if (filter != null) {
                        filter.addControls(main.getMapLayout(), updater);
                        filter.showControls();
                    }
                }
            }
        }

        @Override
        public HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
            HashMap<String, String> result = new HashMap<>();
            // we only want to apply a level tag automatically to newly created objects if they don't already have the
            // tag and not when the filter is inverted
            Filter filter = logic.getFilter();
            if (filter != null && filter instanceof IndoorFilter && !((IndoorFilter) filter).isInverted() && e.getState() == OsmElement.STATE_CREATED
                    && !e.hasTagKey(Tags.KEY_LEVEL)) {
                result.put(Tags.KEY_LEVEL, Integer.toString(((IndoorFilter) filter).getLevel()));
            }
            return result;
        }

        @Override
        public ArrayList<PresetElementPath> getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
            return null;
        }
    }),

    MODE_CORRECT(R.string.mode_correct, "CORRECT", true, true, true, false, null, R.drawable.unlocked_correct_white, new ModeConfig() {

        @Override
        public void setup(final Main main, final Logic logic) {
            Filter.Update updater = new Filter.Update() {
                @Override
                public void execute() {
                    logic.invalidateMap();
                    main.scheduleAutoLock();
                }
            };
            Filter filter = logic.getFilter();
            if (filter != null) {
                if (!(filter instanceof CorrectFilter)) {
                    filter.saveState();
                    filter.hideControls();
                    filter.removeControls();
                    CorrectFilter complete = new CorrectFilter();
                    complete.saveFilter(filter);
                    logic.setFilter(complete);
                    complete.addControls(main.getMapLayout(), updater);
                }
            } else { // no filter yet
                logic.setFilter(new CorrectFilter());
                logic.getFilter().addControls(main.getMapLayout(), updater);
            }
            logic.getFilter().showControls();
            logic.deselectAll();
        }

        @Override
        public void teardown(final Main main, final Logic logic) {
            Filter.Update updater = new Filter.Update() {
                @Override
                public void execute() {
                    logic.invalidateMap();
                    main.scheduleAutoLock();
                }
            };

            // indoor mode is a special case of a filter
            // needs to be removed here and previous filter, if any, restored
            Filter filter = logic.getFilter();
            if (filter != null) {
                if (filter instanceof CorrectFilter) {
                    filter.saveState();
                    filter.hideControls();
                    filter.removeControls();
                    filter = filter.getSavedFilter();
                    logic.setFilter(filter);
                    if (filter != null) {
                        filter.addControls(main.getMapLayout(), updater);
                        filter.showControls();
                    }
                }
            }
        }

        @Override
        public ArrayList<PresetElementPath> getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
            ArrayList<PresetElementPath> result = new ArrayList<>();
            Preset[] presets = App.getCurrentPresets(ctx);
            if (presets.length > 0 && presets[0] != null) {
                PresetItem pi = Preset.findBestMatch(presets, e.getTags());
                if (pi != null) { // there naturally may not be a preset
                    result.add(pi.getPath(presets[0].getRootGroup()));
                }
            }
            return result;
        }

        @Override
        public HashMap<String, String> getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
            return null;
        }
    });

    final private int        nameResId;
    final private String     tag;
    final private boolean    selectable;
    final private boolean    editable;
    final private boolean    geomEditable;
    final private boolean    supportFilters;
    final private Mode       subModeOf;
    private boolean          enabled        = true;
    private int              iconResourceId = -1;
    final private ModeConfig config;

    Mode(int nameResId, String tag, boolean selectable, boolean editable, boolean geomEditable, boolean supportsFilters, Mode subModeOf, int iconResourceId,
            ModeConfig config) {
        /**
         * string resource id for the name
         */
        this.nameResId = nameResId;
        /**
         * Unique tag for this mode
         */
        this.tag = tag;
        /**
         * Elements are selectable
         */
        this.selectable = selectable;
        /**
         * Elements are editable
         */
        this.editable = editable;
        /**
         * Geometry can be edited
         */
        this.geomEditable = geomEditable;
        /**
         * Doesn't have filters of its own
         */
        this.supportFilters = supportsFilters;
        /**
         * Variant of another mode
         */
        this.subModeOf = subModeOf;
        /**
         * Lock button icon
         */
        this.iconResourceId = iconResourceId;
        /**
         * Methods for configuring the mode
         */
        this.config = config;
    }

    String getName(Context ctx) {
        return ctx.getString(nameResId);
    }

    boolean elementsSelectable() {
        return selectable;
    }

    boolean elementsEditable() {
        return editable;
    }

    boolean elementsGeomEditiable() {
        return geomEditable;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    boolean isEnabled() {
        return enabled;
    }

    boolean supportsFilters() {
        return supportFilters;
    }

    int iconResourceId() {
        return iconResourceId;
    }

    Mode isSubModeOf() {
        return subModeOf;
    }

    String tag() {
        return tag;
    }

    void setup(Main main, Logic logic) {
        if (config != null) {
            config.setup(main, logic);
        }
    }

    void teardown(Main main, Logic logic) {
        if (config != null) {
            config.teardown(main, logic);
        }
    }

    /**
     * Return the Mode for a given tag
     * 
     * @param tag the tag we are looking for
     * @return the corresponding Mode
     */
    static Mode modeForTag(String tag) {
        for (Mode mode : Mode.values()) {
            if (mode.tag().equals(tag)) {
                return mode;
            }
        }
        return null; // can't happen
    }

    /**
     * Get any special tags for this mode, not very elegant
     * 
     * @param logic the current Logic instance
     * @param e the selected element
     * @return map containing the additional tags or null
     */
    @Nullable
    public HashMap<String, String> getExtraTags(Logic logic, OsmElement e) {
        if (config != null) {
            return config.getExtraTags(logic, e);
        }
        return null;
    }

    @Nullable
    public ArrayList<PresetElementPath> getPresetItems(Context ctx, OsmElement e) {
        if (config != null) {
            return config.getPresetItems(ctx, e);
        }
        return null;
    }
}
