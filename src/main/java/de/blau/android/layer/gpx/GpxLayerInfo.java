package de.blau.android.layer.gpx;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.TableLayoutUtils;
import de.blau.android.gpx.Track;
import de.blau.android.layer.LayerType;

public class GpxLayerInfo extends LayerInfo {
    private static final String DEBUG_TAG = GpxLayerInfo.class.getName();

    public static final String LAYER_ID_KEY = "layerId";

    private String layerId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layerId = getArguments().getString(LAYER_ID_KEY);
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
        MapOverlay layer = (MapOverlay) ((Main) getActivity()).getMap().getLayer(LayerType.GPX, layerId);
        if (layer != null) {
            tableLayout.addView(TableLayoutUtils.createFullRowTitle(activity, layer.getName(), tp));
            tableLayout.addView(TableLayoutUtils.divider(activity));
            Track track = layer.getTrack();
            if (track != null) {
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.track_points, Integer.toString(track.getTrackPoints().size()), tp));
                tableLayout.addView(TableLayoutUtils.createRow(activity, R.string.way_points, Integer.toString(track.getWayPoints().size()), tp));
            }
        } else {
            Log.e(DEBUG_TAG, "layerInfo null");
        }
        return sv;
    }
}
