package de.blau.android.propertyeditor;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.exception.UiStateException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetItem;
import de.blau.android.presets.PresetRole;
import de.blau.android.propertyeditor.RelationMembershipFragment.RelationMembershipRow;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ScrollingLinearLayoutManager;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;

/**
 * UI for managing relation members
 * 
 * @author Simon Poole
 *
 */
public class RelationMembersFragment extends SelectableRowsFragment implements PropertyRows, DataUpdate {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, RelationMembersFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = RelationMembersFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String MEMBERS_KEY = "members";
    private static final String ID_KEY      = "id";

    private static final String FILENAME_MEMBERS      = "current_members" + "." + FileExtensions.RES;
    private static final String FILENAME_ORIG_MEMBERS = "orig_members" + "." + FileExtensions.RES;

    private ArrayList<RelationMemberDescription>               savedMembers = null;
    private long                                               id           = -1;
    private SavingHelper<ArrayList<RelationMemberDescription>> savingHelper = new SavingHelper<>();

    PropertyEditorListener propertyEditorListener;

    private LinearLayoutManager layoutManager;

    private List<MemberEntry>     membersInternal = new ArrayList<>();
    private RelationMemberAdapter adapter;
    private RecyclerView          membersVerticalLayout;

    private int notDownloadedNodeRes;
    private int notDownloadedWayRes;
    private int notDownloadedRelationRes;

    enum Connected {
        NOT, UP, DOWN, BOTH, RING_TOP, RING, RING_BOTTOM, CLOSEDWAY, CLOSEDWAY_UP, CLOSEDWAY_DOWN, CLOSEDWAY_BOTH, CLOSEDWAY_RING
    }

    class MemberEntry extends RelationMemberDescription {
        private static final long serialVersionUID = 1L;

        Connected      connected;
        boolean        selected;
        boolean        enabled = true;
        transient Node up      = null;
        transient Node down    = null;

        /**
         * Create a MemberEntry from a RelationMemberDescription
         * 
         * @param rmd the RelationMemberDescription
         */
        public MemberEntry(@NonNull RelationMemberDescription rmd) {
            super(rmd);
        }

        /**
         * If the row element is a Way return an unused end
         * 
         * @return the Node at the unused end or null
         */
        @Nullable
        public Node getUnusedEnd() {
            OsmElement e = getElement();
            if (e instanceof Way) {
                Node first = ((Way) e).getFirstNode();
                Node last = ((Way) e).getLastNode();
                if (up != null && down == null) {
                    return up.equals(first) ? last : first;
                }
                if (up == null && down != null) {
                    return down.equals(first) ? last : first;
                }
            }
            return null;
        }
    }

    /**
     * Create a new RelationMembersFragment instance
     * 
     * @param id the id of the Relation
     * @param members a List of the members
     * @return a new RelationMembersFragment instance
     */
    @NonNull
    public static RelationMembersFragment newInstance(long id, ArrayList<RelationMemberDescription> members) {
        RelationMembersFragment f = new RelationMembersFragment();

        Bundle args = new Bundle();
        args.putLong(ID_KEY, id);
        args.putSerializable(MEMBERS_KEY, members);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        Fragment parent = getParentFragment();
        Util.implementsInterface(parent, PropertyEditorListener.class);
        propertyEditorListener = (PropertyEditorListener) parent;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout relationMembersLayout = (LinearLayout) inflater.inflate(R.layout.members_view, null);
        membersVerticalLayout = (RecyclerView) relationMembersLayout.findViewById(R.id.members_vertical_layout);
        layoutManager = new ScrollingLinearLayoutManager(getActivity(), 10000);
        membersVerticalLayout.setLayoutManager(layoutManager);
        membersVerticalLayout.setSaveEnabled(false);

        // if this is a relation get members
        ArrayList<RelationMemberDescription> members;
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring from saved state");
            id = savedInstanceState.getLong(ID_KEY);
            members = savingHelper.load(getContext(), Long.toString(id) + FILENAME_MEMBERS, true);
            if (members != null) {
                for (RelationMemberDescription rmd : members) {
                    rmd.update();
                }
            }
        } else if (savedMembers != null) {
            Log.d(DEBUG_TAG, "Restoring from instance variable");
            members = savedMembers;
        } else {
            id = getArguments().getLong(ID_KEY);
            members = Util.getSerializeableArrayList(getArguments(), MEMBERS_KEY, RelationMemberDescription.class);
            /*
             * Saving this argument (done by the FragmentManager) will typically exceed the 1MB transaction size limit
             * and cause a android.os.TransactionTooLargeException
             */
            getArguments().remove(MEMBERS_KEY);
            /*
             * Save to file for undo
             */
            savingHelper.save(getContext(), Long.toString(id) + FILENAME_ORIG_MEMBERS, members, true);
        }

        if (members != null) {
            getMemberEntries(members, membersInternal);
        }

        notDownloadedNodeRes = ThemeUtils.getResIdFromAttribute(getContext(), R.attr.not_downloaded_node_small);
        notDownloadedWayRes = ThemeUtils.getResIdFromAttribute(getContext(), R.attr.not_downloaded_line_small);
        notDownloadedRelationRes = ThemeUtils.getResIdFromAttribute(getContext(), R.attr.not_downloaded_relation_small);

        setIcons(membersInternal);

        adapter = new RelationMemberAdapter(getContext(), this, inflater, membersInternal, (buttonView, isChecked) -> {
            if (isChecked) {
                memberSelected();
            } else {
                deselectRow();
            }
        }, propertyEditorListener.getCapabilities().getMaxStringLength());
        membersVerticalLayout.setAdapter(adapter);

        CheckBox headerCheckBox = (CheckBox) relationMembersLayout.findViewById(R.id.header_member_selected);
        headerCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectAllRows();
            } else {
                deselectAllRows();
            }
        });

        return relationMembersLayout;
    }

    @Override
    public void onDataUpdate() {
        Log.d(DEBUG_TAG, "onDataUpdate");
        Relation r = (Relation) propertyEditorListener.getElement();
        List<MemberEntry> tempEntries = new ArrayList<>();
        final ArrayList<RelationMemberDescription> currentMembers = PropertyEditorData.getRelationMemberDescriptions(r, new ArrayList<>());
        getMemberEntries(currentMembers, tempEntries);
        setIcons(tempEntries);
        if (!tempEntries.equals(membersInternal)) {
            Log.d(DEBUG_TAG, "onDataUpdate current members have changed");
            ScreenMessage.toastTopInfo(getContext(), R.string.toast_updating_members);
            membersInternal.clear();
            membersInternal.addAll(tempEntries);
            adapter.notifyDataSetChanged();
            savingHelper.save(getContext(), Long.toString(id) + FILENAME_ORIG_MEMBERS, currentMembers, true);
        }
    }

    /**
     * Loop over the the members and set the connection icon
     */
    void setIcons() {
        setIcons(membersInternal);
    }

    /**
     * Loop over the the members and set the connection icon
     *
     * @param entries a List of MemberEntry
     */
    void setIcons(@NonNull List<MemberEntry> entries) {
        int s = entries.size();
        Connected[] status = new Connected[s];
        int ringStart = 0;
        for (int i = 0; i < s; i++) {
            MemberEntry row = entries.get(i);
            if (!row.downloaded()) {
                status[i] = Connected.NOT;
                ringStart = i + 1; // next element
                continue;
            }
            int pos = entries.indexOf(row);
            MemberEntry prev = null;
            MemberEntry next = null;

            prev = pos - 1 >= 0 ? entries.get(pos - 1) : null;
            next = pos + 1 < s ? entries.get(pos + 1) : null;

            MemberEntry current = row;
            status[i] = getConnection(prev, current, next);

            // check for ring
            if ((status[i] == Connected.UP || status[i] == Connected.CLOSEDWAY_UP) && i != ringStart) {
                MemberEntry ringStartMember = entries.get(ringStart);
                if (current.getUnusedEnd() != null && ringStartMember.getUnusedEnd() != null && current.getUnusedEnd().equals(ringStartMember.getUnusedEnd())) {
                    status[ringStart] = Connected.RING_TOP;
                    status[i] = Connected.RING_BOTTOM;
                    for (int j = ringStart + 1; j < i; j++) {
                        if (status[j] == Connected.CLOSEDWAY_BOTH) {
                            status[j] = Connected.CLOSEDWAY_RING;
                        } else {
                            status[j] = Connected.RING;
                        }
                    }
                }
                ringStart = i + 1; // next element
            } else if (status[i] == Connected.NOT || status[i] == Connected.CLOSEDWAY) {
                ringStart = i + 1; // next element
            }
        }
        // actually set the connection status
        for (int i = 0; i < s; i++) {
            entries.get(i).connected = status[i];
        }
    }

    /**
     * Determine how the current member is connected to the previous and following one
     * 
     * @param previousRow the previous row
     * @param currentRow the current row
     * @param nextRow the next row
     * @return a Connected value describing the connection
     */
    private Connected getConnection(@Nullable MemberEntry previousRow, @NonNull MemberEntry currentRow, @Nullable MemberEntry nextRow) {
        Connected result = Connected.NOT;
        RelationMemberDescription previous = previousRow;
        RelationMemberDescription current = currentRow;
        RelationMemberDescription next = nextRow;
        synchronized (current) {
            String currentType = current.getType();
            if (current.getElement() == null) {
                // FIXME this seems to happen on restore in not quite clear circumstances
                Log.e(DEBUG_TAG, "Element not downloaded for " + current.getDescription());
                current.update();
                return result;
            }
            if (Way.NAME.equals(currentType)) {
                Way w = (Way) current.getElement();
                currentRow.up = null;
                currentRow.down = null;
                if (w.isClosed()) {
                    result = Connected.CLOSEDWAY;
                    if (previous != null) {
                        synchronized (previous) {
                            if (previous.downloaded()) {
                                if (Way.NAME.equals(previous.getType())) {
                                    if (previousRow.down != null) {
                                        result = Connected.CLOSEDWAY_UP;
                                        currentRow.up = previousRow.down;
                                    }
                                } else if (Node.NAME.equals(previous.getType())) {
                                    Node prevNode = (Node) previous.getElement();
                                    if (w.hasNode(prevNode)) {
                                        result = Connected.CLOSEDWAY_UP;
                                        currentRow.up = prevNode;
                                    }
                                } else {
                                    // FIXME previous is a relation and we could in principle check if we can connect to
                                    // it
                                }
                            }
                        }
                    }
                    if (next != null) {
                        synchronized (next) {
                            if (next.downloaded()) {
                                OsmElement nextElement = next.getElement();
                                if (Way.NAME.equals(next.getType())) {
                                    Way nextWay = (Way) nextElement;
                                    Node nextFirst = nextWay.getFirstNode();
                                    Node nextLast = nextWay.getLastNode();
                                    if (w.hasNode(nextLast) || w.hasNode(nextFirst)) {
                                        if (result == Connected.CLOSEDWAY_UP) {
                                            result = Connected.CLOSEDWAY_BOTH;
                                        } else {
                                            result = Connected.CLOSEDWAY_DOWN;
                                        }
                                        currentRow.down = w.hasNode(nextLast) ? nextLast : nextFirst;
                                    }
                                } else if (Node.NAME.equals(next.getType())) {
                                    Node nextNode = (Node) nextElement;
                                    if (w.hasNode(nextNode)) {
                                        if (result == Connected.CLOSEDWAY_UP) {
                                            result = Connected.CLOSEDWAY_BOTH;
                                        } else {
                                            result = Connected.CLOSEDWAY_DOWN;
                                        }
                                        currentRow.down = nextNode;
                                    }
                                } else {
                                    // FIXME next is a relation and we could in principle check if we can connect to it
                                }
                            }
                        }
                    }
                } else {
                    Node notused = null;
                    Node first = w.getFirstNode();
                    Node last = w.getLastNode();
                    if (previous != null) {
                        synchronized (previous) {
                            if (previous.downloaded()) {
                                if (Way.NAME.equals(previous.getType())) {
                                    if (previousRow.down != null) {
                                        currentRow.up = previousRow.down;
                                        if (currentRow.up.equals(first)) {
                                            notused = last;
                                        } else {
                                            notused = first;
                                        }
                                        result = Connected.UP;
                                    }
                                } else if (Node.NAME.equals(previous.getType())) {
                                    Node prevNode = (Node) previous.getElement();
                                    if (prevNode.equals(first)) {
                                        notused = last;
                                        result = Connected.UP;
                                        currentRow.up = first;
                                    } else if (prevNode.equals(last)) {
                                        notused = first;
                                        result = Connected.UP;
                                        currentRow.up = last;
                                    }
                                } else {
                                    // FIXME previous is a relation and we could in principle check if we can connect to
                                    // it
                                }
                            }
                        }
                    }
                    if (next != null) {
                        synchronized (next) {
                            if (next.downloaded()) {
                                OsmElement nextElement = next.getElement();
                                if (Way.NAME.equals(next.getType())) {
                                    Way nextWay = (Way) nextElement;
                                    if (nextWay.isClosed()) {
                                        if (notused == null && (nextWay.hasNode(first) || nextWay.hasNode(last))) {
                                            result = Connected.DOWN;
                                            currentRow.down = nextWay.hasNode(first) ? first : last;
                                        } else if (notused != null && nextWay.hasNode(notused)) {
                                            result = Connected.BOTH;
                                            currentRow.down = notused;
                                        }
                                    } else {
                                        Node nextFirst = nextWay.getFirstNode();
                                        Node nextLast = nextWay.getLastNode();
                                        if (notused == null
                                                && (nextLast.equals(first) || nextFirst.equals(first) || nextLast.equals(last) || nextFirst.equals(last))) {
                                            result = Connected.DOWN;
                                            currentRow.down = nextLast.equals(first) || nextFirst.equals(first) ? first : last;
                                        } else if (notused != null && (nextLast.equals(notused) || nextFirst.equals(notused))) {
                                            result = Connected.BOTH;
                                            currentRow.down = notused;
                                        }
                                    }
                                } else if (Node.NAME.equals(next.getType())) {
                                    Node nextNode = (Node) nextElement;
                                    boolean firstNodeMatches = nextNode.equals(first);
                                    if (notused == null && (firstNodeMatches || nextNode.equals(last))) {
                                        result = Connected.DOWN;
                                        currentRow.down = firstNodeMatches ? first : last;
                                    } else if (notused != null && nextNode.equals(notused)) {
                                        result = Connected.BOTH;
                                        currentRow.down = notused;
                                    }
                                } else {
                                    // FIXME next is a relation and we could in principle check if we can connect to it
                                }
                            }
                        }
                    }
                }
            } else if (Node.NAME.equals(currentType)) {
                Node n = (Node) current.getElement();
                if (previous != null) {
                    synchronized (previous) {
                        if (Way.NAME.equals(previous.getType()) && previous.downloaded()) {
                            Way way = (Way) previous.getElement();
                            if (way.getLastNode().equals(n) || way.getFirstNode().equals(n)) {
                                result = Connected.UP;
                            }
                        }
                    }
                }
                if (next != null) {
                    synchronized (next) {
                        if (Way.NAME.equals(next.getType()) && next.downloaded()) {
                            Way way = (Way) next.getElement();
                            if (way.getLastNode().equals(n) || way.getFirstNode().equals(n)) {
                                if (result == Connected.UP) {
                                    result = Connected.BOTH;
                                } else {
                                    result = Connected.DOWN;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        savedMembers = getMembersList();
        outState.putLong(ID_KEY, id);
        savingHelper.save(getContext(), Long.toString(id) + FILENAME_MEMBERS, savedMembers, true);
        Log.d(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    /**
     * A row representing an editable member of a relation, consisting of edits for role and display of other values and
     * a delete button.
     */
    public static class RelationMemberRow extends LinearLayout {

        private RelationMembersFragment owner;
        private CheckBox                selected;
        private AutoCompleteTextView    roleEdit;
        private ImageView               typeView;
        TextView                        elementView;
        private TextWatcher             watcher;

        private RelationMemberDescription rmd;

        /**
         * Construct a new row
         * 
         * @param context Android Context
         */
        public RelationMemberRow(@NonNull Context context) {
            super(context);
        }

        /**
         * Construct a new row
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public RelationMemberRow(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        /**
         * Set the fragment for this view
         * 
         * @param owner the "owning" Fragment
         */
        public void setOwner(@NonNull RelationMembersFragment owner) {
            this.owner = owner;
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            if (isInEditMode()) {
                return; // allow visual editor to work
            }
            selected = (CheckBox) findViewById(R.id.member_selected);

            roleEdit = (AutoCompleteTextView) findViewById(R.id.editMemberRole);
            roleEdit.setOnKeyListener(PropertyEditorFragment.myKeyListener);

            typeView = (ImageView) findViewById(R.id.memberType);

            elementView = (TextView) findViewById(R.id.memberObject);

            roleEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    roleEdit.setAdapter(getMemberRoleAutocompleteAdapter());
                    if (roleEdit.getText().length() == 0) {
                        roleEdit.showDropDown();
                    }
                }
            });

            roleEdit.setOnClickListener(v -> {
                if (v.hasFocus()) {
                    ((AutoCompleteTextView) v).showDropDown();
                }
            });

            RelationMembershipFragment.setRoleOnItemClickListener(roleEdit);
        }

        /**
         * Sets the per row values for a relation member
         * 
         * @param rmd the information on the relation member
         * @return RelationMemberRow object for convenience
         */
        public RelationMemberRow setValues(@NonNull RelationMemberDescription rmd) {

            String desc = rmd.getDescription();
            if (watcher != null) {
                roleEdit.removeTextChangedListener(watcher);
            }
            roleEdit.setText(rmd.getRole());
            this.rmd = rmd;
            typeView.setTag(rmd.getType());
            elementView.setText(desc);
            return this;
        }

        /**
         * Set the OnCheckedChangeListener
         * 
         * @param listener the OnCheckedChangeListener
         */
        public void setOnCheckedChangeListener(@Nullable OnCheckedChangeListener listener) {
            selected.setOnCheckedChangeListener(listener);
        }

        /**
         * Get the element type for the row
         * 
         * @return the element type
         */
        public String getType() {
            return (String) typeView.getTag();
        }

        /**
         * Set the connection icon
         * 
         * @param ctx an Android Context
         * @param rmd the RelationMemberDescription
         * @param c how the element is connected
         */
        public void setIcon(@NonNull Context ctx, RelationMemberDescription rmd, @NonNull Connected c) {
            String objectType = rmd.getType();
            int iconId = 0;
            if (rmd.downloaded()) {
                if (Node.NAME.equals(objectType)) {
                    switch (c) {
                    case UP:
                        iconId = R.attr.node_up;
                        break;
                    case DOWN:
                        iconId = R.attr.node_down;
                        break;
                    case BOTH:
                        iconId = R.attr.node_both;
                        break;
                    default:
                        iconId = R.attr.node_small;
                        break;
                    }
                } else if (Way.NAME.equals(objectType)) {
                    switch (c) {
                    case UP:
                        iconId = R.attr.line_up;
                        break;
                    case DOWN:
                        iconId = R.attr.line_down;
                        break;
                    case BOTH:
                        iconId = R.attr.line_both;
                        break;
                    case RING:
                        iconId = R.attr.ring;
                        break;
                    case RING_TOP:
                        iconId = R.attr.ring_top;
                        break;
                    case RING_BOTTOM:
                        iconId = R.attr.ring_bottom;
                        break;
                    case CLOSEDWAY:
                        iconId = R.attr.closedway;
                        break;
                    case CLOSEDWAY_UP:
                        iconId = R.attr.closedway_up;
                        break;
                    case CLOSEDWAY_DOWN:
                        iconId = R.attr.closedway_down;
                        break;
                    case CLOSEDWAY_BOTH:
                        iconId = R.attr.closedway_both;
                        break;
                    case CLOSEDWAY_RING:
                        iconId = R.attr.closedway_ring;
                        break;
                    default:
                        iconId = R.attr.line_small;
                        break;
                    }
                } else if (Relation.NAME.equals(objectType)) {
                    iconId = R.attr.relation_small;
                } else {
                    // don't know yet
                }
                typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx, iconId));
            } else {
                if (Node.NAME.equals(objectType)) {
                    typeView.setImageResource(owner.notDownloadedNodeRes);
                } else if (Way.NAME.equals(objectType)) {
                    typeView.setImageResource(owner.notDownloadedWayRes);
                } else if (Relation.NAME.equals(objectType)) {
                    typeView.setImageResource(owner.notDownloadedRelationRes);
                } else {
                    // don't know yet
                }
            }
        }

        /**
         * Set a water for the role autocomplete
         * 
         * @param watcher the watcher
         */
        public void setRoleWatcher(@NonNull TextWatcher watcher) {
            this.watcher = watcher;
            roleEdit.addTextChangedListener(watcher);
        }

        /**
         * Checks if the fields in this row are empty
         * 
         * @return true if both fields are empty, false if at least one is filled
         */
        public boolean isEmpty() {
            return "".equals(roleEdit.getText().toString().trim());
        }

        // return the status of the checkbox
        @Override
        public boolean isSelected() {
            return selected.isChecked();
        }

        /**
         * Select this row
         */
        public void select() {
            selected.setChecked(true);
        }

        /**
         * De-select this row
         */
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
         * Create an ArrayAdapter containing role values for a certain member Note: this uses the tags of the first
         * element if multiple are selected to determine which preset to use
         * 
         * Note this does not support multi-select
         * 
         * @return an ArrayAdapter
         */
        @NonNull
        ArrayAdapter<PresetRole> getMemberRoleAutocompleteAdapter() {
            List<PresetRole> roles = new ArrayList<>();
            PropertyEditorListener listener = (PropertyEditorListener) owner.getParentFragment();
            List<Map<String, String>> allTags = listener.getUpdatedTags();
            if (allTags != null && !allTags.isEmpty()) {
                PresetItem relationPreset = Preset.findBestMatch(listener.getPresets(), allTags.get(0), null, null);
                if (relationPreset != null) {
                    Map<String, Integer> counter = new HashMap<>();
                    int position = 0;
                    List<String> tempRoles = App.getMruTags().getRoles(relationPreset);
                    if (tempRoles != null) {
                        for (String role : tempRoles) {
                            roles.add(new PresetRole(role, null, rmd.getType()));
                            counter.put(role, position++);
                        }
                    }
                    List<String> regions = owner.propertyEditorListener.getIsoCodes();
                    List<PresetRole> tempPresetRoles = rmd.downloaded() ? relationPreset.getRoles(getContext(), rmd.getElement(), null, regions)
                            : relationPreset.getRoles(rmd.getType(), regions);
                    if (tempPresetRoles != null) {
                        RelationMembershipRow.countAndAddRoles(tempPresetRoles, counter, roles);
                    }
                }
            }
            return new ArrayAdapter<>(getContext(), R.layout.autocomplete_row, roles);
        }
    }

    @Override
    protected SelectedRowsActionModeCallback getActionModeCallback() {
        return new RelationMemberSelectedActionModeCallback(this, adapter, membersInternal);
    }

    /**
     * Start the ActionMode for when an element is selected
     */
    private void memberSelected() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback == null) {
                actionModeCallback = getActionModeCallback();
                ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
            }
            actionModeCallback.invalidate();
        }
    }

    @Override
    public void deselectRow() {
        synchronized (actionModeCallbackLock) {
            if (actionModeCallback != null) {
                if (actionModeCallback.rowsDeselected(true)) {
                    actionModeCallback = null;
                } else {
                    actionModeCallback.invalidate();
                }
            }
        }
    }

    @Override
    public void selectAllRows() { // selects all members
        final View rowLayout = getOurView();
        rowLayout.post( // as there can be a very large number of rows don't do it here
                () -> {
                    for (MemberEntry m : membersInternal) {
                        if (m.enabled) {
                            m.selected = true;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    memberSelected();
                });
    }

    @Override
    public void deselectAllRows() { // deselects all members
        final View rowLayout = getOurView();
        rowLayout.post( // as there can be a very large number of rows don't do it here
                () -> {
                    for (MemberEntry m : membersInternal) {
                        if (m.enabled) {
                            m.selected = false;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    deselectRow();
                });
    }

    /**
     */
    private interface RelationMemberHandler {
        /**
         * Process the contents of a RelationMemberRow
         * 
         * @param row the RelationMemberRow
         */
        void handleRelationMember(final MemberEntry row);
    }

    /**
     * Perform some processing for each row in the relation members view.
     * 
     * @param handler The handler that will be called for each rowr.
     */

    private void processRelationMembers(final RelationMemberHandler handler) {
        for (MemberEntry entry : membersInternal) {
            handler.handleRelationMember(entry);
        }
    }

    /**
     * Collect all interesting values from the relation member view RelationMemberDescritption is an extended version of
     * RelationMember that holds a textual description of the element instead of the element itself
     * 
     * Updating the MRU role is tricky as we want to avoid going through the list of members multiple times, the
     * solution is not exact due to this
     * 
     * @return ArrayList&lt;RelationMemberDescription&gt;.
     */
    ArrayList<RelationMemberDescription> getMembersList() {
        final ArrayList<RelationMemberDescription> members = new ArrayList<>();

        final Relation r = (Relation) propertyEditorListener.getElement();
        final Preset[] presets = propertyEditorListener.getPresets();
        final List<Map<String, String>> allTags = propertyEditorListener.getUpdatedTags();
        final PresetItem presetItem = allTags != null && !allTags.isEmpty() ? Preset.findBestMatch(presets, allTags.get(0), null, null) : null;

        final MultiHashMap<String, String> originalMembersRoles = new MultiHashMap<>(false);
        List<RelationMember> originalMembers = r.getMembers();
        if (originalMembers != null) {
            for (RelationMember rm : originalMembers) {
                if (!"".equals(rm.getRole())) {
                    originalMembersRoles.add(rm.getType() + rm.getRef(), rm.getRole());
                }
            }
        }

        processRelationMembers(row -> {
            RelationMemberDescription rmd = new RelationMemberDescription(row);
            members.add(rmd);
            Set<String> originalRoles = originalMembersRoles.get(row.getType() + row.getRef());
            String role = row.getRole();
            if (!"".equals(role) && !originalRoles.contains(role)) {
                // only add if the role wasn't in use before
                if (presetItem != null) {
                    App.getMruTags().putRole(presetItem, role);
                } else {
                    App.getMruTags().putRole(role);
                }
            }
        });
        return members;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.members_menu, menu);
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
        case R.id.tag_menu_top:
        case R.id.tag_menu_bottom:
            scrollToRow(item.getItemId() == R.id.tag_menu_top ? 0 : membersInternal.size() - 1);
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
     * reload original member list
     */
    void doRevert() {
        List<RelationMemberDescription> members = savingHelper.load(getContext(), Long.toString(id) + FILENAME_ORIG_MEMBERS, true);
        if (members != null) {
            membersInternal.clear();
            getMemberEntries(members, membersInternal);
        }
        setIcons();
        adapter.notifyDataSetChanged();
    }

    /**
     * Get a list of MemberEntry for the adapter
     * 
     * @param members the List of RelationMemberDescription
     * @param entries the List of MemberEntry
     */
    private void getMemberEntries(@NonNull List<RelationMemberDescription> members, @NonNull List<MemberEntry> entries) {
        int pos = 0;
        for (RelationMemberDescription rmd : members) {
            rmd.setPosition(pos);
            entries.add(new MemberEntry(rmd));
            pos++;
        }
    }

    @Override
    public void deselectHeaderCheckBox() {
        CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_member_selected);
        headerCheckBox.setChecked(false);
    }

    /**
     * Scroll the current View so that a row is visible
     * 
     * @param position the position we want to have in view
     */
    public void scrollToRow(int position) {
        membersVerticalLayout.post(() -> layoutManager.smoothScrollToPosition(membersVerticalLayout, null, position));
    }

    /**
     * Get the id of the element we are editing
     * 
     * @return the OSM id
     */
    long getOsmId() {
        return id;
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
            if (v.getId() == R.id.members_vertical_layout) {
                Log.d(DEBUG_TAG, "got correct view in getView");
                return v;
            } else {
                v = v.findViewById(R.id.members_vertical_layout);
                if (v == null) {
                    Log.d(DEBUG_TAG, "didn't find R.id.members_vertical_layout");
                    throw new UiStateException("didn't find R.id.members_vertical_layout");
                } else {
                    Log.d(DEBUG_TAG, "Found members_vertical_layout");
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
