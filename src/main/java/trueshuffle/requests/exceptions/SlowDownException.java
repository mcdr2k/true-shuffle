package trueshuffle.requests.exceptions;

public class SlowDownException extends TrueShuffleRequestException {

    public SlowDownException(Exception e) {
        super(e, Action.SLOW_DOWN);
    }
}
