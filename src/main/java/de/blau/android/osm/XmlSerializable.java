package de.blau.android.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

interface XmlSerializable {

    /**
     * Serialize the element in OSM XML format
     * 
     * @param serializer the serializer
     * @param changeSetId an optional changeset id
     * @throws IllegalArgumentException if the serializer encountered an illegal argument
     * @throws IllegalStateException if the serializer detects an illegal state
     * @throws IOException if writing to the serializer fails
     */
    void toXml(@NonNull XmlSerializer serializer, @Nullable Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException;
}
