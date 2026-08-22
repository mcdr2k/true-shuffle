package nl.martderoos.trueshuffle.jobs;

import java.util.Objects;

/**
 * Thread-safe class describing the state of a {@link TrueShuffleJobExecution job execution}. This object's fields will
 * be updated throughout the execution.
 */
public class TrueShuffleJobStatus {
    private ETrueShuffleJobStatus status = ETrueShuffleJobStatus.WAITING;
    private String message;
    private Throwable failure;
    private TrueShuffleJobPlaylistData sourcePlaylist;
    private TrueShuffleJobPlaylistData targetPlaylist;

    public TrueShuffleJobStatus(ETrueShuffleJobStatus status, String message) {
        this.status = Objects.requireNonNull(status);
        this.message = message;
    }

    /**
     * Get the status of this job.
     *
     * @return the status, never null.
     */
    public synchronized ETrueShuffleJobStatus getStatus() {
        return status;
    }

    /**
     * Get details about the source playlist. That is, the playlist which is used as a reference for the tracks that
     * should be in the target playlist. Note that {@link TrueShuffleJobPlaylistData} is immutable, so changes to
     * the source playlist can only be communicated though polling this function.
     *
     * @return the source playlist's details or null if source playlist data is not available yet.
     */
    public synchronized TrueShuffleJobPlaylistData getSourcePlaylist() {
        return sourcePlaylist;
    }

    synchronized void setSourcePlaylist(TrueShuffleJobPlaylistData sourcePlaylist) {
        this.sourcePlaylist = sourcePlaylist;
    }

    /**
     * Get details about the target playlist. That is, the playlist which will contain the exact same tracks as the
     * source playlist which is then shuffled randomly. Note that {@link TrueShuffleJobPlaylistData} is immutable,
     * so changes to the target playlist can only be communicated though polling this function.
     *
     * @return the target playlist's details or null if target playlist data is not available yet.
     */
    public synchronized TrueShuffleJobPlaylistData getTargetPlaylist() {
        return targetPlaylist;
    }

    synchronized void setTargetPlaylist(TrueShuffleJobPlaylistData targetPlaylist) {
        this.targetPlaylist = targetPlaylist;
    }

    /**
     * Get a descriptive message tied to the status of the job. The message is usually null in the case that the job
     * finishes appropriately.
     *
     * @return the message, possibly null.
     */
    public synchronized String getMessage() {
        return message;
    }

    /**
     * Get the failure that caused this execution to terminate or be skipped.
     *
     * @return the original failure, or null when no failure was recorded.
     */
    public synchronized Throwable getFailure() {
        return failure;
    }

    /**
     * Set the status and the message of the related job. This operation is grouped because the status is usually tied
     * closely to the message. This method will not update the status or message if the job is considered done following
     * {@link ETrueShuffleJobStatus#isDone()}.
     */
    synchronized void setStatusMessage(ETrueShuffleJobStatus status, String message, Throwable failure) {
        if (this.status.isDone())
            return;
        this.message = message;
        this.status = status;
        this.failure = failure;
    }

    void setStatusMessage(ETrueShuffleJobStatus status, String message) {
        setStatusMessage(status, message, null);
    }
}
