package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.List;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Relation;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Display a dialog if we have found an empty relation giving the options or leaving it empty, deleting it or adding
 * some members.
 *
 */
public class EmptyRelation extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, EmptyRelation.class.getSimpleName().length());
    private static final String DEBUG_TAG = EmptyRelation.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG               = "fragment_empty_relation";
    private static final String RELATION_IDS_KEY  = "relations";
    private static final String CURRENT_INDEX_KEY = "index";

    private long[] relationIds;
    private int    currentIndex = 0;

    /**
     * Display a dialog if we have found an empty relation giving the options or leaving it empty, deleting it or adding
     * some members.
     * 
     * @param activity the calling Activity
     * @param relationId the id of the empty Relation
     */
    public static void showDialog(@NonNull FragmentActivity activity, final long relationId) {
        showDialog(activity, Util.wrapInList(relationId));
    }

    /**
     * Display a dialog if we have found (an) empty relation(s) giving the options or leaving it/them empty, deleting
     * it/them or adding some members (if there is just one).
     * 
     * @param activity the calling Activity
     * @param relationIds a List of relation ids
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull List<Long> relationIds) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            EmptyRelation emptyRelationFragment = newInstance(relationIds);
            emptyRelationFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new EmptyRelation dialog instance
     * 
     * @param relationIds ids of the empty Relations
     * @return a new EmptyRelation dialog instance
     */
    @NonNull
    private static EmptyRelation newInstance(final List<Long> relationIds) {
        EmptyRelation f = new EmptyRelation();

        Bundle args = new Bundle();
        long[] ids = new long[relationIds.size()];
        for (int i = 0; i < relationIds.size(); i++) {
            ids[i] = relationIds.get(i);
        }
        args.putLongArray(RELATION_IDS_KEY, ids);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            relationIds = savedInstanceState.getLongArray(RELATION_IDS_KEY);
            currentIndex = savedInstanceState.getInt(CURRENT_INDEX_KEY);
        } else {
            relationIds = getArguments().getLongArray(RELATION_IDS_KEY);
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        return createDialog(currentIndex);
    }

    /**
     * Create a dialog for a specific Relation
     * 
     * @param index the index for the relation id
     * @return a Dialog
     */
    @NonNull
    private AppCompatDialog createDialog(int index) {
        final Logic logic = App.getLogic();
        final Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, relationIds[index]);
        final FragmentActivity activity = requireActivity();
        Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(ThemeUtils.getResIdFromAttribute(activity, R.attr.alert_dialog));
        builder.setTitle(R.string.empty_relation_title);
        builder.setMessage(getString(R.string.empty_relation_message, r != null ? r.getDescription() : Long.toString(relationIds[index])));
        if (activity instanceof Main && relationIds.length == 1) {
            builder.setPositiveButton(R.string.add_members, (dialog, which) -> {
                logic.setSelectedNode(null);
                logic.setSelectedWay(null);
                logic.setSelectedRelation(null);
                ((Main) activity).startSupportActionMode(((Main) activity).getEasyEditManager().addRelationMembersMode(r));
            });
        }
        builder.setNeutralButton(R.string.leave_empty, (dialog, which) -> showNext(index));
        builder.setNegativeButton(R.string.Delete, (dialog, which) -> {
            if (r != null) {
                logic.performEraseRelation(activity, r, true);
            } else {
                Log.e(DEBUG_TAG, "Relation not in memory: " + relationIds[index]);
            }
            showNext(index);
        });
        return builder.create();
    }

    /**
     * Increment the index if possible and show the dialog for the next Relation
     * 
     * @param index the index
     */
    private void showNext(int index) {
        if (index < relationIds.length - 1) {
            currentIndex++;
            try {
                createDialog(currentIndex).show();
            } catch (IllegalStateException isex) {
                Log.e(DEBUG_TAG, "showNext", isex);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLongArray(RELATION_IDS_KEY, relationIds);
        outState.putInt(CURRENT_INDEX_KEY, currentIndex);
    }
}
