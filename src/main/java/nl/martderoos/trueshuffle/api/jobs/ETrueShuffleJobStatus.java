package nl.martderoos.trueshuffle.api.jobs;

/**
 * Enumerates the possible states a {@link TrueShuffleJobExecution} can be in.
 */
public enum ETrueShuffleJobStatus {
    /**
     * Indicates that the job is waiting to be executed.
     */
    WAITING,
    /**
     * Indicates that the job is being executed.
     */
    EXECUTING,
    /**
     * Indicates that the job completed fully.
     */
    COMPLETED,
    /**
     * Indicates that the job was skipped for some specific reason.
     */
    SKIPPED,
    /**
     * Indicates that execution was cancelled before it completed.
     */
    CANCELLED,
    /**
     * Indicates that the job terminated inappropriately.
     */
    TERMINATED;

    /**
     * @return true if the job is still waiting to be executed, false otherwise.
     */
    public boolean isWaiting() {
        return this == WAITING;
    }

    /**
     * @return true if the job is currently being executed but has not yet finished, false otherwise.
     */
    public boolean isRunning() {
        return this == EXECUTING;
    }

    /**
     * @return true if the job has finished executing, which can be either {@link #COMPLETED}, {@link #SKIPPED},
     * {@link #CANCELLED} or {@link #TERMINATED}, false otherwise.
     */
    public boolean isFinished() {
        return this == COMPLETED || this == SKIPPED || this == CANCELLED || this == TERMINATED;
    }
}
