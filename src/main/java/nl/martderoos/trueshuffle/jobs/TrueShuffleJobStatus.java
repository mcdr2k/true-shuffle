package nl.martderoos.trueshuffle.jobs;

/**
 * Thread-safe class describing the state of a {@link TrueShuffleJob job}. This object's fields will be updated
 * throughout the execution of a job.
 */
public class TrueShuffleJobStatus {
    private ETrueShuffleJobStatus status = ETrueShuffleJobStatus.WAITING;
    private String message;
    private TrueShuffleJobPlaylistData sourcePlaylist;
    private TrueShuffleJobPlaylistData targetPlaylist;

    public TrueShuffleJobStatus(ETrueShuffleJobStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Create a new instance with default status {@link ETrueShuffleJobStatus#WAITING} and all other fields null.
     */
    TrueShuffleJobStatus() {

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
     * Set the status and the message of the related job. This operation is grouped because the status is usually tied
     * closely to the message.
     *
     * @param status  the new status.
     * @param message the new message.
     */
    synchronized void setStatusMessage(ETrueShuffleJobStatus status, String message) {
        this.message = message;
        this.status = status;
    }
}
