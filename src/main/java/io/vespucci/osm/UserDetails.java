package io.vespucci.osm;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Class for OSM API 0.6 user details
 * 
 * display name and message counts is the only thing that is interesting for now
 * 
 * @author simon
 *
 */
public class UserDetails {
    private static final String DEBUG_TAG = UserDetails.class.getSimpleName().substring(0, Math.min(23, UserDetails.class.getSimpleName().length()));

    private static final String SENT_ELEMENT      = "sent";
    private static final String UNREAD_ATTR       = "unread";
    private static final String COUNT_ATTR        = "count";
    private static final String RECEIVED_ELEMENT  = "received";
    private static final String MESSAGES_ELEMENT  = "messages";
    private static final String USER_ELEMENT      = "user";
    private static final String DISPLAY_NAME_ATTR = "display_name";

    private String displayName = "unknown";
    private int    received    = 0;
    private int    unread      = 0;
    private int    sent        = 0;

    /**
     * @return the display_name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the received
     */
    public int getReceivedMessages() {
        return received;
    }

    /**
     * @param received the received to set
     */
    public void setReceivedMessages(int received) {
        this.received = received;
    }

    /**
     * @return the unread
     */
    public int getUnreadMessages() {
        return unread;
    }

    /**
     * @param unread the unread to set
     */
    public void setUnreadMessages(int unread) {
        this.unread = unread;
    }

    /**
     * @return the sent
     */
    public int getSentMessages() {
        return sent;
    }

    /**
     * @param sent the sent to set
     */
    public void setSentMessages(int sent) {
        this.sent = sent;
    }

    @NonNull
    static UserDetails fromXml(@NonNull XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        int eventType;
        UserDetails result = new UserDetails();
        boolean messages = false;
        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG && USER_ELEMENT.equals(tagName)) {
                result.setDisplayName(parser.getAttributeValue(null, DISPLAY_NAME_ATTR));
                Log.d(DEBUG_TAG, "getUserDetails display name " + result.getDisplayName());
            }
            if (eventType == XmlPullParser.START_TAG && MESSAGES_ELEMENT.equals(tagName)) {
                messages = true;
            }
            if (eventType == XmlPullParser.END_TAG && MESSAGES_ELEMENT.equals(tagName)) {
                messages = false;
            }
            if (messages) {
                if (eventType == XmlPullParser.START_TAG && RECEIVED_ELEMENT.equals(tagName)) {
                    result.setReceivedMessages(Integer.parseInt(parser.getAttributeValue(null, COUNT_ATTR)));
                    Log.d(DEBUG_TAG, "getUserDetails received " + result.getReceivedMessages());
                    result.setUnreadMessages(Integer.parseInt(parser.getAttributeValue(null, UNREAD_ATTR)));
                    Log.d(DEBUG_TAG, "getUserDetails unread " + result.getUnreadMessages());
                }
                if (eventType == XmlPullParser.START_TAG && SENT_ELEMENT.equals(tagName)) {
                    result.setSentMessages(Integer.parseInt(parser.getAttributeValue(null, COUNT_ATTR)));
                    Log.d(DEBUG_TAG, "getUserDetails sent " + result.getSentMessages());
                }
            }
        }
        return result;
    }
}
