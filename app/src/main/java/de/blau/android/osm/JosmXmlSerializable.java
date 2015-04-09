package de.blau.android.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

public interface JosmXmlSerializable {

	public void toJosmXml(XmlSerializer serializer)
			throws IllegalArgumentException, IllegalStateException, IOException;

}
