package de.blau.android.presets;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import androidx.test.filters.LargeTest;

/**
 * Some preset parsing error tests
 * 
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class PresetParserTest {


    /**
     * Test that missing keys throw an exception
     */
    @Test
    public void missingKey() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = loader.getResourceAsStream("test-preset-missing-key.xml")) {
            Preset preset = Preset.dummyInstance();
            PresetParser.parseXML(preset, input, true);
        } catch (SAXException sex) {
            assertTrue(sex.getMessage().contains("key must be present"));
        } catch (ParserConfigurationException | IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test that missing values throw an exception
     */
    @Test
    public void missingValue() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = loader.getResourceAsStream("test-preset-missing-value.xml")) {
            Preset preset = Preset.dummyInstance();
            PresetParser.parseXML(preset, input, true);
        } catch (SAXException sex) {
            assertTrue(sex.getMessage().contains("value must be present in key field"));
        } catch (ParserConfigurationException | IOException e) {
            fail(e.getMessage());
        }
    }
}
