package nl.martderoos.trueshuffle.requests.exceptions;

import nl.martderoos.trueshuffle.exceptions.TrueShuffleRequestException;

/**
 * Indicates that the access token used expired and that we should refresh it before sending a new request.
 */
public class RefreshTokenException extends TrueShuffleRequestException {
    public RefreshTokenException(String message) {
        super(message);
    }
}
