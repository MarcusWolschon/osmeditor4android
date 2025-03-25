package io.vespucci.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import io.vespucci.tasks.Note;
import io.vespucci.tasks.OsnParser;

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
            List<Note> notes = parser.getNotes();
            assertEquals(59, notes.size());
            for (Note n : notes) {
                if (n.getId() == 693118L) {
                    assertEquals(1487164934000L, n.getLastUpdate().getTime());
                    assertEquals(4, n.count());
                    return;
                }
            }
            fail("Note 12992 not found");
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException e) {
            fail(e.getMessage());
        }
    }
}