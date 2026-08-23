package nl.martderoos.trueshuffle.internal.jobs;

import nl.martderoos.trueshuffle.api.jobs.ETrueShuffleJobStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrueShuffleJobStatusTest {
    @Test
    public void testStatusCannotBeNull() {
        assertThrows(NullPointerException.class, () -> new TrueShuffleJobStatus(null, null));
    }

    @Test
    public void testStatusStoresFailure() {
        var failure = new IllegalStateException("failure");
        var status = new TrueShuffleJobStatus(ETrueShuffleJobStatus.WAITING, "waiting");

        status.setStatusMessage(ETrueShuffleJobStatus.TERMINATED, "terminated", failure);

        assertEquals(ETrueShuffleJobStatus.TERMINATED, status.getStatus());
        assertEquals("terminated", status.getMessage());
        assertSame(failure, status.getFailure());
    }

    @Test
    public void testTerminalStatusCannotBeOverwritten() {
        var failure = new IllegalStateException("failure");
        var status = new TrueShuffleJobStatus(ETrueShuffleJobStatus.TERMINATED, "terminated");

        status.setStatusMessage(ETrueShuffleJobStatus.COMPLETED, "completed", failure);

        assertEquals(ETrueShuffleJobStatus.TERMINATED, status.getStatus());
        assertEquals("terminated", status.getMessage());
        assertNull(status.getFailure());
    }
}
