package nl.martderoos.trueshuffle;

import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

/**
 * Class encapsulating credentials of a TrueShuffle user that is used for leveraging the
 * {@link se.michaelthelin.spotify.SpotifyApi SpotifyApi}.
 *
 * @param issuedSinceEpoch the time in milliseconds (offset from UNIX epoch) at which the access token was issued. Use
 *                         {@link System#currentTimeMillis()} to get the current time in millis since the UNIX epoch.
 * @param accessToken      the current access token of the user, possibly null. There is no guarantee that
 *                         the access token is still valid.
 * @param refreshToken     the current refresh token of the user, possibly null. There is no guarantee that
 *                         the refresh token is still valid.
 * @param expiresInSeconds the amount of seconds the access token is valid for from the moment it was issued, at least 0.
 */
public record TrueShuffleUserCredentials(
        long issuedSinceEpoch,
        String accessToken,
        String refreshToken,
        int expiresInSeconds
) {
    /**
     * @throws IllegalArgumentException when expiresInSeconds is less than 0.
     */
    public TrueShuffleUserCredentials(long issuedSinceEpoch, String accessToken, String refreshToken, int expiresInSeconds) {
        this.issuedSinceEpoch = issuedSinceEpoch;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresInSeconds = expiresInSeconds;
        if (expiresInSeconds < 0) throw new IllegalArgumentException("expiresInSeconds must be at least 0");
    }

    /**
     * @param credentials the credentials that contains the access token, refresh token and expires in seconds.
     */
    public TrueShuffleUserCredentials(long issuedSinceEpoch, AuthorizationCodeCredentials credentials) {
        this(issuedSinceEpoch, credentials.getAccessToken(), credentials.getRefreshToken(), credentials.getExpiresIn());
    }

    /**
     * Test whether this instance's credentials were issued later than the provided credentials.
     *
     * @param other the other credentials to compare issuance time with.
     * @return true if this instance's credentials have been issued more recently than other, false otherwise.
     */
    public boolean isMoreRecentThan(TrueShuffleUserCredentials other) {
        if (other == null) return true;
        return this.issuedSinceEpoch > other.issuedSinceEpoch;
    }
}