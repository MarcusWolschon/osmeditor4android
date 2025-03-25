package io.vespucci.util.egm96;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import io.vespucci.UnitTestUtils;
import io.vespucci.util.egm96.EGM96;

public class Egm96Test {

    private static final String EGMFILE = "EGM96.dat";

    /**
     * Read the offset file and check one offset
     */
    @Test
    public void loadEgm96() {
        try {
            UnitTestUtils.copyFileFromResources(Egm96Test.class, EGMFILE);
            EGM96 egm = new EGM96(EGMFILE);
            assertEquals(47.566, egm.getOffset(47.3979095, 8.3762719), 0.001);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            File egmFile = new File(EGMFILE);
            egmFile.delete();
        }
    }
}