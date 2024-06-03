package de.blau.android.dialogs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.gpx.Track;
import de.blau.android.gpx.TrackPoint;
import de.blau.android.gpx.WayPoint;
import de.blau.android.imageryoffset.ImageryAlignmentActionModeCallback;
import de.blau.android.layer.AbstractConfigurationDialog;
import de.blau.android.layer.ConfigureInterface;
import de.blau.android.layer.DiscardInterface;
import de.blau.android.layer.ExtentInterface;
import de.blau.android.layer.LayerConfig;
import de.blau.android.layer.LayerInfoInterface;
import de.blau.android.layer.LayerType;
import de.blau.android.layer.MapViewLayer;
import de.blau.android.layer.PruneableInterface;
import de.blau.android.layer.StyleableInterface;
import de.blau.android.layer.StyleableLayer;
import de.blau.android.layer.mvt.MapOverlay;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.GpxFile;
import de.blau.android.osm.OsmGpxApi;
import de.blau.android.osm.Server;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;
import de.blau.android.resources.OAMCatalogView;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.resources.TileLayerDialog;
import de.blau.android.resources.TileLayerDialog.OnUpdateListener;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.resources.WmsEndpointDatabaseView;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.Density;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SaveFile;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.mvt.style.Source;
import de.blau.android.util.mvt.style.Style;
import de.blau.android.views.layers.ImageryLayerInfo;
import de.blau.android.views.layers.MapTilesLayer;

/**
 * Layer dialog
 * 
 * @author Simon Poole
 *
 */
public class Layers extends AbstractConfigurationDialog implements OnUpdateListener {
    private static final String DEBUG_TAG = Layers.class.getSimpleName().substring(0, Math.min(23, Layers.class.getSimpleName().length()));

    private static final int  VERTICAL_OFFSET     = 64;
    private static final long MAX_STYLE_FILE_SIZE = 10000000L;

    public static final String TAG = "fragment_layers";

    private int visibleId;
    private int invisibleId;
    private int zoomToExtentId;
    private int menuId;

    private TableLayout tl;

    private OnUpdateListener updateListener;

    /**
     * Show dialog that allows to configure the layers
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        showDialog(activity, newInstance(), TAG);
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
        loadTileLayerSources();

        AppCompatDialog dialog = new AppCompatDialog(getActivity());
        View layout = createView(null);
        // ideally the following code would be included in the layer classes, but no brilliant ideas on how to do this
        // right now
        final FloatingActionButton add = (FloatingActionButton) layout.findViewById(R.id.add);
        add.setOnClickListener(v -> {
            final FragmentActivity activity = getActivity();
            final Preferences prefs = App.getLogic().getPrefs();
            PopupMenu popup = new PopupMenu(getActivity(), add);
            final Map map = App.getLogic().getMap();

            // menu items for adding layers
            MenuItem item = popup.getMenu().add(R.string.menu_layers_load_geojson);
            item.setOnMenuItemClickListener(unused -> {
                addStyleableLayerFromFile(activity, prefs, map, LayerType.GEOJSON);
                return false;
            });

            item = popup.getMenu().add(R.string.menu_layers_add_backgroundlayer);
            item.setOnMenuItemClickListener(unused -> {
                showImagerySelectDialog(null, null, false);
                Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                return true;
            });

            item = popup.getMenu().add(R.string.menu_layers_add_overlaylayer);
            item.setOnMenuItemClickListener(unused -> {
                showImagerySelectDialog(null, null, true);
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

            if (map.getBookmarksLayer() == null) {
                item = popup.getMenu().add(R.string.menu_layers_enable_bookmarkslayer);
                item.setOnMenuItemClickListener(unused -> {
                    de.blau.android.layer.Util.addLayer(activity, LayerType.BOOKMARKS);
                    updateDialogAndPrefs(activity, prefs, map);
                    return true;
                });
            }

            if (map.getLayer(LayerType.MAPILLARY) == null) {
                try (KeyDatabaseHelper keys = new KeyDatabaseHelper(activity); SQLiteDatabase db = keys.getReadableDatabase()) {
                    if (KeyDatabaseHelper.getKey(db, de.blau.android.layer.mapillary.MapillaryOverlay.APIKEY_KEY, EntryType.API_KEY) != null) {
                        item = popup.getMenu().add(R.string.menu_layers_enable_mapillary_layer);
                        item.setOnMenuItemClickListener(unused -> {
                            de.blau.android.layer.Util.addLayer(activity, LayerType.MAPILLARY);
                            updateDialogAndPrefs(activity, prefs, map);
                            Tip.showDialog(activity, R.string.tip_mapillary_privacy_key, R.string.tip_mapillary_privacy);
                            return true;
                        });
                    }
                }
            }

            item = popup.getMenu().add(R.string.layer_add_gpx);
            item.setOnMenuItemClickListener(unused -> {
                addStyleableLayerFromFile(activity, prefs, map, LayerType.GPX);
                return false;
            });

            item = popup.getMenu().add(R.string.layer_download_track);
            item.setOnMenuItemClickListener(unused -> {
                downloadGpxTrack(activity, prefs, map);
                return false;
            });

            item = popup.getMenu().add(R.string.layer_add_custom_imagery);
            item.setOnMenuItemClickListener(unused -> {
                TileLayerDialog.showDialog(this, null);
                return true;
            });

            item = popup.getMenu().add(R.string.layer_add_layer_from_mvt_style);
            item.setOnMenuItemClickListener(unused -> {
                addMVTLayerFromStyle(activity, prefs, map);
                return true;
            });

            item = popup.getMenu().add(R.string.menu_tools_add_imagery_from_oam);
            item.setOnMenuItemClickListener(unused -> {
                OAMCatalogView.showDialog(this, activity instanceof Main ? ((Main) activity).getMap().getViewBox() : null);
                return true;
            });

            item = popup.getMenu().add(R.string.add_imagery_from_wms_endpoint);
            item.setOnMenuItemClickListener(unused -> {
                WmsEndpointDatabaseView.showDialog(this);
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
     * Show a list of available tracks (starting point in the current view), then download on selection
     * 
     * @param activity the calling activity
     * @param prefs the current Preferences
     * @param map the current map object
     */
    private void downloadGpxTrack(@NonNull final FragmentActivity activity, @NonNull final Preferences prefs, @NonNull final Map map) {
        final Logic logic = App.getLogic();
        final Server server = prefs.getServer();
        ExecutorTask<Void, Void, List<GpxFile>> download = new ExecutorTask<Void, Void, List<GpxFile>>(logic.getExecutorService(), logic.getHandler()) {

            @Override
            protected List<GpxFile> doInBackground(Void input) throws Exception {
                return OsmGpxApi.getUserGpxFiles(server, map.getViewBox());
            }

            @Override
            protected void onPostExecute(List<GpxFile> result) {
                if (!result.isEmpty()) {
                    Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.layer_available_tracks);
                    builder.setAdapter(new GpxFileAdapter(activity, result), (DialogInterface dialog, int which) -> {
                        final long id = result.get(which).getId();
                        new ExecutorTask<Void, Void, Uri>(logic.getExecutorService(), logic.getHandler()) {

                            @Override
                            protected Uri doInBackground(Void input) throws Exception {
                                return OsmGpxApi.downloadTrack(prefs.getServer(), id,
                                        FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_GPX).getAbsolutePath(),
                                        result.get(which).getName());
                            }

                            @Override
                            protected void onPostExecute(Uri result) {
                                if (result != null) {
                                    addStyleableLayerFromUri(activity, prefs, map, LayerType.GPX, result, true);
                                }
                            }
                        }.execute();
                    });
                    builder.setPositiveButton(R.string.Done, null);
                    builder.show();
                } else {
                    Tip.showDialog(activity, R.string.tip_empty_gpx_download_key, R.string.tip_empty_gpx_download);
                    ScreenMessage.toastTopWarning(activity, R.string.toast_nothing_found);
                }
            }
        };
        if (Server.checkOsmAuthentication(activity, server, download::execute)) {
            download.execute();
        }
    }

    private class GpxFileAdapter extends ArrayAdapter<GpxFile> {

        /**
         * Get an adapter
         * 
         * @param context an Android Context
         * @param items a List of GpxFile
         */
        public GpxFileAdapter(@NonNull Context context, @NonNull List<GpxFile> items) {
            super(context, R.layout.track_list_item, items);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            LinearLayout ll = (LinearLayout) (!(convertView instanceof LinearLayout) ? View.inflate(getContext(), R.layout.track_list_item, null)
                    : convertView);
            TextView name = ll.findViewById(R.id.name);
            name.setText(getItem(position).getName());
            TextView description = ll.findViewById(R.id.description);
            description.setText(getItem(position).getDescription());
            return ll;
        }
    }

    /**
     * Force load the tile layers by requesting the standard OSM layer
     */
    public void loadTileLayerSources() {
        new ExecutorTask<Void, Void, Void>(App.getLogic().getExecutorService(), App.getLogic().getHandler()) {
            @Override
            protected Void doInBackground(Void param) {
                TileLayerSource.get(getContext(), TileLayerSource.LAYER_MAPNIK, true);
                return null;
            }
        }.execute();
    }

    /**
     * Add a StyleableLayer from a file
     * 
     * @param activity the calling Activity
     * @param prefs current Preferences
     * @param map current Map
     * @param type the layer type
     */
    private void addStyleableLayerFromFile(final FragmentActivity activity, final Preferences prefs, final Map map, @NonNull final LayerType type) {
        Log.d(DEBUG_TAG, "addStyleableLayerFromFile");
        SelectFile.read(activity, R.string.config_osmPreferredDir_key, new ReadFile() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean read(FragmentActivity activity, Uri fileUri) {
                addStyleableLayerFromUri(activity, prefs, map, type, fileUri, true);
                return true;
            }

            @Override
            public void read(FragmentActivity activity, List<Uri> fileUris) {
                for (Uri fileUri : fileUris) {
                    addStyleableLayerFromUri(activity, prefs, map, type, fileUri, false);
                }
            }
        }, true);
    }

    /**
     * Add a StyleableLayer from a file Uri
     * 
     * @param activity the calling Activity
     * @param prefs current Preferences
     * @param map current Map
     * @param type the layer type
     * @param fileUri the file uri
     * @param showDialog show the style dialog if true
     */
    private void addStyleableLayerFromUri(@NonNull final FragmentActivity activity, @NonNull final Preferences prefs, @NonNull final Map map,
            @NonNull LayerType type, @NonNull Uri fileUri, boolean showDialog) {
        final String uriString = fileUri.toString();
        de.blau.android.layer.StyleableLayer layer = (de.blau.android.layer.StyleableLayer) map.getLayer(type, uriString);
        if (layer == null) {
            Log.d(DEBUG_TAG, "addStyleableLayerFromUri " + uriString);
            de.blau.android.layer.Util.addLayer(activity, type, uriString);
            map.setUpLayers(activity);
            layer = (de.blau.android.layer.StyleableLayer) map.getLayer(type, uriString);
            if (layer != null) { // if null setUpLayers will have toasted
                if (showDialog) {
                    LayerStyle.showDialog(activity, layer.getIndex());
                }
                SelectFile.savePref(prefs, R.string.config_osmPreferredDir_key, fileUri);
                layer.invalidate();
                tl.removeAllViews();
                addRows(activity);
            }
        } else {
            ScreenMessage.toastTopWarning(activity, activity.getString(R.string.toast_styleable_layer_exists, fileUri.getLastPathSegment()));
        }
    }

    /**
     * Add a Layer from a mapbox-gl Style
     * 
     * Adds a custom imagery entry then sets the style
     * 
     * @param activity the calling Activity
     * @param prefs current Preferences
     * @param map current Map
     */
    private void addMVTLayerFromStyle(@NonNull final FragmentActivity activity, @NonNull final Preferences prefs, @NonNull final Map map) {
        SelectFile.read(activity, R.string.config_osmPreferredDir_key, new ReadFile() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean read(FragmentActivity activity, Uri fileUri) {
                Style style = new Style();
                try {
                    if (ContentResolverUtil.getSizeColumn(activity, fileUri) > MAX_STYLE_FILE_SIZE) {
                        ScreenMessage.toastTopError(activity, R.string.toast_style_file_too_large);
                        return false;
                    }
                    style.loadStyle(activity, activity.getContentResolver().openInputStream(fileUri));
                    if (style.getSources().size() != 1) {
                        ScreenMessage.toastTopError(activity, R.string.toast_only_one_source_supported);
                        return false;
                    }
                    Entry<String, Source> entry = new ArrayList<>(style.getSources().entrySet()).get(0);
                    try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                        String id = entry.getValue().createLayer(activity, db, entry.getKey(), Category.other, true);
                        de.blau.android.layer.Util.addLayer(activity, LayerType.OVERLAYIMAGERY, id);
                        updateDialogAndPrefs(activity, prefs, map);
                        de.blau.android.layer.mvt.MapOverlay mvtLayer = (MapOverlay) map.getLayer(LayerType.OVERLAYIMAGERY, id);
                        if (mvtLayer != null) {
                            mvtLayer.setStyle(style);
                        } else {
                            Log.e(DEBUG_TAG, "Didn't find MVT layer after adding");
                        }
                    }
                    return true;
                } catch (FileNotFoundException e) {
                    ScreenMessage.toastTopError(activity, activity.getString(R.string.toast_file_not_found, fileUri.toString()));
                    return false;
                } catch (OsmIllegalOperationException e) {
                    ScreenMessage.toastTopError(activity, e.getMessage());
                    return false;
                }
            }
        });
    }

    /**
     * Update the dialog and set the prefs
     * 
     * @param activity calling FragmentActivity
     * @param prefs Preference instance to set
     * @param map the current Map instance
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
        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        visible.setImageResource(layer.isVisible() ? visibleId : invisibleId);
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
        final LayerMenuListener menuListener = new LayerMenuListener(menu, layer);
        menu.setOnClickListener(menuListener);
        cell.setOnClickListener(menuListener);
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
        TableRow.LayoutParams trp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
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

            // maybe we should use an interface here
            if (layer instanceof MapTilesLayer && !(layer instanceof de.blau.android.layer.mapillary.MapillaryOverlay)) {
                // get MRU list from layer
                final String[] tileServerIds = ((MapTilesLayer<?>) layer).getMRU();
                final TileLayerSource tileLayerConfiguration = ((MapTilesLayer<?>) layer).getTileLayerConfiguration();
                final String currentServerId = tileLayerConfiguration.getId();
                for (int i = 0; i < tileServerIds.length; i++) {
                    final String id = tileServerIds[i];

                    if (!currentServerId.equals(id)) {
                        final TileLayerSource tileServer = TileLayerSource.get(activity, id, true);
                        if (tileServer != null) {
                            MenuItem item = menu.add(tileServer.getName());
                            item.setOnMenuItemClickListener(unused -> {
                                if (tileServer != null) {
                                    TableRow row = (TableRow) button.getTag();
                                    setNewImagery(activity, row, (MapTilesLayer<?>) layer, tileServer);
                                    dismissDialog();
                                    layer.invalidate();
                                }
                                return true;
                            });
                        } else {
                            ((MapTilesLayer<?>) layer).removeServerFromMRU(id);
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
                        if (!TileLayerSource.isFullyPopulated()) {
                            // FIXME this is borderline too slow to run on the main thread, maybe run in a thread while
                            // the dialog is starting up
                            de.blau.android.layer.Util.populateImageryLists(activity);
                        }
                        showImagerySelectDialog((TableRow) button.getTag(), (MapTilesLayer<?>) layer, layer.getType() == LayerType.OVERLAYIMAGERY);
                        Tip.showDialog(activity, R.string.tip_imagery_privacy_key, R.string.tip_imagery_privacy);
                    }
                    return true;
                });

                if (TileLayerDatabase.SOURCE_MANUAL.equals(tileLayerConfiguration.getSource())) {
                    MenuItem editItem = menu.add(R.string.layer_edit_custom_imagery_configuration);
                    editItem.setOnMenuItemClickListener(unused -> {
                        try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getReadableDatabase()) {
                            long rowid = TileLayerDatabase.getLayerRowId(db, currentServerId);
                            updateListener = () -> {
                                de.blau.android.layer.Util.populateImageryLists(activity);
                                map.setUpLayers(activity);

                            };
                            TileLayerDialog.showDialog(Layers.this, rowid, null);
                        } catch (IllegalArgumentException iaex) {
                            ScreenMessage.toastTopError(activity, iaex.getMessage());
                        }
                        return true;
                    });
                }
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

            if (layer instanceof StyleableInterface) {
                MenuItem item = menu.add(R.string.layer_change_style);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        LayerStyle.showDialog(activity, layer.getIndex());
                    }
                    return true;
                });
                final boolean stylingEnabled = ((StyleableInterface) layer).stylingEnabled();
                item.setEnabled(stylingEnabled);
                item = menu.add(R.string.layer_reset_style);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        ((StyleableInterface) layer).resetStyling();
                        if (layer instanceof StyleableLayer) {
                            ((StyleableLayer) layer).dirty();
                        }
                        if (layer instanceof de.blau.android.layer.mvt.MapOverlay) {
                            // tiles need to be re-decoded for auto styling to work
                            ((de.blau.android.layer.mvt.MapOverlay) layer).flushTileCache(activity, false);
                        }
                        layer.invalidate();
                    }
                    return true;
                });
                item.setEnabled(stylingEnabled);
            }

            if (layer instanceof de.blau.android.layer.mapillary.MapillaryOverlay) {
                MenuItem item = menu.add(R.string.layer_set_date_range);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        ((de.blau.android.layer.mapillary.MapillaryOverlay) layer).selectDateRange(getActivity(), layer.getIndex());
                    }
                    return true;
                });
            }

            if (layer instanceof de.blau.android.layer.mvt.MapOverlay) {
                MenuItem item = menu.add(R.string.layer_load_style);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        try {
                            ((de.blau.android.layer.mvt.MapOverlay) layer).loadStyleFromFile(getActivity());
                        } catch (IOException e) {
                            ScreenMessage.toastTopInfo(activity, getString(R.string.toast_error_loading_style, e.getLocalizedMessage()));
                        }
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
                        Logic logic = App.getLogic();
                        new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
                            @Override
                            protected void onPreExecute() {
                                Progress.showDialog(activity, Progress.PROGRESS_PRUNING);
                            }

                            @Override
                            protected Void doInBackground(Void arg) {
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
                        ((MapTilesLayer<?>) layer).flushTileCache(activity, true);
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

                item = menu.add(R.string.menu_layers_background_align);
                item.setEnabled(layer.isVisible() && map.isVisible(layer));
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        try {
                            Logic logic = App.getLogic();
                            ImageryAlignmentActionModeCallback backgroundAlignmentActionModeCallback = new ImageryAlignmentActionModeCallback(((Main) activity),
                                    logic.getMode() != Mode.MODE_ALIGN_BACKGROUND ? logic.getMode() : Mode.MODE_EASYEDIT,
                                    ((MapTilesLayer<?>) layer).getContentId());
                            // NOTE needs to be after instance creation, logic.setMode needs to be called -after- this
                            ((Main) activity).setImageryAlignmentActionModeCallback(backgroundAlignmentActionModeCallback);
                            logic.setMode(((Main) activity), Mode.MODE_ALIGN_BACKGROUND);
                            ((Main) activity).startSupportActionMode(backgroundAlignmentActionModeCallback);
                        } catch (IllegalStateException isex) {
                            Log.e(DEBUG_TAG, isex.getMessage());
                        }
                        dismissDialog();
                    }
                    return true;
                });

                if (!((MapTilesLayer<?>) layer).getTileLayerConfiguration().isLocalFile()) {
                    item = menu.add(R.string.layer_test);
                    item.setOnMenuItemClickListener(unused -> {
                        if (layer != null) {
                            TileSourceDiagnostics.showDialog(activity, ((MapTilesLayer<?>) layer).getTileLayerConfiguration(), map.getZoomLevel(),
                                    map.getViewBox());
                        }
                        return true;
                    });
                }
            }
            if (layer instanceof de.blau.android.layer.gpx.MapOverlay) {
                boolean recordingLayer = activity.getString(R.string.layer_gpx_recording).equals(layer.getContentId());
                if (!recordingLayer) {
                    MenuItem item = menu.add(R.string.menu_gps_goto_start);
                    item.setOnMenuItemClickListener(unused -> {
                        if (layer != null && activity instanceof Main) {
                            Track track = ((de.blau.android.layer.gpx.MapOverlay) layer).getTrack();
                            if (track != null) {
                                TrackPoint tp = track.getFirstTrackPoint();
                                if (tp != null) {
                                    ((Main) activity).gotoTrackPoint(App.getLogic(), tp);
                                } else {
                                    ScreenMessage.toastTopWarning(activity, R.string.toast_no_track_points);
                                }
                            }
                        }
                        dismissDialog();
                        return true;
                    });
                    item = menu.add(R.string.menu_gps_goto_first_waypoint);
                    item.setOnMenuItemClickListener(unused -> {
                        if (layer != null && activity instanceof Main) {
                            Track track = ((de.blau.android.layer.gpx.MapOverlay) layer).getTrack();
                            if (track != null) {
                                WayPoint wp = track.getFirstWayPoint();
                                if (wp != null) {
                                    ((Main) activity).gotoTrackPoint(App.getLogic(), wp);
                                } else {
                                    ScreenMessage.toastTopWarning(activity, R.string.toast_no_way_points);
                                }
                            }
                        }
                        dismissDialog();
                        return true;
                    });
                }
                MenuItem item = menu.add(R.string.menu_gps_upload);
                item.setOnMenuItemClickListener(unused -> {
                    if (layer != null) {
                        final Server server = App.getLogic().getPrefs().getServer();
                        if (Server.checkOsmAuthentication(activity, server, () -> GpxUpload.showDialog(activity, layer.getContentId()))) {
                            GpxUpload.showDialog(activity, layer.getContentId());
                        }
                    }
                    return true;
                });
                item = menu.add(R.string.menu_gps_export);
                item.setOnMenuItemClickListener(unused -> {
                    SelectFile.save(activity, R.string.config_osmPreferredDir_key, new SaveFile() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean save(FragmentActivity currentActivity, Uri fileUri) {
                            // FIXME layer will likely not be valid if the activity has been recreated
                            if (layer != null) {
                                final Track track = ((de.blau.android.layer.gpx.MapOverlay) layer).getTrack();
                                if (track != null) {
                                    SavingHelper.asyncExport(currentActivity, track, fileUri);
                                    SelectFile.savePref(App.getLogic().getPrefs(), R.string.config_osmPreferredDir_key, fileUri);
                                }
                            }
                            return true;
                        }
                    });
                    return true;
                });
                if (activity instanceof Main) {
                    item.setEnabled(((Main) activity).isStoragePermissionGranted());
                }
                if (!recordingLayer) {
                    item = menu.add(R.string.layer_start_playback);
                    item.setOnMenuItemClickListener(unused -> {
                        if (layer != null && (activity instanceof Main)) {
                            ((Main) activity).setFollowGPS(true);
                            ((de.blau.android.layer.gpx.MapOverlay) layer).startPlayback();
                            dismissDialog();
                        }
                        return true;
                    });
                    item.setEnabled(!((de.blau.android.layer.gpx.MapOverlay) layer).isPlaying());

                    item = menu.add(R.string.layer_pause_playback);
                    item.setOnMenuItemClickListener(unused -> {
                        if (layer != null) {
                            ((de.blau.android.layer.gpx.MapOverlay) layer).pausePlayback();
                            dismissDialog();
                        }
                        return true;
                    });
                    item.setEnabled(((de.blau.android.layer.gpx.MapOverlay) layer).isPlaying());

                    item = menu.add(R.string.layer_stop_playback);
                    item.setOnMenuItemClickListener(unused -> {
                        if (layer != null) {
                            ((de.blau.android.layer.gpx.MapOverlay) layer).stopPlayback();
                            dismissDialog();
                        }
                        return true;
                    });
                    item.setEnabled(!((de.blau.android.layer.gpx.MapOverlay) layer).isStopped());
                }
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
     * Show a dialog that shows a selection of imagery sources that can be used
     * 
     * @param row the TableRow we were invoked from, can be null if row doesn't exists
     * @param layer the layer we should change imagery for, can be null if layer doesn't exist yet
     * @param isOverlay true if this is for the overlay layer
     */
    private void showImagerySelectDialog(@Nullable final TableRow row, @Nullable final MapTilesLayer<?> layer, boolean isOverlay) {
        final FragmentActivity activity = getActivity();
        final Preferences prefs = App.getLogic().getPrefs();

        Builder builder = new AlertDialog.Builder(activity);

        final LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(getActivity());

        final View layout = themedInflater.inflate(R.layout.layer_selection_dialog, null);
        RadioGroup categoryGroup = (RadioGroup) layout.findViewById(R.id.categoryGroup);
        AppCompatRadioButton allButton = categoryGroup.findViewById(R.id.categoryAll);
        AppCompatRadioButton photoButton = categoryGroup.findViewById(R.id.categoryPhoto);
        AppCompatRadioButton terrainButton = categoryGroup.findViewById(R.id.categoryElevation);
        AppCompatRadioButton qaButton = categoryGroup.findViewById(R.id.categoryQA);

        photoButton.setTag(Category.photo);
        terrainButton.setTag(Category.elevation);
        qaButton.setTag(Category.qa);

        Category backgroundCategory = prefs.getBackgroundCategory();
        Category overlayCategory = prefs.getOverlayCategory();

        allButton.setChecked(true);
        if (isOverlay) {
            photoButton.setVisibility(View.GONE);
            terrainButton.setVisibility(View.GONE);
            qaButton.setVisibility(View.VISIBLE);
            if (Category.qa == overlayCategory) {
                qaButton.setChecked(true);
            }
        } else {
            photoButton.setVisibility(View.VISIBLE);
            terrainButton.setVisibility(View.VISIBLE);
            qaButton.setVisibility(View.GONE);
            if (Category.photo == backgroundCategory) {
                photoButton.setChecked(true);
            } else if (Category.elevation == backgroundCategory) {
                terrainButton.setChecked(true);
            }
        }

        builder.setView(layout);
        builder.setTitle(isOverlay ? R.string.config_overlayLayer_title : R.string.config_backgroundLayer_title);
        builder.setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();

        ViewBox viewBox = App.getLogic().getMap().getViewBox();
        final boolean newLayer = layer == null;
        final TileType tileType = newLayer ? null : layer.getTileLayerConfiguration().getTileType();
        final String[] ids = isOverlay ? TileLayerSource.getOverlayIds(viewBox, true, overlayCategory, tileType)
                : TileLayerSource.getIds(viewBox, true, backgroundCategory, tileType);

        RecyclerView imageryList = (RecyclerView) layout.findViewById(R.id.imageryList);
        LayoutParams buttonLayoutParams = imageryList.getLayoutParams();
        buttonLayoutParams.width = LayoutParams.MATCH_PARENT;

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        imageryList.setLayoutManager(layoutManager);

        final ImageryListAdapter adapter = new ImageryListAdapter(activity, ids,
                newLayer ? TileLayerSource.LAYER_NONE : layer.getTileLayerConfiguration().getId(), isOverlay, buttonLayoutParams,
                new LayerOnCheckedChangeListener(activity, dialog, row, layer, ids));
        adapter.addInfoClickListener((String id) -> {
            TileLayerSource l = TileLayerSource.get(getContext(), id, true);
            if (l != null) {
                LayerInfo f = new ImageryLayerInfo();
                f.setShowsDialog(true);
                Bundle args = new Bundle();
                args.putSerializable(ImageryLayerInfo.LAYER_KEY, l);
                f.setArguments(args);
                LayerInfo.showDialog(getActivity(), f);
            }
        });
        imageryList.setAdapter(adapter);

        categoryGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Category category = checkedId >= 0 ? (Category) group.findViewById(checkedId).getTag() : null;
            imageryList.removeAllViews();
            final String[] idsForButtons = isOverlay ? TileLayerSource.getOverlayIds(viewBox, true, category, null)
                    : TileLayerSource.getIds(viewBox, true, category, null);
            if (isOverlay) {
                prefs.setOverlayCategory(category);
            } else {
                prefs.setBackgroundCategory(category);
            }
            adapter.setIds(getContext(), idsForButtons, isOverlay, true);
            adapter.setOnCheckedChangeListener(new LayerOnCheckedChangeListener(activity, dialog, row, layer, idsForButtons));
        });
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private class LayerOnCheckedChangeListener implements OnCheckedChangeListener {
        final FragmentActivity activity;
        final Dialog           dialog;
        final String[]         ids;
        final TableRow         row;
        final MapTilesLayer<?> layer;

        /**
         * Construct a new listener
         * 
         * @param activity the calling activity
         * @param dialog the dialog
         * @param row the TableRow with the information
         * @param layer the layer the layer to change
         * @param ids the list of the tile source ids to display
         */
        LayerOnCheckedChangeListener(@NonNull FragmentActivity activity, @NonNull Dialog dialog, @Nullable TableRow row, @Nullable MapTilesLayer<?> layer,
                @NonNull String[] ids) {
            this.activity = activity;
            this.dialog = dialog;
            this.ids = ids;
            this.row = row;
            this.layer = layer;
        }

        @SuppressLint("ResourceType")
        @Override
        public void onCheckedChanged(RadioGroup group, int position) {
            if (position != -1 && position < ids.length) {
                final TileLayerSource tileSource = TileLayerSource.get(getActivity(), ids[position], true);
                if (tileSource != null) {
                    setNewImagery(activity, row, layer, tileSource);
                }
            } else {
                Log.e(DEBUG_TAG, "position out of range 0-" + (ids.length - 1) + ": " + position);
            }
            // allow a tiny bit of time to see that the action actually worked
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                dialog.dismiss(); // dismiss this
                dismissDialog(); // and then the caller
            }, 100);
        }
    }

    /**
     * Change the imagery for a tile layer
     * 
     * @param activity the calling activity
     * @param row the TableRow with the information, if null we will only set the prefs
     * @param layer the layer, if null we will only set the prefs
     * @param tileSource the new tileserver to use, if null use the prefs
     */
    private void setNewImagery(@NonNull FragmentActivity activity, @Nullable TableRow row, @Nullable MapTilesLayer<?> layer,
            @Nullable TileLayerSource tileSource) {
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
            LayerConfig[] layerConfigs = db.getLayers();
            if (layer != null) { // existing layer
                setVisibility(db, layer, true);
                final int layerIndex = layer.getIndex();
                if (tileSource != null) {
                    db.setLayerContentId(layerIndex, tileSource.getId());
                } else if (layerIndex < layerConfigs.length) {
                    tileSource = TileLayerSource.get(activity, layerConfigs[layerIndex].getContentId(), true);
                }
                if (tileSource != null) { // still null?
                    App.getDelegator().setImageryRecorded(false);
                    if (row != null) {
                        TextView name = (TextView) row.getChildAt(2);
                        name.setText(tileSource.getName());
                        layer.setRendererInfo(tileSource);
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
            } else if (tileSource != null) { // new layer
                de.blau.android.layer.Util.addImageryLayer(db, layerConfigs, tileSource.isOverlay(), tileSource.getId());
                App.getLogic().getMap().invalidate();
            } else {
                Log.e(DEBUG_TAG, "setNewImagery both layer and tile source null");
            }
            if (activity instanceof Main) {
                ((Main) activity).invalidateOptionsMenu();
            }
        }
        setPrefs(activity, App.getLogic().getPrefs());
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
            setVisibility(db, layer, visible);
        }
    }

    /**
     * Set layer visibility
     * 
     * @param db an AdvancedPrefDatabase instance
     * @param layer the layer
     * @param visible the value to set
     */
    private void setVisibility(@NonNull AdvancedPrefDatabase db, @NonNull MapViewLayer layer, boolean visible) {
        layer.setVisible(visible);
        db.setLayerVisibility(layer.getIndex(), visible);
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
            App.getLogic().getMap().setPrefs(activity, prefs);
        }
    }

    @Override
    public void update() {
        if (updateListener != null) {
            updateListener.update();
            updateListener = null;
        }
        final Logic logic = App.getLogic();
        updateDialogAndPrefs(getActivity(), logic.getPrefs(), logic.getMap());
    }
}
