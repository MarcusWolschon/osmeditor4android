package io.vespucci.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import io.vespucci.util.FileUtil;

public class FileUtilTest {

    /**
     * Test that copying doesn't overwrite
     */
    @Test
    public void copyTest() {
        try {
            File in = File.createTempFile("source", "txt");
            File out = File.createTempFile("destination", "txt");
            FileUtil.copy(in, out);
            assertTrue(out.exists());
            FileUtil.copy(in, out);
            File out2 = new File(out.getAbsolutePath() + " (1)");
            assertTrue(out2.exists());
            assertTrue(out2.delete());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}