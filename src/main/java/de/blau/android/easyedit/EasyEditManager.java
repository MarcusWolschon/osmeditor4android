package de.blau.android.easyedit;

import java.util.ArrayList;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.util.Log;
import android.view.ContextMenu;
import android.view.HapticFeedbackConstants;
import android.view.View;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.easyedit.turnrestriction.FromElementActionModeCallback;
import de.blau.android.easyedit.turnrestriction.RestartFromElementActionModeCallback;
import de.blau.android.easyedit.turnrestriction.ToElementActionModeCallback;
import de.blau.android.easyedit.turnrestriction.ViaElementActionModeCallback;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.tasks.Note;
import de.blau.android.util.Snack;
import de.blau.android.validation.Validator;

/**
 * This class starts the AvtionModes in "EasyEdit" mode (which is now the default)
 * 
 * @author Jan
 * @author Simon Poole
 *
 */
public class EasyEditManager {

    private static final String DEBUG_TAG = EasyEditManager.class.getSimpleName();

    private final Main                 main;
    private final Logic                logic;
    /** the touch listener from Main */

    private ActionMode                 currentActionMode         = null;
    private EasyEditActionModeCallback currentActionModeCallback = null;
    private final Object               actionModeCallbackLock    = new Object();

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
                    || currentActionModeCallback instanceof ExtendSelectionActionModeCallback;
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
     * call if you need to abort the current action mode
     */
    public void finish() {
        if (currentActionMode != null) {
            currentActionMode.finish();
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
    public void setCallBack(ActionMode mode, EasyEditActionModeCallback callback) {
        synchronized (actionModeCallbackLock) {
            currentActionMode = mode;
            currentActionModeCallback = callback;
        }
    }

    /**
     * Handle case where nothing is touched .
     * 
     * @param doubleTap action was a double tap if true
     */
    public void nothingTouched(boolean doubleTap) {
        // User clicked an empty area. If something is selected, deselect it.
        if (!doubleTap && currentActionModeCallback instanceof ExtendSelectionActionModeCallback) {
            Snack.toastTopInfo(getMain(), getMain().getString(R.string.toast_exit_multiselect));
            return; // don't deselect all just because we didn't hit anything
        }
        if (currentActionModeCallback instanceof AddRelationMemberActionModeCallback) {
            Snack.toastTopInfo(getMain(), getMain().getString(R.string.toast_exit_actionmode));
            return; // don't deselect all just because we didn't hit anything
        }
        if (currentActionModeCallback instanceof ViaElementActionModeCallback || currentActionModeCallback instanceof ToElementActionModeCallback
                || currentActionModeCallback instanceof FromElementActionModeCallback
                || currentActionModeCallback instanceof RestartFromElementActionModeCallback) {
            Snack.toastTopInfo(getMain(), getMain().getString(R.string.toast_abort_actionmode));
            return;
        }
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback instanceof ElementSelectionActionModeCallback
                    || currentActionModeCallback instanceof ExtendSelectionActionModeCallback
                    || currentActionModeCallback instanceof WayRotationActionModeCallback || currentActionModeCallback instanceof WayAppendingActionModeCallback
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
        Snack.toastTopInfo(getMain(), toast.toString());
    }

    /**
     * Edit currently selected elements.
     */
    public void editElements() {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback == null) {
                // No callback or didn't handle the click, perform default (select element)
                ActionMode.Callback cb = null;
                OsmElement e = null;
                if (logic.getSelectedNodes() != null && logic.getSelectedNodes().size() == 1 && logic.getSelectedWays() == null
                        && logic.getSelectedRelations() == null) {
                    e = logic.getSelectedNode();
                    cb = new NodeSelectionActionModeCallback(this, (Node) e);
                } else if (logic.getSelectedNodes() == null && logic.getSelectedWays() != null && logic.getSelectedWays().size() == 1
                        && logic.getSelectedRelations() == null) {
                    e = logic.getSelectedWay();
                    cb = new WaySelectionActionModeCallback(this, (Way) e);
                } else if (logic.getSelectedNodes() == null && logic.getSelectedWays() == null && logic.getSelectedRelations() != null
                        && logic.getSelectedRelations().size() == 1) {
                    e = logic.getSelectedRelations().get(0);
                    cb = new RelationSelectionActionModeCallback(this, (Relation) e);
                } else if (logic.getSelectedNodes() != null || logic.getSelectedWays() != null || logic.getSelectedRelations() != null) {
                    ArrayList<OsmElement> selection = new ArrayList<>();
                    if (logic.getSelectedNodes() != null) {
                        selection.addAll(logic.getSelectedNodes());
                    }
                    if (logic.getSelectedWays() != null) {
                        selection.addAll(logic.getSelectedWays());
                    }
                    if (logic.getSelectedRelations() != null) {
                        selection.addAll(logic.getSelectedRelations());
                    }
                    cb = new ExtendSelectionActionModeCallback(this, selection);
                }
                if (cb != null) {
                    getMain().startSupportActionMode(cb);
                    if (e != null) {
                        elementToast(e);
                    }
                }
            }
        }
    }

    /**
     * Start adding elements to an existing empty relation
     * 
     * @param r the Relation to add elements to
     * @return the ActionMode.Callback
     */
    public Callback addRelationMembersMode(@NonNull Relation r) {
        return new AddRelationMemberActionModeCallback(this, r, null);
    }

    /**
     * This gets called when the map is long-clciked in easy-edit mode
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
                return false; // this probably should really return true aka click handled
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
                        new ExtendSelectionActionModeCallback(this, ((ElementSelectionActionModeCallback) currentActionModeCallback).element));
                // add 2nd element FIXME may need some checks
                ((ExtendSelectionActionModeCallback) currentActionModeCallback).handleElementClick(osmElement);
            } else if (currentActionModeCallback instanceof ExtendSelectionActionModeCallback) {
                // ignore for now
            } else if (currentActionModeCallback != null) {
                // ignore for now
            } else {
                // nothing selected
                getMain().startSupportActionMode(new ExtendSelectionActionModeCallback(this, osmElement));
            }
        }
    }

    /**
     * Invalidate the menu of the current ActionMode
     */
    public void invalidate() {
        synchronized (actionModeCallbackLock) {
            if (currentActionMode != null) {
                if (currentActionModeCallback != null) {
                    currentActionModeCallback.update();
                }
                currentActionMode.invalidate();
            }
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
     * Handle results from starting an activity with an Intent
     * 
     * @param requestCode the Intent request code
     * @param resultCode the Intent result code
     * @param data any returned data
     */
    public void handleActivityResult(final int requestCode, final int resultCode, final Intent data) {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback instanceof LongClickActionModeCallback) {
                ((LongClickActionModeCallback) currentActionModeCallback).handleActivityResult(requestCode, resultCode, data);
            }
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
     * Check if the ActionMode wants its own context menu
     * 
     * @return true if we want to show our own context menu
     */
    public boolean needsCustomContextMenu() {
        return isProcessingAction() && currentActionModeCallback.needsCustomContextMenu();
    }

    /**
     * Call the per ActionMode onCreateContextMenu
     * 
     * @param menu the ContextMenu
     */
    public void createContextMenu(ContextMenu menu) {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback != null) {
                currentActionModeCallback.onCreateContextMenu(menu);
            }
        }
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
     */
    public void editNote(@NonNull Note note, @NonNull de.blau.android.layer.tasks.MapOverlay layer) {
        synchronized (actionModeCallbackLock) {
            if (currentActionModeCallback == null || (currentActionModeCallback instanceof NewNoteSelectionActionModeCallback
                    && !((NewNoteSelectionActionModeCallback) currentActionModeCallback).handleNoteClick(note))) {
                getMain().startSupportActionMode(new NewNoteSelectionActionModeCallback(this, note, layer));
            }
        }
    }
}
