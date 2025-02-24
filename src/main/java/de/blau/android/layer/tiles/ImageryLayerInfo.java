package de.blau.android.layer.tiles;

import java.util.Collection;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.TableLayoutUtils;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.Util;

public class ImageryLayerInfo extends LayerInfo {
    private static final String DEBUG_TAG = ImageryLayerInfo.class.getSimpleName().substring(0, Math.min(23, ImageryLayerInfo.class.getSimpleName().length()));

    public static final String LAYER_KEY       = "layer";
    public static final String ERROR_COUNT_KEY = "tileErrorCount";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    TileLayerSource layer          = null;
    long            tileErrorCount = 0L;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layer = Util.getSerializeable(getArguments(), LAYER_KEY, TileLayerSource.class);
        tileErrorCount = getArguments().getLong(ERROR_COUNT_KEY, 0);
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
        tableLayout.setColumnStretchable(2, true);
        if (layer == null) {
            Log.e(DEBUG_TAG, "layer null");
            tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, activity.getString(R.string.layer_info_layer_not_available), tp));
        } else if (!layer.isMetadataLoaded()) {
            Log.e(DEBUG_TAG, "meta data not loaded");
            tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, activity.getString(R.string.layer_info_no_meta_data), tp));
        } else {
            tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, layer.getName(), tp));
            tableLayout.addView(TableLayoutUtils.divider(activity));
            String description = layer.getDescription();
            if (description != null) {
                tableLayout.addView(TableLayoutUtils.createFullRow(activity, description, tp));
                tableLayout.addView(TableLayoutUtils.divider(activity));
            }
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.type, null, layer.getType(), tp));
            if (TileLayerSource.TYPE_WMS.equals(layer.getType())) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.projection, null, layer.getProj(), tp));
            }
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_min_zoom, null, Integer.toString(layer.getMinZoomLevel()), tp));
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_max_zoom, null, Integer.toString(layer.getMaxZoomLevel()), tp));
            long startDate = layer.getStartDate();
            if (startDate > 0) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_start_date, null,
                        DateFormatter.getUtcFormat(DATE_FORMAT).format(startDate), tp));
            }
            long endDate = layer.getEndDate();
            if (endDate >= 0 && endDate < Long.MAX_VALUE) {
                tableLayout.addView(
                        TableLayoutUtils.createRow(activity, R.string.layer_info_end_date, null, DateFormatter.getUtcFormat(DATE_FORMAT).format(endDate), tp));
            }
            tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.errors, null, Long.toString(tileErrorCount), tp));
            String tou = layer.getTouUri();
            if (tou != null) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_terms, null, tou, true, tp));
            }
            String privacyPolicy = layer.getPrivacyPolicyUrl();
            if (privacyPolicy != null) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.layer_info_privacy_policy, null, privacyPolicy, true, tp));
            }
            Logic logic = App.getLogic();
            if (logic != null) {
                Map map = logic.getMap();
                if (map != null) {
                    Collection<Provider> providers = layer.getProviders(map.getZoomLevel(), map.getViewBox());
                    String legend = activity.getString(R.string.attribution);
                    for (Provider provider : providers) {
                        String attributionUrl = provider.getAttributionUrl();
                        boolean hasAttributionUrl = attributionUrl != null;
                        tableLayout.addView(TableLayoutUtils.createRow(activity, legend, null,
                                hasAttributionUrl ? Util.fromHtml("<A href=\"" + attributionUrl + "\">" + provider.getAttribution() + "</A>")
                                        : provider.getAttribution(),
                                hasAttributionUrl, tp, R.attr.colorAccent, R.color.material_teal));
                        legend = "";
                    }
                }
            }
        }
        return sv;
    }
}
