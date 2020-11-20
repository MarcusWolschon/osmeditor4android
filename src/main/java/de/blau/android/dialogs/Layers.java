package de.blau.android.dialogs;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerConfig;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.OAMCatalogView;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.WmsEndpointDatabaseView;
import de.blau.android.util.Density;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SelectFile;
import de.blau.android.util.SizedFixedImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

/**
 * Layer dialog
 * 
 * @author Simon Poole
 *
 */
public class Layers extends SizedFixedImmersiveDialogFragment {

    private static final int VERTICAL_OFFSET = 64;

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
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            Layers layersFragment = newInstance();
            layersFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of the Layers dialog
     * 
     * @return an instance of the Layers dialog
     */
    @NonNull
    private static Layers newInstance() {
        Layers f = new Layers();

        Bundle args = new Bundle();

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        // potentially the imagery layer lists don't exist yet, force create them now
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                TileLayerSource.get(getContext(), TileLayerSource.LAYER_MAPNIK, true);
                return null;
            }
        }.execute();

        AppCompatDialog dialog = new AppCompatDialog(getActivity());
        View layout = createView(null);
        // ideally the following code would be included in the layer classes, but no brilliant ideas on how to do this
        // right now
        final FloatingActionButton add = (FloatingActionButton) layout.findViewById(R.id.add);
        add.setOnClickListener(v -> {
            final FragmentActivity activity = getActivity();
            final Preferences prefs = App.getLogic().getPrefs();
            PopupMenu popup = new PopupMenu(getActivity(), add);
            // menu items for adding layers
            MenuItem item = popup.getMenu().add(R.string.menu_layers_load_geojson);
            final Map map = App.getLogic().getMap();
            item.setOnMenuItemClickListener(unused -> {
                SelectFile.read(activity, R.string.config_osmPreferredDir_key, new ReadFile() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean read(Uri fileUri) {
                        de.blau.android.layer.geojson.MapOverlay geojsonLayer = (de.blau.android.layer.geojson.MapOverlay) map.getLayer(LayerType.GEOJSON,
                                fileUri.toString());
                        if (geojsonLayer == null) {
                            de.blau.android.layer.Util.addLayer(activity, LayerType.GEOJSON, fileUri.toString());
                            map.setUpLayers(activity);
                            geojsonLayer = (de.blau.android.layer.geojson.MapOverlay) map.getLayer(LayerType.GEOJSON, fileUri.toString());
                        }
                        if (geojsonLayer != null) { // if null setUpLayers will have toasted
                            geojsonLayer.resetStyling();
                            LayerStyle.showDialog(activity, geojsonLayer.getIndex());
                            SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                            geojsonLayer.invalidate();
                            tl.removeAllViews();
                            addRows(activity);
                        }
                        return true;
                    }
                });
                return false;
            });

            item = popup.getMenu().add(R.string.menu_layers_add_backgroundlayer);
            item.setOnMenuItemClickListener(unused -> {
                buildImagerySelectDialog(null, null, false).show();
                Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                return true;
            });

            item = popup.getMenu().add(R.string.menu_layers_add_overlaylayer);
            item.setOnMenuItemClickListener(unused -> {
                buildImagerySelectDialog(null, null, true).show();
                Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                return true;
            });

            if (map.getTaskLayer() == null) {
                item = popup.getMenu().add(R.string.menu_layers_add_tasklayer);
                item.setOnMenuItemClickListener(unused -> {
                    de.blau.android.layer.Util.addLayer(activity, LayerType.TASKS);
                    updateDialogAndPrefs(activity, prefs, map);
                    return true;
                });
            }

            if (map.getPhotoLayer() == null) {
                item = popup.getMenu().add(R.string.menu_layers_add_photolayer);
                item.setOnMenuItemClickListener(unused -> {
                    de.blau.android.layer.Util.addLayer(activity, LayerType.PHOTO);
                    updateDialogAndPrefs(activity, prefs, map);
                    return true;
                });
            }

            if (map.getLayer(LayerType.SCALE) == null) {
                String[] scaleValues = activity.getResources().getStringArray(R.array.scale_values);
                if (scaleValues != null && scaleValues.length > 0) {
                    item = popup.getMenu().add(R.string.menu_layers_add_grid);
                    item.setOnMenuItemClickListener(unused -> {
                        de.blau.android.layer.Util.addLayer(activity, LayerType.SCALE);
                        prefs.setScaleLayer(scaleValues[1]);
                        updateDialogAndPrefs(activity, prefs, map);
                        return true;
                    });
                }
            }

            if (map.getLayer(LayerType.MAPILLARY) == null) {
                try (KeyDatabaseHelper keys = new KeyDatabaseHelper(activity); SQLiteDatabase db = keys.getReadableDatabase()) {
                    if (KeyDatabaseHelper.getKey(db, de.blau.android.layer.mapillary.MapOverlay.APIKEY_KEY) != null) {
                        item = popup.getMenu().add(R.string.menu_layers_enable_mapillary_layer);
                        item.setOnMenuItemClickListener(unused -> {
                            de.blau.android.layer.Util.addLayer(activity, LayerType.MAPILLARY);
                            updateDialogAndPrefs(activity, prefs, map);
                            return true;
                        });
                    }
                }
            }

            item = popup.getMenu().add(R.string.menu_tools_add_imagery_from_oam);
            item.setOnMenuItemClickListener(unused -> {
                OAMCatalogView.queryAndSelectLayers(getActivity(), activity instanceof Main ? ((Main) activity).getMap().getViewBox() : null,
                        () -> updateDialogAndPrefs(activity, prefs, map));
                return true;
            });

            item = popup.getMenu().add(R.string.add_imagery_from_wms_endpoint);
            item.setOnMenuItemClickListener(unused -> {
                WmsEndpointDatabaseView ui = new WmsEndpointDatabaseView();
                ui.manageEndpoints(getActivity(), () -> updateDialogAndPrefs(activity, prefs, map));
                return true;
            });

            popup.show();
        });

        Button done = (Button) layout.findViewById(R.id.done);
        done.setOnClickListener(v -> dismissDialog());

        dialog.setContentView(layout);
        ViewCompat.setClipBounds(layout, null);
        // Android 9 clips the popupmenus just above the dialog
        // moving it to the top forces the menu to the bottom which
        // which works
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        wlp.y = Density.dpToPx(getContext(), VERTICAL_OFFSET); // 64 is rather random
        window.setAttributes(wlp);

        return dialog;
    }

    /**
     * Update the dialog and set the prefs
     * 
     * @param activity calling FragmentActivity
     * @param prefs Preference instance to set
     * @param map the curren Map instance
     */
    private void updateDialogAndPrefs(@NonNull final FragmentActivity activity, @NonNull final Preferences prefs, @NonNull final Map map) {
        setPrefs(activity, prefs);
        tl.removeAllViews();
        addRows(activity);
        map.invalidate();
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(container);
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * @param container parent view or null
     * @return the View
     */
    private View createView(@Nullable ViewGroup container) {
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

    @Override
    public void onResume() {
        super.onResume();
        // this will initially be done twice
        // but doing it here allows to update a
        // after configuration changes painlessly
        tl.removeAllViews();
        addRows(getActivity());
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
            tl.addView(createRow(context, layer, tp));
            tl.addView(divider(context));
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
        name = layer.getName();
        boolean isVisible = layer.isVisible();
        visible.setImageResource(isVisible ? visibleId : invisibleId);
        visible.setBackgroundColor(Color.TRANSPARENT);
        visible.setPadding(0, 0, Density.dpToPx(context, 5), 0);
        visible.setOnClickListener(v -> {
            if (layer != null) {
                setVisibility(context, layer, !layer.isVisible());
                visible.setImageResource(layer.isVisible() ? visibleId : invisibleId);
                layer.invalidate();
            }
        });
        tr.addView(visible);

        if (layer instanceof ExtentInterface) {
            final ImageButton zoomToExtent = new ImageButton(context);
            zoomToExtent.setImageResource(zoomToExtentId);
            zoomToExtent.setBackgroundColor(Color.TRANSPARENT);
            zoomToExtent.setPadding(Density.dpToPx(context, 5), 0, Density.dpToPx(context, 5), 0);
            zoomToExtent.setOnClickListener(v -> {
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
                    setVisibility(context, layer, true);
                    visible.setImageResource(visibleId);
                    map.invalidate();
                }
            });
            tr.addView(zoomToExtent);
        } else {
            tr.addView(new View(context));
        }
        TextView cell = new TextView(context);
        cell.setText(name);
        cell.setMinEms(2);
        cell.setHorizontallyScrolling(true);
        cell.setSingleLine(true);
        cell.setEllipsize(TextUtils.TruncateAt.END);
        cell.setPadding(Density.dpToPx(context, 5), 0, Density.dpToPx(context, 5), 0);
        tr.addView(cell);
        final ImageButton menu = new ImageButton(context);
        menu.setImageResource(menuId);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setOnClickListener(new LayerMenuListener(menu, layer));
        cell.setOnClickListener(new LayerMenuListener(menu, layer));
        tr.addView(menu);
        menu.setTag(tr);
        tr.setGravity(Gravity.CENTER_VERTICAL);
        tr.setLayoutParams(tp);
        return tr;
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
            final FragmentActivity activity = getActivity();
            PopupMenu popup = new PopupMenu(activity, button);
            Menu menu = popup.getMenu();
            final Map map = App.getLogic().getMap();

            if (layer instanceof MapTilesLayer) { // maybe we should use an interface here
                // get MRU list from layer
                final String[] tileServerIds = ((MapTilesLayer) layer).getMRU();
                for (int i = 0; i < tileServerIds.length; i++) {
                    final String id = tileServerIds[i];
                    final String currentServerId = ((MapTilesLayer) layer).getTileLayerConfiguration().getId();
                    if (!currentServerId.equals(id)) {
                        final TileLayerSource tileServer = TileLayerSource.get(activity, id, true);
                        if (tileServer != null) {
                            MenuItem item = menu.add(tileServer.getName());
                            item.setOnMenuItemClickListener(unused -> {
                                if (tileServer != null) {
                                    TableRow row = (TableRow) button.getTag();
                                    setNewImagery(activity, row, (MapTilesLayer) layer, tileServer);
                                    dismissDialog();
                                    layer.invalidate();
                                }
                                return true;
                            });
                        } else {
                            ((MapTilesLayer) layer).removeServerFromMRU(id);
                        }
                    }
                    if (i == tileServerIds.length - 1) {
                        MenuItem divider = menu.add("");
                        divider.setEnabled(false);
                    }
                }

                MenuItem item = menu.add(R.string.layer_select_imagery);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        buildImagerySelectDialog((TableRow) button.getTag(), (MapTilesLayer) layer, layer instanceof MapTilesOverlayLayer).show();
                        Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                    }
                    return true;
                });
            }

            if (layer instanceof ConfigureInterface && ((ConfigureInterface) layer).enableConfiguration()) {
                MenuItem item = menu.add(R.string.menu_layers_configure);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        ((ConfigureInterface) layer).configure(activity);
                    }
                    return true;
                });
            }

            if (layer instanceof StyleableLayer) {
                MenuItem item = menu.add(R.string.layer_change_style);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        LayerStyle.showDialog(activity, layer.getIndex());
                    }
                    return true;
                });
            }

            if (layer instanceof LayerInfoInterface) {
                MenuItem item = menu.add(R.string.menu_information);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        ((LayerInfoInterface) layer).showInfo(activity);
                    }
                    return true;
                });
            }

            if (layer instanceof DiscardInterface) {
                MenuItem item = menu.add(R.string.discard);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                            db.deleteLayer(layer.getIndex(), layer.getType());
                            map.setUpLayers(activity);
                            map.invalidate();
                            ((DiscardInterface) layer).discard(getContext());
                            updateDialogAndPrefs(activity, App.getLogic().getPrefs(), map);
                        }
                    }
                    return true;
                });
            }

            if (layer instanceof PruneableInterface) {
                MenuItem item = menu.add(R.string.prune);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected void onPreExecute() {
                                Progress.showDialog(activity, Progress.PROGRESS_PRUNING);
                            }

                            @Override
                            protected Void doInBackground(Void... arg) {
                                ((PruneableInterface) layer).prune();
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                Progress.dismissDialog(activity, Progress.PROGRESS_PRUNING);
                            }
                        }.execute();
                    }
                    return true;
                });
            }

            if (layer instanceof MapTilesLayer) { // these items are less important, show them at the bottom of the menu
                MenuItem item = menu.add(R.string.layer_flush_tile_cache);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        ((MapTilesLayer) layer).flushTileCache(activity);
                        layer.invalidate();
                    }
                    return true;
                });

                item = menu.add(R.string.menu_tools_background_properties);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        BackgroundProperties.showDialog(activity, layer.getIndex());
                    }
                    return true;
                });
            }
            MenuItem item = menu.add(R.string.move_up);
            item.setOnMenuItemClickListener(unused -> {
                if (layer != null) {
                    try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                        db.moveLayer(layer.getIndex(), Math.min(layer.getIndex() + 1, db.layerCount() - 1));
                        updateDialogAndPrefs(activity, App.getLogic().getPrefs(), map);
                        map.invalidate();
                    }
                }
                return true;
            });
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                item.setEnabled(layer.getIndex() < db.layerCount() - 1);
            }
            item = menu.add(R.string.move_down);
            item.setOnMenuItemClickListener(unused -> {
                if (layer != null) {
                    try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                        db.moveLayer(layer.getIndex(), Math.max(layer.getIndex() - 1, 0));
                        updateDialogAndPrefs(activity, App.getLogic().getPrefs(), map);
                        map.invalidate();
                    }
                }
                return true;
            });
            item.setEnabled(layer.getIndex() > 0);
            popup.show();
        }
    }

    /**
     * Build a dialog that shows a selection of imagery sources that can be used
     * 
     * @param row the TableRow we were invoked from, can be null if row doesn't exists
     * @param layer the layer we should change imagery for, can be null if layer doesn't exist yet
     * @param isOverlay true if this is for the overlay layer
     * @return an AlertDialog that can be shown
     */
    private AlertDialog buildImagerySelectDialog(@Nullable final TableRow row, @Nullable final MapTilesLayer layer, boolean isOverlay) {
        final FragmentActivity activity = getActivity();
        final Preferences prefs = App.getLogic().getPrefs();

        Builder builder = new AlertDialog.Builder(activity);

        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(getActivity());

        final View layout = themedInflater.inflate(R.layout.layer_selection_dialog, null);
        RadioGroup categoryGroup = (RadioGroup) layout.findViewById(R.id.categoryGroup);
        AppCompatRadioButton allButton = categoryGroup.findViewById(R.id.categoryAll);
        AppCompatRadioButton photoButton = categoryGroup.findViewById(R.id.categoryPhoto);

        photoButton.setTag(Category.photo);
        photoButton.setVisibility(!isOverlay ? View.VISIBLE : View.GONE);
        AppCompatRadioButton qaButton = categoryGroup.findViewById(R.id.categoryQA);

        qaButton.setTag(Category.qa);
        qaButton.setVisibility(isOverlay ? View.VISIBLE : View.GONE);

        Category backgroundCategory = prefs.getBackgroundCategory();
        Category overlayCategory = prefs.getOverlayCategory();

        if (!isOverlay && Category.photo == backgroundCategory) {
            photoButton.setChecked(true);
        } else if (isOverlay && Category.qa == overlayCategory) {
            qaButton.setChecked(true);
        } else {
            allButton.setChecked(true);
        }

        builder.setView(layout);
        builder.setTitle(isOverlay ? R.string.config_overlayLayer_title : R.string.config_backgroundLayer_title);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();

        ViewBox viewBox = App.getLogic().getMap().getViewBox();
        final String[] ids = isOverlay ? TileLayerSource.getOverlayIds(viewBox, true, overlayCategory)
                : TileLayerSource.getIds(viewBox, true, backgroundCategory);

        RecyclerView imageryList = (RecyclerView) layout.findViewById(R.id.imageryList);
        LayoutParams buttonLayoutParams = imageryList.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        String currentId = layer == null ? TileLayerSource.LAYER_NONE : layer.getTileLayerConfiguration().getId();

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        imageryList.setLayoutManager(layoutManager);

        final Handler handler = new Handler();
        OnCheckedChangeListener onCheckedChangeListener = (group, position) -> {
            if (position != -1 && position < ids.length) {
                final TileLayerSource tileServer = TileLayerSource.get(getActivity(), ids[position], true);
                if (tileServer != null) {
                    setNewImagery(activity, row, layer, tileServer);
                }
            } else {
                Log.e(DEBUG_TAG, "position out of range 0-" + (ids.length - 1) + ": " + position);
            }
            // allow a tiny bit of time to see that the action actually worked
            handler.postDelayed(() -> {
                dialog.dismiss(); // dismiss this
                dismissDialog(); // and then the caller
            }, 100);
        };

        ImageryListAdapter adapter = new ImageryListAdapter(ids, currentId, isOverlay, buttonLayoutParams, onCheckedChangeListener);
        imageryList.setAdapter(adapter);

        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Category category = checkedId >= 0 ? (Category) group.findViewById(checkedId).getTag() : null;
            imageryList.removeAllViews();
            final String[] idsForButtons = isOverlay ? TileLayerSource.getOverlayIds(viewBox, true, category) : TileLayerSource.getIds(viewBox, true, category);
            if (isOverlay) {
                prefs.setOverlayCategory(category);
            } else {
                prefs.setBackgroundCategory(category);
            }
            adapter.setIds(idsForButtons, isOverlay, true);
        });

        return dialog;
    }

    /**
     * Change the imagery for a tile layer
     * 
     * @param activity the calling activity
     * @param row the TableRow with the information, if null we will only set the prefs
     * @param layer the layer, if null we will only set the prefs
     * @param tileServer the new tileserver to use, if null use the prefs
     */
    private void setNewImagery(@NonNull FragmentActivity activity, @Nullable TableRow row, @Nullable MapTilesLayer layer,
            @Nullable TileLayerSource tileServer) {
        Preferences prefs = App.getLogic().getPrefs();
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
            if (layer != null) {
                if (tileServer != null) {
                    db.setLayerContentId(layer.getIndex(), tileServer.getId());
                } else {
                    LayerConfig[] layerConfigs = db.getLayers();
                    if (layer.getIndex() < layerConfigs.length) {
                        tileServer = TileLayerSource.get(activity, layerConfigs[layer.getIndex()].getContentId(), true);
                    }
                }
                if (tileServer != null) {
                    App.getDelegator().setImageryRecorded(false);
                    if (row != null) {
                        TextView name = (TextView) row.getChildAt(2);
                        name.setText(tileServer.getName());
                        layer.setRendererInfo(tileServer);
                    }
                    try {
                        layer.onSaveState(activity);
                    } catch (IOException e) {
                        Log.e(DEBUG_TAG, "setNewImagery save of imagery layer state failed");
                    }
                    layer.invalidate();
                } else {
                    Log.e(DEBUG_TAG, "setNewImagery tile source null");
                }
            } else if (tileServer != null) {
                LayerConfig[] layerConfigs = db.getLayers();
                // determine the position to insert the layer at,
                // essentially on top of the latest layer of the same type
                final boolean isOverlay = tileServer.isOverlay();
                int position = 0;
                for (LayerConfig config : layerConfigs) {
                    if (LayerType.IMAGERY.equals(config.getType()) && config.getPosition() >= position) {
                        position = config.getPosition() + 1;
                    }
                    if (isOverlay && LayerType.OVERLAYIMAGERY.equals(config.getType()) && config.getPosition() >= position) {
                        position = config.getPosition() + 1;
                    }
                }
                db.insertLayer(position, isOverlay ? LayerType.OVERLAYIMAGERY : LayerType.IMAGERY, true, tileServer.getId());
                App.getLogic().getMap().invalidate();
            } else {
                Log.e(DEBUG_TAG, "setNewImagery both layer and tile source null");
            }
            if (activity instanceof Main) {
                ((Main) activity).invalidateOptionsMenu();
            }
        }

        setPrefs(activity, prefs);
    }

    /**
     * Set layer visibility
     * 
     * @param context an Android Context
     * @param layer the layer
     * @param visible the value to set
     */
    private void setVisibility(@NonNull Context context, @NonNull MapViewLayer layer, boolean visible) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
            layer.setVisible(visible);
            db.setLayerVisibility(layer.getIndex(), visible);
        }
    }

    /**
     * Set the Preference instance in Main, Logic and Map
     * 
     * @param activity the calling FragmentActivity
     * @param prefs the new Preference object
     */
    private void setPrefs(@Nullable FragmentActivity activity, @NonNull Preferences prefs) {
        if (activity instanceof Main) {
            ((Main) activity).updatePrefs(prefs);
        }
        App.getLogic().getMap().setPrefs(getContext(), prefs);
    }
}
