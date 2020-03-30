package de.blau.android.resources;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.TileLayerServer.Category;
import de.blau.android.resources.TileLayerServer.Provider;
import de.blau.android.resources.TileLayerServer.Provider.CoverageArea;
import de.blau.android.util.collections.MultiHashMap;

public class TileLayerDatabase extends SQLiteOpenHelper {
    private static final String DEBUG_TAG        = "TileLayerDatabase";
    public static final String  DATABASE_NAME    = "tilelayers";
    private static final int    DATABASE_VERSION = 6;

    public static final String SOURCE_JOSM_IMAGERY    = "JOSM Imagery Sources";    // josm.openstreetmap.de/wiki/maps
    public static final String SOURCE_CUSTOM = "custom"; // user added tile layers from file
    public static final String SOURCE_MANUAL = "manual"; // user added tile layer

    private static final String SOURCES_TABLE = "sources";
    static final String         NAME_FIELD    = "name";
    private static final String UPDATED_FIELD = "updated";

    public static final String  LAYERS_TABLE             = "layers";
    private static final String ID_FIELD                 = "id";
    private static final String TYPE_FIELD               = "server_type";
    private static final String CATEGORY_FIELD           = "category";
    private static final String SOURCE_FIELD             = "source";
    private static final String TILE_URL_FIELD           = "url";
    private static final String TOU_URI_FIELD            = "tou_url";
    private static final String ATTRIBUTION_URL_FIELD    = "attribution_url";
    private static final String ATTRIBUTION_FIELD        = "attribution";
    private static final String OVERLAY_FIELD            = "overlay";
    private static final String DEFAULTLAYER_FIELD       = "default_layer";
    private static final String ZOOM_MIN_FIELD           = "zoom_min";
    private static final String ZOOM_MAX_FIELD           = "zoom_max";
    private static final String OVER_ZOOM_MAX_FIELD      = "over_zoom_max";
    private static final String TILE_WIDTH_FIELD         = "tile_width";
    private static final String TILE_HEIGHT_FIELD        = "tile_height";
    private static final String PROJ_FIELD               = "proj";
    private static final String PREFERENCE_FIELD         = "preference";
    private static final String START_DATE_FIELD         = "start_date";
    private static final String END_DATE_FIELD           = "end_date";
    private static final String NO_TILE_HEADER_FIELD     = "no_tile_header";
    private static final String NO_TILE_VALUE_FIELD      = "no_tile_value";
    private static final String LOGO_URL_FIELD           = "logo_url";
    private static final String LOGO_FIELD               = "logo";
    private static final String DESCRIPTION_FIELD        = "description";
    private static final String PRIVACY_POLICY_URL_FIELD = "privacy_policy_url";

    public static final String  COVERAGES_TABLE = "coverages";
    private static final String LEFT_FIELD      = "left";
    private static final String BOTTOM_FIELD    = "bottom";
    private static final String RIGHT_FIELD     = "right";
    private static final String TOP_FIELD       = "top";

    static final String QUERY_LAYER_BY_ROWID = "SELECT * FROM layers WHERE rowid=?";

    /**
     * Create a new instance of TileLayerDatabase creating the underlying DB is necessary
     * 
     * @param context Android Context
     */
    public TileLayerDatabase(@NonNull final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE sources (name TEXT NOT NULL PRIMARY KEY, updated INTEGER)");
            addSource(db, SOURCE_JOSM_IMAGERY);
            addSource(db, SOURCE_CUSTOM);
            addSource(db, SOURCE_MANUAL);

            db.execSQL(
                    "CREATE TABLE layers (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, server_type TEXT NOT NULL, category TEXT DEFAULT NULL, source TEXT NOT NULL, url TEXT NOT NULL,"
                            + " tou_url TEXT, attribution TEXT, overlay INTEGER NOT NULL DEFAULT 0,"
                            + " default_layer INTEGER NOT NULL DEFAULT 0, zoom_min INTEGER NOT NULL DEFAULT 0, zoom_max INTEGER NOT NULL DEFAULT 18,"
                            + " over_zoom_max INTEGER NOT NULL DEFAULT 4, tile_width INTEGER NOT NULL DEFAULT 256, tile_height INTEGER NOT NULL DEFAULT 256,"
                            + " proj TEXT DEFAULT NULL, preference INTEGER NOT NULL DEFAULT 0, start_date INTEGER DEFAULT NULL, end_date INTEGER DEFAULT NULL,"
                            + " no_tile_header TEXT DEFAULT NULL, no_tile_value TEXT DEFAULT NULL,  logo_url TEXT DEFAULT NULL, logo BLOB DEFAULT NULL,"
                            + " description TEXT DEFAULT NULL, privacy_policy_url TEXT DEFAULT NULL, attribution_url TEXT DEFAULT NULL, FOREIGN KEY(source) REFERENCES sources(name) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX layers_overlay_idx ON layers(overlay)");
            db.execSQL("CREATE INDEX layers_source_idx ON layers(source)");
            db.execSQL("CREATE TABLE coverages (id TEXT NOT NULL, zoom_min INTEGER NOT NULL DEFAULT 0, zoom_max INTEGER NOT NULL DEFAULT 18,"
                    + " left INTEGER DEFAULT NULL, bottom INTEGER DEFAULT NULL, right INTEGER DEFAULT NULL, top INTEGER DEFAULT NULL,"
                    + " FOREIGN KEY(id) REFERENCES layers(id) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX coverages_idx ON coverages(id)");
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion <= 1 && newVersion >= 2) {
            addSource(db, SOURCE_MANUAL);
        }
        if (oldVersion <= 2 && newVersion >= 3) {
            db.execSQL("ALTER TABLE layers ADD COLUMN no_tile_header TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE layers ADD COLUMN no_tile_value TEXT DEFAULT NULL");
        }
        if (oldVersion <= 3 && newVersion >= 4) {
            db.execSQL("ALTER TABLE layers ADD COLUMN description TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE layers ADD COLUMN privacy_policy_url TEXT DEFAULT NULL");
        }
        if (oldVersion <= 4 && newVersion >= 5) {
            db.execSQL("ALTER TABLE layers ADD COLUMN category TEXT DEFAULT NULL");
        }
        if (oldVersion <= 5 && newVersion >= 6) {
            db.execSQL("ALTER TABLE layers ADD COLUMN attribution_url TEXT DEFAULT NULL");
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    /**
     * Add an entry to the source table
     * 
     * @param db writable ruleset database
     * @param source name of the source to add
     */
    public static void addSource(@NonNull SQLiteDatabase db, @NonNull String source) {
        ContentValues values = new ContentValues();
        values.put(NAME_FIELD, source);
        db.insert(SOURCES_TABLE, null, values);
    }

    /**
     * Get the updated value for a source
     * 
     * @param db readable database will be closed by caller
     * @param source name of the source
     * @return a milliseconds since th epoch value or 0 if not set
     */
    public static long getSourceUpdate(@NonNull SQLiteDatabase db, @NonNull String source) {
        Cursor dbresult = db.query(SOURCES_TABLE, null, NAME_FIELD + "='" + source + "'", null, null, null, null);
        try {
            if (dbresult.getCount() >= 1) {
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    return dbresult.getLong(dbresult.getColumnIndex(UPDATED_FIELD));
                }
            }
            return 0;
        } finally {
            dbresult.close();
        }
    }

    /**
     * Delete a specific source which will delete all layers from that source
     * 
     * @param db writable database
     * @param source name of the entry
     * @param updated time in milliseconds when we updated
     */
    public static void updateSource(@NonNull final SQLiteDatabase db, @NonNull String source, long updated) {
        Log.d(DEBUG_TAG, "Updating " + source + " " + updated);
        ContentValues values = new ContentValues();
        values.put(UPDATED_FIELD, updated);
        db.update(SOURCES_TABLE, values, "name='" + source + "'", null);
    }

    /**
     * Delete a specific source which will delete all layers from that source
     * 
     * @param db writable database
     * @param source name of the entry
     */
    public static void deleteSource(@NonNull final SQLiteDatabase db, @NonNull String source) {
        db.delete(SOURCES_TABLE, NAME_FIELD + "=?", new String[] { source });
    }

    /**
     * Add a layer, will add coverage areas to the coverage table
     * 
     * @param db writable database
     * @param source source the layer comes from
     * @param layer a TileLayerServer object
     */
    public static void addLayer(@NonNull SQLiteDatabase db, @NonNull String source, @NonNull TileLayerServer layer) {
        ContentValues values = getContentValuesForLayer(source, layer);
        try {
            db.insertOrThrow(LAYERS_TABLE, null, values);
            // Log.d(DEBUG_TAG, "Added layer from " + source + ": " + layer);
            addCoverageFromLayer(db, layer);
        } catch (SQLiteConstraintException e) {
            // even when in a transaction only this insert will get rolled back
            Log.e(DEBUG_TAG, "Constraint exception " + layer.getId() + " " + e.getMessage());
        }
    }

    /**
     * Add coverage entries
     * 
     * @param db writable database
     * @param layer a TileLayerServer instance
     */
    private static void addCoverageFromLayer(@NonNull SQLiteDatabase db, @NonNull TileLayerServer layer) {
        // insert coverage areas
        List<CoverageArea> coverages = layer.getCoverage();
        if (coverages != null) {
            for (CoverageArea ca : coverages) {
                addCoverage(db, layer.getId(), ca);
            }
        }
    }

    /**
     * Get an ContentValues object suitable for insertion or an update of a layer
     * 
     * @param source the source of the layer, use null if this is an update
     * @param layer TileLayerServer object holding the valuse
     * @return a ContentValues object
     */
    private static ContentValues getContentValuesForLayer(@Nullable String source, @NonNull TileLayerServer layer) {
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, layer.getId());
        values.put(NAME_FIELD, layer.getName());
        values.put(TYPE_FIELD, layer.getType());
        Category category = layer.getCategory();
        if (category != null) {
            values.put(CATEGORY_FIELD, category.name());
        }
        if (source != null) {
            values.put(SOURCE_FIELD, source);
        }
        values.put(TILE_URL_FIELD, layer.getOriginalTileUrl());
        values.put(TOU_URI_FIELD, layer.getTouUri());
        values.put(ATTRIBUTION_URL_FIELD, layer.getAttributionUrl());
        values.put(ATTRIBUTION_FIELD, layer.getAttribution());
        values.put(OVERLAY_FIELD, layer.isOverlay() ? 1 : 0);
        values.put(DEFAULTLAYER_FIELD, layer.isDefaultLayer() ? 1 : 0);
        if (!TileLayerServer.TYPE_BING.equals(layer.getType())) { // bing layer gets these values dynamically
            values.put(ZOOM_MIN_FIELD, layer.getMinZoomLevel());
            values.put(ZOOM_MAX_FIELD, layer.getMaxZoomLevel());
            values.put(TILE_WIDTH_FIELD, layer.getTileWidth());
            values.put(TILE_HEIGHT_FIELD, layer.getTileHeight());
        }
        values.put(OVER_ZOOM_MAX_FIELD, layer.getMaxOverZoom());
        values.put(PROJ_FIELD, layer.getProj());
        values.put(PREFERENCE_FIELD, layer.getPreference());
        values.put(START_DATE_FIELD, layer.getStartDate());
        values.put(END_DATE_FIELD, layer.getEndDate());
        values.put(NO_TILE_HEADER_FIELD, layer.getNoTileHeader());
        if (layer.getNoTileValues() != null) {
            // okhttp returns header values with quotes so we add them here
            StringBuilder storedValues = new StringBuilder();
            boolean first = true;
            for (String v : layer.getNoTileValues()) {
                if (!first) {
                    storedValues.append('|');
                } else {
                    first = false;
                }
                storedValues.append(v);
            }
            values.put(NO_TILE_VALUE_FIELD, storedValues.toString());
        }
        values.put(LOGO_URL_FIELD, layer.getLogoUrl());
        Bitmap logo = layer.getLogo();
        if (logo != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            logo.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            values.put(LOGO_FIELD, byteArray);
        }
        values.put(DESCRIPTION_FIELD, layer.getDescription());
        values.put(PRIVACY_POLICY_URL_FIELD, layer.getPrivacyPolicyUrl());
        return values;
    }

    /**
     * Deletes all layers with a specific source which will delete all coverages for the layers
     * 
     * @param db writable database
     * @param source name of the source
     */
    public static void deleteLayers(@NonNull final SQLiteDatabase db, @NonNull String source) {
        db.delete(LAYERS_TABLE, SOURCE_FIELD + "=?", new String[] { source });
    }

    /**
     * Retrieve a single layer identified by its id
     * 
     * @param context Android Context
     * @param db readable SQLiteDatabase
     * @param id the layer id
     * @return a TileLayerServer instance of null if none could be found
     */
    public static TileLayerServer getLayer(@NonNull Context context, @NonNull SQLiteDatabase db, @NonNull String id) {
        TileLayerServer layer = null;
        Cursor dbresult = db.query(COVERAGES_TABLE, null, ID_FIELD + "='" + id + "'", null, null, null, null);
        Provider provider = getProviderFromCursor(dbresult);

        dbresult = db.query(LAYERS_TABLE, null, ID_FIELD + "='" + id + "'", null, null, null, null);
        if (dbresult.getCount() >= 1) {
            boolean haveEntry = dbresult.moveToFirst();
            if (haveEntry) {
                initLayerFieldIndices(dbresult);
                layer = getLayerFromCursor(context, provider, dbresult);
            }
        }
        dbresult.close();
        return layer;
    }

    /**
     * Retrieve a single layer identified by its mysql rowid
     * 
     * @param context Androic Context
     * @param db readable SWLiteDatabase
     * @param rowId the mysql rowid
     * @return a TileLayerServer instance of null if none could be found
     */
    public static TileLayerServer getLayerWithRowId(@NonNull Context context, @NonNull SQLiteDatabase db, @NonNull int rowId) {
        TileLayerServer layer = null;
        Cursor dbresult = db.rawQuery(
                "SELECT coverages.id as id,left,bottom,right,top,coverages.zoom_min as zoom_min,coverages.zoom_max as zoom_max FROM layers,coverages WHERE layers.rowid=? AND layers.id=coverages.id",
                new String[] { Integer.toString(rowId) });
        Provider provider = getProviderFromCursor(dbresult);

        dbresult = db.rawQuery(QUERY_LAYER_BY_ROWID, new String[] { Integer.toString(rowId) });
        if (dbresult.getCount() >= 1) {
            boolean haveEntry = dbresult.moveToFirst();
            if (haveEntry) {
                initLayerFieldIndices(dbresult);
                layer = getLayerFromCursor(context, provider, dbresult);
            }
        }
        dbresult.close();
        return layer;
    }

    /**
     * Create Provider object containing CoverageAreas from a Cursor
     * 
     * @param cursor the Cursor
     * @return a Provider instance
     */
    private static Provider getProviderFromCursor(Cursor cursor) {
        Provider provider = new Provider();
        if (cursor.getCount() >= 1) {
            Log.d(DEBUG_TAG, "Got 1 or more coverage areas");
            boolean haveEntry = cursor.moveToFirst();
            initCoverageFieldIndices(cursor);
            while (haveEntry) {
                CoverageArea ca = getCoverageFromCursor(cursor);
                provider.addCoverageArea(ca);
                haveEntry = cursor.moveToNext();
            }
        }
        cursor.close();
        return provider;
    }

    /**
     * Update an existing layer in the database
     * 
     * @param db a writable SQLiteDatabase
     * @param layer the layer to write to the database
     */
    public static void updateLayer(@NonNull SQLiteDatabase db, @NonNull TileLayerServer layer) {
        String id = layer.getId();
        Log.d(DEBUG_TAG, "Updating layer " + id);
        deleteCoverage(db, id);
        ContentValues values = getContentValuesForLayer(null, layer);
        db.update(LAYERS_TABLE, values, "id=?", new String[] { id });
        addCoverageFromLayer(db, layer);
    }

    /**
     * Delete the layer with the SQLite rowid
     * 
     * @param db a writable SQLiteDatabase
     * @param rowId the rowId
     */
    public static void deleteLayerWithRowId(@NonNull SQLiteDatabase db, int rowId) {
        db.delete(LAYERS_TABLE, "layers.rowid=?", new String[] { Integer.toString(rowId) });
    }

    private static int idFieldIndex      = -1;
    private static int leftFieldIndex    = -1;
    private static int bottomFieldIndex  = -1;
    private static int rightFieldIndex   = -1;
    private static int topFieldIndex     = -1;
    private static int zoomMinFieldIndex = -1;
    private static int zoomMaxFieldIndex = -1;

    /**
     * Create a CoverageArea from a Cursor
     * 
     * Uses pre-computed field indices
     * 
     * @param cursor the Cursor
     * @return a CoverageArea instance
     */
    private static CoverageArea getCoverageFromCursor(@NonNull Cursor cursor) {
        if (idFieldIndex == -1) {
            throw new IllegalStateException("Coverage field indices not initialized");
        }
        int left = cursor.getInt(leftFieldIndex);
        int bottom = cursor.getInt(bottomFieldIndex);
        int right = cursor.getInt(rightFieldIndex);
        int top = cursor.getInt(topFieldIndex);
        BoundingBox box = new BoundingBox(left, bottom, right, top);
        int zoomMin = cursor.getInt(zoomMinFieldIndex);
        int zoomMax = cursor.getInt(zoomMaxFieldIndex);
        return new CoverageArea(zoomMin, zoomMax, box);
    }

    /**
     * Init the field indices for a Coverage Cursor
     * 
     * @param cursor the Cursor
     */
    private static synchronized void initCoverageFieldIndices(@NonNull Cursor cursor) {
        idFieldIndex = cursor.getColumnIndex(ID_FIELD);
        leftFieldIndex = cursor.getColumnIndex(LEFT_FIELD);
        bottomFieldIndex = cursor.getColumnIndex(BOTTOM_FIELD);
        rightFieldIndex = cursor.getColumnIndex(RIGHT_FIELD);
        topFieldIndex = cursor.getColumnIndex(TOP_FIELD);
        zoomMinFieldIndex = cursor.getColumnIndex(ZOOM_MIN_FIELD);
        zoomMaxFieldIndex = cursor.getColumnIndex(ZOOM_MAX_FIELD);
    }

    /**
     * Get a Cursor for all layers
     * 
     * @param db a readable SQLiteDatabase
     * @return a Cursor pointing to all imagery layers
     */
    static Cursor getAllLayers(@NonNull SQLiteDatabase db) {
        return db.rawQuery("SELECT layers.rowid as _id, name FROM layers", null);
    }

    /**
     * Get a Cursor for all user defined layers
     * 
     * @param db a readable SQLiteDatabase
     * @return a Cursor pointing to all custom imagery layers
     */
    static Cursor getAllCustomLayers(@NonNull SQLiteDatabase db) {
        return db.rawQuery("SELECT layers.rowid as _id, name FROM layers WHERE source=? OR source=?", new String[] { SOURCE_CUSTOM, SOURCE_MANUAL });
    }

    /**
     * Get all layers of a specific type
     * 
     * @param context Android Context
     * @param db a readable SQLiteDatabase
     * @param overlay if true only overlay layers will be returned
     * @return a Map containing the selected layers
     */
    public static Map<String, TileLayerServer> getAllLayers(@NonNull Context context, @NonNull SQLiteDatabase db, boolean overlay) {
        Map<String, TileLayerServer> layers = new HashMap<>();
        MultiHashMap<String, CoverageArea> coverages = new MultiHashMap<>();
        Cursor dbresult = db.rawQuery(
                "SELECT coverages.id as id,left,bottom,right,top,coverages.zoom_min as zoom_min,coverages.zoom_max as zoom_max FROM layers,coverages WHERE coverages.id=layers.id AND overlay=?",
                new String[] { overlay ? "1" : "0" });
        if (dbresult.getCount() >= 1) {
            initCoverageFieldIndices(dbresult);
            boolean haveEntry = dbresult.moveToFirst();
            while (haveEntry) {
                String id = dbresult.getString(idFieldIndex);
                CoverageArea ca = getCoverageFromCursor(dbresult);
                coverages.add(id, ca);
                haveEntry = dbresult.moveToNext();
            }
        }
        dbresult.close();

        dbresult = db.query(LAYERS_TABLE, null, OVERLAY_FIELD + "=" + (overlay ? 1 : 0), null, null, null, null);
        if (dbresult.getCount() >= 1) {
            boolean haveEntry = dbresult.moveToFirst();
            initLayerFieldIndices(dbresult);
            while (haveEntry) {
                String id = dbresult.getString(idLayerFieldIndex);
                Provider provider = new Provider();
                for (CoverageArea ca : coverages.get(id)) {
                    provider.addCoverageArea(ca);
                }
                TileLayerServer layer = getLayerFromCursor(context, provider, dbresult);
                if (layer.replaceApiKey(context)) { // if we have an apikey parameter and can't replace it, don't add
                    layers.put(id, layer);
                } else {
                    Log.e(DEBUG_TAG, "layer " + id + " is missing an apikey, not added");
                }
                haveEntry = dbresult.moveToNext();
            }
        }
        dbresult.close();

        return layers;
    }

    static int idLayerFieldIndex          = -1;
    static int nameFieldIndex             = -1;
    static int typeFieldIndex             = -1;
    static int categoryFieldIndex         = -1;
    static int tileUrlFieldIndex          = -1;
    static int touUrlFieldIndex           = -1;
    static int attributionUrlFieldIndex   = -1;
    static int attributionFieldIndex      = -1;
    static int overlayFieldIndex          = -1;
    static int defaultLayerFieldIndex     = -1;
    static int zoomMinLayerFieldIndex     = -1;
    static int zoomMaxLayerFieldIndex     = -1;
    static int tileWidthFieldIndex        = -1;
    static int tileHeightFieldIndex       = -1;
    static int projFieldIndex             = -1;
    static int preferenceFieldIndex       = -1;
    static int startDateFieldIndex        = -1;
    static int endDateIFieldndex          = -1;
    static int noTileHeaderIndex          = -1;
    static int noTileValueIndex           = -1;
    static int overZoomMaxFieldIndex      = -1;
    static int logoUrlFieldIndex          = -1;
    static int logoFieldIndex             = -1;
    static int descriptionFieldIndex      = -1;
    static int privacyPolicyUrlFieldIndex = -1;

    /**
     * Create a TileLayerServer from a database entry
     * 
     * Uses pre-computed field indices
     * 
     * @param context Android Context
     * @param provider Provider object holding coverage and attribution
     * @param cursor the Cursor
     * @return a TileLayerServer instance
     */
    private static TileLayerServer getLayerFromCursor(@NonNull Context context, @NonNull Provider provider, @NonNull Cursor cursor) {
        if (idLayerFieldIndex == -1) {
            throw new IllegalStateException("Layer field indices not initialized");
        }
        String id = cursor.getString(idLayerFieldIndex);
        String name = cursor.getString(nameFieldIndex);
        String type = cursor.getString(typeFieldIndex);
        String categoryString = cursor.getString(categoryFieldIndex);
        Category category = null;
        if (categoryString != null) {
            category = Category.valueOf(categoryString);
        }
        String tileUrl = cursor.getString(tileUrlFieldIndex);
        String touUri = cursor.getString(touUrlFieldIndex);
        provider.setAttributionUrl(cursor.getString(attributionUrlFieldIndex));
        provider.setAttribution(cursor.getString(attributionFieldIndex));
        boolean overlay = cursor.getInt(overlayFieldIndex) == 1;
        boolean defaultLayer = cursor.getInt(defaultLayerFieldIndex) == 1;
        int zoomLevelMin = cursor.getInt(zoomMinLayerFieldIndex);
        int zoomLevelMax = cursor.getInt(zoomMaxLayerFieldIndex);
        int tileWidth = cursor.getInt(tileWidthFieldIndex);
        int tileHeight = cursor.getInt(tileHeightFieldIndex);
        String proj = cursor.getString(projFieldIndex);
        int preference = cursor.getInt(preferenceFieldIndex);
        long startDate = cursor.getLong(startDateFieldIndex);
        long endDate = cursor.getLong(endDateIFieldndex);
        String noTileHeader = cursor.getString(noTileHeaderIndex);
        String storedValues = cursor.getString(noTileValueIndex);
        String[] noTileValues = null;
        if (storedValues != null) {
            noTileValues = storedValues.split("\\|");
        }
        int maxOverZoom = cursor.getInt(overZoomMaxFieldIndex);
        String logoUrl = cursor.getString(logoUrlFieldIndex);
        byte[] logoBytes = cursor.getBlob(logoFieldIndex);
        String description = cursor.getString(descriptionFieldIndex);
        String privacyPolicyUrl = cursor.getString(privacyPolicyUrlFieldIndex);

        return new TileLayerServer(context, id, name, tileUrl, type, category, overlay, defaultLayer, provider, touUri, null, logoUrl, logoBytes, zoomLevelMin,
                zoomLevelMax, maxOverZoom, tileWidth, tileHeight, proj, preference, startDate, endDate, noTileHeader, noTileValues, description,
                privacyPolicyUrl, true);
    }

    /**
     * Init the field indices for a Layer Cursor
     * 
     * @param cursor the Cursor
     */
    private static synchronized void initLayerFieldIndices(@NonNull Cursor cursor) {
        idLayerFieldIndex = cursor.getColumnIndex(ID_FIELD);
        nameFieldIndex = cursor.getColumnIndex(NAME_FIELD);
        typeFieldIndex = cursor.getColumnIndex(TYPE_FIELD);
        categoryFieldIndex = cursor.getColumnIndex(CATEGORY_FIELD);
        tileUrlFieldIndex = cursor.getColumnIndex(TILE_URL_FIELD);
        touUrlFieldIndex = cursor.getColumnIndex(TOU_URI_FIELD);
        attributionUrlFieldIndex = cursor.getColumnIndex(ATTRIBUTION_URL_FIELD);
        attributionFieldIndex = cursor.getColumnIndex(ATTRIBUTION_FIELD);
        overlayFieldIndex = cursor.getColumnIndex(OVERLAY_FIELD);
        defaultLayerFieldIndex = cursor.getColumnIndex(DEFAULTLAYER_FIELD);
        zoomMinLayerFieldIndex = cursor.getColumnIndex(ZOOM_MIN_FIELD);
        zoomMaxLayerFieldIndex = cursor.getColumnIndex(ZOOM_MAX_FIELD);
        tileWidthFieldIndex = cursor.getColumnIndex(TILE_WIDTH_FIELD);
        tileHeightFieldIndex = cursor.getColumnIndex(TILE_HEIGHT_FIELD);
        projFieldIndex = cursor.getColumnIndex(PROJ_FIELD);
        preferenceFieldIndex = cursor.getColumnIndex(PREFERENCE_FIELD);
        startDateFieldIndex = cursor.getColumnIndex(START_DATE_FIELD);
        endDateIFieldndex = cursor.getColumnIndex(END_DATE_FIELD);
        noTileHeaderIndex = cursor.getColumnIndex(NO_TILE_HEADER_FIELD);
        noTileValueIndex = cursor.getColumnIndex(NO_TILE_VALUE_FIELD);
        overZoomMaxFieldIndex = cursor.getColumnIndex(OVER_ZOOM_MAX_FIELD);
        logoUrlFieldIndex = cursor.getColumnIndex(LOGO_URL_FIELD);
        logoFieldIndex = cursor.getColumnIndex(LOGO_FIELD);
        descriptionFieldIndex = cursor.getColumnIndex(DESCRIPTION_FIELD);
        privacyPolicyUrlFieldIndex = cursor.getColumnIndex(PRIVACY_POLICY_URL_FIELD);
    }

    /**
     * Add a CoverageArea to the database
     * 
     * @param db a writable database
     * @param layerId the id of the layer we are associated with
     * @param coverage the CoverageArea object
     */
    private static void addCoverage(@NonNull SQLiteDatabase db, @NonNull String layerId, @NonNull CoverageArea coverage) {
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, layerId);
        values.put(ZOOM_MIN_FIELD, coverage.getMinZoomLevel());
        values.put(ZOOM_MAX_FIELD, coverage.getMaxZoomLevel());
        BoundingBox box = coverage.getBoundingBox();
        if (box != null) {
            values.put(LEFT_FIELD, box.getLeft());
            values.put(BOTTOM_FIELD, box.getBottom());
            values.put(RIGHT_FIELD, box.getRight());
            values.put(TOP_FIELD, box.getTop());
            db.insert(COVERAGES_TABLE, null, values);
            // Log.d(DEBUG_TAG, "Added box for " + layerId + ": " + box.toApiString());
        }
    }

    /**
     * Delete all coverage areas for a specific layer id
     * 
     * @param db a writable database
     * @param id the id for which we want to delete the coverage
     */
    public static void deleteCoverage(@NonNull SQLiteDatabase db, @NonNull String id) {
        db.delete(COVERAGES_TABLE, "id=?", new String[] { id });
    }
}
