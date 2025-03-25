package io.vespucci.resources;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.vespucci.exception.UnsupportedFormatException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.util.GeoMath;
import io.vespucci.util.Util;
import io.vespucci.util.Version;

/**
 * Minimal class to hold a list of layers parsed from a WMS servers GetCapabilities request response,
 * 
 * This ignores everything we currently don't want or need in vespucci
 * 
 * @author Simon Poole
 *
 */
public class WmsCapabilities {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WmsCapabilities.class.getSimpleName().length());
    private static final String DEBUG_TAG = WmsCapabilities.class.getSimpleName().substring(0, TAG_LEN);

    private static final String WMS_1_3_0 = "1.3.0";

    private static final String ONE                        = "1";
    private static final String TRUE                       = "true";
    private static final String QUERYABLE                  = "queryable";
    private static final String VERSION                    = "version";
    private static final String CAPABILITY                 = "Capability";
    private static final String WMS_CAPABILITIES           = "WMS_Capabilities";
    private static final String WMT_MS_CAPABILITIES        = "WMT_MS_Capabilities";
    private static final String BOUNDING_BOX               = "BoundingBox";
    private static final String LAYER                      = "Layer";
    private static final String STYLE                      = "Style";
    private static final String CRS                        = "CRS";
    private static final String SRS                        = "SRS";
    private static final String ABSTRACT                   = "Abstract";
    private static final String NAME                       = "Name";
    private static final String TITLE                      = "Title";
    private static final String FORMAT                     = "Format";
    private static final String WEST_BOUND_LONGITUDE       = "westBoundLongitude";
    private static final String EAST_BOUND_LONGITUDE       = "eastBoundLongitude";
    private static final String SOUTH_BOUND_LATITUDE       = "southBoundLatitude";
    private static final String NORTH_BOUND_LATITUDE       = "northBoundLatitude";
    private static final String EX_GEOGRAPHIC_BOUNDING_BOX = "EX_GeographicBoundingBox";
    private static final String MIN_SCALE_DENOMINATOR      = "MinScaleDenominator";
    private static final String ATTRIBUTION                = "Attribution";
    private static final String REQUEST                    = "Request";
    private static final String GETMAP                     = "GetMap";
    private static final String GET                        = "Get";
    private static final String MAXY_ATTR                  = "maxy";
    private static final String MAXX_ATTR                  = "maxx";
    private static final String MINY_ATTR                  = "miny";
    private static final String MINX_ATTR                  = "minx";
    private static final String ONLINE_RESOURCE            = "OnlineResource";
    private static final String XLINK_HREF_ATTR            = "xlink:href";

    private static final String NONAME = "nn";

    private static final String IMAGE_BMP       = "image/bmp";
    private static final String IMAGE_PNG       = "image/png";
    private static final String IMAGE_PNG8      = "image/png8";
    public static final String  IMAGE_JPEG      = "image/jpeg";
    private static final String IMAGE_JPEG_PNG  = "image/vnd.jpeg-png";
    private static final String IMAGE_JPEG_PNG8 = "image/vnd.jpeg-png8";

    // order is most preferred to least
    public static final List<String> FORMAT_PREFERENCE = Collections
            .unmodifiableList(Arrays.asList(IMAGE_JPEG_PNG8, IMAGE_JPEG_PNG, IMAGE_PNG8, IMAGE_PNG, IMAGE_JPEG, IMAGE_BMP));

    enum State {
        BASE, LAYER, STYLE, EXGEOGRAPHICBOUNDINGBOX, ATTRIBUTION, CAPABILITY, REQUEST, GETMAP, GET
    }

    public class Layer {
        BoundingBox extent;
        String      title;
        String      name;
        String      proj;
        String      description;
        Version     wmsVersion;
        String      format;
        double      gsd;

        /**
         * Get a GetMap url suitable for use in Vespucci
         * 
         * @param baseUrl the base url
         * @return a GetMap url
         */
        @NonNull
        String getTileUrl(@NonNull String baseUrl) {
            return Util.appendQuery(baseUrl, String.format(
                    "FORMAT=%s&TRANSPARENT=TRUE&VERSION=%s&SERVICE=WMS&REQUEST=GetMap&LAYERS=%s&STYLES=&%s=%s&WIDTH={width}&HEIGHT={height}&BBOX={bbox}",
                    format, wmsVersion.toString(), name, is130(wmsVersion) ? CRS : SRS, proj));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append(title);
            builder.append("\n\t");
            builder.append(wmsVersion);
            builder.append(" ");
            builder.append(proj);
            if (extent != null) {
                builder.append("\n\t[");
                builder.append(extent.toPrettyString());
                builder.append("]");
            }
            builder.append("\n\t");
            builder.append(getTileUrl(""));
            return builder.toString();
        }
    }

    private class LayerTemp {
        boolean       group;
        String        title;
        String        name;
        final boolean queryable;
        String        description;
        String        crs;
        String        boxCrs;
        BigDecimal    minx;
        BigDecimal    miny;
        BigDecimal    maxx;
        BigDecimal    maxy;
        double        gsd;        // misnomer

        /**
         * Construct a new temporary container for layer information
         * 
         * @param queryable set to true if the queryable attribute is 1
         */
        LayerTemp(boolean queryable, @Nullable LayerTemp parent) {
            this.queryable = queryable;
            if (parent != null) {
                crs = parent.crs;
            }
        }
    }

    final List<Layer> layers = new ArrayList<>();

    private String  tileFormat;
    private String  getMapUrl;
    private Version wmsVersion;

    /**
     * Construct a new container for layers parsed from a WMS getCapabilities request response
     * 
     * @param is the InputStram to parse
     * @throws ParserConfigurationException on parse issues
     * @throws SAXException on parser issues
     * @throws IOException if reading the InputStream fails
     */
    public WmsCapabilities(@NonNull InputStream is) throws ParserConfigurationException, SAXException, IOException {
        parse(is);
    }

    /**
     * Parse the response from a WMS getCapabilities request
     * 
     * Will handle both 1.1.1 and 1.3 versions, the layer hierarchy is flattened with the hierarchy reflected in the the
     * layer title
     * 
     * @param input the InputStream to parse
     * @throws ParserConfigurationException on parse issues
     * @throws SAXException on parser issues
     * @throws IOException if reading the InputStream fails
     */
    private void parse(@NonNull InputStream input) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance(); // NOSONAR
        SAXParser saxParser = factory.newSAXParser();

        saxParser.parse(input, new DefaultHandler() {
            State                    currentState = State.BASE;
            Deque<State>             stateStack   = new ArrayDeque<>();
            private Deque<LayerTemp> layerStack   = new ArrayDeque<>();

            StringBuilder buffer = null;

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                LayerTemp current = layerStack.peek();
                switch (currentState) {
                case LAYER:
                    switch (name) {
                    case TITLE:
                    case NAME:
                    case ABSTRACT:
                    case CRS:
                    case SRS:
                    case MIN_SCALE_DENOMINATOR:
                        buffer = new StringBuilder();
                        break;
                    case STYLE:
                        stateStack.push(currentState);
                        currentState = State.STYLE;
                        break;
                    case ATTRIBUTION:
                        stateStack.push(currentState);
                        currentState = State.ATTRIBUTION;
                        break;
                    case LAYER:
                        current.group = true;
                        stateStack.push(currentState);
                        layerStack.push(new LayerTemp(isQueryable(attr), current));
                        break;
                    case BOUNDING_BOX:
                        String tempCrs = attr.getValue(is130(wmsVersion) ? CRS : SRS);
                        if (TileLayerSource.EPSG_4326.equals(tempCrs)
                                || (TileLayerSource.is3857compatible(tempCrs) && !TileLayerSource.EPSG_4326.equals(current.boxCrs))) {
                            try {
                                current.minx = new BigDecimal(attr.getValue(MINX_ATTR));
                                current.miny = new BigDecimal(attr.getValue(MINY_ATTR));
                                current.maxx = new BigDecimal(attr.getValue(MAXX_ATTR));
                                current.maxy = new BigDecimal(attr.getValue(MAXY_ATTR));
                                current.boxCrs = tempCrs;
                            } catch (NumberFormatException e) {
                                Log.e(DEBUG_TAG, "Error in bounding box " + e.getMessage());
                            }
                        }
                        break;
                    case EX_GEOGRAPHIC_BOUNDING_BOX:
                        stateStack.push(currentState);
                        currentState = State.EXGEOGRAPHICBOUNDINGBOX;
                        break;
                    default:
                        // ignore
                    }
                    break;
                case EXGEOGRAPHICBOUNDINGBOX:
                    switch (name) {
                    case WEST_BOUND_LONGITUDE:
                    case EAST_BOUND_LONGITUDE:
                    case SOUTH_BOUND_LATITUDE:
                    case NORTH_BOUND_LATITUDE:
                        buffer = new StringBuilder();
                        break;
                    default:
                        // ignore
                    }
                    break;
                case STYLE:
                    // ignored
                    break;
                case BASE:
                    switch (name) {
                    case WMT_MS_CAPABILITIES:
                    case WMS_CAPABILITIES:
                        wmsVersion = new Version(attr.getValue(VERSION));
                        break;
                    case CAPABILITY:
                        stateStack.push(currentState);
                        currentState = State.CAPABILITY;
                        break;
                    default:
                        // Ignored
                    }
                    break;
                case CAPABILITY:
                    switch (name) {
                    case FORMAT:
                        buffer = new StringBuilder();
                        break;
                    case REQUEST:
                        stateStack.push(currentState);
                        currentState = State.REQUEST;
                        break;
                    case LAYER:
                        stateStack.push(currentState);
                        currentState = State.LAYER;
                        layerStack.push(new LayerTemp(isQueryable(attr), current));
                        break;
                    default:
                        // ignore
                    }
                    break;
                case REQUEST:
                    if (GETMAP.equals(name)) {
                        stateStack.push(currentState);
                        currentState = State.GETMAP;
                    }
                    break;
                case GETMAP:
                    switch (name) {
                    case FORMAT:
                        buffer = new StringBuilder();
                        break;
                    case GET:
                        stateStack.push(currentState);
                        currentState = State.GET;
                        break;
                    default:
                        // ignored
                    }
                    break;
                case GET:
                    if (ONLINE_RESOURCE.equals(name)) {
                        getMapUrl = attr.getValue(XLINK_HREF_ATTR);
                    }
                    break;
                default:
                    // ignored
                }
            }

            /**
             * Check if queryable flag is set
             * 
             * @param attr the current attributes
             * @return true is queryable is set
             */
            private boolean isQueryable(@NonNull Attributes attr) {
                final String queryable = attr.getValue(QUERYABLE);
                return ONE.equals(queryable) || TRUE.equalsIgnoreCase(queryable);
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (buffer != null) {
                    buffer.append(Arrays.copyOfRange(ch, start, start + length));
                }
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {
                LayerTemp current = layerStack.peek();
                switch (currentState) {
                case LAYER:
                    switch (name) {
                    case TITLE:
                        current.title = buffer.toString();
                        buffer = null;
                        break;
                    case NAME:
                        current.name = buffer.toString();
                        buffer = null;
                        break;
                    case ABSTRACT:
                        current.description = buffer.toString();
                        buffer = null;
                        break;
                    case CRS:
                    case SRS:
                        String tempCrs = buffer.toString();
                        if (TileLayerSource.isLatLon(tempCrs) || (TileLayerSource.is3857compatible(tempCrs) && !TileLayerSource.isLatLon(current.crs))) {
                            current.crs = tempCrs;
                        }
                        buffer = null;
                        break;
                    case MIN_SCALE_DENOMINATOR:
                        // tries to calculate something roughly equivalent to min pixel size
                        try {
                            current.gsd = Double.parseDouble(buffer.toString());
                        } catch (NumberFormatException nfex) {
                            // ignore
                        }
                        break;
                    case LAYER:
                        if (!current.group && current.name != null) {
                            try {
                                Layer layer = constructLayer(layerStack);
                                layers.add(layer);
                            } catch (UnsupportedFormatException fex) {
                                Log.e(DEBUG_TAG, fex.getMessage());
                            }
                        }
                        layerStack.pop();
                        currentState = stateStack.pop();
                        break;
                    default:
                        // ignore
                    }
                    break;
                case EXGEOGRAPHICBOUNDINGBOX:
                    switch (name) {
                    case WEST_BOUND_LONGITUDE:
                        current.minx = bufferToBigDecimal(buffer, -180);
                        break;
                    case EAST_BOUND_LONGITUDE:
                        current.maxx = bufferToBigDecimal(buffer, 180);
                        break;
                    case SOUTH_BOUND_LATITUDE:
                        current.miny = bufferToBigDecimal(buffer, -90);
                        break;
                    case NORTH_BOUND_LATITUDE:
                        current.maxy = bufferToBigDecimal(buffer, 90);
                        break;
                    case EX_GEOGRAPHIC_BOUNDING_BOX:
                        current.boxCrs = TileLayerSource.EPSG_4326;
                        if (is130(wmsVersion)) {// switch things around
                            BigDecimal temp = current.minx;
                            current.minx = current.miny;
                            current.miny = temp;
                            temp = current.maxx;
                            current.maxx = current.maxy;
                            current.maxy = temp;
                        }
                        buffer = null;
                        currentState = stateStack.pop();
                        break;
                    default:
                        // ignore
                    }
                    break;
                case STYLE:
                    if (STYLE.equals(name)) {
                        currentState = stateStack.pop();
                    }
                    break;
                case ATTRIBUTION:
                    if (ATTRIBUTION.equals(name)) {
                        currentState = stateStack.pop();
                    }
                    break;
                case BASE:
                    switch (name) {
                    case WMT_MS_CAPABILITIES:
                    case WMS_CAPABILITIES:
                        break;
                    default:
                        // ignore
                    }
                    break;
                case CAPABILITY:
                    if (CAPABILITY.equals(name)) {
                        currentState = stateStack.pop();
                    }
                    break;
                case REQUEST:
                    if (REQUEST.equals(name)) {
                        currentState = stateStack.pop();
                    }
                    break;
                case GETMAP:
                    switch (name) {
                    case GETMAP:
                        currentState = stateStack.pop();
                        break;
                    case FORMAT:
                        String tempFormat = buffer.toString();
                        int preference = FORMAT_PREFERENCE.indexOf(tempFormat);
                        if (preference >= 0 && (tileFormat == null || preference < FORMAT_PREFERENCE.indexOf(tileFormat))) {
                            tileFormat = tempFormat;
                        }
                        buffer = null;
                        break;
                    default:
                        // ignore
                    }
                    break;
                case GET:
                    if (GET.equals(name)) {
                        currentState = stateStack.pop();
                    }
                    break;
                }
            }

            /**
             * Try to convert the contents of a StringBuilder to a BigDecimal
             * 
             * @param input the input StringBuilder
             * @param defaultValue the value to use if the StringBuilder cannot be converted
             * @return a BigDecimal
             */
            @NonNull
            private BigDecimal bufferToBigDecimal(@NonNull StringBuilder input, int defaultValue) {
                try {
                    return new BigDecimal(buffer.toString());
                } catch (NumberFormatException nfe) {
                    return new BigDecimal(180);
                }
            }
        });
    }

    /**
     * Construct a Layer from a stack of parsed layer entries, taking inheritance into account
     * 
     * @param stack a stack containing the parsed entries
     * @return a Layer object
     */
    @NonNull
    private Layer constructLayer(@NonNull Deque<LayerTemp> stack) {
        Layer layer = new Layer();
        layer.wmsVersion = wmsVersion;
        layer.format = tileFormat;
        if (layer.format == null) {
            throw new UnsupportedFormatException("No supported image format");
        }
        StringBuilder resultTitle = new StringBuilder();
        boolean first = true;
        Iterator<LayerTemp> it = stack.descendingIterator();
        while (it.hasNext()) {
            LayerTemp t = it.next();
            if (!first) {
                resultTitle.append(" - ");
            } else {
                first = false;
            }
            if (t.title != null && !"".equals(t.title)) {
                resultTitle.append(t.title);
            } else if (t.name != null && !"".equals(t.name)) {
                resultTitle.append(t.name);
            } else {
                resultTitle.append(NONAME);
            }
            if (t.crs != null) {
                layer.proj = t.crs;
            } else {
                throw new UnsupportedFormatException("No supported projection");
            }
            if (t.boxCrs == null) {
                continue;
            }
            try {
                if (TileLayerSource.isLatLon(t.boxCrs)) {
                    if (is130(wmsVersion) && TileLayerSource.EPSG_4326.equals(t.boxCrs)) { // flip axis
                        layer.extent = new BoundingBox(scaledDecimal(t.miny), scaledDecimal(t.minx), scaledDecimal(t.maxy), scaledDecimal(t.maxx));
                    } else {
                        layer.extent = new BoundingBox(scaledDecimal(t.minx), scaledDecimal(t.miny), scaledDecimal(t.maxx), scaledDecimal(t.maxy));
                    }
                } else { // EPSG:3857
                    layer.extent = new BoundingBox(Math.toDegrees(t.minx.doubleValue() / GeoMath.EARTH_RADIUS_EQUATOR),
                            GeoMath.mercatorToLat(t.miny.doubleValue()), Math.toDegrees(t.maxx.doubleValue() / GeoMath.EARTH_RADIUS_EQUATOR),
                            GeoMath.mercatorToLat(t.maxy.doubleValue()));
                }
            } catch (IllegalArgumentException iae) {
                Log.e(DEBUG_TAG, iae.getMessage());
            }
        }
        layer.title = resultTitle.toString();
        LayerTemp current = stack.peek();
        layer.name = current.name;
        layer.description = current.description;
        if (current.gsd != 0) {
            layer.gsd = current.gsd;
        }
        return layer;
    }

    /**
     * Check if this is a WMS 1.3 or later service
     * 
     * @param version the Version to check
     * @return true if this is a WMS 1.3 or later service
     */
    public static boolean is130(@NonNull Version version) {
        return version.largerThanOrEqual(WMS_1_3_0);
    }

    /**
     * The URL for the GetMap request
     * 
     * @return the URL or null if not set
     */
    @Nullable
    public String getGetMapUrl() {
        return getMapUrl;
    }

    /**
     * Scale a BigDecimal to internal int format
     * 
     * @param val input BigDecimal
     * @return a scaled int
     */
    private int scaledDecimal(@Nullable BigDecimal val) {
        if (val == null) {
            throw new IllegalArgumentException("Null argument");
        }
        return val.scaleByPowerOfTen(Node.COORDINATE_SCALE).intValue();
    }
}
