package nl.martderoos.trueshuffle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrueShuffleUserCredentialsTest {
    @Test
    public void testIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TrueShuffleUserCredentials(0, "a", "r", -1);
        });
        assertDoesNotThrow(() -> new TrueShuffleUserCredentials(0, "a", "r", 0));
    }

    @SuppressWarnings("ConstantValue")
    @Test
    public void isMoreRecentThanTest() {
        var credentials = new TrueShuffleUserCredentials(100, "a", "r", 0);
        assertFalse(credentials.isMoreRecentThan(credentials));

        var newerCredentials = new TrueShuffleUserCredentials(900, "a", "r", 0);
        assertFalse(credentials.isMoreRecentThan(newerCredentials));
        assertTrue(newerCredentials.isMoreRecentThan(credentials));

        var olderCredentials = new TrueShuffleUserCredentials(0, "a", "r", 0);
        assertTrue(credentials.isMoreRecentThan(olderCredentials));
        assertFalse(olderCredentials.isMoreRecentThan(credentials));

        assertTrue(credentials.isMoreRecentThan(null));
        assertTrue(newerCredentials.isMoreRecentThan(null));
        assertTrue(olderCredentials.isMoreRecentThan(null));
    }
}
