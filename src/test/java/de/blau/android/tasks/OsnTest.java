package de.blau.android.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class OsnTest {

    /**
     * Read an osn file
     */
    @Test
    public void readOsn() {
        InputStream input = getClass().getResourceAsStream("/test.osn");
        OsnParser parser = new OsnParser();
        try {
            parser.start(input);
            assertEquals(59, parser.getNotes().size());
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException e) {
            fail(e.getMessage());
        }
    }
}