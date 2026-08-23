package nl.martderoos.trueshuffle.internal;

import nl.martderoos.trueshuffle.api.exceptions.InitializationException;
import nl.martderoos.trueshuffle.api.model.TrueShuffleClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class InternalTrueShuffleClientTest {
    // because the client has to have a valid connection with Spotify, there is no way to test the client further
    @Test
    public void testIllegalArguments() {
        assertThrows(NullPointerException.class, () -> TrueShuffleClient.create("cid", "secret", null));
        assertThrows(NullPointerException.class, () -> TrueShuffleClient.create("cid", null, "uri"));
        assertThrows(NullPointerException.class, () -> TrueShuffleClient.create(null, "secret", "uri"));
    }

    @Test
    public void testInitializationThrowingOnErrors() {
        var client = TrueShuffleClient.create("cid", "secret", "uri");
        assertThrows(InitializationException.class, client::initialize);
    }
}
