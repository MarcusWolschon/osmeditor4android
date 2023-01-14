package de.blau.android.easyedit;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import android.content.DialogInterface;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.ActionMenuView;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.HelpViewer;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.TagConflictDialog;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.RelationUtils;
import de.blau.android.osm.Result;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.SerializableState;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

/**
 * Base class for ActionMode callbacks inside {@link EasyEditManager}. Derived classes should call
 * {@link #onCreateActionMode(ActionMode, Menu)} and {@link #onDestroyActionMode(ActionMode)}. It will handle
 * registering and de-registering the action mode callback with the {@link EasyEditManager}. When the
 * {@link EasyEditManager} receives a click on a node or way, it may pass it to the current action mode callback. The
 * callback can then swallow it by returning true or allow the default handling to happen by returning false in the
 * {@link #handleElementClick(OsmElement)} method.
 * 
 * @author Jan
 * @author Simon
 *
 */
public abstract class EasyEditActionModeCallback implements ActionMode.Callback {

    private static final String       DEBUG_TAG    = "EasyEditActionModeCa...";
    protected int                     helpTopic    = 0;
    MenuUtil                          menuUtil;
    private ActionMenuView            cabBottomBar;
    protected final Main              main;
    protected final Logic             logic;
    protected final EasyEditManager   manager;
    protected ActionMode              mode;
    private boolean                   created      = true;
    protected Map<OsmElement, Result> savedResults = new HashMap<>();

    public static final int GROUP_MODE = 0;
    public static final int GROUP_BASE = 1;

    protected static final int MENUITEM_HELP = 0;

    /**
     * Construct a new ActionModeCallback
     * 
     * @param manager the current EasyEditManager instance
     */
    protected EasyEditActionModeCallback(@NonNull EasyEditManager manager) {
        this.main = manager.getMain();
        this.logic = App.getLogic();
        this.manager = manager;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        Log.d(DEBUG_TAG, "onCreateActionMode");
        manager.setCallBack(mode, this);
        this.mode = mode;
        main.hideLock();
        main.hideLayersControl();

        menuUtil = new MenuUtil(main);

        if (main.getBottomBar() != null) {
            View v = main.findViewById(R.id.cab_stub);
            if (v instanceof ViewStub) { // only need to inflate once
                ViewStub stub = (ViewStub) v;
                stub.setLayoutResource(R.layout.toolbar);
                stub.setInflatedId(R.id.cab_stub);
                cabBottomBar = (ActionMenuView) stub.inflate();
                MenuUtil.setupBottomBar(main, cabBottomBar, main.isFullScreen(), App.getPreferences(main).lightThemeEnabled());
            } else if (v instanceof ActionMenuView) {
                cabBottomBar = (ActionMenuView) v;
                cabBottomBar.setVisibility(View.VISIBLE);
                cabBottomBar.getMenu().clear();
            }
            main.hideBottomBar();
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Log.d(DEBUG_TAG, "onDestroyActionMode");
        manager.setCallBack(null, null);
        App.getLogic().hideCrosshairs();
        main.invalidateMap();
        main.triggerMenuInvalidation();
        if (cabBottomBar != null) {
            cabBottomBar.setVisibility(View.GONE);
            main.showBottomBar();
        }
        main.showLock();
        main.showLayersControl();
    }

    /**
     * This method gets called when the map is clicked, before checking for clicked nodes/ways. The ActionModeCallback
     * can then either return true to indicate that the click was handled (or should be ignored), or return false to
     * indicate default handling should apply (which includes checking for node/way clicks and calling the corresponding
     * methods).
     * 
     * @param x the x screen coordinate of the click
     * @param y the y screen coordinate of the click
     * @return true if the click has been handled, false if default handling should apply
     */
    public boolean handleClick(float x, float y) {
        return false;
    }

    /**
     * This method gets called when an OsmElement click has to be handled. The ActionModeCallback can then either return
     * true to indicate that the click was handled (or should be ignored), or return false to indicate default handling
     * should apply.
     * 
     * @param element the OsmElement that was clicked
     * @return true if the click has been handled, false if default handling should apply
     */
    public boolean handleElementClick(@NonNull OsmElement element) {
        return false;
    }

    /**
     * This method gets called when an OsmElement long click has to be handled. The ActionModeCallback can then either return
     * true to indicate that the click was handled (or should be ignored), or return false to indicate default handling
     * should apply.
     * 
     * @param element the OsmElement that was long clicked
     * @return true if the click has been handled, false if default handling should apply
     */
    public boolean handleElementLongClick(@NonNull OsmElement element) {
        return false;
    }
    
    /**
     * Indicate if the mode uses long clicks internally
     * 
     * @return true if the mode uses long click
     */
    public boolean usesLongClick() {
        return false;
    }

    /**
     * Check if the mode only supports selection of OSM elements
     * 
     * @return true is only OSM elements can be selected
     */
    public boolean elementsOnly() {
        return false;
    }

    /** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menuUtil.reset();
        // we should return true on the first invocation
        if (created) {
            created = false;
            return true;
        }
        /*
         * This is a hack around google not providing a way to handle clicking on the "done"/"close" button as it uses
         * reflection it is dependent on the code in the androidx libs. THis further cannot be called in
         * onCreateActionMode as the Views don't seem to have been inflated yet.
         */
        View close = getActionCloseView();
        if (close != null) {
            close.setOnClickListener(v -> onCloseClicked());
        }
        return false;
    }

    /** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Log.d(DEBUG_TAG, "onActionItemClicked");
        if (item.getItemId() == MENUITEM_HELP) {
            startHelp();
        }
        return false;
    }

    /**
     * Start the HelpViewer on the current topic
     */
    private void startHelp() {
        if (helpTopic != 0) {
            HelpViewer.start(main, helpTopic);
        } else {
            Snack.barWarning(main, R.string.toast_nohelp); // this is essentially just an error message
        }
    }

    /**
     * Modify behavior of back button in action mode
     * 
     * @return true
     */
    public boolean onBackPressed() {
        mode.finish();
        return true;
    }

    /**
     * Modify the behaviour of the "done"/"close" button
     */
    protected void onCloseClicked() {
        Log.d(DEBUG_TAG, "onCloseClicked");
        mode.finish();
    }

    /**
     * Process a short cut keyboard command
     * 
     * @param c the Character
     * @return true is an action was found
     */
    public boolean processShortcut(@NonNull Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_help)) {
            startHelp();
            return true;
        }
        return false;
    }

    /**
     * Try to arrange menu items a best as possible
     * 
     * @param menu the Menu to arrange
     */
    protected void arrangeMenu(@NonNull Menu menu) {
        menuUtil.setShowAlways(menu);
    }

    /**
     * Called if a context menu needs to be created
     * 
     * @param menu the menu
     * @return true if we created a menu
     */
    public boolean onCreateContextMenu(@NonNull ContextMenu menu) {
        return false;
    }

    /**
     * Called when tags have changed
     */
    protected void update() {
    }

    /**
     * Replace the menu used by the action mode by our toolbar if necessary
     * 
     * @param menu original menu
     * @param actionMode the current action mode
     * @param callback the callback we are currently in
     * @return the Menu
     */
    @NonNull
    protected Menu replaceMenu(@NonNull Menu menu, @NonNull final ActionMode actionMode, @NonNull final ActionMode.Callback callback) {
        if (cabBottomBar != null) {
            menu = cabBottomBar.getMenu();
            cabBottomBar.setOnMenuItemClickListener(item -> callback.onActionItemClicked(actionMode, item));
        }
        return menu;
    }

    /**
     * Finds which ways can be merged with a way. For this, the ways must not be equal, need to share at least one end
     * node, and either at least one of them must not have tags, or the tags on both ways must be equal.
     * 
     * @param way the way into which other ways may be merged
     * @return a list of all ways which can be merged into the given way
     */
    @NonNull
    protected Set<OsmElement> findMergeableWays(@NonNull Way way) {
        Set<Way> candidates = new HashSet<>();
        Set<OsmElement> result = new HashSet<>();
        final Node firstNode = way.getFirstNode();
        candidates.addAll(logic.getWaysForNode(firstNode));
        final Node lastNode = way.getLastNode();
        candidates.addAll(logic.getWaysForNode(lastNode));
        final SortedMap<String, String> tags = way.getTags();
        final Set<Entry<String, String>> tagsSet = tags.entrySet();
        for (Way candidate : candidates) {
            final SortedMap<String, String> candidateTags = candidate.getTags();
            if ((way != candidate) && (candidate.isEndNode(firstNode) || candidate.isEndNode(lastNode))
                    && (candidateTags.isEmpty() || tags.isEmpty() || tagsSet.equals(candidateTags.entrySet()))) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Finds which ways or nodes can be used as a via element in a restriction relation
     * 
     * @param way the from way
     * @return a list of all applicable objects
     */
    @NonNull
    protected Set<OsmElement> findViaElements(@NonNull Way way) {
        return findViaElements(way, true);
    }

    /**
     * Finds which ways or nodes can be used as a via element in a restriction relation
     * 
     * @param way the from way
     * @param includeNodes if true include via nodes
     * @return a list of all applicable objects
     */
    @NonNull
    protected Set<OsmElement> findViaElements(@NonNull Way way, boolean includeNodes) {
        Set<OsmElement> result = new HashSet<>();
        for (Node n : way.getNodes()) {
            for (Way w : logic.getWaysForNode(n)) {
                if (w.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
                    result.add(w);
                    if (includeNodes) {
                        result.add(n); // result is a set so we wont get dups
                    }
                }
            }
        }
        return result;
    }

    /**
     * Find possible elements for the "to" role of a restriction relation
     * 
     * @param viaElement the current via OSM element
     * @return a set of the candidate to OSM elements
     */
    @NonNull
    protected Set<OsmElement> findToElements(@NonNull OsmElement viaElement) {
        Set<OsmElement> result = new HashSet<>();
        Set<Node> nodes = new HashSet<>();
        if (Node.NAME.equals(viaElement.getName())) {
            nodes.add((Node) viaElement);
        } else if (Way.NAME.equals(viaElement.getName())) {
            nodes.addAll(((Way) viaElement).getNodes());
        } else {
            Log.e(DEBUG_TAG, "Unknown element type for via element " + viaElement.getName() + " " + viaElement.getDescription());
        }
        for (Node n : nodes) {
            for (Way w : logic.getWaysForNode(n)) {
                if (w.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
                    result.add(w);
                }
            }
        }
        return result;
    }

    /**
     * Attempts to retrieve the ActionMode close button using reflection.
     *
     * From:
     * https://stackoverflow.com/questions/27438644/how-do-we-show-a-back-button-instead-of-donecheckmark-button-in-the-contextual
     *
     * @return the View or null if not found
     */
    @Nullable
    private View getActionCloseView() {
        if (mode != null) {
            Object modeObject = mode;
            try {
                final Field wrappedObjectField = modeObject.getClass().getDeclaredField("mWrappedObject");
                wrappedObjectField.setAccessible(true); // NOSONAR
                modeObject = wrappedObjectField.get(mode);
            } catch (Exception ex) {
                // ignore
            }
            try {
                final Field contextViewField = modeObject.getClass().getDeclaredField("mContextView");
                contextViewField.setAccessible(true); // NOSONAR
                Object mContextView = contextViewField.get(modeObject);
                final Field closeField = mContextView.getClass().getDeclaredField("mClose");
                closeField.setAccessible(true); // NOSONAR
                final Object mClose = closeField.get(mContextView);
                if (mClose instanceof View) {
                    View closeButton = ((View) mClose).findViewById(R.id.action_mode_close_button);
                    if (closeButton == null) {
                        Log.e(DEBUG_TAG, "action_mode_close_button not found");
                    }
                    return closeButton;
                } else {
                    Log.e(DEBUG_TAG, "mClose has an unexpected type " + (mClose != null ? mClose.getClass().getCanonicalName() : " null"));
                }
            } catch (Exception ex) {
                Log.e(DEBUG_TAG, ex.getClass().getSimpleName() + " in #getActionCloseView: " + ex.getLocalizedMessage());
            }
        } else {
            Log.e(DEBUG_TAG, "getActionCloseView mode is null");
        }
        return null;
    }

    /**
     * Save any state that is needed to restart
     * 
     * @param state object to store state in
     */
    public void saveState(@NonNull SerializableState state) {
    }

    /**
     * Check the result of a way split operation, and if there was an error display a dialog
     * 
     * @param originalWay the original Way
     * @param resultList a List containing Results of the split, new Way in 1st
     */
    protected void checkSplitResult(@NonNull Way originalWay, @Nullable List<Result> resultList) {
        saveSplitResult(originalWay, resultList);
        if (!savedResults.isEmpty()) {
            TagConflictDialog.showDialog(main, new ArrayList<>(savedResults.values()));
        }
    }

    /**
     * Save the result of a way split operation for later display if necessary
     * 
     * @param originalWay the original Way
     * @param resultList a List containing Results of the split, new Way in 1st
     */
    protected void saveSplitResult(@NonNull Way originalWay, @Nullable List<Result> resultList) {
        if (resultList != null && !resultList.isEmpty() && (resultList.get(0).hasIssue() || resultList.size() > 1)) {
            List<Result> tempList = new ArrayList<>(resultList);
            Result first = tempList.get(0);
            if (first.hasIssue()) { // create a result for the original way
                Result orig = new Result();
                orig.setElement(originalWay);
                orig.addAllIssues(first.getIssues());
                tempList.add(1, orig);
            } else {
                tempList.remove(0);
            }
            for (Result r : tempList) {
                Result saved = savedResults.get(r.getElement());
                if (saved != null) {
                    saved.addAllIssues(r.getIssues());
                } else {
                    savedResults.put(r.getElement(), r);
                }
            }
        }
    }

    /**
     * Get the new Way from a split
     * 
     * @param result a List of Results
     * @return the new split off Way or null
     */
    @Nullable
    protected Way newWayFromSplitResult(@Nullable List<Result> result) {
        return result != null && !result.isEmpty() ? (Way) result.get(0).getElement() : null;
    }

    /**
     * Split ways after checking that we have all neighbouring relevant relation members
     * 
     * @param ways the List of Ways we will split
     * @param runnable run this once we've downloaded any missing ways or otherwise can continue
     */
    protected void splitSafe(@NonNull List<Way> ways, @NonNull Runnable runnable) {
        List<Long> missing = new ArrayList<>();
        for (Way way : ways) {
            missing.addAll(RelationUtils.checkForNeighbours(way));
        }
        if (!missing.isEmpty() && main.isConnectedOrConnecting()) {
            Builder builder = new AlertDialog.Builder(main);
            builder.setTitle(R.string.split_safe_title);
            builder.setMessage(R.string.split_safe_message);
            builder.setPositiveButton(R.string.download,
                    (DialogInterface dialog, int which) -> logic.downloadElements(main, null, missing, null, new PostAsyncActionHandler() {

                        @Override
                        public void onSuccess() {
                            runnable.run();
                        }

                        @Override
                        public void onError(@Nullable AsyncResult result) {
                            ErrorAlert.showDialog(main, result);
                        }
                    }));
            builder.setNegativeButton(R.string.ignore, (DialogInterface dialog, int which) -> runnable.run());
            builder.setNeutralButton(R.string.abort, (DialogInterface dialog, int which) -> manager.finish());
            builder.show();
        } else {
            runnable.run();
        }
    }

    /**
     * De-select elements
     * 
     * @param logic the Logic instance
     * @param deselectRelationElements if true de-select relation members too
     */
    protected static void deselect(@NonNull Logic logic, boolean deselectRelationElements) {
        logic.setClickableElements(null);
        logic.setReturnRelations(true);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        if (deselectRelationElements) { // back button or done pressed early
            logic.setSelectedRelationWays(null);
            logic.setSelectedRelationNodes(null);
        }
    }

    /**
     * Log and display a message when we didn't get the element we expected
     * 
     * @param debugTag the tag for the log
     * @param element the unexpected element
     */
    protected void unexpectedElement(@NonNull String debugTag, @NonNull OsmElement element) {
        Log.e(DEBUG_TAG, element.getName() + " clicked");
        Snack.toastTopError(main, main.getString(R.string.toast_unexpected_element, element.getDescription(true)));
    }
}