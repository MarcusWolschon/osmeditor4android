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
    }

    @Test
    public void linkTest() {
        Tag2Link t2l = new Tag2Link(ApplicationProvider.getApplicationContext());
        assertEquals("https://wikipedia.org/wiki/de%3ASchloss%20Lenzburg", t2l.get(Tags.KEY_WIKIPEDIA, "de:Schloss Lenzburg"));
        assertEquals("https://www.wikidata.org/entity/Q668647", t2l.get(Tags.KEY_WIKIDATA, "Q668647"));
    }
}