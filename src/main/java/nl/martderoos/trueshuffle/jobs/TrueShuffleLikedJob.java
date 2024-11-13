package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.TrueShuffleUser;
import nl.martderoos.trueshuffle.model.ShufflePlaylist;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import nl.martderoos.trueshuffle.utility.ShuffleUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static nl.martderoos.trueshuffle.jobs.TrueShuffleJobPlaylistData.newLikedSongsData;
import static nl.martderoos.trueshuffle.jobs.TrueShuffleJobPlaylistData.newPlaylistData;

/**
 * Dedicated thread-safe and immutable class that contains the data required for shuffling any user's liked songs.
 * An instance of this class may be saved persistently and may be executed repeatedly on a schedule. This class can only
 * shuffle a user's liked songs. If you wish to shuffle an actual playlist then you need to use {@link TrueShufflePlaylistJob}.
 */
public final class TrueShuffleLikedJob extends TrueShuffleJob {
    private static final Logger LOGGER = LogManager.getLogger(TrueShuffleLikedJob.class);
    private final String targetPlaylistId;

    /**
     * @param userId the user identifier for which we will shuffle their liked songs (non-nullable).
     * @throws NullPointerException if userId is null.
     */
    public TrueShuffleLikedJob(String userId) {
        this(userId, null);
    }

    /**
     * @param userId           the user identifier for which we will shuffle their liked songs (non-nullable).
     * @param targetPlaylistId the unique playlist identifier which will be the target playlist for the shuffle (nullable).
     *                         Note that the user must be the owner of this target playlist for this to work. If this
     *                         argument is null, then the actual target playlist used is dynamic and may change based
     *                         on certain conditions.
     * @throws NullPointerException if userId is null.
     */
    public TrueShuffleLikedJob(String userId, String targetPlaylistId) {
        super(userId);
        this.targetPlaylistId = targetPlaylistId;
    }

    @Override
    protected void internalExecute(TrueShuffleUser user, TrueShuffleJobStatus status) throws FatalRequestResponseException {
        LOGGER.info("Executing liked songs shuffle for user: {} with target playlist id: {}", getUserId(), targetPlaylistId);
        var api = user.getApi();
        var library = user.getUserLibrary();

        status.setSourcePlaylist(newLikedSongsData(LIKED_SONGS_TRUE_SHUFFLE));

        ShufflePlaylist target;
        if (targetPlaylistId != null) {
            target = library.getPlaylistById(targetPlaylistId);
            if (!user.getUserLibrary().isOwner(target)) {
                status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED,
                        String.format("Could not shuffle liked songs into %s because we are not the owner of the target playlist", target.getName())
                );
                return;
            }
        } else {
            target = findOrCreateUniqueUserOwnedPlaylistByName(
                    library,
                    status,
                    LIKED_SONGS_TRUE_SHUFFLE,
                    "Liked Songs shuffled by TrueShuffle"
            );

            if (target == null)
                return;
        }

        status.setTargetPlaylist(newPlaylistData(target.getPlaylistId(), target.getName(), target.getImages()));
        ShuffleUtil.shuffleInto(api, target, library.getUserLikedTracksUris());
        status.setTargetPlaylist(newPlaylistData(target.getPlaylistId(), target.getName(), target.getImages()));
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
