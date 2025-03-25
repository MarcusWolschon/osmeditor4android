package io.vespucci.services.util;

import java.util.HashMap;
import java.util.Map;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteProgram;
import android.os.CancellationSignal;

@Implements(value = SQLiteProgram.class)
public class ShadowSQLiteProgram extends ShadowSQLiteCloseable {

    @RealObject
    private SQLiteProgram realProgram;

    protected Map<Integer, Long>   longMap   = new HashMap<>();
    protected Map<Integer, String> stringMap = new HashMap<>();

    protected SQLiteDatabase db = null;

    /**
     * Save the SQLiteDatabase and then call through to the actual constructor
     * 
     * @param db the database
     * @param sql sql statement
     * @param bindArgs arguments to bind
     * @param cancellationSignalForPrepare ?
     */
    @Implementation
    public void __constructor__(SQLiteDatabase db, String sql, Object[] bindArgs, CancellationSignal cancellationSignalForPrepare) { // NOSONAR
        this.db = db;
        Shadow.invokeConstructor(SQLiteProgram.class, realProgram, ClassParameter.from(SQLiteDatabase.class, db), ClassParameter.from(String.class, sql),
                ClassParameter.from(Object[].class, bindArgs), ClassParameter.from(CancellationSignal.class, cancellationSignalForPrepare));
    }

    /**
     * Mock implementation that stores it's arguments then calls through to the original
     * 
     * @param index index of value in statement
     * @param value the value to bind to it
     */
    @Implementation
    protected void bindLong(int index, long value) {
        Shadow.directlyOn(realProgram, SQLiteProgram.class).bindLong(index, value);
        longMap.put(index, value);
    }

    /**
     * Mock implementation that stores it's arguments then calls through to the original
     * 
     * @param index index of value in statement
     * @param value the value to bind to it
     */
    @Implementation
    protected void bindString(int index, String value) {
        Shadow.directlyOn(realProgram, SQLiteProgram.class).bindString(index, value);
        stringMap.put(index, value);
    }
}
