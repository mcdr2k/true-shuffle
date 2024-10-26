package nl.martderoos.trueshuffle.requests.exceptions;

/**
 * When thrown, indicates that an Api request could not complete appropriately and that any attempt to retry the
 * request will likely fail.
 */
public class FatalRequestResponseException extends TrueShuffleRequestException {
    public FatalRequestResponseException(Exception e) {
        super(e, Action.TERMINATE);
    }

    public FatalRequestResponseException(String message, Exception e) {
        super(message, e, Action.TERMINATE);
    }

    public FatalRequestResponseException(String message) {
        super(message, Action.TERMINATE);
    }
}
