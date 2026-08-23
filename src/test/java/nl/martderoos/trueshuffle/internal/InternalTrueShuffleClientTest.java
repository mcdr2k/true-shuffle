package nl.martderoos.trueshuffle.internal;

import nl.martderoos.trueshuffle.api.exceptions.InitializationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class InternalTrueShuffleClientTest {
    // because the client has to have a valid connection with Spotify, there is no way to test the client further
    @Test
    public void testIllegalArguments() {
        assertThrows(NullPointerException.class, () -> new InternalTrueShuffleClient("cid", "secret", null));
        assertThrows(NullPointerException.class, () -> new InternalTrueShuffleClient("cid", null, "uri"));
        assertThrows(NullPointerException.class, () -> new InternalTrueShuffleClient(null, "secret", "uri"));
    }

    @Test
    public void testInitializationThrowingOnErrors() {
        var client = new InternalTrueShuffleClient("cid", "secret", "uri");
        assertThrows(InitializationException.class, client::initialize);
    }
}
