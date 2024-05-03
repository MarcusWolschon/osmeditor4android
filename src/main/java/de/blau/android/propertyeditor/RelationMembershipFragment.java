package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Bundle;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.exception.UiStateException;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.osm.RelationUtils;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetRole;
import de.blau.android.util.ArrayAdapterWithState;
import de.blau.android.util.Enabled;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;

/**
 * UI for managing a elements membership in relations
 * 
 * @author Simon Poole
 *
 */
public class RelationMembershipFragment extends SelectableRowsFragment implements PropertyRows, OnItemSelectedListener, DataUpdate {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RelationMembershipFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = RelationMembershipFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String PARENTS_KEY      = "parents";
    private static final String ELEMENT_TYPE_KEY = "element_type";

    private LayoutInflater inflater = null;

    private MultiHashMap<Long, RelationMemberPosition> savedParents = null;
    private String                                     elementType  = null;

    private int maxStringLength; // maximum key, value and role length

    private PropertyEditorListener propertyEditorListener;

    private ArrayAdapter<RelationHolder> relationAdapter;
    private List<RelationHolder>         relationHolderList;

    /**
     * Create a new RelationMembershipFragment instance
     * 
     * @param parents a HashMap containing the parent Relations
     * @param type the element type of the edited object
     * @return a new RelationMembershipFragment instance
     */
    @NonNull
    public static RelationMembershipFragment newInstance(@Nullable MultiHashMap<Long, RelationMemberPosition> parents, @NonNull String type) {
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
        Fragment parent = getParentFragment();
        Util.implementsInterface(parent, PropertyEditorListener.class);
        propertyEditorListener = (PropertyEditorListener) parent;
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

        // Inflate the layout for this fragment
        this.inflater = inflater;
        LinearLayout parentRelationsLayout = (LinearLayout) inflater.inflate(R.layout.membership_view, container, false);
        LinearLayout membershipVerticalLayout = (LinearLayout) parentRelationsLayout.findViewById(R.id.membership_vertical_layout);
        membershipVerticalLayout.setSaveEnabled(false);

        MultiHashMap<Long, RelationMemberPosition> parents;
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring from saved state");
            parents = Util.getSerializeable(savedInstanceState, PARENTS_KEY, MultiHashMap.class);
            elementType = savedInstanceState.getString(ELEMENT_TYPE_KEY);
        } else if (savedParents != null) {
            Log.d(DEBUG_TAG, "Restoring from instance variable");
            parents = savedParents;
        } else {
            parents = Util.getSerializeable(getArguments(), PARENTS_KEY, MultiHashMap.class);
            elementType = getArguments().getString(ELEMENT_TYPE_KEY);
        }

        maxStringLength = propertyEditorListener.getCapabilities().getMaxStringLength();

        relationHolderList = getAllRelations(new ArrayList<>());

        // Adapter containing all Relations
        relationAdapter = new ArrayAdapterWithState<>(getActivity(), R.layout.autocomplete_row, relationHolderList);

        loadParents(membershipVerticalLayout, parents, elementType);

        CheckBox headerCheckBox = (CheckBox) parentRelationsLayout.findViewById(R.id.header_membership_selected);
        headerCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectAllRows();
            } else {
                deselectAllRows();
            }
        });
        return parentRelationsLayout;
    }

    @Override
    public void onDataUpdate() {
        Log.d(DEBUG_TAG, "onDataUpdate");
        OsmElement element = propertyEditorListener.getElement();
        final MultiHashMap<Long, RelationMemberPosition> newParents = PropertyEditorData.getParentMap(element, new MultiHashMap<>(false, true));
        final MultiHashMap<Long, RelationMemberPosition> oldParents = getParentRelationMap();
        if (!oldParents.equals(newParents)) {
            ScreenMessage.toastTopInfo(getContext(), R.string.toast_updating_parents);
            loadParents(newParents, element.getName());
        }
        relationHolderList.clear();
        relationHolderList = getAllRelations(relationHolderList);
        relationAdapter.notifyDataSetChanged();
    }

    /**
     * Get a list of all relations except the edited object if it is one
     * 
     * @param relations the List to fill
     * @return the list for convenience
     */
    @NonNull
    private List<RelationHolder> getAllRelations(@NonNull List<RelationHolder> relations) {
        final Context context = getContext();
        int limit = propertyEditorListener.getCapabilities().getMaxRelationMembers();
        relations.add(new RelationHolder(context, null, limit)); // empty list entry
        final OsmElement element = propertyEditorListener.getElement();
        final long osmId = element.getOsmId();
        final boolean isRelation = Relation.NAME.equals(elementType);
        List<Relation> temp = new ArrayList<>(App.getDelegator().getCurrentStorage().getRelations());
        RelationUtils.sortRelationListByDistance(Util.wrapInList(element), temp);
        for (Relation r : temp) {
            // we don't want to make it too easy to create relation loops and
            // filter out the current element out if it is a relation
            if (isRelation && r.getOsmId() == osmId) {
                continue;
            }
            relations.add(new RelationHolder(context, r, limit));
        }
        return relations;
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
                    insertNewMembership(membershipVerticalLayout, rmp.getRole(), r, elementType, rmp.getPosition(), -1, false);
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
    private RelationMembershipRow insertNewMembership(@NonNull LinearLayout membershipVerticalLayout, final String role, @Nullable final Relation r,
            @NonNull String elementType, int memberPos, final int position, boolean showSpinner) {
        RelationMembershipRow row = (RelationMembershipRow) inflater.inflate(R.layout.relation_membership_row, membershipVerticalLayout, false);
        row.setOwner(this);
        if (r != null) {
            row.setValues(role, r, elementType, memberPos, relationAdapter, relationHolderList);
        } else {
            row.setRelationAdapter(relationAdapter);
        }
        membershipVerticalLayout.addView(row, (position == -1) ? membershipVerticalLayout.getChildCount() : position);
        row.setShowSpinner(showSpinner);

        row.roleEdit.addTextChangedListener(new SanitizeTextWatcher(getActivity(), maxStringLength));

        row.selected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                parentSelected();
            } else {
                deselectRow();
            }
        });

        return row;
    }

    static class RelationHolder implements Enabled {
        private final Relation relation;
        private final Context  ctx;
        private final int      limit;

        /**
         * Wrapper around relations so that we can display a nice String
         * 
         * @param ctx an Android Context
         * @param relation the Relation
         * @param limit the max number of relation members
         */
        RelationHolder(@NonNull Context ctx, @Nullable Relation relation, int limit) {
            this.ctx = ctx;
            this.relation = relation;
            this.limit = limit;
        }

        @Override
        public String toString() {
            return relation == null ? "" : relation.getDescription(ctx);
        }

        @Override
        public boolean isEnabled() {
            return relation == null || relation.getMemberCount() < limit;
        }
    }

    /**
     * A row representing a parent relation with an edit for role and further values and a delete button.
     */
    public static class RelationMembershipRow extends LinearLayout implements SelectedRowsActionModeCallback.Row {

        private static final int           UNSET          = -1;    // relations never get id -1
        private RelationMembershipFragment owner;
        private long                       relationId     = UNSET; // flag value for new relation memberships
        private CheckBox                   selected;
        private AutoCompleteTextView       roleEdit;
        private Spinner                    parentEdit;
        private boolean                    showSpinner    = false;
        private String                     elementType    = null;
        private PresetItem                 relationPreset = null;
        private int                        position;

        /**
         * Construct a row
         * 
         * @param context an Android Context
         */
        public RelationMembershipRow(@NonNull Context context) {
            super(context);
        }

        /**
         * Construct a row
         * 
         * @param context an Android Context
         * @param attrs an AttributeSet
         */
        public RelationMembershipRow(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        /**
         * Set the fragment for this view
         * 
         * @param owner the "owning" Fragment
         */
        public void setOwner(@NonNull RelationMembershipFragment owner) {
            this.owner = owner;
            parentEdit.setOnItemSelectedListener(owner);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            selected = (CheckBox) findViewById(R.id.parent_selected);

            roleEdit = (AutoCompleteTextView) findViewById(R.id.editRole);
            roleEdit.setOnKeyListener(PropertyEditorFragment.myKeyListener);

            parentEdit = (Spinner) findViewById(R.id.editParent);

            roleEdit.setOnClickListener(v -> {
                if (v.hasFocus()) {
                    if (roleEdit.getAdapter() == null) {
                        roleEdit.setAdapter(getMembershipRoleAutocompleteAdapter());
                    }
                    ((AutoCompleteTextView) v).showDropDown();
                }
            });

            setRoleOnItemClickListener(roleEdit);
        }

        /**
         * Get the best matching preset for the Relation
         * 
         * @return a PresetItem or null
         */
        @Nullable
        PresetItem getRelationPreset() {
            Preset[] presets = ((PropertyEditorListener) owner.getParentFragment()).getPresets();
            Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, relationId);
            if (relationPreset == null && presets != null && r != null) {
                relationPreset = Preset.findBestMatch(presets, r.getTags(), null, null);
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
                int pos = 0;
                List<String> tempRoles = App.getMruTags().getRoles(presetItem);
                if (tempRoles != null) {
                    for (String role : tempRoles) {
                        result.add(new PresetRole(role, null, elementType));
                        counter.put(role, pos++);
                    }
                }
                PropertyEditorListener listener = (PropertyEditorListener) owner.getParentFragment();
                List<PresetRole> tempPresetRoles = presetItem.getRoles(getContext(), listener.getElement(),
                        ((PropertyEditorFragment) listener).getKeyValueMapSingle(true), listener.getIsoCodes());
                if (tempPresetRoles != null) {
                    countAndAddRoles(tempPresetRoles, counter, result);
                }
            } else {
                List<String> tempRoles = App.getMruTags().getRoles();
                if (tempRoles != null) {
                    for (String role : tempRoles) {
                        result.add(new PresetRole(role, null, null));
                    }
                }
            }
            return new ArrayAdapter<>(getContext(), R.layout.autocomplete_row, result);
        }

        /**
         * Add the roles in input to result, counting them in the process
         * 
         * @param input input list of roles
         * @param counter map holding role counts
         * @param result output list of unique roles with counts in the hint
         */
        static void countAndAddRoles(@NonNull List<PresetRole> input, @NonNull Map<String, Integer> counter, @NonNull List<PresetRole> result) {
            Collections.sort(input);
            for (PresetRole presetRole : input) {
                Integer counterPos = counter.get(presetRole.getRole());
                if (counterPos != null) {
                    result.get(counterPos).setHint(presetRole.getHint());
                    continue;
                }
                result.add(presetRole);
            }
        }

        /**
         * Set the role and relation for this row
         * 
         * @param role the role of this element in the Relation
         * @param r the Relation it is a member of
         * @param elementType the type of the element
         * @param position the the selected elements position in the list of members
         * @param relationAdapter the adapter with potential parent Relations
         * @param relationHolderList the current parents?
         * @return the RelationMembershipRow object for convenience
         */
        public RelationMembershipRow setValues(@NonNull String role, @NonNull Relation r, @NonNull String elementType, int position,
                @NonNull ArrayAdapter<RelationHolder> relationAdapter, @NonNull List<RelationHolder> relationHolderList) {
            relationId = r.getOsmId();
            roleEdit.setText(role);
            parentEdit.setAdapter(relationAdapter);
            parentEdit.setSelection(getRelationIndex(relationHolderList, r));
            this.elementType = elementType;
            this.position = position;
            return this;
        }

        /**
         * Get the index of the specified Relation
         * 
         * @param relationHolderList the list of relations
         * @param r the Relation
         * @return the index
         */
        private int getRelationIndex(@NonNull List<RelationHolder> relationHolderList, @NonNull Relation r) {
            int i = 0;
            for (RelationHolder rh : relationHolderList) {
                if (r.equals(rh.relation)) {
                    return i;
                }
                i++;
            }
            return 0;
        }

        /**
         * Set the adapter containing all Relations
         * 
         * @param relationAdapter the adapter
         */
        public void setRelationAdapter(@NonNull ArrayAdapter<RelationHolder> relationAdapter) {
            parentEdit.setAdapter(relationAdapter);
        }

        /**
         * Sets the Relation for this row
         * 
         * @param relationPos the position in the adapter
         * @param r the Relation to set for this row
         * @return the RelationMembershipRow object for convenience
         */
        public RelationMembershipRow setRelation(int relationPos, @Nullable Relation r) {
            if (r != null) {
                relationId = r.getOsmId();
                parentEdit.setSelection(relationPos);
                position = r.getMembers().size(); // last position
                Log.d(DEBUG_TAG, "Set parent relation to " + relationId + " " + r.getDescription());
            } else {
                relationId = UNSET;
            }
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
                View cf = owner.getActivity().getCurrentFocus();
                if (cf == roleEdit) {
                    focusRow(0);
                }
                LinearLayout membershipVerticalLayout = (LinearLayout) owner.getOurView();
                membershipVerticalLayout.removeView(this);
                membershipVerticalLayout.invalidate();
            } else {
                Log.d("PropertyEditor", "deleteRow owner null");
            }
        }

        /**
         * Move the focus to the role field of the specified row.
         * 
         * @param index The index of the row to move to, counting from 0.
         * @return true if the row was successfully focused, false otherwise.
         */
        private boolean focusRow(int index) {
            RelationMembershipRow row = (RelationMembershipRow) ((LinearLayout) getParent()).getChildAt(index);
            return row != null && row.roleEdit.requestFocus();
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

        @Override
        public void select() {
            selected.setChecked(true);
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
     * Set the OnItemClickListener for the role view
     * 
     * @param role the role view
     * 
     */
    static void setRoleOnItemClickListener(@NonNull AutoCompleteTextView role) {
        role.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(DEBUG_TAG, "onItemClicked role");
            Object o = parent.getItemAtPosition(position);
            if (o instanceof StringWithDescription) {
                role.setText(((StringWithDescription) o).getValue());
            } else if (o instanceof String) {
                role.setText((String) o);
            } else if (o instanceof PresetRole) {
                role.setText(((PresetRole) o).getRole());
            }
        });
    }

    @Override
    protected SelectedRowsActionModeCallback getActionModeCallback() {
        return new ParentSelectedActionModeCallback(this, (LinearLayout) getOurView());
    }

    /**
     * Start the action mode when a row is selected
     */
    private void parentSelected() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback == null) {
                actionModeCallback = getActionModeCallback();
                ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
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
        for (int i = 0; i < size; i++) {
            View view = membershipVerticalLayout.getChildAt(i);
            RelationMembershipRow row = (RelationMembershipRow) view;
            handler.handleParentRelation(row);
        }
    }

    /**
     * Collect all interesting values from the parent relation view value
     * 
     * @return a MultiHashMap¬&lt;Long,RelationMemberDescription&gt; of relation and role with position in that
     *         relation, pairs.
     */
    @NonNull
    MultiHashMap<Long, RelationMemberPosition> getParentRelationMap() {
        final MultiHashMap<Long, RelationMemberPosition> parents = new MultiHashMap<>(false, true);
        processParentRelations(row -> {
            if (row.relationId != -1) {
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
                if (rm != null && !"".equals(role) && rm.getRole() != null && !rm.getRole().equals(role)) {
                    // only add if the role actually differs
                    if (presetItem != null) {
                        App.getMruTags().putRole(presetItem, role);
                    } else {
                        App.getMruTags().putRole(role);
                    }
                }
            }
        });
        return parents;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Log.d(DEBUG_TAG, "onItemSelected");
        Relation relation = ((RelationHolder) parent.getItemAtPosition(pos)).relation;
        if (view != null && relation != null) {
            Log.d(DEBUG_TAG, "selected " + relation.getDescription());
            ViewParent pv = view.getParent();
            while (!(pv instanceof RelationMembershipRow)) {
                pv = pv.getParent();
            }
            if (relation.getOsmId() != ((RelationMembershipRow) pv).relationId) {
                ((RelationMembershipRow) pv).setRelation(pos, relation);
            }
        } else {
            Log.d(DEBUG_TAG, "onItemselected view or relation is null");
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
            propertyEditorListener.updateAndFinish();
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
            return false;
        }
    }

    /**
     * reload original arguments
     */
    @SuppressWarnings("unchecked")
    void doRevert() {
        loadParents(Util.getSerializeable(getArguments(), PARENTS_KEY, MultiHashMap.class), getArguments().getString(ELEMENT_TYPE_KEY));
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
