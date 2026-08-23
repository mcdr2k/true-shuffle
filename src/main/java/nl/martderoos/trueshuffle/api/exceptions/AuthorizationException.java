package nl.martderoos.trueshuffle.api.exceptions;

/**
 * Indicates that something went wrong during the authorization phase of true shuffle.
 */
public class AuthorizationException extends TrueShuffleException {
    public AuthorizationException(String message, Exception e) {
        super(message, e);
    }
}
