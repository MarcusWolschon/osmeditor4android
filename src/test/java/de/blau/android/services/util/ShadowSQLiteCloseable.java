package de.blau.android.services.util;

import org.robolectric.annotation.Implements;

import android.database.sqlite.SQLiteClosable;

@Implements(value = SQLiteClosable.class)
public class ShadowSQLiteCloseable {
    // empty
}
