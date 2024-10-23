package nl.martderoos.trueshuffle.requests.exceptions;

/**
 * Indicates that the access token used expired and that we should refresh it before sending a new request.
 */
public class RefreshTokenException extends TrueShuffleRequestException {
    public RefreshTokenException(Exception e) {
        super(e, Action.REFRESH_ACCESS_TOKEN);
    }
}
