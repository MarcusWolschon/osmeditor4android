package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.exception.UiStateException;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetRole;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;

/**
 * UI for managing a elements membership in relations
 * 
 * @author Simon Poole
 *
 */
public class RelationMembershipFragment extends BaseFragment implements PropertyRows, OnItemSelectedListener {
    private static final String DEBUG_TAG = RelationMembershipFragment.class.getSimpleName();

    private static final String PARENTS_KEY      = "parents";
    private static final String ELEMENT_TYPE_KEY = "element_type";

    private LayoutInflater inflater = null;

    private MultiHashMap<Long, RelationMemberPosition> savedParents = null;
    private String                                     elementType  = null;

    private int maxStringLength; // maximum key, value and role length

    private PropertyEditorListener propertyEditorListener;

    private static SelectedRowsActionModeCallback parentSelectedActionModeCallback = null;
    private static final Object                   actionModeCallbackLock           = new Object();

    /**
     * Create a new RelationMembershipFragment instance
     * 
     * @param parents a HashMap containing the parent Relations
     * @param type the element type of the edited object
     * @return a new RelationMembershipFragment instance
     */
    @NonNull
    public static RelationMembershipFragment newInstance(MultiHashMap<Long, RelationMemberPosition> parents, String type) {
        RelationMembershipFragment f = new RelationMembershipFragment();

        Bundle args = new Bundle();
        args.putSerializable(PARENTS_KEY, parents);
        args.putString(ELEMENT_TYPE_KEY, type);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        try {
            propertyEditorListener = (PropertyEditorListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement PropertyEditorListener");
        }
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView parentRelationsLayout = null;
        LinearLayout membershipVerticalLayout = null;

        // Inflate the layout for this fragment
        this.inflater = inflater;
        parentRelationsLayout = (ScrollView) inflater.inflate(R.layout.membership_view, container, false);
        membershipVerticalLayout = (LinearLayout) parentRelationsLayout.findViewById(R.id.membership_vertical_layout);
        // membershipVerticalLayout.setSaveFromParentEnabled(false);
        membershipVerticalLayout.setSaveEnabled(false);

        MultiHashMap<Long, RelationMemberPosition> parents;
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring from saved state");
            parents = (MultiHashMap<Long, RelationMemberPosition>) savedInstanceState.getSerializable(PARENTS_KEY);
            elementType = savedInstanceState.getString(ELEMENT_TYPE_KEY);
        } else if (savedParents != null) {
            Log.d(DEBUG_TAG, "Restoring from instance variable");
            parents = savedParents;
        } else {
            parents = (MultiHashMap<Long, RelationMemberPosition>) getArguments().getSerializable(PARENTS_KEY);
            elementType = getArguments().getString(ELEMENT_TYPE_KEY);
        }

        Preferences prefs = App.getLogic().getPrefs();
        Server server = prefs.getServer();
        maxStringLength = server.getCachedCapabilities().getMaxStringLength();

        loadParents(membershipVerticalLayout, parents, elementType);

        CheckBox headerCheckBox = (CheckBox) parentRelationsLayout.findViewById(R.id.header_membership_selected);
        headerCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectAllRows();
                } else {
                    deselectAllRows();
                }
            }
        });

        return parentRelationsLayout;
    }

    /**
     * Creates rows from a map containing the id of the parent relations and the role in that relation
     * 
     * @param parents map containing the id of the parent relations and the role in that relation
     * @param elementType type of the element being edited
     */
    private void loadParents(final MultiHashMap<Long, RelationMemberPosition> parents, @NonNull String elementType) {
        LinearLayout membershipVerticalLayout = (LinearLayout) getOurView();
        loadParents(membershipVerticalLayout, parents, elementType);
    }

    /**
     * Creates rows from a map containing the id of the parent relations and the role in that relation
     * 
     * @param membershipVerticalLayout the Layout holding the rows
     * @param parents map containing the id of the parent relations and the role in that relation
     * @param elementType type of the element being edited
     */
    private void loadParents(LinearLayout membershipVerticalLayout, final MultiHashMap<Long, RelationMemberPosition> parents, @NonNull String elementType) {
        membershipVerticalLayout.removeAllViews();
        if (parents != null && !parents.isEmpty()) {
            StorageDelegator storageDelegator = App.getDelegator();
            for (Long id : parents.getKeys()) {
                Relation r = (Relation) storageDelegator.getOsmElement(Relation.NAME, id);
                Set<RelationMemberPosition> rmps = parents.get(id);
                for (RelationMemberPosition rmp : rmps) {
                    insertNewMembership(membershipVerticalLayout, rmp.getRole(), r, elementType, rmp.getPosition(), 0, false);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        savedParents = getParentRelationMap();
        outState.putSerializable(PARENTS_KEY, savedParents);
        outState.putString(ELEMENT_TYPE_KEY, elementType);
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        synchronized (actionModeCallbackLock) {
            if (parentSelectedActionModeCallback != null) {
                parentSelectedActionModeCallback.currentAction.finish();
                parentSelectedActionModeCallback = null;
            }
        }
    }

    /**
     * Insert a new row with a parent relation
     * 
     * @param membershipVerticalLayout the Layout holding the rows
     * @param role role of this element in the relation
     * @param r the relation
     * @param elementType type of the element being edited
     * @param memberPos position of the member in the relation member list
     * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at
     *            beginning.
     * @param showSpinner show the role spinner on insert
     * @return the new RelationMembershipRow
     */
    private RelationMembershipRow insertNewMembership(LinearLayout membershipVerticalLayout, final String role, final Relation r, @NonNull String elementType,
            int memberPos, final int position, boolean showSpinner) {
        RelationMembershipRow row = (RelationMembershipRow) inflater.inflate(R.layout.relation_membership_row, membershipVerticalLayout, false);

        if (r != null) {
            row.setValues(role, r, elementType, memberPos);
        }
        membershipVerticalLayout.addView(row, (position == -1) ? membershipVerticalLayout.getChildCount() : position);
        row.setShowSpinner(showSpinner);

        row.roleEdit.addTextChangedListener(new SanitizeTextWatcher(getActivity(), maxStringLength));

        row.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    parentSelected();
                } else {
                    deselectRow();
                }
            }
        });

        return row;
    }

    static class RelationHolder {
        final Relation relation;
        final Context  ctx;

        /**
         * Wrapper around relations so that we can display a nice String
         * 
         * @param ctx an Android Context
         * @param relation the Relation
         */
        RelationHolder(@NonNull Context ctx, @NonNull Relation relation) {
            this.ctx = ctx;
            this.relation = relation;
        }

        @Override
        public String toString() {
            return relation.getDescription(ctx);
        }
    }

    /**
     * A row representing a parent relation with an edit for role and further values and a delete button.
     */
    public static class RelationMembershipRow extends LinearLayout implements SelectedRowsActionModeCallback.Row {

        private PropertyEditor       owner;
        private long                 relationId     = -1;   // flag value for new relation memberships
        private CheckBox             selected;
        private AutoCompleteTextView roleEdit;
        private Spinner              parentEdit;
        private boolean              showSpinner    = false;
        private String               elementType    = null;
        private PresetItem           relationPreset = null;
        private int                  position;

        /**
         * Construct a roe
         * 
         * @param context an Android Context
         */
        public RelationMembershipRow(@NonNull Context context) {
            super(context);
            owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or
                                                                        // in Eclipse
        }

        /**
         * Construct a roe
         * 
         * @param context an Android Context
         * @param attrs an AttributeSet
         */
        public RelationMembershipRow(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or
                                                                        // in Eclipse
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            selected = (CheckBox) findViewById(R.id.parent_selected);

            roleEdit = (AutoCompleteTextView) findViewById(R.id.editRole);
            roleEdit.setOnKeyListener(PropertyEditor.myKeyListener);

            parentEdit = (Spinner) findViewById(R.id.editParent);
            ArrayAdapter<RelationHolder> a = getRelationSpinnerAdapter();
            parentEdit.setAdapter(a);
            parentEdit.setOnItemSelectedListener(owner.relationMembershipFragment);

            roleEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        roleEdit.setAdapter(getMembershipRoleAutocompleteAdapter());
                        if (/* running && */roleEdit.getText().length() == 0) {
                            roleEdit.showDropDown();
                        }
                    }
                }
            });

            OnClickListener autocompleteOnClick = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.hasFocus()) {
                        ((AutoCompleteTextView) v).showDropDown();
                    }
                }
            };

            roleEdit.setOnClickListener(autocompleteOnClick);

            roleEdit.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(DEBUG_TAG, "onItemClicked value");
                    Object o = parent.getItemAtPosition(position);
                    if (o instanceof StringWithDescription) {
                        roleEdit.setText(((StringWithDescription) o).getValue());
                    } else if (o instanceof String) {
                        roleEdit.setText((String) o);
                    } else if (o instanceof PresetRole) {
                        roleEdit.setText(((PresetRole) o).getRole());
                    }
                }
            });
        }

        /**
         * Get the best matching preset for the Relation
         * 
         * @return a PresetItem or null
         */
        @Nullable
        PresetItem getRelationPreset() {
            Preset[] presets = owner.getPresets();
            Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, relationId);
            if (relationPreset == null && presets != null && r != null) {
                relationPreset = Preset.findBestMatch(presets, r.getTags());
            }
            return relationPreset;
        }

        /**
         * Create an ArrayAdapter containing role values for the edited object in a parent Relation
         * 
         * @return an ArrayAdapter holding the possible roles
         */
        @NonNull
        ArrayAdapter<PresetRole> getMembershipRoleAutocompleteAdapter() {
            List<PresetRole> result = new ArrayList<>();
            PresetItem presetItem = getRelationPreset();
            if (presetItem != null) {
                Map<String, Integer> counter = new HashMap<>();
                int position = 0;
                List<String> tempRoles = App.getMruTags().getRoles(presetItem);
                if (tempRoles != null) {
                    for (String role : tempRoles) {
                        result.add(new PresetRole(role, null, elementType));
                        counter.put(role, position++);
                    }
                }
                List<PresetRole> tempPresetRoles = presetItem.getRoles(getContext(), owner.getElement(), owner.getKeyValueMapSingle(true));
                if (tempPresetRoles != null) {
                    Collections.sort(tempPresetRoles);
                    for (PresetRole presetRole : tempPresetRoles) {
                        Integer counterPos = counter.get(presetRole.getRole());
                        if (counterPos != null) {
                            result.get(counterPos).setHint(presetRole.getHint());
                            continue;
                        }
                        result.add(presetRole);
                    }
                }
            } else {
                List<String> tempRoles = App.getMruTags().getRoles();
                if (tempRoles != null) {
                    for (String role : tempRoles) {
                        result.add(new PresetRole(role, null, null));
                    }
                }
            }
            return new ArrayAdapter<>(owner, R.layout.autocomplete_row, result);
        }

        /**
         * Get an ArrayAdapter containing all the Relations currently downloaded
         * 
         * @return an ArrayAdapter holding the Relations
         */
        @NonNull
        ArrayAdapter<RelationHolder> getRelationSpinnerAdapter() {
            //
            List<RelationHolder> result = new ArrayList<>();
            for (Relation r : App.getDelegator().getCurrentStorage().getRelations()) {
                result.add(new RelationHolder(getContext(), r));
            }
            return new ArrayAdapter<>(owner, R.layout.autocomplete_row, result);
        }

        /**
         * Set the role and relation for this row
         * 
         * @param role the role of this element in the Relation
         * @param r the Relation it is a member of
         * @param elementType the type of the element
         * @param position the position in the list of members
         * @return the RelationMembershipRow object for convenience
         */
        public RelationMembershipRow setValues(String role, Relation r, String elementType, int position) {
            relationId = r.getOsmId();
            roleEdit.setText(role);
            parentEdit.setSelection(App.getDelegator().getCurrentStorage().getRelations().indexOf(r));
            this.elementType = elementType;
            this.position = position;
            return this;
        }

        /**
         * Sets the Relation for this row
         * 
         * @param r the Relation to set for this row
         * @return the RelationMembershipRow object for convenience
         */
        public RelationMembershipRow setRelation(Relation r) {
            relationId = r.getOsmId();
            parentEdit.setSelection(App.getDelegator().getCurrentStorage().getRelations().indexOf(r));
            position = r.getMembers().size(); // last position
            Log.d(DEBUG_TAG, "Set parent relation to " + relationId + " " + r.getDescription());
            relationPreset = null; // zap to force it to be re-calculated
            roleEdit.setAdapter(getMembershipRoleAutocompleteAdapter()); // update
            return this;
        }

        /**
         * The role of the member in this row
         * 
         * @return the role of this member
         */
        @NonNull
        public String getRole() {
            return roleEdit.getText().toString();
        }

        /**
         * Deletes this row
         */
        @Override
        public void delete() {
            if (owner != null) {
                View cf = owner.getCurrentFocus();
                if (cf == roleEdit) {
                    // owner.focusRow(0); // FIXME focus is on this fragment
                }
                LinearLayout membershipVerticalLayout = (LinearLayout) owner.relationMembershipFragment.getOurView();
                membershipVerticalLayout.removeView(this);
                membershipVerticalLayout.invalidate();
            } else {
                Log.d("PropertyEditor", "deleteRow owner null");
            }
        }

        /**
         * awful hack to show spinner after insert
         */
        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
            if (showSpinner) {
                parentEdit.performClick();
                showSpinner = false;
            }
        }

        // return the status of the checkbox
        @Override
        public boolean isSelected() {
            return selected.isChecked();
        }

        @Override
        public void deselect() {
            selected.setChecked(false);
        }

        /**
         * Disable the checkbox for this row
         */
        public void disableCheckBox() {
            selected.setEnabled(false);
        }

        /**
         * Enable the checkbox for this row
         */
        protected void enableCheckBox() {
            selected.setEnabled(true);
        }

        /**
         * Set if the spinner should be shown
         * 
         * @param showSpinner if true the spinner is shown
         */
        public void setShowSpinner(boolean showSpinner) {
            this.showSpinner = showSpinner;
        }
    } // RelationMembershipRow

    /**
     * Start the action mode when a row is selected
     */
    private void parentSelected() {
        synchronized (actionModeCallbackLock) {
            LinearLayout rowLayout = (LinearLayout) getOurView();
            if (parentSelectedActionModeCallback == null) {
                parentSelectedActionModeCallback = new SelectedRowsActionModeCallback(this, rowLayout);
                ((AppCompatActivity) getActivity()).startSupportActionMode(parentSelectedActionModeCallback);
            }
        }
    }

    @Override
    public void deselectRow() {
        synchronized (actionModeCallbackLock) {
            if (parentSelectedActionModeCallback != null && parentSelectedActionModeCallback.rowsDeselected(true)) {
                parentSelectedActionModeCallback = null;
            }
        }
    }

    @Override
    public void selectAllRows() { // select all parents
        LinearLayout rowLayout = (LinearLayout) getOurView();

        int i = rowLayout.getChildCount();
        while (--i >= 0) {
            RelationMembershipRow row = (RelationMembershipRow) rowLayout.getChildAt(i);
            if (row.selected.isEnabled()) {
                row.selected.setChecked(true);
            }
        }
    }

    @Override
    public void deselectAllRows() { // // select all parents
        LinearLayout rowLayout = (LinearLayout) getOurView();

        int i = rowLayout.getChildCount();
        while (--i >= 0) {
            RelationMembershipRow row = (RelationMembershipRow) rowLayout.getChildAt(i);
            if (row.selected.isEnabled()) {
                row.selected.setChecked(false);
            }
        }
    }

    /**
     */
    private interface ParentRelationHandler {
        /**
         * Process the contents of one RelationMembershipRow
         * 
         * @param row the RelationMembershipRow
         */
        void handleParentRelation(@NonNull final RelationMembershipRow row);
    }

    /**
     * Perform some processing for each row in the parent relation view.
     * 
     * @param handler The handler that will be called for each row.
     */
    private void processParentRelations(final ParentRelationHandler handler) {
        LinearLayout membershipVerticalLayout = (LinearLayout) getOurView();
        final int size = membershipVerticalLayout.getChildCount();
        for (int i = 0; i < size; ++i) {
            View view = membershipVerticalLayout.getChildAt(i);
            RelationMembershipRow row = (RelationMembershipRow) view;
            handler.handleParentRelation(row);
        }
    }

    /**
     * Collect all interesting values from the parent relation view value
     * 
     * @return a MultiHashMap¬&lt;Long,RelationMemberDescription&gt; of relation and role with position in that relation,
     *         pairs.
     */
    MultiHashMap<Long, RelationMemberPosition> getParentRelationMap() {
        final MultiHashMap<Long, RelationMemberPosition> parents = new MultiHashMap<>(false, true);
        processParentRelations(new ParentRelationHandler() {
            @Override
            public void handleParentRelation(final RelationMembershipRow row) {
                String role = row.roleEdit.getText().toString().trim();
                RelationMemberPosition rmp = new RelationMemberPosition(
                        new RelationMember(row.elementType, propertyEditorListener.getElement().getOsmId(), role), row.position);
                parents.add(row.relationId, rmp);
                Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, row.relationId);
                if (r == null) {
                    Log.e(DEBUG_TAG, "Inconsistent state: parent relation " + row.relationId + " not in storage");
                    return;
                }
                RelationMember rm = r.getMember(propertyEditorListener.getElement());
                PresetItem presetItem = row.getRelationPreset();
                if (rm != null) { // can't really happen
                    if (!"".equals(role) && rm.getRole() != null && !rm.getRole().equals(role)) {
                        // only add if the role actually differs
                        if (presetItem != null) {
                            App.getMruTags().putRole(presetItem, role);
                        } else {
                            App.getMruTags().putRole(role);
                        }
                    }
                }
            }
        });
        return parents;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        Relation relation = ((RelationHolder) parent.getItemAtPosition(pos)).relation;
        Log.d(DEBUG_TAG, relation.getDescription());
        if (view != null) {
            ViewParent pv = view.getParent();
            while (!(pv instanceof RelationMembershipRow)) {
                pv = pv.getParent();
            }
            ((RelationMembershipRow) pv).setRelation(relation);
        } else {
            Log.d(DEBUG_TAG, "onItemselected view is null");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.membership_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            ((PropertyEditor) getActivity()).sendResultAndFinish();
            return true;
        case R.id.tag_menu_revert:
            doRevert();
            return true;
        case R.id.tag_menu_addtorelation:
            addToRelation(elementType);
            return true;
        case R.id.tag_menu_select_all:
            selectAllRows();
            return true;
        case R.id.tag_menu_help:
            HelpViewer.start(getActivity(), R.string.help_propertyeditor);
            return true;
        default:
            return true;
        }
    }

    /**
     * reload original arguments
     */
    @SuppressWarnings("unchecked")
    void doRevert() {
        loadParents((MultiHashMap<Long, RelationMemberPosition>) getArguments().getSerializable(PARENTS_KEY), getArguments().getString(ELEMENT_TYPE_KEY));
    }

    /**
     * Add this object to an existing relation
     * 
     * @param elementType the type of the OsmElement
     */
    private void addToRelation(@NonNull String elementType) {
        insertNewMembership((LinearLayout) getOurView(), null, null, elementType, 0, -1, true).roleEdit.requestFocus();
    }

    @Override
    public void deselectHeaderCheckBox() {
        CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_membership_selected);
        headerCheckBox.setChecked(false);
    }

    /**
     * Return the view we have our rows in and work around some android craziness
     * 
     * @return the row container view
     */
    @NonNull
    private View getOurView() {
        // android.support.v4.app.NoSaveStateFrameLayout
        View v = getView();
        if (v != null) {
            if (v.getId() == R.id.membership_vertical_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.membership_vertical_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.membership_vertical_layout");
                    throw new UiStateException("didn't find R.id.membership_vertical_layout");
                } else {
                    Log.d(DEBUG_TAG, "Found R.id.membership_vertical_layoutt");
                }
                return v;
            }
        } else {
            // given that this is always fatal might as well throw the exception here
            Log.d(DEBUG_TAG, "got null view in getView");
            throw new UiStateException("got null view in getView");
        }
    }
}
