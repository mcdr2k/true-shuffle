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

    public UserLibrary getUserLibrary() {
        return userLibrary;
    }

    public String getUserId() {
        return userId;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public CountryCode getCountry() {
        return country;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public Image[] getImages() {
        return images;
    }

    public ShuffleApi getApi() {
        return api;
    }

    void assignCredentials(AuthorizationCodeCredentials credentials) {
        api.assignCredentials(credentials);
    }
}
