package io.vespucci.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import io.vespucci.util.LocaleUtils;

public class LocaleUtilsTest {

    /**
     * Check that Locales correctly get transformed to strings
     */
    @Test
    public void toLanguageTag() {
        Locale java = Locale.forLanguageTag("de-CH");
        assertEquals("de-CH", LocaleUtils.toLanguageTag(java));

        java = Locale.forLanguageTag("iw");
        assertEquals("he", LocaleUtils.toLanguageTag(java));

        java = Locale.forLanguageTag("in");
        assertEquals("id", LocaleUtils.toLanguageTag(java));

        Locale ny = LocaleUtils.forLanguageTagCompat("no-NO-NY");
        assertEquals("nn-NO", LocaleUtils.toLanguageTag(ny));
    }

    /**
     * Check that Locales correctly get generated from strings
     */
    @Test
    public void fromLanguageTag() {
        Locale java = Locale.forLanguageTag("de-CH");

        Locale locale = LocaleUtils.forLanguageTag("de-CH");
        assertEquals(java, locale);
        locale = LocaleUtils.forLanguageTagCompat("de-CH");
        assertEquals(java, locale);

        java = Locale.forLanguageTag("");
        locale = LocaleUtils.forLanguageTag("");
        assertEquals(java, locale);

        java = Locale.forLanguageTag("de");
        locale = LocaleUtils.forLanguageTag("de");
        assertEquals(java, locale);
    }

    /**
     * Check for latin script use
     */
    @Test
    public void latinScript() {
        Locale locale = Locale.forLanguageTag("de-CH");
        assertTrue(LocaleUtils.usesLatinScript(locale));
        locale = Locale.forLanguageTag("zh");
        assertFalse(LocaleUtils.usesLatinScript(locale));
    }
    
    @Test
    public void isLanguage() {
        assertTrue(LocaleUtils.isLanguage("de_CH"));
        assertTrue(LocaleUtils.isLanguage("de-Runr"));
        assertTrue(LocaleUtils.isLanguage("de"));
        assertFalse(LocaleUtils.isLanguage("xxxxxx"));
    }
}