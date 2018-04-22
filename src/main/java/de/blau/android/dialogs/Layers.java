package de.blau.android.dialogs;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.DisableInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.layers.MapTilesLayer;

/**
 * Very simple dialog fragment to display some info on an OSM element
 * 
 * @author simon
 *
 */
public class Layers extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = Layers.class.getName();

    private static final String TAG = "fragment_layers";

    private int visibleId;
    private int invisibleId;
    private int zoomToExtentId;
    private int menuId;

    TableLayout tl;

    /**
     * Show an info dialog for the supplied GeoJSON Feature
     * 
     * @param activity the calling Activity
     */
    static public void showDialog(FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            Layers elementInfoFragment = newInstance();
            elementInfoFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(FragmentActivity activity) {
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ft.remove(fragment);
            }
            ft.commit();
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Create a new instance of the Layers dialog
     * 
     * @return an instance of the Layers dialog
     */
    private static Layers newInstance() {
        Layers f = new Layers();

        Bundle args = new Bundle();

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getActivity());
        View layout = createView(null);
        final FloatingActionButton add = (FloatingActionButton) layout.findViewById(R.id.add);
        add.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getActivity(), add);
                // menu items for adding layers
                MenuItem addGeoJSON = popup.getMenu().add(R.string.menu_layers_add_geojson);
                addGeoJSON.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem arg0) {
                        final FragmentActivity activity = getActivity();
                        SelectFile.read(activity, R.string.config_osmPreferredDir_key, new ReadFile() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public boolean read(Uri fileUri) {
                                de.blau.android.layer.geojson.MapOverlay geojsonLayer = App.getLogic().getMap().getGeojsonLayer();
                                if (geojsonLayer != null) {
                                    try {
                                        geojsonLayer.resetStyling();
                                        geojsonLayer.loadGeoJsonFile(activity, fileUri);
                                        Preferences prefs = new Preferences(activity);
                                        SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                                        geojsonLayer.invalidate();
                                        LayerStyle.showDialog(activity, geojsonLayer.getIndex());
                                        tl.removeAllViews();
                                        addRows(activity);
                                    } catch (IOException e) {
                                        // display a toast?
                                    }
                                }
                                return true;
                            }
                        });
                        return false;
                    }
                });
                popup.show();
            }
        });
        Button done = (Button) layout.findViewById(R.id.done);
        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dismissDialog();
            }
        });
        dialog.setContentView(layout);
        return dialog;
    }

    /**
     * Dismiss the dialog if it exists
     */
    private void dismissDialog() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(container);
        }
        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Create the view we want to display
     * 
     * @param container parent view or null
     * @return the View
     */
    private View createView(ViewGroup container) {
        LayoutInflater inflater;
        FragmentActivity activity = getActivity();
        inflater = ThemeUtils.getLayoutInflater(activity);
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.layers_view, container, false);
        tl = (TableLayout) layout.findViewById(R.id.layers_vertical_layout);
        tl.setShrinkAllColumns(false);
        tl.setColumnShrinkable(2, true);
        tl.setStretchAllColumns(false);
        tl.setColumnStretchable(2, true);

        addRows(activity);

        return layout;
    }

    /**
     * Add a row to the TableLayout
     * 
     * @param context Android context
     */
    private void addRows(@NonNull Context context) {
        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(2, 0, 2, 0);

        visibleId = ThemeUtils.getResIdFromAttribute(context, R.attr.layer_visible);
        invisibleId = ThemeUtils.getResIdFromAttribute(context, R.attr.layer_not_visible);
        zoomToExtentId = ThemeUtils.getResIdFromAttribute(context, R.attr.zoom_to_layer_extent);
        menuId = ThemeUtils.getResIdFromAttribute(context, R.attr.more_small);
        List<MapViewLayer> layers = App.getLogic().getMap().getLayers();
        Collections.reverse(layers);
        for (MapViewLayer layer : layers) {
            if (layer.isEnabled()) {
                tl.addView(createRow(context, layer, tp));
                tl.addView(divider(context));
            }
        }
    }

    /**
     * Create a row in the dialog for a specific layer
     * 
     * @param context Android context
     * @param layer the MapViewLayer
     * @param tp LayoutParams for this row
     * @return a TableRow
     */
    @NonNull
    TableRow createRow(@NonNull Context context, @NonNull final MapViewLayer layer, @NonNull TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        final ImageButton visible = new ImageButton(context);
        String name = null;
        boolean isVisible = false;
        name = layer.getName();
        isVisible = layer.isVisible();
        visible.setImageResource(isVisible ? visibleId : invisibleId);
        visible.setBackgroundColor(Color.TRANSPARENT);
        // visible.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        visible.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (layer != null) {
                    layer.setVisible(!layer.isVisible());
                    visible.setImageResource(layer.isVisible() ? visibleId : invisibleId);
                    layer.invalidate();
                }
            }
        });
        tr.addView(visible);

        if (layer != null && layer instanceof ExtentInterface) {
            final ImageButton zoomToExtent = new ImageButton(context);
            zoomToExtent.setImageResource(zoomToExtentId);
            zoomToExtent.setBackgroundColor(Color.TRANSPARENT);
            zoomToExtent.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (layer != null) {
                        dismissDialog();
                        Logic logic = App.getLogic();
                        Map map = logic.getMap();
                        BoundingBox extent = ((ExtentInterface) layer).getExtent();
                        if (extent == null) {
                            extent = ViewBox.getMaxMercatorExtent();
                        }
                        map.getViewBox().fitToBoundingBox(map, extent);
                        if (getActivity() instanceof Main) {
                            ((Main) getActivity()).setFollowGPS(false);
                        }
                        logic.updateStyle();
                        layer.setVisible(true);
                        visible.setImageResource(visibleId);
                        map.invalidate();
                    }
                }
            });
            tr.addView(zoomToExtent);
        } else {
            tr.addView(new View(context));
        }
        TextView cell = new TextView(context);
        cell = new TextView(context);
        if (name != null) {
            cell.setText(name);
            cell.setMinEms(2);
            cell.setHorizontallyScrolling(true);
            cell.setSingleLine(true);
            cell.setEllipsize(TextUtils.TruncateAt.END);
            cell.setPadding(5, 0, 0, 0);
            tr.addView(cell);
        }
        if (layer != null && needsMenu(layer)) {
            final ImageButton menu = new ImageButton(context);
            menu.setImageResource(menuId);
            menu.setBackgroundColor(Color.TRANSPARENT);
            menu.setOnClickListener(new LayerMenuListener(menu, layer));
            tr.addView(menu);
            menu.setTag(tr);
        } else {
            tr.addView(new View(context));
        }
        tr.setGravity(Gravity.CENTER_VERTICAL);
        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Check if we should show a menu for the layer
     * 
     * @param layer the layer to check
     * @return true if we should show a menu button
     */
    private boolean needsMenu(final MapViewLayer layer) {
        return !(layer instanceof de.blau.android.layer.data.MapOverlay);
    }

    /**
     * Create a divider View to be added to a TableLAyout
     * 
     * @param context Android context
     * @return a thin TableRow
     */
    public static TableRow divider(@NonNull Context context) {
        TableRow tr = new TableRow(context);
        View v = new View(context);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1);
        trp.span = 4;
        v.setLayoutParams(trp);
        v.setBackgroundColor(Color.rgb(204, 204, 204));
        tr.addView(v);
        return tr;
    }

    class LayerMenuListener implements View.OnClickListener {

        final MapViewLayer layer;
        final View         button;

        /**
         * Construct a new listener for the layer menu button
         * 
         * @param button the menu button
         * @param layer the layer
         */
        LayerMenuListener(@NonNull View button, @NonNull MapViewLayer layer) {
            this.button = button;
            this.layer = layer;
        }

        @Override
        public void onClick(View arg0) {
            PopupMenu popup = new PopupMenu(getActivity(), button);

            if (layer instanceof MapTilesLayer) { // maybe we should use an interface here
                // get MRU list from layer
                final String[] tileServerIds = ((MapTilesLayer) layer).getMRU();
                for (int i = 0; i < tileServerIds.length; i++) {
                    final String id = tileServerIds[i];
                    final String currentServerId = ((MapTilesLayer) layer).getTileLayerConfiguration().getId();
                    if (!currentServerId.equals(id)) {
                        final TileLayerServer tileServer = TileLayerServer.get(getActivity(), id, true);
                        if (tileServer != null) {
                            MenuItem item = popup.getMenu().add(tileServer.getName());
                            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    if (tileServer != null) {
                                        ((MapTilesLayer) layer).setRendererInfo(tileServer);
                                        Preferences prefs = new Preferences(getContext());
                                        if (tileServer.isOverlay()) {
                                            prefs.setOverlayLayer(id);
                                        } else {
                                            prefs.setBackGroundLayer(id);
                                        }
                                        App.getDelegator().setImageryRecorded(false);
                                        TableRow row = (TableRow) button.getTag();
                                        TextView name = (TextView) row.getChildAt(2);
                                        name.setText(tileServer.getName());
                                        dismissDialog();
                                        layer.invalidate();
                                    }
                                    return true;
                                }
                            });
                        }
                    }
                    if (i == tileServerIds.length - 1) {
                        MenuItem divider = popup.getMenu().add("");
                        divider.setEnabled(false);
                    }
                }
                MenuItem item = popup.getMenu().add(R.string.layer_flush_tile_cache);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            ((MapTilesLayer) layer).flushTileCache(getActivity());
                            layer.invalidate();
                        }
                        return true;
                    }
                });
                item = popup.getMenu().add(R.string.menu_tools_background_properties);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            BackgroundProperties.showDialog(getActivity(), layer.getIndex());
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof StyleableLayer) {
                MenuItem item = popup.getMenu().add(R.string.layer_change_style);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            LayerStyle.showDialog(getActivity(), layer.getIndex());
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof DisableInterface) {
                MenuItem item = popup.getMenu().add(R.string.disable);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            Context context = getContext();
                            ((DisableInterface) layer).disable(context);
                            App.getLogic().getMap().setPrefs(context, new Preferences(context));
                            tl.removeAllViews();
                            addRows(context);
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof DiscardInterface) {
                MenuItem item = popup.getMenu().add(R.string.discard);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            ((DiscardInterface) layer).discard(getContext());
                            TableRow row = (TableRow) button.getTag();
                            tl.removeView(row);
                        }
                        return true;
                    }
                });
            }
            popup.show();
        }
    }
}
