package nl.martderoos.trueshuffle.adhoc;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

/**
 * Specification for a {@link DataSource} that may throw a {@link FatalRequestResponse}
 * @param <T> the type of data this source may produce
 */
public interface ApiDataSource<T> extends DataSource<T, FatalRequestResponse> {

}
