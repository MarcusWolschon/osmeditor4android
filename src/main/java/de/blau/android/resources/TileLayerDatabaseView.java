package de.blau.android.resources;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
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
import de.blau.android.views.layers.MapTilesLayer;

public class TileLayerDatabaseView {
    private static final String DEBUG_TAG = TileLayerDatabaseView.class.getSimpleName();

    private static final String DEBUG_LOG = "TileLayerDatabaseUI";

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
        View rulesetView = (View) LayoutInflater.from(activity).inflate(R.layout.layer_list, null);
        alertDialog.setTitle(R.string.custom_layer_title);
        alertDialog.setView(rulesetView);
        final SQLiteDatabase writableDb = new TileLayerDatabase(activity).getWritableDatabase();
        ListView layerList = (ListView) rulesetView.findViewById(R.id.listViewLayer);
        layerCursor = TileLayerDatabase.getAllCustomLayers(writableDb);
        layerAdapter = new LayerAdapter(writableDb, activity, layerCursor);
        layerList.setAdapter(layerAdapter);
        alertDialog.setNeutralButton(R.string.done, null);
        alertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                layerCursor.close();
                writableDb.close();
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
                        TileLayerDatabase.deleteLayerWithRowId(writableDb, id);
                        newLayerCursor(writableDb);
                        resetLayer(activity, writableDb);
                    }

                });
                alertDialog.show();
                return true;
            }
        });
        final FloatingActionButton fab = (FloatingActionButton) rulesetView.findViewById(R.id.add);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TileLayerDialog.showLayerDialog(activity, writableDb, -1, null, new TileLayerDialog.OnUpdateListener() {
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

        public LayerAdapter(final SQLiteDatabase db, final FragmentActivity activity, Cursor cursor) {
            super(activity, cursor, 0);
            this.db = db;
            this.activity = activity;
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
            view.setLongClickable(true);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer id = (Integer) view.getTag();
                    TileLayerDialog.showLayerDialog(activity, db, id != null ? id : -1, null, new TileLayerDialog.OnUpdateListener() {
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
     * Regenerate the in memory imagery configs
     * 
     * @param context Android Context
     * @param db a readable DB
     */
    protected static void resetLayer(Context context, SQLiteDatabase db) {
        TileLayerServer.getListsLocked(context, db, true);
        Logic logic = App.getLogic();
        if (logic != null) {
            MapTilesLayer layer = logic.getMap().getBackgroundLayer();
            if (layer != null) {
                layer.getTileProvider().update();
            }
        }
    }
}
