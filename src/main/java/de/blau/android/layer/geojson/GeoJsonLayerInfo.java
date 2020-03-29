package de.blau.android.layer.geojson;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.TableLayoutUtils;
import de.blau.android.util.GeoJSONConstants;

public class GeoJsonLayerInfo extends LayerInfo {
    private static final String DEBUG_TAG = GeoJsonLayerInfo.class.getName();

    public static final String LAYER_INFO_KEY = "layerInfo";

    de.blau.android.layer.geojson.MapOverlay.Info layerInfo = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layerInfo = (de.blau.android.layer.geojson.MapOverlay.Info) getArguments().getSerializable(LAYER_INFO_KEY);
    }

    @Override
    protected View createView(@Nullable ViewGroup container) {
        Log.d(DEBUG_TAG, "createView");
        ScrollView sv = createEmptyView(container);
        FragmentActivity activity = getActivity();
        TableLayout tableLayout = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);
        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT);
        tp.setMargins(10, 2, 10, 2);
        tableLayout.setColumnShrinkable(1, false);
        if (layerInfo != null) {
            tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, layerInfo.name, tp));
            tableLayout.addView(TableLayoutUtils.divider(activity));
            String path = layerInfo.path;
            if (path != null) {
                tableLayout.addView(TableLayoutUtils.createFullRow(activity, path, tp));
                tableLayout.addView(TableLayoutUtils.divider(activity));
            }
            tableLayout.addView(TableLayoutUtils.createRow(activity, GeoJSONConstants.POINT, Integer.toString(layerInfo.pointCount), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, GeoJSONConstants.MULTIPOINT, Integer.toString(layerInfo.multiPointCount), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, GeoJSONConstants.LINESTRING, Integer.toString(layerInfo.linestringCount), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, GeoJSONConstants.MULTILINESTRING, Integer.toString(layerInfo.multiLinestringCount), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, GeoJSONConstants.POLYGON, Integer.toString(layerInfo.polygonCount), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, GeoJSONConstants.MULTIPOLYGON, Integer.toString(layerInfo.multiPolygonCount), tp));
            tableLayout.addView(
                    TableLayoutUtils.createRow(activity, GeoJSONConstants.GEOMETRYCOLLECTION, Integer.toString(layerInfo.geometrycollectionCount), tp));
        } else {
            Log.e(DEBUG_TAG, "layerInfo null");
        }
        return sv;
    }
}
