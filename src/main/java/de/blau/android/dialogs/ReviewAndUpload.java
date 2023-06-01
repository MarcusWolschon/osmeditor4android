package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerTabStrip;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.UploadListener;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Server;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.propertyeditor.tagform.TextRow;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.FilterlessArrayAdapter;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.LocaleUtils;
import de.blau.android.util.OnPageSelectedListener;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.validation.ExtendedValidator;
import de.blau.android.validation.FormValidation;
import de.blau.android.validation.NotEmptyValidator;
import de.blau.android.validation.Validator;
import de.blau.android.views.ExtendedViewPager;

/**
 * Dialog for final review of changes and adding comment and source tags before upload
 * 
 *
 */
public class ReviewAndUpload extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = ReviewAndUpload.class.getSimpleName();

    public static final String  TAG          = "fragment_confirm_upload";
    private static final String ELEMENTS_KEY = "elements";

    private static final int NO_PAGE   = -1;
    public static final int  TAGS_PAGE = 1;

    private ExtendedViewPager pager = null;

    private AutoCompleteTextView comment;
    private AutoCompleteTextView source;

    private List<OsmElement> elements = null;

    static final Comparator<ChangedElement> DEFAULT_COMPARATOR = (ce0, ce1) -> {
        OsmElement element0 = ce0.element;
        OsmElement element1 = ce1.element;
        int problems0 = element0.getCachedProblems();
        int problems1 = element1.getCachedProblems();
        if (problems0 > Validator.OK && problems1 <= Validator.OK) {
            return -1;
        }
        if (problems0 <= Validator.OK && problems1 > Validator.OK) {
            return 1;
        }
        if (element0.isTagged() && !element1.isTagged()) {
            return -1;
        }
        if (!element0.isTagged() && element1.isTagged()) {
            return 1;
        }
        byte ce0State = element0.getState();
        byte ce1State = element1.getState();
        if (ce0State == OsmElement.STATE_CREATED && ce1State != OsmElement.STATE_CREATED) {
            return -1;
        }
        if (ce0State != OsmElement.STATE_CREATED && ce1State == OsmElement.STATE_CREATED) {
            return 1;
        }
        if (ce0State == OsmElement.STATE_MODIFIED && ce1State == OsmElement.STATE_DELETED) {
            return -1;
        }
        if (ce0State == OsmElement.STATE_DELETED && ce1State == OsmElement.STATE_MODIFIED) {
            return 1;
        }
        String ce0Type = element0.getName();
        String ce1Type = element1.getName();
        if (Node.NAME.equals(ce0Type) && !Node.NAME.equals(ce1Type)) {
            return -1;
        }
        if (Way.NAME.equals(ce0Type) && Relation.NAME.equals(ce1Type)) {
            return -1;
        }
        if (Way.NAME.equals(ce0Type) && Node.NAME.equals(ce1Type)) {
            return 1;
        }
        if (Relation.NAME.equals(ce0Type) && !Relation.NAME.equals(ce1Type)) {
            return 1;
        }
        return 0;
    };

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
        if (elements != null) {
            args.putSerializable(ELEMENTS_KEY, new ArrayList<>(elements));
        }
        f.setArguments(args);
        f.setShowsDialog(true);
        return f;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            elements = (List<OsmElement>) savedInstanceState.getSerializable(ELEMENTS_KEY);
        } else {
            elements = (List<OsmElement>) getArguments().getSerializable(ELEMENTS_KEY);
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

        addChangesToView(activity, (ListView) layout.findViewById(R.id.upload_changes), elements, DEFAULT_COMPARATOR, ReviewAndUpload.TAG);

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

        FormValidation commentValidator = new NotEmptyValidator(comment, getString(R.string.upload_validation_error_empty_comment));
        FormValidation sourceValidator = new NotEmptyValidator(source, getString(R.string.upload_validation_error_empty_source));
        List<FormValidation> validators = Arrays.asList(commentValidator, sourceValidator);

        builder.setPositiveButton(R.string.transfer_download_current_upload, null);

        builder.setNegativeButton(R.string.no, (dialog, which) -> saveCommentAndSource(comment, source));

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(
                new UploadListener(activity, comment, source, openChangeset ? closeOpenChangeset : null, closeChangeset, requestReview, validators, elements));

        return dialog;
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

    /**
     * Add changes to a ListView
     * 
     * @param activity the current Activity
     * @param changesView the ListView used to hold the changes views
     * @param elements a List of OsmElement or null
     * @param comparator a Comparator for sorting the changes
     * @param parentTag tag of parent dialog
     */
    static void addChangesToView(@NonNull final FragmentActivity activity, @NonNull final ListView changesView, @Nullable List<OsmElement> elements,
            @NonNull Comparator<ChangedElement> comparator, @Nullable String parentTag) {
        ExtendedValidator validator = new ExtendedValidator(activity, App.getDefaultValidator(activity));
        final ChangedElement[] changes = getPendingChanges(activity.getResources(), elements == null ? App.getLogic().getPendingChangedElements() : elements);
        revalidate(activity, validator, changes);
        Arrays.sort(changes, comparator);

        changesView.setAdapter(new ValidatorArrayAdapter(activity, R.layout.changes_list_item, changes, validator));
        changesView.setOnItemClickListener((parent, view, position, id) -> {
            ChangedElement clicked = changes[position];
            OsmElement element = clicked.element;
            byte elemenState = element.getState();
            boolean deleted = elemenState == OsmElement.STATE_DELETED;
            if (elemenState == OsmElement.STATE_MODIFIED || deleted) {
                ElementInfo.showDialog(activity, 0, element, !deleted, parentTag);
            } else {
                ElementInfo.showDialog(activity, element, !deleted, parentTag);
            }
        });
    }

    /**
     * Rerun validation on the changes
     * 
     * @param context an Android Context
     * @param validator the Validator to use
     * @param changes the list of changes
     */
    private static void revalidate(@NonNull Context context, @NonNull Validator validator, @NonNull final ChangedElement[] changes) {
        for (ChangedElement ce : changes) {
            OsmElement element = ce.element;
            element.resetHasProblem();
            element.hasProblem(context, validator);
        }
        if (context instanceof Main) {
            ((Main) context).invalidateMap();
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

    static class ChangedElement {
        final OsmElement element;
        final String     description;

        /**
         * Construct a new instance
         * 
         * @param resources the current Resources
         * @param element the OsmElement to wrap
         */
        ChangedElement(@NonNull Resources resources, @NonNull OsmElement element) {
            this.element = element;
            description = element.getStateDescription(resources);
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Get the pending changes
     * 
     * This will sort the result in a reasonable way: tagged elements first then untagged, newly created before modified
     * and then deleted, then the convention node, way and relation ordering.
     * 
     * @param resources the current Resources
     * @param changedElements the (unsorted) list of changed elements
     * @return a List of all pending pending elements to upload
     */
    @NonNull
    private static ChangedElement[] getPendingChanges(@NonNull Resources resources, @NonNull List<OsmElement> changedElements) {
        List<ChangedElement> result = new ArrayList<>();
        for (OsmElement e : changedElements) {
            result.add(new ChangedElement(resources, e));
        }
        return result.toArray(new ChangedElement[result.size()]);
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

    /**
     * Highlight elements for upload that have a potential issue
     * 
     * @author Simon Poole
     *
     */
    private static class ValidatorArrayAdapter extends ArrayAdapter<ChangedElement> {
        final ChangedElement[] elements;
        final Validator        validator;
        final ColorStateList   colorStateList;

        /**
         * Construct a new instance
         * 
         * @param context Android Context
         * @param resource the resource id of the per item layout
         * @param elements the array holding the elements
         * @param validator the Validator to use
         */
        public ValidatorArrayAdapter(@NonNull Context context, int resource, @NonNull ChangedElement[] elements, @NonNull Validator validator) {
            super(context, resource, elements);
            this.elements = elements;
            this.validator = validator;
            colorStateList = ColorStateList.valueOf(ThemeUtils.getStyleAttribColorValue(context, R.attr.snack_error, R.color.material_red));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            View v = super.getView(position, convertView, container);
            TextView textView = (TextView) v.findViewById(R.id.text1);
            if (textView != null) {
                OsmElement element = elements[position].element;
                if (OsmElement.STATE_DELETED != element.getState() && element.hasProblem(null, validator) != Validator.OK) {
                    setTintList(textView, colorStateList);
                } else {
                    setTintList(textView, null);
                }
            } else {
                Log.e("ValidatorAdapterView", "position " + position + " view is null");
            }
            return v;
        }

        /**
         * Backwards compatible way of setting the a tint list on a TextView
         * 
         * @param textView the TextView
         * @param stateList the ColorStateList
         */
        void setTintList(@NonNull TextView textView, @Nullable ColorStateList stateList) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                textView.setCompoundDrawableTintList(stateList);
            } else {
                for (Drawable d : textView.getCompoundDrawables()) {
                    if (d != null) {
                        DrawableCompat.setTintList(d, stateList);
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (elements != null) {
            outState.putSerializable(ELEMENTS_KEY, new ArrayList<>(elements));
        }
    }
}
