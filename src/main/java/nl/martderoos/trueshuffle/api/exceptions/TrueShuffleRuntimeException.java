package nl.martderoos.trueshuffle.api.exceptions;

/**
 * Root runtime exception for true shuffle exceptions.
 */
public class TrueShuffleRuntimeException extends RuntimeException {
    public TrueShuffleRuntimeException() {
        super();
    }

    public TrueShuffleRuntimeException(String message) {
        super(message);
    }

    public TrueShuffleRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrueShuffleRuntimeException(Throwable cause) {
        super(cause);
    }
}
