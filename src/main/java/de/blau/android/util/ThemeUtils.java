package de.blau.android.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.prefs.Preferences;

/**
 *
 * @author Simon Poole
 * 
 * @see <a href=
 *      "http://stackoverflow.com/questions/3083627/android-reference-the-value-of-an-attribute-in-the-currently-applied-theme-fro/3679026#3679026">Reference
 *      the value of an attribute in the currently applied theme</a>
 *
 */
public final class ThemeUtils {

    private static final String DEBUG_TAG = ThemeUtils.class.getSimpleName().substring(0, Math.min(23, ThemeUtils.class.getSimpleName().length()));

    /**
     * Private constructor
     */
    private ThemeUtils() {
        // Prevent instantiation since this is a utility class
    }

    /**
     * Returns the color value of the style attribute queried.
     *
     * <p>
     * The attribute will be queried from the theme returned from {@link Context#getTheme()}.
     * </p>
     *
     * @param context the caller's context
     * @param attribResId the attribute id (i.e. R.attr.some_attribute)
     * @param defaultRes the value to return if the attribute does not exist
     * @return the color value for the attribute or defaultValue
     */
    public static int getStyleAttribColorValue(@NonNull final Context context, final int attribResId, final int defaultRes) {
        final TypedValue tv = new TypedValue();
        final boolean found = context.getTheme().resolveAttribute(attribResId, tv, true);
        if (!found) {
            Log.d(DEBUG_TAG, "themed color not found");
            return ContextCompat.getColor(context, defaultRes);
        }
        return tv.data;
    }

    /**
     * Get the actual resource id for an attribute
     * 
     * @param context an Anroid Context
     * @param attr the attribute id
     * @return the resource id or 0 if not found
     */
    public static int getResIdFromAttribute(@NonNull final Context context, final int attr) {
        if (attr == 0) {
            Log.d(DEBUG_TAG, "getResIdFromAttribute attr zero");
            return 0;
        }
        final TypedValue typedvalueattr = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, typedvalueattr, true)) {
            Log.d(DEBUG_TAG, "getResIdFromAttribute attr " + attr + " not found");
            return 0;
        }
        return typedvalueattr.resourceId;
    }

    /**
     * Get a themed dimension from an attribute
     * 
     * @param context an Android COntext
     * @param attr the dimension attribute
     * @return the dimension or 0 if not found
     */
    public static int getDimensionFromAttribute(@NonNull final Context context, final int attr) {
        int[] attrs = new int[] { attr /* index 0 */ };
        TypedArray ta = null;
        try {
            ta = context.getTheme().obtainStyledAttributes(attrs);
            return ta.getDimensionPixelSize(0, 0);
        } catch (Resources.NotFoundException nfe) {
            Log.d(DEBUG_TAG, "getIntFromAttribute attr " + attr + " not found");
            return 0;
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
    }

    /**
     * Get the height of the action bar from the current theme
     * 
     * @param ctx an Android Context
     * @return the height of 0 if not found
     */
    public static int getActionBarHeight(@NonNull Context ctx) {
        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, ctx.getResources().getDisplayMetrics());
        }
        return 0;
    }

    /**
     * Get an inflater for dialogs that has been themed
     * 
     * @param caller Android Context
     * @return a themed Inflater
     */
    public static LayoutInflater getLayoutInflater(@NonNull Context caller) {
        Context context = getThemedContext(caller, R.style.Theme_DialogLight, R.style.Theme_DialogDark);
        return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Get a Context that uses our current Theme
     * 
     * @param caller calling Context
     * @param lightTheme resource id for the light theme
     * @param darkTheme resource id for the dark theme
     * @return a themed Context
     */
    @NonNull
    public static ContextThemeWrapper getThemedContext(@NonNull Context caller, int lightTheme, int darkTheme) {
        // don't use Preferences here as this will create a lot of disk activity and create the default folder
        return new ContextThemeWrapper(caller, Preferences.lightThemeEnabled(caller) ? lightTheme : darkTheme);
    }

    /**
     * Find a Drawable and tint it
     * 
     * @param ctx android Context
     * @param resource the resource id of the drawable
     * @param attr the id of the themeable color attribtue
     * @return the tinted Drawable
     */
    @NonNull
    public static Drawable getTintedDrawable(@NonNull Context ctx, int resource, int attr) {
        Drawable drawable = ContextCompat.getDrawable(ctx, resource);
        return getTintedDrawable(ctx, drawable, attr);
    }

    /**
     * Find a Drawable and tint it
     * 
     * @param ctx android Context
     * @param drawable the drawable
     * @param attr the id of the themeable color attribtue
     * @return the tinted Drawable
     */
    @NonNull
    public static Drawable getTintedDrawable(@NonNull Context ctx, @NonNull Drawable drawable, int attr) {
        ColorStateList tint = ContextCompat.getColorStateList(ctx, ThemeUtils.getResIdFromAttribute(ctx, attr));
        DrawableCompat.setTintList(drawable, tint);
        return drawable;
    }

    /**
     * Set a color for a span
     * 
     * @param context an Android context
     * @param span the span
     * @param colorAttr the color attribute resource id
     * @param colorFallback a fallback color
     */
    public static void setSpanColor(@NonNull Context context, @Nullable SpannableString span, int colorAttr, int colorFallback) {
        if (span != null) {
            span.setSpan(new ForegroundColorSpan(getStyleAttribColorValue(context, colorAttr, colorFallback)), 0, span.length(), 0);
        }
    }

    /**
     * Get a correctly themed AlerDialog.Builder
     * 
     * @param context an Android Context
     * @return a AlerDialog.Builder
     */
    public static AlertDialog.Builder getAlertDialogBuilder(@NonNull Context context) {
        return getAlertDialogBuilder(context, App.getPreferences(context));
    }

    /**
     * Get a correctly themed AlerDialog.Builder
     * 
     * @param context an Android Context
     * @param prefs the current Preferences
     * @return a AlerDialog.Builder
     */
    public static AlertDialog.Builder getAlertDialogBuilder(@NonNull Context context, @NonNull Preferences prefs) {
        return getAlertDialogBuilder(context, prefs.lightThemeEnabled());
    }

    /**
     * Get a correctly themed AlerDialog.Builder
     * 
     * @param context an Android Context
     * @param lightTheme use the lightTheme
     * @return a AlerDialog.Builder
     */
    public static AlertDialog.Builder getAlertDialogBuilder(@NonNull Context context, boolean lightTheme) {
        return new AlertDialog.Builder(new ContextThemeWrapper(context, lightTheme ? R.style.Theme_AlertDialogLight : R.style.Theme_AlertDialog));
    }
}
