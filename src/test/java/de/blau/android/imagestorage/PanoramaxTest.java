package de.blau.android.imagestorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.filters.LargeTest;

/**
 * Note: these test currently only test the filter logic not the UI
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class PanoramaxTest {

    private static final String API_AUTH_TOKENS_GENERATE = "api/auth/tokens/generate";

    @Test
    public void apiUrlWithTrailingApi() {
        try {
            assertEquals("https://panoramax.openstreetmap.fr/api/auth/tokens/generate",
                    PanoramaxStorage.getApiUrl("https://panoramax.openstreetmap.fr/api", API_AUTH_TOKENS_GENERATE).toString());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void apiUrlWithTrailingApi2() {
        try {
            assertEquals("https://panoramax.openstreetmap.fr/api/auth/tokens/generate",
                    PanoramaxStorage.getApiUrl("https://panoramax.openstreetmap.fr/api/", API_AUTH_TOKENS_GENERATE).toString());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void apiUrlWithoutTrailingApi() {
        try {
            assertEquals("https://panoramax.openstreetmap.fr/api/auth/tokens/generate",
                    PanoramaxStorage.getApiUrl("https://panoramax.openstreetmap.fr/", API_AUTH_TOKENS_GENERATE).toString());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void apiUrlWithoutTrailingApi2() {
        try {
            assertEquals("https://panoramax.openstreetmap.fr/test/api/auth/tokens/generate",
                    PanoramaxStorage.getApiUrl("https://panoramax.openstreetmap.fr/test", API_AUTH_TOKENS_GENERATE).toString());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }
    }
    
    @Test
    public void apiUrlWithoutTrailingApi3() {
        try {
            assertEquals("https://panoramax.openstreetmap.fr/test/api/auth/tokens/generate",
                    PanoramaxStorage.getApiUrl("https://panoramax.openstreetmap.fr/test/", API_AUTH_TOKENS_GENERATE).toString());
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }
    }
}
