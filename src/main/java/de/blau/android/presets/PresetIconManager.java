package de.blau.android.presets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.blau.android.contract.FileExtensions;
import de.blau.android.util.Density;
import de.blau.android.util.Hash;

/**
 * This class manages loading of Preset icons and asset files. Please see Preset.java for an explanation of possible
 * data sources.
 * 
 * @author Jan
 *
 */
public class PresetIconManager {

    private static final String DEBUG_TAG = "PresetIconManager";

    /** context of own application */
    private final Context context;

    /** base path for downloaded icons */
    private final String basePath;

    private static final String ASSET_IMAGE_PREFIX = "images/";

    /** Asset manager for default assets (e.g. icons) stored in a separate APK, if available */
    private final AssetManager externalDefaultAssets;

    /** Asset manager for internal preset assets */
    private final AssetManager internalAssets;

    /** Asset manager for preset assets stored in a separate APK, if available */
    private final AssetManager externalAssets;

    /** the name of an external package containing assets (may be null), used for debug output */
    private final String externalAssetPackage;

    private static final String EXTERNAL_DEFAULT_ASSETS_PACKAGE = "org.openstreetmap.vespucci.defaultpreset";

    /**
     * Creates a new PresetIconManager.
     * 
     * @param context Android context to use for loading data
     * @param basePath Base path for images downloaded for this preset. May be null.
     * @param externalAssetPackage Name of external package to use for loading assets. May be null.
     */
    public PresetIconManager(@NonNull Context context, @Nullable String basePath, @Nullable String externalAssetPackage) {
        this.context = context;
        this.basePath = basePath;
        this.externalAssetPackage = externalAssetPackage;

        AssetManager tmpExternalDefaultAssets = null;
        try {
            Context extCtx = context.createPackageContext(EXTERNAL_DEFAULT_ASSETS_PACKAGE, 0);
            tmpExternalDefaultAssets = extCtx.getAssets();
        } catch (NameNotFoundException e) {
            Log.i(DEBUG_TAG, "External default asset package not installed");
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Exception while loading external default assets", e);
        }
        externalDefaultAssets = tmpExternalDefaultAssets;

        AssetManager tmpExternalDataAssets = null;
        if (externalAssetPackage != null) {
            try {
                Context extCtx = context.createPackageContext(externalAssetPackage, 0);
                tmpExternalDataAssets = extCtx.getAssets();
            } catch (NameNotFoundException e) {
                Log.e(DEBUG_TAG, "External data asset package not found" + externalAssetPackage);
            } catch (Exception e) {
                Log.e(DEBUG_TAG, "Exception while loading external asset package " + externalAssetPackage, e);
            }
        }
        externalAssets = tmpExternalDataAssets;

        internalAssets = context.getAssets();
    }

    /**
     * Gets a drawable for a URL.<br>
     * If the URL is a HTTP(S) URL and a base path is given, it will be checked for the downloaded drawable.<br>
     * Otherwise, the URL will be considered a relative path, checked for ".." to avoid path traversal, and it will be
     * attempted to load the corresponding image from the asset image directory. Handles icons directly in a zipped
     * presets director too.<br>
     * 
     * @param url either a local preset url of the format "presets/xyz.png", or a http/https url
     * @param size icon size in dp
     * @return null if icon file not found or a drawable of [size]x[size] dp.
     */
    @Nullable
    public BitmapDrawable getDrawable(@Nullable String url, int size) {
        if (url == null) {
            return null;
        }
        try (InputStream stream = openStreamForIcon(url)) {
            return bitmapDrawableFromStream(context, size, stream, isSvg(url));
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "Failed to load preset icon " + url, e);
            return null;
        }
    }

    /**
     * Get an InputStream for an icon
     * 
     * @param url an url of file path
     * @return an InputStream
     * @throws IOException if we can't read the icon
     */
    @NonNull
    private InputStream openStreamForIcon(@NonNull String url) throws IOException {
        if (basePath != null && externalAssetPackage == null) {
            if (Preset.isUrl(url)) {
                return new FileInputStream(basePath + "/" + hashPath(url));
            } else if ((isPng(url) || isSvg(url)) && !url.contains("..")) {
                return new FileInputStream(basePath + "/" + url);
            }
        } else if (!url.contains("..")) {
            return openAsset(ASSET_IMAGE_PREFIX + url, true);
        }
        throw new IOException("unknown icon URL type for " + url);
    }

    /**
     * Check if a path ends with .png
     * 
     * @param path the path
     * @return true if it ends with .png
     */
    public boolean isPng(String path) {
        return path.endsWith("." + FileExtensions.PNG);
    }

    /**
     * Check if a path ends with .svg
     * 
     * @param path the path
     * @return true if it ends with .svg
     */
    public static boolean isSvg(@NonNull String path) {
        return path.endsWith("." + FileExtensions.SVG);
    }

    /**
     * Decode a BitmapDrawable from an InputStream
     * 
     * @param context an Android Context
     * @param size size of the Drawable
     * @param stream the InputStream
     * @param isSvg the input stream is from an SVG format icon
     * @return a BitmapDrawable
     */
    @Nullable
    public static BitmapDrawable bitmapDrawableFromStream(@NonNull Context context, int size, @NonNull InputStream stream, boolean isSvg) {
        try {
            Bitmap bitmap = isSvg ? bitmapFromSVG(stream) : BitmapFactory.decodeStream(stream);
            bitmap.setDensity(Bitmap.DENSITY_NONE);
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            int pxsize = Density.dpToPx(context, size);
            drawable.setBounds(0, 0, pxsize, pxsize);
            return drawable;
        } catch (SVGParseException | IOException e) {
            Log.e(DEBUG_TAG, "Unable to parse svg icon " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a Bitmap from a SVG InputStream
     * 
     * @param stream the InputStream
     * @return the Bitmap
     * @throws SVGParseException if SVG parsing failed
     * @throws IOException on IO issues
     */
    @NonNull
    private static Bitmap bitmapFromSVG(@NonNull InputStream stream) throws SVGParseException, IOException {
        Bitmap bitmap;
        SVG svg = SVG.getFromInputStream(stream);
        if (svg.getDocumentWidth() == -1) {
            throw new IOException("Invalid SVG width");
        }
        // Create a canvas to draw onto
        bitmap = Bitmap.createBitmap((int) Math.ceil(svg.getDocumentWidth()), (int) Math.ceil(svg.getDocumentHeight()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        svg.renderToCanvas(canvas);
        return bitmap;
    }

    /**
     * Like {@link #getDrawable(String, int)}, but returns a transparent placeholder instead of null
     *
     * @param url either a local preset url of the format "presets/xyz.png", or a http/https url
     * @param size icon size in dp
     * @return a drawable of [size]x[size] dp
     */
    @NonNull
    public Drawable getDrawableOrPlaceholder(@Nullable String url, int size) {
        Drawable result = getDrawable(url, size);
        if (result != null) {
            return result;
        } else {
            return getPlaceHolder(size);
        }
    }

    /**
     * Return a dummy icon
     * 
     * @param size size of one side of the (square) place holder
     * @return a transparent Drawable
     */
    @NonNull
    public Drawable getPlaceHolder(int size) {
        Drawable placeholder = new ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent));
        int pxsize = Density.dpToPx(context, size);
        placeholder.setBounds(0, 0, pxsize, pxsize);
        return placeholder;
    }

    /**
     * Creates a unique identifier for the given path and adds back the original extension
     * 
     * @param path the path to hash
     * @return a unique, file-name safe value
     */
    @NonNull
    public static String hashPath(@NonNull String path) {
        return Hash.sha256(path).substring(0, 24) + "." + (isSvg(path) ? FileExtensions.SVG : FileExtensions.PNG);
    }

    /**
     * Loads an asset, trying first the preset-specific external asset file (if given), then if allowDefaults is set the
     * default external assets and default internal assets.
     * 
     * @param path the path to try and open
     * @param allowDefault if set to false, loading default assets will be suppressed, returning null if the external
     *            asset file does not contain this asset
     * @return An InputStream returned by {@link AssetManager#open(String)}, or null if no asset could be opened
     */
    @Nullable
    public InputStream openAsset(@NonNull String path, boolean allowDefault) {
        // First try external assets, if available
        try {
            if (externalAssets != null) {
                return externalAssets.open(path);
            }
        } catch (Exception e) {
            // IGNORE
        }

        if (!allowDefault) {
            Log.e(DEBUG_TAG, "Failed to load preset-specific asset " + path + "[externalAssetPackage=" + externalAssetPackage + "]");
            return null;
        }

        // then external default assets
        try {
            if (externalDefaultAssets != null) {
                return externalDefaultAssets.open(path);
            }
        } catch (Exception e) {
            // IGNORE
        }

        // and finally built-in assets
        try {
            return internalAssets.open(path);
        } catch (Exception e) {
            // IGNORE
        }

        // if everything fails
        Log.e(DEBUG_TAG, "Could not load asset " + path + " from any source " + "[externalAssetPackage=" + externalAssetPackage + "]");
        return null;
    }
}
