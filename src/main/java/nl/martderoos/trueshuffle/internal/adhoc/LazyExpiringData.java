package nl.martderoos.trueshuffle.internal.adhoc;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Thread-safe class that encapsulates lazily evaluated data that may expire over time.
 *
 * @param <T> The data to store.
 * @param <E> The exception that can be thrown when attempting to reload data.
 */
public class LazyExpiringData<T, E extends Exception> {
    private final boolean forceReloadOnNull;
    private final DataSource<T, E> source;
    private final long refreshTimeoutNanos;
    private final LongSupplier clock;

    private volatile T data;
    private volatile long validUntil;
    private volatile boolean valid;

    public LazyExpiringData(DataSource<T, E> source) {
        this(source, true);
    }

    public LazyExpiringData(DataSource<T, E> source, boolean forceReloadOnNull) {
        this(source, forceReloadOnNull, 10, TimeUnit.MINUTES);
    }

    public LazyExpiringData(DataSource<T, E> source, long refreshTimeout, TimeUnit timeUnit) {
        this(source, true, refreshTimeout, timeUnit);
    }

    public LazyExpiringData(DataSource<T, E> source, boolean forceReloadOnNull, long refreshTimeout, TimeUnit timeUnit) {
        this(source, forceReloadOnNull, refreshTimeout, timeUnit, System::nanoTime);
    }

    LazyExpiringData(DataSource<T, E> source, boolean forceReloadOnNull, long refreshTimeout, TimeUnit timeUnit, LongSupplier clock) {
        this.source = Objects.requireNonNull(source);
        this.forceReloadOnNull = forceReloadOnNull;
        if (refreshTimeout < 0)
            throw new IllegalArgumentException("refreshTimeout must not be negative");
        this.refreshTimeoutNanos = Objects.requireNonNull(timeUnit).toNanos(refreshTimeout);
        this.clock = Objects.requireNonNull(clock);
    }

    private synchronized T checkReload(boolean forceReload) throws E {
        var data = this.data;
        if (forceReload || !valid || clock.getAsLong() >= validUntil) {
            expire();
            return reload();
        }
        return data;
    }

    private synchronized T reload() throws E {
        var data = source.load();
        setData(data);
        return data;
    }

    /**
     * Invalidate the current data which will force a reload upon the next attempt to get the data
     */
    public final synchronized void invalidate() {
        data = null;
        valid = false;
    }

    /**
     * Validate the current data for the configured amount of time as provided to the constructor
     */
    public final synchronized void validate() {
        validateForAtLeast(refreshTimeoutNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Mark the current data as expired without discarding it.
     */
    public final synchronized void expire() {
        valid = false;
    }

    /**
     * Get the currently cached value without attempting to load or refresh it.
     *
     * @return the last successfully loaded value, possibly null.
     */
    public final synchronized T getCachedData() {
        return data;
    }

    /**
     * Set the data. This method will also call {@link #validate()} to ensure that this new data is valid for the
     * configured amount of time provided to the constructor.
     */
    public final synchronized void setData(T data) {
        this.data = data;
        validate();
    }

    /**
     * Validate the current data held for at least the provided amount of time.
     * If the current data is valid for longer than the provided amount of time to validate for, then this call
     * will have no effect.
     *
     * @param validForAtLeast The value of time
     * @param timeUnit        The unit of time
     */
    public final synchronized void validateForAtLeast(long validForAtLeast, TimeUnit timeUnit) {
        if (validForAtLeast < 0)
            throw new IllegalArgumentException("validForAtLeast must not be negative");

        var now = clock.getAsLong();
        var durationNanos = Objects.requireNonNull(timeUnit).toNanos(validForAtLeast);
        var newValidUntil = saturatingAdd(now, durationNanos);
        if (!valid || newValidUntil - validUntil > 0)
            validUntil = newValidUntil;
        valid = true;
    }

    private static long saturatingAdd(long value, long amount) {
        if (amount > 0 && value > Long.MAX_VALUE - amount)
            return Long.MAX_VALUE;
        return value + amount;
    }

    /**
     * Get the data if available. Will cause a reload if the data is not available. If the {@link DataSource} is configured
     * to be able to return null, then this function may also return null.
     *
     * @return The already available data or the newly retrieved data if it were not available.
     * @throws E When the {@link DataSource} throws an exception.
     */
    public final T getData() throws E {
        return getData(false);
    }

    /**
     * Get the data if available. Will cause a reload if the data is not available. If the {@link DataSource} is configured
     * to be able to return null, then this function may also return null.
     *
     * @param forceReload Whether to reload regardless of the current available data.
     * @return The already available data or the newly retrieved data if it were not available.
     * @throws E When the {@link DataSource} throws an exception.
     */
    public final T getData(boolean forceReload) throws E {
        return checkReload(forceReload || (forceReloadOnNull && data == null));
    }
}
