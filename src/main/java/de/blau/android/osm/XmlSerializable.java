package de.blau.android.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

interface XmlSerializable {

    void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException;

}
