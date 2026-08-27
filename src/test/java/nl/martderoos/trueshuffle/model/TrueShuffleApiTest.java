package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.api.TrueShufflePlaylistMetadata;
import nl.martderoos.trueshuffle.api.TrueShuffleUserCredentials;
import nl.martderoos.trueshuffle.requests.RequestHandler;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.miscellaneous.PlaylistTracksInformation;
import se.michaelthelin.spotify.model_objects.special.SnapshotResult;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TrueShuffleApiTest {
    @Test
    public void testInitialCredentials() {
        var spotifyApi = realSpotifyApi();
        spotifyApi.setAccessToken("a-token");
        spotifyApi.setRefreshToken("r-token");
        var api = new TrueShuffleApi(spotifyApi, createUser());

        assertEquals("a-token", api.getAccessToken());
        assertEquals("r-token", api.getRefreshToken());

        var credentials = api.getCredentials();
        assertEquals("a-token", credentials.accessToken());
        assertEquals("r-token", credentials.refreshToken());
    }

    @Test
    public void testAssigningFullCredentials() {
        var spotifyApi = realSpotifyApi();
        var api = new TrueShuffleApi(spotifyApi, createUser());

        var credentials = new TrueShuffleUserCredentials(System.currentTimeMillis(), "new-a-token", "new-r-token", 100);
        api.assignCredentials(credentials);

        assertEquals("new-a-token", api.getAccessToken());
        assertEquals("new-r-token", api.getRefreshToken());
    }

    @Test
    public void testAssigningPartialCredentials() {
        var spotifyApi = realSpotifyApi();
        var api = new TrueShuffleApi(spotifyApi, createUser());

        var credentials = new TrueShuffleUserCredentials(System.currentTimeMillis(), "new-a-token", "new-r-token", 50);
        api.assignCredentials(credentials);
        var credentials2 = new TrueShuffleUserCredentials(credentials.issuedSinceEpoch() + 1, "new-a-token2", null, 60);
        api.assignCredentials(credentials2);
        var uselessCredentials = new TrueShuffleUserCredentials(credentials.issuedSinceEpoch() + 2, null, null, 70);
        api.assignCredentials(uselessCredentials);

        var result = api.getCredentials();
        assertEquals(credentials2.issuedSinceEpoch(), result.issuedSinceEpoch());
        assertEquals(60, result.expiresInSeconds());

        assertEquals("new-a-token2", api.getAccessToken());
        assertEquals("new-r-token", api.getRefreshToken());
    }

    @Test
    public void testAssigningExpiredCredentials() {
        var spotifyApi = realSpotifyApi();
        var api = new TrueShuffleApi(spotifyApi, createUser());

        var credentials = new TrueShuffleUserCredentials(System.currentTimeMillis(), "a-token1", "r-token1", 0);
        api.assignCredentials(credentials);
        var expiredCredentials = new TrueShuffleUserCredentials(0, "a-token2", "r-token2", 0);
        api.assignCredentials(expiredCredentials);

        assertEquals("a-token1", api.getAccessToken());
        assertEquals("r-token1", api.getRefreshToken());
    }

    @Test
    public void testStreamPlaylist() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(playlist().build());
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals("pid", api.streamPlaylist("pid").id());
    }

    @Test
    public void testStreamPlaylistSimplified() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(playlist().build());
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals("pid", api.streamPlaylistSimplified("pid").id());
    }

    @Test
    public void testStreamUserPlaylists() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(new Paging.Builder<PlaylistSimplified>().setItems(new PlaylistSimplified[]{playlistSimplified().build()}).setTotal(1).build());
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals(List.of("pid"), api.streamUserPlaylists(10).stream().map(TrueShufflePlaylistMetadata::id).toList());
    }

    @Test
    public void testSearchPlaylistByExactNameFiltersNonExactMatches() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(new Paging.Builder<PlaylistSimplified>().setItems(new PlaylistSimplified[]{playlistSimplified().build(), playlistSimplified().setName("other").build()}).setTotal(2).build());
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals(List.of("pid"), api.searchPlaylistByExactName("playlist-name", 10).stream().map(TrueShufflePlaylistMetadata::id).toList());
    }

    @Test
    public void testStreamUserLikedTracksUris() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(new Paging.Builder<SavedTrack>().setItems(new SavedTrack[]{new SavedTrack.Builder().setTrack(new Track.Builder().setUri("track-uri").build()).build()}).setTotal(1).build());
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals(List.of("track-uri"), api.streamUserLikedTracksUris(10));
    }

    @Test
    public void testStreamPlaylistTracksUris() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(new Paging.Builder<PlaylistTrack>()
                .setItems(new PlaylistTrack[]{new PlaylistTrack.Builder().setTrack(new Track.Builder().setUri("track-uri").build()).build()})
                .setTotal(1).build());
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals(List.of("track-uri"), api.streamPlaylistTracksUris("pid", 10));
    }

    @Test
    public void testAddTracks() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(snapshot("new-snapshot"));
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals("new-snapshot", api.addTracks("pid", "snapshot", List.of("a", "b")));
    }

    @Test
    public void testRemoveTracks() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(snapshot("new-snapshot"));
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals("new-snapshot", api.removeTracks("pid", "snapshot", List.of("a", "b")));
    }

    @Test
    public void testReorderTrack() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(snapshot("new-snapshot"));
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals("new-snapshot", api.reorderTrack("pid", 2, 0, "snapshot"));
    }

    @Test
    public void testUploadPlaylist() throws Exception {
        var handler = mock(RequestHandler.class);
        when(handler.handleRequest(any())).thenReturn(playlist().build());
        var api = new TrueShuffleApi(realSpotifyApi(), createUser(), handler);

        assertEquals("pid", api.uploadPlaylist("playlist-name", "description").id());
    }

    @Test
    public void testUserDetails() {
        var api = new TrueShuffleApi(realSpotifyApi(), createUser());

        assertEquals("uid", api.getUserId());
        assertEquals("uname", api.getDisplayName());
    }

    private SpotifyApi realSpotifyApi() {
        return SpotifyApi.builder().setClientId("client-id").setClientSecret("client-secret").setAccessToken("access-token").build();
    }

    private Playlist.Builder playlist() {
        return new Playlist.Builder().setId("pid").setName("playlist-name").setOwner(createUser()).setCollaborative(false).setPublicAccess(true).setSnapshotId("snapshot").setTracks(new Paging.Builder<PlaylistTrack>().setTotal(0).build());
    }

    private PlaylistSimplified.Builder playlistSimplified() {
        return new PlaylistSimplified.Builder()
                .setId("pid")
                .setName("playlist-name")
                .setOwner(createUser())
                .setCollaborative(false)
                .setPublicAccess(true)
                .setSnapshotId("snapshot")
                .setTracks(new PlaylistTracksInformation.Builder().setTotal(0).build());
    }

    private SnapshotResult snapshot(String snapshotId) {
        return new SnapshotResult.Builder().setSnapshotId(snapshotId).build();
    }

    private User createUser() {
        return new User.Builder().setId("uid").setDisplayName("uname").build();
    }
}
