package nl.martderoos.trueshuffle;

import com.neovisionaries.i18n.CountryCode;
import nl.martderoos.trueshuffle.jobs.ShuffleJobStatus;
import nl.martderoos.trueshuffle.jobs.ShuffleLikedJob;
import nl.martderoos.trueshuffle.jobs.ShufflePlaylistJob;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.UserLibrary;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Image;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * todo: re-document
 * Class that encapsulates a Spotify user and provides TrueShuffle functionalities.
 * Note that the underlying api used by this instance is synchronized such that no more than 1 thread can actually
 * modify the user's data at the same time. This means that it may take quite some time before control is returned
 * when using {@link UserLibrary} or {@link ShufflePlaylist} directly.
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
     * Perform a shuffle on the provided playlist for this user, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param executor The execution schedule (should be an asynchronous schedule).
     * @return The status of the shuffle job, which is updated continuously until it has finished.
     */
    public ShuffleJobStatus shuffleLikedSongs(Executor executor) {
        var job = new ShuffleLikedJob(this);
        return job.execute(Objects.requireNonNull(executor));
    }

    /**
     * Perform a shuffle on the provided playlist for this user, following the provided executor's schedule.
     * If one wishes to monitor the status of this job, an asynchronous executor must be provided. Otherwise, this function,
     * will not return until it has completed execution.
     *
     * @param playlistId The id of the playlist to shuffle.
     * @param executor   The execution schedule (should be an asynchronous schedule).
     * @return The status of the shuffle job, which is updated continuously until it has finished.
     */
    public ShuffleJobStatus shufflePlaylist(String playlistId, Executor executor) {
        var job = new ShufflePlaylistJob(this, playlistId);
        return job.execute(Objects.requireNonNull(executor));
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
     * @return null, TrueShuffle is not authorized to access this information (requires USER_READ_EMAIL)
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get the user's profile image
     * @return the user's profile images in different resolutions
     */
    public Image[] getImages() {
        return images;
    }

    /**
     * Get the underlying {@link ShuffleApi} linked to this user. Can be used
     * @return the linked api, never null
     */
    public ShuffleApi getApi() {
        return api;
    }

    /**
     * Update credentials up the underlying spotify api
     * @param credentials
     */
    void assignCredentials(AuthorizationCodeCredentials credentials) {
        api.assignCredentials(credentials);
    }
}
