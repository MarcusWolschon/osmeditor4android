package de.blau.android.dialogs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.util.List;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.gpx.TrackSegment;
import de.blau.android.osm.Gpx;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class TrackSegmentInfo extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, TrackSegmentInfo.class.getSimpleName().length());
    private static final String DEBUG_TAG = TrackSegmentInfo.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_view_tracksegment";

    private static final String LAYER_NAME_KEY = "layer_name";
    private static final String SEGMENT_KEY    = "segment";

    private TrackSegment segment = null;
    private String       layerName;

    /**
     * Show dialog for a WayPoint
     * 
     * @param activity the calling activity
     * @param layerName String version of uri of the enclosing file
     * @param segment the TrackSegment
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull String layerName, @NonNull TrackSegment segment) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            TrackSegmentInfo trackSegmentInfoFragment = newInstance(layerName, segment);
            trackSegmentInfoFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog created with showDialog
     * 
     * @param activity the calling activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of this dialog
     * 
     * @param uriString String version of uri of the enclosing file
     * @param segment the WayPoint
     * 
     * @return the FragmentDialog instance
     */
    private static TrackSegmentInfo newInstance(@NonNull String uriString, @NonNull TrackSegment segment) {
        TrackSegmentInfo f = new TrackSegmentInfo();

        Bundle args = new Bundle();
        args.putString(LAYER_NAME_KEY, uriString);
        args.putSerializable(SEGMENT_KEY, segment);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            layerName = savedInstanceState.getString(LAYER_NAME_KEY);
            segment = de.blau.android.util.Util.getSerializeable(savedInstanceState, SEGMENT_KEY, TrackSegment.class);
        } else {
            layerName = getArguments().getString(LAYER_NAME_KEY);
            segment = de.blau.android.util.Util.getSerializeable(getArguments(), SEGMENT_KEY, TrackSegment.class);
        }

        FragmentActivity activity = getActivity();
        Builder builder = ThemeUtils.getAlertDialogBuilder(activity);

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, null, false);
        if (segment == null) {
            Log.e(DEBUG_TAG, "Null segment");
            return builder.create();
        }
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        tl.setColumnShrinkable(1, true);
        if (de.blau.android.util.Util.notEmpty(layerName)) {
            tl.addView(TableLayoutUtils.createRow(activity, R.string.gpx_track, layerName, tp));
        }

        tl.addView(setAlignmentEnd(TableLayoutUtils.createRow(activity, R.string.gpx_track_segment_start, Integer.toString(segment.getStart()), tp), 1));
        tl.addView(setAlignmentEnd(TableLayoutUtils.createRow(activity, R.string.gpx_track_segment_end, Integer.toString(segment.getEnd()), tp), 1));

        builder.setView(sv);
        builder.setTitle(R.string.track_segment_title);
        builder.setPositiveButton(R.string.create_osm_object, (dialog, which) -> {
            final Logic logic = App.getLogic();
            int maxNodes = logic.getPrefs().getServer().getCachedCapabilities().getMaxWayNodes();
            List<OsmElement> elements = Gpx.trackPointsToWays(App.getDelegator().getFactory(), segment.getSegment(), maxNodes);
            if (elements.isEmpty()) {
                return;
            }
            logic.addElements(activity, elements);
            if (activity instanceof Main) {
                for (OsmElement e : elements) {
                    if (e instanceof Way) {
                        logic.addSelectedWay((Way) e);
                    }
                }
                ((Main) activity).getEasyEditManager().startElementSelectionMode();
            }
        });
        builder.setNegativeButton(R.string.menu_download_along_way,
                (dialog, which) -> DownloadAlongDialog.show((AppCompatActivity) getActivity(), R.string.download_along_way_title, segment, null));
        builder.setNeutralButton(R.string.cancel, null);

        return builder.create();
    }

    /**
     * Align text in the table cell to the end/right
     * 
     * @param row the TableRow
     * @param child child index
     * @return row for convenience
     */
    @NonNull
    private TableRow setAlignmentEnd(@NonNull TableRow row, int child) {
        if (row.getChildCount() > child) {
            row.getChildAt(child).setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
        return row;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(LAYER_NAME_KEY, layerName);
        outState.putSerializable(SEGMENT_KEY, segment);
    }
}
