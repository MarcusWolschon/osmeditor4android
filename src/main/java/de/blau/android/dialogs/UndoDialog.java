package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerTabStrip;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.UndoStorage;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.Density;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.ExtendedViewPager;

public class UndoDialog extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, UndoDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = UndoDialog.class.getSimpleName().substring(0, TAG_LEN);

    public static final String TAG = "fragment_undo";

    public static final int REDO_PAGE = 1;

    /**
     * Instantiate and show the dialog
     * 
     * @param activity the calling FragmentActivity
     */
    public static void showDialog(FragmentActivity activity) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        UndoDialog undoDialogFragment = newInstance();
        try {
            undoDialogFragment.setShowsDialog(true);
            undoDialogFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
            ACRAHelper.nocrashReport(isex, isex.getMessage());
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling FragmentActivity
     */
    public static void dismissDialog(FragmentActivity activity) {
        Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of this Fragment
     * 
     * @return a new ConfirmUpload instance
     */
    private static UndoDialog newInstance() {
        return new UndoDialog();
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {

        Logic logic = App.getLogic();
        UndoStorage undo = logic.getUndo();
        FragmentActivity activity = getActivity();

        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        Builder builder = ThemeUtils.getAlertDialogBuilder(activity);
        builder.setTitle(R.string.checkpoints);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.undo_redo_tabs, null);
        ExtendedViewPager pager = (ExtendedViewPager) layout.findViewById(R.id.pager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) pager.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(activity, R.attr.colorAccent, R.color.dark_grey));

        pager.setAdapter(new ViewPagerAdapter(activity, layout, new int[] { R.id.undo_page, R.id.redo_page }, new int[] { R.string.undo, R.string.redo }));

        builder.setView(layout);
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();

        // UNDO
        final String[] undoActions = undo.getUndoActions(activity);

        List<UndoDialogItem> undoItems = new ArrayList<>();
        for (int i = undoActions.length - 1; i >= 0; i--) {
            undoItems.add(new UndoDialogItem(activity, undoActions.length - i, false, undoActions[i]));
        }
        ListView undoList = layout.findViewById(R.id.undo_checkpoints);
        final UndoAdapter undoAdapter = new UndoAdapter(activity, undoItems);
        undoList.setAdapter(undoAdapter);
        undoList.setOnItemClickListener(new UndoItemClickListener(activity, logic, dialog, undoActions));

        // REDO
        final String[] redoActions = undo.getRedoActions(activity);

        List<UndoDialogItem> redoItems = new ArrayList<>();
        for (int i = redoActions.length - 1; i >= 0; i--) {
            redoItems.add(new UndoDialogItem(activity, redoActions.length - i, true, redoActions[i]));
        }
        ListView redoList = layout.findViewById(R.id.redo_checkpoints);
        final UndoAdapter redoAdapter = new UndoAdapter(activity, redoItems);
        redoList.setAdapter(redoAdapter);
        redoList.setOnItemClickListener(new UndoItemClickListener(activity, logic, dialog, redoActions));

        if (undoItems.isEmpty() && !redoItems.isEmpty()) {
            pager.setCurrentItem(REDO_PAGE);
        }

        return dialog;
    }

    private class UndoItemClickListener implements OnItemClickListener {
        final FragmentActivity activity;
        final Logic            logic;
        final AlertDialog      undoDialog;
        final String[]         actions;

        /**
         * Construct a new listener
         * 
         * @param activity the current Activity
         * @param logic the current Logic instance
         * @param undoDialog the current AlertDialog
         * @param actions an array holding the list of items
         */
        UndoItemClickListener(@NonNull final FragmentActivity activity, @NonNull final Logic logic, @NonNull final AlertDialog undoDialog,
                @NonNull String[] actions) {
            this.activity = activity;
            this.logic = logic;
            this.undoDialog = undoDialog;
            this.actions = actions;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final UndoDialogItem item = (UndoDialogItem) parent.getAdapter().getItem(position);
            if (item.index > 1) { // not the top item
                AlertDialog.Builder builder = ThemeUtils.getAlertDialogBuilder(activity);
                builder.setTitle(item.isRedo ? R.string.redo : R.string.undo);
                builder.setNeutralButton(R.string.cancel, null);
                builder.setNegativeButton(R.string.undo_redo_one, (dialog, which) -> {
                    int checkpointIndex = actions.length - item.index;
                    if (item.isRedo) {
                        logic.redo(checkpointIndex);
                    } else {
                        logic.undo(checkpointIndex);
                    }
                    dismissAndInvalidate(activity, logic, undoDialog);
                });
                builder.setPositiveButton(item.isRedo ? R.string.redo_all : R.string.undo_all, (dialog, which) -> {
                    for (int i = 0; i < item.index; i++) {
                        undoRedoLast(logic, item.isRedo);
                    }
                    dismissAndInvalidate(activity, logic, undoDialog);
                });
                builder.create().show();
            } else { // just undo/redo top item without asking
                undoRedoLast(logic, item.isRedo);
                dismissAndInvalidate(activity, logic, undoDialog);
            }
        }

        /**
         * Undo or redo the last undo / redo checkpoint
         * 
         * @param logic the current
         * @param redo if true redo the checkpoint
         */
        private void undoRedoLast(@NonNull final Logic logic, boolean redo) {
            if (redo) {
                logic.redo();
            } else {
                logic.undo();
            }
        }

        /**
         * Dismiss the dialog and invalidate the main display
         * 
         * @param activity the current Activity
         * @param logic the current instance of logic
         * @param dialog the Dialog
         */
        private void dismissAndInvalidate(@NonNull final FragmentActivity activity, @NonNull Logic logic, @NonNull final DialogInterface dialog) {
            dialog.dismiss();
            if (activity instanceof Main) {
                ((Main) activity).resync(logic);
                ((Main) activity).invalidateMap();
                ((Main) activity).invalidateOptionsMenu();
            }
        }
    }

    private static class UndoAdapter extends ArrayAdapter<UndoDialogItem> {

        /**
         * Construct an adapter for the undo/redo elements
         * 
         * @param context an Android Context
         * @param objects a List of UndoDialogItem
         */
        public UndoAdapter(@NonNull Context context, @NonNull List<UndoDialogItem> objects) {
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

        /**
         * Construct a new UndoDialogItem
         * 
         * @param ctx an Android Context
         * @param index the index into the undo items
         * @param isRedo true if this is actually a redo item
         * @param name name for the item
         */
        private UndoDialogItem(@NonNull Context ctx, int index, boolean isRedo, @NonNull String name) {
            super(ctx);
            Resources r = ctx.getResources();
            CharSequence text = de.blau.android.util.Util.fromHtml(r.getString(isRedo ? R.string.redo : R.string.undo) + ": " + name);
            if (index == 1) {
                SpannableString latest = new SpannableString(r.getString(R.string.undo_latest));
                ThemeUtils.setSpanColor(ctx, latest, R.attr.colorAccent, R.color.material_teal);
                text = TextUtils.concat(latest, de.blau.android.util.Util.fromHtml("<BR>"), text);
            }
            setText(text);
            int pad = Density.dpToPx(ctx, 15);
            setPadding(pad, pad, pad, pad);
            setCompoundDrawablePadding(pad);
            int iconRes = isRedo ? ThemeUtils.getResIdFromAttribute(ctx, R.attr.undolist_redo) : ThemeUtils.getResIdFromAttribute(ctx, R.attr.undolist_undo);
            boolean rtl = de.blau.android.util.Util.isRtlScript(ctx);
            setCompoundDrawablesWithIntrinsicBounds(!rtl ? iconRes : 0, 0, rtl ? iconRes : 0, 0);
            this.index = index;
            this.isRedo = isRedo;
        }
    }
}
