package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.List;

import static nl.martderoos.trueshuffle.utility.PlaylistUtil.toSimplifiedPlaylist;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class UserLibraryTest {
    @Test
    public void testGetLikedTracks() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        var lib = new UserLibrary(api);
        when(api.streamUserLikedTracksUris(anyInt())).thenReturn(List.of("l1", "l3"));
        assertEquals(List.of("l1", "l3"), lib.getUserLikedTracksUris());
    }

    @Test
    public void testCreatingNewPlaylist() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        when(api.getUserId()).thenReturn("user");

        var lib = new UserLibrary(api);
        var uploadedPlaylist = defaultPlaylistBuilder().setId("upload-id").setName("newPlaylist").build();
        when(api.uploadPlaylist(anyString(), anyString())).thenReturn(uploadedPlaylist);

        lib.createPlaylist("newPlaylist", "newDescription");

        verify(api).uploadPlaylist("newPlaylist", "newDescription");

        var actualFoundPlaylist = lib.getPlaylistByName("newPlaylist", true).get(0);
        assertEquals("newPlaylist", actualFoundPlaylist.getName());
        assertEquals("upload-id", actualFoundPlaylist.getPlaylistId());

        actualFoundPlaylist = lib.getPlaylistById("upload-id");
        assertNotNull(actualFoundPlaylist);
        assertEquals("newPlaylist", actualFoundPlaylist.getName());
    }

    @Test
    public void testGetPlaylistByName() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        when(api.getUserId()).thenReturn("user");

        PlaylistSimplified p1 = toSimplifiedPlaylist(defaultPlaylistBuilder().setName("cool").setId("pid1").build());
        PlaylistSimplified p2 = toSimplifiedPlaylist(defaultPlaylistBuilder().setName("cool").setId("pid2").setOwner(createUser("some guy", "some name")).build());
        PlaylistSimplified p3 = toSimplifiedPlaylist(defaultPlaylistBuilder().setName("cool").setId("pid3").build());
        when(api.searchPlaylistByExactName(eq("cool"), anyInt())).thenReturn(List.of(p1, p2, p3));

        var lib = new UserLibrary(api);
        var result = lib.getPlaylistByName("cool", false);
        assertEquals(3, result.size());
        assertEquals("pid1", result.get(0).getPlaylistId());
        assertEquals("pid2", result.get(1).getPlaylistId());
        assertEquals("pid3", result.get(2).getPlaylistId());
        result = lib.getPlaylistByName("cool", true);
        assertEquals(2, result.size());
        assertEquals("pid1", result.get(0).getPlaylistId());
        assertEquals("pid3", result.get(1).getPlaylistId());

        // cached result, so only 1 call made
        verify(api, times(1)).searchPlaylistByExactName(eq("cool"), anyInt());
    }

    @Test
    public void testGetPlaylistById() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        when(api.getUserId()).thenReturn("user");

        PlaylistSimplified p1 = toSimplifiedPlaylist(defaultPlaylistBuilder().setName("n1").setId("pid1").build());
        PlaylistSimplified p2 = toSimplifiedPlaylist(defaultPlaylistBuilder().setName("n2").setId("pid2").setOwner(createUser("some guy", "some name")).build());
        when(api.streamPlaylistSimplified("pid1")).thenReturn(p1);
        when(api.streamPlaylistSimplified("pid2")).thenReturn(p2);

        var lib = new UserLibrary(api);
        var result = lib.getPlaylistById("pid1");

        assertEquals("pid1", result.getPlaylistId());
        assertTrue(result.isMutable());
        lib.getPlaylistById("pid1");

        result = lib.getPlaylistById("pid2");
        assertEquals("pid2", result.getPlaylistId());
        assertFalse(result.isMutable());

        // caching
        verify(api, times(1)).streamPlaylistSimplified("pid1");
        verify(api, times(1)).streamPlaylistSimplified("pid2");
    }

    @Test
    public void testGetEmptyMostRecentPlaylists() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        when(api.getUserId()).thenReturn("user");

        var lib = new UserLibrary(api);
        var result = lib.getMostRecentPlaylists(10);
        assertEquals(List.of(), result);
        result = lib.getMostRecentPlaylists(10);
        assertEquals(List.of(), result);

        verify(api, times(1)).streamUserPlaylists(anyInt());
    }

    @Test
    public void testGetNonEmptyMostRecentPlaylists() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        when(api.getUserId()).thenReturn("user");

        var p1 = toSimplifiedPlaylist(defaultPlaylistBuilder().setId("p1").build());
        var p2 = toSimplifiedPlaylist(defaultPlaylistBuilder().setId("p2").build());
        var p3 = toSimplifiedPlaylist(defaultPlaylistBuilder().setId("p3").build());
        when(api.streamUserPlaylists(anyInt())).thenReturn(List.of(p2, p1, p3));

        var lib = new UserLibrary(api);
        var result = lib.getMostRecentPlaylists(10);
        assertEquals(List.of(p2, p1, p3), result);
        result = lib.getMostRecentPlaylists(10);
        assertEquals(List.of(p2, p1, p3), result);

        verify(api, times(1)).streamUserPlaylists(anyInt());
    }

    private Playlist.Builder defaultPlaylistBuilder() {
        return new Playlist.Builder()
                .setCollaborative(false)
                .setId("pid")
                .setName("playlist-name")
                .setOwner(createUser("user", "display-name"))
                .setPublicAccess(true)
                .setSnapshotId("snap")
                .setTracks(new Paging.Builder<PlaylistTrack>().setTotal(0).build());
    }

    private User createUser(String userId, String displayName) {
        return new User.Builder()
                .setId(userId)
                .setDisplayName(displayName)
                .build();
    }
}
