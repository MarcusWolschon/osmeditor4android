package de.blau.android.util.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class LowAllocArrayListTest {

    /**
     * addAll from LowAllocArrayList
     */
    @Test
    public void addAll() {
        LowAllocArrayList<String> first = new LowAllocArrayList<>(Arrays.asList("1", "2", "3"));
        LowAllocArrayList<String> last = new LowAllocArrayList<>(Arrays.asList("4", "5", "6"));
        assertTrue(first.addAll(last));
        assertEquals(6, first.size());
        assertEquals("6", first.get(5));
    }

    /**
     * addAll from ArrayList
     */
    @Test
    public void addAll2() {
        LowAllocArrayList<String> first = new LowAllocArrayList<>(Arrays.asList("1", "2", "3"));
        ArrayList<String> last = new ArrayList<>(Arrays.asList("4", "5", "6"));
        assertTrue(first.addAll(last));
        assertEquals(6, first.size());
        assertEquals("6", first.get(5));
    }

    /**
     * addAll from ArrayList
     */
    @Test
    public void addAll3() {
        LowAllocArrayList<String> first = new LowAllocArrayList<>(Arrays.asList("1", "2", "3"));
        LowAllocArrayList<String> last = new LowAllocArrayList<>(Arrays.asList());
        assertFalse(first.addAll(last));
    }
    
    @Test 
    public void constructor() {
        final int count = 10000;
        LowAllocArrayList<String>[] array = new LowAllocArrayList[count]; 
        long start = System.currentTimeMillis();
        for (int i=0;i < count;i++) {
            array[i] =  new LowAllocArrayList<>();
        }
        System.out.println("Done in " + (System.currentTimeMillis() - start));
        for (int i=0;i < count;i++) {
            array[i].add(Integer.toString(i));
        }
    }
}