package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.exceptions.ImmutablePlaylistException;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.List;

import static nl.martderoos.trueshuffle.utility.PlaylistUtil.toSimplifiedPlaylist;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ShufflePlaylistTest {
    @Test
    public void testShuffleInPlace() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        var simplified = toSimplifiedPlaylist(defaultPlaylistBuilder().build());
        when(api.streamPlaylistSimplified(eq("pid"))).thenReturn(simplified);
        var playlist = new ShufflePlaylist(api, simplified, true);

        playlist.shuffleInPlace();

        verify(api, times(3)).reorderTrack(any(), anyInt(), anyInt(), any());
    }

    @Test
    public void testAddAndRemoveTracks() throws FatalRequestResponseException {
        var api = mock(ShuffleApi.class);
        var simplified = toSimplifiedPlaylist(defaultPlaylistBuilder().build());
        when(api.streamPlaylistSimplified(eq("pid"))).thenReturn(simplified);
        when(api.removeTracks(any(), any(), any())).thenReturn("snap2");
        var playlist = new ShufflePlaylist(api, simplified, true);

        playlist.addAndRemoveTracks(List.of("t1", "t4", "t5"), List.of("t2, t3"));

        verify(api).removeTracks("pid", "snap", List.of("t2, t3"));
        verify(api).addTracks("pid", "snap2", List.of("t1", "t4", "t5"));
    }

    @Test
    public void testImmutablePlaylist() {
        var api = mock(ShuffleApi.class);
        var simplified = toSimplifiedPlaylist(defaultPlaylistBuilder().build());
        var playlist = new ShufflePlaylist(api, simplified, false);

        assertThrows(ImmutablePlaylistException.class, playlist::shuffleInPlace);
        assertThrows(ImmutablePlaylistException.class, () -> playlist.addAndRemoveTracks(null, null));
    }

    private Playlist.Builder defaultPlaylistBuilder() {
        return new Playlist.Builder()
                .setCollaborative(false)
                .setId("pid")
                .setName("playlist-name")
                .setOwner(createUser("user", "display-name"))
                .setPublicAccess(true)
                .setSnapshotId("snap")
                .setTracks(new Paging.Builder<PlaylistTrack>().setTotal(3).build());
    }

    private User createUser(String userId, String displayName) {
        return new User.Builder()
                .setId(userId)
                .setDisplayName(displayName)
                .build();
    }
}
