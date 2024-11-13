package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.TrueShuffleUser;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.UserLibrary;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.List;

import static nl.martderoos.trueshuffle.utility.PlaylistUtil.toSimplifiedPlaylist;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class TrueShufflePlaylistJobTest {

    @Test
    public void testShufflePlaylistIntoDesignatedPlaylist() throws Exception {
        var api = mock(ShuffleApi.class);
        var library = mock(UserLibrary.class);
        var user = mock(TrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var sourcePlaylist = defaultPlaylistBuilder().build();
        var sourceShufflePlaylist = spy(new ShufflePlaylist(api, toSimplifiedPlaylist(sourcePlaylist), false));
        when(library.getPlaylistById(sourcePlaylist.getId())).thenReturn(sourceShufflePlaylist);
        var sourceTracks = List.of("t1", "t2", "t3");
        doReturn(sourceTracks).when(sourceShufflePlaylist).getPlaylistTracksUris(); // works differently for spies

        var targetPlaylist = defaultPlaylistBuilder().setId("target").setName("target-name").build();
        var targetShufflePlaylist = spy(new ShufflePlaylist(api, toSimplifiedPlaylist(targetPlaylist), true));
        when(library.getPlaylistById(targetPlaylist.getId())).thenReturn(targetShufflePlaylist);
        when(library.isOwner(targetShufflePlaylist)).thenReturn(true);
        Mockito.doNothing().when(targetShufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(targetShufflePlaylist).addAndRemoveTracks(any(), any());
        var targetTracks = List.of("t1", "t4");
        when(api.streamPlaylistTracksUris(eq(targetPlaylist.getId()), anyInt())).thenReturn(targetTracks);
        when(api.getUserId()).thenReturn("user");
        when(api.getDisplayName()).thenReturn("user display name");

        var job = new TrueShufflePlaylistJob("user", "pid", "target");
        var result = job.execute((s) -> user, Runnable::run);

        verify(targetShufflePlaylist).shuffleInPlace();
        verify(targetShufflePlaylist).addAndRemoveTracks(eq(List.of("t2", "t3")), eq(List.of("t4")));
        assertEquals(ETrueShuffleJobStatus.FINISHED, result.getStatus());
        assertFalse(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("target", result.getTargetPlaylist().getPlaylistId());
        assertEquals("target-name", result.getTargetPlaylist().getName());
    }

    @Test
    public void testShufflePlaylistIntoDesignatedSelf() throws Exception {
        var api = mock(ShuffleApi.class);
        var library = mock(UserLibrary.class);
        var user = mock(TrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var sourcePlaylist = defaultPlaylistBuilder().build();
        var sourceShufflePlaylist = spy(new ShufflePlaylist(api, toSimplifiedPlaylist(sourcePlaylist), false));
        when(library.getPlaylistById(sourcePlaylist.getId())).thenReturn(sourceShufflePlaylist);
        when(library.isOwner(sourceShufflePlaylist)).thenReturn(true);
        var sourceTracks = List.of("t1", "t2", "t3");
        doReturn(sourceTracks).when(sourceShufflePlaylist).getPlaylistTracksUris(); // works differently for spies

        Mockito.doNothing().when(sourceShufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(sourceShufflePlaylist).addAndRemoveTracks(any(), any());
        when(api.getUserId()).thenReturn("user");
        when(api.getDisplayName()).thenReturn("user display name");

        var job = new TrueShufflePlaylistJob("user", "pid", "pid");
        var result = job.execute((s) -> user, Runnable::run);

        verify(sourceShufflePlaylist).shuffleInPlace();
        verify(sourceShufflePlaylist, times(0)).addAndRemoveTracks(any(), any());
        assertEquals(ETrueShuffleJobStatus.FINISHED, result.getStatus());
        assertFalse(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("pid", result.getTargetPlaylist().getPlaylistId());
        assertEquals("p-name", result.getTargetPlaylist().getName());
    }

    @Test
    public void testShufflePlaylistIntoItselfIfOwnerAndNoDesignatedPlaylistAssigned() throws Exception {
        var api = mock(ShuffleApi.class);
        var library = mock(UserLibrary.class);
        var user = mock(TrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var sourcePlaylist = defaultPlaylistBuilder().build();
        var sourceShufflePlaylist = spy(new ShufflePlaylist(api, toSimplifiedPlaylist(sourcePlaylist), false));
        when(library.getPlaylistById(sourcePlaylist.getId())).thenReturn(sourceShufflePlaylist);
        when(library.isOwner(sourceShufflePlaylist)).thenReturn(true);
        var sourceTracks = List.of("t1", "t2", "t3");
        doReturn(sourceTracks).when(sourceShufflePlaylist).getPlaylistTracksUris(); // works differently for spies

        Mockito.doNothing().when(sourceShufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(sourceShufflePlaylist).addAndRemoveTracks(any(), any());
        when(api.getUserId()).thenReturn("user");
        when(api.getDisplayName()).thenReturn("user display name");

        var job = new TrueShufflePlaylistJob("user", "pid");
        var result = job.execute((s) -> user, Runnable::run);

        verify(sourceShufflePlaylist).shuffleInPlace();
        verify(sourceShufflePlaylist, times(0)).addAndRemoveTracks(any(), any());
        assertEquals(ETrueShuffleJobStatus.FINISHED, result.getStatus());
        assertFalse(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("pid", result.getTargetPlaylist().getPlaylistId());
        assertEquals("p-name", result.getTargetPlaylist().getName());
    }

    @Test
    public void testShufflePlaylistAfterCopyWhenNotTheOwner() throws Exception {
        var api = mock(ShuffleApi.class);
        var library = mock(UserLibrary.class);
        var user = mock(TrueShuffleUser.class);

        when(user.getUserLibrary()).thenReturn(library);
        when(user.getApi()).thenReturn(api);

        var sourcePlaylist = defaultPlaylistBuilder().build();
        var sourceShufflePlaylist = spy(new ShufflePlaylist(api, toSimplifiedPlaylist(sourcePlaylist), false));
        when(library.getPlaylistById(sourcePlaylist.getId())).thenReturn(sourceShufflePlaylist);
        when(library.isOwner(sourceShufflePlaylist)).thenReturn(false);
        var sourceTracks = List.of("t1", "t2", "t3");
        doReturn(sourceTracks).when(sourceShufflePlaylist).getPlaylistTracksUris(); // works differently for spies

        var targetPlaylist = defaultPlaylistBuilder().setId("target").setName("target-name").build();
        var targetShufflePlaylist = spy(new ShufflePlaylist(api, toSimplifiedPlaylist(targetPlaylist), true));
        when(library.getPlaylistByName(sourcePlaylist.getName() + TrueShuffleJob.TRUE_SHUFFLE_SUFFIX, true)).thenReturn(List.of(targetShufflePlaylist));
        when(library.isOwner(targetShufflePlaylist)).thenReturn(true);
        Mockito.doNothing().when(targetShufflePlaylist).shuffleInPlace();
        Mockito.doNothing().when(targetShufflePlaylist).addAndRemoveTracks(any(), any());
        var targetTracks = List.of("t1", "t4");
        when(api.streamPlaylistTracksUris(eq(targetPlaylist.getId()), anyInt())).thenReturn(targetTracks);
        when(api.getUserId()).thenReturn("user");
        when(api.getDisplayName()).thenReturn("user display name");

        var job = new TrueShufflePlaylistJob("user", "pid");
        var result = job.execute((s) -> user, Runnable::run);

        verify(targetShufflePlaylist).shuffleInPlace();
        verify(targetShufflePlaylist).addAndRemoveTracks(eq(List.of("t2", "t3")), eq(List.of("t4")));
        assertEquals(ETrueShuffleJobStatus.FINISHED, result.getStatus());
        assertFalse(result.getSourcePlaylist().isLikedSongsPlaylist());
        assertEquals("target", result.getTargetPlaylist().getPlaylistId());
        assertEquals("target-name", result.getTargetPlaylist().getName());
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
                .setName("p-name")
                .setOwner(createUser("user", "display-name"))
                .setPublicAccess(true)
                .setSnapshotId("snap")
                .setTracks(new Paging.Builder<PlaylistTrack>().setTotal(0).build());
    }
}
