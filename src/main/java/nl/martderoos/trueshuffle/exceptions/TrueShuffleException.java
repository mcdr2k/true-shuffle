package nl.martderoos.trueshuffle.exceptions;

/**
 * Root exception for true shuffle exceptions.
 */
public class TrueShuffleException extends Exception {
    public TrueShuffleException(Throwable cause) {
        super(cause);
    }

    public TrueShuffleException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrueShuffleException(String message) {
        super(message);
    }

    public TrueShuffleException() {
        super();
    }
}
