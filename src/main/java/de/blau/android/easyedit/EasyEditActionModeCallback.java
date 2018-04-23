package de.blau.android.easyedit;

import java.util.HashSet;
import java.util.Set;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.ActionMenuView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.Snack;

/**
 * Base class for ActionMode callbacks inside {@link EasyEditManager}. Derived classes should call
 * {@link #onCreateActionMode(ActionMode, Menu)} and {@link #onDestroyActionMode(ActionMode)}. It will handle
 * registering and de-registering the action mode callback with the {@link EasyEditManager}. When the
 * {@link EasyEditManager} receives a click on a node or way, it may pass it to the current action mode callback. The
 * callback can then swallow it by returning true or allow the default handling to happen by returning false in the
 * {@link #handleElementClick(OsmElement)} method.
 * 
 * @author Jan
 *
 */
public abstract class EasyEditActionModeCallback implements ActionMode.Callback {

    private static final String     DEBUG_TAG = "EasyEditActionModeCa...";
    protected int                   helpTopic = 0;
    MenuUtil                        menuUtil;
    private ActionMenuView          cabBottomBar;
    protected final Main            main;
    protected final Logic           logic;
    protected final EasyEditManager manager;
    ActionMode                      mode;

    public static final int GROUP_MODE = 0;
    public static final int GROUP_BASE = 1;

    protected static final int MENUITEM_HELP = 0;

    protected EasyEditActionModeCallback(EasyEditManager manager) {
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
                Preferences prefs = new Preferences(main);
                MenuUtil.setupBottomBar(main, cabBottomBar, main.isFullScreen(), prefs.lightThemeEnabled());
            } else if (v instanceof ActionMenuView) {
                cabBottomBar = (ActionMenuView) v;
                cabBottomBar.setVisibility(View.VISIBLE);
                cabBottomBar.getMenu().clear();
            }
            main.hideBottomBar();
        }
        return false;
    }

    /**
     * Override this is you want to create a custom context menu in onCreateContextMenu
     * 
     * @return false as default
     */
    public boolean needsCustomContextMenu() {
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
    public boolean handleElementClick(OsmElement element) {
        return false;
    }

    /** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menuUtil.reset();
        return false;
    }

    /** {@inheritDoc} */ // placed here for convenience, allows to avoid unnecessary methods in subclasses
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Log.d(DEBUG_TAG, "onActionItemClicked");
        if (item.getItemId() == MENUITEM_HELP) {
            if (helpTopic != 0) {
                HelpViewer.start(main, helpTopic);
            } else {
                Snack.barWarning(main, R.string.toast_nohelp); // this is essentially just an error message
            }
        }
        return false;
    }

    /**
     * modify behavior of back button in action mode
     * 
     * @return
     */
    public boolean onBackPressed() {
        mode.finish();
        return true;
    }

    public boolean processShortcut(Character c) {
        return false;
    }

    void arrangeMenu(Menu menu) {
        menuUtil.setShowAlways(menu);
    }

    public void onCreateContextMenu(ContextMenu menu) {
    }

    /**
     * Replace the menu used by the action mode by our toolbar if necessary
     * 
     * @param menu original menu
     * @param actionMode the current action mode
     * @param callback the callback we are currently in
     * @return
     */
    protected Menu replaceMenu(Menu menu, final ActionMode actionMode, final ActionMode.Callback callback) {
        if (cabBottomBar != null) {
            menu = cabBottomBar.getMenu();
            android.support.v7.widget.ActionMenuView.OnMenuItemClickListener listener = new android.support.v7.widget.ActionMenuView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return callback.onActionItemClicked(actionMode, item);
                }
            };
            cabBottomBar.setOnMenuItemClickListener(listener);
        }
        return menu;
    }

    /**
     * Takes a parameter for a node and one for a way. If the way is not null, opens a tag editor for the way.
     * Otherwise, opens a tag editor for the node (unless the node is also null, then nothing happens).
     * 
     * @param possibleNode a node that was edited, or null
     * @param possibleWay a way that was edited, or null
     * @param select select the element before starting the PropertyEditor
     * @param askForName ask for a name tag first
     */
    void tagApplicable(@Nullable final Node possibleNode, @Nullable final Way possibleWay, final boolean select, final boolean askForName) {
        if (possibleWay == null) {
            // Single node was added
            if (possibleNode != null) { // null-check to be sure
                if (select) {
                    main.startSupportActionMode(new NodeSelectionActionModeCallback(manager, possibleNode));
                }
                main.performTagEdit(possibleNode, null, false, false, askForName);
            } else {
                Log.e(DEBUG_TAG, "tagApplicable called with null arguments");
            }
        } else { // way was added
            if (select) {
                main.startSupportActionMode(new WaySelectionActionModeCallback(manager, possibleWay));
            }
            main.performTagEdit(possibleWay, null, false, false, askForName);
        }
    }

    /**
     * Finds which ways can be merged with a way. For this, the ways must not be equal, need to share at least one end
     * node, and either at least one of them must not have tags, or the tags on both ways must be equal.
     * 
     * @param way the way into which other ways may be merged
     * @return a list of all ways which can be merged into the given way
     */
    protected Set<OsmElement> findMergeableWays(@NonNull Way way) {
        Set<Way> candidates = new HashSet<>();
        Set<OsmElement> result = new HashSet<>();
        candidates.addAll(logic.getWaysForNode(way.getFirstNode()));
        candidates.addAll(logic.getWaysForNode(way.getLastNode()));
        for (Way candidate : candidates) {
            if ((way != candidate) && (candidate.isEndNode(way.getFirstNode()) || candidate.isEndNode(way.getLastNode()))
                    && (candidate.getTags().isEmpty() || way.getTags().isEmpty() || way.getTags().entrySet().equals(candidate.getTags().entrySet()))
            // TODO check for relations too
            ) {
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
    protected Set<OsmElement> findViaElements(@NonNull Way way) {

        Set<OsmElement> result = new HashSet<>();
        for (Node n : way.getNodes()) {
            for (Way w : logic.getWaysForNode(n)) {
                if (w.getTagWithKey(Tags.KEY_HIGHWAY) != null) {
                    result.add(w);
                    result.add(n); // result is a set so we wont get dups
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
}
