package de.blau.android.filter;

import java.util.List;

import android.content.Context;
import android.os.Build;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.Density;
import de.blau.android.util.Util;

/**
 * Filter plus UI for indoor tagging see https://wiki.openstreetmap.org/wiki/Simple_Indoor_Tagging
 * 
 * NOTE: the relevant ways should be processed before nodes
 * 
 * @author simon
 *
 */
public class IndoorFilter extends InvertableFilter {
    /**
     * 
     */
    private static final long   serialVersionUID = 4L;
    private static final String DEBUG_TAG        = "IndoorFilter";

    /**
     * Current level
     */
    private int level = 0;

    /**
     * Construct a new instance of IndoorFilter
     */
    public IndoorFilter() {
        super();
    }

    @Override
    public boolean include(Node node, boolean selected) {
        Include include = cachedNodes.get(node);
        if (include != null) {
            return include != Include.DONT;
        }
        if (!inverted) {
            include = (selected
                    || (node.hasTags() && (contains(node.getTagWithKey(Tags.KEY_LEVEL), level) || contains(node.getTagWithKey(Tags.KEY_REPEAT_ON), level))))
                            ? Include.INCLUDE
                            : Include.DONT;
        } else {
            include = (selected || (node.hasTags() && !node.hasTagKey(Tags.KEY_LEVEL) && !node.hasTagKey(Tags.KEY_REPEAT_ON))) ? Include.INCLUDE : Include.DONT;
        }

        if (include == Include.DONT) {
            // check if it is a relation member
            List<Relation> parents = node.getParentRelations();
            if (parents != null) {
                for (Relation r : parents) {
                    if (include(r, false)) {
                        include = Include.INCLUDE; // inherit include status from relation
                        break;
                    }
                }
            }
        }

        cachedNodes.put(node, include);
        return include != Include.DONT;
    }

    @Override
    public boolean include(Way way, boolean selected) {
        Include include = cachedWays.get(way);
        if (include != null) {
            return include != Include.DONT;
        }
        if (!inverted) {
            include = (selected || (way.hasTags() && (contains(way.getTagWithKey(Tags.KEY_LEVEL), level)
                    || contains(way.getTagWithKey(Tags.KEY_REPEAT_ON), level) || buildingHasLevel(way, level)))) ? Include.INCLUDE : Include.DONT;
        } else {
            include = (selected || (way.hasTags() && !way.hasTagKey(Tags.KEY_LEVEL) && !way.hasTagKey(Tags.KEY_REPEAT_ON)
                    && !(way.hasTagKey(Tags.KEY_MIN_LEVEL) || way.hasTagKey(Tags.KEY_MAX_LEVEL)))) ? Include.INCLUDE : Include.DONT;
        }

        if (include == Include.DONT) {
            // check if it is a relation member
            List<Relation> parents = way.getParentRelations();
            if (parents != null) {
                for (Relation r : parents) {
                    if (include(r, false)) {
                        include = Include.INCLUDE; // inherit include status from relation
                        break;
                    }
                }
            }
        }

        for (Node n : way.getNodes()) {
            Include includeNode = cachedNodes.get(n);
            if (includeNode == null || (include != Include.DONT && includeNode == Include.DONT)) {
                // if not originally included overwrite now
                if (include == Include.DONT && (n.hasTags() || n.hasParentRelations())) { // no entry yet so we have to
                                                                                          // check tags and relations
                    include(n, false);
                    continue;
                }
                cachedNodes.put(n, include);
            }
        }
        cachedWays.put(way, include);

        return include != Include.DONT;
    }

    @Override
    public boolean include(Relation relation, boolean selected) {
        Include include = cachedRelations.get(relation);
        if (include != null) {
            return include != Include.DONT;
        }
        if (!inverted) {
            include = (selected || buildingHasLevel(relation, level)) ? Include.INCLUDE : Include.DONT;
        } else {
            include = (selected || (relation.hasTags() && !(relation.hasTagKey(Tags.KEY_MIN_LEVEL) || relation.hasTagKey(Tags.KEY_MAX_LEVEL))))
                    ? Include.INCLUDE
                    : Include.DONT;
        }

        cachedRelations.put(relation, include);
        List<RelationMember> members = relation.getMembers();
        if (members != null) {
            for (RelationMember rm : members) {
                OsmElement element = rm.getElement();
                if (element != null) {
                    if (element instanceof Way) {
                        Way w = (Way) element;
                        Include includeWay = cachedWays.get(w);
                        if (includeWay == null) {
                            // status hasn't been determined, do that now
                            includeWay = include(w, false) ? Include.INCLUDE_WITH_WAYNODES : Include.DONT;
                        }
                        if (include != Include.DONT && includeWay == Include.DONT) {
                            // if not originally included overwrite now
                            for (Node n : w.getNodes()) {
                                cachedNodes.put(n, include);
                            }
                            cachedWays.put(w, include);
                        }
                    } else if (element instanceof Node) {
                        Node n = (Node) element;
                        Include includeNode = cachedNodes.get(n);
                        if (includeNode == null) {
                            // status hasn't been determined, do that now
                            includeNode = include(n, false) ? Include.INCLUDE : Include.DONT;
                        }
                        if (include != Include.DONT && includeNode == Include.DONT) {
                            // if not originally included overwrite now
                            cachedNodes.put(n, include);
                        }
                    } else if (element instanceof Relation) {
                        // FIXME determine if further inheritance actually makes sense
                    }
                }
            }
        }

        return include != Include.DONT;
    }

    /**
     * Check if a specific level is included in a level spec
     * 
     * @param levelSpec either a single integer, a semi-colon separated list, or a range
     * @param level level we are interested in
     * @return true if the level is contained in levelSpec
     */
    private boolean contains(String levelSpec, int level) {
        if (levelSpec == null || "".equals(levelSpec)) {
            return false;
        }
        String[] l = levelSpec.split(";");
        if (l.length > 1) {
            for (String i : l) {
                // noinspection EmptyCatchBlock
                try {
                    if (Integer.parseInt(i) == level) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            return false;
        } else {
            int hyphen = levelSpec.indexOf('-', 1);
            if (hyphen > 0) { // needs to be split
                l = levelSpec.split("-", 2);
                if (l.length == 2 && !"".equals(l[0])) {
                    try {
                        return level >= Integer.parseInt(l[0]) && level <= Integer.parseInt(l[1]);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                try {
                    return level == Integer.parseInt(levelSpec);
                } catch (NumberFormatException e) {
                    return false;
                }
            } else {
                try {
                    return level == Integer.parseInt(levelSpec);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
    }

    /**
     * Check if a object is a building or building:part, has min_level and max_level keys and level is between the min
     * and max
     * 
     * @param b the OsmElement
     * @param level our current level
     * @return true if the building/building:part has a level between (inclusive) min/max
     */
    private static boolean buildingHasLevel(OsmElement b, int level) {
        if (b.hasTagKey(Tags.KEY_BUILDING) || b.hasTagKey(Tags.KEY_BUILDING_PART)) {
            String minLevel = b.getTagWithKey(Tags.KEY_MIN_LEVEL);
            String maxLevel = b.getTagWithKey(Tags.KEY_MAX_LEVEL);
            if (minLevel != null && maxLevel != null) {
                try {
                    return level >= Integer.parseInt(minLevel) && level <= Integer.parseInt(maxLevel);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * @return indoor mode level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Set level used in indoor mode
     * 
     * @param level the level to set
     */
    public void setLevel(int level) {
        if (level != this.level) {
            clear();
        }
        this.level = level;
    }

    /**
     * Indoor filter controls
     */
    private transient FloatingActionButton        levelUp;
    private transient FrameLayout                 levelDisplay;
    private transient TextView                    levelText;
    private transient FloatingActionButton        levelDown;
    private transient ViewGroup                   parent;
    private transient RelativeLayout              controls;
    private transient Update                      update;
    private transient RelativeLayout.LayoutParams originalLayoutParamsUp;
    private transient RelativeLayout.LayoutParams disabledLayoutParamsUp;
    private transient RelativeLayout.LayoutParams originalLayoutParamsDown;
    private transient RelativeLayout.LayoutParams disabledLayoutParamsDown;

    @Override
    public void addControls(ViewGroup layout, final Update update) {
        Log.d(DEBUG_TAG, "adding filter controls");
        this.parent = layout;
        this.update = update;
        levelUp = (FloatingActionButton) parent.findViewById(R.id.levelUp);
        levelDisplay = (FrameLayout) parent.findViewById(R.id.level);
        levelText = (TextView) parent.findViewById(R.id.levelText);
        FloatingActionButton levelTextButton = (FloatingActionButton) parent.findViewById(R.id.levelTextButton);
        levelDown = (FloatingActionButton) parent.findViewById(R.id.levelDown);
        // we weren't already added ...
        if (levelUp == null || levelDisplay == null || levelText == null || levelDown == null) {
            Context context = layout.getContext();
            Preferences prefs = new Preferences(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            controls = (RelativeLayout) inflater
                    .inflate(prefs.followGPSbuttonPosition().equals("LEFT") ? R.layout.indoor_controls_right : R.layout.indoor_controls_left, layout);
            levelUp = (FloatingActionButton) controls.findViewById(R.id.levelUp);
            levelDisplay = (FrameLayout) controls.findViewById(R.id.level);
            levelText = (TextView) controls.findViewById(R.id.levelText);
            levelTextButton = (FloatingActionButton) controls.findViewById(R.id.levelTextButton);
            levelDown = (FloatingActionButton) controls.findViewById(R.id.levelDown);
            //
            // the following is a hack around https://github.com/MarcusWolschon/osmeditor4android/issues/771
            //
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                originalLayoutParamsUp = (LayoutParams) levelUp.getLayoutParams();
                disabledLayoutParamsUp = new RelativeLayout.LayoutParams(originalLayoutParamsUp);
                originalLayoutParamsDown = (LayoutParams) levelDown.getLayoutParams();
                disabledLayoutParamsDown = new RelativeLayout.LayoutParams(originalLayoutParamsDown);
                int margin = Density.dpToPx(context, 8);
                disabledLayoutParamsUp.setMargins(margin, margin, margin, margin);
                disabledLayoutParamsDown.setMargins(margin, margin, margin, margin);
            }
        }

        // indoor controls
        levelUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int level = getLevel();
                Log.d(DEBUG_TAG, "Current level " + level);
                updateLevel(level + 1);
                update.execute();
            }
        });
        Util.setAlpha(levelUp, Main.FABALPHA);
        levelText.setText(Integer.toString(getLevel()));
        levelTextButton.setClickable(true);
        levelTextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View b) {
                Log.d(DEBUG_TAG, "Level clicked");
                setupControls(true);
            }
        });
        Util.setAlpha(levelTextButton, Main.FABALPHA);
        levelDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int level = getLevel();
                Log.d(DEBUG_TAG, "Current level " + level);
                updateLevel(level - 1);
                update.execute();
            }
        });
        Util.setAlpha(levelDown, Main.FABALPHA);
        setupControls(false);
    }

    /**
     * Setup the up and down buttons and the level display
     * 
     * @param toggle if true toggle between inverted and normal filter mode
     */
    private void setupControls(boolean toggle) {
        if (toggle) {
            clear();
        }
        inverted = toggle ? !inverted : inverted;
        if (inverted) {
            levelText.setText("--");
            levelUp.setEnabled(false);
            levelDown.setEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && disabledLayoutParamsUp != null && disabledLayoutParamsDown != null) {
                levelUp.setLayoutParams(disabledLayoutParamsUp);
                levelDown.setLayoutParams(disabledLayoutParamsDown);
            }
        } else {
            updateLevel(level);
            levelUp.setEnabled(true);
            levelDown.setEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && originalLayoutParamsUp != null && originalLayoutParamsDown != null) {
                levelUp.setLayoutParams(originalLayoutParamsUp);
                levelDown.setLayoutParams(originalLayoutParamsDown);
            }
        }
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
        // NOTE order is important
        if (levelDown != null) {
            levelDown.hide();
        }
        if (levelDisplay != null) {
            levelDisplay.setVisibility(View.GONE);
        }
        if (levelUp != null) {
            levelUp.hide();
        }
    }

    @Override
    public void showControls() {
        // NOTE order is important
        if (levelUp != null) {
            levelUp.show();
        }
        if (levelDisplay != null) {
            levelDisplay.setVisibility(View.VISIBLE);
        }
        if (levelDown != null) {
            levelDown.show();
        }
    }

    /**
     * Update the displayed level
     * 
     * @param level the level to show on the button
     */
    private void updateLevel(int level) {
        Log.d(DEBUG_TAG, "setting level to " + level);
        if (levelText != null) {
            levelText.setText(Integer.toString(level));
        }
        setLevel(level);
    }
}
