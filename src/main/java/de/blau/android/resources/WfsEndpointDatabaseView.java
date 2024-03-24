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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.dialogs.LayerStyle;
import de.blau.android.dialogs.Progress;
import de.blau.android.layer.LayerType;
import de.blau.android.osm.Server;
import de.blau.android.resources.TileLayerDialog.OnUpdateListener;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.resources.WfsCapabilities.Feature;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Util;

/**
 * WMS endpoint management UI
 */
public class WfsEndpointDatabaseView extends ImmersiveDialogFragment implements OnUpdateListener {
    private static final String DEBUG_TAG = WfsEndpointDatabaseView.class.getSimpleName().substring(0,
            Math.min(23, WfsEndpointDatabaseView.class.getSimpleName().length()));

    private EndpointAdapter endpointAdapter;

    private SQLiteDatabase writableDb;

    private static final String TAG = "fragment_wfs_endpoints";

    /**
     * Query the list of WMS endpoints catalog and display the results for selection
     * 
     * @param activity the calling Activity
     * 
     */
    public static void showDialog(@NonNull Fragment parent) {
        dismissDialog(parent);
        try {
            FragmentManager fm = parent.getChildFragmentManager();
            FragmentActivity activity = parent.getActivity();
            if (activity instanceof Main) {
                ((Main) activity).descheduleAutoLock();
            }
            WfsEndpointDatabaseView fragment = newInstance();
            fragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param parent the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull Fragment parent) {
        de.blau.android.dialogs.Util.dismissDialog(parent, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @param result the List of Result elements
     * @return a WmsEndpointDatabaseView instance
     */
    private static WfsEndpointDatabaseView newInstance() {
        WfsEndpointDatabaseView f = new WfsEndpointDatabaseView();
        Bundle args = new Bundle();

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        View endpointListView = LayoutInflater.from(activity).inflate(R.layout.layer_list, null);
        alertDialog.setTitle(R.string.wms_endpoints_title);
        alertDialog.setView(endpointListView);
        final TileLayerDatabase tlDb = new TileLayerDatabase(activity); // NOSONAR will be closed when dismissed
        writableDb = tlDb.getWritableDatabase();
        ListView endpointList = (ListView) endpointListView.findViewById(R.id.listViewLayer);
        Cursor endpointCursor = TileLayerDatabase.getEndPoints(writableDb, TileLayerSource.TYPE_WFS_ENDPOINT);
        endpointAdapter = new EndpointAdapter(writableDb, activity, endpointCursor);
        endpointList.setAdapter(endpointAdapter);
        alertDialog.setNeutralButton(R.string.done, null);
        alertDialog.setOnDismissListener(dialog -> {
            endpointCursor.close();
            writableDb.close();
            tlDb.close();
        });

        endpointList.setOnItemLongClickListener((parent, view, position, id) -> {
            final Integer idTag = (Integer) view.getTag();
            WmsEndpointDialog.showDialog(activity, TileLayerSource.TYPE_WFS_ENDPOINT, idTag, () -> newLayerCursor(writableDb));
            return true;
        });
        final FloatingActionButton fab = (FloatingActionButton) endpointListView.findViewById(R.id.add);
        fab.setOnClickListener(v -> WmsEndpointDialog.showDialog(activity, TileLayerSource.TYPE_WFS_ENDPOINT, -1, () -> newLayerCursor(writableDb)));
        return alertDialog.create();
    }

    private class EndpointAdapter extends CursorAdapter {
        private static final String SERVICE_PARAM            = "service";
        private static final String REQUEST_PARAM            = "request";
        private static final String GET_CAPABILITIES_REQUEST = "GetCapabilities";

        final SQLiteDatabase   db;
        final FragmentActivity activity;

        /**
         * A cursor adapter that binds Layers to Views
         * 
         * @param db an open db
         * @param activity the calling activity
         * @param cursor the Cursor
         * @param onUpdateListener call this if layer has been added
         */
        public EndpointAdapter(@NonNull final SQLiteDatabase db, @NonNull final FragmentActivity activity, @NonNull Cursor cursor) {
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
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            view.setTag(id);
            String name = cursor.getString(cursor.getColumnIndexOrThrow(TileLayerDatabase.NAME_FIELD));
            TextView nameView = (TextView) view.findViewById(R.id.name);
            nameView.setText(name);
            view.setLongClickable(true);
            view.setOnClickListener(v -> {
                Integer idTag = (Integer) view.getTag();
                final TileLayerSource endpoint = TileLayerDatabase.getLayerWithRowId(activity, db, idTag);
                Logic logic = App.getLogic();
                new ExecutorTask<Void, Integer, WfsCapabilities>(logic.getExecutorService(), logic.getHandler()) {
                    @Override
                    protected void onPreExecute() {
                        Progress.showDialog(activity, Progress.PROGRESS_DOWNLOAD);
                    }

                    @Override
                    protected WfsCapabilities doInBackground(Void params) throws IOException, ParserConfigurationException, SAXException {
                        String url = Util.appendQuery(sanitize(endpoint.getTileUrl()),
                                REQUEST_PARAM + "=" + GET_CAPABILITIES_REQUEST + "&" + SERVICE_PARAM + "=wfs");
                        try (InputStream is = Server.openConnection(activity, new URL(url))) {
                            return new WfsCapabilities(is);
                        }
                    }

                    @Override
                    protected void onBackgroundError(Exception e) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                        Log.e(DEBUG_TAG, e.getMessage());
                        ScreenMessage.toastTopError(context, activity.getString(R.string.toast_querying_wms_server_failed, e.getMessage()));
                        e.printStackTrace();
                    }

                    @Override
                    protected void onPostExecute(WfsCapabilities result) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                        if (result.features.isEmpty()) {
                            ScreenMessage.toastTopError(activity, R.string.toast_nothing_found);
                            return;
                        }
                        Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(R.string.select_layer_title);
                        builder.setNeutralButton(R.string.Done, null);
                        View layerListView = LayoutInflater.from(activity).inflate(R.layout.wms_layer_list, null);
                        ListView layerList = (ListView) layerListView.findViewById(R.id.listViewLayer);
                        builder.setView(layerListView);
                        List<String> layers = new ArrayList<>();
                        for (Feature feature : result.features) {
                            layers.add(feature.name);
                        }
                        FilteredAdapter adapter = new FilteredAdapter(activity, R.layout.layer_list_item, R.id.name, layers);
                        layerList.setAdapter(adapter);
                        layerList.setOnItemClickListener((parent, view, position, id) -> {
                            Feature feature = result.features.get(position);
                            String uriString = feature.getUrl();
                            Map map = App.getLogic().getMap();
                            de.blau.android.layer.StyleableLayer layer = (de.blau.android.layer.StyleableLayer) map.getLayer(LayerType.DATA, uriString);
                            if (layer == null) {

                                Log.d(DEBUG_TAG, "addStyleableLayerFromUri " + uriString);
                                de.blau.android.layer.Util.addLayer(activity, LayerType.DATA, uriString);

                                layer = (de.blau.android.layer.StyleableLayer) map.getLayer(LayerType.DATA, uriString);
                                // layer.entry.box = feature.extent;
                                // if (layer != null) { // if null setUpLayers will have toasted
                                // LayerStyle.showDialog(activity, layer.getIndex());
                                // layer.invalidate();
                                // }

                            } else {
                                ScreenMessage.toastTopWarning(activity, activity.getString(R.string.toast_styleable_layer_exists, uriString));
                            }
                        });
                        builder.create().show();
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
                if (!"".equals(n) && !REQUEST_PARAM.equalsIgnoreCase(n) && !SERVICE_PARAM.equalsIgnoreCase(n)) {
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
        Cursor newCursor = TileLayerDatabase.getEndPoints(db, TileLayerSource.TYPE_WFS_ENDPOINT);
        Cursor oldCursor = endpointAdapter.swapCursor(newCursor);
        oldCursor.close();
        endpointAdapter.notifyDataSetChanged();
    }

    @Override
    public void update() {
        TileLayerDatabaseView.resetLayer(getActivity(), writableDb);
        TileLayerDialog.update(this);
    }
}
