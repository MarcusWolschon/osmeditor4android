// Created by plusminus on 18:23:16 - 25.09.2008
package  de.blau.android.views.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.OpenStreetMapTile;
import de.blau.android.util.Density;
import de.blau.android.util.Offset;
import de.blau.android.util.jsonreader.JsonReader;

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
			private BoundingBox bbox = null;
			private RectF area = new RectF(); // left<=right, top<=bottom
			/**
			 * Create a coverage area given XML data.
			 * @param parser The XML parser.
			 * @throws XmlPullParserException If there was a problem parsing the XML.
			 * @throws NumberFormatException If any of the numbers couldn't be parsed.
			 */
			public CoverageArea(XmlPullParser parser) throws IOException, NumberFormatException, XmlPullParserException {
				int eventType;
				double bottom = 0.0d;
				double top = 0.0d;
				double left = 0.0d;
				double right = 0.0d;
				
				
				while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
					String tagName = parser.getName();
					if (eventType == XmlPullParser.END_TAG) {
						if ("CoverageArea".equals(tagName)) {
							break;
						}
					}
					if (eventType == XmlPullParser.START_TAG) {
						if ("ZoomMin".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
							zoomMin = Integer.parseInt(parser.getText().trim());
						}
						if ("ZoomMax".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
							zoomMax = Integer.parseInt(parser.getText().trim());
						}
						if ("NorthLatitude".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
							top = Double.parseDouble(parser.getText().trim());
						}
						if ("SouthLatitude".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
							bottom = Double.parseDouble(parser.getText().trim());
						}
						if ("EastLongitude".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
							right = Double.parseDouble(parser.getText().trim());
						}
						if ("WestLongitude".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
							left = Double.parseDouble(parser.getText().trim());
						}
					}
				}
				bbox = new BoundingBox(left,bottom,right,top);
			}
			
			public CoverageArea(int zoomMin, int zoomMax, BoundingBox bbox) {
				this.zoomMin = zoomMin;
				this.zoomMax = zoomMax;
				this.bbox = bbox;
			}
			
			/**
			 * Test if the given zoom and area is covered by this coverage area.
			 * @param zoom The zoom level to test.
			 * @param area The map area to test.
			 * @return true if the given zoom and area are covered by this
			 * coverage area.
			 */
			public boolean covers(int zoom, BoundingBox area) {
				return (zoom >= zoomMin && zoom <= zoomMax && (this.bbox == null || this.bbox.intersects(area)));
			}
			
			public boolean covers(BoundingBox area) {
				return this.bbox == null || this.bbox.intersects(area);
			}
			
			public boolean covers(double lon, double lat) {
				return this.bbox == null || this.bbox.isIn((int)(lat*1E7d), (int)(lon*1E7d));
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
					if ("ImageryProvider".equals(tagName)) {
						break;
					}
				}
				if (eventType == XmlPullParser.START_TAG) {
					if ("Attribution".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						attribution = parser.getText().trim();
					}
					if ("CoverageArea".equals(tagName)) {
						try {
							coverageAreas.add(new CoverageArea(parser));
						} catch (Exception x) {
							// do nothing
						}
					}
				}
			}
		}
		
		
		public Provider() {
		}
		
		public void addCoverageArea(CoverageArea ca) {
			coverageAreas.add(ca);
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
		public boolean covers(int zoom, BoundingBox area) {
			for (CoverageArea a : coverageAreas) {
				if (a.covers(zoom, area)) {
					return true;
				}
			}
			return false;
		}
		
		public boolean covers(BoundingBox area) {
			if (coverageAreas.size() == 0)
				return true;
			for (CoverageArea a : coverageAreas) {
				if (a.covers(area)) {
					return true;
				}
			}
			return false;
		}
		
		public CoverageArea getCoverageArea(double lon, double lat) {
			if (coverageAreas.size() == 0)
				return null;
			CoverageArea result = null;
			for (CoverageArea a : coverageAreas) {
				if (a.covers(lon,lat)) {
					if (result == null)
						result = a;
					else {
						if (a.zoomMax > result.zoomMax)
							result = a;
					}
				}
				Log.d("OpenStreetMapTileServer","maxZoom " + a.zoomMax);
			}
			return result;
		}
	}
	
	private static OpenStreetMapTileServer cachedBackground = null;
	private static OpenStreetMapTileServer cachedOverlay = null;
	
	private Resources r;
	
	// ===========================================================
	// Fields
	// ===========================================================

	
	private boolean metadataLoaded;
	private String id, name, tileUrl, imageFilenameExtension, touUri;
	private boolean overlay, defaultLayer;
	private int zoomLevelMin, zoomLevelMax, tileWidth, tileHeight;
	private Drawable brandLogo;
	private Queue<String> subdomains = new LinkedList<String>();
	private int defaultAlpha;
	private Collection<Provider> providers = new ArrayList<Provider>();
	private Offset[] offsets;
	
	private static HashMap<String,OpenStreetMapTileServer> backgroundServerList =new HashMap<String,OpenStreetMapTileServer>();
	private static HashMap<String,OpenStreetMapTileServer> overlayServerList = new HashMap<String,OpenStreetMapTileServer>();
	private static boolean ready = false;
	private static Context myCtx;

	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	private void loadInfo(String metadataUrl) {
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
				URLConnection conn = new URL(replaceGeneralParameters(metadataUrl)).openConnection();
				conn.setRequestProperty("User-Agent", Application.userAgent);
				is = conn.getInputStream();
			}
			parser.setInput(is, null);
			
			int eventType;
			while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
				String tagName = parser.getName();
				if (eventType == XmlPullParser.START_TAG) {
					if ("BrandLogoUri".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
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
							Bitmap brandLogoBitmap = BitmapFactory.decodeStream(bis);
							// scale according to density
							if (brandLogoBitmap != null)
								brandLogo = new BitmapDrawable(r,Bitmap.createScaledBitmap(brandLogoBitmap, Density.dpToPx(myCtx,brandLogoBitmap.getWidth()), Density.dpToPx(myCtx,brandLogoBitmap.getHeight()), false)); 
						}
					}
					if ("ImageUrl".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						tileUrl = parser.getText().trim();
						//Log.d("OpenStreetMapTileServer","loadInfo tileUrl " + tileUrl);
						int extPos = tileUrl.lastIndexOf(".jpeg"); //TODO fix this awlful hack
						if (extPos >= 0)
							imageFilenameExtension = ".jpg";
						// extract switch values
						final String SWITCH_START = "{switch:";
						int switchPos = tileUrl.indexOf(SWITCH_START);
						if (switchPos >= 0) {
							int switchEnd = tileUrl.indexOf("}",switchPos);
							if (switchEnd >= 0) {
								String switchValues = tileUrl.substring(switchPos+SWITCH_START.length(), switchEnd);
								for (String subdomain : switchValues.split(",")) {
									subdomains.add(subdomain);
								}
								StringBuffer t = new StringBuffer(tileUrl);
								tileUrl = t.replace(switchPos, switchEnd + 1, "{subdomain}").toString();
							}
						}
					}
					if ("string".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						subdomains.add(parser.getText().trim());
					}
					if ("ImageWidth".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						tileWidth = Integer.parseInt(parser.getText().trim());
					}
					if ("ImageHeight".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						tileHeight = Integer.parseInt(parser.getText().trim());
					}
					if ("ZoomMin".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						zoomLevelMin = Integer.parseInt(parser.getText().trim());
					}
					if ("ZoomMax".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
						zoomLevelMax = Integer.parseInt(parser.getText().trim());
					}
					if ("ImageryProvider".equals(tagName)) {
						try {
							providers.add(new Provider(parser));
						} catch (IOException e) {
							// if the provider can't be parsed, we can't do
							// much about it
							Log.e("OpenStreetMapTileServer", "ImageryProvider problem", e);
						} catch (XmlPullParserException e) {
							// if the provider can't be parsed, we can't do
							// much about it
							Log.e("OpenStreetMapTileServer", "ImageryProvider problem", e);
						}
					}
				}
			}
			metadataLoaded = true;
			// once we've got here, a selected layer that was previously non-available might now be available ... reset map preferences
			if (myCtx == Application.mainActivity) { // don't do this in the service
				Application.mainActivity.getMap().setPrefs(new Preferences(myCtx));
			}
		} catch (IOException e) {
			Log.d("OpenStreetMapTileServer", "Tileserver problem (IOException) metadata URL " + metadataUrl, e);
		} catch (XmlPullParserException e) {
			Log.e("OpenStreetMapTileServer", "Tileserver problem (XmlPullParserException) metadata URL " + metadataUrl, e);
		}
	}
	
	/**
	 * 
	 * @param r
	 * @param id
	 * @param name
	 * @param url
	 * @param type
	 * @param overlay
	 * @param zoomLevelMin
	 * @param zoomLevelMax
	 * @param tileWidth
	 * @param tileHeight
	 * @param async			run loadInfo in a AsyncTask needed for main process
	 */
	private OpenStreetMapTileServer(final Resources r, final String id, final String name, final String url, final String type, 
			final boolean overlay, final boolean defaultLayer, final Provider provider, final String termsOfUseUrl,
			final int zoomLevelMin, final int zoomLevelMax, final int tileWidth, final int  tileHeight, boolean async) {
		this.r = r;
		this.id = id;
		this.name = name;
		tileUrl = url;
		this.overlay = overlay;
		this.defaultLayer = defaultLayer;
		this.zoomLevelMin = zoomLevelMin; 
		this.zoomLevelMax = zoomLevelMax;
		this.tileWidth = tileWidth; 
		this.tileHeight = tileHeight;
		this.touUri = termsOfUseUrl;
		this.offsets = new Offset[zoomLevelMax-zoomLevelMin+1];
		if (provider != null)
			providers.add(provider);
		
		metadataLoaded = true;
		
		if (name == null) {
			// parse error or other fatal issue
			this.name = "INVALID";
		}
		if (this.id == null) {
			// generate id from name
			this.id = name.replaceAll("[\\W\\_]","");
		}
		// 
		this.id = this.id.toUpperCase();

		//TODO think of a elegant way to do this
		if (type.equals("bing")) { // hopelessly hardwired
			if (backgroundServerList.containsKey(this.id))
				return; // awful hack to avoid calling loadInfo more than once in this process 
			Log.d("OpenStreetMapTileServer","bing url " + tileUrl);
			metadataLoaded = false;

			if (async) {
				new AsyncTask<String, Void, Void>() {
					@Override
					protected Void doInBackground(String... params) {
						loadInfo(params[0]);
						return null;
					}
				}.execute(tileUrl);
			} else 
				loadInfo(tileUrl);
			return;
		} else if (type.equals("scanex")) { // hopelessly hardwired
			tileUrl = "http://irs.gis-lab.info/?layers="+tileUrl.toLowerCase()+"&request=GetTile&z={zoom}&x={x}&y={y}";
			imageFilenameExtension = ".jpg";
			return;
		}
		
		int extPos = tileUrl.lastIndexOf('.');
		if (extPos >= 0)
			imageFilenameExtension = tileUrl.substring(extPos);
		// extract switch values
		final String SWITCH_START = "{switch:";
		int switchPos = tileUrl.indexOf(SWITCH_START);
		if (switchPos >= 0) {
			int switchEnd = tileUrl.indexOf("}",switchPos);
			if (switchEnd >= 0) {
				String switchValues = tileUrl.substring(switchPos+SWITCH_START.length(), switchEnd);
				for (String subdomain : switchValues.split(",")) {
					subdomains.add(subdomain);
				}
				StringBuffer t = new StringBuffer(tileUrl);
				tileUrl = t.replace(switchPos, switchEnd + 1, "{subdomain}").toString();
			}
		}
	}
	
	
	
	/**
	 * Get the default tile layer.
	 * @param r Application resources.
	 * @return The default tile layer.
	 */
	public static OpenStreetMapTileServer getDefault(final Resources r, final boolean async) {
		// ask for an invalid renderer, so we'll get the fallback default
		return get(Application.mainActivity, "", async);
	}
	
	/**
	 * Get the tile server information for a specified tile server id. If the given
	 * id cannot be found, a default renderer is selected.
	 * @param r The application resources.
	 * @param id The internal id of the tile layer, eg "MAPNIK"
	 * @return
	 */

	
	public synchronized static OpenStreetMapTileServer get(final Context ctx, final String id, final boolean async) {	
		Resources r = ctx.getResources();
		myCtx = ctx;
		
		synchronized (backgroundServerList) {
			if (!ready) {
				Log.d("OpenStreetMapTileServer","Parsing configuration files");

				AssetManager assetManager = ctx.getAssets();
				
				String[] imageryFiles = {"imagery_vespucci.json","imagery.json"}; // entries in earlier files will not be overwritten by later ones
				for (String fn:imageryFiles) {
					try {
						InputStream is = assetManager.open(fn);
						JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
						try {
							
							try {
								reader.beginArray();
								while (reader.hasNext()) {
									OpenStreetMapTileServer osmts = readServer(reader, r, async);
									if (osmts != null) {
										if (osmts.overlay && !overlayServerList.containsKey(osmts.id)) {
											// Log.d("OpenStreetMapTileServer","Adding overlay " + osmts.overlay + " " + osmts.toString());
											overlayServerList.put(osmts.id,osmts);
										}
										else if (!backgroundServerList.containsKey(osmts.id)){
											// Log.d("OpenStreetMapTileServer","Adding background " + osmts.overlay + " " + osmts.toString());
											backgroundServerList.put(osmts.id,osmts);
										}
									}
								}
								reader.endArray();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} 
						}
						finally {
						       reader.close();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				ready = true;
			}
		}
		// Log.d("OpenStreetMapTileServer", "id " + id + " list size " + backgroundServerList.size() + " " + overlayServerList.size() + " cached " + (cachedBackground == null?"null":cachedBackground.id));
		
		if (cachedBackground != null && cachedBackground.id.equals(id))
			return cachedBackground;
		else if (cachedOverlay != null && cachedOverlay.id.equals(id))
			return cachedOverlay; 
		else { 
			OpenStreetMapTileServer tempOSMTS =  overlayServerList.get(id);
			boolean overlay = tempOSMTS != null;
			
			if (overlay) {
				if (cachedOverlay == null || !cachedOverlay.id.equals(id)) {
					cachedOverlay = overlayServerList.get(id);
					if (cachedOverlay == null || !cachedOverlay.metadataLoaded)
						cachedOverlay = overlayServerList.get("NONE");
					Log.d("OpenStreetMapTileServer", "cachedOverlay " + (cachedOverlay == null?"null":cachedOverlay.id));
				}
				return cachedOverlay;
			} else { 
				if (cachedBackground == null || !cachedBackground.id.equals(id)) {
					cachedBackground = backgroundServerList.get(id);
					if (cachedBackground == null || !cachedBackground.metadataLoaded)
						cachedBackground = backgroundServerList.get("MAPNIK");
					Log.d("OpenStreetMapTileServer", "requested id " + id + " cached " + (cachedBackground == null?"null":cachedBackground.id));
				}
				return cachedBackground;
			}
		} 
	}
	
	private static OpenStreetMapTileServer readServer(JsonReader reader, final Resources r, boolean async) {
		String id = null;
		String name = null;
		String url = null;
		String type = null;
		boolean overlay = false;
		boolean defaultLayer = false;
		Provider.CoverageArea extent = null;
		Provider provider = null;
		String termsOfUseUrl = null;
		try {
			reader.beginObject();
			while (reader.hasNext()) {
				String jsonName = reader.nextName();
				if (jsonName.equals("type")) {
					type = reader.nextString(); 
			    } else if (jsonName.equals("id")) {
					id = reader.nextString();
			    } else if (jsonName.equals("url")) {
			        url = reader.nextString();
			    } else if (jsonName.equals("name")) {
			    	name = reader.nextString();
			    } else if (jsonName.equals("overlay")) {
			    	overlay = reader.nextBoolean();
			    } else if (jsonName.equals("default")) {
			    	defaultLayer = reader.nextBoolean();
			    } else if (jsonName.equals("extent")) {
			    	extent = readExtent(reader);
			    	if (extent != null) {
			    		if (provider == null)
			    			provider = new Provider();
			    		provider.addCoverageArea(extent);
			    	}
			    } else if (jsonName.equals("attribution")) {
			    	if (provider == null) 
			    		provider = new Provider();
			    	readAttribution(reader, provider, termsOfUseUrl);
			    } else {
			    	reader.skipValue();
			    }
			}
			reader.endObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (type == null || type.equals("wms"))
			return null;
		OpenStreetMapTileServer osmts = new OpenStreetMapTileServer(r, id, name, url, type, overlay, defaultLayer, provider, termsOfUseUrl,
				extent != null ? extent.zoomMin : 0, extent != null ? extent.zoomMax : 18, 256, 256, async);
		return osmts;
	}
	
	private static Provider.CoverageArea readExtent(JsonReader reader) {
		int zoomMin = 0;
		int zoomMax = 18;
		BoundingBox bbox = null;
		try {
			reader.beginObject();
			while (reader.hasNext()) {
				String jsonName = reader.nextName();
				if (jsonName.equals("max_zoom")) {
					zoomMax = reader.nextInt();
				} else if (jsonName.equals("min_zoom")) {
					zoomMin = reader.nextInt();
				} else if (jsonName.equals("bbox")) {
					bbox = readBbox(reader);
				} else{
			    	reader.skipValue();
			    }
			}
			reader.endObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new Provider.CoverageArea(zoomMin, zoomMax, bbox);
	}
	
	private static BoundingBox readBbox(JsonReader reader) {
		double left = 0, right = 0, top = 0, bottom = 0;
		BoundingBox bbox;
		try {
			reader.beginObject();
			while (reader.hasNext()) {
				String jsonName = reader.nextName();
				if (jsonName.equals("min_lon")) {
					left = reader.nextDouble();
				} else if (jsonName.equals("max_lon")) {
					right = reader.nextDouble();
				} else if (jsonName.equals("min_lat")) {
					bottom = reader.nextDouble();
				} else if (jsonName.equals("max_lat")) {
					top = reader.nextDouble();
				} else{
			    	reader.skipValue();
			    }
			}
			reader.endObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			bbox = new BoundingBox(left, bottom, right, top);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return bbox;
	}
	
	private static void readAttribution(JsonReader reader, Provider provider, String termsOfUseUrl) {
	
		try {
			reader.beginObject();
			while (reader.hasNext()) {
				String jsonName = reader.nextName();
				if (jsonName.equals("text")) {
					provider.attribution = reader.nextString();
				} else if (jsonName.equals("url")) {
					termsOfUseUrl = reader.nextString();
				} else {
			    	reader.skipValue();
			    }
			}
			reader.endObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// ===========================================================
	// Methods
	// ===========================================================
	
	public boolean isMetadataLoaded() {
		return metadataLoaded;
	}
	
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
		if (!metadataLoaded) throw new IllegalStateException("metadata not loaded");
		return tileWidth;
	}
	
	/**
	 * Get the tile height.
	 * @return The tile height in pixels.
	 */
	public int getTileHeight() {
		if (!metadataLoaded) throw new IllegalStateException("metadata not loaded");
		return tileHeight;
	}
	
	/**
	 * Get the minimum zoom level these tiles are available at.
	 * @return Minimum zoom level for which the tile layer is available.
	 */
	public int getMinZoomLevel() {
		if (!metadataLoaded) throw new IllegalStateException("metadata not loaded");
		return zoomLevelMin;
	}
	
	/**
	 * Get the maximum zoom level these tiles are available at.
	 * @return Maximum zoom level for which the tile layer is available.
	 */
	public int getMaxZoomLevel() {
		if (!metadataLoaded) throw new IllegalStateException("metadata not loaded");
//		if (providers != null && providers.size() > 0) {
//			zoomLevelMax = 0;
//			BoundingBox bbox = Application.mainActivity.getMap().getViewBox();
//			for (Provider p:providers) {
//				Provider.CoverageArea ca = p.getCoverageArea((bbox.getLeft() + bbox.getWidth()/2)/1E7d, bbox.getCenterLat());
//				if (ca != null && ca.zoomMax > zoomLevelMax)
//					zoomLevelMax = ca.zoomMax;
//				Log.d("OpenStreetMapTileServer","Provider " + p.getAttribution() + " max zoom " + zoomLevelMax);
//			}
//		}
		return zoomLevelMax;
	}
	
	/**
	 * Get the filename extensions that applies to the tile images.
	 * @return Image filename extension, eg ".png".
	 */
	public String getImageExtension() {
		// Log.d("OpenStreetMapTileServer","extension " + imageFilenameExtension);
		return imageFilenameExtension;
	}
	
	/**
	 * Get the branding logo for the tile layer.
	 * @return The branding logo, or null if there is none.
	 */
	public Drawable getBrandLogo() {
		if (!metadataLoaded) throw new IllegalStateException("metadata not loaded");
		return brandLogo;
	}
	
	/**
	 * Get the attributions that apply to the given map display.
	 * @param zoom Zoom level of the display.
	 * @param area Displayed area to get the attributions of.
	 * @return Collections of attributions that apply to the specified area and zoom.
	 */
	public Collection<String> getAttributions(final int zoom, final BoundingBox area) {
		if (!metadataLoaded) throw new IllegalStateException("metadata not loaded");
		Collection<String> ret = new ArrayList<String>();
		for (Provider p : providers) {
			if (p.getAttribution() != null)
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
	 * Get the latE7 offset
	 * @param zoomLevel TODO
	 * @return offset in WGS84, null == no offset
	 */
	public Offset getOffset(int zoomLevel) {
		if (zoomLevel < zoomLevelMin) {
			return null;
		}
		if (zoomLevel > zoomLevelMax) {
			return offsets[zoomLevelMax-zoomLevelMin];
		}
		return offsets[zoomLevel-zoomLevelMin];	
	}
	
	/**
	 * Set the lat offset
	 * @param o in WGS84
	 */
	public void setOffset(int zoomLevel, double offsetLon, double offsetLat) {
		// Log.d("OpenStreetMapTileServer","setOffset " + zoomLevel + " " + offsetLon + " " + offsetLat);
		if (zoomLevel < zoomLevelMin || zoomLevel > zoomLevelMax) {
			return; // do nothing
		}
		if (offsets[zoomLevel-zoomLevelMin]==null)
			offsets[zoomLevel-zoomLevelMin] = new Offset();
		offsets[zoomLevel-zoomLevelMin].lon = offsetLon;
		offsets[zoomLevel-zoomLevelMin].lat = offsetLat;
	}
		
	/**
	 * Set the offset for all zoom levels
	 * @param o in WGS84
	 */
	public void setOffset(double offsetLon, double offsetLat) {
		for (int i=0;i<offsets.length;i++) {
			if (offsets[i] == null)
				offsets[i] = new Offset();
			offsets[i].lon = offsetLon;
			offsets[i].lat = offsetLat;
		}
	}
	
	/**
	 * Set the offset for a range of zoom levels
	 * @param o in WGS84
	 */
	public void setOffset(int startZoom, int endZoom, double offsetLon, double offsetLat) {
		for (int z=startZoom;z<=endZoom;z++) {
			setOffset(z, offsetLon, offsetLat);
		}
	}
	
	public Offset[] getOffsets() {
		return offsets;
	}
	
	public void setOffsets(Offset[] offsets) {
		this.offsets = offsets;
	}
	
	/**
	 * Get all the available tile layer IDs. Slightly complex to get a reasonable order
	 * @param filtered only return servers that overlap/intersect with the current bbox
	 * @return All available tile layer IDs.
	 */
	public static String[] getIds(boolean filtered) {
		LinkedList<String> ids = new LinkedList<String>();
		boolean noneSeen = false;
		TreeSet<String> sortedKeySet = new TreeSet<String>(backgroundServerList.keySet());
		for (String key:sortedKeySet) {
			OpenStreetMapTileServer osmts = backgroundServerList.get(key);
			if (filtered) {
				if (osmts.providers.size() > 0) {
					boolean covers = false; // default is to not include  
					for (Provider p:osmts.providers) {
						if (p.covers(Application.mainActivity.getMap().getViewBox())) { 
							covers = true;
							break;
						}
					}
					if (!covers) {
						continue;
					}
				}
			}
			
			if (osmts.defaultLayer) {
				if (noneSeen)
					ids.add(1,key);
				else
					ids.add(0,key);
				if (key.equals("NONE"))
					noneSeen = true;
			} else
				ids.addLast(key);
		}
		String [] result = new String[ids.size()];
		for (int i = 0; i<result.length; i++)
			result[i] = ids.get(i);
		return  result;
	}
	
	/**
	 * Get tile server names
	 * @param filtered
	 * @return
	 */
	public static String[] getNames(boolean filtered) {
		ArrayList<String> names = new ArrayList<String>();
		for (String key:getIds(filtered)) {
			OpenStreetMapTileServer osmts = backgroundServerList.get(key);
			names.add(osmts.name);
		}
		String [] result = new String[names.size()];
		for (int i = 0; i<result.length; i++)
			result[i] = names.get(i);
		return  result;
	}
	
	/**
	 * Get tile server names from list of ids
	 * @param ids
	 * @return
	 */
	public static String[] getNames(String[] ids) {
		ArrayList<String> names = new ArrayList<String>();
		for (String key:ids) {
			OpenStreetMapTileServer osmts = backgroundServerList.get(key);
			names.add(osmts.name);
		}
		String [] result = new String[names.size()];
		for (int i = 0; i<result.length; i++)
			result[i] = names.get(i);
		return  result;
	}
	/**
	 * Get all the available tile layer IDs. Slightly complex to get a reasonable order
	 * @param filtered only return servers that overlap/intersect with the current bbox
	 * @return All available tile layer IDs.
	 */
	public static String[] getOverlayIds(boolean filtered) {
		LinkedList<String> ids = new LinkedList<String>();
		boolean noneSeen = false;
		TreeSet<String> sortedKeySet = new TreeSet<String>(overlayServerList.keySet());
		for (String key:sortedKeySet) {
			OpenStreetMapTileServer osmts = overlayServerList.get(key);
			if (filtered) {
				if (osmts.providers.size() > 0) {
					boolean covers = false; // default is to not include  
					for (Provider p:osmts.providers) {
						if (p.covers(Application.mainActivity.getMap().getViewBox())) { 
							covers = true;
							break;
						}
					}
					if (!covers) {
						continue;
					}
				}
			}
			
			if (osmts.defaultLayer) {
				if (noneSeen)
					ids.add(1,key);
				else
					ids.add(0,key);
				if (key.equals("NONE"))
					noneSeen = true;
			} else
				ids.addLast(key);
		}
		String [] result = new String[ids.size()];
		for (int i = 0; i<result.length; i++)
			result[i] = ids.get(i);
		return  result;
	}
	
	/**
	 * Get tile server names
	 * @param filtered
	 * @return
	 */
	public static String[] getOverlayNames(boolean filtered) {
		ArrayList<String> names = new ArrayList<String>();
		for (String key:getIds(filtered)) {
			OpenStreetMapTileServer osmts = overlayServerList.get(key);
			names.add(osmts.name);
		}
		String [] result = new String[names.size()];
		for (int i = 0; i<result.length; i++)
			result[i] = names.get(i);
		return  result;
	}
	
	/**
	 * Get tile server names from list of ids
	 * @param ids
	 * @return
	 */
	public static String[] getOverlayNames(String[] ids) {
		ArrayList<String> names = new ArrayList<String>();
		for (String key:ids) {
			OpenStreetMapTileServer osmts = overlayServerList.get(key);
			names.add(osmts.name);
		}
		String [] result = new String[names.size()];
		for (int i = 0; i<result.length; i++)
			result[i] = names.get(i);
		return  result;
	}
	
	
	private static String replaceParameter(final String s, final String param, final String value) {
		String result = s;
		// replace "${param}"
		result = result.replaceFirst("\\$\\{" + param + "\\}", value);
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
		if (!metadataLoaded) throw new IllegalStateException("metadata not loaded");
		String result = tileUrl;
		
//		// Position-sensitive replacements - Potlatch !/!/! syntax
//		result = result.replaceFirst("\\!", Integer.toString(aTile.zoomLevel));
//		result = result.replaceFirst("\\!", Integer.toString(aTile.x));
//		result = result.replaceFirst("\\!", Integer.toString(aTile.y));
		
		// Named replacements
		result = replaceParameter(result, "zoom", Integer.toString(aTile.zoomLevel));
		result = replaceParameter(result, "z", Integer.toString(aTile.zoomLevel));
		result = replaceParameter(result, "x", Integer.toString(aTile.x));
		result = replaceParameter(result, "y", Integer.toString(aTile.y));
		result = replaceParameter(result, "quadkey", quadTree(aTile));
		
		// Rotate through the list of subdomains
		String subdomain = null;
		synchronized (subdomains) {
			subdomain = subdomains.poll();
			if (subdomain != null) subdomains.add(subdomain);
		}
		if (subdomain != null) result = replaceParameter(result, "subdomain", subdomain);
		
		return result;
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
	
	/**
	 * This is essentially the code in in the reference implementation see
	 * https://trac.openstreetmap.org/browser/subversion/applications/editors/josm/plugins/imagery_offset_db/src/iodb/ImageryIdGenerator.java#L14
	 * @return the id for a imagery offset database query
	 */
	public String getImageryOffsetId() {
        if( tileUrl == null )
  	            return null;
  	
        // predefined layers
        if(id.equals("BING")) {
            return "bing";
        }

        if(tileUrl.contains("irs.gis-lab.info")) {
            return "scanex_irs";
        }

        if(id.equalsIgnoreCase("Mapbox")) {
            return "mapbox";
        }
        
        // Remove protocol
        int i = tileUrl.indexOf("://");
        tileUrl = tileUrl.substring(i + 3);

        // Split URL into address and query string
        i = tileUrl.indexOf('?');
        String query = "";
        if( i > 0 ) {
            query = tileUrl.substring(i);
            tileUrl = tileUrl.substring(0, i);
        }

        TreeMap<String, String> qparams = new TreeMap<String, String>();
        String[] qparamsStr = query.length() > 1 ? query.substring(1).split("&") : new String[0];
        for( String param : qparamsStr ) {
            String[] kv = param.split("=");
            kv[0] = kv[0].toLowerCase();
            // TMS: skip parameters with variable values
            if( kv.length > 1 && kv[1].indexOf('{') >= 0 && kv[1].indexOf('}') > 0 )
                continue;
            qparams.put(kv[0].toLowerCase(), kv.length > 1 ? kv[1] : null);
        }

        // Reconstruct query parameters
        StringBuilder sb = new StringBuilder();
        for( String qk : qparams.keySet() ) {
            if( sb.length() > 0 )
                sb.append('&');
            else if( query.length() > 0 )
                sb.append('?');
            sb.append(qk).append('=').append(qparams.get(qk));
        }
        query = sb.toString();

        // TMS: remove /{zoom} and /{y}.png parts
        tileUrl = tileUrl.replaceAll("\\/\\{[^}]+\\}(?:\\.\\w+)?", "");
        // TMS: remove variable parts
        tileUrl = tileUrl.replaceAll("\\{[^}]+\\}", "");
        while( tileUrl.contains("..") )
            tileUrl = tileUrl.replace("..", ".");
        if( tileUrl.startsWith(".") )
            tileUrl = tileUrl.substring(1);

        return tileUrl + query;
    }
	
	@Override
	public String toString() {
		return 	"ID: " + id + " Name " + name + " maxZoom " + zoomLevelMax + " Tile URL " + tileUrl;
	}
}
