package de.blau.android;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import de.blau.android.exception.OsmIllegalOperationException;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.validation.Validator;

/**
 *
 * @author simon
 *
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ValidatorTest {
    static String DEBUG_TAG = "ValidatorTest";

    Main main = null;
    View v    = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    @Before
    public void setup() {
        main = mActivityRule.getActivity();
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
    }

    @Test
    public void baseValidator() {

        // read in test data
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test3.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        
        Node zumRueden = (Node) App.getDelegator().getOsmElement(Node.NAME, 370530329);
        Assert.assertNotNull(zumRueden);
        Validator validator = App.getDefaultValidator(main);
        Assert.assertEquals(Validator.AGE, zumRueden.hasProblem(main, validator) & Validator.AGE);
        Assert.assertEquals(Validator.MISSING_TAG, zumRueden.getCachedProblems() & Validator.MISSING_TAG);

        StringBuilder problemString = new StringBuilder();
        for (String s : validator.describeProblem(main, zumRueden)) {
            problemString.append(s);
        }
        Assert.assertTrue(problemString.toString().contains(main.getString(R.string.toast_needs_resurvey)));
        java.util.Map<String, String> tags = new HashMap<String, String>(zumRueden.getTags());
        SimpleDateFormat format = new SimpleDateFormat(Tags.CHECK_DATE_FORMAT);
        tags.put(Tags.KEY_CHECK_DATE, format.format(new Date()));
        try {
            App.getLogic().setTags(main, zumRueden, tags);
        } catch (OsmIllegalOperationException oioe) {
            Assert.fail();
        }
        Assert.assertEquals(0, zumRueden.hasProblem(main, validator) & Validator.AGE);

        Way limmatQuai = (Way) App.getDelegator().getOsmElement(Way.NAME, 147759323);
        Assert.assertEquals(Validator.OK, limmatQuai.hasProblem(main, validator));
        tags = new HashMap<String, String>(limmatQuai.getTags());
        String originalHighway = tags.get(Tags.KEY_HIGHWAY);
        tags.put(Tags.KEY_HIGHWAY, Tags.VALUE_ROAD);
        try {
            App.getLogic().setTags(main, limmatQuai, tags);
            Assert.assertEquals(Validator.HIGHWAY_ROAD, limmatQuai.hasProblem(main, validator) & Validator.HIGHWAY_ROAD);
            problemString = new StringBuilder();
            for (String s : validator.describeProblem(main, limmatQuai)) {
                problemString.append(s);
            }
            Assert.assertTrue(problemString.toString().contains(main.getString(R.string.toast_unsurveyed_road)));
            tags = new HashMap<String, String>(limmatQuai.getTags());
            tags.put(Tags.KEY_HIGHWAY, originalHighway);
            Assert.assertNotNull(tags.remove(Tags.KEY_NAME));
            App.getLogic().setTags(main, limmatQuai, tags);
            Assert.assertEquals(Validator.MISSING_TAG, limmatQuai.hasProblem(main, validator) & Validator.MISSING_TAG);
        } catch (OsmIllegalOperationException oioe) {
            Assert.fail();
        }

        Relation rcn66 = (Relation) App.getDelegator().getOsmElement(Relation.NAME, 28059);
        Assert.assertEquals(Validator.OK, rcn66.hasProblem(main, validator));
        tags = new HashMap<String, String>(rcn66.getTags());
        tags.remove(Tags.KEY_TYPE);
        try {
            App.getLogic().setTags(main, rcn66, tags);
            Assert.assertEquals(Validator.NO_TYPE, rcn66.hasProblem(main, validator) & Validator.NO_TYPE);
        } catch (OsmIllegalOperationException oioe) {
            Assert.fail();
        }
        problemString = new StringBuilder();
        for (String s : validator.describeProblem(main, rcn66)) {
            problemString.append(s);
        }
        Assert.assertTrue(problemString.toString().contains(main.getString(R.string.toast_notype)));

        // SQLiteDatabase db = new ValidatorRulesDatabaseHelper(main).getWritableDatabase();
        // ValidatorRulesDatabase.updateResurvey(db, id, key, value, days);
    }
    
    @Test
    public void baseValidatorUk() {
        // read in test data
        final CountDownLatch signal1 = new CountDownLatch(1);
        Logic logic = App.getLogic();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("london.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        
        Way constitutionHill = (Way) App.getDelegator().getOsmElement(Way.NAME, 451984385L);
        Validator validator = App.getDefaultValidator(main);
        Assert.assertEquals(Validator.OK, constitutionHill.hasProblem(main, validator));
        HashMap<String, String> tags = new HashMap<>(constitutionHill.getTags());
        tags.put(Tags.KEY_MAXSPEED, "30");
        try {
            App.getLogic().setTags(main, constitutionHill, tags);
            Assert.assertEquals(Validator.IMPERIAL_UNITS, constitutionHill.hasProblem(main, validator) & Validator.IMPERIAL_UNITS);
        } catch (OsmIllegalOperationException oioe) {
            Assert.fail();
        }
    }

}