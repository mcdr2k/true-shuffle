package nl.martderoos.trueshuffle.utility;

import nl.martderoos.trueshuffle.api.TrueShufflePlaylistMetadata;
import nl.martderoos.trueshuffle.api.TrueShufflePlaylistOwner;
import nl.martderoos.trueshuffle.exceptions.FatalRequestResponseException;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.TrueShuffleApi;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class ShuffleUtilTest {
    TrueShufflePlaylistMetadata data = new TrueShufflePlaylistMetadata("pid", "name", new TrueShufflePlaylistOwner("owner-id", "owner-display-name"), "description", false, false, "snapshot", 10, List.of());

    @Test
    public void testShuffleDiffUniqueTracks() throws FatalRequestResponseException {
        var tracks = List.of("t1", "t2", "t3", "t4", "t5");
        var playlistMock = mock(ShufflePlaylist.class);
        when(playlistMock.getTracksUris()).thenReturn(tracks);
        when(playlistMock.getMetadata()).thenReturn(data);
        ShuffleUtil.shuffleInto(mock(TrueShuffleApi.class), playlistMock, List.of("t2", "t3", "t6"));

        verify(playlistMock).addAndRemoveTracks(eq(List.of("t6")), eq(List.of("t1", "t4", "t5")));
        verify(playlistMock).shuffleInPlace();
    }

    @Test
    public void testShuffleDiffDuplicateTracks() throws FatalRequestResponseException {
        // t2 and t3 have duplicates
        var tracks = List.of("t1", "t2", "t3", "t4", "t5", "t2", "t3", "t2");
        var playlistMock = mock(ShufflePlaylist.class);
        when(playlistMock.getTracksUris()).thenReturn(tracks);
        when(playlistMock.getMetadata()).thenReturn(data);
        // t1 is the only duplicate
        ShuffleUtil.shuffleInto(mock(TrueShuffleApi.class), playlistMock, List.of("t1", "t3", "t6", "t1"));

        // note that the order is a bit funny but this is just how the algorithm works
        verify(playlistMock).addAndRemoveTracks(eq(List.of("t6", "t1")), eq(List.of("t2", "t4", "t5", "t2", "t3", "t2")));
        verify(playlistMock).shuffleInPlace();
    }
}
