package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.TrueShuffleUser;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.UserLibrary;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.List;

import static nl.martderoos.trueshuffle.jobs.TrueShuffleJob.LIKED_SONGS_TRUE_SHUFFLE;
import static nl.martderoos.trueshuffle.utility.PlaylistUtil.toSimplifiedPlaylist;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class TrueShuffleLikedJobTest {
    @Test
    public void testShuffleLikedSongsIntoPlaylistNamedLikedSongsTrueShuffle() throws Exception {
        var api = mock(ShuffleApi.class);
        var library = mock(UserLibrary.class);
        var user = mock(TrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var playlist = defaultPlaylistBuilder().build();
        var simplified = toSimplifiedPlaylist(playlist);
        var shufflePlaylist = spy(new ShufflePlaylist(api, simplified, true));
        Mockito.doNothing().when(shufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(shufflePlaylist).addAndRemoveTracks(any(), any());
        when(library.createPlaylist(anyString(), anyString())).thenReturn(shufflePlaylist);
        when(library.getPlaylistByName(eq(shufflePlaylist.getName()), anyBoolean())).thenReturn(List.of(shufflePlaylist));
        when(library.getPlaylistById("pid")).thenReturn(shufflePlaylist);

        var targetPlaylistTracks = List.of("t1", "t4");
        var likedTracks = List.of("t1", "t2", "t3");
        when(library.getUserLikedTracksUris()).thenReturn(likedTracks);

        var job = new TrueShuffleLikedJob("user");

        when(api.getUserId()).thenReturn("user");
        when(api.streamPlaylistTracksUris(eq("pid"), anyInt())).thenReturn(targetPlaylistTracks);
        when(api.getDisplayName()).thenReturn("user display name");

        var result = job.execute((s) -> user, Runnable::run);

        verify(shufflePlaylist).shuffleInPlace();
        verify(shufflePlaylist).addAndRemoveTracks(eq(List.of("t2", "t3")), eq(List.of("t4")));
        assertEquals(ETrueShuffleJobStatus.FINISHED, result.getStatus());
        assertTrue(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("pid", result.getTargetPlaylist().getPlaylistId());
        assertEquals(LIKED_SONGS_TRUE_SHUFFLE, result.getTargetPlaylist().getName());
    }

    @Test
    public void testShuffleLikedSongsIntoDesignatedPlaylist() throws Exception {
        var api = mock(ShuffleApi.class);
        var library = mock(UserLibrary.class);
        var user = mock(TrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var targetPlaylist = defaultPlaylistBuilder().setId("target").build();
        var targetSimplified = toSimplifiedPlaylist(targetPlaylist);
        var targetShufflePlaylist = spy(new ShufflePlaylist(api, targetSimplified, true));
        Mockito.doNothing().when(targetShufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(targetShufflePlaylist).addAndRemoveTracks(any(), any());
        when(library.getPlaylistById("target")).thenReturn(targetShufflePlaylist);
        when(library.isOwner(targetShufflePlaylist)).thenReturn(true);

        var targetPlaylistTracks = List.of("t1", "t4");
        var likedTracks = List.of("t1", "t2", "t3");
        when(library.getUserLikedTracksUris()).thenReturn(likedTracks);

        var job = new TrueShuffleLikedJob("user", "target");

        when(api.getUserId()).thenReturn("user");
        when(api.streamPlaylistTracksUris(eq(targetPlaylist.getId()), anyInt())).thenReturn(targetPlaylistTracks);
        when(api.getDisplayName()).thenReturn("user display name");

        var result = job.execute((s) -> user, Runnable::run);

        verify(targetShufflePlaylist).shuffleInPlace();
        verify(targetShufflePlaylist).addAndRemoveTracks(eq(List.of("t2", "t3")), eq(List.of("t4")));
        assertEquals(ETrueShuffleJobStatus.FINISHED, result.getStatus());
        assertTrue(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("target", result.getTargetPlaylist().getPlaylistId());
        assertEquals(LIKED_SONGS_TRUE_SHUFFLE, result.getTargetPlaylist().getName());
    }

    private User createUser(String userId, String displayName) {
        return new User.Builder()
                .setId(userId)
                .setDisplayName(displayName)
                .build();
    }

    private Playlist.Builder defaultPlaylistBuilder() {
        return new Playlist.Builder()
                .setCollaborative(false)
                .setId("pid")
                .setName(TrueShuffleJob.LIKED_SONGS_TRUE_SHUFFLE)
                .setOwner(createUser("user", "display-name"))
                .setPublicAccess(true)
                .setSnapshotId("snap")
                .setTracks(new Paging.Builder<PlaylistTrack>().setTotal(0).build());
    }
}
