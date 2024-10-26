package nl.martderoos.trueshuffle;

import nl.martderoos.trueshuffle.exceptions.AuthorizationException;
import nl.martderoos.trueshuffle.exceptions.InitializationException;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.jobs.TrueShuffleJobStatus;
import nl.martderoos.trueshuffle.jobs.TrueShuffleLikedJob;
import nl.martderoos.trueshuffle.jobs.TrueShufflePlaylistJob;
import nl.martderoos.trueshuffle.jobs.TrueShuffleUserResolver;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.requests.RequestHandler;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Thread-safe class for managing TrueShuffle users. Additionally, exposes methods for shuffling a user's playlists.
 */
public class TrueShuffleClient {
    private final URI redirectUri;
    private final String cid;
    private final String secret;
    private final SpotifyApi client;
    private volatile boolean initialized = false;

    private final RequestHandler handler = new RequestHandler(null);
    private final TrueShuffleUserResolver resolver = this::getAuthorizedUser;

    private final Map<String, TrueShuffleUser> authorizedUsersMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Creates a new client from provided client-id, secret and redirect uri (callback). Note that this client must
     * still be initialized using {@link #initialize()} before it can be used.
     *
     * @param cid         The client-id to use.
     * @param secret      The secret to use.
     * @param redirectUri The redirect uri (callback) to use for authorization.
     */
    public TrueShuffleClient(String cid, String secret, String redirectUri) {
        this.cid = Objects.requireNonNull(cid);
        this.secret = Objects.requireNonNull(secret);
        this.redirectUri = SpotifyHttpManager.makeUri(Objects.requireNonNull(redirectUri));
        client = SpotifyApi.builder()
                .setClientId(cid)
                .setClientSecret(secret)
                .setRedirectUri(this.redirectUri)
                .build();
    }

    /**
     * Attempts to initialize the client by verifying the client id and secret with Spotify.
     */
    public synchronized void initialize() throws InitializationException {
        if (initialized)
            return;
        try {
            refreshClientCredentials();
            initialized = true;
        } catch (FatalRequestResponseException e) {
            throw new InitializationException(e);
        }
    }

    /**
     * <p>Attempts to add an authorized user to this client by validating the provided credentials. The userId is optional
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
    public TrueShuffleUser addAuthorizedUser(String userId, TrueShuffleUserCredentials credentials) throws AuthorizationException {
        verifyInit();
        Objects.requireNonNull(credentials);
        if (credentials.accessToken() == null || credentials.accessToken().isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null nor empty");
        }

        try {
            var user = getAuthorizedUser(userId);
            user.assignCredentials(credentials);
            return user;
        } catch (UserNotFoundException e) {
            // ok
        }

        var api = SpotifyApi.builder()
                .setClientId(cid)
                .setClientSecret(secret)
                .setAccessToken(credentials.accessToken())
                .setRefreshToken(credentials.refreshToken())
                .build();
        try {
            var userData = handler.handleRequest(api.getCurrentUsersProfile().build());
            return addOrReuseAuthorizedUser(api, credentials, userData);
        } catch (FatalRequestResponseException e) {
            throw new AuthorizationException("Provided credentials were not sufficient to authorize TrueShuffle, it is possible that they have expired.", e);
        }
    }

    /**
     * Attempts to add an authorized user to this client by validating the provided code with Spotify.
     *
     * @param code the code received from Spotify through the redirect (callback) link upon authorization.
     * @return the ShuffleApi that is bound to this specific user.
     * @throws AuthorizationException when the code provided is invalid (or could not be validated).
     * @throws IllegalStateException  when the client has not been initialized yet.
     */
    public TrueShuffleUser addAuthorizedUser(String code) throws AuthorizationException {
        verifyInit();
        Objects.requireNonNull(code);

        AuthorizationCodeCredentials credentials;
        SpotifyApi api;

        // first retrieve access and refresh tokens with the code
        try {
            credentials = handler.handleRequest(client.authorizationCode(code).build());
            api = SpotifyApi.builder()
                    .setClientId(cid)
                    .setClientSecret(secret)
                    .setAccessToken(credentials.getAccessToken())
                    .setRefreshToken(credentials.getRefreshToken())
                    .build();
        } catch (FatalRequestResponseException e) {
            throw new AuthorizationException("Could not authorize a user with the provided code.", e);
        }

        try {
            var userData = handler.handleRequest(api.getCurrentUsersProfile().build());
            return addOrReuseAuthorizedUser(api, new TrueShuffleUserCredentials(credentials), userData);
        } catch (FatalRequestResponseException e) {
            throw new AuthorizationException("Provided code could be validated but the user's data (id, display name) could not be retrieved.", e);
        }
    }

    private synchronized TrueShuffleUser addOrReuseAuthorizedUser(SpotifyApi api, TrueShuffleUserCredentials credentials, User userData) {
        verifyInit();
        try {
            // if user already exists, let's reuse it
            // update credentials (actually, the current credentials used by the api would still be valid until expired)
            // however, it is possible that we get a new refresh token, which in turn invalidates the current refresh token
            var authorizedUser = getAuthorizedUser(userData.getId());
            authorizedUser.assignCredentials(credentials);
            return authorizedUser;
        } catch (UserNotFoundException e) {
            // ok
        }

        // new user
        var shuffleApi = new ShuffleApi(api, userData);
        var trueShuffleUser = new TrueShuffleUser(userData, shuffleApi);
        authorizedUsersMap.put(trueShuffleUser.getUserId(), trueShuffleUser);
        return trueShuffleUser;
    }

    /**
     * Removes an authorized user from the set of authorized users.
     */
    public void removeAuthorizedUser(String userId) {
        authorizedUsersMap.remove(userId);
    }

    /**
     * @return a shallow copy of the complete set of all current authorized users known to this client, never null.
     */
    public Set<String> getAuthorizedUsers() {
        synchronized (authorizedUsersMap) {
            return new HashSet<>(authorizedUsersMap.keySet());
        }
    }

    /**
     * Retrieve a shuffle user by means of a unique user identifier.
     *
     * @param userId the user identifier of the user to find.
     * @return the user (never null).
     * @throws UserNotFoundException if the user could not be found.
     */
    public TrueShuffleUser getAuthorizedUser(String userId) throws UserNotFoundException {
        var user = authorizedUsersMap.get(userId);
        if (user == null)
            throw new UserNotFoundException(userId);
        return user;
    }

    /**
     * Perform a shuffle on the user's liked songs, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param userId   the id of the user.
     * @param executor the execution schedule (should be an asynchronous schedule).
     * @return the status of the shuffle job, which is updated continuously until it has finished.
     * @throws UserNotFoundException when no user could be found with the provided user identifier.
     * @throws IllegalStateException when the client has not been initialized yet.
     */
    public TrueShuffleJobStatus shuffleLikedSongs(String userId, Executor executor) throws UserNotFoundException {
        verifyInit();
        Objects.requireNonNull(executor);
        getAuthorizedUser(userId);
        var job = new TrueShuffleLikedJob(userId);
        return job.execute(resolver, executor);
    }

    /**
     * Perform a shuffle on the provided playlist for a specific user, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param userId     the id of the user.
     * @param playlistId the id of the playlist to shuffle.
     * @param executor   the execution schedule (should be an asynchronous schedule).
     * @return the status of the shuffle job, which is updated continuously until it has finished.
     * @throws UserNotFoundException when no user could be found with the provided user identifier.
     * @throws IllegalStateException when the client has not been initialized yet.
     */
    public TrueShuffleJobStatus shufflePlaylist(String userId, String playlistId, Executor executor) throws UserNotFoundException {
        verifyInit();
        Objects.requireNonNull(executor);
        getAuthorizedUser(userId);
        var job = new TrueShufflePlaylistJob(userId, playlistId);
        return job.execute(resolver, executor);
    }

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
    public URI getAuthorizationURI(String state) {
        var uriBuilder = client.authorizationCodeUri()
                .scope(
                        AuthorizationScope.USER_LIBRARY_READ,           // read liked songs
                        AuthorizationScope.PLAYLIST_READ_PRIVATE,       // read private playlists
                        AuthorizationScope.PLAYLIST_READ_COLLABORATIVE, // read collaborative playlists
                        AuthorizationScope.PLAYLIST_MODIFY_PRIVATE,     // modify private playlists
                        AuthorizationScope.PLAYLIST_MODIFY_PUBLIC       // modify public playlists
                );

        if (state != null && !state.isBlank())
            uriBuilder.state(state);

        return uriBuilder.build().execute();
    }

    /**
     * @return the redirect uri set during construction of this instance.
     */
    public URI getRedirectUri() {
        return redirectUri;
    }

    /**
     * Generates a random 'state' to be used for security purposes. Note that this generated state should be stored
     * by the user in their own code as to be able to use it further down the authorization flow. This program
     * itself does nothing with it unless provided to some of this instance's functions, like {@link #getAuthorizationURI(String)}.
     * The random state generated is produced by a call to {@link UUID#randomUUID()}.
     */
    public static String generateRandomState() {
        return UUID.randomUUID().toString();
    }

    private void refreshClientCredentials() throws FatalRequestResponseException {
        var credentials = handler.handleRequest(client.clientCredentials().build());
        client.setAccessToken(credentials.getAccessToken());
    }

    private void verifyInit() throws IllegalStateException {
        if (!initialized) throw new IllegalStateException("Cannot execute this method before initialization");
    }
}
