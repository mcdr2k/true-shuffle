package nl.martderoos.trueshuffle.paging;

import se.michaelthelin.spotify.model_objects.specification.Paging;

/**
 * Capture of items for a specific loaded page. It is a direct product of executing a {@link SpotifyFuturePage<T>}.
 * Contains additional functionality for navigating through multiple pages.
 *
 * @param <T> the type of the items this page may capture.
 */
public class SpotifyResultPage<T> {
    private final Paging<T> paging;

    private final SpotifyFuturePage<T> next;

    /**
     * Create a new page (capture) of items.
     *
     * @param paging a page of items.
     * @param loader the loader to use for the next page of items, which should be the same loader that was used to
     *               load this page's items.
     */
    public SpotifyResultPage(Paging<T> paging, SpotifyPageLoader<T> loader) {
        this.paging = paging;

        if (paging.getNext() == null) {
            this.next = null;
        } else {
            this.next = new SpotifyFuturePage<>(loader, getOffset() + getItems().length, getLimit());
        }
    }

    /**
     * Get the items captured by this page.
     *
     * @return the items captured, never null.
     */
    public T[] getItems() {
        return paging.getItems();
    }

    /**
     * Get the total number of paginated items one would end up with if one were to aggregate this
     * {@link #getItems() page's items} along with all previous pages' items and next pages' items.
     * All previous and next pages will return the same total.
     */
    public int getTotal() {
        return paging.getTotal();
    }

    /**
     * Get the offset used for this page.
     */
    public int getOffset() {
        return paging.getOffset();
    }

    /**
     * Get the limit used for this page, the maximum number of items the page may ever contain.
     */
    public int getLimit() {
        return paging.getLimit();
    }

    /**
     * Get the next page, if any.
     *
     * @return a future of the next page, or null if this is the last page.
     */
    public SpotifyFuturePage<T> getNext() {
        return next;
    }
}
