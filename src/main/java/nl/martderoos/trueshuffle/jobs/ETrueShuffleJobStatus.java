package nl.martderoos.trueshuffle.jobs;

/**
 * Enumerates the possible states a {@link TrueShuffleJob} can be in.
 */
public enum ETrueShuffleJobStatus {
    /**
     * Indicates that the job is waiting to be executed
     */
    WAITING,
    /**
     * Indicates that the job is being executed
     */
    EXECUTING,
    /**
     * Indicates that the job finished appropriately
     */
    FINISHED,
    /**
     * Indicates that the job was skipped for some specific reason
     */
    SKIPPED,
    /**
     * Indicates that the job terminated inappropriately
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
     * @return true if the job has finished executing, which can be either {@link #FINISHED}, {@link #SKIPPED} or
     * {@link #TERMINATED}, false otherwise.
     */
    public boolean isDone() {
        return this == FINISHED || this == SKIPPED || this == TERMINATED;
    }
}
