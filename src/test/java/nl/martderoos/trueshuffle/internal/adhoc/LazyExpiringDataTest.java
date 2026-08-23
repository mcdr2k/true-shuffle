package nl.martderoos.trueshuffle.internal.adhoc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

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
    public void testQuickRefreshTimeout() {
        var clock = new TestClock();
        var data = new LazyExpiringData<>(new Source(), true, 5, TimeUnit.MILLISECONDS, clock);
        assertEquals(1, data.getData());
        clock.advanceMillis(6);
        assertEquals(2, data.getData());
        clock.advanceMillis(6);
        assertEquals(3, data.getData());
        clock.advanceMillis(6);
        assertEquals(4, data.getData());
        clock.advanceMillis(6);
        assertEquals(5, data.getData());
    }

    @Test
    public void testRefreshTimeoutWithValidateForAtLeast() {
        var clock = new TestClock();
        var data = new LazyExpiringData<>(new Source(), true, 10, TimeUnit.MILLISECONDS, clock);
        assertEquals(1, data.getData());
        assertEquals(1, data.getData());
        clock.advanceMillis(11);
        assertEquals(2, data.getData());
        data.validateForAtLeast(1, TimeUnit.MINUTES);
        clock.advanceMillis(20);
        assertEquals(2, data.getData()); // still valid
    }

    @Test
    public void testValidateForAtLeastPreferLongestValidation() {
        var data = new LazyExpiringData<>(new Source(), 100, TimeUnit.MILLISECONDS);
        assertEquals(1, data.getData());
        data.validateForAtLeast(0, TimeUnit.MINUTES);
        assertEquals(1, data.getData());
    }

    @Test
    public void testNegativeDurationsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new LazyExpiringData<>(new Source(), -1, TimeUnit.SECONDS));

        var data = new LazyExpiringData<>(new Source());
        assertThrows(IllegalArgumentException.class,
                () -> data.validateForAtLeast(-1, TimeUnit.SECONDS));
    }

    @Test
    public void testVeryLargeDurationDoesNotExpireImmediately() {
        var data = new LazyExpiringData<>(new Source(), Long.MAX_VALUE, TimeUnit.DAYS);

        assertEquals(1, data.getData());
        assertEquals(1, data.getData());
    }

    @Test
    public void testCachedValueIsRetainedAfterRefreshFailure() throws Exception {
        var source = new FailingSource();
        var data = new LazyExpiringData<>(source, true, 1, TimeUnit.MINUTES, new TestClock());

        assertEquals(1, data.getData());
        source.fail = true;
        data.expire();
        assertThrows(Exception.class, data::getData);
        assertEquals(1, data.getCachedData());

        source.fail = false;
        assertEquals(2, data.getData());
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

    private static class TestClock implements LongSupplier {
        private long nanos;

        @Override
        public long getAsLong() {
            return nanos;
        }

        void advanceMillis(long millis) {
            nanos += TimeUnit.MILLISECONDS.toNanos(millis);
        }
    }

    private static class FailingSource implements DataSource<Integer, Exception> {
        private boolean fail;
        private int value;

        @Override
        public Integer load() throws Exception {
            if (fail)
                throw new Exception("load failed");
            return ++value;
        }
    }
}
