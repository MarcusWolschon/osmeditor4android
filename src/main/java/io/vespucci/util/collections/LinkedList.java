package io.vespucci.util.collections;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Implementation of LinkedList that exposes the List members and allows efficient insertion relative to other members
 * 
 * @author simon
 *
 */
public class LinkedList<V> implements List<V> {

    private final Member<V> head;
    private final Member<V> tail;
    private final Member<V> free;
    private int             size;

    public static class Member<T> {
        private T         element;
        private Member<T> next;
        private Member<T> prev;

        /**
         * Allocate a new Member
         * 
         * @param element the element we are storing in the List
         * @param next the next Member
         * @param prev the previous Member
         */
        public Member(@Nullable T element, @Nullable Member<T> next, @Nullable Member<T> prev) {
            this.element = element;
            this.next = next;
            this.prev = prev;
        }

        /**
         * @return the element
         */
        public T getElement() {
            return element;
        }

        /**
         * @param element the element to set
         */
        public void setElement(T element) {
            this.element = element;
        }
    }

    /**
     * Construct and initialize a new LinkedList
     */
    public LinkedList() {
        head = new Member<>(null, null, null);
        tail = new Member<>(null, null, null);
        free = new Member<>(null, null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Construct and initialize a new LinkedList with initialCapacity Members pre-allocated
     * 
     * @param initialCapacity the initical number of allocated Members
     */
    public LinkedList(int initialCapacity) {
        this();
        for (int i = 0; i < initialCapacity; i++) {
            Member<V> m = new Member<>(null, null, null);
            m.next = free.next;
            free.next = m;
        }
    }

    /**
     * Construct and initialize a new LinkedList from a Collection
     * 
     * @param collection the Collection
     */
    public LinkedList(Collection<? extends V> collection) {
        this();
        for (V v : collection) {
            add(v);
        }
    }

    /**
     * Add element to the end of the list
     * 
     * @param element the element
     * @return the corresponding Member
     */
    @NonNull
    public Member<V> addMember(@Nullable V element) {
        return addBefore(tail, element);
    }

    /**
     * Add element before member
     * 
     * @param member the Member
     * @param element the element
     * @return a Member containing element
     */
    @NonNull
    public Member<V> addBefore(@NonNull Member<V> member, V element) {
        Member<V> m = newMember();
        m.setElement(element);
        Member<V> temp = member.prev;
        member.prev = m;
        m.next = member;
        m.prev = temp;
        temp.next = m;
        size++;
        return m;
    }

    /**
     * Add element after member
     * 
     * @param member the Member
     * @param element the element
     * @return a Member containing element
     */
    @NonNull
    public Member<V> addAfter(@NonNull Member<V> member, V element) {
        Member<V> m = newMember();
        m.setElement(element);
        Member<V> temp = member.next;
        member.next = m;
        m.prev = member;
        m.next = temp;
        temp.prev = m;
        size++;
        return m;
    }

    /**
     * Get a new Member either from the free list, or instantiate a new one
     * 
     * @return a Member
     */
    @NonNull
    private Member<V> newMember() {
        if (free.next != null) {
            Member<V> result = free.next;
            free.next = result.next;
            return result;
        }
        return new Member<>(null, null, null);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        Member<V> current = head.next;
        while (!current.equals(tail)) {
            if ((current.getElement() == null && o == null) || (current.getElement() != null && current.getElement().equals(o))) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    @Override
    public Iterator<V> iterator() {
        return new Iterator<V>() {
            Member<V> current = head;

            @Override
            public boolean hasNext() {
                return !current.next.equals(tail);
            }

            @Override
            public V next() {
                current = current.next;
                if (current.equals(tail)) {
                    throw new NoSuchElementException();
                }
                return current.getElement();
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        return toArray(result);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            @SuppressWarnings("unchecked")
            T[] newArray = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            a = newArray;
        }
        @SuppressWarnings("unchecked")
        Member<T> current = (Member<T>) head.next;
        int i = 0;
        while (!current.equals(tail)) {
            a[i++] = current.element;
            current = current.next;
        }
        return a;
    }

    @Override
    public boolean add(V e) {
        addMember(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        Member<V> current = head.next;
        while (!current.equals(tail)) {
            if ((current.getElement() == null && o == null) || (current.getElement() != null && current.getElement().equals(o))) {
                current.next.prev = current.prev;
                current.prev.next = current.next;
                free(current);
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Move a Member to the free list
     * 
     * @param member the Member to "free"
     */
    private void free(@NonNull Member<V> member) {
        member.next = free.next;
        free.next = member;
        member.setElement(null);
        member.prev = null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object v : c) {
            if (!contains(v)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        for (V v : c) {
            add(v);
        }
        return !c.isEmpty();
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        Member<V> m = getMember(index);
        for (V v : c) {
            addBefore(m, v);
        }
        return !c.isEmpty();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return removeElements(c::contains);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return removeElements(element -> !c.contains(element));
    }

    interface Condition<V> {
        /**
         * Check if a condition is true for element
         * 
         * @param element the element
         * @return true if the condition is true
         */
        boolean check(V element);
    }

    /**
     * Remove elements from the list if a condition is true
     * 
     * @param c the condition
     * @return true if the list was modified
     */
    private boolean removeElements(Condition<V> c) {
        boolean result = false;
        Member<V> current = head.next;
        while (!current.equals(tail)) {
            if (c.check(current.getElement())) {
                Member<V> temp = current.next;
                current.next.prev = current.prev;
                current.prev.next = current.next;
                free(current);
                size--;
                current = temp;
                result = true;
            } else {
                current = current.next;
            }
        }
        return result;
    }

    @Override
    public void clear() {
        // move every thing to the free list
        if (!head.next.equals(tail)) {
            Member<V> temp = head.next;
            head.next = tail;
            tail.prev.next = null;
            tail.prev = head;
            size = 0;
            Member<V> current = temp;
            while (current != null) {
                temp = current.next;
                free(current);
                current = temp;
            }
        }

    }

    @Override
    public V get(int index) {
        Member<V> current = getMember(index);
        return current.getElement();
    }

    /**
     * Get the Member at index
     * 
     * @param index the index
     * @return a Member
     */
    @NonNull
    public Member<V> getMember(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " is larger than " + size);
        }
        Member<V> current = head;
        for (int i = 0; i <= index; i++) {
            current = current.next;
        }
        return current;
    }

    @Override
    public V set(int index, V element) {
        Member<V> member = getMember(index);
        V old = member.getElement();
        member.setElement(element);
        return old;
    }

    @Override
    public void add(int index, V element) {
        Member<V> member = getMember(index);
        addBefore(member, element);
    }

    @Override
    public V remove(int index) {
        Member<V> member = getMember(index);
        V element = member.getElement();
        member.next.prev = member.prev;
        member.prev.next = member.next;
        free(member);
        size--;
        return element;
    }

    @Override
    public int indexOf(Object o) {
        Member<V> current = head.next;
        int index = 0;
        while (!current.equals(tail)) {
            if ((current.getElement() == null && o == null) || (current.getElement() != null && current.getElement().equals(o))) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        Member<V> current = head.next;
        int index = 0;
        int found = -1;
        while (!current.equals(tail)) {
            if ((current.getElement() == null && o == null) || (current.getElement() != null && current.getElement().equals(o))) {
                found = index;
            }
            current = current.next;
            index++;
        }
        return found;
    }

    @Override
    public ListIterator<V> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }
}
