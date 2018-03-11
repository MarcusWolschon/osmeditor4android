// Created by plusminus on 18:23:16 - 25.09.2008
package de.blau.android.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import de.blau.android.App;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.contract.Files;
import de.blau.android.contract.Paths;
import de.blau.android.contract.Urls;
import de.blau.android.imageryoffset.Offset;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer.Provider.CoverageArea;
import de.blau.android.services.util.MapTile;
import de.blau.android.util.Density;
import de.blau.android.util.GeoContext;
import de.blau.android.util.GeoMath;
import de.blau.android.util.SavingHelper;
import de.blau.android.views.layers.MapTilesLayer;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The OpenStreetMapRendererInfo stores information about available tile servers.
 * 
 * This class was taken from OpenStreetMapViewer (original package org.andnav.osm) in 2010-06 by Marcus Wolschon to be
 * integrated into the de.blau.android.osmeditor4android.
 * 
 * @author Nicolas Gramlich
 * @author Marcus Wolschon Marcus@Wolschon.biz
 *
 */
public class TileLayerServer {
    static final String         EPSG_900913     = "EPSG:900913";
    static final String         EPSG_3857       = "EPSG:3857";
    static final String         TYPE_BING       = "bing";
    static final String         TYPE_TMS        = "tms";
    static final String         TYPE_WMS        = "wms";
    static final String         TYPE_SCANEX     = "scanex";
    private static final String DEBUG_TAG       = "TileLayerServer";
    public static final String  LAYER_MAPNIK    = "MAPNIK";
    public static final String  LAYER_NONE      = "NONE";
    public static final String  LAYER_NOOVERLAY = "NOOVERLAY";
    public static final String  LAYER_BING      = "BING";

    /**
     * A tile layer provide has some attribution text, and one or more coverage areas.
     * 
     * @author Andrew Gregory
     */
    static class Provider {
        /**
         * A coverage area is a range of zooms and a bounding box.
         * 
         * @author Andrew Gregory
         */
        static class CoverageArea {
            /** Zoom and area of this coverage area. */
            private int         zoomMin;
            private int         zoomMax;
            private BoundingBox bbox = null;

            /**
             * Create a coverage area given XML data.
             * 
             * @param parser The XML parser.
             * @throws IOException
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
                bbox = new BoundingBox(left, bottom, right, top);
            }

            public CoverageArea(int zoomMin, int zoomMax, @Nullable BoundingBox bbox) {
                this.zoomMin = zoomMin;
                this.zoomMax = zoomMax;
                this.bbox = bbox;
            }

            /**
             * Test if the given zoom and area is covered by this coverage area.
             * 
             * @param zoom The zoom level to test.
             * @param area The map area to test.
             * @return true if the given zoom and area are covered by this coverage area.
             */
            public boolean covers(int zoom, BoundingBox area) {
                return (zoom >= zoomMin && zoom <= zoomMax && (this.bbox == null || this.bbox.intersects(area)));
            }

            public boolean covers(@NonNull BoundingBox area) {
                return this.bbox == null || this.bbox.intersects(area);
            }

            public boolean covers(double lon, double lat) {
                return this.bbox == null || this.bbox.isIn((int) (lon * 1E7d), (int) (lat * 1E7d));
            }

            public int getMinZoomLevel() {
                return zoomMin;
            }

            public int getMaxZoomLevel() {
                return zoomMax;
            }

            public BoundingBox getBoundingBox() {
                return bbox;
            }
        }

        /** Attribution for this provider. */
        private String             attribution;
        /** Coverage area provided by this provider. */
        private List<CoverageArea> coverageAreas = new ArrayList<>();

        /**
         * Create a new Provider from XML data.
         * 
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

        /**
         * Default constructor
         */
        public Provider() {
        }

        /**
         * Add a CoverageArea to the list
         * 
         * @param ca the CoverageArea to add
         */
        public void addCoverageArea(CoverageArea ca) {
            coverageAreas.add(ca);
        }

        /**
         * Get the attribution for this provider.
         * 
         * @return The attribution for this provider.
         */
        public String getAttribution() {
            return attribution;
        }

        /**
         * Set the attribution for this provider
         * 
         * @param attribution the attribution string
         */
        public void setAttribution(String attribution) {
            this.attribution = attribution;
        }

        /**
         * Test if the provider covers the given zoom and area.
         * 
         * @param zoom Zoom level to test.
         * @param area Map area to test.
         * @return true if the provider has coverage of the given zoom and area.
         */
        public boolean covers(int zoom, BoundingBox area) {
            if (coverageAreas.isEmpty()) {
                return true;
            }
            for (CoverageArea a : coverageAreas) {
                if (a.covers(zoom, area)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Test if the provider covers the area
         * 
         * @param area area to test
         * @return true if the provider has coverage of the given area
         */
        public boolean covers(@NonNull BoundingBox area) {
            if (coverageAreas.isEmpty()) {
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
            if (coverageAreas.isEmpty()) {
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

        /**
         * Get the highest zoom CoverageArea for a location
         * 
         * @param lon longitude
         * @param lat latitude
         * @return a CoverageArea or null if none could be found
         */
        @Nullable
        public CoverageArea getCoverageArea(double lon, double lat) {
            CoverageArea result = null;
            if (!coverageAreas.isEmpty()) {
                for (CoverageArea a : coverageAreas) {
                    if (a.covers(lon, lat)) {
                        if (result == null) {
                            result = a;
                        } else {
                            if (a.zoomMax > result.zoomMax) {
                                result = a;
                            }
                        }
                    }
                    Log.d(DEBUG_TAG, "maxZoom " + a.zoomMax);
                }
            }
            return result;
        }
    }

    private static final int PREFERENCE_DEFAULT = 0;
    private static final int PREFERENCE_BEST    = 10;

    public static final int DEFAULT_MIN_ZOOM     = 0;
    public static final int DEFAULT_MAX_ZOOM     = 18;
    public static final int DEFAULT_MAX_OVERZOOM = 4;

    public static final int DEFAULT_TILE_SIZE = 256;
    static final int        WMS_TILE_SIZE     = 512;

    // ===========================================================
    // Fields
    // ===========================================================

    private Context             ctx;
    private boolean             metadataLoaded;
    private String              id;
    private String              name;
    private String              type;
    private String              tileUrl;
    private String              originalUrl;
    private String              imageFilenameExtension;
    private String              touUri;
    private boolean             overlay;
    private boolean             defaultLayer;
    private int                 zoomLevelMin;
    private int                 zoomLevelMax;
    private int                 tileWidth;
    private int                 tileHeight;
    private String              proj;
    private int                 preference;
    private long                startDate    = -1L;
    private long                endDate      = -1L;
    private int                 maxOverZoom  = DEFAULT_MAX_OVERZOOM; // currently hardwired
    private String              logoUrl      = null;
    private Bitmap              logoBitmap   = null;
    private Drawable            logoDrawable = null;
    private final Queue<String> subdomains   = new LinkedList<>();
    private int                 defaultAlpha;
    private List<Provider>      providers    = new ArrayList<>();

    private boolean  readOnly = false;
    private String   imageryOffsetId; // cached id for offset DB
    private Offset[] offsets;

    private static Map<String, TileLayerServer> backgroundServerList = null;
    private static Map<String, TileLayerServer> overlayServerList    = null;
    private static Object                       serverListLock       = new Object();
    private static boolean                      ready                = false;
    private static List<String>                 imageryBlacklist     = null;

    private static Map<String, Drawable> logoCache = new HashMap<>();
    private static final Drawable        NOLOGO    = new ColorDrawable();

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * Load additional data on the source from an URL, potentially async This is mainly used for bing imagery
     * 
     * @param metadataUrl the url for the meta-data
     */
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
                Request request = new Request.Builder().url(replaceGeneralParameters(metadataUrl)).build();
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
                Call metadataCall = client.newCall(request);
                Response metadataCallResponse = metadataCall.execute();
                if (metadataCallResponse.isSuccessful()) {
                    ResponseBody responseBody = metadataCallResponse.body();
                    is = responseBody.byteStream();
                } else {
                    throw new IOException(metadataCallResponse.message());
                }
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
                            logoDrawable = ContextCompat.getDrawable(ctx, resid);
                        } else {
                            // assume Internet URL
                            logoDrawable = getLogoFromUrl(brandLogoUri);
                        }
                    }
                    if ("ImageUrl".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                        tileUrl = parser.getText().trim();
                        // Log.d("OpenStreetMapTileServer","loadInfo tileUrl " + tileUrl);
                        int extPos = tileUrl.lastIndexOf(".jpeg"); // TODO fix this awlful hack
                        if (extPos >= 0) {
                            imageFilenameExtension = ".jpg";
                        }
                        // extract switch values
                        final String SWITCH_START = "{switch:";
                        int switchPos = tileUrl.indexOf(SWITCH_START);
                        if (switchPos >= 0) {
                            int switchEnd = tileUrl.indexOf('}', switchPos);
                            if (switchEnd >= 0) {
                                String switchValues = tileUrl.substring(switchPos + SWITCH_START.length(), switchEnd);
                                Collections.addAll(subdomains, switchValues.split(","));
                                StringBuilder t = new StringBuilder(tileUrl);
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
                            setMaxZoom(Integer.parseInt(parser.getText().trim()));
                        }
                    }
                    if ("ImageryProvider".equals(tagName)) {
                        try {
                            providers.add(new Provider(parser));
                        } catch (IOException | XmlPullParserException e) {
                            // if the provider can't be parsed, we can't do
                            // much about it
                            Log.e(DEBUG_TAG, "ImageryProvider problem", e);
                        }
                    }
                }
            }
            metadataLoaded = true;
            // once we've got here, a selected layer that was previously non-available might now be available ... reset
            // map preferences
            if (ctx instanceof Main) { // don't do this in the service
                ((Main) ctx).getMap().setPrefs(ctx, new Preferences(ctx));
            }
        } catch (IOException e) {
            Log.d(DEBUG_TAG, "Tileserver problem (IOException) metadata URL " + metadataUrl, e);
        } catch (XmlPullParserException e) {
            Log.e(DEBUG_TAG, "Tileserver problem (XmlPullParserException) metadata URL " + metadataUrl, e);
        }
    }

    /**
     * Retrieve a logo from the Internet
     * 
     * @param brandLogoUri the url
     * @return a scaled BitmapDrawable or null if the logo couldn't be found
     */
    @Nullable
    private BitmapDrawable getLogoFromUrl(@NonNull String brandLogoUri) {
        InputStream bis = null;
        try {
            Request request = new Request.Builder().url(replaceGeneralParameters(brandLogoUri)).build();
            OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
            Call logoCall = client.newCall(request);
            Response logoCallResponse = logoCall.execute();
            if (logoCallResponse.isSuccessful()) {
                ResponseBody responseBody = logoCallResponse.body();
                bis = responseBody.byteStream();
                Bitmap brandLogoBitmap = BitmapFactory.decodeStream(bis);
                return scaledBitmap(brandLogoBitmap);
            } else {
                throw new IOException(logoCallResponse.message());
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "getLogoFromUrl using " + brandLogoUri + " got " + e.getMessage());
        } finally {
            SavingHelper.close(bis);
        }
        return null;
    }

    /**
     * Scale logos so that they are max 24dp high
     * 
     * @param bitmap input Bitmap
     * @return a scaled BitmapDrawable
     */
    private BitmapDrawable scaledBitmap(Bitmap bitmap) {
        // scale according to density
        if (bitmap != null) {
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            float scale = height > 24 ? 24f / height : 1f;
            return new BitmapDrawable(ctx.getResources(),
                    Bitmap.createScaledBitmap(bitmap, Density.dpToPx(ctx, Math.round(width * scale)), Density.dpToPx(ctx, Math.round(height * scale)), false));
        }
        return null;
    }

    /**
     * Construct a new TileLayerServer object from the parameters
     * 
     * @param ctx android context
     * @param id the layer id
     * @param name the layer name
     * @param url the template url for the layer
     * @param type the special types of layer: "bing","scanex"
     * @param overlay true if this layer is an overlay
     * @param defaultLayer true if this should be used as the default
     * @param provider a Provider object containing detailed provider information
     * @param termsOfUseUrl a url pointing to terms of use
     * @param icon string containing the logo data, an url or a Android resource name
     * @param logoUrl if not null contains an URL for the logo
     * @param logoBytes if not null contains the bytes of a Bitmap
     * @param zoomLevelMin minimum supported zoom level
     * @param zoomLevelMax maximum supported room level
     * @param maxOverZoom maximum overzoom to allow
     * @param tileWidth width of the tiles in pixels
     * @param tileHeight height of the tiles in pixels
     * @param proj supported projection for WMS servers
     * @param preference relative preference (larger == better)
     * @param startDate start date as a ms since epoch value, -1 if not available
     * @param endDate end date as a ms since epoch value, -1 if not available
     * @param async run loadInfo in a AsyncTask needed for main process
     */
    TileLayerServer(final Context ctx, final String id, final String name, final String url, final String type, final boolean overlay,
            final boolean defaultLayer, final Provider provider, final String termsOfUseUrl, final String icon, String logoUrl, byte[] logoBytes,
            final int zoomLevelMin, final int zoomLevelMax, int maxOverZoom, final int tileWidth, final int tileHeight, final String proj, final int preference,
            final long startDate, final long endDate, boolean async) {

        this.ctx = ctx;
        this.id = id;
        this.name = name;
        this.type = type;
        tileUrl = url;
        originalUrl = url;
        this.overlay = overlay;
        this.defaultLayer = defaultLayer;
        this.zoomLevelMin = zoomLevelMin;
        this.setMaxZoom(zoomLevelMax);
        this.maxOverZoom = maxOverZoom;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.proj = proj;
        this.touUri = termsOfUseUrl;

        this.offsets = new Offset[zoomLevelMax - zoomLevelMin + 1];
        this.preference = preference;
        this.startDate = startDate;
        this.endDate = endDate;

        if (provider != null) {
            providers.add(provider);
        }

        metadataLoaded = true;

        if (name == null) {
            // parse error or other fatal issue
            this.name = "INVALID";
        }
        if (this.id == null) {
            // generate id from name
            this.id = nameToId(this.name);
        }
        //
        this.id = this.id.toUpperCase(Locale.US);

        if (originalUrl.startsWith("file:")) { // mbtiles no further processing needed
            readOnly = true;
        }

        if (proj != null) { // wms
            if (tileUrl.contains("image/jpeg")) {
                imageFilenameExtension = ".jpg";
            }
        }

        if (icon != null) {
            // String of the format "data:image/png;base64,iV....
            String[] splitString = icon.split(",", 2);
            if (splitString.length == 2 && "data:image/png;base64".equals(splitString[0])) {
                byte[] iconData = Base64.decode(splitString[1], 0);
                logoBitmap = BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
            } else if (icon.startsWith("http")) {
                this.logoUrl = icon;
            }
        } else if (logoUrl != null) {
            this.logoUrl = logoUrl;
        } else if (logoBytes != null) {
            logoBitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.length);
        }

        // TODO think of a elegant way to do this
        if (type.equals(TYPE_BING)) { // hopelessly hardwired
            Log.d(DEBUG_TAG, "bing url " + tileUrl);
            metadataLoaded = false;

            if (async) {
                new AsyncTask<String, Void, Void>() {
                    @Override
                    protected Void doInBackground(String... params) {
                        loadInfo(params[0]);
                        Log.i(DEBUG_TAG, "Meta-data loaded for layer " + getId());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                    }
                }.execute(tileUrl);
            } else
                loadInfo(tileUrl);
            return;
        } else if (type.equals("scanex")) { // hopelessly hardwired
            tileUrl = "http://irs.gis-lab.info/?layers=" + tileUrl.toLowerCase(Locale.US) + "&request=GetTile&z={zoom}&x={x}&y={y}";
            imageFilenameExtension = ".jpg";
            return;
        }

        int extPos = tileUrl.lastIndexOf('.');
        if (extPos >= 0) {
            imageFilenameExtension = tileUrl.substring(extPos);
        }
        // extract switch values
        final String SWITCH_START = "{switch:";
        int switchPos = tileUrl.indexOf(SWITCH_START);
        if (switchPos >= 0) {
            int switchEnd = tileUrl.indexOf('}', switchPos);
            if (switchEnd >= 0) {
                String switchValues = tileUrl.substring(switchPos + SWITCH_START.length(), switchEnd);
                Collections.addAll(subdomains, switchValues.split(","));
                StringBuilder t = new StringBuilder(tileUrl);
                tileUrl = t.replace(switchPos, switchEnd + 1, "{subdomain}").toString();
            }
        }
    }

    /**
     * Munge a name in to something id like
     * 
     * @param name input name String
     * @return a String with an id
     */
    public static String nameToId(final String name) {
        return name.replaceAll("[\\W\\_]", "").toUpperCase(Locale.US);
    }

    /**
     * Get the default tile layer.
     * 
     * @param ctx Android Context.
     * @param async retrieve meta-data async if true
     * @return The default tile layer.
     */
    public static TileLayerServer getDefault(final Context ctx, final boolean async) {
        // ask for an invalid renderer, so we'll get the fallback default
        return get(ctx, "", async);
    }

    /**
     * Parse a geojson format InputStream for imagery configs and add them to backgroundServerList or overlayServerList
     * 
     * @param ctx android context
     * @param source from which source this config is
     * @param writeableDb SQLiteDatabase
     * @param is InputStream to parse
     * @param async obtain meta data async (bing only)
     * @throws IOException
     */
    public static void parseImageryFile(@NonNull Context ctx, @NonNull SQLiteDatabase writeableDb, @NonNull String source, @NonNull InputStream is,
            final boolean async) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        FeatureCollection fc = FeatureCollection.fromJson(sb.toString());
        for (Feature f : fc.getFeatures()) {
            TileLayerServer osmts = geojsonToServer(ctx, f, async);
            if (osmts != null) {
                TileLayerDatabase.addLayer(writeableDb, source, osmts);
            } else {
                Log.w(DEBUG_TAG, "Imagery layer config couldn't be parsed/unsupported");
            }
        }
        TileLayerDatabase.updateSource(writeableDb, source, System.currentTimeMillis());
    }

    /**
     * Get a string with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the string we want to retrieve
     * @return the string or null if it couldb't be found
     */
    @Nullable
    static String getJosnString(@NonNull JsonObject jsonObject, @NonNull String name) {
        JsonElement field = jsonObject.get(name);
        if (field != null) {
            return field.getAsString();
        }
        return null;
    }

    /**
     * Get a boolean with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the boolean we want to retrieve
     * @return the value or false if it couldb't be found
     */
    static boolean getJosnBoolean(@NonNull JsonObject jsonObject, @NonNull String name) {
        JsonElement field = jsonObject.get(name);
        if (field != null) {
            return field.getAsBoolean();
        }
        return false;
    }

    /**
     * Get an int with name from a JsonObject
     * 
     * @param jsonObject the JsonObject
     * @param name the name of the boolean we want to retrieve
     * @param defaultValue the value to use if the int couldn't be found
     * @return the value or defaltValue if it couldb't be found
     */
    static int getJosnInteger(@NonNull JsonObject jsonObject, @NonNull String name, final int defaultValue) {
        JsonElement field = jsonObject.get(name);
        if (field != null) {
            return field.getAsInt();
        }
        return defaultValue;
    }

    /**
     * Create a TileLayerServer instance from a GeoJson Feature
     * 
     * @param ctx Android Context
     * @param f the GeoJosn Feature containing the config
     * @param async if true retrieve meta-info async
     * @return a TileLayerServer instance of null if it couldn't be created
     */
    @Nullable
    private static TileLayerServer geojsonToServer(@NonNull Context ctx, @NonNull Feature f, boolean async) {
        TileLayerServer osmts = null;

        try {

            int tileWidth = DEFAULT_TILE_SIZE;
            int tileHeight = DEFAULT_TILE_SIZE;

            JsonObject properties = f.getProperties();

            List<BoundingBox> boxes = GeoContext.getBoundingBoxes(f);
            int minZoom = getJosnInteger(properties, "min_zoom", DEFAULT_MIN_ZOOM);
            int maxZoom = getJosnInteger(properties, "max_zoom", DEFAULT_MAX_ZOOM);
            Provider provider = new Provider();
            if (boxes.isEmpty()) {
                provider.addCoverageArea(new Provider.CoverageArea(minZoom, maxZoom, null));
            } else {
                for (BoundingBox box : boxes) {
                    provider.addCoverageArea(new Provider.CoverageArea(minZoom, maxZoom, box));
                }
            }

            String type = getJosnString(properties, "type");
            String id = getJosnString(properties, "id");
            String url = getJosnString(properties, "url");
            String name = getJosnString(properties, "name");
            boolean overlay = getJosnBoolean(properties, "overlay");
            boolean defaultLayer = getJosnBoolean(properties, "default");
            int preference = getJosnBoolean(properties, "best") ? PREFERENCE_BEST : PREFERENCE_DEFAULT;

            JsonObject attribution = (JsonObject) properties.get("attribution");
            String termsOfUseUrl = null;
            if (attribution != null) {
                termsOfUseUrl = getJosnString(attribution, "url");
                provider.setAttribution(getJosnString(attribution, "text"));
            }
            String icon = getJosnString(properties, "icon");
            long startDate = -1L;
            long endDate = -1L;
            String dateString = getJosnString(properties, "start_date");
            if (dateString != null) {
                startDate = dateStringToTime(dateString);
            }
            dateString = getJosnString(properties, "end_date");
            if (dateString != null) {
                endDate = dateStringToTime(dateString);
            }

            String proj = null;
            JsonArray projections = (JsonArray) properties.get("available_projections");
            if (projections != null) {
                for (JsonElement p : projections) {
                    String supportedProj = p.getAsString();
                    if (EPSG_3857.equals(supportedProj) || EPSG_900913.equals(supportedProj)) {
                        proj = supportedProj;
                        tileWidth = WMS_TILE_SIZE;
                        tileHeight = WMS_TILE_SIZE;
                        break;
                    }
                }
            }

            if (type == null || url == null || ("wms".equals(type) && proj == null)) {
                Log.w(DEBUG_TAG, "name " + name + " id " + id + " type " + type + " url " + url);
                if ("wms".equals(type)) {
                    Log.w(DEBUG_TAG, "projections: " + projections);
                }
                return null;
            }
            osmts = new TileLayerServer(ctx, id, name, url, type, overlay, defaultLayer, provider, termsOfUseUrl, icon, null, null, minZoom, maxZoom,
                    DEFAULT_MAX_OVERZOOM, tileWidth, tileHeight, proj, preference, startDate, endDate, async);
        } catch (UnsupportedOperationException uoex) {
            Log.e(DEBUG_TAG, "Got " + uoex.getMessage());
        }
        return osmts;
    }

    /**
     * Get the tile server information for a specified tile server id. If the given id cannot be found, a default
     * renderer is selected.
     * 
     * Note: will read the the config files it that hasn't happened yet
     * 
     * @param ctx activity context
     * @param id The internal id of the tile layer, eg "MAPNIK"
     * @param async get meta data asynchronously
     * @return the selected TileLayerServer
     */
    @Nullable
    public static TileLayerServer get(@NonNull final Context ctx, @NonNull final String id, final boolean async) {
        synchronized (serverListLock) {
            if (!ready) {
                TileLayerDatabase db = new TileLayerDatabase(ctx);
                getLists(ctx, db, async);
                db.close();
                if (imageryBlacklist != null && async) {
                    applyBlacklist(imageryBlacklist);
                }
                ready = true;
            }
        }

        if (id == null || "".equals(id)) { // empty id
            return backgroundServerList.get(LAYER_NONE); // nothing works for all layers :-)
        }

        TileLayerServer overlay = overlayServerList.get(id);
        if (overlay != null) {
            return overlay;
        } else {
            TileLayerServer background = backgroundServerList.get(id);
            if (background != null) {
                return background;
            }
        }
        synchronized (serverListLock) {
            // layer couldn't be found in memory, check database
            Log.d(DEBUG_TAG, "Getting layer " + id + " from database");
            TileLayerDatabase db = new TileLayerDatabase(ctx);
            TileLayerServer layer = TileLayerDatabase.getLayer(ctx, db.getReadableDatabase(), id);
            db.close();
            if (layer != null) {
                if (layer.isOverlay()) {
                    overlayServerList.put(id, layer);
                } else {
                    backgroundServerList.put(id, layer);
                }
                return layer;
            }
            Log.e(DEBUG_TAG, "Layer " + id + " null from database");
        }
        // catch all
        return null;
    }

    /**
     * Read a file from assets containing layer configurations and add them to the database
     * 
     * @param ctx Android Context
     * @param writableDb a writable SQLiteDatabase
     * @param newConfig set to true if we are updating an existing database
     * @param async set this to true if running in the foreground or similar
     */
    public static void createOrUpdateFromAssetsSource(@NonNull final Context ctx, @NonNull SQLiteDatabase writableDb, boolean newConfig, final boolean async) {
        Log.d(DEBUG_TAG, "DB not initalized, parsing configuration files");
        AssetManager assetManager = ctx.getAssets();
        long start = System.currentTimeMillis();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                writableDb.beginTransaction();
            }
            // entries in earlier files will not be overwritten by later ones
            if (newConfig) {
                // delete old
                TileLayerDatabase.deleteSource(writableDb, TileLayerDatabase.SOURCE_ELI);
                TileLayerDatabase.addSource(writableDb, TileLayerDatabase.SOURCE_ELI);
            }
            String[] imageryFiles = { Files.FILE_NAME_VESPUCCI_IMAGERY, Files.FILE_NAME_USER_IMAGERY };
            for (String fn : imageryFiles) {
                try {
                    InputStream is = assetManager.open(fn);
                    parseImageryFile(ctx, writableDb, TileLayerDatabase.SOURCE_ELI, is, async);
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "reading conf file " + fn + " got " + e.getMessage());
                    throw e;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                writableDb.setTransactionSuccessful();
            }
        } catch (IOException e) {
            // already logged
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                writableDb.endTransaction();
            }
        }
        Log.d(DEBUG_TAG, " elapsed time " + (System.currentTimeMillis() - start) / 1000);
    }

    /**
     * Read a file from the Vespucci directory containing custom layer configurations and add them to the database
     * 
     * @param ctx Android Context
     * @param writeableDb a writable SQLiteDatabase
     * @param async set this to true if running in the foreground or similar
     */
    public static void createOrUpdateCustomSource(@NonNull final Context ctx, @NonNull SQLiteDatabase writeableDb, final boolean async) {
        long lastDatabaseUpdate = TileLayerDatabase.getSourceUpdate(writeableDb, TileLayerDatabase.SOURCE_CUSTOM);
        long lastUpdateTime = 0L;

        File sdcard = Environment.getExternalStorageDirectory();
        String userImagery = sdcard.getPath() + "/" + Paths.DIRECTORY_PATH_VESPUCCI + "/" + Files.FILE_NAME_USER_IMAGERY;
        Log.i(DEBUG_TAG, "Trying to read custom imagery from " + userImagery);
        try {
            File userImageryFile = new File(userImagery);
            lastUpdateTime = userImageryFile.lastModified();
            boolean newConfig = lastUpdateTime > lastDatabaseUpdate;
            if (lastDatabaseUpdate == 0 || newConfig) {
                try {
                    writeableDb.beginTransaction();
                    if (newConfig) {
                        // delete old
                        TileLayerDatabase.deleteSource(writeableDb, TileLayerDatabase.SOURCE_CUSTOM);
                        TileLayerDatabase.addSource(writeableDb, TileLayerDatabase.SOURCE_CUSTOM);
                    }
                    InputStream is = new FileInputStream(new File(userImagery));
                    parseImageryFile(ctx, writeableDb, TileLayerDatabase.SOURCE_CUSTOM, is, async);
                    writeableDb.setTransactionSuccessful();
                } finally {
                    writeableDb.endTransaction();
                }
            }
        } catch (IOException e) {
            Log.i(DEBUG_TAG, "no custom conf files found");
        }
    }

    /**
     * Read a file from the editor layer index containing layer configurations and update the database with them
     * 
     * @param ctx Android Context
     * @param writeableDb a writable SQLiteDatabase
     * @throws IOException
     */
    public static void updateFromEli(@NonNull final Context ctx, @NonNull SQLiteDatabase writeableDb) throws IOException {
        Log.d(DEBUG_TAG, "Uüdating from editor-layer-index");
        AssetManager assetManager = ctx.getAssets();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                writeableDb.beginTransaction();
            }
            // delete old
            TileLayerDatabase.deleteSource(writeableDb, TileLayerDatabase.SOURCE_ELI);
            TileLayerDatabase.addSource(writeableDb, TileLayerDatabase.SOURCE_ELI);

            // still need to read out base config first
            try {
                InputStream is = assetManager.open(Files.FILE_NAME_VESPUCCI_IMAGERY);
                parseImageryFile(ctx, writeableDb, TileLayerDatabase.SOURCE_ELI, is, true);
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "reading conf files got " + e.getMessage());
            }
            InputStream is = null;
            try {
                Request request = new Request.Builder().url(Urls.ELI).build();
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
                Call eliCall = client.newCall(request);
                Response eliCallResponse = eliCall.execute();
                if (eliCallResponse.isSuccessful()) {
                    ResponseBody responseBody = eliCallResponse.body();
                    is = responseBody.byteStream();
                    parseImageryFile(ctx, writeableDb, TileLayerDatabase.SOURCE_ELI, is, true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        writeableDb.setTransactionSuccessful();
                    }
                    getListsLocked(ctx, writeableDb, true);
                } else {
                    throw new IOException(eliCallResponse.message());
                }
            } finally {
                SavingHelper.close(is);
            }
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                writeableDb.endTransaction();
            }
        }
        MapTilesLayer layer = App.getLogic().getMap().getBackgroundLayer();
        if (layer != null) {
            layer.getTileProvider().update();
        }
    }

    /**
     * Set the in memory lists from the database
     * 
     * @param ctx Android Context
     * @param db an instance of TileLayerDatabase
     * @param populate flag that indicates if we should fully populate the lists or not
     */
    private static void getLists(@NonNull final Context ctx, @NonNull TileLayerDatabase db, boolean populate) {
        getLists(ctx, db.getReadableDatabase(), populate);
    }

    /**
     * Set the in memory lists from the database
     * 
     * @param ctx Android Context
     * @param db the SQLiteDatabase
     * @param populate flag that indicates if we should fully populate the lists or not
     */
    private static void getLists(@NonNull final Context ctx, @NonNull SQLiteDatabase db, boolean populate) {
        long start = System.currentTimeMillis();
        if (populate) {
            overlayServerList = TileLayerDatabase.getAllLayers(ctx, db, true);
            backgroundServerList = TileLayerDatabase.getAllLayers(ctx, db, false);
        } else {
            overlayServerList = new HashMap<String, TileLayerServer>();
            backgroundServerList = new HashMap<String, TileLayerServer>();
            // these three layers have to exist or else we are borked
            TileLayerServer overlay = TileLayerDatabase.getLayer(ctx, db, LAYER_NOOVERLAY);
            overlayServerList.put(LAYER_NOOVERLAY, overlay);
            TileLayerServer background = TileLayerDatabase.getLayer(ctx, db, LAYER_NONE);
            overlayServerList.put(LAYER_NONE, background);
            background = TileLayerDatabase.getLayer(ctx, db, LAYER_MAPNIK);
            overlayServerList.put(LAYER_MAPNIK, background);
        }
        Log.d(DEBUG_TAG, "Generating TileLayer lists took " + (System.currentTimeMillis() - start) / 1000);
    }

    /**
     * Set the in memory lists from the database, locks against concurrent change
     * 
     * @param ctx Android Context
     * @param db the SQLiteDatabase
     * @param populate flag that indicates if we should fully populate the lists or not
     */
    public static void getListsLocked(@NonNull final Context ctx, @NonNull SQLiteDatabase db, boolean populate) {
        synchronized (serverListLock) {
            getLists(ctx, db, populate);
        }
    }

    /**
     * Parse a RFC3339 timestamp into a time value since epoch, ignores non date parts
     * 
     * @param timeStamp the date string to parse
     * @return the time value or -1 if parsing failed
     */
    private static long dateStringToTime(String timeStamp) {
        long result = -1L;
        if (timeStamp != null && !"".equals(timeStamp)) {
            String[] parts = timeStamp.split("T");
            String f = "yyyy-MM-dd";
            try {
                int l = parts[0].length();
                if (l == 4) { // slightly hackish way of determining which format to use
                    f = "yyyy";
                } else if (l < 8) {
                    f = "yyyy-MM";
                }
                Date d = new SimpleDateFormat(f, Locale.US).parse(parts[0]);
                result = d.getTime();
            } catch (ParseException e) {
                Log.e(DEBUG_TAG, "Invalid RFC3339 value (" + f + ") " + timeStamp + " " + e.getMessage());
            }
        }
        return result;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * Check if the meta data for this layer is loaded
     * 
     * @return true if the meta data is loaded
     */
    public boolean isMetadataLoaded() {
        return metadataLoaded;
    }

    /**
     * Get the Tile layer ID.
     * 
     * @return Tile layer ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the tile width.
     * 
     * @return The tile width in pixels.
     */
    public int getTileWidth() {
        checkMetaData();
        return tileWidth;
    }

    /**
     * Get the tile height.
     * 
     * @return The tile height in pixels.
     */
    public int getTileHeight() {
        checkMetaData();
        return tileHeight;
    }

    /**
     * Get the minimum zoom level these tiles are available at.
     * 
     * @return Minimum zoom level for which the tile layer is available.
     */
    public int getMinZoomLevel() {
        checkMetaData();
        return zoomLevelMin;
    }

    /**
     * Throw an exception if the meta-data isn0t loaded
     */
    private void checkMetaData() {
        if (!metadataLoaded) {
            throw new IllegalStateException("metadata not loaded");
        }
    }

    /**
     * Get the maximum zoom level these tiles are available at.
     * 
     * @return Maximum zoom level for which the tile layer is available.
     */
    public int getMaxZoomLevel() {
        checkMetaData();
        // if (providers != null && providers.size() > 0) {
        // zoomLevelMax = 0;
        // BoundingBox bbox = Application.mainActivity.getMap().getViewBox();
        // for (Provider p:providers) {
        // Provider.CoverageArea ca = p.getCoverageArea((bbox.getLeft() + bbox.getWidth()/2)/1E7d, bbox.getCenterLat());
        // if (ca != null && ca.zoomMax > zoomLevelMax)
        // zoomLevelMax = ca.zoomMax;
        // Log.d("OpenStreetMapTileServer","Provider " + p.getAttribution() + " max zoom " + zoomLevelMax);
        // }
        // }
        return getlMaxZoom();
    }

    /**
     * Get the filename extensions that applies to the tile images.
     * 
     * @return Image filename extension, eg ".png".
     */
    public String getImageExtension() {
        return imageFilenameExtension;
    }

    /**
     * Get the branding logo for the tile layer.
     * 
     * Retrieves logos where we have an url asynchronouls
     * 
     * @return The branding logo, or null if there is none.
     */
    public Drawable getLogoDrawable() {
        checkMetaData();
        /**
         * We have an url but haven't got the logo yet retrieve it now for use on next redraw
         */
        if (logoDrawable == null && (logoUrl != null || logoBitmap != null)) {
            if (logoBitmap != null) {
                logoDrawable = scaledBitmap(logoBitmap);
            } else {
                new AsyncTask<String, Void, Void>() {
                    @Override
                    protected Void doInBackground(String... params) {
                        synchronized (TileLayerServer.this) {
                            Drawable cached = logoCache.get(logoUrl);
                            if (cached != NOLOGO && logoUrl != null) { // recheck logoURl
                                if (cached != null) {
                                    logoDrawable = cached;
                                } else {
                                    Log.d(DEBUG_TAG, "getLogoDrawable logoUrl " + logoUrl);
                                    logoDrawable = getLogoFromUrl(logoUrl);
                                    if (logoDrawable == null) {
                                        logoCache.put(logoUrl, NOLOGO);
                                    } else {
                                        logoCache.put(logoUrl, logoDrawable);
                                    }
                                }
                                logoUrl = null;
                            }
                        }
                        return null;
                    }
                }.execute(logoUrl);
            }
        }
        return logoDrawable;
    }

    /**
     * Get the attributions that apply to the given map display.
     * 
     * @param zoom Zoom level of the display.
     * @param area Displayed area to get the attributions of.
     * @return Collections of attributions that apply to the specified area and zoom.
     */
    public Collection<String> getAttributions(final int zoom, final BoundingBox area) {
        checkMetaData();
        Collection<String> ret = new ArrayList<>();
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
     * 
     * @return The End User Terms Of Use URI.
     */
    public String getTouUri() {
        return touUri;
    }

    /**
     * Get the latE7 offset
     * 
     * @param zoomLevel the zoom level we want the offset for
     * @return offset in WGS84, null == no offset
     */
    public Offset getOffset(int zoomLevel) {
        if (zoomLevel < zoomLevelMin) {
            return null;
        }
        if (zoomLevel > getlMaxZoom()) {
            return offsets[getlMaxZoom() - zoomLevelMin];
        }
        return offsets[zoomLevel - zoomLevelMin];
    }

    /**
     * Set the lat offset for one specific zoom
     * 
     * @param zoomLevel zoom level to set the offset for
     * @param offsetLon offset in lon direction in WGS84
     * @param offsetLat offset in lat direction in WGS84
     */
    public void setOffset(int zoomLevel, double offsetLon, double offsetLat) {
        Log.d("OpenStreetMapTileServer", "setOffset " + zoomLevel + " " + offsetLon + " " + offsetLat);
        zoomLevel = Math.max(zoomLevel, zoomLevelMin); // clamp to min/max values
        zoomLevel = Math.min(zoomLevel, getlMaxZoom());
        if (offsets[zoomLevel - zoomLevelMin] == null) {
            offsets[zoomLevel - zoomLevelMin] = new Offset();
        }
        offsets[zoomLevel - zoomLevelMin].setDeltaLon(offsetLon);
        offsets[zoomLevel - zoomLevelMin].setDeltaLat(offsetLat);
    }

    /**
     * Set the offset for all zoom levels
     * 
     * @param offsetLon offset in lon direction in WGS84
     * @param offsetLat offset in lat direction in WGS84
     */
    public void setOffset(double offsetLon, double offsetLat) {
        for (int i = 0; i < offsets.length; i++) {
            if (offsets[i] == null)
                offsets[i] = new Offset();
            offsets[i].setDeltaLon(offsetLon);
            offsets[i].setDeltaLat(offsetLat);
        }
    }

    /**
     * Set the offset for a range of zoom levels
     * 
     * @param startZoom start of zoom range
     * @param endZoom end of zoom range
     * @param offsetLon offset in lon direction in WGS84
     * @param offsetLat offset in lat direction in WGS84
     */
    public void setOffset(int startZoom, int endZoom, double offsetLon, double offsetLat) {
        for (int z = startZoom; z <= endZoom; z++) {
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
     * Return the name for this layer
     * 
     * @return the current name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Set the name for this layer
     * 
     * @param name the name to use
     */
    public void setName(@NonNull String name) {
        this.name = name;
    }

    /**
     * Check if this layer is "read only" aka a mbtiles file
     * 
     * @return true if read only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Return a sorted list of tile servers
     * 
     * Takes the preference and end date in to account
     * 
     * @param filtered if true only return those layers with a coverage area that overlaps with the supplied bounding
     *            box
     * @param servers input list of servers to sort and potentially filter
     * @param box bounding box that we are interested in
     * @return list of tile servers
     */
    @NonNull
    private static List<TileLayerServer> getServersFilteredSorted(boolean filtered, @NonNull Map<String, TileLayerServer> servers, @Nullable BoundingBox box) {
        TileLayerServer noneLayer = null;
        List<TileLayerServer> list = new ArrayList<>();
        for (TileLayerServer osmts : servers.values()) {
            if (filtered && box != null) {
                if (!osmts.covers(box)) {
                    continue;
                }
            }
            // add this after sorting
            if (LAYER_NONE.equals(osmts.id) || LAYER_NOOVERLAY.equals(osmts.id)) {
                noneLayer = osmts;
                continue;
            }
            // add the rest now
            list.add(osmts);
        }
        // sort according to preference, end date and default layer flag in the future we might take bb size in to
        // account
        Collections.sort(list, new Comparator<TileLayerServer>() {
            @Override
            public int compare(TileLayerServer t1, TileLayerServer t2) {
                if (t1.preference < t2.preference) {
                    return 1;
                } else if (t1.preference > t2.preference) {
                    return -1;
                }
                if (t1.endDate == t2.endDate || (t1.endDate < 0 && t2.endDate < 0)) {
                    if (t1.defaultLayer != t2.defaultLayer) {
                        return t2.defaultLayer ? 1 : -1;
                    }
                    return t1.getName().compareToIgnoreCase(t2.getName()); // alphabetic
                } else {
                    // assumption no end date == ongoing
                    return t1.endDate > 0 && (t1.endDate < t2.endDate || t2.endDate < 0) ? 1 : -1;
                }
            }
        });
        // add NONE
        if (noneLayer != null) {
            list.add(0, noneLayer);
        }
        return list;
    }

    /**
     * Test if the bounding box is covered by this tile source
     * 
     * @param box the bounding box we want to test
     * @return true if covered or no coverage information
     */
    public boolean covers(BoundingBox box) {
        if (!providers.isEmpty()) {
            for (Provider p : providers) {
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
     * 
     * @param box the bounding box we want to get the max zoom for
     * @return maximum zoom for this area, -1 if nothing found
     */
    public int getMaxZoom(BoundingBox box) {
        int max = 0;
        if (!providers.isEmpty()) {
            for (Provider p : providers) {
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
     * Get all the available tile layer IDs.
     * 
     * @param box bounding box to test coverage against
     * @param filtered only return servers that overlap/intersect with the bounding box
     * @return available tile layer IDs.
     */
    @NonNull
    public static String[] getIds(@Nullable BoundingBox box, boolean filtered) {
        List<String> ids = new ArrayList<>();
        List<TileLayerServer> list = getServersFilteredSorted(filtered, backgroundServerList, box);
        for (TileLayerServer t : list) {
            ids.add(t.id);
        }
        String[] idArray = new String[ids.size()];
        ids.toArray(idArray);
        return idArray;
    }

    /**
     * Get all the available tile layer names.
     * 
     * @param box bounding box to test coverage against
     * @param filtered only return servers that overlap/intersect with the bounding box
     * @return available tile layer names.
     */
    @NonNull
    public static String[] getNames(@Nullable BoundingBox box, boolean filtered) {
        ArrayList<String> names = new ArrayList<>();
        for (String key : getIds(box, filtered)) {
            TileLayerServer osmts = backgroundServerList.get(key);
            names.add(osmts.name);
        }
        String[] result = new String[names.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = names.get(i);
        return result;
    }

    /**
     * Get tile server names from list of ids
     * 
     * @param ids array containing the ids
     * @return array containing the names
     */
    public static String[] getNames(String[] ids) {
        ArrayList<String> names = new ArrayList<>();
        for (String key : ids) {
            TileLayerServer osmts = backgroundServerList.get(key);
            names.add(osmts.name);
        }
        String[] result = new String[names.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = names.get(i);
        return result;
    }

    /**
     * Get all the available overlay tile layer IDs.
     * 
     * @param box bounding box to test coverage against
     * @param filtered only return servers that overlap/intersect with the bounding box
     * @return available tile layer IDs.
     */
    @NonNull
    public static String[] getOverlayIds(@Nullable BoundingBox box, boolean filtered) {
        List<String> ids = new ArrayList<>();
        List<TileLayerServer> list = getServersFilteredSorted(filtered, overlayServerList, box);
        for (TileLayerServer t : list) {
            ids.add(t.id);
        }
        String[] idArray = new String[ids.size()];
        ids.toArray(idArray);
        return idArray;
    }

    /**
     * Get all the available overlay tile layer names.
     * 
     * @param box bounding box to test coverage against
     * @param filtered only return servers that overlap/intersect with the bounding box
     * @return available tile layer names.
     */
    @NonNull
    public static String[] getOverlayNames(@Nullable BoundingBox box, boolean filtered) {
        ArrayList<String> names = new ArrayList<>();
        for (String key : getIds(box, filtered)) {
            TileLayerServer osmts = overlayServerList.get(key);
            names.add(osmts.name);
        }
        String[] result = new String[names.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = names.get(i);
        return result;
    }

    /**
     * Get tile server names from list of ids
     * 
     * @param ids id list
     * @return list of names
     */
    @NonNull
    public static String[] getOverlayNames(@NonNull String[] ids) {
        ArrayList<String> names = new ArrayList<>();
        for (String key : ids) {
            TileLayerServer osmts = overlayServerList.get(key);
            names.add(osmts.name);
        }
        String[] result = new String[names.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = names.get(i);
        return result;
    }

    /**
     * Replace a parameter of form {param} in a string
     * 
     * Slow and only for use with parameters that don't need to be set more than once
     * 
     * @param s input String
     * @param param parameter to replace
     * @param value value to replace param with
     * @return the string with the parameter replaced
     */
    private static String replaceParameter(@NonNull final String s, @NonNull final String param, @NonNull final String value) {
        String result = s;
        // replace "${param}"
        // not used in imagery index result = result.replaceFirst("\\$\\{" + param + "\\}", value);
        // replace "$param"
        // not used in imagery index result = result.replaceFirst("\\$" + param, value);
        // replace "{param}"
        result = result.replaceFirst("\\{" + param + "\\}", value);
        return result;
    }

    /**
     * Replace some specific parameters that we use. Currently culture and bingapikey
     * 
     * @param s the input string
     * @return the string with replaced parameters
     */
    private String replaceGeneralParameters(@NonNull final String s) {
        Resources r = ctx.getResources();
        final Locale l = r.getConfiguration().locale;
        String result = s;
        result = replaceParameter(result, "culture", l.getLanguage().toLowerCase(Locale.US) + "-" + l.getCountry().toLowerCase(Locale.US));
        try {
            result = replaceParameter(result, "bingapikey", r.getString(R.string.bingapikey));
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "replacing bingapi key failed: " + ex.getMessage());
        }
        return result;
    }

    private static final int BASE  = 0;
    private static final int PARAM = 1;

    /**
     * Allocate the following just once
     */
    StringBuilder builder    = new StringBuilder(100); // 100 is just an estimate to avoid re-allocating
    StringBuilder param      = new StringBuilder();
    StringBuilder quadKey    = new StringBuilder();
    StringBuilder boxBuilder = new StringBuilder();

    /**
     * Get the URL that can be used to obtain the image of the given tile.
     * 
     * This is 5-100 times faster than the previous implementation.
     * 
     * @param aTile The tile to get the URL for.
     * @return URL of the given tile.
     */
    public synchronized String getTileURLString(final MapTile aTile) {
        checkMetaData();
        builder.setLength(0);
        int state = BASE;
        for (char c : tileUrl.toCharArray()) {
            if (state == BASE) {
                if (c == '{') {
                    state = PARAM;
                    param.setLength(0); // reset
                } else {
                    builder.append(c);
                }
            } else {
                if (c == '}') {
                    state = BASE;
                    String p = param.toString();
                    switch (p) {
                    case "x":
                        builder.append(Integer.toString(aTile.x));
                        break;
                    case "y":
                        builder.append(Integer.toString(aTile.y));
                        break;
                    case "z":
                        builder.append(Integer.toString(aTile.zoomLevel));
                        break;
                    case "zoom":
                        builder.append(Integer.toString(aTile.zoomLevel));
                        break;
                    case "ty":
                    case "-y":
                        int ymax = 1 << aTile.zoomLevel;
                        int y = ymax - aTile.y - 1;
                        builder.append(Integer.toString(y));
                        break;
                    case "quadkey":
                        builder.append(quadTree(aTile));
                        break;
                    case "subdomain":
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
                        break;
                    case "proj": // WMS support from here on
                        builder.append(proj);
                        break;
                    case "width":
                        builder.append(Integer.toString(tileWidth));
                        break;
                    case "height":
                        builder.append(Integer.toString(tileHeight));
                        break;
                    case "bbox":
                        builder.append(wmsBox(aTile));
                        break;
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
     * 
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
     * Converts TMS tile coordinates to WMS bounding box for EPSG:2857/900913
     * 
     * @param aTile The tile coordinates to convert
     * @return a WMS bounding box string
     */
    String wmsBox(final MapTile aTile) {
        int ymax = 1 << aTile.zoomLevel;
        int y = ymax - aTile.y - 1;
        boxBuilder.setLength(0);
        boxBuilder.append(GeoMath.tile2lonMerc(tileWidth, aTile.x, aTile.zoomLevel));
        boxBuilder.append(',');
        boxBuilder.append(GeoMath.tile2latMerc(tileHeight, y, aTile.zoomLevel));
        boxBuilder.append(',');
        boxBuilder.append(GeoMath.tile2lonMerc(tileWidth, aTile.x + 1, aTile.zoomLevel));
        boxBuilder.append(',');
        boxBuilder.append(GeoMath.tile2latMerc(tileHeight, y + 1, aTile.zoomLevel));
        return boxBuilder.toString();
    }

    /**
     * Get the maximum we over zoom for this layer
     * 
     * @return the maximum number of additional zoom levels we overzoom
     */
    public int getMaxOverZoom() {
        return maxOverZoom;
    }

    /**
     * This is essentially the code in in the reference implementation see
     * 
     * https://trac.openstreetmap.org/browser/subversion/applications/editors/josm/plugins/imagery_offset_db/src/iodb/ImageryIdGenerator.java#L24
     * 
     * @return the id for a imagery offset database query
     */
    public String getImageryOffsetId() {
        if (imageryOffsetId != null) {
            return imageryOffsetId;
        }
        String url = originalUrl;
        if (url == null) {
            return null;
        }

        // predefined layers
        if (id.equals(LAYER_BING)) {
            return TYPE_BING;
        }

        if (url.contains("irs.gis-lab.info")) {
            return "scanex_irs";
        }

        if (id.equalsIgnoreCase("Mapbox")) {
            return "mapbox";
        }

        // Remove protocol
        int i = url.indexOf("://");
        if (i == -1) { // TODO more sanity checks
            return "invalid_URL";
        }
        url = url.substring(i + 3);

        // Split URL into address and query string
        i = url.indexOf('?');
        String query = "";
        if (i > 0) {
            query = url.substring(i);
            url = url.substring(0, i);
        }

        TreeMap<String, String> qparams = new TreeMap<>();
        String[] qparamsStr = query.length() > 1 ? query.substring(1).split("&") : new String[0];
        for (String param : qparamsStr) {
            String[] kv = param.split("=");
            kv[0] = kv[0].toLowerCase(Locale.US);
            // TMS: skip parameters with variable values and Mapbox's access token
            if ((kv.length > 1 && kv[1].indexOf('{') >= 0 && kv[1].indexOf('}') > 0) || kv[0].equals("access_token")) {
                continue;
            }
            qparams.put(kv[0].toLowerCase(Locale.US), kv.length > 1 ? kv[1] : null);
        }

        // Reconstruct query parameters
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> qk : qparams.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            } else if (query.length() > 0) {
                sb.append('?');
            }
            sb.append(qk.getKey()).append('=').append(qk.getValue());
        }
        query = sb.toString();

        // TMS: remove /{zoom} and /{y}.png parts
        url = url.replaceAll("\\/\\{[^}]+\\}(?:\\.\\w+)?", "");
        // TMS: remove variable parts
        url = url.replaceAll("\\{[^}]+\\}", "");
        while (url.contains("..")) {
            url = url.replace("..", ".");
        }
        if (url.startsWith(".")) {
            url = url.substring(1);
        }
        imageryOffsetId = url + query;

        return imageryOffsetId;
    }

    /**
     * Remove all background and overlay entries that match the supplied blacklist
     * 
     * @param blacklist list of servers that should be removed
     */
    public static void applyBlacklist(List<String> blacklist) {
        // first compile the regexs
        List<Pattern> patterns = new ArrayList<>();
        for (String regex : blacklist) {
            patterns.add(Pattern.compile(regex));
        }
        synchronized (serverListLock) {
            for (Pattern p : patterns) {
                if (backgroundServerList != null) {
                    for (String key : new TreeSet<>(backgroundServerList.keySet())) { // shallow copy
                        TileLayerServer osmts = backgroundServerList.get(key);
                        Matcher m = p.matcher(osmts.tileUrl);
                        if (m.find()) {
                            backgroundServerList.remove(key);
                            Log.d(DEBUG_TAG, "Removed background tile layer " + key);
                        }
                    }
                }
                if (overlayServerList != null) {
                    for (String key : new TreeSet<>(overlayServerList.keySet())) { // shallow copy
                        TileLayerServer osmts = overlayServerList.get(key);
                        Matcher m = p.matcher(osmts.tileUrl);
                        if (m.find()) {
                            overlayServerList.remove(key);
                            Log.d(DEBUG_TAG, "Removed overlay tile layer " + key);
                        }
                    }
                }
            }
        }
    }

    public static void setBlacklist(List<String> bl) {
        imageryBlacklist = bl;
    }

    /**
     * Getter for the start date
     * 
     * @return the start date as ms since the epoch
     */
    public long getStartDate() {
        return startDate;
    }

    /**
     * Getter for the end date
     * 
     * @return the end date as ms since the epoch
     */
    public long getEndDate() {
        return endDate;
    }

    /**
     * Return the attribution string of the 1st provider
     * 
     * Assumption is that in the simple case there is only one provider
     * 
     * @return a string containing attribution information or null if none
     */
    @Nullable
    public String getAttribution() {
        if (!providers.isEmpty()) {
            return providers.get(0).getAttribution();
        }
        return null;
    }

    /**
     * Return the coverage areas of the 1st provider
     * 
     * Assumption is that in the simple case there is only one provider
     * 
     * @return a List of Provider.CoverageArea
     */
    @Nullable
    public List<Provider.CoverageArea> getCoverage() {
        if (!providers.isEmpty()) {
            return providers.get(0).coverageAreas;
        }
        return null;
    }

    /**
     * Check the overlay flag
     * 
     * @return true if this is an overlay
     */
    public boolean isOverlay() {
        return overlay;
    }

    /**
     * Set the overlay flag
     * 
     * @param overlay if true the layer will be considered for overlaying
     */
    void setOverlay(boolean overlay) {
        this.overlay = overlay;
    }

    public boolean isDefaultLayer() {
        return defaultLayer;
    }

    /**
     * Return the projection that should be used if this is a WMS server
     * 
     * @return the projection as a String or null if not a WMS server
     */
    @Nullable
    public String getProj() {
        return proj;
    }

    /**
     * Get the preference/ranking for this layer
     * 
     * @return a numeric indication of preference, higher is better, 0 the default
     */
    public int getPreference() {
        return preference;
    }

    /**
     * Get the url for the logo
     * 
     * @return a string with the url or null if none exists
     */
    @Nullable
    public String getLogoUrl() {
        return logoUrl;
    }

    /**
     * Get the bitmap for the logo
     * 
     * @return a Bitmap or null if none exists
     */
    @Nullable
    public Bitmap getLogo() {
        return logoBitmap;
    }

    /**
     * Get the processed tile url
     * 
     * @return the processed (non-tile specific replacement made) tile url
     */
    @NonNull
    public String getTileUrl() {
        return tileUrl;
    }

    /**
     * ¨ Get the tile url
     * 
     * @return the tile url
     */
    @NonNull
    public String getOriginalTileUrl() {
        return originalUrl;
    }

    /**
     * Set the unprocessed url
     * 
     * @param originalUrl the unprocessed url
     */
    void setOriginalTileUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    @Override
    public String toString() {
        return "ID: " + id + " Name " + name + " maxZoom " + getlMaxZoom() + " Tile URL " + tileUrl;
    }

    /**
     * Get the type "tms", "wms", "bing", "scanex" of the layer
     * 
     * @return the layer type
     */
    public String getType() {
        return type;
    }

    /**
     * Set provider list to a single Provider
     * 
     * @param provider Provider to use
     */
    public void setProvider(Provider provider) {
        providers.clear();
        providers.add(provider);
    }

    /**
     * Get a BoundingBox that covers all of the layers CoverageAreas
     * 
     * @return a BoundingBox covering all CoverageAreas
     */
    public BoundingBox getOverallCoverage() {
        if (providers.isEmpty() || providers.get(0).coverageAreas == null || providers.get(0).coverageAreas.isEmpty()) {
            return ViewBox.getMaxMercatorExtent();
        }
        BoundingBox box = null;
        for (Provider provider : providers) {
            for (CoverageArea coverage : provider.coverageAreas) {
                if (box == null) {
                    box = new BoundingBox(coverage.bbox);
                } else {
                    box.union(coverage.bbox);
                }
            }
        }
        return box;
    }

    /**
     * @return the zoomLevelMin
     */
    public int getMinZoom() {
        return zoomLevelMin;
    }

    /**
     * @param zoomLevelMin the zoomLevelMin to set
     */
    public void setMinZoom(int zoomLevelMin) {
        this.zoomLevelMin = zoomLevelMin;
    }

    /**
     * @return the zoomLevelMax
     */
    public int getlMaxZoom() {
        return zoomLevelMax;
    }

    /**
     * @param zoomLevelMax the zoomLevelMax to set
     */
    public void setMaxZoom(int zoomLevelMax) {
        this.zoomLevelMax = zoomLevelMax;
    }
}
