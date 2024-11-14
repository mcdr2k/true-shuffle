package nl.martderoos.trueshuffle.model;

import nl.martderoos.trueshuffle.TrueShuffleUserCredentials;
import org.junit.jupiter.api.Test;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ShuffleApiTest {
    @Test
    public void testInitialCredentials() {
        var spotifyApi = mock(SpotifyApi.class);
        when(spotifyApi.getAccessToken()).thenReturn("a-token");
        when(spotifyApi.getRefreshToken()).thenReturn("r-token");
        var api = new ShuffleApi(spotifyApi, createUser());

        assertEquals("a-token", api.getAccessToken());
        assertEquals("r-token", api.getRefreshToken());

        var credentials = api.getCredentials();
        assertEquals("a-token", credentials.accessToken());
        assertEquals("r-token", credentials.refreshToken());
    }

    @Test
    public void testAssigningFullCredentials() {
        var spotifyApi = mock(SpotifyApi.class);
        var api = new ShuffleApi(spotifyApi, createUser());

        var credentials = new TrueShuffleUserCredentials(System.currentTimeMillis(), "new-a-token", "new-r-token", 100);
        api.assignCredentials(credentials);

        verify(spotifyApi).setAccessToken("new-a-token");
        verify(spotifyApi).setRefreshToken("new-r-token");
    }

    @Test
    public void testAssigningPartialCredentials() {
        var spotifyApi = mock(SpotifyApi.class);
        var api = new ShuffleApi(spotifyApi, createUser());

        var credentials = new TrueShuffleUserCredentials(System.currentTimeMillis(), "new-a-token", "new-r-token", 50);
        api.assignCredentials(credentials);
        var credentials2 = new TrueShuffleUserCredentials(credentials.issuedSinceEpoch() + 1, "new-a-token2", null, 60);
        api.assignCredentials(credentials2);
        var uselessCredentials = new TrueShuffleUserCredentials(credentials.issuedSinceEpoch() + 2, null, null, 70);
        api.assignCredentials(uselessCredentials);

        var result = api.getCredentials();
        assertEquals(credentials2.issuedSinceEpoch(), result.issuedSinceEpoch());
        assertEquals(60, result.expiresInSeconds());

        verify(spotifyApi).setAccessToken("new-a-token");
        verify(spotifyApi).setAccessToken("new-a-token2");
        verify(spotifyApi, times(2)).setAccessToken(any());

        verify(spotifyApi).setRefreshToken("new-r-token");
        verify(spotifyApi, times(1)).setRefreshToken(any());
    }

    @Test
    public void testAssigningExpiredCredentials() {
        var spotifyApi = mock(SpotifyApi.class);
        var api = new ShuffleApi(spotifyApi, createUser());

        var credentials = new TrueShuffleUserCredentials(System.currentTimeMillis(), "a-token1", "r-token1", 0);
        api.assignCredentials(credentials);
        var expiredCredentials = new TrueShuffleUserCredentials(0, "a-token2", "r-token2", 0);
        api.assignCredentials(expiredCredentials);

        verify(spotifyApi, times(1)).setAccessToken(any());
        verify(spotifyApi, times(1)).setRefreshToken(any());
    }

    private User createUser() {
        return new User.Builder()
                .setId("uid")
                .setDisplayName("uname")
                .build();
    }
}
