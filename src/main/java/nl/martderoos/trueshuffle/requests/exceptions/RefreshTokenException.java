package nl.martderoos.trueshuffle.requests.exceptions;

public class RefreshTokenException extends TrueShuffleRequestException {
    public RefreshTokenException(Exception e) {
        super(e, Action.REFRESH_ACCESS_TOKEN);
    }
}
