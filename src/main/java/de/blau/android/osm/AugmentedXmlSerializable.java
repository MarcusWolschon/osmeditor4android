package de.blau.android.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

/**
 * Serialize to AugmentedDiff XML
 * 
 * @author simon
 *
 */
public interface AugmentedXmlSerializable {

    /**
     * Generate augmented diff format OSM XML files
     * 
     * @param serializer the XML serializer
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    void toAugmentedXml(XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException;
}
