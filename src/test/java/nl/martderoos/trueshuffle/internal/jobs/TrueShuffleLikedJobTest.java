package nl.martderoos.trueshuffle.internal.jobs;

import nl.martderoos.trueshuffle.internal.InternalTrueShuffleUser;
import nl.martderoos.trueshuffle.api.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.api.jobs.ETrueShuffleJobStatus;
import nl.martderoos.trueshuffle.internal.model.TrueShuffleApi;
import nl.martderoos.trueshuffle.internal.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.internal.model.TrueShuffleUserLibrary;
import nl.martderoos.trueshuffle.internal.requests.exceptions.FatalRequestResponseException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.michaelthelin.spotify.model_objects.specification.*;

import java.util.List;

import static nl.martderoos.trueshuffle.internal.jobs.TrueShuffleJob.LIKED_SONGS_TRUE_SHUFFLE;
import static nl.martderoos.trueshuffle.internal.utility.PlaylistUtil.toPlaylistData;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class TrueShuffleLikedJobTest {
    @Test
    public void testShuffleLikedSongsIntoPlaylistNamedLikedSongsTrueShuffle() throws Exception {
        var api = mock(TrueShuffleApi.class);
        var library = mock(TrueShuffleUserLibrary.class);
        var user = mock(InternalTrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var playlist = defaultPlaylistBuilder().build();
        var simplified = toPlaylistData(playlist);
        var shufflePlaylist = spy(new ShufflePlaylist(api, simplified, true));
        Mockito.doNothing().when(shufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(shufflePlaylist).addAndRemoveTracks(any(), any());
        when(library.createPlaylist(anyString(), anyString())).thenReturn(shufflePlaylist);
        when(library.getPlaylistByName(eq(shufflePlaylist.getMetadata().name()), anyBoolean())).thenReturn(List.of(shufflePlaylist));
        when(library.getPlaylistById("pid")).thenReturn(shufflePlaylist);

        var targetPlaylistTracks = List.of("t1", "t4");
        var likedTracks = List.of("t1", "t2", "t3");
        when(library.getUserLikedTracksUris()).thenReturn(likedTracks);

        when(api.getUserId()).thenReturn("user");
        when(api.streamPlaylistTracksUris(eq("pid"), anyInt())).thenReturn(targetPlaylistTracks);
        when(api.getDisplayName()).thenReturn("user display name");

        var job = new TrueShuffleLikedJob("user");
        var result = new TrueShuffleJobExecutor().execute(job, (s) -> user, Runnable::run).getStatus();

        verify(shufflePlaylist).shuffleInPlace();
        verify(shufflePlaylist).addAndRemoveTracks(eq(List.of("t2", "t3")), eq(List.of("t4")));
        assertEquals(ETrueShuffleJobStatus.COMPLETED, result.getStatus());
        assertTrue(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("pid", result.getTargetPlaylist().getPlaylistId());
        assertEquals(LIKED_SONGS_TRUE_SHUFFLE, result.getTargetPlaylist().getName());
    }

    @Test
    public void testShuffleLikedSongsIntoDesignatedPlaylist() throws Exception {
        var api = mock(TrueShuffleApi.class);
        var library = mock(TrueShuffleUserLibrary.class);
        var user = mock(InternalTrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var targetPlaylist = defaultPlaylistBuilder().setId("target").build();
        var targetSimplified = toPlaylistData(targetPlaylist);
        var targetShufflePlaylist = spy(new ShufflePlaylist(api, targetSimplified, true));
        Mockito.doNothing().when(targetShufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(targetShufflePlaylist).addAndRemoveTracks(any(), any());
        when(library.getPlaylistById("target")).thenReturn(targetShufflePlaylist);
        when(library.isOwner(targetShufflePlaylist)).thenReturn(true);

        var targetPlaylistTracks = List.of("t1", "t4");
        var likedTracks = List.of("t1", "t2", "t3");
        when(library.getUserLikedTracksUris()).thenReturn(likedTracks);

        when(api.getUserId()).thenReturn("user");
        when(api.streamPlaylistTracksUris(eq(targetPlaylist.getId()), anyInt())).thenReturn(targetPlaylistTracks);
        when(api.getDisplayName()).thenReturn("user display name");

        var job = new TrueShuffleLikedJob("user", "target");
        var result = new TrueShuffleJobExecutor().execute(job, (s) -> user, Runnable::run).getStatus();

        verify(targetShufflePlaylist).shuffleInPlace();
        verify(targetShufflePlaylist).addAndRemoveTracks(eq(List.of("t2", "t3")), eq(List.of("t4")));
        assertEquals(ETrueShuffleJobStatus.COMPLETED, result.getStatus());
        assertTrue(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("target", result.getTargetPlaylist().getPlaylistId());
        assertEquals(LIKED_SONGS_TRUE_SHUFFLE, result.getTargetPlaylist().getName());
    }

    @Test
    public void testShuffleLikedResponseToFatalRequest() throws Exception {
        var api = mock(TrueShuffleApi.class);
        var library = mock(TrueShuffleUserLibrary.class);
        var user = mock(InternalTrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        when(library.getPlaylistById("target")).thenThrow(new FatalRequestResponseException("STUB"));

        when(api.getUserId()).thenReturn("user");
        when(api.getDisplayName()).thenReturn("user display name");

        var job = new TrueShuffleLikedJob("user", "target");
        var result = new TrueShuffleJobExecutor().execute(job, (s) -> user, Runnable::run).getStatus();

        assertEquals(ETrueShuffleJobStatus.TERMINATED, result.getStatus());
        assertTrue(result.getMessage().contains("STUB"));
        assertTrue(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertNull(result.getTargetPlaylist());
    }

    @Test
    public void testShuffleLikedSongsIntoInvalidDesignatedPlaylist() throws Exception {
        var api = mock(TrueShuffleApi.class);
        var library = mock(TrueShuffleUserLibrary.class);
        var user = mock(InternalTrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var targetPlaylist = defaultPlaylistBuilder().setId("target").build();
        var targetSimplified = toPlaylistData(targetPlaylist);
        var targetShufflePlaylist = spy(new ShufflePlaylist(api, targetSimplified, true));
        when(library.getPlaylistById("target")).thenReturn(targetShufflePlaylist);
        when(library.isOwner(targetShufflePlaylist)).thenReturn(false); // <-- not the owner!

        var job = new TrueShuffleLikedJob("user", "target");
        var result = new TrueShuffleJobExecutor().execute(job, (s) -> user, Runnable::run).getStatus();

        assertEquals(ETrueShuffleJobStatus.TERMINATED, result.getStatus());
        assertTrue(result.getMessage().contains("owner"));
        assertTrue(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertNull(result.getTargetPlaylist());
    }

    @Test
    public void testShuffleLikedSongsWithUnknownUser() throws Exception {
        var api = mock(TrueShuffleApi.class);
        var library = mock(TrueShuffleUserLibrary.class);
        var user = mock(InternalTrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var targetPlaylist = defaultPlaylistBuilder().setId("target").build();
        var targetSimplified = toPlaylistData(targetPlaylist);
        var targetShufflePlaylist = spy(new ShufflePlaylist(api, targetSimplified, true));
        Mockito.doNothing().when(targetShufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(targetShufflePlaylist).addAndRemoveTracks(any(), any());
        when(library.getPlaylistById("target")).thenReturn(targetShufflePlaylist);
        when(library.isOwner(targetShufflePlaylist)).thenReturn(false); // <-- not the owner!

        var job = new TrueShuffleLikedJob("peter", "target");
        var result = new TrueShuffleJobExecutor().execute(job, (s) -> {
            throw new UserNotFoundException("peter");
        }, Runnable::run).getStatus();

        assertEquals(ETrueShuffleJobStatus.SKIPPED, result.getStatus());
        assertTrue(result.getMessage().contains("user"));
        assertNull(result.getSourcePlaylist());
        assertNull(result.getTargetPlaylist());
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
