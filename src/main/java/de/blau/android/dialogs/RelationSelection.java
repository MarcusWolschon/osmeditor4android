package de.blau.android.dialogs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.RelationMember;
import de.blau.android.osm.RelationMemberPosition;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetRole;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.Density;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.util.collections.MultiHashMap;

/**
 * Layer dialog
 * 
 * @author Simon Poole
 *
 */
public class RelationSelection extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = RelationSelection.class.getName();

    private static final String TAG = "fragment_relation_selection";

    private static final String NODE_IDS_KEY     = "nodeIds";
    private static final String WAY_IDS_KEY      = "wayIds";
    private static final String RELATION_IDS_KEY = "relationIds";
    private static final String LISTENER_KEY     = "listener";

    TableLayout tl;

    private MultiHashMap<Long, RelationMemberPosition> memberships;
    private OnRelationsSelectedListener                listener;

    private long[] nodeIds;
    private long[] wayIds;
    private long[] relationIds;

    public static void showDialog(@NonNull FragmentActivity activity, @NonNull OsmElement element, @NonNull OnRelationsSelectedListener listener) {
        List<OsmElement> elements = de.blau.android.util.Util.wrapInList(element);
        showDialog(activity, elements, listener);
    }

    /**
     * Show an info dialog for the supplied GeoJSON Feature
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull List<OsmElement> elements, @NonNull OnRelationsSelectedListener listener) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            RelationSelection relationSelectionFragment = newInstance(elements, listener);
            relationSelectionFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of the Layers dialog
     * 
     * @return an instance of the Layers dialog
     */
    @NonNull
    private static RelationSelection newInstance(@NonNull List<OsmElement> elements, @NonNull OnRelationsSelectedListener listener) {
        RelationSelection f = new RelationSelection();

        Bundle args = new Bundle();

        LongHashSet nodeIds = new LongHashSet();
        LongHashSet wayIds = new LongHashSet();
        LongHashSet relationIds = new LongHashSet();
        for (OsmElement e : elements) {
            switch (e.getName()) {
            case Node.NAME:
                nodeIds.put(e.getOsmId());
                break;
            case Way.NAME:
                wayIds.put(e.getOsmId());
                break;
            case Relation.NAME:
                nodeIds.put(e.getOsmId());
                break;
            }
        }
        args.putLongArray(NODE_IDS_KEY, nodeIds.values());
        args.putLongArray(WAY_IDS_KEY, wayIds.values());
        args.putLongArray(RELATION_IDS_KEY, relationIds.values());
        args.putSerializable(LISTENER_KEY, listener);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StorageDelegator delegator = App.getDelegator();
        nodeIds = getArguments().getLongArray(NODE_IDS_KEY);
        wayIds = getArguments().getLongArray(WAY_IDS_KEY);
        relationIds = getArguments().getLongArray(RELATION_IDS_KEY);
        memberships = new MultiHashMap<>();
        addMemberships(memberships, delegator, nodeIds, Node.NAME);
        addMemberships(memberships, delegator, wayIds, Way.NAME);
        addMemberships(memberships, delegator, relationIds, Relation.NAME);
        listener = (OnRelationsSelectedListener) getArguments().getSerializable(LISTENER_KEY);
    }

    /**
     * @param target
     * @param delegator
     */
    private void addMemberships(MultiHashMap<Long, RelationMemberPosition> target, StorageDelegator delegator, long[] ids, String type) {
        for (Long id : ids) {
            OsmElement o = delegator.getOsmElement(type, id);
            List<Relation> parents = o.getParentRelations();
            if (parents != null) {
                for (Relation p : parents) {
                    List<RelationMember> members = p.getMembers();
                    for (int i = 0; i < members.size(); i++) {
                        target.add(p.getOsmId(), new RelationMemberPosition(members.get(i), i));
                    }
                }
            }
        }
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        View layout = createView(null);
        builder.setNeutralButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onRelationsSelected(memberships);
            }
        });
        builder.setView(layout);
        ViewCompat.setClipBounds(layout, null);
        return builder.create();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(container);
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * @param container parent view or null
     * @return the View
     */
    private View createView(@Nullable ViewGroup container) {
        LayoutInflater inflater;
        FragmentActivity activity = getActivity();
        inflater = ThemeUtils.getLayoutInflater(activity);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.relation_selection_view, container, false);
        tl = (TableLayout) layout.findViewById(R.id.relation_vertical_layout);
        tl.setShrinkAllColumns(false);
        tl.setColumnShrinkable(2, true);
        tl.setStretchAllColumns(false);
        tl.setColumnStretchable(2, true);

        addRows(activity);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        // this will initially be done twice
        // but doing it here allows to update a
        // after configuration changes painlessly
        tl.removeAllViews();
        addRows(getActivity());
    }

    /**
     * Add a row to the TableLayout
     * 
     * @param context Android context
     */
    private void addRows(@NonNull Context context) {
        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(2, 0, 2, 0);

        ViewBox viewbox = new ViewBox(App.getLogic().getViewBox());
        viewbox.expand(2.0);
        List<Relation> relations = App.getDelegator().getCurrentStorage().getRelations();

        for (Relation r : relations) {
            BoundingBox box = r.getBounds();
            if (box != null && box.intersects(viewbox)) {
                tl.addView(createRow(context, r, tp));
            }
        }
    }

    /**
     * Create a row in the dialog for a specific layer
     * 
     * @param context Android context
     * @param layer the MapViewLayer
     * @param tp LayoutParams for this row
     * @return a TableRow
     */
    @NonNull
    TableRow createRow(@NonNull Context context, @NonNull final Relation r, @NonNull TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        final AppCompatCheckBox check = new AppCompatCheckBox(context);

        check.setPadding(0, 0, Density.dpToPx(context, 5), 0);

        Set<RelationMemberPosition> members = memberships.get(r.getOsmId());
        int count = 0; // number of times the selected elements turn up in the relations
        Set<String> roles = new HashSet<>();
        for (RelationMemberPosition member : members) {
            RelationMember rm = member.getRelationMember();
            long id = rm.getRef();
            switch (rm.getType()) {
            case Node.NAME:
                if (contains(nodeIds, id)) {
                    count++;
                    roles.add(rm.getRole());
                }
                break;
            case Way.NAME:
                if (contains(wayIds, id)) {
                    count++;
                    roles.add(rm.getRole());
                }
                break;
            case Relation.NAME:
                if (contains(relationIds, id)) {
                    count++;
                    roles.add(rm.getRole());
                }
                break;
            }
        }

        check.setChecked(count > 0);
        System.out.println("Size " + count + " " + nodeIds.length + " " + wayIds.length + " " + relationIds.length);
        check.setEnabled((count == nodeIds.length + wayIds.length + relationIds.length && roles.size() == 1) || count == 0);
        check.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Long id = Long.valueOf(r.getOsmId());
                if (isChecked && !memberships.containsKey(id)) {
                    StorageDelegator delegator = App.getDelegator();
                    Relation r = (Relation) delegator.getOsmElement(Relation.NAME, id);
                    int pos = r.getMembers().size();
                    for (long nodeId : nodeIds) {
                        memberships.add(id, new RelationMemberPosition(new RelationMember("", delegator.getOsmElement(Node.NAME, nodeId)), pos));
                        pos++;
                    }
                    for (long wayId : wayIds) {
                        memberships.add(id, new RelationMemberPosition(new RelationMember("", delegator.getOsmElement(Way.NAME, wayId)), pos));
                        pos++;
                    }
                    for (long relationId : relationIds) {
                        memberships.add(id, new RelationMemberPosition(new RelationMember("", delegator.getOsmElement(Relation.NAME, relationId)), pos));
                        pos++;
                    }
                }
                if (!isChecked && memberships.containsKey(id)) {
                    memberships.removeKey(id);
                }

            }

        });
        tr.addView(check);

        AutoCompleteTextView roleView = new AutoCompleteTextView(context);
        roleView.setAdapter(getRoleAutocompleteAdapter(r, roles));
        roleView.setThreshold(1);
        OnClickListener autocompleteOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.hasFocus()) {
                    ((AutoCompleteTextView) v).showDropDown();
                }
            }
        };

        roleView.setOnClickListener(autocompleteOnClick);
        
        roleView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(DEBUG_TAG, "onItemClicked value");
                Object o = parent.getItemAtPosition(position);
                if (o instanceof StringWithDescription) {
                    roleView.setText(((StringWithDescription) o).getValue());
                } else if (o instanceof String) {
                    roleView.setText((String) o);
                } else if (o instanceof PresetRole) {
                    roleView.setText(((PresetRole) o).getRole());
                }
            }
        });
        
        roleView.setEms(4);
        roleView.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tr.addView(roleView);

        TextView relationName = new TextView(context);
        relationName.setText(r.getDescription(context));
        relationName.setMinEms(2);
        relationName.setHorizontallyScrolling(true);
        relationName.setSingleLine(true);
        relationName.setEllipsize(TextUtils.TruncateAt.END);
        relationName.setPadding(Density.dpToPx(context, 5), 0, Density.dpToPx(context, 5), 0);
        tr.addView(relationName);
        tr.setGravity(Gravity.CENTER_VERTICAL);
        tr.setLayoutParams(tp);
        return tr;
    }

    private boolean contains(long[] array, long id) {
        for (long a : array) {
            if (id == a) {
                return true;
            }
        }
        return false;
    }

    PresetItem getRelationPreset(long relationId) {
        Preset[] presets = App.getCurrentPresets(getContext());
        Relation r = (Relation) App.getDelegator().getOsmElement(Relation.NAME, relationId);
        if (presets != null && r != null) {
            return Preset.findBestMatch(presets, r.getTags());
        }
        return null;
    }

    ArrayAdapter<PresetRole> getRoleAutocompleteAdapter(@NonNull Relation r, @NonNull Set<String> existingRoles) {
        List<PresetRole> result = new ArrayList<>();
        PresetItem presetItem = getRelationPreset(r.getOsmId());
        if (presetItem != null) {
            Map<String, Integer> counter = new HashMap<>();
            int position = 0;
            List<String> tempRoles = App.getMruTags().getRoles(presetItem);
            if (tempRoles != null) {
                for (String role : tempRoles) {
                    result.add(new PresetRole(role, null, null));
                    counter.put(role, position++);
                }
            }
            List<PresetRole> tempPresetRoles = presetItem.getRoles(null);
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
        System.out.println("Roles found " + result.size() + " for " + r.getDescription());
        return new ArrayAdapter<>(getContext(), R.layout.autocomplete_row, result);
    }

    /**
     * Create a divider View to be added to a TableLAyout
     * 
     * @param context Android context
     * @return a thin TableRow
     */
    public static TableRow divider(@NonNull Context context) {
        TableRow tr = new TableRow(context);
        View v = new View(context);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1);
        trp.span = 4;
        v.setLayoutParams(trp);
        v.setBackgroundColor(Color.rgb(204, 204, 204));
        tr.addView(v);
        return tr;
    }

}
