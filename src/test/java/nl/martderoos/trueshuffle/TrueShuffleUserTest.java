package nl.martderoos.trueshuffle;

import com.neovisionaries.i18n.CountryCode;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TrueShuffleUserTest {
    @Test
    public void testSimpleGetters() {
        var user = new User.Builder()
                .setId("uid")
                .setDisplayName("u-name")
                .setBirthdate("x/y/z")
                .setEmail("a@b.c")
                .setImages(new Image[5])
                .setCountry(CountryCode.NL)
                .build();
        var api = mock(ShuffleApi.class);
        var trueShuffleUser = new TrueShuffleUser(user, api);

        assertEquals("uid", trueShuffleUser.getUserId());
        assertEquals("u-name", trueShuffleUser.getDisplayName());
        assertEquals("x/y/z", trueShuffleUser.getBirthdate());
        assertEquals("a@b.c", trueShuffleUser.getEmail());
        assertArrayEquals(new Image[5], trueShuffleUser.getImages());
        assertEquals(CountryCode.NL, trueShuffleUser.getCountry());
        assertEquals(api, trueShuffleUser.getApi());
    }

    @Test
    public void testCredentials() {
        var user = new User.Builder().setId("uid").build();
        var api = mock(ShuffleApi.class);
        var trueShuffleUser = new TrueShuffleUser(user, api);

        var currentCredentials = trueShuffleUser.getCredentials();
        var credentials = new TrueShuffleUserCredentials(10, "access", "refresh", 4);
        trueShuffleUser.assignCredentials(credentials);

        assertTrue(credentials.isMoreRecentThan(currentCredentials));
        verify(api, times(1)).getCredentials();
        verify(api).assignCredentials(eq(credentials));
    }
}
