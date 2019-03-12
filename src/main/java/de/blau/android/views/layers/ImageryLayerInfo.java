package de.blau.android.views.layers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.TableLayoutUtils;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.DateFormatter;

public class ImageryLayerInfo extends LayerInfo {
    private static final String DEBUG_TAG = ImageryLayerInfo.class.getName();

    public static final String LAYER_KEY = "layer";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    TileLayerServer layer = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layer = (TileLayerServer) getArguments().getSerializable(LAYER_KEY);
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
        if (layer != null) {
            tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, layer.getName(), tp));
            tableLayout.addView(TableLayoutUtils.divider(activity));
            String description = layer.getDescription();
            if (description != null) {
                tableLayout.addView(TableLayoutUtils.createFullRow(activity, description, tp));
                tableLayout.addView(TableLayoutUtils.divider(activity));
            }
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.type, null, layer.getType(), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_min_zoom, null, Integer.toString(layer.getMinZoomLevel()), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_max_zoom, null, Integer.toString(layer.getMaxZoomLevel()), tp));
            long startDate = layer.getStartDate();
            if (startDate > 0) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_start_date, null,
                        DateFormatter.getUtcFormat(DATE_FORMAT).format(startDate), tp));
            }
            long endDate = layer.getEndDate();
            if (endDate < Long.MAX_VALUE) {
                tableLayout.addView(
                        TableLayoutUtils.createRow(activity, R.string.layer_info_end_date, null, DateFormatter.getUtcFormat(DATE_FORMAT).format(endDate), tp));
            }
            String tou = layer.getTouUri();
            if (tou != null) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_terms, null, tou, true, tp));
            }
            String privacyPolicy = layer.getPrivacyPolicyUrl();
            if (privacyPolicy != null) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_privacy_policy, null, privacyPolicy, true, tp));
            }
        } else {
            Log.e(DEBUG_TAG, "layer null");
        }
        return sv;
    }
}
