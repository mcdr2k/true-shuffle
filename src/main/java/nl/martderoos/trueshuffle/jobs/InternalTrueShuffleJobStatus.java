package nl.martderoos.trueshuffle.jobs;

import java.util.Objects;

/**
 * Thread-safe class describing the state of a {@link InternalTrueShuffleJobExecution job execution}. This object's fields will
 * be updated throughout the execution.
 */
public class InternalTrueShuffleJobStatus implements TrueShuffleJobStatus {
    private ETrueShuffleJobStatus status;
    private String message;
    private Throwable failure;
    private TrueShuffleJobPlaylistData sourcePlaylist;
    private TrueShuffleJobPlaylistData targetPlaylist;

    public InternalTrueShuffleJobStatus(ETrueShuffleJobStatus status, String message) {
        this.status = Objects.requireNonNull(status);
        this.message = message;
    }

    public synchronized ETrueShuffleJobStatus getStatus() {
        return status;
    }

    public synchronized TrueShuffleJobPlaylistData getSourcePlaylist() {
        return sourcePlaylist;
    }

    synchronized void setSourcePlaylist(TrueShuffleJobPlaylistData sourcePlaylist) {
        this.sourcePlaylist = sourcePlaylist;
    }

    public synchronized TrueShuffleJobPlaylistData getTargetPlaylist() {
        return targetPlaylist;
    }

    synchronized void setTargetPlaylist(TrueShuffleJobPlaylistData targetPlaylist) {
        this.targetPlaylist = targetPlaylist;
    }

    public synchronized String getMessage() {
        return message;
    }

    public synchronized Throwable getFailure() {
        return failure;
    }

    /**
     * Set the status and the message of the related job. This operation is grouped because the status is usually tied
     * closely to the message. This method will not update the status or message if the job is considered {@link ETrueShuffleJobStatus#isFinished() finished}.
     */
    synchronized void setStatusMessage(ETrueShuffleJobStatus status, String message, Throwable failure) {
        if (this.status.isFinished())
            return;
        this.message = message;
        this.status = status;
        this.failure = failure;
    }

    void setStatusMessage(ETrueShuffleJobStatus status, String message) {
        setStatusMessage(status, message, null);
    }
}
