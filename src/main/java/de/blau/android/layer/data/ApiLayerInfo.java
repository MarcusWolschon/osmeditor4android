package de.blau.android.layer.data;

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
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.TableLayoutUtils;
import de.blau.android.osm.MapSplitSource;
import de.blau.android.osm.Server;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.MBTileConstants;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.util.DateFormatter;

public class ApiLayerInfo extends LayerInfo {
    private static final String DEBUG_TAG = ApiLayerInfo.class.getName();

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm Z";

    TileLayerSource layer = null;

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
            tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, getString(R.string.api_info_mapsplit_source), tp));
            Map<String, String> meta = db.getMetadata();
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
        } else if (server.hasReadOnly()) {
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.readonly_url, null, server.getReadOnlyUrl(), tp));
        }
        StorageDelegator delegator = App.getDelegator();
        if (delegator != null) {
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
        }
        return sv;
    }
}
