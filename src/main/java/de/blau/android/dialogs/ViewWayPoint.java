package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.gpx.WayPoint;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Tags;
import de.blau.android.presets.Preset;
import de.blau.android.presets.PresetElement;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.presets.PresetGroup;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class ViewWayPoint extends ImmersiveDialogFragment {

    private static final String WAYPOINT = "waypoint";

    private static final String DEBUG_TAG = ViewWayPoint.class.getName();

    private static final String TAG = "fragment_view_waypoint";

    private WayPoint wp = null;

    /**
     * Show dialog for a WayPoint
     * 
     * @param activity the calling activity
     * @param wp the WayPoint
     */
    public static void showDialog(FragmentActivity activity, WayPoint wp) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ViewWayPoint elementInfoFragment = newInstance(wp);
            elementInfoFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog created with showDialog
     * 
     * @param activity the calling activity
     */
    private static void dismissDialog(FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of this dialog
     * 
     * @param wp the WayPoint
     * @return the FragmentDialog instance
     */
    private static ViewWayPoint newInstance(WayPoint wp) {
        ViewWayPoint f = new ViewWayPoint();

        Bundle args = new Bundle();
        args.putSerializable(WAYPOINT, wp);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, App.getPreferences(getActivity()).lightThemeEnabled() ? R.style.Theme_DialogLight : R.style.Theme_DialogDark);
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG, "restoring from saved state");
            wp = de.blau.android.util.Util.getSerializeable(savedInstanceState, WAYPOINT, WayPoint.class);
        } else {
            wp = de.blau.android.util.Util.getSerializeable(getArguments(), WAYPOINT, WayPoint.class);
        }

        FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(activity);

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, null, false);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        if (wp != null) {
            tl.setColumnShrinkable(1, true);
            if (wp.getName() != null) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.name, wp.getName(), tp));
            }
            if (wp.getDescription() != null) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.description, wp.getDescription(), tp));
            }
            if (wp.getType() != null) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.type, wp.getType(), tp));
            }
            long timestamp = wp.getTime();
            if (timestamp > 0) {
                tl.addView(
                        TableLayoutUtils.createRow(activity, R.string.created, DateFormatter.getUtcFormat(OsmParser.TIMESTAMP_FORMAT).format(timestamp), tp));
            }

            tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lon_label, String.format(Locale.US, "%.7f", wp.getLongitude()) + "°", tp));
            tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lat_label, String.format(Locale.US, "%.7f", wp.getLatitude()) + "°", tp));

            if (wp.hasAltitude()) {
                tl.addView(TableLayoutUtils.createRow(activity, R.string.altitude, String.format(Locale.US, "%.0f", wp.getAltitude()) + "m", tp));
            }
        }
        builder.setView(sv);
        builder.setTitle(R.string.waypoint_title);
        builder.setPositiveButton(R.string.create_osm_object, (dialog, which) -> createObjectFromWayPoint(wp, false));
        builder.setNegativeButton(R.string.create_osm_object_search, (dialog, which) -> createObjectFromWayPoint(wp, true));
        builder.setNeutralButton(R.string.cancel, null);

        return builder.create();
    }

    /**
     * Create a Node from information in and the position of a way point
     * 
     * @param wp the WayPoint
     * @param useSearch if the type field is present search the presets for a match
     */
    private void createObjectFromWayPoint(final WayPoint wp, final boolean useSearch) {
        Logic logic = App.getLogic();
        FragmentActivity activity = getActivity();
        Node n = logic.performAddNode(activity, wp.getLongitude(), wp.getLatitude());
        if (activity instanceof Main) {
            PresetElementPath presetPath = null;
            if (useSearch && wp.getType() != null) {
                List<PresetElement> searchResults = new ArrayList<>(
                        SearchIndexUtils.searchInPresets(getActivity(), wp.getType(), ElementType.NODE, 1, 1, null));
                if (!searchResults.isEmpty()) {
                    Preset[] presets = App.getCurrentPresets(activity);
                    PresetGroup rootGroup = presets[0].getRootGroup();
                    presetPath = searchResults.get(0).getPath(rootGroup);
                } else {
                    ScreenMessage.barInfo(getActivity(), R.string.toast_nothing_found);
                }
            }

            HashMap<String, String> tags = new HashMap<>();
            if (wp.getName() != null) {
                tags.put(Tags.KEY_NAME, wp.getName());
            }
            if (wp.getDescription() != null) {
                tags.put(Tags.KEY_NOTE, wp.getDescription());
            }
            ((Main) activity).performTagEdit(n, presetPath, tags, presetPath == null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WAYPOINT, wp);
    }
}
