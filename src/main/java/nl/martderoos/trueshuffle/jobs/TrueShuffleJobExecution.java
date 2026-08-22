package nl.martderoos.trueshuffle.jobs;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents one attempt to execute a {@link TrueShuffleJob}.
 */
public final class TrueShuffleJobExecution {
    private final TrueShuffleJob job;
    private final TrueShuffleJobStatus status;
    private final Future<?> future;
    private final UUID id;

    TrueShuffleJobExecution(TrueShuffleJob job, Future<?> future) {
        this.job = Objects.requireNonNull(job);
        this.status = new TrueShuffleJobStatus(ETrueShuffleJobStatus.WAITING, null);
        this.future = Objects.requireNonNull(future);
        this.id = UUID.randomUUID();
    }

    /**
     * @return the immutable job description being executed.
     */
    public TrueShuffleJob getJob() {
        return job;
    }

    /**
     * @return the live status of this execution.
     */
    public TrueShuffleJobStatus getStatus() {
        return status;
    }

    /**
     * @return true when this execution has reached a terminal state.
     */
    public boolean isDone() {
        return status.getStatus().isDone();
    }

    /**
     * @return true when cancellation was requested successfully.
     */
    public boolean isCancelled() {
        return status.getStatus() == ETrueShuffleJobStatus.CANCELLED;
    }

    /**
     * Attempts to cancel this execution.
     *
     * @return true if cancellation was requested successfully.
     */
    public boolean cancel() {
        if (future.isDone())
            return false;
        if (!future.cancel(true))
            return false;
        status.setStatusMessage(ETrueShuffleJobStatus.CANCELLED, "Execution was cancelled");
        return true;
    }

    /**
     * Waits until this execution reaches a terminal state.
     *
     * @throws InterruptedException if the waiting thread is interrupted.
     */
    public void await() throws InterruptedException {
        try {
            future.get();
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IllegalStateException("Job execution failed unexpectedly", exception.getCause());
        } catch (java.util.concurrent.CancellationException ignored) {
            // Cancellation is reflected by the execution status.
        }
    }

    /**
     * Waits until this execution reaches a terminal state or the timeout expires.
     *
     * @throws InterruptedException if the waiting thread is interrupted.
     * @throws TimeoutException if the timeout expires first.
     */
    public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            future.get(timeout, unit);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Job execution failed unexpectedly", e.getCause());
        } catch (java.util.concurrent.CancellationException ignored) {
            // Cancellation is reflected by the execution status.
        }
    }

    /**
     * @return the unique identifier of this execution.
     */
    public UUID getId() {
        return id;
    }
}
