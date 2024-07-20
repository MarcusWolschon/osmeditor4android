package de.blau.android.address;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.ShadowWorkManager;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.ViewBox;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.ElementSearch;
import de.blau.android.util.GeoMath;
import de.blau.android.util.IntCoordinates;
import de.blau.android.util.StreetPlaceNamesAdapter;
import de.blau.android.util.Util;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class PredictionTest {

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        readData();
        Address.resetLastAddresses(ApplicationProvider.getApplicationContext());
    }

    /**
     * Post test teardown
     */
    @After
    public void teardown() {
        Address.resetLastAddresses(ApplicationProvider.getApplicationContext());
    }

    /**
     * Test that searching for nearby streets works
     */
    @Test
    public void elementSearchStreetTest() {
        StorageDelegator d = App.getDelegator();
        Node n = (Node) d.getOsmElement(Node.NAME, 101792984L);
        assertNotNull(n);
        ElementSearch es = new ElementSearch(new IntCoordinates(n.getLon(), n.getLat()), false);
        String[] names = es.getStreetNames();
        assertNotNull(names);
        try {
            assertEquals(49855526L, es.getStreetId("Rosenweg"));
        } catch (OsmException e) {
            fail(e.getMessage());
        }
        // Kirchstrasse has multiple segments, we should get the one nearest
        try {
            assertEquals(47001847L, es.getStreetId("Kirchstrasse"));
        } catch (OsmException e) {
            fail(e.getMessage());
        }
        assertEquals("Bernetstrasse", names[0]); // nearest
        assertEquals(8, names.length);
    }

    /**
     * Test that searching for nearby places works
     */
    @Test
    public void elementSearchPlaceTest() {
        StorageDelegator d = App.getDelegator();
        Node n = (Node) d.getOsmElement(Node.NAME, 101792984L);
        assertNotNull(n);
        ElementSearch es = new ElementSearch(new IntCoordinates(n.getLon(), n.getLat()), false);
        String[] names = es.getPlaceNames();
        assertNotNull(names);
        assertEquals(1, names.length);
        assertEquals("Bergdietikon", names[0]);
    }

    /**
     * Test the street name adapter
     */
    @Test
    public void streetNamesAdapterTest() {
        StreetPlaceNamesAdapter streetNamesAutocompleteAdapter = new StreetPlaceNamesAdapter(ApplicationProvider.getApplicationContext(),
                R.layout.autocomplete_row, App.getDelegator(), Node.NAME, 101792984L, null, false);
        assertNotNull(streetNamesAutocompleteAdapter);
        assertEquals(8, streetNamesAutocompleteAdapter.getCount());
    }

    /**
     * Test the place name adapter
     */
    @Test
    public void placeNamesAdapterTest() {
        StreetPlaceNamesAdapter placeNamesAutocompleteAdapter = new StreetPlaceNamesAdapter(ApplicationProvider.getApplicationContext(),
                R.layout.autocomplete_row, App.getDelegator(), Node.NAME, 101792984L, null, true);
        assertNotNull(placeNamesAutocompleteAdapter);
        assertEquals(1, placeNamesAutocompleteAdapter.getCount());
        ValueWithCount v = placeNamesAutocompleteAdapter.getItem(0);
        assertEquals("Bergdietikon", v.getValue());
    }

    /**
     * Predict an address on a Node
     */
    @Test
    public void predictTest() {
        Main main = Robolectric.setupActivity(Main.class);

        Node n = App.getLogic().performAddNode(main, 8.3864151D, 47.3906916D);
        Map<String, List<String>> tags = new HashMap<>();
        Address.predictAddressTags(ApplicationProvider.getApplicationContext(), Node.NAME, n.getOsmId(),
                new ElementSearch(new IntCoordinates(n.getLon(), n.getLat()), false), tags, Address.NO_HYSTERESIS);
        System.out.println(tags);
        try {
            assertEquals(19, Integer.parseInt(tags.get(Tags.KEY_ADDR_HOUSENUMBER).get(0)));
        } catch (NumberFormatException nfex) {
            fail(nfex.getMessage());
        }
        assertEquals("Bernetstrasse", tags.get(Tags.KEY_ADDR_STREET).get(0));
    }

    /**
     * Predict and address on a Node that is part of a building outline
     */
    @Test
    public void predictEntranceTest() {
        Main main = Robolectric.setupActivity(Main.class);
        de.blau.android.Map map = App.getLogic().getMap();
        App.getLogic().setZoom(map, 22);
        map.getViewBox().moveTo(map, (int) (8.3866124D * 1E7d), (int) (47.38996877D * 1E7d));
        map.invalidate();

        final ViewBox viewBox = App.getLogic().getViewBox();
        App.getLogic().performAdd(main, GeoMath.lonToX(map.getWidth(), viewBox, 8.3866124D),
                GeoMath.latToY(map.getHeight(), map.getWidth(), viewBox, 47.3899687D));
        Node n = App.getLogic().getSelectedNode();

        Map<String, List<String>> tags = new HashMap<>();
        Address.predictAddressTags(ApplicationProvider.getApplicationContext(), Node.NAME, n.getOsmId(),
                new ElementSearch(new IntCoordinates(n.getLon(), n.getLat()), false), tags, Address.NO_HYSTERESIS);
        System.out.println(tags);
        try {
            assertEquals(19, Integer.parseInt(tags.get(Tags.KEY_ADDR_HOUSENUMBER).get(0)));
        } catch (NumberFormatException nfex) {
            fail(nfex.getMessage());
        }
        assertEquals("Bernetstrasse", tags.get(Tags.KEY_ADDR_STREET).get(0));
        assertNotNull(tags.get(Tags.KEY_ENTRANCE));
        assertEquals(Tags.VALUE_YES, tags.get(Tags.KEY_ENTRANCE).get(0));
    }

    /**
     * Predict an address with addr:place
     */
    @Test
    public void predictWithPlaceTest() {
        Main main = Robolectric.setupActivity(Main.class);
        final Logic logic = App.getLogic();
        // add some addresses for seeding
        Node n = logic.performAddNode(main, 8.3866757D, 47.3898405D);
        Map<String, String> t = new HashMap<>();
        t.put(Tags.KEY_ADDR_PLACE, "Bernold");
        t.put(Tags.KEY_ADDR_HOUSENUMBER, "1");
        logic.setTags(main, n, t);
        Address.updateLastAddresses(main, null, Node.NAME, n.getOsmId(), Util.getListMap(t), false);
        n = logic.performAddNode(main, 8.3867170D, 47.3897365D);
        t.put(Tags.KEY_ADDR_HOUSENUMBER, "2");
        logic.setTags(main, n, t);
        Address.updateLastAddresses(main, null, Node.NAME, n.getOsmId(), Util.getListMap(t), true);

        n = logic.performAddNode(main, 8.3866107D, 47.3900845D);
        Map<String, List<String>> tags = new HashMap<>();
        Address.predictAddressTags(ApplicationProvider.getApplicationContext(), Node.NAME, n.getOsmId(),
                new ElementSearch(new IntCoordinates(n.getLon(), n.getLat()), false), tags, Address.NO_HYSTERESIS);
        System.out.println(tags);
        try {
            assertEquals(3, Integer.parseInt(tags.get(Tags.KEY_ADDR_HOUSENUMBER).get(0)));
        } catch (NumberFormatException nfex) {
            fail(nfex.getMessage());
        }
        assertEquals("Bernold", tags.get(Tags.KEY_ADDR_PLACE).get(0));
    }

    /**
     * Init the storage delegator with test data
     */
    private void readData() {
        InputStream input = getClass().getResourceAsStream("/test2.osm");
        OsmParser parser = new OsmParser();
        try {
            parser.start(input);
            Storage storage = parser.getStorage();
            StorageDelegator d = App.getDelegator();
            d.setCurrentStorage(storage);
            d.fixupApiStorage();
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException | IllegalStateException e) {
            fail(e.getMessage());
        }
    }
}