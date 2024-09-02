package de.blau.android.resources;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import ch.poole.android.numberpicker.library.NumberPicker;
import ch.poole.geo.pmtiles.Constants;
import ch.poole.geo.pmtiles.Reader;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.FileExtensions;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.Tip;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.resources.TileLayerSource.Provider.CoverageArea;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.services.util.MBTileProviderDataBase;
import de.blau.android.util.ContentResolverUtil;
import de.blau.android.util.DatabaseUtil;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.FileUtil;
import de.blau.android.util.FragmentUtil;
import de.blau.android.util.GeoMath;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.OkHttpFileChannel;
import de.blau.android.util.ReadFile;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SelectFile;
import de.blau.android.util.Util;
import okhttp3.OkHttpClient;

public class TileLayerDialog extends ImmersiveDialogFragment {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, TileLayerDialog.class.getSimpleName().length());
    protected static final String DEBUG_TAG = TileLayerDialog.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_layer_dialog";

    private static final String ID_KEY          = "results";
    private static final String LAYER_ENTRY_KEY = "layerEntry";

    private static final String SOURCE_TYPE = "sourceType";
    private static final String TILE_TYPE   = "tileType";
    private static final String ATTRIBUTION = "attribution";

    public interface OnUpdateListener {
        /**
         * This will be called after we have updated the database
         */
        void update();
    }

    /**
     * Show a dialog to add or edit a custom imagery layer
     * 
     * @param parent Android Context
     * @param entry an entry from OAM, WMS or null
     * @param onUpdate call this if the DB has been updated
     */
    public static void showDialog(@NonNull Fragment parent, @Nullable LayerEntry entry) {
        showDialog(parent, -1, entry);
    }

    /**
     * Show a dialog to add or edit a custom imagery layer
     * 
     * @param parent the calling FragmentActivity
     * @param result the List of Result elements
     */
    public static void showDialog(@NonNull Fragment parent, final long id, @Nullable LayerEntry layerEntry) {
        dismissDialog(parent);
        try {
            FragmentManager fm = parent.getChildFragmentManager();
            FragmentActivity activity = parent.getActivity();
            if (activity instanceof Main) {
                ((Main) activity).descheduleAutoLock();
            }
            TileLayerDialog fragment = newInstance(id, layerEntry);

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
    private static TileLayerDialog newInstance(final long id, @Nullable LayerEntry layerEntry) {
        TileLayerDialog f = new TileLayerDialog();
        Bundle args = new Bundle();
        args.putLong(ID_KEY, id);
        args.putSerializable(LAYER_ENTRY_KEY, layerEntry);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    private long       id = -1;
    private LayerEntry layerEntry;

    private EditText     nameEdit;
    private CheckBox     overlayCheck;
    private NumberPicker minZoomPicker;
    private NumberPicker maxZoomPicker;
    private EditText     urlEdit;
    private NumberPicker tileSizePicker;
    private Spinner      categorySpinner;
    private EditText     leftEdit;
    private EditText     bottomEdit;
    private EditText     rightEdit;
    private EditText     topEdit;

    private long            startDate = -1;
    private long            endDate   = -1;
    private boolean         layerExists;
    private TileLayerSource layer     = null;

    FragmentActivity activity;

    final Map<String, Object> metadataMap = new HashMap<>();

    final OnClickListener readFileListener = v -> SelectFile.read(getActivity(), R.string.config_mbtilesPreferredDir_key, new ReadFile() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean read(FragmentActivity currentActivity, final Uri contentUri) {
            TileLayerDialog fragment = (TileLayerDialog) FragmentUtil.findFragmentByTag(currentActivity, TAG);
            if (fragment == null) {
                Log.e(DEBUG_TAG, "Restored fragment is null");
                return false;
            }
            // on Android API 29 and up we need to copy the file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // copy file
                String fileName = ContentResolverUtil.getDisplaynameColumn(currentActivity, contentUri);
                try {
                    final File destination = new File(FileUtil.getPublicDirectory(FileUtil.getPublicDirectory(), Paths.DIRECTORY_PATH_IMPORTS), fileName);
                    FileUtil.importFile(currentActivity, contentUri, destination,
                            () -> configureFromFile(fragment, Uri.parse(FileUtil.FILE_SCHEME_PREFIX + destination.getAbsolutePath())));
                } catch (IOException ex) {
                    return false;
                }
                return true;
            } else {
                // rewrite content: Uris
                final Uri fileUri = FileUtil.contentUriToFileUri(currentActivity, contentUri);
                if (fileUri == null) {
                    ScreenMessage.toastTopError(currentActivity, R.string.not_found_title);
                    return false;
                }
                return configureFromFile(fragment, fileUri);
            }
        }
    });

    /**
     * Configure a pmtiles source from the data in the file
     * 
     * @param fragment the current, potentially recreated instance of this
     * @param reader a Reader instance
     * @param json the json metadata from the Reader
     */
    private void configureFromPMTiles(TileLayerDialog fragment, @NonNull Reader reader, @NonNull String json) {
        metadataMap.put(SOURCE_TYPE, TileLayerSource.TYPE_PMT_3);
        metadataMap.put(TILE_TYPE, Constants.TYPE_MVT == reader.getTileType() ? TileType.MVT : null);
        double[] box = reader.getBounds();
        setBoundingBoxFields(fragment, formatDouble(box[0]), formatDouble(box[1]), formatDouble(box[2]), formatDouble(box[3]));
        minZoomPicker.setValue(reader.getMinZoom());
        maxZoomPicker.setValue(reader.getMaxZoom());
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonObject()) {
                JsonObject metaData = ((JsonObject) root);
                JsonElement name = metaData.get(Constants.METADATA_NAME);
                if (name != null) {
                    nameEdit.setText(name.getAsString());
                }
                JsonElement layerType = metaData.get(Constants.METADATA_TYPE);
                if (layerType != null) {
                    overlayCheck.setChecked(Constants.METADATA_TYPE_OVERLAY.equals(layerType.getAsString()));
                }
                JsonElement attribution = metaData.get(Constants.METADATA_ATTRIBUTION);
                if (attribution != null) {
                    metadataMap.put(ATTRIBUTION, attribution.getAsString());
                }
            }
        } catch (JsonSyntaxException jsex) {
            // do nothing
        }
    }

    /**
     * Configure a MapBoxTiles source from the file metadata
     * 
     * @param fragment the current, potentially recreated instance of this
     * @param fileUri the uri for the file
     */
    private void configureFromMBT(@NonNull TileLayerDialog fragment, @NonNull Uri fileUri) {
        MBTileProviderDataBase db = new MBTileProviderDataBase(getActivity(), fileUri, 1);
        Map<String, String> metadata = db.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            throw new SQLiteException("MBTiles metadata missing");
        }
        int[] zooms = db.getMinMaxZoom();
        if (zooms.length == 2) {
            fragment.minZoomPicker.setValue(zooms[0]);
            fragment.maxZoomPicker.setValue(zooms[1]);
        }
        db.close();

        final String format = metadata.get(MBTileConstants.FORMAT);
        final boolean isMVT = MBTileConstants.PBF.equals(format);
        if (!(MBTileConstants.PNG.equals(format) || MBTileConstants.JPG.equals(format) || isMVT)) {
            ScreenMessage.toastTopError(activity, getString(R.string.toast_unsupported_format, format));
            return;
        }
        fragment.metadataMap.put(TILE_TYPE, isMVT ? TileType.MVT : null);

        String name = metadata.get(MBTileConstants.NAME);
        if (name != null) {
            fragment.nameEdit.setText(name);
        }
        fragment.overlayCheck.setChecked(MBTileConstants.OVERLAY.equals(metadata.get(MBTileConstants.TYPE)));
        String bounds = metadata.get(MBTileConstants.BOUNDS);
        if (bounds != null) {
            String[] corners = bounds.split(",", 4);
            if (corners.length == 4) {
                setBoundingBoxFields(fragment, corners[0], corners[1], corners[2], corners[3]);
            }
        }
    }

    /**
     * Configure the entry from the contents of the file
     * 
     * @param fragment the current, potentially recreated instance of this
     * @param fileUri the file Uri for the file
     * 
     * @return true if successful
     */
    private boolean configureFromFile(@NonNull TileLayerDialog fragment, @NonNull Uri fileUri) {
        try {
            final String path = fileUri.getPath();
            if (DatabaseUtil.isValidSQLite(path)) {
                configureFromMBT(fragment, fileUri);
            } else {
                try (Reader reader = new Reader(new File(path))) {
                    configureFromPMTiles(fragment, reader, reader.getMetadata());
                }
            }
            // this should really be in the metadata
            fragment.tileSizePicker.setValue(TileLayerSource.DEFAULT_TILE_SIZE);
            fragment.urlEdit.setText(fileUri.toString());
            SelectFile.savePref(App.getLogic().getPrefs(), R.string.config_mbtilesPreferredDir_key, fileUri);
            return true;
        } catch (JsonSyntaxException e) {
            Log.e(DEBUG_TAG, "Invalid JSON metadata " + e.getMessage());
        } catch (SQLiteException | IOException sqex) {
            Log.e(DEBUG_TAG, "Not a SQLite/MBTiles database or PMTiles file " + fileUri + " " + sqex.getMessage());
        }
        ScreenMessage.toastTopError(activity, R.string.toast_not_mbtiles);
        return false;
    }

    private class SaveListener implements View.OnClickListener {
        String  layerId   = null;
        boolean isOverlay = false;

        /**
         * Get the bounding box
         * 
         * @param topText
         * @param rightText
         * @param bottomText
         * @param leftText
         * 
         * @return the box or null if invalid
         */
        @Nullable
        private BoundingBox getBoundingBox(String leftText, String bottomText, String rightText, String topText) {
            try {
                BoundingBox box = new BoundingBox(Double.parseDouble(leftText), Double.parseDouble(bottomText), Double.parseDouble(rightText),
                        Double.parseDouble(topText));
                if (box.isValid()) {
                    return box;
                }
            } catch (NumberFormatException nfe) {
                // return null is enough
            }
            return null;
        }

        /**
         * Actually save the layer
         * 
         * @return true if successful
         */
        boolean save() {
            String name = nameEdit.getText().toString().trim();
            final boolean emptyName = "".equals(name);
            String tileUrl = urlEdit.getText().toString().trim();
            if (emptyName && tileUrl.endsWith("." + FileExtensions.PMTILES) && tileUrl.startsWith(Schemes.HTTP)) {
                configureFromRemote(tileUrl);
                return false;
            }

            layerId = layerExists ? layer.getId() : TileLayerSource.nameToId(name);
            isOverlay = overlayCheck.isChecked();
            Category category = Category.values()[categorySpinner.getSelectedItemPosition()];
            Provider provider = new Provider();

            int minZoom = minZoomPicker.getValue();
            int maxZoom = maxZoomPicker.getValue();

            String leftText = leftEdit.getText().toString().trim();
            String bottomText = bottomEdit.getText().toString().trim();
            String rightText = rightEdit.getText().toString().trim();
            String topText = topEdit.getText().toString().trim();
            try {
                if (!("".equals(leftText) && "".equals(bottomText) && "".equals(rightText) && "".equals(topText))) {
                    BoundingBox box = getBoundingBoxFromText(leftText, bottomText, rightText, topText);
                    CoverageArea ca = new CoverageArea(minZoom, maxZoom, box);
                    provider.addCoverageArea(ca);
                }
                String attribution = (String) metadataMap.get(ATTRIBUTION);
                if (Util.notEmpty(attribution)) {
                    // strip html
                    provider.setAttribution(Util.fromHtml(attribution).toString());
                }
                if (emptyName) {
                    throw new IllegalArgumentException(getString(R.string.toast_name_empty));
                }
                checkTileServerUrl(tileUrl);
                String proj = TileLayerSource.projFromUrl(tileUrl);
                if (proj != null && !TileLayerSource.supportedProjection(proj)) {
                    throw new IllegalArgumentException(getString(R.string.toast_unsupported_projection, proj));
                }
                if (isOverlay && (tileUrl.contains(WmsCapabilities.IMAGE_JPEG) || tileUrl.contains("." + FileExtensions.JPG))) {
                    throw new IllegalArgumentException(getString(R.string.toast_jpeg_not_transparent));
                }
                if (minZoom > maxZoom) {
                    throw new IllegalArgumentException(getString(R.string.toast_min_zoom));
                }
                int tileSize = tileSizePicker.getValue();

                try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                    TileLayerSource existing = TileLayerDatabase.getLayerWithUrl(activity, db, tileUrl);
                    if (existing != null && !existing.getId().equals(layerId)) {
                        // we are not editing the same entry
                        throw new IllegalArgumentException(getString(R.string.toast_tile_layer_exists, existing.getName()));
                    }
                    TileLayerSource.addOrUpdateCustomLayer(activity, db, layerId, layer, startDate, endDate, name, provider, category,
                            (String) metadataMap.get(SOURCE_TYPE), (TileType) metadataMap.get(TILE_TYPE), minZoom, maxZoom, tileSize, isOverlay, tileUrl);
                }
                if (TileLayerSource.is3857compatible(proj) && tileSize == TileLayerSource.DEFAULT_TILE_SIZE) {
                    Tip.showOptionalDialog(activity, R.string.tip_wms_tile_size_key, R.string.tip_wms_tile_size);
                }
            } catch (IllegalArgumentException iaex) { // abort and leave the dialog intact
                ScreenMessage.toastTopError(activity, iaex.getMessage());
                return false;
            }
            return true;
        }

        /**
         * Get a BoundingBox from the the left/bottom/right/top text values
         * 
         * @param leftText left
         * @param bottomText bottom
         * @param rightText tight
         * @param topText top
         * @return a BoundingBox
         */
        @NonNull
        private BoundingBox getBoundingBoxFromText(String leftText, String bottomText, String rightText, String topText) {
            BoundingBox box = getBoundingBox(leftText, bottomText, rightText, topText);
            if (box == null) {
                throw new IllegalArgumentException(getString(R.string.toast_invalid_box));
            }
            return box;
        }

        /**
         * Check that the tile server url is actually valid
         * 
         * @param tileUrl the url with placeholders
         */
        private void checkTileServerUrl(@NonNull String tileUrl) {
            if ("".equals(tileUrl)) {
                throw new IllegalArgumentException(getString(R.string.toast_url_empty));
            }
            if (hasPlaceholder(tileUrl, TileLayerSource.PROJ_PLACEHOLDER) || hasPlaceholder(tileUrl, TileLayerSource.WKID_PLACEHOLDER)) {
                throw new IllegalArgumentException(getString(R.string.toast_url_config_file_placeholders));
            }
        }

        /**
         * Check if the supplied tile url contains the specified placeholder
         * 
         * @param tileUrl the url
         * @param placeholder the placeholder
         * @return true if tileUrl contains placeholder in curly brackets
         */
        private boolean hasPlaceholder(@NonNull String tileUrl, @NonNull String placeholder) {
            return tileUrl.indexOf(TileLayerSource.PLACEHOLDER_START + placeholder + TileLayerSource.PLACEHOLDER_END) >= 0;
        }

        @Override
        public void onClick(View v) {
            saveAndUpdate(false);
        }

        /**
         * Configure from remote information
         * 
         * @param tileUrl the Url of the source
         */
        private void configureFromRemote(String tileUrl) {
            new ExecutorTask<Void, Void, Void>() {
                Reader reader;
                String json;

                @Override
                protected void onPreExecute() {
                    Progress.showDialog(activity, Progress.PROGRESS_DOWNLOAD);
                }

                @Override
                protected Void doInBackground(Void input) throws Exception {
                    // try to get info from network source
                    OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                            .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
                    reader = new Reader(new OkHttpFileChannel(client, tileUrl));
                    json = reader.getMetadata();
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                    configureFromPMTiles(TileLayerDialog.this, reader, json);
                }

                @Override
                protected void onBackgroundError(Exception e) {
                    Progress.dismissDialog(activity, Progress.PROGRESS_DOWNLOAD);
                    ScreenMessage.toastTopError(activity, getString(R.string.toast_unable_to_configure_from_source, e.getLocalizedMessage()));
                }

            }.execute();
        }

        /**
         * Save and call the appropriate OnUpdateListener
         * 
         * @param set if true make this the current layer
         */
        private void saveAndUpdate(boolean set) {
            if (save()) {
                if (set) {
                    try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                        de.blau.android.layer.Util.addImageryLayer(db, db.getLayers(), isOverlay, layerId);
                    }
                }
                // update the in memory lists async
                new ExecutorTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void input) throws Exception {
                        de.blau.android.layer.Util.populateImageryLists(activity);
                        return null;
                    }
                }.execute();
                update(TileLayerDialog.this);
                getDialog().dismiss();
            }
        }
    }

    /**
     * Update the parent
     * 
     * @param child the current fragment
     */
    public static void update(@NonNull Fragment child) {
        Object listener = child.getParentFragment();
        if (!(listener instanceof OnUpdateListener)) {
            listener = child.getContext();
        }
        if (listener instanceof OnUpdateListener) {
            ((OnUpdateListener) listener).update();
        } else {
            Log.e(DEBUG_TAG, "OnUpdateListener not found");
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = savedInstanceState == null ? getArguments() : savedInstanceState;
        id = bundle.getLong(ID_KEY);
        layerEntry = Util.getSerializeable(bundle, LAYER_ENTRY_KEY, LayerEntry.class);

        layerExists = id > 0;

        activity = getActivity();

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        View templateView = LayoutInflater.from(activity).inflate(R.layout.layer_item, null);
        alertDialog.setView(templateView);

        nameEdit = (EditText) templateView.findViewById(R.id.name);
        final ImageButton fileButton = (ImageButton) templateView.findViewById(R.id.file_button);
        urlEdit = (EditText) templateView.findViewById(R.id.url);
        overlayCheck = (CheckBox) templateView.findViewById(R.id.overlay);
        categorySpinner = (Spinner) templateView.findViewById(R.id.category);

        minZoomPicker = (NumberPicker) templateView.findViewById(R.id.zoom_min);
        maxZoomPicker = (NumberPicker) templateView.findViewById(R.id.zoom_max);

        leftEdit = (EditText) templateView.findViewById(R.id.left);
        bottomEdit = (EditText) templateView.findViewById(R.id.bottom);
        rightEdit = (EditText) templateView.findViewById(R.id.right);
        topEdit = (EditText) templateView.findViewById(R.id.top);

        tileSizePicker = (NumberPicker) templateView.findViewById(R.id.tile_size);

        alertDialog.setTitle(R.string.add_layer_title);

        if (layerExists || layerEntry != null) {
            // existing data in some form
            fileButton.setVisibility(View.GONE);

            if (layerEntry == null) {
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
                if (!coverages.isEmpty()) {
                    BoundingBox box = coverages.get(0).getBoundingBox();
                    Log.d(DEBUG_TAG, "Coverage box " + box);
                    if (box != null) {
                        setBoundingBoxFields(this, box);
                    }
                }
                tileSizePicker.setValue(layer.getTileWidth());
                final TileLayerSource finalLayer = layer;
                alertDialog.setTitle(R.string.edit_layer_title);
                alertDialog.setNeutralButton(R.string.Delete, (dialog, which) -> {
                    Log.d(DEBUG_TAG, "deleting layer " + Long.toString(id));
                    TileLayerDatabaseView.removeLayerSelection(activity, finalLayer);
                    try (TileLayerDatabase tlDb = new TileLayerDatabase(activity); SQLiteDatabase db = tlDb.getWritableDatabase()) {
                        TileLayerDatabase.deleteLayerWithRowId(db, id);
                    }
                    update(this);
                });
            } else {
                nameEdit.setText(layerEntry.title);
                urlEdit.setText(layerEntry.tileUrl);
                minZoomPicker.setValue(TileLayerSource.DEFAULT_MIN_ZOOM);
                int maxZoom = TileLayerSource.DEFAULT_MAX_ZOOM;
                if (layerEntry.box != null) {
                    setBoundingBoxFields(this, layerEntry.box);
                    try {
                        double centerLat = (layerEntry.box.getBottom() + layerEntry.box.getHeight() / 2D) / 1E7D;
                        maxZoom = GeoMath.resolutionToZoom(layerEntry.gsd, centerLat);
                    } catch (IllegalArgumentException iaex) {
                        Log.e(DEBUG_TAG, "Got " + iaex.getMessage());
                    }
                }
                maxZoomPicker.setValue(maxZoom);
                startDate = layerEntry.startDate;
                endDate = layerEntry.endDate;
                if (layerEntry.provider != null) {
                    String attribution = layerEntry.provider;
                    if (layerEntry.license != null) {
                        attribution += " " + layerEntry.license;
                    }
                    metadataMap.put(ATTRIBUTION, attribution);
                }
                tileSizePicker.setValue(TileLayerSource.DEFAULT_TILE_SIZE);
                alertDialog.setNeutralButton(R.string.cancel, null);
            }
        } else {
            minZoomPicker.setValue(TileLayerSource.DEFAULT_MIN_ZOOM);
            maxZoomPicker.setValue(TileLayerSource.DEFAULT_MAX_ZOOM);

            fileButton.setOnClickListener(readFileListener);

            alertDialog.setNeutralButton(R.string.cancel, null);
        }

        alertDialog.setNegativeButton(R.string.save, (dialog, which) -> {
            // dummy
        });
        alertDialog.setPositiveButton(R.string.save_and_set, (dialog, which) -> {
            // dummy
        });

        final AlertDialog dialog = alertDialog.create();

        final OnClickListener saveListener = new SaveListener();

        dialog.setOnShowListener((DialogInterface d) -> {
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(saveListener);
            if (layer != null) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.GONE);
            } else {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new SaveListener() {
                    @Override
                    public void onClick(View v) {
                        ((SaveListener) saveListener).saveAndUpdate(true);
                    }
                });
            }

        });

        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        savedState.putLong(ID_KEY, id);
        savedState.putSerializable(LAYER_ENTRY_KEY, layerEntry);
    }

    /**
     * Get a double as a String with appropriate precision
     * 
     * @param d the double
     * @return a String representation of the double
     */
    @NonNull
    private String formatDouble(double d) {
        try {
            return BigDecimal.valueOf(d).setScale(5, RoundingMode.DOWN).toString();
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "Formatting failed " + e.getMessage());
            return "";
        }
    }

    /**
     * Set the TextViews for a BoundingBox
     * 
     * @param fragment the current, potentially recreated instance of this
     * @param box the BoundingBox
     */
    private void setBoundingBoxFields(@NonNull TileLayerDialog fragment, @NonNull BoundingBox box) {
        setBoundingBoxFields(fragment, formatDouble(box.getLeft() / 1E7D), formatDouble(box.getBottom() / 1E7D), formatDouble(box.getRight() / 1E7D),
                formatDouble(box.getTop() / 1E7D));
    }

    /**
     * Set the TextViews for a BoundingBox
     * 
     * @param fragment the current, potentially recreated instance of this
     * @param left coordinate of left side
     * @param bottom coordinate of bottom
     * @param right coordinate of right
     * @param top coordinate of top
     */
    private void setBoundingBoxFields(@NonNull TileLayerDialog fragment, @NonNull String left, @NonNull String bottom, @NonNull String right,
            @NonNull String top) {
        fragment.leftEdit.setText(left);
        fragment.bottomEdit.setText(bottom);
        fragment.rightEdit.setText(right);
        fragment.topEdit.setText(top);
    }
}