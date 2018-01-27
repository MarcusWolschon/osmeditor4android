package de.blau.android.resources;

import java.util.List;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;

public class OAMCatalogView {
    private static final String DEBUG_TAG = OAMCatalogView.class.getSimpleName();

    /**
     * Show a list of the layers in the database, selection will either load a template or start the edit dialog on it
     * 
     * @param activity Android context
     * @param catalog List of OAMCatalog.Entry
     * @param box a BoundingBox to search in or null for the whole world
     */
    static public void displayLayers(@NonNull final FragmentActivity activity, final List<OAMCatalog.Entry> catalog, final BoundingBox box) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View rulesetView = (View) LayoutInflater.from(activity).inflate(R.layout.oam_layer_list, null);
        dialogBuilder.setTitle(R.string.oam_layer_title);
        dialogBuilder.setView(rulesetView);
        ListView layerList = (ListView) rulesetView.findViewById(R.id.listViewLayer);

        dialogBuilder.setNeutralButton(R.string.done, null);
        dialogBuilder.setOnDismissListener(null);
        final TileLayerDatabase db = new TileLayerDatabase(activity);
        dialogBuilder.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                TileLayerDatabaseView.resetLayer(activity, db.getReadableDatabase());
                db.close();
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        ArrayAdapter<OAMCatalog.Entry> layerAdapter = new ArrayAdapter<OAMCatalog.Entry>(activity, android.R.layout.simple_list_item_1, catalog);
        layerList.setAdapter(layerAdapter);
        layerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                OAMCatalog.Entry entry = (OAMCatalog.Entry) catalog.get(position);

                TileLayerDialog.showLayerDialog(activity, db.getWritableDatabase(), entry, new TileLayerDialog.OnUpdateListener() {
                    @Override
                    public void update() {
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }
}
