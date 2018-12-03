package de.blau.android.easyedit;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.layer.tasks.MapOverlay;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.TaskFragment;
import de.blau.android.util.Util;

public class NewNoteSelectionActionModeCallback extends EasyEditActionModeCallback {

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
        menu.add(Menu.NONE, MENUITEM_VIEW, Menu.NONE, R.string.menu_view).setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_tagedit));
        menu.add(Menu.NONE, MENUITEM_DELETE, Menu.CATEGORY_SYSTEM, R.string.delete);
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help)
                .setAlphabeticShortcut(Util.getShortCut(main, R.string.shortcut_help));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        super.onActionItemClicked(mode, item);
        switch (item.getItemId()) {
        case MENUITEM_VIEW:
            TaskFragment.showDialog(main, note);
            break;
        case MENUITEM_DELETE:
            new AlertDialog.Builder(main).setTitle(R.string.delete).setMessage(R.string.delete_note_description)
                    .setPositiveButton(R.string.delete_note, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getTaskStorage().delete(note);
                            main.getMap().invalidate();
                            if (mode != null) {
                                mode.finish();
                            }
                        }
                    }).show();
            break;
        }
        return true;
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
            TaskFragment.showDialog(main, note);
            return true;
        }
        return false;
    }
}
