package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.Selection;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.util.Coordinates;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Util;

/**
 * Rotate the current selection
 */
public class RotationActionModeCallback extends AbortableActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RotationActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = RotationActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final String NODE_IDS_KEY    = "nodes";
    private static final String ANGLE_KEY       = "angle";
    private static final String COORDINATES_KEY = "coordinates";

    private Drawable savedButton;

    private float             totalAngle = Float.MAX_VALUE;
    private List<Node>        nodes      = null;
    /**
     * This will store the original Node coordinates
     */
    private List<Coordinates> coordinates;

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public RotationActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        totalAngle = state.getFloat(ANGLE_KEY);
        List<Long> ids = state.getList(NODE_IDS_KEY);
        StorageDelegator delegator = App.getDelegator();
        nodes = new ArrayList<>();
        for (Long id : ids) {
            Node node = (Node) delegator.getOsmElement(Way.NAME, id);
            if (node != null) {
                nodes.add(node);
            } else {
                throw new IllegalStateException("Failed to find node " + id);
            }
        }
        coordinates = state.getList(COORDINATES_KEY);
    }

    /**
     * Construct a new RotationActionModeCallback
     * 
     * Current assumes that the Way is selected
     * 
     * @param manager the current EasyEditMAnager instance
     * @param way the Way to rotate
     */
    public RotationActionModeCallback(@NonNull EasyEditManager manager) {
        super(manager);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_rotate;
        super.onCreateActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onCreateActionMode");
        logic.createCheckpoint(main, R.string.undo_action_rotate);
        logic.setRotationMode(true);
        logic.showCrosshairsForCentroid();

        FloatingActionButton button = main.getSimpleActionsButton();
        button.setOnClickListener(v -> manager.startElementSelectionMode());
        savedButton = button.getDrawable();
        button.setImageResource(R.drawable.check_white);
        mode.setTitle(R.string.actionmode_rotate);
        mode.setSubtitle(null);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        super.onPrepareActionMode(mode, menu);
        main.enableSimpleActionsButton();
        if (!prefs.areSimpleActionsEnabled()) {
            main.showSimpleActionsButton();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        logic.setRotationMode(false);
        logic.hideCrosshairs();
        FloatingActionButton button = main.getSimpleActionsButton();
        button.setImageDrawable(savedButton);
        main.setSimpleActionsButtonListener();
        if (!prefs.areSimpleActionsEnabled()) {
            main.hideSimpleActionsButton();
        }
        super.onDestroyActionMode(mode);
    }

    /**
     * Rotate selected objects
     * 
     * Note that this needs to rotate all the nodes of all objects at once to avoid rotating the same one multiple
     * times, special case exactly one node selected.
     * 
     * @param activity the current Activity
     * @param selection the Selection
     * @param centroidX x centroid coordinate
     * @param centroidY y centroid coordinate
     * @param angle angle in radians that we rotated
     * @param direction rotation direction (+ == clockwise)
     */
    public void rotateSelection(@NonNull FragmentActivity activity, @NonNull final Selection selection, float centroidX, float centroidY, float angle,
            int direction) {
        Logic logic = App.getLogic();
        Map map = logic.getMap();
        int w = map.getWidth();
        int h = map.getHeight();
        ViewBox v = map.getViewBox();
        StorageDelegator delegator = App.getDelegator();
        if (nodes == null) {
            // special case for rotating nodes
            if (selection.count() == 1 && selection.nodeCount() == 1) {
                updateDirection(activity, (float) Math.toDegrees(angle), direction, selection.getNodes().get(0));
                return;
            }

            nodes = getNodesFromSelection(selection);

            coordinates = new ArrayList<>(nodes.size());
            totalAngle = direction * angle;
        } else {
            totalAngle = this.totalAngle + direction * angle;
        }

        delegator.rotateNodes(nodes, totalAngle, centroidX, centroidY, w, h, v, coordinates);
    }

    /**
     * Get all unique nodes from a Selection object
     * 
     * @param selection the Selection
     * @return a List of Nodes
     */
    @NonNull
    private List<Node> getNodesFromSelection(@NonNull final Selection selection) {
        List<Node> result = new ArrayList<>();
        List<Node> selectedNodes = selection.getNodes();
        if (Util.notEmpty(selectedNodes)) {
            result.addAll(selectedNodes);
        }

        List<Way> selectedWays = selection.getWays();
        if (selectedWays != null) {
            for (Way way : selectedWays) {
                result.addAll(way.getNodes());
            }
        }

        List<Relation> relations = selection.getRelations();
        if (relations != null) {
            addNodesFromRelations(relations, result);
        }

        // make the nodes in the list unique
        return new ArrayList<>(new HashSet<>(result));
    }

    /**
     * Add nodes from multipolygons to the provided List
     * 
     * @param relations the Relations
     * @param result the resulting List
     */
    private void addNodesFromRelations(@NonNull List<Relation> relations, @NonNull List<Node> result) {
        for (Relation r : relations) {
            if (!r.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON) || !r.allDownloaded()) {
                continue;
            }
            for (RelationMember m : r.getMembers(Way.NAME)) {
                OsmElement e = m.getElement();
                if (e instanceof Way) { // have to test for null in any case
                    result.addAll(((Way) e).getNodes());
                }
            }
        }
    }

    /**
     * Update the direction tag of a node
     * 
     * @param angle the angle to turn in degrees
     * @param direction rotation direction (+ == clockwise)
     * @param node the Node
     */
    private void updateDirection(@NonNull Context context, float angle, int direction, @NonNull final Node node) {
        // this is obviously rather expensive bit avoids having state somewhere else
        String directionKey = Tags.getDirectionKey(Preset.findBestMatch(context, App.getCurrentPresets(context), node.getTags(), null, node, false), node);
        if (directionKey == null) {
            Log.e(DEBUG_TAG, "direction key not found");
            return;
        }
        java.util.Map<String, String> tags = new HashMap<>(node.getTags());
        Float currentAngle = Tags.parseDirection(tags.get(directionKey));
        if (currentAngle == Float.NaN) {
            currentAngle = 0f;
        }
        angle = (currentAngle + angle * direction) % 360f;
        if (angle < 0) {
            angle = angle + 360f;
        }
        tags.put(directionKey, Integer.toString((int) angle));
        App.getDelegator().setTags(node, tags);
        Logic logic = App.getLogic();
        Map map = logic.getMap();
        map.invalidate();
    }

    @Override
    public void saveState(SerializableState state) {
        state.putFloat(ANGLE_KEY, totalAngle);
        List<Long> nodeIds = new ArrayList<>();
        for (Node n : nodes) {
            nodeIds.add(n.getOsmId());
        }
        state.putList(NODE_IDS_KEY, nodeIds);
        state.putList(COORDINATES_KEY, coordinates);
    }
}
