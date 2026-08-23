package nl.martderoos.trueshuffle.internal.requests.exceptions;

import nl.martderoos.trueshuffle.api.exceptions.TrueShuffleException;

/**
 * Root exception for TrueShuffle http request failures.
 */
public class TrueShuffleRequestException extends TrueShuffleException {
    TrueShuffleRequestException(String message) {
        super(message);
    }

    TrueShuffleRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
