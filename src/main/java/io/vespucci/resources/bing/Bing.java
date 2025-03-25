package io.vespucci.resources.bing;

import java.io.IOException;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import io.vespucci.contract.FileExtensions;
import io.vespucci.osm.BoundingBox;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.resources.TileLayerSource.Provider;
import io.vespucci.resources.TileLayerSource.Provider.CoverageArea;

/**
 * Utilities for handling Bing meta information
 * 
 * @author Andrew Gregory
 * @author Simon Poole
 *
 */
public final class Bing {

    private static final String DEBUG_TAG = Bing.class.getSimpleName().substring(0, Math.min(23, Bing.class.getSimpleName().length()));

    /**
     * Private constructor to disable instantiation
     */
    private Bing() {
        // private
    }

    /**
     * Create a coverage area given Bing XML data.
     * 
     * @param parser The XML parser.
     * @return a CoverageArea
     * @throws IOException if there was an IO error
     * @throws XmlPullParserException If there was a problem parsing the XML.
     * @throws NumberFormatException If any of the numbers couldn't be parsed.
     */
    @NonNull
    private static CoverageArea coverageArea(XmlPullParser parser) throws IOException, NumberFormatException, XmlPullParserException {
        int eventType;
        int zoomMin = 0;
        int zoomMax = 20;
        double bottom = 0.0d;
        double top = 0.0d;
        double left = 0.0d;
        double right = 0.0d;
        BoundingBox bbox;

        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.END_TAG && "CoverageArea".equals(tagName)) {
                break; // the loop
            }
            if (eventType == XmlPullParser.START_TAG && parser.next() == XmlPullParser.TEXT) {
                switch (tagName) {
                case "ZoomMin":
                    zoomMin = Integer.parseInt(parser.getText().trim());
                    break;
                case "ZoomMax":
                    zoomMax = Integer.parseInt(parser.getText().trim());
                    break;
                case "NorthLatitude":
                    top = Double.parseDouble(parser.getText().trim());
                    break;
                case "SouthLatitude":
                    bottom = Double.parseDouble(parser.getText().trim());
                    break;
                case "EastLongitude":
                    right = Double.parseDouble(parser.getText().trim());
                    break;
                case "WestLongitude":
                    left = Double.parseDouble(parser.getText().trim());
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown field " + tagName);
                }
            }
        }
        bbox = new BoundingBox(left, bottom, right, top);

        return new CoverageArea(zoomMin, zoomMax, bbox);
    }

    /**
     * Create a new Provider from Bing XML data.
     * 
     * @param parser The XML parser.
     * @return a Provider
     * @throws IOException If there was a problem parsing the XML.
     * @throws XmlPullParserException If there was a problem parsing the XML.
     */
    @NonNull
    private static Provider provider(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        Provider result = new Provider();

        int eventType;
        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.END_TAG && "ImageryProvider".equals(tagName)) {
                break;
            }
            if (eventType == XmlPullParser.START_TAG) {
                if ("Attribution".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    result.setAttribution(parser.getText().trim());
                }
                if ("CoverageArea".equals(tagName)) {
                    try {
                        result.getCoverageAreas().add(coverageArea(parser));
                    } catch (Exception x) {
                        // do nothing
                    }
                }
            }
        }
        return result;
    }

    /**
     * load meta information from Bing (or from other sources using the same format)
     * 
     * @param ctx am Android Context
     * @param source the TileLayerSource we will set the information in
     * @param parser the Xml parser
     * @throws NumberFormatException
     * @throws XmlPullParserException
     * @throws IOException
     */
    public static void loadMeta(@NonNull Context ctx, @NonNull TileLayerSource source, @NonNull XmlPullParser parser)
            throws NumberFormatException, XmlPullParserException, IOException {
        int eventType;
        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("BrandLogoUri".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    String brandLogoUri = parser.getText().trim();
                    if (brandLogoUri.startsWith("@drawable/")) {
                        // internal URL
                        int resid = ctx.getResources().getIdentifier(brandLogoUri.substring(10), "drawable", "de.blau.android");
                        source.setLogoDrawable(ContextCompat.getDrawable(ctx, resid));
                    } else {
                        // assume Internet URL
                        source.setLogoDrawable(source.getLogoFromUrl(brandLogoUri));
                    }
                }
                if ("ImageUrl".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    String tileUrl = parser.getText().trim();
                    int extPos = tileUrl.lastIndexOf(".jpeg");
                    if (extPos >= 0) {
                        source.setImageExtension(FileExtensions.JPG);
                    }
                    // extract switch values
                    final String SWITCH_START = "{switch:";
                    int switchPos = tileUrl.indexOf(SWITCH_START);
                    if (switchPos >= 0) {
                        int switchEnd = tileUrl.indexOf('}', switchPos);
                        if (switchEnd >= 0) {
                            String switchValues = tileUrl.substring(switchPos + SWITCH_START.length(), switchEnd);
                            Collections.addAll(source.getSubdomains(), switchValues.split(","));
                            StringBuilder t = new StringBuilder(tileUrl);
                            tileUrl = t.replace(switchPos, switchEnd + 1, "{subdomain}").toString();
                        }
                    }
                    source.setTileUrl(tileUrl);
                }
                if ("string".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    source.getSubdomains().add(parser.getText().trim());
                }
                if ("ImageWidth".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    source.setTileWidth(Integer.parseInt(parser.getText().trim()));
                }
                if ("ImageHeight".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    source.setTileHeight(Integer.parseInt(parser.getText().trim()));
                }
                if ("ZoomMin".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    source.setMinZoom(Integer.parseInt(parser.getText().trim()));
                }
                if ("ZoomMax".equals(tagName) && parser.next() == XmlPullParser.TEXT) {
                    source.setMaxZoom(Integer.parseInt(parser.getText().trim()));
                }
                if ("ImageryProvider".equals(tagName)) {
                    try {
                        source.getProviders().add(Bing.provider(parser));
                    } catch (IOException | XmlPullParserException e) {
                        // if the provider can't be parsed, we can't do
                        // much about it
                        Log.e(DEBUG_TAG, "ImageryProvider problem", e);
                    }
                }
            }
        }
    }
}
