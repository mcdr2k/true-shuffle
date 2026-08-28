package nl.martderoos.trueshuffle.api;

import nl.martderoos.trueshuffle.exceptions.AuthorizationException;
import nl.martderoos.trueshuffle.exceptions.InitializationException;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.jobs.TrueShuffleJob;
import nl.martderoos.trueshuffle.jobs.TrueShuffleJobExecution;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.Executor;

public interface TrueShuffleClient {
    /**
     * Attempts to initialize the client by verifying the client id and secret with Spotify.
     */
    void initialize() throws InitializationException;

    /**
     * <p>Add an authorized user to this client by validating the provided credentials. The userId is optional
     * because Spotify is able to derive the user from both the access token and the refresh token alone. If the userId
     * is null and the access token is invalid, the refresh token is used to get a new access token. It is preferred
     * to not leave the userId field null because then we can reuse the current authorized user if available.</p>
     * <p>This method should mainly be used to add previously authorized users to this client. Especially useful when the
     * client was taken offline.</p>
     *
     * @param userId      the unique identifier of the user whom the credentials are for (nullable).
     * @param credentials the credentials of the user (not nullable). The access token of the credentials may not be
     *                    null nor empty, but it may be invalid.
     * @return the ShuffleApi that is bound to a specific user corresponding to the provided userId and credentials.
     * @throws AuthorizationException   when both the access token and refresh token of the credentials are invalid.
     *                                  The only way to 'fix' this is to get a new code from Spotify and calling
     *                                  {@link #addAuthorizedUser(String)}.
     * @throws NullPointerException     when credentials is null.
     * @throws IllegalArgumentException when the access token of the credentials is null or empty.
     * @throws IllegalStateException    when the client has not been initialized yet.
     */
    TrueShuffleUser addAuthorizedUser(String userId, TrueShuffleUserCredentials credentials) throws AuthorizationException;

    /**
     * Add an authorized user to this client by validating the provided code with Spotify.
     *
     * @param code the code received from Spotify through the redirect (callback) link upon authorization.
     * @return the ShuffleApi that is bound to this specific user.
     * @throws AuthorizationException when the code provided is invalid (or could not be validated).
     * @throws IllegalStateException  when the client has not been initialized yet.
     */
    TrueShuffleUser addAuthorizedUser(String code) throws AuthorizationException;

    /**
     * Remove an authorized user from the set of authorized users.
     */
    void removeAuthorizedUser(String userId);

    /**
     * @return the complete immutable set of all current authorized users known to this client, never null.
     */
    Set<String> getAuthorizedUsers();

    /**
     * Retrieve a shuffle user by means of a unique user identifier.
     *
     * @param userId the user identifier of the user to find.
     * @return the user (never null).
     * @throws UserNotFoundException if the user could not be found.
     */
    TrueShuffleUser getAuthorizedUser(String userId) throws UserNotFoundException;

    /**
     * Perform a shuffle on the user's liked songs, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param userId   the id of the user.
     * @param executor the execution schedule (should be an asynchronous schedule).
     * @return the execution handle, which exposes status and lifecycle operations.
     * @throws IllegalStateException when the client has not been initialized yet.
     */
    TrueShuffleJobExecution shuffleLikedSongs(String userId, Executor executor);

    /**
     * Perform a shuffle on the provided playlist for a specific user, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param userId     the id of the user.
     * @param playlistId the id of the playlist to shuffle.
     * @param executor   the execution schedule (should be an asynchronous schedule).
     * @return the execution handle, which exposes status and lifecycle operations.
     * @throws IllegalStateException when the client has not been initialized yet.
     */
    TrueShuffleJobExecution shufflePlaylist(String userId, String playlistId, Executor executor);

    /**
     * Execute a job using the provided executor.
     * @return the execution of the job.
     */
    TrueShuffleJobExecution execute(TrueShuffleJob job, Executor executor);

    /**
     * Builds the URI for this client which redirects users to the authorization page of spotify with the appropriate
     * scopes and state.
     *
     * @param state optional, but strongly recommended (ignored if blank). The state can be useful for correlating requests and responses.
     *              Because your redirect_uri can be guessed, using a state value can increase your assurance that an
     *              incoming connection is the result of an authentication request. If you generate a random string or
     *              encode the hash of some client state (e.g., a cookie) in this state variable, you can validate the
     *              response to additionally ensure that the request and response originated in the same browser. This
     *              provides protection against attacks such as cross-site request forgery.
     */
    URI getAuthorizationURI(String state);

    /**
     * @return the redirect uri to be used by Spotify when a user authorizes TrueShuffle.
     */
    URI getRedirectUri();
}
