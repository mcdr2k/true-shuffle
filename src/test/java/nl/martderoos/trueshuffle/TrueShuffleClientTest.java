package nl.martderoos.trueshuffle;

import nl.martderoos.trueshuffle.exceptions.InitializationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TrueShuffleClientTest {
    // because the client has to have a valid connection with Spotify, there is no way to test the client further
    @Test
    public void testIllegalArguments() {
        assertThrows(NullPointerException.class, () -> new TrueShuffleClient("cid", "secret", null));
        assertThrows(NullPointerException.class, () -> new TrueShuffleClient("cid", null, "uri"));
        assertThrows(NullPointerException.class, () -> new TrueShuffleClient(null, "secret", "uri"));
    }

    @Test
    public void testInitializationThrowingOnErrors() {
        var client = new TrueShuffleClient("cid", "secret", "uri");
        assertThrows(InitializationException.class, client::initialize);
    }
}
