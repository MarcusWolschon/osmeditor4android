package de.blau.android.filter;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Util;

/**
 * Filter plus UI for filtering on presets NOTE: the relevant ways should be processed before nodes
 * 
 * @author simon
 *
 */
public class PresetFilter extends CommonFilter {

    /**
     * 
     */
    private static final long   serialVersionUID = 7L;
    private static final String DEBUG_TAG        = "PresetFilter";

    public static final String                        FILENAME     = "lastpresetfilter.res";
    private transient SavingHelper<PresetElementPath> savingHelper = new SavingHelper<>();

    private transient Preset[]      preset          = null;
    private transient Context       context;
    private transient PresetElement element         = null;
    private PresetElementPath       path            = null;
    private boolean                 includeWayNodes = false;

    /**
     * Construct a new PresetFilter
     * 
     * @param context Android Context
     */
    public PresetFilter(@NonNull Context context) {
        super();
        Log.d(DEBUG_TAG, "Constructor");
        synchronized (this) {
            if (savingHelper != null) {
                path = savingHelper.load(context, FILENAME, false);
            }
        }
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
            Preset filterPreset = new Preset(Arrays.asList(element));
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
    @Nullable
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

    @Override
    public void saveState() {
        synchronized (this) {
            if (path != null && savingHelper != null) {
                savingHelper.save(context, FILENAME, path, false);
            }
        }
    }

    /**
     * @return true if way nodes are included
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

    @Override
    protected Include filter(@NonNull OsmElement e) {
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
            presetFilterButton.hide(); // workaround https://issuetracker.google.com/issues/117476935
            presetFilterButton.show();
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
            presetFilterButton.hide();
        }
    }

    @Override
    public void showControls() {
        if (presetFilterButton != null) {
            presetFilterButton.show();
        }
    }
}
