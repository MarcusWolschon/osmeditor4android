package de.blau.android.easyedit.turnrestriction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import android.view.Menu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import de.blau.android.R;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.NonSimpleActionModeCallback;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.StorageException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.osm.Way;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;

public class ViaElementActionModeCallback extends NonSimpleActionModeCallback {
    private static final String DEBUG_TAG  = "ViaElement..";
    private final Way           fromWay;
    private OsmElement          viaElement;
    private Set<OsmElement>     cachedToElements;
    private boolean             toSelected = false;
    private int                 titleId    = R.string.actionmode_restriction_to;

    /**
     * Construct a new callback for determining the to element of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param from selected "from" role Way
     * @param via selected "via" role OsmElement
     * @param results saved intermediate results
     */
    public ViaElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way from, @NonNull OsmElement via,
            @Nullable Map<OsmElement, Result> results) {
        super(manager);
        fromWay = from;
        viaElement = via;
        cachedToElements = findToElements(viaElement);
        if (results != null) {
            savedResults = results;
        }
    }

    /**
     * Construct a new callback for determining the to element of a turn restriction
     * 
     * @param manager the current EasyEditManager instance
     * @param from selected "from" role Way
     * @param via selected "via" role OsmElement
     * @param toElements potential "to" role OsmElements
     * @param results saved intermediate results
     */
    public ViaElementActionModeCallback(@NonNull EasyEditManager manager, @NonNull Way from, @NonNull OsmElement via, @NonNull Set<OsmElement> toElements,
            @Nullable Map<OsmElement, Result> results) {
        super(manager);
        fromWay = from;
        viaElement = via;
        cachedToElements = toElements;
        this.titleId = R.string.actionmode_restriction_restart_to;
        if (results != null) {
            savedResults = results;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_addingrestriction;
        mode.setTitle(titleId);
        logic.setClickableElements(cachedToElements);
        logic.setReturnRelations(false);
        if (Node.NAME.equals(viaElement.getName())) {
            logic.addSelectedRelationNode((Node) viaElement);
        } else {
            logic.addSelectedRelationWay((Way) viaElement);
        }
        super.onCreateActionMode(mode, menu);
        return true;
    }

    @Override
    public boolean handleElementClick(OsmElement element) { // NOSONAR
        // due to clickableElements, only valid elements can be clicked
        super.handleElementClick(element);
        final Way toWay = (Way) element;
        if (Node.NAME.equals(viaElement.getName())) {
            nextStep(element, (Node) viaElement, toWay);
        } else if (Way.NAME.equals(viaElement.getName())) {
            final Way viaWay = (Way) viaElement;
            final Node viaNode = ((Way) viaElement).getCommonNode(toWay);
            if (!viaWay.isEndNode(viaNode)) {
                splitSafe(Util.wrapInList(viaWay), () -> {
                    try {
                        // split via way and use appropriate segment
                        List<Result> result = logic.performSplit(main, viaWay, viaNode, true);
                        Way newViaWay = newWayFromSplitResult(result);
                        if (newViaWay != null) {
                            checkSplitResult(viaWay, result);
                            ScreenMessage.barInfo(main, R.string.toast_split_via);
                            if (fromWay.hasNode(newViaWay.getFirstNode()) || fromWay.hasNode(newViaWay.getLastNode())) {
                                viaElement = newViaWay;
                            }
                        } else {
                            Log.e(DEBUG_TAG, "newViaWay is null");
                        }
                        nextStep(element, viaNode, toWay);
                    } catch (OsmIllegalOperationException | StorageException ex) {
                        // toast has already been displayed
                        manager.finish();
                    }
                });
                return true;
            }
            nextStep(element, viaNode, toWay);
        } else {
            unexpectedElement(DEBUG_TAG, element);
            return true;
        }
        return true;
    }

    /**
     * The next step in adding the restriction
     * 
     * @param element the clicked element
     * @param viaNode the via node
     * @param toWay the to way
     */
    private void nextStep(@NonNull OsmElement element, @NonNull Node viaNode, @NonNull Way toWay) {
        if (viaElement != null && viaElement.equals(toWay)) {
            main.startSupportActionMode(
                    new RestrictionWaySplittingActionModeCallback(manager, R.string.actionmode_restriction_split_via, toWay, fromWay, savedResults));
        } else if (!toWay.isEndNode(viaNode) && !toWay.isClosed()) { // now check if we need to split the toWay
            splitSafe(Util.wrapInList(toWay), () -> {
                try {
                    List<Result> result = logic.performSplit(main, toWay, viaNode, true);
                    Way newToWay = newWayFromSplitResult(result);
                    saveSplitResult(toWay, result);
                    ScreenMessage.barInfo(main, R.string.toast_split_to);
                    Set<OsmElement> toCandidates = new HashSet<>();
                    toCandidates.add(toWay);
                    toCandidates.add(newToWay);
                    main.startSupportActionMode(new ViaElementActionModeCallback(manager, fromWay, viaElement, toCandidates, savedResults));
                } catch (OsmIllegalOperationException | StorageException ex) {
                    // toast has already been displayed
                    manager.finish();
                }
            });
        } else {
            toSelected = true;
            main.startSupportActionMode(new ToElementActionModeCallback(manager, fromWay, viaElement, (Way) element, savedResults)); // NOSONAR
            // viaElement can't actually be null
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        deselect(logic, !toSelected);
        super.onDestroyActionMode(mode);
    }
}
