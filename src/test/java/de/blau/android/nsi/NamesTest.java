package de.blau.android.nsi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.nsi.Names.NameAndTags;
import de.blau.android.osm.Tags;
import de.blau.android.util.SearchIndexUtils;
import de.blau.android.util.collections.MultiHashMap;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class NamesTest {

    private static final String MOBILITY    = "Mobility";
    private static final String MC_DONALD_S = "McDonald's";

    /**
     * 
     */
    @Test
    public void nsiTest() {
        Names names = new Names(ApplicationProvider.getApplicationContext());
        SortedMap<String, String> map = new TreeMap<>();
        map.put(Tags.KEY_AMENITY, Tags.VALUE_FAST_FOOD);
        map.put("cusine", "burger");
        assertTrue(checkForMcD(names.getNames(map, null)));
        //
        MultiHashMap<String, NameAndTags> index = names.getSearchIndex();
        assertTrue(checkForMcD(index.get(SearchIndexUtils.normalize(MC_DONALD_S))));

        // now country stuff
        map.clear();
        map.put(Tags.KEY_AMENITY, "car_sharing");
        assertTrue(checkForMobility(names.getNames(map, null)));
        assertTrue(checkForMobility(names.getNames(map, Arrays.asList("CH"))));
        assertFalse(checkForMobility(names.getNames(map, Arrays.asList("DE"))));
    }

    /**
     * Check for McDonalds
     * 
     * @param result the Collection of NameAndTags to check
     * @return true if found
     */
    private boolean checkForMcD(Collection<NameAndTags> result) {
        boolean found = false;
        for (NameAndTags nt : result) {
            if (MC_DONALD_S.equals(nt.getName()) && Tags.VALUE_FAST_FOOD.equals(nt.getTags().get(Tags.KEY_AMENITY))
                    && "burger".equals(nt.getTags().get("cuisine"))) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Check for mobility
     * 
     * @param result the Collection of NameAndTags to check
     * @return true if found
     */
    private boolean checkForMobility(Collection<NameAndTags> result) {
        boolean found = false;
        for (NameAndTags nt : result) {
            if (MOBILITY.equals(nt.getName()) && "car_sharing".equals(nt.getTags().get(Tags.KEY_AMENITY))) {
                found = true;
                break;
            }
        }
        return found;
    }
}