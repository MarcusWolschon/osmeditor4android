package io.vespucci.util;

import java.io.File;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.annotation.NonNull;

/**
 * From http://stackoverflow.com/questions/5332328/sqliteopenhelper-problem-with- fully-qualified-db-path-name
 * 
 *
 */
public class CustomDatabaseContext extends ContextWrapper {

    private static final String DEBUG_CONTEXT = "DatabaseContext";

    private final String path;

    /**
     * Construct a new instance
     * 
     * @param base the Context we want to wrap
     * @param path the path to the directory that should hold the databases disrectory
     */
    public CustomDatabaseContext(@NonNull Context base, @NonNull String path) {
        super(base);
        this.path = path;
    }

    @Override
    public File getDatabasePath(String name) {
        String dbfile = path + File.separator + "databases" + File.separator + name;
        if (!dbfile.endsWith(".db")) {
            dbfile += ".db";
        }

        File result = new File(dbfile);

        if (!result.getParentFile().exists()) {
            // noinspection ResultOfMethodCallIgnored
            result.getParentFile().mkdirs();
        }

        if (Log.isLoggable(DEBUG_CONTEXT, Log.WARN)) {
            Log.w(DEBUG_CONTEXT, "getDatabasePath(" + name + ") = " + result.getAbsolutePath());
        }

        return result;
    }

    /*
     * this version is called for android devices >= api-11. thank to @damccull for fixing this.
     */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name, mode, factory);
    }

    /* this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode,
        // factory);
        if (Log.isLoggable(DEBUG_CONTEXT, Log.WARN)) {
            Log.w(DEBUG_CONTEXT, "openOrCreateDatabase(" + name + ",,) = " + result.getPath());
        }
        return result;
    }
}
