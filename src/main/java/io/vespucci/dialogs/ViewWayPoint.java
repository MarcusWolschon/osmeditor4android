package io.vespucci.dialogs;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.gpx.WayPoint;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmParser;
import io.vespucci.osm.Tags;
import io.vespucci.osm.OsmElement.ElementType;
import io.vespucci.presets.Preset;
import io.vespucci.presets.PresetElement;
import io.vespucci.presets.PresetElementPath;
import io.vespucci.presets.PresetGroup;
import io.vespucci.util.ContentResolverUtil;
import io.vespucci.util.DateFormatter;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.SearchIndexUtils;
import io.vespucci.util.ThemeUtils;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class ViewWayPoint extends ImmersiveDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ViewWayPoint.class.getSimpleName().length());
    private static final String DEBUG_TAG = ViewWayPoint.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_view_waypoint";

    private static final String URI_KEY      = "uri";
    private static final String WAYPOINT_KEY = "waypoint";

    private WayPoint wp = null;
    private String   uriString;

    /**
     * Show dialog for a WayPoint
     * 
     * @param activity the calling activity
     * @param uriString String version of uri of the enclosing file
     * @param wp the WayPoint
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull String uriString, @NonNull WayPoint wp) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            ViewWayPoint elementInfoFragment = newInstance(uriString, wp);
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
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        io.vespucci.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of this dialog
     * 
     * @param uriString String version of uri of the enclosing file
     * @param wp the WayPoint
     * 
     * @return the FragmentDialog instance
     */
    private static ViewWayPoint newInstance(@NonNull String uriString, @NonNull WayPoint wp) {
        ViewWayPoint f = new ViewWayPoint();

        Bundle args = new Bundle();
        args.putString(URI_KEY, uriString);
        args.putSerializable(WAYPOINT_KEY, wp);

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
            uriString = savedInstanceState.getString(URI_KEY);
            wp = io.vespucci.util.Util.getSerializeable(savedInstanceState, WAYPOINT_KEY, WayPoint.class);
        } else {
            uriString = getArguments().getString(URI_KEY);
            wp = io.vespucci.util.Util.getSerializeable(getArguments(), WAYPOINT_KEY, WayPoint.class);
        }

        FragmentActivity activity = getActivity();
        Builder builder = new AlertDialog.Builder(activity);

        // inflater needs to be got from a themed view or else all our custom stuff will not style correctly
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);

        ScrollView sv = (ScrollView) inflater.inflate(R.layout.element_info_view, null, false);
        if (wp == null) {
            Log.e(DEBUG_TAG, "Null WayPoint");
            return builder.create();
        }
        TableLayout tl = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);

        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(10, 2, 10, 2);

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
            tl.addView(TableLayoutUtils.createRow(activity, R.string.created, DateFormatter.getUtcFormat(OsmParser.TIMESTAMP_FORMAT).format(timestamp), tp));
        }

        tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lon_label, String.format(Locale.US, "%.7f", wp.getLongitude()) + "°", tp));
        tl.addView(TableLayoutUtils.createRow(activity, R.string.location_lat_label, String.format(Locale.US, "%.7f", wp.getLatitude()) + "°", tp));

        if (wp.hasAltitude()) {
            tl.addView(TableLayoutUtils.createRow(activity, R.string.altitude, String.format(Locale.US, "%.0f", wp.getAltitude()) + "m", tp));
        }
        Uri gpxUri = Uri.parse(uriString);
        List<WayPoint.Link> links = wp.getLinks();
        if (io.vespucci.util.Util.notEmpty(links)) {
            for (WayPoint.Link link : links) {
                final String description = link.getDescription();
                TableRow row = TableLayoutUtils.createRow(activity, getString(R.string.waypoint_link),
                        io.vespucci.util.Util
                                .notEmpty(description) ? description : link.getUrl(), false,
                        (View v) -> playLinkUri(activity, gpxUri, link), tp);
                tl.addView(row);
                row.requestFocus();
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
     * Attempt to play/view whatever is linked to in the Link
     * 
     * This uses a hack to find the content Uri for the file which is dubious
     * 
     * @param context an Android Context
     * @param gpxUri the URI for the GPX file
     * @param link the Link Element
     */
    private void playLinkUri(@NonNull Context context, @NonNull Uri gpxUri, @NonNull WayPoint.Link link) {
        Uri uri = Uri.parse(link.getUrl());
        if (uri.getScheme() != null) {
            context.startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
            return;
        }
        Uri actualUri = Uri.parse(ContentResolverUtil.getPath(context, gpxUri));
        Uri.Builder uriBuilder = new Uri.Builder();
        List<String> pathSegments = actualUri.getPathSegments();
        for (String segment : pathSegments.subList(0, pathSegments.size() - 1)) {
            uriBuilder.appendPath(segment);
        }
        for (String segment : uri.getPathSegments()) {
            uriBuilder.appendPath(segment);
        }
        uri = uriBuilder.build();
        // the following is a hack suggested in
        // https://stackoverflow.com/questions/7305504/convert-file-uri-to-content-uri
        MediaScannerConnection.scanFile(getContext(), new String[] { uri.getPath() }, null, (String s, Uri scanUri) -> {
            if (scanUri == null) {
                ScreenMessage.barError(getActivity(), getString(R.string.toast_file_not_found, s));
                return;
            }
            context.startActivity(new Intent(Intent.ACTION_VIEW).setData(scanUri));
        });
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
        outState.putString(URI_KEY, uriString);
        outState.putSerializable(WAYPOINT_KEY, wp);
    }
}
