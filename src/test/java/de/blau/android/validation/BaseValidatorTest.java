package de.blau.android.validation;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElementFactory;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.StorageDelegatorTest;
import de.blau.android.osm.Tags;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class BaseValidatorTest {

    /**
     * Test relation validation
     */
    @Test
    public void relationTest() {
        Validator v = App.getDefaultValidator(ApplicationProvider.getApplicationContext());
        StorageDelegator d = new StorageDelegator();
        OsmElementFactory factory = d.getFactory();
        Relation r = factory.createRelationWithNewId();
        int result = v.validate(r);
        assertEquals(Validator.NO_TYPE, result & Validator.NO_TYPE);
        assertEquals(Validator.EMPTY_RELATION, result & Validator.EMPTY_RELATION);
        Map<String, String> tags = new HashMap<>();
        tags.put(Tags.KEY_TYPE, "test");
        d.setTags(r, tags);
        result = v.validate(r);
        assertEquals(0, result & Validator.NO_TYPE);
        Node n = factory.createNodeWithNewId(StorageDelegatorTest.toE7(51.476), StorageDelegatorTest.toE7(0.006));
        d.addMemberToRelation(n, "test", r);
        result = v.validate(r);
        assertEquals(0, result & Validator.EMPTY_RELATION);
    }
}