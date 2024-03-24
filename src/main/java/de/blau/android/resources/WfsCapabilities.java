package de.blau.android.resources;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.contract.MimeTypes;
import de.blau.android.exception.UnsupportedFormatException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.util.Version;

/**
 * Minimal class to hold a list of layers parsed from a WMS servers GetCapabilities request response,
 * 
 * This ignores everything we currently don't want or need in vespucci
 * 
 * @author Simon Poole
 *
 */
public class WfsCapabilities {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, WfsCapabilities.class.getSimpleName().length());
    private static final String DEBUG_TAG = WfsCapabilities.class.getSimpleName().substring(0, TAG_LEN);

    private static final String WFS_CAPABILITIES_ELEM    = "WFS_Capabilities";
    private static final String VERSION_ATTR             = "version";
    private static final String OPERATIONS_METADATA_ELEM = "OperationsMetadata";
    private static final String OPERATION_ELEM           = "Operation";
    private static final String PARAMETER_ELEM           = "Parameter";
    private static final String NAME_ELEM                = "Name";
    private static final String NAME_ATTR                = "name";
    private static final String FEATURE_TYPE_LIST_ELEM   = "FeatureTypeList";
    private static final String FEATURE_TYPE_ELEM        = "FeatureType";
    private static final String XLINK_HREF_ATTR          = "xlink:href";
    private static final String OP_GET_FEATURE_ELEM      = "GetFeature";
    private static final String OUTPUT_FORMAT_PARAM      = "outputFormat";
    private static final String ALLOWED_VALUES_ELEM      = "AllowedValues";
    private static final String VALUE_ELEM               = "Value";
    private static final String DCP_ELEM                 = "DCP";
    private static final String HTTP_ELEM                = "HTTP";
    private static final String GET_ELEM                 = "Get";
    private static final String UPPER_CORNER_ELEM        = "UpperCorner";
    private static final String LOWER_CORNER_ELEM        = "LowerCorner";
    private static final String FORMAT_ELEM              = "Format";
    private static final String WGS84_BOUNDING_BOX_ELEM  = "WGS84BoundingBox";
    private static final String OUTPUT_FORMATS_ELEM      = "OutputFormats";
    private static final String OTHER_SRS_ELEM           = "OtherSRS";
    private static final String OTHER_CRS_ELEM           = "OtherCRS";
    private static final String DEFAULT_SRS_ELEM         = "DefaultSRS";
    private static final String DEFAULT_CRS_ELEM         = "DefaultCRS";

    enum State {
        BASE, OPERATIONSMETADATA, OPERATION, PARAMETER, ALLOWEDVALUES, FEATURETYPELIST, FEATURETYPE, NAME, DEFAULTSRS, OTHERSRS, DCP, HTTP, WGS84BOUNDINGBOX, OUTPUTFORMATS
    }

    public class Feature {
        BoundingBox extent;
        String      name;
        String      proj;
        String      format;

        /**
         * Get a GetFeature url suitable for use in Vespucci
         * 
         * @return a GetFeature url
         */
        @NonNull
        String getUrl() {
            Operation getFeature = operations.get(OP_GET_FEATURE_ELEM);
            if (getFeature == null) {
                throw new IllegalStateException("GetFeature configuration missing");
            }
            return de.blau.android.util.Util.appendQuery(getFeature.url,
                    String.format("outputFormat=%s&VERSION=%s&SERVICE=WFS&typeName=%s&REQUEST=GetFeature&BBOX={bbox},%s&srsName=%s",
                            MimeTypes.APPLICATION_TYPE + "/" + MimeTypes.JSON_SUBTYPE, "1.1.1", name, TileLayerSource.EPSG_4326, TileLayerSource.EPSG_4326));
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
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
            builder.append(getUrl());
            return builder.toString();
        }
    }

    private class FeatureTemp {
        protected String       name;
        protected String       format;
        protected List<String> supportedCRS = new ArrayList<>();
        protected BigDecimal[] lowerCorner;
        protected BigDecimal[] upperCorner;

    }

    private class Parameter {
        final String name;
        List<String> allowedValues = new ArrayList<>();

        Parameter(@NonNull String name) {
            this.name = name;
        }
    }

    private class Operation {
        final String           name;
        Map<String, Parameter> parameters = new HashMap<>();
        protected String       url;

        Operation(@NonNull String name) {
            this.name = name;
        }
    }

    final Map<String, Operation> operations = new HashMap<>();
    final List<Feature>          features   = new ArrayList<>();

    private String  getMapUrl;
    private Version wmsVersion;

    /**
     * Construct a new container for Features parsed from a WFS getCapabilities request response
     * 
     * @param is the InputStram to parse
     * @throws ParserConfigurationException on parse issues
     * @throws SAXException on parser issues
     * @throws IOException if reading the InputStream fails
     */
    public WfsCapabilities(@NonNull InputStream is) throws ParserConfigurationException, SAXException, IOException {
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
            State                      currentState = State.BASE;
            private Deque<FeatureTemp> featureStack = new ArrayDeque<>();

            StringBuilder buffer        = null;
            Parameter     tempParameter = null;
            Operation     tempOperation = null;

            /**
             * ${@inheritDoc}.
             */
            @Override
            public void startElement(String uri, String localName, String name, Attributes attr) throws SAXException {
                FeatureTemp current = featureStack.peek();
                switch (currentState) {
                case BASE:
                    switch (localName) {
                    case WFS_CAPABILITIES_ELEM:
                        String versionStr = attr.getValue(VERSION_ATTR);
                        if (versionStr == null) {
                            throw new UnsupportedOperationException("No WFS version");
                        }
                        Version version = new Version(versionStr);
                        if (version.getMajor() != 1 || version.getMajor() != 1) {
                            throw new UnsupportedOperationException("WFS version " + version + " not supported");
                        }
                    case OPERATIONS_METADATA_ELEM:
                        currentState = State.OPERATIONSMETADATA;
                        break;
                    case FEATURE_TYPE_LIST_ELEM:
                        currentState = State.FEATURETYPELIST;
                        break;
                    }
                    break;
                case OPERATIONSMETADATA:
                    switch (localName) {
                    case OPERATION_ELEM:
                        String nameAttr = attr.getValue(NAME_ATTR);
                        tempOperation = new Operation(nameAttr);
                        currentState = State.OPERATION;
                        break;
                    }
                    break;
                case OPERATION:
                    switch (localName) {
                    case PARAMETER_ELEM:
                        String nameAttr = attr.getValue(NAME_ATTR);
                        tempParameter = new Parameter(nameAttr);
                        currentState = State.PARAMETER;
                        break;
                    case DCP_ELEM:
                        currentState = State.DCP;
                    }
                    break;
                case PARAMETER:
                    switch (localName) {
                    case ALLOWED_VALUES_ELEM:
                        currentState = State.ALLOWEDVALUES;
                        break;
                    case VALUE_ELEM:
                        buffer = new StringBuilder();
                        break;
                    }
                    break;
                case ALLOWEDVALUES:
                    switch (localName) {
                    case VALUE_ELEM:
                        buffer = new StringBuilder();
                        break;
                    }
                    break;
                case DCP:
                    switch (localName) {
                    case HTTP_ELEM:
                        currentState = State.HTTP;
                        break;
                    }
                case HTTP:
                    if (GET_ELEM.equals(localName)) {
                        tempOperation.url = attr.getValue(XLINK_HREF_ATTR);
                    }
                    break;
                case FEATURETYPELIST:
                    switch (localName) {
                    case FEATURE_TYPE_ELEM:
                        current = new FeatureTemp();
                        featureStack.push(current);
                        currentState = State.FEATURETYPE;
                        break;
                    }

                    break;
                case FEATURETYPE:
                    switch (localName) {
                    case NAME_ELEM:
                    case DEFAULT_CRS_ELEM:
                    case DEFAULT_SRS_ELEM:
                    case OTHER_CRS_ELEM:
                    case OTHER_SRS_ELEM:
                        buffer = new StringBuilder();
                        break;
                    case OUTPUT_FORMATS_ELEM:
                        currentState = State.OUTPUTFORMATS;
                        break;
                    case WGS84_BOUNDING_BOX_ELEM:
                        currentState = State.WGS84BOUNDINGBOX;
                        break;
                    }
                    break;
                case OUTPUTFORMATS:
                    switch (localName) {
                    case FORMAT_ELEM:
                        buffer = new StringBuilder();
                        break;
                    }
                    break;
                case WGS84BOUNDINGBOX:
                    switch (localName) {
                    case LOWER_CORNER_ELEM:
                    case UPPER_CORNER_ELEM:
                        buffer = new StringBuilder();
                        break;
                    }
                    break;
                default:
                    // ignored
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (buffer != null) {
                    buffer.append(Arrays.copyOfRange(ch, start, start + length));
                }
            }

            @Override
            public void endElement(String uri, String localName, String name) throws SAXException {
                FeatureTemp current = featureStack.peek();
                String bufferString = buffer != null ? buffer.toString() : null;
                switch (currentState) {
                case BASE:
                    switch (localName) {
                    case WFS_CAPABILITIES_ELEM:
                        Operation getFeature = operations.get(OP_GET_FEATURE_ELEM);
                        Iterator<FeatureTemp> it = featureStack.descendingIterator();
                        boolean supportsJson = getFeature.parameters.get(OUTPUT_FORMAT_PARAM).allowedValues.contains(MimeTypes.JSON_SUBTYPE);
                        while (it.hasNext()) {
                            try {
                                features.add(constructFeature(it.next(), supportsJson));
                            } catch (UnsupportedFormatException ufex) {
                                System.out.println(ufex.getMessage());
                            }
                        }
                        break;
                    }
                case OPERATIONSMETADATA:
                    switch (localName) {
                    case OPERATIONS_METADATA_ELEM:
                        currentState = State.BASE;
                        break;
                    }
                    break;
                case OPERATION:
                    switch (localName) {
                    case OPERATION_ELEM:
                        operations.put(tempOperation.name, tempOperation);
                        currentState = State.OPERATIONSMETADATA;
                        break;
                    default:
                        // ignore
                    }
                    break;
                case PARAMETER:
                    switch (localName) {
                    case PARAMETER_ELEM:
                        if (tempOperation != null) {
                            tempOperation.parameters.put(tempParameter.name, tempParameter);
                        }
                        tempParameter = null;
                        currentState = State.OPERATION;
                        break;
                    case VALUE_ELEM:
                        tempParameter.allowedValues.add(bufferString);
                        buffer = null;
                        break;
                    }
                    break;
                case ALLOWEDVALUES:
                    switch (localName) {
                    case VALUE_ELEM:
                        tempParameter.allowedValues.add(bufferString);
                        buffer = null;
                        break;
                    case ALLOWED_VALUES_ELEM:
                        currentState = State.PARAMETER;
                        break;
                    }
                    break;
                case DCP:
                    switch (localName) {
                    case DCP_ELEM:
                        currentState = State.OPERATION;
                        break;
                    }
                    break;
                case HTTP:
                    switch (localName) {
                    case HTTP_ELEM:
                        currentState = State.DCP;
                        break;
                    }
                    break;
                case FEATURETYPELIST:
                    switch (localName) {
                    case FEATURE_TYPE_LIST_ELEM:
                        currentState = State.BASE;
                        break;
                    }
                    break;
                case FEATURETYPE:
                    switch (localName) {
                    case NAME_ELEM:
                        current.name = bufferString;
                        buffer = null;
                        break;
                    case DEFAULT_CRS_ELEM:
                    case DEFAULT_SRS_ELEM:
                    case OTHER_CRS_ELEM:
                    case OTHER_SRS_ELEM:
                        current.supportedCRS.add(bufferString);
                        buffer = null;
                        break;
                    case FEATURE_TYPE_ELEM:
                        currentState = State.FEATURETYPELIST;
                        break;
                    }
                    break;
                case OUTPUTFORMATS:
                    switch (localName) {
                    case FORMAT_ELEM:
                        String[] mimeType = bufferString.split(";");
                        if ((MimeTypes.APPLICATION_TYPE + "/" + MimeTypes.JSON_SUBTYPE).equals(mimeType[0])) {
                            current.format = mimeType[0];
                        }
                        break;
                    case OUTPUT_FORMATS_ELEM:
                        currentState = State.FEATURETYPE;
                        break;
                    }
                    break;
                case WGS84BOUNDINGBOX:
                    switch (localName) {
                    case LOWER_CORNER_ELEM:
                        current.lowerCorner = getCoords(bufferString);
                        break;
                    case UPPER_CORNER_ELEM:
                        current.upperCorner = getCoords(bufferString);
                        break;
                    case WGS84_BOUNDING_BOX_ELEM:
                        currentState = State.FEATURETYPE;
                        break;
                    }
                    break;
                default:
                    Log.e(DEBUG_TAG, "Unknown state");
                }
            }

            /**
             * 
             */
            private BigDecimal[] getCoords(@NonNull String coordString) {
                String[] coord = coordString.split(" ");
                return new BigDecimal[] { new BigDecimal(coord[0]), new BigDecimal(coord[1]) };
            }
        });
    }

    /**
     * Construct a Layer from a stack of parsed layer entries, taking inheritance into account
     * 
     * @param supportsJson
     * 
     * @param stack a stack containing the parsed entries
     * @return a Layer object
     */
    @NonNull
    private Feature constructFeature(@NonNull FeatureTemp t, boolean supportsJson) {
        if (t.format == null && !supportsJson) {
            throw new UnsupportedFormatException("No supported output format");
        }
        if (!hasCrs(t, TileLayerSource.EPSG_4326)) {
            throw new UnsupportedFormatException("Doesn't support CRS/SRS " + TileLayerSource.EPSG_4326);
        }
        Feature feature = new Feature();
        try {
            feature.extent = new BoundingBox(scaledDecimal(t.lowerCorner[0]), scaledDecimal(t.lowerCorner[1]), scaledDecimal(t.upperCorner[0]),
                    scaledDecimal(t.upperCorner[1]));
        } catch (IllegalArgumentException iae) {
            Log.e(DEBUG_TAG, iae.getMessage());
        }
        System.out.println("feature name " + t.name);
        feature.name = t.name;
        return feature;
    }

    /**
     * Check if we support a specific CRS/SRS
     * 
     * @param t the temp Feature
     * @param crs the CRS string
     * @return true if supported
     */
    private boolean hasCrs(@NonNull FeatureTemp t, @NonNull String crs) {
        for (String e : t.supportedCRS) {
            if (e.toLowerCase().endsWith(crs.toLowerCase())) {
                return true;
            }
        }
        return false;
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
