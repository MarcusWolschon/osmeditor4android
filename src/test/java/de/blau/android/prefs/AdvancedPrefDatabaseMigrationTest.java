package de.blau.android.prefs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class AdvancedPrefDatabaseMigrationTest {

    /**
     * Note this will only catch crashes as is, not logic errors
     */
    @Test
    public void migrationTest() {
        final Context ctx = ApplicationProvider.getApplicationContext();
        ctx.deleteDatabase(AdvancedPrefDatabase.DATABASE_NAME);
        try (OriginalAdvancedPrefDatabase db = new OriginalAdvancedPrefDatabase(ctx)) {
            // this should create the version 2 or so database
            assertEquals(OriginalAdvancedPrefDatabase.DATA_VERSION, db.getReadableDatabase().getVersion());
        }
        try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(ctx)) {
            // this should migrate to the current state
            assertEquals(AdvancedPrefDatabase.DATA_VERSION, db.getReadableDatabase().getVersion());
        }
    }
}
