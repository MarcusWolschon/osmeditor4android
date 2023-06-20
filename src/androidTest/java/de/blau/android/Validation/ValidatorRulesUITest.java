package de.blau.android.Validation;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.CheckBox;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.blau.android.LayerUtils;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.TestUtils;
import de.blau.android.filter.PresetFilterActivity;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.validation.ValidatorRulesDatabase;
import de.blau.android.validation.ValidatorRulesDatabaseHelper;
import de.blau.android.validation.ValidatorRulesUI;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ValidatorRulesUITest {

    private Context context = null;
    private Instrumentation.ActivityMonitor monitor = null;
    private Instrumentation instrumentation = null;
    private UiDevice device = null;
    private Main main = null;
    private Map map = null;
    private ValidatorRulesUI validatorRulesUI;
    private SQLiteDatabase testDb;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
            instrumentation = InstrumentationRegistry.getInstrumentation();
            device = UiDevice.getInstance(instrumentation);
            context = instrumentation.getTargetContext();
            monitor = instrumentation.addMonitor(ValidatorRulesUI.class.getName(), null, false);
            main = mActivityRule.getActivity();
            Preferences prefs = new Preferences(context);
            LayerUtils.removeImageryLayers(context);
            map = main.getMap();
            map.setPrefs(main, prefs);

            TestUtils.grantPermissons(device);
            TestUtils.dismissStartUpDialogs(device, main);
            TestUtils.stopEasyEdit(main);
            TestUtils.loadTestData(main, "test2.osm");
//        solo = new Solo(getInstrumentation(), getActivity())

        testDb = SQLiteDatabase.create(null);
        // Initialize the test database
        initTestDatabase(testDb);
        // Create a new instance of the ValidatorRulesDatabaseHelper with the test database
        ValidatorRulesDatabaseHelper testDbHelper = new ValidatorRulesDatabaseHelper(context) {
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                // Do nothing to prevent upgrades during testing
            }
        };
        // Create a new instance of the ValidatorRulesUI with the test database helper
        validatorRulesUI = new ValidatorRulesUI(testDbHelper);
    }
    /**
     * Post-test teardown
     */
    @After
    public void cleanup() {
            testDb.close();
            instrumentation.removeMonitor(monitor);
    }
    @Test
    public void testManageRulesetContents() {
            // Create and show the dialog
            validatorRulesUI.manageRulesetContents(context);

//// Use Robotium to interact with UI and click on checkboxes
//        CheckBox headerSelectedCheckbox = (CheckBox) solo.getView(R.id.header_selected);
//        solo.clickOnView(headerSelectedCheckbox);
//
//            Dialog dialog = ShadowDialog.getLatestDialog();
//            assertNotNull(dialog);
//
//            // Click on the checkbox
//            CheckBox headerSelected = dialog.findViewById(R.id.header_selected);
//            assertNotNull(headerSelected);
//            headerSelected.performClick();
//
//            // Dismiss the dialog
//            dialog.dismiss();
    }

    private void initTestDatabase(SQLiteDatabase db) {
        // Recreate necessary tables and data for test database
        db.execSQL("DROP TABLE IF EXISTS rulesets");
        db.execSQL("CREATE TABLE rulesets (id INTEGER, name TEXT)");
        ValidatorRulesDatabase.addRuleset(db, 0, "Default");

        db.execSQL("DROP TABLE IF EXISTS resurveytags");
        db.execSQL(
                "CREATE TABLE resurveytags (ruleset INTEGER, key TEXT, value TEXT DEFAULT NULL, is_regexp INTEGER DEFAULT 0, days INTEGER DEFAULT 365, selected INTEGER DEFAULT 1, FOREIGN KEY(ruleset) REFERENCES rulesets(id))");
        ValidatorRulesDatabase.addResurvey(db, 0, Tags.KEY_SHOP, null, false, 365, true);
        ValidatorRulesDatabase.addResurvey(db, 0, Tags.KEY_AMENITY, Tags.VALUE_RESTAURANT, false,365, true);
        ValidatorRulesDatabase.addResurvey(db, 0, Tags.KEY_AMENITY, Tags.VALUE_FAST_FOOD, false, 365, true);
        ValidatorRulesDatabase.addResurvey(db, 0, Tags.KEY_AMENITY, Tags.VALUE_CAFE, false, 365, true);
        ValidatorRulesDatabase.addResurvey(db, 0, Tags.KEY_AMENITY, Tags.VALUE_PUB, false, 365, true);
        ValidatorRulesDatabase.addResurvey(db, 0, Tags.KEY_AMENITY, Tags.VALUE_BAR, false, 365, true);
        ValidatorRulesDatabase.addResurvey(db, 0, Tags.KEY_AMENITY, Tags.VALUE_TOILETS, false, 365, true);
    }
}





