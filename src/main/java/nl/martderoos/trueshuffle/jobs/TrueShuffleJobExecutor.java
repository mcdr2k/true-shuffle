package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.InternalTrueShuffleUser;
import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import nl.martderoos.trueshuffle.requests.exceptions.FatalRequestResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

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
            execution.getStatus().setFailure(exception);
            execution.getStatus().setStatusMessage(ETrueShuffleJobStatus.TERMINATED, "Could not schedule job execution: " + exception.getMessage());
        }
        return execution;
    }

    private void run(
            TrueShuffleJob job,
            TrueShuffleUserResolver resolver,
            TrueShuffleJobExecution execution
    ) {
        var status = execution.getStatus();
        var jobName = job.getClass().getSimpleName() + "-" + job.getUserId();

        try {
            InternalTrueShuffleUser user = resolver.resolve(job.getUserId());
            status.setStatusMessage(ETrueShuffleJobStatus.EXECUTING, null);
            job.perform(user, status);

            if (status.getStatus() == ETrueShuffleJobStatus.EXECUTING) {
                status.setStatusMessage(ETrueShuffleJobStatus.FINISHED, null);
                LOGGER.info("{} completed appropriately", jobName);
            }
        } catch (UserNotFoundException exception) {
            LOGGER.info("Skipped {} because we could not find the specified user: {}", jobName, exception.getMessage());
            status.setFailure(exception);
            status.setStatusMessage(ETrueShuffleJobStatus.SKIPPED, exception.getMessage());
        } catch (FatalRequestResponseException exception) {
            var message = jobName + " could not complete: " + exception.getMessage();
            LOGGER.info(message, exception);
            status.setFailure(exception);
            status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED, message);
        } catch (RuntimeException exception) {
            LOGGER.error("{} encountered an unexpected issue", jobName, exception);
            status.setFailure(exception);
            status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED, "Encountered an unexpected issue");
        } catch (Error error) {
            LOGGER.error("{} encountered an unrecoverable error", jobName, error);
            status.setFailure(error);
            status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED, "Encountered an unrecoverable error");
            throw error;
        }
    }
}
