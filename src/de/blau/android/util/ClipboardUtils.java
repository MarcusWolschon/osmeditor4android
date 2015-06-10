package de.blau.android.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
/**
 * Adapted from the broken google example 
 * @see http://developer.android.com/guide/topics/text/copy-paste.html
 * Should work with pre-HONEYCOMB versions too
 *
 */
public class ClipboardUtils {
	
	static ClipboardManager clipboard = null;
		
	/**
	 * Return true if there is text in the clipboard
	 * @param ctx
	 * @return
	 */
	@SuppressLint("NewApi")
	static boolean checkForText(Context ctx){
		if (clipboard == null) {
			clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return clipboard.hasPrimaryClip();
		} else {
			return clipboard.hasText();
		}
	}
	
	/**
	 * Return text content of clipboard as individual lines 
	 * @param ctx
	 * @return
	 */
	@SuppressLint("NewApi")
	public static ArrayList<String> getTextLines(Context ctx) {

		String EOL = "\\r?\\n|\\r";
		
		if (checkForText(ctx)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

				// Gets the clipboard as text.
				CharSequence cs = item.getText();
				if (cs == null) { // item might be an URI
					Uri pasteUri = item.getUri();
					if (pasteUri != null) { // FIXME untested
						try {
							Log.d("ClipboardUtils","Clipboard contains an uri");
							ContentResolver cr = ctx.getContentResolver();
							String uriMimeType = cr.getType(pasteUri);
							//					pasteData = resolveUri(pasteUri);
							// If the return value is not null, the Uri is a content Uri
							if (uriMimeType != null) {

							    // Does the content provider offer a MIME type that the current application can use?
							    if (uriMimeType.equals(ClipDescription.MIMETYPE_TEXT_PLAIN)) {

							        // Get the data from the content provider.
							        Cursor pasteCursor = cr.query(pasteUri, null, null, null, null);

							        // If the Cursor contains data, move to the first record
							        if (pasteCursor != null) {
							            if (pasteCursor.moveToFirst()) {
							            	String pasteData =  pasteCursor.getString(0);
							            	return new ArrayList<String>(Arrays.asList(pasteData.split(EOL)));
							            }
							            // close the Cursor
								        pasteCursor.close();
							        }
							     }
							 }
						} catch (Exception e) { // FIXME given that the above is unteted, cath all here
							Log.e("ClipboardUtils","Resolving URI failed " + e);
							e.printStackTrace();
							return null;
						}
					}
				} else {
					Log.d("ClipboardUtils","Clipboard contains text");
					String pasteData = cs.toString();
					return new ArrayList<String>(Arrays.asList(pasteData.split(EOL)));
				}
			} else {
				// Gets the clipboard as text.
				CharSequence cs = clipboard.getText();
				if (cs != null) {
					String pasteData = cs.toString();
					if (pasteData != null) { // should always be the case
						return new ArrayList<String>(Arrays.asList(pasteData.split(EOL)));
					}
				}
			}
			Log.e("ClipboardUtils","Clipboard contains an invalid data type");
		}
		return null;
	}
		
	/**
	 * Return content of clipboard as key value tuples assuming key=value notation  
	 * @param ctx
	 * @return
	 */
	public static ArrayList<KeyValue> getKeyValues(Context ctx) {
		ArrayList<String> textLines = getTextLines(ctx);
		if (textLines != null) {
			ArrayList<KeyValue> keysAndValues = new ArrayList<KeyValue>();
			for (String line:textLines) {
				if (line.contains("=")) {
					String[] r = line.split("=",2);
					if (r.length == 2) {
						keysAndValues.add(new KeyValue(r[0],r[1]));
					} else {
						Log.e("ClipboardUtils","Split of key = value failed");
					}
				} else {
					keysAndValues.add(new KeyValue(null, line));
					Log.d("ClipboardUtils","no key, value=" + line);
				}
			}
			return keysAndValues;
		} else {
			return null;
		}
	}
	
	/**
	 * Copy tags to clipboard as multi-line text in the form
	 * key1=value1
	 * key2=value2
	 * .....
	 * @param tags
	 */
	@SuppressLint("NewApi")
	public static void copyTags(Context ctx,Map<String,String> tags) {
		ClipboardManager clipboard = (ClipboardManager)
		        ctx.getSystemService(Context.CLIPBOARD_SERVICE);
		StringBuffer tagsAsText = new StringBuffer();
		
		for (String key:tags.keySet()) {
			tagsAsText.append(key+"="+tags.get(key)+"\n");
		}
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ClipData clip = ClipData.newPlainText("OSM Tags",tagsAsText.toString());
			clipboard.setPrimaryClip(clip);
		} else {
			clipboard.setText(tagsAsText.toString());
		}
	}
}
