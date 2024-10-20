package nl.martderoos.trueshuffle.requests;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;
import nl.martderoos.trueshuffle.requests.exceptions.RefreshTokenException;
import nl.martderoos.trueshuffle.requests.exceptions.RetryShortlyException;
import nl.martderoos.trueshuffle.requests.exceptions.SlowDownException;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.*;
import se.michaelthelin.spotify.requests.IRequest;
import nl.martderoos.trueshuffle.requests.exceptions.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Simplifies handling errors during Spotify API calls. This class is NOT thread-safe.
 * Use {@link SynchronizedRequestHandler} for a thread-safe implementation. However, if many requests are sent in succession
 * within a single method, it may be better to actually bind an instance of this class to a collection of methods
 * that work with the same handler and then synchronize the methods within the collection.
 */
public class RequestHandler {
    // todo: shared medium upon receiving slowdown responses
    private int delayRequestCount;
    private int slowDownCount;
    private final TokenRefresher refresher;
    private boolean refreshingToken = false;

    public RequestHandler(TokenRefresher refresher) {
        this.refresher = refresher;
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public <T> T handleRequest(IRequest<T> request) throws FatalRequestResponse {
        try {
            return request.execute();
        } catch (IOException e) {
            // should not happen if we have a proper connection, so if it does just terminate the job
            throw new FatalRequestResponse(e);

        } catch (SpotifyWebApiException e) {
            return handleError(request, e);

        } catch (ParseException e) {
            // should NEVER happen; the only thing that could potentially happen is that the api changes and that we
            // had not updated the service yet to work with the new api, resulting in malformed data received
            throw new FatalRequestResponse(e);
        } finally {
            delayRequestCount = 0;
            slowDownCount = 0;
        }
    }

    private <T> T handleError(IRequest<T> request, SpotifyWebApiException e) throws FatalRequestResponse {
        try {
            rethrow(e);
            throw new FatalRequestResponse("** IMPOSSIBLE **");
        } catch (RetryShortlyException ex) {
            return delayRequest(request, TimeUnit.SECONDS, 10);
        } catch (SlowDownException ex) {
            slowDown();
            return handleRequest(request);
        } catch (RefreshTokenException ex) {
            if (this.refresher == null) throw new FatalRequestResponse(e);
            if (refreshingToken) throw new FatalRequestResponse(e);

            try {
                // attempt to refresh token once, then retry
                refreshingToken = true;
                refresher.refreshToken();
                return handleRequest(request);
            } catch (Exception e2) {
                throw new FatalRequestResponse(e2);
            } finally {
                refreshingToken = false;
            }
        }
    }

    private static void rethrow(SpotifyWebApiException e) throws FatalRequestResponse, RetryShortlyException, SlowDownException, RefreshTokenException {
        if (e instanceof BadGatewayException) {
            // The server was acting as a gateway or proxy and received an invalid response from the upstream server
            // note to self: could be just unlucky here, so let us just try again in a bit
            throw new RetryShortlyException(e);
        } else if (e instanceof BadRequestException) {
            // The request could not be understood by the server due to malformed syntax
            // note to self: this should never happen, assuming that the api we use is correct
            throw new FatalRequestResponse(e);
        } else if (e instanceof ForbiddenException) {
            // The server understood the request, but is refusing to fulfill it.
            // note to self: in general http codes this is used to indicate that you are authenticated, but you are not
            // allowed to perform that operation. With Spotify, this could mean that our access was revoked entirely...
            // todo: possibly remove this user from our database entirely (requires some callback)
            throw new FatalRequestResponse(e);
        } else if (e instanceof InternalServerErrorException) {
            // You should never receive this error because our clever coders catch them all ...
            // but if you are unlucky enough to get one, please report it to us
            throw new FatalRequestResponse(e);
        } else if (e instanceof NotFoundException) {
            // The requested resource could not be found. This error can be due to a temporary or permanent condition
            // note to self: this 'should' not happen, possible that we try to modify a deleted album
            throw new FatalRequestResponse(e);
        } else if (e instanceof ServiceUnavailableException) {
            // The server is currently unable to handle the request due to a temporary condition which will be
            // alleviated after some delay. You can choose to resend the request again.
            // note to self: may decide to wait some arbitrary amount of time and then try again
            throw new RetryShortlyException(e);
        } else if (e instanceof TooManyRequestsException) {
            // rate limiting has been applied
            // note to self: this means we need to slow down the requests accordingly
            throw new SlowDownException(e);
        } else if (e instanceof UnauthorizedException) {
            // The request requires user authorization or, if the request included authorization credentials,
            // authorization has been refused for those credentials
            // note to self: this could mean that our token expired, so let's refresh it once and try again
            throw new RefreshTokenException(e);
        }
        // the if-statements should have exhausted all options
        // but in the case they did not, let's just terminate the call
        throw new FatalRequestResponse(e);
    }

    private <T> T delayRequest(IRequest<T> request, TimeUnit timeUnit, long amount) throws FatalRequestResponse {
        delayRequestCount++;
        if (delayRequestCount > 2) {
            throw new FatalRequestResponse("Two attempts to fulfill the request failed.");
        }
        try {
            Thread.sleep(timeUnit.toMillis(amount * delayRequestCount));
        } catch (InterruptedException e) {
            // ok
        }
        return handleRequest(request);
    }

    private void slowDown() {
        this.slowDownCount++;
        try {
            Thread.sleep(Math.min(2000L * this.slowDownCount, 60_000));
        } catch (InterruptedException e) {
            // ok
        }
    }
}
