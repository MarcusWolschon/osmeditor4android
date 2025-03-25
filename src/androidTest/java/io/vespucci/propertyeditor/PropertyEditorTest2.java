package io.vespucci.propertyeditor;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import io.vespucci.App;
import io.vespucci.prefs.Preferences;

/**
 * Run all tests in PropertyEditorTest with new task flag
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PropertyEditorTest2 extends PropertyEditorTest {

    /**
     * Pre-test setup
     */
    @Override
    @Before
    public void setup() {
        super.setup();
        Preferences prefs = new Preferences(context);
        prefs.setNewTaskForPropertyEditor(true);
        App.getLogic().setPrefs(prefs);
    }

    /**
     * Post-test teardown
     */
    @Override
    @After
    public void teardown() {
        super.setup();
        Preferences prefs = new Preferences(context);
        prefs.setNewTaskForPropertyEditor(false);
        App.getLogic().setPrefs(prefs);
    }
}