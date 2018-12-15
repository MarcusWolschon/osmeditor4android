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
     * @throws IllegalArgumentException
     * @throws IllegalStateException
     * @throws IOException
     */
    void toXml(@NonNull XmlSerializer serializer, @Nullable Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException;
}
