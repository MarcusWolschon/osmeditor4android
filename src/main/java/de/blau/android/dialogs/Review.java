package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.ThemeUtils;

/**
 * Dialog for review of changes (cut down version of ReviewAndUpload
 *
 */
public class Review extends AbstractReviewDialog {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Review.class.getSimpleName().length());
    private static final String DEBUG_TAG = Review.class.getSimpleName().substring(0, TAG_LEN);

    public static final String TAG = "fragment_review";

    private ListView listView;

    /**
     * Instantiate and show the dialog
     * 
     * @param activity the calling FragmentActivity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        Review reviewDialogFragment = newInstance();
        try {
            reviewDialogFragment.show(fm, TAG);
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
    public static void dismissDialog(@NonNull FragmentActivity activity) {
        Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of this Fragment
     * 
     * @return a new ConfirmUpload instance
     */
    @NonNull
    private static Review newInstance() {
        Review f = new Review();
        Bundle args = new Bundle();
        args.putString(TAG_KEY, TAG);
        f.setArguments(args);
        f.setShowsDialog(true);
        return f;
    }

    private OnCheckedChangeListener selectAllListener = (CompoundButton buttonView, boolean isChecked) -> {
        final ValidatorArrayAdapter adapter = (ValidatorArrayAdapter) listView.getAdapter();
        for (ChangedElement e : adapter.elements) {
            e.selected = isChecked;
        }
        adapter.notifyDataSetChanged();
    };

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.review_changes_title);

        final View layout = inflater.inflate(R.layout.review, null);
        builder.setView(layout);

        listView = layout.findViewById(R.id.upload_changes);

        CheckBox checkbox = layout.findViewById(R.id.checkBoxAll);
        checkbox.setOnCheckedChangeListener(selectAllListener);

        builder.setNegativeButton(R.string.Done, null);
        builder.setNeutralButton(R.string.review_upload_selected, (DialogInterface dialog, int which) -> {
            List<OsmElement> toUpload = new ArrayList<>();
            for (ChangedElement e : ((ValidatorArrayAdapter) listView.getAdapter()).elements) {
                if (e.selected) {
                    toUpload.add(e.element);
                }
            }
            ReviewAndUpload.showDialog(activity, toUpload);
        });

        AppCompatDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface d) -> ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false));

        return dialog;
    }

    @Override
    protected void createChangesView() {
        addChangesToView(getActivity(), listView, elements, DEFAULT_COMPARATOR, getArguments().getString(TAG_KEY), R.layout.changes_list_item_with_checkbox);
        listView.getAdapter().registerDataSetObserver(new ListObserver());
    }

    private class ListObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            boolean somethingSelected = false;
            boolean somethingNotSelected = false;
            for (ChangedElement e : ((ValidatorArrayAdapter) listView.getAdapter()).elements) {
                if (e.selected && !somethingSelected) {
                    somethingSelected = true;
                    continue;
                }
                if (!e.selected && !somethingNotSelected) {
                    somethingNotSelected = true;
                }
            }
            final CheckBox checkBox = (CheckBox) requireDialog().findViewById(R.id.checkBoxAll);
            if (somethingNotSelected && checkBox.isChecked()) {
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(!somethingNotSelected);
                checkBox.setOnCheckedChangeListener(selectAllListener);
            }
            ((AlertDialog) requireDialog()).getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(somethingSelected);
        }
    }
}
