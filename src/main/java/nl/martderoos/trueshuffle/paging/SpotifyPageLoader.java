package nl.martderoos.trueshuffle.paging;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import se.michaelthelin.spotify.model_objects.specification.Paging;

/**
 * Functional interface that describes loading a specific page from paginated data.
 *
 * @param <T> the type of data which this loader can return pages of
 */
@FunctionalInterface
public interface SpotifyPageLoader<T> {
    /**
     * Loads a specific page from paginated data.
     *
     * @param offset the offset to start loading data from where an offset of 0 indicates the first item of all data.
     * @param limit  the maximum number of items the page may contain.
     * @return the items captured by the page specification.
     * @throws FatalRequestResponseException when an issue is encountered while leveraging the Spotify API
     */
    Paging<T> loadPage(int offset, int limit) throws FatalRequestResponseException;
}
