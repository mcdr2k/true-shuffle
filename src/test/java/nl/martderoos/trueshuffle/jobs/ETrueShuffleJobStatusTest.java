package nl.martderoos.trueshuffle.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ETrueShuffleJobStatusTest {
    @Test
    public void testIsWaiting() {
        assertTrue(ETrueShuffleJobStatus.WAITING.isWaiting());
        assertFalse(ETrueShuffleJobStatus.EXECUTING.isWaiting());
        assertFalse(ETrueShuffleJobStatus.FINISHED.isWaiting());
        assertFalse(ETrueShuffleJobStatus.SKIPPED.isWaiting());
        assertFalse(ETrueShuffleJobStatus.TERMINATED.isWaiting());
    }

    @Test
    public void testIsRunning() {
        assertFalse(ETrueShuffleJobStatus.WAITING.isRunning());
        assertTrue(ETrueShuffleJobStatus.EXECUTING.isRunning());
        assertFalse(ETrueShuffleJobStatus.FINISHED.isRunning());
        assertFalse(ETrueShuffleJobStatus.SKIPPED.isRunning());
        assertFalse(ETrueShuffleJobStatus.TERMINATED.isRunning());
    }

    @Test
    public void testIsDone() {
        assertFalse(ETrueShuffleJobStatus.WAITING.isDone());
        assertFalse(ETrueShuffleJobStatus.EXECUTING.isDone());
        assertTrue(ETrueShuffleJobStatus.FINISHED.isDone());
        assertTrue(ETrueShuffleJobStatus.SKIPPED.isDone());
        assertTrue(ETrueShuffleJobStatus.TERMINATED.isDone());
    }
}
