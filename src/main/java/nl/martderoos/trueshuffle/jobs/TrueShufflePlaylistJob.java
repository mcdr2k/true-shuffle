package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.TrueShuffleUser;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Dedicated thread-safe and immutable class that contains the data required for shuffling any user's playlists.
 * It also implements the functionality necessary to do so. An instance of this class may be saved persistently
 * and may be executed repeatedly on a schedule. This class cannot shuffle a user's liked songs, for this you need to
 * use {@link TrueShuffleLikedJob}.
 */
public final class TrueShufflePlaylistJob extends TrueShuffleJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrueShufflePlaylistJob.class);
    private final String sourcePlaylistId;
    private final String targetPlaylistId;

    /**
     * @param userId           the user identifier for which we will perform the shuffle (need not be the owner of the playlist).
     *                         In particular, the user for which the shuffle will be performed will also be the owner of the
     *                         actual shuffled (target) playlist.
     * @param sourcePlaylistId the unique playlist identifier which will be the source playlist for the shuffle.
     * @throws NullPointerException if any argument is null.
     */
    public TrueShufflePlaylistJob(String userId, String sourcePlaylistId) {
        this(userId, sourcePlaylistId, null);
    }

    /**
     * @param userId           the user identifier for which we will perform the shuffle (need not be the owner of the playlist).
     *                         In particular, the user for which the shuffle will be performed will also be the owner of the
     *                         actual shuffled (target) playlist.
     * @param sourcePlaylistId the unique playlist identifier which will be the source playlist for the shuffle.
     * @param targetPlaylistId the unique playlist identifier which will be the target playlist for the shuffle (nullable).
     *                         Note that the user must be the owner of this target playlist for this to work. If this
     *                         argument is null, then the actual target playlist used is dynamic and may change based
     *                         on certain conditions.
     * @throws NullPointerException if either userId or sourcePlaylistId is null.
     */
    public TrueShufflePlaylistJob(String userId, String sourcePlaylistId, String targetPlaylistId) {
        super(userId);
        this.sourcePlaylistId = Objects.requireNonNull(sourcePlaylistId);
        this.targetPlaylistId = targetPlaylistId;
    }

    @Override
    protected void internalExecute(TrueShuffleUser user, TrueShuffleJobStatus status) throws FatalRequestResponseException {
        LOGGER.info("Executing playlist shuffle for user: {} with source playlist id: {} and target playlist id: {}", getUserId(), sourcePlaylistId, targetPlaylistId);
        var library = user.getUserLibrary();
        var sourcePlaylist = library.getPlaylistById(sourcePlaylistId);
        if (sourcePlaylistId.equals(targetPlaylistId)) {
            // source equals target, so shuffle in-place
            shuffleInPlace(user, status, sourcePlaylist);
        } else if (targetPlaylistId != null) {
            // dedicated target playlist, so do a shuffle after copy
            var targetPlaylist = library.getPlaylistById(targetPlaylistId);
            shuffleAfterCopy(user, status, sourcePlaylist, targetPlaylist);
        } else if (library.isOwner(sourcePlaylist)) {
            // if we are the owner, just shuffle in-place
            shuffleInPlace(user, status, sourcePlaylist);
        } else {
            // otherwise do a shuffle after copy where the target playlist will be created on the fly
            shuffleAfterCopy(user, status, sourcePlaylist, null);
        }
    }

    /**
     * Get the unique identifier of the source playlist.
     *
     * @return the source playlist's unique identifier, never null.
     */
    public String getSourcePlaylistId() {
        return sourcePlaylistId;
    }

    /**
     * Get the unique identifier of the target playlist.
     *
     * @return the target playlist's unique identifier, or null if there is no dedicated target playlist.
     */
    public String getTargetPlaylistId() {
        return targetPlaylistId;
    }
}
