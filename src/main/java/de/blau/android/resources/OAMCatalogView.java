package de.blau.android.resources;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.TileLayerDialog.OnUpdateListener;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

public final class OAMCatalogView extends CancelableDialogFragment implements OnUpdateListener {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OAMCatalogView.class.getSimpleName().length());
    private static final String DEBUG_TAG = OAMCatalogView.class.getSimpleName().substring(0, TAG_LEN);

    private static final String BOUNDING_BOX_KEY = "boundingbox";

    private static final String TAG = "fragment_oam_catalog";

    /**
     * Query the OAM catalog and display the results for selection
     * 
     * @param activity the calling Activity
     * @param box a BoundingBox to search in or null for the whole world
     * 
     */
    public static void showDialog(@NonNull Fragment parent, @Nullable final BoundingBox box) {
        dismissDialog(parent);
        try {
            FragmentManager fm = parent.getChildFragmentManager();
            FragmentActivity activity = parent.getActivity();
            if (activity instanceof Main) {
                ((Main) activity).descheduleAutoLock();
            }
            OAMCatalogView fragment = newInstance(box);
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
     * @return a TileLayerDialog instance
     */
    private static OAMCatalogView newInstance(@Nullable final BoundingBox box) {
        OAMCatalogView f = new OAMCatalogView();
        Bundle args = new Bundle();
        args.putSerializable(BOUNDING_BOX_KEY, box);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    /**
     * Show a list of OAMCatalog.Entry(s)
     * 
     * @param activity Android context
     * @param catalog List of OAMCatalog.Entry
     * @param box a BoundingBox to search in or null for the whole world
     * @param updateListener a TileLayerDialog.OnUpdateListener to execute or null
     */
    private void displayLayers(@NonNull final ListView layerList, @NonNull final List<LayerEntry> catalog) {
        ArrayAdapter<LayerEntry> layerAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, catalog);
        layerList.setAdapter(layerAdapter);
        layerList.setOnItemClickListener((parent, view, position, id) -> {
            LayerEntry entry = catalog.get(position);
            TileLayerDialog.showDialog(OAMCatalogView.this, entry);
        });
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = savedInstanceState == null ? getArguments() : savedInstanceState;

        BoundingBox box = Util.getSerializeable(bundle, BOUNDING_BOX_KEY, BoundingBox.class);

        FragmentActivity activity = getActivity();
        AlertDialog.Builder dialogBuilder = ThemeUtils.getAlertDialogBuilder(activity);
        View layerListView = LayoutInflater.from(activity).inflate(R.layout.oam_layer_list, null);
        dialogBuilder.setTitle(R.string.oam_layer_title);
        dialogBuilder.setView(layerListView);
        ListView layerList = (ListView) layerListView.findViewById(R.id.listViewLayer);

        dialogBuilder.setNeutralButton(R.string.done, null);

        dialogBuilder.setOnDismissListener(d -> {
            try (final TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getReadableDatabase()) {
                TileLayerDatabaseView.resetLayer(activity, db);
            }
        });

        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, List<LayerEntry>>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected void onPreExecute() {
                Progress.showDialog(activity, Progress.PROGRESS_QUERY_OAM);
            }

            @Override
            protected List<LayerEntry> doInBackground(Void param) {
                OAMCatalog catalog = new OAMCatalog();
                List<LayerEntry> list = null;
                try {
                    list = catalog.getEntries(activity, App.getLogic().getPrefs().getOAMServer(), box);
                    final int found = catalog.getFound();
                    final int limit = catalog.getLimit();
                    if (found > limit) {
                        activity.runOnUiThread(
                                () -> ScreenMessage.toastTopWarning(activity, activity.getString(R.string.toast_returning_less_than_found, limit, found)));
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
                    displayLayers(layerList, catalog);
                } else {
                    ScreenMessage.toastTopInfo(activity, R.string.toast_nothing_found);
                }
            }
        }.execute();

        return dialogBuilder.create();
    }

    @Override
    public void update() {
        getDialog().dismiss();
        try (final TileLayerDatabase tlDb = new TileLayerDatabase(getActivity()); SQLiteDatabase db = tlDb.getReadableDatabase()) {
            TileLayerDatabaseView.resetLayer(getActivity(), db);
        }
        TileLayerDialog.update(this);
    }
}
