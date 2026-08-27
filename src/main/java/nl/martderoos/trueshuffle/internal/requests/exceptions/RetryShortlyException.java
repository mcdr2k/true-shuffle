package nl.martderoos.trueshuffle.internal.requests.exceptions;

import nl.martderoos.trueshuffle.api.requests.exceptions.TrueShuffleRequestException;

/**
 * Indicates that we should wait a bit before retrying the request. This is likely due to Spotify rejecting the request
 * due to server-side issues or high load.
 */
public class RetryShortlyException extends TrueShuffleRequestException {
    public RetryShortlyException(String message) {
        super(message);
    }
}
