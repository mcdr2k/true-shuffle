package nl.martderoos.trueshuffle.paging;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.model_objects.specification.Paging;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class PageAggregatorTest {
    @Test
    public void testNoData() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader());
        var future = new SpotifyFuturePage<>(spyLoader, 0, 100);
        var result = PageAggregator.aggregate(future, 5);
        assertEquals(List.of(), result);
        verify(spyLoader, times(1)).loadPage(eq(0), anyInt());
    }

    @Test
    public void testHardLimit5() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 0, 100);
        var result = PageAggregator.aggregate(future, 5);
        assertEquals(List.of(1, 2, 3, 4, 5), result);
        verify(spyLoader, times(1)).loadPage(eq(0), anyInt());
    }

    @Test
    public void testHardLimitZero() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 0, 100);
        var result = PageAggregator.aggregate(future, 0);
        assertEquals(List.of(), result);
        verify(spyLoader, times(1)).loadPage(eq(0), anyInt());
    }

    @Test
    public void testHardLimitEnd() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 0, 100);
        var result = PageAggregator.aggregate(future, 10);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result);
        verify(spyLoader, times(1)).loadPage(eq(0), anyInt());
    }

    @Test
    public void testHardLimitExceedingDataSize() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 0, 100);
        var result = PageAggregator.aggregate(future, 333);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result);
        verify(spyLoader, times(1)).loadPage(eq(0), anyInt());
    }

    @Test
    public void testPageLimitWithoutHardLimit() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 0, 3);
        var result = PageAggregator.aggregate(future, Integer.MAX_VALUE);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), result);
        verify(spyLoader, times(4)).loadPage(anyInt(), anyInt());
    }

    @Test
    public void testPageLimitWithHardLimitOf8() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 0, 3);
        var result = PageAggregator.aggregate(future, 8);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), result);
        // loads 2 pages with 3 items and 1 page with just 2 items
        verify(spyLoader, times(3)).loadPage(anyInt(), anyInt());
    }

    @Test
    public void testRetrieval1By1WithHardLimit7() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 0, 1);
        var result = PageAggregator.aggregate(future, 7);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), result);
        verify(spyLoader, times(7)).loadPage(anyInt(), anyInt());
    }

    @Test
    public void testRetrieval1By1WithHardLimit5AndOffset4() throws FatalRequestResponseException {
        var spyLoader = spy(new IntLoader(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var future = new SpotifyFuturePage<>(spyLoader, 4, 1);
        var result = PageAggregator.aggregate(future, 5);
        assertEquals(List.of(5, 6, 7, 8, 9), result);
        verify(spyLoader, times(5)).loadPage(anyInt(), anyInt());
    }

    private static class IntLoader implements SpotifyPageLoader<Integer> {
        private final Integer[] data;

        private IntLoader(Integer... data) {
            this.data = data;
        }

        @Override
        public Paging<Integer> loadPage(int offset, int limit) throws FatalRequestResponseException {
            if (offset < 0) throw new FatalRequestResponseException("Invalid offset");
            if (limit < 0) throw new FatalRequestResponseException("Invalid limit");
            int min = Math.min(limit, data.length - offset);
            var items = new Integer[min];
            System.arraycopy(data, offset, items, 0, min);
            String next = null;
            var nextOffset = offset + items.length;
            if (nextOffset < data.length) next = nextOffset + ":" + limit;
            return new Paging.Builder<Integer>()
                    .setPrevious("undefined")
                    .setHref(offset + ":" + limit)
                    .setNext(next)
                    .setLimit(limit)
                    .setOffset(offset)
                    .setTotal(data.length)
                    .setItems((items))
                    .build();
        }
    }
}
