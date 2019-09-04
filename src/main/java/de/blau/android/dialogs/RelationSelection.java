package de.blau.android.dialogs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.ViewBox;
import de.blau.android.util.Density;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.collections.LongHashSet;

/**
 * Layer dialog
 * 
 * @author Simon Poole
 *
 */
public class RelationSelection extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = RelationSelection.class.getName();

    private static final String TAG = "fragment_relation_selection";

    private static final String RELATION_IDS_KEY = "relationIds";
    private static final String LISTENER_KEY     = "listener";

    TableLayout tl;

    private Map<Long, String>           relationIds;
    private OnRelationsSelectedListener listener;

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

        LongHashSet relationIds = new LongHashSet();
        for (OsmElement e : elements) {
            List<Relation> parents = e.getParentRelations();
            if (parents != null) {
                for (Relation p : parents) {
                    relationIds.put(p.getOsmId(), p.);
                }
            }
        }
        args.putLongArray(RELATION_IDS_KEY, relationIds.values());
        args.putSerializable(LISTENER_KEY, listener);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        relationIds = new ArrayList<Long>();
        for (Long id : getArguments().getLongArray(RELATION_IDS_KEY)) {
            relationIds.add(id);
        }
        listener = (OnRelationsSelectedListener) getArguments().getSerializable(LISTENER_KEY);
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        View layout = createView(null);
        builder.setNeutralButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onRelationsSelected();
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

        List<Relation> relations = App.getDelegator().getCurrentStorage().getRelations();

        for (Relation r : relations) {
            tl.addView(createRow(context, r, relationIds.contains(Long.valueOf(r.getOsmId())), tp));
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
    TableRow createRow(@NonNull Context context, @NonNull final Relation r, boolean selected, @NonNull TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        final AppCompatCheckBox check = new AppCompatCheckBox(context);

        check.setPadding(0, 0, Density.dpToPx(context, 5), 0);

        check.setChecked(selected);
        check.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Long id = Long.valueOf(r.getOsmId());
                if (isChecked && !relationIds.contains(id)) {
                    relationIds.add(id);
                }
                if (!isChecked && relationIds.contains(id)) {
                    relationIds.remove(id);
                }

            }

        });
        tr.addView(check);

        TextView cell = new TextView(context);
        cell.setText(r.getDescription(context));
        cell.setMinEms(2);
        cell.setHorizontallyScrolling(true);
        cell.setSingleLine(true);
        cell.setEllipsize(TextUtils.TruncateAt.END);
        cell.setPadding(Density.dpToPx(context, 5), 0, Density.dpToPx(context, 5), 0);
        tr.addView(cell);
        tr.setGravity(Gravity.CENTER_VERTICAL);
        tr.setLayoutParams(tp);
        return tr;
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
