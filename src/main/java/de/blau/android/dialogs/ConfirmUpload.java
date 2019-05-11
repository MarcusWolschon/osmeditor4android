package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.UploadListener;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.ACRAHelper;
import de.blau.android.util.FilterlessArrayAdapter;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.validation.FormValidation;
import de.blau.android.validation.NotEmptyValidator;
import de.blau.android.validation.Validator;
import de.blau.android.views.ExtendedViewPager;

/**
 * Dialog for final review of changes and adding comment and source tags before upload
 * 
 *
 */
public class ConfirmUpload extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = ConfirmUpload.class.getSimpleName();

    public static final String TAG = "fragment_confirm_upload";

    public static final int NO_PAGE    = -1;
    public static final int EDITS_PAGE = 0;
    public static final int TAGS_PAGE  = 1;

    private View              layout = null;
    private ExtendedViewPager pager  = null;

    private Resources resources;

    /**
     * Instantiate and show the dialog
     * 
     * @param activity the calling FragmentActivity
     */
    public static void showDialog(FragmentActivity activity) {
        dismissDialog(activity);

        FragmentManager fm = activity.getSupportFragmentManager();
        ConfirmUpload confirmUploadDialogFragment = newInstance();
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
    public static void dismissDialog(FragmentActivity activity) {
        Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of this Fragment
     * 
     * @return a new ConfirmUpload instance
     */
    private static ConfirmUpload newInstance() {
        ConfirmUpload f = new ConfirmUpload();
        f.setShowsDialog(true);
        return f;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(DEBUG_TAG, "onAttach");
        if (!(context instanceof Main)) {
            throw new ClassCastException(context.toString() + " can only be called from Main");
        }
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        resources = getResources();
        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.confirm_upload_title);

        layout = inflater.inflate(R.layout.upload_tabs, null);
        pager = (ExtendedViewPager) layout.findViewById(R.id.pager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) pager.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(getContext(), R.attr.colorAccent, R.color.dark_grey));

        pager.setAdapter(new ViewPagerAdapter());

        builder.setView(layout);

        // Review page
        TextView changesHeading = (TextView) layout.findViewById(R.id.review_heading);
        int changeCount = App.getDelegator().getApiElementCount();
        changesHeading.setText(getResources().getQuantityString(R.plurals.confirm_upload_text, changeCount, changeCount));
        ListView changesView = (ListView) layout.findViewById(R.id.upload_changes);
        final ChangedElement[] changes = getPendingChanges();
        changesView.setAdapter(new ValidatorArrayAdapter(activity, R.layout.changes_list_item, changes, App.getDefaultValidator(getContext())));
        changesView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChangedElement clicked = changes[position];
                OsmElement element = clicked.element;
                byte elemenState = element.getState();
                boolean deleted = elemenState == OsmElement.STATE_DELETED;
                if (elemenState == OsmElement.STATE_MODIFIED || deleted) {
                    ElementInfo.showDialog(getActivity(), App.getDelegator().getUndo().getOriginal(element), element, !deleted);
                } else {
                    ElementInfo.showDialog(getActivity(), element, !deleted);
                }
            }
        });

        // Comment and upload page
        CheckBox closeChangeset = (CheckBox) layout.findViewById(R.id.upload_close_changeset);
        closeChangeset.setChecked(new Preferences(activity).closeChangesetOnSave());
        CheckBox requestReview = (CheckBox) layout.findViewById(R.id.upload_request_review);

        final AutoCompleteTextView comment = (AutoCompleteTextView) layout.findViewById(R.id.upload_comment);
        List<String> comments = new ArrayList<String>(App.getLogic().getLastComments());
        FilterlessArrayAdapter<String> commentAdapter = new FilterlessArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, comments);
        comment.setAdapter(commentAdapter);
        String lastComment = App.getLogic().getLastComment();
        comment.setText(lastComment == null ? "" : lastComment);
        OnClickListener autocompleteOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.hasFocus()) {
                    ((AutoCompleteTextView) v).showDropDown();
                }
            }
        };
        comment.setOnClickListener(autocompleteOnClick);
        comment.setThreshold(1);
        comment.setOnKeyListener(new MyKeyListener());
        ImageButton clearComment = (ImageButton) layout.findViewById(R.id.upload_comment_clear);
        clearComment.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                comment.setText("");
            }
        });

        final AutoCompleteTextView source = (AutoCompleteTextView) layout.findViewById(R.id.upload_source);
        List<String> sources = new ArrayList<String>(App.getLogic().getLastSources());
        FilterlessArrayAdapter<String> sourceAdapter = new FilterlessArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, sources);
        source.setAdapter(sourceAdapter);
        String lastSource = App.getLogic().getLastSource();
        source.setText(lastSource == null ? "" : lastSource);
        source.setOnClickListener(autocompleteOnClick);
        source.setThreshold(1);
        source.setOnKeyListener(new MyKeyListener());
        ImageButton clearSource = (ImageButton) layout.findViewById(R.id.upload_source_clear);
        clearSource.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                source.setText("");
            }
        });

        FormValidation commentValidator = new NotEmptyValidator(comment, getString(R.string.upload_validation_error_empty_comment));
        FormValidation sourceValidator = new NotEmptyValidator(source, getString(R.string.upload_validation_error_empty_source));
        List<FormValidation> validators = Arrays.asList(commentValidator, sourceValidator);

        builder.setPositiveButton(R.string.transfer_download_current_upload, null);

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveCommentAndSource(comment, source);
            }
        });
        builder.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                saveCommentAndSource(comment, source);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new UploadListener((Main) activity, comment, source, closeChangeset, requestReview, validators));
        return dialog;
    }

    /**
     * Save the comment and source contents
     * 
     * @param comment the comment TextView
     * @param source the source TextView
     */
    private void saveCommentAndSource(@NonNull TextView comment, @NonNull AutoCompleteTextView source) {
        Logic logic = App.getLogic();
        logic.pushComment(comment.getText().toString(), true);
        logic.pushSource(source.getText().toString(), true);
    }

    /**
     * Show a specific page
     * 
     * @param activity the activity this fragment was created by
     * @param item index of page to show
     */
    public static void showPage(@NonNull AppCompatActivity activity, int item) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment != null) {
            ((ConfirmUpload) fragment).pager.setCurrentItem(item);
        }
    }

    /**
     * Return the current page we are on
     * 
     * @param activity the activity this fragment was created by
     * @return the current page index (or a value indicating that something went wrong)
     */
    public static int getPage(@NonNull AppCompatActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(TAG);
        if (fragment != null) {
            return ((ConfirmUpload) fragment).pager.getCurrentItem();
        }
        return NO_PAGE;
    }

    private class ChangedElement {
        final OsmElement element;

        /**
         * Construct a new instance
         * 
         * @param element the OsmElement to wrap
         */
        ChangedElement(@NonNull OsmElement element) {
            this.element = element;
        }

        @Override
        public String toString() {
            return element.getStateDescription(resources);
        }
    }

    /**
     * Get the pending changes
     * 
     * This will sort the result in a reasonable way: tagged elements first then untagged, newly created before modified
     * and then deleted, then the convention node, way and relation ordering.
     * 
     * @return a List of all pending pending elements to upload
     */
    private ChangedElement[] getPendingChanges() {

        List<OsmElement> changedElements = App.getLogic().getPendingChangedElements();
        List<ChangedElement> result = new ArrayList<>();
        for (OsmElement e : changedElements) {
            result.add(new ChangedElement(e));
        }
        Collections.sort(result, new Comparator<ChangedElement>() {
            @Override
            public int compare(ChangedElement ce0, ChangedElement ce1) {

                if (ce0.element.isTagged() && !ce1.element.isTagged()) {
                    return -1;
                }
                if (!ce0.element.isTagged() && ce1.element.isTagged()) {
                    return 1;
                }
                byte ce0State = ce0.element.getState();
                byte ce1State = ce1.element.getState();
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
                String ce0Type = ce0.element.getName();
                String ce1Type = ce1.element.getName();
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
            }
        });
        return result.toArray(new ChangedElement[result.size()]);
    }

    /**
     * For whatever reason the softkeyboard doesn't work as expected with AutoCompleteTextViews This listener simply
     * moves focus to the next view below on enter being pressed or dismisses the keyboard
     */
    private class MyKeyListener implements OnKeyListener {
        @Override
        public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
                if (view instanceof EditText) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        View nextView = view.focusSearch(View.FOCUS_DOWN);
                        if (nextView != null && nextView.isFocusable()) {
                            nextView.requestFocus();
                            return true;
                        } else {
                            if (view instanceof AutoCompleteTextView) {
                                ((AutoCompleteTextView) view).dismissDropDown();
                            }
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                }
            }
            return false;
        }
    }

    private class ViewPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {

            int resId = 0;
            switch (position) {
            case 0:
                resId = R.id.review_page;
                break;
            case 1:
                resId = R.id.tags_page;
                break;
            }
            return layout.findViewById(resId);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            switch (position) {
            case EDITS_PAGE:
                return getString(R.string.confirm_upload_edits_page);
            case TAGS_PAGE:
                return getString(R.string.menu_tags);
            }
            return "";
        }
    }

    /**
     * Highlight elements for upload that have a potential issue
     * 
     * @author Simon Poole
     *
     */
    private class ValidatorArrayAdapter extends ArrayAdapter<ChangedElement> {
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
            colorStateList = ColorStateList.valueOf(ThemeUtils.getStyleAttribColorValue(context, R.color.material_red, 0xFFFF0000));
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
}
