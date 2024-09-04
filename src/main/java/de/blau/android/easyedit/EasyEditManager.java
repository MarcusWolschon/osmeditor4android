package de.blau.android.easyedit;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.ActionMode.Callback;
import de.blau.android.App;
import de.blau.android.DisambiguationMenu;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.easyedit.route.RestartRouteSegmentActionModeCallback;
import de.blau.android.easyedit.route.RouteSegmentActionModeCallback;
import de.blau.android.easyedit.turnrestriction.FromElementActionModeCallback;
import de.blau.android.easyedit.turnrestriction.RestartFromElementActionModeCallback;
import de.blau.android.easyedit.turnrestriction.ToElementActionModeCallback;
import de.blau.android.easyedit.turnrestriction.ViaElementActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.tasks.Note;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SerializableState;
import de.blau.android.validation.Validator;

/**
 * This class starts the AvtionModes in "EasyEdit" mode (which is now the default)
 * 
 * @author Jan
 * @author Simon Poole
 *
 */
public class EasyEditManager {

    private static final String DEBUG_TAG = EasyEditManager.class.getSimpleName().substring(0, Math.min(23, EasyEditManager.class.getSimpleName().length()));

    private static final int INVALIDATION_DELAY = 100; // minimum delay before action mode will be invalidated

    private final Main  main;
    private final Logic logic;

    private ActionMode                 currentActionMode         = null;
    private EasyEditActionModeCallback currentActionModeCallback = null;
    private final Object               actionModeCallbackLock    = new Object();

    private String restartActionModeCallbackName = null;

    private boolean contextMenuEnabled;

    private static final List<String> restartable = Collections.unmodifiableList(Arrays.asList(RouteSegmentActionModeCallback.class.getCanonicalName(),
            RestartRouteSegmentActionModeCallback.class.getCanonicalName(), EditRelationMembersActionModeCallback.class.getCanonicalName(),
            PathCreationActionModeCallback.class.getCanonicalName(), WaySegmentModifyActionModeCallback.class.getCanonicalName(),
            WaySegmentActionModeCallback.class.getCanonicalName(), WaySplittingActionModeCallback.class.getCanonicalName(),
            ClosedWaySplittingActionModeCallback.class.getCanonicalName(), WaySelectPartActionModeCallback.class.getCanonicalName()));

    public static final String              FILENAME     = "easyeditmanager.res";
    private SavingHelper<SerializableState> savingHelper = new SavingHelper<>();

    /**
     * Construct a new instance of the manager
     * 
     * @param main the instance of Main we are being used from
     */
    public EasyEditManager(Main main) {
        this.main = main;
        this.logic = App.getLogic();
    }

    /**
     * Returns true if an ActionMode is currently active
     * 
     * @return true if we are in an ActionMode
     */
    public boolean isProcessingAction() {
        synchronized (actionModeCallbackLock) {
            return (currentActionModeCallback != null);
        }
    }

    /**
     * Check if an Element is selected
     * 
     * @return true if we are in one of the ELementSelection modes
     */
    public boolean inElementSelectedMode() {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback instanceof ElementSelectionActionModeCallback
                    || currentActionModeCallback instanceof MultiSelectWithGeometryActionModeCallback;
        }
    }

    /**
     * Check if a Way is selected
     * 
     * @return true if we are in one of the ELementSelection modes
     */
    public boolean inWaySelectedMode() {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback instanceof WaySelectionActionModeCallback;
        }
    }

    /**
     * Check if a new Note is selected
     * 
     * @return true if we are in new Note ActionMode
     */
    public boolean inNewNoteSelectedMode() {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback instanceof NewNoteSelectionActionModeCallback;
        }
    }

    /**
     * Check if we are creating a new path
     * 
     * @return true if we are in the PathCreationActionMode
     */
    public boolean inPathCreationMode() {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback instanceof PathCreationActionModeCallback;
        }
    }

    /**
     * Check if we are in multi-select mode
     * 
     * @return true if we are in the multi-select mode
     */
    public boolean inMultiSelectMode() {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback instanceof MultiSelectActionModeCallback;
        }
    }

    /**
     * Check if we are in a mode that supports dragging
     * 
     * @return true if we are in a supported ActionMode
     */
    public boolean draggingEnabled() {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback instanceof ElementSelectionActionModeCallback
                    || currentActionModeCallback instanceof MultiSelectWithGeometryActionModeCallback
                    || currentActionModeCallback instanceof NewNoteSelectionActionModeCallback
                    || currentActionModeCallback instanceof RotationActionModeCallback;
        }
    }

    /**
     * Call if you need to abort the current action mode
     */
    public void finish() {
        if (currentActionMode != null) {
            currentActionMode.finish();
            // remove any saved state
            try {
                new File(FILENAME).delete(); // NOSONAR requires API 26
            } catch (SecurityException e) {
                Log.e(DEBUG_TAG, "Deleting " + FILENAME + " raised " + e.getMessage());
            }
        }
    }

    /**
     * Call to let the action mode (if any) have a first go at the click.
     * 
     * @param x the x coordinate (screen coordinate?) of the click
     * @param y the y coordinate (screen coordinate?) of the click
     * @return true if the click was handled
     */
    public boolean actionModeHandledClick(float x, float y) {
        return (currentActionModeCallback != null && currentActionModeCallback.handleClick(x, y));
    }

    /**
     * Set the ActionMode and the corresponding callback
     * 
     * @param mode the new ActionMode
     * @param callback the new Callback
     */
    public void setCallBack(@Nullable ActionMode mode, @Nullable EasyEditActionModeCallback callback) {
        synchronized (actionModeCallbackLock) {
            currentActionMode = mode;
            currentActionModeCallback = callback;
        }
    }

    /**
     * Get the name of the current callback class as a string
     * 
     * @return the class name of the current callback or null
     */
    @Nullable
    public String getActionModeCallbackName() {
        return currentActionModeCallback == null ? null : currentActionModeCallback.getClass().getCanonicalName();
    }

    /**
     * Set the name of the callback to use when we are restarting
     * 
     * @param restartActionModeCallbackName the name
     */
    public void setRestartActionModeCallbackName(@NonNull String restartActionModeCallbackName) {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback == null) {
                this.restartActionModeCallbackName = restartActionModeCallbackName;
            }
        }
    }

    /**
     * Handle case where nothing is touched .
     * 
     * @param doubleTap action was a double tap if true
     */
    public void nothingTouched(boolean doubleTap) {
        // User clicked an empty area. If something is selected, deselect it.
        if (!doubleTap && inMultiSelectMode()) {
            ScreenMessage.toastTopInfo(getMain(), getMain().getString(R.string.toast_exit_multiselect));
            return; // don't deselect all just because we didn't hit anything
        }
        if (currentActionModeCallback instanceof EditRelationMembersActionModeCallback || currentActionModeCallback instanceof BuilderActionModeCallback
                || currentActionModeCallback instanceof ViaElementActionModeCallback || currentActionModeCallback instanceof ToElementActionModeCallback
                || currentActionModeCallback instanceof FromElementActionModeCallback
                || currentActionModeCallback instanceof RestartFromElementActionModeCallback
                || currentActionModeCallback instanceof RotationActionModeCallback) {
            ScreenMessage.toastTopInfo(getMain(), getMain().getString(R.string.toast_abort_actionmode));
            return;
        }
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback instanceof ElementSelectionActionModeCallback
                    || currentActionModeCallback instanceof MultiSelectActionModeCallback
                    || currentActionModeCallback instanceof WayAppendingActionModeCallback
                    || currentActionModeCallback instanceof NewNoteSelectionActionModeCallback) {
                currentActionMode.finish();
            }
        }
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        logic.setSelectedRelationWays(null);
        logic.setSelectedRelationNodes(null);
        main.getMap().deselectObjects();
    }

    /**
     * Handle editing the given element.
     * 
     * @param element The OSM element to edit.
     */
    public void editElement(@NonNull OsmElement element) {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback == null || !currentActionModeCallback.handleElementClick(element)) {
                // No callback or didn't handle the click, perform default (select element)
                ActionMode.Callback cb = null;
                if (element instanceof Node) {
                    cb = new NodeSelectionActionModeCallback(this, (Node) element);
                } else if (element instanceof Way) {
                    cb = new WaySelectionActionModeCallback(this, (Way) element);
                } else if (element instanceof Relation) {
                    cb = new RelationSelectionActionModeCallback(this, (Relation) element);
                }
                if (cb != null) {
                    getMain().startSupportActionMode(cb);
                    elementToast(element);
                }
            }
        }
    }

    /**
     * Display a toast with a description and a list of problems for an element
     * 
     * @param element the element to display the information for
     */
    private void elementToast(@NonNull OsmElement element) {
        StringBuilder toast = new StringBuilder(element.getDescription(getMain()));
        Validator validator = App.getDefaultValidator(getMain());
        if (element.hasProblem(getMain(), validator) != Validator.OK) {
            toast.append('\n');
            String[] problems = validator.describeProblem(getMain(), element);
            if (problems.length != 0) {
                for (String problem : problems) {
                    toast.append('\n');
                    toast.append(problem);
                }
            }
        }
        ScreenMessage.toastTopInfo(getMain(), toast.toString());
    }

    /**
     * Edit currently selected elements, tries to restart a previous mode if it has been saved
     */
    public void restart() {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback == null) {
                Log.d(DEBUG_TAG, "Trying to restart " + restartActionModeCallbackName);
                if (isRestartable(restartActionModeCallbackName)) {
                    new ExecutorTask<Void, Void, SerializableState>(logic.getExecutorService(), logic.getHandler()) {
                        @Override
                        protected SerializableState doInBackground(Void param) {
                            return savingHelper.load(main, FILENAME, false, true, true);
                        }

                        @Override
                        protected void onPostExecute(SerializableState state) {
                            try {
                                if (state != null) {
                                    try {
                                        Class<?> clazz = Class.forName(restartActionModeCallbackName);
                                        Constructor<?> constructor = clazz.getConstructor(EasyEditManager.class, SerializableState.class);
                                        ActionMode.Callback cb = (ActionMode.Callback) constructor.newInstance(EasyEditManager.this, state);
                                        getMain().startSupportActionMode(cb);
                                        return;
                                    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                                            | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException exception) {
                                        Log.e(DEBUG_TAG, "Restarting " + restartActionModeCallbackName + " received " + exception.getClass().getCanonicalName()
                                                + " " + exception.getMessage());
                                    }
                                }
                                Log.e(DEBUG_TAG, "restart, saved state is null");
                                startElementSelectionMode();
                            } finally {
                                restartActionModeCallbackName = null;
                            }
                        }
                    }.execute();
                } else {
                    startElementSelectionMode();
                }
            }

        }
    }

    /**
     * Edit currently selected elements
     */
    public void editElements() {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback == null) {
                startElementSelectionMode();
            }
        }
    }

    /**
     * Start an element selection mode based on selected elements
     */
    public void startElementSelectionMode() {
        ActionMode.Callback cb = null;
        OsmElement e = null;
        List<OsmElement> selection = logic.getSelectedElements();
        if (selection.size() == 1) {
            e = selection.get(0);
            if (e instanceof Node) {
                cb = new NodeSelectionActionModeCallback(this, (Node) e);
            } else if (e instanceof Way) {
                cb = new WaySelectionActionModeCallback(this, (Way) e);
            } else if (e instanceof Relation) {
                cb = new RelationSelectionActionModeCallback(this, (Relation) e);
            }
        } else {
            cb = new MultiSelectWithGeometryActionModeCallback(this, selection);
        }
        if (cb != null && (e != null || !selection.isEmpty())) {
            getMain().startSupportActionMode(cb);
            if (e != null) {
                elementToast(e);
            }
        }
    }

    /**
     * Check if the saved ActionModeCallback cab be restarted or not
     * 
     * @param actionModeCallbackName the name of the callback
     * @return true if the callback can be restarted
     */
    private boolean isRestartable(@NonNull String actionModeCallbackName) {
        return restartable.contains(actionModeCallbackName);
    }

    /**
     * Start adding elements to an existing empty relation
     * 
     * @param r the Relation to add elements to
     * @return the ActionMode.Callback
     */
    public Callback addRelationMembersMode(@NonNull Relation r) {
        return new EditRelationMembersActionModeCallback(this, r, (OsmElement) null);
    }

    /**
     * Indicate if the current mode uses long click internally, that is not for new nodes and ways
     * 
     * @return true if the current mode uses long click
     */
    public boolean usesLongClick() {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback != null && currentActionModeCallback.usesLongClick();
        }
    }

    /**
     * This gets called when the map is long-clicked in easy-edit mode and a element is the target
     * 
     * @param v the View that was long clicked
     * @param e an OsmElement
     * @return true if we handled the click
     */
    public boolean handleLongClick(@Nullable View v, @NonNull OsmElement e) {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback != null && currentActionModeCallback.handleElementLongClick(e)) {
                if (v != null) {
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
                return true;
            }
            return false;
        }
    }

    /**
     * This gets called when the map is long-clicked in easy-edit mode
     * 
     * @param v the View that was long clicked
     * @param x screen X coordinate
     * @param y screen Y coordinate
     * @return true if we handled the click
     */
    public boolean handleLongClick(@NonNull View v, float x, float y) {
        synchronized (actionModeCallbackLock) {
            if ((currentActionModeCallback instanceof PathCreationActionModeCallback)) {
                // we don't do long clicks in the above modes
                Log.d("EasyEditManager", "handleLongClick ignoring long click");
                return false;
            }
        }
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        if (getMain().startSupportActionMode(new LongClickActionModeCallback(this, x, y)) == null) {
            getMain().startSupportActionMode(new PathCreationActionModeCallback(this, x, y));
        }
        return true;
    }

    /**
     * Start ExtendedSelection / multi-select mode
     * 
     * @param osmElement initial selected element
     */
    public void startExtendedSelection(@NonNull OsmElement osmElement) {
        synchronized (actionModeCallbackLock) {
            if ((currentActionModeCallback instanceof WaySelectionActionModeCallback) || (currentActionModeCallback instanceof NodeSelectionActionModeCallback)
                    || (currentActionModeCallback instanceof RelationSelectionActionModeCallback)) {
                // one element already selected
                ((ElementSelectionActionModeCallback) currentActionModeCallback).deselect = false; // keep the element
                                                                                                   // visually selected
                getMain().startSupportActionMode(
                        new MultiSelectWithGeometryActionModeCallback(this, ((ElementSelectionActionModeCallback) currentActionModeCallback).element));
                // add 2nd element
                ((MultiSelectWithGeometryActionModeCallback) currentActionModeCallback).handleElementClick(osmElement);
            } else if (currentActionModeCallback instanceof MultiSelectWithGeometryActionModeCallback) {
                // ignore for now
            } else if (currentActionModeCallback != null) {
                // ignore for now
            } else {
                // nothing selected
                getMain().startSupportActionMode(new MultiSelectWithGeometryActionModeCallback(this, osmElement));
            }
        }
    }

    private Runnable invalidateMode = () -> {
        synchronized (actionModeCallbackLock) {
            if (currentActionMode != null) {
                currentActionMode.invalidate();
            }
        }
    };

    /**
     * Invalidate the menu of the current ActionMode
     */
    public void invalidate() {
        synchronized (actionModeCallbackLock) {
            if (currentActionMode != null) {
                if (currentActionModeCallback != null) {
                    currentActionModeCallback.update();
                }
                Map map = main.getMap();
                if (map != null) {
                    map.removeCallbacks(invalidateMode);
                    map.postDelayed(invalidateMode, INVALIDATION_DELAY);
                }
            }
        }
    }

    /**
     * Invalidate if needed when new data is present
     */
    public void invalidateOnDownload() {
        if (currentActionModeCallback instanceof BuilderActionModeCallback) {
            invalidate();
        }
    }

    /**
     * call the onBackPressed method for the currently active action mode
     * 
     * @return true if the press was consumed
     */
    public boolean handleBackPressed() {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback != null) {
                Log.d(DEBUG_TAG, "handleBackPressed for " + currentActionModeCallback.getClass().getSimpleName());
                return currentActionModeCallback.onBackPressed();
            }
            return false;
        }
    }

    /**
     * Process keyboard shortcuts
     * 
     * @param c the character
     * @return true if we processed the character
     */
    public boolean processShortcut(Character c) {
        synchronized (actionModeCallbackLock) {
            return currentActionModeCallback != null && currentActionModeCallback.processShortcut(c);
        }
    }

    /**
     * Show the context menu programmatically
     * 
     * This is slightly complicated because Android will always show a menu on long press if one has been created
     */
    public void showDisambiguationMenu() {
        synchronized (actionModeCallbackLock) {
            contextMenuEnabled = true;
        }
        main.showDisambiguationMenu();
    }

    /**
     * Call the per ActionMode onCreateContextMenu
     * 
     * @param menu the ContextMenu
     * @return true if a menu was created
     */
    public boolean createDisambiguationMenu(DisambiguationMenu menu) {
        try {
            synchronized (actionModeCallbackLock) {
                if (currentActionModeCallback != null && contextMenuEnabled && isProcessingAction()) {
                    return currentActionModeCallback.onCreateDisambiguationMenu(menu);
                }
            }
            return false;
        } finally {
            synchronized (actionModeCallbackLock) {
                contextMenuEnabled = false;
            }
        }
    }

    /**
     * Check if the current mode only supports OSM elements for selection
     * 
     * @return true if the current mode only supports OSM elements for selection
     */
    public boolean elementsOnly() {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback != null) {
                return currentActionModeCallback.elementsOnly();
            }
        }
        return false;
    }

    /**
     * @return the current instance of Main
     */
    public Main getMain() {
        return main;
    }

    /**
     * Start a mode for new Notes (can be moved and edited)
     * 
     * @param note the Note to edit
     * @param layer the current task layer
     * @return true if the Note editing mode could be started
     */
    public boolean editNote(@NonNull Note note, @NonNull de.blau.android.layer.tasks.MapOverlay layer) {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback == null || inElementSelectedMode() || (currentActionModeCallback instanceof NewNoteSelectionActionModeCallback
                    && !((NewNoteSelectionActionModeCallback) currentActionModeCallback).handleNoteClick(note))) {
                getMain().startSupportActionMode(new NewNoteSelectionActionModeCallback(this, note, layer));
                return true;
            }
        }
        return false;
    }

    /**
     * Save the state of the current ActionMode.Callback
     */
    public void saveState() {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback != null) {
                SerializableState state = new SerializableState();
                currentActionModeCallback.saveState(state);
                savingHelper.save(main, FILENAME, state, false, true);
            }
        }
    }

    /**
     * Update the selection
     * 
     * If nothing is selected the current mode is terminated, in multi-select the selection is adjusted
     */
    public void updateSelection() {
        synchronized (actionModeCallbackLock) {
            // only need to test if anything at all is still selected
            if (logic.selectedNodesCount() + logic.selectedWaysCount() + logic.selectedRelationsCount() == 0) {
                finish();
            } else if (currentActionModeCallback instanceof MultiSelectWithGeometryActionModeCallback) {
                ((MultiSelectWithGeometryActionModeCallback) currentActionModeCallback).updateSelection();
            }
        }
    }
}
