package de.blau.android.osm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import de.blau.android.util.Util;

/**
 * An OSM Changeset
 * 
 * Currently we do not provide for changeset discussions
 * 
 * @author simon
 *
 */
public class Changeset {
    private static final String DEBUG_TAG = "Changeset";

    long osmId = -1;

    boolean open = false;

    String generator;

    final TreeMap<String, String> tags;

    /**
     * Default constructor
     */
    Changeset() {
        tags = new TreeMap<>();
    }

    /**
     * Construct a new Changeset setting some tags from arguments
     * 
     * Always adds a locale tag.
     * 
     * @param generator the id of this application for OSM
     * @param comment value for the comment tag
     * @param source value for the source tag
     * @param imagery value for the imagery_used tag
     * @param extraTags Additional tags to add
     */
    Changeset(@NonNull String generator, @Nullable final String comment, @Nullable final String source, @Nullable final String imagery,
            @Nullable Map<String, String> extraTags) {
        this();
        this.generator = generator;
        tags.put(Tags.KEY_CREATED_BY, generator);
        putTag(Tags.KEY_COMMENT, comment);
        putTag(Tags.KEY_SOURCE, source);
        tags.put(Tags.KEY_LOCALE, Util.toBcp47Language(Locale.getDefault()));
        putTag(Tags.KEY_IMAGERY_USED, imagery);
        if (extraTags != null) {
            for (Entry<String, String> t : extraTags.entrySet()) {
                putTag(t.getKey(), t.getValue());
            }
        }
    }

    /**
     * Add a tag
     * 
     * @param key the tag key (if null no tag will be added)
     * @param value the tag value (if null no tag will be added)
     */
    private void putTag(@Nullable final String key, @Nullable final String value) {
        if (key != null && !"".equals(key) && value != null && !"".equals(value)) {
            tags.put(key, value);
        }
    }

    /**
     * Create a new Changeset from an InputStream in XML format
     * 
     * @param parser an XmlPullParser instance
     * @param is the InputStream
     * @return a Changeset
     * @throws XmlPullParserException if parsing fails
     * @throws IOException if an IO operation fails
     */
    @NonNull
    static Changeset parse(@NonNull XmlPullParser parser, @NonNull InputStream is) throws XmlPullParserException, IOException {

        parser.setInput(is, null);
        int eventType;
        Changeset result = new Changeset();

        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                switch (tagName) {
                case "changeset":
                    result.open = "true".equals(parser.getAttributeValue(null, "open"));
                    try {
                        result.osmId = Long.parseLong(parser.getAttributeValue(null, "id"));
                    } catch (NumberFormatException nex) {
                        throw new XmlPullParserException(nex.getMessage());
                    }
                    break;
                case "tag":
                    String k = parser.getAttributeValue(null, "k");
                    String v = parser.getAttributeValue(null, "v");
                    result.tags.put(k, v);
                    break;
                default:
                    // nothing
                }
                Log.d(DEBUG_TAG, "#" + result.osmId + " is " + (result.open ? "open" : "closed"));
            }
        }
        return result;
    }

    /**
     * Generate xml for the changeset tags
     * 
     * @return an XmlSerializable for the tags
     */
    XmlSerializable tagsToXml() {
        return new XmlSerializable() {
            @Override
            public void toXml(XmlSerializer serializer, Long changeSetId) throws IllegalArgumentException, IllegalStateException, IOException {
                Server.startXml(serializer, generator);
                serializer.startTag("", OsmXml.CHANGESET);
                for (Entry<String, String> t : tags.entrySet()) {
                    addTag(serializer, t.getKey(), t.getValue());
                }
                serializer.endTag("", OsmXml.CHANGESET);
                Server.endXml(serializer);
            }

            /**
             * Serialize a tag
             * 
             * @param serializer the serializer
             * @param key the key
             * @param value the value
             * @throws IOException if writing to the serializer fails
             */
            private void addTag(@NonNull XmlSerializer serializer, @NonNull String key, @NonNull String value) throws IOException {
                serializer.startTag("", OsmXml.TAG);
                serializer.attribute("", "k", key);
                serializer.attribute("", "v", value);
                serializer.endTag("", OsmXml.TAG);
            }
        };
    }
}
