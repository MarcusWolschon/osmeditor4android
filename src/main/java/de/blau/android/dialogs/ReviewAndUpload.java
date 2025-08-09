package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;
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

    private static final String REQUEST_REVIEW_KEY       = "requestReview";
    private static final String CLOSE_CHANGESET_KEY      = "closeChangeset";
    private static final String CLOSE_OPEN_CHANGESET_KEY = "closeOpenChangeset";
    private static final String SOURCE_KEY               = "source";
    private static final String COMMENT_KEY              = "comment";
    private static final String CUSTOM_TAGS_KEY          = "customTags";
    private static final String TEMP_TAGS_KEY            = "tempTags";
    private static final String SAVEDCUSTOMCHANGESETTAGS = "savedcustomchangesettags.dat";
    private static final String TEMPCUSTOMCHANGESETTAGS  = "tempcustomchangesettags.dat";

    private static final int NO_PAGE   = -1;
    public static final int  TAGS_PAGE = 1;

    private static SavingHelper<HashMap<String, String>> customTagSaver = new SavingHelper<>();

    private ExtendedViewPager pager = null;

    private AutoCompleteTextView commentView;
    private AutoCompleteTextView sourceView;
    private LinearLayout         persistentCustomTagLayout;
    private LinearLayout         transientCustomTagLayout;
    private CheckBox             closeOpenChangesetCheck;
    private CheckBox             closeChangesetCheck;
    private CheckBox             requestReviewCheck;

    private LayoutInflater inflater;
    private int            maxStringLength;

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
     * Dismiss the dialog, cleaning up transient tags
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
        FragmentActivity activity = getActivity();
        Preferences prefs = App.getPreferences(activity);

        boolean requestReview = false;
        boolean closeOpenChangeset = false;
        boolean closeChangeset = prefs.closeChangesetOnSave();

        final Logic logic = App.getLogic();
        String lastComment = logic.getLastComment();
        lastComment = lastComment == null ? "" : lastComment;
        String lastSource = logic.getLastSource();
        lastSource = lastSource == null ? "" : lastSource;

        Map<String, String> persistentCustomTags = customTagSaver.load(getContext(), SAVEDCUSTOMCHANGESETTAGS, false);
        Map<String, String> transientCustomTags = customTagSaver.load(getContext(), TEMPCUSTOMCHANGESETTAGS, false);

        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            de.blau.android.dialogs.Util.getElements(getContext(), savedInstanceState);
            Serializable temp = Util.getSerializeable(savedInstanceState, CUSTOM_TAGS_KEY, Serializable.class);
            if (temp instanceof Map) { // not null
                persistentCustomTags = (Map<String, String>) temp;
            }
            temp = Util.getSerializeable(savedInstanceState, TEMP_TAGS_KEY, Serializable.class);
            if (temp instanceof Map) { // not null
                transientCustomTags = (Map<String, String>) temp;
            }
            lastComment = savedInstanceState.getString(COMMENT_KEY, lastComment);
            lastSource = savedInstanceState.getString(SOURCE_KEY, lastSource);
            closeOpenChangeset = savedInstanceState.getBoolean(CLOSE_OPEN_CHANGESET_KEY, closeOpenChangeset);
            closeChangeset = savedInstanceState.getBoolean(CLOSE_CHANGESET_KEY, closeChangeset);
            requestReview = savedInstanceState.getBoolean(REQUEST_REVIEW_KEY, requestReview);
        } else {
            elements = de.blau.android.dialogs.Util.getElements(getContext(), getArguments());
        }

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        inflater = ThemeUtils.getLayoutInflater(activity);

        Builder builder = ThemeUtils.getAlertDialogBuilder(activity);
        builder.setTitle(R.string.confirm_upload_title);

        View layout = setupPager(activity);
        builder.setView(layout);

        // Review page
        TextView changesHeading = (TextView) layout.findViewById(R.id.review_heading);
        int changeCount = elements == null ? App.getDelegator().getApiElementCount() : elements.size();
        changesHeading.setText(getResources().getQuantityString(R.plurals.confirm_upload_text, changeCount, changeCount));

        // Comment and upload page

        Server server = prefs.getServer();
        boolean openChangeset = server.hasOpenChangeset();
        maxStringLength = server.getCachedCapabilities().getMaxStringLength();

        TextView closeOpenChangesetLabel = (TextView) layout.findViewById(R.id.upload_close_open_changeset_label);
        closeOpenChangesetLabel.setVisibility(openChangeset ? View.VISIBLE : View.GONE);
        closeOpenChangesetCheck = (CheckBox) layout.findViewById(R.id.upload_close_open_changeset);
        closeOpenChangesetCheck.setVisibility(openChangeset ? View.VISIBLE : View.GONE);
        closeOpenChangesetCheck.setChecked(closeOpenChangeset);

        closeChangesetCheck = (CheckBox) layout.findViewById(R.id.upload_close_changeset);
        closeChangesetCheck.setChecked(closeChangeset);

        requestReviewCheck = (CheckBox) layout.findViewById(R.id.upload_request_review);
        requestReviewCheck.setChecked(requestReview);

        CheckBox emptyCommentWarning = (CheckBox) layout.findViewById(R.id.upload_empty_comment_warning);
        emptyCommentWarning.setChecked(prefs.emptyCommentWarning());
        emptyCommentWarning.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            prefs.setEmptyCommentWarning(isChecked);
            Tip.showDialog(getActivity(), R.string.tip_empty_comment_warning_key, R.string.tip_empty_comment_warning);
        });

        commentView = (AutoCompleteTextView) layout.findViewById(R.id.upload_comment);

        List<String> comments = new ArrayList<>(logic.getLastComments());
        FilterlessArrayAdapter<String> commentAdapter = new FilterlessArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, comments);
        commentView.setAdapter(commentAdapter);
        commentView.setText(lastComment);
        OnClickListener autocompleteOnClick = v -> {
            if (v.hasFocus()) {
                ((AutoCompleteTextView) v).showDropDown();
            }
        };
        commentView.setOnClickListener(autocompleteOnClick);
        commentView.setThreshold(1);
        commentView.setOnKeyListener(new MyKeyListener());
        setAutoCaps(commentView);
        ImageButton clearComment = (ImageButton) layout.findViewById(R.id.upload_comment_clear);
        clearComment.setOnClickListener(v -> {
            commentView.setText("");
            commentView.requestFocus();
        });

        sourceView = (AutoCompleteTextView) layout.findViewById(R.id.upload_source);
        List<String> sources = new ArrayList<>(logic.getLastSources());
        FilterlessArrayAdapter<String> sourceAdapter = new FilterlessArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, sources);
        sourceView.setAdapter(sourceAdapter);
        sourceView.setText(lastSource);
        sourceView.setOnClickListener(autocompleteOnClick);
        sourceView.setThreshold(1);
        sourceView.setOnKeyListener(new MyKeyListener());
        setAutoCaps(sourceView);
        ImageButton clearSource = (ImageButton) layout.findViewById(R.id.upload_source_clear);
        clearSource.setOnClickListener(v -> {
            sourceView.setText("");
            sourceView.requestFocus();
        });

        // custom tag tab
        persistentCustomTagLayout = (LinearLayout) layout.findViewById(R.id.persistent_custom_tag_row_layout);
        if (persistentCustomTags != null) {
            for (Entry<String, String> customTag : persistentCustomTags.entrySet()) {
                addNewCustomTagRow(persistentCustomTagLayout, customTag.getKey(), customTag.getValue());
            }
        }
        ensureEmptyRow(persistentCustomTagLayout);
        transientCustomTagLayout = (LinearLayout) layout.findViewById(R.id.transient_custom_tag_row_layout);
        if (transientCustomTags != null) {
            for (Entry<String, String> tempTag : transientCustomTags.entrySet()) {
                addNewCustomTagRow(transientCustomTagLayout, tempTag.getKey(), tempTag.getValue());
            }
        }
        ensureEmptyRow(transientCustomTagLayout);

        builder.setPositiveButton(R.string.transfer_download_current_upload, null);

        builder.setNegativeButton(R.string.no, (dialog, which) -> saveTags(commentView, sourceView, persistentCustomTagLayout, transientCustomTagLayout));

        AlertDialog dialog = builder.create();
        final UploadListener listener = new UploadListener(activity, commentView, sourceView, openChangeset ? closeOpenChangesetCheck : null,
                closeChangesetCheck, requestReviewCheck, elements);
        listener.setPersistentTagContainer(persistentCustomTagLayout);
        listener.setTransientTagContainer(transientCustomTagLayout);
        dialog.setOnShowListener(listener);

        return dialog;
    }

    /**
     * Setup and return the ViewPager
     * 
     * @param activity the current FragmentActivity
     * @return the View containing the pager
     */
    @NonNull
    private View setupPager(@NonNull FragmentActivity activity) {
        final View layout = inflater.inflate(R.layout.upload_tabs, null);
        pager = (ExtendedViewPager) layout.findViewById(R.id.pager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) pager.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.colorAccent, R.color.dark_grey));

        pager.setAdapter(new ViewPagerAdapter(activity, layout, new int[] { R.id.review_page, R.id.tags_page, R.id.custom_tags_page },
                new int[] { R.string.confirm_upload_edits_page, R.string.menu_tags, R.string.confirm_upload_custom_tags_page }));
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
        pager.setOffscreenPageLimit(2); //
        return layout;
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
        saveTags(commentView, sourceView, persistentCustomTagLayout, transientCustomTagLayout);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        saveTags(commentView, sourceView, persistentCustomTagLayout, transientCustomTagLayout);
    }

    /**
     * This needs to be called before the Dialog is dismissed
     */
    public void removeTransientCustomTags() {
        // cleanup transient tags
        transientCustomTagLayout.removeAllViews();
        getContext().deleteFile(ReviewAndUpload.TEMPCUSTOMCHANGESETTAGS);

    }

    /**
     * Save the tags (aka nearly everything that is persistent in this modal)
     * 
     * @param comment the comment TextView
     * @param source the source TextView
     * @param persitentTagLayoutt container holding persistent custom tags
     */
    private void saveTags(@NonNull TextView comment, @NonNull AutoCompleteTextView source, @Nullable LinearLayout persitentTagLayout,
            @NonNull LinearLayout transientTagLayout) {
        Logic logic = App.getLogic();
        logic.pushComment(comment.getText().toString().trim(), true);
        logic.pushSource(source.getText().toString().trim(), true);
        HashMap<String, String> tags = new HashMap<>();
        UploadListener.addCustomTags(persitentTagLayout, tags);
        customTagSaver.save(getContext(), SAVEDCUSTOMCHANGESETTAGS, tags, false);
        tags.clear();
        UploadListener.addCustomTags(transientTagLayout, tags);
        customTagSaver.save(getContext(), TEMPCUSTOMCHANGESETTAGS, tags, false);
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
    private final class MyKeyListener implements OnKeyListener {
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
     * Custom view for holding a custom changeset tag
     */
    public static class CustomTagRow extends LinearLayout {

        private AutoCompleteTextView       keyEdit;
        private CustomAutoCompleteTextView valueEdit;
        private ImageButton                delete;

        /**
         * Construct a View holding the key and value for a tag
         * 
         * @param context an Android Context
         */
        public CustomTagRow(@NonNull Context context) {
            super(context);
        }

        /**
         * Construct a View holding the key and value for a tag
         * 
         * @param context an Android Context
         * @param attrs am AttributeSet
         */
        public CustomTagRow(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            keyEdit = (AutoCompleteTextView) findViewById(R.id.editKey);
            valueEdit = (CustomAutoCompleteTextView) findViewById(R.id.editValue);

            delete = (ImageButton) findViewById(R.id.delete);
            delete.setOnClickListener((View v) -> deleteRow(CustomTagRow.this));

            OnClickListener autocompleteOnClick = v -> {
                if (v.hasFocus()) {
                    ((AutoCompleteTextView) v).showDropDown();
                }
            };
            // set an empty adapter on both views to be on the safe side
            ArrayAdapter<String> empty = new ArrayAdapter<>(getContext(), R.layout.autocomplete_row, new String[0]);
            keyEdit.setAdapter(empty);
            valueEdit.setAdapter(empty);
            keyEdit.setOnClickListener(autocompleteOnClick);
            valueEdit.setOnClickListener(autocompleteOnClick);
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            if (w == 0 && h == 0) {
                return;
            }

            // this is not really satisfactory
            keyEdit.setDropDownAnchor(valueEdit.getId());
            // note wrap_content does not actually wrap the contents of the drop
            // down, instead in makes it the same width as the AutoCompleteTextView
            valueEdit.setDropDownWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
            valueEdit.setParentWidth(w);
            //
        }

        /**
         * Sets key and value values
         * 
         * @param tagKey the key value to set
         * @param tagValues List of values to set
         * @return the CustomTagRow object for convenience
         */
        @NonNull
        public CustomTagRow setKeyAndValue(@NonNull String tagKey, @Nullable String tagValue) {
            keyEdit.setText(tagKey);
            if (tagValue != null) {
                valueEdit.setText(tagValue);
            } else {
                valueEdit.setText("");
            }
            return this;
        }

        /**
         * Get the current contents of the EditText for the key
         * 
         * @return the tag key as a String
         */
        @NonNull
        public String getKey() {
            return keyEdit.getText().toString();
        }

        /**
         * Get the current contents of the EditText for the tag value
         * 
         * @return the key as a String
         */
        @NonNull
        public String getValue() {
            return valueEdit.getText().toString();
        }

        /**
         * Checks if the fields in this row are empty
         * 
         * @return true if both fields are empty, false if at least one is filled
         */
        public boolean isEmpty() {
            return "".equals(keyEdit.getText().toString().trim()) && "".equals(valueEdit.getText().toString().trim());
        }

        /**
         * Hide the delete button
         */
        public void hideDelete() {
            delete.setVisibility(View.INVISIBLE);
        }

        /**
         * Show the delete button
         */
        void enableDelete() {
            delete.setVisibility(View.VISIBLE);
        }

        public void deleteRow(CustomTagRow customTagRow) {
            ViewParent parent = customTagRow.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(customTagRow);
            }
        }

    }

    /**
     * Ensures that at least one empty custom tag row exists (creating one if needed)
     * 
     * @param rowLayout layout holding the rows
     * @return the first empty row found (or the one created), or null if loading was not finished (loaded == false),
     *         null if rowLayout is null
     */
    @Nullable
    private CustomTagRow ensureEmptyRow(@Nullable LinearLayout rowLayout) {
        if (rowLayout == null) {
            return null;
        }
        CustomTagRow ret = null;
        int i = rowLayout.getChildCount();
        while (--i >= 0) {
            CustomTagRow row = (CustomTagRow) rowLayout.getChildAt(i);
            if (row != null) {
                boolean isEmpty = row.isEmpty();
                if (ret == null) {
                    ret = isEmpty ? row : addNewCustomTagRow(rowLayout, "", "");
                } else if (isEmpty) {
                    rowLayout.removeViewAt(i);
                }
            } else {
                Log.e(DEBUG_TAG, "ensureEmptyRow no row at position " + i);
            }
        }
        if (ret == null) {
            return addNewCustomTagRow(rowLayout, "", "");
        }
        return ret;
    }

    /**
     * Add a new custom tag row to a container view
     * 
     * @param container the target view
     * @param tagKey the tag key
     * @param tagValue the tag value
     * @return a CustomTagRow instance
     */
    @NonNull
    private CustomTagRow addNewCustomTagRow(@NonNull final LinearLayout container, final String tagKey, final String tagValue) {
        final CustomTagRow row = (CustomTagRow) inflater.inflate(R.layout.custom_tag_row, container, false);
        /**
         * This TextWatcher reacts to previously empty cells being filled to add additional rows where needed add
         * removes any formatting and truncates to maximum supported API string length
         */
        TextWatcher textWatcher = new TextWatcher() {
            private boolean wasEmpty;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // nop
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                wasEmpty = row.isEmpty();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (wasEmpty == (s.length() > 0)) {
                    // changed from empty to not-empty or vice versa
                    row.enableDelete();
                    ensureEmptyRow(container);
                }

                Util.sanitizeString(getActivity(), s, maxStringLength);
            }
        };
        row.keyEdit.addTextChangedListener(textWatcher);

        final TextWatcher valueTextWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                textWatcher.beforeTextChanged(s, start, count, after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textWatcher.onTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                textWatcher.afterTextChanged(s);
            }
        };
        row.valueEdit.addTextChangedListener(valueTextWatcher);

        row.setKeyAndValue(tagKey, tagValue);

        container.addView(row);
        return row;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        HashMap<String, String> tags = new HashMap<>();
        UploadListener.addCustomTags(persistentCustomTagLayout, tags);
        outState.putSerializable(CUSTOM_TAGS_KEY, tags);
        tags.clear();
        UploadListener.addCustomTags(transientCustomTagLayout, tags);
        outState.putSerializable(TEMP_TAGS_KEY, tags);
        outState.putString(COMMENT_KEY, commentView.getText().toString());
        outState.putString(SOURCE_KEY, sourceView.getText().toString());
        outState.putBoolean(CLOSE_OPEN_CHANGESET_KEY, closeOpenChangesetCheck.isChecked());
        outState.putBoolean(CLOSE_CHANGESET_KEY, closeChangesetCheck.isChecked());
        outState.putBoolean(REQUEST_REVIEW_KEY, requestReviewCheck.isChecked());
    }
}
