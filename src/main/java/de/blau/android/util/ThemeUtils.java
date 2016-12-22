package de.blau.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 * 
 * @see http://stackoverflow.com/questions/3083627/android-reference-the-value-of-an-attribute-in-the-currently-applied-theme-fro/3679026#3679026
 *
 */
public final class ThemeUtils {
    // Prevent instantiation since this is a utility class
    private ThemeUtils() {}

   /**
    * Returns the color value of the style attribute queried.
    *
    * <p>The attribute will be queried from the theme returned from {@link Context#getTheme()}.</p>
    *
    * @param context the caller's context
    * @param attribResId the attribute id (i.e. R.attr.some_attribute)
    * @param defaultValue the value to return if the attribute does not exist
    * @return the color value for the attribute or defaultValue
    */
    public static int getStyleAttribColorValue(final Context context, final int attribResId, final int defaultValue) {
        final TypedValue tv = new TypedValue();
        final boolean found = context.getTheme().resolveAttribute(attribResId, tv, true);
        if (!found) {
        	Log.d("ThemeUtils", "themed color not found");
        	return defaultValue;
        }
        return tv.data;
    }
    
    public static int getResIdFromAttribute(final Context context,final int attr)
    {
    	if(attr==0) {
    		Log.d("ThemeUtils", "getResIdFromAttribute attr zero");
    		return 0;
    	}
    	final TypedValue typedvalueattr=new TypedValue();
    	if (!context.getTheme().resolveAttribute(attr,typedvalueattr,true)) {
    		Log.d("ThemeUtils", "getResIdFromAttribute attr "+ attr + " not found");
    		return 0;
    	}
    	return typedvalueattr.resourceId; 
    }
    
    public static int getDimensionFromAttribute(final Context context,final int attr)
    {
    	int[] attrs = new int[] { attr /* index 0 */};
    	TypedArray ta = null;
    	try {
			ta = context.getTheme().obtainStyledAttributes(attrs);
			return ta.getDimensionPixelSize(0, 0);
		} catch (Resources.NotFoundException nfe) {
    		Log.d("ThemeUtils", "getIntFromAttribute attr "+ attr + " not found");
    		return 0;
		} finally {
			if (ta != null) {
				ta.recycle();
			}
		} 	
    }
    
    public static LayoutInflater getLayoutInflater(Context caller) {
    	Preferences prefs = new Preferences(caller);
		Context context =  new ContextThemeWrapper(caller, prefs.lightThemeEnabled() ? R.style.Theme_DialogLight : R.style.Theme_DialogDark);
		return (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
}
