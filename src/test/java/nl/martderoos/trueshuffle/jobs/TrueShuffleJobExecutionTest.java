package nl.martderoos.trueshuffle.jobs;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class TrueShuffleJobExecutionTest {
    @Test
    public void testCancelUpdatesStatusAndInterruptsFuture() {
        var future = mock(Future.class);
        when(future.isDone()).thenReturn(false);
        when(future.cancel(true)).thenReturn(true);
        var execution = new TrueShuffleJobExecution(new TrueShuffleLikedJob("user"), future);

        assertTrue(execution.cancel());

        assertTrue(execution.isCancelled());
        assertTrue(execution.isDone());
        assertEquals(ETrueShuffleJobStatus.CANCELLED, execution.getStatus().getStatus());
        verify(future).cancel(true);
    }

    @Test
    public void testCancelReturnsFalseWhenFutureIsDone() {
        var future = mock(Future.class);
        when(future.isDone()).thenReturn(true);
        var execution = new TrueShuffleJobExecution(new TrueShuffleLikedJob("user"), future);

        assertFalse(execution.cancel());

        assertFalse(execution.isCancelled());
        verify(future, never()).cancel(anyBoolean());
    }

    @Test
    public void testAwaitWrapsExecutionFailure() throws Exception {
        var cause = new IllegalStateException("failure");
        var future = mock(Future.class);
        when(future.get()).thenThrow(new ExecutionException(cause));
        var execution = new TrueShuffleJobExecution(new TrueShuffleLikedJob("user"), future);

        var exception = assertThrows(IllegalStateException.class, execution::await);

        assertSame(cause, exception.getCause());
    }

    @Test
    public void testTimedAwaitPropagatesTimeout() throws Exception {
        var future = mock(Future.class);
        when(future.get(1, TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        var execution = new TrueShuffleJobExecution(new TrueShuffleLikedJob("user"), future);

        assertThrows(TimeoutException.class, () -> execution.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testExecutionHasUniqueId() {
        var first = new TrueShuffleJobExecution(new TrueShuffleLikedJob("user"), mock(Future.class));
        var second = new TrueShuffleJobExecution(new TrueShuffleLikedJob("user"), mock(Future.class));

        assertNotEquals(first.getId(), second.getId());
    }
}
