package nl.martderoos.trueshuffle.paging;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;
import se.michaelthelin.spotify.model_objects.specification.Paging;

@FunctionalInterface
public interface SpotifyPageConsumer<T> {
    Paging<T> consumePage(int offset, int limit) throws FatalRequestResponse;
}
