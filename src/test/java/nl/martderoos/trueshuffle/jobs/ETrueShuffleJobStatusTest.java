package nl.martderoos.trueshuffle.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ETrueShuffleJobStatusTest {
    @Test
    public void testIsWaiting() {
        assertTrue(ETrueShuffleJobStatus.WAITING.isWaiting());
        assertFalse(ETrueShuffleJobStatus.EXECUTING.isWaiting());
        assertFalse(ETrueShuffleJobStatus.COMPLETED.isWaiting());
        assertFalse(ETrueShuffleJobStatus.SKIPPED.isWaiting());
        assertFalse(ETrueShuffleJobStatus.CANCELLED.isWaiting());
        assertFalse(ETrueShuffleJobStatus.TERMINATED.isWaiting());
    }

    @Test
    public void testIsRunning() {
        assertFalse(ETrueShuffleJobStatus.WAITING.isRunning());
        assertTrue(ETrueShuffleJobStatus.EXECUTING.isRunning());
        assertFalse(ETrueShuffleJobStatus.COMPLETED.isRunning());
        assertFalse(ETrueShuffleJobStatus.SKIPPED.isRunning());
        assertFalse(ETrueShuffleJobStatus.CANCELLED.isRunning());
        assertFalse(ETrueShuffleJobStatus.TERMINATED.isRunning());
    }

    @Test
    public void testIsFinished() {
        assertFalse(ETrueShuffleJobStatus.WAITING.isFinished());
        assertFalse(ETrueShuffleJobStatus.EXECUTING.isFinished());
        assertTrue(ETrueShuffleJobStatus.COMPLETED.isFinished());
        assertTrue(ETrueShuffleJobStatus.SKIPPED.isFinished());
        assertTrue(ETrueShuffleJobStatus.CANCELLED.isFinished());
        assertTrue(ETrueShuffleJobStatus.TERMINATED.isFinished());
    }
}
