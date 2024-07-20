package de.blau.android.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.JavaResources;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.ShadowWorkManager;
import de.blau.android.SignalHandler;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;

/**
 *
 * @author simon
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class ValidatorTest {

    Main  main  = null;
    Logic logic = null;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        App.resetPresets();
        resetValidator();
        main = Robolectric.buildActivity(Main.class).create().resume().get();
        Preferences prefs = new Preferences(ApplicationProvider.getApplicationContext());
        logic = App.getLogic();
        logic.setMap(new Map(ApplicationProvider.getApplicationContext()), false);
        logic.getMap().setPrefs(main, prefs);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        App.resetPresets();
        resetValidator();
    }

    /**
     * Test base validation stuff, age, missing tags, road names
     */
    @Test
    public void baseValidator() {
        readTestData("test3.osm");

        Node zumRueden = (Node) App.getDelegator().getOsmElement(Node.NAME, 370530329);
        assertNotNull(zumRueden);
        Validator validator = App.getDefaultValidator(main);
        assertEquals(Validator.AGE, zumRueden.hasProblem(main, validator) & Validator.AGE);
        assertEquals(Validator.MISSING_TAG, zumRueden.getCachedProblems() & Validator.MISSING_TAG);

        StringBuilder problemString = new StringBuilder();
        for (String s : validator.describeProblem(main, zumRueden)) {
            problemString.append(s);
        }
        assertTrue(problemString.toString().contains(main.getString(R.string.toast_needs_resurvey)));
        java.util.Map<String, String> tags = new HashMap<>(zumRueden.getTags());
        SimpleDateFormat format = new SimpleDateFormat(Tags.CHECK_DATE_FORMAT);
        tags.put(Tags.KEY_CHECK_DATE, format.format(new Date()));
        try {
            App.getLogic().setTags(main, zumRueden, tags);
        } catch (OsmIllegalOperationException oioe) {
            fail();
        }
        assertEquals(0, zumRueden.hasProblem(main, validator) & Validator.AGE);

        Way limmatQuai = (Way) App.getDelegator().getOsmElement(Way.NAME, 147759323);
        assertEquals(Validator.OK, limmatQuai.hasProblem(main, validator));
        tags = new HashMap<>(limmatQuai.getTags());
        String originalHighway = tags.get(Tags.KEY_HIGHWAY);
        tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_ROAD);
        try {
            App.getLogic().setTags(main, limmatQuai, tags);
            assertEquals(Validator.HIGHWAY_ROAD, limmatQuai.hasProblem(main, validator) & Validator.HIGHWAY_ROAD);
            problemString = new StringBuilder();
            for (String s : validator.describeProblem(main, limmatQuai)) {
                problemString.append(s);
            }
            assertTrue(problemString.toString().contains(main.getString(R.string.toast_unsurveyed_road)));
            tags = new HashMap<>(limmatQuai.getTags());
            tags.put(Tags.KEY_HIGHWAY, originalHighway);
            assertNotNull(tags.remove(Tags.KEY_NAME));
            App.getLogic().setTags(main, limmatQuai, tags);
            assertEquals(Validator.MISSING_TAG, limmatQuai.hasProblem(main, validator) & Validator.MISSING_TAG);
        } catch (OsmIllegalOperationException oioe) {
            fail();
        }

        Relation rcn66 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 28059);
        assertEquals(Validator.OK, rcn66.hasProblem(main, validator));
        tags = new HashMap<>(rcn66.getTags());
        tags.remove(Tags.KEY_TYPE);
        try {
            App.getLogic().setTags(main, rcn66, tags);
            assertEquals(Validator.NO_TYPE, rcn66.hasProblem(main, validator) & Validator.NO_TYPE);
        } catch (OsmIllegalOperationException oioe) {
            fail();
        }
        problemString = new StringBuilder();
        for (String s : validator.describeProblem(main, rcn66)) {
            problemString.append(s);
        }
        assertTrue(problemString.toString().contains(main.getString(R.string.toast_notype)));
    }

    /**
     * Localized validation test
     */
    @Test
    public void baseValidatorUk() {
        readTestData("london.osm");

        Way constitutionHill = (Way) App.getDelegator().getOsmElement(Way.NAME, 451984385L);
        assertNotNull(constitutionHill);
        Validator validator = App.getDefaultValidator(main);
        assertEquals(Validator.OK, constitutionHill.hasProblem(main, validator));
        HashMap<String, String> tags = new HashMap<>(constitutionHill.getTags());
        tags.put(Tags.KEY_MAXSPEED, "30");
        try {
            App.getLogic().setTags(main, constitutionHill, tags);
            assertEquals(Validator.IMPERIAL_UNITS, constitutionHill.hasProblem(main, validator) & Validator.IMPERIAL_UNITS);
        } catch (OsmIllegalOperationException oioe) {
            fail();
        }
    }

    @Test
    public void missingTagsCH() {
        setupTestPreset();
        readTestData("motorway_link_ch.osm");

        Way link = (Way) App.getDelegator().getOsmElement(Way.NAME, 22937041L);
        assertNotNull(link);
        assertTrue(link.hasTagKey(Tags.KEY_REF));
        java.util.Map<String, String> tags = new HashMap<>(link.getTags());
        tags.remove(Tags.KEY_REF);
        logic.setTags(main, link, tags);
        Validator validator = App.getDefaultValidator(main);
        assertEquals(Validator.MISSING_TAG, link.hasProblem(main, validator));
    }

    @Test
    public void missingTagsUS() {
        setupTestPreset();
        readTestData("motorway_link_us.osm");

        Way link = (Way) App.getDelegator().getOsmElement(Way.NAME, 688767606L);
        assertNotNull(link);
        assertFalse(link.hasTagKey(Tags.KEY_REF));
        Validator validator = App.getDefaultValidator(main);
        assertEquals(Validator.OK, link.hasProblem(main, validator));
    }

    /**
     * Read test data into memory
     * 
     * @param fileName the resource file name
     */
    private void readTestData(@NonNull String fileName) {
        // read in test data
        final CountDownLatch signal1 = new CountDownLatch(1);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = loader.getResourceAsStream(fileName)) {
            logic.readOsmFile(main, is, false, new SignalHandler(signal1));
            signal1.await(10, TimeUnit.SECONDS); // NOSONAR
        } catch (IOException | InterruptedException e1) { // NOSONAR
            fail(e1.getMessage());
        }
    }

    /**
     * Setup a test preset uses reflection to fudge things
     */
    private void setupTestPreset() {
        try {
            File testPresetFile = JavaResources.copyFileFromResources(ApplicationProvider.getApplicationContext(), "test_preset1.xml", null, "test_preset");
            Preset testPreset = new Preset(ApplicationProvider.getApplicationContext(), testPresetFile.getParentFile(), false);
            App.resetPresets();
            Field field = App.class.getDeclaredField("currentPresets");
            field.setAccessible(true); // NOSONAR
            field.set(null, new Preset[] { testPreset }); // NOSONAR
            field.setAccessible(false); // NOSONAR
            resetValidator();
        } catch (IOException | NoSuchAlgorithmException | ParserConfigurationException | SAXException | NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Reset the validator
     */
    private void resetValidator() {
        try {
            Field field = App.class.getDeclaredField("defaultValidator");
            field.setAccessible(true); // NOSONAR
            field.set(null, null); // NOSONAR
            field.setAccessible(false); // NOSONAR
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            fail(e.getMessage());
        }
    }
}