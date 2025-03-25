package io.vespucci.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.vespucci.net.ContentDispositionFileNameParser;

public class ContentDispositionFileNameParserTest {

    /**
     * Parser a header value and check that we get the expected value
     */
    @Test
    public void parseTest() {
        assertEquals("1956.gpx", ContentDispositionFileNameParser.parse("attachment; filename=\"1956.gpx\"; filename*=UTF-8''1956.gpx"));
        assertEquals("1956.gpx", ContentDispositionFileNameParser.parse("attachment; filename=\"1956.gpx\"; filename*=ISO-8859-1''1956.gpx"));
        assertEquals("1956.gpx", ContentDispositionFileNameParser.parse("attachment; filename=\"1956.gpx\"; filename*=1956.gpx"));
        assertEquals("1956.gpx", ContentDispositionFileNameParser.parse("attachment; filename*=UTF-8''1956.gpx"));
        assertEquals("1956.gpx", ContentDispositionFileNameParser.parse("attachment; filename*=ISO-8859-1''1956.gpx"));
        assertEquals("1956.gpx", ContentDispositionFileNameParser.parse("attachment; filename*=1956.gpx"));
        assertEquals("19 56.gpx", ContentDispositionFileNameParser.parse("attachment; filename=\"19%2056.gpx\"; filename*=19%2056.gpx"));
        // note the following should actually work, the parser is not conformant here
        assertNull(ContentDispositionFileNameParser.parse("attachment; filename=\"1956.gpx\""));
        assertNull(ContentDispositionFileNameParser.parse("attachment"));
    }
}
