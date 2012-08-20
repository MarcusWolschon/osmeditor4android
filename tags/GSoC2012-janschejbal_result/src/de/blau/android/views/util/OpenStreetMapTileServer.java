// Created by plusminus on 18:23:16 - 25.09.2008
package  de.blau.android.views.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.util.UglyHackForStrictMode;

/**
 * The OpenStreetMapRendererInfo stores information about available tile servers.
 * <br/>
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.androin
 * OSMEditor. 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 *
 */
public class OpenStreetMapTileServer {
	
	/** A tile layer provide has some attribution text, and one or more coverage areas.
	 * @author Andrew Gregory
	 */
	private static class Provider {
		/** A coverage area is a range of zooms and a bounding box.
		 * @author Andrew Gregory
		 */
		private static class CoverageArea {
			/** Zoom and area of this coverage area. */
			private int zoomMin;
			private int zoomMax;
			private RectF area = new RectF(); // left<=right, top<=bottom
			/**
			 * Create a coverage area given XML data.
			 * @param parser The XML parser.
			 * @throws XmlPullParserException If there was a problem parsing the XML.
			 * @throws NumberFormatException If any of the numbers couldn't be parsed.
			 */
			public CoverageArea(XmlPullParser parser) throws IOException, NumberFormatException, XmlPullParserException {
				int eventType;
				while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
					String tagName = parser.getName();
					if (eventType == XmlPullParser.END_TAG) {
						if (tagName.equals("CoverageArea")) {
							break;
						}
					}
					if (eventType == XmlPullParser.START_TAG) {
						if (tagName.equals("ZoomMin") && parser.next() == XmlPullParser.TEXT) {
							zoomMin = Integer.parseInt(parser.getText().trim());
						}
						if (tagName.equals("ZoomMax") && parser.next() == XmlPullParser.TEXT) {
							zoomMax = Integer.parseInt(parser.getText().trim());
						}
						// NOTE: North->bottom and South->top is apparently reversed, but
						// that is what is required for RectF.intersects() to work.
						if (tagName.equals("NorthLatitude") && parser.next() == XmlPullParser.TEXT) {
							area.bottom = Float.parseFloat(parser.getText().trim());
						}
						if (tagName.equals("SouthLatitude") && parser.next() == XmlPullParser.TEXT) {
							area.top = Float.parseFloat(parser.getText().trim());
						}
						if (tagName.equals("EastLongitude") && parser.next() == XmlPullParser.TEXT) {
							area.right = Float.parseFloat(parser.getText().trim());
						}
						if (tagName.equals("WestLongitude") && parser.next() == XmlPullParser.TEXT) {
							area.left = Float.parseFloat(parser.getText().trim());
						}
					}
				}
			}
			/**
			 * Test if the given zoom and area is covered by this coverage area.
			 * @param zoom The zoom level to test.
			 * @param area The map area to test.
			 * @return true if the given zoom and area are covered by this
			 * coverage area.
			 */
			public boolean covers(int zoom, RectF area) {
				return (zoom >= zoomMin && zoom <= zoomMax && RectF.intersects(this.area, area));
			}
		}
		/** Attribution for this provider. */
		private String attribution;
		/** Coverage area provided by this provider. */
		private Collection<CoverageArea> coverageAreas = new ArrayList<CoverageArea>();
		/**
		 * Create a new Provider from XML data.
		 * @param parser The XML parser.
		 * @throws IOException If there was a problem parsing the XML.
		 * @throws XmlPullParserException If there was a problem parsing the XML.
		 */
		public Provider(XmlPullParser parser) throws XmlPullParserException, IOException {
			int eventType;
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				String tagName = parser.getName();
				if (eventType == XmlPullParser.END_TAG) {
					if (tagName.equals("ImageryProvider")) {
						break;
					}
				}
				if (eventType == XmlPullParser.START_TAG) {
					if (tagName.equals("Attribution") && parser.next() == XmlPullParser.TEXT) {
						attribution = parser.getText().trim();
					}
					if (tagName.equals("CoverageArea")) {
						try {
							coverageAreas.add(new CoverageArea(parser));
						} catch (Exception x) {
							// do nothing
						}
					}
				}
			}
		}
		/**
		 * Get the attribution for this provider.
		 * @return The attribution for this provider.
		 */
		public String getAttribution() {
			return attribution;
		}
		/**
		 * Test if the provider covers the given zoom and area.
		 * @param zoom Zoom level to test.
		 * @param area Map area to test.
		 * @return true if the provider has coverage of the given zoom and area.
		 */
		public boolean covers(int zoom, RectF area) {
			for (CoverageArea a : coverageAreas) {
				if (a.covers(zoom, area)) {
					return true;
				}
			}
			return false;
		}
	}
	
	private static OpenStreetMapTileServer cached = null;
	
	private Resources r;
	
	// ===========================================================
	// Fields
	// ===========================================================
	
	private String id, tileUrl, imageFilenameExtension, touUri;
	private int zoomLevelMin, zoomLevelMax, tileWidth, tileHeight;
	private Drawable brandLogo;
	private Queue<String> subdomains = new LinkedList<String>();
	private Collection<Provider> providers = new ArrayList<Provider>();
	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	private OpenStreetMapTileServer(final Resources r, final String id, final String config) {
		String[] cfgItems = config.split("\\s+");
		this.r = r;
		this.id = id;
		switch (cfgItems.length) {
		case 5:
			tileUrl = cfgItems[0];
			imageFilenameExtension = cfgItems[1];
			zoomLevelMin = Integer.parseInt(cfgItems[2]);
			zoomLevelMax = Integer.parseInt(cfgItems[3]);
			int zoom = Integer.parseInt(cfgItems[4]);
			tileWidth = 1 << zoom;
			tileHeight = 1 << zoom;
			brandLogo = null;
			break;
		case 2:
		case 3:
			imageFilenameExtension = cfgItems[0];
			String metadataUrl = cfgItems[1];
			touUri = (cfgItems.length > 2) ? cfgItems[2] : null;
			
			try {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser parser = factory.newPullParser();
				// Get the tile metadata
				InputStream is;
				if (metadataUrl.startsWith("@raw/")) {
					// internal URL
					int resid = r.getIdentifier(metadataUrl.substring(5), "raw", "de.blau.android");
					is = r.openRawResource(resid);
				} else {
					// assume Internet URL
					// TODO network IO must not be done on main thread - remove ugly hack after fixing (do not forget the endLegacySection below)
					UglyHackForStrictMode.beginLegacySection();
					URLConnection conn = new URL(replaceGeneralParameters(metadataUrl)).openConnection();
					conn.setRequestProperty("User-Agent", Application.userAgent);
					is = conn.getInputStream();
				}
				parser.setInput(is, null);
				
				int eventType;
				while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
					String tagName = parser.getName();
					if (eventType == XmlPullParser.START_TAG) {
						if (tagName.equals("BrandLogoUri") && parser.next() == XmlPullParser.TEXT) {
							String brandLogoUri = parser.getText().trim();
							if (brandLogoUri.startsWith("@drawable/")) {
								// internal URL
								int resid = r.getIdentifier(brandLogoUri.substring(10), "drawable", "de.blau.android");
								brandLogo = r.getDrawable(resid);
							} else {
								// assume Internet URL
								URLConnection conn = new URL(replaceGeneralParameters(brandLogoUri)).openConnection();
								conn.setRequestProperty("User-Agent", Application.userAgent);
								InputStream bis = conn.getInputStream();
								brandLogo = new BitmapDrawable(bis);
							}
						}
						if (tagName.equals("ImageUrl") && parser.next() == XmlPullParser.TEXT) {
							tileUrl = parser.getText().trim();
						}
						if (tagName.equals("string") && parser.next() == XmlPullParser.TEXT) {
							subdomains.add(parser.getText().trim());
						}
						if (tagName.equals("ImageWidth") && parser.next() == XmlPullParser.TEXT) {
							tileWidth = Integer.parseInt(parser.getText().trim());
						}
						if (tagName.equals("ImageHeight") && parser.next() == XmlPullParser.TEXT) {
							tileHeight = Integer.parseInt(parser.getText().trim());
						}
						if (tagName.equals("ZoomMin") && parser.next() == XmlPullParser.TEXT) {
							zoomLevelMin = Integer.parseInt(parser.getText().trim());
						}
						if (tagName.equals("ZoomMax") && parser.next() == XmlPullParser.TEXT) {
							zoomLevelMax = Integer.parseInt(parser.getText().trim());
						}
						if (tagName.equals("ImageryProvider")) {
							try {
								providers.add(new Provider(parser));
							} catch (IOException e) {
								// if the provider can't be parsed, we can't do
								// much about it
								Log.e("Vespucci", "ImageryProvider problem", e);
							} catch (XmlPullParserException e) {
								// if the provider can't be parsed, we can't do
								// much about it
								Log.e("Vespucci", "ImageryProvider problem", e);
							}
						}
					}
				}
			} catch (IOException e) {
				Log.e("Vespucci", "Tileserver problem", e);
			} catch (XmlPullParserException e) {
				Log.e("Vespucci", "Tileserver problem", e);
			}
			UglyHackForStrictMode.endLegacySection(); // TODO remove ugly hack (see above)
			break;
		default:
			tileUrl = "";
			imageFilenameExtension = "";
			zoomLevelMin = 0;
			zoomLevelMax = 0;
			tileWidth = 256;
			tileHeight = 256;
			brandLogo = null;
			break;
		}
	}
	
	/**
	 * Get the default tile layer.
	 * @param r Application resources.
	 * @return The default tile layer.
	 */
	public static OpenStreetMapTileServer getDefault(final Resources r) {
		// ask for an invalid renderer, so we'll get the fallback default
		return get(r, "");
	}
	
	/**
	 * Get the tile server information for a specified tile server id. If the given
	 * id cannot be found, a default renderer is selected.
	 * @param r The application resources.
	 * @param id The internal id of the tile layer, eg "MAPNIK"
	 * @return
	 */
	public static OpenStreetMapTileServer get(final Resources r, final String id) {
		if (cached == null || !cached.id.equals(id)) {
			String ids[] = r.getStringArray(R.array.renderer_ids);
			String cfgs[] = r.getStringArray(R.array.renderer_configs);
			cached = null;
			for (int i = 0; i < ids.length; ++i) {
				if (ids[i].equals(id) ||
						// check for default renderer MAPNIK here
						(cached == null && ids[i].equals("MAPNIK"))) {
					cached = new OpenStreetMapTileServer(r, ids[i], cfgs[i]);
				}
			}
		}
		return cached;
	}
	
	// ===========================================================
	// Methods
	// ===========================================================
	
	/**
	 * Get the Tile layer ID.
	 * @return Tile layer ID.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Get the tile width.
	 * @return The tile width in pixels.
	 */
	public int getTileWidth() {
		return tileWidth;
	}
	
	/**
	 * Get the tile height.
	 * @return The tile height in pixels.
	 */
	public int getTileHeight() {
		return tileHeight;
	}
	
	/**
	 * Get the minimum zoom level these tiles are available at.
	 * @return Minimum zoom level for which the tile layer is available.
	 */
	public int getMinZoomLevel() {
		return zoomLevelMin;
	}
	
	/**
	 * Get the maximum zoom level these tiles are available at.
	 * @return Maximum zoom level for which the tile layer is available.
	 */
	public int getMaxZoomLevel() {
		return zoomLevelMax;
	}
	
	/**
	 * Get the filename extensions that applies to the tile images.
	 * @return Image filename extension, eg ".png".
	 */
	public String getImageExtension() {
		return imageFilenameExtension;
	}
	
	/**
	 * Get the branding logo for the tile layer.
	 * @return The branding logo, or null if there is none.
	 */
	public Drawable getBrandLogo() {
		return brandLogo;
	}
	
	/**
	 * Get the attributions that apply to the given map display.
	 * @param zoom Zoom level of the display.
	 * @param area Displayed area to get the attributions of.
	 *  NOTE: left<=right and top<=bottom,
	 *  i.e. left=west,right=east,top=south,bottom=north
	 * @return Collections of attributions that apply to the specified area and zoom.
	 */
	public Collection<String> getAttributions(final int zoom, final RectF area) {
		Collection<String> ret = new ArrayList<String>();
		for (Provider p : providers) {
			if (p.covers(zoom, area)) {
				ret.add(p.getAttribution());
			}
		}
		return ret;
	}
	
	/**
	 * Get the End User Terms Of Use URI.
	 * @return The End User Terms Of Use URI.
	 */
	public String getTouUri() {
		return touUri;
	}
	
	/**
	 * Get all the available tile layer IDs.
	 * @param r Application resources.
	 * @return All available tile layer IDs.
	 */
	public static String[] getIds(final Resources r) {
		return r.getStringArray(R.array.renderer_ids);
	}
	
	private static String replaceParameter(final String s, final String param, final String value) {
		String result = s;
		// replace "$param"
		result = result.replaceFirst("\\$" + param, value);
		// replace "{param}"
		result = result.replaceFirst("\\{" + param + "\\}", value);
		return result;
	}
	
	private String replaceGeneralParameters(final String s) {
		final Locale l = r.getConfiguration().locale;
		String result = s;
		result = replaceParameter(result, "culture", l.getLanguage().toLowerCase() + "-" + l.getCountry().toLowerCase());
		// Bing API key assigned to Andrew Gregory
		result = replaceParameter(result, "bingapikey", "AtCQFkJNgUBEVk9qiNMBCNExDMVCiz5Hgvn20tpG3AfONcQUcumDChHDnhhNs0YA");
		// Cloudmade API key assigned to Andrew Gregory
		result = replaceParameter(result, "cloudmadeapikey", "9dce5357e52940298136dcf75b8e056b");
		return result;
	}
	
	/**
	 * Get the URL that can be used to obtain the image of the given tile.
	 * @param aTile The tile to get the URL for.
	 * @return URL of the given tile.
	 */
	public String getTileURLString(final OpenStreetMapTile aTile) {
		String result = tileUrl;
		
		// Position-sensitive replacements - Potlatch !/!/! syntax
		result = result.replaceFirst("\\!", Integer.toString(aTile.zoomLevel));
		result = result.replaceFirst("\\!", Integer.toString(aTile.x));
		result = result.replaceFirst("\\!", Integer.toString(aTile.y));
		
		// Named replacements
		result = replaceParameter(result, "zoom", Integer.toString(aTile.zoomLevel));
		result = replaceParameter(result, "z", Integer.toString(aTile.zoomLevel));
		result = replaceParameter(result, "x", Integer.toString(aTile.x));
		result = replaceParameter(result, "y", Integer.toString(aTile.y));
		result = replaceParameter(result, "quadkey", quadTree(aTile));
		
		// Subdomains
		if (!subdomains.isEmpty()) {
			// Rotate through the list of subdomains
			String subdomain = subdomains.remove();
			result = replaceParameter(result, "subdomain", subdomain);
			subdomains.add(subdomain);
		}
		
		return replaceGeneralParameters(result);
	}
	
	/**
	 * Converts TMS tile coordinates to QuadTree
	 * @param aTile The tile coordinates to convert
	 * @return The QuadTree as String.
	 */
	private static String quadTree(final OpenStreetMapTile aTile) {
		StringBuilder quadKey = new StringBuilder();
		for (int i = aTile.zoomLevel; i > 0; i--) {
			int digit = 0;
			int mask = 1 << (i - 1);
			if ((aTile.x & mask) != 0)
				digit += 1;
			if ((aTile.y & mask) != 0)
				digit += 2;
			quadKey.append(digit);
		}
		return quadKey.toString();
	}
	
}
