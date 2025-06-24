package de.blau.android.validation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Instrumentation;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.prefs.PrefEditorFragment;
import de.blau.android.prefs.Preferences;
import de.blau.android.validation.ValidatorRulesUI;
import de.blau.android.LayerUtils;
import de.blau.android.TestUtils;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ValidatorRulesUITest {

    Context context = null;
    Instrumentation.ActivityMonitor monitor = null;
    Instrumentation instrumentation = null;
    UiDevice device = null;
    Main main = null;
    ValidatorRulesUI validatorRulesUI;

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

        TestUtils.grantPermissons(device);
        TestUtils.dismissStartUpDialogs(device, main);
        TestUtils.stopEasyEdit(main);

    }

//    @After
//    public void teardown(){ instrumentation.removeMonitor(monitor);}

    @Test
    public void addRuleset(){
//        ValidatorRulesUI Vri = new ValidatorRulesUI();
//        Vri.manageRulesetContents(context);
//        TestUtils.clickText(device, false, "shop", true);
    }
}
//    @Test
//    public void testToggleHeaderCheckbox() {
//        // Open the resurveyList and check the initial state
//        Cursor resurveyCursor = ValidatorRulesDatabase.queryResurveyByName(writableDb, ValidatorRulesDatabase.DEFAULT_RULESET_NAME);
//        ResurveyAdapter resurveyAdapter = new ResurveyAdapter(writableDb, context, resurveyCursor);
//        resurveyList.setAdapter(resurveyAdapter);
//        runUiThreadTasksIncludingDelayedTasks();
//
//
//        // Click the header checkbox
//        headerEnabled.performClick();
//        headerEnabled.performClick();
//        runUiThreadTasksIncludingDelayedTasks();
//
//        // Verify that all child checkboxes are checked
//        assertFalse(headerEnabled.isChecked());
//        for (int i = 0; i < resurveyList.getChildCount(); i++) {
//        View childView = resurveyList.getChildAt(i);
//        CheckBox childCheckBox = childView.findViewById(R.id.resurvey_enabled);
//        assertFalse(childCheckBox.isChecked());
//        }
//
//        // Click the "Done" button to save the changes
//        // Assuming you have a method to get the "Done" button and click it
//        clickDoneButton();
//
//        // Re-open the resurveyList
//        resurveyCursor = ValidatorRulesDatabase.queryResurveyByName(writableDb, ValidatorRulesDatabase.DEFAULT_RULESET_NAME);
//        resurveyAdapter = new ResurveyAdapter(writableDb, context, resurveyCursor);
//        resurveyList.setAdapter(resurveyAdapter);
//        runUiThreadTasksIncludingDelayedTasks();
//
//        // Verify that the changes are persisted
//        assertFalse(headerEnabled.isChecked());
//        for (int i = 0; i < resurveyList.getChildCount(); i++) {
//        View childView = resurveyList.getChildAt(i);
//        CheckBox childCheckBox = childView.findViewById(R.id.resurvey_enabled);
//        assertFalse(childCheckBox.isChecked());
//        }
//        }
//
//// Helper method to click the "Done" button
//private void clickDoneButton() {
//        // Implement the logic to find and click the "Done" button
//        // based on your application's UI structure
//        }
//}
