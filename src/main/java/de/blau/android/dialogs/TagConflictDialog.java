package de.blau.android.dialogs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Issue;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Result;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public class TagConflictDialog extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = TagConflictDialog.class.getSimpleName();

    private static final String TAG = "fragment_tag_conflict";

    private static final String RESULTS_KEY = "results";

    private List<Result> result;

    /**
     * Show a dialog with a list of issues
     * 
     * @param activity the calling FragmentActivity
     * @param result the List of Result elements
     */
    public static void showDialog(@NonNull AppCompatActivity activity, @NonNull List<Result> result) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            TagConflictDialog tagConflictFragment = newInstance(result);
            tagConflictFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull AppCompatActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @param result the List of Result elements
     * @return a TagConflictDialog instance
     */
    private static TagConflictDialog newInstance(@NonNull List<Result> result) {
        TagConflictDialog f = new TagConflictDialog();
        Bundle args = new Bundle();
        args.putSerializable(RESULTS_KEY, (Serializable) result);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            result = (List<Result>) savedInstanceState.getSerializable(RESULTS_KEY);
            // restore the elements
            for (Result r : result) {
                r.restoreElement(App.getDelegator());
            }
        } else {
            result = (List<Result>) getArguments().getSerializable(RESULTS_KEY);
        }
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        View layout = inflater.inflate(R.layout.tag_conflict, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.tag_conflict_title);
        builder.setMessage(R.string.tag_conflict_message);

        ListView list = layout.findViewById(R.id.elements);
        list.setAdapter(new ResultArrayAdapter(getContext(), R.layout.tag_conflict_item, R.id.text1, result));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Result clicked = result.get(position);
            OsmElement element = clicked.getElement();
            ElementInfo.showDialog(getActivity(), App.getDelegator().getUndo().getUndoElements(element).size() - 1, element, false);
        });
        builder.setView(layout);
        builder.setPositiveButton(R.string.done, null);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        Tip.showDialog(getActivity(), R.string.tip_tag_conflict_key, R.string.tip_tag_conflict);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof Main) {
            ((Main) getActivity()).scheduleAutoLock();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        // directly saving OsmElements is a bad idea
        for (Result r : result) {
            r.saveElement();
        }
        outState.putSerializable(RESULTS_KEY, new ArrayList<Result>(result));
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    /**
     * Highlight results that have a potential issue
     * 
     * @author Simon Poole
     *
     */
    private class ResultArrayAdapter extends ArrayAdapter<Result> {
        final List<Result> results;
        final int          warningColor;
        final int          errorColor;

        /**
         * Construct a new instance of the Adapter
         * 
         * @param context an Android COntext
         * @param layout the layout id
         * @param resource the id of the TextView in the layout
         * @param results the List of Result
         */
        public ResultArrayAdapter(@NonNull Context context, int layout, int resource, @NonNull List<Result> results) {
            super(context, layout, resource, results);
            this.results = results;
            errorColor = ThemeUtils.getStyleAttribColorValue(getContext(), R.color.material_red, 0xFFF44336);
            warningColor = ThemeUtils.getStyleAttribColorValue(getContext(), R.color.material_orange, 0xFFFF9800);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            View v = super.getView(position, convertView, container);
            TextView elementView = (TextView) v.findViewById(R.id.text1);
            if (elementView != null) {
                final Result r = results.get(position);
                OsmElement element = r.getElement();
                elementView.setText(element.getDescription(getContext()));
                SpannableStringBuilder spanBuilder = new SpannableStringBuilder();
                if (r.hasIssue()) {
                    for (Issue issue : r.getIssues()) {
                        SpannableString issueSpan = new SpannableString(issue.toTranslatedString(getContext()));
                        issueSpan.setSpan(new ForegroundColorSpan(issue.isError() ? errorColor : warningColor), 0, issueSpan.length(), 0);
                        spanBuilder.append(issueSpan);
                        spanBuilder.append(" ");
                    }
                } else {
                    Log.w(DEBUG_TAG, "Element " + element.getDescription(getContext()) + " has no issues");
                }
                TextView issueView = (TextView) v.findViewById(R.id.issues);
                issueView.setText(spanBuilder);
            } else {
                Log.e("ResultAdapterView", "position " + position + " view is null");
            }
            return v;
        }
    }
}
