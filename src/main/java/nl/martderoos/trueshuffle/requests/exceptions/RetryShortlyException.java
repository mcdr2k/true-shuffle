package nl.martderoos.trueshuffle.requests.exceptions;

/**
 * Indicates that we should wait a bit before retrying the request. This is likely due to Spotify rejecting the request
 * due to server-side issues or high load.
 */
public class RetryShortlyException extends TrueShuffleRequestException {
    public RetryShortlyException(Exception e) {
        super(e, Action.RETRY_SHORTLY);
    }
}
