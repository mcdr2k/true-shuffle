package nl.martderoos.trueshuffle.internal.requests.exceptions;

import nl.martderoos.trueshuffle.api.requests.exceptions.TrueShuffleRequestException;

/**
 * Indicates that the access token used expired and that we should refresh it before sending a new request.
 */
public class RefreshTokenException extends TrueShuffleRequestException {
    public RefreshTokenException(String message) {
        super(message);
    }
}
