package io.vespucci.util;

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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.R;

/**
 * Adapted from the broken google example
 * 
 * @author Simon Poole
 * 
 * @see <a href="http://developer.android.com/guide/topics/text/copy-paste.html">Android copy-paste</a>
 *
 */
public final class ClipboardUtils {

    private static final String DEBUG_TAG = ClipboardUtils.class.getSimpleName().substring(0, Math.min(23, ClipboardUtils.class.getSimpleName().length()));

    private static final String EOL                 = "\\r?\\n|\\r";
    private static final String NON_BREAKABLE_SPACE = "\u00A0";

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
    @NonNull
    private static List<String> getTextLines(@NonNull Context ctx) {
        List<String> result = new ArrayList<>();
        if (!checkForText(ctx)) {
            Log.e(DEBUG_TAG, "Clipboard contains an invalid data type");
            return result;
        }
        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        // Gets the clipboard as text.
        CharSequence cs = item.getText();
        if (cs == null) { // item might be an URI
            Uri pasteUri = item.getUri();
            if (pasteUri == null) {
                Log.e(DEBUG_TAG, "Clipboard doesn't contain an URI");
                return result;
            }
            // FIXME untested
            try {
                Log.d(DEBUG_TAG, "Clipboard contains an uri");
                ContentResolver cr = ctx.getContentResolver();
                String uriMimeType = cr.getType(pasteUri);
                // If the return value is not null, the Uri is a content Uri
                if (uriMimeType == null || !uriMimeType.equals(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    Log.e(DEBUG_TAG, "Clipboard URI doesn't refer to text");
                    return result;
                }
                // Does the content provider offer a MIME type that the current application can use?
                // Get the data from the content provider.
                Cursor pasteCursor = cr.query(pasteUri, null, null, null, null);
                // If the Cursor contains data, move to the first record
                if (pasteCursor != null) {
                    if (pasteCursor.moveToFirst()) {
                        String pasteData = pasteCursor.getString(0);
                        result.addAll(Arrays.asList(pasteData.split(EOL)));
                    }
                    // close the Cursor
                    pasteCursor.close();
                }
            } catch (Exception e) { // catch all here
                Log.e(DEBUG_TAG, "Resolving URI failed " + e);
            }
        } else {
            Log.d(DEBUG_TAG, "Clipboard contains text");
            String pasteData = cs.toString();
            result.addAll(Arrays.asList(pasteData.split(EOL)));
        }
        return result;
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
        if (!textLines.isEmpty()) {
            List<KeyValue> keysAndValues = new ArrayList<>();
            for (String line : textLines) {
                if (line != null) {
                    String[] r = line.split("=", 2);
                    if (r.length == 2) {
                        keysAndValues.add(new KeyValue(trim(r[0]), trim(r[1])));
                    } else {
                        keysAndValues.add(new KeyValue("", trim(line)));
                    }
                }
            }
            return keysAndValues;
        }
        return null;
    }

    /**
     * Trim, removing non-breakable space too
     * 
     * This is necessary as we can't use strip() prior to API 33, and particularly stuff copied pasted from a website
     * may contain non-breakable space, naturally we should probably remove other UTF WS too.
     * 
     * @param in the input string
     * @return a trimmed string
     */
    @NonNull
    private static String trim(@NonNull String in) {
        return in.replace(NON_BREAKABLE_SPACE, " ").trim();
    }

    /**
     * Copy tags to clipboard as multi-line text in the form key1=value1 key2=value2 .....
     * 
     * @param ctx Android Context
     * @param tags Map containing the tags
     */
    public static void copyTags(@NonNull Context ctx, @NonNull Map<String, String> tags) {
        StringBuilder tagsAsText = new StringBuilder();
        for (Entry<String, String> entry : tags.entrySet()) {
            tagsAsText.append(entry.getKey() + "=" + entry.getValue() + "\n");
        }
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(ctx.getString(R.string.osm_tags), tagsAsText.toString());
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Copy some text to the system clipboard
     * 
     * @param ctx an Android Context
     * @param label a label
     * @param text the text to copy
     */
    public static void copyText(@NonNull Context ctx, @NonNull String label, @NonNull CharSequence text) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }
}
