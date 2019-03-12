package de.blau.android.dialogs;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Relation;
import de.blau.android.util.ThemeUtils;

/**
 * Display a dialog if we have found an empty relation giving the options or leaving it empty, deleting it or adding
 * some members.
 *
 */
public class EmptyRelation extends DialogFragment {

    private static final String DEBUG_TAG = EmptyRelation.class.getSimpleName();

    private static final String TAG             = "fragment_empty_relation";
    private static final String RELATION_ID_KEY = "relation";

    private long relationId = -1L;

    /**
     * Display a dialog if we have found an empty relation giving the options or leaving it empty, deleting it or adding
     * some members.
     * 
     * @param activity the calling Activity
     * @param relationId the id of the empty Relation
     */
    public static void showDialog(@NonNull FragmentActivity activity, final long relationId) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            EmptyRelation emptyRelationFragment = newInstance(relationId);
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
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "dismissDialog", isex);
        }
    }

    /**
     * Create a new EmptyRelation dialog instance
     * 
     * @param relationId the id of the empty Relation
     * @return a new EmptyRelation dialog instance
     */
    @NonNull
    private static EmptyRelation newInstance(final long relationId) {
        EmptyRelation f = new EmptyRelation();

        Bundle args = new Bundle();
        args.putLong(RELATION_ID_KEY, relationId);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        relationId = getArguments().getLong(RELATION_ID_KEY);
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final Logic logic = App.getLogic();
        final Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, relationId);
        final FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(ThemeUtils.getResIdFromAttribute(activity, R.attr.alert_dialog));
        builder.setTitle(R.string.empty_relation_title);
        builder.setMessage(getString(R.string.empty_relation_message, r != null ? r.getDescription() : Long.toString(relationId)));
        if (activity instanceof Main) {
            builder.setPositiveButton(R.string.add_members, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    logic.setSelectedNode(null);
                    logic.setSelectedWay(null);
                    logic.setSelectedRelation(null);
                    ((Main) activity).startSupportActionMode(((Main) activity).getEasyEditManager().addRelationMembersMode(r));
                }
            });
        }
        builder.setNeutralButton(R.string.leave_empty, null);
        builder.setNegativeButton(R.string.Delete, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (r != null) {
                    logic.performEraseRelation(activity, r, true);
                } else {
                    Log.e(DEBUG_TAG, "Relation not in memory: " + relationId);
                }
            }
        });

        return builder.create();
    }
}
