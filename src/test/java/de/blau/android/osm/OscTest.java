package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class OscTest {

    /**
     * Read an osc file
     */
    @Test
    public void readOsmAndOsc() {
        InputStream input = getClass().getResourceAsStream("/osmand.osc");
        OsmChangeParser parser = new OsmChangeParser();
        try {
            parser.start(input);
            Storage storage = parser.getStorage();
            assertFalse(storage.isEmpty());
            assertEquals(1, storage.getElements().size());
            assertEquals(1, parser.getNotes().size());
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException e) {
            fail(e.getMessage());
        }
    }
}