package nl.martderoos.trueshuffle.internal;

import nl.martderoos.trueshuffle.api.exceptions.InitializationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class InternalTrueShuffleClientTest {
    @Test
    public void testIllegalArguments() {
        assertThrows(NullPointerException.class, () -> InternalTrueShuffleClient.create("cid", "secret", null));
        assertThrows(NullPointerException.class, () -> InternalTrueShuffleClient.create("cid", null, "uri"));
        assertThrows(NullPointerException.class, () -> InternalTrueShuffleClient.create(null, "secret", "uri"));
    }

    @Test
    public void testInitializationThrowingOnErrors() {
        var client = InternalTrueShuffleClient.create("cid", "secret", "uri");
        assertThrows(InitializationException.class, client::initialize);
    }
}
