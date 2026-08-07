package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import org.xmlpull.v1.XmlPullParserException;

import crosby.binary.file.BlockInputStream;
import de.blau.android.util.Hash;

public class PbfTest {

    /**
     * Read a pbf osm file into a Storage object, write the object to XML and compare
     */
    @Test
    public void readPbf() {
        Storage storage = read();
        try {
            File file = File.createTempFile("liechtenstein-latest.osm", ".xml");
            System.out.println(file.getAbsolutePath());
            file.deleteOnExit();
            OsmXml.write(storage, null, new FileOutputStream(file), "Vespucci Unit Tests");
            try (DigestInputStream hashStream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("SHA-256"))) {
                byte[] buffer = new byte[1024];
                while (hashStream.read(buffer, 0, buffer.length) != -1) {
                    // do nothing
                }
                // Finish hash
                String hashValue = Hash.toHex(hashStream.getMessageDigest().digest());
                System.out.println(hashValue);
                assertEquals("f0cfa054c4c6189f7aed3fb4a81d36bc99ba80b92c3abf09d95a45330d5c68a5", hashValue);
            }
        } catch (IllegalArgumentException | IllegalStateException | XmlPullParserException | IOException | NoSuchAlgorithmException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Read and parse a pbf from resources
     * 
     * @return a Storage instance containing the data
     */
    public static Storage read() {
        InputStream input = PbfTest.class.getResourceAsStream("/liechtenstein-latest.osm.pbf");
        Storage storage = new Storage();
        OsmPbfParser brad = new OsmPbfParser(storage);
        try (BlockInputStream bis = new BlockInputStream(input, brad)) {
            bis.process();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return storage;
    }
}