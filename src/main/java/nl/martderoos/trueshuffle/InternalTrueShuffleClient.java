package nl.martderoos.trueshuffle;

import nl.martderoos.trueshuffle.api.TrueShuffleClient;
import nl.martderoos.trueshuffle.api.TrueShuffleUser;
import nl.martderoos.trueshuffle.api.TrueShuffleUserCredentials;
import nl.martderoos.trueshuffle.exceptions.AuthorizationException;
import nl.martderoos.trueshuffle.exceptions.FatalRequestResponseException;
import nl.martderoos.trueshuffle.exceptions.InitializationException;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.jobs.*;
import nl.martderoos.trueshuffle.model.TrueShuffleApi;
import nl.martderoos.trueshuffle.requests.RequestHandler;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Thread-safe class for managing TrueShuffle users.
 */
public class InternalTrueShuffleClient implements TrueShuffleClient {
    private final URI redirectUri;
    private final String cid;
    private final String secret;
    private final SpotifyApi client;
    private volatile boolean initialized = false;

    private final RequestHandler handler;
    private final TrueShuffleUserResolver resolver;
    private final TrueShuffleJobExecutor jobExecutor = new TrueShuffleJobExecutor();

    private final Map<String, InternalTrueShuffleUser> authorizedUsersMap = Collections.synchronizedMap(new HashMap<>());

    private InternalTrueShuffleClient(String cid, String secret, String redirectUri) {
        this(
                cid,
                secret,
                redirectUri,
                SpotifyApi.builder()
                        .setClientId(cid)
                        .setClientSecret(secret)
                        .setRedirectUri(SpotifyHttpManager.makeUri(Objects.requireNonNull(redirectUri)))
                        .build(),
                new RequestHandler(null)
        );
    }

    InternalTrueShuffleClient(String cid, String secret, String redirectUri, SpotifyApi client, RequestHandler handler) {
        this(cid, secret, redirectUri, client, handler, null);
    }

    InternalTrueShuffleClient(
            String cid,
            String secret,
            String redirectUri,
            SpotifyApi client,
            RequestHandler handler,
            TrueShuffleUserResolver resolver
    ) {
        this.cid = Objects.requireNonNull(cid);
        this.secret = Objects.requireNonNull(secret);
        this.redirectUri = SpotifyHttpManager.makeUri(Objects.requireNonNull(redirectUri));
        this.client = Objects.requireNonNull(client);
        this.handler = Objects.requireNonNull(handler);
        this.resolver = resolver == null ? this::getInternalAuthorizedUser : resolver;
    }

    @Override
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

    @Override
    public TrueShuffleUser addAuthorizedUser(String userId, TrueShuffleUserCredentials credentials) throws AuthorizationException {
        verifyInit();
        Objects.requireNonNull(credentials);
        if (credentials.accessToken() == null || credentials.accessToken().isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be null nor empty");
        }

        try {
            if (userId != null) {
                var user = getInternalAuthorizedUser(userId);
                user.assignCredentials(credentials);
                return user;
            }
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

    @Override
    public TrueShuffleUser addAuthorizedUser(String code) throws AuthorizationException {
        verifyInit();
        Objects.requireNonNull(code);

        AuthorizationCodeCredentials credentials;
        SpotifyApi api;

        // first retrieve access and refresh tokens with the code
        var issuedSinceEpoch = System.currentTimeMillis();
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
            return addOrReuseAuthorizedUser(api, new TrueShuffleUserCredentials(issuedSinceEpoch, credentials), userData);
        } catch (FatalRequestResponseException e) {
            throw new AuthorizationException("Provided code could be validated but the user's data (id, display name) could not be retrieved.", e);
        }
    }

    private synchronized InternalTrueShuffleUser addOrReuseAuthorizedUser(SpotifyApi api, TrueShuffleUserCredentials credentials, User userData) {
        verifyInit();
        try {
            // if user already exists, let's reuse it
            // update credentials (actually, the current credentials used by the api would still be valid until expired)
            // however, it is possible that we get a new refresh token, which in turn invalidates the current refresh token
            var authorizedUser = getInternalAuthorizedUser(userData.getId());
            authorizedUser.assignCredentials(credentials);
            return authorizedUser;
        } catch (UserNotFoundException e) {
            // ok
        }

        // new user
        var shuffleApi = new TrueShuffleApi(api, userData);
        var trueShuffleUser = new InternalTrueShuffleUser(userData, shuffleApi);
        authorizedUsersMap.put(trueShuffleUser.getUserId(), trueShuffleUser);
        return trueShuffleUser;
    }

    @Override
    public void removeAuthorizedUser(String userId) {
        authorizedUsersMap.remove(userId);
    }

    @Override
    public Set<String> getAuthorizedUsers() {
        synchronized (authorizedUsersMap) {
            return new HashSet<>(authorizedUsersMap.keySet());
        }
    }

    private InternalTrueShuffleUser getInternalAuthorizedUser(String userId) throws UserNotFoundException {
        var user = authorizedUsersMap.get(userId);
        if (user == null)
            throw new UserNotFoundException(userId);
        return user;
    }

    @Override
    public TrueShuffleUser getAuthorizedUser(String userId) throws UserNotFoundException {
        return getInternalAuthorizedUser(userId);
    }

    private InternalTrueShuffleJobExecution execute(TrueShuffleJob job, Executor executor) throws UserNotFoundException {
        verifyInit();
        Objects.requireNonNull(job);
        Objects.requireNonNull(executor);
        resolver.resolve(job.getUserId());
        return jobExecutor.execute(job, resolver, executor);
    }

    @Override
    public InternalTrueShuffleJobExecution shuffleLikedSongs(String userId, Executor executor) throws UserNotFoundException {
        return execute(new TrueShuffleLikedJob(userId), executor);
    }

    @Override
    public InternalTrueShuffleJobExecution shufflePlaylist(String userId, String playlistId, Executor executor) throws UserNotFoundException {
        return execute(new TrueShufflePlaylistJob(userId, playlistId), executor);
    }

    @Override
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

    public URI getRedirectUri() {
        return redirectUri;
    }

    private void refreshClientCredentials() throws FatalRequestResponseException {
        var credentials = handler.handleRequest(client.clientCredentials().build());
        client.setAccessToken(credentials.getAccessToken());
    }

    private void verifyInit() throws IllegalStateException {
        if (!initialized) throw new IllegalStateException("Cannot execute this method before initialization");
    }

    /**
     * Creates a new client from provided client-id, secret and redirect uri (callback). Note that this client must
     * still be initialized using {@link #initialize()} before it can be used.
     *
     * @param cid         The client-id to use.
     * @param secret      The secret to use.
     * @param redirectUri The redirect uri (callback) to use for authorization.
     */
    public static TrueShuffleClient create(String cid, String secret, String redirectUri) {
        return new InternalTrueShuffleClient(cid, secret, redirectUri);
    }
}
