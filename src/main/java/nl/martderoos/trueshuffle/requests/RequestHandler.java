package nl.martderoos.trueshuffle.requests;

import nl.martderoos.trueshuffle.requests.exceptions.*;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.*;
import se.michaelthelin.spotify.requests.IRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe class that generalizes handling errors during Spotify API calls. It also provides a retry mechanism
 * for when requests are rejected by Spotify.
 */
public class RequestHandler {
    /**
     * The maximum number of retries before a single request is considered a lost cause.
     */
    public static final int MAX_RETRIES = 8;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    private final AccessTokenRefresher refresher;

    /**
     * Create a new handler with provided {@link AccessTokenRefresher}.
     *
     * @param refresher the refresher to use, can be null. If the access token expires, then this handler will not be
     *                  able to finish any following requests in the case that it is null. The refresher is guaranteed
     *                  to never be called concurrently from within the same handler.
     */
    public RequestHandler(AccessTokenRefresher refresher) {
        this.refresher = refresher;
    }

    /**
     * Send the request, retrying it at most {@link #MAX_RETRIES} times before considering it a lost cause. Note that
     * retries will be implemented using exponential backoff, meaning that it may take quite some time before this
     * method returns.
     *
     * @param request the request to execute.
     * @param <T>     the type of the returned value.
     * @return the returned value by the request.
     * @throws FatalRequestResponseException if the request is deemed to never succeed or fails once more after 10 retries.
     */
    public <T> T handleRequest(IRequest<T> request) throws FatalRequestResponseException {
        return new ApiRequest<>(request).execute();
    }

    // sync to prevent concurrent token refresh
    private synchronized void refreshToken() throws FatalRequestResponseException {
        if (this.refresher == null)
            throw new FatalRequestResponseException("Cannot refresh access token because no token refresher was provided");
        refresher.refreshAccessToken();
    }

    private class ApiRequest<T> {
        private static final int MIN_WAIT_TIME_SECONDS = 1;
        private static final int MAX_WAIT_TIME_SECONDS = 300; // wait 5 minutes at most
        private final IRequest<T> request;
        private int retries = 0;
        private Exception lastException;

        private ApiRequest(IRequest<T> request) {
            this.request = request;
        }

        public T execute() throws FatalRequestResponseException {
            while (retries < MAX_RETRIES) {
                try {
                    return request.execute();
                } catch (IOException | ParseException e) {
                    // IOException should not happen if we have a proper connection, so if it does just terminate the job
                    // ParseException should NEVER happen unless the api has changed
                    throw new FatalRequestResponseException(e);
                } catch (SpotifyWebApiException e) {
                    // handle and then continue
                    handleError(e);
                }

                retries++;
            }
            throw new FatalRequestResponseException("Request exceeded maximum number of retries, cause of last exception was: " + lastException.getMessage());
        }

        private void handleError(SpotifyWebApiException spotifyException) throws FatalRequestResponseException {
            lastException = spotifyException;

            try {
                rethrow(spotifyException);
            } catch (RetryShortlyException e) {
                // exponential backoff
                var backoffSeconds = (int) Math.pow(2, retries);
                sleep(backoffSeconds, TimeUnit.SECONDS);
            } catch (SlowDownException e) {
                sleep(e.getSlowdownSeconds(), TimeUnit.SECONDS);
            } catch (RefreshTokenException e) {
                refreshToken();
            }
        }

        @SuppressWarnings("SameParameterValue")
        private void sleep(long amount, TimeUnit unit) {
            try {
                var millis = unit.toMillis(amount);
                millis = Math.min(millis, MAX_WAIT_TIME_SECONDS);
                millis = Math.max(MIN_WAIT_TIME_SECONDS, millis);
                LOGGER.debug("A request has been delayed for {} milliseconds", millis);
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                // ok
            }
        }
    }

    private static void rethrow(SpotifyWebApiException e) throws FatalRequestResponseException, RetryShortlyException, SlowDownException, RefreshTokenException {
        if (e instanceof BadGatewayException) {
            // The server was acting as a gateway or proxy and received an invalid response from the upstream server
            // note to self: could be just unlucky here, so let us just try again in a bit
            throw new RetryShortlyException(e);
        } else if (e instanceof BadRequestException) {
            // The request could not be understood by the server due to malformed syntax
            // note to self: this should never happen, assuming that the api we use is correct
            throw new FatalRequestResponseException(e);
        } else if (e instanceof ForbiddenException) {
            // The server understood the request, but is refusing to fulfill it.
            // note to self: in general http codes this is used to indicate that you are authenticated, but you are not
            // allowed to perform that operation. With Spotify, this could mean that our access was revoked entirely...
            throw new AuthorizationRevokedException(e.getMessage());
        } else if (e instanceof InternalServerErrorException) {
            // You should never receive this error because our clever coders catch them all ...
            // but if you are unlucky enough to get one, please report it to us
            throw new FatalRequestResponseException(e);
        } else if (e instanceof NotFoundException) {
            // The requested resource could not be found. This error can be due to a temporary or permanent condition
            // note to self: this may happen during concurrent modifications or when we try to access inaccessible resources like private playlists
            throw new FatalRequestResponseException(e);
        } else if (e instanceof ServiceUnavailableException) {
            // The server is currently unable to handle the request due to a temporary condition which will be
            // alleviated after some delay. You can choose to resend the request again.
            // note to self: may decide to wait some arbitrary amount of time and then try again
            throw new RetryShortlyException(e);
        } else if (e instanceof TooManyRequestsException tmr) {
            // rate limiting has been applied
            // note to self: this means we need to slow down the requests accordingly
            throw new SlowDownException(e, tmr.getRetryAfter());
        } else if (e instanceof UnauthorizedException) {
            // The request requires user authentication
            // note to self: this could mean that our token expired, so let's refresh it and try again
            throw new RefreshTokenException(e);
        }
        // the if-statements should have exhausted all options
        // but in the case they did not, let's just terminate the call
        throw new FatalRequestResponseException(e);
    }
}
