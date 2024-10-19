package trueshuffle.paging;

import trueshuffle.requests.exceptions.FatalRequestResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PageAggregator {
    private PageAggregator() {

    }

    public static <T> List<T> aggregate(SpotifyFuturePage<T> target, int hardLimit) throws FatalRequestResponse {
        var page = target.execute();
        hardLimit = Math.min(page.getTotal(), hardLimit);

        List<T> result = new ArrayList<>(hardLimit);
        addSome(result, Arrays.asList(page.getItems()), hardLimit);

        var next = page.getNext();
        while (next != null && result.size() < hardLimit) {
            page = next.execute();
            addSome(result, Arrays.asList(page.getItems()), hardLimit - result.size());
            next = page.getNext();
        }

        return result;
    }

    private static <T> void addSome(List<T> target, List<T> items, int count) {
        target.addAll(items.subList(0, Math.min(items.size(), count)));
    }
}
