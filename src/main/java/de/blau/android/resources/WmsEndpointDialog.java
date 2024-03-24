package de.blau.android.resources;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import android.content.DialogInterface;
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
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.ThemeUtils;

public final class WmsEndpointDialog {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, WmsEndpointDialog.class.getSimpleName().length());
    protected static final String DEBUG_TAG = WmsEndpointDialog.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Private constructor to prevent instantiation
     */
    private WmsEndpointDialog() {
        // private
    }

    /**
     * Show a dialog for editing and saving a layer entry
     * 
     * @param activity Android Context
     * @param endPointType TODO
     * @param id the rowid of the layer entry in the database or -1 if not saved yet
     * @param onUpdate call this if the DB has been updated
     */
    static void showDialog(@NonNull final FragmentActivity activity, final String endPointType, final int id, @Nullable final OnUpdateListener onUpdate) {
        final boolean existing = id > 0;
        AlertDialog.Builder builder = ThemeUtils.getAlertDialogBuilder(activity);
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
            String endpointId = null;

            @Override
            public void onClick(View v) {
                String name = nameEdit.getText().toString().trim();
                endpointId = existing ? existingEndpoint.getId() : TileLayerSource.nameToId(name);

                String endpointUrl = urlEdit.getText().toString().trim();
                if ("".equals(endpointUrl)) {
                    ScreenMessage.toastTopError(activity, R.string.toast_url_empty);
                    return;
                }
                try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                    TileLayerSource.addOrUpdateCustomLayer(activity, db, endpointId, existingEndpoint, -1, -1, name, null, null,
                            endPointType, null, -1, -1, TileLayerSource.WMS_TILE_SIZE, false, endpointUrl);
                }
                if (onUpdate != null) {
                    onUpdate.update();
                }
                dialog.dismiss();
            }
        }

        final OnClickListener saveListener = new SaveListener();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(saveListener);
    }
}