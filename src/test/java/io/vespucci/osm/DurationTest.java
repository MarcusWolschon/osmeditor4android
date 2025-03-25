package io.vespucci.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.vespucci.osm.Duration;

public class DurationTest {

    /**
     * Parse duration values
     */
    @Test
    public void parse() {
        assertEquals(32 * 60, Duration.parse("32"));
        assertEquals(32 * 60, Duration.parse("00:32"));
        assertEquals(32 * 60, Duration.parse("00:32:00"));

        assertEquals(0, Duration.parse("00:00"));

        assertEquals(8 * 3600 + 32 * 60, Duration.parse("08:32"));

        assertEquals(0, Duration.parse("00:00:00"));
        assertEquals(32, Duration.parse("00:00:32"));

        assertEquals(11 * 3600 + 8 * 60 + 32, Duration.parse("11:08:32"));
        assertEquals(111 * 3600 + 8 * 60 + 32, Duration.parse("111:08:32"));

        try {
            Duration.parse("A teat");
            fail();
        } catch (NumberFormatException nfe) {
            // expected
        }
    }

    /**
     * Test output formatting
     */
    @Test
    public void output() {
        assertEquals("00:00:32", Duration.toString(32));
        assertEquals("32", Duration.toString(32 * 60));
        assertEquals("11:08", Duration.toString(11 * 3600 + 8 * 60));
        assertEquals("11:08:32", Duration.toString(11 * 3600 + 8 * 60 + 32));
        assertEquals("111:08:32", Duration.toString(111 * 3600 + 8 * 60 + 32));
    }
}