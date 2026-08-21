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
     * @param original if true the original version, otherwise the current, only relevant for indirect changes
     * @param undo instance of UndoStorage only used if original is true
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    void toAugmentedXml(XmlSerializer serializer, boolean original, UndoStorage undo) throws IllegalArgumentException, IllegalStateException, IOException;
}
