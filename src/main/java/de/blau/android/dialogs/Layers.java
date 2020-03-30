package de.blau.android.dialogs;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DisableInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.OAMCatalogView;
import de.blau.android.resources.TileLayerDialog;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.resources.TileLayerServer.Category;
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
        AppCompatDialog dialog = new AppCompatDialog(getActivity());
        View layout = createView(null);
        // ideally the following code would be included in the layer classes, but no brilliant ideas on how to do this
        // right now
        final FloatingActionButton add = (FloatingActionButton) layout.findViewById(R.id.add);
        add.setOnClickListener(new OnClickListener() {

            /**
             * Update the dialog and set the prefs
             * 
             * @param activity calling FragmentActivity
             * @param prefs Preference instance to set
             */
            private void updateDialogAndPrefs(final FragmentActivity activity, final Preferences prefs) {
                setPrefs(activity, prefs);
                tl.removeAllViews();
                addRows(activity);
            }

            @Override
            public void onClick(View v) {
                final FragmentActivity activity = getActivity();
                final Preferences prefs = App.getLogic().getPrefs();
                PopupMenu popup = new PopupMenu(getActivity(), add);
                // menu items for adding layers
                MenuItem item = popup.getMenu().add(R.string.menu_layers_load_geojson);
                final Map map = App.getLogic().getMap();
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem arg0) {
                        SelectFile.read(activity, R.string.config_osmPreferredDir_key, new ReadFile() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public boolean read(Uri fileUri) {
                                de.blau.android.layer.geojson.MapOverlay geojsonLayer = map.getGeojsonLayer();
                                if (geojsonLayer != null) {
                                    try {
                                        geojsonLayer.resetStyling();
                                        if (geojsonLayer.loadGeoJsonFile(activity, fileUri)) {
                                            SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                                            geojsonLayer.invalidate();
                                            LayerStyle.showDialog(activity, geojsonLayer.getIndex());
                                            tl.removeAllViews();
                                            addRows(activity);
                                        }
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
                if (map.getBackgroundLayer() == null) {
                    item = popup.getMenu().add(R.string.menu_layers_add_backgroundlayer);
                    item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem arg0) {
                            buildImagerySelectDialog(null, null, false).show();
                            Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                            return true;
                        }
                    });
                }
                if (map.getOverlayLayer() == null || !Map.activeOverlay(prefs.overlayLayer())) {
                    item = popup.getMenu().add(R.string.menu_layers_add_overlaylayer);
                    item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem arg0) {
                            buildImagerySelectDialog(null, null, true).show();
                            Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                            return true;
                        }
                    });
                }
                if (!prefs.areBugsEnabled()) {
                    item = popup.getMenu().add(R.string.menu_layers_add_tasklayer);
                    item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem arg0) {
                            prefs.setBugsEnabled(true);
                            updateDialogAndPrefs(activity, prefs);
                            return true;
                        }
                    });
                }
                if (!prefs.isPhotoLayerEnabled()) {
                    item = popup.getMenu().add(R.string.menu_layers_add_photolayer);
                    item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem arg0) {
                            prefs.setPhotoLayerEnabled(true);
                            updateDialogAndPrefs(activity, prefs);
                            return true;
                        }
                    });
                }
                String[] scaleValues = activity.getResources().getStringArray(R.array.scale_values);
                if (scaleValues != null && scaleValues.length > 0) {
                    if (prefs.scaleLayer().equals(scaleValues[0])) {
                        item = popup.getMenu().add(R.string.menu_layers_add_grid);
                        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem arg0) {
                                prefs.setScaleLayer(scaleValues[1]);
                                updateDialogAndPrefs(activity, prefs);
                                return true;
                            }
                        });
                    }
                }
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
        visible.setPadding(0, 0, Density.dpToPx(context, 5), 0);
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

        if (layer instanceof ExtentInterface) {
            final ImageButton zoomToExtent = new ImageButton(context);
            zoomToExtent.setImageResource(zoomToExtentId);
            zoomToExtent.setBackgroundColor(Color.TRANSPARENT);
            zoomToExtent.setPadding(Density.dpToPx(context, 5), 0, Density.dpToPx(context, 5), 0);
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

            if (layer instanceof MapTilesLayer) { // maybe we should use an interface here
                // get MRU list from layer
                final String[] tileServerIds = ((MapTilesLayer) layer).getMRU();
                for (int i = 0; i < tileServerIds.length; i++) {
                    final String id = tileServerIds[i];
                    final String currentServerId = ((MapTilesLayer) layer).getTileLayerConfiguration().getId();
                    if (!currentServerId.equals(id)) {
                        final TileLayerServer tileServer = TileLayerServer.get(activity, id, true);
                        if (tileServer != null) {
                            MenuItem item = menu.add(tileServer.getName());
                            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    if (tileServer != null) {
                                        TableRow row = (TableRow) button.getTag();
                                        setNewImagery(activity, row, (MapTilesLayer) layer, tileServer);
                                        dismissDialog();
                                        layer.invalidate();
                                    }
                                    return true;
                                }
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
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            buildImagerySelectDialog((TableRow) button.getTag(), (MapTilesLayer) layer, layer instanceof MapTilesOverlayLayer).show();
                            Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                        }
                        return true;
                    }
                });

                if (!(layer instanceof MapTilesOverlayLayer)) {
                    item = menu.add(R.string.menu_tools_add_imagery_from_oam);
                    item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            OAMCatalogView.queryAndSelectLayers(getActivity(), activity instanceof Main ? ((Main) activity).getMap().getViewBox() : null,
                                    new TileLayerDialog.OnUpdateListener() {
                                        @Override
                                        public void update() {
                                            if (layer != null) {
                                                layer.invalidate();
                                                dismissDialog();
                                            }
                                        }
                                    });
                            return true;
                        }
                    });
                }

                item = menu.add(R.string.layer_flush_tile_cache);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            ((MapTilesLayer) layer).flushTileCache(activity);
                            layer.invalidate();
                        }
                        return true;
                    }
                });
                item = menu.add(R.string.menu_tools_background_properties);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            BackgroundProperties.showDialog(activity, layer.getIndex());
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof ConfigureInterface && ((ConfigureInterface) layer).enableConfiguration()) {
                MenuItem item = menu.add(R.string.menu_layers_configure);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            ((ConfigureInterface) layer).configure(activity);
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof StyleableLayer) {
                MenuItem item = menu.add(R.string.layer_change_style);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            LayerStyle.showDialog(activity, layer.getIndex());
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof LayerInfoInterface) {
                MenuItem item = menu.add(R.string.menu_information);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            ((LayerInfoInterface) layer).showInfo(activity);
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof DisableInterface) {
                MenuItem item = menu.add(R.string.disable);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            Context context = getContext();
                            ((DisableInterface) layer).disable(context);
                            setPrefs(activity, new Preferences(context));
                            tl.removeAllViews();
                            addRows(context);
                            App.getLogic().getMap().invalidate();
                        }
                        return true;
                    }
                });
            }
            if (layer instanceof DiscardInterface) {
                MenuItem item = menu.add(R.string.discard);
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
            if (layer instanceof PruneableInterface) {
                MenuItem item = menu.add(R.string.prune);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (layer != null) {
                            ((PruneableInterface) layer).prune();
                        }
                        return true;
                    }
                });
            }
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

        RadioGroup valueGroup = (RadioGroup) layout.findViewById(R.id.valueGroup);

        builder.setView(layout);

        ViewBox viewBox = App.getLogic().getMap().getViewBox();
        builder.setTitle(isOverlay ? R.string.config_overlayLayer_title : R.string.config_backgroundLayer_title);
        final String[] ids = isOverlay ? TileLayerServer.getOverlayIds(viewBox, true, overlayCategory)
                : TileLayerServer.getIds(viewBox, true, prefs.getBackgroundCategory());

        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();

        addButtons(activity, dialog, row, layer, isOverlay, valueGroup, ids);

        categoryGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Category category = checkedId >= 0 ? (Category) group.findViewById(checkedId).getTag() : null;
                valueGroup.removeAllViews();
                final String[] ids = isOverlay ? TileLayerServer.getOverlayIds(viewBox, true, category) : TileLayerServer.getIds(viewBox, true, category);
                if (isOverlay) {
                    prefs.setOverlayCategory(category);
                } else {
                    prefs.setBackgroundCategory(category);
                }
                addButtons(activity, dialog, row, layer, isOverlay, valueGroup, ids);
            }
        });

        return dialog;
    }

    /**
     * Add the per layer RadioButtons
     * 
     * @param activity calling Activity
     * @param dialog the Dialog
     * @param row the row in the layers dialog or null
     * @param layer the current layer
     * @param isOverlay true if an overlay
     * @param valueGroup the RadioGroup
     * @param ids the list of layer ids
     */
    private void addButtons(@NonNull FragmentActivity activity, @NonNull Dialog dialog, @Nullable final TableRow row, @Nullable final MapTilesLayer layer,
            boolean isOverlay, @NonNull RadioGroup valueGroup, @NonNull final String[] ids) {
        LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;
        String[] names = isOverlay ? TileLayerServer.getOverlayNames(ids) : TileLayerServer.getNames(ids);
        String currentId = layer == null ? TileLayerServer.LAYER_NONE : layer.getTileLayerConfiguration().getId();
        valueGroup.setOnCheckedChangeListener(null); // remove any existing listener
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            final AppCompatRadioButton button = new AppCompatRadioButton(activity);
            button.setText(names[i]);
            button.setTag(id);
            button.setChecked(id.equals(currentId));
            button.setLayoutParams(buttonLayoutParams);
            button.setId(i);
            valueGroup.addView(button);
        }
        final Handler handler = new Handler();
        valueGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != -1) {
                    final TileLayerServer tileServer = TileLayerServer.get(getActivity(), ids[checkedId], true);
                    if (tileServer != null) {
                        setNewImagery(activity, row, layer, tileServer);
                        if (layer != null) {
                            try {
                                layer.onSaveState(activity);
                            } catch (IOException e) {
                                Log.e(DEBUG_TAG, "save of imagery layer state failed");
                            }
                            layer.invalidate();
                        } else {
                            App.getLogic().getMap().invalidate();
                        }
                    }
                }
                // allow a tiny bit of time to see that the action actually worked
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss(); // dismiss this
                        dismissDialog(); // and then the caller
                    }
                }, 100);
            }
        });
    }

    /**
     * Change the imagery for a tile layer
     * 
     * @param activity the calling activity
     * @param row the TableRow with the information, if null we will only set the prefs
     * @param layer the layer, if null we will only set the prefs
     * @param tileServer the new tileserver to use
     */
    private void setNewImagery(FragmentActivity activity, @Nullable TableRow row, @Nullable MapTilesLayer layer, @NonNull final TileLayerServer tileServer) {
        Preferences prefs = new Preferences(getContext());
        if (tileServer.isOverlay()) {
            prefs.setOverlayLayer(tileServer.getId());
        } else {
            prefs.setBackGroundLayer(tileServer.getId());
        }
        App.getDelegator().setImageryRecorded(false);
        if (row != null && layer != null) {
            TextView name = (TextView) row.getChildAt(2);
            name.setText(tileServer.getName());
            layer.setRendererInfo(tileServer);
        }
        if (activity instanceof Main) {
            ((Main) activity).invalidateOptionsMenu();
        }
        setPrefs(activity, prefs);
    }

    /**
     * Set the Preference instance in Main and Map
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
