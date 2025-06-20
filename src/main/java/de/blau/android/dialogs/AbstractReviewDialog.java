package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.UndoStorage;
import de.blau.android.osm.Way;
import de.blau.android.util.MaxHeightDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.validation.ExtendedValidator;
import de.blau.android.validation.Validator;

/**
 * Common methods for review of changes
 */
public abstract class AbstractReviewDialog extends MaxHeightDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, AbstractReviewDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = AbstractReviewDialog.class.getSimpleName().substring(0, TAG_LEN);

    protected static final String TAG_KEY = "tag";

    protected List<OsmElement> elements = null;

    protected static final Comparator<ChangedElement> DEFAULT_COMPARATOR = (ce0, ce1) -> {
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

    @Override
    public void onStart() {
        super.onStart();
        Log.d(DEBUG_TAG, "onStart");
        createChangesView();
    }

    protected abstract void createChangesView();

    /**
     * Add changes to a ListView
     * 
     * @param activity the current Activity
     * @param changesView the ListView used to hold the changes views
     * @param elements a List of OsmElement or null
     * @param comparator a Comparator for sorting the changes
     * @param parentTag tag of parent dialog
     * @param itemResource the layout resource for the item
     */
    protected void addChangesToView(@NonNull final FragmentActivity activity, @NonNull final ListView changesView, @Nullable List<OsmElement> elements,
            @NonNull Comparator<ChangedElement> comparator, @Nullable String parentTag, int itemResource) {
        ExtendedValidator validator = new ExtendedValidator(activity, App.getDefaultValidator(activity));
        final ChangedElement[] changes = getPendingChanges(activity.getResources(), elements == null ? App.getLogic().getPendingChangedElements() : elements);
        revalidate(activity, validator, changes);
        Arrays.sort(changes, comparator);

        changesView.setAdapter(new ValidatorArrayAdapter(activity, itemResource, changes, validator, parentTag));
    }

    /**
     * Rerun validation on the changes
     * 
     * @param context an Android Context
     * @param validator the Validator to use
     * @param changes the list of changes
     */
    private void revalidate(@NonNull Context context, @NonNull Validator validator, @NonNull final ChangedElement[] changes) {
        for (ChangedElement ce : changes) {
            OsmElement element = ce.element;
            element.resetHasProblem();
            element.hasProblem(context, validator);
        }
        if (context instanceof Main) {
            ((Main) context).invalidateMap();
        }
    }

    protected static class ChangedElement {
        final OsmElement element;
        final String     description;
        boolean          selected;

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
     * Highlight elements for upload that have a potential issue
     * 
     * @author Simon Poole
     *
     */
    protected static class ValidatorArrayAdapter extends ArrayAdapter<ChangedElement> {
        final ChangedElement[] elements;
        final Validator        validator;
        final ColorStateList   colorStateList;
        final String           parentTag;

        /**
         * Construct a new instance
         * 
         * @param context Android Context
         * @param resource the resource id of the per item layout
         * @param elements the array holding the elements
         * @param validator the Validator to use
         * @param parentTag
         */
        public ValidatorArrayAdapter(@NonNull Context context, int resource, @NonNull final ChangedElement[] elements, @NonNull Validator validator,
                @Nullable String parentTag) {
            super(context, resource, R.id.text1, elements);
            this.elements = elements;
            this.validator = validator;
            this.parentTag = parentTag;
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
                textView.setOnClickListener(view -> {
                    ChangedElement clicked = elements[position];
                    OsmElement e = clicked.element;
                    byte elemenState = element.getState();
                    boolean deleted = elemenState == OsmElement.STATE_DELETED;
                    final FragmentActivity fragmentActivity = (FragmentActivity) view.getContext();
                    if (elemenState == OsmElement.STATE_MODIFIED || deleted) {
                        ElementInfo.showDialog(fragmentActivity, UndoStorage.ORIGINAL_ELEMENT_INDEX, e, !deleted, true, parentTag);
                    } else {
                        ElementInfo.showDialog(fragmentActivity, e, !deleted, parentTag);
                    }
                });
            } else {
                Log.e("ValidatorAdapterView", "position " + position + " view is null");
            }
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkBox1);
            if (checkBox != null) {
                checkBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
                    elements[position].selected = isChecked;
                    notifyDataSetChanged();
                });
                checkBox.setChecked(elements[position].selected);
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
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        if (elements != null) {
            Util.putElementsInBundle(elements, outState);
        }
    }
}
