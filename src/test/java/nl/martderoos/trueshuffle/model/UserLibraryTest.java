package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
