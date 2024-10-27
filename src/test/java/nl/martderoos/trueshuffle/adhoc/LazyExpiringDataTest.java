package nl.martderoos.trueshuffle.adhoc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LazyExpiringDataTest {
    @Test
    public void testLaziness() {
        var source = new Source();
        var data = new LazyExpiringData<>(source);
        assertFalse(source.loaded);
        assertEquals(0, source.x);
        data.getData(true);
        assertTrue(source.loaded);
    }

    @Test
    public void testGetData() {
        var data = new LazyExpiringData<>(new Source());
        assertEquals(1, data.getData());
        assertEquals(1, data.getData());
        assertEquals(2, data.getData(true));
        assertEquals(2, data.getData());
    }

    @Test
    public void testSetData() {
        var data = new LazyExpiringData<>(new Source());
        data.setData(59);
        assertEquals(59, data.getData());
        assertEquals(59, data.getData());
    }

    @Test
    public void testInvalidate() {
        var data = new LazyExpiringData<>(new Source());
        assertEquals(1, data.getData());
        data.invalidate();
        assertEquals(2, data.getData());
        assertEquals(2, data.getData());
    }

    @Test
    public void testValidateNull() {
        var data = new LazyExpiringData<>(new Source(), false);
        data.validate();
        assertNull(data.getData());
    }

    @Test
    public void testValidateAfterInvalidateResultsInNull() {
        var data = new LazyExpiringData<>(new Source(), false);
        assertEquals(1, data.getData());
        data.invalidate();
        data.validate();
        assertNull(data.getData());
    }

    @Test
    public void testQuickRefreshTimeout() throws InterruptedException {
        var data = new LazyExpiringData<>(new Source(), 5, TimeUnit.MILLISECONDS);
        assertEquals(1, data.getData());
        Thread.sleep(6);
        assertEquals(2, data.getData());
        Thread.sleep(6);
        assertEquals(3, data.getData());
        Thread.sleep(6);
        assertEquals(4, data.getData());
        Thread.sleep(6);
        assertEquals(5, data.getData());
    }

    @Test
    public void testRefreshTimeoutWithValidateForAtLeast() throws InterruptedException {
        var data = new LazyExpiringData<>(new Source(), 10, TimeUnit.MILLISECONDS);
        // note that this test could potentially fail it not given enough CPU time
        assertEquals(1, data.getData());
        assertEquals(1, data.getData());
        Thread.sleep(11);
        assertEquals(2, data.getData());
        data.validateForAtLeast(1, TimeUnit.MINUTES);
        Thread.sleep(20);
        assertEquals(2, data.getData()); // still valid
    }

    @Test
    public void testValidateForAtLeastPreferLongestValidation() {
        var data = new LazyExpiringData<>(new Source(), 100, TimeUnit.MILLISECONDS);
        assertEquals(1, data.getData());
        data.validateForAtLeast(0, TimeUnit.MINUTES);
        assertEquals(1, data.getData());
    }

    private static class Source implements DataSource<Integer, RuntimeException> {
        private boolean loaded = false;
        private int x;

        @Override
        public Integer load() {
            loaded = true;
            return ++x;
        }
    }
}
