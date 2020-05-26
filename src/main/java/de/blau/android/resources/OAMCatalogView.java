package de.blau.android.resources;

import java.io.IOException;
import java.util.List;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.dialogs.Progress;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

public final class OAMCatalogView {
    private static final String DEBUG_TAG = OAMCatalogView.class.getSimpleName();

    /**
     * Private constructor to stop instantiation
     */
    private OAMCatalogView() {
        // private
    }

    /**
     * Show a list of OAMCatalog.Entry(s)
     * 
     * @param activity Android context
     * @param catalog List of OAMCatalog.Entry
     * @param box a BoundingBox to search in or null for the whole world
     * @param updateListener a TileLayerDialog.OnUpdateListener to execute or null
     */
    public static void displayLayers(@NonNull final FragmentActivity activity, @NonNull final List<LayerEntry> catalog, @Nullable final BoundingBox box,
            @Nullable final TileLayerDialog.OnUpdateListener updateListener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View layerListView = LayoutInflater.from(activity).inflate(R.layout.oam_layer_list, null);
        dialogBuilder.setTitle(R.string.oam_layer_title);
        dialogBuilder.setView(layerListView);
        ListView layerList = (ListView) layerListView.findViewById(R.id.listViewLayer);

        dialogBuilder.setNeutralButton(R.string.done, null);

        dialogBuilder.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                try (final TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getReadableDatabase()) {
                    TileLayerDatabaseView.resetLayer(activity, db);
                }
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        ArrayAdapter<LayerEntry> layerAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, catalog);
        layerList.setAdapter(layerAdapter);
        layerList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LayerEntry entry = catalog.get(position);
                TileLayerDialog.showLayerDialog(activity, entry, new TileLayerDialog.OnUpdateListener() {
                    @Override
                    public void update() {
                        dialog.dismiss();
                        try (final TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getReadableDatabase()) {
                            TileLayerDatabaseView.resetLayer(activity, db);
                        }
                        if (updateListener != null) {
                            updateListener.update();
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    /**
     * Query the OAM catalog and display the results for selection
     * 
     * @param activity the calling Activity
     * @param box a BoundingBox to search in or null for the whole world
     * @param updateListener a TileLayerDialog.OnUpdateListener to execute or null
     */
    public static void queryAndSelectLayers(@NonNull FragmentActivity activity, @Nullable final BoundingBox box,
            @Nullable final TileLayerDialog.OnUpdateListener updateListener) {
        new AsyncTask<Void, Void, List<LayerEntry>>() {
            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_QUERY_OAM);
            }

            @Override
            protected List<LayerEntry> doInBackground(Void... params) {
                OAMCatalog catalog = new OAMCatalog();
                List<LayerEntry> list = null;
                try {
                    list = catalog.getEntries(activity, Urls.OAM_SERVER, box);
                    final int found = catalog.getFound();
                    final int limit = catalog.getLimit();
                    if (found > limit) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snack.toastTopWarning(activity, activity.getString(R.string.toast_returning_less_than_found, limit, found));
                            }
                        });
                    }
                } catch (final IOException iox) {
                    Log.e(DEBUG_TAG, "Add imagery from oam " + iox.getMessage());
                    Util.toastDowloadError(activity, iox);
                }
                return list;
            }

            @Override
            protected void onPostExecute(List<LayerEntry> catalog) {
                Progress.dismissDialog(activity, Progress.PROGRESS_QUERY_OAM);
                if (catalog != null && !catalog.isEmpty()) {
                    OAMCatalogView.displayLayers(activity, catalog, box, updateListener);
                } else {
                    Snack.toastTopInfo(activity, R.string.toast_nothing_found);
                }
            }
        }.execute();
    }
}
