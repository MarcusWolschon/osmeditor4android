package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

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

public class ElementIssueDialog extends ImmersiveDialogFragment {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ElementIssueDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = ElementIssueDialog.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_element_issue";

    private static final String TITLE_KEY   = "title_key";
    private static final String MESSAGE_KEY = "message_key";
    private static final String TIP_KEY_KEY = "tip_key_key";
    private static final String TIP_KEY     = "tip_key";
    private static final String RESULTS_KEY = "results";

    private int          titleRes;
    private int          messageRes;
    private int          tipKeyRes;
    private int          tipRes;
    private List<Result> result;

    /**
     * Show a dialog with a list of tag conflict issues
     * 
     * @param activity the calling FragmentActivity
     * @param result the List of Result elements
     */
    public static void showTagConflictDialog(@NonNull AppCompatActivity activity, @NonNull List<Result> result) {
        showDialog(activity, R.string.tag_conflict_title, R.string.tag_conflict_message, R.string.tip_tag_conflict_key, R.string.tip_tag_conflict, result);
    }

    /**
     * Show a dialog with a list of issues caused by replacing geometry
     * 
     * @param activity the calling FragmentActivity
     * @param result the List of Result elements
     */
    public static void showReplaceGeometryIssuetDialog(@NonNull AppCompatActivity activity, @NonNull List<Result> result) {
        showDialog(activity, R.string.replace_geometry_issue_title, R.string.replace_geometry_issue_message, R.string.tip_replace_geometry_key,
                R.string.tip_replace_geometry, result);
    }

    /**
     * Show a dialog with a list of issues
     * 
     * @param activity the calling FragmentActivity
     * @param titleRes resource id for the title
     * @param messageRes resource if for the message
     * @param tipKeyRes tip key resource
     * @param tipRes tip message resource
     * @param result the List of Result elements
     */
    private static void showDialog(@NonNull AppCompatActivity activity, int titleRes, int messageRes, int tipKeyRes, int tipRes, @NonNull List<Result> result) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (activity instanceof Main) {
                ((Main) activity).descheduleAutoLock();
            }
            ElementIssueDialog elementIssueFragment = newInstance(titleRes, messageRes, tipKeyRes, tipRes, result);
            elementIssueFragment.show(fm, TAG);
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
     * @param titleRes resource id for the title
     * @param messageRes resource if for the message
     * @param tipKeyRes tip key resource
     * @param tipRes tip message resource
     * @param result the List of Result elements
     * @return a TagConflictDialog instance
     */
    private static ElementIssueDialog newInstance(int titleRes, int messageRes, int tipKeyRes, int tipRes, @NonNull List<Result> result) {
        ElementIssueDialog f = new ElementIssueDialog();
        Bundle args = new Bundle();
        args.putInt(TITLE_KEY, titleRes);
        args.putInt(MESSAGE_KEY, messageRes);
        args.putInt(TIP_KEY_KEY, tipKeyRes);
        args.putInt(TIP_KEY, tipRes);
        args.putSerializable(RESULTS_KEY, (Serializable) result);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Recreating from saved state");
            titleRes = savedInstanceState.getInt(TITLE_KEY);
            messageRes = savedInstanceState.getInt(MESSAGE_KEY);
            tipKeyRes = savedInstanceState.getInt(TIP_KEY_KEY);
            tipRes = savedInstanceState.getInt(TIP_KEY);
            result = Util.getSerializeableArrayList(savedInstanceState, RESULTS_KEY, Result.class);
            // restore the elements
            for (Result r : result) {
                r.restoreElement(App.getDelegator());
            }
        } else {
            Bundle args = getArguments();
            titleRes = args.getInt(TITLE_KEY);
            messageRes = args.getInt(MESSAGE_KEY);
            tipKeyRes = args.getInt(TIP_KEY_KEY);
            tipRes = args.getInt(TIP_KEY);
            result = Util.getSerializeableArrayList(getArguments(), RESULTS_KEY, Result.class);
        }
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
        View layout = inflater.inflate(R.layout.tag_conflict, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(titleRes);

        // setMessage doesn't erally work with custom layouts, so DIY here
        TextView message = layout.findViewById(R.id.message);
        message.setText(messageRes);

        ListView list = layout.findViewById(R.id.elements);
        list.setAdapter(new ResultArrayAdapter(getContext(), R.layout.tag_conflict_item, R.id.text1, result));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Result clicked = result.get(position);
            OsmElement element = clicked.getElement();
            if (element != null) {
                ElementInfo.showDialog(getActivity(), App.getDelegator().getUndo().getUndoElements(element).size() - 1, element, false);
            } else {
                Log.e(DEBUG_TAG, "Clicked element is null");
            }
        });
        builder.setView(layout);
        builder.setPositiveButton(R.string.done, null);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        Tip.showDialog(getActivity(), tipKeyRes, tipRes);
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
        outState.putInt(TITLE_KEY, titleRes);
        outState.putInt(MESSAGE_KEY, messageRes);
        outState.putInt(TIP_KEY_KEY, tipKeyRes);
        outState.putInt(TIP_KEY, tipRes);
        List<Result> toSave = new ArrayList<>();
        // directly saving OsmElements is a bad idea
        // we need to copy everything as otherwise we
        // remove the reference to the element from
        // the in use copy of Result
        for (Result r : result) {
            Result savedResult = new Result(r);
            toSave.add(savedResult);
            savedResult.saveElement();
        }
        outState.putSerializable(RESULTS_KEY, new ArrayList<Result>(toSave));
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
            errorColor = ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.snack_error, R.color.material_red);
            warningColor = ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.snack_warning, R.color.material_orange);
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
