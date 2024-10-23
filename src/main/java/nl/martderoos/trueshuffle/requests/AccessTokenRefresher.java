package nl.martderoos.trueshuffle.requests;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

/**
 * Functional interface describing a way to refresh an access token. Required for {@link RequestHandler} to work
 * in the case that the access token has expired.
 */
@FunctionalInterface
public interface AccessTokenRefresher {
    /**
     * Refresh the access token. Implementers should take into account that this function may be called multiple times
     * within a quick succession (although it will never be called concurrently by a {@link RequestHandler}). Refreshing
     * the access token within a short timespan should have no effect. Implementers should be careful not to call
     * functions that require access tokens. Doing so will lead to a stackoverflow or a deadlock.
     *
     * @throws FatalRequestResponse when the access token cannot be refreshed
     */
    void refreshAccessToken() throws FatalRequestResponse;
}
