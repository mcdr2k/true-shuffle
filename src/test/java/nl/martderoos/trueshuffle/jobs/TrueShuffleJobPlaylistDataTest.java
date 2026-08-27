package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.api.TrueShuffleImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nl.martderoos.trueshuffle.jobs.TrueShuffleJobPlaylistData.newLikedSongsData;
import static nl.martderoos.trueshuffle.jobs.TrueShuffleJobPlaylistData.newPlaylistData;
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
        assertEquals(List.of(), data.getImages());
    }

    @Test
    public void testRealPlaylistFactoryMethod() {
        var empty = List.<TrueShuffleImage>of();
        assertThrows(NullPointerException.class, () -> newPlaylistData(null, "playlist", empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData("", "playlist", empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData(" ", "playlist", empty));

        assertThrows(NullPointerException.class, () -> newPlaylistData("pid", null, empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData("pid", "", empty));
        assertThrows(IllegalArgumentException.class, () -> newPlaylistData("pid", " ", empty));

        assertDoesNotThrow(() -> newPlaylistData("pid", "playlist", null)); // allow no images

        var image = new TrueShuffleImage("url", 0, 0);
        var data = newPlaylistData("pid", "playlist", List.of(image));
        assertEquals("pid", data.getPlaylistId());
        assertEquals("playlist", data.getName());
        assertEquals(List.of(image), data.getImages());
    }
}
