package io.vespucci.layer.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.dialogs.LayerInfo;
import io.vespucci.dialogs.TableLayoutUtils;
import io.vespucci.osm.MapSplitSource;
import io.vespucci.osm.Server;
import io.vespucci.osm.Storage;
import io.vespucci.osm.StorageDelegator;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.MBTileConstants;
import io.vespucci.services.util.MBTileProviderDataBase;
import io.vespucci.util.DateFormatter;

public class ApiLayerInfo extends LayerInfo {
    private static final String DEBUG_TAG = ApiLayerInfo.class.getSimpleName().substring(0, Math.min(23, ApiLayerInfo.class.getSimpleName().length()));

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm Z";

    private static final Map<String, Integer> META_FIELDS_TO_RES = new HashMap<>();
    static {
        META_FIELDS_TO_RES.put(MBTileConstants.BOUNDS, R.string.api_info_bounds);
        META_FIELDS_TO_RES.put(MBTileConstants.MAXZOOM, R.string.layer_info_max_zoom);
        META_FIELDS_TO_RES.put(MBTileConstants.MINZOOM, R.string.layer_info_min_zoom);
        META_FIELDS_TO_RES.put(MapSplitSource.LATEST_DATE, R.string.api_info_latest_date);
        META_FIELDS_TO_RES.put(MapSplitSource.ATTRIBUTION, R.string.api_info_attribution);
    }

    @Override
    protected View createView(@Nullable ViewGroup container) {
        Log.d(DEBUG_TAG, "createView");
        ScrollView sv = createEmptyView(container);
        FragmentActivity activity = getActivity();
        TableLayout tableLayout = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);
        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        tp.setMargins(10, 2, 10, 2);
        tableLayout.setColumnShrinkable(1, false);
        Preferences prefs = new Preferences(getContext());
        Server server = prefs.getServer();

        tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, prefs.getApiName(), tp));
        tableLayout.addView(TableLayoutUtils.divider(activity));

        tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.config_api_url_title, null, server.getReadWriteUrl(), tp));

        MBTileProviderDataBase db = server.getMapSplitSource();
        if (db != null) {
            addMapsplitInfo(activity, tableLayout, tp, db);
        } else if (server.hasReadOnly()) {
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.readonly_url, null, server.getReadOnlyUrl(), tp));
        }
        StorageDelegator delegator = App.getDelegator();
        TableLayout t2 = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout_2);
        t2.addView(TableLayoutUtils.createFullRowTitle(activity, getString(R.string.data_in_memory), tp));
        t2.addView(TableLayoutUtils.createRow(activity, "", getString(R.string.total), getString(R.string.changed), tp));
        Storage currentStorage = delegator.getCurrentStorage();
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.nodes), Integer.toString(currentStorage.getNodes().size()),
                Integer.toString(delegator.getApiNodeCount()), tp, -1, -1));
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.ways), Integer.toString(currentStorage.getWays().size()),
                Integer.toString(delegator.getApiWayCount()), tp, -1, -1));
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.relations), Integer.toString(currentStorage.getRelations().size()),
                Integer.toString(delegator.getApiRelationCount()), tp, -1, -1));
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.bounding_boxes), Integer.toString(currentStorage.getBoundingBoxes().size()), null,
                tp, -1, -1));
        return sv;
    }

    /**
     * If a Mapsplit RO source is being used, add some info for that
     * 
     * @param activity the current Activity
     * @param tableLayout the table
     * @param tp table layout params
     * @param db the mapsplit sqlite file
     */
    private void addMapsplitInfo(@NonNull FragmentActivity activity, @NonNull TableLayout tableLayout, @NonNull TableLayout.LayoutParams tp,
            @NonNull MBTileProviderDataBase db) {
        tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, getString(R.string.api_info_mapsplit_source), tp));
        Map<String, String> meta = db.getMetadata();
        if (meta == null) {
            Log.e(DEBUG_TAG, "Meta info from MBT file missing");
            return;
        }
        List<String> keys = new ArrayList<>(meta.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Integer keyRes = META_FIELDS_TO_RES.get(key);
            if (keyRes == null) {
                continue;
            }
            if (MapSplitSource.LATEST_DATE.equals(key)) {
                try {
                    String date = DateFormatter.getUtcFormat(DATE_FORMAT).format(Long.parseLong(meta.get(key)));
                    tableLayout.addView(TableLayoutUtils.createRow(activity, keyRes, null, date, tp));
                } catch (NumberFormatException e) {
                    // Skip
                    Log.e(DEBUG_TAG, "Invalid date in MSF file " + e.getMessage());
                }
            } else if (MBTileConstants.BOUNDS.equals(key)) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, keyRes, null, db.getBounds().toPrettyString(), tp));
            } else {
                tableLayout.addView(TableLayoutUtils.createRow(activity, keyRes, null, meta.get(key), tp));
            }
        }
    }
}
