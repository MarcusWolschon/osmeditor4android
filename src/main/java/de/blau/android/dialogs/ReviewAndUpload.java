package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerTabStrip;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.listener.UploadListener;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.tagform.TextRow;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.FilterlessArrayAdapter;
import de.blau.android.util.LocaleUtils;
import de.blau.android.util.OnPageSelectedListener;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.ExtendedViewPager;

/**
 * Dialog for final review of changes and adding comment and source tags before upload
 * 
 *
 */
public class ReviewAndUpload extends AbstractReviewDialog {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ReviewAndUpload.class.getSimpleName().length());
    private static final String DEBUG_TAG = ReviewAndUpload.class.getSimpleName().substring(0, TAG_LEN);

    public static final String TAG = "fragment_confirm_upload";

    private static final int NO_PAGE   = -1;
    public static final int  TAGS_PAGE = 1;

    private ExtendedViewPager pager = null;

    private AutoCompleteTextView comment;
    private AutoCompleteTextView source;

    /**
     * Instantiate and show the dialog
     * 
     * @param activity the calling FragmentActivity
     * @param elements an optional list of changed elements
     */
    public static void showDialog(@NonNull FragmentActivity activity, @Nullable List<OsmElement> elements) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        ReviewAndUpload confirmUploadDialogFragment = newInstance(elements);
        try {
            confirmUploadDialogFragment.show(fm, TAG);
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
     * @param elements an optional list of changed elements
     * @return a new ConfirmUpload instance
     */
    @NonNull
    private static ReviewAndUpload newInstance(@Nullable List<OsmElement> elements) {
        ReviewAndUpload f = new ReviewAndUpload();
        Bundle args = new Bundle();
        args.putString(TAG_KEY, TAG);
        if (elements != null) {
            de.blau.android.dialogs.Util.putElementsInBundle(elements, args);
        }
        f.setArguments(args);
        f.setShowsDialog(true);
        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreateDialog");
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            elements = de.blau.android.dialogs.Util.getElementsFromBundle(savedInstanceState);
        } else {
            elements = de.blau.android.dialogs.Util.getElementsFromBundle(getArguments());
        }

        FragmentActivity activity = getActivity();
        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.confirm_upload_title);

        final View layout = inflater.inflate(R.layout.upload_tabs, null);
        pager = (ExtendedViewPager) layout.findViewById(R.id.pager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) pager.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.colorAccent, R.color.dark_grey));

        pager.setAdapter(new ViewPagerAdapter(activity, layout, new int[] { R.id.review_page, R.id.tags_page },
                new int[] { R.string.confirm_upload_edits_page, R.string.menu_tags }));
        pager.addOnPageChangeListener((OnPageSelectedListener) position -> {
            AlertDialog dialog = ((AlertDialog) getDialog());
            if (dialog != null) {
                Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.clearFocus();
                }
            }
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
        });

        builder.setView(layout);

        // Review page
        TextView changesHeading = (TextView) layout.findViewById(R.id.review_heading);
        int changeCount = elements == null ? App.getDelegator().getApiElementCount() : elements.size();
        changesHeading.setText(getResources().getQuantityString(R.plurals.confirm_upload_text, changeCount, changeCount));

        // Comment and upload page
        Preferences prefs = App.getPreferences(activity);
        Server server = prefs.getServer();
        boolean openChangeset = server.hasOpenChangeset();
        TextView closeOpenChangesetLabel = (TextView) layout.findViewById(R.id.upload_close_open_changeset_label);
        closeOpenChangesetLabel.setVisibility(openChangeset ? View.VISIBLE : View.GONE);
        CheckBox closeOpenChangeset = (CheckBox) layout.findViewById(R.id.upload_close_open_changeset);
        closeOpenChangeset.setVisibility(openChangeset ? View.VISIBLE : View.GONE);

        CheckBox closeChangeset = (CheckBox) layout.findViewById(R.id.upload_close_changeset);
        closeChangeset.setChecked(prefs.closeChangesetOnSave());
        CheckBox requestReview = (CheckBox) layout.findViewById(R.id.upload_request_review);

        CheckBox emptyCommentWarning = (CheckBox) layout.findViewById(R.id.upload_empty_comment_warning);
        emptyCommentWarning.setChecked(prefs.emptyCommentWarning());
        emptyCommentWarning.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.setEmptyCommentWarning(isChecked);
            Tip.showDialog(getActivity(), R.string.tip_empty_comment_warning_key, R.string.tip_empty_comment_warning);
        });

        comment = (AutoCompleteTextView) layout.findViewById(R.id.upload_comment);
        List<String> comments = new ArrayList<>(App.getLogic().getLastComments());
        FilterlessArrayAdapter<String> commentAdapter = new FilterlessArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, comments);
        comment.setAdapter(commentAdapter);
        String lastComment = App.getLogic().getLastComment();
        comment.setText(lastComment == null ? "" : lastComment);
        OnClickListener autocompleteOnClick = v -> {
            if (v.hasFocus()) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        };
        comment.setOnClickListener(autocompleteOnClick);
        comment.setThreshold(1);
        comment.setOnKeyListener(new MyKeyListener());
        setAutoCaps(comment);
        ImageButton clearComment = (ImageButton) layout.findViewById(R.id.upload_comment_clear);
        clearComment.setOnClickListener(v -> {
            comment.setText("");
            comment.requestFocus();
        });

        source = (AutoCompleteTextView) layout.findViewById(R.id.upload_source);
        List<String> sources = new ArrayList<>(App.getLogic().getLastSources());
        FilterlessArrayAdapter<String> sourceAdapter = new FilterlessArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, sources);
        source.setAdapter(sourceAdapter);
        String lastSource = App.getLogic().getLastSource();
        source.setText(lastSource == null ? "" : lastSource);
        source.setOnClickListener(autocompleteOnClick);
        source.setThreshold(1);
        source.setOnKeyListener(new MyKeyListener());
        setAutoCaps(source);
        ImageButton clearSource = (ImageButton) layout.findViewById(R.id.upload_source_clear);
        clearSource.setOnClickListener(v -> {
            source.setText("");
            source.requestFocus();
        });

        builder.setPositiveButton(R.string.transfer_download_current_upload, null);

        builder.setNegativeButton(R.string.no, (dialog, which) -> saveCommentAndSource(comment, source));

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(
                new UploadListener(activity, comment, source, openChangeset ? closeOpenChangeset : null, closeChangeset, requestReview, elements));

        return dialog;
    }

    protected void createChangesView() {
        addChangesToView(getActivity(), (ListView) requireDialog().findViewById(R.id.upload_changes), elements, DEFAULT_COMPARATOR,
                getArguments().getString(TAG_KEY), R.layout.changes_list_item);
    }

    /**
     * Set sentence capitalization if we are using Latin script
     * 
     * @param textView the TextView to modify
     */
    private void setAutoCaps(@NonNull final TextView textView) {
        if (LocaleUtils.usesLatinScript(Util.getPrimaryLocale(getResources()))) {
            textView.setInputType(
                    (textView.getInputType() & ~TextRow.INPUTTYPE_CAPS_MASK) | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        saveCommentAndSource(comment, source);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        saveCommentAndSource(comment, source);
    }

    /**
     * Save the comment and source contents
     * 
     * @param comment the comment TextView
     * @param source the source TextView
     */
    private void saveCommentAndSource(@NonNull TextView comment, @NonNull AutoCompleteTextView source) {
        Logic logic = App.getLogic();
        logic.pushComment(comment.getText().toString().trim(), true);
        logic.pushSource(source.getText().toString().trim(), true);
    }

    /**
     * Show a specific page
     * 
     * @param activity the activity this fragment was created by
     * @param item index of page to show
     */
    public static void showPage(@NonNull FragmentActivity activity, int item) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment != null) {
            ((ReviewAndUpload) fragment).pager.setCurrentItem(item);
        }
    }

    /**
     * Return the current page we are on
     * 
     * @param activity the activity this fragment was created by
     * @return the current page index (or a value indicating that something went wrong)
     */
    public static int getPage(@NonNull FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment != null) {
            return ((ReviewAndUpload) fragment).pager.getCurrentItem();
        }
        return NO_PAGE;
    }

    /**
     * For whatever reason the softkeyboard doesn't work as expected with AutoCompleteTextViews This listener simply
     * moves focus to the next view below on enter being pressed or dismisses the keyboard
     */
    private class MyKeyListener implements OnKeyListener {
        @Override
        public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
            Button button = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
            if (button != null) {
                button.setFocusableInTouchMode(false);
            }
            if ((keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) && view instanceof EditText
                    && keyCode == KeyEvent.KEYCODE_ENTER) {
                View nextView = view.focusSearch(View.FOCUS_DOWN);
                if (nextView != null && nextView.isFocusable()) {
                    nextView.requestFocus();
                    return true;
                } else {
                    if (view instanceof AutoCompleteTextView) {
                        ((AutoCompleteTextView) view).dismissDropDown();
                        if (button != null) {
                            button.setFocusableInTouchMode(true);
                            button.requestFocus();
                        }
                    }
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
            return false;
        }
    }
}
