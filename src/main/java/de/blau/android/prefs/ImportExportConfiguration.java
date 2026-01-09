package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.OpenParams;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import ch.poole.openinghoursfragment.templates.TemplateDatabase;
import ch.poole.openinghoursfragment.templates.TemplateDatabaseHelper;
import de.blau.android.R;
import de.blau.android.filter.TagFilterDatabaseHelper;
import de.blau.android.osm.OsmXml;
import de.blau.android.resources.KeyDatabaseHelper;
import de.blau.android.resources.TileLayerDatabase;
import de.blau.android.validation.ValidatorRulesDatabase;
import de.blau.android.validation.ValidatorRulesDatabaseHelper;

public final class ImportExportConfiguration {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ImportExportConfiguration.class.getSimpleName().length());
    private static final String DEBUG_TAG = ImportExportConfiguration.class.getSimpleName().substring(0, TAG_LEN);

    private static class ColumnMeta {
        String  type;
        boolean primaryKey;
    }

    private static final String STRING_SET_VALUE       = "StringSetValue";
    private static final String VERSION                = "version";
    private static final String CONFIGURATION          = "Configuration";
    private static final String SHARED_PREFERENCES     = "shared_preferences";
    private static final String NAME_ATTR              = "name";
    private static final String SECTION                = "Section";
    private static final String STRING_SET_PREF        = "StringSetPref";
    private static final String BOOLEAN_PREF           = "BooleanPref";
    private static final String FLOAT_PREF             = "FloatPref";
    private static final String LONG_PREF              = "LongPref";
    private static final String INT_PREF               = "IntPref";
    private static final String VALUE_ATTR             = "value";
    private static final String KEY_ATTR               = "key";
    private static final String STRING_PREF            = "StringPref";
    private static final String NULL                   = "null";
    private static final String TABLE_ATTR             = "table";
    private static final String PRIMARY_KEY_COLUMNS    = "PrimaryKeyColumns";
    private static final String COLUMN_TYPES           = "ColumnTypes";
    private static final String ROW                    = "Row";
    private static final String ACRA_ENABLE            = "acra.enable";
    private static final String ACRA_PREFIX            = "acra.";
    private static final String TRUNCATE_ATTR          = "truncate";
    private static final String TABLE_INFO_PRIMARY_KEY = "pk";
    private static final String TABLE_INFO_TYPE        = "type";
    private static final String TABLE_INFO_NAME        = "name";

    private static final Map<String, Class<?>> databaseHelpers = new HashMap<>();
    static {
        databaseHelpers.put(AdvancedPrefDatabase.DATABASE_NAME, AdvancedPrefDatabase.class);
        databaseHelpers.put(TileLayerDatabase.DATABASE_NAME, TileLayerDatabase.class);
        databaseHelpers.put(TagFilterDatabaseHelper.DATABASE_NAME, TagFilterDatabaseHelper.class);
        databaseHelpers.put(ValidatorRulesDatabaseHelper.DATABASE_NAME, ValidatorRulesDatabaseHelper.class);
        databaseHelpers.put(KeyDatabaseHelper.DATABASE_NAME, KeyDatabaseHelper.class);
        databaseHelpers.put(TemplateDatabaseHelper.DATABASE_NAME, TemplateDatabaseHelper.class);
    }

    /**
     * Default private constructor
     */
    private ImportExportConfiguration() {
        // nothing
    }

    /**
     * Export a configuration.
     * 
     * @param ctx an Android Context
     * @param outputStream output stream for the XML data
     * @throws IOException other problems
     */
    public static void exportConfig(@NonNull Context ctx, @NonNull OutputStream outputStream) throws IOException {
        try {
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(outputStream, OsmXml.UTF_8);
            serializer.startDocument(OsmXml.UTF_8, null);
            serializer.startTag(null, CONFIGURATION);
            sharedPreferences(ctx, serializer);
            sqlite(ctx, AdvancedPrefDatabase.DATABASE_NAME, AdvancedPrefDatabase.APIS_TABLE,
                    " where id not in (" + "'" + AdvancedPrefDatabase.ID_DEFAULT + "'," + "'" + AdvancedPrefDatabase.ID_SANDBOX + "'," + "'"
                            + AdvancedPrefDatabase.ID_OHM + "')",
                    Arrays.asList(AdvancedPrefDatabase.ACCESSTOKEN_COL, AdvancedPrefDatabase.ACCESSTOKENSECRET_COL), false, serializer);
            sqlite(ctx, AdvancedPrefDatabase.DATABASE_NAME, AdvancedPrefDatabase.IMAGESTORES_TABLE,
                    " where id not in (" + "'" + AdvancedPrefDatabase.ID_WIKIMEDIA_COMMONS + "'," + "'" + AdvancedPrefDatabase.ID_PANORAMAX_DEV + "')", null,
                    false, serializer);
            sqlite(ctx, AdvancedPrefDatabase.DATABASE_NAME, AdvancedPrefDatabase.PRESETS_TABLE, " where id != '" + AdvancedPrefDatabase.ID_DEFAULT + "'", null,
                    false, serializer);
            sqlite(ctx, AdvancedPrefDatabase.DATABASE_NAME, AdvancedPrefDatabase.GEOCODERS_TABLE, "", null, false, serializer);
            sqlite(ctx, AdvancedPrefDatabase.DATABASE_NAME, AdvancedPrefDatabase.LAYERS_TABLE, "", null, true, serializer);
            sqlite(ctx, TileLayerDatabase.DATABASE_NAME, TileLayerDatabase.LAYERS_TABLE, " where source='" + TileLayerDatabase.SOURCE_MANUAL + "'", null, false,
                    serializer);
            // filter names need to be set before the entries
            sqlite(ctx, TagFilterDatabaseHelper.DATABASE_NAME, TagFilterDatabaseHelper.FILTER_NAME_TABLE, "", null, false, serializer);
            sqlite(ctx, TagFilterDatabaseHelper.DATABASE_NAME, TagFilterDatabaseHelper.FILTERENTRIES_TABLE, "", null, false, serializer);
            // currently there is only the default ruleset so we don't save it
            sqlite(ctx, ValidatorRulesDatabaseHelper.DATABASE_NAME, ValidatorRulesDatabase.RESURVEY_TABLE, "", null, true, serializer);
            sqlite(ctx, ValidatorRulesDatabaseHelper.DATABASE_NAME, ValidatorRulesDatabase.CHECK_TABLE, "", null, true, serializer);
            sqlite(ctx, KeyDatabaseHelper.DATABASE_NAME, KeyDatabaseHelper.KEYS_TABLE, " where " + KeyDatabaseHelper.CUSTOM_FIELD, null, false, serializer);
            sqlite(ctx, TemplateDatabaseHelper.DATABASE_NAME, TemplateDatabase.TEMPLATES_TABLE, "", null, true, serializer);
            serializer.endTag(null, CONFIGURATION);
            serializer.endDocument();
        } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException | IOException | SQLiteException e) {
            String msg = ctx.getString(R.string.toast_configuration_export_failed, e.getLocalizedMessage());
            Log.e(DEBUG_TAG, msg);
            throw new IOException(msg);
        }
    }

    /**
     * Import a configuration.
     * 
     * There is no requirement that all possible sections are present.
     * 
     * @param ctx an Android Context
     * @param inputStream input stream with the XML data
     * @throws IOException other problems
     */
    public static void importConfig(@NonNull Context ctx, @NonNull InputStream inputStream) throws IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        factory.setNamespaceAware(true);
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, new Parser(ctx));
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | SQLiteException e) {
            String msg = ctx.getString(R.string.toast_configuration_import_failed, e.getLocalizedMessage());
            Log.e(DEBUG_TAG, msg);
            throw new IOException(msg);
        }
    }

    /**
     * Serialize nearly all SharedPreferences.
     * 
     * Skips some ACRA prefs.
     * 
     * @param ctx an Android Context
     * @param serializer an XmlSerializer instance
     * @throws IllegalArgumentException on unexpected input
     * @throws IllegalStateException serialization error
     * @throws IOException other problems
     */
    private static void sharedPreferences(@NonNull Context ctx, @NonNull XmlSerializer serializer)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, SECTION);
        serializer.attribute(null, NAME_ATTR, SHARED_PREFERENCES);
        serializer.attribute(null, VERSION, "0.1");
        Map<String, ?> prefMap = PreferenceManager.getDefaultSharedPreferences(ctx).getAll();
        for (Entry<String, ?> entry : prefMap.entrySet()) {
            final String key = entry.getKey();
            // we don't control acra prefs with exception of enabling it
            if (key.startsWith(ACRA_PREFIX) && !ACRA_ENABLE.equals(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                serializer.startTag(null, STRING_PREF);
                serializer.attribute(null, KEY_ATTR, key);
                serializer.attribute(null, VALUE_ATTR, (String) value);
                serializer.endTag(null, STRING_PREF);
            } else if (value instanceof Integer) {
                serializer.startTag(null, INT_PREF);
                serializer.attribute(null, KEY_ATTR, key);
                serializer.attribute(null, VALUE_ATTR, ((Integer) value).toString());
                serializer.endTag(null, INT_PREF);
            } else if (value instanceof Long) {
                serializer.startTag(null, LONG_PREF);
                serializer.attribute(null, KEY_ATTR, key);
                serializer.attribute(null, VALUE_ATTR, ((Long) value).toString());
                serializer.endTag(null, LONG_PREF);
            } else if (value instanceof Float) {
                serializer.startTag(null, FLOAT_PREF);
                serializer.attribute(null, KEY_ATTR, key);
                serializer.attribute(null, VALUE_ATTR, ((Float) value).toString());
                serializer.endTag(null, FLOAT_PREF);
            } else if (value instanceof Boolean) {
                serializer.startTag(null, BOOLEAN_PREF);
                serializer.attribute(null, KEY_ATTR, key);
                serializer.attribute(null, VALUE_ATTR, ((Boolean) value).toString());
                serializer.endTag(null, BOOLEAN_PREF);
            } else if (value instanceof Set) {
                serializer.startTag(null, STRING_SET_PREF);
                serializer.attribute(null, KEY_ATTR, key);
                for (String v : ((Set<String>) value)) {
                    serializer.startTag(null, STRING_SET_VALUE);
                    serializer.attribute(null, VALUE_ATTR, v);
                    serializer.endTag(null, STRING_SET_VALUE);
                }
                serializer.endTag(null, STRING_SET_PREF);
            }
        }
        serializer.endTag(null, SECTION);
    }

    /**
     * Serialize a sqlite database
     * 
     * @param ctx an Android Context
     * @param dbName the name of the database
     * @param table the name of the table
     * @param query query mainly to exclude rows
     * @param ignoreColumns columns to ignore
     * @param truncate ad tthe truncate flag
     * @param serializer serializer the XmlSerializer
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    private static void sqlite(@NonNull Context ctx, @NonNull String dbName, @NonNull String table, @NonNull String query, @Nullable List<String> ignoreColumns,
            boolean truncate, @NonNull XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {

        SQLiteDatabase.OpenParams.Builder builder = new SQLiteDatabase.OpenParams.Builder();
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(ctx.getDatabasePath(dbName), builder.addOpenFlags(SQLiteDatabase.OPEN_READONLY).build())) {
            Map<String, ColumnMeta> tableMeta = getColumnTypes(db, table);
            try (Cursor cursor = db.rawQuery("select * from " + table + query, null)) {
                boolean more = cursor.moveToFirst();
                if (!more) {
                    serializer.comment("Database " + dbName + " db table " + table + " has nothing to export");
                    return;
                }
                serializer.startTag(null, SECTION);
                serializer.attribute(null, NAME_ATTR, dbName);
                serializer.attribute(null, TABLE_ATTR, table);
                serializer.attribute(null, VERSION, "0.1");
                serializer.attribute(null, TRUNCATE_ATTR, Boolean.toString(truncate));
                String[] cols = cursor.getColumnNames();
                serializeTableMeta(serializer, tableMeta, cols);

                while (more) {
                    serializer.startTag(null, ROW);
                    for (int i = 0; i < cols.length; i++) {
                        final String column = cols[i];
                        if (ignoreColumns != null && ignoreColumns.contains(column)) {
                            continue;
                        }
                        final int colType = cursor.getType(i);
                        switch (colType) {
                        case Cursor.FIELD_TYPE_NULL:
                            serializer.attribute(null, column, NULL);
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            serializer.attribute(null, column, Long.toString(cursor.getLong(i)));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            serializer.attribute(null, column, Double.toString(cursor.getDouble(i)));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            serializer.attribute(null, column, cursor.getString(i));
                            break;
                        default:
                            Log.w(DEBUG_TAG, "Unsupported sqlite type " + colType);
                        }
                    }
                    serializer.endTag(null, ROW);
                    more = cursor.moveToNext();
                }
                serializer.endTag(null, SECTION);
            }
        } catch (SQLiteCantOpenDatabaseException sex) {
            // Databases that don't exist (yet) don't need to be exported
            serializer.comment("Database " + dbName + " table " + table + " doesn't exist yet");
        }
    }

    /**
     * Serialize the table meta information
     * 
     * @param serializer the XmlSerializer
     * @param tableMeta the meta information
     * @param cols column names
     * @throws IOException if serializing goes wrong
     */
    private static void serializeTableMeta(XmlSerializer serializer, Map<String, ColumnMeta> tableMeta, String[] cols) throws IOException {
        serializer.startTag(null, COLUMN_TYPES);
        for (int i = 0; i < cols.length; i++) {
            final String column = cols[i];
            serializer.attribute(null, column, tableMeta.get(column).type);
        }
        serializer.endTag(null, COLUMN_TYPES);
        serializer.startTag(null, PRIMARY_KEY_COLUMNS);
        for (int i = 0; i < cols.length; i++) {
            final String column = cols[i];
            if (tableMeta.get(column).primaryKey) {
                serializer.attribute(null, column, Boolean.toString(true));
            }
        }
        serializer.endTag(null, PRIMARY_KEY_COLUMNS);
    }

    /**
     * One parser to rule them all
     */
    private static class Parser extends DefaultHandler {
        private static final String REAL_TYPE    = "REAL";
        private static final String TEXT_TYPE    = "TEXT";
        private static final String INTEGER_TYPE = "INTEGER";

        private final Context ctx;

        private Editor      editor;
        private String      stringSetKey;
        private Set<String> stringSetValues = new HashSet<>();

        private SQLiteDatabase          db;
        private String                  table;
        private Map<String, ColumnMeta> columnTypes;

        enum State {
            BASE, SHARED_PREFERENCES, DATABASE
        }

        State state = State.BASE;

        Parser(@NonNull Context ctx) {
            this.ctx = ctx;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void startElement(final String uri, final String name, final String qName, final Attributes atts) {
            switch (state) {
            case BASE:
                if (!SECTION.equals(name)) {
                    return;
                }
                final String nameAttr = atts.getValue(NAME_ATTR);
                if (SHARED_PREFERENCES.equals(nameAttr)) {
                    state = State.SHARED_PREFERENCES;
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                    editor = prefs.edit();
                    return;
                }
                table = atts.getValue(TABLE_ATTR);
                if (table == null) {
                    return;
                }
                try {
                    Log.i(DEBUG_TAG, "Importing DB " + nameAttr + " table " + table);
                    db = openDatabase(nameAttr);
                    columnTypes = getColumnTypes(db, table);
                    if (Boolean.parseBoolean(atts.getValue(TRUNCATE_ATTR))) {
                        db.execSQL("DELETE FROM " + table);
                    }
                    state = State.DATABASE;
                } catch (IllegalArgumentException ex) {
                    final String msg = "Unable to import table " + table;
                    Log.e(DEBUG_TAG, msg);
                    throw new IllegalArgumentException(msg + " " + ex.getLocalizedMessage());
                }
                return;
            case SHARED_PREFERENCES:
                parseSharedPreference(name, atts);
                return;
            case DATABASE:
                if (db != null && ROW.equals(name)) {
                    addRow(db, table, atts);
                }
                return;
            default:
                // nothing
            }
        }

        /**
         * Get a writeable SQLiteDatabase.
         * 
         * If the database doesn't exist this will invoke the SQLiteOpenHelper to create the database first
         * 
         * @param name the database name
         */
        @NonNull
        private SQLiteDatabase openDatabase(@NonNull final String name) {
            final File databasePath = ctx.getDatabasePath(name);
            SQLiteDatabase.OpenParams.Builder builder = new SQLiteDatabase.OpenParams.Builder();
            OpenParams flags = builder.addOpenFlags(SQLiteDatabase.OPEN_READWRITE).build();
            try {
                return SQLiteDatabase.openDatabase(databasePath, flags);
            } catch (SQLiteException s) {
                // database doesn't exist yet, need to open it with database helper so that tables get created
                Class<?> helperClass = databaseHelpers.get(name);
                if (helperClass != null) {
                    Log.i(DEBUG_TAG, "Trying to call database helper for " + name);
                    Constructor<?> constructor;
                    try {
                        constructor = helperClass.getConstructor(Context.class);
                        try (SQLiteOpenHelper helper = (SQLiteOpenHelper) constructor.newInstance(ctx); SQLiteDatabase dummy = helper.getWritableDatabase();) {
                            // we don't actually do anything with this instance, just close it
                        }
                        return SQLiteDatabase.openDatabase(databasePath, flags);
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                        throw new IllegalArgumentException(e.getMessage());
                    }
                }
                throw new IllegalArgumentException("Unable to open database " + name);
            }
        }

        /**
         * Add a row to a table, inserting or updating as necessary (requires a primary key)
         * 
         * @param db the writable database
         * @param table the name of the table
         * @param atts the xml attributes
         */
        private void addRow(@NonNull SQLiteDatabase db, @NonNull String table, @NonNull Attributes atts) {
            ContentValues values = new ContentValues();
            String pkColumn = null;
            for (int i = 0; i < atts.getLength(); i++) {
                String column = atts.getLocalName(i);
                String valueStr = atts.getValue(i);
                if (NULL.equals(valueStr)) {
                    // skip for now
                    continue;
                }
                try {
                    ColumnMeta meta = columnTypes.get(column);
                    if (meta.primaryKey) {
                        pkColumn = column;
                    }
                    switch (meta.type) {
                    case INTEGER_TYPE:
                        values.put(column, Integer.parseInt(valueStr));
                        break;
                    case TEXT_TYPE:
                        values.put(column, valueStr);
                        break;
                    case REAL_TYPE:
                        values.put(column, Double.parseDouble(valueStr));
                        break;
                    default:
                        Log.e(DEBUG_TAG, "Unknown column type " + column + " " + meta.type);
                    }
                } catch (NumberFormatException nfex) {
                    Log.e(DEBUG_TAG, "Skipped column " + column + " " + nfex.getLocalizedMessage());
                }
            }
            try {
                db.insertOrThrow(table, null, values);
            } catch (SQLiteConstraintException cex) {
                if (pkColumn != null) {
                    db.update(table, values, pkColumn + "= ?", new String[] { values.getAsString(pkColumn) });
                    return;
                }
                Log.e(DEBUG_TAG, "Unable to insert row " + cex.getLocalizedMessage());
            } catch (SQLiteException sex) {
                Log.e(DEBUG_TAG, "Unable to insert row " + sex.getLocalizedMessage());
            }
        }

        private void parseSharedPreference(String name, Attributes atts) {
            String key = atts.getValue(KEY_ATTR);
            switch (name) {
            case STRING_PREF:
                editor.putString(key, atts.getValue(VALUE_ATTR));
                return;
            case INT_PREF:
                editor.putInt(key, Integer.parseInt(atts.getValue(VALUE_ATTR)));
                return;
            case LONG_PREF:
                editor.putLong(key, Long.parseLong(atts.getValue(VALUE_ATTR)));
                return;
            case FLOAT_PREF:
                editor.putFloat(key, Float.parseFloat(atts.getValue(VALUE_ATTR)));
                return;
            case BOOLEAN_PREF:
                editor.putBoolean(key, Boolean.parseBoolean(atts.getValue(VALUE_ATTR)));
                return;
            case STRING_SET_PREF:
                stringSetKey = key;
                stringSetValues.clear();
                return;
            case STRING_SET_VALUE:
                stringSetValues.add(atts.getValue(VALUE_ATTR));
                return;
            default:
                Log.w(DEBUG_TAG, "Unknown preferences type " + name);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(final String uri, final String name, final String qName) throws SAXException {
            switch (state) {
            case BASE:
                return;
            case SHARED_PREFERENCES:
                switch (name) {
                case SECTION:
                    Log.i(DEBUG_TAG, "commiting edits");
                    editor.apply();
                    state = State.BASE;
                    return;
                case STRING_SET_PREF:
                    if (stringSetKey != null) {
                        editor.putStringSet(stringSetKey, stringSetValues);
                    } else {
                        Log.w(DEBUG_TAG, "stringSetKey is null");
                    }
                    return;
                default:
                    // nothing
                }
                return;
            case DATABASE:
                if (SECTION.equals(name)) {
                    if (db != null) {
                        db.close();
                    }
                    db = null;
                    table = null;
                    state = State.BASE;
                }
                return;
            default:
                // nothing
            }
        }
    }

    /**
     * Get column information for a SQLite table
     * 
     * @param db a readable SQLiteDatabase
     * @param table the table name
     * @return a map from column name to meta information
     */
    private static Map<String, ColumnMeta> getColumnTypes(@NonNull SQLiteDatabase db, @NonNull String table) {
        try (Cursor cursor = db.rawQuery("pragma table_info(" + table + ")", null)) {
            boolean more = cursor.moveToFirst();
            if (more) {
                Map<String, ColumnMeta> result = new HashMap<>();
                int nameIndex = cursor.getColumnIndex(TABLE_INFO_NAME);
                int typeIndex = cursor.getColumnIndex(TABLE_INFO_TYPE);
                int pkIndex = cursor.getColumnIndex(TABLE_INFO_PRIMARY_KEY);
                while (more) {
                    ColumnMeta meta = new ColumnMeta();
                    meta.type = cursor.getString(typeIndex);
                    meta.primaryKey = cursor.getInt(pkIndex) > 0;
                    result.put(cursor.getString(nameIndex), meta);
                    more = cursor.moveToNext();
                }
                return result;
            }
            throw new IllegalArgumentException("No table information");
        }
    }
}
