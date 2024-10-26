package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.TrueShuffleUser;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.model.ShuffleApi;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.UserLibrary;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import nl.martderoos.trueshuffle.utility.ShuffleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Executor;

import static nl.martderoos.trueshuffle.jobs.TrueShuffleJobPlaylistData.newPlaylistData;

/**
 * Thread-safe and immutable sealed base class for TrueShuffle-like jobs.
 *
 * @see TrueShuffleLikedJob
 * @see TrueShufflePlaylistJob
 */
public abstract sealed class TrueShuffleJob permits TrueShuffleLikedJob, TrueShufflePlaylistJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrueShuffleJob.class);
    public static final String TRUE_SHUFFLE_SUFFIX = " - TrueShuffle";
    public static final String LIKED_SONGS_TRUE_SHUFFLE = "Liked Songs" + TRUE_SHUFFLE_SUFFIX;

    private final String userId;

    TrueShuffleJob(String userId) {
        this.userId = Objects.requireNonNull(userId);
    }

    /**
     * Executes this job, following the provided executor's schedule.
     *
     * @param executor The execution schedule.
     * @return The status of this job, which is updated continuously throughout the execution of this job.
     */
    public final TrueShuffleJobStatus execute(TrueShuffleUserResolver resolver, Executor executor) {
        var status = new TrueShuffleJobStatus();
        executor.execute(() -> this.execute(resolver, status));
        return status;
    }

    private void execute(TrueShuffleUserResolver resolver, TrueShuffleJobStatus status) {
        final var jobName = getClass().getSimpleName() + "-" + userId;
        TrueShuffleUser user;
        try {
            user = resolver.resolve(userId);
        } catch (UserNotFoundException e) {
            LOGGER.info("Skipped {} because we could not find the specified user: {}", jobName, e.getMessage());
            status.setStatusMessage(ETrueShuffleJobStatus.SKIPPED, e.getMessage());
            return;
        }
        status.setStatusMessage(ETrueShuffleJobStatus.EXECUTING, null);
        try {
            internalExecute(user, status);
            if (status.getStatus() == ETrueShuffleJobStatus.EXECUTING) {
                status.setStatusMessage(ETrueShuffleJobStatus.FINISHED, null);
                LOGGER.info("{} completed appropriately", jobName);
            } else if (status.getStatus() != ETrueShuffleJobStatus.FINISHED) {
                LOGGER.info("{} completed with status {} and message {}: ", jobName, status.getStatus(), status.getMessage());
            }
        } catch (FatalRequestResponseException e) {
            var message = jobName + " could not complete: " + e.getMessage();
            LOGGER.error(message);
            status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED, message);
        }
    }

    /**
     * Abstract method for subclasses to implement their logic.
     *
     * @param user   the user for which to execute the job, never null.
     * @param status the status that may be updated continuously throughout the job, never null.
     */
    protected abstract void internalExecute(TrueShuffleUser user, TrueShuffleJobStatus status) throws FatalRequestResponseException;

    /**
     * Attempts to find or create a playlist with the given name. If 2 or more playlists already exist with the provided
     * name, then this function will update the job status to being {@link ETrueShuffleJobStatus#SKIPPED}. If exactly 1 playlist
     * exists with the provided name, then it will be returned. Otherwise, a new playlist is created with such a name
     * and returned.
     *
     * @param library     the library to use for search.
     * @param status      the status to update continuously.
     * @param name        the exact name of the playlist to search for.
     * @param description the description of the returned playlist in the case that we create a new one.
     * @return null if the provided name is not unique for a user's playlists.
     */
    protected static ShufflePlaylist findOrCreateUniquePlaylistByName(UserLibrary library, TrueShuffleJobStatus status, String name, String description) throws FatalRequestResponseException {
        var list = library.getPlaylistByName(name, true);
        if (list == null || list.isEmpty()) {
            return library.createPlaylist(name, description);
        }

        if (list.size() == 1) {
            return list.get(0);
        }

        status.setStatusMessage(ETrueShuffleJobStatus.SKIPPED, String.format("Multiple playlists exist already with the name '%s'", name));
        return null;
    }

    /**
     * Shuffles a playlist in-place, updating the status continuously. If the provided user is not the owner of the
     * provided source playlist, then this method will update the status and return early.
     *
     * @param status the status to update continuously.
     * @param source the playlist to shuffle in-place.
     */
    protected static void shuffleInPlace(TrueShuffleUser user, TrueShuffleJobStatus status, ShufflePlaylist source) throws FatalRequestResponseException {
        var library = user.getUserLibrary();
        if (!library.isOwner(source)) {
            status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED,
                    String.format("Could not shuffle playlist %s (%s) in-place because we are not the owner of the playlist", source.getName(), source.getPlaylistId())
            );
            return;
        }
        status.setSourcePlaylist(newPlaylistData(source.getPlaylistId(), source.getName(), source.getImages()));
        status.setTargetPlaylist(newPlaylistData(source.getPlaylistId(), source.getName(), source.getImages()));

        source.shuffleInPlace();

        status.setTargetPlaylist(newPlaylistData(source.getPlaylistId(), source.getName(), source.getImages()));
    }

    /**
     * Shuffle a playlist by means of shuffle-after-copy. That is, the source playlist's tracks will be copied over to
     * the target playlist's tracks. Once the tracks have been transferred, the target playlist is shuffled in-place.
     * If the target playlist is null, then a new playlist will be created for the user. If the target playlist is not
     * null but the provided user is not the owner of the playlist, then this method will update the status and return early.
     * This operation makes use of {@link ShuffleUtil#shuffleInto(ShuffleApi, ShufflePlaylist, Collection)} to perform
     * the shuffle.
     *
     * @param user   the user to perform the shuffle for.
     * @param status the status to update continuously.
     * @param source the playlist from which we will copy the tracks to the target playlist.
     * @param target the target playlist that will contain the tracks of the source playlist and is then shuffled
     *               afterward (nullable).
     */
    protected static void shuffleAfterCopy(TrueShuffleUser user, TrueShuffleJobStatus status, ShufflePlaylist source, ShufflePlaylist target) throws FatalRequestResponseException {
        String name;
        if (target != null) {
            name = target.getName();
            if (!user.getUserLibrary().isOwner(target)) {
                status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED,
                        String.format("Could not shuffle playlist %s into %s because we are not the owner of the target playlist", source.getName(), target.getName())
                );
                return;
            }
        } else {
            name = source.getName();
            if (!name.endsWith(TRUE_SHUFFLE_SUFFIX))
                name += TRUE_SHUFFLE_SUFFIX;
        }

        LOGGER.info("Copying {} to {} before shuffling", source.getName(), name);
        status.setSourcePlaylist(newPlaylistData(source.getPlaylistId(), source.getName(), source.getImages()));

        if (target == null) {
            target = findOrCreateUniquePlaylistByName(
                    user.getUserLibrary(),
                    status,
                    name,
                    source.getName() + " shuffled by TrueShuffle"
            );
            if (target == null)
                return;
        }

        status.setTargetPlaylist(newPlaylistData(target.getPlaylistId(), target.getName(), target.getImages()));
        ShuffleUtil.shuffleInto(user.getApi(), target, source.getPlaylistTracksUris());
        status.setTargetPlaylist(newPlaylistData(target.getPlaylistId(), target.getName(), target.getImages()));
    }

    /**
     * Get the user identifier for which we will perform this job.
     *
     * @return the user identifier, never null.
     */
    public String getUserId() {
        return userId;
    }
}
