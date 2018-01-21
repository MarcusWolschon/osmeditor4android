package de.blau.android.resources;

import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import ch.poole.android.numberpicker.library.NumberPicker;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.TileLayerServer.Provider;
import de.blau.android.resources.TileLayerServer.Provider.CoverageArea;
import de.blau.android.util.Snack;
import de.blau.android.views.layers.MapTilesLayer;

public class TileLayerDatabaseUI {
    private static final String DEBUG_TAG = TileLayerDatabaseUI.class.getSimpleName();

    private static final String DEBUG_LOG = "TileLayerDatabaseUI";

    /**
     * Ruleset database related methods and fields
     */
    private Cursor       layerCursor;
    private LayerAdapter layerAdapter;

    /**
     * Show a list of the layers in the database, selection will either load a template or start the edit dialog on it
     * 
     * @param context Android context
     */
    public void manageLayers(@NonNull final Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        View rulesetView = (View) LayoutInflater.from(context).inflate(R.layout.layer_list, null);
        alertDialog.setTitle(R.string.custom_layer_title);
        alertDialog.setView(rulesetView);
        final SQLiteDatabase writableDb = new TileLayerDatabase(context).getWritableDatabase();
        ListView layerList = (ListView) rulesetView.findViewById(R.id.listViewLayer);
        layerCursor = TileLayerDatabase.getAllCustomLayers(writableDb);
        layerAdapter = new LayerAdapter(writableDb, context, layerCursor);
        layerList.setAdapter(layerAdapter);
        alertDialog.setNeutralButton(R.string.done, null);
        alertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                layerCursor.close();
                writableDb.close();
            }
        });
        final FloatingActionButton fab = (FloatingActionButton) rulesetView.findViewById(R.id.add);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, fab);

                // menu items for adding rules
                MenuItem addResurveyEntry = popup.getMenu().add(R.string.add_layer_title);
                addResurveyEntry.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showLayerDialog(context, writableDb, false, -1);
                        return true;
                    }
                });
                popup.show();// showing popup menu
            }
        });
        alertDialog.show();
    }

    private class LayerAdapter extends CursorAdapter {
        final SQLiteDatabase db;

        public LayerAdapter(final SQLiteDatabase db, Context context, Cursor cursor) {
            super(context, cursor, 0);
            this.db = db;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            View view = LayoutInflater.from(context).inflate(R.layout.layer_list_item, parent, false);
            return view;
        }

        @Override
        public void bindView(final View view, final Context context, Cursor cursor) {
            Log.d(DEBUG_TAG, "bindView");
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            view.setTag(id);
            Log.d(DEBUG_TAG, "bindView id " + id);
            String name = cursor.getString(cursor.getColumnIndexOrThrow(TileLayerDatabase.NAME_FIELD));

            TextView nameView = (TextView) view.findViewById(R.id.name);
            nameView.setText(name);

            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer id = (Integer) view.getTag();
                    showLayerDialog(context, db, true, id != null ? id : -1);
                }
            });
        }
    }

    /**
     * Replace the current cursor for the resurvey table
     * 
     * @param db the template database
     */
    private void newLayerCursor(@NonNull final SQLiteDatabase db) {
        Cursor newCursor = TileLayerDatabase.getAllCustomLayers(db);
        Cursor oldCursor = layerAdapter.swapCursor(newCursor);
        oldCursor.close();
        layerAdapter.notifyDataSetChanged();
    }

    /**
     * Show a dialog for editing and saving a layer entry
     * 
     * @param context Android Context
     * @param db a writable instance of the layer entry database
     * @param existing true if this is not a new layer entry
     * @param id the rowid of the layer entry in the database or -1 if not saved yet
     */
    private void showLayerDialog(@NonNull final Context context, @NonNull final SQLiteDatabase db, final boolean existing, final int id) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        View templateView = (View) LayoutInflater.from(context).inflate(R.layout.layer_item, null);
        alertDialog.setView(templateView);

        final EditText nameEdit = (EditText) templateView.findViewById(R.id.name);
        final EditText urlEdit = (EditText) templateView.findViewById(R.id.url);
        final CheckBox overlayCheck = (CheckBox) templateView.findViewById(R.id.overlay);
        final EditText leftEdit = (EditText) templateView.findViewById(R.id.left);
        final EditText bottomEdit = (EditText) templateView.findViewById(R.id.bottom);
        final EditText rightEdit = (EditText) templateView.findViewById(R.id.right);
        final EditText topEdit = (EditText) templateView.findViewById(R.id.top);
        final NumberPicker minZoomPicker = (NumberPicker) templateView.findViewById(R.id.zoom_min);
        final NumberPicker maxZoomPicker = (NumberPicker) templateView.findViewById(R.id.zoom_max);

        TileLayerServer layer = null;
        ;
        if (existing) {
            layer = TileLayerDatabase.getLayerWithRowId(context, db, id);

            nameEdit.setText(layer.getName());
            urlEdit.setText(layer.getOriginalTileUrl());
            overlayCheck.setChecked(layer.isOverlay());
            minZoomPicker.setValue(layer.getMinZoomLevel());
            maxZoomPicker.setValue(layer.getMaxZoomLevel());
            List<CoverageArea> coverages = layer.getCoverage();
            if (coverages != null && !coverages.isEmpty()) {
                BoundingBox box = coverages.get(0).getBoundingBox();
                Log.d(DEBUG_LOG, "Coverage box " + box);
                if (box != null) {
                    leftEdit.setText(Double.toString(box.getLeft() / 1E7D));
                    bottomEdit.setText(Double.toString(box.getBottom() / 1E7D));
                    rightEdit.setText(Double.toString(box.getRight() / 1E7D));
                    topEdit.setText(Double.toString(box.getTop() / 1E7D));
                }
            }

            alertDialog.setTitle(R.string.edit_layer_title);
            alertDialog.setNeutralButton(R.string.Delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(DEBUG_TAG, "deleting layer " + Integer.toString(id));
                    TileLayerDatabase.deleteLayerWithRowId(db, id);
                    newLayerCursor(db);
                    resetLayer(context, db);
                }
            });
        } else {
            minZoomPicker.setValue(TileLayerServer.DEFAULT_MIN_ZOOM);
            maxZoomPicker.setValue(TileLayerServer.DEFAULT_MIN_ZOOM);
            alertDialog.setTitle(R.string.add_layer_title);
        }
        alertDialog.setNegativeButton(R.string.Cancel, null);

        alertDialog.setPositiveButton(R.string.Save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dummy
            }
        });
        final TileLayerServer existingLayer = layer;
        final AlertDialog dialog = alertDialog.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Provider provider = new Provider();
                String leftText = leftEdit.getText().toString().trim();
                String bottomText = bottomEdit.getText().toString().trim();
                String rightText = rightEdit.getText().toString().trim();
                String topText = topEdit.getText().toString().trim();
                int minZoom = minZoomPicker.getValue();
                int maxZoom = maxZoomPicker.getValue();
                boolean moan = false;
                if (!("".equals(leftText) || "".equals(bottomText) || "".equals(rightText) || "".equals(topText))) {
                    Log.d(DEBUG_TAG, "left >" + leftText + "<");
                    BoundingBox box = null;
                    try {
                        box = new BoundingBox(Double.parseDouble(leftText), Double.parseDouble(bottomText), Double.parseDouble(rightText),
                                Double.parseDouble(topText));
                        moan = !box.isValid();
                    } catch (NumberFormatException nfe) {
                        moan = true;
                    }
                    if (moan) {
                        Snack.toastTopError(context, R.string.toast_invalid_box);
                    } else {
                        CoverageArea ca = new CoverageArea(TileLayerServer.DEFAULT_MIN_ZOOM, TileLayerServer.DEFAULT_MAX_ZOOM, box);
                        provider.addCoverageArea(ca);
                    }
                }
                String name = nameEdit.getText().toString().trim();
                if ("".equals(name)) {
                    Snack.toastTopError(context, R.string.toast_name_empty);
                    moan = true;
                }
                String tileUrl = urlEdit.getText().toString().trim();
                if ("".equals(tileUrl)) {
                    Snack.toastTopError(context, R.string.toast_url_empty);
                    moan = true;
                }
                if (minZoom >= maxZoom) {
                    Snack.toastTopError(context, R.string.toast_min_zoom);
                    moan = true;
                }
                if (moan) { // abort and leave the dialog intact
                    return;
                }
                int tileSize = TileLayerServer.DEFAULT_TILE_SIZE;
                String proj = null;
                // hack, but saves people extracting and then having to re-select the projection
                if (tileUrl.contains(TileLayerServer.EPSG_3857)) {
                    proj = TileLayerServer.EPSG_3857;
                    tileSize = TileLayerServer.WMS_TILE_SIZE;
                } else if (tileUrl.contains(TileLayerServer.EPSG_900913)) {
                    proj = TileLayerServer.EPSG_900913;
                    tileSize = TileLayerServer.WMS_TILE_SIZE;
                }
                String id = name.toUpperCase();
                if (!existing) {
                    TileLayerServer layer = new TileLayerServer(context, id, name, tileUrl, "tms", overlayCheck.isChecked(), false, provider, null, null, null,
                            null, minZoom, maxZoom, TileLayerServer.DEFAULT_MAX_OVERZOOM, tileSize, tileSize, proj, 0, Long.MIN_VALUE, Long.MAX_VALUE, true);
                    TileLayerDatabase.addLayer(db, TileLayerDatabase.SOURCE_CUSTOM, layer);
                } else {
                    existingLayer.setProvider(provider);
                    existingLayer.setName(name);
                    existingLayer.setOriginalTileUrl(tileUrl);
                    existingLayer.setOverlay(overlayCheck.isChecked());
                    TileLayerDatabase.updateLayer(db, existingLayer);
                }
                newLayerCursor(db);
                resetLayer(context, db);
                dialog.dismiss();
            }
        });
    }

    /**
     * Regenerate the in memory imagery configs
     * 
     * @param context Android Context
     * @param db a readable DB
     */
    protected void resetLayer(Context context, SQLiteDatabase db) {
        TileLayerServer.getListsLocked(context, db, true);
        MapTilesLayer layer = App.getLogic().getMap().getBackgroundLayer();
        if (layer != null) {
            layer.getTileProvider().update();
        }
    }
}
