package nl.martderoos.trueshuffle.internal.jobs;

import nl.martderoos.trueshuffle.internal.InternalTrueShuffleUser;
import nl.martderoos.trueshuffle.api.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.api.jobs.ETrueShuffleJobStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Schedules and manages executions of immutable shuffle job descriptions.
 */
public final class TrueShuffleJobExecutor {
    private static final Logger LOGGER = LogManager.getLogger(TrueShuffleJobExecutor.class);

    /**
     * Executes a job using the supplied user resolver and executor.
     *
     * @param job      the immutable job description.
     * @param resolver the user resolver.
     * @param executor the execution schedule.
     * @return the execution handle.
     */
    public TrueShuffleJobExecution execute(
            TrueShuffleJob job,
            TrueShuffleUserResolver resolver,
            Executor executor
    ) {
        Objects.requireNonNull(job);
        Objects.requireNonNull(resolver);
        Objects.requireNonNull(executor);

        var executionReference = new AtomicReference<TrueShuffleJobExecution>();
        var task = new FutureTask<Void>(() -> {
            run(job, resolver, executionReference.get());
            return null;
        });
        var execution = new TrueShuffleJobExecution(job, task);
        executionReference.set(execution);

        try {
            executor.execute(task);
        } catch (RuntimeException exception) {
            task.cancel(false);
            execution.getStatus().setStatusMessage(ETrueShuffleJobStatus.TERMINATED, "Could not schedule job execution: " + exception.getMessage(), exception);
        }
        return execution;
    }

    private void run(
            TrueShuffleJob job,
            TrueShuffleUserResolver resolver,
            TrueShuffleJobExecution execution
    ) {
        var status = execution.getStatus();

        try {
            InternalTrueShuffleUser user = resolver.resolve(job.getUserId());
            status.setStatusMessage(ETrueShuffleJobStatus.EXECUTING, null);
            job.perform(user, status);
            updateStatus(status, ETrueShuffleJobStatus.COMPLETED, null, null);
        } catch (UserNotFoundException exception) {
            updateStatus(status, ETrueShuffleJobStatus.SKIPPED, exception.getMessage(), exception);
        } catch (Exception e) {
            updateStatus(status, ETrueShuffleJobStatus.TERMINATED, e.getMessage(), e);
        } catch (Error throwable) {
            updateStatus(status, ETrueShuffleJobStatus.TERMINATED, throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            var message = status.getMessage();
            if (message != null && !message.isEmpty()) {
                LOGGER.info("Execution of {} with id {} finished with status: {}. {}", job.getClass().getSimpleName(), execution.getId(), status.getStatus(), message, status.getFailure());
            } else {
                LOGGER.info("Execution of {} with id {} finished with status: {}.", job.getClass().getSimpleName(), execution.getId(), status.getStatus(), status.getFailure());
            }
        }
    }

    private static void updateStatus(TrueShuffleJobStatus status, ETrueShuffleJobStatus newStatus, String message, Throwable failure) {
        if (cancellationRequested(status)) {
            status.setStatusMessage(ETrueShuffleJobStatus.CANCELLED, formatCancellationMessage(failure), failure);
        } else {
            status.setStatusMessage(newStatus, message, failure);
        }
    }

    private static String formatCancellationMessage(Throwable failure) {
        if (failure != null) {
            return "Execution was cancelled: " + failure.getMessage();
        } else {
            return "Execution was cancelled";
        }
    }

    private static boolean cancellationRequested(TrueShuffleJobStatus status) {
        // interrupt check only works from worker thread
        return status.getStatus() == ETrueShuffleJobStatus.CANCELLED || Thread.currentThread().isInterrupted();
    }
}
