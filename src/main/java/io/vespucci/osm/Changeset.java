package io.vespucci.osm;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.util.LocaleUtils;

/**
 * An OSM Changeset
 * 
 * Currently we do not provide for changeset discussions
 * 
 * @author simon
 *
 */
public class Changeset {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Changeset.class.getSimpleName().length());
    private static final String DEBUG_TAG = Changeset.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TRUE               = "true";
    private static final String ID_ATTR            = "id";
    private static final String OPEN_ATTR          = "open";
    private static final String CHANGES_COUNT_ATTR = "changes_count";

    private long                          osmId = -1;
    private boolean                       open  = false;
    private String                        generator;
    private int                           changes;
    private final TreeMap<String, String> tags;

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
        getTags().put(Tags.KEY_CREATED_BY, generator);
        putTag(Tags.KEY_COMMENT, comment);
        putTag(Tags.KEY_SOURCE, source);
        getTags().put(Tags.KEY_LOCALE, LocaleUtils.toBcp47Language(Locale.getDefault()));
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
            getTags().put(key, value);
        }
    }

    /**
     * Get the changeset id
     * 
     * @return the osmId
     */
    long getOsmId() {
        return osmId;
    }

    /**
     * Check if this changeset is open
     * 
     * @return true is open
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Retrieve the number of changes in this changeset
     * 
     * @return the number of changes
     */
    public int getChanges() {
        return changes;
    }

    /**
     * Get the changeset tags
     * 
     * @return the tags
     */
    @NonNull
    TreeMap<String, String> getTags() {
        return tags;
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
            if (eventType != XmlPullParser.START_TAG) {
                continue;
            }
            switch (tagName) {
            case OsmXml.CHANGESET:
                if (parser.getAttributeCount() < 3) { // in testing these won't be available
                    break;
                }
                result.open = TRUE.equals(parser.getAttributeValue(null, OPEN_ATTR));
                try {
                    result.osmId = Long.parseLong(parser.getAttributeValue(null, ID_ATTR));
                    String changesStr = parser.getAttributeValue(null, CHANGES_COUNT_ATTR);
                    result.changes = Integer.parseInt(changesStr);
                } catch (NumberFormatException | NullPointerException ex) {
                    throw new XmlPullParserException(ex.getMessage());
                }
                break;
            case OsmElement.TAG:
                String k = parser.getAttributeValue(null, OsmElement.TAG_KEY_ATTR);
                String v = parser.getAttributeValue(null, OsmElement.TAG_VALUE_ATTR);
                result.tags.put(k, v);
                break;
            default:
                Log.d(DEBUG_TAG, "Unknown element " + tagName);
            }
        }
        Log.d(DEBUG_TAG, "#" + result.osmId + " is " + (result.isOpen() ? "open" : "closed" + " " + result.changes + " changes"));
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
                for (Entry<String, String> t : getTags().entrySet()) {
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
                serializer.startTag("", OsmElement.TAG);
                serializer.attribute("", OsmElement.TAG_KEY_ATTR, key);
                serializer.attribute("", OsmElement.TAG_VALUE_ATTR, value);
                serializer.endTag("", OsmElement.TAG);
            }
        };
    }
}
