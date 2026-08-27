package nl.martderoos.trueshuffle;

import nl.martderoos.trueshuffle.InternalTrueShuffleClient;
import nl.martderoos.trueshuffle.InternalTrueShuffleUser;
import nl.martderoos.trueshuffle.exceptions.FatalRequestResponseException;
import nl.martderoos.trueshuffle.exceptions.InitializationException;
import nl.martderoos.trueshuffle.exceptions.AuthorizationException;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.jobs.ETrueShuffleJobStatus;
import nl.martderoos.trueshuffle.api.TrueShufflePlaylistMetadata;
import nl.martderoos.trueshuffle.api.TrueShufflePlaylistOwner;
import nl.martderoos.trueshuffle.api.TrueShuffleUserCredentials;
import nl.martderoos.trueshuffle.requests.RequestHandler;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.InternalTrueShuffleUserLibrary;
import nl.martderoos.trueshuffle.model.TrueShuffleApi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.User;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InternalTrueShuffleClientTest {
    @Test
    public void testIllegalArguments() {
        assertThrows(NullPointerException.class, () -> InternalTrueShuffleClient.create("cid", "secret", null));
        assertThrows(NullPointerException.class, () -> InternalTrueShuffleClient.create("cid", null, "uri"));
        assertThrows(NullPointerException.class, () -> InternalTrueShuffleClient.create(null, "secret", "uri"));
    }

    @Test
    public void testInitializationThrowingOnErrors() {
        var client = InternalTrueShuffleClient.create("cid", "secret", "uri");
        assertThrows(InitializationException.class, client::initialize);
    }

    @Test
    public void testClientStartsWithNoAuthorizedUsers() {
        var client = InternalTrueShuffleClient.create("cid", "secret", "http://localhost/callback");

        assertNotNull(client.getAuthorizedUsers());
        assertTrue(client.getAuthorizedUsers().isEmpty());
    }

    @Test
    public void testRemoveAuthorizedUser() {
        var client = InternalTrueShuffleClient.create("cid", "secret", "http://localhost/callback");

        client.removeAuthorizedUser("user");

        assertTrue(client.getAuthorizedUsers().isEmpty());
    }

    @Test
    public void testGetAuthorizedUserThrowsWhenUserIsMissing() {
        var client = InternalTrueShuffleClient.create("cid", "secret", "http://localhost/callback");

        assertThrows(UserNotFoundException.class, () -> client.getAuthorizedUser("user"));
    }

    @Test
    public void testAddAuthorizedUserRequiresInitialization() {
        var client = InternalTrueShuffleClient.create("cid", "secret", "http://localhost/callback");
        var credentials = new TrueShuffleUserCredentials(0, "access-token", "refresh-token", 3600);

        assertThrows(IllegalStateException.class, () -> client.addAuthorizedUser("user", credentials));
        assertThrows(IllegalStateException.class, () -> client.addAuthorizedUser("code"));
    }

    @Test
    public void testShuffleOperationsRequireInitialization() {
        var client = InternalTrueShuffleClient.create("cid", "secret", "http://localhost/callback");
        Executor executor = Runnable::run;

        assertThrows(IllegalStateException.class, () -> client.shuffleLikedSongs("user", executor));
        assertThrows(IllegalStateException.class, () -> client.shufflePlaylist("user", "playlist", executor));
    }

    @Test
    public void testAuthorizationUriAndRedirectUri() {
        URI redirectUri = URI.create("http://localhost/callback");
        var client = InternalTrueShuffleClient.create("cid", "secret", redirectUri.toString());

        assertEquals(redirectUri, client.getRedirectUri());
        assertTrue(client.getAuthorizationURI("test-state").toString().contains("state=test-state"));
        assertFalse(client.getAuthorizationURI(null).toString().contains("state="));
    }

    @Test
    public void testInitializeUsesSpotifyClientAndStoresAccessToken() throws Exception {
        var spotifyApi = mock(SpotifyApi.class, Mockito.RETURNS_DEEP_STUBS);
        var handler = mock(RequestHandler.class);
        var credentials = mock(ClientCredentials.class);
        when(credentials.getAccessToken()).thenReturn("client-access-token");
        when(spotifyApi.clientCredentials().build()).thenReturn(new ClientCredentialsRequest.Builder("cid", "secret").build());
        when(handler.handleRequest(any())).thenReturn(credentials);

        var client = new InternalTrueShuffleClient("cid", "secret", "http://localhost/callback", spotifyApi, handler);

        client.initialize();

        verify(handler).handleRequest(any());
        verify(spotifyApi).setAccessToken("client-access-token");
    }

    @Test
    public void testAddAuthorizedUserWithCredentialsUsesSpotifyProfile() throws Exception {
        var spotifyApi = mock(SpotifyApi.class, Mockito.RETURNS_DEEP_STUBS);
        var handler = mock(RequestHandler.class);
        var clientCredentials = mock(ClientCredentials.class);
        var userData = new User.Builder()
                .setId("user")
                .setDisplayName("display name")
                .build();
        when(clientCredentials.getAccessToken()).thenReturn("client-access-token");
        when(spotifyApi.clientCredentials().build()).thenReturn(new ClientCredentialsRequest.Builder("cid", "secret").build());
        when(handler.handleRequest(any())).thenReturn(clientCredentials);

        var client = new InternalTrueShuffleClient("cid", "secret", "http://localhost/callback", spotifyApi, handler);
        client.initialize();

        Mockito.reset(handler);
        when(handler.handleRequest(any())).thenReturn(userData);
        var credentials = new TrueShuffleUserCredentials(0, "access-token", "refresh-token", 3600);

        var user = client.addAuthorizedUser(null, credentials);

        assertEquals("user", user.getUserId());
        assertEquals("display name", user.getDisplayName());
        assertEquals(Set.of("user"), client.getAuthorizedUsers());
    }

    @Test
    public void testAddAuthorizedUserWithUnknownUserIdUsesSpotifyProfile() throws Exception {
        var spotifyApi = mock(SpotifyApi.class, Mockito.RETURNS_DEEP_STUBS);
        var handler = mock(RequestHandler.class);
        var clientCredentials = mock(ClientCredentials.class);
        var userData = new User.Builder()
                .setId("profile-user")
                .setDisplayName("display name")
                .build();
        when(clientCredentials.getAccessToken()).thenReturn("client-access-token");
        when(spotifyApi.clientCredentials().build()).thenReturn(new ClientCredentialsRequest.Builder("cid", "secret").build());
        when(handler.handleRequest(any())).thenReturn(clientCredentials);

        var client = new InternalTrueShuffleClient("cid", "secret", "http://localhost/callback", spotifyApi, handler);
        client.initialize();

        Mockito.reset(handler);
        when(handler.handleRequest(any())).thenReturn(userData);
        var credentials = new TrueShuffleUserCredentials(0, "access-token", "refresh-token", 3600);

        var user = client.addAuthorizedUser("unknown-user", credentials);

        assertEquals("profile-user", user.getUserId());
        assertEquals(Set.of("profile-user"), client.getAuthorizedUsers());
    }

    @Test
    public void testAddAuthorizedUserWithCredentialsWrapsProfileFailure() throws Exception {
        var spotifyApi = mock(SpotifyApi.class, Mockito.RETURNS_DEEP_STUBS);
        var handler = mock(RequestHandler.class);
        var clientCredentials = mock(ClientCredentials.class);
        when(clientCredentials.getAccessToken()).thenReturn("client-access-token");
        when(spotifyApi.clientCredentials().build()).thenReturn(new ClientCredentialsRequest.Builder("cid", "secret").build());
        when(handler.handleRequest(any())).thenReturn(clientCredentials);

        var client = new InternalTrueShuffleClient("cid", "secret", "http://localhost/callback", spotifyApi, handler);
        client.initialize();

        Mockito.reset(handler);
        when(handler.handleRequest(any())).thenThrow(new FatalRequestResponseException("failed"));
        var credentials = new TrueShuffleUserCredentials(0, "access-token", "refresh-token", 3600);

        assertThrows(AuthorizationException.class, () -> client.addAuthorizedUser(null, credentials));
    }

    @Test
    public void testAddAuthorizedUserWithCodeUsesSpotifyAuthorizationAndProfile() throws Exception {
        var spotifyApi = mock(SpotifyApi.class, Mockito.RETURNS_DEEP_STUBS);
        var handler = mock(RequestHandler.class);
        var clientCredentials = mock(ClientCredentials.class);
        var authorizationCredentials = mock(AuthorizationCodeCredentials.class);
        var userData = new User.Builder()
                .setId("code-user")
                .setDisplayName("display name")
                .build();
        when(clientCredentials.getAccessToken()).thenReturn("client-access-token");
        when(spotifyApi.clientCredentials().build()).thenReturn(new ClientCredentialsRequest.Builder("cid", "secret").build());
        when(handler.handleRequest(any())).thenReturn(clientCredentials);

        var client = new InternalTrueShuffleClient("cid", "secret", "http://localhost/callback", spotifyApi, handler);
        client.initialize();

        when(authorizationCredentials.getAccessToken()).thenReturn("access-token");
        when(authorizationCredentials.getRefreshToken()).thenReturn("refresh-token");
        when(authorizationCredentials.getExpiresIn()).thenReturn(3600);
        var authorizationRequest = mock(AuthorizationCodeRequest.class);
        when(spotifyApi.authorizationCode("code").build()).thenReturn(authorizationRequest);
        Mockito.reset(handler);
        when(handler.handleRequest(any())).thenReturn(authorizationCredentials, userData);

        var user = client.addAuthorizedUser("code");

        assertEquals("code-user", user.getUserId());
        assertEquals("display name", user.getDisplayName());
        assertEquals(Set.of("code-user"), client.getAuthorizedUsers());
        verify(handler, Mockito.times(2)).handleRequest(any());
    }

    @Test
    public void testShuffleLikedSongsExecutesThroughProvidedExecutor() throws Exception {
        var user = mock(InternalTrueShuffleUser.class);
        var library = mock(InternalTrueShuffleUserLibrary.class);
        var api = mock(TrueShuffleApi.class);
        var target = mock(ShufflePlaylist.class);
        var handler = mock(RequestHandler.class);
        var client = new InternalTrueShuffleClient("cid", "secret", "http://localhost/callback",
                mock(SpotifyApi.class, Mockito.RETURNS_DEEP_STUBS), handler, userId -> user);
        initialize(client, handler);
        var metadata = new TrueShufflePlaylistMetadata(
                "target",
                "Liked Songs - TrueShuffle",
                new TrueShufflePlaylistOwner("user", "user"),
                null,
                false,
                false,
                "snapshot",
                2,
                List.of()
        );

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);
        when(library.getPlaylistByName("Liked Songs - TrueShuffle", true)).thenReturn(List.of(target));
        when(library.isOwnerOf(target)).thenReturn(true);
        when(target.getMetadata()).thenReturn(metadata);
        when(target.getTracksUris()).thenReturn(List.of("old-track"));
        when(library.getUserLikedTracksUris()).thenReturn(List.of("liked-track"));
        doNothing().when(target).addAndRemoveTracks(any(), any());
        var execution = client.shuffleLikedSongs("user", Runnable::run);

        assertEquals(ETrueShuffleJobStatus.COMPLETED, execution.getStatus().getStatus());
        verify(target).addAndRemoveTracks(eq(List.of("liked-track")), eq(List.of("old-track")));
    }

    @Test
    public void testShufflePlaylistExecutesThroughProvidedExecutor() throws Exception {
        var user = mock(InternalTrueShuffleUser.class);
        var source = mock(ShufflePlaylist.class);
        var handler = mock(RequestHandler.class);
        var client = new InternalTrueShuffleClient("cid", "secret", "http://localhost/callback",
                mock(SpotifyApi.class, Mockito.RETURNS_DEEP_STUBS), handler, userId -> user);
        initialize(client, handler);
        var library = mock(InternalTrueShuffleUserLibrary.class);
        var metadata = new TrueShufflePlaylistMetadata(
                "playlist",
                "Playlist",
                new TrueShufflePlaylistOwner("user", "user"),
                null,
                false,
                false,
                "snapshot",
                0,
                List.of()
        );

        when(user.getUserLibrary()).thenReturn(library);
        when(library.getPlaylistById("playlist")).thenReturn(source);
        when(library.isOwnerOf(source)).thenReturn(true);
        when(source.getMetadata()).thenReturn(metadata);
        doNothing().when(source).shuffleInPlace();
        var execution = client.shufflePlaylist("user", "playlist", Runnable::run);

        assertEquals(ETrueShuffleJobStatus.COMPLETED, execution.getStatus().getStatus());
        verify(source).shuffleInPlace();
    }

    private void initialize(InternalTrueShuffleClient client, RequestHandler handler) throws Exception {
        var credentials = mock(ClientCredentials.class);
        when(credentials.getAccessToken()).thenReturn("client-access-token");
        when(handler.handleRequest(any())).thenReturn(credentials);
        client.initialize();
    }

}
