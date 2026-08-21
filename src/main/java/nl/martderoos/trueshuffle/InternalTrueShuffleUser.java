package nl.martderoos.trueshuffle;

import com.neovisionaries.i18n.CountryCode;
import nl.martderoos.trueshuffle.jobs.TrueShuffleLikedJob;
import nl.martderoos.trueshuffle.jobs.TrueShufflePlaylistJob;
import nl.martderoos.trueshuffle.model.TrueShuffleApi;
import nl.martderoos.trueshuffle.model.TrueShuffleImage;
import nl.martderoos.trueshuffle.model.TrueShuffleUserLibrary;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Class that encapsulates a Spotify user and provides raw TrueShuffle functionalities hidden behind
 * {@link #getApi()} and {@link #getUserLibrary()}. Note that TrueShuffle jobs ({@link TrueShuffleLikedJob} and
 * {@link TrueShufflePlaylistJob}) glue multiple operations across playlists together as if they were a single operation.
 */
public class InternalTrueShuffleUser implements TrueShuffleUser {
    private final TrueShuffleApi api;
    private final TrueShuffleUserLibrary userLibrary;

    private final String userId;
    private final String birthdate;
    private final String displayName;
    private final String email;
    private final List<TrueShuffleImage> images;

    public InternalTrueShuffleUser(User user, TrueShuffleApi api) {
        this.api = Objects.requireNonNull(api);

        Objects.requireNonNull(user);
        this.userId = user.getId();
        this.birthdate = user.getBirthdate();
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.images = Arrays.stream(Objects.requireNonNullElse(user.getImages(), new Image[0]))
                .map(image -> image == null ? null : convert(image))
                .toList();

        this.userLibrary = new TrueShuffleUserLibrary(api);
    }

    /**
     * Get the user's library.
     */
    public TrueShuffleUserLibrary getUserLibrary() {
        return userLibrary;
    }

    /**
     * Get the user's unique identifier.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Get the user's birthdate.
     */
    public String getBirthdate() {
        return birthdate;
    }

    /**
     * Get the user's (non-unique) display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the user's email. This email address may or may not be verified by Spotify.
     *
     * @return null, TrueShuffle is not authorized to access this information (requires USER_READ_EMAIL).
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get the user's profile image.
     *
     * @return the user's profile images in different resolutions.
     */
    public List<TrueShuffleImage> getImages() {
        return images;
    }

    /**
     * Get the underlying {@link TrueShuffleApi} linked to this user. Can be used.
     *
     * @return the linked api, never null.
     */
    public TrueShuffleApi getApi() {
        return api;
    }

    /**
     * Update credentials of the underlying spotify api.
     */
    void assignCredentials(TrueShuffleUserCredentials credentials) {
        api.assignCredentials(credentials);
    }

    /**
     * Get the current credentials used by the underlying {@link TrueShuffleApi}.
     *
     * @return the credentials, never null.
     */
    public TrueShuffleUserCredentials getCredentials() {
        return api.getCredentials();
    }

    private static TrueShuffleImage convert(Image image) {
        return new TrueShuffleImage(image.getUrl(), image.getWidth(), image.getHeight());
    }
}
