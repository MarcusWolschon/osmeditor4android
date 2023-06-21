package de.blau.android.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.layer.LayerType;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

public class TileLayerDatabaseView {
    private static final String DEBUG_TAG = TileLayerDatabaseView.class.getSimpleName();

    /**
     * Ruleset database related methods and fields
     */
    private LayerAdapter layerAdapter;

    /**
     * Show a list of the layers in the database, selection will either load a template or start the edit dialog on it
     * 
     * @param activity Android context
     */
    public void manageLayers(@NonNull final FragmentActivity activity) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        View layerListView = LayoutInflater.from(activity).inflate(R.layout.layer_list, null);
        alertDialog.setTitle(R.string.custom_layer_title);
        alertDialog.setView(layerListView);
        final TileLayerDatabase tlDb = new TileLayerDatabase(activity); // NOSONAR will be closed when dismissed
        final SQLiteDatabase writableDb = tlDb.getWritableDatabase();
        ListView layerList = (ListView) layerListView.findViewById(R.id.listViewLayer);
        Cursor layerCursor = TileLayerDatabase.getAllCustomLayers(writableDb);
        layerAdapter = new LayerAdapter(writableDb, activity, layerCursor);
        layerList.setAdapter(layerAdapter);
        alertDialog.setNeutralButton(R.string.done, null);
        alertDialog.setOnDismissListener(dialog -> {
            layerCursor.close();
            writableDb.close();
            tlDb.close();
        });

        layerList.setOnItemLongClickListener((parent, view, position, unused) -> {
            final Integer id = (Integer) view.getTag();
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle(R.string.delete_layer);
            dialog.setNeutralButton(R.string.cancel, null);
            dialog.setPositiveButton(R.string.delete, (d, which) -> {
                TileLayerSource tileServer = TileLayerDatabase.getLayerWithRowId(activity, writableDb, id);
                removeLayerSelection(activity, tileServer);
                TileLayerDatabase.deleteLayerWithRowId(writableDb, id);
                newLayerCursor(writableDb);
                resetLayer(activity, writableDb);
            });
            dialog.show();
            return true;
        });
        final FloatingActionButton fab = (FloatingActionButton) layerListView.findViewById(R.id.add);
        fab.setOnClickListener(v -> TileLayerDialog.showLayerDialog(activity, -1, null, () -> {
            newLayerCursor(writableDb);
            resetLayer(activity, writableDb);
        }));
        alertDialog.show();
    }

    private class LayerAdapter extends CursorAdapter {
        final SQLiteDatabase   db;
        final FragmentActivity activity;

        /**
         * A cursor adapter that binds Layers to Views
         * 
         * @param db an open db
         * @param activity the calling activity
         * @param cursor the Cursor
         */
        public LayerAdapter(@NonNull final SQLiteDatabase db, @NonNull final FragmentActivity activity, @NonNull Cursor cursor) {
            super(activity, cursor, 0);
            this.db = db;
            this.activity = activity;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            return LayoutInflater.from(context).inflate(R.layout.layer_list_item, parent, false);
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
            view.setLongClickable(true);
            view.setOnClickListener(v -> {
                Integer tag = (Integer) view.getTag();
                TileLayerDialog.showLayerDialog(activity, tag != null ? tag : -1, null, () -> {
                    newLayerCursor(db);
                    resetLayer(activity, db);
                });
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
     * Regenerate the in memory imagery configs and try to make configuration consistent
     * 
     * @param context Android Context
     * @param db a readable DB
     */
    protected static void resetLayer(@NonNull Context context, @NonNull SQLiteDatabase db) {
        TileLayerSource.getListsLocked(context, db, true);
        Logic logic = App.getLogic();
        if (logic != null) {
            MapTilesLayer<?> background = logic.getMap().getBackgroundLayer();
            if (background != null) {
                updateLayerConfig(context, background);
            }
            MapTilesOverlayLayer<?> overlay = logic.getMap().getOverlayLayer();
            if (overlay != null) {
                updateLayerConfig(context, overlay);
            }
        }
    }

    /**
     * Update the config of the current layer(s) including setting the prefs
     * 
     * @param context Android Context
     * @param layer the layer we are updating
     */
    public static void updateLayerConfig(@NonNull Context context, @Nullable MapTilesLayer<?> layer) {
        if (layer == null) {
            return;
        }
        Log.d(DEBUG_TAG, "updating layer " + layer.getName());
        TileLayerSource config = layer.getTileLayerConfiguration();
        if (config != null) {
            TileLayerSource newConfig = TileLayerSource.get(context, config.getId(), false);
            if (newConfig != null) { // if null the layer has been deleted
                boolean isOverlay = layer.getType() == LayerType.OVERLAYIMAGERY;
                if ((isOverlay && !newConfig.isOverlay()) || (!isOverlay && newConfig.isOverlay())) {
                    // not good overlay as background or the other way around
                    try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
                        db.deleteLayer(isOverlay ? LayerType.OVERLAYIMAGERY : LayerType.IMAGERY, newConfig.getId());
                    }
                } else {
                    layer.setRendererInfo(newConfig);
                }
            }
        }
        layer.getTileProvider().update();
        checkMru(layer, layer.getType() == LayerType.OVERLAYIMAGERY ? TileLayerSource.getOverlayIds(null, false, null, null)
                : TileLayerSource.getIds(null, false, null, null));
    }

    /**
     * Check the contents of a layers MRU against the actually available layers
     * 
     * @param layer the layer with the MRU
     * @param idArray an array of ids
     */
    private static void checkMru(@NonNull MapTilesLayer<?> layer, @NonNull String[] idArray) {
        List<String> ids = Arrays.asList(idArray);
        List<String> mruIds = new ArrayList<>(Arrays.asList(layer.getMRU()));
        for (String id : mruIds) {
            if (!ids.contains(id)) {
                layer.removeServerFromMRU(id);
            }
        }
    }

    /**
     * If the current layer is deleted zap the respective prefs
     * 
     * @param context an Android Context
     * @param layerConfig the layer
     */
    protected static void removeLayerSelection(@NonNull Context context, @Nullable final TileLayerSource layerConfig) {
        if (layerConfig != null) {
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(context)) {
                db.deleteLayer(layerConfig.isOverlay() ? LayerType.OVERLAYIMAGERY : LayerType.IMAGERY, layerConfig.getId());
            }
        } else {
            Log.e(DEBUG_TAG, "layerConfig should not be null here");
        }
    }
}
