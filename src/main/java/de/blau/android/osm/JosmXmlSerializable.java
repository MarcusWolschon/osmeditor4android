package de.blau.android.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

/**
 * Serialize to JOSM specific XML
 * 
 * @author simon
 *
 */
public interface JosmXmlSerializable {

    /**
     * Generate JOSM format OSM XML files
     * 
     * @param serializer the XML serializer
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    void toJosmXml(XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException;
}
