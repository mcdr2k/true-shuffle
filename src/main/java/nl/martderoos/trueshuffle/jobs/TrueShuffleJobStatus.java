package nl.martderoos.trueshuffle.jobs;

public interface TrueShuffleJobStatus {
    /**
     * Get the status of this job.
     *
     * @return the status, never null.
     */
    ETrueShuffleJobStatus getStatus();

    /**
     * Get details about the source playlist. That is, the playlist which is used as a reference for the tracks that
     * should be in the target playlist. Note that {@link TrueShuffleJobPlaylistData} is immutable, so changes to
     * the source playlist can only be communicated though polling this function. The source playlist is always null
     * when shuffling liked songs.
     *
     * @return the source playlist's details or null if source playlist data is not available yet.
     */
    TrueShuffleJobPlaylistData getSourcePlaylist();

    /**
     * Get details about the target playlist. That is, the playlist which will contain the exact same tracks as the
     * source playlist which is then shuffled randomly. Note that {@link TrueShuffleJobPlaylistData} is immutable,
     * so changes to the target playlist can only be communicated though polling this function.
     *
     * @return the target playlist's details or null if target playlist data is not available yet.
     */
    TrueShuffleJobPlaylistData getTargetPlaylist();

    /**
     * Get a descriptive message tied to the status of the job. The message is usually null in the case that the job
     * finishes appropriately.
     *
     * @return the message, possibly null.
     */
    String getMessage();

    /**
     * Get the failure that caused this execution to terminate or be skipped.
     *
     * @return the original failure, or null when no failure was recorded.
     */
    Throwable getFailure();
}
