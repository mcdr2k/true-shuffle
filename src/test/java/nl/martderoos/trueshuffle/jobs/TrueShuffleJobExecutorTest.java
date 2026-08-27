package nl.martderoos.trueshuffle.jobs;

import nl.martderoos.trueshuffle.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class TrueShuffleJobExecutorTest {
    @Test
    public void testUnknownUserIsSkipped() {
        var failure = new UserNotFoundException("unknown");
        var execution = execute(ignored -> {
            throw failure;
        }, Runnable::run);

        assertEquals(ETrueShuffleJobStatus.SKIPPED, execution.getStatus().getStatus());
        assertEquals("Could not find 'unknown' among the set of authorised users.", execution.getStatus().getMessage());
        assertSame(failure, execution.getStatus().getFailure());
    }

    @Test
    public void testFatalFailureTerminatesExecution() {
        var failure = new RuntimeException("fatal");
        var execution = execute(ignored -> {
            throw failure;
        }, Runnable::run);

        assertEquals(ETrueShuffleJobStatus.TERMINATED, execution.getStatus().getStatus());
        assertEquals("fatal", execution.getStatus().getMessage());
        assertSame(failure, execution.getStatus().getFailure());
    }

    @Test
    public void testUnexpectedErrorIsRethrownByAwait() {
        var failure = new AssertionError("unexpected");
        var execution = execute(ignored -> {
            throw failure;
        }, Runnable::run);

        var exception = assertThrows(IllegalStateException.class, execution::await);

        assertSame(failure, exception.getCause());
        assertEquals(ETrueShuffleJobStatus.TERMINATED, execution.getStatus().getStatus());
        assertSame(failure, execution.getStatus().getFailure());
    }

    @Test
    public void testRejectedExecutionTerminatesWithoutBeingCancelled() {
        var failure = new RejectedExecutionException("rejected");
        var execution = execute(
                ignored -> null,
                ignored -> {
                    throw failure;
                }
        );

        assertEquals(ETrueShuffleJobStatus.TERMINATED, execution.getStatus().getStatus());
        assertFalse(execution.isCancelled());
        assertSame(failure, execution.getStatus().getFailure());
    }

    @Test
    public void testCancellationWinsOverInterruptedRequestFailure() throws Exception {
        var resolverStarted = new CountDownLatch(1);
        var worker = new AtomicReference<Thread>();
        Executor executor = command -> {
            var thread = new Thread(command);
            worker.set(thread);
            thread.start();
        };

        var execution = execute(ignored -> {
            resolverStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("request interrupted", exception);
            }
            return null;
        }, executor);

        assertTrue(resolverStarted.await(1, TimeUnit.SECONDS));
        assertTrue(execution.cancel());
        execution.await();
        worker.get().join(1000);

        assertEquals(ETrueShuffleJobStatus.CANCELLED, execution.getStatus().getStatus());
        assertTrue(execution.isCancelled());
    }

    private static InternalTrueShuffleJobExecution execute(TrueShuffleUserResolver resolver, Executor executor) {
        return new TrueShuffleJobExecutor().execute(
                new TrueShuffleLikedJob("user"),
                resolver,
                executor
        );
    }
}
