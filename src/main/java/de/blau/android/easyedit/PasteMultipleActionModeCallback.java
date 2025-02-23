package de.blau.android.easyedit;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main.UndoInterface;
import de.blau.android.R;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

/**
 * The only reason for this class is that we can show/hide the undo button depending on if we have pasted something or
 * not
 */
public class PasteMultipleActionModeCallback extends EasyEditActionModeCallback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, PasteMultipleActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = PasteMultipleActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private int pasteCount = 1; // one paste will happen before we are started

    private abstract class UndoListener implements UndoInterface, OnClickListener, OnLongClickListener {

        @Override
        public boolean onLongClick(View v) {
            ScreenMessage.toastTopWarning(v.getContext(), R.string.toast_unsupported_operation);
            return true;
        }
    }

    private final UndoListener undoListener;

    /**
     * Construct a new PasteMultipleActionModeCallback
     * 
     * @param manager the current EasyEditMAnager instance
     */
    public PasteMultipleActionModeCallback(@NonNull EasyEditManager manager) {
        super(manager);
        undoListener = new UndoListener() {

            @Override
            public void onClick(View v) {
                undo(App.getLogic());
            }

            @Override
            public void undo(Logic logic) {
                if (pasteCount > 0) {
                    String name = logic.undo(false);
                    if (name != null) {
                        ScreenMessage.toastTopInfo(main, main.getString(R.string.undo_message, name));
                    }
                    pasteCount--;
                }
                logic.getMap().invalidate();
                manager.invalidate();
            }
        };
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        helpTopic = R.string.help_simple_actions;
        super.onCreateActionMode(mode, menu);
        Log.d(DEBUG_TAG, "onCreateActionMode");

        mode.setTitle(R.string.menu_paste_multiple);
        mode.setSubtitle(R.string.simple_paste_multiple);

        // setup menu
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu = replaceMenu(menu, mode, this);
        menu.clear();
        menuUtil.reset();
        super.onPrepareActionMode(mode, menu);
        final MenuInflater menuInflater = main.getMenuInflater();
        menuInflater.inflate(R.menu.undo_action, menu);
        MenuItem undoItem = menu.findItem(R.id.undo_action);
        undoItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        View undoView = undoItem.getActionView();
        undoView.setOnClickListener(undoListener);
        undoView.setOnLongClickListener(undoListener);
        undoItem.setVisible(pasteCount > 0);
        SimpleActionModeCallback.setUpClipboardButtons(manager, main, menu);
        menu.add(GROUP_BASE, MENUITEM_HELP, Menu.CATEGORY_SYSTEM | 10, R.string.menu_help).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_help));
        return true;
    }

    @Override
    public boolean handleClick(float x, float y) {
        App.getLogic().pasteFromClipboard(main, 0, x, y);
        if (pasteCount == 0) {
            manager.invalidate();
        }
        pasteCount++;
        return true;
    }
}
