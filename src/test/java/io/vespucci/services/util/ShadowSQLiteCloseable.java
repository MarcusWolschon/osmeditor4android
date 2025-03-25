package io.vespucci.services.util;

import org.robolectric.annotation.Implements;

import android.database.sqlite.SQLiteClosable;

@Implements(value = SQLiteClosable.class)
public class ShadowSQLiteCloseable {
    // empty
}
