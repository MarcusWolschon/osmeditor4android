package de.blau.android.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import de.blau.android.util.Hash;

public class XmlTest {

    /**
     * Read an osm file into a Storage object, write the object to XML and compare hash
     */
    @Test
    public void readXml() {
        InputStream input = getClass().getResourceAsStream("/test2.osm");
        OsmParser parser = new OsmParser();
        try {
            parser.start(input);
            Storage storage = parser.getStorage();
            assertFalse(storage.isEmpty());
            File file = File.createTempFile("test.osm", ".xml");
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
                assertEquals("19bc1a700e9a7c0130625b1f8a20273283b93a982b60f5c756d7c099af8739fb", hashValue);
            } catch (NoSuchAlgorithmException e) {
                fail(e.getMessage());
            }
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
            fail(e.getMessage());
        }
    }
    
    /**
     * Read an osm file into a Storage object, write the object to OSC and compare hash
     */
    @Test
    public void oscWrite() {
        InputStream input = getClass().getResourceAsStream("/test2.osm");
        OsmParser parser = new OsmParser();
        try {
            parser.start(input);
            Storage storage = parser.getStorage();
            assertFalse(storage.isEmpty());
            File file = File.createTempFile("test.osm", ".osc");
            file.deleteOnExit();
            OsmXml.writeOsmChange(storage, new FileOutputStream(file), 123456789L, 10000, "Vespucci Unit Tests");
            try (DigestInputStream hashStream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("SHA-256"))) {
                byte[] buffer = new byte[1024];
                while (hashStream.read(buffer, 0, buffer.length) != -1) {
                    // do nothing
                }
                // Finish hash
                String hashValue = Hash.toHex(hashStream.getMessageDigest().digest());
                System.out.println(hashValue);
                assertEquals("f5bb3f1f27b4f7e9061d514d726424bf822691f5645af00731936733e3f8c01f", hashValue);
            } catch (NoSuchAlgorithmException e) {
                fail(e.getMessage());
            }
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException | XmlPullParserException e) {
            fail(e.getMessage());
        }
    }
}