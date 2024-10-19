package trueshuffle.requests.exceptions;

import java.util.Objects;

public class TrueShuffleRequestException extends Exception {
    private final Action action;

    TrueShuffleRequestException(Exception e, Action action) {
        super(e);
        this.action = Objects.requireNonNull(action);
    }

    TrueShuffleRequestException(String message, Exception e, Action action) {
        super(message, e);
        this.action = Objects.requireNonNull(action);
    }

    TrueShuffleRequestException(String message, Action action) {
        super(message);
        this.action = Objects.requireNonNull(action);
    }

    public Action getAction() {
        return action;
    }

    public enum Action {
        /**
         * Indicates that we should slow down with our requests since we hit the rate limit.
         * Can be done by only sending a few requests per second.
         */
        SLOW_DOWN,
        /**
         * The request could not be handled at the moment (serverside issues, high load...), try again after a small
         * amount of time (&lt; 10s).
         */
        RETRY_SHORTLY,
        /**
         * Terminate the request there is no recovery available anymore for this particular request.
         */
        TERMINATE,
        /**
         * The access token used at the moment has expired. Use the refresh token to retrieve a new access token
         * from the Spotify API. After refreshing the access token, you can retry the request.
         */
        REFRESH_ACCESS_TOKEN,
    }
}
