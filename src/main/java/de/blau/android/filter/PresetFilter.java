package de.blau.android.filter;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.util.Util;

/**
 * Filter plus UI for filtering on presets NOTE: the relevant ways should be processed before nodes
 * 
 * @author simon
 *
 */
public class PresetFilter extends Filter {

    /**
     * 
     */
    private static final long   serialVersionUID = 7L;
    private static final String DEBUG_TAG        = "PresetFilter";

    private boolean                 enabled         = true;
    private transient Preset        preset[]        = null;
    private transient Context       context;
    private transient PresetElement element         = null;
    private PresetElementPath       path            = null;
    private boolean                 includeWayNodes = false;
    private boolean                 inverted        = false;

    /**
     * Construct a new PresetFilter
     * 
     * @param context Android Context
     */
    public PresetFilter(Context context) {
        super();
        Log.d(DEBUG_TAG, "Constructor");
        init(context);
    }

    /**
     * Set the PresetItem or PresetGroup that is used for filtering
     * 
     * @param path the PresetELementPath of the item to use
     */
    void setPresetElement(@NonNull PresetElementPath path) {
        clear();
        if (path != null) {
            this.path = path;
            Preset[] presets = App.getCurrentPresets(context);
            Preset searchPreset = App.getCurrentRootPreset(context);
            element = Preset.getElementByPath(searchPreset.getRootGroup(), path);
            if (element == null) {
                Log.e(DEBUG_TAG, path.toString() + " not found");
                return;
            }
            Log.d(DEBUG_TAG, "Setting preset to " + element.getName() + " parent " + element.getParent());
            Preset filterPreset = new Preset(Arrays.asList(new Preset.PresetElement[] { element }));
            for (Preset p : presets) {
                if (p != null) {
                    filterPreset.addObjectKeys(p.getObjectKeys());
                }
            }
            preset = new Preset[] { filterPreset };
        }
        if (update != null) {
            update.execute();
        }
        setIcon();
    }

    /**
     * Return the current element used for filtering
     * 
     * @return the PresetEelemt currently in use
     */
    public PresetElement getPresetElement() {
        return element;
    }

    @Override
    public void init(Context context) {
        Log.d(DEBUG_TAG, "init");
        this.context = context;
        clear();
        if (path != null) {
            setPresetElement(path);
        }
    }

    /**
     * @return true if way nodes are incldued
     */
    public boolean includeWayNodes() {
        return includeWayNodes;
    }

    /**
     * Include way nodes when ways are included
     * 
     * @param on if true include Way nodes
     */
    public void setIncludeWayNodes(boolean on) {
        Log.d(DEBUG_TAG, "set include way nodes " + on);
        this.includeWayNodes = on;
    }

    /**
     * @return is the filter inverted?
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * Invert the filter
     * 
     * @param inverted invert the filter if true
     */
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    /**
     * Filter the OsemElement against a Preset
     * 
     * @param e the OsmElement
     * @return an Include enum indicating the status
     */
    private Include filter(@NonNull OsmElement e) {
        Include include = Include.DONT;
        if (preset != null) {
            PresetItem item = Preset.findMatch(preset, e.getTags());
            if (item != null) {
                include = includeWayNodes ? Include.INCLUDE_WITH_WAYNODES : Include.INCLUDE;
            }
            if (include == Include.DONT) {
                // check if it is a relation member
                List<Relation> parents = e.getParentRelations();
                if (parents != null) {
                    for (Relation r : parents) {
                        Include relationInclude = testRelation(r, false);
                        if (relationInclude != null && relationInclude != Include.DONT) {
                            return relationInclude; // inherit include status from relation
                        }
                    }
                }
            }
        }
        return include;
    }

    @Override
    public boolean include(Node node, boolean selected) {
        // Log.d(DEBUG_TAG, "include Node " + node.getOsmId() + "?");
        if (!enabled || selected) {
            return true;
        }
        Include include = cachedNodes.get(node);
        if (include != null) {
            // Log.d(DEBUG_TAG, "include Node " + include + " was in cache");
            return include != Include.DONT;
        }

        include = filter(node);
        // Log.d(DEBUG_TAG, "include Node " + include);
        cachedNodes.put(node, include);
        return include != Include.DONT;
    }

    @Override
    public boolean include(Way way, boolean selected) {
        if (!enabled) {
            return true;
        }
        Include include = cachedWays.get(way);
        if (include != null) {
            return include != Include.DONT;
        }

        include = filter(way);

        if (include == Include.INCLUDE_WITH_WAYNODES) {
            for (Node n : way.getNodes()) {
                Include includeNode = cachedNodes.get(n);
                if (includeNode == null || (include != Include.DONT && includeNode == Include.DONT)) {
                    // if not originally included overwrite now
                    if (include == Include.DONT && (n.hasTags() || n.hasParentRelations())) { // no entry yet so we have
                                                                                              // to check tags and
                                                                                              // relations
                        include(n, false);
                        continue;
                    }
                    cachedNodes.put(n, include);
                }
            }
        }
        cachedWays.put(way, include);

        return include != Include.DONT || selected;
    }

    @Override
    public boolean include(Relation relation, boolean selected) {
        return testRelation(relation, selected) != Include.DONT;
    }

    Include testRelation(Relation relation, boolean selected) {
        if (!enabled || selected) {
            return Include.INCLUDE_WITH_WAYNODES;
        }
        Include include = cachedRelations.get(relation);
        if (include != null) {
            return include;
        }

        include = filter(relation);

        cachedRelations.put(relation, include);
        List<RelationMember> members = relation.getMembers();
        if (members != null) {
            for (RelationMember rm : members) {
                OsmElement element = rm.getElement();
                if (element != null) {
                    if (element instanceof Way) {
                        Way w = (Way) element;
                        Include includeWay = cachedWays.get(w);
                        if (includeWay == null || (include != Include.DONT && includeWay == Include.DONT)) {
                            // if not originally included overwrite now
                            if (include == Include.INCLUDE_WITH_WAYNODES) {
                                for (Node n : w.getNodes()) {
                                    cachedNodes.put(n, include);
                                }
                            }
                            cachedWays.put(w, include);
                        }
                    } else if (element instanceof Node) {
                        Node n = (Node) element;
                        Include includeNode = cachedNodes.get(n);
                        if (includeNode == null || (include != Include.DONT && includeNode == Include.DONT)) {
                            // if not originally included overwrite now
                            cachedNodes.put(n, include);
                        }
                    } else if (element instanceof Relation) {
                        // FIXME not clear if we really want to do this
                    }
                }
            }
        }
        return include;
    }

    /**
     * Tag filter controls
     */
    private transient FloatingActionButton presetFilterButton;
    private transient ViewGroup            parent;
    private transient RelativeLayout       controls;
    private transient Update               update;

    @Override
    public void addControls(ViewGroup layout, final Update update) {
        Log.d(DEBUG_TAG, "adding filter controls");
        this.parent = layout;
        this.update = update;
        presetFilterButton = (FloatingActionButton) parent.findViewById(R.id.tagFilterButton);
        final Context context = layout.getContext();
        // we weren't already added ...
        if (presetFilterButton == null) {
            Preferences prefs = new Preferences(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            String buttonPos = layout.getContext().getString(R.string.follow_GPS_left);
            controls = (RelativeLayout) inflater
                    .inflate(prefs.followGPSbuttonPosition().equals(buttonPos) ? R.layout.tagfilter_controls_right : R.layout.tagfilter_controls_left, layout);
            presetFilterButton = (FloatingActionButton) controls.findViewById(R.id.tagFilterButton);
        }
        setIcon();
        presetFilterButton.setClickable(true);
        presetFilterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View b) {
                Log.d(DEBUG_TAG, "Button clicked");
                PresetFilterActivity.start(context);
            }
        });
        Util.setAlpha(presetFilterButton, Main.FABALPHA);
        setupControls(false);
    }

    /**
     * Set the icon on the filter button
     */
    private void setIcon() {
        if (element != null && presetFilterButton != null) {
            BitmapDrawable icon = element.getMapIcon();
            if (icon != null && icon.getBitmap() != null) {
                BitmapDrawable buttonIcon = new BitmapDrawable(context.getResources(), icon.getBitmap());
                presetFilterButton.setImageDrawable(buttonIcon);
            } else {
                presetFilterButton.setImageResource(R.drawable.ic_filter_list_black_36dp);
            }
        }
    }

    /**
     * Setup filter controls
     * 
     * @param toggle if true enable the controls
     */
    private void setupControls(boolean toggle) {
        enabled = toggle ? !enabled : enabled;
        update.execute();
    }

    @Override
    public void removeControls() {
        if (parent != null && controls != null) {
            parent.removeView(controls);
        }
    }

    @Override
    public void hideControls() {
        if (presetFilterButton != null) {
            presetFilterButton.setImageResource(R.drawable.ic_filter_list_black_36dp);
            presetFilterButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void showControls() {
        if (presetFilterButton != null) {
            presetFilterButton.setVisibility(View.VISIBLE);
        }
    }
}
