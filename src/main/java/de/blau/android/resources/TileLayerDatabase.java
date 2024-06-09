package de.blau.android.resources;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.TileLayerSource.Category;
import de.blau.android.resources.TileLayerSource.Header;
import de.blau.android.resources.TileLayerSource.Provider;
import de.blau.android.resources.TileLayerSource.Provider.CoverageArea;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.util.collections.MultiHashMap;

public class TileLayerDatabase extends SQLiteOpenHelper {
    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, TileLayerDatabase.class.getSimpleName().length());
    protected static final String DEBUG_TAG = TileLayerDatabase.class.getSimpleName().substring(0, TAG_LEN);

    public static final String DATABASE_NAME    = "tilelayers";
    private static final int   DATABASE_VERSION = 9;

    public static final String SOURCE_ELI          = "eli";    // editor-layer-index
    public static final String SOURCE_JOSM_IMAGERY = "josm";   // josm.openstreetmap.de/wiki/maps
    public static final String SOURCE_CUSTOM       = "custom"; // user added tile layers from file
    public static final String SOURCE_MANUAL       = "manual"; // user added tile layer

    private static final String SOURCES_TABLE = "sources";
    static final String         NAME_FIELD    = "name";
    private static final String UPDATED_FIELD = "updated";

    public static final String  LAYERS_TABLE             = "layers";
    private static final String ID_FIELD                 = "id";
    private static final String TYPE_FIELD               = "server_type";
    private static final String TILE_TYPE_FIELD          = "tile_type";
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
    private static final String NO_TILE_TILE_FIELD       = "no_tile_tile";
    private static final String LOGO_URL_FIELD           = "logo_url";
    private static final String LOGO_FIELD               = "logo";
    private static final String DESCRIPTION_FIELD        = "description";
    private static final String PRIVACY_POLICY_URL_FIELD = "privacy_policy_url";

    public static final String  COVERAGES_TABLE = "coverages";
    private static final String LEFT_FIELD      = "left";
    private static final String BOTTOM_FIELD    = "bottom";
    private static final String RIGHT_FIELD     = "right";
    private static final String TOP_FIELD       = "top";

    private static final String HEADERS_TABLE      = "headers";
    private static final String HEADER_NAME_FIELD  = "name";
    private static final String HEADER_VALUE_FIELD = "value";

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
                    "CREATE TABLE layers (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, server_type TEXT NOT NULL, category TEXT DEFAULT NULL, tile_type TEXT DEFAULT NULL,"
                            + " source TEXT NOT NULL, url TEXT NOT NULL," + " tou_url TEXT, attribution TEXT, overlay INTEGER NOT NULL DEFAULT 0,"
                            + " default_layer INTEGER NOT NULL DEFAULT 0, zoom_min INTEGER NOT NULL DEFAULT 0, zoom_max INTEGER NOT NULL DEFAULT 18,"
                            + " over_zoom_max INTEGER NOT NULL DEFAULT 4, tile_width INTEGER NOT NULL DEFAULT 256, tile_height INTEGER NOT NULL DEFAULT 256,"
                            + " proj TEXT DEFAULT NULL, preference INTEGER NOT NULL DEFAULT 0, start_date INTEGER DEFAULT NULL, end_date INTEGER DEFAULT NULL,"
                            + " no_tile_header TEXT DEFAULT NULL, no_tile_value TEXT DEFAULT NULL, no_tile_tile BLOB DEFAULT NULL, logo_url TEXT DEFAULT NULL, logo BLOB DEFAULT NULL,"
                            + " description TEXT DEFAULT NULL, privacy_policy_url TEXT DEFAULT NULL, attribution_url TEXT DEFAULT NULL, FOREIGN KEY(source) REFERENCES sources(name) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX layers_overlay_idx ON layers(overlay)");
            db.execSQL("CREATE INDEX layers_source_idx ON layers(source)");
            db.execSQL("CREATE TABLE coverages (id TEXT NOT NULL, zoom_min INTEGER NOT NULL DEFAULT 0, zoom_max INTEGER NOT NULL DEFAULT 18,"
                    + " left INTEGER DEFAULT NULL, bottom INTEGER DEFAULT NULL, right INTEGER DEFAULT NULL, top INTEGER DEFAULT NULL,"
                    + " FOREIGN KEY(id) REFERENCES layers(id) ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX coverages_idx ON coverages(id)");
            createHeadersTable(db);
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
        if (oldVersion <= 6 && newVersion >= 7) {
            db.execSQL("ALTER TABLE layers ADD COLUMN no_tile_tile BLOB DEFAULT NULL");
        }
        if (oldVersion <= 7 && newVersion >= 8) {
            db.execSQL("ALTER TABLE layers ADD COLUMN tile_type TEXT DEFAULT NULL");
        }
        if (oldVersion <= 8 && newVersion >= 9) {
            createHeadersTable(db);
        }
    }

    /**
     * Create a table for custom headers
     * 
     * @param db a writable database instance
     */
    private void createHeadersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE headers (id TEXT NOT NULL, name TEXT NOT NULL, value TEXT NOT NULL,"
                + " FOREIGN KEY(id) REFERENCES layers(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX headers_idx ON headers(id)");
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
     * @return a milliseconds since the epoch value or 0 if not set
     */
    public static long getSourceUpdate(@NonNull SQLiteDatabase db, @NonNull String source) {
        try (Cursor dbresult = db.query(SOURCES_TABLE, null, NAME_FIELD + "='" + source + "'", null, null, null, null)) {
            if (dbresult.getCount() >= 1) {
                boolean haveEntry = dbresult.moveToFirst();
                if (haveEntry) {
                    return dbresult.getLong(dbresult.getColumnIndexOrThrow(UPDATED_FIELD));
                }
            }
        } catch (IllegalArgumentException iaex) {
            Log.e(DEBUG_TAG, "failed to get source update value " + iaex.getMessage());
        }
        return 0;
    }

    /**
     * Update a source entry
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
     * Add a layer, will add coverage areas to the coverage and headers to the headers table
     * 
     * @param db writable database
     * @param source source the layer comes from
     * @param layer a TileLayerSource object
     */
    public static void addLayer(@NonNull SQLiteDatabase db, @NonNull String source, @NonNull TileLayerSource layer) {
        ContentValues values = getContentValuesForLayer(source, layer);
        try {
            db.insertOrThrow(LAYERS_TABLE, null, values);
            addCoverageFromLayer(db, layer);
            addHeadersFromLayer(db, layer);
        } catch (SQLiteConstraintException e) {
            // even when in a transaction only this insert will get rolled back
            Log.e(DEBUG_TAG, "Constraint exception " + source + " " + layer + " " + e.getMessage());
        }
    }

    /**
     * Add coverage entries
     * 
     * @param db writable database
     * @param layer a TileLayerSource instance
     */
    private static void addCoverageFromLayer(@NonNull SQLiteDatabase db, @NonNull TileLayerSource layer) {
        // insert coverage areas
        for (CoverageArea ca : layer.getCoverage()) {
            addCoverage(db, layer.getId(), ca);
        }
    }

    /**
     * Add header entries
     * 
     * @param db writable database
     * @param layer a TileLayerSource instance
     */
    private static void addHeadersFromLayer(@NonNull SQLiteDatabase db, @NonNull TileLayerSource layer) {
        // insert coverage areas
        List<Header> headers = layer.getHeaders();
        if (headers != null) {
            for (Header h : headers) {
                addHeader(db, layer.getId(), h);
            }
        }
    }

    /**
     * Get an ContentValues object suitable for insertion or an update of a layer
     * 
     * @param source the source of the layer, use null if this is an update
     * @param layer TileLayerSource object holding the values
     * @return a ContentValues object
     */
    private static ContentValues getContentValuesForLayer(@Nullable String source, @NonNull TileLayerSource layer) {
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, layer.getId());
        values.put(NAME_FIELD, layer.getName());
        values.put(TYPE_FIELD, layer.getType());
        values.put(TILE_TYPE_FIELD, layer.getTileType().name());
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
        if (!TileLayerSource.TYPE_BING.equals(layer.getType())) { // bing layer gets these values dynamically
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
        byte[] noTileTile = layer.getNoTileTile();
        if (noTileTile != null) {
            values.put(NO_TILE_TILE_FIELD, noTileTile);
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
    @Nullable
    public static TileLayerSource getLayer(@NonNull Context context, @NonNull SQLiteDatabase db, @NonNull String id) {
        TileLayerSource layer = null;
        try (Cursor providerCursor = db.query(COVERAGES_TABLE, null, ID_FIELD + "='" + id + "'", null, null, null, null)) {
            Provider provider = getProviderFromCursor(providerCursor);
            try (Cursor layerCursor = db.query(LAYERS_TABLE, null, ID_FIELD + "='" + id + "'", null, null, null, null)) {
                if (layerCursor.getCount() >= 1) {
                    boolean haveEntry = layerCursor.moveToFirst();
                    if (haveEntry) {
                        initLayerFieldIndices(layerCursor);
                        layer = getLayerFromCursor(context, provider, layerCursor);
                        setHeadersForLayer(db, layer);
                    }
                }
            }
        }
        return layer;
    }

    /**
     * Retrieve headers for a layer and set them
     * 
     * @param db a readable DB instance
     * @param layer the layer
     */
    private static void setHeadersForLayer(SQLiteDatabase db, TileLayerSource layer) {
        try (Cursor headerCursor = db.query(HEADERS_TABLE, null, ID_FIELD + "='" + layer.getId() + "'", null, null, null, null)) {
            initHeaderFieldIndices(headerCursor);
            List<Header> headers = getHeadersFromCursor(headerCursor);
            if (!headers.isEmpty()) {
                layer.setHeaders(headers);
            }
        }
    }

    /**
     * Retrieve a single layer identified by its url
     * 
     * @param context Android Context
     * @param db readable SQLiteDatabase
     * @param url the tile url
     * @return a TileLayerServer instance of null if none could be found
     */
    @Nullable
    public static TileLayerSource getLayerWithUrl(@NonNull Context context, @NonNull SQLiteDatabase db, @NonNull String url) {
        TileLayerSource layer = null;
        try (Cursor layerCursor = db.query(LAYERS_TABLE, null, TILE_URL_FIELD + "=?", new String[] { url }, null, null, null)) {
            if (layerCursor.getCount() >= 1) {
                boolean haveEntry = layerCursor.moveToFirst();
                if (haveEntry) {
                    initLayerFieldIndices(layerCursor);
                    String id = layerCursor.getString(idLayerFieldIndex);
                    try (Cursor providerCursor = db.query(COVERAGES_TABLE, null, ID_FIELD + "='" + id + "'", null, null, null, null)) {
                        Provider provider = getProviderFromCursor(providerCursor);
                        layer = getLayerFromCursor(context, provider, layerCursor);
                        setHeadersForLayer(db, layer);
                    }
                }
            }
        }
        return layer;
    }

    /**
     * Get a layers rowid
     * 
     * @param db the database
     * @param id the layer id
     * @return the rowid
     */
    public static long getLayerRowId(@NonNull SQLiteDatabase db, @NonNull String id) throws IllegalArgumentException {
        try (Cursor layerCursor = db.query(LAYERS_TABLE, new String[] { "rowid" }, ID_FIELD + "='" + id + "'", null, null, null, null)) {
            if (layerCursor.getCount() >= 1) {
                boolean haveEntry = layerCursor.moveToFirst();
                if (haveEntry) {
                    return layerCursor.getLong(layerCursor.getColumnIndexOrThrow("rowid"));
                }
            }
        }
        return -1;
    }

    /**
     * Retrieve a single layer identified by its mysql rowid
     * 
     * @param context Androic Context
     * @param db readable SWLiteDatabase
     * @param rowId the mysql rowid
     * @return a TileLayerServer instance of null if none could be found
     */
    public static TileLayerSource getLayerWithRowId(@NonNull Context context, @NonNull SQLiteDatabase db, @NonNull long rowId) {
        TileLayerSource layer = null;
        try (Cursor providerCursor = db.rawQuery(
                "SELECT coverages.id as id,left,bottom,right,top,coverages.zoom_min as zoom_min,coverages.zoom_max as zoom_max FROM layers,coverages WHERE layers.rowid=? AND layers.id=coverages.id",
                new String[] { Long.toString(rowId) })) {
            Provider provider = getProviderFromCursor(providerCursor);
            try (Cursor layerCursor = db.rawQuery(QUERY_LAYER_BY_ROWID, new String[] { Long.toString(rowId) })) {
                if (layerCursor.getCount() >= 1) {
                    boolean haveEntry = layerCursor.moveToFirst();
                    if (haveEntry) {
                        initLayerFieldIndices(layerCursor);
                        layer = getLayerFromCursor(context, provider, layerCursor);
                        setHeadersForLayer(db, layer);
                    }
                }
            } catch (IllegalArgumentException iaex) {
                Log.e(DEBUG_TAG, "retrieving layer failed " + iaex.getMessage());
            }
        }
        return layer;
    }

    /**
     * Create Provider object containing CoverageAreas from a Cursor
     * 
     * @param cursor the Cursor
     * @return a Provider instance
     */
    @NonNull
    private static Provider getProviderFromCursor(@NonNull Cursor cursor) {
        Provider provider = new Provider();
        try {
            if (cursor.getCount() >= 1) {
                Log.d(DEBUG_TAG, "Got 1 or more coverage areas");
                boolean haveEntry = cursor.moveToFirst();
                initCoverageFieldIndices(cursor);
                while (haveEntry) {
                    provider.addCoverageArea(getCoverageFromCursor(cursor));
                    haveEntry = cursor.moveToNext();
                }
            }
        } catch (IllegalArgumentException iaex) {
            Log.e(DEBUG_TAG, "retrieving provider failed " + iaex.getMessage());
        }
        return provider;
    }

    /**
     * Get Headers from a Cursor
     * 
     * @param cursor the Cursor
     * @return a List of Header objects
     */
    @NonNull
    private static List<Header> getHeadersFromCursor(@NonNull Cursor cursor) {
        List<Header> headers = new ArrayList<>();
        try {
            if (cursor.getCount() >= 1) {
                Log.d(DEBUG_TAG, "Got 1 or more headers");
                boolean haveEntry = cursor.moveToFirst();
                initHeaderFieldIndices(cursor);
                while (haveEntry) {
                    headers.add(getHeaderFromCursor(cursor));
                    haveEntry = cursor.moveToNext();
                }
            }
        } catch (IllegalArgumentException iaex) {
            Log.e(DEBUG_TAG, "retrieving headers failed " + iaex.getMessage());
        }
        return headers;
    }

    /**
     * Update an existing layer in the database
     * 
     * @param db a writable SQLiteDatabase
     * @param layer the layer to write to the database
     */
    public static void updateLayer(@NonNull SQLiteDatabase db, @NonNull TileLayerSource layer) {
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
    public static void deleteLayerWithRowId(@NonNull SQLiteDatabase db, long rowId) {
        db.delete(LAYERS_TABLE, "layers.rowid=?", new String[] { Long.toString(rowId) });
    }

    /**
     * Delete a layer with a specific id
     * 
     * @param db a writable SQLiteDatabase
     * @param id the id
     */
    public static void deleteLayerWithId(@NonNull SQLiteDatabase db, @NonNull String id) {
        db.delete(LAYERS_TABLE, "layers.id=?", new String[] { id });
    }

    private static int coverageIdFieldIndex = -1;
    private static int leftFieldIndex       = -1;
    private static int bottomFieldIndex     = -1;
    private static int rightFieldIndex      = -1;
    private static int topFieldIndex        = -1;
    private static int zoomMinFieldIndex    = -1;
    private static int zoomMaxFieldIndex    = -1;

    /**
     * Create a CoverageArea from a Cursor
     * 
     * Uses pre-computed field indices
     * 
     * @param cursor the Cursor
     * @return a CoverageArea instance
     */
    private static CoverageArea getCoverageFromCursor(@NonNull Cursor cursor) {
        if (coverageIdFieldIndex == -1) {
            throw new IllegalStateException("Coverage field indices not initialized");
        }
        BoundingBox box = new BoundingBox(cursor.getInt(leftFieldIndex), cursor.getInt(bottomFieldIndex), cursor.getInt(rightFieldIndex),
                cursor.getInt(topFieldIndex));
        return new CoverageArea(cursor.getInt(zoomMinFieldIndex), cursor.getInt(zoomMaxFieldIndex), box);
    }

    /**
     * Init the field indices for a Coverage Cursor
     * 
     * @param cursor the Cursor
     */
    private static synchronized void initCoverageFieldIndices(@NonNull Cursor cursor) {
        coverageIdFieldIndex = cursor.getColumnIndexOrThrow(ID_FIELD);
        leftFieldIndex = cursor.getColumnIndexOrThrow(LEFT_FIELD);
        bottomFieldIndex = cursor.getColumnIndexOrThrow(BOTTOM_FIELD);
        rightFieldIndex = cursor.getColumnIndexOrThrow(RIGHT_FIELD);
        topFieldIndex = cursor.getColumnIndexOrThrow(TOP_FIELD);
        zoomMinFieldIndex = cursor.getColumnIndexOrThrow(ZOOM_MIN_FIELD);
        zoomMaxFieldIndex = cursor.getColumnIndexOrThrow(ZOOM_MAX_FIELD);
    }

    private static int headerIdFieldIndex    = -1;
    private static int headerNameFieldIndex  = -1;
    private static int headerValueFieldIndex = -1;

    private static Header getHeaderFromCursor(@NonNull Cursor cursor) {
        if (headerIdFieldIndex == -1) {
            throw new IllegalStateException("Header field indices not initialized");
        }
        return new Header(cursor.getString(headerNameFieldIndex), cursor.getString(headerValueFieldIndex));
    }

    /**
     * Init the field indices for a Header Cursor
     * 
     * @param cursor the Cursor
     */
    private static synchronized void initHeaderFieldIndices(@NonNull Cursor cursor) {
        headerIdFieldIndex = cursor.getColumnIndexOrThrow(ID_FIELD);
        headerNameFieldIndex = cursor.getColumnIndexOrThrow(HEADER_NAME_FIELD);
        headerValueFieldIndex = cursor.getColumnIndexOrThrow(HEADER_VALUE_FIELD);
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
     * Get a Cursor for all WMS end points
     * 
     * @param db a readable SQLiteDatabase
     * @return a Cursor pointing to all WMS end points
     */
    static Cursor getAllWmsEndPoints(@NonNull SQLiteDatabase db) {
        return db.rawQuery("SELECT layers.rowid as _id, name FROM layers WHERE server_type='" + TileLayerSource.TYPE_WMS_ENDPOINT + "' ORDER BY name", null);
    }

    /**
     * Get all layers of either non-overlay or overlay type
     * 
     * Ignores WMS endpoints
     * 
     * @param context Android Context
     * @param db a readable SQLiteDatabase
     * @param overlay if true only overlay layers will be returned
     * @return a Map containing the selected layers
     */
    @NonNull
    public static Map<String, TileLayerSource> getAllLayers(@NonNull Context context, @NonNull SQLiteDatabase db, boolean overlay) {
        Map<String, TileLayerSource> layers = new HashMap<>();
        try {
            MultiHashMap<String, CoverageArea> coveragesById = getCoveragesById(db, overlay);
            Map<String, List<Header>> headersById = getHeadersById(db, overlay);

            try (Cursor layerCursor = db.query(LAYERS_TABLE, null,
                    OVERLAY_FIELD + "=" + boolean2intString(overlay) + " AND " + TYPE_FIELD + " <> '" + TileLayerSource.TYPE_WMS_ENDPOINT + "'", null, null,
                    null, null)) {
                if (layerCursor.getCount() >= 1) {
                    boolean haveEntry = layerCursor.moveToFirst();
                    initLayerFieldIndices(layerCursor);
                    while (haveEntry) {
                        String id = layerCursor.getString(idLayerFieldIndex);
                        Provider provider = new Provider();
                        for (CoverageArea ca : coveragesById.get(id)) {
                            provider.addCoverageArea(ca);
                        }
                        TileLayerSource layer = getLayerFromCursor(context, provider, layerCursor);
                        // if we have an apikey parameter and can't replace it, don't add
                        if (layer.replaceApiKey(context, false)) {
                            layers.put(id, layer);
                        } else {
                            Log.e(DEBUG_TAG, "layer " + id + " is missing an apikey, not added");
                        }
                        List<Header> headers = headersById.get(id);
                        if (headers != null && !headers.isEmpty()) {
                            layer.setHeaders(headers);
                        }
                        haveEntry = layerCursor.moveToNext();
                    }
                }
            }
        } catch (IllegalArgumentException iaex) {
            Log.e(DEBUG_TAG, "Retrieving sources failed " + iaex.getMessage());
        }
        return layers;
    }

    /**
     * Get a Map with layer id and Headers
     * 
     * @param db a readable DB instance
     * @param overlay true is and overlay
     * @return a Map with layer id and Headers
     */
    @NonNull
    private static Map<String, List<Header>> getHeadersById(SQLiteDatabase db, boolean overlay) {
        Map<String, List<Header>> headersById = new HashMap<>();
        try (Cursor headerCursor = db.rawQuery(
                "SELECT headers.id as id,headers.name as name,value FROM layers,headers WHERE headers.id=layers.id AND overlay=?",
                new String[] { boolean2intString(overlay) })) {
            if (headerCursor.getCount() >= 1) {
                initHeaderFieldIndices(headerCursor);
                boolean haveEntry = headerCursor.moveToFirst();
                while (haveEntry) {
                    String id = headerCursor.getString(headerIdFieldIndex);
                    List<Header> headers = headersById.get(id);
                    if (headers == null) {
                        headers = new ArrayList<>();
                        headersById.put(id, headers);
                    }
                    headers.add(getHeaderFromCursor(headerCursor));
                    haveEntry = headerCursor.moveToNext();
                }
            }
        }
        return headersById;
    }

    /**
     * Get a Map with layer id and CoverageArea
     * 
     * @param db a readable DB instance
     * @param overlay true is and overlay
     * @return a Map with layer id and CoverageArea
     */
    private static MultiHashMap<String, CoverageArea> getCoveragesById(SQLiteDatabase db, boolean overlay) {
        MultiHashMap<String, CoverageArea> coveragesById = new MultiHashMap<>();
        try (Cursor coverageCursor = db.rawQuery(
                "SELECT coverages.id as id,left,bottom,right,top,coverages.zoom_min as zoom_min,coverages.zoom_max as zoom_max FROM layers,coverages WHERE coverages.id=layers.id AND overlay=?",
                new String[] { boolean2intString(overlay) })) {
            if (coverageCursor.getCount() >= 1) {
                initCoverageFieldIndices(coverageCursor);
                boolean haveEntry = coverageCursor.moveToFirst();
                while (haveEntry) {
                    String id = coverageCursor.getString(coverageIdFieldIndex);
                    CoverageArea ca = getCoverageFromCursor(coverageCursor);
                    coveragesById.add(id, ca);
                    haveEntry = coverageCursor.moveToNext();
                }
            }
        }
        return coveragesById;
    }

    /**
     * Return a int string value for a boolean
     * 
     * @param bool the boolean
     * @return "1" or "0"
     */
    private static String boolean2intString(boolean bool) {
        return bool ? "1" : "0";
    }

    static int idLayerFieldIndex          = -1;
    static int nameFieldIndex             = -1;
    static int typeFieldIndex             = -1;
    static int tileTypeFieldIndex         = -1;
    static int sourceFieldIndex           = -1;
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
    static int noTileTileIndex            = -1;
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
    @NonNull
    private static TileLayerSource getLayerFromCursor(@NonNull Context context, @NonNull Provider provider, @NonNull Cursor cursor) {
        if (idLayerFieldIndex == -1) {
            throw new IllegalStateException("Layer field indices not initialized");
        }
        String id = cursor.getString(idLayerFieldIndex);
        String name = cursor.getString(nameFieldIndex);
        String type = cursor.getString(typeFieldIndex);
        TileType tileType = null;
        String tileTypeString = cursor.getString(tileTypeFieldIndex);
        if (tileTypeString != null) {
            tileType = TileType.valueOf(tileTypeString);
        }
        String source = cursor.getString(sourceFieldIndex);
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
        byte[] noTileTile = cursor.getBlob(noTileTileIndex);
        int maxOverZoom = cursor.getInt(overZoomMaxFieldIndex);
        String logoUrl = cursor.getString(logoUrlFieldIndex);
        byte[] logoBytes = cursor.getBlob(logoFieldIndex);
        String description = cursor.getString(descriptionFieldIndex);
        String privacyPolicyUrl = cursor.getString(privacyPolicyUrlFieldIndex);

        TileLayerSource layer = new TileLayerSource(context, id, name, tileUrl, type, category, overlay, defaultLayer, provider, touUri, null, logoUrl,
                logoBytes, zoomLevelMin, zoomLevelMax, maxOverZoom, tileWidth, tileHeight, proj, preference, startDate, endDate, noTileHeader, noTileValues,
                privacyPolicyUrl, true);
        layer.setSource(source);
        layer.setDescription(description);
        layer.setNoTileTile(noTileTile);
        if (tileType != null) {
            layer.setTileType(tileType);
        }
        return layer;
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
        tileTypeFieldIndex = cursor.getColumnIndex(TILE_TYPE_FIELD);
        sourceFieldIndex = cursor.getColumnIndex(SOURCE_FIELD);
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
        noTileTileIndex = cursor.getColumnIndex(NO_TILE_TILE_FIELD);
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

    /**
     * Add a header to the database
     * 
     * @param db a writable database
     * @param layerId the id of the layer we are associated with
     * @param header the Header object
     */
    private static void addHeader(@NonNull SQLiteDatabase db, @NonNull String layerId, @NonNull Header header) {
        ContentValues values = new ContentValues();
        values.put(ID_FIELD, layerId);
        values.put(HEADER_NAME_FIELD, header.getName());
        values.put(HEADER_VALUE_FIELD, header.getValue());
        db.insert(HEADERS_TABLE, null, values);
    }

    /**
     * Delete all headers for a specific layer id
     * 
     * @param db a writable database
     * @param id the id for which we want to delete the coverage
     */
    public static void deleteHeader(@NonNull SQLiteDatabase db, @NonNull String id) {
        db.delete(COVERAGES_TABLE, "id=?", new String[] { id });
    }
}
