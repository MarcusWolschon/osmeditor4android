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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Adapted from the broken google example
 * 
 * @see <a href="http://developer.android.com/guide/topics/text/copy-paste.html">Android copy-paste</a> Should work with
 *      pre-HONEYCOMB versions too
 *
 */
public final class ClipboardUtils {

    private static final String DEBUG_TAG = "ClipboardUtils";

    private static final String EOL = "\\r?\\n|\\r";

    @SuppressWarnings("deprecation")
    private static android.text.ClipboardManager oldClipboard = null;
    private static ClipboardManager              clipboard    = null;

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
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static boolean checkForText(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (clipboard == null) {
                clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            }
            return clipboard.hasPrimaryClip();
        } else {
            if (oldClipboard == null) {
                oldClipboard = (android.text.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            }
            return oldClipboard.hasText();
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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
                        } catch (Exception e) { // FIXME given that the above is unteted, cath all here
                            Log.e(DEBUG_TAG, "Resolving URI failed " + e);
                            return null;
                        }
                    }
                } else {
                    Log.d(DEBUG_TAG, "Clipboard contains text");
                    String pasteData = cs.toString();
                    return new ArrayList<>(Arrays.asList(pasteData.split(EOL)));
                }
            } else {
                // Gets the clipboard as text.
                @SuppressWarnings("deprecation")
                CharSequence cs = oldClipboard.getText();
                if (cs != null) {
                    String pasteData = cs.toString();
                    if (pasteData != null) { // should always be the case
                        return new ArrayList<>(Arrays.asList(pasteData.split(EOL)));
                    }
                }
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
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void copyTags(Context ctx, Map<String, String> tags) {

        StringBuilder tagsAsText = new StringBuilder();

        for (Entry<String, String> entry : tags.entrySet()) {
            tagsAsText.append(entry.getKey() + "=" + entry.getValue() + "\n");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("OSM Tags", tagsAsText.toString());
            clipboard.setPrimaryClip(clip);
        } else {
            android.text.ClipboardManager oldClipboard = (android.text.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            oldClipboard.setText(tagsAsText.toString());
        }
    }
}
