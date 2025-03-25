package io.vespucci.util.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.Test;

import androidx.test.filters.LargeTest;
import io.vespucci.util.collections.LinkedList;
import io.vespucci.util.collections.LinkedList.Member;

@LargeTest
public class LinkedListTest {

    /**
     * constructor with capacity
     * 
     * This just runs the code, as we don't have any way exposed to check if it works
     */
    @Test
    public void constructor() {
        LinkedList<String> list = new LinkedList<>(10);
        assertTrue(list.isEmpty());
    }

    /**
     * add
     */
    @Test
    public void add() {
        LinkedList<String> list = new LinkedList<>();
        assertTrue(list.add("1"));
        assertEquals(1, list.size());
        assertEquals("1", list.get(0));
    }

    /**
     * addAll
     */
    @Test
    public void addAll() {
        LinkedList<String> first = new LinkedList<>(Arrays.asList("1", "2", "3"));
        LinkedList<String> last = new LinkedList<>(Arrays.asList("4", "5", "6"));
        assertTrue(first.addAll(last));
        assertEquals(6, first.size());
        assertEquals("6", first.get(5));
    }

    /**
     * addAll
     */
    @Test
    public void addAll2() {
        LinkedList<String> first = new LinkedList<>(Arrays.asList("1", "2", "3"));
        LinkedList<String> last = new LinkedList<>(Arrays.asList());
        assertFalse(first.addAll(last));
    }

    /**
     * clear
     */
    @Test
    public void clear() {
        LinkedList<String> list = new LinkedList<>();
        assertEquals(0, list.size());
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.add("1"));
        assertEquals(1, list.size());
        assertEquals("1", list.get(0));
        list.clear();
        assertEquals(0, list.size());
    }

    /**
     * remove object
     */
    @Test
    public void remove() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertFalse(list.remove(null));
        assertTrue(list.remove("2"));
        assertEquals(2, list.size());
        assertEquals("1", list.get(0));
        assertEquals("3", list.get(1));
        list.add(1, "2");
        assertEquals("2", list.get(1));
    }

    /**
     * remove object
     */
    @Test
    public void remove2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertTrue(list.remove("1"));
        assertEquals(2, list.size());
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
    }

    /**
     * remove object
     */
    @Test
    public void remove3() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertTrue(list.remove("3"));
        assertEquals(2, list.size());
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
    }

    /**
     * remove object
     */
    @Test
    public void remove4() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertFalse(list.remove("4"));
    }

    /**
     * remove object with null
     */
    @Test
    public void remove5() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", null, "3"));
        assertTrue(list.remove("1"));
        assertEquals(2, list.size());
        assertNull(list.get(0));
        assertEquals("3", list.get(1));
        assertTrue(list.remove(null));
        assertEquals(1, list.size());
        assertEquals("3", list.get(0));
    }

    /**
     * remove index
     */
    @Test
    public void remove6() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertEquals("3", list.remove(2));
        assertEquals(2, list.size());
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
    }

    /**
     * removeAll
     */
    @Test
    public void removeAll() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", "2", "3"));
        assertTrue(list.removeAll(Arrays.asList("2", "3")));
        assertEquals(1, list.size());
        assertEquals("1", list.get(0));
    }

    /**
     * removeAll
     */
    @Test
    public void removeAll2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", "2", "3"));
        assertFalse(list.removeAll(Arrays.asList("4")));
    }

    /**
     * retainAll
     */
    @Test
    public void retainAll() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", "2", "3"));
        assertTrue(list.retainAll(Arrays.asList("2", "3")));
        assertEquals(4, list.size());
        assertEquals("2", list.get(0));
    }

    /**
     * retainAll2
     */
    @Test
    public void retainAll2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", "2", "3"));
        assertFalse(list.retainAll(Arrays.asList("1", "2", "3")));
    }

    /**
     * indexOf
     */
    @Test
    public void indexOf() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertEquals(1, list.indexOf("2"));
    }

    /**
     * indexOf not found
     */
    @Test
    public void indexOf2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", null));
        assertEquals(-1, list.indexOf("4"));
    }

    /**
     * indexOf not found
     */
    @Test
    public void indexOf3() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", null, "3"));
        assertEquals(2, list.indexOf(null));
    }

    /**
     * lastIndexOf
     */
    @Test
    public void lastIndexOf() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", "2", "3", null));
        assertEquals(3, list.lastIndexOf("2"));
    }

    /**
     * lastIndexOf
     */
    @Test
    public void lastIndexOf2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", "2", "3"));
        assertEquals(-1, list.lastIndexOf("4"));
    }

    /**
     * lastIndexOf
     */
    @Test
    public void lastIndexOf3() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", "2", "3", null));
        assertEquals(5, list.lastIndexOf(null));
    }

    /**
     * set
     */
    @Test
    public void set() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        list.set(1, "4");
        assertEquals(3, list.size());
        assertEquals("4", list.get(1));
    }

    /**
     * set out of bounds
     */
    @Test
    public void set2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        try {
            list.set(3, "4");
            fail("should get IOBE");
        } catch (IndexOutOfBoundsException ex) {
            // OK
        }
    }

    /**
     * get out of bounds
     */
    @Test
    public void get() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        try {
            @SuppressWarnings("unused")
            String r = list.get(3);
            fail("should get IOBE");
        } catch (IndexOutOfBoundsException ex) {
            // OK
        }
    }

    /**
     * contains
     */
    @Test
    public void contains() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3", null));
        assertTrue(list.contains("1"));
        assertTrue(list.contains("2"));
        assertTrue(list.contains("3"));
        assertTrue(list.contains(null));
        assertFalse(list.contains("4"));
    }

    /**
     * contains
     */
    @Test
    public void contains2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertFalse(list.contains(null));
    }

    /**
     * containsAll
     */
    @Test
    public void containsAll() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertTrue(list.containsAll(Arrays.asList("2", "3")));
        assertFalse(list.containsAll(Arrays.asList("4", "5")));
    }

    /**
     * isEmpty
     */
    @Test
    public void isEmpty() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1"));
        assertTrue(list.remove("1"));
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    /**
     * toArray
     */
    @Test
    public void toArray() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        Object[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals("1", array[0]);
        assertEquals("2", array[1]);
        assertEquals("3", array[2]);
    }

    /**
     * toArray
     */
    @Test
    public void toArray2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        String[] array = list.toArray(new String[3]);
        assertEquals(3, array.length);
        assertEquals("1", array[0]);
        assertEquals("2", array[1]);
        assertEquals("3", array[2]);
    }

    /**
     * toArray
     */
    @Test
    public void toArray3() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        String[] array = list.toArray(new String[2]);
        assertEquals(3, array.length);
        assertEquals("1", array[0]);
        assertEquals("2", array[1]);
        assertEquals("3", array[2]);
    }

    /**
     * iterator
     */
    @Test
    public void iterator() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        Iterator<String> it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals("1", it.next());
        assertTrue(it.hasNext());
        assertEquals("2", it.next());
        assertTrue(it.hasNext());
        assertEquals("3", it.next());
        assertFalse(it.hasNext());
        try {
            @SuppressWarnings("unused")
            String v = it.next();
            fail("should get IOBE");
        } catch (NoSuchElementException ex) {
            // OK
        }
    }

    /**
     * addAll with index
     */
    @Test
    public void addAllIndex() {
        LinkedList<String> first = new LinkedList<>(Arrays.asList("1", "2", "3"));
        LinkedList<String> last = new LinkedList<>(Arrays.asList("4", "5", "6"));
        assertTrue(first.addAll(1, last));
        assertEquals(6, first.size());
        assertEquals("4", first.get(1));
    }

    /**
     * addAll with index
     */
    @Test
    public void addAllIndex2() {
        LinkedList<String> first = new LinkedList<>(Arrays.asList("1", "2", "3"));
        LinkedList<String> last = new LinkedList<>(Arrays.asList());
        assertFalse(first.addAll(1, last));
    }

    /**
     * addMember
     */
    @Test
    public void addMember() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        Member<String> m = list.addMember("4");
        assertEquals("4", m.getElement());
        assertEquals(4, list.size());
        assertEquals("4", list.get(3));
    }

    /**
     * addBefore
     */
    @Test
    public void addBefore() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        Member<String> m = list.getMember(1);
        Member<String> before = list.addBefore(m, "4");
        assertEquals("4", before.getElement());
        assertEquals(4, list.size());
        assertEquals("4", list.get(1));
    }

    /**
     * addAfter
     */
    @Test
    public void addAfter() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        Member<String> m = list.getMember(1);
        Member<String> after = list.addAfter(m, "4");
        assertEquals("4", after.getElement());
        assertEquals(4, list.size());
        assertEquals("4", list.get(2));
    }

    /**
     * getMember
     */
    @Test
    public void getMember() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        assertEquals("2", list.getMember(1).getElement());
        try {
            @SuppressWarnings("unused")
            Member<String> m = list.getMember(3);
            fail("should get IOBE");
        } catch (IndexOutOfBoundsException ex) {
            // OK
        }
    }

    /**
     * listIterator
     */
    @Test
    public void listIterator() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        try {
            @SuppressWarnings("unused")
            ListIterator<String> li = list.listIterator();
            fail("listIterator should be uninplementd");
        } catch (UnsupportedOperationException ex) {
            // OK
        }
    }

    /**
     * listIterator with arg
     */
    @Test
    public void listIterator2() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        try {
            @SuppressWarnings("unused")
            ListIterator<String> li = list.listIterator(2);
            fail("listIterator should be uninplementd");
        } catch (UnsupportedOperationException ex) {
            // OK
        }
    }

    /**
     * subList
     */
    @Test
    public void subList() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("1", "2", "3"));
        try {
            @SuppressWarnings("unused")
            List<String> li = list.subList(1, 3);
            fail("subList should be uninplementd");
        } catch (UnsupportedOperationException ex) {
            // OK
        }
    }
}