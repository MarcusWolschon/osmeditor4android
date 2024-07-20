package de.blau.android.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.contract.Files;
import de.blau.android.net.OAuthHelper.OAuthConfiguration;
import de.blau.android.prefs.API.Auth;
import de.blau.android.resources.KeyDatabaseHelper.EntryType;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class KeyDatabaseTest {

    /**
     * Get OAuth1a keys
     */
    @Test
    public void oAuthKeysTest() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(ApplicationProvider.getApplicationContext());
                InputStream is = loader.getResourceAsStream(Files.FILE_NAME_KEYS_V2)) {
            keyDatabase.keysFromStream(null, is);
            OAuthConfiguration configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), "OpenStreetMap", Auth.OAUTH1A);
            assertEquals("1212121212", configuration.getKey());
            assertEquals("2121212121", configuration.getSecret());
            assertEquals("https://www.openstreetmap.org/", configuration.getOauthUrl());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Check if overwrite protection works as expected
     */
    @Test
    public void overwriteTest() {
        KeyDatabaseHelper.readKeysFromAssets(ApplicationProvider.getApplicationContext());
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(ApplicationProvider.getApplicationContext())) {
            KeyDatabaseHelper.replaceOrDeleteKey(keyDatabase.getWritableDatabase(), "Test", EntryType.API_KEY, "1111111111", true, false, null, null);

            assertEquals("1111111111", KeyDatabaseHelper.getKey(keyDatabase.getReadableDatabase(), "Test", EntryType.API_KEY));

            KeyDatabaseHelper.replaceOrDeleteKey(keyDatabase.getWritableDatabase(), "Test", EntryType.API_KEY, "2222222222", false, false, null, null);
            // this should still be the same
            assertEquals("1111111111", KeyDatabaseHelper.getKey(keyDatabase.getReadableDatabase(), "Test", EntryType.API_KEY));
            KeyDatabaseHelper.replaceOrDeleteKey(keyDatabase.getWritableDatabase(), "Test", EntryType.API_KEY, "2222222222", false, true, null, null);
            assertEquals("2222222222", KeyDatabaseHelper.getKey(keyDatabase.getReadableDatabase(), "Test", EntryType.API_KEY));
        }
    }

    /**
     * Delete an entry
     */
    @Test
    public void deleteTest() {
        KeyDatabaseHelper.readKeysFromAssets(ApplicationProvider.getApplicationContext());
        try (KeyDatabaseHelper keyDatabase = new KeyDatabaseHelper(ApplicationProvider.getApplicationContext())) {
            OAuthConfiguration configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), "OpenStreetMap", Auth.OAUTH1A);
            assertNotNull(configuration);
            KeyDatabaseHelper.deleteKey(keyDatabase.getWritableDatabase(), "OpenStreetMap", EntryType.API_OAUTH1_KEY);
            configuration = KeyDatabaseHelper.getOAuthConfiguration(keyDatabase.getReadableDatabase(), "OpenStreetMap", Auth.OAUTH1A);
            assertNull(configuration);
        }
    }
}
