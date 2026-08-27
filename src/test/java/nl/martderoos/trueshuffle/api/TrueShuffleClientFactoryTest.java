package nl.martderoos.trueshuffle.api;

import nl.martderoos.trueshuffle.api.model.TrueShuffleClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TrueShuffleClientFactoryTest {
    @Test
    public void testCreate() {
        TrueShuffleClient client = TrueShuffleClientFactory.create("cid", "secret", "http://localhost/callback");

        assertNotNull(client);
    }

    @Test
    public void testCreateRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> TrueShuffleClientFactory.create(null, "secret", "http://localhost/callback"));
        assertThrows(NullPointerException.class,
                () -> TrueShuffleClientFactory.create("cid", null, "http://localhost/callback"));
        assertThrows(NullPointerException.class,
                () -> TrueShuffleClientFactory.create("cid", "secret", null));
    }
}
