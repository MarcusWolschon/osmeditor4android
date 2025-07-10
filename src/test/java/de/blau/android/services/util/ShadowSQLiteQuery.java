package de.blau.android.services.util;

import org.robolectric.annotation.Implements;

import android.database.sqlite.SQLiteQuery;

@Implements(value = SQLiteQuery.class)
public class ShadowSQLiteQuery extends ShadowSQLiteProgram {

}
