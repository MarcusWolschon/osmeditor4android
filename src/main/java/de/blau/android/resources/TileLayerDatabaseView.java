package de.blau.android.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.FragmentActivity;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;
import de.blau.android.views.layers.MapTilesLayer;
import de.blau.android.views.layers.MapTilesOverlayLayer;

public class TileLayerDatabaseView {
    private static final String DEBUG_TAG = TileLayerDatabaseView.class.getSimpleName();

    /**
     * Ruleset database related methods and fields
     */
    private Cursor       layerCursor;
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
        layerCursor = TileLayerDatabase.getAllCustomLayers(writableDb);
        layerAdapter = new LayerAdapter(writableDb, activity, layerCursor);
        layerList.setAdapter(layerAdapter);
        alertDialog.setNeutralButton(R.string.done, null);
        alertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                layerCursor.close();
                writableDb.close();
                tlDb.close();
            }
        });

        layerList.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long _id) {
                final Integer id = (Integer) view.getTag();
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                alertDialog.setTitle(R.string.delete_layer);
                alertDialog.setNeutralButton(R.string.cancel, null);
                alertDialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        TileLayerServer tileServer = TileLayerDatabase.getLayerWithRowId(activity, writableDb, id);
                        final Preferences prefs = App.getLogic().getPrefs();
                        removeLayerSelection(prefs, tileServer);
                        TileLayerDatabase.deleteLayerWithRowId(writableDb, id);
                        newLayerCursor(writableDb);
                        resetLayer(activity, writableDb);
                    }

                });
                alertDialog.show();
                return true;
            }
        });
        final FloatingActionButton fab = (FloatingActionButton) layerListView.findViewById(R.id.add);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TileLayerDialog.showLayerDialog(activity, -1, null, new TileLayerDialog.OnUpdateListener() {
                    @Override
                    public void update() {
                        newLayerCursor(writableDb);
                        resetLayer(activity, writableDb);
                    }
                });
            }
        });
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
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer id = (Integer) view.getTag();
                    TileLayerDialog.showLayerDialog(activity, id != null ? id : -1, null, new TileLayerDialog.OnUpdateListener() {
                        @Override
                        public void update() {
                            newLayerCursor(db);
                            resetLayer(activity, db);
                        }
                    });
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
     * Regenerate the in memory imagery configs and try to make configuration consistent
     * 
     * @param context Android Context
     * @param db a readable DB
     */
    protected static void resetLayer(@NonNull Context context, @NonNull SQLiteDatabase db) {
        TileLayerServer.getListsLocked(context, db, true);
        Logic logic = App.getLogic();
        if (logic != null) {
            Preferences prefs = logic.getPrefs();
            MapTilesLayer background = logic.getMap().getBackgroundLayer();
            if (background != null) {
                updateLayerConfig(context, prefs, background);
            }
            MapTilesOverlayLayer overlay = logic.getMap().getOverlayLayer();
            if (overlay != null) {
                updateLayerConfig(context, prefs, overlay);
            }
        }
    }

    /**
     * Update the config of the current layer(s) including setting the prefs
     * 
     * @param context Android Context
     * @param prefs a current Preferences object
     * @param layer the layer we are updating
     */
    public static void updateLayerConfig(@NonNull Context context, @NonNull Preferences prefs, @Nullable MapTilesLayer layer) {
        if (layer != null) {
            Log.d(DEBUG_TAG, "updating layer " + layer.getName());
            TileLayerServer config = layer.getTileLayerConfiguration();
            if (config != null) {
                TileLayerServer newConfig = TileLayerServer.get(context, config.getId(), false);
                if (newConfig != null) { // if null the layer has been deleted
                    boolean isOverlay = layer instanceof MapTilesOverlayLayer;
                    if ((isOverlay && !newConfig.isOverlay()) || (!isOverlay && newConfig.isOverlay())) {
                        // not good overlay as background or the other way around
                        if (!isOverlay) {
                            prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE);
                            layer.setRendererInfo(TileLayerServer.get(context, TileLayerServer.LAYER_NONE, false));
                        } else {
                            prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
                            layer.setRendererInfo(TileLayerServer.get(context, TileLayerServer.LAYER_NOOVERLAY, false));
                        }
                    } else {
                        layer.setRendererInfo(newConfig);
                    }
                }
            }
            layer.getTileProvider().update();
            checkMru(layer, TileLayerServer.getIds(null, false, null));
        }
    }

    /**
     * Check the contents of a layers MRU against the actually available layers
     * 
     * @param layer the layer with the MRU
     * @param idArray an array of ids
     */
    private static void checkMru(@NonNull MapTilesLayer layer, @NonNull String[] idArray) {
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
     * @param prefs a Preference object
     * @param layerConfig the layer
     */
    protected static void removeLayerSelection(@NonNull final Preferences prefs, @Nullable final TileLayerServer layerConfig) {
        if (layerConfig != null) {
            if (layerConfig.getId().equals(prefs.overlayLayer())) {
                prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
            }
            if (layerConfig.getId().equals(prefs.backgroundLayer())) {
                prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE);
            }
        } else {
            Log.e(DEBUG_TAG, "layerConfig should not be null here");
        }
    }
}
