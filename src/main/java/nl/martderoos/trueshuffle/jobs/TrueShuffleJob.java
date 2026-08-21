package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.InternalTrueShuffleUser;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.model.TrueShuffleApi;
import nl.martderoos.trueshuffle.model.TrueShufflePlaylist;
import nl.martderoos.trueshuffle.model.TrueShuffleUserLibrary;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import nl.martderoos.trueshuffle.utility.ShuffleUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Objects;

import static nl.martderoos.trueshuffle.jobs.TrueShuffleJobPlaylistData.newPlaylistData;

/**
 * Thread-safe and immutable sealed base class for TrueShuffle-like job descriptions.
 *
 * @see TrueShuffleLikedJob
 * @see TrueShufflePlaylistJob
 */
public abstract sealed class TrueShuffleJob permits TrueShuffleLikedJob, TrueShufflePlaylistJob {
    private static final Logger LOGGER = LogManager.getLogger(TrueShuffleJob.class);
    public static final String TRUE_SHUFFLE_SUFFIX = " - TrueShuffle";
    public static final String LIKED_SONGS_TRUE_SHUFFLE = "Liked Songs" + TRUE_SHUFFLE_SUFFIX;

    private final String userId;

    TrueShuffleJob(String userId) {
        this.userId = Objects.requireNonNull(userId);
    }

    /**
     * Performs this job's operation.
     *
     * @param user   the user for which to execute the job, never null.
     * @param status the status that may be updated continuously throughout the job, never null.
     */
    abstract void perform(InternalTrueShuffleUser user, TrueShuffleJobStatus status) throws FatalRequestResponseException;

    /**
     * Attempts to find a user owned playlist with the given name or create a new one if it does not exist. If 2 or more
     * playlists already exist with the provided name, then this function will update the job status to being
     * {@link ETrueShuffleJobStatus#SKIPPED}. If exactly 1 playlist exists with the provided name, then it will be
     * returned. Otherwise, a new playlist is created with such a name and returned.
     *
     * @param library     the library to use for search.
     * @param status      the status to update continuously.
     * @param name        the exact name of the playlist to search for.
     * @param description the description of the returned playlist in the case that we create a new one.
     * @return null if the provided name is not unique for a user's playlists.
     */
    protected static ShufflePlaylist findOrCreateUniqueUserOwnedPlaylistByName(TrueShuffleUserLibrary library, TrueShuffleJobStatus status, String name, String description) throws FatalRequestResponseException {
        var list = library.getPlaylistByName(name, true);
        if (list == null || list.isEmpty()) {
            return asInternal(library.createPlaylist(name, description));
        }

        if (list.size() == 1) {
            return asInternal(list.get(0));
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
    protected static void shuffleInPlace(InternalTrueShuffleUser user, TrueShuffleJobStatus status, ShufflePlaylist source) throws FatalRequestResponseException {
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
     * This operation makes use of {@link ShuffleUtil#shuffleInto(TrueShuffleApi, ShufflePlaylist, Collection)} to perform
     * the shuffle.
     *
     * @param user   the user to perform the shuffle for.
     * @param status the status to update continuously.
     * @param source the playlist from which we will copy the tracks to the target playlist.
     * @param target the target playlist that will contain the tracks of the source playlist and is then shuffled
     *               afterward (nullable).
     */
    protected static void shuffleAfterCopy(InternalTrueShuffleUser user, TrueShuffleJobStatus status, ShufflePlaylist source, ShufflePlaylist target) throws FatalRequestResponseException {
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
            target = findOrCreateUniqueUserOwnedPlaylistByName(
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

    private static ShufflePlaylist asInternal(TrueShufflePlaylist playlist) {
        if (!(playlist instanceof ShufflePlaylist internalPlaylist)) {
            throw new IllegalArgumentException("TrueShufflePlaylist must be managed by TrueShuffle");
        }
        return internalPlaylist;
    }
}
