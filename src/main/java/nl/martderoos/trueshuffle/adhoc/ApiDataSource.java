package nl.martderoos.trueshuffle.adhoc;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

public interface ApiDataSource<T> extends DataSource<T, FatalRequestResponse> {

}
