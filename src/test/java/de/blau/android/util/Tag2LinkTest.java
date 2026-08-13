package de.blau.android.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.osm.Tags;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class Tag2LinkTest {

    @Test
    public void isLinkTest() {
        Tag2Link t2l = new Tag2Link(ApplicationProvider.getApplicationContext());
        assertTrue(t2l.isLink(Tags.KEY_WEBSITE));
        assertTrue(t2l.isLink(Tags.KEY_CONTACT_WEBSITE));
        assertTrue(t2l.isLink(Tags.KEY_WIKIPEDIA));
        assertTrue(t2l.isLink(Tags.KEY_WIKIDATA));
        assertTrue(t2l.isLink(Tags.KEY_BRAND_WIKIPEDIA));
        assertTrue(t2l.isLink(Tags.KEY_BRAND_WIKIDATA));
        assertTrue(t2l.isLink("panoramax"));
        assertTrue(t2l.isLink("wikimedia_commons"));
        assertTrue(t2l.isLink("wikimedia_commons:1"));
        assertTrue(t2l.isLink("panoramax:123"));
        assertFalse(t2l.isLink("not_an_url"));
        assertFalse(t2l.isLink("not_an_url:1"));
    }

    @Test
    public void linkTest() {
        Tag2Link t2l = new Tag2Link(ApplicationProvider.getApplicationContext());
        assertEquals("https://wikipedia.org/wiki/de%3ASchloss%20Lenzburg", t2l.get(Tags.KEY_WIKIPEDIA, "de:Schloss Lenzburg"));
        assertEquals("https://www.wikidata.org/entity/Q668647", t2l.get(Tags.KEY_WIKIDATA, "Q668647"));
        assertEquals("https://wikipedia.org/wiki/de%3ASchloss%20Lenzburg", t2l.get(Tags.KEY_WIKIPEDIA + ":1", "de:Schloss Lenzburg"));
    }
    
    @Test
    public void notALinkTest() {
        Tag2Link t2l = new Tag2Link(ApplicationProvider.getApplicationContext());
        assertEquals("de:Schloss Lenzburg", t2l.get("not_an_url", "de:Schloss Lenzburg"));
        assertEquals("de:Schloss Lenzburg", t2l.get("not_an_url:1", "de:Schloss Lenzburg"));
    }
    
    @Test
    public void alreadyLinkTest() {
        Tag2Link t2l = new Tag2Link(ApplicationProvider.getApplicationContext());
        assertEquals("https://wikipedia.org/wiki/de%3ASchloss%20Lenzburg", t2l.get(Tags.KEY_WIKIPEDIA, "https://wikipedia.org/wiki/de%3ASchloss%20Lenzburg"));
    }
}