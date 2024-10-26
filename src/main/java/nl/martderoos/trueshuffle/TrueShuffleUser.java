package nl.martderoos.trueshuffle;

import com.neovisionaries.i18n.CountryCode;
import nl.martderoos.trueshuffle.jobs.TrueShuffleLikedJob;
import nl.martderoos.trueshuffle.jobs.TrueShufflePlaylistJob;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.model.UserLibrary;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Objects;

/**
 * Class that encapsulates a Spotify user and provides raw TrueShuffle functionalities hidden behind
 * {@link #getApi()} and {@link #getUserLibrary()}. Note that TrueShuffle jobs ({@link TrueShuffleLikedJob} and
 * {@link TrueShufflePlaylistJob}) glue multiple operations across playlists together as if they were a single operation.
 */
public class TrueShuffleUser {
    private final ShuffleApi api;
    private final UserLibrary userLibrary;

    private final String userId;
    private final String birthdate;
    private final CountryCode country;
    private final String displayName;
    private final String email;
    private final Image[] images;

    public TrueShuffleUser(User user, ShuffleApi api) {
        this.api = Objects.requireNonNull(api);

        Objects.requireNonNull(user);
        this.userId = user.getId();
        this.birthdate = user.getBirthdate();
        this.country = user.getCountry();
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.images = user.getImages();

        this.userLibrary = new UserLibrary(api);
    }

    /**
     * Get the user's library
     */
    public UserLibrary getUserLibrary() {
        return userLibrary;
    }

    /**
     * Get the user's unique identifier
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get the user's birthdate
     */
    public String getBirthdate() {
        return birthdate;
    }

    /**
     * Get the user's country code
     *
     * @return null, TrueShuffle is not authorized to access this information (requires USER_READ_PRIVATE)
     */
    public CountryCode getCountry() {
        return country;
    }

    /**
     * Get the user's (non-unique) display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the user's email. This email address may or may not be verified by Spotify.
     *
     * @return null, TrueShuffle is not authorized to access this information (requires USER_READ_EMAIL)
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get the user's profile image
     *
     * @return the user's profile images in different resolutions
     */
    public Image[] getImages() {
        return images;
    }

    /**
     * Get the underlying {@link ShuffleApi} linked to this user. Can be used
     *
     * @return the linked api, never null
     */
    public ShuffleApi getApi() {
        return api;
    }

    /**
     * Update credentials of the underlying spotify api
     */
    void assignCredentials(AuthorizationCodeCredentials credentials) {
        api.assignCredentials(credentials);
    }
}
