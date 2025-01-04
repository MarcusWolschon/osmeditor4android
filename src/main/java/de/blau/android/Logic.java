package de.blau.android;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

import javax.net.ssl.SSLProtocolException;
import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.osmosis.osmbinary.file.BlockInputStream;
import org.openstreetmap.osmosis.osmbinary.file.BlockReaderAdapter;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Selection.Ids;
import de.blau.android.contract.HttpStatusCodes;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.AttachedObjectWarning;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.dialogs.ForbiddenLogin;
import de.blau.android.dialogs.InvalidLogin;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.dialogs.UploadConflict;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.easyedit.ElementSelectionActionModeCallback;
import de.blau.android.exception.DataConflictException;
import de.blau.android.exception.IllegalOperationException;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.exception.StorageException;
import de.blau.android.exception.UnsupportedFormatException;
import de.blau.android.filter.Filter;
import de.blau.android.gpx.Track;
import de.blau.android.imageryoffset.ImageryAlignmentActionModeCallback;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.data.MapOverlay;
import de.blau.android.osm.ApiResponse;
import de.blau.android.osm.ApiResponse.Conflict;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.DiscardedTags;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.MapSplitSource;
import de.blau.android.osm.MergeAction;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmChangeParser;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmGpxApi;
import de.blau.android.osm.OsmGpxApi.Visibility;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.OsmPbfParser;
import de.blau.android.osm.OsmXml;
import de.blau.android.osm.PostMergeHandler;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.osm.RelationUtils;
import de.blau.android.osm.ReplaceIssue;
import de.blau.android.osm.Result;
import de.blau.android.osm.Server;
import de.blau.android.osm.SplitIssue;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.UndoStorage.Checkpoint;
import de.blau.android.osm.UserDetails;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.EditState;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Geometry;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MRUList;
import de.blau.android.util.collections.MultiHashMap;
import de.blau.android.validation.Validator;

/**
 * Logic is the gatekeeper to actual object storage and provides higher level operations.
 * <ul>
 * <li>hold selected objects
 * <li>wrap operations with undo checkpoints
 * <li>hold current mode
 * <li>save and load state
 * </ul>
 * 
 * @author mb
 * @author Simon Poole
 */
public class Logic {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Logic.class.getSimpleName().length());
    private static final String DEBUG_TAG = Logic.class.getSimpleName().substring(0, TAG_LEN);

    private static final int    EXECUTOR_THREADS = 4;
    private static final String METHOD_UPLOAD    = "upload";

    /**
     * Enums for directions. Used for translation via cursor-pad.
     */
    public enum CursorPaddirection {
        DIRECTION_LEFT, DIRECTION_DOWN, DIRECTION_RIGHT, DIRECTION_UP
    }

    /**
     * Enums for zooming.
     */
    public static final boolean ZOOM_IN = true;

    public static final boolean ZOOM_OUT = false;

    /**
     * Minimum width of the viewBox for showing the tolerance. When the viewBox is wider, no element selection is
     * possible.
     */
    private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 2;

    /**
     * In MODE_EDIT this value is used for the padding of the display border.
     */
    private static final int PADDING_ON_BORDER_TOUCH = 5;

    /**
     * In MODE_EDIT, when the user moves a node {@link PADDING_ON_BORDER_TOUCH} pixel to a border, the map will be
     * translated by this factor.
     */
    private static final double BORDER_TOCH_TRANSLATION_FACTOR = 0.02;

    /**
     * Translation factor used by cursor-pad.
     */
    private static final float TRANSLATION_FACTOR = 0.15f;

    /**
     * Global factor for all nodes and lines.
     */
    public static final float STROKE_FACTOR = 100000;

    /**
     * Filename of file containing the currently edit state
     */
    private static final String EDITSTATE_FILENAME = "edit.state";

    /** Sorter instance for sorting nodes by distance */
    private static final DistanceSorter<OsmElement, Node> nodeSorter = new DistanceSorter<>();
    /** Sorter instance for sorting ways by distance */
    private static final DistanceSorter<Way, Way>         waySorter  = new DistanceSorter<>();

    /**
     * maximum number of nodes in a way for it still to be moveable, arbitrary number for now
     */
    private static final int MAX_NODES_FOR_MOVE = 100;

    /**
     * maximum depth that we recursively select relations
     */
    private static final int MAX_RELATION_SELECTION_DEPTH = 5;

    /**
     * 24 hours in ms
     */
    private static final long ONE_DAY_MS = 24 * 3600 * 1000L;

    /**
     * Stores the {@link Preferences} as soon as they are available.
     */
    private Preferences prefs;

    /**
     * Stack of selected elements
     */
    private final Deque<Selection> selectionStack;

    /*
     * The following are lists because elements could be add multiple times adding them once per selected relation and
     * the same for deletion avoids having to maintain a count
     */
    /**
     * ways belonging to a selected relation
     */
    private List<Way> selectedRelationWays = null;

    /**
     * nodes belonging to a selected relation
     */
    private List<Node> selectedRelationNodes = null;

    /**
     * relations belonging to a selected relation
     */
    private List<Relation> selectedRelationRelations = null;

    private static final int MRULIST_SIZE = 10;

    /**
     * last changeset comment
     */
    private MRUList<String> lastComments = new MRUList<>(MRULIST_SIZE);

    private String draftComment = null;

    /**
     * last changeset source
     */
    private MRUList<String> lastSources = new MRUList<>(MRULIST_SIZE);

    private String draftSourceComment = null;

    /**
     * last object search string
     */
    private MRUList<String> lastObjectSearches = new MRUList<>(MRULIST_SIZE);

    /**
     * Are we currently dragging a node? Set by {@link #handleTouchEventDown(float, float)}
     */
    private boolean draggingNode = false;

    /**
     * Are we currently dragging a way? Set by {@link #handleTouchEventDown(float, float)}
     */
    private boolean draggingWay = false;

    /**
     * Are we currently dragging a Note? Set by {@link #handleTouchEventDown(float, float)}
     */
    private boolean draggingNote = false;

    private int   startLat;
    private int   startLon;
    private float startY;
    private float startX;
    private float centroidY;
    private float centroidX;

    /**
     * Are we currently dragging a handle?
     */
    private boolean draggingHandle = false;
    private Node    handleNode     = null;

    /**
     * 
     */
    private boolean rotating = false;

    /**
     * Current mode.
     */
    private Mode mode;

    /**
     * Screen locked or not
     */
    private boolean locked;

    /**
     * The viewBox for the map. All changes on this Object are made in here or in {@link Tracker}.
     */
    private final ViewBox viewBox;

    /**
     * An instance of the map. Value set by Main via constructor.
     */
    private Map map;

    private Set<OsmElement> clickableElements;

    /**
     * add relations to result of clicks/touches
     */
    private boolean returnRelations = true;

    /**
     * The currently selected handle to be dragged to create a new node in a way.
     */
    private Handle selectedHandle = null;

    /**
     * This is set if we are dragging a way node
     */
    private Node draggedNode = null;

    /**
     * Filter to apply if any
     */
    private Filter filter = null;

    /**
     * Should we show a warning if hidden/filtered objects are manipulated not persisted in edit state for now
     */
    private boolean attachedObjectWarning = true;

    private ExecutorService executorService;
    private Handler         uiHandler;

    private boolean editingStateRead = false; // set to true after we have read the editing state

    /**
     * Initiate all needed values. Starts Tracker and delegate the first values for the map.
     * 
     */
    Logic() {
        viewBox = new ViewBox(getDelegator().getLastBox());
        mode = Mode.MODE_EASYEDIT;
        setLocked(true);
        executorService = Executors.newFixedThreadPool(EXECUTOR_THREADS);
        uiHandler = new Handler(Looper.getMainLooper());
        selectionStack = new ArrayDeque<>();
        selectionStack.add(new Selection());
    }

    /**
     * Set all {@link Preferences} and delegates them to {@link Tracker} and {@link Map}. The AntiAlias-Flag will be set
     * to {@link Paints}. Map gets repainted.
     * 
     * @param prefs the new Preferences.
     */
    public void setPrefs(@NonNull final Preferences prefs) {
        this.prefs = prefs;
        if (map != null) {
            DataStyle styles = map.getDataStyle();
            final String dataStyleName = prefs.getDataStyle(styles);
            if (!styles.getCurrent().getName().equals(dataStyleName)) {
                styles.switchTo(dataStyleName);
                updateStyle();
            }
        }
    }

    /**
     * Informs the current drawing style of the user preferences affecting drawing, the current screen properties, and
     * clears the way cache.
     */
    public void updateStyle() {

        // zap the cached style for all ways
        for (Way w : getWays()) {
            w.setStyle(null);
        }
        for (Relation r : getRelations()) {
            r.setStyle(null);
        }
        if (map != null) {
            DataStyle styles = map.getDataStyle();
            styles.updateStrokes(strokeWidth(viewBox.getWidth()));
            styles.setAntiAliasing(prefs.isAntiAliasingEnabled());
            map.updateStyle();
            MapOverlay<OsmElement> dataLayer = map.getDataLayer();
            if (dataLayer != null) {
                dataLayer.clearCaches();
            }
        }
    }

    /**
     * @return locked status
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked set locked status
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * Sets new mode. If the new mode is different from the current one, all selected Elements will be nulled, the Map
     * gets repainted, and the action bar will be reset.
     * 
     * @param main instance of main that is calling this
     * @param mode mode to set
     */
    public void setMode(@NonNull final Main main, @NonNull final Mode mode) {
        Log.d(DEBUG_TAG, "current mode " + this.mode + " new mode " + mode);
        if (this.mode == mode) {
            return;
        }
        Mode oldMode = this.mode;
        this.mode = mode;
        main.updateActionbarEditMode();
        main.getMap().deselectObjects();
        deselectAll();
        oldMode.teardown(main, this);
        mode.setup(main, this);
        invalidateMap();
    }

    /**
     * Returns the current mode that the program is in.
     * 
     * @return the mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Checks for changes in the API-Storage.
     * 
     * @return {@link StorageDelegator#hasChanges()}
     */
    public boolean hasChanges() {
        return getDelegator().hasChanges();
    }

    /**
     * Get the current undo instance. For immediate use only - DO NOT CACHE THIS.
     * 
     * @return the UndoStorage, allowing operations like creation of checkpoints and undo/redo.
     */
    public UndoStorage getUndo() {
        return getDelegator().getUndo();
    }

    /**
     * Undo the last checkpoint
     * 
     * @return checkpoint name or null if none available
     */
    @Nullable
    public String undo() {
        return undo(getDelegator(), getDelegator().getUndo().undo());
    }

    /**
     * Undo a specific checkpoint
     * 
     * @param checkpoint index of the checkpoint to undo
     * @return checkpoint name or null if none available
     */
    @Nullable
    public String undo(int checkpoint) {
        return undo(getDelegator(), getDelegator().getUndo().undo(checkpoint));
    }

    /**
     * Undo a checkpoint
     * 
     * @param delegator the current StorageDelegator instance
     * @param toUndo the Checkpoint to undo
     * @return checkpoint name or null if none available
     */
    private String undo(@NonNull final StorageDelegator delegator, @Nullable Checkpoint toUndo) {
        Selection.Ids ids = toUndo != null ? toUndo.getSelection() : null;
        if (ids != null && map != null && map.getContext() instanceof Main) {
            Main main = (Main) map.getContext();
            final EasyEditManager easyEditManager = main.getEasyEditManager();
            easyEditManager.finish();
            final Selection currentSelection = selectionStack.getFirst();
            currentSelection.reset();
            currentSelection.fromIds(main, delegator, ids);
            selectFromTop();
            easyEditManager.editElements();
        }
        checkClipboard(delegator);
        delegator.dirty();
        return toUndo != null ? toUndo.getName() : null;
    }

    /**
     * Check the clipboard for consistency post undo
     */
    private void checkClipboard(@NonNull StorageDelegator delegator) {
        if (!delegator.clipboardIsEmpty()) {
            delegator.checkClipboard();
        }
    }

    /**
     * Wrapper to ensure the dirty flag is set
     * 
     * @return checkpoint name or null if none available
     */
    @Nullable
    public String redo() {
        String name = getDelegator().getUndo().redo();
        getDelegator().dirty();
        return name;
    }

    /**
     * Wrapper to ensure the dirty flag is set
     * 
     * @param checkpoint index of the checkpoint to redo
     * @return checkpoint name or null if none available
     */
    @Nullable
    public String redo(int checkpoint) {
        String name = getDelegator().getUndo().redo(checkpoint);
        getDelegator().dirty();
        return name;
    }

    /**
     * Wrapper to ensure the dirty flag is set
     * 
     * Undo without creating a redo checkpoint
     */
    public void rollback() {
        getDelegator().getUndo().undo(false);
        getDelegator().dirty();
    }

    /**
     * Checks if the viewBox is close enough to the viewBox to be in the ability to edit something.
     * 
     * @return true, if viewBox' width is smaller than {@link #TOLERANCE_MIN_VIEWBOX_WIDTH}.
     */
    public boolean isInEditZoomRange() {
        return (viewBox.getWidth() < TOLERANCE_MIN_VIEWBOX_WIDTH) && (viewBox.getHeight() < TOLERANCE_MIN_VIEWBOX_WIDTH);
    }

    /**
     * Translates the viewBox into the given direction by {@link #TRANSLATION_FACTOR} and sets GPS-Following to false.
     * Map will be repainted.
     * 
     * @param direction the direction of the translation.
     */
    public void translate(@NonNull final CursorPaddirection direction) {
        float translation = viewBox.getWidth() * TRANSLATION_FACTOR;
        try {
            switch (direction) {
            case DIRECTION_LEFT:
                viewBox.translate(map, (int) -translation, 0);
                break;
            case DIRECTION_DOWN:
                viewBox.translate(map, 0, -(GeoMath.latE7ToMercatorE7(viewBox.getTop()) - (long) (viewBox.getBottomMercator() * 1E7D)));
                break;
            case DIRECTION_RIGHT:
                viewBox.translate(map, (int) translation, 0);
                break;
            case DIRECTION_UP:
                viewBox.translate(map, 0, GeoMath.latE7ToMercatorE7(viewBox.getTop()) - (long) (viewBox.getBottomMercator() * 1E7D));
                break;
            }
        } catch (OsmException e) {
            Log.d(DEBUG_TAG, "translate got " + e.getMessage());
        }

        invalidateMap();
    }

    /**
     * Test if the requested zoom operation can be performed.
     * 
     * @param zoomIn The zoom operation: ZOOM_IN or ZOOM_OUT.
     * @return true if the zoom operation can be performed, false if it can't.
     */
    public boolean canZoom(final boolean zoomIn) {
        return zoomIn ? viewBox.canZoomIn() : viewBox.canZoomOut();
    }

    /**
     * Zooms in or out. Checks if the new viewBox is close enough for editing and sends this value to map. Strokes will
     * be updated and map will be repainted.
     * 
     * @param zoomIn true for zooming in.
     */
    public void zoom(final boolean zoomIn) {
        if (zoomIn) {
            viewBox.zoomIn();
        } else {
            viewBox.zoomOut();
        }
        onZoomChanged(map);
        map.postInvalidate();
    }

    /**
     * Set the zoom to a specific tile zoom level.
     * 
     * @param map the current Map object
     * @param z The TMS zoom level to zoom to (from 0 for the whole world to about 19 for small areas).
     */
    public void setZoom(Map map, int z) {
        viewBox.setZoom(map, z);
        onZoomChanged(map);
    }

    /**
     * Call this if zoom has changed
     * 
     * @param map the current Map object
     */
    private void onZoomChanged(@NonNull Map map) {
        map.getDataStyle().updateStrokes(strokeWidth(viewBox.getWidth()));
        if (rotating) {
            showCrosshairsForCentroid();
        } else if (mode == Mode.MODE_ALIGN_BACKGROUND) {
            performBackgroundOffset((Main) map.getContext(), map.getZoomLevel(), 0, 0);
        }
    }

    /**
     * Return a stroke width value that increases with zoom and is capped at a configurable value
     * 
     * @param width screenwidth in 10e7 deg.
     * @return stroke width
     */
    public float strokeWidth(long width) {
        // prefs may not have been initialized
        if (prefs != null) {
            return Math.min(prefs.getMaxStrokeWidth(), STROKE_FACTOR / width);
        }
        return STROKE_FACTOR / width;
    }

    /**
     * Create an undo checkpoint using a resource string as the name, records imagery used at the same time
     * 
     * @param activity that we were called from for access to the resources, if null we will use the resources from App
     * @param stringId the resource id of the string representing the checkpoint name
     */
    public void createCheckpoint(@Nullable Activity activity, int stringId) {
        Resources r = activity != null ? activity.getResources() : App.resources();
        final UndoStorage undo = getDelegator().getUndo();
        boolean firstCheckpoint = !undo.canUndo();
        undo.createCheckpoint(r.getString(stringId), getSelectedIds());
        getDelegator().recordImagery(map);
        if (firstCheckpoint && activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).invalidateOptionsMenu();
        }
    }

    /**
     * Remove an empty undo checkpoint using a resource string as the name
     * 
     * @param activity that we were called from for access to the resources, if null we will use the resources from App
     * @param stringId the resource id of the string representing the checkpoint name
     */
    public void removeCheckpoint(@Nullable Activity activity, int stringId) {
        removeCheckpoint(activity, stringId, false);
    }

    /**
     * Remove an empty undo checkpoint using a resource string as the name
     * 
     * @param activity that we were called from for access to the resources, if null we will use the resources from App
     * @param stringId the resource id of the string representing the checkpoint name
     * @param force if true remove even if not empty
     */
    public void removeCheckpoint(@Nullable Activity activity, int stringId, boolean force) {
        Resources r = activity != null ? activity.getResources() : App.resources();
        getDelegator().getUndo().removeCheckpoint(r.getString(stringId), force);
    }

    /**
     * Delegates the setting of the Tag-list to {@link StorageDelegator}. All existing tags will be replaced.
     * 
     * @param activity activity we were called from
     * @param e element to change the tags on
     * @param tags Tags to be set
     * @throws OsmIllegalOperationException if the e isn't in storage
     */
    public void setTags(@Nullable Activity activity, @NonNull final OsmElement e, @Nullable final java.util.Map<String, String> tags)
            throws OsmIllegalOperationException {
        setTags(activity, e, tags, true);
    }

    /**
     * Delegates the setting of the Tag-list to {@link StorageDelegator}. All existing tags will be replaced.
     * 
     * @param activity activity we were called from
     * @param type type of the element
     * @param osmId OSM-ID of the element
     * @param tags Tags to be set
     * @throws OsmIllegalOperationException if the e isn't in storage
     */
    public void setTags(@Nullable Activity activity, final String type, final long osmId, @Nullable final java.util.Map<String, String> tags)
            throws OsmIllegalOperationException {
        setTags(activity, type, osmId, tags, true);
    }

    /**
     * Delegates the setting of the Tag-list to {@link StorageDelegator}. All existing tags will be replaced.
     * 
     * @param activity activity we were called from
     * @param type type of the element
     * @param osmId OSM-ID of the element
     * @param tags Tags to be set
     * @param createCheckpoint create a checkpoint, except in composite operations this should always be true
     * @throws OsmIllegalOperationException if the e isn't in storage
     */
    public void setTags(@Nullable Activity activity, final String type, final long osmId, @Nullable final java.util.Map<String, String> tags,
            boolean createCheckpoint) throws OsmIllegalOperationException {
        setTags(activity, getDelegator().getOsmElement(type, osmId), tags, createCheckpoint);
    }

    /**
     * Delegates the setting of the Tag-list to {@link StorageDelegator}. All existing tags will be replaced.
     * 
     * @param activity activity we were called from
     * @param type type of the element
     * @param osmId OSM-ID of the element
     * @param tags Tags to be set
     * @param createCheckpoint create a checkpoint, except in composite operations this should always be true
     * @throws OsmIllegalOperationException if the e isn't in storage
     */
    public synchronized void setTags(@Nullable Activity activity, @Nullable OsmElement osmElement, @Nullable final java.util.Map<String, String> tags,
            boolean createCheckpoint) throws OsmIllegalOperationException {
        if (osmElement == null) {
            Log.e(DEBUG_TAG, "Attempted to setTags on a non-existing element");
            throw new OsmIllegalOperationException("Element not in storage");
        } else {
            if (createCheckpoint) {
                createCheckpoint(activity, R.string.undo_action_set_tags);
            }
            getDelegator().setTags(osmElement, tags);
        }
    }

    /**
     * Update parent relations
     * 
     * @param activity activity we were called from
     * @param type type of the element for the Tag-list.
     * @param osmId OSM-ID of the element.
     * @param parents new parent relations
     * @return false if no element exists for the given osmId/type.
     */
    public synchronized boolean updateParentRelations(@Nullable FragmentActivity activity, final String type, final long osmId,
            final MultiHashMap<Long, RelationMemberPosition> parents) {
        OsmElement osmElement = getDelegator().getOsmElement(type, osmId);
        if (osmElement == null) {
            Log.e(DEBUG_TAG, "Attempted to update relations on a non-existing element");
            return false;
        } else {
            List<Relation> originalParents = osmElement.hasParentRelations() ? new ArrayList<>(osmElement.getParentRelations()) : null;
            createCheckpoint(activity, R.string.undo_action_update_relations);
            try {
                getDelegator().updateParentRelations(osmElement, parents);
                if (activity != null) {
                    ElementSelectionActionModeCallback.checkEmptyRelations(activity, originalParents);
                }
                return true;
            } catch (OsmIllegalOperationException | StorageException ex) {
                handleDelegatorException(activity, ex);
                throw ex; // rethrow
            }
        }
    }

    /**
     * Updates the list of members in the selected relation. Actual work is delegated out to {@link StorageDelegator}.
     * 
     * @param activity activity we were called from
     * @param osmId The OSM ID of the relation to change.
     * @param members The new list of members to set for the given relation.
     * @return true if the members was updated
     */
    public synchronized boolean updateRelation(@Nullable FragmentActivity activity, long osmId, List<RelationMemberDescription> members) {
        OsmElement osmElement = getDelegator().getOsmElement(Relation.NAME, osmId);
        if (osmElement == null) {
            Log.e(DEBUG_TAG, "Attempted to update non-existing relation #" + osmId);
            return false;
        } else {
            try {
                createCheckpoint(activity, R.string.undo_action_update_relations);
                getDelegator().updateRelation((Relation) osmElement, members);
                return true;
            } catch (OsmIllegalOperationException | StorageException ex) {
                handleDelegatorException(activity, ex);
                throw ex; // rethrow
            }
        }
    }

    /**
     * Searches for all Ways and Nodes at x,y plus the shown node-tolerance. Nodes have to lie in the mapBox.
     * 
     * @param x display-coordinate.
     * @param y display-coordinate.
     * @return a List of all OsmElements (Nodes and Ways) within the tolerance
     */
    @NonNull
    public List<OsmElement> getClickedNodesAndWays(final float x, final float y) {
        List<OsmElement> result = new ArrayList<>();
        result.addAll(getClickedNodes(x, y));
        result.addAll(getClickedWays(x, y));
        if (returnRelations) {
            // add any relations that the elements are members of
            result.addAll(getParentRelations(result));
        }
        if (clickableElements != null) {
            for (OsmElement e : new ArrayList<OsmElement>(result)) {
                if (!clickableElements.contains(e)) {
                    result.remove(e);
                }
            }
        }
        return result;
    }

    /**
     * Return all the Relations the OsmElements are a member of and parent relations
     * 
     * @param elements the OsmELements to check
     * @return a List of OsmElement
     */
    @NonNull
    private List<Relation> getParentRelations(@NonNull List<OsmElement> elements) {
        List<Relation> relations = new ArrayList<>();
        for (OsmElement e : elements) {
            getParentRelations(e, relations);
        }
        return relations;
    }

    /**
     * Recursively add parent relations, every relation will only be added once
     * 
     * @param e the OsmElement to get the parent Relations of
     * @param relations the List of Relations
     */
    private void getParentRelations(@NonNull OsmElement e, @NonNull List<Relation> relations) {
        if (e.getParentRelations() != null) {
            for (Relation r : e.getParentRelations()) {
                if (!relations.contains(r)) { // not very efficient, could use a set
                    relations.add(r);
                    getParentRelations(r, relations);
                }
            }
        }
    }

    /**
     * Returns all ways within way tolerance from the given coordinates, and their distances from them.
     * 
     * @param includeClosed include closed ways in the result if true
     * @param x x display coordinate
     * @param y y display coordinate
     * @return a hash map mapping Ways to distances
     */
    @NonNull
    private java.util.Map<Way, Double> getClickedWaysWithDistances(boolean includeClosed, final float x, final float y) {
        java.util.Map<Way, Double> result = new HashMap<>();
        boolean showWayIcons = prefs.getShowWayIcons();
        final DataStyle currentStyle = map.getDataStyle().getCurrent();
        final float nodeToleranceValue = currentStyle.getNodeToleranceValue();
        final float wayToleranceValue = wayToleranceForTouch(currentStyle);
        List<Way> ways = getClickableWays();
        for (Way way : ways) {
            List<Node> wayNodes = way.getNodes();
            int wayNodesSize = wayNodes.size();
            if ((way.isClosed() && !includeClosed) || wayNodesSize == 0) {
                continue;
            }
            boolean added = false;

            double A = 0;
            double Y = 0;
            double X = 0;
            float node1X = -Float.MAX_VALUE;
            float node1Y = -Float.MAX_VALUE;
            boolean firstNode = true;
            // Iterate over all WayNodes, but not the last one.
            Node node1 = wayNodes.get(0);
            for (int k = 0; k < wayNodesSize - 1; ++k) {
                Node node2 = wayNodes.get(k + 1);
                if (firstNode) {
                    node1X = lonE7ToX(node1.getLon());
                    node1Y = latE7ToY(node1.getLat());
                    firstNode = false;
                }
                float node2X = lonE7ToX(node2.getLon());
                float node2Y = latE7ToY(node2.getLat());

                double distance = Geometry.isPositionOnLine(wayToleranceValue, x, y, node1X, node1Y, node2X, node2Y);
                if (distance >= 0) {
                    result.put(way, distance);
                    added = true;
                    break;
                }
                // calculations for centroid
                double d = node1X * node2Y - node2X * node1Y;
                A = A + d;
                X = X + (node1X + node2X) * d;
                Y = Y + (node1Y + node2Y) * d;
                node1 = node2;
                node1X = node2X;
                node1Y = node2Y;
            }
            if (Util.notZero(A) && showWayIcons && !added && areaHasIcon(way)) {
                Y = Y / (3 * A); // NOSONAR nonZero tests for zero
                X = X / (3 * A); // NOSONAR nonZero tests for zero
                double distance = Math.hypot(x - X, y - Y);
                if (distance < nodeToleranceValue) {
                    result.put(way, distance);
                }
            }
        }
        return result;
    }

    /**
     * Get the tolerance we use for determining if a way is in tolerance
     * 
     * This is half the width of the whole area
     * 
     * @return the tolerance
     */
    private float wayToleranceForTouch(@NonNull DataStyle style) {
        return style.getWayToleranceValue() / 2;
    }

    /**
     * Get a List of Ways that could be clicked
     * 
     * @return a List of Ways
     */
    @NonNull
    List<Way> getClickableWays() {
        return filter != null ? filter.getVisibleWays() : getWays(map.getViewBox());
    }

    /**
     * Determine if the way should have an icon shown and should respond to a touch event on the icon
     * 
     * @param way the way in question
     * @return true if we should have an icon
     */
    private boolean areaHasIcon(@NonNull Way way) {
        final FeatureStyle style = map.getDataStyle().matchStyle(way);
        return style.getIconPath() != null || style.getLabelKey() != null;
    }

    /**
     * The small mid segment 'x' handles that allow dragging to easily add a new node to a way.
     */
    class Handle {
        float x;
        float y;

        /**
         * Create a new Handle
         * 
         * @param x screen x
         * @param y screen y
         */
        Handle(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Returns a Handle object for the nearest selected way with a mid-way segment handle tolerance from the given
     * coordinates
     * 
     * @param x x display coordinate
     * @param y y display coordinate
     * @return a Handle object or null
     */
    @Nullable
    private synchronized Handle getClickedWayHandleWithDistances(final float x, final float y) {

        Handle result = null;
        double bestDistance = Double.MAX_VALUE;
        final DataStyle currentStyle = map.getDataStyle().getCurrent();
        float wayToleranceValue = wayToleranceForTouch(currentStyle);
        float minLenForHandle = currentStyle.getMinLenForHandle();

        List<Way> ways = getSelectedWays();
        if (ways == null) {
            return null;
        }
        for (Way way : ways) {
            List<Node> wayNodes = way.getNodes();

            float node1X = -Float.MAX_VALUE;
            float node1Y = -Float.MAX_VALUE;
            boolean firstNode = true;
            // Iterate over all WayNodes, but not the last one.
            int wayNodesSize = wayNodes.size();
            Node node1 = wayNodes.get(0);
            for (int k = 0; k < wayNodesSize - 1; ++k) {
                Node node2 = wayNodes.get(k + 1);
                if (firstNode) {
                    node1X = lonE7ToX(node1.getLon());
                    node1Y = latE7ToY(node1.getLat());
                    firstNode = false;
                }
                float node2X = lonE7ToX(node2.getLon());
                float node2Y = latE7ToY(node2.getLat());
                float xDelta = node2X - node1X;
                float yDelta = node2Y - node1Y;

                float handleX = node1X + xDelta / 2;
                float handleY = node1Y + yDelta / 2;

                float differenceX = Math.abs(handleX - x);
                float differenceY = Math.abs(handleY - y);

                node1 = node2;
                node1X = node2X;
                node1Y = node2Y;

                if (((differenceX > wayToleranceValue) && (differenceY > wayToleranceValue)) || Math.hypot(xDelta, yDelta) <= minLenForHandle) {
                    continue;
                }

                double dist = Math.hypot(differenceX, differenceY);
                if ((dist <= wayToleranceValue) && (dist < bestDistance)) {
                    bestDistance = dist;
                    result = new Handle(handleX, handleY);
                }
            }
        }
        return result;
    }

    /**
     * Calculates the on-screen distance between a node and the screen coordinate of a click. Returns null if the node
     * was outside the click tolerance.
     * 
     * @param node the node
     * @param x the x coordinate of the clicked point
     * @param y the y coordinate of the clicked point
     * @return The distance between the clicked point and the node in px if the node was within the tolerance value,
     *         null otherwise
     */
    @Nullable
    private Double clickDistance(@NonNull Node node, final float x, final float y) {
        final DataStyle current = map.getDataStyle().getCurrent();
        return clickDistance(node, x, y, node.isTagged() ? current.getNodeToleranceValue() : wayToleranceForTouch(current));
    }

    /**
     * 
     * 
     * @param point and Object that implements the GeoPoint interface
     * @param x screen x
     * @param y screen y
     * @param tolerance tolerance to apply
     * @return the distance as a double of null if not inside the tolerance
     */
    @Nullable
    private Double clickDistance(@NonNull GeoPoint point, final float x, final float y, float tolerance) {

        float differenceX = Math.abs(lonE7ToX(point.getLon()) - x);
        float differenceY = Math.abs(latE7ToY(point.getLat()) - y);

        if ((differenceX > tolerance) && (differenceY > tolerance)) {
            return null;
        }

        double dist = Math.hypot(differenceX, differenceY);
        return (dist > tolerance) ? null : dist;
    }

    /**
     * Returns all nodes within node tolerance from the given coordinates, and their distances from them.
     * 
     * @param x x display coordinate
     * @param y y display coordinate
     * @param inDownloadOnly if true the node has to be new or in one of the downloaded bounding boxes
     * @return a hash map mapping Nodes to distances
     */
    @NonNull
    private java.util.Map<Node, Double> getClickedNodesWithDistances(final float x, final float y, boolean inDownloadOnly) {
        java.util.Map<Node, Double> result = new HashMap<>();
        for (Node node : getClickableNodes()) {
            int lat = node.getLat();
            int lon = node.getLon();
            if (!inDownloadOnly || node.getState() != OsmElement.STATE_UNCHANGED || getDelegator().isInDownload(lon, lat)) {
                Double dist = clickDistance(node, x, y);
                if (dist != null) {
                    result.put(node, dist);
                }
            }
        }
        return result;
    }

    /**
     * Get all nodes that could be clicked
     * 
     * @return a List of Nodes
     */
    @NonNull
    List<Node> getClickableNodes() {
        List<Node> nodes;
        if (filter != null) {
            nodes = filter.getVisibleNodes();
            if (getSelectedNodes() != null) { // selected Nodes are always visible if a filter is
                // applied
                nodes.addAll(getSelectedNodes());
            }
        } else {
            nodes = getDelegator().getCurrentStorage().getNodes(map.getViewBox());
        }
        return nodes;
    }

    /**
     * Searches for a Node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox.
     * 
     * @param x display-coordinate.
     * @param y display-coordinate.
     * @return all nodes within tolerance found in the currentStorage node-list, ordered ascending by distance.
     */
    @NonNull
    public List<OsmElement> getClickedNodes(final float x, final float y) {
        return nodeSorter.sort(getClickedNodesWithDistances(x, y, true));
    }

    /**
     * Searches for a way end node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox.
     * 
     * @param x display-coordinate.
     * @param y display-coordinate.
     * @return all end nodes within tolerance found in the currentStorage node-list, ordered ascending by distance.
     */
    @NonNull
    public List<OsmElement> getClickedEndNodes(final float x, final float y) {
        List<OsmElement> result = new ArrayList<>();
        List<OsmElement> allNodes = getClickedNodes(x, y);
        for (OsmElement osmElement : allNodes) {
            if (getDelegator().getCurrentStorage().isEndNode((Node) osmElement)) {
                result.add(osmElement);
            }
        }
        return result;
    }

    /**
     * Searches for a Node at x,y plus the shown node-tolerance. The Node has to lay in the mapBox.
     * 
     * @param x display-coordinate.
     * @param y display-coordinate.
     * @return the nearest node found in the current-Storage node-list. null, when no node was found.
     */
    @Nullable
    public Node getClickedNode(final float x, final float y) {
        Node bestNode = null;
        Double bestDistance = Double.MAX_VALUE;
        java.util.Map<Node, Double> candidates = getClickedNodesWithDistances(x, y, false);
        for (Entry<Node, Double> candidate : candidates.entrySet()) {
            if (candidate.getValue() < bestDistance) {
                bestNode = candidate.getKey();
                bestDistance = candidate.getValue();
            }
        }
        return bestNode;
    }

    /**
     * Returns all ways within click tolerance from the given coordinate
     * 
     * @param x x display-coordinate.
     * @param y y display-coordinate.
     * @return the ways
     */
    @NonNull
    private List<Way> getClickedWays(final float x, final float y) {
        return getClickedWays(true, x, y);
    }

    /**
     * Returns all ways within click tolerance from the given coordinate
     * 
     * @param includeClosed include closed ways in the result if true
     * @param x x display-coordinate.
     * @param y y display-coordinate.
     * @return the ways
     */
    @NonNull
    public List<Way> getClickedWays(boolean includeClosed, final float x, final float y) {
        return waySorter.sort(getClickedWaysWithDistances(includeClosed, x, y));
    }

    /**
     * Returns the closest way (within tolerance) to the given coordinates
     * 
     * @param x the x display-coordinate.
     * @param y the y display-coordinate.
     * @return the closest way, or null if no way is found within the tolerance
     */
    @Nullable
    private Way getClickedWay(final float x, final float y) {
        Way bestWay = null;
        Double bestDistance = Double.MAX_VALUE;
        java.util.Map<Way, Double> candidates = getClickedWaysWithDistances(true, x, y);
        for (Entry<Way, Double> candidate : candidates.entrySet()) {
            if (candidate.getValue() < bestDistance) {
                bestWay = candidate.getKey();
                bestDistance = candidate.getValue();
            }
        }
        return bestWay;
    }

    /**
     * Returns a Set of all the clickable OSM elements in storage. Before returning the list is "pruned" to remove any
     * elements on the exclude list.
     * 
     * @param viewBox the BoundingBox currently displayed
     * @param excludes The list of OSM elements to exclude from the results.
     * @return a Set of the clickable OsmElements
     */
    @NonNull
    public Set<OsmElement> findClickableElements(@NonNull BoundingBox viewBox, @NonNull List<OsmElement> excludes) {
        Set<OsmElement> result = new HashSet<>();
        final Storage currentStorage = getDelegator().getCurrentStorage();
        result.addAll(currentStorage.getNodes(viewBox));
        result.addAll(currentStorage.getWays(viewBox));
        if (returnRelations) {
            result.addAll(currentStorage.getRelations());
        }
        for (OsmElement e : excludes) {
            result.remove(e);
        }
        return result;
    }

    /**
     * Get a list of all the Ways connected to the given Node.
     * 
     * @param node The Node.
     * @return A list of all Ways connected to the Node.
     */
    @NonNull
    public List<Way> getWaysForNode(@NonNull final Node node) {
        return getDelegator().getCurrentStorage().getWays(node);
    }

    /**
     * Get a list of all the filtered Ways connected to the given Node.
     * 
     * @param node The Node.
     * @return A list of all Ways connected to the Node.
     */
    @NonNull
    public List<Way> getFilteredWaysForNode(@NonNull final Node node) {
        List<Way> ways = new ArrayList<>();
        for (Way w : getDelegator().getCurrentStorage().getWays(node)) {
            if (getFilter() == null || filter.include(w, false)) {
                ways.add(w);
            }
        }
        return ways;
    }

    /**
     * Test if the given Node is an end node of a Way. Isolated nodes not part of a way are not considered an end node.
     * 
     * @param node Node to test.
     * @return true if the Node is an end node of a Way, false otherwise.
     */
    public boolean isEndNode(@Nullable final Node node) {
        return getDelegator().getCurrentStorage().isEndNode(node);
    }

    /**
     * Check all nodes in way to see if they are in the downloaded data.
     * 
     * @param way the way whose nodes should be checked
     * @return true if the above is the case
     */
    public boolean isInDownload(@NonNull Way way) {
        for (Node n : way.getNodes()) {
            if (!getDelegator().isInDownload(n.getLon(), n.getLat())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles the event when user begins to touch the display. When the viewBox is close enough for editing and the
     * user is in edit-mode a touched node will bet set to selected. draggingNode will be set if a node is to be moved.
     * A eventual movement of this node will be done in
     * {@link #handleTouchEventMove(float, float, float, float, boolean)}.
     * 
     * @param activity the calling Activity
     * @param x display-coord.
     * @param y display-coord.
     */
    synchronized void handleTouchEventDown(@NonNull Activity activity, final float x, final float y) {
        boolean draggingMultiselect = false;
        draggingNode = false;
        draggingWay = false;
        draggingHandle = false;
        draggingNote = false;
        draggedNode = null;
        if (!isLocked() && isInEditZoomRange() && mode.elementsGeomEditable()) {
            if (activity instanceof Main && !((Main) activity).getEasyEditManager().draggingEnabled()) {
                // dragging is currently only supported in element selection modes
                return;
            }
            Task selectedTask = null;
            de.blau.android.layer.tasks.MapOverlay taskLayer = map.getTaskLayer();
            if (taskLayer != null) {
                selectedTask = taskLayer.getSelected();
            }
            final boolean largeDragArea = prefs.largeDragArea();
            final Selection currentSelection = selectionStack.getFirst();
            final int selectedWayCount = currentSelection.wayCount();
            final int selectedNodeCount = currentSelection.nodeCount();
            if (rotating) {
                startX = x;
                startY = y;
            } else if ((selectedNodeCount == 1 || selectedTask != null) && selectedWayCount == 0) { // single node or
                                                                                                    // task dragging
                DataStyle currentStyle = map.getDataStyle().getCurrent();
                float tolerance = largeDragArea ? currentStyle.getLargDragToleranceRadius() : currentStyle.getNodeToleranceValue();
                GeoPoint point = selectedTask != null ? selectedTask : currentSelection.getNode();
                if (clickDistance(point, x, y, tolerance) != null) {
                    draggingNode = selectedTask == null;
                    draggingNote = selectedTask != null;
                    if (largeDragArea) {
                        startX = lonE7ToX(point.getLon());
                        startY = latE7ToY(point.getLat());
                    }
                }
            } else {
                Handle handle = getClickedWayHandleWithDistances(x, y);
                if (handle != null) {
                    Log.d(DEBUG_TAG, "start handle drag");
                    selectedHandle = handle;
                    draggingHandle = true;
                } else if (selectedWayCount == 1 && selectedNodeCount == 0) {
                    // single way, way handle or way node dragging or way rotation
                    List<Way> clickedWays = getClickedWays(true, x, y);
                    if (!clickedWays.isEmpty()) {
                        List<OsmElement> clickedNodes = getClickedNodes(x, y);
                        final Way selectedWay = currentSelection.getWay();
                        draggedNode = getCommonNode(selectedWay, clickedNodes);
                        if (prefs.isWayNodeDraggingEnabled() && draggedNode != null) {
                            draggingNode = true;
                            if (largeDragArea) {
                                startX = lonE7ToX(draggedNode.getLon());
                                startY = latE7ToY(draggedNode.getLat());
                            }
                        } else if (clickedWays.contains(selectedWay)) {
                            startLat = yToLatE7(y);
                            startLon = xToLonE7(x);
                            draggingWay = true;
                        }
                    }
                } else {
                    // check for multi-select
                    if ((selectedWayCount > 1 || selectedNodeCount > 1) || (selectedWayCount > 0 && selectedNodeCount > 0)) {
                        Log.d(DEBUG_TAG, "Multi select detected");
                        boolean foundSelected = false;
                        if (selectedWayCount > 0) {
                            List<Way> clickedWays = getClickedWays(x, y);
                            for (Way w : clickedWays) {
                                if (currentSelection.contains(w)) {
                                    foundSelected = true;
                                    break;
                                }
                            }
                        }
                        if (!foundSelected && selectedNodeCount > 0) {
                            List<OsmElement> clickedNodes = getClickedNodes(x, y);
                            for (OsmElement n : clickedNodes) {
                                if (currentSelection.contains(n)) {
                                    foundSelected = true;
                                    break;
                                }
                            }
                        }
                        if (foundSelected) {
                            startLat = yToLatE7(y);
                            startLon = xToLonE7(x);
                            startX = x;
                            startY = y;
                            draggingMultiselect = true;
                            draggingWay = true;
                        }
                    }
                }
            }
        }
        Log.d(DEBUG_TAG, "handleTouchEventDown creating checkpoints");
        if ((draggingNode || draggingWay)) {
            if (draggingMultiselect) {
                createCheckpoint(activity, R.string.undo_action_moveobjects);
            } else {
                createCheckpoint(activity, draggingNode ? R.string.undo_action_movenode : R.string.undo_action_moveway);
            }
        }
    }

    /**
     * Get a common node
     * 
     * @param way the Way
     * @param nodes the list of Nodes (for historic reasons OsmElements)
     * @return the first common node or null
     */
    @Nullable
    private Node getCommonNode(@NonNull Way way, @NonNull List<OsmElement> nodes) {
        for (OsmElement e : nodes) {
            if (way.hasNode((Node) e)) {
                return (Node) e;
            }
        }
        return null;
    }

    /**
     * Handle the end of a touch event (aka lifting the finger from the screen)
     * 
     * @param x screen x
     * @param y screen y
     */
    synchronized void handleTouchEventUp(final float x, final float y) {
        handleNode = null;
        draggingHandle = false;
    }

    /**
     * Calculates the coordinates for the center of the selected objects and displays a crosshair there.
     */
    public synchronized void showCrosshairsForCentroid() {
        int[] coords = calcCentroid(selectionStack.getFirst().getAll());
        if (coords.length == 2) {
            centroidX = lonE7ToX(coords[1]);
            centroidY = latE7ToY(coords[0]);
            showCrosshairs(centroidX, centroidY);
        } else {
            Log.e(DEBUG_TAG, "Unable to calcualte centroid for selection");
        }
    }

    /**
     * Handles a finger-movement on the touchscreen. Moves a node when draggingNode was set by
     * {@link #handleTouchEventDown(float, float)}. Otherwise the movement will be interpreted as map-translation. Map
     * will be repainted.
     * 
     * @param main the current instance of Main
     * @param absoluteX The absolute display-coordinate.
     * @param absoluteY The absolute display-coordinate.
     * @param relativeX The difference to the last absolute display-coordinate.
     * @param relativeY The difference to the last absolute display-coordinate.
     * @throws OsmIllegalOperationException if one of the operations triggered went wrong
     */
    synchronized void handleTouchEventMove(@NonNull Main main, final float absoluteX, final float absoluteY, final float relativeX, final float relativeY) {
        final Selection currentSelection = selectionStack.getFirst();
        try {
            if (draggingNode || draggingWay || draggingHandle || draggingNote) {
                int lat = yToLatE7(absoluteY);
                int lon = xToLonE7(absoluteX);
                final int selectedWayCount = currentSelection.wayCount();
                final int selectedNodeCount = currentSelection.nodeCount();
                // checkpoint created where draggingNode is set
                if ((draggingNode && ((selectedNodeCount == 1 && selectedWayCount == 0) || selectedWayCount == 1)) || draggingHandle || draggingNote) {
                    if (draggingHandle) { // create node only if we are really dragging
                        if (handleNode == null && selectedHandle != null && selectedWayCount > 0) {
                            Log.d(DEBUG_TAG, "creating node at handle position");
                            handleNode = addOnWay(main, currentSelection.getWays(), selectedHandle.x, selectedHandle.y, true);
                            selectedHandle = null;
                        }
                        if (handleNode != null) {
                            getDelegator().moveNode(handleNode, lat, lon);
                        }

                    } else {
                        if (prefs.largeDragArea()) {
                            startY = startY + relativeY;
                            startX = startX - relativeX;
                            lat = yToLatE7(startY);
                            lon = xToLonE7(startX);
                        }
                        if (draggingNode) {
                            if (selectedNodeCount == 1) {
                                draggedNode = currentSelection.getNode();
                            }
                            displayAttachedObjectWarning(main, draggedNode);
                            getDelegator().moveNode(draggedNode, lat, lon);
                        } else {
                            de.blau.android.layer.tasks.MapOverlay taskLayer = map.getTaskLayer();
                            if (taskLayer != null) {
                                Task selectedTask = taskLayer.getSelected();
                                if (selectedTask.isNew()) {
                                    App.getTaskStorage().move(selectedTask, lat, lon);
                                } else {
                                    ScreenMessage.barWarning(main, R.string.toast_move_note_warning);
                                }
                            }
                        }
                    }
                } else { // way dragging and multi-select
                    List<Node> nodes = new ArrayList<>();
                    if (selectedWayCount > 0) { // shouldn't happen but might be a race condition
                        for (Way w : currentSelection.getWays()) {
                            nodes.addAll(w.getNodes());
                        }
                    }
                    if (selectedNodeCount > 0) {
                        nodes.addAll(currentSelection.getNodes());
                    }
                    displayAttachedObjectWarning(main, nodes);
                    getDelegator().moveNodes(nodes, lat - startLat, lon - startLon);
                    if (nodes.size() > MAX_NODES_FOR_MOVE && selectedWayCount == 1 && selectedNodeCount == 0) {
                        ScreenMessage.toastTopWarning(main, main.getString(R.string.toast_way_nodes_moved, nodes.size()));
                    }
                    // update
                    startLat = lat;
                    startLon = lon;
                }
                translateOnBorderTouch(absoluteX, absoluteY);
                main.getEasyEditManager().invalidate(); // if we are in an action mode update menubar
            } else if (rotating) {
                double aY = startY - centroidY;
                double aX = startX - centroidX;
                double bY = absoluteY - centroidY;
                double bX = absoluteX - centroidX;

                double aSq = (startY - absoluteY) * (startY - absoluteY) + (startX - absoluteX) * (startX - absoluteX);
                double bSq = bX * bX + bY * bY;
                double cSq = aX * aX + aY * aY;
                double cosAngle = Math.max(-1.0D, Math.min(1.0D, (bSq + cSq - aSq) / (2 * Math.sqrt(bSq) * Math.sqrt(cSq))));

                double det = aX * bY - aY * bX;
                int direction = det < 0 ? -1 : 1;

                rotateSelection(main, currentSelection, (float) Math.acos(cosAngle), direction);
                startY = absoluteY;
                startX = absoluteX;
                main.getEasyEditManager().invalidate(); // if we are in an action mode update menubar
            } else if (mode == Mode.MODE_ALIGN_BACKGROUND) {
                performBackgroundOffset(main, map.getZoomLevel(), relativeX, relativeY);
            } else {
                performTranslation(map, relativeX, relativeY);
                main.getEasyEditManager().invalidateOnDownload();
            }
        } catch (OsmIllegalOperationException | StorageException e) {
            handleDelegatorException(main, e);
        } catch (IllegalOperationException e) { // generated by moving a note
            ScreenMessage.barError(main, e.getMessage());
        }
        invalidateMap();
    }

    /**
     * Rotate selected objects
     * 
     * Note that this needs to rotate all the nodes of all objects at once to avoid rotating the same one multiple
     * times, special cases exactly one node selected.
     * 
     * @param activity the current Activity
     * @param selection the Selection
     * @param angle angle in radians that we rotated
     * @param direction rotation direction (+ == clockwise)
     */
    private void rotateSelection(@NonNull FragmentActivity activity, @NonNull final Selection selection, float angle, int direction) {
        displayAttachedObjectWarning(activity, selection.getAll());
        List<Node> nodes = new ArrayList<>();
        List<Way> selectedWays = selection.getWays();
        if (selectedWays != null) {
            for (Way w : selectedWays) {
                nodes.addAll(w.getNodes());
            }
        }
        List<Node> selectedNodes = selection.getNodes();
        if (selectedNodes != null) {
            if (selection.count() == 1) {
                updateDirection(activity, (float) Math.toDegrees(angle), direction, selectedNodes.get(0));
                return;
            }
            nodes.addAll(selectedNodes);
        }
        List<Relation> relations = selection.getRelations();
        if (relations != null) {
            for (Relation r : relations) {
                if (r.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON) && r.allDownloaded()) {
                    for (RelationMember m : r.getMembers(Way.NAME)) {
                        OsmElement e = m.getElement();
                        if (e instanceof Way) {
                            nodes.addAll(((Way) e).getNodes());
                        }
                    }
                }
            }
        }
        getDelegator().rotateNodes(nodes, angle, direction, centroidX, centroidY, map.getWidth(), map.getHeight(), viewBox);
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
        if (directionKey != null) {
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
            getDelegator().setTags(node, tags);
            map.invalidate();
        }
    }

    /**
     * @return is we should show warnings when filtered attached objects are being changed
     */
    private boolean showAttachedObjectWarning() {
        return attachedObjectWarning;
    }

    /**
     * Determine if we should show warnings when filtered attached objects are being changed
     * 
     * @param show if true show warnings
     */
    public void setAttachedObjectWarning(boolean show) {
        attachedObjectWarning = show;
    }

    /**
     * Puts the editor into the mode where the selected way will be rotated by the handleTouchEventMove function on move
     * events.
     * 
     * @param on new state
     */
    public void setRotationMode(boolean on) {
        rotating = on;
    }

    /**
     * Check if we are rotating an object
     * 
     * @return true if we are in rotation mode
     */
    public boolean isRotationMode() {
        return rotating;
    }

    /**
     * Converts screen-coords to gps-coords and delegates translation to {@link BoundingBox#translate(int, int)}.
     * GPS-Following will be disabled.
     * 
     * @param map current map view
     * @param screenTransX Movement on the screen.
     * @param screenTransY Movement on the screen.
     */
    private void performTranslation(@NonNull Map map, final float screenTransX, final float screenTransY) {
        int height = map.getHeight();
        int lon = xToLonE7(screenTransX);
        int lat = yToLatE7(height - screenTransY);
        int relativeLon = lon - viewBox.getLeft();
        int relativeLat = lat - viewBox.getBottom();

        try {
            viewBox.translate(map, relativeLon, relativeLat);
        } catch (OsmException e) {
            Log.d(DEBUG_TAG, "performTranslation got " + e.getMessage());
        }
    }

    /**
     * Converts screen-coords to gps-coords and offsets background layer.
     * 
     * @param main current instance of Main
     * @param zoomLevel the current zoom level
     * @param screenTransX Movement on the screen.
     * @param screenTransY Movement on the screen.
     */
    private void performBackgroundOffset(@NonNull Main main, int zoomLevel, final float screenTransX, final float screenTransY) {
        ImageryAlignmentActionModeCallback callback = main.getImageryAlignmentActionModeCallback();
        if (callback != null) {
            callback.setOffset(zoomLevel, screenTransX, screenTransY);
        } else {
            Log.e(DEBUG_TAG, "performBackgroundOffset callback null");
        }
    }

    /**
     * Executes an add-command for x,y. Adds new nodes and ways to storage. When more than one Node were
     * created/selected then a new way will be created.
     * 
     * Set selected Node and Way appropriately
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param x screen-coordinate
     * @param y screen-coordinate
     * @throws OsmIllegalOperationException if the operation coudn't be performed
     */
    public synchronized void performAdd(@Nullable final FragmentActivity activity, final float x, final float y) throws OsmIllegalOperationException {
        performAdd(activity, x, y, true, true);
    }

    /**
     * Executes an add-command for x,y. Adds new nodes and ways to storage. When more than one Node were
     * created/selected then a new way will be created.
     * 
     * Set selected Node and Way appropriately
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param x screen-coordinate
     * @param y screen-coordinate
     * @param createCheckpoint create a new undo checkpoint, always set to true except if part of a composite operation
     * @param snap if true existing nodes will be reused and new nodes created on nearby ways
     * @throws OsmIllegalOperationException if the operation coudn't be performed
     */
    public synchronized void performAdd(@Nullable final FragmentActivity activity, final float x, final float y, boolean createCheckpoint, boolean snap)
            throws OsmIllegalOperationException {
        Log.d(DEBUG_TAG, "performAdd");
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_add);
        }
        Node nextNode;
        Node lSelectedNode = getSelectedNode();
        Way lSelectedWay = getSelectedWay();

        try {
            nextNode = snap ? getClickedNodeOrCreatedWayNode(x, y) : getClickedNode(x, y);
            if (lSelectedNode == null) {
                // This will be the first node.
                if (!snap || nextNode == null) {
                    lSelectedNode = addNode(activity, x, y);
                } else {
                    lSelectedNode = nextNode;
                }
            } else {
                // this is not the first node
                if (nextNode == null) {
                    // clicked on empty space -> create a new Node
                    if (lSelectedWay == null) {
                        // This is the second Node, so we create a new Way and add the previous selected node to this
                        // way
                        lSelectedWay = getDelegator().createAndInsertWay(lSelectedNode);
                    }
                    lSelectedNode = addNode(activity, x, y);
                    getDelegator().addNodeToWay(lSelectedNode, lSelectedWay);
                } else {
                    // User clicks an existing Node
                    if (nextNode == lSelectedNode) {
                        // User clicks the last Node -> end here with adding
                        removeCheckpoint(activity, R.string.undo_action_add);
                        lSelectedNode = null;
                        lSelectedWay = null;
                    } else {
                        // Create a new way with the existing node, which was clicked.
                        if (lSelectedWay == null) {
                            lSelectedWay = getDelegator().createAndInsertWay(lSelectedNode);
                        }
                        if (!snap) {
                            nextNode = addNode(activity, x, y);
                        }
                        // Add the new Node.
                        getDelegator().addNodeToWay(nextNode, lSelectedWay);
                        lSelectedNode = nextNode;
                    }
                }
            }
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
        setSelectedNode(lSelectedNode);
        setSelectedWay(lSelectedWay);
    }

    /**
     * Create and add node to storage
     * 
     * @param activity optional activity for warnings
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @return the new Node
     */
    @NonNull
    private Node addNode(@Nullable final Activity activity, final float x, final float y) {
        return addNode(activity, xToLonE7(x), yToLatE7(y));
    }

    /**
     * Create and add node to storage
     * 
     * @param activity optional activity for warnings
     * @param lon longitude WGS84*1E7
     * @param lat latitude WGS84*1E7
     * @return the new Node
     */
    private Node addNode(@Nullable final Activity activity, int lon, int lat) {
        Node node = getDelegator().getFactory().createNodeWithNewId(lat, lon);
        getDelegator().insertElementSafe(node);
        outsideOfDownload(activity, lon, lat);
        return node;
    }

    /**
     * If the coordinates are not in a downloaded area show a warning
     * 
     * @param activity the calling Activity, if null no warning will be displayed
     * @param lonE7 WGS84 longitude*1E7
     * @param latE7 WGS84 latitude*1E7
     * @return true if we are not in a downloaded area
     */
    private boolean outsideOfDownload(@Nullable final Activity activity, int lonE7, int latE7) {
        if (!getDelegator().isInDownload(lonE7, latE7)) {
            Log.d(DEBUG_TAG, "Outside of download");
            if (activity != null) {
                ScreenMessage.toastTopWarning(activity, R.string.toast_outside_of_download);
            }
            return true;
        }
        return false;
    }

    /**
     * Add elements
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param elements a List of OsmElements
     */
    public void addElements(@Nullable final Activity activity, @NonNull List<OsmElement> elements) {
        createCheckpoint(activity, R.string.undo_action_add);
        for (OsmElement e : elements) {
            getDelegator().insertElementSafe(e);
        }
    }

    /**
     * Simplified version of creating a new node that takes geo coords and doesn't try to merge with existing features
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param lonD WGS84 longitude
     * @param latD WGS84 latitude
     * @return the created node
     */
    @NonNull
    public Node performAddNode(@Nullable final Activity activity, double lonD, double latD) {
        int lon = (int) (lonD * 1E7D);
        int lat = (int) (latD * 1E7D);
        return performAddNode(activity, lon, lat);
    }

    /**
     * Simplified version of creating a new node that takes geo coords and doesn't try to merge with existing features
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @return the created node
     */
    @NonNull
    public Node performAddNode(@Nullable final Activity activity, float x, float y) {
        return performAddNode(activity, xToLonE7(x), yToLatE7(y));
    }

    /**
     * Simplified version of creating a new node that takes geo coords and doesn't try to merge with existing features
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param lonE7 WGS84*1E7 longitude
     * @param latE7 WGS84*1E7 latitude
     * @return the created node
     */
    @NonNull
    public synchronized Node performAddNode(@Nullable final Activity activity, int lonE7, int latE7) {
        Log.d(DEBUG_TAG, "performAddNode");
        createCheckpoint(activity, R.string.undo_action_add);
        Node newNode = getDelegator().getFactory().createNodeWithNewId(latE7, lonE7);
        getDelegator().insertElementSafe(newNode);
        outsideOfDownload(activity, lonE7, latE7);
        setSelectedNode(newNode);
        return newNode;
    }

    /**
     * Executes an add node operation for x,y but only if on a way. Adds new node to storage and will select it.
     * 
     * If no new node is created any previously selected Node will remain selected
     * 
     * @param activity activity we were called from
     * @param ways candidate ways if null all ways will be considered
     * @param x screen-coordinate
     * @param y screen-coordinate
     * @param forceNew ignore nearby existing nodes
     * @return the new node or null if none was created
     * @throws OsmIllegalOperationException if the operation would create an illegal state
     */
    @Nullable
    public synchronized Node performAddOnWay(@Nullable Activity activity, @Nullable List<Way> ways, final float x, final float y, boolean forceNew)
            throws OsmIllegalOperationException {
        Node savedSelectedNode = getSelectedNode();
        Node newSelectedNode = addOnWay(activity, ways, x, y, forceNew);
        if (newSelectedNode == null) {
            setSelectedNode(savedSelectedNode);
            return null;
        }
        setSelectedNode(newSelectedNode);
        return newSelectedNode;
    }

    /**
     * Executes an add node operation for x,y but only if on a way. Adds new node to storage, doesn't select it.
     * 
     * @param activity activity we were called from
     * @param ways candidate ways if null all ways will be considered
     * @param x screen-coordinate
     * @param y screen-coordinate
     * @param forceNew ignore nearby existing nodes
     * @return the new node or null if none was created
     * @throws OsmIllegalOperationException if the operation would create an illegal state
     */
    @Nullable
    private Node addOnWay(Activity activity, List<Way> ways, final float x, final float y, boolean forceNew) {
        createCheckpoint(activity, R.string.undo_action_add);
        try {
            return getClickedNodeOrCreatedWayNode(ways, x, y, forceNew);
        } catch (OsmIllegalOperationException e) {
            rollback();
            throw new OsmIllegalOperationException(e);
        }
    }

    /**
     * Delete a node
     * 
     * @param activity activity this is running in, null if none
     * @param node Node to delete
     * @param createCheckpoint create an Undo checkpoint
     */
    public synchronized void performEraseNode(@Nullable final FragmentActivity activity, @NonNull final Node node, boolean createCheckpoint) {
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_deletenode);
        }
        displayAttachedObjectWarning(activity, node); // needs to be done before removal
        getDelegator().removeNode(node);
        invalidateMap();
        outsideOfDownload(activity, node.getLon(), node.getLat());
    }

    /**
     * Set new coordinates for an existing node and center viewbox on them
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param node node to set position on
     * @param lon longitude (WGS84)
     * @param lat latitude (WGS84)
     */
    public void performSetPosition(@Nullable final FragmentActivity activity, @NonNull final Node node, double lon, double lat) {

        int lonE7 = (int) (lon * 1E7d);
        int latE7 = (int) (lat * 1E7d);
        performSetPosition(activity, node, lonE7, latE7, true);
        viewBox.moveTo(map, lonE7, latE7);
        invalidateMap();
    }

    /**
     * Set new coordinates for an existing node
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param node node to set position on
     * @param lonE7 longitude (WGS84*E7)
     * @param latE7 latitude (WGS84*E7)
     * @param createCheckpoint if true create an undo checkpoint
     */
    public void performSetPosition(@Nullable final FragmentActivity activity, @NonNull final Node node, int lonE7, int latE7, boolean createCheckpoint) {
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_movenode);
        }
        try {
            displayAttachedObjectWarning(activity, node);
            getDelegator().moveNode(node, latE7, lonE7);
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Deletes a way.
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param way the way to be deleted
     * @param deleteOrphanNodes if true, way nodes that have no tags and are in no other ways will be deleted too, if
     *            activity is not null tags that would be discarded are ignored too
     * @param createCheckpoint if true create an undo checkpoint
     */
    public synchronized void performEraseWay(@Nullable final FragmentActivity activity, @NonNull final Way way, final boolean deleteOrphanNodes,
            boolean createCheckpoint) {
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_deleteway);
        }
        displayAttachedObjectWarning(activity, way); // needs to be done before removal
        HashSet<Node> nodes = deleteOrphanNodes ? new HashSet<>(way.getNodes()) : null; // HashSet guarantees uniqueness
        getDelegator().removeWay(way);
        if (deleteOrphanNodes) {
            DiscardedTags discardedTags = activity != null ? App.getDiscardedTags(activity) : null;
            for (Node node : nodes) {
                if (getWaysForNode(node).isEmpty() && (node.getTags().isEmpty() || (discardedTags != null && discardedTags.only(node)))) {
                    getDelegator().removeNode(node);
                }
            }
        }
        invalidateMap();
    }

    /**
     * Delete a relation
     * 
     * @param activity activity this is running in, null if none
     * @param relation Relation to delete
     * @param createCheckpoint create an Undo checkpoint
     */
    public synchronized void performEraseRelation(@Nullable final FragmentActivity activity, @NonNull final Relation relation, boolean createCheckpoint) {
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_delete_relation);
        }
        displayAttachedObjectWarning(activity, relation); // needs to be done before removal
        getDelegator().removeRelation(relation);
        invalidateMap();
    }

    /**
     * Erase a list of objects
     * 
     * Note check before calling if way nodes are in downloaded area
     * 
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param selection objects to delete
     */
    public synchronized void performEraseMultipleObjects(@Nullable final FragmentActivity activity, @NonNull List<OsmElement> selection) {
        // need to make three passes
        createCheckpoint(activity, R.string.undo_action_delete_objects);
        displayAttachedObjectWarning(activity, selection); // needs to be done before removal
        for (OsmElement e : selection) {
            if (e instanceof Relation && e.getState() != OsmElement.STATE_DELETED) {
                performEraseRelation(activity, (Relation) e, false);
            }
        }
        for (OsmElement e : selection) {
            if (e instanceof Way && e.getState() != OsmElement.STATE_DELETED) {
                performEraseWay(activity, (Way) e, true, false);
            }
        }
        for (OsmElement e : selection) {
            if (e instanceof Node && e.getState() != OsmElement.STATE_DELETED) {
                performEraseNode(activity, (Node) e, false);
            }
        }
    }

    /**
     * Do some clean up and display toast after we've received an Exception from the delegator
     * 
     * In general this should not happen with correct inputs.
     * 
     * @param activity the calling activity or null
     * @param ex the Exception to handle
     */
    private void handleDelegatorException(@Nullable final FragmentActivity activity, @NonNull Exception ex) {
        if (ex instanceof OsmIllegalOperationException) {
            dismissAttachedObjectWarning(activity);
            if (activity != null) {
                ScreenMessage.toastTopError(activity, activity.getString(R.string.toast_illegal_operation, ex.getLocalizedMessage()));
            }
            rollback();
            invalidateMap();
        } else if ((ex instanceof StorageException) && activity != null) {
            ScreenMessage.toastTopError(activity, R.string.toast_out_of_memory);
        }
    }

    /**
     * Splits a way at a given node
     * 
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param way the way to split
     * @param node the node at which the way should be split
     * @param fromEnd create new Way from Nodes after node
     * @return a List of Result objects containing the new Way and any issues
     * @throws OsmIllegalOperationException if the operation failed
     * @throws StorageException if we ran out of memory
     */
    @NonNull
    public synchronized List<Result> performSplit(@Nullable final FragmentActivity activity, @NonNull final Way way, @NonNull final Node node,
            boolean fromEnd) {
        createCheckpoint(activity, R.string.undo_action_split_way);
        try {
            List<Result> result = getDelegator().splitAtNode(way, node, fromEnd);
            invalidateMap();
            return result;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Split a closed way, needs two nodes
     * 
     * @param activity activity we were called from
     * @param way Way to split
     * @param node1 first split point
     * @param node2 second split point
     * @param createPolygons create polygons by closing the split ways if true
     * @return a List of Result objects containing the original Way in the 1st element and the new Way in the 2ndand any
     *         issues
     * @throws OsmIllegalOperationException if the operation failed
     * @throws StorageException if we ran out of memory
     */
    @NonNull
    public synchronized List<Result> performClosedWaySplit(@Nullable FragmentActivity activity, @NonNull Way way, @NonNull Node node1, @NonNull Node node2,
            boolean createPolygons) {
        createCheckpoint(activity, R.string.undo_action_split_way);
        try {
            displayAttachedObjectWarning(activity, way);
            List<Result> results = getDelegator().splitAtNodes(way, node1, node2, createPolygons);
            if (!createPolygons) {
                checkForArea(activity, way, results);
            }
            invalidateMap();
            return results;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Extract a segment from a way (the way between two nodes of the same way)
     * 
     * @param activity activity we were called fron
     * @param way Way to split
     * @param node1 first split point
     * @param node2 second split point
     * @return the segment in the 1st Result if successful, otherwise the results contain issues
     */
    @NonNull
    public synchronized List<Result> performExtractSegment(@Nullable FragmentActivity activity, @NonNull Way way, @NonNull Node node1, @NonNull Node node2) {
        createCheckpoint(activity, R.string.undo_action_extract_segment);
        try {
            displayAttachedObjectWarning(activity, way);
            List<Result> result = null;
            if (way.isClosed()) {
                // extracted segment is in the 2nd result
                result = getDelegator().splitAtNodes(way, node1, node2, false);
                result = result.subList(1, result.size());
                checkForArea(activity, way, result);
                return result;
            } else if (way.isEndNode(node1)) {
                result = extractSegmentAtEnd(way, node1, node2);
            } else if (way.isEndNode(node2)) {
                result = extractSegmentAtEnd(way, node2, node1);
            } else {
                result = getDelegator().splitAtNode(way, node1, true);
                Result first = result.get(0);
                boolean splitOriginal = way.hasNode(node2);
                Way newWay = (Way) first.getElement();
                List<Result> result2 = getDelegator().splitAtNode(way.hasNode(node2) ? way : newWay, node2, true);
                Way newWay2 = (Way) result2.get(0).getElement();
                first.setElement(newWay2.hasNode(node1) && newWay2.hasNode(node2) ? newWay2 : (splitOriginal ? way : newWay));
            }
            invalidateMap();
            return result;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex;
        }
    }

    /**
     * If Way has implied area semantics or an explicit area=yes, add an issue to the result
     * 
     * @param activity an Activity
     * @param way the Way
     * @param results a list of Result
     */
    private void checkForArea(@Nullable FragmentActivity activity, @NonNull Way way, @NonNull List<Result> results) {
        if (way.hasTag(Tags.KEY_AREA, Tags.VALUE_YES) || (activity != null && App.getAreaTags(activity).isImpliedArea(way.getTags()))) {
            results.get(0).addIssue(SplitIssue.SPLIT_AREA);
        }
    }

    /**
     * Extract a segment at the end of a way
     * 
     * @param way the original Way
     * @param endNode the end Node
     * @param splitNode the Node to split at
     * @return a List of Result with the segment in the 1st Result
     */
    private List<Result> extractSegmentAtEnd(@NonNull Way way, @NonNull Node endNode, @NonNull Node splitNode) {
        List<Result> result = getDelegator().splitAtNode(way, splitNode, true);
        if (result.isEmpty()) {
            throw new OsmIllegalOperationException("Splitting way " + way.getOsmId() + " at node " + splitNode.getOsmId() + " failed");
        }
        Result first = result.get(0);
        Way newWay = (Way) first.getElement();
        first.setElement(newWay.isEndNode(endNode) ? newWay : way);
        return result;
    }

    /**
     * Remove a Node from a specific Way, if the Node is untagged and not a member of a further Way it will be deleted
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param way the Way
     * @param node the Node
     */
    public synchronized void performRemoveNodeFromWay(@Nullable FragmentActivity activity, @NonNull Way way, @NonNull Node node) {
        createCheckpoint(activity, R.string.undo_action_remove_node_from_way);
        displayAttachedObjectWarning(activity, node);
        getDelegator().removeNodeFromWay(way, node);
        invalidateMap();
    }

    /**
     * Remove a end Node from a specific Way, if the Node is untagged and not a member of a further Way it will be
     * deleted, if the 2nd last node is removed the Way will be deleted.
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param fromEnd if true remove last node else first
     * @param way the Way
     * @param deleteNode delete the Node after removing it
     * @param createCheckPoint if true create an undo checkpoint
     */
    public synchronized void performRemoveEndNodeFromWay(@Nullable FragmentActivity activity, boolean fromEnd, @NonNull Way way, boolean deleteNode,
            boolean createCheckPoint) {
        if (createCheckPoint) {
            createCheckpoint(activity, R.string.undo_action_remove_node_from_way);
        }
        displayAttachedObjectWarning(activity, way.getLastNode());
        getDelegator().removeEndNodeFromWay(fromEnd, way, deleteNode);
        invalidateMap();
    }

    /**
     * Merge two ways. Ways must be valid (i.e. have at least two nodes) and mergeable (i.e. have a common start/end
     * node).
     * 
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param mergeInto Way to merge the other way into. This way will be kept.
     * @param mergeFrom Way to merge into the other. This way will be deleted.
     * @return a List of Result with the merged OsmElement and a list of issues if any
     * @throws OsmIllegalOperationException if the operation couldn't be performed
     */
    public synchronized List<Result> performMerge(@Nullable final FragmentActivity activity, @NonNull Way mergeInto, @NonNull Way mergeFrom) {
        createCheckpoint(activity, R.string.undo_action_merge_ways);
        try {
            displayAttachedObjectWarning(activity, mergeInto, mergeFrom, true); // needs to be done before merge
            MergeAction action = new MergeAction(getDelegator(), mergeInto, mergeFrom, getSelectedIds());
            List<Result> result = action.mergeWays();
            invalidateMap();
            return result;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Merge a sorted list of ways
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param sortedWays list of ways to be merged
     * @return a List of Result, includes merged way and anything else of interest
     */
    @NonNull
    public synchronized List<Result> performMerge(@Nullable FragmentActivity activity, @NonNull List<OsmElement> sortedWays) {
        createCheckpoint(activity, R.string.undo_action_merge_ways);
        displayAttachedObjectWarning(activity, sortedWays, true); // needs to be done before merge
        if (sortedWays.isEmpty()) {
            throw new OsmIllegalOperationException("No ways to merge");
        }
        for (OsmElement e : sortedWays) {
            if (!(e instanceof Way)) {
                throw new OsmIllegalOperationException("Only ways can be merged");
            }
        }
        try {
            List<Result> overallResult = new ArrayList<>();
            Result result = new Result();
            overallResult.add(result);
            Way previousWay = (Way) sortedWays.get(0);
            result.setElement(previousWay);
            for (int i = 1; i < sortedWays.size(); i++) {
                Way nextWay = (Way) sortedWays.get(i);
                MergeAction action = new MergeAction(getDelegator(), previousWay, nextWay, getSelectedIds());
                List<Result> tempResult = action.mergeWays();
                final Result newMergeResult = tempResult.get(0);
                if (!(newMergeResult.getElement() instanceof Way)) {
                    throw new IllegalStateException("mergeWays didn't return a Way");
                }
                if (newMergeResult.hasIssue()) {
                    Log.d(DEBUG_TAG, "ways " + previousWay.getDescription() + " and " + nextWay + " caused a merge conflict");
                    result.addAllIssues(newMergeResult.getIssues());
                }
                result.setElement(newMergeResult.getElement());
                overallResult.addAll(tempResult.subList(1, tempResult.size()));
                if (previousWay.getState() == OsmElement.STATE_DELETED) {
                    previousWay = nextWay;
                }
            }
            return overallResult;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Merge two closed ways
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param ways list of ways to be merged
     * @return a List of Result, includes merged way and anything else of interest
     */
    public synchronized List<Result> performPolygonMerge(@Nullable FragmentActivity activity, @NonNull List<Way> ways) {
        createCheckpoint(activity, R.string.undo_action_merge_polygons);
        displayAttachedObjectWarning(activity, ways, true); // needs to be done before merge
        if (!(ways.size() == 2 && ways.get(0).isClosed() && ways.get(1).isClosed())) {
            throw new OsmIllegalOperationException("No mergeable polygons");
        }
        try {
            MergeAction action = new MergeAction(getDelegator(), ways.get(0), ways.get(1), getSelectedIds());
            return action.mergeSimplePolygons(map);
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Orthogonalize a way (aka make angles 90°)
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param way way to square
     */
    public void performOrthogonalize(@Nullable FragmentActivity activity, @Nullable Way way) {
        if (way == null || way.getNodes().size() < 3) {
            Log.e(DEBUG_TAG, "performOrthogonalize way " + (way == null ? "is null" : " has " + way.nodeCount()) + " nodes");
            return;
        }
        performOrthogonalize(activity, Util.wrapInList(way));
    }

    /**
     * Orthogonalize multiple ways at once (aka make angles 90°)
     * 
     * As this can take a noticeable amount of time, we execute async and display a toast when finished
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param ways ways to square
     */
    public void performOrthogonalize(@Nullable FragmentActivity activity, @Nullable List<Way> ways) {
        if (ways == null || ways.isEmpty()) {
            Log.e(DEBUG_TAG, "performOrthogonalize no ways");
            return;
        }

        final int threshold = prefs.getOrthogonalizeThreshold();
        new ExecutorTask<Void, Void, StorageException>(executorService, uiHandler) {

            @Override
            protected StorageException doInBackground(Void param) {
                createCheckpoint(activity, R.string.undo_action_orthogonalize);
                try {
                    getDelegator().orthogonalizeWay(ways, threshold);
                } catch (StorageException ex) {
                    return ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(StorageException ex) {
                if (ex != null) {
                    handleDelegatorException(activity, ex);
                    return;
                }
                invalidateMap();
                if (activity != null) {
                    ScreenMessage.toastTopInfo(activity, R.string.Done);
                }
                if (getFilter() != null && showAttachedObjectWarning()) {
                    Set<Node> nodes = new HashSet<>();
                    for (Way w : ways) {
                        nodes.addAll(w.getNodes());
                    }
                    displayAttachedObjectWarning(activity, nodes);
                }

            }
        }.execute();
    }

    /**
     * Replace node in all ways it is a member of with a new node, leaving node selected, if it already is. Note:
     * relation memberships are not modified
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param node node to extract from any ways it is a member of
     * @return the new way node or null if the node was not a way node
     */
    @Nullable
    public synchronized Node performExtract(@Nullable FragmentActivity activity, final Node node) {
        if (node != null) {
            try {
                createCheckpoint(activity, R.string.undo_action_extract_node);
                displayAttachedObjectWarning(activity, node); // this needs to be done -before- we replace the node
                Node newNode = getDelegator().replaceNode(node);
                invalidateMap();
                return newNode;
            } catch (OsmIllegalOperationException | StorageException ex) {
                handleDelegatorException(activity, ex);
                throw ex; // rethrow
            }
        }
        return null;
    }

    /**
     * If any Nodes or Ways are close to the node (within the tolerance), return them, if any Nodes are found don't
     * check Ways.
     * 
     * @param nodeToJoin the Node we want to join
     * @return a List of OsmElements
     */
    @NonNull
    public List<OsmElement> findJoinableElements(@NonNull Node nodeToJoin) {
        List<OsmElement> closestElements = new ArrayList<>();
        float jx = lonE7ToX(nodeToJoin.getLon());
        float jy = latE7ToY(nodeToJoin.getLat());
        // start by looking for the closest nodes
        for (Node node : getDelegator().getCurrentStorage().getNodes()) {
            if (nodeToJoin.equals(node)) {
                continue;
            }
            Double distance = clickDistance(node, jx, jy);
            if (distance != null && (filter == null || filter.include(node, false))) {
                closestElements.add(node);
            }
        }
        if (!closestElements.isEmpty()) {
            return closestElements;
        }
        // fall back to closest ways
        final float tolerance = wayToleranceForTouch(map.getDataStyle().getCurrent());
        for (Way way : getDelegator().getCurrentStorage().getWays()) {
            List<Node> wayNodes = way.getNodes();
            if (way.hasNode(nodeToJoin) || wayNodes.isEmpty()) {
                continue;
            }
            Node firstNode = wayNodes.get(0);
            float node1X = lonE7ToX(firstNode.getLon());
            float node1Y = latE7ToY(firstNode.getLat());
            for (int i = 1, wayNodesSize = wayNodes.size(); i < wayNodesSize; ++i) {
                Node node2 = wayNodes.get(i);
                float node2X = lonE7ToX(node2.getLon());
                float node2Y = latE7ToY(node2.getLat());
                double distance = Geometry.isPositionOnLine(tolerance, jx, jy, node1X, node1Y, node2X, node2Y);
                if (distance >= 0 && (filter == null || filter.include(way, false))) {
                    closestElements.add(way);
                }
                node1X = node2X;
                node1Y = node2Y;
            }
        }
        return closestElements;
    }

    /**
     * Merge a node with other Nodes.
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param elements List of Node that the Node will be merged to.
     * @param nodeToJoin Node to be merged
     * @return a List of MergeResult objects containing the result of the merge
     */
    @NonNull
    public synchronized List<Result> performMergeNodes(@Nullable FragmentActivity activity, @NonNull List<OsmElement> elements, @NonNull Node nodeToJoin) {
        Log.d(DEBUG_TAG, "performMergeNodes " + nodeToJoin.getOsmId() + " " + elements.size() + " targets");
        List<Result> overallResult = new ArrayList<>();
        if (elements.isEmpty()) {
            return overallResult;
        }
        createCheckpoint(activity, R.string.undo_action_join);
        Result result = null;
        for (OsmElement element : elements) {
            nodeToJoin = (Node) (!overallResult.isEmpty() ? overallResult.get(0).getElement() : nodeToJoin);
            if (element.equals(nodeToJoin)) {
                throw new OsmIllegalOperationException("Trying to join node to itself");
            }
            displayAttachedObjectWarning(activity, element, nodeToJoin); // needs to be done before join
            MergeAction action = new MergeAction(getDelegator(), element, nodeToJoin, getSelectedIds());
            try {
                List<Result> tempResult = action.mergeNodes();
                if (overallResult.isEmpty()) {
                    overallResult = tempResult;
                    result = overallResult.get(0);
                } else {
                    final Result newMergeResult = tempResult.get(0);
                    result.setElement(newMergeResult.getElement()); // NOSONAR potential new result element
                    result.addAllIssues(newMergeResult.getIssues());
                    overallResult.addAll(tempResult.subList(1, tempResult.size()));
                }
            } catch (OsmIllegalOperationException | StorageException ex) {
                handleDelegatorException(activity, ex);
                throw ex; // rethrow
            }
            if (!(result.getElement() instanceof Node)) {
                throw new IllegalStateException("mergeNodes didn't return a Node");
            }
        }
        invalidateMap();
        return overallResult;
    }

    /**
     * Join a Node to one or more Ways
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param elements List of Ways that the Node will be merged to.
     * @param nodeToJoin Node to be merged
     * @return a List of Results object containing the result of the merge and if the result was successful
     */
    @NonNull
    public synchronized List<Result> performJoinNodeToWays(@Nullable FragmentActivity activity, @NonNull List<OsmElement> elements, @NonNull Node nodeToJoin) {
        if (elements.isEmpty()) {
            return new ArrayList<>();
        }
        List<Result> result = null;
        final float tolerance = map.getDataStyle().getCurrent().getWayToleranceValue() / 2f;
        createCheckpoint(activity, R.string.undo_action_join);
        for (OsmElement element : elements) {
            if (!(element instanceof Way)) {
                // Note if no ways are in elements this will create an empty checkpoint
                continue;
            }
            nodeToJoin = (Node) (result != null ? result.get(0).getElement() : nodeToJoin);
            Way way = (Way) element;
            List<Node> wayNodes = way.getNodes();
            if (wayNodes.contains(nodeToJoin)) {
                throw new OsmIllegalOperationException("Trying to join node to itself in way");
            }
            List<Result> tempResult = null;
            float x = lonE7ToX(nodeToJoin.getLon());
            float y = latE7ToY(nodeToJoin.getLat());
            Node node1 = wayNodes.get(0);
            float node1X = lonE7ToX(node1.getLon());
            float node1Y = latE7ToY(node1.getLat());
            for (int i = 1, wayNodesSize = wayNodes.size(); i < wayNodesSize; ++i) {
                Node node2 = wayNodes.get(i);
                float node2X = lonE7ToX(node2.getLon());
                float node2Y = latE7ToY(node2.getLat());
                double distance = Geometry.isPositionOnLine(tolerance, x, y, node1X, node1Y, node2X, node2Y);
                if (distance >= 0) {
                    float[] p = GeoMath.closestPoint(x, y, node1X, node1Y, node2X, node2Y);
                    int lat = yToLatE7(p[1]);
                    int lon = xToLonE7(p[0]);
                    Node node = null;
                    if (node == null && lat == node1.getLat() && lon == node1.getLon()) {
                        node = node1;
                    }
                    if (node == null && lat == node2.getLat() && lon == node2.getLon()) {
                        node = node2;
                    }
                    try {
                        if (node == null) {
                            displayAttachedObjectWarning(activity, way, nodeToJoin); // needs to be done before join
                            // move the existing node onto the way and insert it into the way
                            getDelegator().moveNode(nodeToJoin, lat, lon);
                            getDelegator().addNodeToWayAfter(i - 1, nodeToJoin, way);
                            tempResult = Util.wrapInList(new Result(nodeToJoin));
                        } else {
                            displayAttachedObjectWarning(activity, node, nodeToJoin); // needs to be done before join
                            // merge node into target Node
                            MergeAction action = new MergeAction(getDelegator(), node, nodeToJoin, getSelectedIds());
                            tempResult = action.mergeNodes();
                        }
                    } catch (OsmIllegalOperationException | StorageException ex) {
                        handleDelegatorException(activity, ex);
                        throw ex; // rethrow
                    }
                    break; // need to leave loop !!!
                }
                node1 = node2;
                node1X = node2X;
                node1Y = node2Y;
            }
            if (result == null) {
                result = tempResult;
            } else if (tempResult != null && !tempResult.isEmpty()) { // if null we didn't actually merge anything
                final Result newMergeResult = tempResult.get(0);
                final Result mergeResult = result.get(0);
                mergeResult.setElement(newMergeResult.getElement());
                if (newMergeResult.hasIssue()) {
                    mergeResult.addAllIssues(newMergeResult.getIssues());
                }
                result.addAll(tempResult.subList(1, tempResult.size()));
            }
        }
        invalidateMap();
        return result;
    }

    /**
     * Unjoin all ways joined by the given node.
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param node Node that is joining the ways to be unjoined.
     */
    public synchronized void performUnjoinWays(@Nullable FragmentActivity activity, @NonNull Node node) {
        try {
            createCheckpoint(activity, R.string.undo_action_unjoin_ways);
            displayAttachedObjectWarning(activity, node); // needs to be done before unjoin
            getDelegator().unjoinWays(node);
            invalidateMap();
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Unjoin a way
     * 
     * @param activity activity this was called from, if null no warnings will be displayed
     * @param way the Way to unjoin
     * @param ignoreSimilar don't unjoin from ways with the same primary key if true, but replace the node in them too
     */
    public synchronized void performUnjoinWay(@Nullable FragmentActivity activity, @NonNull Way way, boolean ignoreSimilar) {
        try {
            createCheckpoint(activity, R.string.undo_action_unjoin_ways);
            displayAttachedObjectWarning(activity, way); // needs to be done before unjoin
            getDelegator().unjoinWay(activity, way, ignoreSimilar);
            invalidateMap();
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Reverse a ways direction
     * 
     * Note this does NOT reverse oneway tags, since we assume that changing direction if the oneway was the whole point
     * of calling this
     * 
     * @param activity activity we were called from
     * @param way the way to reverse
     * @return true if reverseWay returned true, implying that tags had to be reversed
     */
    @NonNull
    public synchronized List<Result> performReverse(@Nullable FragmentActivity activity, @NonNull Way way) {
        try {
            createCheckpoint(activity, R.string.undo_action_reverse_way);
            List<Result> result = getDelegator().reverseWay(way);
            invalidateMap();
            return result;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Determine the Way and Node to start appending to a way
     * 
     * @param way the Way we are going to append to
     * @param node the Node we're starting at
     */
    public synchronized void performAppendStart(@Nullable Way way, @Nullable Node node) {
        setSelectedNode(node);
        setSelectedWay(way);
        invalidateMap();
    }

    /**
     * Append a Node to the selected Way, if the selected Node is clicked finish, otherwise create a new Node at the
     * location
     * 
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param x screen x coordinate
     * @param y screen y coordinate
     * @param createCheckpoint normally true, only set to false in a multi-step operation, for which the checkpoint has
     *            already been created
     * @param snap if true existing nodes will be reused and new nodes created on nearby ways
     * @throws OsmIllegalOperationException if the operation couldn't be performed
     */
    public synchronized void performAppendAppend(@Nullable final Activity activity, final float x, final float y, boolean createCheckpoint, boolean snap)
            throws OsmIllegalOperationException {
        Log.d(DEBUG_TAG, "performAppendAppend");
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_append);
        }
        Node lSelectedNode = getSelectedNode();
        Way lSelectedWay = getSelectedWay();
        try {
            Node node = snap ? getClickedNodeOrCreatedWayNode(x, y) : getClickedNode(x, y);
            if (node == lSelectedNode) {
                lSelectedNode = null;
                lSelectedWay = null;
            } else if (lSelectedWay != null) { // may have been de-selected before we got here
                if (!snap || node == null) { // always create new node if join is false
                    node = addNode(activity, x, y);
                }
                getDelegator().appendNodeToWay(lSelectedNode, node, lSelectedWay);
                lSelectedNode = node;
            }
        } catch (OsmIllegalOperationException | StorageException e) {
            rollback();
            throw e;
        }
        setSelectedNode(lSelectedNode);
        setSelectedWay(lSelectedWay);
        invalidateMap();
    }

    /**
     * Replace a Ways geometry adding/deleting nodes if necessary
     * 
     * @param activity optional Activity
     * @param target way that will get the new geometry
     * @param geometry list of GeoPoint with the new geometry
     * @return a List of Result elements
     */
    @NonNull
    public synchronized <T extends GeoPoint> List<Result> performReplaceGeometry(@Nullable final FragmentActivity activity, @NonNull Way target,
            @NonNull List<T> geometry) {
        StorageDelegator delegator = getDelegator();
        createCheckpoint(activity, R.string.undo_action_replace_geometry);
        final int geometrySize = geometry.size();
        try {
            delegator.validateWayNodeCount(geometrySize);
            boolean sourceClosed = geometry.get(0).equals(geometry.get(geometrySize - 1));
            int sourceNodeCount = geometrySize - (sourceClosed ? 1 : 0);

            int targetNodeCount = target.nodeCount() - (target.isClosed() ? 1 : 0);
            // copy without closing node if present
            List<Node> targetNodes = new ArrayList<>(target.getNodes().subList(0, targetNodeCount));
            List<Node> newNodes = new ArrayList<>();
            for (int i = 0; i < sourceNodeCount; i++) {
                final GeoPoint sourceNode = geometry.get(i);
                // find a suitable target node
                int sourceLon = sourceNode.getLon();
                int sourceLat = sourceNode.getLat();
                Node targetNode = findTargetNode(targetNodes, sourceLon / 1E7D, sourceLat / 1E7D);
                if (targetNode != null) {
                    performSetPosition(activity, targetNode, sourceLon, sourceLat, false);
                } else {
                    targetNode = addNode(activity, sourceLon, sourceLat);
                }
                newNodes.add(targetNode);
            }
            if (sourceClosed) {
                newNodes.add(newNodes.get(0)); // close the target
            }
            delegator.replaceWayNodes(newNodes, target);
            // final act: delete all unused untagged nodes left
            List<Result> result = new ArrayList<>();
            for (Node n : targetNodes) {
                if (!n.isTagged() && !n.hasParentRelations() && getWaysForNode(n).isEmpty()) {
                    performEraseNode(activity, n, false);
                } else {
                    Result r = new Result(n);
                    r.addIssue(ReplaceIssue.EXTRACTED_NODE);
                    result.add(r);
                }
            }
            return result;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Get a node that we can move to a new position in a ways geometry without losing too much information
     * 
     * @param targetNodes List of Nodes that we can use
     * @param newLon new longitude
     * @param newLat new latitude
     * @return a Node or null
     */
    @Nullable
    private Node findTargetNode(@NonNull List<Node> targetNodes, double newLon, double newLat) {
        Node bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        for (Node target : targetNodes) {
            double distance = GeoMath.haversineDistance(target.getLon() / 1E7D, target.getLat() / 1E7D, newLon, newLat);
            if (distance < bestDistance) {
                if (target.hasTags() && prefs != null && distance > prefs.getReplaceTolerance()) { // only use tagged
                                                                                                   // nodes if they are
                                                                                                   // really close to
                                                                                                   // new
                    // position
                    continue;
                }
                bestDistance = distance;
                bestTarget = target;
            }
        }
        if (bestTarget != null) {
            targetNodes.remove(bestTarget);
        }
        return bestTarget;
    }

    /**
     * Tries to locate the selected node. If x,y lays on a way, a new node at this location will be created, stored in
     * storage and returned.
     * 
     * @param x the x screen coordinate
     * @param y the y screen coordinate
     * @return the selected node or the created node, if x,y lays on a way. Null if any node or way was selected.
     * @throws OsmIllegalOperationException if the operation couldn't be performed
     */
    private synchronized Node getClickedNodeOrCreatedWayNode(final float x, final float y) throws OsmIllegalOperationException {
        return getClickedNodeOrCreatedWayNode(null, x, y, false);
    }

    /**
     * Tries to locate the selected node. If x,y lays on a way, a new node at this location will be created, stored in
     * storage and returned.
     * 
     * @param ways list of candidate ways or null for all
     * @param x the x screen coordinate
     * @param y the y screen coordinate
     * @param forceNew do not return existing nodes in tolerance range
     * @return the selected node or the created node, if x,y lays on a way. null if any node or way was selected.
     * @throws OsmIllegalOperationException if the operation couldn't be performed
     */
    @Nullable
    private synchronized Node getClickedNodeOrCreatedWayNode(@Nullable List<Way> ways, final float x, final float y, boolean forceNew)
            throws OsmIllegalOperationException {
        Node node = null;
        if (!forceNew) {
            node = getClickedNode(x, y);
            if (node != null) {
                return node;
            }
        }
        if (ways == null) {
            ways = getDelegator().getCurrentStorage().getWays(map.getViewBox());
        }
        Node savedNode1 = null;
        Node savedNode2 = null;
        List<Way> savedWays = new ArrayList<>();
        List<Integer> savedWaysNodeIndex = new ArrayList<>();
        double savedDistance = Double.MAX_VALUE;
        final float tolerance = wayToleranceForTouch(map.getDataStyle().getCurrent());
        // create a new node on a way
        for (Way way : ways) {
            if (filter != null && !filter.include(way, isSelected(way))) {
                continue;
            }
            List<Node> wayNodes = way.getNodes();
            float node1X = -Float.MAX_VALUE;
            float node1Y = -Float.MAX_VALUE;
            boolean firstNode = true;
            Node node1 = wayNodes.get(0);
            int wayNodesSize = wayNodes.size();
            for (int k = 1; k < wayNodesSize; ++k) {
                Node node2 = wayNodes.get(k);
                if (firstNode) {
                    node1X = lonE7ToX(node1.getLon());
                    node1Y = latE7ToY(node1.getLat());
                    firstNode = false;
                }
                float node2X = lonE7ToX(node2.getLon());
                float node2Y = latE7ToY(node2.getLat());

                double distance = Geometry.isPositionOnLine(tolerance, x, y, node1X, node1Y, node2X, node2Y);
                if (distance >= 0) {
                    if ((savedNode1 == null && savedNode2 == null) || distance < savedDistance) {
                        savedNode1 = node1;
                        savedNode2 = node2;
                        savedDistance = distance;
                        savedWays.clear();
                        savedWays.add(way);
                        savedWaysNodeIndex.clear();
                        savedWaysNodeIndex.add(k - 1);
                    } else if ((node1 == savedNode1 && node2 == savedNode2) || (node1 == savedNode2 && node2 == savedNode1)) {
                        savedWays.add(way);
                        savedWaysNodeIndex.add(k - 1);
                    }
                }
                node1 = node2;
                node1X = node2X;
                node1Y = node2Y;
            }
        }
        // way(s) found in tolerance range
        if (savedNode1 != null && savedNode2 != null) {
            node = createNodeOnWay(savedNode1, savedNode2, x, y);
            if (node != null) {
                getDelegator().insertElementSafe(node);
                for (int i = 0; i < savedWays.size(); i++) {
                    getDelegator().addNodeToWayAfter(savedWaysNodeIndex.get(i), node, savedWays.get(i));
                }
            }
        }
        return node;
    }

    /**
     * Creates a new node at x,y between node1 and node2. When x,y does not lay on the line between node1 and node2 it
     * will return null.
     * 
     * @param node1 the first node
     * @param node2 the second node
     * @param x screen coordinate where the new node shall be created.
     * @param y screen coordinate where the new node shall be created.
     * @return a new created node at lon/lat corresponding to x,y. When x,y does not lay on the line between node1 and
     *         node2 it will return null.
     */
    private synchronized Node createNodeOnWay(final Node node1, final Node node2, final float x, final float y) {
        // Nodes have to be converted to screen-coordinates, due to a better tolerance-check.
        float node1X = lonE7ToX(node1.getLon());
        float node1Y = latE7ToY(node1.getLat());
        float node2X = lonE7ToX(node2.getLon());
        float node2Y = latE7ToY(node2.getLat());

        // At first, we check if the x,y is in the bounding box clamping by node1 and node2.
        if (Geometry.isPositionOnLine(wayToleranceForTouch(map.getDataStyle().getCurrent()), x, y, node1X, node1Y, node2X, node2Y) >= 0) {
            float[] p = GeoMath.closestPoint(x, y, node1X, node1Y, node2X, node2Y);
            int lat = yToLatE7(p[1]);
            int lon = xToLonE7(p[0]);
            return getDelegator().getFactory().createNodeWithNewId(lat, lon);
        }
        return null;
    }

    /**
     * Translates the {@link #viewBox} in the direction of x/y's next border.
     * 
     * @param x screen-coordinate
     * @param y screen-coordinate
     */
    private void translateOnBorderTouch(final float x, final float y) {
        int translationOnBorderTouch = (int) (viewBox.getWidth() * BORDER_TOCH_TRANSLATION_FACTOR);

        try {
            if (x > map.getWidth() - PADDING_ON_BORDER_TOUCH) {
                viewBox.translate(map, translationOnBorderTouch, 0);
            } else if (x < PADDING_ON_BORDER_TOUCH) {
                viewBox.translate(map, -translationOnBorderTouch, 0);
            }

            if (y > map.getHeight() - PADDING_ON_BORDER_TOUCH) {
                viewBox.translate(map, 0, -translationOnBorderTouch);
            } else if (y < PADDING_ON_BORDER_TOUCH) {
                viewBox.translate(map, 0, translationOnBorderTouch);
            }
        } catch (OsmException e) {
            Log.d(DEBUG_TAG, "translateOnBorderTouch got " + e.getMessage());
        }
    }

    /**
     * Loads the area defined by mapBox from the OSM-Server.
     * 
     * @param context Android Context, if this is a FRagmentActivity, animations will be shown
     * @param mapBox Box defining the area to be loaded.
     * @param add if true add this data to existing
     * @param postLoadHandler handler to execute after successful download
     */
    public synchronized void downloadBox(@NonNull final Context context, @NonNull final BoundingBox mapBox, final boolean add,
            @Nullable final PostAsyncActionHandler postLoadHandler) {
        final Validator validator = App.getDefaultValidator(context);

        final PostMergeHandler postMerge = (OsmElement e) -> e.hasProblem(context, validator);

        new ExecutorTask<Boolean, Void, AsyncResult>(executorService, uiHandler) {

            boolean hasActivity = context instanceof FragmentActivity;

            @Override
            protected void onPreExecute() {
                if (hasActivity) {
                    Progress.showDialog((FragmentActivity) context, Progress.PROGRESS_DOWNLOAD);
                }
            }

            @Override
            protected AsyncResult doInBackground(Boolean arg) {
                boolean merge = arg != null && arg.booleanValue();
                Server server = prefs.getServer();
                mapBox.makeValidForApi(server.getCachedCapabilities().getMaxArea());
                return download(context, server, mapBox, postMerge, null, merge, false);
            }

            @Override
            protected void onPostExecute(AsyncResult result) {
                if (hasActivity) {
                    Progress.dismissDialog((FragmentActivity) context, Progress.PROGRESS_DOWNLOAD);
                }
                Map mainMap = context instanceof Main ? ((Main) context).getMap() : null;
                if (mainMap != null) {
                    try {
                        viewBox.setRatio(mainMap, (float) mainMap.getWidth() / (float) mainMap.getHeight());
                    } catch (OsmException e) {
                        Log.d(DEBUG_TAG, "downloadBox got " + e.getMessage());
                    }
                }
                int code = result.getCode();
                if (code != 0) {
                    if (code == ErrorCodes.OUT_OF_MEMORY && getDelegator().isDirty()) {
                        result = new AsyncResult(ErrorCodes.OUT_OF_MEMORY_DIRTY);
                    }
                    try {
                        if (hasActivity && !((FragmentActivity) context).isFinishing()) {
                            ErrorAlert.showDialog((FragmentActivity) context, result);
                        }
                    } catch (Exception ex) { // now and then this seems to throw a WindowManager.BadTokenException,
                                             // however report, don't crash
                        ACRAHelper.nocrashReport(ex, ex.getMessage());
                    }
                    if (postLoadHandler != null) {
                        postLoadHandler.onError(result);
                    }
                } else {
                    if (postLoadHandler != null) {
                        postLoadHandler.onSuccess();
                    }
                }
                if (mainMap != null) {
                    mainMap.getDataStyle().updateStrokes(strokeWidth(viewBox.getWidth()));
                    // this is always a manual download so make the layer visible
                    de.blau.android.layer.data.MapOverlay<OsmElement> dataLayer = mainMap.getDataLayer();
                    if (dataLayer != null) {
                        dataLayer.setVisible(true);
                    }
                    invalidateMap();
                }
                if (hasActivity) {
                    ((FragmentActivity) context).invalidateOptionsMenu();
                }
            }
        }.execute(add);
    }

    /**
     * Remove a BoundingBox from the list held by the StorageDelegator
     * 
     * @param mapBox the BoundingBox to remove
     */
    public void removeBoundingBox(@Nullable final BoundingBox mapBox) {
        if (mapBox != null) {
            getDelegator().deleteBoundingBox(mapBox);
        }
    }

    /**
     * Loads the area defined by mapBox from the OSM-Server.
     * 
     * Will prune storage if the node count goes too high
     * 
     * @param context android context
     * @param server the Server object we are using
     * @param validator the Validator to apply to downloaded data
     * @param mapBox Box defining the area to be loaded.
     * @param handler listener to call when the download is completed
     */
    public void autoDownloadBox(@NonNull final Context context, @NonNull final Server server, @NonNull final Validator validator,
            @NonNull final BoundingBox mapBox, @Nullable PostAsyncActionHandler handler) {

        final PostMergeHandler postMerge = (OsmElement e) -> e.hasProblem(context, validator);

        new ExecutorTask<Void, Void, AsyncResult>(executorService, uiHandler) {
            @Override
            protected AsyncResult doInBackground(Void arg) {
                Server server = prefs.getServer();
                mapBox.makeValidForApi(server.getCachedCapabilities().getMaxArea());
                AsyncResult result = download(context, server, mapBox, postMerge, handler, true, true);
                if (prefs.autoPrune() && getDelegator().reachedPruneLimits(prefs.getAutoPruneNodeLimit(), prefs.getAutoPruneBoundingBoxLimit())) {
                    ViewBox pruneBox = new ViewBox(map.getViewBox());
                    pruneBox.scale(1.6);
                    getDelegator().prune(pruneBox);
                }
                return result;
            }
        }.execute();
    }

    /**
     * Download/Load a bounding box full of OSM data
     * 
     * @param ctx an Android Context
     * @param server the API Server configuration
     * @param mapBox the BoundingBox
     * @param postMerge handler to call after merging
     * @param handler handler to call when everything is finished
     * @param merge if true merge the data with existing data, if false replace
     * @param background this is being called in the background and shouldn't do any thing that effects the UI
     * @return a ReadAsyncResult with detailed result information
     */
    public AsyncResult download(@NonNull final Context ctx, @NonNull Server server, @NonNull final BoundingBox mapBox,
            @Nullable final PostMergeHandler postMerge, @Nullable final PostAsyncActionHandler handler, boolean merge, boolean background) {
        AsyncResult result = new AsyncResult(ErrorCodes.OK);
        try {
            if (!background) {
                if (server.hasReadOnly()) {
                    if (server.hasMapSplitSource()) {
                        if (!MapSplitSource.intersects(server.getMapSplitSource(), mapBox)) {
                            return new AsyncResult(ErrorCodes.NO_DATA);
                        }
                    } else {
                        server.getReadOnlyCapabilities();
                        if (!(server.readOnlyApiAvailable() && server.readOnlyReadableDB())) {
                            return new AsyncResult(ErrorCodes.API_OFFLINE);
                        }
                        server.getCapabilities();
                    }
                } else {
                    server.getCapabilities();
                    if (!(server.apiAvailable() && server.readableDB())) {
                        return new AsyncResult(ErrorCodes.API_OFFLINE);
                    }
                }
            }

            Storage input = null;
            if (server.hasMapSplitSource()) {
                input = MapSplitSource.readBox(ctx, server.getMapSplitSource(), mapBox);
            } else {
                try (InputStream in = server.getStreamForBox(ctx, mapBox)) {
                    final OsmParser osmParser = new OsmParser();
                    osmParser.start(in);
                    input = osmParser.getStorage();
                }
            }

            if (merge) { // incremental load
                getDelegator().mergeData(input, postMerge);
                if (mapBox != null) {
                    getDelegator().mergeBoundingBox(mapBox);
                }
            } else { // replace data with new download
                getDelegator().reset(false);
                getDelegator().setCurrentStorage(input); // this sets dirty flag
                if (mapBox != null) {
                    Log.d(DEBUG_TAG, "downloadBox setting original bbox");
                    getDelegator().setOriginalBox(mapBox);
                }
            }
            if (!background) {
                // Main maybe not available and by extension there may be no valid Map object
                Map currentMap = ctx instanceof Main ? ((Main) ctx).getMap() : null;
                if (currentMap != null) {
                    // set to current or previous
                    viewBox.fitToBoundingBox(currentMap, mapBox != null ? mapBox : getDelegator().getLastBox());
                }
            }
            if (handler != null) {
                handler.onSuccess();
            }
        } catch (SAXException e) {
            Exception ce = e.getException();
            if ((ce instanceof StorageException) && ((StorageException) ce).getCode() == StorageException.OOM) {
                result = new AsyncResult(ErrorCodes.OUT_OF_MEMORY);
            } else {
                result = new AsyncResult(ErrorCodes.INVALID_DATA_RECEIVED, e.getMessage());
            }
        } catch (ParserConfigurationException | UnsupportedFormatException e) {
            result = new AsyncResult(ErrorCodes.INVALID_DATA_RECEIVED, e.getMessage());
        } catch (OsmServerException e) {
            switch (e.getHttpErrorCode()) {
            case HttpURLConnection.HTTP_BAD_REQUEST:
                // check error messages
                Matcher m = Server.ERROR_MESSAGE_BAD_OAUTH_REQUEST.matcher(e.getMessage());
                if (m.matches()) {
                    result = new AsyncResult(ErrorCodes.INVALID_LOGIN);
                } else {
                    result = new AsyncResult(ErrorCodes.BOUNDING_BOX_TOO_LARGE);
                }
                break;
            case HttpStatusCodes.HTTP_TOO_MANY_REQUESTS:
            case HttpStatusCodes.HTTP_BANDWIDTH_LIMIT_EXCEEDED:
                result = new AsyncResult(ErrorCodes.DOWNLOAD_LIMIT_EXCEEDED);
                break;
            default:
                result = new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessage());
            }
        } catch (StorageException sex) {
            result = new AsyncResult(ErrorCodes.OUT_OF_MEMORY);
        } catch (DataConflictException dce) {
            result = new AsyncResult(ErrorCodes.DATA_CONFLICT);
        } catch (SSLProtocolException e) {
            result = new AsyncResult(ErrorCodes.SSL_HANDSHAKE);
        } catch (IOException e) {
            result = new AsyncResult(ErrorCodes.NO_CONNECTION);
        } catch (IllegalStateException iex) {
            result = new AsyncResult(ErrorCodes.CORRUPTED_DATA);
        } catch (Exception e) {
            result = new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessage());
        }
        if (result.getCode() != ErrorCodes.OK) {
            removeBoundingBox(mapBox);
            if (handler != null) {
                handler.onError(result);
            }
            Log.e(DEBUG_TAG, "downloadBox problem downloading " + result.getClass() + " " + result.getMessage());
        }
        return result;
    }

    /**
     * Re-downloads the same areas that we already have
     * 
     * @param activity activity this was called from
     * @param reset storage before reloading if true, discards any changes! If false this updates the unchanged data.
     * @param postLoadHandler handler to run once download is complete
     * 
     * @see #downloadBox(activity, BoundingBox, boolean)
     */
    void redownload(@NonNull final FragmentActivity activity, boolean reset, @Nullable PostAsyncActionHandler postLoadHandler) {
        List<BoundingBox> boxes = new ArrayList<>(getDelegator().getBoundingBoxes());
        if (reset) {
            getDelegator().reset(false);
        } else {
            getDelegator().pruneAll();
            getDelegator().getCurrentStorage().clearBoundingBoxList();
        }
        final Validator validator = App.getDefaultValidator(activity);
        final PostMergeHandler postMerge = (OsmElement e) -> e.hasProblem(activity, validator);
        new ExecutorTask<Void, Void, AsyncResult>(executorService, uiHandler) {
            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_DOWNLOAD);
            }

            @Override
            protected AsyncResult doInBackground(Void arg) {
                Server server = prefs.getServer();
                final float maxArea = server.getCachedCapabilities().getMaxArea();
                for (BoundingBox box : boxes) {
                    if (box != null && box.isValidForApi(maxArea)) {
                        AsyncResult result = download(activity, server, box, postMerge, null, true, true);
                        if (result.getCode() != 0) {
                            return result;
                        }
                    }
                }
                return new AsyncResult(ErrorCodes.OK, null);
            }

            @Override
            protected void onPostExecute(AsyncResult result) {
                Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                int code = result.getCode();
                if (code == 0) {
                    if (postLoadHandler != null) {
                        postLoadHandler.onSuccess();
                    }
                    return;
                }
                try {
                    if (!activity.isFinishing()) {
                        ErrorAlert.showDialog(activity, result);
                    }
                } catch (Exception ex) { // now and then this seems to throw a WindowManager.BadTokenException,
                                         // however report, don't crash
                    ACRAHelper.nocrashReport(ex, ex.getMessage());
                }
                for (BoundingBox box : boxes) { // recreate the boundingbox list
                    getDelegator().addBoundingBox(box);
                }
                if (postLoadHandler != null) {
                    postLoadHandler.onError(null);
                }
            }
        }.execute();
    }

    /**
     * Return a single element from the API, does not merge into storage
     * 
     * Note: currently doesn't check if the API is available or not
     * 
     * @param activity the activity that called us
     * @param type type of the element
     * @param id id of the element
     * @return element if successful, null if not
     */
    @Nullable
    public OsmElement getElement(@Nullable final Activity activity, final String type, final long id) {

        class GetElementTask extends ExecutorTask<Void, Void, OsmElement> {

            /**
             * Create a new GetElementTask
             * 
             * @param executorService ExecutorService to run this on
             * @param handler an Handler
             */
            protected GetElementTask(@NonNull ExecutorService executorService, @NonNull Handler handler) {
                super(executorService, handler);
            }

            @Override
            protected OsmElement doInBackground(Void arg) {
                final OsmParser osmParser = new OsmParser();
                if (downloadElement(activity, type, id, false, false, osmParser) == ErrorCodes.OK) {
                    return osmParser.getStorage().getOsmElement(type, id);
                }
                return null;
            }
        }
        GetElementTask loader = new GetElementTask(executorService, uiHandler);
        loader.execute();

        try {
            return loader.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR cancel does interrupt the
                                                                                   // thread in question
            loader.cancel();
            return null;
        }
    }

    /**
     * Return a single, possibly deleted, element from the API, does not merge into storage
     * 
     * Note: currently doesn't check if the API is available or not
     * 
     * @param ctx an Android Context
     * @param type type of the element
     * @param id id of the element
     * @return element if successful, null if not
     * @throws OsmServerException if something goes wrong
     */
    @Nullable
    public OsmElement getElementWithDeleted(@Nullable final Context ctx, final String type, final long id) throws OsmServerException {

        ExecutorTask<Void, Void, OsmElement> loader = new ExecutorTask<Void, Void, OsmElement>(executorService, uiHandler) {
            @Override
            protected OsmElement doInBackground(Void arg) throws SAXException, IOException, ParserConfigurationException {
                try {
                    final Server server = getPrefs().getServer();
                    final OsmParser osmParser = new OsmParser(true);
                    final Storage storage = multiFetch(ctx, server, osmParser, type, new long[] { id });
                    OsmElement result = storage.getOsmElement(type, id);
                    if (!Way.NAME.equals(type)) {
                        return result;
                    }
                    downloadMissingWayNodes(ctx, server, osmParser, result);
                    return result;
                } catch (SAXException ex) {
                    Log.e(DEBUG_TAG, "getElementWithDeleted problem parsing", ex);
                    throw checkSAXException(ex);
                } catch (ParserConfigurationException ex) {
                    Log.e(DEBUG_TAG, "getElementWithDeleted problem with parser", ex);
                    throw new OsmServerException(ErrorCodes.INVALID_DATA_RECEIVED, ex.getLocalizedMessage());
                } catch (IOException ex) {
                    Log.e(DEBUG_TAG, "getElementWithDeleted no connection", ex);
                    throw new OsmServerException(ErrorCodes.NO_CONNECTION, ex.getLocalizedMessage());
                }
            }
        };
        loader.execute();

        try {
            return loader.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            // cancel does interrupt the thread in question
            loader.cancel();
            throw new OsmServerException(ErrorCodes.NO_CONNECTION, e.getLocalizedMessage());
        }
    }

    /**
     * Check what caused the SAXException
     * 
     * @param ex the SAXException
     * @return an appropriate OsmServerException
     */
    @NonNull
    private OsmServerException checkSAXException(@NonNull SAXException ex) {
        Exception ce = ex.getException();
        if ((ce instanceof StorageException) && ((StorageException) ce).getCode() == StorageException.OOM) {
            return new OsmServerException(ErrorCodes.OUT_OF_MEMORY, ce.getLocalizedMessage());
        }
        return new OsmServerException(ErrorCodes.INVALID_DATA_RECEIVED, ex.getLocalizedMessage());
    }

    /**
     * Return multiple, possibly deleted, elements from the API, does not merge into main storage
     * 
     * Note: currently doesn't check if the API is available or not
     * 
     * @param ctx an Android Context
     * @param type type of elements to get
     * @param ids array elements ids
     * @return a Storage object holding all the elements
     * @throws OsmServerException if something goes wrong
     */
    @NonNull
    public Storage getElementsWithDeleted(@Nullable final Context ctx, @NonNull String type, @NonNull long[] ids) throws OsmServerException {

        ExecutorTask<Void, Void, Storage> loader = new ExecutorTask<Void, Void, Storage>(executorService, uiHandler) {

            @Override
            protected Storage doInBackground(Void arg) throws OsmServerException {
                final Server server = getPrefs().getServer();
                final OsmParser osmParser = new OsmParser(true);
                try {
                    Storage storage = multiFetch(ctx, server, osmParser, type, ids);
                    if (Way.NAME.equals(type)) {
                        for (long wayId : ids) {
                            OsmElement way = storage.getOsmElement(Way.NAME, wayId);
                            downloadMissingWayNodes(ctx, server, osmParser, way);
                        }
                    }
                    return storage;
                } catch (SAXException ex) {
                    Log.e(DEBUG_TAG, "getElementsWithDeleted problem parsing", ex);
                    throw checkSAXException(ex);
                } catch (ParserConfigurationException ex) {
                    Log.e(DEBUG_TAG, "getElementsWithDeleted problem with parser", ex);
                    throw new OsmServerException(ErrorCodes.INVALID_DATA_RECEIVED, ex.getLocalizedMessage());
                } catch (IOException ex) {
                    Log.e(DEBUG_TAG, "getElementsWithDeleted no connection", ex);
                    throw new OsmServerException(ErrorCodes.NO_CONNECTION, ex.getLocalizedMessage());
                } catch (Exception ex) {
                    throw ex;
                }
            }
        };
        loader.execute();
        try {
            return loader.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
            // cancel does interrupt the thread in question
            loader.cancel();
            throw new OsmServerException(ErrorCodes.NO_CONNECTION, e.getLocalizedMessage());
        }
    }

    /**
     * Use the multi-fetch API to retrieve multiple elements taking URL length in to account
     * 
     * @param ctx an Android COntext
     * @param server the current Server instance
     * @param osmParser an OsmParser instance
     * @param type the type of element to retrieve
     * @param ids an array holding the element ids
     * @return a Storage instance holding the elements
     * @throws SAXException parsing error
     * @throws IOException if we can't download the nodes
     * @throws ParserConfigurationException parsing error
     */
    @NonNull
    private Storage multiFetch(@NonNull final Context ctx, @NonNull final Server server, @NonNull OsmParser osmParser, @NonNull String type,
            @NonNull long[] ids) throws SAXException, IOException, ParserConfigurationException {
        int end = 0;
        for (int start = 0; start < Math.min(start + Server.MULTI_FETCH_MAX_ELEMENTS, ids.length); start = end) {
            end = start + Math.min(Server.MULTI_FETCH_MAX_ELEMENTS, ids.length - start);
            try (InputStream in = server.getStreamForElements(ctx, type, Arrays.copyOfRange(ids, start, end))) {
                osmParser.reinit();
                osmParser.start(in);
            }
        }
        return osmParser.getStorage();
    }

    /**
     * Fixup missing way nodes
     * 
     * @param ctx an Android Context
     * @param server the current server
     * @param osmParser an OsmParser
     * @param way the way we ant to get the nodes for
     * @throws SAXException parsing error
     * @throws IOException if we can't download the nodes
     * @throws ParserConfigurationException parsing error
     */
    private void downloadMissingWayNodes(@NonNull final Context ctx, @NonNull final Server server, @NonNull final OsmParser osmParser, @Nullable OsmElement way)
            throws SAXException, IOException, ParserConfigurationException {
        // as the API doesn't return way nodes for this call we need to patch things up here
        if (way == null) {
            throw new OsmServerException(ErrorCodes.NOT_FOUND, "downloadMissingWayNodes null way");
        }
        if (way.getState() == OsmElement.STATE_DELETED) {
            return; // no nodes
        }

        List<Node> tempNodes = ((Way) way).getNodes();
        long[] realNodes = new long[tempNodes.size()];
        for (int i = 0; i < realNodes.length; i++) {
            realNodes[i] = tempNodes.get(i).getOsmId();
        }
        Storage storage = multiFetch(ctx, server, osmParser, Node.NAME, realNodes);

        tempNodes.clear();
        for (long id : realNodes) {
            final Node realNode = (Node) storage.getOsmElement(Node.NAME, id);
            if (realNode != null) {
                tempNodes.add(realNode);
            } else {
                Log.e(DEBUG_TAG, "getElementWithDeleted unable to replace node " + id);
            }
        }
    }

    /**
     * Download a single element from the API and merge
     * 
     * Note: currently doesn't check if the API is available or not
     * 
     * @param ctx Android context
     * @param type type of the element
     * @param id OSM id of the element
     * @param relationFull if we are downloading a relation download with full option
     * @param withParents download parent relations and ways
     * @param postLoadHandler callback to execute after download completes if null method waits for download to finish
     * @return an error code, 0 for success
     */
    public synchronized int downloadElement(@Nullable final Context ctx, @NonNull final String type, final long id, final boolean relationFull,
            final boolean withParents, @Nullable final PostAsyncActionHandler postLoadHandler) {
        ExecutorTask<Void, Void, Integer> loader = new ExecutorTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void arg) {
                final OsmParser osmParser = new OsmParser();
                int result = downloadElement(ctx, type, id, relationFull, withParents, osmParser);
                if (result == ErrorCodes.OK) {
                    try {
                        getDelegator().mergeData(osmParser.getStorage(), null);
                    } catch (DataConflictException dcex) {
                        result = ErrorCodes.DATA_CONFLICT;
                    } catch (IllegalStateException iex) {
                        result = ErrorCodes.CORRUPTED_DATA;
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (postLoadHandler == null) {
                    return;
                }
                if (result == ErrorCodes.OK) {
                    postLoadHandler.onSuccess();
                    return;
                }
                postLoadHandler.onError(null);
            }
        };

        loader.execute();

        if (postLoadHandler == null) {
            try {
                return loader.get(20, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR cancel does interrupt
                                                                                       // the thread in question
                loader.cancel();
                return -1;
            }
        }
        return 0;
    }

    /**
     * Download an element from the OSM API
     * 
     * @param ctx optional Android Context
     * @param type type the element type ("node", "way", "relation")
     * @param id the OSM id
     * @param relationFull if type is "relation" then include member elements
     * @param withParents include relations the element is a member of and for nodes, parent ways
     * @param osmParser the OsmParser instance that will hold the download result
     * @return an error code or 0 for success
     */
    private int downloadElement(@Nullable final Context ctx, @NonNull final String type, final long id, final boolean relationFull, final boolean withParents,
            @NonNull final OsmParser osmParser) {
        int result = ErrorCodes.OK;
        try {
            final Server server = getPrefs().getServer();

            // we always retrieve ways with nodes, relations "full" is optional
            try (InputStream in = server.getStreamForElement(ctx,
                    (Relation.NAME.equals(type) && relationFull) || Way.NAME.equals(type) ? Server.MODE_FULL : null, type, id)) {
                osmParser.start(in);
            }
            if (withParents) {
                if (Node.NAME.equals(type)) {
                    try (InputStream in = server.getStreamForElement(ctx, Server.MODE_WAYS, type, id)) {
                        osmParser.reinit();
                        osmParser.start(in);
                    }
                }
                // optional retrieve relations the element is a member of and for nodes, parent ways
                try (InputStream in = server.getStreamForElement(ctx, Server.MODE_RELATIONS, type, id)) {
                    osmParser.reinit();
                    osmParser.start(in);
                }
            }
        } catch (SAXException e) {
            Log.e(DEBUG_TAG, "downloadElement problem parsing", e);
            Exception ce = e.getException();
            if ((ce instanceof StorageException) && ((StorageException) ce).getCode() == StorageException.OOM) {
                result = ErrorCodes.OUT_OF_MEMORY;
            } else {
                result = ErrorCodes.INVALID_DATA_RECEIVED;
            }
        } catch (ParserConfigurationException e) {
            Log.e(DEBUG_TAG, "downloadElement problem with parser", e);
            result = ErrorCodes.INVALID_DATA_RECEIVED;
        } catch (OsmServerException e) {
            result = e.getHttpErrorCode();
            Log.e(DEBUG_TAG, "downloadElement problem downloading", e);
        } catch (IOException e) {
            result = ErrorCodes.NO_CONNECTION;
            Log.e(DEBUG_TAG, "downloadElement no connection", e);
        }
        return result;
    }

    /**
     * Convert a List&lt;Long&gt; to an array of long
     * 
     * @param list the List of Long
     * @return an array holding the corresponding long values
     */
    @NonNull
    private long[] toLongArray(@NonNull List<Long> list) {
        long[] array = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    /**
     * Return multiple elements from the API and merge them in to our data
     * 
     * Note: currently doesn't check if the API is available or not
     * 
     * @param ctx Android context
     * @param nodes List containing the node ids
     * @param ways List containing the way ids
     * @param relations List containing the relation ids
     * @param postLoadHandler callback to execute after download completes if null method waits for download to finish
     * @return a ReadAsyncResult
     */
    public synchronized AsyncResult downloadElements(@NonNull final Context ctx, @Nullable final List<Long> nodes, @Nullable final List<Long> ways,
            @Nullable final List<Long> relations, @Nullable final PostAsyncActionHandler postLoadHandler) {

        class DownLoadElementsTask extends ExecutorTask<Void, Void, AsyncResult> {

            /**
             * Create a new DownLoadElementsTask
             * 
             * @param executorService ExecutorService to run this on
             * @param handler an Handler
             */
            protected DownLoadElementsTask(@NonNull ExecutorService executorService, @NonNull Handler handler) {
                super(executorService, handler);
            }

            @Override
            protected AsyncResult doInBackground(Void arg) {
                try {
                    final OsmParser osmParser = new OsmParser(true);
                    Server server = getPrefs().getServer();
                    if (nodes != null && !nodes.isEmpty()) {
                        multiFetch(ctx, server, osmParser, Node.NAME, toLongArray(nodes));
                    }
                    if (ways != null && !ways.isEmpty()) {
                        final long[] wayIds = toLongArray(ways);
                        Storage storage = multiFetch(ctx, server, osmParser, Way.NAME, wayIds);
                        for (long wayId : wayIds) {
                            OsmElement way = storage.getOsmElement(Way.NAME, wayId);
                            downloadMissingWayNodes(ctx, server, osmParser, way);
                        }
                    }
                    if (relations != null && !relations.isEmpty()) {
                        multiFetch(ctx, server, osmParser, Relation.NAME, toLongArray(relations));
                    }
                    getDelegator().mergeData(osmParser.getStorage(), null);
                } catch (IllegalStateException iex) {
                    return new AsyncResult(ErrorCodes.CORRUPTED_DATA);
                } catch (SAXException e) {
                    Log.e(DEBUG_TAG, "downloadElement problem parsing", e);
                    Exception ce = e.getException();
                    if ((ce instanceof StorageException) && ((StorageException) ce).getCode() == StorageException.OOM) {
                        return new AsyncResult(ErrorCodes.OUT_OF_MEMORY);
                    }
                    return new AsyncResult(ErrorCodes.INVALID_DATA_RECEIVED);
                } catch (ParserConfigurationException e) {
                    Log.e(DEBUG_TAG, "downloadElements problem parsing", e);
                    return new AsyncResult(ErrorCodes.INVALID_DATA_RECEIVED);
                } catch (OsmServerException e) {
                    Log.e(DEBUG_TAG, "downloadElements problem downloading", e);
                    return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessageWithDescription());
                } catch (DataConflictException dce) {
                    return new AsyncResult(ErrorCodes.DATA_CONFLICT);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "downloadElements problem downloading", e);
                    return new AsyncResult(ErrorCodes.NO_CONNECTION);
                }
                return null;
            }

            @Override
            protected void onPostExecute(AsyncResult result) {
                if (result == null) {
                    if (postLoadHandler != null) {
                        postLoadHandler.onSuccess();
                    }
                } else {
                    if (postLoadHandler != null) {
                        postLoadHandler.onError(result);
                    }
                }
            }

        }
        DownLoadElementsTask loader = new DownLoadElementsTask(executorService, uiHandler);
        loader.execute();

        if (postLoadHandler == null) {
            try {
                return loader.get(20, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR cancel does interrupt
                                                                                       // the thread in question
                loader.cancel();
                return new AsyncResult(ErrorCodes.NO_CONNECTION);
            }
        } else {
            return new AsyncResult(ErrorCodes.OK);
        }
    }

    /**
     * Remove an element if it is deleted on the server
     * 
     * Element is deleted on server, delete locally but don't upload A bit iffy because of memberships in other objects
     * 
     * @param activity activity we were called from
     * @param e element to delete
     * @param createCheckpoint create an undo checkpoint if true
     */
    public synchronized void updateToDeleted(@Nullable Activity activity, @NonNull OsmElement e, boolean createCheckpoint) {
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_fix_conflict);
        }
        if (e.getName().equals(Node.NAME)) {
            getDelegator().removeNode((Node) e);
        } else if (e.getName().equals(Way.NAME)) {
            getDelegator().removeWay((Way) e);
        } else if (e.getName().equals(Relation.NAME)) {
            getDelegator().removeRelation((Relation) e);
        }
        getDelegator().removeFromUpload(e, OsmElement.STATE_DELETED);
    }

    /**
     * Replace an element by a downloaded one with a higher version
     * 
     * @param activity activity we were called from
     * @param e the element to replace
     * @param postLoad code to run once we've finished
     */
    public synchronized void replaceElement(@Nullable Activity activity, @NonNull OsmElement e, @Nullable PostAsyncActionHandler postLoad) {
        createCheckpoint(activity, R.string.undo_action_fix_conflict);
        getDelegator().removeFromUpload(e, OsmElement.STATE_UNCHANGED); // this will allow merging to replace it
        downloadElement(activity, e.getName(), e.getOsmId(), false, true, postLoad);
    }

    /**
     * Read a file in (J)OSM format from device
     * 
     * @param activity activity that called this
     * @param uri uri of file to load
     * @param add unused currently
     * @throws FileNotFoundException when the selected file could not be found
     */
    public void readOsmFile(@NonNull final FragmentActivity activity, final Uri uri, boolean add) throws FileNotFoundException {
        readOsmFile(activity, uri, add, null);
    }

    /**
     * Read a file in (J)OSM format from device
     * 
     * @param activity activity that called this
     * @param uri uri of file to load
     * @param add unused currently
     * @param postLoad callback to execute once file is loaded
     * @throws FileNotFoundException when the selected file could not be found
     */
    public void readOsmFile(@NonNull final FragmentActivity activity, final Uri uri, boolean add, final PostAsyncActionHandler postLoad)
            throws FileNotFoundException {
        final InputStream is = activity.getContentResolver().openInputStream(uri);
        readOsmFile(activity, is, add, postLoad);
    }

    /**
     * Read a stream in (J)OSM format
     * 
     * @param context Android Context
     * @param is input
     * @param add unused currently (if there are new objects in the file they could potentially conflict with in memory
     *            ones)
     * @param postLoad callback to execute once stream has been loaded
     * @throws FileNotFoundException when the selected file could not be found
     */
    public void readOsmFile(@NonNull final Context context, @NonNull final InputStream is, boolean add, @Nullable final PostAsyncActionHandler postLoad) {

        new ReadAsyncClass(executorService, uiHandler, context, is, false, postLoad) {
            @Override
            protected AsyncResult doInBackground(Boolean arg) {
                try {
                    final OsmParser osmParser = new OsmParser();
                    osmParser.clearBoundingBoxes(); // this removes the default bounding box
                    try (final InputStream in = new BufferedInputStream(is)) {
                        osmParser.start(in);
                        StorageDelegator sd = getDelegator();
                        sd.reset(false);
                        sd.setCurrentStorage(osmParser.getStorage()); // this sets dirty flag
                        sd.fixupApiStorage();
                        if (!add && sd.getBoundingBoxes().isEmpty()) {
                            // ensure a valid bounding box
                            sd.addBoundingBox(sd.getCurrentStorage().calcBoundingBoxFromData());
                        }
                        if (map != null) {
                            viewBox.fitToBoundingBox(map, sd.getLastBox()); // set to current or previous
                        }
                    }
                } catch (SAXException e) {
                    Log.e(DEBUG_TAG, "Problem parsing ", e);
                    Exception ce = e.getException();
                    if (ce instanceof StorageException) {
                        return new AsyncResult(ErrorCodes.OUT_OF_MEMORY, ce.getMessage());
                    }
                    return new AsyncResult(ErrorCodes.INVALID_DATA_READ, e.getMessage());
                } catch (StorageException sex) {
                    return new AsyncResult(ErrorCodes.OUT_OF_MEMORY, sex.getMessage());
                } catch (ParserConfigurationException e) {
                    Log.e(DEBUG_TAG, "Problem parsing", e);
                    return new AsyncResult(ErrorCodes.INVALID_DATA_READ, e.getMessage());
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Problem reading", e);
                    return new AsyncResult(ErrorCodes.INVALID_DATA_READ, e.getMessage());
                }
                return new AsyncResult(ErrorCodes.OK, null);
            }
        }.execute(add);
    }

    /**
     * Write data to a file in (J)OSM compatible format, if fileName contains directories these are created, otherwise
     * it is stored in the standard public dir
     * 
     * @param activity the calling FragmentActivity
     * @param fileName path of the file to save to
     * @param postSaveHandler if not null executes code after saving
     */
    public void writeOsmFile(@NonNull final FragmentActivity activity, @NonNull final String fileName, @Nullable final PostAsyncActionHandler postSaveHandler) {
        try {
            File outfile = FileUtil.openFileForWriting(activity, fileName);
            Log.d(DEBUG_TAG, "Saving to " + outfile.getPath());
            writeOsmFile(activity, new FileOutputStream(outfile), postSaveHandler);
        } catch (IOException e) {
            if (!activity.isFinishing()) {
                ErrorAlert.showDialog(activity, ErrorCodes.FILE_WRITE_FAILED);
            }
            if (postSaveHandler != null) {
                postSaveHandler.onError(null);
            }
        }
    }

    /**
     * Write data to an URI in (J)OSM compatible format¨
     * 
     * @param activity the calling FragmentActivity
     * @param uri URI to save to
     * @param postSaveHandler if not null executes code after saving
     */
    public void writeOsmFile(@NonNull final FragmentActivity activity, @NonNull final Uri uri, @Nullable final PostAsyncActionHandler postSaveHandler) {
        try {
            writeOsmFile(activity, activity.getContentResolver().openOutputStream(uri, FileUtil.TRUNCATE_WRITE_MODE), postSaveHandler);
        } catch (IOException e) {
            if (!activity.isFinishing()) {
                ErrorAlert.showDialog(activity, ErrorCodes.FILE_WRITE_FAILED);
            }
            if (postSaveHandler != null) {
                postSaveHandler.onError(null);
            }
        }
    }

    /**
     * Asynchronously write data to an OutputStream in (J)OSM compatible format
     * 
     * @param activity the calling FragmentActivity
     * @param fout OutputStream to write to, note: do not close in caller
     * @param postSaveHandler if not null executes code after saving
     */
    private void writeOsmFile(@NonNull final FragmentActivity activity, @NonNull final OutputStream fout,
            @Nullable final PostAsyncActionHandler postSaveHandler) {

        new ExecutorTask<Void, Void, Integer>(executorService, uiHandler) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_SAVING);
            }

            @Override
            protected Integer doInBackground(Void arg) {
                int result = 0;
                try (OutputStream out = new BufferedOutputStream(fout)) {
                    OsmXml.write(getDelegator().getCurrentStorage(), getDelegator().getApiStorage(), out, App.getUserAgent());
                } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException | IOException e) {
                    result = ErrorCodes.FILE_WRITE_FAILED;
                    Log.e(DEBUG_TAG, "Problem writing", e);
                } finally {
                    SavingHelper.close(fout);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                Progress.dismissDialog(activity, Progress.PROGRESS_SAVING);
                Map mainMap = activity instanceof Main ? ((Main) activity).getMap() : null;
                if (mainMap != null) {
                    try {
                        viewBox.setRatio(mainMap, (float) mainMap.getWidth() / (float) mainMap.getHeight());
                    } catch (OsmException e) {
                        Log.d(DEBUG_TAG, "writeOsmFile got " + e.getMessage());
                    }
                }
                if (result != 0) {
                    if (result == ErrorCodes.OUT_OF_MEMORY && getDelegator().isDirty()) {
                        result = ErrorCodes.OUT_OF_MEMORY_DIRTY;
                    }
                    if (!activity.isFinishing()) {
                        ErrorAlert.showDialog(activity, result);
                    }
                    if (postSaveHandler != null) {
                        postSaveHandler.onError(null);
                    }
                } else {
                    if (postSaveHandler != null) {
                        postSaveHandler.onSuccess();
                    }
                }
            }
        }.execute();
    }

    /**
     * Read a stream in PBF format
     * 
     * @param activity activity that called this
     * @param uri the file uri
     * @param add unused currently (if there are new objects in the file they could potentially conflict with in memory
     *            ones)
     * @throws FileNotFoundException when the selected file could not be found
     */
    public void readPbfFile(@NonNull final FragmentActivity activity, @NonNull Uri uri, boolean add) throws FileNotFoundException {
        final InputStream is = activity.getContentResolver().openInputStream(uri);
        readPbfFile(activity, is, add, null);
    }

    /**
     * Read a stream in PBF format
     * 
     * @param activity activity that called this
     * @param is InputStream
     * @param add unused currently (if there are new objects in the file they could potentially conflict with in memory
     *            ones)
     * @param postLoad callback to execute once stream has been loaded
     */
    private synchronized void readPbfFile(@NonNull final FragmentActivity activity, @NonNull final InputStream is, boolean add,
            @Nullable final PostAsyncActionHandler postLoad) {

        new ReadAsyncClass(executorService, uiHandler, activity, is, add, postLoad) {
            @Override
            protected AsyncResult doInBackground(Boolean arg) {
                synchronized (Logic.this) {
                    try {
                        Storage storage = new Storage();
                        try {
                            BlockReaderAdapter opp = new OsmPbfParser(storage);
                            new BlockInputStream(is, opp).process();
                            StorageDelegator sd = getDelegator();
                            sd.reset(false);
                            sd.setCurrentStorage(storage); // this sets dirty flag
                            sd.fixupApiStorage();
                            if (map != null) {
                                viewBox.fitToBoundingBox(map, sd.getLastBox()); // set to current or previous
                            }
                        } finally {
                            SavingHelper.close(is);
                        }
                    } catch (StorageException sex) {
                        Log.e(DEBUG_TAG, "Problem reading PBF " + sex.getMessage());
                        return new AsyncResult(ErrorCodes.OUT_OF_MEMORY, sex.getMessage());
                    } catch (IOException | RuntimeException e) {
                        Log.e(DEBUG_TAG, "Problem parsing PBF ", e);
                        return new AsyncResult(ErrorCodes.INVALID_DATA_READ, e.getMessage());
                    }
                    return new AsyncResult(ErrorCodes.OK, null);
                }
            }
        }.execute(add);
    }

    /**
     * Read an osmChange format file and then apply the contents
     * 
     * Supports OsmAnd extension for Notes
     * 
     * @param activity the calling activity
     * @param fileUri the URI for the file
     * @param postLoad a callback to call post load
     * @throws FileNotFoundException if the file coudn't be found
     */
    public void applyOscFile(@NonNull FragmentActivity activity, @NonNull Uri fileUri, @Nullable final PostAsyncActionHandler postLoad)
            throws FileNotFoundException {

        final InputStream is = activity.getContentResolver().openInputStream(fileUri);

        new ReadAsyncClass(executorService, uiHandler, activity, is, false, postLoad) {
            @Override
            protected AsyncResult doInBackground(Boolean arg) {
                synchronized (Logic.this) {
                    StorageDelegator sd = getDelegator();
                    try (final InputStream in = new BufferedInputStream(is)) {
                        OsmChangeParser oscParser = new OsmChangeParser();
                        oscParser.clearBoundingBoxes(); // this removes the default bounding box
                        oscParser.start(in);
                        createCheckpoint((FragmentActivity) context, R.string.undo_action_apply_osc);
                        if (!sd.applyOsc(oscParser.getStorage(), null)) {
                            removeCheckpoint((FragmentActivity) context, R.string.undo_action_apply_osc, true);
                            return new AsyncResult(ErrorCodes.APPLYING_OSC_FAILED);
                        }
                        if (map != null) {
                            viewBox.fitToBoundingBox(map, sd.getLastBox()); // set to current or previous
                        }
                        // support for OSMAND extension
                        List<Note> notes = oscParser.getNotes();
                        if (!notes.isEmpty()) {
                            TransferTasks.merge(context, App.getTaskStorage(), notes);
                            TransferTasks.addBoundingBoxFromData(App.getTaskStorage(), notes);
                        }
                    } catch (UnsupportedFormatException | IOException | SAXException | ParserConfigurationException e) {
                        Log.e(DEBUG_TAG, "Problem parsing OSC ", e);
                        return new AsyncResult(ErrorCodes.INVALID_DATA_READ, e.getMessage());
                    } catch (IllegalStateException iex) {
                        return new AsyncResult(ErrorCodes.CORRUPTED_DATA);
                    } catch (StorageException sex) {
                        return new AsyncResult(sd.isDirty() ? ErrorCodes.OUT_OF_MEMORY_DIRTY : ErrorCodes.OUT_OF_MEMORY);
                    } finally {
                        SavingHelper.close(is);
                    }
                    return new AsyncResult(ErrorCodes.OK, null);
                }
            }
        }.execute();
    }

    /**
     * Saves to a file (synchronously)
     * 
     * @param context an Android Context
     */
    synchronized void save(@NonNull final Context context) {
        try {
            getDelegator().writeToFile(context);
            App.getTaskStorage().writeToFile(context);
            if (map != null) {
                map.saveLayerState(context);
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Problem saving", e);
        }
    }

    /**
     * Saves to a file (asynchronously)
     * 
     * @param context context an Android Context
     */
    void saveAsync(@NonNull final Context context) {
        new ExecutorTask<Void, Void, Void>(executorService, uiHandler) {
            @Override
            protected Void doInBackground(Void params) {
                save(context);
                // the disadvantage of saving async is that something might have
                // changed during the write .... so we force the dirty flags on
                getDelegator().dirty();
                App.getTaskStorage().setDirty();
                return null;
            }
        }.execute();
    }

    /**
     * Saves the current editing state (selected objects, editing mode, etc) to file.
     * 
     * @param main the current Main instance
     */
    synchronized void saveEditingState(@NonNull Main main) {
        if (editingStateRead) {
            EditState editState = new EditState(main, this, main.getImageFileName(), viewBox, main.getFollowGPS(), prefs.getServer().getOpenChangeset());
            new SavingHelper<EditState>().save(main, EDITSTATE_FILENAME, editState, false, true);
            main.getEasyEditManager().saveState();
        } else {
            Log.w(DEBUG_TAG, "EditingState not loaded skipping save");
        }
    }

    /**
     * Loads the current editing state (selected objects, editing mode, etc) from file.
     * 
     * @param main instance of main to setup
     * @param setViewBox set the view box if true
     */
    void loadEditingState(@NonNull Main main, boolean setViewBox) {
        EditState editState = new SavingHelper<EditState>().load(main, EDITSTATE_FILENAME, false, false, true);
        if (editState != null) { //
            editState.setMiscState(main, this);
            editState.setSelected(main, this);
            if (setViewBox) {
                editState.setViewBox(this, main.getMap());
            }
            File editStateFile = main.getFileStreamPath(EDITSTATE_FILENAME);
            if (System.currentTimeMillis() - editStateFile.lastModified() > ONE_DAY_MS) {
                Log.w(DEBUG_TAG, "App hasn't been run in a long time, locking");
                main.lock();
            }
        }
        editingStateRead = true;
    }

    /**
     * Loads data from a file in the background.
     * 
     * @param activity the calling FragmentActivity
     * @param postLoad a callback to call after loading
     * 
     */
    public void loadStateFromFile(@NonNull final FragmentActivity activity, @Nullable final PostAsyncActionHandler postLoad) {

        final int READ_FAILED = 0;
        final int READ_OK = 1;
        final int READ_BACKUP = 2;

        final Map mainMap = activity instanceof Main ? ((Main) activity).getMap() : null;

        ExecutorTask<Void, Void, Integer> loader = new ExecutorTask<Void, Void, Integer>(executorService, uiHandler) {

            final AlertDialog progress = ProgressDialog.get(activity, Progress.PROGRESS_LOADING);

            @Override
            protected void onPreExecute() {
                progress.show();
                Log.d(DEBUG_TAG, "loadFromFile onPreExecute");
            }

            @Override
            protected Integer doInBackground(Void v) {
                if (getDelegator().readFromFile(activity)) {
                    setBorders(mainMap);
                    return READ_OK;
                }
                if (getDelegator().readFromFile(activity, StorageDelegator.BACKUP_FILENAME)) {
                    getDelegator().dirty(); // we need to overwrite the saved state asap
                    setBorders(mainMap);
                    return READ_BACKUP;
                }
                return READ_FAILED;
            }

            /**
             * Set the size of the ViewBox
             * 
             * @param map the Map instance
             */
            private void setBorders(@Nullable final Map map) {
                if (map != null) {
                    viewBox.setBorders(map, getDelegator().getLastBox());
                }
            }

            @Override
            protected void onPostExecute(Integer result) {
                Log.d(DEBUG_TAG, "loadFromFile onPostExecute");
                try {
                    progress.dismiss();
                } catch (Exception ex) {
                    Log.e(DEBUG_TAG, "loadFromFile dismiss dialog failed with " + ex);
                }
                if (result == READ_FAILED) {
                    Log.d(DEBUG_TAG, "loadfromFile: File read failed");
                    ScreenMessage.barError(activity, R.string.toast_data_stateload_failed);
                    if (postLoad != null) {
                        postLoad.onError(null);
                    }
                    return;
                }
                Log.d(DEBUG_TAG, "loadfromFile: File read correctly");
                if (mainMap != null) {
                    try {
                        viewBox.setRatio(mainMap, (float) mainMap.getWidth() / (float) mainMap.getHeight());
                    } catch (Exception e) {
                        // invalid dimensions or similar error
                        viewBox.setBorders(mainMap, new BoundingBox(-GeoMath.MAX_LON, -GeoMath.MAX_COMPAT_LAT, GeoMath.MAX_LON, GeoMath.MAX_COMPAT_LAT));
                    }
                    mainMap.getDataStyle().updateStrokes(STROKE_FACTOR / viewBox.getWidth()); // safety measure if not
                                                                                              // done in
                    // loadEiditngState
                    synchronized (Logic.this) {
                        loadEditingState((Main) activity, true);
                    }
                } else {
                    Log.e(DEBUG_TAG, "loadFromFile map is null");
                }

                if (postLoad != null) {
                    postLoad.onSuccess();
                }
                if (mainMap != null) {
                    invalidateMap();
                }
                // this updates the Undo icon if present
                activity.invalidateOptionsMenu();
                if (result == READ_BACKUP) {
                    ScreenMessage.barError(activity, R.string.toast_used_backup);
                }
            }
        };
        loader.execute();
    }

    /**
     * Loads the saved task state from a file in the background.
     * 
     * @param activity the activity calling this method
     * @param postLoad if not null call this after loading
     */
    void loadTasksFromFile(@NonNull final Activity activity, @Nullable final PostAsyncActionHandler postLoad) {

        final int READ_FAILED = 0;
        final int READ_OK = 1;
        final int READ_BACKUP = 2;

        ExecutorTask<Void, Void, Integer> loader = new ExecutorTask<Void, Void, Integer>(executorService, uiHandler) {

            @Override
            protected void onPreExecute() {
                Log.d(DEBUG_TAG, "loadTasksFromFile onPreExecute");
            }

            @Override
            protected Integer doInBackground(Void v) {
                if (App.getTaskStorage().readFromFile(activity)) {
                    return READ_OK;
                }
                return READ_FAILED;
            }

            @Override
            protected void onPostExecute(Integer result) {
                Log.d(DEBUG_TAG, "loadTasksFromFile onPostExecute");
                if (result != READ_FAILED) {
                    Log.d(DEBUG_TAG, "loadTasksfromFile: File read correctly");
                    if (postLoad != null) {
                        postLoad.onSuccess();
                    }
                    if (result == READ_BACKUP) {
                        ScreenMessage.barError(activity, R.string.toast_used_bug_backup);
                    }
                } else {
                    Log.d(DEBUG_TAG, "loadTasksfromFile: File read failed");
                    if (postLoad != null) {
                        postLoad.onError(null);
                    }
                }
            }
        };
        loader.execute();
    }

    /**
     * Loads the saved layer state in the background.
     * 
     * @param activity the activity calling this method
     * @param postLoad if not null call this after loading
     */
    void loadLayerState(@NonNull final Activity activity, @Nullable final PostAsyncActionHandler postLoad) {

        final int READ_FAILED = 0;
        final int READ_OK = 1;

        ExecutorTask<Void, Void, Integer> loader = new ExecutorTask<Void, Void, Integer>(executorService, uiHandler) {

            @Override
            protected void onPreExecute() {
                Log.d(DEBUG_TAG, "loadLayerState onPreExecute");
            }

            @Override
            protected Integer doInBackground(Void v) {
                boolean result = true;
                for (MapViewLayer layer : map.getLayers()) {
                    if (layer == null) {
                        continue;
                    }
                    try {
                        boolean layerResult = layer.onRestoreState(activity);
                        result = result && layerResult;
                    } catch (Exception e) {
                        // Never crash
                        Log.e(DEBUG_TAG, "loadLayerState failed for " + layer.getName() + " " + e.getMessage());
                        result = false;
                    }
                }
                return result ? READ_OK : READ_FAILED;
            }

            @Override
            protected void onPostExecute(Integer result) {
                Log.d(DEBUG_TAG, "loadLayerState onPostExecute");
                map.invalidate();
                if (postLoad == null) {
                    return;
                }
                if (result != READ_FAILED) {
                    Log.d(DEBUG_TAG, "loadLayerState: state loaded correctly");
                    postLoad.onSuccess();
                    return;
                }
                Log.d(DEBUG_TAG, "loadLayerState: state load failed");
                postLoad.onError(null);
            }
        };
        loader.execute();
    }

    /**
     * Loads data from a file
     *
     * Note that this doesn't try to read the backup state
     * 
     * @param activity the activity calling this method
     */
    public void syncLoadFromFile(@NonNull FragmentActivity activity) {

        final int READ_FAILED = 0;
        final int READ_OK = 1;

        int result = READ_FAILED;

        Map mainMap = activity instanceof Main ? ((Main) activity).getMap() : null;
        final boolean hasMap = mainMap != null;
        Progress.showDialog(activity, Progress.PROGRESS_LOADING);

        if (getDelegator().readFromFile(activity)) {
            if (hasMap) {
                viewBox.setBorders(mainMap, getDelegator().getLastBox());
            }
            result = READ_OK;
        }

        Progress.dismissDialog(activity, Progress.PROGRESS_LOADING);
        if (result != READ_FAILED) {
            Log.d(DEBUG_TAG, "syncLoadfromFile: File read correctly");
            if (hasMap) {
                try {
                    viewBox.setRatio(mainMap, (float) mainMap.getWidth() / (float) mainMap.getHeight());
                } catch (Exception e) {
                    // invalid dimensions or similar error
                    viewBox.setBorders(mainMap, new BoundingBox(-180.0, -GeoMath.MAX_COMPAT_LAT, 180.0, GeoMath.MAX_COMPAT_LAT));
                }
                mainMap.getDataStyle().updateStrokes(STROKE_FACTOR / viewBox.getWidth());
                loadEditingState((Main) activity, true);
                invalidateMap();
            }
            activity.invalidateOptionsMenu();
        } else {
            Log.d(DEBUG_TAG, "syncLoadfromFile: File read failed");
            ScreenMessage.barError(activity, R.string.toast_data_stateload_failed);
        }
    }

    /**
     * Uploads to the server in the background.
     * 
     * @param activity Activity this is called from
     * @param comment Changeset comment.
     * @param source The changeset source tag to add.
     * @param closeOpenChangeset If true try to close any open changeset first
     * @param closeChangeset Whether to close the changeset after upload or not.
     * @param extraTags Additional tags to add to changeset
     * @param elements List of OsmElement to upload if null all changed elements will be uploaded
     * @param postUploadHandler code to execute after an upload
     */
    public void upload(@NonNull final FragmentActivity activity, @Nullable final String comment, @Nullable final String source, boolean closeOpenChangeset,
            final boolean closeChangeset, @Nullable final java.util.Map<String, String> extraTags, @Nullable final List<OsmElement> elements,
            @Nullable final PostAsyncActionHandler postUploadHandler) {
        final String PROGRESS_TAG = "data";
        final Server server = prefs.getServer();
        new ExecutorTask<Void, Void, UploadResult>(executorService, uiHandler) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
                pushComment(comment, false);
                pushSource(source, false);
            }

            @Override
            protected UploadResult doInBackground(Void params) {
                UploadResult result = new UploadResult();
                try {
                    server.getCapabilities(); // update status
                    if (!(server.apiAvailable() && server.writableDB())) {
                        result.setError(ErrorCodes.API_OFFLINE);
                        return result;
                    }
                    // set comment here if empty to avoid saving it
                    getDelegator().uploadToServer(server, Util.isEmpty(comment) ? activity.getString(R.string.upload_auto_summary) : comment, source,
                            closeOpenChangeset, closeChangeset, extraTags, elements);
                } catch (final OsmServerException e) {
                    int errorCode = e.getHttpErrorCode();
                    result.setHttpError(errorCode);
                    result.setMessage(e.getMessageWithDescription());
                    switch (errorCode) {
                    case HttpURLConnection.HTTP_GONE:
                        result.setError(ErrorCodes.ALREADY_DELETED);
                        result.setMessage(e.getMessage());
                        break;
                    case HttpURLConnection.HTTP_CONFLICT:
                    case HttpURLConnection.HTTP_PRECON_FAILED:
                    case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:
                        result.setError(ErrorCodes.UPLOAD_CONFLICT);
                        result.setMessage(e.getMessage());
                        break;
                    case HttpStatusCodes.HTTP_TOO_MANY_REQUESTS:
                        result.setError(ErrorCodes.UPLOAD_LIMIT_EXCEEDED);
                        result.setMessage(e.getMessage());
                        break;
                    default:
                        mapErrorCode(errorCode, result);
                    }
                } catch (final IOException | NumberFormatException e) {
                    Log.e(DEBUG_TAG, METHOD_UPLOAD, e);
                    result.setError(ErrorCodes.UPLOAD_PROBLEM);
                    result.setMessage(e.getLocalizedMessage());
                } catch (final NullPointerException e) {
                    Log.e(DEBUG_TAG, METHOD_UPLOAD, e);
                    ACRAHelper.nocrashReport(e, e.getMessage());
                }
                return result;
            }

            @Override
            protected void onPostExecute(UploadResult result) {
                Progress.dismissDialog(activity, Progress.PROGRESS_UPLOADING, PROGRESS_TAG);
                final int error = result.getError();
                try {
                    final StorageDelegator delegator = getDelegator();
                    if (error == ErrorCodes.OK) {
                        ScreenMessage.barInfo(activity, R.string.toast_upload_success);
                        if (elements == null) {
                            delegator.clearUndo(); // only clear on successful upload
                        } else {
                            delegator.clearUndo(elements);
                        }
                        // save now to avoid problems if it doesn't succeed later on, this currently writes
                        // sync and potentially cause ANRs
                        save(activity);
                        activity.invalidateOptionsMenu();
                        if (postUploadHandler != null) {
                            postUploadHandler.onSuccess();
                        }
                    }
                    if (!activity.isFinishing()) {
                        switch (error) {
                        case ErrorCodes.UPLOAD_CONFLICT:
                            Conflict conflict = ApiResponse.parseConflictResponse(result.getHttpError(), result.getMessage());
                            if (conflict instanceof ApiResponse.ClosedChangesetConflict) {
                                // this can really only happen if the changeset is closed between when we check for an
                                // open one and we starting the upload
                                ScreenMessage.toastTopWarning(activity, R.string.upload_conflict_message_changeset_closed);
                                this.execute(); // restart new changeset will be opened automatically
                                return;
                            } else if (conflict instanceof ApiResponse.BoundingBoxTooLargeError) {
                                if (!closeOpenChangeset) {
                                    // we've potentially already uploaded something, so don't reuse this changeset
                                    server.resetChangeset();
                                }
                                ErrorAlert.showDialog(activity, ErrorCodes.UPLOAD_BOUNDING_BOX_TOO_LARGE, result.getMessage());
                            } else if (conflict instanceof ApiResponse.ChangesetLocked) {
                                ErrorAlert.showDialog(activity, ErrorCodes.UPLOAD_PROBLEM, result.getMessage());
                            } else {
                                UploadConflict.showDialog(activity, conflict, elements);
                            }
                            break;
                        case ErrorCodes.INVALID_LOGIN:
                            InvalidLogin.showDialog(activity);
                            break;
                        case ErrorCodes.FORBIDDEN:
                            ForbiddenLogin.showDialog(activity, result.getMessage());
                            break;
                        case ErrorCodes.BAD_REQUEST:
                        case ErrorCodes.NOT_FOUND:
                        case ErrorCodes.UNKNOWN_ERROR:
                        case ErrorCodes.UPLOAD_PROBLEM:
                        case ErrorCodes.UPLOAD_LIMIT_EXCEEDED:
                            ErrorAlert.showDialog(activity, error, result.getMessage());
                            break;
                        case ErrorCodes.ALREADY_DELETED:
                            conflict = ApiResponse.parseConflictResponse(result.getHttpError(), result.getMessage());
                            if (conflict instanceof ApiResponse.AlreadyDeletedConflict) {
                                final OsmElement deletedElement = delegator.getOsmElement(conflict.getElementType(), conflict.getElementId());
                                delegator.removeFromUpload(deletedElement, OsmElement.STATE_DELETED);
                                if (elements != null) {
                                    elements.remove(deletedElement);
                                }
                                ScreenMessage.toastTopWarning(activity,
                                        activity.getString(R.string.upload_conflict_message_already_deleted, deletedElement.getDescription(true)));
                                this.execute(); // restart
                                return;
                            } // NOSONAR fall through
                        default:
                            ErrorAlert.showDialog(activity, error);
                        }
                        if (postUploadHandler != null) {
                            postUploadHandler.onError(null);
                        }
                    }
                } catch (Exception ex) {
                    Log.e(DEBUG_TAG, "Unexpected exception in upload " + ex.getMessage());
                    ACRAHelper.nocrashReport(ex, ex.getMessage());
                } finally {
                    invalidateCurrentFocus(activity);
                }
            }
        }.execute();
    }

    /**
     * Map "standard" http error codes from the API to internal codes
     * 
     * @param errorCode the error code
     * @param result a result object
     */
    public static void mapErrorCode(int errorCode, @NonNull UploadResult result) {
        switch (errorCode) {
        case HttpURLConnection.HTTP_FORBIDDEN:
            result.setError(ErrorCodes.FORBIDDEN);
            break;
        case HttpURLConnection.HTTP_UNAUTHORIZED:
            result.setError(ErrorCodes.INVALID_LOGIN);
            break;
        case HttpURLConnection.HTTP_BAD_REQUEST:
            result.setError(ErrorCodes.BAD_REQUEST);
            break;
        case HttpURLConnection.HTTP_NOT_FOUND:
            result.setError(ErrorCodes.NOT_FOUND);
            break;
        case HttpURLConnection.HTTP_INTERNAL_ERROR:
        case HttpURLConnection.HTTP_BAD_GATEWAY:
        case HttpURLConnection.HTTP_UNAVAILABLE:
            result.setError(ErrorCodes.UPLOAD_PROBLEM);
            break;
        default:
            Log.e(DEBUG_TAG, METHOD_UPLOAD + " " + result.getMessage());
            result.setError(ErrorCodes.UNKNOWN_ERROR);
            break;
        }
    }

    /**
     * Invalidate whatever has focus
     * 
     * @param activity the current Activity
     */
    private void invalidateCurrentFocus(@NonNull final Activity activity) {
        View currentFocus = activity.getCurrentFocus();
        if (currentFocus != null) {
            currentFocus.invalidate();
        }
    }

    /**
     * Uploads a GPS track to the server.
     * 
     * @param activity he calling FragementActivity
     * @param track the track to upload
     * @param description a description of the track sent to the server
     * @param tags the tags to apply to the GPS track (comma delimited)
     * @param visibility the track visibility, one of the following: private, public, trackable, identifiable
     */
    public void uploadTrack(@NonNull final FragmentActivity activity, @NonNull final Track track, @NonNull final String description, @NonNull final String tags,
            final Visibility visibility) {
        final Server server = prefs.getServer();
        new ExecutorTask<Void, Void, UploadResult>(executorService, uiHandler) {

            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_UPLOADING);
            }

            @Override
            protected UploadResult doInBackground(Void params) {
                UploadResult result = new UploadResult();
                try {
                    OsmGpxApi.uploadTrack(server, track, description, tags, visibility);
                } catch (final OsmServerException e) {
                    Log.e(DEBUG_TAG, e.getMessage());
                    int errorCode = e.getHttpErrorCode();
                    result.setHttpError(errorCode);
                    result.setMessage(e.getMessage());
                    Logic.mapErrorCode(errorCode, result);
                } catch (final IOException e) {
                    result.setError(ErrorCodes.NO_CONNECTION);
                    Log.e(DEBUG_TAG, "", e);
                } catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
                    result.setError(ErrorCodes.UPLOAD_PROBLEM);
                    Log.e(DEBUG_TAG, "", e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(UploadResult result) {
                Progress.dismissDialog(activity, Progress.PROGRESS_UPLOADING);
                invalidateCurrentFocus(activity);
                final int resultCode = result.getError();
                if (resultCode == ErrorCodes.OK) {
                    ScreenMessage.barInfo(activity, R.string.toast_upload_success);
                    return;
                }
                if (activity.isFinishing()) {
                    return;
                }
                if (resultCode == ErrorCodes.INVALID_LOGIN) {
                    InvalidLogin.showDialog(activity);
                } else {
                    ErrorAlert.showDialog(activity, resultCode);
                }
            }
        }.execute();
    }

    /**
     * Show a snackbar indicating how many unread mails are on the server
     * 
     * @param activity activity calling this method
     * @param server current server configuration
     */
    public void checkForMail(@NonNull final FragmentActivity activity, @NonNull final Server server) {
        new ExecutorTask<Void, Void, Integer>(executorService, uiHandler) {
            @Override
            protected Integer doInBackground(Void params) {
                int result = 0;

                UserDetails userDetails = server.getUserDetails();
                if (userDetails != null) {
                    result = userDetails.getUnreadMessages();
                }
                return result;
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result > 0) {
                    try {
                        if (activity != null) {
                            ScreenMessage.barInfo(activity, activity.getResources().getQuantityString(R.plurals.toast_unread_mail, result, result),
                                    R.string.read_mail, v -> {
                                        try {
                                            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Urls.OSM_LOGIN)));
                                        } catch (Exception ex) {
                                            // never crash
                                            Log.e(DEBUG_TAG, "Linking to the OSM login page failed " + ex.getMessage());
                                        }
                                    });
                        }
                    } catch (java.util.IllegalFormatFlagsException iffex) {
                        // do nothing ... this is stop bugs in the Android format parsing crashing the upload, happens
                        // at least with the PL string
                    }
                }
            }
        }.execute();
    }

    /**
     * Make a new Note at the given screen X/Y coordinates.
     * 
     * @param x The screen X-coordinate of the bug.
     * @param y The screen Y-coordinate of the bug.
     * @return The new Note, which must have a comment added before it can be submitted to OSM.
     */
    @NonNull
    public Note makeNewNote(final float x, final float y) {
        int lat = yToLatE7(y);
        int lon = xToLonE7(x);
        return new Note(lat, lon);
    }

    /**
     * Setter to a) set the internal value and b) push the value to {@link #map}.
     * 
     * @param selectedNode node to select
     */
    public synchronized void setSelectedNode(@Nullable final Node selectedNode) {
        selectionStack.getFirst().setNode(selectedNode);
        map.setSelectedNodes(selectionStack.getFirst().getNodes());
        resetFilterCache();
    }

    /**
     * Add nodes to the internal list
     * 
     * @param selectedNode node to add to selection
     */
    public synchronized void addSelectedNode(@NonNull final Node selectedNode) {
        selectionStack.getFirst().add(selectedNode);
        resetFilterCache();
    }

    /**
     * @return the selectedNode (currently simply the first in the list)
     */
    @Nullable
    public final synchronized Node getSelectedNode() {
        return selectionStack.getFirst().getNode();
    }

    /**
     * Get list of selected nodes
     * 
     * @return a List of Nodes that are selected
     */
    @Nullable
    public List<Node> getSelectedNodes() {
        return selectionStack.getFirst().getNodes();
    }

    /**
     * Return how many nodes are selected
     * 
     * @return a count of the selected Nodes
     */
    public int selectedNodesCount() {
        return selectionStack.getFirst().nodeCount();
    }

    /**
     * De-select a node
     * 
     * @param node node to remove from selection
     */
    public synchronized void removeSelectedNode(@NonNull Node node) {
        if (selectionStack.getFirst().remove(node)) {
            resetFilterCache();
        }
    }

    /**
     * Setter to a) set the internal value and b) push the value to {@link #map}.
     * 
     * @param selectedWay way to select
     */
    public synchronized void setSelectedWay(@Nullable final Way selectedWay) {
        selectionStack.getFirst().setWay(selectedWay);
        map.setSelectedWays(selectionStack.getFirst().getWays());
        resetFilterCache();
    }

    /**
     * Adds the given way to the list of currently selected ways.
     * 
     * @param selectedWay way to add to selection
     */
    public synchronized void addSelectedWay(@NonNull final Way selectedWay) {
        selectionStack.getFirst().add(selectedWay);
        resetFilterCache();
    }

    /**
     * @return the selectedWay (currently simply the first in the list)
     */
    @Nullable
    public final synchronized Way getSelectedWay() {
        return selectionStack.getFirst().getWay();
    }

    /**
     * Get list of selected ways
     * 
     * @return a List of Ways that are selected
     */
    @Nullable
    public List<Way> getSelectedWays() {
        return selectionStack.getFirst().getWays();
    }

    /**
     * Return how many ways are selected
     * 
     * @return a count of the selected Ways
     */
    public int selectedWaysCount() {
        return selectionStack.getFirst().wayCount();
    }

    /**
     * Removes the given way from the list of currently selected ways.
     * 
     * @param way way to de-select
     */
    public synchronized void removeSelectedWay(@NonNull Way way) {
        if (selectionStack.getFirst().remove(way)) {
            resetFilterCache();
        }
    }

    /**
     * Setter to a) set the internal value and b) push the value to {@link #map}.
     * 
     * @param selectedRelation relation to select
     */
    public synchronized void setSelectedRelation(@Nullable final Relation selectedRelation) {
        selectionStack.getFirst().setRelation(selectedRelation);
        if (selectedRelation != null) {
            setSelectedRelationMembers(selectedRelation);
        }
        resetFilterCache();
    }

    /**
     * De-select the relation
     * 
     * @param relation relation to remove from selection
     */
    public synchronized void removeSelectedRelation(@NonNull Relation relation) {
        if (selectionStack.getFirst().remove(relation)) {
            setSelectedRelationNodes(null); // de-select all
            setSelectedRelationWays(null);
            setSelectedRelationRelations(null);
            if (selectionStack.getFirst().relationCount() > 0) {
                for (Relation r : getSelectedRelations()) { // re-select
                    setSelectedRelationMembers(r);
                }
            }
            resetFilterCache();
        }
    }

    /**
     * Adds the given relation to the list of currently selected relations.
     * 
     * @param selectedRelation relation to add to selection
     */
    public synchronized void addSelectedRelation(@NonNull final Relation selectedRelation) {
        selectionStack.getFirst().add(selectedRelation);
        setSelectedRelationMembers(selectedRelation);
        resetFilterCache();
    }

    /**
     * Get list of selected RelaTions
     * 
     * @return a List of Relations that are selected
     */
    @Nullable
    public List<Relation> getSelectedRelations() {
        return selectionStack.getFirst().getRelations();
    }

    /**
     * Return how many Relations are selected
     * 
     * @return a count of the selected Relations
     */
    public int selectedRelationsCount() {
        return selectionStack.getFirst().relationCount();
    }

    /**
     * Select all the elements in the List
     * 
     * @param elements a List of OsmElement to select
     */
    public synchronized void setSelection(@NonNull List<OsmElement> elements) {
        Selection currentSelection = selectionStack.getFirst();
        for (OsmElement e : elements) {
            currentSelection.add(e);
        }
        resetFilterCache();
    }

    /**
     * Helper to clear the current, if any, filter cache
     */
    private void resetFilterCache() {
        if (filter != null) {
            filter.clear();
        }
    }

    /**
     * Get all current selected OsmElements
     * 
     * @return a List, potentially empty, containing all seleced elemetns
     */
    @NonNull
    public synchronized List<OsmElement> getSelectedElements() {
        List<OsmElement> result = new ArrayList<>();
        final Selection currentSelection = selectionStack.getFirst();
        List<Node> selectedNodes = currentSelection.getNodes();
        if (selectedNodes != null) {
            result.addAll(selectedNodes);
        }
        List<Way> selectedWays = currentSelection.getWays();
        if (selectedWays != null) {
            result.addAll(selectedWays);
        }
        List<Relation> selectedRelations = currentSelection.getRelations();
        if (selectedRelations != null) {
            result.addAll(selectedRelations);
        }
        return result;
    }

    /**
     * Get the ids of currently selected objects
     * 
     * @return an Selection.Ids object containing the ids of currently selected objects
     */
    public Ids getSelectedIds() {
        return selectionStack.getFirst().getIds();
    }

    /**
     * Check is all selected elements exist, return true if we actually had to remove something
     * 
     * @return true if a selected element didn't exist anymore
     */
    boolean resyncSelected() {
        boolean result = false;
        final Storage currentStorage = getDelegator().getCurrentStorage();
        if (selectedNodesCount() > 0) {
            for (Node n : new ArrayList<>(getSelectedNodes())) {
                if (!currentStorage.contains(n)) {
                    removeSelectedNode(n);
                    result = true;
                }
            }
        }
        if (selectedWaysCount() > 0) {
            for (Way w : new ArrayList<>(getSelectedWays())) {
                if (!currentStorage.contains(w)) {
                    removeSelectedWay(w);
                    result = true;
                }
            }
        }
        if (selectedRelationsCount() > 0) {
            setSelectedRelationNodes(null); // de-select all
            setSelectedRelationWays(null);
            for (Relation r : new ArrayList<>(getSelectedRelations())) {
                if (!currentStorage.contains(r)) {
                    removeSelectedRelation(r);
                    result = true;
                } else {
                    setSelectedRelationMembers(r);
                }
            }
        }
        return result;
    }

    /**
     * Check if a specific OsmElement is selected
     * 
     * @param e the OsmElement to check
     * @return true is e is selected
     */
    public synchronized boolean isSelected(@Nullable OsmElement e) {
        return e != null && selectionStack.getFirst().contains(e);
    }

    /**
     * Get a list of all nodes currently in storage
     * 
     * @return unmodifiable list of all nodes currently loaded
     */
    public List<Node> getNodes() {
        return getDelegator().getCurrentStorage().getNodes();
    }

    /**
     * Get a list of all nodes contained in bounding box box currently in storage
     * 
     * @param box the bounding box
     * @return unmodifiable list of all nodes currently loaded contained in box
     */
    public List<Node> getNodes(BoundingBox box) {
        return getDelegator().getCurrentStorage().getNodes(box);
    }

    /**
     * Get a list of all modified (created, modified, deleted) nodes currently in storage
     * 
     * @return all modified nodes currently loaded
     */
    public List<Node> getModifiedNodes() {
        return getDelegator().getApiStorage().getNodes();
    }

    /**
     * Get a list of all nodes contained in bounding box box currently in storage
     * 
     * @param box the bounding box
     * @return all modified nodes currently loaded contained in box
     */
    public List<Node> getModifiedNodes(BoundingBox box) {
        return getDelegator().getApiStorage().getNodes(box);
    }

    /**
     * Get a list of all ways currently in storage
     * 
     * @return unmodifiable list of all ways currently loaded
     */
    public List<Way> getWays() {
        return getDelegator().getCurrentStorage().getWays();
    }

    /**
     * Get a list of all ways contained in or possibly intersecting the bounding box box currently in storage
     * 
     * @param box the bounding box
     * @return unmodifiable list of all nodes currently loaded contained in box
     */
    public List<Way> getWays(BoundingBox box) {
        return getDelegator().getCurrentStorage().getWays(box);
    }

    /**
     * Get a list of all modified (created, modified, deleted) ways currently in storage
     * 
     * @return unmodifiable list of all modified ways currently loaded
     */
    public List<Way> getModifiedWays() {
        return getDelegator().getApiStorage().getWays();
    }

    /**
     * Get a list of all relations currently in storage
     * 
     * @return unmodifiable list of all relations currently loaded
     */
    public List<Relation> getRelations() {
        return getDelegator().getCurrentStorage().getRelations();
    }

    /**
     * Get a list of all modified (created, modified, deleted) relations currently in storage
     * 
     * @return unmodifiable list of all modified relations currently loaded
     */
    public List<Relation> getModifiedRelations() {
        return getDelegator().getApiStorage().getRelations();
    }

    /**
     * Will be called when the screen orientation was changed.
     * 
     * @param map the new Map-Instance. Be aware: The View-dimensions are not yet set...
     * @param deselect if true de-select objects
     */
    public void setMap(@NonNull Map map, boolean deselect) {
        Log.d(DEBUG_TAG, "setting map");
        this.map = map;
        map.setDelegator(getDelegator());
        map.setViewBox(viewBox);
        if (deselect) {
            map.deselectObjects();
            selectionStack.getFirst().reset();
            resetFilterCache();
        }
        invalidateMap();
    }

    /**
     * Getter for testing
     * 
     * @return map object
     */
    @Nullable
    public Map getMap() {
        return map;
    }

    /**
     * Convenience method to invalidate the map
     */
    void invalidateMap() {
        if (map != null) {
            map.invalidate();
        }
    }

    /**
     * Get a list of all pending changes to upload
     * 
     * @param aCaller an Android Context
     * @return a list of all pending changes to upload
     */
    public List<String> getPendingChanges(@NonNull final Context aCaller) {
        return getDelegator().listChanges(aCaller.getResources());
    }

    /**
     * @return a list of all pending changes to upload
     */
    public synchronized List<OsmElement> getPendingChangedElements() {
        return getDelegator().listChangedElements();
    }

    /**
     * Sets the set of elements that can currently be clicked.
     * <ul>
     * <li>If set to null, the map will use default behaviour.</li>
     * <li>If set to a non-null value, the map will highlight only elements in the list.</li>
     * </ul>
     * 
     * @param <T> type of element
     * @param clickable a set of elements to which highlighting should be limited, or null to remove the limitation
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends OsmElement> void setClickableElements(Set<T> clickable) {
        clickableElements = (Set<OsmElement>) clickable;
    }

    /**
     * Get elements that can currently be clicked
     * 
     * @return the list of clickable elements. May be null, meaning no restrictions on clickable elements
     */
    @Nullable
    public synchronized Set<OsmElement> getClickableElements() {
        return clickableElements;
    }

    /**
     * Sets if we return relations when touching/clicking.
     * 
     * @param on true if we should return relations
     */
    public void setReturnRelations(boolean on) {
        returnRelations = on;
    }

    /**
     * Checks if an element exists, i.e. is in currentStorage
     * 
     * @param element the element that is to be checked
     * @return true if the element exists, false otherwise
     */
    public boolean exists(OsmElement element) {
        return getDelegator().getCurrentStorage().contains(element);
    }

    /**
     * Get the screen X coordinate
     * 
     * @param node the Node
     * @return the X coordinate (in pixels) of the given node's position on the screen (note that the returned position
     *         may be outside of the screens bounds).
     */
    public float getNodeScreenX(@NonNull Node node) {
        return lonE7ToX(node.getLon());
    }

    /**
     * Get the screen Y coordinate
     * 
     * @param node the Node
     * @return the Y coordinate (in pixels) of the given node's position on the screen (note that the returned position
     *         may be outside of the screens bounds).
     */
    public float getNodeScreenY(@NonNull Node node) {
        return latE7ToY(node.getLat());
    }

    /** Helper class for ordering nodes/ways by distance from a click */
    private static class DistanceSorter<O extends OsmElement, T extends O> {
        private Comparator<Entry<T, Double>> comparator = (lhs, rhs) -> {
            if (lhs == rhs) {
                return 0;
            }
            if (lhs.getValue() > rhs.getValue()) {
                return 1;
            }
            if (lhs.getValue() < rhs.getValue()) {
                return -1;
            }
            return 0;
        };

        /**
         * Takes an element-distance map and returns the elements ordered by distance
         *
         * @param input Map with the element and distance
         * @return a sorted List of the input
         */
        public List<O> sort(java.util.Map<T, Double> input) {
            List<Entry<T, Double>> entries = new ArrayList<>(input.entrySet());
            Collections.sort(entries, comparator);

            List<O> result = new ArrayList<>(entries.size());
            for (Entry<T, Double> entry : entries) {
                result.add(entry.getKey());
            }
            return result;
        }
    }

    /**
     * Creates a turn restriction relation using the given objects as the members in the relation.
     * 
     * @param activity activity we were called from
     * @param fromWay the way on which turning off of is restricted in some fashion
     * @param viaElement the "intersection node" at which the turn is restricted
     * @param toWay the way that the turn restriction prevents turning onto
     * @param restrictionType the kind of turn which is restricted
     * @return a relation element for the turn restriction
     */
    @NonNull
    public Relation createRestriction(@Nullable FragmentActivity activity, @NonNull Way fromWay, @NonNull OsmElement viaElement, @NonNull Way toWay,
            @Nullable String restrictionType) {
        createCheckpoint(activity, R.string.undo_action_create_relation);
        try {
            Relation restriction = getDelegator().createAndInsertRelation(null);
            SortedMap<String, String> tags = new TreeMap<>();
            tags.put(Tags.VALUE_RESTRICTION, restrictionType == null ? "" : restrictionType);
            tags.put(Tags.KEY_TYPE, Tags.VALUE_RESTRICTION);
            getDelegator().setTags(restriction, tags);
            RelationMember from = new RelationMember(Tags.ROLE_FROM, fromWay);
            getDelegator().addMemberToRelation(from, restriction);
            RelationMember via = new RelationMember(Tags.ROLE_VIA, viaElement);
            getDelegator().addMemberToRelation(via, restriction);
            RelationMember to = new RelationMember(Tags.ROLE_TO, toWay);
            getDelegator().addMemberToRelation(to, restriction);
            return restriction;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Creates a new relation containing the given members.
     * 
     * @param activity activity we were called from
     * @param type the 'type=*' tag to set on the relation itself
     * @param members the osm elements to include in the relation
     * @return the new relation
     */
    @NonNull
    public Relation createRelation(@Nullable FragmentActivity activity, String type, List<OsmElement> members) {
        createCheckpoint(activity, R.string.undo_action_create_relation);
        try {
            Relation relation = getDelegator().createAndInsertRelation(members);
            setRelationType(type, relation);
            return relation;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Creates a new relation containing the given members.
     * 
     * @param activity activity we were called from
     * @param type the 'type=*' tag to set on the relation itself
     * @param members the osm elements to include in the relation
     * @return the new relation
     */
    @NonNull
    public Relation createRelationFromMembers(@Nullable Activity activity, String type, List<RelationMember> members) {
        createCheckpoint(activity, R.string.undo_action_create_relation);
        Relation relation = getDelegator().createAndInsertRelationFromMembers(members);
        setRelationType(type, relation);
        return relation;
    }

    /**
     * Set the type tag for a Relation
     * 
     * @param type the type or null
     * @param relation the Relation
     */
    private void setRelationType(@Nullable String type, @NonNull Relation relation) {
        SortedMap<String, String> tags = new TreeMap<>(relation.getTags());
        tags.put(Tags.KEY_TYPE, type != null ? type : "");
        getDelegator().setTags(relation, tags);
    }

    /**
     * Adds the list of elements to the given relation with an empty role set for each new member.
     * 
     * @param activity activity we were called from
     * @param relation Relation we want to add the members to
     * @param members List of members to add
     */
    public void addMembers(@Nullable FragmentActivity activity, @NonNull Relation relation, @NonNull List<OsmElement> members) {
        createCheckpoint(activity, R.string.undo_action_update_relations);
        try {
            getDelegator().addMembersToRelation(relation, members);
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Adds the list of RelationMembers to the given relation
     * 
     * @param activity activity we were called from
     * @param relation Relation we want to add the members to
     * @param members List of RelationMembers to add
     */
    public void addRelationMembers(@Nullable FragmentActivity activity, @NonNull Relation relation, @NonNull List<RelationMember> members) {
        createCheckpoint(activity, R.string.undo_action_update_relations);
        try {
            getDelegator().addRelationMembersToRelation(relation, members);
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Update the list of RelationMembers for given relation
     * 
     * @param activity activity we were called from
     * @param relation Relation we want to update
     * @param removeMembers List of RelationMembers to remove
     * @param addMembers List of RelationMembers to add
     */
    public void updateRelationMembers(@Nullable FragmentActivity activity, @NonNull Relation relation, @NonNull List<RelationMember> removeMembers,
            @NonNull List<RelationMember> addMembers) {
        createCheckpoint(activity, R.string.undo_action_update_relations);
        try {
            getDelegator().removeRelationMembersFromRelation(relation, removeMembers);
            getDelegator().addRelationMembersToRelation(relation, addMembers);
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex; // rethrow
        }
    }

    /**
     * Sets the set of ways that belong to a relation and should be highlighted. If set to null, the map will use
     * default behaviour. If set to a non-null value, the map will highlight only elements in the list.
     * 
     * This will make a shallow copy of the list passed to avoid unexpected behaviour when adding further ways
     * 
     * @param ways set of elements to which highlighting should be limited, or null to remove the limitation
     */
    public void setSelectedRelationWays(@Nullable List<Way> ways) {
        selectedRelationWays = ways == null ? null : new ArrayList<>(ways);
    }

    /**
     * Add a selected member way
     * 
     * @param way the way
     */
    public void addSelectedRelationWay(@NonNull Way way) {
        if (selectedRelationWays == null) {
            selectedRelationWays = new LinkedList<>();
        }
        selectedRelationWays.add(way);
    }

    /**
     * Remove a selected member way
     * 
     * @param way the way
     */
    public void removeSelectedRelationWay(@NonNull Way way) {
        if (selectedRelationWays != null) {
            selectedRelationWays.remove(way);
        }
    }

    /**
     * Get a list of selected relation member ways
     * 
     * @return a List of Ways or null
     */
    @Nullable
    public List<Way> getSelectedRelationWays() {
        return selectedRelationWays;
    }

    /**
     * Remove a selected member element
     * 
     * @param e the element
     */
    public void removeSelectedRelationElement(@NonNull OsmElement e) {
        if (e instanceof Node) {
            removeSelectedRelationNode((Node) e);
        } else if (e instanceof Way) {
            removeSelectedRelationWay((Way) e);
        } else if (e instanceof Relation) {
            removeSelectedRelationRelation((Relation) e);
        } else {
            Log.e(DEBUG_TAG, "removeSelectedRelationElement unknown element " + e);
        }
    }

    /**
     * Set relation members to be highlighted
     * 
     * @param r the Relation holding the members
     */
    public void setSelectedRelationMembers(@Nullable Relation r) {
        setSelectedRelationMembers(r, 0);
    }

    /**
     * Set relation members to be highlighted
     * 
     * @param r the Relation holding the members
     * @param depth current recursion depth
     */
    private synchronized void setSelectedRelationMembers(@Nullable Relation r, int depth) {
        if (r == null) {
            return;
        }
        for (RelationMember rm : r.getMembers()) {
            OsmElement e = rm.getElement();
            if (e != null) {
                switch (e.getName()) {
                case Way.NAME:
                    addSelectedRelationWay((Way) e);
                    break;
                case Node.NAME:
                    addSelectedRelationNode((Node) e);
                    break;
                case Relation.NAME:
                    // break recursion if already selected or max depth exceeded
                    if ((selectedRelationRelations == null || !selectedRelationRelations.contains(e)) && depth <= MAX_RELATION_SELECTION_DEPTH) {
                        addSelectedRelationRelation((Relation) e, depth);
                    }
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown relation member " + e.getName());
                }
            }
        }
    }

    /**
     * Sets the set of nodes that belong to a relation and should be highlighted. If set to null, the map will use
     * default behaviour. If set to a non-null value, the map will highlight only elements in the list.
     * 
     * @param nodes set of elements to which highlighting should be limited, or null to remove the limitation
     */
    public void setSelectedRelationNodes(@Nullable List<Node> nodes) {
        selectedRelationNodes = nodes;
    }

    /**
     * Add a selected member node
     * 
     * @param node the node
     */
    public void addSelectedRelationNode(@NonNull Node node) {
        if (selectedRelationNodes == null) {
            selectedRelationNodes = new LinkedList<>();
        }
        selectedRelationNodes.add(node);
    }

    /**
     * Remove a selected member node
     * 
     * @param node the node
     */
    public void removeSelectedRelationNode(@NonNull Node node) {
        if (selectedRelationNodes != null) {
            selectedRelationNodes.remove(node);
        }
    }

    /**
     * Get a List of selected relation member nodes or null
     * 
     * @return a List of Nodes
     */
    @Nullable
    public List<Node> getSelectedRelationNodes() {
        return selectedRelationNodes;
    }

    /**
     * Sets the set of relations that belong to a relation and should be highlighted. If set to null, the map will use
     * default behaviour. If set to a non-null value, the map will highlight only elements in the list.
     * 
     * @param relations set of elements to which highlighting should be limited, or null to remove the limitation
     */
    public synchronized void setSelectedRelationRelations(List<Relation> relations) {
        selectedRelationRelations = relations;
        if (selectedRelationRelations != null) {
            for (Relation r : selectedRelationRelations) {
                setSelectedRelationMembers(r);
            }
        }
    }

    /**
     * Add a Relation to the List of selected Relations
     * 
     * @param relation the Relation to add
     */
    public void addSelectedRelationRelation(@NonNull Relation relation) {
        addSelectedRelationRelation(relation, 0);
    }

    /**
     * Add a Relation to the List of selected Relations
     * 
     * @param relation the Relation to add
     * @param depth current recursion depth
     */
    private synchronized void addSelectedRelationRelation(@NonNull Relation relation, int depth) {
        if (selectedRelationRelations == null) {
            selectedRelationRelations = new LinkedList<>();
        }
        selectedRelationRelations.add(relation);
        setSelectedRelationMembers(relation, depth);
    }

    /**
     * Remove a relation from the List of selected Relations
     * 
     * @param relation the Relation to de-select
     */
    public synchronized void removeSelectedRelationRelation(@NonNull Relation relation) {
        if (selectedRelationRelations != null) {
            selectedRelationRelations.remove(relation);
        }
    }

    /**
     * Get the List of selected Relations
     * 
     * @return the List or null if none
     */
    @Nullable
    public synchronized List<Relation> getSelectedRelationRelations() {
        return selectedRelationRelations;
    }

    /**
     * If currently Relations are selected we may need to update the member highlighting
     */
    public synchronized void reselectRelationMembers() {
        List<Relation> selected = getSelectedRelations();
        if (selected != null && !selected.isEmpty()) {
            if (selectedRelationNodes != null) {
                selectedRelationNodes.clear();
            }
            if (selectedRelationWays != null) {
                selectedRelationWays.clear();
            }
            if (selectedRelationRelations != null) {
                selectedRelationRelations.clear();
            }
            for (Relation r : selected) {
                setSelectedRelationMembers(r);
            }
        }
    }

    /**
     * De-select all OsmElements
     * 
     * Note: does not de-select a selected Task
     */
    public void deselectAll() {
        setSelectedNode(null);
        setSelectedWay(null);
        setSelectedRelation(null);
        setSelectedRelationNodes(null);
        setSelectedRelationWays(null);
        setSelectedRelationRelations(null);
    }

    /**
     * Get the current stack of selections
     * 
     * @return the selection stack
     */
    @NonNull
    public synchronized Deque<Selection> getSelectionStack() {
        return selectionStack;
    }

    /**
     * Set the stack of selections
     * 
     * @param stack the stack we want to set
     */
    public synchronized void setSelectionStack(@NonNull Deque<Selection> stack) {
        if (!stack.isEmpty()) { // the stack needs to have at least one element
            selectionStack.clear();
            selectionStack.addAll(stack);
            selectFromTop();
        } else {
            Log.e(DEBUG_TAG, "Attempt to set empty selection stack");
        }
    }

    /**
     * Do map and filter setup from current top of selection stack
     */
    private void selectFromTop() {
        final Selection currentSelection = selectionStack.getFirst();
        map.setSelectedNodes(currentSelection.getNodes());
        map.setSelectedWays(currentSelection.getWays());
        reselectRelationMembers();
        resetFilterCache();
    }

    /**
     * Pop the current selection from the stack and select everything from the new top
     */
    public synchronized void popSelection() {
        if (selectionStack.size() > 1) {
            selectionStack.pop();
            selectFromTop();
        } else {
            Log.e(DEBUG_TAG, "Attempt to pop last selection from stack");
        }
    }

    /**
     * Push a new empty Selection and reset everything
     */
    public synchronized void pushSelection() {
        pushSelection(new Selection());
    }

    /**
     * Use a new Selection and push it to the top of the stack
     *
     * @param selection the Selection to use
     */
    public synchronized void pushSelection(@NonNull Selection selection) {
        selectionStack.push(selection);
        selectFromTop();
    }

    /**
     * Fixup an object with a version conflict
     * 
     * Note: when we undelete relations this may fail if a newly created relation references one that hasn't been
     * undeleted yet
     * 
     * @param activity activity we were called from
     * @param newVersion new version to use
     * @param elementLocal the local instance of the element
     * @param elementOnServer the remote instance of the element
     * @param createCheckpoint create an undo checkpoint if true
     */
    public void fixElementWithConflict(@Nullable Activity activity, long newVersion, @NonNull OsmElement elementLocal, @NonNull OsmElement elementOnServer,
            boolean createCheckpoint) {
        if (createCheckpoint) {
            createCheckpoint(activity, R.string.undo_action_fix_conflict);
        }
        if (elementOnServer.getState() == OsmElement.STATE_DELETED) { // deleted on server
            if (elementLocal.getState() == OsmElement.STATE_DELETED) {
                // deleted locally too
                // note this sets the state to unchanged, but the element
                // isn't referenced anywhere anymore so that doesn't matter
                getDelegator().removeFromUpload(elementLocal, OsmElement.STATE_UNCHANGED);
                return;
            }
            // not locally deleted
            // given that the element is deleted on the server we likely need to add it back to ways and relations
            // there too, we force an upload by bumping the version
            if (Node.NAME.equals(elementLocal.getName())) {
                bumpVersion(getWaysForNode((Node) elementLocal));
            }
            if (elementLocal.hasParentRelations()) {
                bumpVersion(elementLocal.getParentRelations());
            }
        }
        getDelegator().setOsmVersion(elementLocal, newVersion);
    }

    /**
     * Bump the version of a list of osm elements by one
     * 
     * @param elements List of OsmElement
     */
    private <T extends OsmElement> void bumpVersion(List<T> elements) {
        for (OsmElement e : elements) {
            if (e.getState() != OsmElement.STATE_CREATED) {
                getDelegator().setOsmVersion(e, e.getOsmVersion() + 1);
            }
        }
    }

    /**
     * Displays a crosshair marker on the screen at the coordinates given (in pixels).
     * 
     * @param x screen x coordinate
     * @param y screen y coordinate
     */
    public void showCrosshairs(float x, float y) {
        map.showCrosshairs(x, y);
        invalidateMap();
    }

    /**
     * Hide the crosshairs display
     */
    public void hideCrosshairs() {
        map.hideCrosshairs();
    }

    /**
     * Copy element to clipboard
     * 
     * @param element element to copy
     */
    public void copyToClipboard(@NonNull OsmElement element) {
        List<OsmElement> list = new ArrayList<>();
        list.add(element);
        copyToClipboard(list);
    }

    /**
     * Copy elements to clipboard
     * 
     * @param elements elements to copy
     */
    public void copyToClipboard(@NonNull List<OsmElement> elements) {
        int[] centroid = calcCentroid(elements);
        if (centroid.length != 2) {
            Log.e(DEBUG_TAG, "Unable to determine centroid");
            return;
        }
        getDelegator().copyToClipboard(elements, centroid[0], centroid[1]);
    }

    /**
     * Cut element to clipboard
     * 
     * @param activity the activity we were called from
     * @param element element to cut
     */
    public void cutToClipboard(@Nullable Activity activity, @NonNull OsmElement element) {
        List<OsmElement> list = new ArrayList<>();
        list.add(element);
        cutToClipboard(activity, list);
    }

    /**
     * Cut element to clipboard
     * 
     * @param activity the activity we were called from
     * @param elements the elements to cut
     */
    public void cutToClipboard(@Nullable Activity activity, @NonNull List<OsmElement> elements) {
        createCheckpoint(activity, R.string.undo_action_cut);
        int[] centroid = calcCentroid(elements);
        if (centroid.length != 2) {
            Log.e(DEBUG_TAG, "Unable to determine centroid");
            return;
        }
        getDelegator().cutToClipboard(elements, centroid[0], centroid[1]);
        invalidateMap();
    }

    /**
     * This calculates a centroid for a collection of different objects
     * 
     * It does not weight with the area of the objects and therefore is not a center of mass equivalent, this seems to
     * better match the expectation of a user (in which points have the same importance as an area or a way.
     * 
     * @param elements a list of OsmElements
     * @return the centroid or an empty array if an error occurs [lat, lon] WGS84*E7
     */
    @NonNull
    private int[] calcCentroid(@NonNull List<OsmElement> elements) {
        try {
            if (elements.isEmpty()) {
                throw new IllegalArgumentException("empty element list for for centroid");
            }
            long latE7 = 0;
            long lonE7 = 0;
            int count = 0;
            for (OsmElement e : elements) {
                if (e instanceof Node) {
                    latE7 += ((Node) e).getLat();
                    lonE7 += ((Node) e).getLon();
                } else if (e instanceof Way) {
                    // use current centroid of way
                    int[] centroid = Geometry.centroid(map.getWidth(), map.getHeight(), viewBox, (Way) e);
                    if (centroid.length != 2) {
                        throw new IllegalArgumentException("centroid of way " + e.getDescription() + " is null");
                    }
                    latE7 += centroid[0];
                    lonE7 += centroid[1];
                } else if (e instanceof Relation && e.hasTag(Tags.KEY_TYPE, Tags.VALUE_MULTIPOLYGON)) {
                    int[] centroid = RelationUtils.calcCentroid((Relation) e, map, viewBox);
                    latE7 += centroid[0];
                    lonE7 += centroid[1];
                } else {
                    throw new IllegalArgumentException("unknown object type for centroid " + e);
                }
                count++;
            }
            if (count == 0) {
                throw new IllegalArgumentException("no valid centroids");
            }
            return new int[] { (int) (latE7 / count), (int) (lonE7 / count) };
        } catch (IllegalArgumentException iaex) {
            Log.e(DEBUG_TAG, iaex.getMessage());
            return new int[0];
        }
    }

    /**
     * Paste current contents of the clipboard
     * 
     * @param activity the activity we were called from
     * @param x screen x to position the object at
     * @param y screen y to position the object at
     * @return the pasted object or null if the clipboard was empty
     */
    @Nullable
    public List<OsmElement> pasteFromClipboard(@Nullable Activity activity, float x, float y) {
        createCheckpoint(activity, R.string.undo_action_paste);
        int lat = yToLatE7(y);
        int lon = xToLonE7(x);
        return getDelegator().pasteFromClipboard(lat, lon);
    }

    /**
     * Check if the clipboard is empty
     * 
     * @return true if empty
     */
    public boolean clipboardIsEmpty() {
        return getDelegator().clipboardIsEmpty();
    }

    /**
     * Arrange way points in a circle
     * 
     * Note: currently only works if map is present
     * 
     * @param activity this method was called from, if null no warnings will be displayed
     * @param way way to circulize
     */
    public void performCirculize(@Nullable FragmentActivity activity, @NonNull Way way) {
        if (way.getNodes().size() < 3) {
            return;
        }
        try {
            createCheckpoint(activity, R.string.undo_action_circulize);
            getDelegator().circulizeWay(map, prefs.getMinCircleNodes(), prefs.getMaxCircleSegment(), prefs.getMinCircleSegment(), way);
            invalidateMap();
            displayAttachedObjectWarning(activity, way);
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex;
        }
    }

    /**
     * Create a circle from at least three nodes
     * 
     * @param activity this method was called from, if null no warnings will be displayed
     * @param nodes list of at least three nodes
     * @return the circle
     */
    @NonNull
    public Way createCircle(@Nullable FragmentActivity activity, @NonNull List<Node> nodes) {
        try {
            createCheckpoint(activity, R.string.undo_action_add);
            Way circle = getDelegator().createCircle(map, prefs.getMinCircleNodes(), prefs.getMaxCircleSegment(), prefs.getMinCircleSegment(), nodes);
            invalidateMap();
            displayAttachedObjectWarning(activity, nodes);
            return circle;
        } catch (OsmIllegalOperationException | StorageException ex) {
            handleDelegatorException(activity, ex);
            throw ex;
        }
    }

    /**
     * Convenience function calls GeoMath.xToLonE7
     * 
     * @param x screen X coordinate
     * @return the WGS84*1E7 longitude
     */
    public int xToLonE7(float x) {
        return GeoMath.xToLonE7(map.getWidth(), viewBox, x);
    }

    /**
     * Convenience function calls GeoMath.yToLatE7
     * 
     * @param y screen Y coordinate
     * @return the WGS84*1E7 latitude
     */
    public int yToLatE7(float y) {
        return GeoMath.yToLatE7(map.getHeight(), map.getWidth(), viewBox, y);
    }

    /**
     * Convenience function calls GeoMath.lonE7ToX
     * 
     * @param lonE7 the WGS84*1E7 longitude
     * @return the screen X coordinate
     */
    public float lonE7ToX(int lonE7) {
        return GeoMath.lonE7ToX(map.getWidth(), viewBox, lonE7);
    }

    /**
     * convenience function
     * 
     * @param latE7 the WGS84*1E7 latitude
     * @return the screen Y coordinate
     */
    public float latE7ToY(int latE7) {
        return GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, latE7);
    }

    /**
     * @return the delegator
     */
    private static StorageDelegator getDelegator() {
        return App.getDelegator();
    }

    /**
     * Lock the StorageDelegator
     */
    public void getDataLock() {
        getDelegator().lock();
    }

    /**
     * Unlock the StorageDelegator
     */
    public void dataUnlock() {
        getDelegator().unlock();
    }

    /**
     * @return the viewBox
     */
    public ViewBox getViewBox() {
        return viewBox;
    }

    /**
     * Return the last used comment
     * 
     * @return comment
     */
    @Nullable
    public String getLastComment() {
        if (getDraftComment() == null) {
            return lastComments.last();
        }
        return getDraftComment();
    }

    /**
     * Return the last used comments index 0 is the most recent one
     * 
     * @return ArrayList of the comments
     */
    @NonNull
    public List<String> getLastComments() {
        return lastComments;
    }

    /**
     * Set the list of last comments
     * 
     * @param comments the List to set
     */
    public void setLastComments(@NonNull List<String> comments) {
        lastComments = new MRUList<>(comments);
        lastComments.ensureCapacity(MRULIST_SIZE);
    }

    /**
     * Push a comment on to the last comments MRU list
     * 
     * @param comment the comment to push
     * @param draft if true store as draft
     */
    public void pushComment(@Nullable String comment, boolean draft) {
        if (comment != null && !"".equals(comment)) {
            if (draft) {
                setDraftComment(comment);
            } else {
                setDraftComment(null);
                lastComments.push(comment);
            }
        }
    }

    /**
     * @return the draft comment if any
     */
    @Nullable
    public String getDraftComment() {
        return draftComment;
    }

    /**
     * A draft comment is not stored in the MRU
     * 
     * @param comment set the draft comment
     */
    public void setDraftComment(@Nullable String comment) {
        this.draftComment = comment;
    }

    /**
     * Return the last used source string
     * 
     * @return source
     */
    @Nullable
    public String getLastSource() {
        if (getDraftSourceComment() == null) {
            return lastSources.last();
        }
        return getDraftSourceComment();
    }

    /**
     * Return the last used source strings index 0 is the most recent one
     * 
     * @return a List of the source strings
     */
    @NonNull
    public List<String> getLastSources() {
        return lastSources;
    }

    /**
     * Set the list of last used source strings
     * 
     * @param sources the List to set
     */
    public void setLastSources(@NonNull List<String> sources) {
        lastSources = new MRUList<>(sources);
        lastSources.ensureCapacity(MRULIST_SIZE);
    }

    /**
     * Push a source comment on to the last source comments MRU list
     * 
     * @param source the source comment to push
     * @param draft if true store as draft
     */
    public void pushSource(@Nullable String source, boolean draft) {
        if (source != null && !"".equals(source)) {
            if (draft) {
                setDraftSourceComment(source);
            } else {
                setDraftSourceComment(null);
                lastSources.push(source);
            }
        }
    }

    /**
     * Get the draft source comment
     * 
     * @return the lastSourceDraft
     */
    @Nullable
    public String getDraftSourceComment() {
        return draftSourceComment;
    }

    /**
     * Set the draft source comment
     * 
     * @param source the lastSourceDraft to set
     */
    public void setDraftSourceComment(@Nullable String source) {
        this.draftSourceComment = source;
    }

    /**
     * Return the last used object search strings index 0 is the most recent one
     * 
     * @return a List of the object search strings
     */
    @NonNull
    public List<String> getLastObjectSearches() {
        return lastObjectSearches;
    }

    /**
     * Set the list of last used object search strings
     * 
     * @param searches the List to set
     */
    public void setLastObjectSearches(@NonNull List<String> searches) {
        lastObjectSearches = new MRUList<>(searches);
        lastObjectSearches.ensureCapacity(MRULIST_SIZE);
    }

    /**
     * Push an object search string on to the last object search MRU list
     * 
     * @param objectSearch the object search string to push
     */
    public void pushObjectSearch(@Nullable String objectSearch) {
        if (objectSearch != null && !"".equals(objectSearch)) {
            lastObjectSearches.push(objectSearch);
        }
    }

    /**
     * @return the current object filter
     */
    @Nullable
    public Filter getFilter() {
        return filter;
    }

    /**
     * Set the object filter
     * 
     * @param filter the Filter to set or null
     */
    public void setFilter(@Nullable Filter filter) {
        this.filter = filter;
    }

    /**
     * Display a warning if an operation on the element e would effect a filtered/hidden object
     * 
     * @param <T> the OsmElement type
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param e the OsmELement
     */
    private <T extends OsmElement> void displayAttachedObjectWarning(@Nullable FragmentActivity activity, T e) {
        displayAttachedObjectWarning(activity, Util.wrapInList(e));
    }

    /**
     * Display a warning if an operation on the element e1 or e2 would effect a filtered/hidden object
     * 
     * @param <T> the OsmElement type
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param e1 first OsmElement
     * @param e2 2nd OsmELement
     */
    private <T extends OsmElement> void displayAttachedObjectWarning(@Nullable FragmentActivity activity, T e1, T e2) {
        List<T> a = Util.wrapInList(e1);
        a.add(e2);
        displayAttachedObjectWarning(activity, a);
    }

    /**
     * Display a warning if an operation on the element e1 or e2 would effect a filtered/hidden object
     * 
     * @param <T> the OsmElement type
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param e1 first OsmElement
     * @param e2 2nd OsmELement
     * @param checkRelationsOnly if true only check Relations
     */
    private <T extends OsmElement> void displayAttachedObjectWarning(@Nullable FragmentActivity activity, @NonNull T e1, @NonNull T e2,
            boolean checkRelationsOnly) {
        List<T> a = Util.wrapInList(e1);
        a.add(e2);
        displayAttachedObjectWarning(activity, a, checkRelationsOnly);
    }

    /**
     * Display a warning if an operation on the elements included in list would effect a filtered/hidden object
     * 
     * @param <T> the OsmElement type
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param list List of OsmElements
     */
    private <T extends OsmElement> void displayAttachedObjectWarning(@Nullable FragmentActivity activity, Collection<T> list) {
        displayAttachedObjectWarning(activity, list, false);
    }

    /**
     * Display a warning if an operation on the elements included in list would effect a filtered/hidden object
     * 
     * @param <T> the OsmElement type
     * @param activity activity this method was called from, if null no warnings will be displayed
     * @param list List of OsmElements
     * @param checkRelationsOnly if true only check Relations
     */
    private <T extends OsmElement> void displayAttachedObjectWarning(@Nullable FragmentActivity activity, Collection<T> list, boolean checkRelationsOnly) {
        if (getFilter() == null || !showAttachedObjectWarning()) {
            return;
        }
        for (T e : list) {
            if (!checkRelationsOnly && ((e instanceof Node && displayAttachedObjectWarning(activity, (Node) e))
                    || (e instanceof Way && displayAttachedObjectWarning(activity, (Way) e)))) {
                continue;
            }
            displayAttachedRelationWarning(activity, e);
        }
    }

    /**
     * Display a warning if a hidden parent relation would be modified
     * 
     * @param activity the calling activity
     * @param w the Way
     * @return true if a warning is displayed
     */
    private void displayAttachedRelationWarning(@Nullable FragmentActivity activity, @NonNull OsmElement e) {
        final Filter f = getFilter();
        if (activity != null && f != null && e.hasParentRelations()) {
            for (Relation r : e.getParentRelations()) {
                if (!f.include(r, false)) {
                    AttachedObjectWarning.showDialog(activity);
                    return;
                }
            }
        }
    }

    /**
     * Display a warning if a hidden object would be modified
     * 
     * @param activity the calling activity
     * @param w the Way
     * @return true if a warning is displayed
     */
    private boolean displayAttachedObjectWarning(@Nullable FragmentActivity activity, @NonNull Way w) {
        if (activity != null) {
            for (Node n : w.getNodes()) {
                if (displayAttachedObjectWarning(activity, n)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Display a warning if a hidden object would be modified
     * 
     * @param activity the calling activity
     * @param n the Node
     * @return true if a warning is displayed
     */
    private boolean displayAttachedObjectWarning(@Nullable FragmentActivity activity, @NonNull Node n) {
        final Filter f = getFilter();
        if (activity != null && f != null) {
            List<Way> ways = getWaysForNode(n);
            if (!ways.isEmpty()) {
                for (Way w : ways) {
                    if (!f.include(w, false)) {
                        AttachedObjectWarning.showDialog(activity);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Dismiss the warning dialog
     * 
     * @param activity activity this method was called from
     */
    private void dismissAttachedObjectWarning(@Nullable FragmentActivity activity) {
        if (activity != null) {
            AttachedObjectWarning.dismissDialog(activity);
        }
    }

    /**
     * Get the Preferences instance held by this logic instance
     * 
     * @return the Preferences instance
     */
    public Preferences getPrefs() {
        return prefs;
    }

    /**
     * Get the ExecutorService allocated when this instance of Logic was created
     * 
     * @return the executorService
     */
    @NonNull
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Get the Handler allocated when this instance of Logic was created
     * 
     * @return the Handler
     */
    @NonNull
    public Handler getHandler() {
        return uiHandler;
    }
}
