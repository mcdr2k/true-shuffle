package trueshuffle.adhoc;

import trueshuffle.requests.exceptions.FatalRequestResponse;

import java.util.concurrent.TimeUnit;

public class LazyExpiringApiData<T> extends LazyExpiringData<T, FatalRequestResponse> {

    public LazyExpiringApiData(DataSource<T, FatalRequestResponse> source) {
        super(source);
    }

    public LazyExpiringApiData(DataSource<T, FatalRequestResponse> source, boolean forceReloadOnNull) {
        super(source, forceReloadOnNull);
    }

    public LazyExpiringApiData(DataSource<T, FatalRequestResponse> source, long refreshTimeout, TimeUnit timeUnit) {
        super(source, refreshTimeout, timeUnit);
    }

    public LazyExpiringApiData(DataSource<T, FatalRequestResponse> source, boolean forceReloadOnNull, long refreshTimeout, TimeUnit timeUnit) {
        super(source, forceReloadOnNull, refreshTimeout, timeUnit);
    }
}
