package de.blau.android.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Adapted from the broken google example
 * 
 * @author Simon Poole
 * 
 * @see <a href="http://developer.android.com/guide/topics/text/copy-paste.html">Android copy-paste</a>
 *
 */
public final class ClipboardUtils {

    private static final String DEBUG_TAG = "ClipboardUtils";

    private static final String EOL = "\\r?\\n|\\r";

    private static ClipboardManager clipboard = null;

    /**
     * Private constructor to stop instantiation
     */
    private ClipboardUtils() {
        // private
    }

    /**
     * Return true if there is text in the clipboard
     * 
     * @param ctx Android Context
     * @return true if there is text present
     */
    @SuppressLint("NewApi")
    public static boolean checkForText(Context ctx) {
        if (clipboard == null) {
            clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        }
        return clipboard.hasPrimaryClip();
    }

    /**
     * Return text content of clipboard as individual lines
     * 
     * @param ctx Android Context
     * @return list of Strings
     */
    @SuppressLint("NewApi")
    private static List<String> getTextLines(@NonNull Context ctx) {
        if (checkForText(ctx)) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

            // Gets the clipboard as text.
            CharSequence cs = item.getText();
            if (cs == null) { // item might be an URI
                Uri pasteUri = item.getUri();
                if (pasteUri != null) { // FIXME untested
                    try {
                        Log.d(DEBUG_TAG, "Clipboard contains an uri");
                        ContentResolver cr = ctx.getContentResolver();
                        String uriMimeType = cr.getType(pasteUri);
                        // pasteData = resolveUri(pasteUri);
                        // If the return value is not null, the Uri is a content Uri
                        if (uriMimeType != null) {
                            // Does the content provider offer a MIME type that the current application can use?
                            if (uriMimeType.equals(ClipDescription.MIMETYPE_TEXT_PLAIN)) {

                                // Get the data from the content provider.
                                Cursor pasteCursor = cr.query(pasteUri, null, null, null, null);

                                // If the Cursor contains data, move to the first record
                                if (pasteCursor != null) {
                                    if (pasteCursor.moveToFirst()) {
                                        String pasteData = pasteCursor.getString(0);
                                        return new ArrayList<>(Arrays.asList(pasteData.split(EOL)));
                                    }
                                    // close the Cursor
                                    pasteCursor.close();
                                }
                            }
                        }
                    } catch (Exception e) { // FIXME given that the above is untested, catch all here
                        Log.e(DEBUG_TAG, "Resolving URI failed " + e);
                        return null;
                    }
                }
            } else {
                Log.d(DEBUG_TAG, "Clipboard contains text");
                String pasteData = cs.toString();
                return new ArrayList<>(Arrays.asList(pasteData.split(EOL)));
            }
            Log.e(DEBUG_TAG, "Clipboard contains an invalid data type");
        }
        return null;
    }

    /**
     * Return content of clipboard as key value tuples assuming key=value notation
     * 
     * @param ctx Android Context
     * @return list of KeyValue objects
     */
    @Nullable
    public static List<KeyValue> getKeyValues(@NonNull Context ctx) {
        List<String> textLines = getTextLines(ctx);
        if (textLines != null) {
            List<KeyValue> keysAndValues = new ArrayList<>();
            for (String line : textLines) {
                if (line != null) {
                    String[] r = line.split("=", 2);
                    if (r.length == 2) {
                        keysAndValues.add(new KeyValue(r[0], r[1]));
                    } else {
                        keysAndValues.add(new KeyValue("", line));
                    }
                }
            }
            return keysAndValues;
        } else {
            return null;
        }
    }

    /**
     * Copy tags to clipboard as multi-line text in the form key1=value1 key2=value2 .....
     * 
     * @param ctx Android Context
     * @param tags Map containing the tags
     */
    public static void copyTags(Context ctx, Map<String, String> tags) {

        StringBuilder tagsAsText = new StringBuilder();

        for (Entry<String, String> entry : tags.entrySet()) {
            tagsAsText.append(entry.getKey() + "=" + entry.getValue() + "\n");
        }

        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("OSM Tags", tagsAsText.toString());
        clipboard.setPrimaryClip(clip);
    }
}
