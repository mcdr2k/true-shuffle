package nl.martderoos.trueshuffle.paging;

import se.michaelthelin.spotify.model_objects.specification.Paging;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

@FunctionalInterface
public interface SpotifyPageConsumer<T> {
    Paging<T> consumePage(int offset, int limit) throws FatalRequestResponse;
}
