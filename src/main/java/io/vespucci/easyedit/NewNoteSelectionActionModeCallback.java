package io.vespucci.easyedit;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.layer.tasks.MapOverlay;
import io.vespucci.tasks.Note;
import io.vespucci.tasks.NoteFragment;
import io.vespucci.util.Util;

public class NewNoteSelectionActionModeCallback extends EasyEditActionModeCallback {
    private static final String DEBUG_TAG = NewNoteSelectionActionModeCallback.class.getSimpleName().substring(0, Math.min(23, NewNoteSelectionActionModeCallback.class.getSimpleName().length()));

    private static final int MENUITEM_VIEW   = 1;
    private static final int MENUITEM_DELETE = 2;

    final Note               note;
    private final MapOverlay layer;

    /**
     * Construct a new callback for editing new nodes
     * 
     * @param manager the current EasyEditManager instance
     * @param note the Note to edit
     * @param layer the current task layer
     */
    public NewNoteSelectionActionModeCallback(@NonNull EasyEditManager manager, @NonNull Note note, @NonNull MapOverlay layer) {
        super(manager);
        this.note = note;
        this.layer = layer;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        helpTopic = R.string.help_newnoteselection;
        mode.setTitle(R.string.actionmode_newnoteselect);
        mode.setSubtitle(null);
        logic.setSelectedNode(null);
        logic.setSelectedWay(null);
        logic.setSelectedRelationWays(null);
        logic.setSelectedRelationNodes(null);
        main.invalidateMap();
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        super.onPrepareActionMode(mode, menu);
        menu.clear();
        menuUtil.reset();
        menu.add(Menu.NONE, MENUITEM_VIEW, Menu.NONE, R.string.menu_view);
        menu.add(Menu.NONE, MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete);
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_VIEW:
            NoteFragment.showDialog(main, note);
            break;
        case MENUITEM_DELETE:
            menuDelete();
            break;
        default:
            Log.w(DEBUG_TAG, "Unknown menu item " + item.getItemId());
        }
        return true;
    }

    /**
     * Delete the Note after showing a Dialog fr confirmation
     */
    private void menuDelete() {
        new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.delete_note_description)
                .setPositiveButton(R.string.delete_note, (dialog, which) -> {
                    App.getTaskStorage().delete(note);
                    main.getMap().invalidate();
                    if (mode != null) {
                        mode.finish();
                    }
                }).show();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        layer.deselectObjects();
    }

    /**
     * Handle a click on a Note when we are already active
     * 
     * @param note Note that was clicked
     * @return true if this was the Note that we currently are editing
     */
    public boolean handleNoteClick(Note note) {
        if (note.equals(this.note)) {
            NoteFragment.showDialog(main, note);
            return true;
        }
        return false;
    }

    @Override
    public boolean processShortcut(Character c) {
        if (c == Util.getShortCut(main, R.string.shortcut_remove)) {
            menuDelete();
            return true;
        }
        return super.processShortcut(c);
    }
}
