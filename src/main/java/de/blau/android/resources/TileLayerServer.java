// Created by plusminus on 18:23:16 - 25.09.2008
package  de.blau.android.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.gson.stream.JsonReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.Density;
import de.blau.android.util.Offset;

/**
 * The OpenStreetMapRendererInfo stores information about available tile servers.
 * 
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06
 * by Marcus Wolschon to be integrated into the de.blau.android.osmeditor4android.
 *  
 * @author Nicolas Gramlich
 * @author Marcus Wolschon Marcus@Wolschon.biz
 *
 */
public class TileLayerServer {
	public static final String LAYER_MAPNIK = "MAPNIK";
	public static final String LAYER_NONE = "NONE";
	private static final String DEBUG_TAG = TileLayerServer.class.getName();
	
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
		 * 
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
			if (coverageAreas.size() == 0) {
				return true;
			}
			for (CoverageArea a : coverageAreas) {
				if (a.covers(area)) {
					return true;
				}
			}
			return false;
		}
		

		public int getZoom(BoundingBox area) {
			if (coverageAreas.size() == 0) {
				return -1;
			}
			int max = 0;
			for (CoverageArea a : coverageAreas) {
				if (a.covers(area)) {
					int m = a.zoomMax;
					if (m > max) {
						max = m;
					}
				}
			}
			return max;
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

	private static final int PREFERENCE_DEFAULT = 0;
	private static final int PREFERENCE_BEST = 10;
	
	private static TileLayerServer cachedBackground = null;
	private static TileLayerServer cachedOverlay = null;
	
	// ===========================================================
	// Fields
	// ===========================================================

	private Context ctx;
	private boolean metadataLoaded;
	private String id, name, tileUrl, imageFilenameExtension, touUri;
	private boolean overlay, defaultLayer;
	private int zoomLevelMin, zoomLevelMax, tileWidth, tileHeight, preference;
	private int maxOverZoom = 3; // currently hardwired
	private Drawable brandLogo;
	private final Queue<String> subdomains = new LinkedList<String>();
	private int defaultAlpha;
	private Collection<Provider> providers = new ArrayList<Provider>();
	private Offset[] offsets;
	
	private static final HashMap<String,TileLayerServer> backgroundServerList =new HashMap<String,TileLayerServer>();
	private static HashMap<String,TileLayerServer> overlayServerList = new HashMap<String,TileLayerServer>();
	private static boolean ready = false;
	private static ArrayList<String> imageryBlacklist = null;

	
	// ===========================================================
	// Constructors
	// ===========================================================
	
	private void loadInfo(String metadataUrl) {
		try {
			Resources r = ctx.getResources();
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
				conn.setRequestProperty("User-Agent", App.userAgent);
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
							brandLogo = ContextCompat.getDrawable(ctx, resid);
						} else {
							// assume Internet URL
							URLConnection conn = new URL(replaceGeneralParameters(brandLogoUri)).openConnection();
							conn.setRequestProperty("User-Agent", App.userAgent);
							InputStream bis = conn.getInputStream();
							Bitmap brandLogoBitmap = BitmapFactory.decodeStream(bis);
							// scale according to density
							if (brandLogoBitmap != null)
								brandLogo = new BitmapDrawable(r,Bitmap.createScaledBitmap(brandLogoBitmap, Density.dpToPx(ctx,brandLogoBitmap.getWidth()), Density.dpToPx(ctx,brandLogoBitmap.getHeight()), false)); 
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
								Collections.addAll(subdomains, switchValues.split(","));
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
						// hack for bing
						if (!metadataUrl.contains("virtualearth")) {
							zoomLevelMax = Integer.parseInt(parser.getText().trim());
						} 
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
			if (ctx instanceof Main) { // don't do this in the service
				((Main)ctx).getMap().setPrefs(ctx, new Preferences(ctx));
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
	private TileLayerServer(final Context ctx, final String id, final String name, final String url, final String type, 
			final boolean overlay, final boolean defaultLayer, final Provider provider, final String termsOfUseUrl,
			final int zoomLevelMin, final int zoomLevelMax, final int tileWidth, final int  tileHeight, final int preference, boolean async) {
		this.ctx = ctx;
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
		this.preference = preference;
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
		this.id = this.id.toUpperCase(Locale.US);

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
			tileUrl = "http://irs.gis-lab.info/?layers="+tileUrl.toLowerCase(Locale.US)+"&request=GetTile&z={zoom}&x={x}&y={y}";
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
				Collections.addAll(subdomains, switchValues.split(","));
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
	public static TileLayerServer getDefault(final Context ctx, final boolean async) {
		// ask for an invalid renderer, so we'll get the fallback default
		return get(ctx, "", async);
	}
	
	/**
	 * Parse json files for imagery configs and add them to backgroundServerList or overlayServerList
	 * @param r
	 * @param is
	 * @param async
	 * @throws IOException
	 */
	private static void parseImageryFile(Context ctx, InputStream is, final boolean async) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
		try {
			reader.beginArray();
			while (reader.hasNext()) {
				TileLayerServer osmts = readServer(ctx, reader, async);
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
		} catch (IOException ioex) {
			Log.d(DEBUG_TAG,"Imagery file ignored " + ioex);
		} catch (IllegalStateException isex) {
			Log.d(DEBUG_TAG,"Imagery file ignored " + isex);
		}
		finally {
			try {
				reader.close();
			} catch (IOException ioex) {
				Log.d(DEBUG_TAG,"Ignored " + ioex);
			}
		}
	}
	
	/**
	 * Get the tile server information for a specified tile server id. If the given
	 * id cannot be found, a default renderer is selected. 
	 * Note: will read the the config files it that hasn't happened yet
	 * @param ctx activity context
	 * @param id The internal id of the tile layer, eg "MAPNIK"
     * @param async get meta data asynchronously
	 */
	public synchronized static TileLayerServer get(final Context ctx, final String id, final boolean async) {	
		Resources r = ctx.getResources();
		
		synchronized (backgroundServerList) {
			if (!ready) {
				Log.d("OpenStreetMapTileServer","Parsing configuration files");

				final String FILE_NAME_USER_IMAGERY = "imagery.json";
				final String FILE_NAME_VESPUCCI_IMAGERY = "imagery_vespucci.json";
				
				File sdcard = Environment.getExternalStorageDirectory();
				String userImagery = sdcard.getPath() + "/" +
						Paths.DIRECTORY_PATH_VESPUCCI + "/" +
						FILE_NAME_USER_IMAGERY;
				Log.i("OpenStreetMapTileServer","Trying to read custom imagery from " + userImagery);
				try {
					InputStream is = new FileInputStream(new File(userImagery));		
					parseImageryFile(ctx, is, async);
				} catch (IOException e) {
					// Don't care if reading fails
				}
						
				AssetManager assetManager = ctx.getAssets();
				// entries in earlier files will not be overwritten by later ones
				String[] imageryFiles = {FILE_NAME_VESPUCCI_IMAGERY, FILE_NAME_USER_IMAGERY};
				for (String fn:imageryFiles) {
					try {
						InputStream is = assetManager.open(fn);
						parseImageryFile(ctx, is, async);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (imageryBlacklist != null) {
					applyBlacklist(imageryBlacklist);
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
			TileLayerServer tempOSMTS =  overlayServerList.get(id);
			boolean overlay = tempOSMTS != null;
			
			if (overlay) {
				if (cachedOverlay == null || !cachedOverlay.id.equals(id)) {
					cachedOverlay = overlayServerList.get(id);
					if (cachedOverlay == null || !cachedOverlay.metadataLoaded)
						cachedOverlay = overlayServerList.get(LAYER_NONE);
					Log.d("OpenStreetMapTileServer", "cachedOverlay " + (cachedOverlay == null?"null":cachedOverlay.id));
				}
				return cachedOverlay;
			} else { 
				if (cachedBackground == null || !cachedBackground.id.equals(id)) {
					cachedBackground = backgroundServerList.get(id);
					if (cachedBackground == null || !cachedBackground.metadataLoaded)
						cachedBackground = backgroundServerList.get(LAYER_MAPNIK);
					Log.d("OpenStreetMapTileServer", "requested id " + id + " cached " + (cachedBackground == null?"null":cachedBackground.id));
				}
				return cachedBackground;
			}
		} 
	}
	
	private static TileLayerServer readServer(Context ctx, JsonReader reader, boolean async) {
		String id = null;
		String name = null;
		String url = null;
		String type = null;
		boolean overlay = false;
		boolean defaultLayer = false;
		Provider.CoverageArea extent = null;
		Provider provider = null;
		String termsOfUseUrl = null;
		int preference = PREFERENCE_DEFAULT;
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
			    	termsOfUseUrl = readAttribution(reader, provider);
			    } else if (jsonName.equals("best")) {
			    	preference = reader.nextBoolean() ? PREFERENCE_BEST : PREFERENCE_DEFAULT;
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
		TileLayerServer osmts = new TileLayerServer(ctx, id, name, url, type, overlay, defaultLayer, provider, termsOfUseUrl,
				extent != null ? extent.zoomMin : 0, extent != null ? extent.zoomMax : 18, 256, 256, preference, async);
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
	
	private static String readAttribution(JsonReader reader, Provider provider) {
		String termsOfUseUrl = null;
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
		return termsOfUseUrl;
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
	 * @param zoomLevel the zoom level we want the offset for
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
	 * Set the lat offset for one specific zoom
     * @param zoomLevel zoom level to set the offset for
     * @param offsetLon offest in lon direction in WGS84
     * @param offsetLat offest in lat direction in WGS84
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
	 * @param offsetLon offest in lon direction in WGS84
     * @param offsetLat offest in lat direction in WGS84
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
     * @param startZoom start of zoom range
     * @param endZoom end of zoom range
     * @param offsetLon offest in lon direction in WGS84
     * @param offsetLat offest in lat direction in WGS84
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
	 * Return the name for this imagery
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	private static List<TileLayerServer> getServersFilteredSorted(boolean filtered, HashMap<String,TileLayerServer> serverMap, BoundingBox currentBox) {
		TileLayerServer noneLayer = null;
		List<TileLayerServer> list = new ArrayList<TileLayerServer>();
		for (TileLayerServer osmts:serverMap.values()) {
			if (filtered && currentBox != null) {
				if (!osmts.covers(currentBox)) {
					continue;
				}
			}
			// add this after sorting
			if (LAYER_NONE.equals(osmts.id)) {
				noneLayer = osmts;
				continue;
			}
			// add the rest now
			list.add(osmts);
		}
		// sort according to preference, at one time we might take bb size in to account
		Collections.sort(list, new Comparator<TileLayerServer>() {
			@Override
			public int compare(TileLayerServer t1, TileLayerServer t2) {
				if (t1.preference < t2.preference) {
					return 1;
			    } else if (t1.preference > t2.preference) {
			    	return -1;
			    }
				return t1.getName().compareToIgnoreCase(t2.getName()); // alphabetic
			}});
		// add NONE
		if (noneLayer != null) {
			list.add(0,noneLayer);
		}
		return list;
	}
	
	/**
	 * Test if the bounding box is covered by this tile source
	 * @param box the bounding box we want to test
	 * @return true if covered or no coverage information
	 */
	public boolean covers(BoundingBox box) {
		if (providers.size() > 0) { 
			for (Provider p:providers) {
				if (p.covers(box)) { 
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Get location dependent max zoom
	 * @param box the bounding box we want to get the max zoom for
	 * @return maximum zoom for this area, -1 if nothing found
	 */
	public int getMaxZoom(BoundingBox box) {
		int max = 0;
		if (providers.size() > 0) { 
			for (Provider p:providers) {
				int m = p.getZoom(box);
				if (m > max) { 
					max = m;
				}
			}
			return max;
		}
		return -1;
	}
	
	/**
	 * Get all the available tile layer IDs. Slightly complex to get a reasonable order
	 * @param filtered only return servers that overlap/intersect with the current bbox
	 * @return All available tile layer IDs.
	 */
	public static String[] getIds(BoundingBox viewBox, boolean filtered) {
		List<String> ids = new ArrayList<String>();
		List<TileLayerServer> list = getServersFilteredSorted(filtered, backgroundServerList, viewBox);
		for (TileLayerServer t:list) {
			ids.add(t.id);
		}
		String[] idArray = new String[ids.size()];
		ids.toArray(idArray);
		return idArray;
	}
	
	/**
	 * Get tile server names
	 * @param filtered
	 * @return
	 */
	public static String[] getNames(BoundingBox viewBox, boolean filtered) {
		ArrayList<String> names = new ArrayList<String>();
		for (String key:getIds(viewBox, filtered)) {
			TileLayerServer osmts = backgroundServerList.get(key);
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
			TileLayerServer osmts = backgroundServerList.get(key);
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
	public static String[] getOverlayIds(BoundingBox viewBox, boolean filtered) {
		List<String> ids = new ArrayList<String>();
		List<TileLayerServer> list = getServersFilteredSorted(filtered, overlayServerList, viewBox);
		for (TileLayerServer t:list) {
			ids.add(t.id);
		}
		String[] idArray = new String[ids.size()];
		ids.toArray(idArray);
		return idArray;
	}
	
	/**
	 * Get tile server names
	 * @param filtered
	 * @return
	 */
	public static String[] getOverlayNames(BoundingBox viewBox, boolean filtered) {
		ArrayList<String> names = new ArrayList<String>();
		for (String key:getIds(viewBox, filtered)) {
			TileLayerServer osmts = overlayServerList.get(key);
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
			TileLayerServer osmts = overlayServerList.get(key);
			names.add(osmts.name);
		}
		String [] result = new String[names.size()];
		for (int i = 0; i<result.length; i++)
			result[i] = names.get(i);
		return  result;
	}
	
	/*
	 * 
	 */
	private static String replaceParameter(final String s, final String param, final String value) {
		String result = s;
		// replace "${param}"
		// not used in imagery index result = result.replaceFirst("\\$\\{" + param + "\\}", value);
		// replace "$param"
		// not used in imagery index result = result.replaceFirst("\\$" + param, value);
		// replace "{param}"
		result = result.replaceFirst("\\{" + param + "\\}", value);
		return result;
	}
	
	private String replaceGeneralParameters(final String s) {
		Resources r = ctx.getResources();
		final Locale l = r.getConfiguration().locale;
		String result = s;
		result = replaceParameter(result, "culture", l.getLanguage().toLowerCase(Locale.US) + "-" + l.getCountry().toLowerCase(Locale.US));
		// Bing API key assigned to Andrew Gregory
		try {
			result = replaceParameter(result, "bingapikey", r.getString(R.string.bingapikey));
		} catch (Exception ex) {
			Log.e(DEBUG_TAG,"replacing bingapi key failed: " + ex.getMessage());
		}
		return result;
	}
	
	private static final int BASE = 0;
	private static final int PARAM = 1;
	/**
	 * Allocate the following just once
	 */
	StringBuilder builder = new StringBuilder(100); // 100 is just an estimate to avoid re-allocating
	StringBuilder param = new StringBuilder();	
	StringBuilder quadKey = new StringBuilder();
	
	/**
	 * Get the URL that can be used to obtain the image of the given tile.
	 * 
	 * This is 5-100 times faster than the previous implementation.
	 * @param aTile The tile to get the URL for.
	 * @return URL of the given tile.
	 */
	public String getTileURLString(final MapTile aTile) {
		if (!metadataLoaded) {
			throw new IllegalStateException("metadata not loaded");
		}
		builder.setLength(0);		
		int state = BASE;
		for(char c:tileUrl.toCharArray()) {
			if (state == BASE) {
				if (c=='{') {
					state = PARAM;
					param.setLength(0); // reset
				} else {
					builder.append(c);
				}
			} else {
				if (c=='}') {
					state = BASE;
					String p = param.toString();
					if ("x".equals(p)) {
						builder.append(Integer.toString(aTile.x));
					} else if ("y".equals(p)) {
						builder.append(Integer.toString(aTile.y));
					} else if ("z".equals(p)) {
						builder.append(Integer.toString(aTile.zoomLevel));
					} else if ("zoom".equals(p)) {
						builder.append(Integer.toString(aTile.zoomLevel));
					} else if ("-y".equals(p)) {
						int ymax = 1 << aTile.zoomLevel;
						int y = ymax - aTile.y - 1;
						builder.append(Integer.toString(y));
					} else if ("quadkey".equals(p)) {
						builder.append(quadTree(aTile));
					} else if ("subdomain".equals(p)) {
						// Rotate through the list of sub-domains
						String subdomain = null;
						synchronized (subdomains) {
							subdomain = subdomains.poll();
							if (subdomain != null) {
								subdomains.add(subdomain);
							}
						}
						if (subdomain != null) {
							builder.append(subdomain);
						}
					}
				} else {
					param.append(c);
				}
			}
		}
		
		return builder.toString();
	}
		
	/**
	 * Converts TMS tile coordinates to QuadTree
	 * @param aTile The tile coordinates to convert
	 * @return The QuadTree as String.
	 */
	String quadTree(final MapTile aTile) {
		quadKey.setLength(0);
		for (int i = aTile.zoomLevel; i > 0; i--) {
			int digit = 0;
			int mask = 1 << (i - 1);
			if ((aTile.x & mask) != 0) {
				digit += 1;
			}
			if ((aTile.y & mask) != 0) {
				digit += 2;
			}
			quadKey.append(digit);
		}
		return quadKey.toString();
	}
	
	/**
	 * Get the maximum we over zoom for this layer
	 * @return
	 */
	public int getMaxOverZoom() {
		return maxOverZoom;
	}
	
	/**
	 * This is essentially the code in in the reference implementation see
	 * 
	 * https://trac.openstreetmap.org/browser/subversion/applications/editors/josm/plugins/imagery_offset_db/src/iodb/ImageryIdGenerator.java#L14
	 * 
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
        if (i == -1) { // TODO more sanity checks
        	return "invalid_URL";
        }
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
            kv[0] = kv[0].toLowerCase(Locale.US);
            // TMS: skip parameters with variable values
            if( kv.length > 1 && kv[1].indexOf('{') >= 0 && kv[1].indexOf('}') > 0 )
                continue;
            qparams.put(kv[0].toLowerCase(Locale.US), kv.length > 1 ? kv[1] : null);
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
	
	/**
	 * Remove all background and overlay entries that match the supplied blacklist
	 * @param blacklist list of servers that sould be removed
	 */
	public static void applyBlacklist(ArrayList<String> blacklist) {
		// first compile the regexs
		ArrayList<Pattern> patterns = new ArrayList<Pattern>();
		for (String regex:blacklist) {
			patterns.add(Pattern.compile(regex));
		}
		for (Pattern p:patterns) {
			for (String key:new TreeSet<String>(backgroundServerList.keySet())) { // shallow copy
				TileLayerServer osmts = backgroundServerList.get(key);
				Matcher m = p.matcher(osmts.tileUrl);
				if (m.find()) {
					backgroundServerList.remove(key);
					if (cachedBackground != null && cachedBackground.equals(osmts)) {
						cachedBackground = null;
					}
					Log.d("OpenStreetMapTileServer","Removed background tile layer " + key);
				}
			}
			for (String key:new TreeSet<String>(overlayServerList.keySet())) { // shallow copy
				TileLayerServer osmts = overlayServerList.get(key);
				Matcher m = p.matcher(osmts.tileUrl);
				if (m.find()) {
					overlayServerList.remove(key);
					if (cachedOverlay != null && cachedOverlay.equals(osmts)) {
						cachedOverlay = null;
					}
					Log.d("OpenStreetMapTileServer","Removed overlay tile layer " + key);
				}
			}
		}
	}
	
	public static void setBlacklist(ArrayList<String> bl) {
		imageryBlacklist  = bl;
	}
	
	@Override
	public String toString() {
		return 	"ID: " + id + " Name " + name + " maxZoom " + zoomLevelMax + " Tile URL " + tileUrl;
	}
}
