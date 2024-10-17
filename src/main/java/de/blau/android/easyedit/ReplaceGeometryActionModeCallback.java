package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.dialogs.ElementIssueDialog;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.MergeAction;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
import de.blau.android.util.SerializableState;

/**
 * Callback for replacing geometry for nodes and ways
 * 
 * @author simon
 *
 */
public class ReplaceGeometryActionModeCallback extends NonSimpleActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ReplaceGeometryActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = ReplaceGeometryActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TARGET_TYPE_KEY = "target type";

    private final OsmElement target;
    private List<Way>        visibleWays = new ArrayList<>();

    /**
     * Construct a new callback for replacing geometry for nodes and ways
     * 
     * @param manager the current EasyEditManager instance
     * @param target the node or way that will get its geometry replaced
     */
    public ReplaceGeometryActionModeCallback(@NonNull EasyEditManager manager, @NonNull OsmElement target) {
        super(manager);
        this.target = target;
    }

    /**
     * Construct a new callback from saved state
     * 
     * @param manager the current EasyEditManager instance
     * @param state the saved state
     */
    public ReplaceGeometryActionModeCallback(@NonNull EasyEditManager manager, @NonNull SerializableState state) {
        super(manager);
        if (Way.NAME.equals(state.getString(TARGET_TYPE_KEY))) {
            target = getSavedWay(state);
        } else {
            target = getSavedNode(state);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setSubtitle(R.string.actionmode_subtitle_replace_geometry);
        App.getDelegator().getCurrentStorage().getWays(logic.getViewBox(), visibleWays);
        if (target instanceof Way) {
            visibleWays.remove(target);
        }
        logic.setClickableElements(new HashSet<>(visibleWays));
        logic.setReturnRelations(false);
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        logic.setClickableElements(null);
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        super.handleElementClick(element);
        if (element.equals(target)) {
            return true;
        }

        java.util.Map<String, String> targetTags = new HashMap<>(target.getTags());
        Logic logic = App.getLogic();
        try {
            if (target instanceof Way) {
                final List<Result> result = logic.performReplaceGeometry(main, (Way) target, ((Way) element).getNodes());
                AlertDialog.Builder builder = new AlertDialog.Builder(main);
                builder.setTitle(R.string.remove_geometry_source);
                builder.setPositiveButton(R.string.Yes, (dialog, id) -> logic.performEraseWay(main, ((Way) element), true, false));
                builder.setNegativeButton(R.string.No, null);
                AlertDialog d = builder.create();
                d.setOnDismissListener((DialogInterface dialog) -> {
                    main.startSupportActionMode(new WaySelectionActionModeCallback(manager, (Way) target));
                    if (!result.isEmpty()) {
                        ElementIssueDialog.showReplaceGeometryIssuetDialog(main, result);
                    }
                });
                d.show();
            }
            if (target instanceof Node) {
                // arguably this section should really be in Logic 
                logic.createCheckpoint(main, R.string.undo_action_replace_geometry);
                Node toReplace = findYoungestUntaggedNode((Way) element);
                logic.setTags(main, Node.NAME, target.getOsmId(), null, false);
                logic.performSetPosition(main, (Node) target, toReplace.getLon(), toReplace.getLat(), false);
                MergeAction merge = new MergeAction(App.getDelegator(), target, toReplace, false);
                List<Result> mergeResult = merge.mergeNodes();
                java.util.Map<String, String> mergedTags = MergeAction.mergeTags(element, targetTags);
                Result r = mergeResult.get(0);
                MergeAction.checkForMergedTags(targetTags, element.getTags(), mergedTags, r);
                logic.setTags(main, Way.NAME, element.getOsmId(), mergedTags, false);
                if (mergeResult.size() > 1 || r.hasIssue()) {
                    ElementIssueDialog.showTagConflictDialog(main, mergeResult);
                }
                logic.deselectAll();
                logic.addSelectedWay((Way) element);
                main.startSupportActionMode(new WaySelectionActionModeCallback(manager, (Way) element));
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            // logic will have toasted
            Log.w(DEBUG_TAG, ex.getMessage());
        }
        return true;
    }

    /**
     * Find a suitable node for replacement in a Way
     * 
     * @param way the Way
     * @return a Node
     */
    @NonNull
    private Node findYoungestUntaggedNode(@NonNull Way way) {
        Node result = way.getFirstNode();
        for (Node wn : way.getNodes()) {
            if (result.isTagged() && !wn.isTagged()) {
                result = wn;
                continue;
            }
            if (result.isTagged() == wn.isTagged() && (wn.getOsmId() > result.getOsmId() || wn.getOsmId() < 0)) {
                result = wn;
            }
        }
        return result;
    }

    @Override
    public void saveState(SerializableState state) {
        state.putLong(target instanceof Way ? WAY_ID_KEY : NODE_ID_KEY, target.getOsmId());
        state.putString(TARGET_TYPE_KEY, target.getName());
    }
}
