package nl.martderoos.trueshuffle.paging;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for aggregating pages of data into a single list
 */
public class PageAggregator {
    private static final Logger LOGGER = LogManager.getLogger(PageAggregator.class);

    private PageAggregator() {

    }

    /**
     * Aggregates the provided initial page's items and all subsequent pages' items into a single list of items. This
     * method will filter out null objects if allowed.
     *
     * @param initialPage the first page from which we will start aggregating.
     * @param hardLimit   the limit on the total number of items this function may return at most.
     * @param <T>         the type of items we may return
     * @param allowAndFilterNulls whether you allow that Spotify returns null for some items in a page. If allowed, the
     *                            null values will be filtered out of the result. If not allowed, this method will throw
     *                            a {@link FatalRequestResponseException}.
     * @return the list of items, never null but may be empty.
     * @throws FatalRequestResponseException when a page fails to load.
     */
    public static <T> List<T> aggregate(SpotifyFuturePage<T> initialPage, int hardLimit, boolean allowAndFilterNulls) throws FatalRequestResponseException {
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

        var nonNullableResult = result.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (nonNullableResult.size() != result.size()) {
            int nullCount = result.size() - nonNullableResult.size();
            if (!allowAndFilterNulls) {
                throw new FatalRequestResponseException(String.format("Found %s null values among the aggregated pages, which is not allowed.", nullCount));
            }
            LOGGER.debug("Null values were found among the aggregated pages. In particular, we found {} null values in a list of {} possibly null values.", nullCount, result.size());
        }
        return nonNullableResult;
    }


    private static <T> void addSome(List<T> target, List<T> items, int count) {
        target.addAll(items.subList(0, Math.min(items.size(), count)));
    }
}
