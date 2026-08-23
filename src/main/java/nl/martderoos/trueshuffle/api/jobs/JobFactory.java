package nl.martderoos.trueshuffle.api.jobs;

import nl.martderoos.trueshuffle.internal.jobs.TrueShuffleJob;
import nl.martderoos.trueshuffle.internal.jobs.TrueShuffleLikedJob;
import nl.martderoos.trueshuffle.internal.jobs.TrueShufflePlaylistJob;

/**
 * Factory for creating immutable TrueShuffle jobs.
 */
public final class JobFactory {
    private JobFactory() {
    }

    /**
     * Creates a job that shuffles a user's liked songs.
     *
     * @param userId the user identifier for which the job will run.
     * @return the new liked-songs shuffle job.
     */
    public static TrueShuffleJob createLikedSongsJob(String userId) {
        return new TrueShuffleLikedJob(userId);
    }

    /**
     * Creates a job that shuffles a user's liked songs into a specific target playlist.
     *
     * @param userId           the user identifier for which the job will run.
     * @param targetPlaylistId the target playlist identifier.
     * @return the new liked-songs shuffle job.
     */
    public static TrueShuffleJob createLikedSongsJob(String userId, String targetPlaylistId) {
        return new TrueShuffleLikedJob(userId, targetPlaylistId);
    }

    /**
     * Creates a playlist shuffle job.
     *
     * @param userId           the user identifier for which the job will run.
     * @param sourcePlaylistId the source playlist identifier.
     * @return the new playlist shuffle job.
     */
    public static TrueShuffleJob createPlaylistJob(String userId, String sourcePlaylistId) {
        return new TrueShufflePlaylistJob(userId, sourcePlaylistId);
    }

    /**
     * Creates a playlist shuffle job with a specific target playlist.
     *
     * @param userId           the user identifier for which the job will run.
     * @param sourcePlaylistId the source playlist identifier.
     * @param targetPlaylistId the target playlist identifier.
     * @return the new playlist shuffle job.
     */
    public static TrueShuffleJob createPlaylistJob(String userId, String sourcePlaylistId, String targetPlaylistId) {
        return new TrueShufflePlaylistJob(userId, sourcePlaylistId, targetPlaylistId);
    }
}
