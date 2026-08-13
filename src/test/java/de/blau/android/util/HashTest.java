package de.blau.android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class HashTest {

    /**
     * Test SHA-256 hashing against known values
     */
    @Test
    public void sha256() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Hash.sha256(""));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hash.sha256("abc"));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Hash.sha256("hello"));
    }

    /**
     * Test that different inputs produce different hashes
     */
    @Test
    public void sha256DifferentInputs() {
        assertNotEquals(Hash.sha256("hello"), Hash.sha256("world"));
    }

    /**
     * Test hex conversion
     */
    @Test
    public void toHex() {
        assertEquals("", Hash.toHex(new byte[] {}));
        assertEquals("00", Hash.toHex(new byte[] { 0 }));
        assertEquals("ff", Hash.toHex(new byte[] { (byte) 0xFF }));
        assertEquals("0102ff", Hash.toHex(new byte[] { 1, 2, (byte) 0xFF }));
    }
}
