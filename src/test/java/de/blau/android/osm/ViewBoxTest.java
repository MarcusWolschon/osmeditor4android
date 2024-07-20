package de.blau.android.osm;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.app.Activity;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.ShadowWorkManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = { ShadowWorkManager.class }, sdk=33)
@LargeTest
public class ViewBoxTest {

    Context  context;
    Activity activity;
    Logic    logic;
    Map      map;

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        activity = Robolectric.buildActivity(Main.class).create().resume().get();
        logic = App.getLogic();
        map = logic.getMap();
    }

    /**
     * moveTo
     */
    @Test
    public void moveToTest() {
        final ViewBox viewBox = map.getViewBox();
        viewBox.setZoom(map, 15);
        viewBox.moveTo(map, 0, 0);
        double[] center = viewBox.getCenter();
        assertEquals(0, center[0], 0.01);
        assertEquals(0, center[1], 0.01);
        viewBox.moveTo(map, (int) (100 * 1E7), (int) (45 * 1E7));
        center = viewBox.getCenter();
        assertEquals(100, center[0], 0.01);
        assertEquals(45, center[1], 0.01);
        viewBox.moveTo(map, (int) (-120 * 1E7), (int) (45 * 1E7));
        center = viewBox.getCenter();
        assertEquals(-120, center[0], 0.01);
        assertEquals(45, center[1], 0.01);
        viewBox.moveTo(map, (int) (-200 * 1E7), (int) (45 * 1E7));
        center = viewBox.getCenter();
        assertEquals(160, center[0], 0.01);
        assertEquals(45, center[1], 0.01);
    }

}