package nl.martderoos.trueshuffle.paging;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;

import java.util.Objects;

/**
 * Describes a future of a page of items, producing a {@link SpotifyResultPage<T>}.
 *
 * @param <T> the type of items this future may capture.
 */
public class SpotifyFuturePage<T> {
    private final SpotifyPageLoader<T> pageLoader;

    private final int offset;
    private final int limit;

    /**
     * Create a new future. Calls {@link #SpotifyFuturePage(SpotifyPageLoader, int)} with an offset of 0.
     *
     * @param pageLoader the loader that can be used to load this page.
     */
    public SpotifyFuturePage(SpotifyPageLoader<T> pageLoader) {
        this(pageLoader, 0);
    }

    /**
     * Create a new future. Calls {@link #SpotifyFuturePage(SpotifyPageLoader, int, int)} with a limit of 50.
     * Note that the default of 50 is in line with most (if not all) Spotify API calls. Retrieving data from Spotify
     * usually restricts us to a maximum of 50 items per request.
     *
     * @param pageLoader the loader that can be used to load this page.
     * @param offset     the offset of this page, which are the amount of skipped items before starting to capture items
     *                   for this page.
     */
    public SpotifyFuturePage(SpotifyPageLoader<T> pageLoader, int offset) {
        this(pageLoader, offset, 50);
    }

    /**
     * Create a new future with provided arguments.
     *
     * @param pageLoader the loader that can be used to load this page.
     * @param offset     the offset of this page, which are the amount of skipped items before starting to capture items
     *                   for this page.
     * @param limit      the maximum number of items this page may contain upon load.
     */
    public SpotifyFuturePage(SpotifyPageLoader<T> pageLoader, int offset, int limit) {
        if (offset < 0) throw new IllegalArgumentException("Offset must be at least 0");
        if (limit < 0) throw new IllegalArgumentException("Limit must be at least 0");
        this.pageLoader = Objects.requireNonNull(pageLoader);
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * Load this page.
     *
     * @return a result page, never null.
     * @throws FatalRequestResponseException when the page fails to load.
     */
    public SpotifyResultPage<T> load() throws FatalRequestResponseException {
        return new SpotifyResultPage<>(pageLoader.loadPage(offset, limit), pageLoader);
    }

    /**
     * Get the offset of this future. The offset determines the index of the first item this future will load.
     *
     * @return the offset of this future, at least 0.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Get the maximum number of elements this future may load.
     *
     * @return the maximum number of elements this future may load, at least 0.
     */
    public int getLimit() {
        return limit;
    }
}
