package nl.martderoos.trueshuffle.api;

import java.util.List;

public interface TrueShuffleUser {
    /**
     * Get the user's library.
     */
    TrueShuffleUserLibrary getUserLibrary();

    /**
     * Get the user's unique identifier.
     */
    String getUserId();

    /**
     * Get the user's birthdate.
     */
    String getBirthdate();

    /**
     * Get the user's (non-unique) display name.
     */
    String getDisplayName();

    /**
     * Get the user's email. This email address may or may not be verified by Spotify.
     *
     * @return null, TrueShuffle is not authorized to access this information (requires USER_READ_EMAIL).
     */
    String getEmail();

    /**
     * Get the user's profile image.
     *
     * @return the user's profile images in different resolutions.
     */
    List<TrueShuffleImage> getImages();
}
