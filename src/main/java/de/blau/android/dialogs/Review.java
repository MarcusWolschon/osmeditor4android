package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.osm.OsmElement;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * Dialog for review of changes (cut down version of ReviewAndUpload
 *
 */
public class Review extends AbstractReviewDialog {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Review.class.getSimpleName().length());
    private static final String DEBUG_TAG = Review.class.getSimpleName().substring(0, TAG_LEN);

    public static final String TAG = "fragment_review";

    private static final String STATE_FILENAME = "review_state" + "." + FileExtensions.RES;

    private ListView    listView;
    private Set<String> checked;

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
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
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

        Builder builder = ThemeUtils.getAlertDialogBuilder(activity);
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
            ReviewAndUpload.showDialog(activity, App.getDelegator().addRequiredElements(activity, toUpload));
        });

        AppCompatDialog dialog = builder.create();
        checked = new SavingHelper<HashSet<String>>().load(getContext(), STATE_FILENAME, false);
        dialog.setOnShowListener((DialogInterface d) -> ((AlertDialog) d).getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(!Util.isEmpty(checked)));
        return dialog;
    }

    @Override
    protected void createChangesView() {
        addChangesToView(getActivity(), listView, elements, DEFAULT_COMPARATOR, getArguments().getString(TAG_KEY), R.layout.changes_list_item_with_checkbox,
                (OsmElement e) -> checked != null && checked.contains(getElementKey(e)), () -> {
                    ValidatorArrayAdapter adapter = (ValidatorArrayAdapter) listView.getAdapter();
                    adapter.registerDataSetObserver(new ListObserver());
                });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        saveState();
    }

    /**
     * Save the selection state
     */
    private void saveState() {
        Log.d(DEBUG_TAG, "Saving selection state");
        HashSet<String> checked = new HashSet<>();
        for (ChangedElement e : ((ValidatorArrayAdapter) listView.getAdapter()).elements) {
            if (e.selected) {
                checked.add(getElementKey(e.element));
            }
        }
        new SavingHelper<HashSet<String>>().save(getContext(), STATE_FILENAME, checked, false);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        saveState();
    }

    private final class ListObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            final ValidatorArrayAdapter validatorArrayAdapter = (ValidatorArrayAdapter) listView.getAdapter();
            final ChangedElement[] changedElements = validatorArrayAdapter.elements;
            final int childCount = listView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                CheckBox checkBox = (CheckBox) listView.getChildAt(i).findViewById(R.id.checkBox1);
                if (checkBox != null) {
                    checkBox.setOnCheckedChangeListener(null);
                    checkBox.setChecked(changedElements[i].selected);
                    checkBox.setOnCheckedChangeListener(validatorArrayAdapter.getOnCheckedChangeListener(i));
                }
            }

            boolean somethingSelected = false;
            boolean somethingNotSelected = false;
            for (ChangedElement e : changedElements) {
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        saveState();
    }

    /**
     * Get an unique key for a specific version of an OsmElement
     * 
     * @param e the element
     * @return an unique string
     */
    @NonNull
    private String getElementKey(@NonNull OsmElement e) {
        return e.getName() + Long.toString(e.getOsmId()) + "_" + Long.toString(e.getOsmVersion());
    }
}
