package io.vespucci.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import io.vespucci.osm.BoundingBox;
import io.vespucci.resources.WmsCapabilities;
import io.vespucci.resources.WmsCapabilities.Layer;

public class WmsCapabilitiesTest {

    private static final String DEBUG_TAG = WmsCapabilitiesTest.class.getSimpleName().substring(0, Math.min(23, WmsCapabilitiesTest.class.getSimpleName().length()));

    /**
     * Parse a sample getcapabilities 1.3.0 response
     */
    @Test
    public void parse130() {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/wms_capabilities.xml")) {
            WmsCapabilities capa = new WmsCapabilities(is);
            assertEquals(106, capa.layers.size());
            Layer test = null;
            for (Layer t : capa.layers) {
                if ("50".equals(t.name)) {
                    test = t;
                    break;
                }
            }
            assertNotNull(test);
            assertEquals(new BoundingBox(-76.4551640, -55.0517270, -53.6410790, -21.6721860), test.extent); // NOSONAR
            assertEquals(
                    "?FORMAT=image/png8&TRANSPARENT=TRUE&VERSION=1.3.0&SERVICE=WMS&REQUEST=GetMap&LAYERS=50&STYLES=&CRS=EPSG:4326&WIDTH={width}&HEIGHT={height}&BBOX={bbox}",
                    test.getTileUrl(""));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse a sample getcapabilities 1.1.1 response
     */
    @Test
    public void parse111() {
        try (InputStream is = getClass().getResourceAsStream("/wms_capabilities_1.1.1.xml")) {
            WmsCapabilities capa = new WmsCapabilities(is);
            assertEquals(106, capa.layers.size());
            Layer test = null;
            for (Layer t : capa.layers) {
                if ("50".equals(t.name)) {
                    test = t;
                    break;
                }
            }
            assertNotNull(test);
            assertEquals(new BoundingBox(-76.4551640, -55.0517270, -53.6410790, -21.6721860), test.extent); // NOSONAR
            assertEquals(
                    "?FORMAT=image/png8&TRANSPARENT=TRUE&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS=50&STYLES=&SRS=EPSG:4326&WIDTH={width}&HEIGHT={height}&BBOX={bbox}",
                    test.getTileUrl(""));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }
}
