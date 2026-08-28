package nl.martderoos.trueshuffle.adhoc;

import nl.martderoos.trueshuffle.exceptions.FatalRequestResponseException;

import java.util.concurrent.TimeUnit;

public class LazyExpiringApiData<T> extends LazyExpiringData<T, FatalRequestResponseException> {

    public LazyExpiringApiData(DataSource<T, FatalRequestResponseException> source) {
        super(source);
    }

    public LazyExpiringApiData(DataSource<T, FatalRequestResponseException> source, boolean forceReloadOnNull, long refreshTimeout, TimeUnit timeUnit) {
        super(source, forceReloadOnNull, refreshTimeout, timeUnit);
    }
}
