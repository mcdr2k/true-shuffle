package nl.martderoos.trueshuffle.api.jobs;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents an attempt to execute a job.
 */
public interface TrueShuffleJobExecution {
    /**
     * @return the unique identifier of this execution.
     */
    UUID getId();

    /**
     * @return the live status of this execution.
     */
    TrueShuffleJobStatus getStatus();

    /**
     * @return true when this execution has reached a terminal state.
     */
    boolean isFinished();

    /**
     * Attempts to cancel this execution.
     *
     * @return true if cancellation was requested successfully.
     */
    boolean cancel();

    /**
     * Waits until this execution reaches a terminal state.
     *
     * @throws InterruptedException if the waiting thread is interrupted.
     */
    void await() throws InterruptedException;

    /**
     * Waits until this execution reaches a terminal state or the timeout expires.
     *
     * @throws InterruptedException if the waiting thread is interrupted.
     * @throws TimeoutException if the timeout expires first.
     */
    void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;
}
