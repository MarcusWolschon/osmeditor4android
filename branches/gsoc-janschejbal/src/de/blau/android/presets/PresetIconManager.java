package de.blau.android.presets;

import java.io.FileInputStream;
import java.io.InputStream;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import de.blau.android.util.Hash;
import de.blau.android.util.SavingHelper;

public class PresetIconManager {
	/** context of own application */
	private final Context context;
	
	/** base path for downloaded icons */
	private final String basePath;
	
	private final static String ASSET_IMAGE_PREFIX = "images/";
	
	/** Asset manager for default assets (e.g. icons) stored in a separate APK, if available */
	private final AssetManager externalDefaultAssets;
	
	/**  Asset manager for internal preset assets */
	private final AssetManager internalAssets;
	
	/**  Asset manager for preset assets stored in a separate APK, if available */
	private final AssetManager externalAssets;

	/** the name of an external package containing assets (may be null), used for debug output */
	private final String externalAssetPackage;
	
	private final static String EXTERNAL_DEFAULT_ASSETS_PACKAGE = "org.openstreetmap.vespucci.defaultpreset";
	
	/**
	 * Creates a new PresetIconManager.
	 * @param context Vespucci context to use for loading data
	 * @param basePath Base path for images downloaded for this preset. May be null.
	 * @param externalAssetPackage Name of external package to use for loading assets. May be null.
	 */
	public PresetIconManager(Context context, String basePath, String externalAssetPackage) {
		this.context = context;
		this.basePath = basePath;
		this.externalAssetPackage = externalAssetPackage;
		
		AssetManager tmpExternalDefaultAssets = null;
		try {
			Context extCtx = context.createPackageContext(EXTERNAL_DEFAULT_ASSETS_PACKAGE, 0);
			tmpExternalDefaultAssets = extCtx.getAssets();
		} catch (NameNotFoundException e) {
			Log.i("PresetIconManager", "External default asset package not installed");
		} catch (Exception e) {
			Log.e("PresetIconManager", "Exception while loading external default assets", e);
		}
		this.externalDefaultAssets = tmpExternalDefaultAssets;
		
		AssetManager tmpExternalDataAssets = null;
		if (externalAssetPackage != null) {
			try {
				Context extCtx = context.createPackageContext(externalAssetPackage, 0);
				tmpExternalDataAssets = extCtx.getAssets();
			} catch (NameNotFoundException e) {
				Log.e("PresetIconManager", "External data asset package not found" + externalAssetPackage);
			} catch (Exception e) {
				Log.e("PresetIconManager", "Exception while loading external asset package " + externalAssetPackage, e);
			}
		}
		this.externalAssets = tmpExternalDataAssets;
		
		this.internalAssets = context.getAssets(); 
	}
	
	/**
	 * Gets a drawable for a URL.<br>
	 * If the URL is a HTTP(S) URL and a base path is given, it will be checked for the downloaded drawable.<br>
	 * Otherwise, the URL will be considered a relative path, checked for ".." to avoid path traversal,
	 * and it will be attempted to load the corresponding image from the asset image directory.<br>
	 * @param url either a local preset url of the format "presets/xyz.png", or a http/https url
	 * @param size icon size in dp
	 * @return null if icon file not found or a drawable of [size]x[size] dp.
	 */
	public BitmapDrawable getDrawable(String url, int size) {
		if (url == null) return null;
		
		InputStream pngStream = null;
		try {
			if (basePath != null && (url.startsWith("http://") || url.startsWith("https://"))) {
				pngStream = new FileInputStream(basePath+"/"+hash(url)+".png");
			} else if (!url.contains("..")) {
				pngStream = openAsset(ASSET_IMAGE_PREFIX+url, true);
			} else {
				Log.e("PresetIconManager", "unknown icon URL type for " + url);
			}
			
			if (pngStream == null) return null;
			
			BitmapDrawable drawable = new BitmapDrawable(context.getResources(), pngStream); // resources used only for density
			drawable.getBitmap().setDensity(Bitmap.DENSITY_NONE);
			int pxsize = dpToPx(size);
			drawable.setBounds(0, 0, pxsize, pxsize);
			return drawable;
		} catch (Exception e) {
			Log.e("PresetIconManager", "Failed to load preset icon " + url, e);
			return null;
		} finally {
			SavingHelper.close(pngStream);
		}
	}
	
	/**
	 * Like {@link #getDrawable(String, int)}, but returns a transparent placeholder
	 * instead of null
	 */
	public Drawable getDrawableOrPlaceholder(String url, int size) {
		Drawable result = getDrawable(url, size);
		if (result != null) {
			return result;
		} else {
			Drawable placeholder = new ColorDrawable(android.R.color.transparent);
			int pxsize = dpToPx(size);
			placeholder.setBounds(0,0, pxsize, pxsize);
			return placeholder;
		}
	}

	/**
	 * Converts a size in dp to pixels
	 * @param dp size in display point
	 * @return size in pixels (for the current display metrics)
	 */
	private int dpToPx(int dp) {
		return Math.round(dp * context.getResources().getDisplayMetrics().density);
	}
	
	/**
	 * Creates a unique identifier for the given value
	 * @param value the value to hash
	 * @return a unique, file-name safe identifier
	 */
	public static String hash(String value) {
		return Hash.sha256(value).substring(0, 24);
	}
	
	/**
	 * Loads an asset, trying first the preset-specific external asset file (if given),
	 * then if allowDefaults is set the default external assets and default internal assets.
	 * @param path
	 * @param allowDefault if set to false, loading default assets will be suppressed,
	 *        returning null if the external asset file does not contain this asset
	 * @return An InputStream returned by {@link AssetManager#open(String)}, or null if no asset could be opened
	 */
	public InputStream openAsset(String path, boolean allowDefault) {
		// First try external assets, if available
		try {
			if (externalAssets != null) return externalAssets.open(path);
		} catch (Exception e) {} // ignore
		
		if (!allowDefault) {
			Log.e("PresetIconManager", "Failed to load preset-specific asset " + path +
					"[externalAssetPackage="+externalAssetPackage+"]");
			return null;
		}
		
		// then external default assets
		try {
			if (externalDefaultAssets != null) return externalDefaultAssets.open(path);
		} catch (Exception e) {} // ignore
		
		// and finally built-in assets
		try {
			return internalAssets.open(path);
		} catch (Exception e) {} // ignore
		
		// if everything fails
		Log.e("PresetIconManager", "Could not load asset " + path + " from any source "+
				"[externalAssetPackage="+externalAssetPackage+"]");
		return null;
	}
	
}
