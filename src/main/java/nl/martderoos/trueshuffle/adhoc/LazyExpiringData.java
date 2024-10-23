package nl.martderoos.trueshuffle.adhoc;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe class that encapsulates lazily evaluated data that may expire over time.
 *
 * @param <T> The data to store.
 * @param <E> The exception that can be thrown when attempting to reload data.
 */
public class LazyExpiringData<T, E extends Exception> {
    private final boolean forceReloadOnNull;
    private final DataSource<T, E> source;
    private final long refreshTimeoutMillis;

    private volatile T data;
    private volatile long validTill = 0;

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
        this.source = Objects.requireNonNull(source);
        this.forceReloadOnNull = forceReloadOnNull;
        this.refreshTimeoutMillis = Objects.requireNonNull(timeUnit).toMillis(refreshTimeout);
    }

    private synchronized T checkReload(boolean forceReload) throws E {
        var data = this.data;
        if (forceReload || System.currentTimeMillis() > validTill) {
            invalidate();
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
        validTill = 0;
    }

    /**
     * Validate the current data for the configured amount of time as provided to the constructor
     */
    public final synchronized void validate() {
        validateForAtLeast(refreshTimeoutMillis, TimeUnit.MILLISECONDS);
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
        var stillValidFor = validTill - System.currentTimeMillis();
        validForAtLeast = timeUnit.toMillis(validForAtLeast);
        if (stillValidFor < validForAtLeast)
            validTill = System.currentTimeMillis() + validForAtLeast;
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
