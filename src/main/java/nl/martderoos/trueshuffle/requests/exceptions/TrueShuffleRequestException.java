package nl.martderoos.trueshuffle.requests.exceptions;

import nl.martderoos.trueshuffle.exceptions.TrueShuffleException;

/**
 * Root exception for TrueShuffle http request failures.
 */
public class TrueShuffleRequestException extends TrueShuffleException {
    TrueShuffleRequestException(String message) {
        super(message);
    }
}
