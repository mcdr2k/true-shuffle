package nl.martderoos.trueshuffle.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ItemCounterTest {
    @Test
    public void testContains() {
        var counter = new ItemCounter<Integer>();
        counter.addAll(List.of(1, 2, 3, 4, 5));
        assertTrue(counter.contains(1));
        assertTrue(counter.contains(5));

        assertFalse(counter.contains(0));
        assertFalse(counter.contains(6));
    }

    @Test
    public void testGetCount() {
        var counter = new ItemCounter<>(List.of(1, 2, 2, 4, 5, 2, 5, 2));

        assertEquals(0, counter.getCount(0));
        assertEquals(1, counter.getCount(1));
        assertEquals(4, counter.getCount(2));
        assertEquals(0, counter.getCount(3));
        assertEquals(1, counter.getCount(4));
        assertEquals(2, counter.getCount(5));
        assertEquals(0, counter.getCount(6));
    }

    @Test
    public void testRemove() {
        var counter = new ItemCounter<>(List.of(1, 2, 2, 4, 5, 2, 5, 2));
        assertFalse(counter.remove(0));

        assertTrue(counter.remove(1));
        assertFalse(counter.remove(1));

        assertTrue(counter.remove(2));
        assertTrue(counter.remove(2));
        assertTrue(counter.remove(2));
        assertTrue(counter.remove(2));
        assertFalse(counter.remove(2));
    }
}
