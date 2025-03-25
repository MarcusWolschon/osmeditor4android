package io.vespucci.tasks;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import io.vespucci.tasks.OsmoseMeta;
import io.vespucci.tasks.OsmoseMeta.OsmoseClass;

public class OsmoseMetaTest {

    private static final String DEBUG_TAG = OsmoseMetaTest.class.getSimpleName().substring(0, Math.min(23, OsmoseMetaTest.class.getSimpleName().length()));

    /**
     * Parse a response with 1 item and class
     */
    @Test
    public void parse1() {
        try (InputStream is = getClass().getResourceAsStream("/osmose_meta_1.json")) {
            OsmoseMeta meta = new OsmoseMeta();
            meta.parse(is);
            OsmoseClass osmoseClass = meta.getOsmoseClass("3130", 31301);
            assertNotNull(osmoseClass);
            System.out.print(osmoseClass.fix);
        } catch (IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse a response with all items
     */
    @Test
    public void parseAll() {
        try (InputStream is = getClass().getResourceAsStream("/osmose_meta_all.json")) {
            OsmoseMeta meta = new OsmoseMeta();
            meta.parse(is);
            OsmoseClass osmoseClass = meta.getOsmoseClass("3130", 31301);
            assertNotNull(osmoseClass);
        } catch (IOException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }
}
