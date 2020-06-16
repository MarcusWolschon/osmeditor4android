package de.blau.android.resources;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.R;
import de.blau.android.resources.TileLayerDialog.OnUpdateListener;
import de.blau.android.util.Snack;

public final class WmsEndpointDialog {
    
    /**
     * Private constructor to prevent instantiation
     */
    private WmsEndpointDialog() {
        // private
    }

    protected static final String DEBUG_TAG = WmsEndpointDialog.class.getSimpleName();

    /**
     * how a dialog for editing and saving a layer entry
     * 
     * @param activity Android Context
     * @param onUpdate call this if the DB has been updated
     */
    public static void showLayerDialog(@NonNull FragmentActivity activity, @Nullable final OnUpdateListener onUpdate) {
        showLayerDialog(activity, -1, onUpdate);
    }

    /**
     * Show a dialog for editing and saving a layer entry
     * 
     * @param activity Android Context
     * @param id the rowid of the layer entry in the database or -1 if not saved yet
     * @param onUpdate call this if the DB has been updated
     */
    static void showLayerDialog(@NonNull final FragmentActivity activity, final int id, @Nullable final OnUpdateListener onUpdate) {
        final boolean existing = id > 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View templateView = LayoutInflater.from(activity).inflate(R.layout.wms_endpoint_item, null);
        builder.setView(templateView);

        final EditText nameEdit = (EditText) templateView.findViewById(R.id.name);
        final EditText urlEdit = (EditText) templateView.findViewById(R.id.url);

        TileLayerSource endpoint = null;

        if (existing) {
            try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getReadableDatabase()) {
                endpoint = TileLayerDatabase.getLayerWithRowId(activity, db, id);
            }
            nameEdit.setText(endpoint.getName());
            urlEdit.setText(endpoint.getOriginalTileUrl());

            builder.setTitle(R.string.edit_wms_endpoint_title);
            if (TileLayerDatabase.SOURCE_MANUAL.equals(endpoint.getSource())) {
                builder.setNegativeButton(R.string.Delete, (dialog, which) -> {
                    Log.d(DEBUG_TAG, "deleting layer " + Integer.toString(id));
                    try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                        TileLayerDatabase.deleteLayerWithRowId(db, id);
                    }
                    if (onUpdate != null) {
                        onUpdate.update();
                    }
                });
            }
        } else {
            builder.setTitle(R.string.add_wms_endpoint_title);
        }

        builder.setNeutralButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            // dummy
        });

        final TileLayerSource existingEndpoint = endpoint;

        final AlertDialog dialog = builder.create();

        class SaveListener implements View.OnClickListener {
            String  endpointId = null;
            boolean saved      = false;

            @Override
            public void onClick(View v) {
                String name = nameEdit.getText().toString().trim();
                endpointId = existing ? existingEndpoint.getId() : TileLayerSource.nameToId(name);

                String endpointUrl = urlEdit.getText().toString().trim();
                if ("".equals(endpointUrl)) {
                    Snack.toastTopError(activity, R.string.toast_url_empty);
                    return;
                }
                try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                    TileLayerSource.addOrUpdateCustomLayer(activity, db, endpointId, existingEndpoint, -1, -1, name, null, null,
                            TileLayerSource.TYPE_WMS_ENDPOINT, -1, -1, false, endpointUrl);
                }
                if (onUpdate != null) {
                    onUpdate.update();
                }
                dialog.dismiss();
                saved = true;
            }
        }

        final OnClickListener saveListener = new SaveListener();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(saveListener);
    }
}