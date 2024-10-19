package trueshuffle.adhoc;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LazyExpiringData<T, E extends Exception> {
    private final boolean forceReloadOnNull;
    private final DataSource<T, E> source;
    private final long refreshTimeoutMillis;

    private T data;
    private long validTill = 0;

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

    private void checkReload(boolean forceReload) throws E {
        if (forceReload || System.currentTimeMillis() > validTill) {
            invalidate();
            reload();
        }
    }

    /**
     * Invalidates the current data held by this class, resulting in a reload upon the next call to {@link #getData(boolean)}.
     * Invalidate is always called before a reload happens.
     */
    public void invalidate() {
        data = null;
        validTill = 0;
    }

    public final void validate() {
        validateForAtLeast(refreshTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void validateForAtLeast(long validForAtLeast, TimeUnit timeUnit) {
        var stillValidFor = validTill - System.currentTimeMillis();
        validForAtLeast = timeUnit.toMillis(validForAtLeast);
        if (stillValidFor < validForAtLeast)
            validTill = System.currentTimeMillis() + validForAtLeast;
    }

    public final void validateWith(T data) {
        this.data = data;
        validate();
    }

    private void reload() throws E {
        validateWith(source.load());
    }

    public final T getData() throws E {
        return getData(false);
    }

    public final T getData(boolean forceReload) throws E {
        checkReload(forceReload || (forceReloadOnNull && data == null));
        return data;
    }
}
