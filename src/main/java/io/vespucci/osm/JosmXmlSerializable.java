package io.vespucci.osm;

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
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    void toJosmXml(XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException;
}
