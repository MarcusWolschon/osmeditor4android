package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement.ElementType;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Track.WayPoint;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetElement;
import de.blau.android.presets.Preset.PresetGroup;
import de.blau.android.presets.PresetElementPath;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class ViewWayPoint extends DialogFragment {

    private static final String WAYPOINT = "waypoint";

    private static final String DEBUG_TAG = ViewWayPoint.class.getName();

    private static final String TAG = "fragment_view_waypoint";

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
        Preferences prefs = new Preferences(getActivity());
        if (prefs.lightThemeEnabled()) {
            setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_DialogLight);
        } else {
            setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_DialogDark);
        }
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(activity);

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, null, false);
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        final WayPoint wp = (WayPoint) getArguments().getSerializable(WAYPOINT);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

        if (wp != null) {
            // tl.setShrinkAllColumns(true);
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
        builder.setPositiveButton(R.string.create_osm_object, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                createObjectFromWayPoint(wp, false);
            }
        });
        builder.setNegativeButton(R.string.create_osm_object_search, new OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                createObjectFromWayPoint(wp, true);
            }
        });
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
                List<PresetElement> searchResults = new ArrayList<>(SearchIndexUtils.searchInPresets(getActivity(), wp.getType(), ElementType.NODE, 1, 1));
                if (searchResults != null && !searchResults.isEmpty()) {
                    Preset[] presets = App.getCurrentPresets(activity);
                    PresetGroup rootGroup = presets[0].getRootGroup();
                    presetPath = searchResults.get(0).getPath(rootGroup);
                } else {
                    Snack.barInfo(getActivity(), R.string.toast_nothing_found);
                }
            }

            HashMap<String, String> tags = new HashMap<>();
            if (wp.getName() != null) {
                tags.put(Tags.KEY_NAME, wp.getName());
            }
            if (wp.getDescription() != null) {
                tags.put(Tags.KEY_NOTE, wp.getDescription());
            }
            ((Main) activity).performTagEdit(n, presetPath, tags, presetPath == null ? true : false);
        }
    }
}
