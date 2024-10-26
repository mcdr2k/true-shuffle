package nl.martderoos.trueshuffle.exceptions;

/**
 * Indicates an attempt to modify an immutable playlist.
 */
public class ImmutablePlaylistException extends RuntimeException {
    public ImmutablePlaylistException(final String message) {
        super(message);
    }
}
