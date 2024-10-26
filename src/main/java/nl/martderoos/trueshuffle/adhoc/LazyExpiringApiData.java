package nl.martderoos.trueshuffle.adhoc;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;

import java.util.concurrent.TimeUnit;

public class LazyExpiringApiData<T> extends LazyExpiringData<T, FatalRequestResponseException> {

    public LazyExpiringApiData(DataSource<T, FatalRequestResponseException> source) {
        super(source);
    }

    public LazyExpiringApiData(DataSource<T, FatalRequestResponseException> source, boolean forceReloadOnNull) {
        super(source, forceReloadOnNull);
    }

    public LazyExpiringApiData(DataSource<T, FatalRequestResponseException> source, long refreshTimeout, TimeUnit timeUnit) {
        super(source, refreshTimeout, timeUnit);
    }

    public LazyExpiringApiData(DataSource<T, FatalRequestResponseException> source, boolean forceReloadOnNull, long refreshTimeout, TimeUnit timeUnit) {
        super(source, forceReloadOnNull, refreshTimeout, timeUnit);
    }
}
