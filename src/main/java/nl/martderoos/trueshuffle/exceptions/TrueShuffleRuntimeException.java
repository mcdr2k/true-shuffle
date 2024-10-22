package nl.martderoos.trueshuffle.exceptions;

/**
 * Root runtime exception for true shuffle exceptions
 */
public class TrueShuffleRuntimeException extends TrueShuffleException {
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
