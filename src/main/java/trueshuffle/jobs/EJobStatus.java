package trueshuffle.jobs;

public enum EJobStatus {
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
     * @return True if the job is still waiting to be executed.
     */
    public boolean isWaiting() {
        return this == WAITING;
    }
    /**
     * @return True if the job is currently being executed but has not yet finished.
     */
    public boolean isRunning() {
        return this == EXECUTING;
    }
    /**
     * @return True if the job has finished executing, which can be either {@link #FINISHED}, {@link #SKIPPED} or
     * {@link #TERMINATED}.
     */
    public boolean isDone() {
        return this == FINISHED || this == SKIPPED || this == TERMINATED;
    }
}
