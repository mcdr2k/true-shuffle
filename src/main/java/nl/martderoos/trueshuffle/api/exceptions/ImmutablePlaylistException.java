package nl.martderoos.trueshuffle.api.exceptions;

/**
 * Indicates an attempt to modify an immutable playlist.
 */
public class ImmutablePlaylistException extends RuntimeException {
    public ImmutablePlaylistException(final String message) {
        super(message);
    }
}
