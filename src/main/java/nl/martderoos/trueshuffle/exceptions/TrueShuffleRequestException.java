package nl.martderoos.trueshuffle.exceptions;

import nl.martderoos.trueshuffle.exceptions.TrueShuffleException;

/**
 * Root exception for TrueShuffle http request failures.
 */
public class TrueShuffleRequestException extends TrueShuffleException {
    protected TrueShuffleRequestException(String message) {
        super(message);
    }

    protected TrueShuffleRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
