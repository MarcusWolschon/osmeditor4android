package io.vespucci.util.mvt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.filters.LargeTest;
import io.vespucci.util.IntegerUtil;
import io.vespucci.util.mvt.style.Color;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33)
@LargeTest
public class ColorTest {

    /**
     * Parse some color examples
     */
    @Test
    public void decodeColors() {
        assertEquals(Long.parseLong("B3FFFFFF", 16), IntegerUtil.toUnsignedLong(Color.parseColor("rgba(255,255,255,0.7)")));
        assertEquals(Integer.parseInt("e0e4dd", 16), Color.parseColor("#e0e4dd"));
        assertEquals(Integer.parseInt("ffddee", 16), Color.parseColor("#fde"));
        assertEquals(Integer.parseInt("ffffff", 16), Color.parseColor("white"));
        assertEquals(Integer.parseInt("9ACD32", 16), Color.parseColor("yellowgreen"));
        assertEquals(Long.parseLong("FF6E8CDD", 16), IntegerUtil.toUnsignedLong(Color.parseColor("hsl(224,62%,65%)")));
        assertEquals(Long.parseLong("B36E8CDD", 16), IntegerUtil.toUnsignedLong(Color.parseColor("hsla(224,62%,65%,0.7)")));
    }
}