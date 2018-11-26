package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.UndoStorage;
import de.blau.android.util.Density;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class UndoDialogFactory {

    public static void showUndoDialog(final Main main, final Logic logic, final UndoStorage undo) {
        Builder dialog = new Builder(main);
        dialog.setTitle(R.string.checkpoints);

        final String[] undoActions = undo.getUndoActions(main);
        final String[] redoActions = undo.getRedoActions(main);

        List<UndoDialogItem> items = new ArrayList<>();
        for (int i = 0; i < redoActions.length; i++) {
            items.add(new UndoDialogItem(main, redoActions.length - i, true, redoActions[i]));
        }
        for (int i = undoActions.length - 1; i >= 0; i--) {
            items.add(new UndoDialogItem(main, undoActions.length - i, false, undoActions[i]));
        }
        final UndoAdapter adapter = new UndoAdapter(main, items);
        dialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                final UndoDialogItem item = adapter.getItem(which);
                if (item.index > 1) { // not the top item
                    AlertDialog.Builder builder = new AlertDialog.Builder(main);
                    builder.setTitle(R.string.undo_redo_title);
                    builder.setNeutralButton(R.string.cancel, null);
                    builder.setNegativeButton(R.string.undo_redo_one, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            if (item.isRedo) {
                                logic.redo(redoActions.length - item.index);
                            } else {
                                logic.undo(undoActions.length - item.index);
                            }
                            dismissAndInvalidate(main, logic, dialog);
                        }
                    });
                    builder.setPositiveButton(item.isRedo ? R.string.redo_all : R.string.undo_all, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            for (int i = 0; i < item.index; i++) {
                                undoRedoLast(logic, item.isRedo);
                            }
                            dismissAndInvalidate(main, logic, dialog);
                        }
                    });
                    builder.create().show();
                } else { // just undo/redo top item without asking
                    undoRedoLast(logic, item.isRedo);
                    dismissAndInvalidate(main, logic, dialog);
                }
            }
        });
        dialog.show();
    }

    /**
     * Undo or redo the last undo / redo checkpoint
     * 
     * @param logic the current
     * @param redo if true redo the checkpoint
     */
    private static void undoRedoLast(@NonNull final Logic logic, boolean redo) {
        if (redo) {
            logic.redo();
        } else {
            logic.undo();
        }
    }

    /**
     * Dismiss the dialog and invalidate the main display
     * 
     * @param main the current instance of Main
     * @param logic the current instance of logic
     * @param dialog the Dialog
     */
    private static void dismissAndInvalidate(@NonNull final Main main, @NonNull Logic logic, @NonNull final DialogInterface dialog) {
        dialog.dismiss();
        main.resync(logic);
        main.invalidateMap();
        main.supportInvalidateOptionsMenu();
    }

    private static class UndoAdapter extends ArrayAdapter<UndoDialogItem> {
        public UndoAdapter(Context context, List<UndoDialogItem> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return getItem(position);
        }
    }

    private static final class UndoDialogItem extends AppCompatTextView {

        public final int     index;
        public final boolean isRedo;

        private UndoDialogItem(Context ctx, int index, boolean isRedo, String name) {
            super(ctx);
            Resources r = ctx.getResources();
            setText(Util.fromHtml(r.getString(isRedo ? R.string.redo : R.string.undo) + ": " + name));
            int pad = Density.dpToPx(15);
            setPadding(pad, pad, pad, pad);
            setCompoundDrawablePadding(pad);
            setCompoundDrawablesWithIntrinsicBounds(
                    isRedo ? ThemeUtils.getResIdFromAttribute(ctx, R.attr.undolist_redo) : ThemeUtils.getResIdFromAttribute(ctx, R.attr.undolist_undo), 0, 0,
                    0);
            this.index = index;
            this.isRedo = isRedo;
        }
    }

}
