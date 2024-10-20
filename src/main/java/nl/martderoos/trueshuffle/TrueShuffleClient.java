package nl.martderoos.trueshuffle;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;
import nl.martderoos.trueshuffle.exceptions.TrueShuffleAuthorisationException;
import nl.martderoos.trueshuffle.exceptions.TrueShuffleInitialisationException;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.jobs.ShuffleJobStatus;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.requests.SynchronizedRequestHandler;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponse;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Thread-safe class for managing TrueShuffle users.
 */
public class TrueShuffleClient {
    private final URI redirectUri;
    private final String cid;
    private final String secret;
    private final SpotifyApi client;
    private boolean initialised = false;

    private final SynchronizedRequestHandler handler = new SynchronizedRequestHandler(null);

    private static final Map<String, TrueShuffleUser> authorisedUsersMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Creates a new client from provided client-id, secret and redirect uri (callback).
     *
     * @param cid                   The client-id to use.
     * @param secret                The secret to use.
     * @param redirectUri           The redirect uri (callback) to use for authorisation.
     * @param initialiseImmediately Whether to initialise this client immediately during construction of the class.
     *                              If true, an attempt is made to verify the client-id, secret and redirect uri with
     *                              Spotify by means of a web request. Otherwise, this instance cannot be used until a
     *                              call is made to {@link #initialise()} (note: you can call any function you want but
     *                              they will all result in exceptions until initialisation). The constructor may throw
     *                              exceptions when the initialisation fails.
     * @throws TrueShuffleInitialisationException When an attempt to initialise this client fails.
     */
    public TrueShuffleClient(String cid,
                             String secret,
                             String redirectUri,
                             boolean initialiseImmediately) throws TrueShuffleInitialisationException {
        this.cid = Objects.requireNonNull(cid);
        this.secret = Objects.requireNonNull(secret);
        this.redirectUri = SpotifyHttpManager.makeUri(Objects.requireNonNull(redirectUri));
        client = SpotifyApi.builder()
                .setClientId(cid)
                .setClientSecret(secret)
                .setRedirectUri(this.redirectUri)
                .build();

        if (initialiseImmediately)
            initialise();
    }

    /**
     * Attempts to initialise the client by verifying the client id and secret with Spotify.
     */
    public synchronized void initialise() throws TrueShuffleInitialisationException {
        if (initialised)
            return;
        try {
            refreshClientCredentials();
            initialised = true;
        } catch (FatalRequestResponse e) {
            throw new TrueShuffleInitialisationException(e);
        }
    }

    /**
     * Attempts to add an authorised user to this client by validating the provided code with Spotify.
     *
     * @param code The code received from Spotify through the redirect (callback) link upon authorisation.
     * @return The ShuffleApi that is bound to this specific user.
     * @throws TrueShuffleAuthorisationException When the code provided is invalid (or could not be validated).
     */
    public synchronized TrueShuffleUser addAuthorisedUser(String code) throws TrueShuffleAuthorisationException {
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
        } catch (FatalRequestResponse e) {
            throw new TrueShuffleAuthorisationException("Could not authorise a user with the provided code.", e);
        }

        try {
            var userData = handler.handleRequest(api.getCurrentUsersProfile().build());
            return addOrReuseAuthorisedUser(api, credentials, userData);
        } catch (FatalRequestResponse e) {
            throw new TrueShuffleAuthorisationException("Provided code could be validated but the user's data (id, display name) could not be retrieved.", e);
        }
    }

    private TrueShuffleUser addOrReuseAuthorisedUser(SpotifyApi api, AuthorizationCodeCredentials credentials, User userData) {
        try {
            // user already exists, let's reuse it
            // update credentials (actually, the current credentials used by the api would still be valid until expired)
            // however, it is possible that we get a new refresh token, which in turn invalidates the current refresh token
            var authorisedUser = getAuthorisedUser(userData.getId());
            authorisedUser.assignCredentials(credentials);
            return authorisedUser;
        } catch (UserNotFoundException e) {
            // ok
        }

        // new user
        var shuffleApi = new ShuffleApi(api, userData);
        var trueShuffleUser = new TrueShuffleUser(userData, shuffleApi);
        authorisedUsersMap.put(trueShuffleUser.getUserId(), trueShuffleUser);
        return trueShuffleUser;
    }

    /**
     * Removes an authorised user from the set of authorised users.
     */
    public synchronized void removeAuthorisedUser(String userId) {
        authorisedUsersMap.remove(userId);
    }

    public synchronized Set<String> getAuthorisedUsers() {
        return new HashSet<>(authorisedUsersMap.keySet());
    }

    /**
     * Perform a shuffle on the provided playlist for a specific user, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param userId   The id of the user.
     * @param executor The execution schedule (should be an asynchronous schedule).
     * @return The status of the shuffle job, which is updated continuously until it has finished.
     * @throws UserNotFoundException When no user could be found with the provided user identifier.
     */
    public ShuffleJobStatus shuffleLikedSongs(String userId, Executor executor) throws UserNotFoundException {
        var user = getAuthorisedUser(userId);
        return user.shuffleLikedSongs(executor);
    }

    /**
     * Perform a shuffle on the provided playlist for a specific user, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param userId     The id of the user.
     * @param playlistId The id of the playlist to shuffle.
     * @param executor   The execution schedule (should be an asynchronous schedule).
     * @return The status of the shuffle job, which is updated continuously until it has finished.
     * @throws UserNotFoundException When no user could be found with the provided user identifier.
     */
    public ShuffleJobStatus shufflePlaylist(String userId, String playlistId, Executor executor) throws UserNotFoundException {
        var user = getAuthorisedUser(userId);
        return user.shufflePlaylist(playlistId, executor);
    }

    public synchronized TrueShuffleUser getAuthorisedUser(String userId) throws UserNotFoundException {
        var user = authorisedUsersMap.get(userId);
        if (user == null)
            throw new UserNotFoundException(userId);
        return user;
    }

    /**
     * Builds the URI for this client which redirects users to the authorisation page of spotify with the appropriate
     * scopes and state.
     *
     * @param state Optional, but strongly recommended (ignored if blank). The state can be useful for correlating requests and responses.
     *              Because your redirect_uri can be guessed, using a state value can increase your assurance that an
     *              incoming connection is the result of an authentication request. If you generate a random string or
     *              encode the hash of some client state (e.g., a cookie) in this state variable, you can validate the
     *              response to additionally ensure that the request and response originated in the same browser. This
     *              provides protection against attacks such as cross-site request forgery.
     */
    public URI getAuthorisationURI(String state) {
        var uriBuilder = client.authorizationCodeUri()
                .scope(
                        AuthorizationScope.USER_LIBRARY_READ,           // read liked songs
                        AuthorizationScope.PLAYLIST_READ_PRIVATE,       // read private playlists
                        AuthorizationScope.PLAYLIST_READ_COLLABORATIVE, // read collaborative playlists
                        AuthorizationScope.PLAYLIST_MODIFY_PRIVATE,     // modify private playlists
                        AuthorizationScope.PLAYLIST_MODIFY_PUBLIC);     // modify public playlists

        if (state != null && !state.isBlank())
            uriBuilder.state(state);

        return uriBuilder.build().execute();
    }

    /**
     * Generates a random 'state' to be used for security purposes. Note that this generated state should be stored
     * by the user in their own code as to be able to use it further down the authorisation flow. This program
     * itself does nothing with it unless provided to some of this instance's functions, like {@link #getAuthorisationURI(String)}.
     * The random state generated is produced by a call to {@link UUID#randomUUID()}.
     */
    public static String generateRandomState() {
        return UUID.randomUUID().toString();
    }

    private void refreshClientCredentials() throws FatalRequestResponse {
        var credentials = handler.handleRequest(client.clientCredentials().build());
        client.setAccessToken(credentials.getAccessToken());
    }
}