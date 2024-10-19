package trueshuffle.requests.exceptions;

public class RetryShortlyException extends TrueShuffleRequestException {
    public RetryShortlyException(Exception e) {
        super(e, Action.RETRY_SHORTLY);
    }
}
