package io.vespucci.presets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import io.vespucci.presets.Regionalizable;

public class RegionalizableTest {

    class TestRegion extends Regionalizable {
        // just defaults
    }

    /**
     * Test positive appliesIn
     */
    @Test
    public void appliesIn() {
        TestRegion tr = new TestRegion();

        tr.setRegions("ch,de,eu");
        assertTrue(tr.appliesIn("CH"));
        assertTrue(tr.appliesIn("DE"));
        assertTrue(tr.appliesIn("EU"));
        assertFalse(tr.appliesIn("TR"));

        assertTrue(tr.appliesIn(Arrays.asList("CH")));
        assertTrue(tr.appliesIn(Arrays.asList("DE")));
        assertTrue(tr.appliesIn(Arrays.asList("EU")));
        assertFalse(tr.appliesIn(Arrays.asList("TR")));

        assertTrue(tr.appliesIn(Arrays.asList("CH", "DE")));
        assertTrue(tr.appliesIn(Arrays.asList("CH", "EU")));
        assertTrue(tr.appliesIn(Arrays.asList("CH", "DE", "EU")));
        assertTrue(tr.appliesIn(Arrays.asList("CH", "DE", "EU", "TR")));
    }

    /**
     * Test negative appliesIn
     */
    @Test
    public void appliesInExclude() {
        TestRegion tr = new TestRegion();
        tr.setExcludeRegions(true);

        tr.setRegions("ch,de,eu");
        assertFalse(tr.appliesIn("CH"));
        assertFalse(tr.appliesIn("DE"));
        assertFalse(tr.appliesIn("EU"));
        assertTrue(tr.appliesIn("TR"));

        assertFalse(tr.appliesIn(Arrays.asList("CH")));
        assertFalse(tr.appliesIn(Arrays.asList("DE")));
        assertFalse(tr.appliesIn(Arrays.asList("EU")));
        assertTrue(tr.appliesIn(Arrays.asList("TR")));

        assertFalse(tr.appliesIn(Arrays.asList("CH", "DE")));
        assertFalse(tr.appliesIn(Arrays.asList("CH", "Eu")));
        assertFalse(tr.appliesIn(Arrays.asList("CH", "DE", "EU")));
        assertFalse(tr.appliesIn(Arrays.asList("CH", "DE", "EU", "TR")));
    }
}