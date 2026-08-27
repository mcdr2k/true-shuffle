package nl.martderoos.trueshuffle.jobs;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents one attempt to execute a {@link TrueShuffleJob}.
 */
public final class InternalTrueShuffleJobExecution implements TrueShuffleJobExecution {
    private final TrueShuffleJob job;
    private final InternalTrueShuffleJobStatus status;
    private final Future<?> future;
    private final UUID id;

    InternalTrueShuffleJobExecution(TrueShuffleJob job, Future<?> future) {
        this.job = Objects.requireNonNull(job);
        this.status = new InternalTrueShuffleJobStatus(ETrueShuffleJobStatus.WAITING, null);
        this.future = Objects.requireNonNull(future);
        this.id = UUID.randomUUID();
    }

    public boolean isFinished() {
        return status.getStatus().isFinished();
    }

    public boolean isCancelled() {
        return status.getStatus() == ETrueShuffleJobStatus.CANCELLED;
    }

    public boolean cancel() {
        if (future.isDone())
            return false;
        if (!future.cancel(true))
            return false;
        status.setStatusMessage(ETrueShuffleJobStatus.CANCELLED, "Execution was cancelled");
        return true;
    }

    public void await() throws InterruptedException {
        try {
            future.get();
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IllegalStateException("Job execution failed unexpectedly", exception.getCause());
        } catch (java.util.concurrent.CancellationException ignored) {
            // Cancellation is reflected by the execution status.
        }
    }

    public void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            future.get(timeout, unit);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Job execution failed unexpectedly", e.getCause());
        } catch (java.util.concurrent.CancellationException ignored) {
            // Cancellation is reflected by the execution status.
        }
    }

    public TrueShuffleJob getJob() {
        return job;
    }

    public UUID getId() {
        return id;
    }

    public InternalTrueShuffleJobStatus getStatus() {
        return status;
    }
}
