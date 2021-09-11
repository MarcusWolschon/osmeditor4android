package de.blau.android.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.osm.Server;
import de.blau.android.resources.TileLayerDialog.OnUpdateListener;
import de.blau.android.resources.WmsCapabilities.Layer;
import de.blau.android.util.Snack;
import de.blau.android.util.Util;

/**
 * WMS endpoint management UI
 */
public class WmsEndpointDatabaseView {
    private static final String DEBUG_TAG = WmsEndpointDatabaseView.class.getSimpleName();

    private EndpointAdapter endpointAdapter;

    /**
     * Show a list of the layers in the database, selection will either load a template or start the edit dialog on it
     * 
     * @param activity Android context
     * @param onUpdateListener call this if layer has been added
     */
    public void manageEndpoints(@NonNull final FragmentActivity activity, @Nullable OnUpdateListener onUpdateListener) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        View endpointListView = LayoutInflater.from(activity).inflate(R.layout.layer_list, null);
        alertDialog.setTitle(R.string.wms_endpoints_title);
        alertDialog.setView(endpointListView);
        final TileLayerDatabase tlDb = new TileLayerDatabase(activity); // NOSONAR will be closed when dismissed
        final SQLiteDatabase writableDb = tlDb.getWritableDatabase();
        ListView endpointList = (ListView) endpointListView.findViewById(R.id.listViewLayer);
        Cursor endpointCursor = TileLayerDatabase.getAllWmsEndPoints(writableDb);
        endpointAdapter = new EndpointAdapter(writableDb, activity, endpointCursor, onUpdateListener);
        endpointList.setAdapter(endpointAdapter);
        alertDialog.setNeutralButton(R.string.done, null);
        alertDialog.setOnDismissListener(dialog -> {
            endpointCursor.close();
            writableDb.close();
            tlDb.close();
        });

        endpointList.setOnItemLongClickListener((parent, view, position, id) -> {
            final Integer idTag = (Integer) view.getTag();
            WmsEndpointDialog.showLayerDialog(activity, idTag, () -> newLayerCursor(writableDb));
            return true;
        });
        final FloatingActionButton fab = (FloatingActionButton) endpointListView.findViewById(R.id.add);
        fab.setOnClickListener(v -> WmsEndpointDialog.showLayerDialog(activity, -1, () -> newLayerCursor(writableDb)));
        alertDialog.show();
    }

    private class EndpointAdapter extends CursorAdapter {
        final SQLiteDatabase   db;
        final FragmentActivity activity;
        final OnUpdateListener onUpdateListener;

        /**
         * A cursor adapter that binds Layers to Views
         * 
         * @param db an open db
         * @param activity the calling activity
         * @param cursor the Cursor
         * @param onUpdateListener call this if layer has been added
         */
        public EndpointAdapter(@NonNull final SQLiteDatabase db, @NonNull final FragmentActivity activity, @NonNull Cursor cursor,
                final @Nullable OnUpdateListener onUpdateListener) {
            super(activity, cursor, 0);
            this.db = db;
            this.activity = activity;
            this.onUpdateListener = onUpdateListener;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            return LayoutInflater.from(context).inflate(R.layout.layer_list_item, parent, false);
        }

        @Override
        public void bindView(final View view, final Context context, Cursor cursor) {
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            view.setTag(id);
            String name = cursor.getString(cursor.getColumnIndexOrThrow(TileLayerDatabase.NAME_FIELD));

            TextView nameView = (TextView) view.findViewById(R.id.name);
            nameView.setText(name);
            view.setLongClickable(true);
            view.setOnClickListener(v -> {
                Integer idTag = (Integer) view.getTag();
                final TileLayerSource endpoint = TileLayerDatabase.getLayerWithRowId(activity, db, idTag);
                new AsyncTask<Void, Integer, WmsCapabilities>() {
                    @Override
                    protected void onPreExecute() {
                        Progress.showDialog(activity, Progress.PROGRESS_DOWNLOAD);
                    }

                    @Override
                    protected WmsCapabilities doInBackground(Void... params) {
                        String url = Util.appendQuery(sanitize(endpoint.getTileUrl()), "request=GetCapabilities&service=wms");
                        try (InputStream is = Server.openConnection(activity, new URL(url))) {
                            return new WmsCapabilities(is);
                        } catch (IOException | ParserConfigurationException | SAXException e) {
                            Log.e(DEBUG_TAG, e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(WmsCapabilities result) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                        if (result != null) {
                            Builder builder = new AlertDialog.Builder(activity);
                            builder.setTitle(R.string.select_layer_title);
                            builder.setNeutralButton(R.string.Done, null);
                            View layerListView = LayoutInflater.from(activity).inflate(R.layout.wms_layer_list, null);
                            ListView layerList = (ListView) layerListView.findViewById(R.id.listViewLayer);
                            builder.setView(layerListView);
                            List<String> layers = new ArrayList<>();
                            for (Layer layer : result.layers) {
                                layers.add(layer.title);
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.layer_list_item, R.id.name, layers);
                            layerList.setAdapter(adapter);
                            layerList.setOnItemClickListener((parent, view, position, id) -> {
                                LayerEntry entry = new LayerEntry();
                                Layer layer = result.layers.get(position);
                                entry.title = layer.title;
                                entry.tileUrl = layer.getTileUrl(result.getGetMapUrl() != null ? result.getGetMapUrl() : sanitize(endpoint.getTileUrl()));
                                entry.box = layer.extent;
                                entry.gsd = layer.gsd;
                                TileLayerDialog.showLayerDialog(activity, -1, entry, () -> {
                                    TileLayerDatabaseView.resetLayer(activity, db);
                                    if (onUpdateListener != null) {
                                        onUpdateListener.update();
                                    }
                                });
                            });

                            builder.create().show();
                        } else {
                            Snack.toastTopError(activity, R.string.toast_nothing_found);
                        }
                    }
                }.execute();
            });
        }

        /**
         * Remove WMS request and service parameters
         * 
         * @param url the url to sanitize
         * @return the sanitized url string
         */
        @NonNull
        private String sanitize(@NonNull String url) {
            Uri uri = Uri.parse(url);
            Uri.Builder uriBuilder = uri.buildUpon();
            uriBuilder.clearQuery();
            for (String n : uri.getQueryParameterNames()) {
                if (!"request".equalsIgnoreCase(n) && !"service".equalsIgnoreCase(n)) {
                    uriBuilder.appendQueryParameter(n, uri.getQueryParameter(n));
                }
            }
            return uriBuilder.build().toString();
        }
    }

    /**
     * Replace the current cursor
     * 
     * @param db the database
     */
    private void newLayerCursor(@NonNull final SQLiteDatabase db) {
        Cursor newCursor = TileLayerDatabase.getAllWmsEndPoints(db);
        Cursor oldCursor = endpointAdapter.swapCursor(newCursor);
        oldCursor.close();
        endpointAdapter.notifyDataSetChanged();
    }
}
