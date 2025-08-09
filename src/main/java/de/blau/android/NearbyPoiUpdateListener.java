package de.blau.android;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.filter.Filter;
import de.blau.android.layer.UpdateInterface;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.util.Screen;
import de.blau.android.util.collections.LowAllocArrayList;

public class NearbyPoiUpdateListener<E> implements UpdateInterface.OnUpdateListener<E> {

    private static final int UPDATE_MIN_INTERVAL   = 100; // minimum interval between updates in ms
    private static final int POI_LAYOUT_MIN_HEIGHT = 5;
    private static final int ROW_PADDING           = 5;
    private static final int LANDSCAPE_COLS        = 4;
    private static final int PORTRAIT_COLS         = 2;

    private final String[]  defaultKeys;
    private final PoiFilter withoutFilter;
    private final PoiFilter withFilter = (OsmElement e, Filter filter) -> filter.include(e, false);

    List<Node>       nodes     = new LowAllocArrayList<>();
    List<Way>        ways      = new LowAllocArrayList<>();
    List<Relation>   relations = new LowAllocArrayList<>();
    List<OsmElement> all       = new LowAllocArrayList<>();

    private final Map          map;
    private final Runnable     display;
    private final RecyclerView layout;

    private int updates = 0; // count of update requests since last update

    /**
     * Construct a new listener for nearby POI updates
     * 
     * @param ctx an Android Context
     * @param map the current Map instance
     * @param layout the layout we will add the POIs to
     */
    NearbyPoiUpdateListener(@NonNull Context ctx, @NonNull Map map, @NonNull RecyclerView layout) {
        this.map = map;
        this.layout = layout;
        layout.setPadding(0, ROW_PADDING, 0, 0);
        GridLayoutManager layoutManager = new GridLayoutManager(ctx,
                ctx instanceof Activity && Screen.isLandscape((Activity) ctx) ? LANDSCAPE_COLS : PORTRAIT_COLS);
        layout.setLayoutManager(layoutManager);
        layout.setAdapter(new PoiListAdapter(ctx, map, layout, all));
        if (layout.getItemDecorationCount() == 0) {
            layout.addItemDecoration(new EqualSpacingDecoration(PoiListAdapter.ROW_MARGIN));
        }
        defaultKeys = App.getPreferences(ctx).poiKeys().toArray(new String[0]);
        withoutFilter = (OsmElement e, Filter filter) -> e.hasTagKey(defaultKeys);

        display = () -> {
            all.clear();
            final Filter filter = App.getLogic().getFilter();
            filterElements(all, nodes, filter);
            filterElements(all, ways, filter != null);
            filterElements(all, relations, filter != null);
            final double[] center = map.getViewBox().getCenter();
            final int[] loc = new int[] { (int) (center[1] * 1E7), (int) (center[0] * 1E7) };
            Collections.sort(all, (OsmElement e1, OsmElement e2) -> Double.compare(e1.getMinDistance(loc), e2.getMinDistance(loc)));
            layout.getAdapter().notifyDataSetChanged();
            updates = 0;
        };
    }

    private interface PoiFilter {
        /**
         * "Accept" a POI for display
         * 
         * @param e the OsmElement for the POI
         * @param filter any Filter that needs to be pre-applied
         * @return true if we should display the POI
         */
        boolean accept(@NonNull OsmElement e, @Nullable Filter filter);
    }

    /**
     * Add Nodes that we want to display
     * 
     * @param result List containing the filtered Nodes
     * @param elements the input Nodes
     * @param filter a Filter to apply
     */
    private void filterElements(@NonNull List<OsmElement> result, @NonNull List<Node> elements, @Nullable Filter filter) {
        PoiFilter poiFilter = filter == null ? withoutFilter : withFilter;
        for (OsmElement e : elements) {
            if (poiFilter.accept(e, filter) && e.hasTags()) {
                result.add(e);
            }
        }
    }

    /**
     * Add OsmElements that we want to display
     * 
     * @param result List containing the filtered OsmElements
     * @param elements the input OsmElements
     * @param preFiltered true if the input was already filtered
     */
    private <O extends OsmElement> void filterElements(@NonNull List<OsmElement> result, @NonNull List<O> elements, boolean preFiltered) {
        for (OsmElement e : elements) {
            if (preFiltered || withoutFilter.accept(e, null)) {
                result.add(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Collection<E>> void onUpdate(C... objects) {
        if (layout.getHeight() <= POI_LAYOUT_MIN_HEIGHT) {
            return;
        }
        // this is sync with onDraw
        nodes.clear();
        nodes.addAll((Collection<Node>) objects[0]);
        ways.clear();
        ways.addAll((Collection<Way>) objects[1]);
        relations.clear();
        relations.addAll((Collection<Relation>) objects[2]);

        if (updates == 0) {
            map.postDelayed(display, UPDATE_MIN_INTERVAL);
        }
        updates++;
    }
}
