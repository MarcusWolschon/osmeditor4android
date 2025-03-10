package de.blau.android;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.easyedit.SimpleActionModeCallback.SimpleAction;
import de.blau.android.filter.CorrectFilter;
import de.blau.android.filter.Filter;
import de.blau.android.filter.IndoorFilter;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetItem;

/**
 * Enums for modes.
 */
public enum Mode {

    /**
     * Edit geometries in "easyedit" mode
     */
    MODE_EASYEDIT(R.string.mode_easy, "EASY", true, true, true, true, null, R.drawable.unlocked_white, new FilterModeConfig()),
    /**
     * Tag edit only mode
     */
    MODE_TAG_EDIT(R.string.mode_tag_only, "TAG", true, true, false, true, null, R.drawable.unlocked_tag_white, new FilterModeConfig()),
    /**
     * Address adding mode
     */
    MODE_ADDRESS(R.string.mode_address, "ADDRESS", true, true, true, true, null, R.drawable.unlocked_address_white, new FilterModeConfig() {

        @Override
        public Set<SimpleAction> enabledSimpleActions() {
            return Collections.unmodifiableSet(EnumSet.of(SimpleAction.ADDRESS_NODE, SimpleAction.INTERPOLATION_WAY, SimpleAction.NOTE));
        }
    }),
    /**
     * Background alignment mode
     */
    MODE_ALIGN_BACKGROUND(R.string.mode_easy, "EASY", false, false, false, false, MODE_EASYEDIT, R.drawable.unlocked_white, new ModeConfig() {

        @Override
        public void setup(Main main, Logic logic) {
            if (main.getImageryAlignmentActionModeCallback() == null) {
                Log.d("Logic", "weird state of edit mode, resetting");
                logic.setMode(main, Mode.MODE_EASYEDIT);
            }
        }

        @Override
        public void teardown(Main main, Logic logic) {
            // empty
        }

        @SuppressWarnings("unchecked")
        @Override
        public <M extends Map<String, String> & Serializable> M getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
            return (M) new HashMap<String, String>();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <L extends List<PresetElementPath> & Serializable> L getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
            return (L) new ArrayList<PresetElementPath>();
        }
    }),
    /**
     * Indoor mode
     */
    MODE_INDOOR(R.string.mode_indoor, "INDOOR", true, true, true, false, null, R.drawable.unlocked_indoor_white, new ModeConfig() {

        @Override
        public void setup(final Main main, final Logic logic) {
            StandardUpdater updater = new StandardUpdater(logic, main);
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
            StandardUpdater updater = new StandardUpdater(logic, main);
            // indoor mode is a special case of a filter
            // needs to be removed here and previous filter, if any, restored
            Filter filter = logic.getFilter();
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

        @Override
        public <M extends Map<String, String> & Serializable> M getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
            @SuppressWarnings("unchecked")
            M result = (M) new HashMap<String, String>();
            // we only want to apply a level tag automatically to newly created objects if they don't already have the
            // tag and not when the filter is inverted
            Filter filter = logic.getFilter();
            if (filter instanceof IndoorFilter && !((IndoorFilter) filter).isInverted() && e.getState() == OsmElement.STATE_CREATED
                    && !e.hasTagKey(Tags.KEY_LEVEL)) {
                result.put(Tags.KEY_LEVEL, Integer.toString(((IndoorFilter) filter).getLevel()));
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <L extends List<PresetElementPath> & Serializable> L getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
            return (L) new ArrayList<PresetElementPath>();
        }
    }), MODE_CORRECT(R.string.mode_correct, "CORRECT", true, true, true, false, null, R.drawable.unlocked_correct_white, new ModeConfig() {

        @Override
        public void setup(final Main main, final Logic logic) {
            StandardUpdater updater = new StandardUpdater(logic, main);
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
            // indoor mode is a special case of a filter
            // needs to be removed here and previous filter, if any, restored
            Filter filter = logic.getFilter();
            if (filter instanceof CorrectFilter) {
                filter.saveState();
                filter.hideControls();
                filter.removeControls();
                filter = filter.getSavedFilter();
                logic.setFilter(filter);
                if (filter != null) {
                    filter.addControls(main.getMapLayout(), () -> {
                        logic.invalidateMap();
                        main.scheduleAutoLock();
                    });
                    filter.showControls();
                }
            }
        }

        @Override
        public <L extends List<PresetElementPath> & Serializable> L getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
            @SuppressWarnings("unchecked")
            L result = (L) new ArrayList<PresetElementPath>();
            Preset[] presets = App.getCurrentPresets(ctx);
            if (presets.length > 0 && presets[0] != null) {
                PresetItem pi = Preset.findBestMatch(presets, e.getTags(), null, null);
                if (pi != null) { // there naturally may not be a preset
                    result.add(pi.getPath(presets[0].getRootGroup()));
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <M extends Map<String, String> & Serializable> M getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
            return (M) new HashMap<String, String>();
        }
    }), MODE_VOICE(R.string.mode_voice, "VOICE", true, true, true, true, null, R.drawable.unlocked_voice_white, new FilterModeConfig() {

        @Override
        public Set<SimpleAction> enabledSimpleActions() {
            return Collections.unmodifiableSet(EnumSet.of(SimpleAction.VOICE_NODE, SimpleAction.VOICE_NOTE));
        }
    });

    private final int        nameResId;
    private final String     tag;
    private final boolean    selectable;
    private final boolean    editable;
    private final boolean    geomEditable;
    private final boolean    supportFilters;
    private final Mode       subModeOf;
    private boolean          enabled        = true;
    private int              iconResourceId = -1;
    private final ModeConfig config;

    /**
     * Construct a new Mode
     * 
     * @param nameResId resource id for the name
     * @param tag unique string id
     * @param selectable OsmElements can be selected
     * @param editable OsmElements can be edited
     * @param geomEditable the geometry of OsmElements can be changed
     * @param supportsFilters filters can be set in this mode (aka does not use filters itself)
     * @param subModeOf set if this is a child Mode
     * @param iconResourceId resource id for an icon
     * @param config setup and teardown configuration for the Mode
     */
    Mode(int nameResId, @NonNull String tag, boolean selectable, boolean editable, boolean geomEditable, boolean supportsFilters, @Nullable Mode subModeOf,
            int iconResourceId, @Nullable ModeConfig config) {
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

    /**
     * Get the name of this mode
     * 
     * @param ctx an Android Context
     * @return the name as a String
     */
    @Nullable
    String getName(@NonNull Context ctx) {
        return ctx.getString(nameResId);
    }

    /**
     * Check if elements are selectable in this mode
     * 
     * @return true if elements are selectable
     */
    public boolean elementsSelectable() {
        return selectable;
    }

    /**
     * Check if elements are editable in this mode
     * 
     * @return true if elements are editable
     */
    boolean elementsEditable() {
        return editable;
    }

    /**
     * Check if elements geometry is editable in this mode
     * 
     * @return true if elements geometry is editable
     */
    public boolean elementsGeomEditable() {
        return geomEditable;
    }

    /**
     * Set the enabled status of this mode
     * 
     * @param enabled if true the mode will be enabled
     */
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if this mode is enabled
     * 
     * @return true if enabled
     */
    boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if this mode supports filters
     * 
     * @return true if this mode supports filters
     */
    boolean supportsFilters() {
        return supportFilters;
    }

    /**
     * Get the icon resources id for this mode
     * 
     * @return the icon resources id for this mode
     */
    int iconResourceId() {
        return iconResourceId;
    }

    /**
     * Get the parent Mode
     * 
     * @return the parent Mode or null if none
     */
    @Nullable
    Mode isSubModeOf() {
        return subModeOf;
    }

    /**
     * Get the unique tag for this mode
     * 
     * @return the tag
     */
    @NonNull
    String tag() {
        return tag;
    }

    /**
     * Setup this mode
     * 
     * @param main the current Main instance
     * @param logic the current Logic instance
     */
    void setup(@NonNull Main main, @NonNull Logic logic) {
        if (config != null) {
            config.setup(main, logic);
        }
    }

    /**
     * Teardown this mode
     * 
     * @param main the current Main instance
     * @param logic the current Logic instance
     */
    void teardown(@NonNull Main main, @NonNull Logic logic) {
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
    @SuppressWarnings("unchecked")
    @NonNull
    public <M extends Map<String, String> & Serializable> M getExtraTags(@NonNull Logic logic, @NonNull OsmElement e) {
        if (config != null) {
            return config.getExtraTags(logic, e);
        }
        return (M) new HashMap<String, String>();
    }

    /**
     * Get any PresetITems that should automatically be applied to the OsmElement
     * 
     * @param ctx an Android Context
     * @param e the OsmElement
     * @return a List of PresetElementPath or null if none available
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <L extends List<PresetElementPath> & Serializable> L getPresetItems(@NonNull Context ctx, @NonNull OsmElement e) {
        if (config != null) {
            return config.getPresetItems(ctx, e);
        }
        return (L) new ArrayList<PresetElementPath>();
    }

    /**
     * Get the currently enabled SimpleActions
     * 
     * @return a Set of SimpleAction
     */
    @NonNull
    public Set<SimpleAction> enabledSimpleActions() {
        return config.enabledSimpleActions();
    }
}