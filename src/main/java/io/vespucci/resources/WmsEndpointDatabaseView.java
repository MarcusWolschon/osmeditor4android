package io.vespucci.resources;

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
import android.widget.EditText;
import android.widget.Filter;
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
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.dialogs.Progress;
import io.vespucci.osm.Server;
import io.vespucci.resources.TileLayerDialog.OnUpdateListener;
import io.vespucci.resources.WmsCapabilities.Layer;
import io.vespucci.util.ExecutorTask;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.OnTextChangedWatcher;
import io.vespucci.util.ScreenMessage;
import io.vespucci.util.Util;
import io.vespucci.util.WidestItemArrayAdapter;

/**
 * WMS endpoint management UI
 */
public class WmsEndpointDatabaseView extends ImmersiveDialogFragment implements OnUpdateListener {
    private static final String DEBUG_TAG = WmsEndpointDatabaseView.class.getSimpleName().substring(0,
            Math.min(23, WmsEndpointDatabaseView.class.getSimpleName().length()));

    private EndpointAdapter endpointAdapter;

    private SQLiteDatabase writableDb;

    private static final String TAG = "fragment_wms_endpoints";

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
            WmsEndpointDatabaseView fragment = newInstance();
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
        io.vespucci.dialogs.Util.dismissDialog(parent, TAG);
    }

    /**
     * Create new instance of this object
     * 
     * @param result the List of Result elements
     * @return a WmsEndpointDatabaseView instance
     */
    private static WmsEndpointDatabaseView newInstance() {
        WmsEndpointDatabaseView f = new WmsEndpointDatabaseView();
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
        Cursor endpointCursor = TileLayerDatabase.getAllWmsEndPoints(writableDb);
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
            WmsEndpointDialog.showDialog(activity, idTag, () -> newLayerCursor(writableDb));
            return true;
        });
        final FloatingActionButton fab = (FloatingActionButton) endpointListView.findViewById(R.id.add);
        fab.setOnClickListener(v -> WmsEndpointDialog.showDialog(activity, -1, () -> newLayerCursor(writableDb)));
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
                new ExecutorTask<Void, Integer, WmsCapabilities>(logic.getExecutorService(), logic.getHandler()) {
                    @Override
                    protected void onPreExecute() {
                        Progress.showDialog(activity, Progress.PROGRESS_DOWNLOAD);
                    }

                    @Override
                    protected WmsCapabilities doInBackground(Void params) throws IOException, ParserConfigurationException, SAXException {
                        String url = Util.appendQuery(sanitize(endpoint.getTileUrl()),
                                REQUEST_PARAM + "=" + GET_CAPABILITIES_REQUEST + "&" + SERVICE_PARAM + "=wms");
                        try (InputStream is = Server.openConnection(activity, new URL(url))) {
                            return new WmsCapabilities(is);
                        }
                    }

                    @Override
                    protected void onBackgroundError(Exception e) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                        Log.e(DEBUG_TAG, e.getMessage());
                        ScreenMessage.toastTopError(context, activity.getString(R.string.toast_querying_wms_server_failed, e.getMessage()));
                    }

                    @Override
                    protected void onPostExecute(WmsCapabilities result) {
                        Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                        if (result.layers.isEmpty()) {
                            ScreenMessage.toastTopError(activity, R.string.toast_nothing_found);
                            return;
                        }
                        Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(R.string.select_layer_title);
                        builder.setNeutralButton(R.string.Done, null);
                        View layerListView = LayoutInflater.from(activity).inflate(R.layout.wms_layer_list, null);
                        ListView layerList = (ListView) layerListView.findViewById(R.id.listViewLayer);
                        EditText searchField = (EditText) layerListView.findViewById(R.id.searchField);
                        builder.setView(layerListView);
                        List<String> layers = new ArrayList<>();
                        for (Layer layer : result.layers) {
                            layers.add(layer.title);
                        }
                        FilteredAdapter adapter = new FilteredAdapter(activity, R.layout.layer_list_item, R.id.name, layers);
                        layerList.setAdapter(adapter);
                        layerList.setOnItemClickListener((parent, view, position, id) -> {
                            LayerEntry entry = new LayerEntry();
                            Layer layer = result.layers.get(position);
                            entry.title = layer.title;
                            entry.tileUrl = layer.getTileUrl(result.getGetMapUrl() != null ? result.getGetMapUrl() : sanitize(endpoint.getTileUrl()));
                            entry.box = layer.extent;
                            entry.gsd = layer.gsd;
                            TileLayerDialog.showDialog(WmsEndpointDatabaseView.this, -1, entry);
                        });
                        searchField.addTextChangedListener(
                                (OnTextChangedWatcher) (CharSequence cs, int start, int count, int after) -> adapter.getFilter().filter(cs));
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
        Cursor newCursor = TileLayerDatabase.getAllWmsEndPoints(db);
        Cursor oldCursor = endpointAdapter.swapCursor(newCursor);
        oldCursor.close();
        endpointAdapter.notifyDataSetChanged();
    }

    @Override
    public void update() {
        TileLayerDatabaseView.resetLayer(getActivity(), writableDb);
        TileLayerDialog.update(this);
    }

    private class FilteredAdapter extends WidestItemArrayAdapter<String> {
        private final List<String> originalObjects;

        public FilteredAdapter(Context context, int resource, int textView, List<String> objects) {
            super(context, resource, textView, objects);
            originalObjects = new ArrayList<>(objects);
        }

        @Override
        public Filter getFilter() {

            return new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    for (String v : (List<String>) results.values) {
                        add(v);
                    }
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    List<String> filteredNames = new ArrayList<>();

                    constraint = constraint.toString().toLowerCase();
                    for (String object : originalObjects) {
                        if (object.toLowerCase().contains(constraint.toString())) {
                            filteredNames.add(object);
                        }
                    }

                    results.count = filteredNames.size();
                    results.values = filteredNames;
                    return results;
                }
            };
        }
    }
}
