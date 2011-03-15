// Created by plusminus on 18:23:16 - 25.09.2008
package  de.blau.android.views.util;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.services.util.OpenStreetMapTile;

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
	
	private static String nodeText(Node n) {
		return n.getFirstChild().getNodeValue().trim();
	}
	
	private static String tagValue(Element e, String tagName) {
		try {
			return nodeText(e.getElementsByTagName(tagName).item(0));
		} catch (Exception x) {
			return null;
		}
	}
	
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
			private RectF area; // left<=right, top<=bottom
			/**
			 * Create a coverage area given a CoverageArea element.
			 * @param coverageArea A CoverageArea tile metadata element.
			 */
			public CoverageArea(Element coverageArea) {
				zoomMin = Integer.parseInt(tagValue(coverageArea, "ZoomMin"));
				zoomMax = Integer.parseInt(tagValue(coverageArea, "ZoomMax"));
				float n = Float.parseFloat(tagValue(coverageArea, "NorthLatitude"));
				float s = Float.parseFloat(tagValue(coverageArea, "SouthLatitude"));
				float e = Float.parseFloat(tagValue(coverageArea, "EastLongitude"));
				float w = Float.parseFloat(tagValue(coverageArea, "WestLongitude"));
				area = new RectF(w, s, e, n);
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
		 * Create a new Provider from an ImageryProvider metadata element.
		 * @param imageryProvider An ImageryProvider metadata element.
		 */
		public Provider(Element imageryProvider) {
			attribution = tagValue(imageryProvider, "Attribution");
			NodeList nl = imageryProvider.getElementsByTagName("CoverageArea");
			for (int i = 0; i < nl.getLength(); ++i) {
				coverageAreas.add(new CoverageArea((Element)nl.item(i)));
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
		default:
			imageFilenameExtension = cfgItems[0];
			String metadataUrl = cfgItems[1];
			touUri = (cfgItems.length > 2) ? cfgItems[2] : null;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
				// Get the tile metadata
				InputStream is;
				if (metadataUrl.startsWith("@raw/")) {
					// internal URL
					int resid = r.getIdentifier(metadataUrl.substring(5), "raw", "de.blau.android");
					is = r.openRawResource(resid);
				} else {
					// assume Internet URL
					is = new URL(replaceGeneralParameters(metadataUrl)).openStream();
				}
				Document d = factory.newDocumentBuilder().parse(is);
				NodeList nl;
				
				// See if there's a branding logo
				String brandLogoUri = tagValue(d.getDocumentElement(), "BrandLogoUri");
				brandLogo = null;
				if (brandLogoUri != null) {
					if (brandLogoUri.startsWith("@drawable/")) {
						// internal URL
						int resid = r.getIdentifier(brandLogoUri.substring(10), "drawable", "de.blau.android");
						brandLogo = r.getDrawable(resid);
					} else {
						// assume Internet URL
						InputStream bis = new URL(replaceGeneralParameters(brandLogoUri)).openStream();
						brandLogo = new BitmapDrawable(bis);
					}
				}
				
				// Get the various parameters
				Element metadata = (Element)d.getElementsByTagName("ImageryMetadata").item(0);
				nl = metadata.getChildNodes();
				for (int i = 0; i < nl.getLength(); ++i) {
					Node n = nl.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE) {
						if (n.getNodeName().equals("ImageUrl")) {
							tileUrl = nodeText(n);
						}
						if (n.getNodeName().equals("ImageUrlSubdomains")) {
							NodeList s = ((Element)n).getElementsByTagName("string");
							for (int si = 0; si < s.getLength(); ++si) {
								subdomains.add(nodeText(s.item(si)));
							}
						}
						if (n.getNodeName().equals("ImageWidth")) {
							tileWidth = Integer.parseInt(nodeText(n));
						}
						if (n.getNodeName().equals("ImageHeight")) {
							tileHeight = Integer.parseInt(nodeText(n));
						}
						if (n.getNodeName().equals("ZoomMin")) {
							zoomLevelMin = Integer.parseInt(nodeText(n));
						}
						if (n.getNodeName().equals("ZoomMax")) {
							zoomLevelMax = Integer.parseInt(nodeText(n));
						}
						if (n.getNodeName().equals("ImageryProvider")) {
							// Collect attribution information
							providers.add(new Provider((Element)n));
						}
					}
				}
			} catch (Exception e) {
				Log.e("Vespucci", "Tileserver problem", e);
			}
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
	public Collection<String> getAttributions(int zoom, RectF area) {
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
	
	private static String replaceParameter(String s, String param, String value) {
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
