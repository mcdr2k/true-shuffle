package nl.martderoos.trueshuffle.jobs;

import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.model_objects.specification.Image;

import static nl.martderoos.trueshuffle.jobs.TrueShuffleJobPlaylistData.*;
import static org.junit.jupiter.api.Assertions.*;

public class TrueShuffleJobPlaylistDataTest {
    @Test
    public void testLikedSongsFactoryMethod() {
        assertThrows(NullPointerException.class, () -> newLikedSongsData(null));
        assertThrows(IllegalArgumentException.class, () -> newLikedSongsData(""));
        assertThrows(IllegalArgumentException.class, () -> newLikedSongsData(" "));
        var data = newLikedSongsData("MyLikedSongs");
        assertNull(data.getPlaylistId());
        assertEquals("MyLikedSongs", data.getName());
        assertNull(data.getImages());
    }

    @Test
    public void testRealPlaylistFactoryMethod() {
        var empty = new Image[0];
        assertThrows(NullPointerException.class, () -> newPlaylistData(null, "playlist", empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData("", "playlist", empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData(" ", "playlist", empty));

        assertThrows(NullPointerException.class, () -> newPlaylistData("pid", null, empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData("pid", "", empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData("pid", " ", empty));

        assertDoesNotThrow(() -> newPlaylistData("pid", "playlist", null)); // allow no images

        var data = newPlaylistData("pid", "playlist", new Image[]{null});
        assertEquals("pid", data.getPlaylistId());
        assertEquals("playlist", data.getName());
        assertArrayEquals(new Image[]{null}, data.getImages());
    }
}
