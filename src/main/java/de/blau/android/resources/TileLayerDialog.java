package de.blau.android.resources;

import java.util.List;
import java.util.Map;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import ch.poole.android.numberpicker.library.NumberPicker;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.OAMCatalog.Entry;
import de.blau.android.resources.TileLayerServer.Category;
import de.blau.android.resources.TileLayerServer.Provider;
import de.blau.android.resources.TileLayerServer.Provider.CoverageArea;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.util.DatabaseUtil;
import de.blau.android.util.FileUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ReadFile;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Snack;

public class TileLayerDialog {

    protected static final String DEBUG_TAG = "TileLayerDialog";

    public interface OnUpdateListener {
        /**
         * This will be called after we have updated the database
         */
        void update();
    }

    /**
     * how a dialog for editing and saving a layer entry
     * 
     * @param activity Android Context
     * @param entry an entry from OAM
     * @param onUpdate call this if the DB has been updated
     */
    public static void showLayerDialog(@NonNull FragmentActivity activity, @Nullable Entry entry, @Nullable final OnUpdateListener onUpdate) {
        showLayerDialog(activity, -1, entry, onUpdate);
    }

    /**
     * Show a dialog for editing and saving a layer entry
     * 
     * @param activity Android Context
     * @param id the rowid of the layer entry in the database or -1 if not saved yet
     * @param oamEntry an entry from OAM or null
     * @param onUpdate call this if the DB has been updated
     */
    static void showLayerDialog(@NonNull final FragmentActivity activity, final int id, @Nullable Entry oamEntry, @Nullable final OnUpdateListener onUpdate) {
        final boolean existing = id > 0;
        final Preferences prefs = new Preferences(activity);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        final View templateView = LayoutInflater.from(activity).inflate(R.layout.layer_item, null);
        alertDialog.setView(templateView);

        final EditText nameEdit = (EditText) templateView.findViewById(R.id.name);
        final ImageButton fileButton = (ImageButton) templateView.findViewById(R.id.file_button);
        final EditText urlEdit = (EditText) templateView.findViewById(R.id.url);
        final CheckBox overlayCheck = (CheckBox) templateView.findViewById(R.id.overlay);
        final Spinner categorySpinner = (Spinner) templateView.findViewById(R.id.category);

        final NumberPicker minZoomPicker = (NumberPicker) templateView.findViewById(R.id.zoom_min);
        final NumberPicker maxZoomPicker = (NumberPicker) templateView.findViewById(R.id.zoom_max);

        TileLayerServer layer = null;
        String attribution = null;

        long startDate = -1;
        long endDate = -1;

        alertDialog.setTitle(R.string.add_layer_title);

        if (existing || oamEntry != null) {
            fileButton.setVisibility(View.GONE);

            if (oamEntry == null) {
                try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getReadableDatabase()) {
                    layer = TileLayerDatabase.getLayerWithRowId(activity, db, id);
                }
                nameEdit.setText(layer.getName());
                urlEdit.setText(layer.getOriginalTileUrl());
                overlayCheck.setChecked(layer.isOverlay());
                Category category = layer.getCategory();
                categorySpinner.setSelection(category != null ? category.ordinal() : Category.other.ordinal());
                minZoomPicker.setValue(layer.getMinZoomLevel());
                maxZoomPicker.setValue(layer.getMaxZoomLevel());
                List<CoverageArea> coverages = layer.getCoverage();
                if (coverages != null && !coverages.isEmpty()) {
                    BoundingBox box = coverages.get(0).getBoundingBox();
                    Log.d(DEBUG_TAG, "Coverage box " + box);
                    if (box != null) {
                        setBoundingBoxFields(templateView, box);
                    }
                }
            } else {
                nameEdit.setText(oamEntry.title);
                urlEdit.setText(oamEntry.tileUrl);
                minZoomPicker.setValue(TileLayerServer.DEFAULT_MIN_ZOOM);
                int maxZoom = TileLayerServer.DEFAULT_MAX_ZOOM;
                if (oamEntry.box != null) {
                    setBoundingBoxFields(templateView, oamEntry.box);
                    try {
                        double centerLat = (oamEntry.box.getBottom() + oamEntry.box.getHeight() / 2D) / 1E7D;
                        maxZoom = GeoMath.resolutionToZoom(oamEntry.gsd, centerLat);
                    } catch (IllegalArgumentException iaex) {
                        Log.e(DEBUG_TAG, "Got " + iaex.getMessage());
                    }
                }
                maxZoomPicker.setValue(maxZoom);
                startDate = oamEntry.startDate;
                endDate = oamEntry.endDate;
                if (oamEntry.provider != null) {
                    attribution = oamEntry.provider;
                    if (oamEntry.license != null) {
                        attribution += " " + oamEntry.license;
                    }
                }
                alertDialog.setNeutralButton(R.string.cancel, null);
            }

            if (existing) {
                final TileLayerServer finalLayer = layer;
                alertDialog.setTitle(R.string.edit_layer_title);
                alertDialog.setNeutralButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(DEBUG_TAG, "deleting layer " + Integer.toString(id));
                        TileLayerDatabaseView.removeLayerSelection(prefs, finalLayer);
                        try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                            TileLayerDatabase.deleteLayerWithRowId(db, id);
                        }
                        if (onUpdate != null) {
                            onUpdate.update();
                        }
                    }
                });
            } else {
                alertDialog.setNeutralButton(R.string.cancel, null);
            }
        } else {
            minZoomPicker.setValue(TileLayerServer.DEFAULT_MIN_ZOOM);
            maxZoomPicker.setValue(TileLayerServer.DEFAULT_MAX_ZOOM);

            fileButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    SelectFile.read(activity, R.string.config_mbtilesPreferredDir_key, new ReadFile() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean read(Uri fileUri) {
                            try {
                                // rewrite content: Uris
                                fileUri = FileUtil.contentUriToFileUri(activity, fileUri);
                                if (fileUri == null) {
                                    Snack.toastTopError(activity, R.string.not_found_title);
                                    return false;
                                }
                                if (!DatabaseUtil.isValidSQLite(fileUri.getPath())) {
                                    throw new SQLiteException("Not a SQLite database file");
                                }
                                MBTileProviderDataBase db = new MBTileProviderDataBase(activity, fileUri, 1);
                                Map<String, String> metadata = db.getMetadata();
                                if (metadata == null || metadata.isEmpty()) {
                                    throw new SQLiteException("MBTiles metadata missing");
                                }
                                int[] zooms = db.getMinMaxZoom();
                                db.close();

                                final String format = metadata.get(MBTileConstants.FORMAT);
                                if (!(MBTileConstants.PNG.equals(format) || MBTileConstants.JPG.equals(format))) {
                                    Snack.toastTopError(activity, activity.getResources().getString(R.string.toast_unsupported_format, format));
                                    return true;
                                }
                                urlEdit.setText(fileUri.toString());
                                String name = metadata.get(MBTileConstants.NAME);
                                if (name != null) {
                                    nameEdit.setText(name);
                                }
                                overlayCheck.setChecked(MBTileConstants.OVERLAY.equals(metadata.get(MBTileConstants.TYPE)));
                                String bounds = metadata.get(MBTileConstants.BOUNDS);
                                if (bounds != null) {
                                    String[] corners = bounds.split(",", 4);
                                    if (corners.length == 4) {
                                        setBoundingBoxFields(templateView, corners[0], corners[1], corners[2], corners[3]);
                                    }
                                }
                                if (zooms != null && zooms.length == 2) {
                                    minZoomPicker.setValue(zooms[0]);
                                    maxZoomPicker.setValue(zooms[1]);
                                }
                                SelectFile.savePref(prefs, R.string.config_mbtilesPreferredDir_key, fileUri);
                                return true;
                            } catch (SQLiteException sqex) {
                                Log.e(DEBUG_TAG, "Not a SQLite/MBTiles database " + fileUri + " " + sqex.getMessage());
                                Snack.toastTopError(activity, R.string.toast_not_mbtiles);
                                return false;
                            }
                        }
                    });
                }
            });

            alertDialog.setNeutralButton(R.string.cancel, null);
        }

        alertDialog.setNegativeButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dummy
            }
        });
        alertDialog.setPositiveButton(R.string.save_and_set, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dummy
            }
        });
        final TileLayerServer existingLayer = layer;

        final long finalStartDate = startDate;
        final long finalEndDate = endDate;

        final String finalAttribution = attribution;

        final AlertDialog dialog = alertDialog.create();

        class SaveListener implements View.OnClickListener {
            String  layerId   = null;
            boolean isOverlay = false;
            boolean saved     = false;

            @Override
            public void onClick(View v) {
                String name = nameEdit.getText().toString().trim();
                layerId = existing ? existingLayer.getId() : TileLayerServer.nameToId(name);
                isOverlay = overlayCheck.isChecked();
                Category category = Category.values()[categorySpinner.getSelectedItemPosition()];
                Provider provider = new Provider();
                String leftText = ((EditText) templateView.findViewById(R.id.left)).getText().toString().trim();
                String bottomText = ((EditText) templateView.findViewById(R.id.bottom)).getText().toString().trim();
                String rightText = ((EditText) templateView.findViewById(R.id.right)).getText().toString().trim();
                String topText = ((EditText) templateView.findViewById(R.id.top)).getText().toString().trim();
                int minZoom = minZoomPicker.getValue();
                int maxZoom = maxZoomPicker.getValue();
                boolean moan = false;
                if (!("".equals(leftText) || "".equals(bottomText) || "".equals(rightText) || "".equals(topText))) {
                    BoundingBox box = null;
                    try {
                        box = new BoundingBox(Double.parseDouble(leftText), Double.parseDouble(bottomText), Double.parseDouble(rightText),
                                Double.parseDouble(topText));
                        moan = !box.isValid();
                    } catch (NumberFormatException nfe) {
                        moan = true;
                    }
                    if (moan) {
                        Snack.toastTopError(activity, R.string.toast_invalid_box);
                    } else {
                        CoverageArea ca = new CoverageArea(minZoom, maxZoom, box);
                        provider.addCoverageArea(ca);
                    }
                }
                if (finalAttribution != null && !"".equals(finalAttribution)) {
                    provider.setAttribution(finalAttribution);
                }
                if ("".equals(name)) {
                    Snack.toastTopError(activity, R.string.toast_name_empty);
                    moan = true;
                }
                String tileUrl = urlEdit.getText().toString().trim();
                if ("".equals(tileUrl)) {
                    Snack.toastTopError(activity, R.string.toast_url_empty);
                    moan = true;
                }
                if (minZoom > maxZoom) {
                    Snack.toastTopError(activity, R.string.toast_min_zoom);
                    moan = true;
                }
                if (moan) { // abort and leave the dialog intact
                    return;
                }
                try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                    TileLayerServer.addOrUpdateCustomLayer(activity, db, layerId, existingLayer, finalStartDate, finalEndDate, name, provider, category,
                            minZoom, maxZoom, isOverlay, tileUrl);
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
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(saveListener);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new SaveListener() {
            @Override
            public void onClick(View v) {
                super.onClick(v);
                if (saved) {
                    if (isOverlay) {
                        prefs.setOverlayLayer(layerId);
                    } else {
                        prefs.setBackGroundLayer(layerId);
                    }
                }
            }
        });
    }

    /**
     * Set the TextViews for a BoundingBox
     * 
     * @param layout the parent layout of the TextViews
     * @param box the BoundingBox
     */
    private static void setBoundingBoxFields(View layout, BoundingBox box) {
        setBoundingBoxFields(layout, Double.toString(box.getLeft() / 1E7D), Double.toString(box.getBottom() / 1E7D), Double.toString(box.getRight() / 1E7D),
                Double.toString(box.getTop() / 1E7D));
    }

    /**
     * Set the TextViews for a BoundingBox
     * 
     * @param layout the parent layout of the TextViews
     * @param left coordinate of left side
     * @param bottom coordinate of bottom
     * @param right coordinate of right
     * @param top coordinate of top
     */
    private static void setBoundingBoxFields(View layout, String left, String bottom, String right, String top) {
        final EditText leftEdit = (EditText) layout.findViewById(R.id.left);
        final EditText bottomEdit = (EditText) layout.findViewById(R.id.bottom);
        final EditText rightEdit = (EditText) layout.findViewById(R.id.right);
        final EditText topEdit = (EditText) layout.findViewById(R.id.top);
        leftEdit.setText(left);
        bottomEdit.setText(bottom);
        rightEdit.setText(right);
        topEdit.setText(top);
    }
}