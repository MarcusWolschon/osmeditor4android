package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.exception.UiStateException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberDescription;
import de.blau.android.osm.Server;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.PresetRole;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.util.collections.MultiHashMap;

/**
 * UI for managing relation members
 * 
 * @author Simon Poole
 *
 */
public class RelationMembersFragment extends BaseFragment implements PropertyRows {

    private static final String MEMBERS_KEY = "members";

    private static final String ID_KEY = "id";

    private static final String DEBUG_TAG = RelationMembersFragment.class.getSimpleName();

    private LayoutInflater inflater = null;

    private ArrayList<RelationMemberDescription>                         savedMembers          = null;
    private long                                                         id                    = -1;
    private transient SavingHelper<ArrayList<RelationMemberDescription>> savingHelper          = new SavingHelper<>();
    public static final String                                           FILENAME_MEMBERS      = "members.res";
    public static final String                                           FILENAME_ORIG_MEMBERS = "orig_members.res";

    private int maxStringLength; // maximum key, value and role length

    private PropertyEditorListener propertyEditorListener;

    private static SelectedRowsActionModeCallback memberSelectedActionModeCallback = null;
    private static final Object                   actionModeCallbackLock           = new Object();

    enum Connected {
        NOT, UP, DOWN, BOTH, RING_TOP, RING, RING_BOTTOM, CLOSEDWAY, CLOSEDWAY_UP, CLOSEDWAY_DOWN, CLOSEDWAY_BOTH, CLOSEDWAY_RING
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
        try {
            propertyEditorListener = (PropertyEditorListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement PropertyEditorListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @SuppressWarnings("unchecked")
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        this.inflater = inflater;
        ScrollView relationMembersLayout = (ScrollView) inflater.inflate(R.layout.members_view, null);
        LinearLayout membersVerticalLayout = (LinearLayout) relationMembersLayout.findViewById(R.id.members_vertical_layout);
        // membersVerticalLayout.setSaveFromParentEnabled(false);
        membersVerticalLayout.setSaveEnabled(false);

        // if this is a relation get members
        ArrayList<RelationMemberDescription> members;
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "Restoring from saved state");
            id = savedInstanceState.getLong(ID_KEY);
            members = savingHelper.load(getContext(), FILENAME_MEMBERS, true);
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
            members = (ArrayList<RelationMemberDescription>) getArguments().getSerializable(MEMBERS_KEY);
            /*
             * Saving this argument (done by the FragmentManager) will typically exceed the 1MB transaction size limit
             * and cause a android.os.TransactionTooLargeException
             */
            getArguments().remove(MEMBERS_KEY);
            /*
             * Save to file for undo
             */
            savingHelper.save(getContext(), FILENAME_ORIG_MEMBERS, members, true);
        }

        Preferences prefs = App.getLogic().getPrefs();
        Server server = prefs.getServer();
        maxStringLength = server.getCachedCapabilities().getMaxStringLength();

        loadMembers(membersVerticalLayout, members);
        setIcons(membersVerticalLayout);
        CheckBox headerCheckBox = (CheckBox) relationMembersLayout.findViewById(R.id.header_member_selected);
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

        return relationMembersLayout;
    }

    /**
     * Creates rows from a List of RelationMemberDescriptions
     * 
     * @param members the list of members
     */
    private void loadMembers(@Nullable final ArrayList<RelationMemberDescription> members) {
        LinearLayout membersVerticalLayout = (LinearLayout) getOurView();
        loadMembers(membersVerticalLayout, members);
    }

    /**
     * Creates rows from a List of RelationMemberDescriptions
     * 
     * @param membersVerticalLayout the layout holding the rows
     * @param members the list of members
     */
    private void loadMembers(@NonNull LinearLayout membersVerticalLayout, @Nullable final ArrayList<RelationMemberDescription> members) {
        membersVerticalLayout.removeAllViews();
        if (members != null && !members.isEmpty()) {
            for (int i = 0; i < members.size(); i++) {
                RelationMemberDescription current = members.get(i);
                insertNewMember(membersVerticalLayout, Integer.toString(i), current, -1, Connected.NOT, false);
            }
        }
    }

    /**
     * Loop over the the members and set the connection icon
     */
    void setIcons() {
        setIcons((LinearLayout) getOurView());
    }

    /**
     * Loop over the the members and set the connection icon
     *
     * @param rowLayout the layout holding the rows
     */
    void setIcons(@NonNull LinearLayout rowLayout) {
        int s = rowLayout.getChildCount();
        Connected[] status = new Connected[s];
        int ringStart = 0;
        for (int i = 0; i < s; i++) {
            RelationMemberRow row = (RelationMemberRow) rowLayout.getChildAt(i);
            if (!row.getRelationMemberDescription().downloaded()) {
                status[i] = Connected.NOT;
                ringStart = i + 1; // next element
                continue;
            }
            int pos = rowLayout.indexOfChild(row);
            RelationMemberRow prev = null;
            RelationMemberRow next = null;

            prev = pos - 1 >= 0 ? ((RelationMemberRow) rowLayout.getChildAt(pos - 1)) : null;
            next = pos + 1 < s ? ((RelationMemberRow) rowLayout.getChildAt(pos + 1)) : null;

            RelationMemberRow current = row;
            status[i] = getConnection(prev, current, next);

            // check for ring
            if ((status[i] == Connected.UP || status[i] == Connected.CLOSEDWAY_UP) && i != ringStart) {
                RelationMemberRow ringStartMember = ((RelationMemberRow) rowLayout.getChildAt(ringStart));
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
        // actually set the icons
        for (int i = 0; i < s; i++) {
            RelationMemberRow row = (RelationMemberRow) rowLayout.getChildAt(i);
            row.setIcon(getActivity(), row.getRelationMemberDescription(), status[i]);
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
    private Connected getConnection(@Nullable RelationMemberRow previousRow, @NonNull RelationMemberRow currentRow, @Nullable RelationMemberRow nextRow) {
        Connected result = Connected.NOT;
        RelationMemberDescription previous = previousRow != null ? previousRow.getRelationMemberDescription() : null;
        RelationMemberDescription current = currentRow.getRelationMemberDescription();
        RelationMemberDescription next = nextRow != null ? nextRow.getRelationMemberDescription() : null;
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
                                        } else if (nextWay.hasNode(notused)) {
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
                                        } else if (nextLast.equals(notused) || nextFirst.equals(notused)) {
                                            result = Connected.BOTH;
                                            currentRow.down = notused;
                                        }
                                    }
                                } else if (Node.NAME.equals(next.getType())) {
                                    Node nextNode = (Node) nextElement;
                                    if (notused == null && (nextNode.equals(first) || nextNode.equals(last))) {
                                        result = Connected.DOWN;
                                        currentRow.down = nextNode.equals(first) ? first : last;
                                    } else if (nextNode.equals(notused)) {
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
                            if (((Way) previous.getElement()).getLastNode().equals(n) || ((Way) previous.getElement()).getFirstNode().equals(n)) {
                                result = Connected.UP;
                            }
                        }
                    }
                }
                if (next != null) {
                    synchronized (next) {
                        if (Way.NAME.equals(next.getType()) && next.downloaded()) {
                            if (((Way) next.getElement()).getLastNode().equals(n) || ((Way) next.getElement()).getFirstNode().equals(n)) {
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
        savingHelper.save(getContext(), FILENAME_MEMBERS, savedMembers, true);
        Log.w(DEBUG_TAG, "onSaveInstanceState bundle size " + Util.getBundleSize(outState));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(DEBUG_TAG, "onConfigurationChanged");
        synchronized (actionModeCallbackLock) {
            if (memberSelectedActionModeCallback != null) {
                memberSelectedActionModeCallback.currentAction.finish();
                memberSelectedActionModeCallback = null;
            }
        }
    }

    /**
     * Insert a new row with a relation member
     * 
     * @param membersVerticalLayout the Layout holding the rowa
     * @param pos (currently unused)
     * @param rmd information on the relation member
     * @param position the position where this should be inserted. set to -1 to insert at end, or 0 to insert at
     *            beginning.
     * @param c connection type
     * @param select if true select this row
     * @return The new RelationMemberRow.
     */
    RelationMemberRow insertNewMember(final LinearLayout membersVerticalLayout, final String pos, final RelationMemberDescription rmd, final int position,
            final Connected c, boolean select) {
        RelationMemberRow row = null;

        if (rmd.downloaded()) {
            row = (RelationMemberRow) inflater.inflate(R.layout.relation_member_downloaded_row, membersVerticalLayout, false);
        } else {
            row = (RelationMemberRow) inflater.inflate(R.layout.relation_member_row, membersVerticalLayout, false);
        }

        row.setValues(getActivity(), pos, rmd, c);

        // need to do this before the listener is set
        if (select) {
            row.select();
        }

        row.roleEdit.addTextChangedListener(new SanitizeTextWatcher(getActivity(), maxStringLength));

        membersVerticalLayout.addView(row, (position == -1) ? membersVerticalLayout.getChildCount() : position);

        row.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    memberSelected(membersVerticalLayout);
                } else {
                    deselectRow();
                }
            }
        });

        return row;
    }

    /**
     * A row representing an editable member of a relation, consisting of edits for role and display of other values and
     * a delete button.
     */
    public static class RelationMemberRow extends LinearLayout implements SelectedRowsActionModeCallback.Row {

        private PropertyEditor       owner;
        private CheckBox             selected;
        private AutoCompleteTextView roleEdit;
        private ImageView            typeView;
        private TextView             elementView;

        /**
         * used for storing which end of a way was used for what
         */
        transient Node up   = null;
        transient Node down = null;

        private RelationMemberDescription rmd;

        /**
         * Construct a new row
         * 
         * @param context Android Context
         */
        public RelationMemberRow(@NonNull Context context) {
            super(context);
            owner = (PropertyEditor) (isInEditMode() ? null : context); // Can only be instantiated inside TagEditor or
                                                                        // in Eclipse
        }

        /**
         * Construct a new row
         * 
         * @param context Android Context
         * @param attrs an AttributeSet
         */
        public RelationMemberRow(@NonNull Context context, @Nullable AttributeSet attrs) {
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
            selected = (CheckBox) findViewById(R.id.member_selected);

            roleEdit = (AutoCompleteTextView) findViewById(R.id.editMemberRole);
            roleEdit.setOnKeyListener(PropertyEditor.myKeyListener);
            // lastEditKey.setSingleLine(true);

            typeView = (ImageView) findViewById(R.id.memberType);

            elementView = (TextView) findViewById(R.id.memberObject);

            roleEdit.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        roleEdit.setAdapter(getMemberRoleAutocompleteAdapter());
                        if (/* running && */ roleEdit.getText().length() == 0) {
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
                    Log.d(DEBUG_TAG, "onItemClicked role");
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
         * Sets the per row values for a relation member
         * 
         * @param ctx Android Context (not used)
         * @param pos position (not used)
         * @param rmd the information on the relation member
         * @param c Connected status (not used)
         * @return RelationMemberRow object for convenience
         */
        public RelationMemberRow setValues(Context ctx, String pos, RelationMemberDescription rmd, Connected c) {

            String desc = rmd.getDescription();
            this.rmd = rmd;
            roleEdit.setText(rmd.getRole());

            // setIcon(ctx, rmd, c);
            typeView.setTag(rmd.getType());
            elementView.setText(desc);
            return this;
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
         * Get the RelationMemberDescription for the row
         * 
         * @return a RelationMemberDescription
         */
        public RelationMemberDescription getRelationMemberDescription() {
            return rmd;
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
                    typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx, R.attr.not_downloaded_node_small));
                } else if (Way.NAME.equals(objectType)) {
                    typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx, R.attr.not_downloaded_line_small));
                } else if (Relation.NAME.equals(objectType)) {
                    typeView.setImageResource(ThemeUtils.getResIdFromAttribute(ctx, R.attr.not_downloaded_relation_small));
                } else {
                    // don't know yet
                }
            }
        }

        /**
         * If the row element is a Way return an unused end
         * 
         * @return the Ndoe at the unused end or null
         */
        @Nullable
        public Node getUnusedEnd() {
            OsmElement e = rmd.getElement();
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

        /**
         * Get the id of the element this row is for
         * 
         * @return the OSM id
         */
        public long getOsmId() {
            return rmd.getRef();
        }

        /**
         * Get the role for this row
         * 
         * @return the role as a String
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
                    // owner.focusRow(0); // FIXME focus is on this row
                }
                LinearLayout membersVerticalLayout = (LinearLayout) owner.relationMembersFragment.getOurView();
                membersVerticalLayout.removeView(this);
                membersVerticalLayout.invalidate();
            } else {
                Log.d("PropertyEditor", "deleteRow owner null");
            }
        }

        /**
         * Checks if the fields in this row are empty
         * 
         * @return true if both fields are empty, false if at least one is filled
         */
        public boolean isEmpty() {
            return roleEdit.getText().toString().trim().equals("");
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
         * Create an ArrayAdapter containing role values for a certain member Note: this uses the tags of the first
         * element if multiple are selected to determine which preset to use
         * 
         * @return an ArrayAdapter
         */
        @NonNull
        ArrayAdapter<PresetRole> getMemberRoleAutocompleteAdapter() { // FIXME for multiselect
            List<PresetRole> roles = new ArrayList<>();
            List<LinkedHashMap<String, String>> allTags = owner.getUpdatedTags();
            if (allTags != null && !allTags.isEmpty()) {
                if (owner.presets != null) { //
                    PresetItem relationPreset = Preset.findBestMatch(owner.presets, allTags.get(0));
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
                        List<PresetRole> tempPresetRoles = rmd.downloaded() ? relationPreset.getRoles(getContext(), rmd.getElement(), null)
                                : relationPreset.getRoles(rmd.getType());
                        if (tempPresetRoles != null) {
                            Collections.sort(tempPresetRoles);
                            for (PresetRole presetRole : tempPresetRoles) {
                                Integer counterPos = counter.get(presetRole.getRole());
                                if (counterPos != null) {
                                    roles.get(counterPos).setHint(presetRole.getHint());
                                    continue;
                                }
                                roles.add(presetRole);
                            }
                        }
                    }
                }
            }
            return new ArrayAdapter<>(owner, R.layout.autocomplete_row, roles);
        }
    }

    /**
     * Start the ActionMode for when an element is selected
     * 
     * @param rowLayout the Layout holding the element rows
     */
    private void memberSelected(@NonNull LinearLayout rowLayout) {
        synchronized (actionModeCallbackLock) {
            if (memberSelectedActionModeCallback == null) {
                memberSelectedActionModeCallback = new RelationMemberSelectedActionModeCallback(this, rowLayout);
                ((AppCompatActivity) getActivity()).startSupportActionMode(memberSelectedActionModeCallback);
            }
            memberSelectedActionModeCallback.invalidate();
        }
    }

    @Override
    public void deselectRow() {
        synchronized (actionModeCallbackLock) {
            if (memberSelectedActionModeCallback != null) {
                if (memberSelectedActionModeCallback.rowsDeselected(true)) {
                    memberSelectedActionModeCallback = null;
                } else {
                    memberSelectedActionModeCallback.invalidate();
                }
            }
        }
    }

    @Override
    public void selectAllRows() { // selects all members
        final LinearLayout rowLayout = (LinearLayout) getOurView();
        rowLayout.post(new Runnable() { // as there can be a very large number of rows don't do it here
            @Override
            public void run() {
                int i = rowLayout.getChildCount();
                while (--i >= 0) {
                    RelationMemberRow row = (RelationMemberRow) rowLayout.getChildAt(i);
                    if (row.selected.isEnabled()) {
                        row.selected.setChecked(true);
                    }
                }
            }
        });
    }

    @Override
    public void deselectAllRows() { // deselects all members
        final LinearLayout rowLayout = (LinearLayout) getOurView();
        rowLayout.post(new Runnable() { // as there can be a very large number of rows don't do it here
            @Override
            public void run() {
                int i = rowLayout.getChildCount();
                while (--i >= 0) {
                    RelationMemberRow row = (RelationMemberRow) rowLayout.getChildAt(i);
                    if (row.selected.isEnabled()) {
                        row.selected.setChecked(false);
                    }
                }
            }
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
        void handleRelationMember(final RelationMemberRow row);
    }

    /**
     * Perform some processing for each row in the relation members view.
     * 
     * @param handler The handler that will be called for each rowr.
     */

    private void processRelationMembers(final RelationMemberHandler handler) {
        LinearLayout relationMembersLayout = (LinearLayout) getOurView();
        final int size = relationMembersLayout.getChildCount();
        for (int i = 0; i < size; ++i) { // -> avoid header
            View view = relationMembersLayout.getChildAt(i);
            RelationMemberRow row = (RelationMemberRow) view;
            handler.handleRelationMember(row);
        }
    }

    /**
     * Collect all interesting values from the relation member view RelationMemberDescritption is an extended version of
     * RelationMember that holds a textual description of the element instead of the element itself
     * 
     * Updating the MEU role is tricky as we want to avoid going through the list of members multiple times, the
     * solution is not exact due to this
     * 
     * @return ArrayList&lt;RelationMemberDescription&gt;.
     */
    ArrayList<RelationMemberDescription> getMembersList() {
        final ArrayList<RelationMemberDescription> members = new ArrayList<>();

        final Relation r = (Relation) propertyEditorListener.getElement();
        final Preset[] presets = propertyEditorListener.getPresets();
        final List<LinkedHashMap<String, String>> allTags = propertyEditorListener.getUpdatedTags();
        final PresetItem presetItem = presets != null && allTags != null && !allTags.isEmpty() ? Preset.findBestMatch(presets, allTags.get(0)) : null;

        final MultiHashMap<String, String> originalMembesRoles = new MultiHashMap<>(false);
        if (r != null) {
            List<RelationMember> originalMembers = r.getMembers();
            if (originalMembers != null) {
                for (RelationMember rm : originalMembers) {
                    if (!"".equals(rm.getRole())) {
                        originalMembesRoles.add(rm.getType() + rm.getRef(), rm.getRole());
                    }
                }
            }
        }

        processRelationMembers(new RelationMemberHandler() {
            @Override
            public void handleRelationMember(final RelationMemberRow row) {
                String type = ((String) row.typeView.getTag()).trim();
                String role = row.roleEdit.getText().toString().trim();
                String desc = row.elementView.getText().toString().trim();
                RelationMemberDescription rmd = new RelationMemberDescription(type, row.rmd.getRef(), role, desc);
                members.add(rmd);

                Set<String> originalRoles = originalMembesRoles.get(type + row.rmd.getRef());
                if (originalRoles != null) {
                    if (!"".equals(role) && !originalRoles.contains(role)) {
                        // only add if the role wasn't in use before
                        if (presetItem != null) {
                            App.getMruTags().putRole(presetItem, role);
                        } else {
                            App.getMruTags().putRole(role);
                        }
                    }
                }
            }
        });
        return members;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        // final MenuInflater inflater = getSupportMenuInflater();
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.members_menu, menu);
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
        case R.id.tag_menu_top:
        case R.id.tag_menu_bottom:
            scrollToRow(null, item.getItemId() == R.id.tag_menu_top, false);
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
        ArrayList<RelationMemberDescription> members = savingHelper.load(getContext(), FILENAME_ORIG_MEMBERS, true);
        if (members != null) {
            for (RelationMemberDescription rmd : members) {
                rmd.update();
            }
        }
        loadMembers(members);
        setIcons();
    }

    @Override
    public void deselectHeaderCheckBox() {
        CheckBox headerCheckBox = (CheckBox) getView().findViewById(R.id.header_member_selected);
        headerCheckBox.setChecked(false);
    }

    /**
     * Scroll the current View so that a row is visible
     * 
     * @param row the row to display, if null scroll to top or bottom of sv
     * @param up if true scroll to top if row is null, otherwise scroll to bottom
     * @param force if true always try to scroll even if row is already on screen
     */
    public void scrollToRow(@Nullable final View row, final boolean up, boolean force) {
        Util.scrollToRow(getView(), row, up, force);
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
