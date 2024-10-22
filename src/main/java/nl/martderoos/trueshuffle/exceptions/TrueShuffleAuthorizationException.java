package nl.martderoos.trueshuffle.exceptions;

/**
 * Indicates that something went wrong during the authorization phase of true shuffle
 */
public class TrueShuffleAuthorizationException extends TrueShuffleException {
    public TrueShuffleAuthorizationException(String message, Exception e) {
        super(message, e);
    }
}
