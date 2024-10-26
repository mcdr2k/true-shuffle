package nl.martderoos.trueshuffle.requests.exceptions;

/**
 * Indicates that we should slow down sending requests because we hit Spotify's rate limiter. This exception contains
 * a suggested amount of time to wait before sending any other requests.
 */
public class SlowDownException extends TrueShuffleRequestException {
    private final int slowdownSeconds;

    public SlowDownException(Exception e, int slowdownSeconds) {
        super(e, Action.SLOW_DOWN);
        this.slowdownSeconds = slowdownSeconds;
    }

    /**
     * Get the suggested wait time from Spotify which can be used to as a suggested amount of time to wait before
     * sending any other request.
     */
    public int getSlowdownSeconds() {
        return slowdownSeconds;
    }
}
