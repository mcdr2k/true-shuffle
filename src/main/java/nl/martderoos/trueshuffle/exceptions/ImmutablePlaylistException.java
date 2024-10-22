package nl.martderoos.trueshuffle.exceptions;

public class ImmutablePlaylistException extends RuntimeException {
    public ImmutablePlaylistException(final String message) {
        super(message);
    }
}
