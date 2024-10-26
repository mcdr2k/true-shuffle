package nl.martderoos.trueshuffle.paging;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for aggregating pages of data into a single list
 */
public class PageAggregator {
    private PageAggregator() {

    }

    /**
     * Aggregates the provided initial page's items and all subsequent pages' items into a single list of items.
     *
     * @param initialPage the first page from which we will start aggregating.
     * @param hardLimit   the limit on the total number of items this function may return at most.
     * @param <T>         the type of items we may return
     * @return the list of items, never null but may be empty.
     * @throws FatalRequestResponseException when a page fails to load.
     */
    public static <T> List<T> aggregate(SpotifyFuturePage<T> initialPage, int hardLimit) throws FatalRequestResponseException {
        var page = initialPage.load();
        hardLimit = Math.min(page.getTotal(), hardLimit);

        List<T> result = new ArrayList<>(hardLimit);
        addSome(result, Arrays.asList(page.getItems()), hardLimit);

        var next = page.getNext();
        while (next != null && result.size() < hardLimit) {
            page = next.load();
            addSome(result, Arrays.asList(page.getItems()), hardLimit - result.size());
            next = page.getNext();
        }

        return result;
    }


    private static <T> void addSome(List<T> target, List<T> items, int count) {
        target.addAll(items.subList(0, Math.min(items.size(), count)));
    }
}
