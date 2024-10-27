package nl.martderoos.trueshuffle.paging;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.model_objects.specification.Paging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SpotifyFuturePageTest {
    private final SpotifyPageLoader<Object> uselessLoader = (o, l) -> null;

    @Test
    public void testSingleArgConstructor() {
        var future = new SpotifyFuturePage<>(uselessLoader);
        assertEquals(0, future.getOffset());
        assertEquals(50, future.getLimit());
        assertThrows(NullPointerException.class, () -> new SpotifyFuturePage<>(null));
    }

    @Test
    public void testTwoArgConstructor() {
        var future = new SpotifyFuturePage<>(uselessLoader, 5);
        assertEquals(5, future.getOffset());
        assertEquals(50, future.getLimit());

        assertThrows(NullPointerException.class, () -> new SpotifyFuturePage<>(null, 5));
        assertThrows(IllegalArgumentException.class, () -> new SpotifyFuturePage<>(uselessLoader, -1));
    }

    @Test
    public void testThreeArgConstructor() {
        var future = new SpotifyFuturePage<>(uselessLoader, 0, 0);
        assertEquals(0, future.getOffset());
        assertEquals(0, future.getLimit());

        assertThrows(NullPointerException.class, () -> new SpotifyFuturePage<>(null, 5, 5));
        assertThrows(IllegalArgumentException.class, () -> new SpotifyFuturePage<>(uselessLoader, -1, 5));
        assertThrows(IllegalArgumentException.class, () -> new SpotifyFuturePage<>(uselessLoader, 5, -1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFutureLoadFirstResult() throws FatalRequestResponseException {
        var items = new String[]{"first", "second", "third"};
        var paging = new Paging.Builder<String>()
                .setPrevious(null)
                .setHref("/1")
                .setNext("/2")
                .setLimit(3)
                .setOffset(0)
                .setTotal(6)
                .setItems(items)
                .build();

        SpotifyPageLoader<String> loader = mock(SpotifyPageLoader.class);
        when(loader.loadPage(anyInt(), anyInt())).thenReturn(paging);

        var future = new SpotifyFuturePage<>(loader);
        var result = future.load();
        verify(loader).loadPage(eq(0), eq(50));
        assertEquals(0, result.getOffset());
        assertEquals(3, result.getLimit());
        assertEquals(6, result.getTotal());
        assertArrayEquals(new String[]{"first", "second", "third"}, result.getItems());
        assertNotNull(result.getNext());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFutureLoadSecondaryResult() throws FatalRequestResponseException {
        var items = new String[]{"fourth", "fifth"};
        var pagingBuilder = new Paging.Builder<String>()
                .setPrevious("/1")
                .setHref("/2")
                .setNext("/3")
                .setLimit(2)
                .setOffset(3)
                .setTotal(6)
                .setItems(items);

        SpotifyPageLoader<String> loader = mock(SpotifyPageLoader.class);
        when(loader.loadPage(anyInt(), anyInt())).thenReturn(pagingBuilder.build());

        var future = new SpotifyFuturePage<>(loader, 3, 2);
        var result = future.load();
        verify(loader).loadPage(eq(3), eq(2));
        assertEquals(3, result.getOffset());
        assertEquals(2, result.getLimit());
        assertEquals(6, result.getTotal());
        assertArrayEquals(new String[]{"fourth", "fifth"}, result.getItems());
        assertNotNull(result.getNext());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFutureLoadThirdAndLastResult() throws FatalRequestResponseException {
        var items = new String[]{"sixth"};
        var pagingBuilder = new Paging.Builder<String>()
                .setPrevious("/2")
                .setHref("/3")
                .setNext(null)
                .setLimit(10)
                .setOffset(5)
                .setTotal(6)
                .setItems(items);

        SpotifyPageLoader<String> loader = mock(SpotifyPageLoader.class);
        when(loader.loadPage(anyInt(), anyInt())).thenReturn(pagingBuilder.build());

        var future = new SpotifyFuturePage<>(loader, 5, 10);
        var result = future.load();
        verify(loader).loadPage(eq(5), eq(10));
        assertEquals(5, result.getOffset());
        assertEquals(10, result.getLimit());
        assertEquals(6, result.getTotal());
        assertArrayEquals(new String[]{"sixth"}, result.getItems());
        assertNull(result.getNext());
    }
}
